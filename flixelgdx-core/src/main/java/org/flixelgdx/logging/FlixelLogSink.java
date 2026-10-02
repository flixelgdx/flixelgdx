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
package org.flixelgdx.logging;

import org.jetbrains.annotations.NotNull;

/**
 * Receives every log message that passes a logger's level filter.
 *
 * <p>Think of a sink as a drain at the end of a pipe: the logger collects and formats the water (the
 * message) once, then lets it flow into every drain that is connected. A console, a log file, an
 * in-game overlay, or a crash reporter can each be a sink.
 *
 * <p>Install sinks with {@link FlixelLogger#setConsoleSink(FlixelLogSink)},
 * {@link FlixelLogger#setFileSink(FlixelLogFileSink)}, or
 * {@link FlixelLogger#addExtraSink(FlixelLogSink)}.
 *
 * <p>Example:
 * <pre>{@code
 * Flixel.log.addExtraSink(entry -> {
 *   if (entry.getLevel() == FlixelLogLevel.ERROR) {
 *     crashReporter.record(entry.getMessage().toString());
 *   }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface FlixelLogSink {

  /**
   * Handles one log entry.
   *
   * <p>The entry is reused for every message, and the logger holds a lock while this method runs.
   * Read what you need and return quickly. Copy anything you want to keep (for example, by calling
   * {@code entry.getMessage().toString()}), because the entry changes as soon as this method returns.
   * Logging from inside a sink is allowed, but that nested message is written as a plain line to
   * standard error instead of going through the normal sinks.
   *
   * @param entry The message to handle. Valid only for the duration of this call.
   */
  void write(@NotNull FlixelLogEntry entry);
}
