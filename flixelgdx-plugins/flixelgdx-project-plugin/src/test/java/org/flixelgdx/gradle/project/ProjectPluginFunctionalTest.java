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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs real Gradle builds with the plugin applied to the settings file, to check module inclusion, project info and
 * that game code compiles against the generated class.
 */
class ProjectPluginFunctionalTest {

  private static final String TOML = """
      platforms = ["desktop"]

      [app]
      title = "My Game"
      package = "com.example.mygame"
      company = "My Studio"
      version = "0.1.0"
      """;

  /** The smallest stand-in for the real config class, so the test does not need the whole framework. */
  private static final String CONFIG_STUB = """
      package org.flixelgdx;

      public final class FlixelConfig {
        public final String title;
        public int width;

        private FlixelConfig(String title) {
          this.title = title;
        }

        public static final class Builder {
          private final FlixelConfig config;

          public Builder(String title) { config = new FlixelConfig(title); }
          public Builder company(String company) { return this; }
          public Builder version(String version) { return this; }
          public Builder size(int width, int height) { config.width = width; return this; }
          public Builder framerate(int framerate) { return this; }
          public Builder vsync(boolean vsync) { return this; }
          public Builder fullscreen(boolean fullscreen) { return this; }
          public Builder renderResolution(int width, int height, boolean smooth) { return this; }
          public Builder disableRenderResolution() { return this; }
          public Builder renderScale(float scale, boolean smooth) { return this; }
          public Builder transparentFramebuffer(boolean transparent) { return this; }
          public FlixelConfig build() { return config; }
        }
      }
      """;

  @TempDir
  Path dir;

  @Test
  void gameCodeCompilesAgainstTheGeneratedClass() throws IOException {
    setUp(TOML + "[render]\nresolution = [0, 0]\nscale = 0.5\n");
    write("core/src/main/java/org/flixelgdx/FlixelConfig.java", CONFIG_STUB);
    write("core/src/main/java/com/example/mygame/Game.java", """
        package com.example.mygame;

        public class Game {
          public static final String NAME = Project.config().build().title;
        }
        """);

    BuildResult result = runner(":core:compileJava").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":core:generateProjectClass").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, result.task(":core:compileJava").getOutcome());
    assertTrue(Files.isRegularFile(dir.resolve("core/build/classes/java/main/com/example/mygame/Project.class")));
    assertTrue(Files.readString(dir.resolve(
        "core/build/generated/sources/flixelProject/java/main/com/example/mygame/Project.java"))
        .contains("TITLE = \"My Game\""));
  }

  @Test
  void generationIsUpToDateUntilTheFileChanges() throws IOException {
    setUp(TOML);

    assertEquals(TaskOutcome.SUCCESS, runner(":core:generateProjectClass").build()
        .task(":core:generateProjectClass").getOutcome());
    assertEquals(TaskOutcome.UP_TO_DATE, runner(":core:generateProjectClass").build()
        .task(":core:generateProjectClass").getOutcome());

    write("project.toml", TOML.replace("0.1.0", "0.2.0"));
    assertEquals(TaskOutcome.SUCCESS, runner(":core:generateProjectClass").build()
        .task(":core:generateProjectClass").getOutcome());
  }

  @Test
  void ideReloadWritesTheClassWithoutRunningATask() throws IOException {
    setUp(TOML);

    BuildResult result = runner().build();

    assertTrue(result.task(":core:generateProjectClass") == null, result.getOutput());
    assertTrue(generated().contains("TITLE = \"My Game\""), generated());
  }

  @Test
  void intellijSyncWritesTheClass() throws IOException {
    setUp(TOML);

    runner("help", "-Didea.sync.active=true").build();

    assertTrue(generated().contains("TITLE = \"My Game\""), generated());
  }

  @Test
  void androidWithoutAnSdkIsSkippedWithAWarning() throws IOException {
    setUp(TOML.replace("[\"desktop\"]", "[\"desktop\", \"android\"]"));
    Files.createDirectories(dir.resolve("android"));

    BuildResult result = runner("projects").build();

    assertTrue(result.getOutput().contains("no Android SDK was found"), result.getOutput());
    assertTrue(result.getOutput().contains("ANDROID_HOME"), result.getOutput());
    assertTrue(result.getOutput().contains("':desktop'"), result.getOutput());
    assertFalse(result.getOutput().contains("':android'"), result.getOutput());
  }

  @Test
  void androidIsIncludedWhenTheSdkIsFound() throws IOException {
    setUp(TOML.replace("[\"desktop\"]", "[\"desktop\", \"android\"]"));
    Files.createDirectories(dir.resolve("android"));
    Path sdk = Files.createDirectories(dir.resolve("sdk"));
    write("local.properties", "sdk.dir=" + sdk.toString().replace("\\", "\\\\") + "\n");

    BuildResult result = runner("projects").build();

    assertTrue(result.getOutput().contains("':android'"), result.getOutput());
    assertFalse(result.getOutput().contains("no Android SDK"), result.getOutput());
  }

  @Test
  void invalidTitleFailsWithTheFieldName() throws IOException {
    setUp(TOML.replace("My Game", "My/Game"));

    BuildResult result = runner("projects").buildAndFail();

    assertTrue(result.getOutput().contains("app.title"), result.getOutput());
    assertTrue(result.getOutput().contains("'/'"), result.getOutput());
  }

  @Test
  void missingFileSaysWhereItIsExpected() throws IOException {
    setUp(TOML);
    Files.delete(dir.resolve("project.toml"));

    BuildResult result = runner("projects").buildAndFail();

    assertTrue(result.getOutput().contains("project.toml"), result.getOutput());
    assertTrue(result.getOutput().contains(dir.toString()), result.getOutput());
  }

  @Test
  void missingPlatformFolderFails() throws IOException {
    setUp(TOML.replace("[\"desktop\"]", "[\"desktop\", \"html5\"]"));

    BuildResult result = runner("projects").buildAndFail();

    assertTrue(result.getOutput().contains("'html5'"), result.getOutput());
  }

  @Test
  void groupAndVersionAreSetOnEveryProject() throws IOException {
    setUp(TOML);
    write("build.gradle",
        """
            allprojects {
              tasks.register('show') {
                def line = "${project.path} ${project.group} ${project.version} ${flixelProject.title} ${flixelProject.width}"
                doLast { println 'INFO ' + line }
              }
            }
            """);

    BuildResult result = runner("show").build();

    String output = result.getOutput();
    assertTrue(output.contains("INFO : com.example.mygame 0.1.0 My Game 640"), output);
    assertTrue(output.contains("INFO :core com.example.mygame 0.1.0 My Game 640"), output);
    assertTrue(output.contains("INFO :desktop com.example.mygame 0.1.0 My Game 640"), output);
  }

  @Test
  void otherPluginsCanReadTheTitleAndVersion() throws IOException {
    setUp(TOML);
    write("desktop/build.gradle",
        """
            println 'INFO ' + project.findProperty('flixelProject.title') + ' ' + project.findProperty('flixelProject.version')
            """);

    BuildResult result = runner("projects").build();

    assertTrue(result.getOutput().contains("INFO My Game 0.1.0"), result.getOutput());
  }

  private String generated() throws IOException {
    return Files.readString(dir.resolve(
        "core/build/generated/sources/flixelProject/java/main/com/example/mygame/Project.java"));
  }

  private void setUp(String toml) throws IOException {
    write("settings.gradle", "plugins {\n  id 'org.flixelgdx.project'\n}\nrootProject.name = 'game'\n");
    write("project.toml", toml);
    write("core/build.gradle", "plugins {\n  id 'java-library'\n}\n");
    Files.createDirectories(dir.resolve("desktop"));
  }

  private GradleRunner runner(String... arguments) {
    // The test must not depend on the machine it runs on, so every Android variable is dropped.
    Map<String, String> environment = new HashMap<>(System.getenv());
    environment.remove("ANDROID_HOME");
    environment.remove("ANDROID_SDK_ROOT");
    GradleRunner runner = GradleRunner.create()
        .withProjectDir(dir.toFile())
        .withPluginClasspath()
        .withEnvironment(environment)
        .withArguments(arguments);
    String home = System.getProperty("flixel.test.gradleUserHome");
    if (home != null) {
      runner.withTestKitDir(Path.of(home).toFile());
    }
    return runner;
  }

  private void write(String relative, String text) throws IOException {
    Path file = dir.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, text, StandardCharsets.UTF_8);
  }
}
