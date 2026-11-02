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
 * sampler. The framework puts a <em>preamble</em> (the file {@code src/main/wgsl/flixel_preamble.wgsl} of this
 * module, the single source of truth) in front of it, so every shader sees the same names:
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
 * <p>The preamble is not read from the classpath at run time, since not every platform has one (a browser does
 * not). The build turns the file into the constant {@link FlixelBuiltinWgsl#PREAMBLE}, so it is part of the
 * program on every platform.
 */
public final class FlixelWgsl {

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
    return FlixelBuiltinWgsl.PREAMBLE + '\n' + userSource;
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
   * @return The contents of {@code flixel_preamble.wgsl}.
   */
  @NotNull
  public static String getPreamble() {
    return FlixelBuiltinWgsl.PREAMBLE;
  }
}
