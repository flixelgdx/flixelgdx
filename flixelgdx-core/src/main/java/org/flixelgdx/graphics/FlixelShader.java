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
import org.jetbrains.annotations.NotNull;

/**
 * A ready-to-use, compiled shader program, produced by a graphics backend from a {@link FlixelShaderSource}.
 *
 * <p>This is an opaque handle. Game code never sees the underlying language or GPU object; it only
 * binds the shader (usually by handing it to the sprite batch) and sets named uniform values on it.
 * Each backend provides its own implementation and translates the uniform names to whatever its GPU
 * library expects.
 *
 * <p>Obtain one by compiling a {@link FlixelShaderSource} through {@link FlixelGraphicsManager}, then
 * hand it to the batch to switch effects. Call {@link #destroy()} when you are finished with it so
 * the backend can release its GPU resources.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelShader tint = Flixel.graphics.compileShader(mySource);
 * tint.setUniform("u_strength", 0.5f);
 * // ... bind through the batch, draw, then later:
 * tint.destroy();
 * }</pre>
 *
 * @see FlixelShaderSource
 * @see FlixelGraphicsManager#compileShader(FlixelShaderSource)
 */
public interface FlixelShader extends FlixelDestroyable {

  /**
   * @return {@code true} if this shader compiled successfully and can be used for drawing.
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
