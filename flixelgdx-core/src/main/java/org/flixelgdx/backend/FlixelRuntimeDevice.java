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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;

/**
 * The current machine and program layout, as seen by FlixelGDX: memory usage, where the game is
 * being loaded from, and where log files should go.
 *
 * <p>All of this is inherently platform-specific. A desktop JVM can inspect its heap and classpath;
 * a web browser cannot. So, like the other backend seams ({@link FlixelWindow},
 * {@link FlixelHostIntegration}), this is an interface the active backend fills in. Read it through
 * {@link Flixel#runtime}.
 *
 * <p>Every method has a safe default, so a backend only overrides what it can actually report, and
 * the no-op device ({@link FlixelNoopRuntimeDevice}) keeps calls safe before a backend is installed
 * and on platforms that cannot answer. Desktop builds install {@code FlixelJvmRuntimeDevice}
 * at startup.
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
   * Returns how far the local time zone is from UTC at the given moment, in milliseconds.
   *
   * <p>Log timestamps, and anything else that shows a clock time to a person, need to know the
   * player's local time. Computers keep time as a single count of milliseconds since the Unix epoch,
   * which is the same everywhere in the world (UTC). Adding this offset to that count gives the
   * wall clock time on the player's machine. Think of it as the number you add to a world clock to
   * read the clock on your own wall.
   *
   * <p>The value is positive east of UTC and negative west of it. For example, UTC-5 returns
   * {@code -18000000} and UTC+5:30 returns {@code 19800000}.
   *
   * <p>The method takes the moment you are asking about, instead of returning one fixed number,
   * because the offset changes during the year. Daylight saving time moves clocks forward and
   * backward, so the same place can be UTC-5 in winter and UTC-4 in summer.
   *
   * <p>The default asks the Java time zone rules for the system time zone, which is right on
   * desktop and Android. A backend whose Java runtime cannot see the real time zone (such as the
   * browser, where the Java time zone is always UTC) overrides this.
   *
   * <p>Example:
   *
   * <pre>{@code
   * long now = System.currentTimeMillis();
   * long localNow = now + Flixel.runtime.getUtcOffsetMillis(now);
   * }</pre>
   *
   * @param epochMillis The moment to look up, in milliseconds since the Unix epoch.
   * @return The offset of local time from UTC at that moment, in milliseconds.
   */
  default int getUtcOffsetMillis(long epochMillis) {
    return ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochMilli(epochMillis)).getTotalSeconds() * 1000;
  }

  /**
   * Returns {@code true} when the game is running from a packaged distribution JAR. Defaults to
   * {@code false}.
   *
   * @return {@code true} if the game is running from a packaged JAR, {@code false} otherwise.
   */
  default boolean isRunningFromJar() {
    return false;
  }

  /**
   * Returns {@code true} when the game is running inside an IDE (IntelliJ, Eclipse, and similar).
   * Defaults to {@code false}.
   *
   * @return {@code true} if the game is running inside an IDE, {@code false} otherwise.
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
   * <p>This is called once by {@link Flixel#start}, before the runner is executed.
   *
   * @param handler The crash handler to install.
   */
  default void setCrashHandler(@NotNull FlixelCrashHandler handler) {}
}
