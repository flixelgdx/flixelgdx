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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.util.List;

/**
 * Gradle plugin that packages a FlixelGDX game into a self-contained, double-clickable app for each
 * platform it targets.
 *
 * <p>A packaged game bundles a native launcher, a trimmed Java runtime, and only the code the target
 * platform needs, so a player runs it without installing Java. The plugin is built for FlixelGDX
 * specifically: it knows how the framework ships its per-platform native libraries and keeps only
 * the ones a given target uses, and it caches each downloaded JDK so packaging is fast after the
 * first run.
 *
 * <h2>Usage</h2>
 *
 * <p>Apply the plugin and describe the package in the game module's {@code build.gradle}:
 *
 * <pre>{@code
 * plugins {
 *   id 'java'
 *   id 'org.flixelgdx.packagr' version '<flixel-version>'
 * }
 *
 * packagr {
 *   mainClass = 'com.mygame.DesktopLauncher'
 *
 *   targets {
 *     linuxX64()
 *     windowsX64()
 *   }
 * }
 * }</pre>
 *
 * <p>Each declared target adds a {@code package<Name>} task (for example {@code packageLinuxX64}),
 * and {@code packageAll} builds every target. All of these tasks live in the {@code packagr} group.
 *
 * @see PackagrExtension
 * @see PackageTask
 */
public class PackagrPlugin implements Plugin<Project> {

  private static final String TASK_GROUP = "packagr";

  private static final List<String> DEFAULT_MODULES = List.of(
      "java.base",
      "java.desktop",
      "java.logging",
      "java.management",
      "jdk.unsupported");

  @Override
  public void apply(Project project) {
    PackagrExtension ext =
        project.getExtensions().create(PackagrExtension.NAME, PackagrExtension.class);
    applyConventions(project, ext);

    TaskProvider<Task> packageAll = project.getTasks().register("packageAll", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Packages the game for every declared target.");
    });

    // Packaging needs the game's jar and its runtime classpath, so tasks are wired once the java
    // plugin is present. The target container is reacted to here so a task appears for each target
    // as it is declared in the packagr block.
    project.getPlugins().withId("java",
        plugin -> ext.getTargets().container().all(target -> registerPackageTask(project, ext, target, packageAll)));
  }

  private static void applyConventions(Project project, PackagrExtension ext) {
    ext.getAppName().convention(project.getName());
    ext.getAppVersion().convention(project.provider(() -> String.valueOf(project.getVersion())));
    ext.getJdkVendor().convention("temurin");
    ext.getJdkVersion().convention(17);
    ext.getJlinkModules().convention(DEFAULT_MODULES);
    ext.getJdkCacheDir().convention(project.getLayout().dir(project.provider(
        () -> new File(System.getProperty("user.home"), ".flixelgdx/jdks"))));
  }

  private static void registerPackageTask(Project project, PackagrExtension ext,
      PackagrTarget target, TaskProvider<Task> packageAll) {
    String taskName = "package" + toTaskSuffix(target.getName());
    TaskProvider<Jar> jarTask = project.getTasks().named("jar", Jar.class);

    TaskProvider<PackageTask> packageTask =
        project.getTasks().register(taskName, PackageTask.class, task -> {
          task.setGroup(TASK_GROUP);
          task.setDescription("Packages the game for the " + target.getName() + " target.");
          task.getAppName().set(ext.getAppName());
          task.getMainClass().set(ext.getMainClass());
          task.getJvmArgs().set(ext.getJvmArgs());
          task.getJdkVendor().set(ext.getJdkVendor());
          task.getJdkVersion().set(ext.getJdkVersion());
          task.getModules().set(ext.getJlinkModules());
          task.getTargetOs().set(target.getOs());
          task.getTargetArch().set(target.getArch());
          task.getJdkCacheDir().set(ext.getJdkCacheDir());
          task.getAssetsDir().set(ext.getAssetsDir());
          task.getGameJar().set(jarTask.flatMap(Jar::getArchiveFile));
          task.getRuntimeClasspath().from(project.getConfigurations().named("runtimeClasspath"));
          task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("packagr/" + target.getName()));
          task.dependsOn(jarTask);
        });

    packageAll.configure(task -> task.dependsOn(packageTask));
  }

  /**
   * Turns a target name into the suffix of its task, so {@code linux-x64} becomes {@code LinuxX64}
   * (giving {@code packageLinuxX64}) and {@code steamDeck} becomes {@code SteamDeck}.
   */
  private static String toTaskSuffix(String targetName) {
    StringBuilder sb = new StringBuilder(targetName.length());
    for (String part : targetName.split("[^A-Za-z0-9]+")) {
      if (!part.isEmpty()) {
        sb.append(Character.toUpperCase(part.charAt(0)));
        sb.append(part.substring(1));
      }
    }
    return sb.toString();
  }
}
