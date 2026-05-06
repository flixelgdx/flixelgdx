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

import com.badlogic.gdx.ApplicationLogger;
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
 */
public class FlixelLogger implements ApplicationLogger {

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
    outputLog(defaultTag, evaluateMessage(message), FlixelLogLevel.INFO, false, null, 0, null, null);
  }

  /**
   * Logs an informational message under a custom tag.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   */
  public void info(String tag, Object message) {
    outputLog(tag, evaluateMessage(message), FlixelLogLevel.INFO, false, null, 0, null, null);
  }

  /**
   * Logs an informational message using the default tag with an explicit call site.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #info(Object)}.
   *
   * @param message The message to log (converted via {@code toString()}).
   * @param sourceFileName The JVM source file name at the call site (for example {@code MyState.java}).
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site (no suffix).
   */
  public void infoWithSite(
    Object message,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    infoWithSite(defaultTag, message, sourceFileName, lineNumber, declaringClassName, declaringMethodName);
  }

  /**
   * Logs an informational message under a custom tag with an explicit call site.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #info(Object)}.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void infoWithSite(
    String tag,
    Object message,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    outputLog(
      tag,
      evaluateMessage(message),
      FlixelLogLevel.INFO,
      true,
      sourceFileName,
      lineNumber,
      declaringClassName,
      declaringMethodName);
  }

  /**
   * Logs a warning message using the default tag.
   *
   * @param message The message to log (converted via {@code toString()}).
   */
  public void warn(Object message) {
    outputLog(defaultTag, evaluateMessage(message), FlixelLogLevel.WARN, false, null, 0, null, null);
  }

  /**
   * Logs a warning message under a custom tag.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   */
  public void warn(String tag, Object message) {
    outputLog(tag, evaluateMessage(message), FlixelLogLevel.WARN, false, null, 0, null, null);
  }

  /**
   * Logs a warning message using the default tag with an explicit call site.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #warn(Object)}.
   *
   * @param message The message to log (converted via {@code toString()}).
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void warnWithSite(
    Object message,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    warnWithSite(defaultTag, message, sourceFileName, lineNumber, declaringClassName, declaringMethodName);
  }

  /**
   * Logs a warning message under a custom tag with an explicit call site.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #warn(Object)}.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void warnWithSite(
    String tag,
    Object message,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    outputLog(
      tag,
      evaluateMessage(message),
      FlixelLogLevel.WARN,
      true,
      sourceFileName,
      lineNumber,
      declaringClassName,
      declaringMethodName);
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
    outputLog(tag, msg, FlixelLogLevel.ERROR, false, null, 0, null, null);
  }

  /**
   * Logs an error message using the default tag with an explicit call site and no throwable.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #error(Object)}.
   *
   * @param message The message to log (converted via {@code toString()}).
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void errorWithSite(
    Object message,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    errorWithSite(defaultTag, message, null, sourceFileName, lineNumber, declaringClassName, declaringMethodName);
  }

  /**
   * Logs an error message using the default tag with an explicit call site.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #error(Object)}.
   *
   * @param message The message to log (converted via {@code toString()}).
   * @param throwable The exception to append to the log output, or {@code null} if none.
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void errorWithSite(
    Object message,
    Throwable throwable,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    errorWithSite(defaultTag, message, throwable, sourceFileName, lineNumber, declaringClassName, declaringMethodName);
  }

  /**
   * Logs an error message under a custom tag with an explicit call site and no throwable.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #error(Object)}.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void errorWithSite(
    String tag,
    Object message,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    errorWithSite(tag, message, null, sourceFileName, lineNumber, declaringClassName, declaringMethodName);
  }

  /**
   * Logs an error message under a custom tag with an explicit call site.
   *
   * <p>Typically invoked by the {@code flixelgdx-logging-plugin} bytecode weaver so file and line do not rely on
   * {@link FlixelStackTraceProvider} (for example on TeaVM). You don't need to (nor should you) touch this method;
   * you should use the other methods, such as {@link #error(Object)}.
   *
   * @param tag The tag to associate with this log entry.
   * @param message The message to log (converted via {@code toString()}).
   * @param throwable The exception to append to the log output, or {@code null} if none.
   * @param sourceFileName The JVM source file name at the call site.
   * @param lineNumber The source line number from debug metadata, or {@code 0} if unknown.
   * @param declaringClassName The fully qualified name of the class containing the call site.
   * @param declaringMethodName The simple name of the method containing the call site.
   */
  public void errorWithSite(
    String tag,
    Object message,
    Throwable throwable,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {
    String msg = (throwable != null) ? (evaluateMessage(message) + " | Exception: " + throwable) : evaluateMessage(message);
    outputLog(
      tag,
      msg,
      FlixelLogLevel.ERROR,
      true,
      sourceFileName,
      lineNumber,
      declaringClassName,
      declaringMethodName);
  }

  /**
   * Formats and outputs a log message to the console (according to {@link #logMode}) and, if a
   * file line consumer is set, passes the detailed (plain) line for file output.
   */
  protected void outputLog(String tag, Object message, FlixelLogLevel level) {
    outputLog(tag, evaluateMessage(message), level, false, null, 0, null, null);
  }

  /**
   * Writes a log line using either an explicit call site or {@link #getCaller()} when {@code explicitSite} is false.
   *
   * @param tag The tag for this entry (may be {@code null}).
   * @param rawMessage The message text already evaluated with {@link #evaluateMessage(Object)}.
   * @param level The log level.
   * @param explicitSite When {@code true}, use the four trailing site parameters instead of stack walking.
   * @param sourceFileName Source file name when explicit; ignored when not explicit.
   * @param lineNumber Source line when explicit; ignored when not explicit.
   * @param declaringClassName Fully qualified class name when explicit; ignored when not explicit.
   * @param declaringMethodName Simple method name when explicit; ignored when not explicit.
   */
  protected void outputLog(
    String tag,
    String rawMessage,
    FlixelLogLevel level,
    boolean explicitSite,
    String sourceFileName,
    int lineNumber,
    String declaringClassName,
    String declaringMethodName) {

    String file;
    String simpleFile;
    String method;

    if (explicitSite) {
      // Use provided values for file, line, class, and method name.
      String safeFile = (sourceFileName != null && !sourceFileName.isEmpty()) ? sourceFileName : "UnknownFile.java";
      int safeLine = Math.max(lineNumber, 0);
      String safeClass = (declaringClassName != null) ? declaringClassName : "";
      String safeMethodName = (declaringMethodName != null) ? declaringMethodName : "unknownMethod";

      file = safeFile + ":" + safeLine;

      // Extract the package path for a more detailed "simpleFile" path.
      int lastDot = safeClass.lastIndexOf('.');
      String packagePath = (lastDot > 0)
        ? safeClass.substring(0, lastDot).replace('.', '/')
        : "";

      simpleFile = packagePath.isEmpty()
        ? safeFile + ":" + safeLine
        : packagePath + "/" + safeFile + ":" + safeLine;

      method = safeMethodName + "()";
    } else {
      // Use stack inspection to get call site as a fallback.
      FlixelStackFrame caller = getCaller();

      if (caller == null) {
        // Fallback if stack frame can't be determined.
        file = "UnknownFile.java:0";
        simpleFile = "unknown:0";
        method = "unknown()";
      } else {
        // Pull file name and line number from the caller.
        String callerFile = (caller.getFileName() != null) ? caller.getFileName() : "UnknownFile.java";
        file = callerFile + ":" + caller.getLineNumber();

        // Extract the package path from the caller's class name.
        String className = caller.getClassName();
        int lastDot = (className != null) ? className.lastIndexOf('.') : -1;
        String packagePath = (lastDot > 0)
          ? className.substring(0, lastDot).replace('.', '/')
          : "";

        simpleFile = packagePath.isEmpty()
          ? callerFile + ":" + caller.getLineNumber()
          : packagePath + "/" + callerFile + ":" + caller.getLineNumber();

        // Use method name from the stack frame, or "unknownMethod" as a fallback.
        method = ((caller.getMethodName() != null) ? caller.getMethodName() : "unknownMethod") + "()";
      }
    }

    // Apply the color and underlining based on the level.
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
        appendColored(consoleLine, ts + " ", color, false, false, underlineFile);
        appendColored(consoleLine, levelTag + " ", color, true, false, underlineFile);
        appendColored(consoleLine, tagPart + " ", color, true, false, underlineFile);
        appendColored(consoleLine, filePart + " ", color, true, false, underlineFile);
        appendColored(consoleLine, methodPart, color, false, false, underlineFile);
        appendColored(consoleLine, " " + rawMessage, color, false, true, false);
      }
      System.out.println(consoleLine);
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
   *
   * @param out The string to append the text to.
   * @param text The text to append.
   * @param color The color to append.
   * @param bold Whether to append the bold code.
   * @param italic Whether to append the italic code.
   * @param underline Whether to append the underline code.
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

  @Override
  public void log(String tag, String message) {
    info(tag, message);
  }

  @Override
  public void log(String tag, String message, Throwable exception) {
    error(tag, message, exception);
  }

  @Override
  public void error(String tag, String message) {
    error(tag, message, null);
  }

  @Override
  public void error(String tag, String message, Throwable exception) {
    error(tag, message, exception);
  }

  @Override
  public void debug(String tag, String message) {
    info(tag, message);
  }

  @Override
  public void debug(String tag, String message, Throwable exception) {
    error(tag, message, exception);
  }
}
