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
package org.flixelgdx.backend.android.graphics;

import android.opengl.GLES30;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

/**
 * A compiled GLES shader program for custom game shaders.
 *
 * <p>Game code sets uniforms (for example in {@link org.flixelgdx.util.FlixelShader#applyUniforms()})
 * at a point where this program is not the one currently bound, and OpenGL ES only accepts a
 * uniform upload for the active program. So rather than uploading immediately, this class stores
 * each uniform's latest value and uploads them all in {@link #apply()}, which the batch calls
 * right after it calls {@code glUseProgram} for this shader. Values are stored in reusable
 * holders keyed by name, so setting a uniform every frame allocates nothing after the first time
 * a name is seen.
 *
 * <p>Instances are created by {@link FlixelAndroidGraphics#compileShaderSource} and must only
 * be used on the GL thread.
 */
class FlixelGlesShaderProgram implements FlixelShaderProgram {

  private static final int TYPE_FLOAT = 0;
  private static final int TYPE_INT = 1;
  private static final int TYPE_VEC2 = 2;
  private static final int TYPE_COLOR = 3;
  private static final int TYPE_MATRIX = 4;

  private final int program;
  private final int projTransLocation;
  private final int textureLocation;

  @NotNull
  private final FlixelArray<String> uniformNames = new FlixelArray<>();

  @NotNull
  private final FlixelMap<String, Uniform> uniforms = new FlixelMap<>();

  private boolean valid = true;

  /**
   * Wraps a linked GLES program.
   *
   * @param program The GL program name, from {@link FlixelGlesPrograms#buildCustom}.
   */
  FlixelGlesShaderProgram(int program) {
    this.program = program;
    this.projTransLocation = GLES30.glGetUniformLocation(program, "u_projTrans");
    this.textureLocation = GLES30.glGetUniformLocation(program, "u_texture");
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setUniform(@NotNull String name, float value) {
    Uniform u = obtain(name);
    u.type = TYPE_FLOAT;
    u.data[0] = value;
  }

  @Override
  public void setUniform(@NotNull String name, int value) {
    Uniform u = obtain(name);
    u.type = TYPE_INT;
    u.intValue = value;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {
    Uniform u = obtain(name);
    u.type = TYPE_VEC2;
    u.data[0] = value.x;
    u.data[1] = value.y;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {
    Uniform u = obtain(name);
    u.type = TYPE_COLOR;
    u.data[0] = value.r;
    u.data[1] = value.g;
    u.data[2] = value.b;
    u.data[3] = value.a;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {
    Uniform u = obtain(name);
    u.type = TYPE_MATRIX;
    System.arraycopy(value.val, 0, u.data, 0, 16);
  }

  @Override
  public void destroy() {
    valid = false;
    GLES30.glDeleteProgram(program);
  }

  /**
   * Uploads every stored uniform to the GPU. Must be called while this program is active via
   * {@code glUseProgram}.
   */
  void apply() {
    for (int i = 0; i < uniformNames.getSize(); i++) {
      String name = uniformNames.get(i);
      Uniform u = uniforms.get(name);
      if (!u.located) {
        u.location = GLES30.glGetUniformLocation(program, name);
        u.located = true;
      }
      if (u.location < 0) {
        continue;
      }
      switch (u.type) {
        case TYPE_FLOAT -> GLES30.glUniform1f(u.location, u.data[0]);
        case TYPE_INT -> GLES30.glUniform1i(u.location, u.intValue);
        case TYPE_VEC2 -> GLES30.glUniform2f(u.location, u.data[0], u.data[1]);
        case TYPE_COLOR -> GLES30.glUniform4f(u.location, u.data[0], u.data[1],
            u.data[2], u.data[3]);
        case TYPE_MATRIX -> GLES30.glUniformMatrix4fv(u.location, 1, false, u.data, 0);
        default -> {
        }
      }
    }
  }

  /**
   * Returns the GL program name so the batch can bind it.
   *
   * @return The GL program object name.
   */
  int getGlProgram() {
    return program;
  }

  /**
   * Returns the location of {@code u_projTrans}, or {@code -1} when the shader does not declare
   * it.
   *
   * @return The projection-transform uniform location, or {@code -1}.
   */
  int getProjTransLocation() {
    return projTransLocation;
  }

  /**
   * Returns the location of {@code u_texture}, or {@code -1} when the shader does not declare
   * it.
   *
   * @return The texture sampler uniform location, or {@code -1}.
   */
  int getTextureLocation() {
    return textureLocation;
  }

  /**
   * Returns the reusable holder for a uniform name, creating and registering it the first time.
   *
   * @param name The uniform name.
   * @return The holder to write the new value into.
   */
  private Uniform obtain(String name) {
    Uniform u = uniforms.get(name);
    if (u == null) {
      u = new Uniform();
      uniforms.put(name, u);
      uniformNames.add(name);
    }
    return u;
  }

  /** Stores one uniform's latest value and its cached GL location. */
  private static final class Uniform {

    private final float[] data = new float[16];

    private int location = -1;
    private int type;
    private int intValue;

    private boolean located;
  }
}
