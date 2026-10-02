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

import org.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The contract of a FlixelGDX logger: it turns a message into a log line and hands it to its sinks.
 *
 * <p>Think of a logger as the front desk of a newsroom. You hand it a story (the message), and it
 * stamps the story with a section label (the tag), throws away the stories that are not important
 * enough today (the level), and sends copies to the printer, the archive, and the wall display (the
 * sinks). {@link FlixelDefaultLogger} is the ready-made front desk that {@link Flixel#log} uses.
 *
 * <p>Most games never implement this interface. They call the {@link Flixel#info(Object)} shortcuts or
 * keep a tagged logger in a static field, and they customize output by adding a
 * {@link FlixelLogSink}.
 *
 * <p>Example:
 * <pre>{@code
 * private static final FlixelLogger LOG = Flixel.log.tagged("PlayState");
 *
 * LOG.info("Player spawned.");
 * LOG.warn("Pool exhausted, {} objects dropped", dropped);
 * LOG.debug("Spawned {} enemies at ({}, {})", count, x, y);
 * LOG.error("Failed to load slot {}", slot, exception);
 *
 * Flixel.log.setTag("MyGame");
 * Flixel.log.setLevel(FlixelLogLevel.WARN);
 * Flixel.log.addExtraSink(entry -> history.add(entry.getMessage().toString()));
 * }</pre>
 *
 * <h2>Message format</h2>
 * <p>Every {@code {}} in the message is replaced by the next argument, in order. A {@code {}} that has
 * no argument left stays as the literal text {@code {}}. If the last argument is a {@link Throwable}
 * and no {@code {}} used it, it becomes the exception of the log entry, so its full stack trace is
 * printed. Any other argument that no {@code {}} used is appended after the message, each preceded by
 * a single space. A {@code null} message or argument prints as {@code null}.
 *
 * <h2>Tags</h2>
 * <p>There are no tag parameters on the logging methods. A logger carries its own tag: set it with
 * {@link #setTag(String)}, or make a child logger that carries a different tag with
 * {@link #tagged(String)}.
 *
 * <h2>Threading</h2>
 * <p>Loggers are safe to use from any thread.
 *
 * @see FlixelDefaultLogger
 * @see FlixelLogSink
 */
public interface FlixelLogger {

  /**
   * Returns whether a message at {@code level} would be logged right now.
   *
   * <p>Use this to skip expensive work, such as building a large debug string, when the message would
   * be thrown away anyway.
   *
   * @param level The level to test.
   * @return {@code true} if the level is at least as severe as {@link #getLevel()}.
   */
  boolean isEnabled(@NotNull FlixelLogLevel level);

  /**
   * The allocation-free primitive that every other logging method calls.
   *
   * <p>When {@code varargs} is {@code null}, the arguments are in {@code a1} to {@code a3} and
   * {@code argc} (0 to 3) says how many of them are used. When the call comes from a varargs method,
   * pass the array length as {@code argc} and the array as {@code varargs}, and leave {@code a1} to
   * {@code a3} unused. A {@code null} {@code varargs} with an {@code argc} above 3 counts as no
   * arguments.
   *
   * <p>Game code should call {@code debug}, {@code info}, {@code warn}, or {@code error} instead.
   *
   * @param level The level of the message.
   * @param message The message, or the format string with {@code {}} placeholders.
   * @param argc How many arguments there are.
   * @param a1 The first argument when {@code argc} is 1 to 3.
   * @param a2 The second argument when {@code argc} is 2 or 3.
   * @param a3 The third argument when {@code argc} is 3.
   * @param varargs The arguments when {@code argc} is above 3, otherwise {@code null}.
   */
  void log(
      @NotNull FlixelLogLevel level,
      @Nullable Object message,
      int argc,
      @Nullable Object a1,
      @Nullable Object a2,
      @Nullable Object a3,
      @Nullable Object[] varargs);

  /**
   * Returns a logger that prints the given tag on every message, while sharing this logger's level,
   * mode, sinks, call site resolver, and file settings.
   *
   * <p>Keep the result in a {@code private static final} field instead of calling this repeatedly.
   *
   * <p>Example:
   * <pre>{@code
   * private static final FlixelLogger LOG = Flixel.log.tagged("Pathfinding");
   * }</pre>
   *
   * @param tag The tag to print, or {@code null} for none.
   * @return A logger with its own tag. It keeps pointing at the logger that created it, even if
   *     {@link Flixel#log} is replaced later.
   */
  @NotNull
  FlixelLogger tagged(@Nullable String tag);

  /**
   * Returns the console sink that receives every message.
   *
   * @return The sink, or {@code null} if console output is disabled.
   */
  @Nullable
  FlixelLogSink getConsoleSink();

  /**
   * Sets the console sink that receives every message. Platform launchers install a sink that suits
   * their console; the default prints plain text to standard output.
   *
   * @param sink The new sink, or {@code null} to turn console output off.
   */
  void setConsoleSink(@Nullable FlixelLogSink sink);

  /**
   * Returns the file sink.
   *
   * @return The sink, or {@code null} if the platform has no file logging (for example, on the web).
   */
  @Nullable
  FlixelLogFileSink getFileSink();

  /**
   * Sets the file sink. Set this before {@link #startFileLogging()} is called.
   *
   * @param sink The new sink, or {@code null} to disable file logging.
   */
  void setFileSink(@Nullable FlixelLogFileSink sink);

  /**
   * Adds a sink that receives every message in addition to the console and file sinks. The in-game
   * debug overlay is one of these. {@code null} is ignored.
   *
   * @param sink The sink to add.
   */
  void addExtraSink(@Nullable FlixelLogSink sink);

  /**
   * Removes a sink that was added with {@link #addExtraSink(FlixelLogSink)}.
   *
   * @param sink The sink to remove.
   */
  void removeExtraSink(@Nullable FlixelLogSink sink);

  /**
   * Returns the resolver that finds the file and line of each log call.
   *
   * @return The resolver, or {@code null} if call sites are not resolved.
   */
  @Nullable
  FlixelLogSiteResolver getSiteResolver();

  /**
   * Sets the resolver that finds the file and line of each log call. Platform launchers install the
   * one that works on their platform.
   *
   * @param resolver The new resolver, or {@code null} to report every site as unknown.
   */
  void setSiteResolver(@Nullable FlixelLogSiteResolver resolver);

  /**
   * Starts file logging: opens the {@link #getFileSink() file sink} in the {@link #getLogsFolder() logs
   * folder}. Does nothing when there is no file sink or {@link #canStoreLogs()} is {@code false}.
   */
  void startFileLogging();

  /**
   * Stops file logging and closes the file sink, so buffered lines are written out. Call this when the
   * game shuts down.
   */
  void stopFileLogging();

  /**
   * Returns the minimum level that is logged.
   *
   * @return The level, never {@code null}. Defaults to {@link FlixelLogLevel#DEBUG}, which logs
   *   everything.
   */
  @NotNull
  FlixelLogLevel getLevel();

  /**
   * Sets the minimum level that is logged. Messages less severe than this are thrown away before any
   * work is done for them.
   *
   * @param level The new minimum level. Passing {@code null} resets it to {@link FlixelLogLevel#DEBUG}.
   */
  void setLevel(@NotNull FlixelLogLevel level);

  /**
   * Returns the console format.
   *
   * @return The mode, never {@code null}.
   */
  @NotNull
  FlixelLogMode getMode();

  /**
   * Sets the console format.
   *
   * @param mode The new mode. {@code null} resets it to {@link FlixelLogMode#SIMPLE}.
   */
  void setMode(@NotNull FlixelLogMode mode);

  /**
   * Returns the tag that this logger prints on its messages.
   *
   * @return The tag, never {@code null} but possibly empty.
   */
  @NotNull
  String getTag();

  /**
   * Sets the tag that this logger prints on its messages. The {@link Flixel#info(Object)} family uses
   * the tag of {@link Flixel#log}.
   *
   * @param tag The new tag. {@code null} resets it to an empty tag.
   */
  void setTag(@NotNull String tag);

  /**
   * Returns the custom folder for log files.
   *
   * @return The absolute path to the logs folder, or {@code null} to use the platform default.
   */
  @Nullable
  String getLogsFolder();

  /**
   * Sets a custom folder where log files are stored. Pass an absolute path, for example
   * {@code /path/to/game/logs}. A trailing slash is removed.
   *
   * @param absolutePathToLogsFolder The absolute path, or {@code null} or an empty string to use the
   *   platform default.
   */
  void setLogsFolder(@Nullable String absolutePathToLogsFolder);

  /**
   * Returns the maximum number of log files kept before older ones are deleted.
   *
   * @return The maximum number of log files to retain on disk.
   */
  int getMaxLogFiles();

  /**
   * Sets the maximum number of log files kept before older ones are deleted. Takes effect the next
   * time {@link #startFileLogging()} runs.
   *
   * @param maxLogFiles The maximum number of log files to retain.
   */
  void setMaxLogFiles(int maxLogFiles);

  /**
   * Returns whether the logger may write log files to disk.
   *
   * @return {@code true} if file logging is allowed, {@code false} otherwise.
   */
  boolean canStoreLogs();

  /**
   * Sets whether the logger may write log files to disk. Takes effect the next time
   * {@link #startFileLogging()} runs.
   *
   * @param canStoreLogs {@code true} to allow file logging, {@code false} to disable it.
   */
  void setCanStoreLogs(boolean canStoreLogs);

  /**
   * Logs a message at the {@link FlixelLogLevel#DEBUG} level.
   *
   * @param message The message to log.
   */
  default void debug(@Nullable Object message) {
    log(FlixelLogLevel.DEBUG, message, 0, null, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#DEBUG} level with one argument.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   */
  default void debug(@Nullable String format, @Nullable Object a1) {
    log(FlixelLogLevel.DEBUG, format, 1, a1, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#DEBUG} level with two arguments.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   */
  default void debug(@Nullable String format, @Nullable Object a1, @Nullable Object a2) {
    log(FlixelLogLevel.DEBUG, format, 2, a1, a2, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#DEBUG} level with three arguments.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   * @param a3 The third argument.
   */
  default void debug(@Nullable String format, @Nullable Object a1, @Nullable Object a2, @Nullable Object a3) {
    log(FlixelLogLevel.DEBUG, format, 3, a1, a2, a3, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#DEBUG} level with any number of arguments. Prefer the
   * fixed-argument overloads when you have three or fewer arguments, because they do not create an array.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param args The arguments.
   */
  default void debug(@Nullable String format, @Nullable Object... args) {
    log(FlixelLogLevel.DEBUG, format, args != null ? args.length : 0, null, null, null, args);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#INFO} level. Use it for general information about the game.
   *
   * @param message The message to log.
   */
  default void info(@Nullable Object message) {
    log(FlixelLogLevel.INFO, message, 0, null, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#INFO} level with one argument.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   */
  default void info(@Nullable String format, @Nullable Object a1) {
    log(FlixelLogLevel.INFO, format, 1, a1, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#INFO} level with two arguments.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   */
  default void info(@Nullable String format, @Nullable Object a1, @Nullable Object a2) {
    log(FlixelLogLevel.INFO, format, 2, a1, a2, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#INFO} level with three arguments.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   * @param a3 The third argument.
   */
  default void info(@Nullable String format, @Nullable Object a1, @Nullable Object a2, @Nullable Object a3) {
    log(FlixelLogLevel.INFO, format, 3, a1, a2, a3, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#INFO} level with any number of arguments. Prefer the
   * fixed-argument overloads when you have three or fewer arguments, because they do not create an array.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param args The arguments.
   */
  default void info(@Nullable String format, @Nullable Object... args) {
    log(FlixelLogLevel.INFO, format, args != null ? args.length : 0, null, null, null, args);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#WARN} level. Use it for problems that are not fatal but
   * should be looked at.
   *
   * @param message The message to log.
   */
  default void warn(@Nullable Object message) {
    log(FlixelLogLevel.WARN, message, 0, null, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#WARN} level with one argument.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   */
  default void warn(@Nullable String format, @Nullable Object a1) {
    log(FlixelLogLevel.WARN, format, 1, a1, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#WARN} level with two arguments.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   */
  default void warn(@Nullable String format, @Nullable Object a1, @Nullable Object a2) {
    log(FlixelLogLevel.WARN, format, 2, a1, a2, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#WARN} level with three arguments.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   * @param a3 The third argument.
   */
  default void warn(@Nullable String format, @Nullable Object a1, @Nullable Object a2, @Nullable Object a3) {
    log(FlixelLogLevel.WARN, format, 3, a1, a2, a3, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#WARN} level with any number of arguments. Prefer the
   * fixed-argument overloads when you have three or fewer arguments, because they do not create an array.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param args The arguments.
   */
  default void warn(@Nullable String format, @Nullable Object... args) {
    log(FlixelLogLevel.WARN, format, args != null ? args.length : 0, null, null, null, args);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#ERROR} level. Use it for something that is wrong and
   * needs attention. To attach an exception, pass it as the last argument of one of the other overloads,
   * for example {@code error("Save failed", exception)}.
   *
   * @param message The message to log.
   */
  default void error(@Nullable Object message) {
    log(FlixelLogLevel.ERROR, message, 0, null, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#ERROR} level with one argument. A {@link Throwable}
   * that no {@code {}} uses becomes the exception of the entry.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   */
  default void error(@Nullable String format, @Nullable Object a1) {
    log(FlixelLogLevel.ERROR, format, 1, a1, null, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#ERROR} level with two arguments. A {@link Throwable}
   * that no {@code {}} uses becomes the exception of the entry when it is last.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   */
  default void error(@Nullable String format, @Nullable Object a1, @Nullable Object a2) {
    log(FlixelLogLevel.ERROR, format, 2, a1, a2, null, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#ERROR} level with three arguments. A {@link Throwable}
   * that no {@code {}} uses becomes the exception of the entry when it is last.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param a1 The first argument.
   * @param a2 The second argument.
   * @param a3 The third argument.
   */
  default void error(@Nullable String format, @Nullable Object a1, @Nullable Object a2, @Nullable Object a3) {
    log(FlixelLogLevel.ERROR, format, 3, a1, a2, a3, null);
  }

  /**
   * Logs a message at the {@link FlixelLogLevel#ERROR} level with any number of arguments. Prefer the
   * fixed-argument overloads when you have three or fewer arguments, because they do not create an array.
   *
   * @param format The message, where each {@code {}} is replaced by the next argument.
   * @param args The arguments.
   */
  default void error(@Nullable String format, @Nullable Object... args) {
    log(FlixelLogLevel.ERROR, format, args != null ? args.length : 0, null, null, null, args);
  }
}
