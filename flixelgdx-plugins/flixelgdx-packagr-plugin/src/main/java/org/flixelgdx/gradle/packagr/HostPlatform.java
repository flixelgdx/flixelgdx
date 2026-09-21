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

import java.util.Locale;

/**
 * Detects the operating system and architecture of the machine running the build.
 *
 * <p>This matters because {@code jlink} must run on the host but must also match the target JDK's
 * version. When a game is packaged for the same platform the build runs on, the target JDK's own
 * {@code jlink} is used directly; when packaging for a different platform, a host-platform JDK of the
 * target version supplies a {@code jlink} that runs here (see {@link PackageTask}). Both paths need
 * to know what the host actually is.
 */
public final class HostPlatform {

  private HostPlatform() {}

  /**
   * Returns the operating system of the build machine.
   *
   * @return The host operating system, defaulting to {@link OperatingSystem#LINUX} for any
   *     unrecognized Unix-like name.
   */
  public static OperatingSystem os() {
    String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (name.contains("win")) {
      return OperatingSystem.WINDOWS;
    }
    if (name.contains("mac") || name.contains("darwin")) {
      return OperatingSystem.MACOS;
    }
    return OperatingSystem.LINUX;
  }

  /**
   * Returns the architecture of the build machine.
   *
   * @return The host architecture, or {@code null} when it is neither 64-bit x86 nor 64-bit ARM.
   */
  public static Architecture arch() {
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    if (arch.contains("aarch64") || arch.contains("arm64")) {
      return Architecture.AARCH64;
    }
    if (arch.contains("amd64") || arch.contains("x86_64") || arch.equals("x64")) {
      return Architecture.X64;
    }
    return null;
  }
}
