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

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * Settings plugin that makes a game's {@code project.toml} the single source of truth for what the game is called and
 * how it starts.
 *
 * <p>Without it, the same facts (the title, the version, the window size) get typed into several build files and the
 * game's code, and they drift apart. With it, they live in one file next to {@code settings.gradle}, which can be
 * written by hand or by tools:
 *
 * <pre>{@code
 * [app]
 * title    = "My Game"
 * package  = "com.example.mygame"
 * company  = "My Studio"
 * version  = "0.1.0"
 * language = "java"
 *
 * [window]
 * width  = 640
 * height = 360
 *
 * platforms = ["desktop", "html5", "android"]
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // settings.gradle
 * plugins {
 *   id 'org.flixelgdx.project' version '<flixel-version>'
 * }
 * }</pre>
 *
 * <p>The plugin then:
 *
 * <ul>
 *   <li>Includes {@code core} and every listed platform module. {@code android} is only included when an Android SDK
 *       is found, otherwise a warning explains how to install or point to one.</li>
 *   <li>Sets {@code group} to the package and {@code version} to the version on every project, and adds the
 *       {@link ProjectExtension} as {@code flixelProject}.</li>
 *   <li>Generates a {@code Project} class for the {@code core} module, see {@link GenerateProjectClassTask}. It is
 *       also written when an IDE reloads the Gradle project, so it resolves without a build first.</li>
 *   <li>Makes the defaults of the {@code org.flixelgdx.html5} (page title) and {@code org.flixelgdx.packagr}
 *       (application name and version) plugins follow the file, by publishing the values as the project properties
 *       {@value #TITLE_PROPERTY} and {@value #VERSION_PROPERTY}. Those plugins read plain properties, so they need no
 *       dependency on this one.</li>
 * </ul>
 *
 * @see ProjectToml
 */
public class ProjectPlugin implements Plugin<Settings> {

  /** The name of the file, which sits in the same folder as the settings file. */
  public static final String FILE_NAME = "project.toml";

  /** The name of the task that generates the {@code Project} class. */
  public static final String TASK_NAME = "generateProjectClass";

  /** The project property that holds the title, read by other FlixelGDX plugins for their defaults. */
  public static final String TITLE_PROPERTY = "flixelProject.title";

  /** The project property that holds the version, read by other FlixelGDX plugins for their defaults. */
  public static final String VERSION_PROPERTY = "flixelProject.version";

  private static final Logger LOGGER = Logging.getLogger(ProjectPlugin.class);

  @Override
  public void apply(Settings settings) {
    File dir = settings.getSettingsDir();
    File file = new File(dir, FILE_NAME);
    // Read through the providers, so a configuration cache entry is dropped when the file changes.
    String text = settings.getProviders().fileContents(settings.getLayout().getSettingsDirectory().file(FILE_NAME))
        .getAsText().getOrNull();
    if (text == null) {
      throw new GradleException("Could not find " + FILE_NAME + ". It is expected next to the settings file, at "
          + file.getAbsolutePath() + ".");
    }
    ProjectModel model = ProjectToml.parse(text);

    includeModules(settings, model, dir);

    ProjectExtension info = new ProjectExtension(model);
    settings.getGradle().beforeProject(project -> configure(project, info, file));
  }

  private static void includeModules(Settings settings, ProjectModel model, File dir) {
    include(settings, dir, "core");
    for (String platform : model.platforms()) {
      if (platform.equals("android") && !hasAndroidSdk(settings, dir)) {
        LOGGER.warn("The 'android' platform is listed in {}, but no Android SDK was found, so the android module is "
            + "not included. Install the Android SDK (for example through Android Studio) and set sdk.dir in "
            + "local.properties next to the settings file, or set the ANDROID_HOME environment variable.",
            FILE_NAME);
        continue;
      }
      include(settings, dir, platform);
    }
  }

  private static void include(Settings settings, File dir, String module) {
    if (!new File(dir, module).isDirectory()) {
      throw new GradleException("The module '" + module + "' is needed by " + FILE_NAME + ", but its folder "
          + new File(dir, module).getAbsolutePath() + " does not exist. Create the folder or remove it from "
          + "platforms.");
    }
    settings.include(module);
  }

  private static boolean hasAndroidSdk(Settings settings, File dir) {
    String local = settings.getProviders()
        .fileContents(settings.getLayout().getSettingsDirectory().file("local.properties"))
        .getAsText().getOrNull();
    if (local != null) {
      Properties properties = new Properties();
      try {
        properties.load(new StringReader(local));
      } catch (IOException e) {
        throw new GradleException("Could not read local.properties in " + dir + ".", e);
      }
      if (isDirectory(properties.getProperty("sdk.dir"))) {
        return true;
      }
    }
    return isDirectory(settings.getProviders().environmentVariable("ANDROID_HOME").getOrNull())
        || isDirectory(settings.getProviders().environmentVariable("ANDROID_SDK_ROOT").getOrNull());
  }

  private static boolean isDirectory(String path) {
    return path != null && !path.isBlank() && new File(path.trim()).isDirectory();
  }

  private static void configure(Project project, ProjectExtension info, File file) {
    ProjectModel model = info.getModel();
    project.setGroup(model.packageName());
    project.setVersion(model.version());
    project.getExtensions().add(ProjectExtension.class, ProjectExtension.NAME, info);
    project.getExtensions().getExtraProperties().set(TITLE_PROPERTY, model.title());
    project.getExtensions().getExtraProperties().set(VERSION_PROPERTY, model.version());

    if (project.getPath().equals(":core")) {
      project.getPlugins().withId("java", plugin -> registerCodegen(project, file));
    }
  }

  private static void registerCodegen(Project project, File file) {
    TaskProvider<GenerateProjectClassTask> generate =
        project.getTasks().register(TASK_NAME, GenerateProjectClassTask.class, task -> {
          task.setGroup("build");
          task.setDescription("Generates the Project class from project.toml.");
          task.getProjectFile().set(file);
          task.getOutputDir().convention(
              project.getLayout().getBuildDirectory().dir(GenerateProjectClassTask.OUTPUT_DIR));
        });

    // Registering the output as a source folder (instead of a plain path) makes everything that reads the sources run
    // the task first. The Kotlin plugin compiles the Java source folders too, and the explicit dependency below keeps
    // compileKotlin ordered after the task even if a version reads the folder without that link.
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    Provider<Directory> out = generate.flatMap(GenerateProjectClassTask::getOutputDir);
    sourceSets.getByName("main").getJava().srcDir(out);

    project.getTasks().matching(t -> t.getName().equals("compileKotlin")).configureEach(t -> t.dependsOn(generate));

    // The idea and eclipse plugins write IDE project files from the sources, so the class should exist by then.
    project.getTasks().matching(t -> t.getName().equals("ideaModule") || t.getName().equals("eclipseClasspath"))
        .configureEach(t -> t.dependsOn(generate));

    if (isIdeReload(project)) {
      project.afterEvaluate(p -> generateNow(p, file, generate));
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

  /** Writes the project class right away, so it exists as soon as the IDE has reloaded the project. */
  private static void generateNow(Project project, File file, TaskProvider<GenerateProjectClassTask> task) {
    try {
      GenerateProjectClassTask.generate(file, task.get().getOutputDir().get().getAsFile());
    } catch (GradleException e) {
      // A reload should never fail because of the file. The same error fails the next build, where it belongs.
      project.getLogger().warn("Could not generate the project class: {}", e.getMessage());
    }
  }
}
