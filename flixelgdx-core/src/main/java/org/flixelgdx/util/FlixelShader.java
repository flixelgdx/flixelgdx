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
package org.flixelgdx.util;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;

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
 * <p>Two modes are available:
 * <ul>
 *   <li><b>libGDX mode</b> (constructors): write raw GLSL ES 2.0 targeting the standard
 *     libGDX {@link com.badlogic.gdx.graphics.g2d.SpriteBatch SpriteBatch} attribute contract.
 *     Fragment shaders receive the camera output as {@code uniform sampler2D u_texture} via
 *     the {@code v_texCoords} varying.</li>
 *   <li><b>HaxeFlixel mode} ({@link #fromHaxeFlixel(String)})</b>: write or copy a filter
 *     shader from HaxeFlixel using {@code #pragma header}, {@code #pragma body},
 *     {@code bitmap}, {@code openfl_TextureCoordv}, and {@code flixel_texture2D(...)}. The
 *     preprocessor rewrites those to libGDX-compatible names before compilation.</li>
 * </ul>
 *
 * <p>Raw libGDX mode example - a grayscale filter applied to the default camera:
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
 * Flixel.cameras.first().setShader(gray);
 * }</pre>
 *
 * <p>HaxeFlixel mode example using a {@code .frag} file from disk:
 *
 * <pre>{@code
 * String src = Gdx.files.internal("shaders/bloom.frag").readString();
 * FlixelShader bloom = FlixelShader.fromHaxeFlixel(src);
 * Flixel.cameras.first().setShader(bloom);
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

  /**
   * GLSL {@code #define} macros prepended to every HaxeFlixel fragment shader.
   *
   * <p>These alias HaxeFlixel / OpenFL variable and function names to their libGDX equivalents
   * so the shader source compiles without modification:
   * <ul>
   *   <li>{@code bitmap} - the main texture sampler (maps to {@code u_texture})</li>
   *   <li>{@code openfl_TextureCoordv} - the UV coordinate varying (maps to {@code v_texCoords})</li>
   *   <li>{@code openfl_Alpha} - global alpha value (constant {@code 1.0})</li>
   *   <li>{@code openfl_TextureSize} - texture dimensions uniform (maps to {@code u_textureSize})</li>
   *   <li>{@code openfl_HasColorTransform} - color transform flag (constant {@code false})</li>
   *   <li>{@code flixel_texture2D(t, c)} - texture sampling helper (maps to {@code texture2D(t, c)})</li>
   * </ul>
   */
  private static final String HAXEFLIXEL_DEFINES =
      "#define bitmap u_texture\n"
          + "#define openfl_TextureCoordv v_texCoords\n"
          + "#define openfl_Alpha 1.0\n"
          + "#define openfl_TextureSize u_textureSize\n"
          + "#define openfl_HasColorTransform false\n"
          + "#define flixel_texture2D(t, c) texture2D(t, c)\n";

  /**
   * The GLSL source block that replaces {@code #pragma header} in HaxeFlixel shaders.
   *
   * <p>Declares the uniform sampler, UV coordinate varying, and color varying that the
   * compositing pipeline feeds into the fragment shader each frame. Using the libGDX-native
   * names here means the {@link #HAXEFLIXEL_DEFINES} aliases resolve correctly.
   */
  private static final String HAXEFLIXEL_HEADER_EXPANSION =
      "uniform sampler2D u_texture;\n"
          + "uniform vec2 u_textureSize;\n"
          + "varying vec4 v_color;\n"
          + "varying vec2 v_texCoords;\n";

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
   * Creates a {@code FlixelShader} from a HaxeFlixel-style fragment shader source string.
   *
   * <p>The preprocessor performs three transformations before compilation:
   * <ol>
   *   <li>Prepends {@link #HAXEFLIXEL_DEFINES} so HaxeFlixel names alias to libGDX names.</li>
   *   <li>Replaces {@code #pragma header} with {@link #HAXEFLIXEL_HEADER_EXPANSION},
   *       which declares the texture sampler, size uniform, and UV varyings.</li>
   *   <li>Removes any {@code #pragma body} lines, which have no meaning outside
   *       HaxeFlixel's own template system.</li>
   * </ol>
   *
   * <p>The built-in pass-through vertex shader is used, so no custom vertex source is needed.
   * Compilation errors are logged via {@link Flixel#error(String, Object)}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * String src = Gdx.files.internal("shaders/crt.frag").readString();
   * FlixelShader crt = FlixelShader.fromHaxeFlixel(src);
   * Flixel.cameras.first().setShader(crt);
   * }</pre>
   *
   * @param fragSrc HaxeFlixel fragment shader source, typically read from a {@code .frag} file.
   * @return A compiled {@code FlixelShader} ready to assign to a {@link FlixelCamera}.
   */
  public static FlixelShader fromHaxeFlixel(String fragSrc) {
    return new FlixelShader(DEFAULT_VERT, preprocessHaxeFlixel(fragSrc));
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

  /**
   * Runs the HaxeFlixel-to-libGDX preprocessing pipeline on a raw fragment shader string.
   *
   * @param src Raw HaxeFlixel fragment shader source.
   * @return Preprocessed GLSL ES 2.0 fragment source ready for libGDX compilation.
   */
  private static String preprocessHaxeFlixel(String src) {
    src = src.replace("#pragma header", HAXEFLIXEL_HEADER_EXPANSION);
    src = src.replace("#pragma body", "");
    return HAXEFLIXEL_DEFINES + src;
  }
}
