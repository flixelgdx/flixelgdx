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
package org.flixelgdx.backend.html5.graphics;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLUniformLocation;

/**
 * A compiled WebGL shader program the web backend draws custom-shaded sprites with.
 *
 * <p>The tricky part on the web is <em>when</em> a uniform can be uploaded. Game code sets uniforms
 * (for example in {@link org.flixelgdx.util.FlixelShader#applyUniforms()}) at a point where this
 * program is not the one currently bound, and WebGL only accepts a uniform for the program that is
 * active. So rather than upload immediately, this class remembers each uniform's latest value and
 * uploads them all in {@link #apply(WebGLRenderingContext)}, which the batch calls right after it
 * binds the program. Values are stored in reusable holders keyed by name, so setting a uniform every
 * frame allocates nothing after the first time it is seen.
 */
public class FlixelWebGlShaderProgram implements FlixelShaderProgram {

  private static final int TYPE_FLOAT = 0;
  private static final int TYPE_INT = 1;
  private static final int TYPE_VEC2 = 2;
  private static final int TYPE_COLOR = 3;
  private static final int TYPE_MATRIX = 4;

  @NotNull
  private final WebGLRenderingContext gl;

  @NotNull
  private final WebGLProgram program;

  private final WebGLUniformLocation projTransLocation;
  private final WebGLUniformLocation textureLocation;

  @NotNull
  private final FlixelArray<String> uniformNames = new FlixelArray<>();

  @NotNull
  private final FlixelMap<String, Uniform> uniforms = new FlixelMap<>();

  private boolean valid = true;

  /**
   * Wraps a linked WebGL program.
   *
   * @param gl The rendering context.
   * @param program The linked program.
   */
  public FlixelWebGlShaderProgram(@NotNull WebGLRenderingContext gl, @NotNull WebGLProgram program) {
    this.gl = gl;
    this.program = program;
    this.projTransLocation = gl.getUniformLocation(program, "u_projTrans");
    this.textureLocation = gl.getUniformLocation(program, "u_texture");
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setUniform(@NotNull String name, float value) {
    Uniform uniform = obtain(name);
    uniform.type = TYPE_FLOAT;
    uniform.data[0] = value;
  }

  @Override
  public void setUniform(@NotNull String name, int value) {
    Uniform uniform = obtain(name);
    uniform.type = TYPE_INT;
    uniform.intValue = value;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {
    Uniform uniform = obtain(name);
    uniform.type = TYPE_VEC2;
    uniform.data[0] = value.x;
    uniform.data[1] = value.y;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {
    Uniform uniform = obtain(name);
    uniform.type = TYPE_COLOR;
    uniform.data[0] = value.r;
    uniform.data[1] = value.g;
    uniform.data[2] = value.b;
    uniform.data[3] = value.a;
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {
    Uniform uniform = obtain(name);
    uniform.type = TYPE_MATRIX;
    System.arraycopy(value.val, 0, uniform.data, 0, 16);
  }

  @Override
  public void destroy() {
    valid = false;
    gl.deleteProgram(program);
  }

  /**
   * Uploads every stored uniform to the GPU. Must be called while this program is the active one.
   *
   * @param gl The rendering context.
   */
  public void apply(WebGLRenderingContext gl) {
    for (int i = 0; i < uniformNames.getSize(); i++) {
      String name = uniformNames.get(i);
      Uniform uniform = uniforms.get(name);
      if (!uniform.located) {
        uniform.location = gl.getUniformLocation(program, name);
        uniform.located = true;
      }
      if (uniform.location == null) {
        continue;
      }
      switch (uniform.type) {
        case TYPE_FLOAT -> gl.uniform1f(uniform.location, uniform.data[0]);
        case TYPE_INT -> gl.uniform1i(uniform.location, uniform.intValue);
        case TYPE_VEC2 -> gl.uniform2f(uniform.location, uniform.data[0], uniform.data[1]);
        case TYPE_COLOR -> gl.uniform4f(uniform.location, uniform.data[0], uniform.data[1],
            uniform.data[2], uniform.data[3]);
        case TYPE_MATRIX -> gl.uniformMatrix4fv(uniform.location, false, uniform.data);
        default -> {
        }
      }
    }
  }

  /**
   * Returns the underlying WebGL program so the batch can bind it.
   *
   * @return The linked program.
   */
  @NotNull
  public WebGLProgram getGlProgram() {
    return program;
  }

  /**
   * Returns the location of the {@code u_projTrans} matrix uniform, or {@code null} when the shader
   * does not declare it.
   *
   * @return The projection-transform uniform location.
   */
  public WebGLUniformLocation getProjTransLocation() {
    return projTransLocation;
  }

  /**
   * Returns the location of the {@code u_texture} sampler uniform, or {@code null} when the shader
   * does not declare it.
   *
   * @return The texture sampler uniform location.
   */
  public WebGLUniformLocation getTextureLocation() {
    return textureLocation;
  }

  /**
   * Returns the reusable holder for a uniform name, creating and registering it the first time.
   *
   * @param name The uniform name.
   * @return The holder to write the value into.
   */
  private Uniform obtain(String name) {
    Uniform uniform = uniforms.get(name);
    if (uniform == null) {
      uniform = new Uniform();
      uniforms.put(name, uniform);
      uniformNames.add(name);
    }
    return uniform;
  }

  /** A reusable store for one uniform's latest value and its cached location. */
  private static final class Uniform {

    private final float[] data = new float[16];

    private WebGLUniformLocation location;

    private int type;
    private int intValue;

    private boolean located;
  }
}
