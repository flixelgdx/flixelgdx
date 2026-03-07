package me.stringdotjar.flixelgdx.logging;

import com.badlogic.gdx.files.FileHandle;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.util.FlixelConstants;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Logger instance for Flixel that formats and outputs log messages to the console and optionally
 * to a file. Console output respects the current {@link FlixelLogMode}; file output always uses
 * a detailed format.
 */
public class FlixelLogger {

  /** Default tag to use when logging without a specific tag. */
  private String defaultTag = "";

  /** Where the log file is written (for reference); actual writes go through the file line consumer. */
  private FileHandle logFileLocation;

  /** Log mode for console output. File output always uses {@link FlixelLogMode#DETAILED}. */
  private FlixelLogMode logMode;

  /** Callback for when a log message is written to the file. */
  private Consumer<String> fileLineConsumer;

  /** Provider for collecting stack trace information for the logger. */
  private FlixelStackTraceProvider stackTraceProvider;

  /**
   * Creates a logger that outputs to the console and a file.
   *
   * @param logMode The mode used for console output.
   */
  public FlixelLogger(FlixelLogMode logMode) {
    this(null, logMode, null);
  }

  /**
   * Creates a logger with an optional log file location, optional consumer for file lines, and
   * a custom stack trace provider.
   *
   * @param logFileLocation Where the log file is stored (which might be null).
   * @param logMode The mode used for console output.
   * @param fileLineConsumer Callback for when a log message is written to the file.
   */
  public FlixelLogger(FileHandle logFileLocation, FlixelLogMode logMode, Consumer<String> fileLineConsumer) {
    this.logFileLocation = logFileLocation;
    this.logMode = logMode != null ? logMode : FlixelLogMode.SIMPLE;
    this.fileLineConsumer = fileLineConsumer;
    this.stackTraceProvider = Flixel.getStackTraceProvider();
  }

  public FileHandle getLogFileLocation() {
    return logFileLocation;
  }

  public void setLogFileLocation(FileHandle logFileLocation) {
    this.logFileLocation = logFileLocation;
  }

  public FlixelLogMode getLogMode() {
    return logMode;
  }

  public void setLogMode(FlixelLogMode logMode) {
    this.logMode = logMode != null ? logMode : FlixelLogMode.SIMPLE;
  }

  public Consumer<String> getFileLineConsumer() {
    return fileLineConsumer;
  }

  public void setFileLineConsumer(Consumer<String> fileLineConsumer) {
    this.fileLineConsumer = fileLineConsumer;
  }

  public FlixelStackTraceProvider getStackTraceProvider() {
    return stackTraceProvider;
  }

  public void setStackTraceProvider(FlixelStackTraceProvider stackTraceProvider) {
    this.stackTraceProvider = stackTraceProvider;
  }

  public void info(Object message) {
    info(defaultTag, message);
  }

  public void info(String tag, Object message) {
    outputLog(tag, message.toString(), FlixelLogLevel.INFO);
  }

  public void warn(Object message) {
    warn(defaultTag, message);
  }

  public void warn(String tag, Object message) {
    outputLog(tag, message.toString(), FlixelLogLevel.WARN);
  }

  public void error(Object message) {
    error(defaultTag, message, null);
  }

  public void error(Object message, Throwable throwable) {
    error(defaultTag, message, throwable);
  }

  public void error(String tag, Object message) {
    error(tag, message, null);
  }

  public void error(String tag, Object message, Throwable throwable) {
    String msg = (throwable != null) ? (message.toString() + " | Exception: " + throwable) : message.toString();
    outputLog(tag, msg, FlixelLogLevel.ERROR);
  }

  /**
   * Formats and outputs a log message to the console (according to {@link #logMode}) and, if a
   * file line consumer is set, passes the detailed (plain) line for file output.
   */
  protected void outputLog(String tag, Object message, FlixelLogLevel level) {
    FlixelStackFrame caller = getCaller();

    if (caller == null) {
      return;
    }

    String file;
    String simpleFile;
    String method;

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

    // Apply the color and underlining based on the level.
    String rawMessage = (message != null) ? message.toString() : "null";
    String color = switch (level) {
      case INFO -> FlixelConstants.AsciiCodes.WHITE;
      case WARN -> FlixelConstants.AsciiCodes.YELLOW;
      case ERROR -> FlixelConstants.AsciiCodes.RED;
    };
    boolean underlineFile = (level == FlixelLogLevel.ERROR);

    // Console: use current log mode.
    String coloredLog;
    if (logMode == FlixelLogMode.SIMPLE) {
      coloredLog = colorText(simpleFile + ":", color, true, false, underlineFile)
        + " "
        + colorText(rawMessage, color, false, true, false);
    } else {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      String levelTag = "[" + level + "]";
      String tagPart = "[" + tag + "]";
      String filePart = "[" + file + "]";
      String methodPart = "[" + method + "]";
      coloredLog = colorText(timestamp + " ", color, false, false, false)
        + colorText(levelTag + " ", color, true, false, false)
        + colorText(tagPart + " ", color, true, false, false)
        + colorText(filePart + " ", color, true, false, underlineFile)
        + colorText(methodPart + " ", color, false, false, false)
        + colorText(rawMessage, color, false, true, false);
    }
    System.out.println(coloredLog);

    // File: always detailed (plain, no ANSI).
    if (fileLineConsumer != null) {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      String levelTag = "[" + level + "]";
      String tagPart = "[" + tag + "]";
      String filePart = "[" + file + "]";
      String methodPart = "[" + method + "]";
      String plainLog = timestamp + " " + levelTag + " " + tagPart + " " + filePart + " " + methodPart + " " + rawMessage;
      fileLineConsumer.accept(plainLog);
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
   * Wraps text with ANSI color/format codes for console output.
   */
  protected String colorText(String text, String color, boolean bold, boolean italic, boolean underline) {
    StringBuilder sb = new StringBuilder();
    if (bold) {
      sb.append(FlixelConstants.AsciiCodes.BOLD);
    }
    if (italic) {
      sb.append(FlixelConstants.AsciiCodes.ITALIC);
    }
    if (underline) {
      sb.append(FlixelConstants.AsciiCodes.UNDERLINE);
    }
    sb.append(color);
    sb.append(text);
    sb.append(FlixelConstants.AsciiCodes.RESET);
    return sb.toString();
  }

  public String getDefaultTag() {
    return defaultTag;
  }

  public void setDefaultTag(String defaultTag) {
    this.defaultTag = defaultTag;
  }
}
