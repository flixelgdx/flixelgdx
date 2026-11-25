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

/**
 * One entry of the {@code [defines]} table in {@code project.toml}, which becomes a constant in
 * {@code Project.Defines}.
 *
 * <p>For example, {@code max_saves = 3} becomes the key {@code max_saves}, the constant name {@code MAX_SAVES} and the
 * value {@code 3}, written as {@code public static final int MAX_SAVES = 3;}.
 *
 * @param key The key as written in the file, such as {@code max_saves}. Build scripts look values up by it.
 * @param name The Java constant name, such as {@code MAX_SAVES}.
 * @param value The value, which is a {@link Boolean}, {@link Integer}, {@link Long}, {@link Float} or {@link String}.
 */
public record ProjectDefine(String key, String name, Object value) {

  /**
   * Gives the Java type the constant is declared with.
   *
   * @return One of {@code boolean}, {@code int}, {@code long}, {@code float} and {@code String}.
   */
  public String javaType() {
    if (value instanceof Boolean) {
      return "boolean";
    }
    if (value instanceof Integer) {
      return "int";
    }
    if (value instanceof Long) {
      return "long";
    }
    return value instanceof Float ? "float" : "String";
  }
}
