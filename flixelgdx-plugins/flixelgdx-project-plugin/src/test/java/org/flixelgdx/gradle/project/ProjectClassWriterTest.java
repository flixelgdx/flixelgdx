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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectClassWriterTest {

  private static final String APP = """
      [app]
      title = "My Game"
      package = "com.example.mygame"
      company = "My Studio"
      version = "0.1.0"
      """;

  @Test
  void writesTheClassWithoutRenderValues() {
    String expected = """
        // Generated from project.toml. Do not edit.
        package com.example.mygame;

        import org.flixelgdx.FlixelConfig;

        /** Values from this game's project.toml, generated at build time. */
        public final class Project {
          public static final String TITLE = "My Game";
          public static final String COMPANY = "My Studio";
          public static final String VERSION = "0.1.0";
          public static final int WIDTH = 640;
          public static final int HEIGHT = 360;
          public static final int FRAMERATE = 60;
          public static final boolean VSYNC = true;
          public static final boolean FULLSCREEN = false;

          private Project() {}

          /** Returns a builder filled in from project.toml, ready to tweak or build(). */
          public static FlixelConfig.Builder config() {
            return new FlixelConfig.Builder(TITLE)
                .company(COMPANY)
                .version(VERSION)
                .size(WIDTH, HEIGHT)
                .framerate(FRAMERATE)
                .vsync(VSYNC)
                .fullscreen(FULLSCREEN);
          }
        }
        """;

    assertEquals(expected, ProjectClassWriter.write(ProjectToml.parse(APP)));
  }

  @Test
  void writesTheRenderValuesWhenTheTableIsPresent() {
    String source = ProjectClassWriter.write(ProjectToml.parse(APP + """
        [window]
        width = 1280
        height = 720
        vsync = false

        [render]
        resolution = [320, 180]
        scale = 0.5
        smooth = false
        transparent = true
        """));

    String expected = """
          public static final boolean RENDER_RESOLUTION = true;
          public static final int RENDER_WIDTH = 320;
          public static final int RENDER_HEIGHT = 180;
          public static final float RENDER_SCALE = 0.5f;
          public static final boolean RENDER_SMOOTH = false;
          public static final boolean TRANSPARENT = true;
        """;
    assertTrue(source.contains(expected), source);
    assertTrue(source.contains("""
                .fullscreen(FULLSCREEN)
                .renderResolution(RENDER_WIDTH, RENDER_HEIGHT, RENDER_SMOOTH)
                .renderScale(RENDER_SCALE, RENDER_SMOOTH)
                .transparentFramebuffer(TRANSPARENT);
        """), source);
    assertTrue(source.contains("WIDTH = 1280;") && source.contains("VSYNC = false;"), source);
  }

  @Test
  void disabledResolutionCallsDisable() {
    String source = ProjectClassWriter.write(ProjectToml.parse(APP + "[render]\nresolution = false\n"));
    assertTrue(source.contains(".disableRenderResolution()"), source);
  }

  @Test
  void quotesAndBackslashesAreEscaped() {
    ProjectModel model = new ProjectModel(640, 360, 60, 0, 0, 1f, "The \"Best\" Game", "A\\B", "1",
        "com.example", "java", List.of("desktop"), true, false, false, true, true, false);

    String source = ProjectClassWriter.write(model);

    assertTrue(source.contains("TITLE = \"The \\\"Best\\\" Game\";"), source);
    assertTrue(source.contains("COMPANY = \"A\\\\B\";"), source);
  }
}
