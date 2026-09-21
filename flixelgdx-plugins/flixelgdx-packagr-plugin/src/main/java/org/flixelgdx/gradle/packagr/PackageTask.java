/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.gradle.packagr;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Assembles one self-contained, runnable package for a single target platform.
 *
 * <p>Each run produces a directory a player can copy and double-click: a native launcher executable,
 * a trimmed Java runtime, the game jar and just the dependency jars that platform needs, and the
 * small config the launcher reads to start the JVM. The steps are, in order: resolve and cache the
 * target JDK ({@link JdkResolver}), build the trimmed runtime with {@link Jlink}, copy the game and
 * its platform-matched dependencies (see {@link NativeArtifacts}), copy any configured assets,
 * unpack the launcher for the target, and write the launcher's {@link LaunchConfig configuration}.
 *
 * <p>The launcher for a platform is a small binary bundled in this plugin. When none is bundled for
 * the requested target, packaging fails with a clear message rather than producing a package that
 * cannot start.
 */
@DisableCachingByDefault(
    because = "Packaging produces large, environment-specific output and is not worth build-cache storage.")
public abstract class PackageTask extends DefaultTask {

  private static final String STUB_ROOT = "/org/flixelgdx/gradle/packagr/stub/";

  /** The application name, used for the launcher executable file name. */
  @Input
  public abstract Property<String> getAppName();

  /** The fully qualified main class the launcher starts. */
  @Input
  public abstract Property<String> getMainClass();

  /** The extra arguments passed to the bundled JVM at startup. */
  @Input
  public abstract ListProperty<String> getJvmArgs();

  /** The JDK vendor a trimmed runtime is built from. */
  @Input
  public abstract Property<String> getJdkVendor();

  /** The JDK feature version a trimmed runtime is built from. */
  @Input
  public abstract Property<Integer> getJdkVersion();

  /** The Java modules included in the trimmed runtime. */
  @Input
  public abstract ListProperty<String> getModules();

  /** The operating system this package targets. */
  @Input
  public abstract Property<OperatingSystem> getTargetOs();

  /** The architecture this package targets. */
  @Input
  public abstract Property<Architecture> getTargetArch();

  /** The game's own jar, shipped as the package's main code. */
  @InputFile
  @PathSensitive(PathSensitivity.NAME_ONLY)
  public abstract RegularFileProperty getGameJar();

  /** The game's full runtime classpath; native jars are filtered to the target from it. */
  @Classpath
  public abstract ConfigurableFileCollection getRuntimeClasspath();

  /** An optional directory of assets copied into the package next to the launcher. */
  @InputDirectory
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract DirectoryProperty getAssetsDir();

  /** The shared cache directory downloaded JDKs are stored in (not part of up-to-date checks). */
  @Internal
  public abstract DirectoryProperty getJdkCacheDir();

  /** The directory the finished package is written to. */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  /**
   * Builds the package.
   *
   * @throws IOException When a file cannot be read, written, or downloaded.
   */
  @TaskAction
  public void packageGame() throws IOException {
    if (!getMainClass().isPresent()) {
      throw new GradleException("packagr needs a main class. Set 'mainClass' in the packagr block, "
          + "for example: mainClass = \"com.mygame.DesktopLauncher\".");
    }
    OperatingSystem os = getTargetOs().getOrNull();
    Architecture arch = getTargetArch().getOrNull();
    if (os == null || arch == null) {
      throw new GradleException("packagr target '" + getName() + "' is missing an operating system "
          + "or architecture. Use a convenience method such as linuxX64(), or set both 'os' and "
          + "'arch' on the target.");
    }

    Path out = getOutputDir().get().getAsFile().toPath();
    deleteRecursively(out);
    Files.createDirectories(out);

    Path jdkHome = JdkResolver.resolve(getJdkCacheDir().get().getAsFile().toPath(),
        getJdkVendor().get(), getJdkVersion().get(), os, arch, getLogger());

    File jlinkExe = Jlink.locateHostJlink();
    if (jlinkExe == null) {
      throw new GradleException("Could not find jlink in the JDK running this build. Run the build "
          + "with a full JDK (not a JRE) so packagr can build the trimmed runtime.");
    }
    Path jre = out.resolve("jre");
    Jlink.run(jlinkExe, getJdkVersion().get(), jdkHome.resolve("jmods"), getModules().get(), jre,
        getLogger());

    Path lib = out.resolve("lib");
    Files.createDirectories(lib);
    File gameJar = getGameJar().get().getAsFile();
    Files.copy(gameJar.toPath(), lib.resolve(gameJar.getName()), StandardCopyOption.REPLACE_EXISTING);
    copyDependencies(lib, os, arch);

    copyAssets(out);
    extractLauncher(out, os, arch);
    writeConfig(out);

    getLogger().lifecycle("[packagr] Packaged '{}' for {}-{} at {}.", getAppName().get(), os.token(),
        arch.token(), out);
  }

  private void copyDependencies(Path lib, OperatingSystem os, Architecture arch) throws IOException {
    for (File file : getRuntimeClasspath().getFiles()) {
      // Project output shows up as directories, not jars; the game's own code ships as its jar, so
      // only external jars are copied here, and native jars are kept only for this target.
      if (!file.isFile() || !file.getName().endsWith(".jar")) {
        continue;
      }
      if (!NativeArtifacts.includes(file.getName(), os, arch)) {
        continue;
      }
      Files.copy(file.toPath(), lib.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void copyAssets(Path out) throws IOException {
    if (!getAssetsDir().isPresent()) {
      return;
    }
    File assets = getAssetsDir().get().getAsFile();
    if (!assets.isDirectory()) {
      return;
    }
    Path source = assets.toPath();
    Path dest = out.resolve(assets.getName());
    try (Stream<Path> stream = Files.walk(source)) {
      for (Path path : (Iterable<Path>) stream::iterator) {
        Path relative = source.relativize(path);
        Path target = dest.resolve(relative);
        if (Files.isDirectory(path)) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  private void extractLauncher(Path out, OperatingSystem os, Architecture arch) throws IOException {
    String classifier = os.token() + "-" + arch.token();
    String resource = STUB_ROOT + classifier + "/launcher" + os.exeSuffix();
    Path launcher = out.resolve(getAppName().get() + os.exeSuffix());
    try (InputStream in = PackageTask.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new GradleException("No launcher is bundled for " + classifier + ". Build one for this "
            + "platform (see the plugin's stub/README.md) and commit it, or package for a platform "
            + "that already has a bundled launcher.");
      }
      Files.copy(in, launcher, StandardCopyOption.REPLACE_EXISTING);
    }
    if (os != OperatingSystem.WINDOWS) {
      launcher.toFile().setExecutable(true, false);
    }
  }

  private void writeConfig(Path out) throws IOException {
    String config = LaunchConfig.render(getMainClass().get(), getJvmArgs().get());
    Files.writeString(out.resolve("packagr.cfg"), config, StandardCharsets.UTF_8);
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(path)) {
      stream.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new RuntimeException("Could not clean the package output: " + p, e);
        }
      });
    }
  }
}
