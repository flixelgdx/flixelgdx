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

import org.jetbrains.annotations.Nullable;

/**
 * A backend-agnostic bundle of shader code, holding one variant per graphics backend.
 *
 * <p>Different backends speak different shader languages, and they consume them at different times.
 * There is no single portable shader format, so a {@code FlixelShaderSource} simply carries every
 * variant a game might need, and each backend picks the one it understands:
 *
 * <ul>
 *   <li><b>bgfx</b> (native) uses shaders compiled ahead of time by {@code shaderc} into per-platform
 *       binary blobs. bgfx selects the right blob at runtime (SPIR-V, Metal, D3D, or GLES), so the
 *       {@link #bgfxVertex()} and {@link #bgfxFragment()} payloads are already-compiled bytes.</li>
 *   <li><b>WebGPU</b> (web) uses {@link #wgsl() WGSL} source text, which the browser compiles at
 *       runtime.</li>
 *   <li><b>WebGL</b> (web fallback) uses {@link #glslVertex() GLSL ES} vertex and fragment source
 *       text, compiled at runtime.</li>
 * </ul>
 *
 * <p>You do not have to supply every variant. Provide only the ones for the backends you target;
 * a backend that finds its variant missing reports the shader as unsupported rather than failing
 * hard. The built-in framework shaders ship every variant.
 *
 * <p>Hand a source to {@link FlixelGraphicsManager} (or the active backend) to compile it into a
 * ready-to-use {@link FlixelShader} handle.
 *
 * <p><b>Attribute contract:</b> a shader meant to feed the sprite batch must declare the same
 * vertex attributes the batch writes ({@code a_position}, {@code a_color}, {@code a_texCoord0},
 * {@code a_texIndex}) and the same sampler uniforms ({@code u_texture0} through {@code u_textureN}),
 * expressed in whatever language each variant uses.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelShaderSource source = FlixelShaderSource.builder()
 *     .wgsl(myWgslSource)
 *     .glsl(myGlslVertex, myGlslFragment)
 *     .build();
 * }</pre>
 *
 * @see FlixelShader
 * @see FlixelGraphicsManager
 */
public final class FlixelShaderSource {

  @Nullable
  private final byte[] bgfxVertex;
  @Nullable
  private final byte[] bgfxFragment;
  @Nullable
  private final String wgsl;
  @Nullable
  private final String glslVertex;
  @Nullable
  private final String glslFragment;

  private FlixelShaderSource(Builder builder) {
    this.bgfxVertex = builder.bgfxVertex;
    this.bgfxFragment = builder.bgfxFragment;
    this.wgsl = builder.wgsl;
    this.glslVertex = builder.glslVertex;
    this.glslFragment = builder.glslFragment;
  }

  /**
   * Starts building a source from scratch.
   *
   * @return A fresh builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return Compiled bgfx vertex shader bytes, or {@code null} when no bgfx variant was supplied.
   */
  @Nullable
  public byte[] bgfxVertex() {
    return bgfxVertex;
  }

  /**
   * @return Compiled bgfx fragment shader bytes, or {@code null} when no bgfx variant was supplied.
   */
  @Nullable
  public byte[] bgfxFragment() {
    return bgfxFragment;
  }

  /**
   * @return WGSL source for the WebGPU backend, or {@code null} when no WGSL variant was supplied.
   */
  @Nullable
  public String wgsl() {
    return wgsl;
  }

  /**
   * @return GLSL ES vertex source for the WebGL backend, or {@code null} when no GLSL variant was supplied.
   */
  @Nullable
  public String glslVertex() {
    return glslVertex;
  }

  /**
   * @return GLSL ES fragment source for the WebGL backend, or {@code null} when no GLSL variant was supplied.
   */
  @Nullable
  public String glslFragment() {
    return glslFragment;
  }

  /**
   * Collects one or more per-backend shader variants into a {@link FlixelShaderSource}.
   *
   * <p>Call only the setters for the backends you target, then {@link #build()}.
   */
  public static final class Builder {

    @Nullable
    private byte[] bgfxVertex;
    @Nullable
    private byte[] bgfxFragment;
    @Nullable
    private String wgsl;
    @Nullable
    private String glslVertex;
    @Nullable
    private String glslFragment;

    private Builder() {}

    /**
     * Supplies the compiled bgfx variant (native backend).
     *
     * @param vertex Compiled bgfx vertex shader bytes.
     * @param fragment Compiled bgfx fragment shader bytes.
     * @return This builder, for chaining.
     */
    public Builder bgfx(byte[] vertex, byte[] fragment) {
      this.bgfxVertex = vertex;
      this.bgfxFragment = fragment;
      return this;
    }

    /**
     * Supplies the WGSL variant (WebGPU backend).
     *
     * @param source WGSL source text.
     * @return This builder, for chaining.
     */
    public Builder wgsl(String source) {
      this.wgsl = source;
      return this;
    }

    /**
     * Supplies the GLSL ES variant (WebGL fallback backend).
     *
     * @param vertex GLSL ES vertex source text.
     * @param fragment GLSL ES fragment source text.
     * @return This builder, for chaining.
     */
    public Builder glsl(String vertex, String fragment) {
      this.glslVertex = vertex;
      this.glslFragment = fragment;
      return this;
    }

    /**
     * @return A new immutable {@link FlixelShaderSource} holding the supplied variants.
     */
    public FlixelShaderSource build() {
      return new FlixelShaderSource(this);
    }
  }
}
