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
 * Decides which dependency jars belong in a package for a given platform.
 *
 * <p>The framework's native libraries ship as classified jars on the runtime classpath, one per
 * platform. Their file names carry the classifier the way Maven writes it, for example
 * {@code lwjgl-3.4.2-natives-linux.jar} or {@code lwjgl-3.4.2-natives-windows.jar}. Bundling every
 * one of them would bloat a package with native code for platforms it will never run on, so this
 * helper keeps only the ones matching the target being built.
 *
 * <p>The rule is deliberately simple and driven by the file name: a jar with no {@code natives-}
 * classifier is ordinary code and is always kept, while a jar that does carry one is kept only when
 * both its operating system and its architecture match the target. The classifier vocabulary here
 * follows the naming used by the framework's native dependencies (LWJGL and the Dear ImGui binding):
 * a bare operating-system classifier such as {@code natives-linux} means 64-bit x86, an
 * {@code arm64} or {@code aarch64} token means 64-bit ARM, and 32-bit {@code x86} variants are never
 * shipped.
 */
public final class NativeArtifacts {

  private NativeArtifacts() {}

  /**
   * Returns whether a dependency jar should be included when packaging for a platform.
   *
   * @param fileName The dependency jar's file name.
   * @param os The operating system being packaged for.
   * @param arch The architecture being packaged for.
   * @return {@code true} to include the jar, {@code false} to leave it out.
   */
  public static boolean includes(String fileName, OperatingSystem os, Architecture arch) {
    String name = fileName.toLowerCase(Locale.ROOT);
    if (!name.contains("natives-")) {
      return true;
    }
    if (!matchesOs(name, os)) {
      return false;
    }
    return matchesArch(name, arch);
  }

  private static boolean matchesOs(String name, OperatingSystem os) {
    return switch (os) {
      case LINUX -> name.contains("natives-linux");
      case WINDOWS -> name.contains("natives-windows");
      case MACOS -> name.contains("natives-macos") || name.contains("natives-osx");
    };
  }

  private static boolean matchesArch(String name, Architecture arch) {
    boolean arm = name.contains("arm64") || name.contains("aarch64");
    if (arm) {
      return arch == Architecture.AARCH64;
    }
    boolean is32Bit = name.contains("x86") && !name.contains("x86_64");
    if (is32Bit) {
      return false;
    }
    return arch == Architecture.X64;
  }
}
