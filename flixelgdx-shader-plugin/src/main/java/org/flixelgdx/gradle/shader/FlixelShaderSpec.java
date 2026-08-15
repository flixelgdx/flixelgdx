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
package org.flixelgdx.gradle.shader;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;

/**
 * One named shader to cross-compile, configured inside the {@code flixelShaders} block.
 *
 * <p>The name is the identifier a game loads the compiled shader by at runtime (for example
 * {@code FlixelShader.load("crt")}). Only a fragment source is required; the vertex stage falls
 * back to the framework's built-in pass-through vertex shader, which is correct for almost every
 * sprite and camera effect.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * flixelShaders {
 *   shader('crt') {
 *     fragment = 'crt.frag.glsl'
 *   }
 *   shader('wave') {
 *     fragment = 'wave.frag.glsl'
 *     vertex = 'wave.vert.glsl'   // optional
 *   }
 * }
 * }</pre>
 */
public interface FlixelShaderSpec extends Named {

  /**
   * The fragment shader source file, given as a path relative to the configured source directory
   * (or an absolute path).
   *
   * @return The fragment source path property.
   */
  Property<String> getFragment();

  /**
   * The optional vertex shader source file, given as a path relative to the configured source
   * directory (or an absolute path).
   *
   * <p>When unset, the framework's built-in pass-through vertex shader is used.
   *
   * @return The vertex source path property.
   */
  Property<String> getVertex();
}
