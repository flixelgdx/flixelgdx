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
package org.flixelgdx.functional;

import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.Nullable;

/**
 * Marks an object that can have a {@link FlixelShader} applied to it.
 *
 * <p>Implemented by both {@link org.flixelgdx.FlixelSprite FlixelSprite} (per-sprite batch
 * interruption) and {@link org.flixelgdx.FlixelCamera FlixelCamera} (full-scene FBO
 * post-processing). Both follow the same ownership contract: the shader is NOT owned by the
 * implementing object. The caller is responsible for calling {@link FlixelShader#destroy()} when
 * the shader is no longer needed.
 *
 * <h3>Performance note</h3>
 *
 * <p>The cost differs significantly between the two implementations.
 *
 * <ul>
 *   <li><b>Camera shader</b>: one FBO capture and one composite draw call per camera per frame,
 *       regardless of how many sprites the camera contains. Suitable for full-screen scene effects.
 *   <li><b>Sprite shader</b>: each shader <em>transition</em> in draw order costs one GPU batch
 *       flush (all buffered vertices are submitted before the new shader can take over). Sprites
 *       that share the same shader instance and are drawn consecutively batch together for free.
 *       Mixing many different shaders in draw order causes many flushes and can significantly hurt
 *       performance on low-end hardware. Give players the option to turn off shaders if your game
 *       targets weak devices.
 * </ul>
 *
 * @see org.flixelgdx.FlixelSprite
 * @see org.flixelgdx.FlixelCamera
 */
public interface FlixelShaderable {

  /**
   * Assigns a shader to this object, or removes it by passing {@code null}.
   *
   * <p>The shader is NOT owned by this object. Destroy it yourself by calling
   * {@link FlixelShader#destroy()} when it is no longer needed.
   *
   * @param shader The shader to apply, or {@code null} to remove the current shader.
   */
  void setShader(@Nullable FlixelShader shader);

  /**
   * Returns the shader currently assigned to this object, or {@code null} if none is set.
   *
   * @return The active {@link FlixelShader}, or {@code null}.
   */
  @Nullable
  FlixelShader getShader();
}
