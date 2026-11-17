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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectValidatorTest {

  @ParameterizedTest
  @ValueSource(strings = { "My Game", "Game-2", "a", "Studio (Pty) Ltd", "Name.With.Dots", "COM10", "CONSOLE" })
  void goodNamesPass(String name) {
    assertDoesNotThrow(() -> ProjectValidator.checkName("app.title", name));
  }

  @ParameterizedTest
  @ValueSource(strings = { "a<b", "a>b", "a:b", "a\"b", "a/b", "a\\b", "a|b", "a?b", "a*b" })
  void forbiddenCharactersAreNamed(String name) {
    String message = fail(() -> ProjectValidator.checkName("app.title", name));
    assertTrue(message.contains("app.title"), message);
    assertTrue(message.contains("'" + name.charAt(1) + "'"), message);
  }

  @Test
  void nonAsciiCharacterIsNamedByCodePoint() {
    String message = fail(() -> ProjectValidator.checkName("app.company", "Café"));
    assertTrue(message.contains("app.company"), message);
    assertTrue(message.contains("U+00E9"), message);
  }

  @Test
  void controlCharactersAreRejected() {
    String message = fail(() -> ProjectValidator.checkName("app.title", "a\tb"));
    assertTrue(message.contains("U+0009"), message);
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "   " })
  void blankNamesAreRejected(String name) {
    assertTrue(fail(() -> ProjectValidator.checkName("app.title", name)).contains("blank"));
  }

  @ParameterizedTest
  @ValueSource(strings = { " lead", "trail ", ".lead", "trail." })
  void edgeSpacesAndDotsAreRejected(String name) {
    String message = fail(() -> ProjectValidator.checkName("app.title", name));
    assertTrue(message.contains("start with") || message.contains("end with"), message);
  }

  @ParameterizedTest
  @ValueSource(strings = { "CON", "con", "Prn", "AUX", "nul", "COM1", "com9", "LPT1", "lpt9" })
  void reservedWindowsNamesAreRejected(String name) {
    assertTrue(fail(() -> ProjectValidator.checkName("app.title", name)).contains("reserved"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "0.1.0", "1", "2.3.1-beta+5", "10.0.0-rc.1" })
  void goodVersionsPass(String version) {
    assertDoesNotThrow(() -> ProjectValidator.checkVersion("app.version", version));
  }

  @Test
  void versionMustStartWithADigit() {
    String message = fail(() -> ProjectValidator.checkVersion("app.version", "v1.0"));
    assertTrue(message.contains("app.version") && message.contains("digit"), message);
  }

  @Test
  void versionCharactersAreNamed() {
    String message = fail(() -> ProjectValidator.checkVersion("app.version", "1.0 beta"));
    assertTrue(message.contains("' '"), message);
  }

  @Test
  void emptyVersionIsRejected() {
    assertTrue(fail(() -> ProjectValidator.checkVersion("app.version", "")).contains("empty"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "com.example.mygame", "game", "_x.a1", "org.my_game.v2" })
  void goodPackagesPass(String name) {
    assertDoesNotThrow(() -> ProjectValidator.checkPackage("app.package", name));
  }

  @ParameterizedTest
  @ValueSource(strings = { "Com.example", "com.Example", "com.exa-mple", "com.1abc", "com..x", ".com", "com." })
  void badPackagesAreRejected(String name) {
    assertTrue(fail(() -> ProjectValidator.checkPackage("app.package", name)).contains("app.package"));
  }

  @Test
  void packageKeywordsAreRejected() {
    String message = fail(() -> ProjectValidator.checkPackage("app.package", "com.example.new"));
    assertTrue(message.contains("keyword") && message.contains("'new'"), message);
  }

  @Test
  void packageCharacterIsNamed() {
    String message = fail(() -> ProjectValidator.checkPackage("app.package", "com.exa-mple"));
    assertTrue(message.contains("'-'"), message);
  }

  @Test
  void languageMustBeJavaOrKotlin() {
    assertDoesNotThrow(() -> ProjectValidator.checkLanguage("app.language", "java"));
    assertDoesNotThrow(() -> ProjectValidator.checkLanguage("app.language", "kotlin"));
    assertTrue(fail(() -> ProjectValidator.checkLanguage("app.language", "scala")).contains("app.language"));
  }

  private static String fail(Runnable check) {
    return assertThrows(GradleException.class, check::run).getMessage();
  }
}
