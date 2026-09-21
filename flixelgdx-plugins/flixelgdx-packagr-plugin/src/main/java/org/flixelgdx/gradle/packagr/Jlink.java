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

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runs {@code jlink} to build a small, self-contained runtime image from a JDK's modules.
 *
 * <p>A trimmed runtime is what keeps a package's size reasonable: instead of bundling a whole JDK,
 * only the modules a game actually needs are assembled into a runtime a fraction of the size. The
 * host's own {@code jlink} does the assembly, pointed at the downloaded target JDK's modules, which
 * is what lets a Linux machine build the Windows or macOS runtime for a game (the platform-specific
 * code lives in those downloaded modules, not in {@code jlink} itself).
 *
 * <p>Because {@code jlink} cannot assemble a runtime from a newer JDK's modules than its own, the
 * host {@code jlink} must be at least as new as the target JDK version; this is checked up front so
 * the failure is a clear message rather than a confusing tool error.
 */
public final class Jlink {

  private Jlink() {}

  /**
   * Locates the {@code jlink} executable belonging to the JDK that is running the build.
   *
   * @return The {@code jlink} executable, or {@code null} when the running JVM has no {@code jlink}
   *     (for example when the build runs on a JRE rather than a JDK).
   */
  public static File locateHostJlink() {
    String javaHome = System.getProperty("java.home");
    if (javaHome == null) {
      return null;
    }
    File exe = new File(javaHome, "bin/jlink" + (isWindowsHost() ? ".exe" : ""));
    return exe.isFile() ? exe : null;
  }

  /**
   * Builds a trimmed runtime image.
   *
   * @param jlinkExe The {@code jlink} executable to run.
   * @param targetVersion The target JDK feature version, checked against the host {@code jlink}.
   * @param jmods The target JDK's {@code jmods} directory, used as the module path.
   * @param modules The module names to include in the image.
   * @param output The directory the runtime image is written to (must not already exist).
   * @param logger The Gradle logger used to report progress.
   * @throws IOException When {@code jlink} cannot be run.
   */
  public static void run(File jlinkExe, int targetVersion, Path jmods, List<String> modules,
      Path output, Logger logger) throws IOException {
    int hostVersion = version(jlinkExe);
    if (hostVersion > 0 && hostVersion < targetVersion) {
      throw new GradleException("The build's jlink is Java " + hostVersion + ", which cannot build a "
          + "Java " + targetVersion + " runtime. Run the build on JDK " + targetVersion + " or newer, "
          + "or lower jdkVersion to " + hostVersion + ".");
    }

    // jlink refuses to write into a directory that already exists.
    if (Files.exists(output)) {
      throw new IOException("The jlink output directory already exists: " + output);
    }

    List<String> cmd = new ArrayList<>();
    cmd.add(jlinkExe.getAbsolutePath());
    cmd.add("--module-path");
    cmd.add(jmods.toString());
    cmd.add("--add-modules");
    cmd.add(String.join(",", modules));
    cmd.add("--output");
    cmd.add(output.toString());
    cmd.add("--strip-debug");
    cmd.add("--no-header-files");
    cmd.add("--no-man-pages");
    // The compression flag was renamed between Java 17 and Java 21, so it is chosen from the host
    // jlink's version to avoid a flag the running tool would reject.
    cmd.add(hostVersion >= 21 ? "--compress=zip-6" : "--compress=2");

    logger.info("[packagr] jlink modules: {}", String.join(",", modules));
    ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
    Process process = pb.start();
    String log;
    try (InputStream in = process.getInputStream()) {
      log = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    int code;
    try {
      code = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for jlink.", e);
    }
    if (code != 0) {
      throw new GradleException("jlink failed (exit " + code + "):\n" + log.strip());
    }
  }

  private static int version(File jlinkExe) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(jlinkExe.getAbsolutePath(), "--version")
        .redirectErrorStream(true);
    Process process = pb.start();
    String output;
    try (InputStream in = process.getInputStream()) {
      output = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
    }
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while reading the jlink version.", e);
    }
    int dot = output.indexOf('.');
    String major = dot > 0 ? output.substring(0, dot) : output;
    try {
      return Integer.parseInt(major.strip());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static boolean isWindowsHost() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }
}
