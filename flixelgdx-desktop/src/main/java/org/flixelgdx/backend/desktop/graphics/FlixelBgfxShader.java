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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.bgfx.BGFX;

/**
 * A compiled bgfx shader program.
 *
 * <p>Holds a bgfx program handle built from a vertex and fragment shader pair. Uniform setters
 * cache their values, and {@link #applyUniforms()} hands them to bgfx right before each draw that
 * uses this program. Custom per-sprite, per-camera, and global effect shaders wrap one of these.
 *
 * <p>bgfx only has {@code vec4} and matrix uniforms, so every scalar or vector value is uploaded as
 * a {@code vec4} with the unused components set to zero. The FlixelGDX shader plugin declares each
 * {@code float}, {@code int}, {@code vec2}, and {@code vec3} uniform as a {@code vec4} under the
 * same name, so the values line up with what the shader reads.
 */
public class FlixelBgfxShader implements FlixelShaderProgram {

  @NotNull
  private final FlixelArray<Uniform> uniformList = new FlixelArray<>();

  @NotNull
  private final FlixelMap<String, Uniform> uniforms = new FlixelMap<>();

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
   * Hands every cached uniform value to bgfx so the next submitted draw uses them.
   *
   * <p>bgfx records uniform values into the draw that follows, so this must run after the batch's
   * other state is set and before {@code bgfx_submit(...)}.
   */
  void applyUniforms() {
    for (int i = 0; i < uniformList.getSize(); i++) {
      Uniform u = uniformList.get(i);
      BGFX.bgfx_set_uniform(u.handle, u.data, 1);
    }
  }

  /** Returns the bgfx program handle for submission. */
  short getProgram() {
    return program;
  }

  @Override
  public boolean isValid() {
    return program != -1;
  }

  @Override
  public void setUniform(@NotNull String name, float value) {
    float[] d = obtain(name, false).data;
    d[0] = value;
    d[1] = 0f;
    d[2] = 0f;
    d[3] = 0f;
  }

  @Override
  public void setUniform(@NotNull String name, int value) {
    setUniform(name, (float) value);
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {
    float[] d = obtain(name, false).data;
    d[0] = value.x;
    d[1] = value.y;
    d[2] = 0f;
    d[3] = 0f;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {
    float[] d = obtain(name, false).data;
    d[0] = value.r;
    d[1] = value.g;
    d[2] = value.b;
    d[3] = value.a;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {
    System.arraycopy(value.val, 0, obtain(name, true).data, 0, 16);
  }

  @Override
  public void destroy() {
    for (int i = 0; i < uniformList.getSize(); i++) {
      BGFX.bgfx_destroy_uniform(uniformList.get(i).handle);
    }
    uniformList.clear();
    uniforms.clear();
    if (program != -1) {
      BGFX.bgfx_destroy_program(program);
      program = -1;
    }
  }

  /**
   * Returns the cached holder for a uniform, creating its bgfx handle the first time it is set.
   *
   * @param name The uniform name as declared in the shader.
   * @param matrix Whether the uniform is a 4x4 matrix rather than a {@code vec4}.
   * @return The holder to write the new value into.
   */
  @NotNull
  private Uniform obtain(@NotNull String name, boolean matrix) {
    Uniform u = uniforms.get(name);
    if (u == null) {
      int type = matrix ? BGFX.BGFX_UNIFORM_TYPE_MAT4 : BGFX.BGFX_UNIFORM_TYPE_VEC4;
      u = new Uniform(BGFX.bgfx_create_uniform(name, type, 1), matrix ? 16 : 4);
      uniforms.put(name, u);
      uniformList.add(u);
    }
    return u;
  }

  /** One uniform's bgfx handle and its latest value. */
  private static final class Uniform {

    private final float[] data;

    private final short handle;

    private Uniform(short handle, int size) {
      this.handle = handle;
      this.data = new float[size];
    }
  }
}
