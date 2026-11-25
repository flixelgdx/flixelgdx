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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDefinesTest {

  private static final String APP = """
      [app]
      title = "My Game"
      package = "com.example.mygame"
      company = "My Studio"
      version = "0.1.0"
      """;

  @Test
  void noTableMeansNoDefines() {
    assertEquals(List.of(), ProjectToml.parse(APP).defines());
  }

  @Test
  void valuesKeepTheirTypeAndOrder() {
    List<ProjectDefine> defines = ProjectToml.parse(APP + """
        [defines]
        zeta = "last letter, first entry"
        demo = false
        max_saves = 3
        big = 5_000_000_000
        gravity = 9.8
        """).defines();

    assertEquals(List.of(
        new ProjectDefine("zeta", "ZETA", "last letter, first entry"),
        new ProjectDefine("demo", "DEMO", false),
        new ProjectDefine("max_saves", "MAX_SAVES", 3),
        new ProjectDefine("big", "BIG", 5_000_000_000L),
        new ProjectDefine("gravity", "GRAVITY", 9.8f)), defines);
    assertEquals(List.of("String", "boolean", "int", "long", "float"),
        defines.stream().map(ProjectDefine::javaType).toList());
  }

  @Test
  void keysBecomeUpperSnakeCase() {
    assertEquals("MAX_SAVES", ProjectDefines.toConstantName("max_saves"));
    assertEquals("MAX_SAVES", ProjectDefines.toConstantName("max-saves"));
    assertEquals("MAX_SAVES", ProjectDefines.toConstantName("maxSaves"));
    assertEquals("HUD_SCALE", ProjectDefines.toConstantName("HUDScale"));
    assertEquals("_8BIT_MODE", ProjectDefines.toConstantName("8bit_mode"));
    assertNull(ProjectDefines.toConstantName("---"));
  }

  @Test
  void keysThatBecomeTheSameNameAreRejected() {
    String message = assertThrows(GradleException.class,
        () -> ProjectToml.parse(APP + "[defines]\nmax_saves = 1\nmaxSaves = 2\n")).getMessage();
    assertTrue(message.contains("MAX_SAVES") && message.contains("max_saves"), message);
  }

  @Test
  void unsupportedValuesNameTheKey() {
    String array = assertThrows(GradleException.class,
        () -> ProjectToml.parse(APP + "[defines]\nlevels = [1, 2]\n")).getMessage();
    assertTrue(array.contains("defines.levels"), array);

    String nested = assertThrows(GradleException.class,
        () -> ProjectToml.parse(APP + "[defines]\nplayer.speed = 2.0\n")).getMessage();
    assertTrue(nested.contains("defines.player") && nested.contains("nested"), nested);

    String nan = assertThrows(GradleException.class,
        () -> ProjectToml.parse(APP + "[defines]\nodd = nan\n")).getMessage();
    assertTrue(nan.contains("defines.odd"), nan);

    assertThrows(GradleException.class, () -> ProjectToml.parse(APP + "[defines]\nhuge = 1e300\n"));
    assertThrows(GradleException.class, () -> ProjectToml.parse(APP + "[defines]\nday = 2026-11-24\n"));
    assertThrows(GradleException.class, () -> ProjectToml.parse(APP + "[defines]\n\"!!\" = 1\n"));
  }
}
