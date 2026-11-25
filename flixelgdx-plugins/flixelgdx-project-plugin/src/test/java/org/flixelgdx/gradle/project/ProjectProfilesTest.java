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
import org.junit.jupiter.api.Test;
import org.tomlj.Toml;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectProfilesTest {

  private static final String FILE = """
      platforms = ["desktop", "html5", "android"]

      [app]
      title = "Slime Quest"
      package = "com.example.slimequest"
      company = "Goo Studio"
      version = "0.3.0"

      [window]
      width = 1280
      fullscreen = true

      [defines]
      demo = false
      enable_cheats = false

      [profile.dev]
      window.fullscreen = false
      defines.enable_cheats = true
      defines.log_level = "debug"

      [profile.demo]
      app.title = "Slime Quest Demo"
      defines.demo = true
      platforms = ["desktop", "html5"]
      """;

  @Test
  void noProfileUsesTheFileAsWritten() {
    ProjectModel model = ProjectToml.parse(FILE);

    assertEquals("", model.profile());
    assertTrue(model.fullscreen());
    assertEquals("Slime Quest", model.title());
    assertEquals(ProjectToml.parse(FILE, ""), model);
    assertEquals(ProjectToml.parse(FILE, "  "), model);
  }

  @Test
  void aProfileOnlyChangesTheValuesItMentions() {
    ProjectModel model = ProjectToml.parse(FILE, "dev");

    assertEquals("dev", model.profile());
    assertFalse(model.fullscreen());
    assertEquals(1280, model.width());
    assertEquals("Slime Quest", model.title());
    assertEquals(List.of("desktop", "html5", "android"), model.platforms());
  }

  @Test
  void profilesCanChangeTheTitleAndPlatforms() {
    ProjectModel model = ProjectToml.parse(FILE, "demo");

    assertEquals("Slime Quest Demo", model.title());
    assertEquals(List.of("desktop", "html5"), model.platforms());
    assertTrue(model.fullscreen());
  }

  @Test
  void profileDefinesOverrideInPlaceAndAddNewOnesAtTheEnd() {
    List<ProjectDefine> defines = ProjectToml.parse(FILE, "dev").defines();

    assertEquals(List.of(
        new ProjectDefine("demo", "DEMO", false),
        new ProjectDefine("enable_cheats", "ENABLE_CHEATS", true),
        new ProjectDefine("log_level", "LOG_LEVEL", "debug")), defines);
  }

  @Test
  void aProfileCanAddDefinesWhenTheFileHasNone() {
    String file = FILE.replace("[defines]\ndemo = false\nenable_cheats = false\n", "");

    assertEquals(List.of(new ProjectDefine("demo", "DEMO", true)), ProjectToml.parse(file, "demo").defines());
  }

  @Test
  void anUnknownProfileListsTheOnesThatExist() {
    String message = assertThrows(GradleException.class, () -> ProjectToml.parse(FILE, "relase")).getMessage();

    assertTrue(message.contains("'relase'") && message.contains("dev, demo"), message);
    assertTrue(message.contains("flixel.profile"), message);
  }

  @Test
  void aProfileCannotChangeThePackage() {
    String file = FILE + "\n[profile.other]\napp.package = \"com.example.other\"\n";

    String message = assertThrows(GradleException.class, () -> ProjectToml.parse(file, "other")).getMessage();
    assertTrue(message.contains("profile.other.app.package") && message.contains("applicationIdSuffix"), message);
  }

  @Test
  void errorsPointAtTheProfileLine() {
    String file = FILE + "\n[profile.bad]\nwindow.width = -5\ndefines.levels = [1, 2]\n";

    String width = assertThrows(GradleException.class, () -> ProjectToml.parse(file, "bad")).getMessage();
    assertTrue(width.contains("profile.bad.window.width"), width);

    String define = assertThrows(GradleException.class,
        () -> ProjectToml.parse(file.replace("window.width = -5\n", ""), "bad")).getMessage();
    assertTrue(define.contains("profile.bad.defines.levels"), define);
  }

  @Test
  void aBrokenProfileOnlyFailsWhenItIsSelected() {
    String file = FILE + "\n[profile.bad]\nwindow.width = -5\n";

    assertEquals(1280, ProjectToml.parse(file).width());
    assertEquals(1280, ProjectToml.parse(file, "dev").width());
  }

  @Test
  void profileNamesAreListedInOrder() {
    assertEquals(List.of("dev", "demo"), ProjectToml.profileNames(Toml.parse(FILE)));
    assertEquals(List.of(), ProjectToml.profileNames(Toml.parse("[app]\n")));
    assertThrows(GradleException.class, () -> ProjectToml.profileNames(Toml.parse("profile = 1\n")));
  }
}
