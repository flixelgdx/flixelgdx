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

/**
 * One finding of the validator: an error or a warning, already formatted for people to read.
 *
 * <p>The text starts with {@code <file path>:<line>:<column>: <severity>: <message>} whenever the finding has a
 * position in the user's file, and with {@code <file path>: <severity>: <message>} when it concerns the shader as a
 * whole (for example a missing entry point). A source excerpt may follow on the next lines.
 */
public final class Diagnostic {

  private final String text;
  private final boolean error;

  /**
   * Creates a finding.
   *
   * @param error Whether the finding fails the build.
   * @param text The formatted message.
   */
  public Diagnostic(boolean error, String text) {
    this.error = error;
    this.text = text;
  }

  public boolean isError() {
    return error;
  }

  public String getText() {
    return text;
  }
}
