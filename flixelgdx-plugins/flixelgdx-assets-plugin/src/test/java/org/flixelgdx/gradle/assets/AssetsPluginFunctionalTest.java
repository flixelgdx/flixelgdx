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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs real Gradle builds with the plugin applied, to check that game code can use the generated class and that it
 * is written at the right moments.
 */
class AssetsPluginFunctionalTest {

  private static final String GENERATED =
      "core/build/generated/flixelgdx/assets/com/mygame/Assets.java";

  @TempDir
  Path dir;

  @Test
  void gameCodeCompilesAgainstTheGeneratedClass() throws IOException {
    setUp("");
    touch("assets/shared/sprites/player.png");
    touch("assets/music/theme.ogg");
    useInGame("Assets.Shared.Sprites.PLAYER_PNG + Assets.Music.THEME_OGG");

    BuildResult result = runner(":core:compileJava").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":core:generateAssetsClass").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, result.task(":core:compileJava").getOutcome());
    assertTrue(Files.isRegularFile(dir.resolve("core/build/classes/java/main/com/mygame/Assets$Shared$Sprites.class")));
  }

  @Test
  void removingAUsedFileBreaksTheBuild() throws IOException {
    setUp("");
    touch("assets/shared/sprites/player.png");
    useInGame("Assets.Shared.Sprites.PLAYER_PNG");
    runner(":core:compileJava").build();

    Files.delete(dir.resolve("assets/shared/sprites/player.png"));
    touch("assets/shared/sprites/hero.png");
    BuildResult result = runner(":core:compileJava").buildAndFail();

    assertTrue(result.getOutput().contains("PLAYER_PNG"), result.getOutput());
  }

  @Test
  void taskIsUpToDateUntilTheFolderChanges() throws IOException {
    setUp("");
    touch("assets/a.png");

    assertEquals(TaskOutcome.SUCCESS, outcome(runner(":core:generateAssetsClass").build()));
    assertEquals(TaskOutcome.UP_TO_DATE, outcome(runner(":core:generateAssetsClass").build()));

    touch("assets/b.png");
    assertEquals(TaskOutcome.SUCCESS, outcome(runner(":core:generateAssetsClass").build()));
    assertTrue(generated().contains("B_PNG"));
  }

  @Test
  void ideReloadWritesTheClassWithoutRunningATask() throws IOException {
    setUp("");
    touch("assets/sounds/jump.ogg");

    BuildResult result = runner().build();

    assertTrue(result.task(":core:generateAssetsClass") == null, result.getOutput());
    assertTrue(generated().contains("JUMP_OGG = \"sounds/jump.ogg\""), generated());
  }

  @Test
  void intellijSyncWritesTheClass() throws IOException {
    setUp("");
    touch("assets/sounds/jump.ogg");

    runner("help", "-Didea.sync.active=true").build();

    assertTrue(generated().contains("JUMP_OGG"), generated());
  }

  @Test
  void reloadOnlyWarnsAboutAClash() throws IOException {
    setUp("");
    touch("assets/my-song.ogg");
    touch("assets/my_song.ogg");

    BuildResult reload = runner().build();
    assertTrue(reload.getOutput().contains("Could not generate the assets class"), reload.getOutput());

    BuildResult build = runner(":core:compileJava").buildAndFail();
    assertTrue(build.getOutput().contains("my-song.ogg"), build.getOutput());
    assertTrue(build.getOutput().contains("my_song.ogg"), build.getOutput());
  }

  @Test
  void hiddenAndExcludedFilesGetNoConstant() throws IOException {
    setUp("assetsClass { excludes = ['raw/**', '**/*.psd'] }");
    touch("assets/.DS_Store");
    touch("assets/.git/config");
    touch("assets/raw/source.wav");
    touch("assets/art/player.psd");
    touch("assets/art/player.png");

    runner(":core:generateAssetsClass").build();

    String source = generated();
    assertTrue(source.contains("PLAYER_PNG"), source);
    assertFalse(source.contains("DS_STORE"), source);
    assertFalse(source.contains("CONFIG"), source);
    assertFalse(source.contains("Raw"), source);
    assertFalse(source.contains("PLAYER_PSD"), source);
  }

  @Test
  void packageDefaultsToTheGroupAndClassNameCanChange() throws IOException {
    setUp("group = 'org.example'\nassetsClass { packageName.unset(); className = 'Res' }\n");
    touch("assets/a.png");

    runner(":core:generateAssetsClass").build();

    Path file = dir.resolve("core/build/generated/flixelgdx/assets/org/example/Res.java");
    assertTrue(Files.readString(file).contains("public final class Res {"));
  }

  @Test
  void missingAssetsFolderGivesAnEmptyClass() throws IOException {
    setUp("");

    runner(":core:generateAssetsClass").build();

    assertTrue(generated().contains("private Assets() {}"), generated());
  }

  @Test
  void worksWithTheConfigurationCache() throws IOException {
    setUp("");
    touch("assets/a.png");

    runner(":core:compileJava", "--configuration-cache").build();
    touch("assets/b.png");
    BuildResult result = runner(":core:compileJava", "--configuration-cache").build();

    assertTrue(result.getOutput().contains("Reusing configuration cache"), result.getOutput());
    assertTrue(generated().contains("B_PNG"), generated());
  }

  private TaskOutcome outcome(BuildResult result) {
    return result.task(":core:generateAssetsClass").getOutcome();
  }

  private String generated() throws IOException {
    return Files.readString(dir.resolve(GENERATED));
  }

  /** Sets up a game with the plugin applied to a core module, the way generated projects are laid out. */
  private void setUp(String extra) throws IOException {
    write("settings.gradle", "rootProject.name = 'game'\ninclude 'core'\n");
    write("core/build.gradle", """
        plugins {
          id 'java'
          id 'org.flixelgdx.assets'
        }
        assetsClass { packageName = 'com.mygame' }
        """ + extra + "\n");
  }

  private void useInGame(String expression) throws IOException {
    write("core/src/main/java/com/mygame/Game.java",
        "package com.mygame;\n\npublic class Game {\n  public static final String PATH = " + expression + ";\n}\n");
  }

  private GradleRunner runner(String... arguments) {
    GradleRunner runner = GradleRunner.create()
        .withProjectDir(dir.toFile())
        .withPluginClasspath()
        .withArguments(arguments);
    String home = System.getProperty("flixel.test.gradleUserHome");
    if (home != null) {
      runner.withTestKitDir(Path.of(home).toFile());
    }
    return runner;
  }

  private void touch(String relative) throws IOException {
    write(relative, "x");
  }

  private void write(String relative, String text) throws IOException {
    Path file = dir.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, text, StandardCharsets.UTF_8);
  }
}
