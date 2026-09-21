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

/**
 * Runs {@code jlink} to build a small, self-contained runtime image from a JDK's modules.
 *
 * <p>A trimmed runtime is what keeps a package's size reasonable: instead of bundling a whole JDK,
 * only the modules a game actually needs are assembled into a runtime a fraction of the size.
 *
 * <p>{@code jlink} is strict about versions: its own version must exactly match the version of the
 * modules it links, so a Java 21 {@code jlink} cannot build a Java 17 runtime. The caller therefore
 * supplies a {@code jlink} taken from a JDK of the target version (see {@link PackageTask}), rather
 * than whichever JDK happens to be running the build.
 */
public final class Jlink {

  private Jlink() {}

  /**
   * Builds a trimmed runtime image.
   *
   * @param jlinkExe The {@code jlink} executable to run, whose version matches {@code targetVersion}.
   * @param targetVersion The target JDK feature version, used to pick a compatible compression flag.
   * @param jmods The target JDK's {@code jmods} directory, used as the module path.
   * @param modules The module names to include in the image.
   * @param output The directory the runtime image is written to (must not already exist).
   * @param logger The Gradle logger used to report progress.
   * @throws IOException When {@code jlink} cannot be run.
   */
  public static void run(File jlinkExe, int targetVersion, Path jmods, List<String> modules,
      Path output, Logger logger) throws IOException {
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
    // The compression flag was renamed between Java 17 and Java 21; it is chosen from the target
    // version (which the jlink executable matches) to avoid a flag the tool would reject.
    cmd.add(targetVersion >= 21 ? "--compress=zip-6" : "--compress=2");

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
}
