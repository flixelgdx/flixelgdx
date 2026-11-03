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
package org.flixelgdx.gradle.shader;

import org.flixelgdx.gradle.shader.worker.Diagnostic;
import org.flixelgdx.gradle.shader.worker.DiagnosticReport;
import org.flixelgdx.gradle.shader.worker.SourceText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the helpers that need no GPU: text mapping, the report file and the host target. */
class ShaderHelpersTest {

  @TempDir
  Path dir;

  @Test
  void countsNewlinesAndFindsLines() {
    String text = "one\ntwo\r\nthree";
    assertEquals(2, SourceText.countNewlines(text));
    assertEquals("one", SourceText.lineOf(text, 1));
    assertEquals("two", SourceText.lineOf(text, 2));
    assertEquals("three", SourceText.lineOf(text, 3));
    assertEquals("", SourceText.lineOf(text, 9));
  }

  @Test
  void byteColumnsBecomeCharacterColumns() {
    assertEquals(4, SourceText.charColumn("abcdef", 4));
    // The e with an accent is two bytes, so byte column 5 is the fourth character.
    assertEquals(4, SourceText.charColumn("ébcd", 5));
  }

  @Test
  void findsFunctionsOutsideComments() {
    assertTrue(SourceText.definesFunction("@vertex fn vs_main() {}", "vs_main"));
    assertFalse(SourceText.definesFunction("// fn vs_main() {}\nfn other() {}", "vs_main"));
    assertFalse(SourceText.definesFunction("/* fn vs_main() {} */", "vs_main"));
    assertFalse(SourceText.definesFunction("fn vs_main2() {}", "vs_main"));
  }

  @Test
  void formatsPositionedAndWholeShaderFindings() {
    String positioned = SourceText.format("a.wgsl", 2, 3, "error", "bad", "line one\n  oops");
    assertEquals("a.wgsl:2:3: error: bad\n  oops\n  ^", positioned);
    assertEquals("a.wgsl: error: bad", SourceText.format("a.wgsl", 0, 0, "error", "bad", ""));
  }

  @Test
  void reportSurvivesALineBreakAndBackslash() throws IOException {
    Path file = dir.resolve("report.txt");
    DiagnosticReport.write(List.of(new Diagnostic(true, "a\\b\nsecond"), new Diagnostic(false, "warn")), file);
    List<Diagnostic> read = DiagnosticReport.read(file);
    assertEquals(2, read.size());
    assertTrue(read.get(0).isError());
    assertEquals("a\\b\nsecond", read.get(0).getText());
    assertFalse(read.get(1).isError());
    assertEquals("warn", read.get(1).getText());
  }

  @Test
  void hostTargetsFollowTheNativesNaming() {
    assertEquals("linux-x86_64", HostTarget.of("Linux", "amd64"));
    assertEquals("windows-aarch64", HostTarget.of("Windows 11", "aarch64"));
    assertEquals("macos-aarch64", HostTarget.of("Mac OS X", "arm64"));
    assertThrows(IllegalStateException.class, () -> HostTarget.of("Plan 9", "amd64"));
    assertThrows(IllegalStateException.class, () -> HostTarget.of("Linux", "riscv64"));
  }
}
