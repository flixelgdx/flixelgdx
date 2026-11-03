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
package org.flixelgdx.gradle.shader.worker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the file through which the validator process hands its findings back to Gradle.
 *
 * <p>Each finding is one line: {@code E} or {@code W}, a tab, then the text with backslashes written as
 * {@code \\} and line breaks as {@code \n}.
 */
public final class DiagnosticReport {

  private DiagnosticReport() {}

  /**
   * Writes the findings to a file, replacing it.
   *
   * @param findings The findings to store.
   * @param file The file to write.
   * @throws IOException if the file cannot be written.
   */
  public static void write(List<Diagnostic> findings, Path file) throws IOException {
    StringBuilder out = new StringBuilder();
    for (Diagnostic d : findings) {
      out.append(d.isError() ? 'E' : 'W').append('\t');
      String text = d.getText();
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        switch (c) {
          case '\\' -> out.append("\\\\");
          case '\n' -> out.append("\\n");
          case '\r' -> {
          }
          default -> out.append(c);
        }
      }
      out.append('\n');
    }
    Files.writeString(file, out, StandardCharsets.UTF_8);
  }

  /**
   * Reads the findings from a file written by {@link #write(List, Path)}.
   *
   * @param file The file to read.
   * @return The findings, in the order they were written.
   * @throws IOException if the file cannot be read.
   */
  public static List<Diagnostic> read(Path file) throws IOException {
    List<Diagnostic> found = new ArrayList<>();
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      if (line.length() < 2) {
        continue;
      }
      StringBuilder text = new StringBuilder(line.length());
      for (int i = 2; i < line.length(); i++) {
        char c = line.charAt(i);
        if (c == '\\' && i + 1 < line.length()) {
          char next = line.charAt(++i);
          text.append(next == 'n' ? '\n' : next);
        } else {
          text.append(c);
        }
      }
      found.add(new Diagnostic(line.charAt(0) == 'E', text.toString()));
    }
    return found;
  }
}
