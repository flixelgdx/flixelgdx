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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
import org.jetbrains.annotations.NotNull;

/**
 * A compiled shader program with a FlixelGDX lifecycle.
 *
 * <p>This is the single type game code works with for shader effects. It wraps a backend-owned
 * {@link FlixelShaderProgram} handle and integrates with the standard update/destroy pipeline via
 * {@link FlixelBasic}, which enables time-driven uniform updates in {@link #update(float)} and
 * guaranteed GPU resource cleanup on {@link #destroy()}.
 *
 * <p>Camera-level post-processing is the primary use case: assign a shader to a
 * {@link FlixelCamera} via {@link FlixelCamera#setShader(FlixelShader)} and the camera will
 * automatically render its scene into a framebuffer and composite the result through this shader
 * every frame.
 *
 * <p>To drive per-frame uniforms such as {@code u_time}, subclass {@code FlixelShader}, override
 * {@link #update(float)} to track state, and override {@link #applyUniforms()} to upload it:
 *
 * <pre>{@code
 * public class TimedShader extends FlixelShader {
 *   private float time;
 *
 *   public TimedShader() {
 *     super("...");
 *   }
 *
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     time += elapsed;
 *   }
 *
 *   public void applyUniforms() {
 *     getProgram().setUniform("u_time", time);
 *   }
 * }
 * }</pre>
 */
public class FlixelShader extends FlixelBasic {

  /**
   * The default pass-through vertex shader used when no custom vertex source is provided.
   *
   * <p>Attribute and uniform names match the framework's sprite batch contract so the
   * composite draw in {@link FlixelCamera} works without additional setup.
   *
   * <p>Exposed as {@code public} so subclasses that add custom constructors or factory
   * methods can reuse the same vertex source without duplicating it.
   */
  public static final String DEFAULT_VERT = """
      #ifdef GL_ES
      precision mediump float;
      #endif
      attribute vec4 a_position;
      attribute vec4 a_color;
      attribute vec2 a_texCoord0;
      uniform mat4 u_projTrans;
      varying vec4 v_color;
      varying vec2 v_texCoords;
      void main() {
        v_color = a_color;
        v_color.a = v_color.a * (255.0 / 254.0);
        v_texCoords = a_texCoord0;
        gl_Position = u_projTrans * a_position;
      }
      """;

  private FlixelShaderProgram program;
  private String loadedName;
  private String vertSrc;
  private String fragSrc;

  /**
   * Wraps a backend program compiled by {@link #load(String)} and records the shader name so
   * {@link #reload()} can recompile it later.
   *
   * @param program The backend program handle to wrap.
   * @param loadedName The name passed to {@link #load(String)}.
   */
  private FlixelShader(FlixelShaderProgram program, String loadedName) {
    this.program = program;
    this.loadedName = loadedName;
  }

  /**
   * Prepares a shader using a built-in pass-through vertex shader and the given fragment source.
   *
   * <p>GLSL source compilation is handled at runtime by the web backend. On the desktop bgfx
   * backend, use the FlixelGDX Gradle plugin to generate precompiled shader resources at build
   * time and load them via {@link #load(String)} instead.
   *
   * @param fragSrc GLSL ES 2.0 fragment shader source code.
   */
  public FlixelShader(String fragSrc) {
    this(DEFAULT_VERT, fragSrc);
  }

  /**
   * Prepares a shader from explicit vertex and fragment GLSL source strings.
   *
   * <p>GLSL source compilation is handled at runtime by the web backend. On the desktop bgfx
   * backend, use the FlixelGDX Gradle plugin to generate precompiled shader resources at build
   * time and load them via {@link #load(String)} instead.
   *
   * @param vertSrc GLSL ES 2.0 vertex shader source code.
   * @param fragSrc GLSL ES 2.0 fragment shader source code.
   */
  public FlixelShader(String vertSrc, String fragSrc) {
    this.vertSrc = vertSrc;
    this.fragSrc = fragSrc;
    this.program = Flixel.graphics.compileShaderSource(vertSrc, fragSrc);
  }

  /**
   * Loads a shader that the FlixelGDX Gradle plugin cross-compiled at build time.
   *
   * <p>Give the same name declared in the {@code shaders} build block. The framework picks
   * the precompiled variant matching the active renderer (OpenGL, Vulkan, Metal, or Direct3D) and
   * compiles it into a backend program, so one shader source runs everywhere with no per-platform
   * authoring.
   *
   * <p>If the matching variant is missing (for example, a Direct3D variant a build could not
   * produce without an FXC compiler), the returned shader reports {@link #isCompiled()} as
   * {@code false} and the effect degrades to an unshaded draw rather than crashing.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelShader crt = FlixelShader.load("crt");
   * Flixel.cameras.first().setShader(crt);
   * }</pre>
   *
   * @param name The shader name from the {@code shaders} build block.
   * @return A {@code FlixelShader} wrapping the compiled variant for the active renderer.
   */
  @NotNull
  public static FlixelShader load(@NotNull String name) {
    return new FlixelShader(Flixel.graphics.compileShaderProgram(name), name);
  }

  /**
   * Releases the compiled backend shader and marks this shader as destroyed.
   *
   * <p>After this call, {@link #getProgram()} returns {@code null} and the shader must not be
   * used for rendering. Any {@link FlixelCamera} that holds a reference to this shader should
   * have it cleared via {@link FlixelCamera#setShader(FlixelShader) setShader(null)} before
   * calling this method.
   */
  @Override
  public void destroy() {
    super.destroy();
    if (program != null && program != FlixelUnsupportedShader.INSTANCE) {
      program.destroy();
      program = null;
    }
  }

  /**
   * Recompiles this shader from the same source it was originally built from.
   *
   * <p>Releases the current GPU program and builds a fresh one through the active backend. Any
   * {@link FlixelCamera} or global shader chain holding a reference to this instance picks up the
   * new program automatically on the next frame, because the camera stores the
   * {@code FlixelShader} object rather than the underlying handle.
   *
   * <p>For shaders loaded via {@link #load(String)}, the backend re-reads the precompiled resource
   * for the active renderer. For shaders constructed from GLSL source strings, the backend recompiles
   * the stored source.
   */
  public void reload() {
    if (program != null && program != FlixelUnsupportedShader.INSTANCE) {
      program.destroy();
    }
    if (loadedName != null) {
      program = Flixel.graphics.compileShaderProgram(loadedName);
    } else if (vertSrc != null) {
      program = Flixel.graphics.compileShaderSource(vertSrc, fragSrc);
    }
  }

  /**
   * Uploads per-frame uniforms to the shader while it is bound for rendering.
   *
   * <p>Called automatically by the framework immediately after the composite batch begins the
   * post-processing draw pass, at which point the shader is bound. Do not call this yourself
   * unless the shader is already bound - writing uniforms to an unbound program has no effect on
   * some backends.
   *
   * <p>The base implementation is a no-op. Subclasses should override this to upload whatever
   * uniforms they need and call {@code super.applyUniforms()} for future-proofing.
   */
  public void applyUniforms() {}

  /**
   * Returns the underlying backend shader program, or {@code null} if {@link #destroy()} has
   * been called.
   *
   * @return The compiled backend program handle.
   */
  public FlixelShaderProgram getProgram() {
    return program;
  }

  /**
   * Returns {@code true} if the underlying shader program compiled successfully and is ready for
   * use.
   *
   * @return Whether the backend shader program is valid.
   */
  public boolean isCompiled() {
    return program != null && program.isValid();
  }

  /**
   * Returns whether this shader compiled without errors and is ready to use.
   *
   * @return {@code true} if the shader compiled successfully and is ready to use.
   */
  public boolean getCompiled() {
    return isCompiled();
  }
}
