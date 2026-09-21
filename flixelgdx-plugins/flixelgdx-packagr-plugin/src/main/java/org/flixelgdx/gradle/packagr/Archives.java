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
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Unpacks the JDK archives {@link JdkResolver} downloads, handling both formats a JDK ships in.
 *
 * <p>Windows JDKs come as a {@code .zip} and every other platform's as a {@code .tar.gz}, so this
 * helper reads both. A cross-platform pure-Java extractor is used (rather than shelling out to
 * {@code tar} or {@code unzip}) so packaging a Windows game from Linux, or the other way around,
 * works with no external tools installed. The Unix executable bit and the symbolic links a JDK
 * archive carries are preserved where the host filesystem allows it, which keeps the extracted
 * runtime's {@code bin/java} runnable.
 */
public final class Archives {

  private Archives() {}

  /**
   * Extracts an archive into a destination directory.
   *
   * @param archive The archive file to read.
   * @param destDir The directory to extract into (created if missing).
   * @param zip {@code true} for a {@code .zip} archive, {@code false} for a {@code .tar.gz}.
   * @throws IOException When the archive cannot be read or a file cannot be written.
   */
  public static void extract(Path archive, Path destDir, boolean zip) throws IOException {
    Files.createDirectories(destDir);
    try (InputStream raw = new BufferedInputStream(Files.newInputStream(archive))) {
      if (zip) {
        extractZip(raw, destDir);
      } else {
        extractTarGz(raw, destDir);
      }
    }
  }

  private static void extractZip(InputStream raw, Path destDir) throws IOException {
    try (ZipArchiveInputStream in = new ZipArchiveInputStream(raw)) {
      ZipArchiveEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        Path target = resolveSafely(destDir, entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(target);
          continue;
        }
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        applyMode(target, entry.getUnixMode());
      }
    }
  }

  private static void extractTarGz(InputStream raw, Path destDir) throws IOException {
    try (TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(raw))) {
      TarArchiveEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        Path target = resolveSafely(destDir, entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(target);
        } else if (entry.isSymbolicLink()) {
          createSymbolicLink(target, entry.getLinkName());
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
          applyMode(target, entry.getMode());
        }
      }
    }
  }

  /**
   * Resolves an entry name against the destination and rejects any that would escape it, so a
   * malformed archive cannot write outside the target directory (a "zip slip").
   */
  private static Path resolveSafely(Path destDir, String entryName) throws IOException {
    Path normalized = destDir.resolve(entryName).normalize();
    if (!normalized.startsWith(destDir)) {
      throw new IOException("Archive entry escapes the destination directory: " + entryName);
    }
    return normalized;
  }

  private static void createSymbolicLink(Path target, String linkName) throws IOException {
    try {
      Files.createDirectories(target.getParent());
      Files.deleteIfExists(target);
      Files.createSymbolicLink(target, Paths.get(linkName));
    } catch (IOException | UnsupportedOperationException e) {
      // Some hosts (for example Windows without the privilege) cannot create symbolic links. The
      // links a JDK carries are non-essential for jlink, whose module files are regular, so a
      // failure here is ignored rather than aborting the whole extraction.
    }
  }

  private static void applyMode(Path file, int mode) {
    if (mode <= 0) {
      return;
    }
    boolean ownerExecutable = (mode & 0x40) != 0;
    if (!ownerExecutable) {
      return;
    }
    try {
      Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(file));
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      perms.add(PosixFilePermission.GROUP_EXECUTE);
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(file, perms);
    } catch (IOException | UnsupportedOperationException e) {
      // A non-POSIX filesystem (for example on a Windows host) has no executable bit to set; the
      // extracted files still work there, so this is safe to ignore.
      file.toFile().setExecutable(true, false);
    }
  }
}
