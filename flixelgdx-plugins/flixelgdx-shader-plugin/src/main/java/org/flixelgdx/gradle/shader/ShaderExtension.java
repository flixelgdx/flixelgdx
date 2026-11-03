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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration exposed as the {@code shaders} block in a game module's build script.
 *
 * <p>Every setting has a working default, so a game that keeps its shaders in {@value #DEFAULT_SOURCE_DIR} does
 * not need this block at all.
 *
 * <pre>{@code
 * shaders {
 *   // Where the .wgsl files live (default: src/main/shaders).
 *   sourceDir = layout.projectDirectory.dir("assets/shaders")
 * }
 * }</pre>
 */
public interface ShaderExtension {

  /** The name the extension is registered under. */
  String NAME = "shaders";

  /** The directory, relative to the module, that shaders are read from by default. */
  String DEFAULT_SOURCE_DIR = "src/main/shaders";

  /**
   * The directory scanned for {@code .wgsl} files, including its subfolders.
   *
   * <p>A file at {@code <sourceDir>/fx/wave.wgsl} is loaded in the game as {@code FlixelShader.load("fx/wave")}.
   *
   * @return The source directory property, defaulting to {@value #DEFAULT_SOURCE_DIR}.
   */
  DirectoryProperty getSourceDir();

  /**
   * Whether the shaders are compiled with Dawn during the build to catch mistakes early.
   *
   * <p>Switch this off only on a machine where the Dawn library cannot run. The shaders are still copied into the
   * resources, and the game then reports any mistake at run time instead.
   *
   * @return The validation switch, defaulting to {@code true}.
   */
  Property<Boolean> getValidate();

  /**
   * The version of the jwebgpu libraries the validator is run with.
   *
   * <p>It defaults to the version the framework itself is built with, so it rarely needs setting. The libraries
   * come from Maven Central (add {@code mavenCentral()} to the repositories). A build that swaps in a local jwebgpu
   * checkout with a composite build uses that checkout whatever this says.
   *
   * @return The version property.
   */
  Property<String> getJwebgpuVersion();
}
