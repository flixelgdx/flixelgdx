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

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;

/**
 * Configuration exposed as the {@code flixelShaders} DSL block in a game module's
 * {@code build.gradle}.
 *
 * <p>Declare one entry per shader effect. Each entry names a fragment source (and optionally a
 * vertex source) written in plain GLSL; the plugin cross-compiles them into every backend variant
 * at build time and bundles the results into the module's resources, so the game ships a single
 * shader that runs on OpenGL, Vulkan, Metal, and Direct3D without any per-platform authoring.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * flixelShaders {
 *   // Where the .glsl sources live (default: src/main/shaders).
 *   sourceDir = file('src/main/shaders')
 *
 *   shader('crt') {
 *     fragment = 'crt.frag.glsl'
 *   }
 * }
 * }</pre>
 *
 * @see FlixelShaderSpec
 */
public interface FlixelShaderExtension {

  /** Gradle extension name the DSL block is registered under. */
  String NAME = "flixelShaders";

  /** Default directory, relative to the module, that shader sources are read from. */
  String DEFAULT_SOURCE_DIR = "src/main/shaders";

  /**
   * The directory shader source paths are resolved against.
   *
   * <p>Defaults to {@value #DEFAULT_SOURCE_DIR} relative to the module. Absolute source paths on a
   * {@link FlixelShaderSpec} bypass this directory.
   *
   * @return The source directory property.
   */
  DirectoryProperty getSourceDir();

  /**
   * An optional explicit path to a bgfx {@code shaderc} executable.
   *
   * <p>When unset, the plugin uses the {@code shaderc} bundled for the current operating system,
   * falling back to one found on the system {@code PATH}. Set this to point at a locally built
   * {@code shaderc}, for example when producing Direct3D variants on Windows.
   *
   * @return The compiler path property.
   */
  RegularFileProperty getShadercPath();

  /**
   * The set of shaders to compile.
   *
   * @return The container of shader specs.
   */
  NamedDomainObjectContainer<FlixelShaderSpec> getShaders();

  /**
   * Declares a shader to compile.
   *
   * <p>Convenience for {@code getShaders().create(name, action)} so the DSL reads as
   * {@code shader('name') { ... }}.
   *
   * @param name The shader's identifier, used to load it at runtime.
   * @param action Configures the shader's sources.
   */
  default void shader(String name, Action<? super FlixelShaderSpec> action) {
    getShaders().create(name, action);
  }
}
