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

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;

/**
 * Gradle plugin that generates a type-safe {@code Assets} class listing every file in a game's assets folder.
 *
 * <p>Asset paths are usually typed as plain strings, so a typo or a renamed file only shows up when the game runs
 * and fails to load something. This plugin turns the assets folder into Java code: every folder becomes a nested
 * class in PascalCase, and every file becomes an UPPER_SNAKE_CASE constant holding its path. A missing or renamed
 * file then breaks the build instead of the game.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * plugins {
 *   id 'java'
 *   id 'org.flixelgdx.assets' version '<flixel-version>'
 * }
 *
 * assetsClass {
 *   packageName = 'com.mygame'
 * }
 * }</pre>
 *
 * <p>With {@code assets/shared/sprites/player.png} in the project, game code can then write:
 *
 * <pre>{@code
 * Flixel.assets.load(Assets.Shared.Sprites.PLAYER_PNG); // "shared/sprites/player.png"
 * }</pre>
 *
 * <h2>When the class is generated</h2>
 *
 * <ul>
 *   <li>Before the code compiles: the {@value #TASK_NAME} task's output folder is a Java source folder of the
 *       {@code main} source set, so {@code compileJava} (and anything else that reads the sources) runs it first.</li>
 *   <li>When an IDE reloads the Gradle project: the class is written while the project is loaded, so code
 *       completion knows about new files right after a reload, before anything is built.</li>
 * </ul>
 *
 * @see AssetsExtension
 * @see GenerateAssetsTask
 */
public class AssetsPlugin implements Plugin<Project> {

  /** The name of the task that generates the assets class. */
  public static final String TASK_NAME = "generateAssetsClass";

  /** Patterns that are always excluded, so hidden files such as {@code .DS_Store} never get a constant. */
  private static final List<String> HIDDEN = List.of("**/.*", "**/.*/**");

  @Override
  public void apply(Project project) {
    AssetsExtension ext = project.getExtensions().create(AssetsExtension.NAME, AssetsExtension.class);
    ext.getAssetsDir().convention(
        project.getRootProject().getLayout().getProjectDirectory().dir(AssetsExtension.DEFAULT_ASSETS_DIR));
    ext.getPackageName().convention(project.provider(() -> String.valueOf(project.getGroup())));
    ext.getClassName().convention(AssetsExtension.DEFAULT_CLASS_NAME);

    TaskProvider<GenerateAssetsTask> generate =
        project.getTasks().register(TASK_NAME, GenerateAssetsTask.class, task -> {
          task.setGroup("build");
          task.setDescription("Generates the type-safe assets class from the files in the assets folder.");
          task.getAssetsDir().convention(ext.getAssetsDir());
          task.getAssets().from(ext.getAssetsDir().map(dir -> assetTree(project, dir, ext)));
          task.getPackageName().convention(ext.getPackageName());
          task.getClassName().convention(ext.getClassName());
          task.getOutputDir().convention(project.getLayout().getBuildDirectory().dir(GenerateAssetsTask.OUTPUT_DIR));
        });

    // Registering the task's output as a source folder (instead of a plain path) is what makes compileJava, the
    // sources jar, Javadoc and IDE imports all run the task first.
    project.getPlugins().withId("java", plugin -> {
      SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
      Provider<Directory> out = generate.flatMap(GenerateAssetsTask::getOutputDir);
      sourceSets.getByName("main").getJava().srcDir(out);
    });

    // The idea and eclipse plugins write IDE project files from the sources, so the class should exist by then.
    project.getTasks().matching(t -> t.getName().equals("ideaModule") || t.getName().equals("eclipseClasspath"))
        .configureEach(t -> t.dependsOn(generate));

    if (isIdeReload(project)) {
      project.afterEvaluate(p -> generateNow(p, ext, generate));
    }
  }

  /**
   * Tells whether Gradle is only loading the project for an IDE, without running any task.
   *
   * <p>IntelliJ IDEA (and Android Studio) set {@code idea.sync.active} while they sync. Other IDEs, such as Eclipse
   * and VS Code, load the project through the Gradle tooling API without naming a task, which this also covers.
   */
  private static boolean isIdeReload(Project project) {
    return Boolean.parseBoolean(System.getProperty("idea.sync.active"))
        || project.getGradle().getStartParameter().getTaskNames().isEmpty();
  }

  /** Writes the assets class right away, so it exists as soon as the IDE has reloaded the project. */
  private static void generateNow(Project project, AssetsExtension ext, TaskProvider<GenerateAssetsTask> task) {
    try {
      Directory dir = ext.getAssetsDir().get();
      GenerateAssetsTask.generate(dir.getAsFile(), assetTree(project, dir, ext).getFiles(),
          ext.getPackageName().get(), ext.getClassName().get(), task.get().getOutputDir().get().getAsFile());
    } catch (GradleException e) {
      // A reload should never fail because of an asset name. The same error fails the next build, where it belongs.
      project.getLogger().warn("Could not generate the assets class: {}", e.getMessage());
    }
  }

  private static ConfigurableFileTree assetTree(Project project, Directory dir, AssetsExtension ext) {
    return project.fileTree(dir, tree -> {
      tree.exclude(HIDDEN);
      tree.exclude(ext.getExcludes().get());
    });
  }
}
