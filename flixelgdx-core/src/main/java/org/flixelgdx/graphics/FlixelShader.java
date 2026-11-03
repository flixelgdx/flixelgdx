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
package org.flixelgdx.graphics;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A compiled WGSL shader with a FlixelGDX lifecycle.
 *
 * <p>This is the single type game code works with for shader effects. It wraps a backend-owned
 * {@link FlixelShaderProgram} handle and integrates with the standard update/destroy pipeline via
 * {@link FlixelBasic}, which enables time-driven uniform updates in {@link #update(float)} and
 * guaranteed GPU resource cleanup on {@link #destroy()}.
 *
 * <p>Shaders are written in WGSL, the shader language of WebGPU, and the same file runs on every platform.
 * A shader is one {@code .wgsl} file with a fragment entry point named {@code fs_main}; the framework supplies
 * the camera matrix, the texture, the sampler and a default vertex stage. The file's own settings live in one
 * uniform struct at {@code @group(1) @binding(0)} and are set from Java by member name. The full contract is in
 * the renderer's {@code FlixelWgsl} class.
 *
 * <p>Camera-level post-processing is the primary use case: assign a shader to a
 * {@link FlixelCamera} via {@link FlixelCamera#setShader(FlixelShader)} and the camera will
 * automatically render its scene into a framebuffer and composite the result through this shader
 * every frame.
 *
 * <p>Here is a shader that fades the picture to gray, with a uniform that Java animates:
 *
 * <pre>{@code
 * // shaders/gray.wgsl
 * struct Params {
 *   amount: f32,
 * }
 * @group(1) @binding(0) var<uniform> params: Params;
 *
 * @fragment
 * fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
 *   let c = textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
 *   let g = dot(c.rgb, vec3f(0.299, 0.587, 0.114));
 *   return vec4f(mix(c.rgb, vec3f(g), params.amount), c.a);
 * }
 *
 * // Java
 * FlixelShader gray = FlixelShader.load("gray");
 * Flixel.cameras.first().setShader(gray);
 * gray.setUniform("amount", 0.8f);
 * }</pre>
 *
 * <p>To drive per-frame uniforms such as time, subclass {@code FlixelShader}, override
 * {@link #update(float)} to track state, and override {@link #applyUniforms()} to upload it:
 *
 * <pre>{@code
 * public class TimedShader extends FlixelShader {
 *   private float time;
 *
 *   public TimedShader(String wgsl) {
 *     super(wgsl);
 *   }
 *
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     time += elapsed;
 *   }
 *
 *   public void applyUniforms() {
 *     setUniform("time", time);
 *   }
 * }
 * }</pre>
 */
public class FlixelShader extends FlixelBasic {

  private FlixelShaderProgram program;
  private String loadedName;
  private String source;

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
   * Compiles a shader from the text of a WGSL shader file.
   *
   * <p>The source needs a fragment entry point named {@code fs_main}. It may add a vertex entry point named
   * {@code vs_main}; when it does not, the framework's default vertex stage is used. If the source does not
   * compile, the error is logged and {@link #isCompiled()} reports {@code false}; drawing with such a shader
   * falls back to the plain sprite shader.
   *
   * @param wgslSource The WGSL source of the shader file.
   */
  public FlixelShader(@NotNull String wgslSource) {
    this.source = wgslSource;
    this.program = Flixel.graphics.compileShaderSource(wgslSource);
  }

  /**
   * Loads the shader file {@code shaders/<name>.wgsl}.
   *
   * <p>The file is read through the asset manager, so it can live in the assets folder or in the game's resources
   * (a {@code shaders} folder, which is where the shader plugin puts it). It is compiled the same way as {@link #FlixelShader(String)}, so a plain text
   * file is all a shader needs; there is no build step.
   *
   * <p>If the file is missing or does not compile, the returned shader reports {@link #isCompiled()} as
   * {@code false} and the effect degrades to an unshaded draw rather than crashing.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelShader crt = FlixelShader.load("crt");
   * Flixel.cameras.first().setShader(crt);
   * }</pre>
   *
   * @param name The shader name, which is the file name without the folder or the {@code .wgsl} extension.
   * @return A {@code FlixelShader} wrapping the compiled shader.
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
   * {@code FlixelShader} object rather than the underlying handle. Uniform values set before the reload are
   * lost and must be set again.
   *
   * <p>For shaders created with {@link #load(String)}, the file is read again. For shaders constructed from a
   * source string, the stored source is recompiled.
   */
  public void reload() {
    if (program != null && program != FlixelUnsupportedShader.INSTANCE) {
      program.destroy();
    }
    if (loadedName != null) {
      program = Flixel.graphics.compileShaderProgram(loadedName);
    } else if (source != null) {
      program = Flixel.graphics.compileShaderSource(source);
    }
  }

  /**
   * Uploads per-frame uniforms to the shader while it is bound for rendering.
   *
   * <p>Called automatically by the framework right after the composite batch begins the
   * post-processing draw pass. The base implementation is a no-op. Subclasses should override this to set
   * whatever uniforms they need (with {@link #setUniform(String, float)} and friends) and call
   * {@code super.applyUniforms()} for future-proofing.
   */
  public void applyUniforms() {}

  /**
   * Sets a number in the shader's uniform struct by member name.
   *
   * <p>Does nothing when the shader has no member with that name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param value The value to store.
   */
  public void setUniform(@NotNull String name, float value) {
    if (program != null) {
      program.setUniform(name, value);
    }
  }

  /**
   * Sets an integer in the shader's uniform struct by member name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param value The value to store.
   */
  public void setUniform(@NotNull String name, int value) {
    if (program != null) {
      program.setUniform(name, value);
    }
  }

  /**
   * Sets a {@code vec2} in the shader's uniform struct by member name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param value The vector whose {@code x} and {@code y} are stored.
   */
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {
    if (program != null) {
      program.setUniform(name, value);
    }
  }

  /**
   * Sets a {@code vec3} or {@code vec4} in the shader's uniform struct from a color.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param value The color to store.
   */
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {
    if (program != null) {
      program.setUniform(name, value);
    }
  }

  /**
   * Sets a {@code mat4x4} in the shader's uniform struct by member name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param value The matrix to store.
   */
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {
    if (program != null) {
      program.setUniform(name, value);
    }
  }

  /**
   * Sets a {@code vec2} in the shader's uniform struct by member name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param x The first component.
   * @param y The second component.
   */
  public void setUniform(@NotNull String name, float x, float y) {
    if (program != null) {
      program.setUniform(name, x, y);
    }
  }

  /**
   * Sets a {@code vec3} in the shader's uniform struct by member name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param x The first component.
   * @param y The second component.
   * @param z The third component.
   */
  public void setUniform(@NotNull String name, float x, float y, float z) {
    if (program != null) {
      program.setUniform(name, x, y, z);
    }
  }

  /**
   * Sets a {@code vec4} in the shader's uniform struct by member name.
   *
   * @param name The member name in the {@code @group(1) @binding(0)} struct.
   * @param x The first component.
   * @param y The second component.
   * @param z The third component.
   * @param w The fourth component.
   */
  public void setUniform(@NotNull String name, float x, float y, float z, float w) {
    if (program != null) {
      program.setUniform(name, x, y, z, w);
    }
  }

  /**
   * Returns the underlying backend shader program, or {@code null} if {@link #destroy()} has
   * been called.
   *
   * @return The compiled backend program handle.
   */
  @Nullable
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
