/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.util.FlixelAsciiCodes;
import me.stringdotjar.flixelgdx.util.FlixelString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.Array;

/**
 * Logger instance for Flixel that formats and outputs log messages to the console and optionally
 * to a file. Console output respects the current {@link FlixelLogMode}; file output always uses
 * a detailed format.
 *
 * <p>File logging is controlled per instance: use {@link #setLogsFolder(String)} to set a custom
 * logs folder (when running in an IDE the default is the project root; when running from a JAR
 * it is the directory containing the JAR), {@link #setCanStoreLogs(boolean)} and
 * {@link #setMaxLogFiles(int)} to configure file logging, then {@link #startFileLogging()} to
 * start and {@link #stopFileLogging()} to shut down the log writer thread.
 *
 * <p>Console and file line assembly run when you log, not every frame. Shared formatters and buffers reduce
 * allocation churn versus building many small strings per line.
 */
public class FlixelLogger {

  private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Maximum number of lines the in-game debug console keeps. The overlay trims older lines when this is exceeded.
   */
  public static final int MAX_LOG_ENTRIES = 200;

  /**
   * Whether to write logs to a file when {@link #startFileLogging()} is called.
   *
   * <p>Once {@link #startFileLogging()} is called, setting this will have no effect.
   * You must call {@link #stopFileLogging()} before changing this again.
   */
  private boolean canStoreLogs = true;

  /** Maximum number of log files to keep when file logging is enabled. */
  private int maxLogFiles = 10;

  /** Default tag to use when logging without a specific tag. */
  private String defaultTag = "";

  /** Log mode for console output. File output always uses {@link FlixelLogMode#DETAILED}. */
  private FlixelLogMode logMode;

  /** Provider for collecting stack trace information for the logger. */
  private FlixelStackTraceProvider stackTraceProvider;

  /** Custom logs folder path, or {@code null} to use the platform default. */
  private String customLogsFolderPath = null;

  /** Listeners notified whenever a log message is produced (used by the debug overlay). */
  private final Array<Consumer<FlixelLogEntry>> logListeners = new Array<>(Consumer[]::new);

  /** Registered debug console entries that supply custom lines to the overlay console. */
  private final Array<FlixelDebugConsoleEntry> consoleEntries = new Array<>(FlixelDebugConsoleEntry[]::new);

  /** Reused for ANSI console lines (single game thread in practice). */
  private final FlixelString consoleLine = new FlixelString(512);

  /** Reused for plain file lines. */
  private final FlixelString fileLine = new FlixelString(512);

  /**
   * Creates a logger that outputs to the console and optionally to a file
   * (when a {@link FlixelLogFileHandler} is registered on
   * {@link Flixel#setLogFileHandler}).
   *
   * @param logMode The mode used for console output formatting.
   */
  public FlixelLogger(FlixelLogMode logMode) {
    this.logMode = logMode != null ? logMode : FlixelLogMode.SIMPLE;
    this.stackTraceProvider = Flixel.getStackTraceProvider();
  }

  /**
   * Returns the current log mode used for console output formatting.
   *
   * @return The active log mode, never {@code null}.
   */
  public FlixelLogMode getLogMode() {
    return logMode;
  }

  /**
   * Sets the log mode used for console output formatting. If {@code null}
   * is passed, the mode defaults to {@link FlixelLogMode#SIMPLE}.
   *
   * @param logMode The desired log mode, or {@code null} to reset to the default simple mode.
   */
  public void setLogMode(FlixelLogMode logMode) {
    this.logMode = logMode != null ? logMode : FlixelLogMode.SIMPLE;
  }

  /**
   * Returns the stack trace provider used to determine the caller location
   * when logging messages.
   *
   * @return The current stack trace provider, or {@code null} if none has been set.
   */
  public FlixelStackTraceProvider getStackTraceProvider() {
    return stackTraceProvider;
  }

  /**
   * Sets the stack trace provider used to resolve the calling class and
   * method name for each log message.
   *
   * @param stackTraceProvider The provider to use for stack trace resolution.
   */
  public void setStackTraceProvider(FlixelStackTraceProvider stackTraceProvider) {
    this.stackTraceProvider = stackTraceProvider;
  }

  /**
   * Sets a custom folder where log files will be stored. Pass an absolute path to the folder
   * that should contain the log files (e.g. {@code /path/to/game/logs}). If not set, the default
   * is used: when running in an IDE, the project root's {@code logs} folder; when running from a
   * JAR, the {@code logs} folder next to the JAR.
   *
   * @param absolutePathToLogsFolder The absolute path to the logs folder, or {@code null} to use the default.
   */
  public void setLogsFolder(String absolutePathToLogsFolder) {
    this.customLogsFolderPath = (absolutePathToLogsFolder == null || absolutePathToLogsFolder.isEmpty())
      ? null
      : absolutePathToLogsFolder.replaceAll("/$", "");
  }

  public String getLogsFolder() {
    return customLogsFolderPath;
  }

  public boolean canStoreLogs() {
    return canStoreLogs;
  }

  public void setCanStoreLogs(boolean canStoreLogs) {
    this.canStoreLogs = canStoreLogs;
  }

  public int getMaxLogFiles() {
    return maxLogFiles;
  }

  public void setMaxLogFiles(int maxLogFiles) {
    this.maxLogFiles = maxLogFiles;
  }

  /**
   * Starts file logging by delegating to the registered
   * {@link FlixelLogFileHandler}. If no handler has been registered (for example, on web/TeaVM) or if
   * {@link #canStoreLogs()} returns {@code false}, this method is a no-op.
   *
   * <p>The handler creates the log folder, prunes old files, opens a new
   * timestamped log file, and (on JVM) starts a background writer thread.
   */
  public void startFileLogging() {
    FlixelLogFileHandler handler = Flixel.getLogFileHandler();
    if (handler == null || !canStoreLogs) {
      return;
    }
    handler.start(customLogsFolderPath, maxLogFiles);
  }

  /**
   * Stops file logging by delegating to the registered
   * {@link FlixelLogFileHandler}. The handler flushes any buffered log
   * lines and releases its resources.
   *
   * <p>Call this during game shutdown (for example from
   * {@link me.stringdotjar.flixelgdx.FlixelGame#dispose()}) so that logs
   * written during disposal are persisted.
   */
  public void stopFileLogging() {
    FlixelLogFileHandler handler = Flixel.getLogFileHandler();
    if (handler != null) {
      handler.stop();
    }
  }

  /**
   * Registers a listener that will be notified every time a log message is produced.
   *
   * @param listener A consumer that receives a {@link FlixelLogEntry}.
   */
  public void addLogListener(Consumer<FlixelLogEntry> listener) {
    if (listener != null) {
      logListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered log listener.
   *
   * @param listener The listener to remove.
   */
  public void removeLogListener(Consumer<FlixelLogEntry> listener) {
    logListeners.removeValue(listener, true);
  }

  /**
   * Registers a custom console entry whose lines will be shown in the debug overlay console.
   *
   * @param entry The entry to register.
   */
  public void addConsoleEntry(FlixelDebugConsoleEntry entry) {
    if (entry != null) {
      consoleEntries.add(entry);
    }
  }

  /**
   * Removes a previously registered console entry.
   *
   * @param entry The entry to remove.
   */
  public void removeConsoleEntry(FlixelDebugConsoleEntry entry) {
    consoleEntries.removeValue(entry, true);
  }

  public FlixelDebugConsoleEntry[] getConsoleEntries() {
    return consoleEntries.items;
  }

  /**
   * Logs an informational message using the default tag.
   *
   * @param message The message to log (converted via {@code toString()}).
   */
  public void info(Object message) {
    info(defaultTag, message);
  }

  /**
   * Logs an informational message under a custom tag.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   */
  public void info(String tag, Object message) {
    outputLog(tag, evaluateMessage(message), FlixelLogLevel.INFO);
  }

  /**
   * Logs a warning message using the default tag.
   *
   * @param message The message to log (converted via {@code toString()}).
   */
  public void warn(Object message) {
    warn(defaultTag, message);
  }

  /**
   * Logs a warning message under a custom tag.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   */
  public void warn(String tag, Object message) {
    outputLog(tag, evaluateMessage(message), FlixelLogLevel.WARN);
  }

  /**
   * Logs an error message using the default tag with no throwable.
   *
   * @param message The message to log (converted via {@code toString()}).
   */
  public void error(Object message) {
    error(defaultTag, message, null);
  }

  /**
   * Logs an error message using the default tag, including the throwable's
   * string representation in the output.
   *
   * @param message The message to log (converted via {@code toString()}).
   * @param throwable The exception to append to the log output.
   */
  public void error(Object message, Throwable throwable) {
    error(defaultTag, message, throwable);
  }

  /**
   * Logs an error message under a custom tag with no throwable.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   */
  public void error(String tag, Object message) {
    error(tag, message, null);
  }

  /**
   * Logs an error message under a custom tag, optionally including a
   * throwable in the output.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   * @param throwable The exception to append to the log output, or {@code null} if none.
   */
  public void error(String tag, Object message, Throwable throwable) {
    String msg = (throwable != null) ? (evaluateMessage(message) + " | Exception: " + throwable) : evaluateMessage(message);
    outputLog(tag, msg, FlixelLogLevel.ERROR);
  }

  /**
   * Formats and outputs a log message to the console (according to {@link #logMode}) and, if a
   * file line consumer is set, passes the detailed (plain) line for file output.
   */
  protected void outputLog(String tag, Object message, FlixelLogLevel level) {
    FlixelStackFrame caller = getCaller();

    String file;
    String simpleFile;
    String method;

    if (caller == null) {
      file = "UnknownFile.java:0";
      simpleFile = "unknown:0";
      method = "unknown()";
    } else {
      // Convert the package path and replace the periods (.) with slashes (/)
      // to replicate the familiar Haxe tracing.
      file = (caller.getFileName() != null ? caller.getFileName() : "UnknownFile.java") + ":" + caller.getLineNumber();
      String className = caller.getClassName();
      int lastDot = className != null ? className.lastIndexOf('.') : -1;
      String packagePath = (lastDot > 0) ? className.substring(0, lastDot).replace('.', '/') : "";

      // Assemble the log location and concatenate it together.
      simpleFile = packagePath.isEmpty()
        ? (caller.getFileName() != null ? caller.getFileName() : "UnknownFile.java") + ":" + caller.getLineNumber()
        : packagePath + "/" + (caller.getFileName() != null ? caller.getFileName() : "UnknownFile.java") + ":" + caller.getLineNumber();
      method = (caller.getMethodName() != null ? caller.getMethodName() : "unknownMethod") + "()"; // For detailed mode only.
    }

    // Apply the color and underlining based on the level.
    String rawMessage = evaluateMessage(message);
    String color = switch (level) {
      case INFO -> FlixelAsciiCodes.WHITE;
      case WARN -> FlixelAsciiCodes.YELLOW;
      case ERROR -> FlixelAsciiCodes.RED;
    };
    boolean underlineFile = (level == FlixelLogLevel.ERROR);

    String ts = LocalDateTime.now().format(LOG_TIMESTAMP);

    FlixelLogConsoleSink consoleSink = Flixel.getLogConsoleSink();
    if (consoleSink != null) {
      String safeTag = tag != null ? tag : "";
      consoleSink.emit(level, safeTag, rawMessage, simpleFile + ":", file, method, ts, logMode == FlixelLogMode.DETAILED);
    } else {
      // Console: use current log mode.
      consoleLine.clear();
      if (logMode == FlixelLogMode.SIMPLE) {
        appendColored(consoleLine, simpleFile + ":", color, true, false, underlineFile);
        consoleLine.concat(' ');
        appendColored(consoleLine, rawMessage, color, false, true, false);
      } else {
        String levelTag = "[" + level + "]";
        String tagPart = "[" + tag + "]";
        String filePart = "[" + file + "]";
        String methodPart = "[" + method + "]";
        appendColored(consoleLine, ts + " ", color, false, false, false);
        appendColored(consoleLine, levelTag + " ", color, true, false, false);
        appendColored(consoleLine, tagPart + " ", color, true, false, false);
        appendColored(consoleLine, filePart + " ", color, true, false, underlineFile);
        appendColored(consoleLine, methodPart + " ", color, false, false, false);
        appendColored(consoleLine, rawMessage, color, false, true, false);
      }
      System.out.println(consoleLine.toString());
    }

    // Notify in-game log listeners (e.g. the debug overlay console).
    if (!logListeners.isEmpty()) {
      FlixelLogEntry entry = new FlixelLogEntry(level, tag, rawMessage);
      for (Consumer<FlixelLogEntry> listener : logListeners) {
        listener.accept(entry);
      }
    }

    // File: always detailed (plain, no ANSI).
    FlixelLogFileHandler fileHandler = Flixel.getLogFileHandler();
    if (fileHandler != null && fileHandler.isActive()) {
      String levelTag = "[" + level + "]";
      String tagPart = "[" + tag + "]";
      String filePart = "[" + file + "]";
      String methodPart = "[" + method + "]";
      fileLine.clear();
      fileLine.concat(ts);
      fileLine.concat(' ');
      fileLine.concat(levelTag);
      fileLine.concat(' ');
      fileLine.concat(tagPart);
      fileLine.concat(' ');
      fileLine.concat(filePart);
      fileLine.concat(' ');
      fileLine.concat(methodPart);
      fileLine.concat(' ');
      fileLine.concat(rawMessage);
      fileHandler.write(fileLine.toString());
    }
  }

  /**
   * Gets the location of where the log was called from.
   *
   * <p>This is used to get the file and method name of where the log was called from.
   *
   * @return The location of where the log was called from.
   */
  protected FlixelStackFrame getCaller() {
    return (stackTraceProvider != null) ? stackTraceProvider.getCaller() : null;
  }

  /**
   * Appends {@code text} to {@code out} with ANSI color and style codes for console output.
   */
  private void appendColored(
    FlixelString out, String text, String color, boolean bold, boolean italic, boolean underline) {
    if (bold) {
      out.concat(FlixelAsciiCodes.BOLD);
    }
    if (italic) {
      out.concat(FlixelAsciiCodes.ITALIC);
    }
    if (underline) {
      out.concat(FlixelAsciiCodes.UNDERLINE);
    }
    out.concat(color);
    out.concat(text);
    out.concat(FlixelAsciiCodes.RESET);
  }

  private String evaluateMessage(Object message) {
    return message != null ? message.toString() : "null";
  }

  public String getDefaultTag() {
    return defaultTag;
  }

  public void setDefaultTag(String defaultTag) {
    this.defaultTag = defaultTag != null ? defaultTag : "";
  }
}
