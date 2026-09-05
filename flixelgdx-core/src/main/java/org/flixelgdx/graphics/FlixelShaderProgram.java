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
package org.flixelgdx.graphics;

import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;

/**
 * An opaque, backend-owned handle to a compiled GPU shader program.
 *
 * <p>This interface is the internal seam between the framework and each graphics backend. Game code
 * never interacts with it directly; instead, it uses {@link FlixelShader}, which wraps one of these
 * and integrates with the FlixelGDX update and destroy lifecycle.
 *
 * <p>Each backend supplies its own implementation: the bgfx backend wraps a bgfx program handle,
 * the WebGPU backend wraps a {@code GPUShaderModule} pair, and so on. When no backend is present
 * (headless or pre-startup), {@link FlixelUnsupportedShader} is returned as a safe no-op.
 *
 * <p>Obtain a program by calling
 * {@link FlixelGraphicsManager#compileShaderProgram(byte[], byte[])}. Call {@link #destroy()} to
 * release GPU resources when finished.
 *
 * @see FlixelShader
 * @see FlixelGraphicsManager#compileShaderProgram(byte[], byte[])
 */
public interface FlixelShaderProgram extends FlixelDestroyable {

  /**
   * Returns {@code true} if this program compiled successfully and can be used for drawing.
   *
   * @return {@code true} if the shader program compiled successfully, {@code false} otherwise.
   */
  boolean isValid();

  /**
   * Sets a single-float uniform by name.
   *
   * @param name Uniform name as declared in the shader source.
   * @param value Value to upload.
   */
  void setUniform(@NotNull String name, float value);

  /**
   * Sets an integer uniform by name (for example, a texture unit index).
   *
   * @param name Uniform name as declared in the shader source.
   * @param value Value to upload.
   */
  void setUniform(@NotNull String name, int value);

  /**
   * Sets a two-component vector uniform by name.
   *
   * @param name Uniform name as declared in the shader source.
   * @param value Value to upload.
   */
  void setUniform(@NotNull String name, @NotNull FlixelVector value);

  /**
   * Sets an RGBA color uniform by name.
   *
   * @param name Uniform name as declared in the shader source.
   * @param value Value to upload.
   */
  void setUniform(@NotNull String name, @NotNull FlixelColor value);

  /**
   * Sets a 4x4 matrix uniform by name.
   *
   * @param name Uniform name as declared in the shader source.
   * @param value Value to upload.
   */
  void setUniform(@NotNull String name, @NotNull FlixelMatrix value);
}
