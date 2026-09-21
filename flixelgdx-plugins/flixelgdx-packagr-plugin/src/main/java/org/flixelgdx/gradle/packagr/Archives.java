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
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

  /**
   * Zips a directory tree into a single distributable {@code .zip}, under one top-level folder.
   *
   * <p>Every entry is nested under {@code rootName} so unzipping produces one tidy folder rather than
   * scattering files. The Unix executable bit is preserved for each file, which keeps the launcher
   * and the bundled runtime's {@code bin/java} runnable after a player extracts the archive on Linux
   * or macOS. On a host with no Unix permissions (Windows) every file is marked executable, which is
   * harmless and keeps a cross-built package runnable on its target.
   *
   * @param sourceDir The directory whose contents are zipped.
   * @param zipFile The {@code .zip} file to create (parent directories are created).
   * @param rootName The single top-level folder every entry is nested under.
   * @throws IOException When the directory cannot be read or the archive cannot be written.
   */
  public static void zipDirectory(Path sourceDir, Path zipFile, String rootName) throws IOException {
    Files.createDirectories(zipFile.getParent());
    List<Path> files;
    try (Stream<Path> stream = Files.walk(sourceDir)) {
      files = stream.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList();
    }
    try (OutputStream out = Files.newOutputStream(zipFile);
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out)) {
      for (Path file : files) {
        String relative = sourceDir.relativize(file).toString().replace('\\', '/');
        ZipArchiveEntry entry = new ZipArchiveEntry(file.toFile(), rootName + "/" + relative);
        entry.setUnixMode(zipMode(file));
        zip.putArchiveEntry(entry);
        Files.copy(file, zip);
        zip.closeArchiveEntry();
      }
    }
  }

  private static int zipMode(Path file) {
    try {
      Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
      int mode = 0;
      for (PosixFilePermission perm : perms) {
        mode |= switch (perm) {
          case OWNER_READ -> 0400;
          case OWNER_WRITE -> 0200;
          case OWNER_EXECUTE -> 0100;
          case GROUP_READ -> 0040;
          case GROUP_WRITE -> 0020;
          case GROUP_EXECUTE -> 0010;
          case OTHERS_READ -> 0004;
          case OTHERS_WRITE -> 0002;
          case OTHERS_EXECUTE -> 0001;
        };
      }
      return mode;
    } catch (IOException | UnsupportedOperationException e) {
      // A non-POSIX host cannot report a mode; mark executable so a cross-built package stays
      // runnable on its target (an executable data file is harmless).
      return 0755;
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
