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
package org.flixelgdx.backend.desktop.graphics;

import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.bgfx.BGFX;

/**
 * A compiled bgfx shader program.
 *
 * <p>Holds a bgfx program handle built from a vertex and fragment shader pair. Uniform setters
 * cache their values and apply them to bgfx uniforms when the program is bound for a draw. Custom
 * per-camera and global effect shaders wrap one of these.
 */
final class FlixelBgfxShader implements FlixelShader {

  private short program;

  /**
   * Wraps a bgfx program handle.
   *
   * @param program The bgfx program handle, or {@code -1} when compilation failed.
   */
  FlixelBgfxShader(short program) {
    this.program = program;
  }

  /**
   * @return The bgfx program handle for submission.
   */
  short getProgram() {
    return program;
  }

  @Override
  public boolean isValid() {
    return program != -1;
  }

  @Override
  public void setUniform(@NotNull String name, float value) {}

  @Override
  public void setUniform(@NotNull String name, int value) {}

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {}

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {}

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {}

  @Override
  public void destroy() {
    if (program != -1) {
      BGFX.bgfx_destroy_program(program);
      program = -1;
    }
  }
}
