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
 * Drives Khronos {@code glslang}, the reference GLSL compiler, to turn a shader into SPIR-V.
 *
 * <p>This is the first step of every shader build. glslang actually parses the GLSL, so a mistake in
 * a developer's shader is reported here with the right file name and line number, long before it
 * reaches a player's GPU driver. Its SPIR-V output is the single source of truth every later step
 * translates from.
 *
 * <p>Shaders are compiled with OpenGL semantics ({@code -G}) rather than Vulkan semantics. That keeps
 * plain {@code uniform float u_time;} declarations legal and preserves their names, so the GL-based
 * backends can still look each uniform up by the name game code passes to {@code setUniform(...)}.
 * Locations and bindings a shader leaves out are assigned automatically.
 */
public final class Glslang {

  @NotNull
  private final BundledTool tool;

  private Glslang(@NotNull BundledTool tool) {
    this.tool = tool;
  }

  /**
   * Prepares a runnable compiler, extracting the bundled binary into {@code workDir} when needed.
   *
   * @param workDir A writable directory the plugin owns, used to cache the extracted tool.
   * @param override An explicit compiler path from the extension, or {@code null} to auto-resolve.
   * @return A ready-to-use {@code Glslang}.
   * @throws IOException When no compiler can be found or the bundled binary cannot be extracted.
   */
  @NotNull
  public static Glslang prepare(@NotNull File workDir, @Nullable File override) throws IOException {
    return new Glslang(BundledTool.locate(workDir, override, "glslang", "glslangPath"));
  }

  /**
   * Compiles one GLSL shader stage into a SPIR-V module.
   *
   * @param source The complete GLSL source file, as built by {@link ShaderSources}.
   * @param stage The stage, either {@code vert} or {@code frag}.
   * @param outFile The destination {@code .spv} file (parent directories are created).
   * @return The process result. On failure, the log holds glslang's error messages.
   * @throws IOException When the compiler process cannot be started.
   */
  @NotNull
  public ToolResult compile(@NotNull File source, @NotNull String stage, @NotNull File outFile) throws IOException {
    Files.createDirectories(outFile.getParentFile().toPath());
    return tool.run(List.of(
        "-G",
        "--auto-map-locations",
        "--auto-map-bindings",
        "-S", stage,
        "-o", outFile.getAbsolutePath(),
        source.getAbsolutePath()));
  }
}
