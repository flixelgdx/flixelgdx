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
import org.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;

/**
 * Compiles and links GLES shader programs with the framework's fixed vertex attribute layout.
 *
 * <p>Every program -- whether the built-in multi-texture sprite shader or a custom shader from
 * game code -- must bind vertex attributes to the same fixed locations before linking. That is
 * what lets the batch switch programs without rebinding or reconfiguring its VAO: position is
 * always {@value #POSITION}, texture coordinate is always {@value #TEXCOORD}, color is always
 * {@value #COLOR}, and the multi-texture index is always {@value #TEXINDEX}.
 *
 * <p>Custom shaders typically do not declare {@code a_texIndex}, so
 * {@link #buildCustom(String, String)} only binds the first three locations. The batch then
 * simply leaves attribute slot {@value #TEXINDEX} disabled when a custom shader is active.
 */
final class FlixelGlesPrograms {

  /** Fixed vertex attribute slot for quad position ({@code a_position}). */
  static final int POSITION = 0;

  /** Fixed vertex attribute slot for texture coordinate ({@code a_texCoord0}). */
  static final int TEXCOORD = 1;

  /** Fixed vertex attribute slot for vertex color ({@code a_color}). */
  static final int COLOR = 2;

  /** Fixed vertex attribute slot for the multi-texture slot index ({@code a_texIndex}). */
  static final int TEXINDEX = 3;

  private FlixelGlesPrograms() {}

  /**
   * Compiles and links a built-in batch program, binding all four vertex attributes.
   *
   * @param vertexSource GLSL vertex shader source.
   * @param fragmentSource GLSL fragment shader source.
   * @return The linked program name, or {@code 0} on failure.
   */
  static int build(@NotNull String vertexSource, @NotNull String fragmentSource) {
    int vs = compile(GLES30.GL_VERTEX_SHADER, vertexSource);
    int fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
    if (vs == 0 || fs == 0) {
      if (vs != 0) {
        GLES30.glDeleteShader(vs);
      }
      if (fs != 0) {
        GLES30.glDeleteShader(fs);
      }
      return 0;
    }

    int program = GLES30.glCreateProgram();
    GLES30.glAttachShader(program, vs);
    GLES30.glAttachShader(program, fs);
    GLES30.glBindAttribLocation(program, POSITION, "a_position");
    GLES30.glBindAttribLocation(program, TEXCOORD, "a_texCoord0");
    GLES30.glBindAttribLocation(program, COLOR, "a_color");
    GLES30.glBindAttribLocation(program, TEXINDEX, "a_texIndex");
    GLES30.glLinkProgram(program);

    GLES30.glDeleteShader(vs);
    GLES30.glDeleteShader(fs);

    int[] status = new int[1];
    GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
    if (status[0] == GLES30.GL_FALSE) {
      Flixel.warn("GLES", "Shader program failed to link: " + GLES30.glGetProgramInfoLog(program));
      GLES30.glDeleteProgram(program);
      return 0;
    }
    return program;
  }

  /**
   * Compiles and links a custom (game-supplied) program, binding only the three standard
   * vertex attributes that every shader declares.
   *
   * <p>Custom shaders do not declare {@code a_texIndex}, so binding that location is skipped.
   * The batch will disable attribute slot {@value #TEXINDEX} while this program is active.
   *
   * @param vertexSource GLSL vertex shader source.
   * @param fragmentSource GLSL fragment shader source.
   * @return The linked program name, or {@code 0} on failure.
   */
  static int buildCustom(@NotNull String vertexSource, @NotNull String fragmentSource) {
    int vs = compile(GLES30.GL_VERTEX_SHADER, vertexSource);
    int fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
    if (vs == 0 || fs == 0) {
      if (vs != 0) {
        GLES30.glDeleteShader(vs);
      }
      if (fs != 0) {
        GLES30.glDeleteShader(fs);
      }
      return 0;
    }

    int program = GLES30.glCreateProgram();
    GLES30.glAttachShader(program, vs);
    GLES30.glAttachShader(program, fs);
    GLES30.glBindAttribLocation(program, POSITION, "a_position");
    GLES30.glBindAttribLocation(program, TEXCOORD, "a_texCoord0");
    GLES30.glBindAttribLocation(program, COLOR, "a_color");
    GLES30.glLinkProgram(program);

    GLES30.glDeleteShader(vs);
    GLES30.glDeleteShader(fs);

    int[] status = new int[1];
    GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
    if (status[0] == GLES30.GL_FALSE) {
      Flixel.warn("GLES", "Custom shader failed to link: " + GLES30.glGetProgramInfoLog(program));
      GLES30.glDeleteProgram(program);
      return 0;
    }
    return program;
  }

  /**
   * Compiles a single shader stage, logging and returning {@code 0} on failure.
   *
   * @param type The shader stage constant ({@code GL_VERTEX_SHADER} or
   *     {@code GL_FRAGMENT_SHADER}).
   * @param source The GLSL source.
   * @return The compiled shader name, or {@code 0} on failure.
   */
  private static int compile(int type, @NotNull String source) {
    int shader = GLES30.glCreateShader(type);
    GLES30.glShaderSource(shader, source);
    GLES30.glCompileShader(shader);

    int[] status = new int[1];
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
    if (status[0] == GLES30.GL_FALSE) {
      Flixel.warn("GLES", "Shader stage failed to compile: " + GLES30.glGetShaderInfoLog(shader));
      GLES30.glDeleteShader(shader);
      return 0;
    }
    return shader;
  }
}
