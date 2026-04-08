/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Platform-specific handler for writing log output to a persistent file.
 *
 * <p>Implementations are responsible for the entire file-logging lifecycle: creating
 * the log file, pruning old files when the maximum is exceeded, writing individual
 * log lines (potentially on a background thread to avoid blocking the game loop),
 * and shutting down cleanly on game exit so that all buffered output is flushed.
 *
 * <p>On platforms where file logging is not feasible (for example, web/TeaVM), no
 * handler needs to be registered and the logger will simply skip file output.
 *
 * <p>Register an implementation before {@link me.stringdotjar.flixelgdx.Flixel#initialize}
 * by calling {@link me.stringdotjar.flixelgdx.Flixel#setLogFileHandler}. The handler
 * follows the same injection pattern used by
 * {@link me.stringdotjar.flixelgdx.backend.alert.FlixelAlerter} and
 * {@link FlixelStackTraceProvider}.
 *
 * @see FlixelLogger
 * @see me.stringdotjar.flixelgdx.Flixel#setLogFileHandler(FlixelLogFileHandler)
 */
public interface FlixelLogFileHandler {

  /**
   * Starts file logging in the specified folder, keeping at most {@code maxLogFiles}
   * log files. Older files beyond the limit are deleted before the new file is
   * created.
   *
   * <p>If {@code logsFolderPath} is {@code null}, the implementation should fall
   * back to a platform-appropriate default (for example, next to the running JAR
   * or in the project root during development).
   *
   * <p>Implementations that perform file writes on a background thread should
   * start that thread here.
   *
   * @param logsFolderPath The absolute path to the directory where log files are
   *   stored, or {@code null} to use the platform default.
   * @param maxLogFiles The maximum number of log files to retain. When the
   *   folder already contains this many files, the oldest are deleted before a
   *   new file is created.
   */
  void start(@Nullable String logsFolderPath, int maxLogFiles);

  /**
   * Shuts down the file handler, flushing any buffered log lines and releasing
   * resources such as background threads and file handles.
   *
   * <p>This method should block briefly (for example, up to five seconds) to
   * allow the write queue to drain so that logs written during shutdown are
   * persisted. After this method returns, subsequent calls to
   * {@link #write(String)} are silently ignored.
   */
  void stop();

  /**
   * Writes a single pre-formatted log line to the current log file.
   *
   * <p>Implementations should enqueue the line for asynchronous writing when a
   * background thread is in use, so that the calling game thread is not blocked
   * by disk I/O. If the handler has not been started or has already been stopped,
   * the call is silently ignored.
   *
   * @param logLine The fully formatted, plain-text log line to write. A trailing
   *   newline is appended by the handler if necessary.
   */
  void write(@NotNull String logLine);

  /**
   * Returns whether the handler is currently active and accepting log lines.
   *
   * <p>A handler is active between a successful {@link #start} call and the
   * completion of {@link #stop}.
   *
   * @return {@code true} if file logging is active, {@code false} otherwise.
   */
  boolean isActive();

  /**
   * Returns the platform-appropriate default directory for log files, or
   * {@code null} if the platform does not support file logging.
   *
   * <p>On JVM platforms this typically resolves to a {@code logs/} folder next
   * to the running JAR or in the project root when running from an IDE.
   *
   * @return An absolute path to the default logs directory, or {@code null}
   *   when file logging is not supported on this platform.
   */
  @Nullable
  String getDefaultLogsFolderPath();
}
