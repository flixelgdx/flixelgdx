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
 * Drives Khronos {@code spirv-cross} to translate a SPIR-V module into the shading language a
 * backend needs.
 *
 * <p>If glslang is the compiler that turns GLSL into SPIR-V bytecode, spirv-cross is the translator
 * that turns that bytecode back into readable source for another platform. It works from the
 * compiled module rather than from the developer's text, so the output is always valid code with
 * every macro expanded.
 *
 * <p>Right now it produces ESSL 3.00 for the web (WebGL 2) and Android (OpenGL ES 3) backends.
 * Names of uniforms, attributes, and varyings are kept exactly as written, which is what lets those
 * backends bind them by name.
 */
public final class SpirvCross {

  @NotNull
  private final BundledTool tool;

  private SpirvCross(@NotNull BundledTool tool) {
    this.tool = tool;
  }

  /**
   * Prepares a runnable translator, extracting the bundled binary into {@code workDir} when needed.
   *
   * @param workDir A writable directory the plugin owns, used to cache the extracted tool.
   * @param override An explicit translator path from the extension, or {@code null} to auto-resolve.
   * @return A ready-to-use {@code SpirvCross}.
   * @throws IOException When no translator can be found or the bundled binary cannot be extracted.
   */
  @NotNull
  public static SpirvCross prepare(@NotNull File workDir, @Nullable File override) throws IOException {
    return new SpirvCross(BundledTool.locate(workDir, override, "spirv-cross", "spirvCrossPath"));
  }

  /**
   * Translates a SPIR-V module into ESSL 3.00 source.
   *
   * @param spirv The SPIR-V module produced by {@link Glslang}.
   * @param outFile The destination source file (parent directories are created).
   * @return The process result, including combined output for diagnostics.
   * @throws IOException When the translator process cannot be started.
   */
  @NotNull
  public ToolResult toEssl(@NotNull File spirv, @NotNull File outFile) throws IOException {
    Files.createDirectories(outFile.getParentFile().toPath());
    return tool.run(List.of(
        spirv.getAbsolutePath(),
        "--es",
        "--version", "300",
        "--output", outFile.getAbsolutePath()));
  }
}
