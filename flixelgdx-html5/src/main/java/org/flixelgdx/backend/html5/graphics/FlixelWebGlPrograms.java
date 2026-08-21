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

import org.flixelgdx.Flixel;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;

/**
 * Compiles and links WebGL shader programs against the framework's fixed vertex layout.
 *
 * <p>Every program the web backend uses, whether the built-in sprite shader or a game's custom
 * shader, reads the same interleaved vertex buffer. For that to work, the vertex attributes must sit
 * at the same locations in every program, so this helper binds them by name to fixed slots before
 * linking. That is what lets the batch switch shaders without rebinding its buffers: position is
 * always slot {@value #POSITION}, texture coordinate is always slot {@value #TEXCOORD}, and color is
 * always slot {@value #COLOR}.
 *
 * <p>The attribute and uniform names match the framework's GLSL contract ({@code a_position},
 * {@code a_texCoord0}, {@code a_color}, {@code u_projTrans}, {@code u_texture}), so a shader authored
 * against the shader plugin or built from source compiles unchanged here.
 */
public final class FlixelWebGlPrograms {

  /** The fixed vertex attribute slot for the quad position. */
  public static final int POSITION = 0;

  /** The fixed vertex attribute slot for the texture coordinate. */
  public static final int TEXCOORD = 1;

  /** The fixed vertex attribute slot for the vertex color. */
  public static final int COLOR = 2;

  private FlixelWebGlPrograms() {}

  /**
   * Compiles and links a program from GLSL source, binding the framework's vertex attributes to
   * their fixed slots.
   *
   * @param gl The rendering context.
   * @param vertexSource GLSL vertex shader source.
   * @param fragmentSource GLSL fragment shader source.
   * @return The linked program, or {@code null} when a stage fails to compile or the link fails.
   */
  public static WebGLProgram build(WebGLRenderingContext gl, String vertexSource, String fragmentSource) {
    WebGLShader vertex = compile(gl, WebGLRenderingContext.VERTEX_SHADER, vertexSource);
    WebGLShader fragment = compile(gl, WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource);
    if (vertex == null || fragment == null) {
      return null;
    }

    WebGLProgram program = gl.createProgram();
    gl.attachShader(program, vertex);
    gl.attachShader(program, fragment);
    gl.bindAttribLocation(program, POSITION, "a_position");
    gl.bindAttribLocation(program, TEXCOORD, "a_texCoord0");
    gl.bindAttribLocation(program, COLOR, "a_color");
    gl.linkProgram(program);

    // The shaders are no longer needed once linked into the program.
    gl.deleteShader(vertex);
    gl.deleteShader(fragment);

    if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
      Flixel.warn("Html5", "Shader program failed to link: " + gl.getProgramInfoLog(program));
      gl.deleteProgram(program);
      return null;
    }
    return program;
  }

  /**
   * Compiles a single shader stage, logging and returning {@code null} on failure.
   *
   * @param gl The rendering context.
   * @param type The shader stage constant.
   * @param source The GLSL source.
   * @return The compiled shader, or {@code null} when compilation fails.
   */
  private static WebGLShader compile(WebGLRenderingContext gl, int type, String source) {
    WebGLShader shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
      Flixel.warn("Html5", "Shader stage failed to compile: " + gl.getShaderInfoLog(shader));
      gl.deleteShader(shader);
      return null;
    }
    return shader;
  }
}
