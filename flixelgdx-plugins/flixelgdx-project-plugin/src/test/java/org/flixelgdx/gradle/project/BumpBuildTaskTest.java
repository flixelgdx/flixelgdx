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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BumpBuildTaskTest {

  private static final String APP = """
      [app]
      title   = "My Game"
      package = "com.example.mygame"
      company = "My Studio"
      version = "0.1.0"
      """;

  @Test
  void raisesTheNumberAndKeepsTheRestOfTheLine() {
    String text = APP + "build   = 14   # bump before every release\n\n[window]\nwidth = 800\n";

    String expected = APP + "build   = 15   # bump before every release\n\n[window]\nwidth = 800\n";
    assertEquals(expected, BumpBuildTask.bump(text));
  }

  @Test
  void addsTheNumberAfterTheVersionWhenItIsMissing() {
    String expected = APP.replace("version = \"0.1.0\"\n", "version = \"0.1.0\"\nbuild = 2\n");
    assertEquals(expected, BumpBuildTask.bump(APP));
  }

  @Test
  void keepsTheIndentOfTheVersionLine() {
    String text = "[app]\n  title = \"G\"\n  package = \"a.b\"\n  company = \"C\"\n  version = \"1\"\n";

    String expected = text + "  build = 2\n";
    assertEquals(expected, BumpBuildTask.bump(text));
  }

  @Test
  void addsTheNumberWhenTheFileHasNoFinalLineBreak() {
    String text = APP.strip();
    assertEquals(text + "\nbuild = 2", BumpBuildTask.bump(text));
  }

  @Test
  void keepsWindowsLineEndings() {
    String text = (APP + "build = 3\n").replace("\n", "\r\n");
    assertEquals(text.replace("build = 3", "build = 4"), BumpBuildTask.bump(text));

    String missing = APP.replace("\n", "\r\n");
    String expected = missing.replace("version = \"0.1.0\"\r\n", "version = \"0.1.0\"\r\nbuild = 2\r\n");
    assertEquals(expected, BumpBuildTask.bump(missing));
  }

  @Test
  void readsUnderscoresAndDottedKeys() {
    assertEquals(APP + "build = 1000\n", BumpBuildTask.bump(APP + "build = 9_99\n"));

    String dotted = "app.title = \"G\"\napp.package = \"a.b\"\napp.company = \"C\"\napp.version = \"1\"\n";
    assertEquals(dotted + "app.build = 8\n", BumpBuildTask.bump(dotted + "app.build = 7\n"));
  }

  @Test
  void ignoresBuildKeysInOtherTables() {
    String text = APP + "build = 5\n\n[[installed]]\nbuild = 99\n\n[tools]\nbuild = 3\n";

    String expected = APP + "build = 6\n\n[[installed]]\nbuild = 99\n\n[tools]\nbuild = 3\n";
    assertEquals(expected, BumpBuildTask.bump(text));
  }

  @Test
  void stopsAtTheHighestNumberAndroidAccepts() {
    String message = assertThrows(GradleException.class,
        () -> BumpBuildTask.bump(APP + "build = 2100000000\n")).getMessage();
    assertTrue(message.contains("highest"), message);
  }

  @Test
  void refusesNumbersItCannotEditSafely() {
    assertThrows(GradleException.class, () -> BumpBuildTask.bump(APP + "\"build\" = 5\n"));
  }
}
