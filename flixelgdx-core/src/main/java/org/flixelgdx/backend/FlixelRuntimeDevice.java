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
package org.flixelgdx.backend;

import org.flixelgdx.Flixel;
import org.flixelgdx.logging.FlixelNoopStackTraceProvider;
import org.flixelgdx.logging.FlixelStackTraceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The current machine and program layout, as seen by FlixelGDX: memory usage, where the game is
 * being loaded from, and where log files should go.
 *
 * <p>All of this is inherently platform-specific. A desktop JVM can inspect its heap and classpath;
 * a web browser cannot. So, like the other backend seams ({@link FlixelWindow},
 * {@link FlixelHostIntegration}), this is an interface the active backend fills in. Read it through
 * {@link org.flixelgdx.Flixel#runtime Flixel.runtime}.
 *
 * <p>Every method has a safe default, so a backend only overrides what it can actually report, and
 * the no-op device ({@link FlixelNoopRuntimeDevice}) keeps calls safe before a backend is installed
 * and on platforms that cannot answer. Desktop JVM builds install {@code FlixelJvmRuntimeDevice}
 * from {@code flixelgdx-jvm} at startup.
 *
 * <p>Example:
 *
 * <pre>{@code
 * if (Flixel.runtime.getEnvironment() == FlixelRunEnvironment.JAR) {
 *   // Loading from a packaged distribution.
 * }
 * long heapUsed = Flixel.runtime.getJavaHeapBytes();
 * }</pre>
 *
 * @see FlixelNoopRuntimeDevice
 * @see FlixelRunEnvironment
 */
public interface FlixelRuntimeDevice {

  /**
   * Returns the bytes of managed (Java) heap currently in use, or {@code 0} when the platform
   * cannot report it.
   *
   * @return Used managed heap in bytes.
   */
  default long getJavaHeap() {
    return 0L;
  }

  /**
   * Returns the bytes of native (off-heap) memory currently in use, or {@code 0} when the platform
   * cannot report it.
   *
   * @return Used native memory in bytes.
   */
  default long getNativeHeap() {
    return 0L;
  }

  /**
   * Returns {@code true} when the game is running from a packaged distribution JAR. Defaults to
   * {@code false}.
   */
  default boolean isRunningFromJar() {
    return false;
  }

  /**
   * Returns {@code true} when the game is running inside an IDE (IntelliJ, Eclipse, and similar).
   * Defaults to {@code false}.
   */
  default boolean isRunningInIDE() {
    return false;
  }

  /**
   * Returns the working directory of the game (its code source location: class output directory or
   * JAR path).
   *
   * @return The working directory, or {@code null} when it cannot be determined.
   */
  @Nullable
  default String getWorkingDirectory() {
    return null;
  }

  /**
   * Returns the default directory where log files should be written for the current layout.
   *
   * @return The absolute logs directory path (no trailing separator), or {@code null} when it
   *     cannot be determined.
   */
  @Nullable
  default String getDefaultLogsFolderPath() {
    return null;
  }

  /**
   * Classifies how the game's code is being loaded on this platform.
   *
   * @return The detected environment; {@link FlixelRunEnvironment#UNKNOWN} when the platform cannot
   *     classify its layout.
   */
  default FlixelRunEnvironment getEnvironment() {
    return FlixelRunEnvironment.UNKNOWN;
  }

  /**
   * Returns the current runtime mode. Defaults to {@link FlixelRuntimeMode#RELEASE} when the
   * backend has not set one.
   *
   * @return The active runtime mode, never {@code null}.
   */
  @NotNull
  default FlixelRuntimeMode getMode() {
    return FlixelRuntimeMode.RELEASE;
  }

  /**
   * Sets the runtime mode. Called once by the platform launcher before {@link Flixel#start}.
   *
   * @param mode The runtime mode to apply.
   */
  default void setMode(@NotNull FlixelRuntimeMode mode) {}

  /**
   * Returns the stack trace provider used by the logger to annotate log messages with their call
   * site. Defaults to {@link FlixelNoopStackTraceProvider#INSTANCE} when the backend has not
   * supplied one.
   *
   * @return The active stack trace provider, never {@code null}.
   */
  @NotNull
  default FlixelStackTraceProvider getStackTraceProvider() {
    return FlixelNoopStackTraceProvider.INSTANCE;
  }

  /**
   * Sets the platform-specific stack trace provider. Called by the platform launcher before
   * {@link org.flixelgdx.Flixel#start} so the logger resolves call-site information correctly.
   *
   * @param provider The provider to install.
   */
  default void setStackTraceProvider(@NotNull FlixelStackTraceProvider provider) {}

  /**
   * Installs the supplied handler as the platform's unhandled-exception sink.
   *
   * <p>Each backend wires {@code handler} into whichever crash-detection mechanism its runtime
   * provides. On JVM desktop targets that is {@link Thread#setDefaultUncaughtExceptionHandler}; on
   * HTML5 targets via TeaVM a backend would additionally hook into {@code window.onerror} to catch
   * JavaScript-level exceptions that bypass the Java exception system.
   *
   * <p>The default implementation is a no-op, so platforms that cannot intercept crashes degrade
   * gracefully without errors.
   *
   * <p>This is called once by {@link org.flixelgdx.FlixelGame} during {@code create()}, before the
   * initial state is loaded.
   *
   * @param handler The crash handler to install.
   */
  default void setCrashHandler(@NotNull FlixelCrashHandler handler) {}
}
