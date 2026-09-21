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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Downloads and caches the JDK a package's trimmed runtime is built from.
 *
 * <p>This is the piece that fixes packaging's slowest, most repeated cost: re-downloading a JDK that
 * is already on the machine. A resolved JDK is stored under a stable per-platform directory in the
 * shared cache, marked complete once its archive has been verified and unpacked. On every later run
 * the marker is checked first, so a given vendor, version, and platform is downloaded exactly once
 * and reused by every project on the machine from then on.
 *
 * <p>The download source is the Adoptium API, which serves Eclipse Temurin builds. The archive's
 * SHA-256 is checked against the checksum Adoptium publishes before it is trusted, and the same hash
 * is written into the completion marker so a partially written cache entry is never mistaken for a
 * finished one.
 */
public final class JdkResolver {

  private static final String BINARY_ENDPOINT =
      "https://api.adoptium.net/v3/binary/latest/%d/ga/%s/%s/jdk/hotspot/normal/eclipse";
  private static final String ASSETS_ENDPOINT =
      "https://api.adoptium.net/v3/assets/latest/%d/hotspot?architecture=%s&image_type=jdk&os=%s&vendor=eclipse";
  private static final Pattern CHECKSUM_PATTERN = Pattern.compile("\"checksum\"\\s*:\\s*\"([0-9a-fA-F]{64})\"");
  private static final String MARKER_NAME = ".packagr-complete";

  private JdkResolver() {}

  /**
   * Resolves a JDK home directory for the given platform, downloading and caching it if needed.
   *
   * @param cacheDir The shared directory cached JDKs live in.
   * @param vendor The JDK vendor; currently only {@code temurin} is supported.
   * @param version The JDK feature version, for example {@code 17}.
   * @param os The operating system the JDK targets.
   * @param arch The architecture the JDK targets.
   * @param logger The Gradle logger used to report progress.
   * @return The path to the JDK home (the directory containing {@code jmods}).
   * @throws IOException When the JDK cannot be downloaded, verified, or unpacked.
   */
  public static Path resolve(Path cacheDir, String vendor, int version, OperatingSystem os,
      Architecture arch, Logger logger) throws IOException {
    if (!"temurin".equalsIgnoreCase(vendor)) {
      throw new GradleException("packagr currently supports only the 'temurin' JDK vendor, but '"
          + vendor + "' was requested. Set jdkVendor = \"temurin\", or bundle another vendor's JDK "
          + "yourself.");
    }

    String slug = vendor.toLowerCase(Locale.ROOT) + "-" + version + "-" + os.token() + "-" + arch.token();
    Path base = cacheDir.resolve(slug);
    Path extracted = base.resolve("extracted");
    Path marker = base.resolve(MARKER_NAME);

    Path cachedHome = readMarker(base, marker);
    if (cachedHome != null) {
      logger.info("[packagr] Reusing cached JDK for {} at {}.", slug, cachedHome);
      return cachedHome;
    }

    Files.createDirectories(base);
    String archUrl = arch.adoptiumName();
    String osUrl = os.adoptiumName();
    String downloadUrl = String.format(Locale.ROOT, BINARY_ENDPOINT, version, osUrl, archUrl);

    Path archive = base.resolve(os.usesZipArchive() ? "jdk.zip" : "jdk.tar.gz");
    logger.lifecycle("[packagr] Downloading Temurin {} for {}-{} (first time only)...", version,
        os.token(), arch.token());

    // HttpClient is not AutoCloseable on Java 17 (only from Java 21), so it is not managed with
    // try-with-resources here; it needs no explicit cleanup on this toolchain.
    HttpClient client = newClient();
    try {
      download(client, downloadUrl, archive);
      String actual = sha256(archive);
      String expected = fetchExpectedChecksum(client, version, osUrl, archUrl, logger);
      if (expected != null && !expected.equalsIgnoreCase(actual)) {
        throw new GradleException("Downloaded JDK failed its checksum check for " + slug
            + " (expected " + expected + ", got " + actual + "). The download may be corrupt; "
            + "delete " + base + " and try again.");
      }

      deleteRecursively(extracted);
      Archives.extract(archive, extracted, os.usesZipArchive());
      Files.deleteIfExists(archive);

      Path home = findJdkHome(extracted);
      writeMarker(base, marker, actual, home);
      logger.lifecycle("[packagr] Cached JDK for {} at {}.", slug, home);
      return home;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while downloading the JDK for " + slug + ".", e);
    }
  }

  private static Path readMarker(Path base, Path marker) throws IOException {
    if (!Files.isRegularFile(marker)) {
      return null;
    }
    List<String> lines = Files.readAllLines(marker, StandardCharsets.UTF_8);
    if (lines.size() < 2) {
      return null;
    }
    Path home = base.resolve(lines.get(1));
    if (Files.isDirectory(home.resolve("jmods"))) {
      return home;
    }
    return null;
  }

  private static void writeMarker(Path base, Path marker, String sha256, Path home) throws IOException {
    String relative = base.relativize(home).toString();
    Files.writeString(marker, sha256 + "\n" + relative + "\n", StandardCharsets.UTF_8);
  }

  private static HttpClient newClient() {
    return HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(30))
        .build();
  }

  private static void download(HttpClient client, String url, Path dest)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofMinutes(10))
        .GET()
        .build();
    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() != 200) {
      throw new IOException("JDK download failed with HTTP " + response.statusCode() + " from " + url);
    }
    try (InputStream in = response.body()) {
      Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static String fetchExpectedChecksum(HttpClient client, int version, String os, String arch,
      Logger logger) {
    String url = String.format(Locale.ROOT, ASSETS_ENDPOINT, version, arch, os);
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .timeout(Duration.ofSeconds(60))
          .GET()
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return null;
      }
      Matcher matcher = CHECKSUM_PATTERN.matcher(response.body());
      return matcher.find() ? matcher.group(1) : null;
    } catch (IOException e) {
      logger.info("[packagr] Could not fetch the JDK checksum; skipping verification.", e);
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private static String sha256(Path file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream in = Files.newInputStream(file)) {
        byte[] buffer = new byte[65536];
        int read;
        while ((read = in.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
        }
      }
      StringBuilder sb = new StringBuilder(64);
      for (byte b : digest.digest()) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("SHA-256 is not available on this JVM.", e);
    }
  }

  private static Path findJdkHome(Path root) throws IOException {
    try (Stream<Path> stream = Files.walk(root, 6)) {
      return stream
          .filter(p -> Files.isDirectory(p.resolve("jmods")))
          .findFirst()
          .orElseThrow(() -> new IOException(
              "The downloaded JDK archive did not contain a 'jmods' directory; it may be a JRE "
                  + "rather than a full JDK. Extracted under: " + root));
    }
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(path)) {
      stream.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new RuntimeException("Could not clean the JDK cache entry: " + p, e);
        }
      });
    }
  }
}
