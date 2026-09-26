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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the archive helpers in {@link Archives}.
 *
 * <p>These tests round-trip a small directory through {@link Archives#zipDirectory} and
 * {@link Archives#extract}, which exercises the same {@code putArchiveEntry()} and
 * {@code getNextEntry()} calls a real package build makes. A game module commonly applies other
 * Gradle plugins that bundle their own, older copy of commons-compress on the shared plugin
 * classpath; calling through the wrong overload there throws a {@link NoSuchMethodError} at
 * runtime even though this module compiles and resolves cleanly on its own; see the comments in
 * {@link Archives} for how the calls are kept on the stable, cross-version signatures.
 */
class ArchivesTest {

  @Test
  void zipDirectoryRoundTripsFileContentsAndLayout(@TempDir Path tempDir) throws IOException {
    Path source = tempDir.resolve("source");
    Files.createDirectories(source.resolve("nested"));
    Files.writeString(source.resolve("top.txt"), "top-level file", StandardCharsets.UTF_8);
    Files.writeString(source.resolve("nested/inner.txt"), "nested file", StandardCharsets.UTF_8);

    Path zipFile = tempDir.resolve("out.zip");
    Archives.zipDirectory(source, zipFile, "myapp");

    Path extracted = tempDir.resolve("extracted");
    Archives.extract(zipFile, extracted, true);

    Path root = extracted.resolve("myapp");
    assertEquals("top-level file", Files.readString(root.resolve("top.txt"), StandardCharsets.UTF_8));
    assertEquals("nested file",
        Files.readString(root.resolve("nested/inner.txt"), StandardCharsets.UTF_8));
  }

  @Test
  void zipDirectoryPreservesExecutableBit(@TempDir Path tempDir) throws IOException {
    // A distributable zip from zipDirectory() is meant to be opened by a player's own unzip tool
    // (Explorer, Archive Manager, "unzip"), not by Archives.extract(), which only reads the JDK
    // archives this plugin downloads. Those tools read the Unix mode from the zip's central
    // directory, so the assertion here reads it back the same way, through ZipFile, rather than
    // through the streaming Archives.extract() path (which cannot see central-directory data).
    Path source = tempDir.resolve("source");
    Files.createDirectories(source);
    Path launcher = source.resolve("launcher");
    Files.writeString(launcher, "#!/bin/sh\necho hi\n", StandardCharsets.UTF_8);
    launcher.toFile().setExecutable(true, false);

    Path zipFile = tempDir.resolve("out.zip");
    Archives.zipDirectory(source, zipFile, "myapp");

    try (ZipFile zip = ZipFile.builder().setPath(zipFile).get()) {
      ZipArchiveEntry entry = zip.getEntry("myapp/launcher");
      boolean ownerExecutable = (entry.getUnixMode() & 0100) != 0;
      assertTrue(ownerExecutable, "launcher entry should keep its owner-execute bit in the zip");
    }
  }

  @Test
  void extractTarGzRoundTripsFileContents(@TempDir Path tempDir) throws IOException {
    // The JDK archives packagr downloads for every platform except Windows are .tar.gz, so the
    // extractor is exercised through that format too, not only the .zip path above.
    Path source = tempDir.resolve("source");
    Files.createDirectories(source);
    Files.writeString(source.resolve("readme.txt"), "hello from tar.gz", StandardCharsets.UTF_8);

    Path tarGz = tempDir.resolve("out.tar.gz");
    writeTarGz(source, tarGz);

    Path extracted = tempDir.resolve("extracted");
    Archives.extract(tarGz, extracted, false);

    assertEquals("hello from tar.gz",
        Files.readString(extracted.resolve("readme.txt"), StandardCharsets.UTF_8));
  }

  /**
   * Writes a minimal {@code .tar.gz} archive from a source directory using the same commons-compress
   * writer classes {@link Archives} reads with, so this test does not depend on an external {@code tar}
   * binary being installed.
   */
  private static void writeTarGz(Path sourceDir, Path tarGzFile) throws IOException {
    String name = "readme.txt";
    try (OutputStream out = Files.newOutputStream(tarGzFile);
        GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(out);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
      Path file = sourceDir.resolve(name);
      TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), name);
      tar.putArchiveEntry(entry);
      Files.copy(file, tar);
      tar.closeArchiveEntry();
    }
  }
}
