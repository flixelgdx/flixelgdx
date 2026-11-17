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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectTomlTest {

  private static final String APP = """
      [app]
      title = "My Game"
      package = "com.example.mygame"
      company = "My Studio"
      version = "0.1.0"
      """;

  @Test
  void defaultsMatchTheConfigBuilder() {
    ProjectModel model = ProjectToml.parse(APP);

    assertEquals(640, model.width());
    assertEquals(360, model.height());
    assertEquals(60, model.framerate());
    assertTrue(model.vsync());
    assertFalse(model.fullscreen());
    assertFalse(model.hasRender());
    assertEquals("java", model.language());
    assertEquals(List.of("desktop"), model.platforms());
    assertTrue(model.renderResolution());
    assertEquals(1f, model.renderScale());
    assertTrue(model.renderSmooth());
    assertFalse(model.transparent());
  }

  @Test
  void everyValueIsRead() {
    ProjectModel model = ProjectToml.parse("""
        platforms = ["desktop", "html5", "android", "html5"]

        [app]
        title = "My Game"
        package = "com.example.mygame"
        company = "My Studio"
        version = "1.2.3"
        language = "kotlin"

        [window]
        width = 1280
        height = 720
        framerate = 144
        vsync = false
        fullscreen = true

        [render]
        resolution = [320, 180]
        scale = 0.5
        smooth = false
        transparent = true
        """);

    assertEquals("kotlin", model.language());
    assertEquals(1280, model.width());
    assertEquals(720, model.height());
    assertEquals(144, model.framerate());
    assertFalse(model.vsync());
    assertTrue(model.fullscreen());
    assertTrue(model.hasRender());
    assertEquals(320, model.renderWidth());
    assertEquals(180, model.renderHeight());
    assertEquals(0.5f, model.renderScale());
    assertFalse(model.renderSmooth());
    assertTrue(model.transparent());
    assertEquals(List.of("desktop", "html5", "android"), model.platforms());
  }

  @Test
  void integerScaleIsAccepted() {
    assertEquals(2f, ProjectToml.parse(APP + "[render]\nscale = 2\n").renderScale());
  }

  @Test
  void resolutionCanBeTurnedOff() {
    assertFalse(ProjectToml.parse(APP + "[render]\nresolution = false\n").renderResolution());
  }

  @Test
  void zeroResolutionIsKept() {
    ProjectModel model = ProjectToml.parse(APP + "[render]\nresolution = [0, 0]\n");
    assertEquals(0, model.renderWidth());
    assertEquals(0, model.renderHeight());
  }

  @Test
  void unknownTablesAndKeysAreIgnored() {
    ProjectModel model = ProjectToml.parse(APP + """
        extra = 5
        [window]
        width = 800
        icon = "x.png"
        [installed]
        plugins = ["a", "b"]
        [installed.deep]
        anything = 1
        """);

    assertEquals(800, model.width());
  }

  @Test
  void cliBookkeepingTablesAreIgnored() {
    ProjectModel model = ProjectToml.parse(APP + """
        [[installed]]
        repo = "flixelgdx/flixelgdx-ui"
        package = "flixelgdx-ui"
        kind = "library"
        tag = "v0.7.2"
        libraries = [{ alias = "flixelgdx-ui", bundles = ["core"] }]
        plugins = [{ module = "core", id = "org.flixelgdx.assets", alias = "flixelgdx-assets-plugin" }]
        repositories = []

        [[installed]]
        repo = ""
        package = "my-lib"
        kind = "library"
        tag = ""
        libraries = [{ alias = "my-lib", bundles = ["core"] }]
        plugins = []
        repositories = []
        """);

    assertEquals("My Game", model.title());
    assertEquals(640, model.width());
  }

  @Test
  void requiredValuesAreNamed() {
    for (String key : List.of("title", "company", "version", "package")) {
      String text = APP.lines().filter(line -> !line.startsWith(key)).reduce("", (a, b) -> a + b + "\n");
      String message = assertThrows(GradleException.class, () -> ProjectToml.parse(text)).getMessage();
      assertTrue(message.contains("app." + key), message);
    }
  }

  @Test
  void invalidValuesAreRejectedWithTheFieldName() {
    assertFails(APP.replace("My Game", "Bad:Name"), "app.title");
    assertFails(APP.replace("My Studio", "CON"), "app.company");
    assertFails(APP.replace("0.1.0", "x"), "app.version");
    assertFails(APP.replace("com.example.mygame", "Com.Example"), "app.package");
    assertFails(APP + "[window]\nwidth = 0\n", "window.width");
    assertFails(APP + "[window]\nheight = -2\n", "window.height");
    assertFails(APP + "[window]\nframerate = 0\n", "window.framerate");
    assertFails(APP + "[window]\nvsync = \"yes\"\n", "window.vsync");
    assertFails(APP + "[render]\nscale = 0\n", "render.scale");
    assertFails(APP + "[render]\nresolution = [-1, 4]\n", "render.resolution");
    assertFails(APP + "[render]\nresolution = [1]\n", "render.resolution");
  }

  @Test
  void platformsAreChecked() {
    assertFails("platforms = [\"ios\"]\n" + APP, "not supported yet");
    assertFails("platforms = [\"switch\"]\n" + APP, "not a platform");
    assertFails("platforms = \"desktop\"\n" + APP, "platforms");
  }

  @Test
  void brokenTomlIsReported() {
    assertFails("[app\ntitle = 1", "not valid TOML");
  }

  private static void assertFails(String text, String expected) {
    String message = assertThrows(GradleException.class, () -> ProjectToml.parse(text)).getMessage();
    assertTrue(message.contains(expected), message);
  }
}
