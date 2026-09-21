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
package org.flixelgdx.gradle.packagr;

/**
 * A CPU architecture a game can be packaged for.
 *
 * <p>Each constant records the {@link #token} used in this plugin's resource and output paths
 * (matching the classifier convention shared with the framework's native artifacts) and the
 * {@link #adoptiumName} the JDK download API expects.
 */
public enum Architecture {

  /** 64-bit x86, also known as {@code x64} or {@code amd64}. */
  X64("x86_64", "x64"),

  /** 64-bit ARM, also known as {@code arm64}. */
  AARCH64("aarch64", "aarch64");

  private final String token;
  private final String adoptiumName;

  Architecture(String token, String adoptiumName) {
    this.token = token;
    this.adoptiumName = adoptiumName;
  }

  /**
   * Returns the classifier token used in this plugin's resource and output paths.
   *
   * <p>For example {@code x86_64} in the bundled stub path {@code stub/linux-x86_64/launcher}.
   *
   * @return The architecture token.
   */
  public String token() {
    return token;
  }

  /**
   * Returns the architecture name the Adoptium JDK download API expects.
   *
   * @return The Adoptium architecture name.
   */
  public String adoptiumName() {
    return adoptiumName;
  }
}
