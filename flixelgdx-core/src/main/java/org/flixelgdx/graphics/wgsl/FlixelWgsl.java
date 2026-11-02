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
package org.flixelgdx.graphics.wgsl;

import org.flixelgdx.graphics.FlixelShaderProgram;
import org.jetbrains.annotations.NotNull;

/**
 * The shader contract every FlixelGDX renderer shares: how a user's WGSL file becomes a full shader module.
 *
 * <p>A FlixelGDX shader is one {@code .wgsl} file. It does not declare the camera matrix, the texture or the
 * sampler. The framework puts a <em>preamble</em> ({@link #PREAMBLE}, the single source of truth) in front of it, so
 * every shader sees the same names:
 *
 * <ul>
 *   <li>{@code FlixelVertexInput} and {@code FlixelVertexOutput}, the structs that carry a vertex through the
 *       pipeline.</li>
 *   <li>Bind group 0, owned by the framework: {@code flixel} (a {@code FlixelGlobals} uniform with the matrix
 *       {@code proj} and the sizes {@code texSize} and {@code texelSize}), {@code flixelTexture} and
 *       {@code flixelSampler}.</li>
 *   <li>The default vertex entry point {@code flixel_vs}.</li>
 * </ul>
 *
 * <p>Your file supplies the rest. It must contain a fragment entry point named {@value #FRAGMENT_ENTRY}. It may
 * contain a vertex entry point named {@value #VERTEX_ENTRY}; when it does not, {@value #DEFAULT_VERTEX_ENTRY} is
 * used. Bind group 1 is yours: declare a single uniform struct at {@code @group(1) @binding(0)} and set its
 * members by name with {@link FlixelShaderProgram#setUniform(String, float)} and friends.
 *
 * <p>A complete tint shader looks like this:
 *
 * <pre>{@code
 * struct Params {
 *   tint: vec4f,
 *   amount: f32,
 * }
 * @group(1) @binding(0) var<uniform> params: Params;
 *
 * @fragment
 * fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
 *   let c = textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
 *   return vec4f(mix(c.rgb, params.tint.rgb, params.amount), c.a);
 * }
 * }</pre>
 *
 * <p>Think of the preamble as the stage and the shader file as the act: the stage (lights, floor, curtains) is
 * always built the same way, and each act only describes what happens on it.
 *
 * <p>The preamble is a constant rather than a file, so it is part of the program on every platform and nothing
 * has to read it from a classpath at run time (a browser has none).
 */
public final class FlixelWgsl {

  /**
   * The WGSL text the framework puts in front of every shader file.
   *
   * <p>It declares the vertex structs, bind group 0 and the default vertex entry point. The shader plugin's build
   * copies this text out of this file to validate shaders against the same contract, so it must stay a plain text
   * block: no backslash escapes, and the closing quotes aligned with the text.
   */
  public static final String PREAMBLE = """
      // FlixelGDX WGSL preamble.
      //
      // The framework puts this text in front of every shader before it is compiled, so shader files never
      // declare these names themselves. It is the single source of truth for the shader contract: the renderer
      // uses this text, and the shader plugin's build copies it out of FlixelWgsl.java.
      //
      // Bind group 0 belongs to the framework and is bound for every draw:
      //   binding 0: FlixelGlobals uniform (matrix and texture size)
      //   binding 1: the texture being drawn
      //   binding 2: the sampler (nearest or linear, following the texture's smooth setting)
      //
      // Bind group 1 belongs to the shader. Declare one uniform struct at @group(1) @binding(0) and set its
      // members by name from Java with FlixelShaderProgram.setUniform(...).
      //
      // A shader file must define a fragment entry point named fs_main. It may also define a vertex entry point
      // named vs_main; when it does not, the default flixel_vs below is used.

      struct FlixelGlobals {
        // Maps world pixels to clip space (view-projection times the batch transform). Column-major.
        proj: mat4x4f,
        // Size in pixels of the texture bound at binding 1. A compressed texture is rounded up to whole 4x4 blocks, and
        // texture coordinates are scaled to match, so this is always the size the coordinates are measured against.
        texSize: vec2f,
        // One over texSize, so shaders can step by exactly one texel.
        texelSize: vec2f,
      }

      @group(0) @binding(0) var<uniform> flixel: FlixelGlobals;
      @group(0) @binding(1) var flixelTexture: texture_2d<f32>;
      @group(0) @binding(2) var flixelSampler: sampler;

      // One vertex of a batched quad: world position in pixels, texture coordinate, and the tint color.
      struct FlixelVertexInput {
        @location(0) position: vec2f,
        @location(1) uv: vec2f,
        @location(2) color: vec4f,
      }

      // What the vertex stage hands to the fragment stage. Custom vertex shaders must return this struct.
      struct FlixelVertexOutput {
        @builtin(position) position: vec4f,
        @location(0) uv: vec2f,
        @location(1) color: vec4f,
      }

      @vertex
      fn flixel_vs(in: FlixelVertexInput) -> FlixelVertexOutput {
        var out: FlixelVertexOutput;
        out.position = flixel.proj * vec4f(in.position, 0.0, 1.0);
        out.uv = in.uv;
        out.color = in.color;
        return out;
      }
      """;

  /** The name of the fragment entry point every shader file must define. */
  public static final String FRAGMENT_ENTRY = "fs_main";

  /** The name a shader file gives its optional custom vertex entry point. */
  public static final String VERTEX_ENTRY = "vs_main";

  /** The vertex entry point the preamble provides, used when a shader file defines no {@code vs_main}. */
  public static final String DEFAULT_VERTEX_ENTRY = "flixel_vs";

  /** The bind group the framework owns (matrix, texture and sampler). */
  public static final int FRAMEWORK_GROUP = 0;

  /** The bind group a shader's own uniform struct lives in. */
  public static final int SHADER_GROUP = 1;

  /** The binding index of the shader's uniform struct inside {@link #SHADER_GROUP}. */
  public static final int SHADER_BINDING = 0;

  /** The byte size of the {@code FlixelGlobals} uniform in bind group 0. */
  public static final int GLOBALS_SIZE = 80;

  private FlixelWgsl() {}

  /**
   * Returns the full shader module text for a user's shader file: the preamble followed by the file.
   *
   * @param userSource The text of the {@code .wgsl} file.
   * @return The source to compile as one WGSL module.
   */
  @NotNull
  public static String assemble(@NotNull String userSource) {
    return PREAMBLE + '\n' + userSource;
  }

  /**
   * Returns the name of the vertex entry point to use for a shader file.
   *
   * @param userSource The text of the {@code .wgsl} file.
   * @return {@value #VERTEX_ENTRY} when the file defines it, otherwise {@value #DEFAULT_VERTEX_ENTRY}.
   */
  @NotNull
  public static String vertexEntryOf(@NotNull String userSource) {
    return definesFunction(userSource, VERTEX_ENTRY) ? VERTEX_ENTRY : DEFAULT_VERTEX_ENTRY;
  }

  /**
   * Returns whether a shader file defines a function with the given name.
   *
   * <p>Comments are ignored, so a commented-out {@code fn vs_main} does not count.
   *
   * @param source The WGSL text to scan.
   * @param name The function name to look for.
   * @return {@code true} when {@code fn <name>} appears in code.
   */
  public static boolean definesFunction(@NotNull String source, @NotNull String name) {
    FlixelWgslTokens tokens = FlixelWgslTokens.of(source);
    for (int i = 0; i + 1 < tokens.size(); i++) {
      if (tokens.is(i, "fn") && tokens.is(i + 1, name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the preamble text that {@link #assemble(String)} puts in front of every shader file.
   *
   * @return The same text as {@link #PREAMBLE}.
   */
  @NotNull
  public static String getPreamble() {
    return PREAMBLE;
  }
}
