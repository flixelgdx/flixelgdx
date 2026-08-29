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

import org.flixelgdx.FlixelGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Callback invoked by a platform's {@link FlixelRuntimeDevice} when an unhandled exception is
 * detected.
 *
 * <p>Each backend installs this handler using whatever mechanism its runtime provides. On JVM
 * desktop targets that is {@link Thread#setDefaultUncaughtExceptionHandler}; on HTML5 targets via
 * TeaVM a backend would additionally wire into {@code window.onerror} to catch JavaScript-level
 * exceptions that bypass the Java exception system.
 *
 * <p>The handler itself is supplied by {@link FlixelGame} during {@code create()}.
 * It performs the standard framework crash response: logging the exception, showing an error alert,
 * tearing down game resources, and exiting the process on platforms where that is allowed.
 *
 * <p>{@code thread} may be {@code null} on platforms that do not surface a thread reference at
 * crash time, such as HTML5 backends receiving a {@code window.onerror} event.
 *
 * @see FlixelRuntimeDevice#setCrashHandler(FlixelCrashHandler)
 */
@FunctionalInterface
public interface FlixelCrashHandler {

  /**
   * Called when an unhandled exception is detected.
   *
   * @param thread The thread the exception originated from, or {@code null} when the platform does
   *     not provide one.
   * @param throwable The unhandled exception.
   */
  void onCrash(@Nullable Thread thread, @NotNull Throwable throwable);
}
