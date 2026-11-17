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
package org.flixelgdx.gradle.project;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
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
 * Generates the {@code Project} class from {@code project.toml}.
 *
 * <p>The class is written to {@code <outputDir>/<package folders>/Project.java}. {@link #getOutputDir()} is added to
 * the {@code main} Java sources, so compiling the game always runs this task first.
 *
 * <p>The task is cacheable and only reruns when the contents of {@code project.toml} change.
 */
@CacheableTask
public abstract class GenerateProjectClassTask extends DefaultTask {

  /** The default output directory, relative to the build directory. */
  public static final String OUTPUT_DIR = "generated/sources/flixelProject/java/main";

  /** Writes the project class. */
  @TaskAction
  public void generate() {
    generate(getProjectFile().get().getAsFile(), getOutputDir().get().getAsFile());
  }

  /**
   * Writes the project class for a {@code project.toml}, replacing whatever the output directory held before.
   *
   * <p>The plugin also calls this while Gradle loads the project for an IDE, so the class exists right after a
   * reload, before anything is compiled.
   *
   * @param projectFile The {@code project.toml} to read.
   * @param outputDir The source folder to write the class into.
   * @throws GradleException if the file cannot be read or is invalid, or the class cannot be written.
   */
  static void generate(File projectFile, File outputDir) {
    ProjectModel model;
    try {
      model = ProjectToml.parse(Files.readString(projectFile.toPath(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new GradleException("Could not read " + projectFile + ".", e);
    }
    Path root = outputDir.toPath();
    Path dir = root.resolve(model.packageName().replace('.', '/'));
    Path target = dir.resolve("Project.java");
    String source = ProjectClassWriter.write(model);
    try {
      // Skip the write when nothing changed, so the IDE does not see a fresh file on every reload.
      if (Files.isRegularFile(target) && Files.readString(target, StandardCharsets.UTF_8).equals(source)) {
        return;
      }
      deleteContents(root);
      Files.createDirectories(dir);
      Files.writeString(target, source, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GradleException("Could not write the project class to " + dir + ".", e);
    }
  }

  private static void deleteContents(Path root) throws IOException {
    if (!Files.isDirectory(root)) {
      return;
    }
    List<Path> stale = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(p -> !p.equals(root)).forEach(stale::add);
    }
    // Deepest paths first, so folders are already empty when they are deleted.
    stale.sort((a, b) -> b.getNameCount() - a.getNameCount());
    for (Path path : stale) {
      Files.deleteIfExists(path);
    }
  }

  /**
   * The {@code project.toml} the class is generated from.
   *
   * @return The project file property.
   */
  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  public abstract RegularFileProperty getProjectFile();

  /**
   * The source folder the class is written into.
   *
   * @return The output directory property.
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();
}
