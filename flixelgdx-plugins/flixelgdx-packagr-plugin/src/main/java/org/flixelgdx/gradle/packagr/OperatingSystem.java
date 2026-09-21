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
 * An operating system a game can be packaged for.
 *
 * <p>Each constant ties together the three names the same platform is known by across the packaging
 * pipeline: the {@link #token} used in resource and output paths, the {@link #adoptiumName} the JDK
 * download API expects, and the {@link #exeSuffix} a launcher executable carries on that platform.
 * It also records whether the platform's JDK archive is a {@code .zip} (Windows) or a
 * {@code .tar.gz} (everything else), which decides how {@link JdkResolver} unpacks it.
 */
public enum OperatingSystem {

  /** Linux, distributed as a {@code .tar.gz} JDK archive. */
  LINUX("linux", "linux", "", false),

  /** Windows, distributed as a {@code .zip} JDK archive; launchers end in {@code .exe}. */
  WINDOWS("windows", "windows", ".exe", true),

  /** macOS, distributed as a {@code .tar.gz} JDK archive. */
  MACOS("macos", "mac", "", false);

  private final String token;
  private final String adoptiumName;
  private final String exeSuffix;
  private final boolean zipArchive;

  OperatingSystem(String token, String adoptiumName, String exeSuffix, boolean zipArchive) {
    this.token = token;
    this.adoptiumName = adoptiumName;
    this.exeSuffix = exeSuffix;
    this.zipArchive = zipArchive;
  }

  /**
   * Returns the short lowercase name used in this plugin's resource and output paths.
   *
   * <p>For example {@code linux} in the bundled stub path {@code stub/linux-x86_64/launcher}.
   *
   * @return The platform token.
   */
  public String token() {
    return token;
  }

  /**
   * Returns the operating-system name the Adoptium JDK download API expects.
   *
   * <p>This matches {@link #token} for Linux and Windows, but is {@code mac} for macOS.
   *
   * @return The Adoptium operating-system name.
   */
  public String adoptiumName() {
    return adoptiumName;
  }

  /**
   * Returns the file-name suffix a launcher executable carries on this platform.
   *
   * @return {@code .exe} on Windows, or an empty string elsewhere.
   */
  public String exeSuffix() {
    return exeSuffix;
  }

  /**
   * Returns whether this platform's JDK ships as a {@code .zip} archive rather than a
   * {@code .tar.gz}.
   *
   * @return {@code true} for Windows, {@code false} otherwise.
   */
  public boolean usesZipArchive() {
    return zipArchive;
  }
}
