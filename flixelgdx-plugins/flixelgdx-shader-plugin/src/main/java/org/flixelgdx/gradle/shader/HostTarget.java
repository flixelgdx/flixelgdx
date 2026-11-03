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

import java.util.Locale;

/**
 * Works out which jwebgpu natives module belongs to the machine running the build.
 *
 * <p>The validator loads Dawn from the {@code jwebgpu-natives-<os>-<arch>} jar that matches the host, using the
 * same naming rules as the desktop backend's loader.
 */
final class HostTarget {

  private HostTarget() {}

  /**
   * Returns the target name of this machine, for example {@code linux-x86_64}.
   *
   * @return The target name.
   * @throws IllegalStateException if jwebgpu ships no Dawn library for this operating system or CPU.
   */
  static String current() {
    return of(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
  }

  /**
   * Returns the target name for the given system properties.
   *
   * @param osName The value of {@code os.name}.
   * @param osArch The value of {@code os.arch}.
   * @return The target name.
   * @throws IllegalStateException if jwebgpu ships no Dawn library for the combination.
   */
  static String of(String osName, String osArch) {
    String name = osName.toLowerCase(Locale.ROOT);
    String os;
    if (name.contains("win")) {
      os = "windows";
    } else if (name.contains("mac") || name.contains("darwin")) {
      os = "macos";
    } else if (name.contains("linux")) {
      os = "linux";
    } else {
      throw new IllegalStateException("jwebgpu has no Dawn library for the operating system '" + osName + "'.");
    }
    String arch = switch (osArch.toLowerCase(Locale.ROOT)) {
      case "amd64", "x86_64", "x64" -> "x86_64";
      case "aarch64", "arm64" -> "aarch64";
      default -> throw new IllegalStateException("jwebgpu has no Dawn library for the CPU '" + osArch + "'.");
    };
    return os + "-" + arch;
  }
}
