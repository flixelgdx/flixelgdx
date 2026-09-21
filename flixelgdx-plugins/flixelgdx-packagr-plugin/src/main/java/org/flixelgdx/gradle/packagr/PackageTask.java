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
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.jspecify.annotations.NonNull;

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
 * <p>Each run produces a directory a player can copy and double-click, and a zip of it in the root
 * project's {@code dist} folder for sharing. The directory holds a native launcher executable, a
 * trimmed Java runtime, the game jar and just the dependency jars that platform needs, and the small
 * config the launcher reads to start the JVM. The steps are, in order: resolve and cache the target
 * runtime ({@link JdkResolver}), build the trimmed runtime with {@link Jlink}, copy the game and its
 * platform-matched dependencies (see {@link NativeArtifacts}), unpack the launcher for the target,
 * write the launcher's {@link LaunchConfig configuration}, and zip the result.
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

  /** The feature version of the Java runtime to bundle. */
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

  /** The target's name, used for the output folder and the dist zip file name. */
  @Input
  public abstract Property<String> getTargetName();

  /** The game's own jar, shipped as the package's main code. */
  @InputFile
  @PathSensitive(PathSensitivity.NAME_ONLY)
  public abstract RegularFileProperty getGameJar();

  /** The game's full runtime classpath; native jars are filtered to the target from it. */
  @Classpath
  public abstract ConfigurableFileCollection getRuntimeClasspath();

  /** The shared cache directory downloaded runtimes are stored in (not part of up-to-date checks). */
  @Internal
  public abstract DirectoryProperty getJdkCacheDir();

  /** The directory the finished package is written to. */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  /** The distributable zip of the package, written to the root project's dist folder. */
  @OutputFile
  public abstract RegularFileProperty getDistZip();

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
    Architecture arch = getArchitecture(os);

    Path out = getOutputDir().get().getAsFile().toPath();
    deleteRecursively(out);
    Files.createDirectories(out);

    Path cache = getJdkCacheDir().get().getAsFile().toPath();
    int version = getJdkVersion().get();

    // The target JDK supplies both the modules to trim (its jmods) and, when it is for this same
    // machine, the jlink that assembles them. jlink is version-strict: its version must match the
    // modules', so a target-version jlink is always used rather than the build's own JDK.
    Path targetJdk = JdkResolver.resolve(cache, version, os, arch, getLogger());
    Path linkerJdk = resolveLinkerJdk(cache, version, os, arch, targetJdk);
    File jlinkExe = new File(linkerJdk.toFile(), "bin/jlink" + HostPlatform.os().exeSuffix());
    if (!jlinkExe.isFile()) {
      throw new GradleException("The resolved JDK has no jlink at " + jlinkExe + "; it may be a JRE "
          + "rather than a full JDK.");
    }
    Path jre = out.resolve("jre");
    Jlink.run(jlinkExe, version, targetJdk.resolve("jmods"), getModules().get(), jre, getLogger());

    Path lib = out.resolve("lib");
    Files.createDirectories(lib);
    File gameJar = getGameJar().get().getAsFile();
    Files.copy(gameJar.toPath(), lib.resolve(gameJar.getName()), StandardCopyOption.REPLACE_EXISTING);
    copyDependencies(lib, os, arch);

    extractLauncher(out, os, arch);
    writeConfig(out);

    Path distZip = getDistZip().get().getAsFile().toPath();
    Archives.zipDirectory(out, distZip, getAppName().get() + "-" + getTargetName().get());

    getLogger().lifecycle("[packagr] Packaged '{}' for {}-{}: {} (zip: {}).", getAppName().get(),
        os.token(), arch.token(), out, distZip);
  }

  private @NonNull Architecture getArchitecture(OperatingSystem os) {
    Architecture arch = getTargetArch().getOrNull();
    if (os == null || arch == null) {
      throw new GradleException("packagr target '" + getName() + "' is missing an operating system "
          + "or architecture. Use a convenience method such as linuxX64(), or set both 'os' and "
          + "'arch' on the target.");
    }
    if (os == OperatingSystem.MACOS && arch == Architecture.X64) {
      throw new GradleException("macOS on Intel (x86_64) is not supported, since Apple has deprecated "
          + "it. Target Apple Silicon with macosArm64() instead.");
    }
    return arch;
  }

  /**
   * Returns the JDK whose {@code jlink} both runs on this machine and matches the target version.
   *
   * <p>When the target platform is the one the build runs on, the already-downloaded target JDK
   * serves both roles. Otherwise a host-platform JDK of the same version is downloaded (and cached)
   * purely to provide a {@code jlink} that runs here, while the target JDK's modules are still what
   * gets linked.
   */
  private Path resolveLinkerJdk(Path cache, int version, OperatingSystem targetOs,
      Architecture targetArch, Path targetJdk) throws IOException {
    OperatingSystem hostOs = HostPlatform.os();
    Architecture hostArch = HostPlatform.arch();
    if (targetOs == hostOs && targetArch == hostArch) {
      return targetJdk;
    }
    if (hostArch == null) {
      throw new GradleException("Cannot build a runtime for " + targetOs.token() + "-"
          + targetArch.token() + " because this machine's architecture was not recognized (os.arch="
          + System.getProperty("os.arch") + "). Package on a 64-bit x86 or ARM machine.");
    }
    return JdkResolver.resolve(cache, version, hostOs, hostArch, getLogger());
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
