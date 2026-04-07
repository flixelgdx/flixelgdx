/*********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 *********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.teavm;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Gradle plugin that automates the web assembly steps required to run a FlixelGDX game via
 * TeaVM in a browser.
 *
 * <p>Apply alongside {@code org.teavm} in the web module's {@code build.gradle}. The plugin
 * registers three helper tasks and wires them as dependencies of the TeaVM build tasks so that
 * every {@code generateJavaScript} or {@code javaScriptDevServer} run produces a complete,
 * browser-ready webapp directory.
 *
 * <h2>What the plugin provides</h2>
 *
 * <ul>
 *   <li>{@code flixelCopyAssets} - copies the game's asset directory into
 *       {@code <outputDir>/assets/}.</li>
 *   <li>{@code flixelCopyWebApp} - copies everything in {@code src/main/webapp/} into
 *       {@code <outputDir>/} (skipped if the directory does not exist).</li>
 *   <li>{@code flixelGenerateIndexHtml} - writes a default {@code index.html} into
 *       {@code <outputDir>/} when no {@code index.html} is present in the webapp directory.
 *       The generated page uses the canvas ID configured via {@link FlixelTeaVMExtension}.</li>
 * </ul>
 *
 * <p>All three tasks are added as dependencies of both {@code generateJavaScript} and
 * {@code javaScriptDevServer} (when those tasks exist on the project).
 *
 * <h2>Minimal usage</h2>
 *
 * <p>In the web module's {@code build.gradle}:
 *
 * <pre>{@code
 * plugins {
 *   id 'org.teavm' version '0.13.0'
 *   id 'me.stringdotjar.flixelgdx.teavm' version '0.1.0-beta'
 * }
 *
 * teavm {
 *   all { mainClass = 'com.mygame.teavm.MyTeaVMLauncher' }
 *   js {
 *     addedToWebApp = true
 *     targetFileName = 'teavm.js'
 *     outputDir = file("$buildDir/dist/webapp")
 *   }
 * }
 *
 * dependencies {
 *   implementation project(':core')
 * }
 * }</pre>
 *
 * <p>In the root {@code settings.gradle}, add the FlixelGDX plugin repository to
 * {@code pluginManagement} so Gradle can resolve the plugin:
 *
 * <pre>{@code
 * pluginManagement {
 *   repositories {
 *     mavenLocal()
 *     maven { url 'https://jitpack.io' }
 *     gradlePluginPortal()
 *   }
 * }
 * }</pre>
 *
 * <h2>Optional customization</h2>
 *
 * <pre>{@code
 * flixelgdx {
 *   canvasId = 'my-canvas'                      // default: 'flixelgdx-canvas'
 *   outputDir = file("$buildDir/dist/webapp")   // default: same value
 *   assetsDir = file('../assets')               // default: rootProject/assets/
 *   webappDir = file('src/main/webapp')         // default: same value
 *   generateDefaultIndexHtml = true             // default: true
 * }
 * }</pre>
 *
 * @see FlixelTeaVMExtension
 */
public class FlixelTeaVMPlugin implements Plugin<Project> {

  private static final String TASK_GROUP = "flixelgdx web";
  private static final String INDEX_TEMPLATE = "/me/stringdotjar/flixelgdx/gradle/teavm/default-index.html";

  @Override
  public void apply(Project project) {
    FlixelTeaVMExtension ext = project.getExtensions().create(FlixelTeaVMExtension.NAME, FlixelTeaVMExtension.class);

    ext.getCanvasId().convention(FlixelTeaVMExtension.DEFAULT_CANVAS_ID);
    ext.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("dist/webapp"));
    ext.getWebappDir().convention(project.getLayout().getProjectDirectory().dir("src/main/webapp"));
    ext.getAssetsDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("assets"));
    ext.getGenerateDefaultIndexHtml().convention(true);

    // Create a task to automatically copy the assets folder into the build output.
    project.getTasks().register("flixelCopyAssets", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Copies game assets from the assets directory into the web output directory.");
      task.from(ext.getAssetsDir());
      task.into(ext.getOutputDir().dir("assets"));
    });

    // Copy any user-made web config files into the build output.
    project.getTasks().register("flixelCopyWebApp", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription( "Copies user-provided web resources (e.g. a custom index.html) into the web output directory.");
      task.onlyIf(t -> ext.getWebappDir().get().getAsFile().exists());
      task.from(ext.getWebappDir());
      task.into(ext.getOutputDir());
    });

    // Create a task that automatically runs if no index.html file is detected.
    project.getTasks().register("flixelGenerateIndexHtml", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Generates a default index.html if none is found in the webapp source directory.");
      task.onlyIf(t -> {
        if (!ext.getGenerateDefaultIndexHtml().get()) {
          return false;
        }
        File userIndex = new File(ext.getWebappDir().get().getAsFile(), "index.html");
        return !userIndex.exists();
      });
      // Create a default index.html file, copied from the resources folder.
      task.doLast(t -> {
        try {
          String template;
          try (InputStream in = FlixelTeaVMPlugin.class.getResourceAsStream(INDEX_TEMPLATE)) {
            if (in == null) {
              throw new IOException("default-index.html template not found in plugin JAR at " + INDEX_TEMPLATE);
            }
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          }
          String html = template.replace("{{CANVAS_ID}}", ext.getCanvasId().get());
          File outputDir = ext.getOutputDir().get().getAsFile();
          outputDir.mkdirs();
          Files.writeString(new File(outputDir, "index.html").toPath(), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new RuntimeException("FlixelGDX: failed to generate default index.html.", e);
        }
      });
    });

    // Connect the new tasks to automatically run when either the Java bytecode is being converted
    // to JavaScript or when a local developer server is created to test the game using the plugin.
    project.afterEvaluate(p -> {
      wireTo(p, "generateJavaScript");
      wireTo(p, "javaScriptDevServer");
    });
  }

  private void wireTo(Project project, String taskName) {
    Task task = project.getTasks().findByName(taskName);
    if (task != null) {
      task.dependsOn(
          project.getTasks().named("flixelCopyAssets"),
          project.getTasks().named("flixelCopyWebApp"),
          project.getTasks().named("flixelGenerateIndexHtml"));
    } else {
      project.getLogger().warn("[FlixelGDX] Task '{}' not found. Make sure 'org.teavm' is applied before 'me.stringdotjar.flixelgdx.teavm'.", taskName);
    }
  }
}
