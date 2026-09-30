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
import java.nio.file.Files;
import java.util.List;

/**
 * Locates and drives bgfx's {@code shaderc} compiler, the tool that turns a single {@code .sc}
 * source into the per-renderer bytecode the desktop backend loads at runtime.
 *
 * <p>The same compiler produces the framework's own built-in sprite shader, so its output is
 * guaranteed to be in the exact container format {@code bgfx_create_shader} expects, including the
 * uniform reflection table and the vertex/fragment signature hashes. That is why the plugin drives
 * {@code shaderc} instead of trying to synthesize that binary format by hand.
 *
 * <p>The compiler is resolved like every other {@link BundledTool}. The {@code bgfx_shader.sh}
 * include header is extracted from the plugin JAR next to it the first time it is needed.
 *
 * <p>On Linux and macOS the bundled {@code d3d4linux} Wine shim is extracted alongside the binary so
 * the Direct3D (dx11) variant can be compiled there too, provided Wine is installed. When the shim
 * or Wine is missing, that one variant is skipped and every other variant still compiles.
 */
public final class Shaderc {

  private static final String SHIM_ROOT = "/org/flixelgdx/gradle/shader/tools/windows-shim/";
  private static final String INCLUDE_HEADER = "/org/flixelgdx/gradle/shader/include/bgfx_shader.sh";

  @NotNull
  private final BundledTool tool;

  @NotNull
  private final File includeDir;

  private Shaderc(@NotNull BundledTool tool, @NotNull File includeDir) {
    this.tool = tool;
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
    BundledTool.extractResource(INCLUDE_HEADER, new File(includeDir, "bgfx_shader.sh"));

    // The bundled binary lands in bin/, so the Direct3D shim can sit at ../windows relative to it,
    // the exact location shaderc looks for it.
    BundledTool tool = BundledTool.locate(workDir, override, "shaderc", "shadercPath");
    if (!BundledTool.isWindows()) {
      extractDirect3DShim(workDir);
    }
    return new Shaderc(tool, includeDir);
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
  public ToolResult compile(@NotNull File scFile, @NotNull File varyingFile, @NotNull File outFile,
      @NotNull String type, @NotNull ShaderTarget target) throws IOException {
    Files.createDirectories(outFile.getParentFile().toPath());
    return tool.run(List.of(
        "-f", scFile.getAbsolutePath(),
        "-o", outFile.getAbsolutePath(),
        "--type", type,
        "--platform", target.platform(),
        "-p", target.profile(),
        "--varyingdef", varyingFile.getAbsolutePath(),
        "-i", includeDir.getAbsolutePath()));
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
      BundledTool.extractResource(SHIM_ROOT + "d3d4linux.exe", new File(windowsDir, "d3d4linux.exe"));
      BundledTool.extractResource(SHIM_ROOT + "d3dcompiler_47.dll", new File(windowsDir, "d3dcompiler_47.dll"));
    } catch (IOException e) {
      // Optional tooling; the Direct3D variant is skipped gracefully when the shim is unavailable.
    }
  }
}
