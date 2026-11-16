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
package org.flixelgdx.gradle.assets;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates the assets class from the files in the assets folder.
 *
 * <p>The class is written to {@code <outputDir>/<package folders>/<className>.java}. {@link #getOutputDir()}
 * (default {@value #OUTPUT_DIR} in the build folder) is added to the {@code main} Java sources, so compiling the game
 * always runs this task first and the class can be used like any other.
 *
 * <p>The task is cacheable and up to date while the assets folder and the settings are unchanged. Changing what is
 * inside a file reruns it, but the generated class only depends on the file names, so the code does not recompile
 * unless a file is added, removed or renamed.
 */
@CacheableTask
public abstract class GenerateAssetsTask extends DefaultTask {

  /** The default output directory, relative to the build directory. */
  public static final String OUTPUT_DIR = "generated/sources/flixelgdx/assets/java/main";

  /** Writes the assets class. */
  @TaskAction
  public void generate() {
    generate(getAssetsDir().get().getAsFile(), getAssets().getFiles(), getPackageName().get(),
        getClassName().get(), getOutputDir().get().getAsFile());
  }

  /**
   * Writes the assets class for a set of asset files, replacing whatever the output directory held before.
   *
   * <p>The plugin also calls this while Gradle loads the project for an IDE, so the class exists right after a
   * reload, before anything is compiled.
   *
   * @param assetsDir The assets folder the files are in.
   * @param files The asset files to list.
   * @param packageName The package of the class, or an empty string for the default package.
   * @param className The simple name of the class.
   * @param outputDir The source folder to write the class into.
   * @throws GradleException if the names are not valid Java, two assets clash, or the file cannot be written.
   */
  static void generate(File assetsDir, Iterable<File> files, String packageName, String className, File outputDir) {
    Path root = assetsDir.toPath().toAbsolutePath();
    List<String> paths = new ArrayList<>();
    for (File file : files) {
      paths.add(root.relativize(file.toPath().toAbsolutePath()).toString().replace(File.separatorChar, '/'));
    }
    String source = AssetsClassWriter.write(packageName, className, paths);

    Path dir = outputDir.toPath();
    if (!packageName.isEmpty()) {
      dir = dir.resolve(packageName.replace('.', File.separatorChar));
    }
    Path target = dir.resolve(className + ".java");
    try {
      // Skip the write when nothing changed, so the IDE and the compiler do not see a fresh file on every reload.
      if (Files.isRegularFile(target) && Files.readString(target, StandardCharsets.UTF_8).equals(source)) {
        deleteOthers(outputDir.toPath(), target);
        return;
      }
      deleteOthers(outputDir.toPath(), target);
      Files.createDirectories(dir);
      Files.writeString(target, source, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GradleException("Could not write the assets class to " + target + ".", e);
    }
  }

  /** Removes every file in the output folder except the class, such as one left behind by an older package name. */
  private static void deleteOthers(Path outputDir, Path keep) throws IOException {
    if (!Files.isDirectory(outputDir)) {
      return;
    }
    List<Path> stale = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(outputDir)) {
      walk.filter(p -> !p.equals(keep) && !p.equals(outputDir) && !keep.startsWith(p)).forEach(stale::add);
    }
    // Deepest paths first, so folders are already empty when they are deleted.
    stale.sort((a, b) -> b.getNameCount() - a.getNameCount());
    for (Path path : stale) {
      Files.deleteIfExists(path);
    }
  }

  /**
   * The asset files to list, normally every file in {@link #getAssetsDir()} minus the excluded ones.
   *
   * @return The asset files.
   */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getAssets();

  /**
   * The assets folder, which every constant's path is relative to.
   *
   * @return The assets folder property.
   */
  @Internal
  public abstract DirectoryProperty getAssetsDir();

  /**
   * The package of the generated class.
   *
   * @return The package name property. An empty string means the default package.
   */
  @Input
  public abstract Property<String> getPackageName();

  /**
   * The simple name of the generated class.
   *
   * @return The class name property.
   */
  @Input
  public abstract Property<String> getClassName();

  /**
   * The source folder the class is written into.
   *
   * @return The output directory property.
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();
}
