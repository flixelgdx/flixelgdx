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
package org.flixelgdx;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * A compiled GLSL shader program with a FlixelGDX lifecycle.
 *
 * <p>FlixelShader wraps a libGDX {@link ShaderProgram} and integrates with the standard
 * FlixelGDX update/destroy pipeline via {@link FlixelBasic}. This allows time-based or
 * state-driven uniform updates in {@link #update(float)} and guaranteed GPU resource cleanup
 * on {@link #destroy()}.
 *
 * <p>Camera-level post-processing is the primary use case: assign a shader to a
 * {@link FlixelCamera} via {@link FlixelCamera#setShader(FlixelShader)} and the camera will
 * automatically render its scene into a framebuffer, then composite the result to screen using
 * this shader each frame.
 *
 * <p>FlixelShader targets OpenGL ES 2.0 GLSL. The single-argument constructor uses a built-in
 * pass-through vertex shader that matches the attribute contract of libGDX's standard
 * {@link com.badlogic.gdx.graphics.g2d.SpriteBatch SpriteBatch}
 * ({@code a_position}, {@code a_color}, {@code a_texCoord0}, {@code u_projTrans}). Custom
 * fragment shaders receive the camera output as {@code uniform sampler2D u_texture} and can
 * sample it via the {@code v_texCoords} varying.
 *
 * <p>Example - a grayscale filter applied to the default camera:
 *
 * <pre>{@code
 * FlixelShader gray = new FlixelShader(
 *     "varying vec2 v_texCoords;\n" +
 *     "uniform sampler2D u_texture;\n" +
 *     "void main() {\n" +
 *     "  vec4 c = texture2D(u_texture, v_texCoords);\n" +
 *     "  float g = dot(c.rgb, vec3(0.299, 0.587, 0.114));\n" +
 *     "  gl_FragColor = vec4(g, g, g, c.a);\n" +
 *     "}"
 * );
 * Flixel.camera.setShader(gray);
 * }</pre>
 *
 * <p>Override {@link #update(float)} to drive time-based uniforms, for example:
 *
 * <pre>{@code
 * public class WaveShader extends FlixelShader {
 *   private float time;
 *
 *   public WaveShader() {
 *     super(WAVE_FRAG_SRC);
 *   }
 *
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     time += elapsed;
 *     getProgram().bind();
 *     getProgram().setUniformf("u_time", time);
 *   }
 * }
 * }</pre>
 */
public class FlixelShader extends FlixelBasic {

  /**
   * The default pass-through vertex shader used when no custom vertex source is provided.
   *
   * <p>Attribute and uniform names match the libGDX
   * {@link com.badlogic.gdx.graphics.g2d.SpriteBatch SpriteBatch} contract so the composite
   * draw in {@link FlixelCamera} works without additional setup.
   */
  private static final String DEFAULT_VERT =
      "attribute vec4 a_position;\n"
          + "attribute vec4 a_color;\n"
          + "attribute vec2 a_texCoord0;\n"
          + "uniform mat4 u_projTrans;\n"
          + "varying vec4 v_color;\n"
          + "varying vec2 v_texCoords;\n"
          + "void main() {\n"
          + "  v_color = a_color;\n"
          + "  v_color.a = v_color.a * (255.0 / 254.0);\n"
          + "  v_texCoords = a_texCoord0;\n"
          + "  gl_Position = u_projTrans * a_position;\n"
          + "}\n";

  private ShaderProgram program;

  /**
   * Compiles a shader using a built-in pass-through vertex shader and the given fragment source.
   *
   * <p>The fragment shader receives the camera's framebuffer output as
   * {@code uniform sampler2D u_texture} and can sample via the {@code v_texCoords} varying.
   * Compilation errors are logged via {@link Flixel#error(String, Object)}.
   *
   * @param fragSrc GLSL ES 2.0 fragment shader source code.
   */
  public FlixelShader(String fragSrc) {
    this(DEFAULT_VERT, fragSrc);
  }

  /**
   * Compiles a shader from explicit vertex and fragment source strings.
   *
   * <p>Use this constructor when you need a custom vertex shader, for example to apply
   * mesh deformation or pass additional varyings to the fragment stage.
   * Compilation errors are logged via {@link Flixel#error(String, Object)}.
   *
   * @param vertSrc GLSL ES 2.0 vertex shader source code.
   * @param fragSrc GLSL ES 2.0 fragment shader source code.
   */
  public FlixelShader(String vertSrc, String fragSrc) {
    this.program = new ShaderProgram(vertSrc, fragSrc);
    if (!this.program.isCompiled()) {
      Flixel.error("FlixelShader", "Shader compilation failed:\n" + this.program.getLog());
    }
  }

  /**
   * Releases the compiled {@link ShaderProgram} and marks this shader as destroyed.
   *
   * <p>After this call, {@link #getProgram()} returns {@code null} and the shader must not be
   * used for rendering. Any {@link FlixelCamera} that holds a reference to this shader should
   * have it cleared via {@link FlixelCamera#setShader(FlixelShader) setShader(null)} before
   * calling this method.
   */
  @Override
  public void destroy() {
    super.destroy();
    if (program != null) {
      program.dispose();
      program = null;
    }
  }

  /**
   * Returns the underlying compiled {@link ShaderProgram}, or {@code null} if
   * {@link #destroy()} has been called.
   *
   * @return The compiled shader program.
   */
  public ShaderProgram getProgram() {
    return program;
  }

  /**
   * Returns {@code true} if the shader compiled without errors and is ready to use.
   *
   * @return Whether the underlying {@link ShaderProgram} compiled successfully.
   */
  public boolean isCompiled() {
    return program != null && program.isCompiled();
  }

  /**
   * Returns the compilation log from the underlying {@link ShaderProgram}, useful for
   * diagnosing compilation errors. Returns an empty string if the program is {@code null}.
   *
   * @return The GLSL compiler log.
   */
  public String getLog() {
    return program != null ? program.getLog() : "";
  }
}
