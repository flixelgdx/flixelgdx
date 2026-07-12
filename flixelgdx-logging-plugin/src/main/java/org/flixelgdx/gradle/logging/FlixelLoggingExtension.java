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
package org.flixelgdx.gradle.logging;

import org.gradle.api.provider.Property;

/**
 * Gradle DSL extension for {@code flixelLogging} when using {@link FlixelLoggingPlugin}.
 *
 * <p>Example usage in a game module's {@code build.gradle.kts}:
 *
 * <pre>{@code
 * flixelLogging {
 *   enabled = true
 *   verbose = false
 *   weaveDependencies = true
 * }
 * }</pre>
 */
public interface FlixelLoggingExtension {

  /**
   * Whether the bytecode weaver is active. When set to {@code false} the plugin skips all
   * post-compile transformations and no dependency JAR weaving is registered.
   *
   * <p>Defaults to {@code true}.
   */
  Property<Boolean> getEnabled();

  /**
   * When {@code true}, each rewritten class path is logged at Gradle lifecycle level so you can
   * verify the weaver is touching the expected files.
   *
   * <p>Defaults to {@code false}.
   */
  Property<Boolean> getVerbose();

  /**
   * When {@code true}, the plugin registers a Gradle artifact transform that runs the bytecode
   * weaver over every JAR on {@code runtimeClasspath}. This lets call sites inside third-party
   * libraries (libGDX backends, controller extensions, etc.) carry accurate file and line metadata
   * when {@code FlixelLogger} is the active application logger.
   *
   * <p>Disable this if you have a dependency that is sensitive to bytecode modification, or if
   * you want to limit weaving strictly to your own compiled sources.
   *
   * <p>Defaults to {@code true}.
   */
  Property<Boolean> getWeaveDependencies();
}
