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
package org.flixelgdx.gradle.basisu;

import com.android.build.gradle.BaseExtension;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.tasks.TaskProvider;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gradle plugin that adds opt-in KTX2/Basis Universal texture compression to a FlixelGDX Android
 * module.
 *
 * <p>libGDX decodes every PNG into an uncompressed {@code RGBA8888} texture on the GPU, so a few
 * megabytes of source art can balloon into hundreds of megabytes of video memory. Basis Universal
 * keeps textures compressed on the GPU and transcodes them to the best format each device
 * supports. This plugin automates the encoding step, which normally requires developers to
 * install and run the {@code basisu} command line tool by hand: it fetches a verified {@code
 * basisu} binary for the host platform, caches it in the Gradle user home directory, and wires a
 * {@code compressBasisuTextures} task into the Android build so every {@code .png} under the
 * configured assets directory is compressed automatically. The Android launcher enables the
 * matching runtime loader automatically, so game code keeps requesting {@code .png} paths and
 * transparently gets the compressed texture back; see
 * {@code FlixelAssetManager.enableCompressedTextures()} in {@code flixelgdx-core} for the loading
 * side of this feature.
 *
 * <p>Compression stays off unless a developer opts in, since it changes build output and needs
 * network access on first use to fetch the encoder. See {@link FlixelBasisuExtension} for the
 * {@code enableBasisuCompression} Gradle property and other configuration.
 *
 * <p>Apply in the Android module's {@code build.gradle}, alongside {@code com.android.application}
 * or {@code com.android.library}:
 *
 * <pre>{@code
 * plugins {
 *   id 'com.android.application'
 *   id 'org.flixelgdx.basisu' version "${flixelVersion}"
 * }
 * }</pre>
 *
 * <p>This does not currently cover the TeaVM web backend: neither libGDX's own compressed texture
 * loaders nor Basis Universal's transcoder ship a TeaVM binding, so web builds keep loading plain
 * PNGs regardless of this plugin.
 *
 * @see FlixelBasisuExtension
 */
public class FlixelBasisuPlugin implements Plugin<Project> {

  private static final String TASK_GROUP = "flixelgdx";
  private static final String ENABLE_PROPERTY = "enableBasisuCompression";
  private static final String COMPRESS_TASK_NAME = "compressBasisuTextures";

  // Pinned to a specific commit (rather than a branch) so a compromised or changed upstream file
  // cannot silently start executing different code during a developer's build. Update this
  // alongside the checksums below when bumping to a newer basisu build.
  private static final String BASISU_COMMIT = "168ecd13d2884aacc674f30a5eddc389e4f07fb0";
  private static final String BASISU_RAW_BASE =
      "https://raw.githubusercontent.com/HacksawStudios/basisu/" + BASISU_COMMIT + "/";

  private static final Map<String, BasisuBinary> BASISU_BINARIES = Map.of(
      "linux-x64", new BasisuBinary(
          "bin/linux/x64/basisu", "3c6048214557caf20391ff3c52e7eb80d53efa8798fe5d03b378ed07092f5f02", "basisu"),
      "linux-arm64", new BasisuBinary(
          "bin/linux/arm64/basisu", "d2c2bbb60268b88e4fd69178a61e8bb61bb8524286dbffb15d290a07f3423b80", "basisu"),
      "darwin-x64", new BasisuBinary(
          "bin/darwin/x64/basisu", "d79d27ec9b07025d477edd8aaddf43627d179491157fa29cc86d09e0877665a2", "basisu"),
      "darwin-arm64", new BasisuBinary(
          "bin/darwin/arm64/basisu", "11c385f91b503be3270335f748bb269471b343c8e69bf24c32f0c66d4720daff", "basisu"),
      "windows-x64", new BasisuBinary(
          "bin/win/x64/basisu.exe", "24b02c886c401168684340e5954aee2cf5ed7838b4be5bdfa7c6301802265bcc", "basisu.exe"));

  @Override
  public void apply(@NonNull Project project) {
    FlixelBasisuExtension ext = project.getExtensions().create(FlixelBasisuExtension.NAME, FlixelBasisuExtension.class);

    boolean enabledByProperty = "true".equalsIgnoreCase(String.valueOf(project.findProperty(ENABLE_PROPERTY)));
    ext.getEnabled().convention(enabledByProperty);
    ext.getAssetsDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("assets"));
    ext.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("generated/basisuAssets"));
    ext.getGenerateMipmaps().convention(true);
    ext.getUseUastc().convention(false);
    ext.getExcludes().convention(List.of());

    ConfigurableFileTree sourceImages = project.fileTree(ext.getAssetsDir(), tree -> tree.include("**/*.png"));
    project.afterEvaluate(p -> sourceImages.exclude(ext.getExcludes().get()));

    TaskProvider<Task> compressTask = project.getTasks().register(COMPRESS_TASK_NAME, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription(
          "Compresses PNG assets into KTX2/Basis Universal supercompressed textures for reduced GPU memory usage.");
      task.onlyIf(t -> ext.getEnabled().get());
      task.getInputs().files(sourceImages).skipWhenEmpty();
      task.getOutputs().dir(ext.getOutputDir());

      task.doLast(t -> {
        File assetsRoot = ext.getAssetsDir().get().getAsFile();
        File outputRoot = ext.getOutputDir().get().getAsFile();
        outputRoot.mkdirs();

        File encoder = resolveEncoderBinary(project);

        for (File source : sourceImages) {
          String relativePath = assetsRoot.toPath().relativize(source.toPath()).toString();
          String relativeOut = relativePath.substring(0, relativePath.length() - ".png".length()) + ".ktx2";
          File dest = new File(outputRoot, relativeOut);
          File destDir = dest.getParentFile();
          if (destDir != null) {
            destDir.mkdirs();
          }

          List<String> command = new ArrayList<>();
          command.add(encoder.getAbsolutePath());
          command.add(source.getAbsolutePath());
          command.add("-ktx2");
          if (ext.getGenerateMipmaps().get()) {
            command.add("-mipmap");
          }
          if (ext.getUseUastc().get()) {
            command.add("-uastc");
          }
          command.add("-output_file");
          command.add(dest.getAbsolutePath());

          runEncoder(project, command);
        }
      });
    });

    project.getPlugins().withId("com.android.application", p -> wireAndroid(project, ext, compressTask));
    project.getPlugins().withId("com.android.library", p -> wireAndroid(project, ext, compressTask));
  }

  /**
   * Wires compressed output into an Android module's assets source set once the project is fully
   * configured, so the compressed {@code .ktx2} files ship alongside the source PNGs they were
   * built from.
   *
   * <p>AGP's asset-merge tasks (for example {@code mergeDebugAssets}) do not honor
   * {@link com.android.build.gradle.api.AndroidSourceDirectorySet#exclude} for asset packaging:
   * every source directory is copied into the merge output as-is, regardless of any exclude
   * patterns configured on the source set. Excluding the plain PNGs there is a no-op, so instead
   * each merge task gets a {@code doLast} action that deletes the plain PNG once the merge
   * completes, wherever a compressed {@code .ktx2} sibling exists in the merged output.
   *
   * @param project The Gradle project the plugin was applied to.
   * @param ext The resolved {@link FlixelBasisuExtension} for this project.
   * @param compressTask The registered {@code compressBasisuTextures} task.
   */
  private void wireAndroid(
      @NonNull Project project,
      @NonNull FlixelBasisuExtension ext,
      @NonNull TaskProvider<Task> compressTask) {
    project.afterEvaluate(p -> {
      if (!ext.getEnabled().get()) {
        return;
      }

      BaseExtension android = p.getExtensions().getByType(BaseExtension.class);
      var assets = android.getSourceSets().getByName("main").getAssets();
      // Resolve to a plain File rather than passing the live Provider: ext.getOutputDir() is
      // also registered as compressBasisuTextures' declared output (see task.getOutputs().dir
      // above), and Android Studio's sync model builder refuses to resolve providers carrying
      // task-producer metadata, since sync must never trigger task execution. A plain File has
      // no such metadata; the explicit dependsOn below still guarantees build-time ordering.
      assets.srcDir(ext.getOutputDir().get().getAsFile());

      p.getTasks().matching(t -> t.getName().matches("merge.*Assets")).configureEach(t -> {
        t.dependsOn(compressTask);
        t.doLast(unused -> {
          for (File outputDir : t.getOutputs().getFiles()) {
            deleteUncompressedSiblings(outputDir);
          }
        });
      });
    });
  }

  /**
   * Recursively deletes plain PNGs under {@code dir} that have a compressed {@code .ktx2}
   * sibling in the same directory, so a merged Android assets folder does not ship both formats
   * for the same image.
   *
   * @param dir Directory to scan; ignored if it does not exist or is not a directory.
   */
  private void deleteUncompressedSiblings(@NonNull File dir) {
    File[] children = dir.listFiles();
    if (children == null) {
      return;
    }
    for (File child : children) {
      if (child.isDirectory()) {
        deleteUncompressedSiblings(child);
        continue;
      }
      if (!child.getName().endsWith(".ktx2")) {
        continue;
      }
      String pngName = child.getName().substring(0, child.getName().length() - ".ktx2".length()) + ".png";
      File pngSibling = new File(child.getParentFile(), pngName);
      if (pngSibling.isFile()) {
        pngSibling.delete();
      }
    }
  }

  /**
   * Ensures a verified {@code basisu} encoder binary for the host platform is present in the
   * Gradle user home cache, downloading and checksum-verifying it first if needed.
   *
   * @param project The Gradle project (used for logging and the Gradle user home path).
   * @return The cached, executable encoder binary.
   */
  @NonNull
  private File resolveEncoderBinary(@NonNull Project project) {
    String platformKey = detectPlatformKey();
    BasisuBinary binary = BASISU_BINARIES.get(platformKey);
    if (binary == null) {
      throw new GradleException(
          "FlixelGDX: no prebuilt basisu encoder binary is available for platform \"" + platformKey
              + "\". Supported platforms: " + BASISU_BINARIES.keySet()
              + ". Disable enableBasisuCompression on this machine, or build basisu from source.");
    }

    File cacheDir = new File(
        project.getGradle().getGradleUserHomeDir(), "caches/flixelgdx-basisu/" + BASISU_COMMIT + "/" + platformKey);
    File binaryFile = new File(cacheDir, binary.executableName());
    if (binaryFile.isFile() && sha256Matches(binaryFile, binary.sha256())) {
      return binaryFile;
    }

    project.getLogger().lifecycle("[FlixelGDX] Fetching basisu encoder for {}...", platformKey);
    cacheDir.mkdirs();
    downloadTo(BASISU_RAW_BASE + binary.repoPath(), binaryFile);

    if (!sha256Matches(binaryFile, binary.sha256())) {
      throw new GradleException(
          "FlixelGDX: downloaded basisu encoder checksum mismatch for platform \"" + platformKey
              + "\". Expected " + binary.sha256() + ". This may indicate a corrupted download or a compromised "
              + "upstream file; refusing to execute an unverified binary.");
    }
    if (!binaryFile.setExecutable(true, false)) {
      project.getLogger().warn("[FlixelGDX] Could not mark basisu encoder as executable at {}.", binaryFile);
    }
    return binaryFile;
  }

  /**
   * Downloads {@code url} to {@code dest}, overwriting any existing file.
   *
   * @param url Source URL.
   * @param dest Destination file.
   */
  private void downloadTo(@NonNull String url, @NonNull File dest) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    try {
      HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(dest.toPath()));
      if (response.statusCode() != 200) {
        throw new GradleException(
            "FlixelGDX: failed to download basisu encoder from " + url + " (HTTP " + response.statusCode() + ").");
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new GradleException("FlixelGDX: failed to download basisu encoder from " + url + ".", e);
    }
  }

  /**
   * Checks whether {@code file}'s SHA-256 digest matches {@code expectedHex}.
   *
   * @param file File to hash.
   * @param expectedHex Expected digest, as lowercase hex.
   * @return {@code true} if the file exists and its digest matches.
   */
  private boolean sha256Matches(@NonNull File file, @NonNull String expectedHex) {
    if (!file.isFile()) {
      return false;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format(Locale.ROOT, "%02x", b));
      }
      return hex.toString().equalsIgnoreCase(expectedHex);
    } catch (IOException | NoSuchAlgorithmException e) {
      return false;
    }
  }

  /**
   * Detects the host platform as an {@code <os>-<arch>} key matching {@link #BASISU_BINARIES}.
   *
   * @return The detected platform key (for example {@code "linux-x64"}).
   */
  @NonNull
  private String detectPlatformKey() {
    String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String archName = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

    String os;
    if (osName.contains("win")) {
      os = "windows";
    } else if (osName.contains("mac") || osName.contains("darwin")) {
      os = "darwin";
    } else {
      os = "linux";
    }

    String arch = (archName.contains("aarch64") || archName.contains("arm64")) ? "arm64" : "x64";
    return os + "-" + arch;
  }

  /**
   * Runs the {@code basisu} encoder with {@code command}, throwing a {@link GradleException} with
   * the captured output if it exits non-zero.
   *
   * @param project The Gradle project (used for logging).
   * @param command Full command line, including the encoder binary path.
   */
  private void runEncoder(@NonNull Project project, @NonNull List<String> command) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new GradleException("FlixelGDX: basisu encoder failed (exit " + exitCode + "):\n" + output);
      }
      project.getLogger().info("[FlixelGDX] {}", output);
    } catch (IOException e) {
      throw new GradleException("FlixelGDX: failed to run basisu encoder.", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GradleException("FlixelGDX: basisu encoder was interrupted.", e);
    }
  }

  /**
   * A single platform's {@code basisu} encoder binary: its path within the HacksawStudios/basisu
   * repository at {@link #BASISU_COMMIT}, its expected SHA-256 digest, and the executable file
   * name to cache it under.
   *
   * @param repoPath Path of the binary within the source repository.
   * @param sha256 Expected SHA-256 digest, as lowercase hex.
   * @param executableName File name to cache the binary under locally.
   */
  private record BasisuBinary(@NonNull String repoPath, @NonNull String sha256, @NonNull String executableName) {
  }
}
