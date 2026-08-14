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
package org.flixelgdx.gradle.shader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Locates and drives bgfx's {@code shaderc} compiler, the tool that turns a single {@code .sc}
 * source into the per-renderer bytecode the FlixelGDX backends load at runtime.
 *
 * <p>The same compiler produces the framework's own built-in sprite shader, so its output is
 * guaranteed to be in the exact container format {@code bgfx_create_shader} expects, including the
 * uniform reflection table and the vertex/fragment signature hashes. That is why the plugin drives
 * {@code shaderc} instead of trying to synthesize that binary format by hand.
 *
 * <p>The compiler is resolved in priority order: an explicit path configured on the extension, the
 * binary bundled in this plugin for the current operating system, and finally a {@code shaderc}
 * found on the system {@code PATH}. The bundled binary and the {@code bgfx_shader.sh} include header
 * are extracted from the plugin JAR into a cache directory the first time they are needed.
 *
 * <p>On Linux and macOS the bundled {@code d3d4linux} Wine shim is extracted alongside the binary so
 * the Direct3D (dx11) variant can be compiled there too, provided Wine is installed. When the shim
 * or Wine is missing, that one variant is skipped and every other variant still compiles.
 */
public final class Shaderc {

  private static final String TOOLS_ROOT = "/org/flixelgdx/gradle/shader/tools/";
  private static final String SHIM_ROOT = "/org/flixelgdx/gradle/shader/tools/windows-shim/";
  private static final String INCLUDE_HEADER = "/org/flixelgdx/gradle/shader/include/bgfx_shader.sh";

  @NotNull
  private final File executable;

  @NotNull
  private final File includeDir;

  private Shaderc(@NotNull File executable, @NotNull File includeDir) {
    this.executable = executable;
    this.includeDir = includeDir;
  }

  /**
   * Prepares a runnable compiler, extracting the bundled binary and include header into
   * {@code workDir} when needed.
   *
   * @param workDir A writable directory the plugin owns, used to cache the extracted tool.
   * @param override An explicit compiler path from the extension, or {@code null} to auto-resolve.
   * @return A ready-to-use {@code Shaderc}.
   * @throws IOException When no compiler can be found or the bundled files cannot be extracted.
   */
  @NotNull
  public static Shaderc prepare(@NotNull File workDir, @Nullable File override) throws IOException {
    Files.createDirectories(workDir.toPath());
    File includeDir = new File(workDir, "include");
    Files.createDirectories(includeDir.toPath());
    extractResource(INCLUDE_HEADER, new File(includeDir, "bgfx_shader.sh"));

    // An explicit path always wins.
    if (override != null) {
      if (!override.isFile()) {
        throw new IOException("Configured shaderc path does not exist: " + override.getAbsolutePath());
      }
      return new Shaderc(override, includeDir);
    }

    // The binary bundled for this operating system.
    String classifier = hostClassifier();
    String exeName = isWindows() ? "shaderc.exe" : "shaderc";
    String resource = TOOLS_ROOT + classifier + "/" + exeName;
    if (Shaderc.class.getResource(resource) != null) {
      // shaderc is placed under bin/ so the Direct3D shim can sit at ../windows relative to it, the
      // exact location shaderc looks for it.
      File binDir = new File(workDir, "bin");
      Files.createDirectories(binDir.toPath());
      File dest = new File(binDir, exeName);
      extractResource(resource, dest);
      if (!isWindows()) {
        dest.setExecutable(true, false);
        extractDirect3DShim(workDir);
      }
      return new Shaderc(dest, includeDir);
    }

    // A shaderc found on PATH.
    File onPath = findOnPath(exeName);
    if (onPath != null) {
      return new Shaderc(onPath, includeDir);
    }

    throw new IOException(
        "No shaderc compiler is bundled for this platform (" + classifier + ") and none was found on "
            + "PATH. Set the compiler path with the 'shadercPath' option in the flixelShaders block, or "
            + "add a bundled binary for this platform to the plugin.");
  }

  /**
   * Compiles one shader stage into a single target variant.
   *
   * @param scFile The bgfx {@code .sc} source file.
   * @param varyingFile The shared {@code varying.def.sc} file.
   * @param outFile The destination {@code .bin} file (parent directories are created).
   * @param type The stage, either {@code vertex} or {@code fragment}.
   * @param target The backend variant to produce.
   * @return The process result, including combined output for diagnostics.
   * @throws IOException When the compiler process cannot be started.
   */
  @NotNull
  public Result compile(@NotNull File scFile, @NotNull File varyingFile, @NotNull File outFile,
      @NotNull String type, @NotNull ShaderTarget target) throws IOException {
    Files.createDirectories(outFile.getParentFile().toPath());
    List<String> cmd = new ArrayList<>();
    cmd.add(executable.getAbsolutePath());
    cmd.add("-f");
    cmd.add(scFile.getAbsolutePath());
    cmd.add("-o");
    cmd.add(outFile.getAbsolutePath());
    cmd.add("--type");
    cmd.add(type);
    cmd.add("--platform");
    cmd.add(target.platform());
    cmd.add("-p");
    cmd.add(target.profile());
    cmd.add("--varyingdef");
    cmd.add(varyingFile.getAbsolutePath());
    cmd.add("-i");
    cmd.add(includeDir.getAbsolutePath());

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
      throw new IOException("Interrupted while waiting for shaderc.", e);
    }
    return new Result(code == 0, log.strip());
  }

  private static void extractResource(@NotNull String resource, @NotNull File dest) throws IOException {
    try (InputStream in = Shaderc.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Plugin resource missing: " + resource);
      }
      Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Extracts the {@code d3d4linux} Wine shim into {@code <workDir>/windows} so shaderc can compile
   * the Direct3D (dx11) variant on Linux and macOS.
   *
   * <p>The shim is the real Windows FXC compiler ({@code d3dcompiler_47.dll}) run through Wine by
   * {@code d3d4linux.exe}. shaderc looks for both next to itself under {@code ../windows}, which is
   * why the binary lives in {@code bin/}. The shim is best-effort: if it is absent, or Wine is not
   * installed, the Direct3D variant is simply skipped with a warning while every other variant
   * still compiles.
   *
   * @param workDir The plugin's scratch directory.
   */
  private static void extractDirect3DShim(@NotNull File workDir) {
    if (Shaderc.class.getResource(SHIM_ROOT + "d3d4linux.exe") == null) {
      return;
    }
    try {
      File windowsDir = new File(workDir, "windows");
      Files.createDirectories(windowsDir.toPath());
      extractResource(SHIM_ROOT + "d3d4linux.exe", new File(windowsDir, "d3d4linux.exe"));
      extractResource(SHIM_ROOT + "d3dcompiler_47.dll", new File(windowsDir, "d3dcompiler_47.dll"));
    } catch (IOException e) {
      // Optional tooling; the Direct3D variant is skipped gracefully when the shim is unavailable.
    }
  }

  @Nullable
  private static File findOnPath(@NotNull String exeName) {
    String path = System.getenv("PATH");
    if (path == null) {
      return null;
    }
    for (String entry : path.split(File.pathSeparator)) {
      File candidate = new File(entry, exeName);
      if (candidate.isFile() && candidate.canExecute()) {
        return candidate;
      }
    }
    return null;
  }

  @NotNull
  private static String hostClassifier() {
    if (isWindows()) {
      return "windows-x86_64";
    }
    String os = osName();
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean arm = arch.contains("aarch64") || arch.contains("arm64");
    if (os.contains("mac") || os.contains("darwin")) {
      return arm ? "macos-aarch64" : "macos-x86_64";
    }
    return arm ? "linux-aarch64" : "linux-x86_64";
  }

  private static boolean isWindows() {
    return osName().contains("win");
  }

  @NotNull
  private static String osName() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  }

  /**
   * The outcome of a single {@code shaderc} invocation.
   *
   * @param success Whether the compiler exited successfully.
   * @param log The combined standard output and error, trimmed, for diagnostics.
   */
  public record Result(boolean success, @NotNull String log) {
  }
}
