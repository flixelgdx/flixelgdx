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

import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelLogSink} that writes log output to a persistent file.
 *
 * <p>Implementations are responsible for the entire file-logging lifecycle: creating the log file,
 * pruning old files when the maximum is exceeded, writing individual log lines (potentially on a
 * background thread to avoid blocking the game loop), and shutting down cleanly on game exit so that
 * all buffered output is flushed.
 *
 * <p>On platforms where file logging is not feasible (for example, the web), no file sink needs to be
 * installed and the logger simply skips file output.
 *
 * <p>Install an implementation with {@link FlixelLogger#setFileSink(FlixelLogFileSink)} from the
 * platform launcher, before the game starts. The logger calls {@link #open(String, int)} when
 * {@link FlixelLogger#startFileLogging()} runs and {@link #close()} when
 * {@link FlixelLogger#stopFileLogging()} runs. It only calls {@link #write(FlixelLogEntry)} while
 * {@link #isOpen()} is {@code true}.
 *
 * <p>Implementations must only delete files that match the {@code flixel-*.log} pattern when pruning
 * old logs. A logs folder may be shared with other files that belong to the player or the game, and
 * those must never be touched.
 *
 * @see FlixelLogger
 */
public interface FlixelLogFileSink extends FlixelLogSink {

  /**
   * Starts file logging in the specified folder, keeping at most {@code maxLogFiles} log files.
   * Older files beyond the limit are deleted before the new file is created.
   *
   * <p>If {@code logsFolderPath} is {@code null}, the implementation should fall back to a
   * platform-appropriate default (for example, next to the running JAR or in the project root during
   * development).
   *
   * <p>Implementations that perform file writes on a background thread should start that thread here.
   *
   * @param logsFolderPath The absolute path to the directory where log files are stored, or {@code null}
   *   to use the platform default.
   * @param maxLogFiles The maximum number of log files to retain. When the folder already contains this
   *   many {@code flixel-*.log} files, the oldest are deleted before a new file is created.
   */
  void open(@Nullable String logsFolderPath, int maxLogFiles);

  /**
   * Shuts down the file sink, flushing any buffered log lines and releasing resources such as
   * background threads and file handles.
   *
   * <p>This method should block briefly (for example, up to five seconds) to allow the write queue to
   * drain so that logs written during shutdown are persisted. After this method returns, subsequent
   * calls to {@link #write(FlixelLogEntry)} are silently ignored.
   */
  void close();

  /**
   * Returns whether the sink is currently active and accepting log lines.
   *
   * <p>A sink is open between a successful {@link #open(String, int)} call and the completion of
   * {@link #close()}.
   *
   * @return {@code true} if file logging is active, {@code false} otherwise.
   */
  boolean isOpen();

  /**
   * Returns the platform-appropriate default directory for log files, or {@code null} if the platform
   * does not support file logging.
   *
   * <p>On JVM platforms this typically resolves to a {@code logs/} folder next to the running JAR or in
   * the project root when running from an IDE.
   *
   * @return An absolute path to the default logs directory, or {@code null} when file logging is not
   *   supported on this platform.
   */
  @Nullable
  String getDefaultLogsFolderPath();
}
