package me.stringdotjar.flixelgdx.logging;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.util.FlixelConstants;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

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
public class FlixelLogger {

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

  /** Where the log file is written (for reference); actual writes go through the file line consumer. */
  private FileHandle logFileLocation;

  /** Log mode for console output. File output always uses {@link FlixelLogMode#DETAILED}. */
  private FlixelLogMode logMode;

  /** Callback for when a log message is written to the file. */
  private Consumer<String> fileLineConsumer;

  /** Provider for collecting stack trace information for the logger. */
  private FlixelStackTraceProvider stackTraceProvider;

  private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
  private final Object logQueueLock = new Object();
  private volatile boolean logWriterShutdownRequested = false;
  private Thread logThread;
  private String customLogsFolderPath = null; // Null means use default (IDE root or JAR dir).

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

  /**
   * Sets a custom folder where log files will be stored. Pass an absolute path to the folder
   * that should contain the log files (e.g. {@code /path/to/game/logs}). If not set, the default
   * is used: when running in an IDE, the project root's {@code logs} folder; when running from a
   * JAR, the {@code logs} folder next to the JAR.
   *
   * @param absolutePathToLogsFolder Absolute path to the logs folder, or {@code null} to use the default.
   */
  public void setLogsFolder(String absolutePathToLogsFolder) {
    this.customLogsFolderPath = (absolutePathToLogsFolder == null || absolutePathToLogsFolder.isEmpty())
      ? null
      : absolutePathToLogsFolder.replaceAll("/$", "");
  }

  /**
   * Returns the custom logs folder path set by {@link #setLogsFolder(String)}, or {@code null} if
   * the default location is used.
   */
  public String getLogsFolder() {
    return customLogsFolderPath;
  }

  /** Returns whether file logging is enabled when {@link #startFileLogging()} is called. */
  public boolean canStoreLogs() {
    return canStoreLogs;
  }

  /** Sets whether to write logs to a file when {@link #startFileLogging()} is called. */
  public void setCanStoreLogs(boolean canStoreLogs) {
    this.canStoreLogs = canStoreLogs;
  }

  /** Returns the maximum number of log files to keep when file logging is enabled. */
  public int getMaxLogFiles() {
    return maxLogFiles;
  }

  /** Sets the maximum number of log files to keep; older files are deleted when exceeded. */
  public void setMaxLogFiles(int maxLogFiles) {
    this.maxLogFiles = maxLogFiles;
  }

  /**
   * Enqueues a single log line for this logger's file writer thread. Used by this logger's file consumer.
   */
  void enqueueLogToFile(String log) {
    logQueue.add(log);
    synchronized (logQueueLock) {
      logQueueLock.notify();
    }
  }

  /**
   * Starts or configures file logging for this logger. When {@link #canStoreLogs()} is {@code true},
   * creates the logs folder (default or custom), prunes old log files, creates a new timestamped log
   * file, and starts a background thread that writes log lines to that file. When {@code canStoreLogs}
   * is {@code false}, no writer thread is started and any existing log files in the logs folder are deleted.
   */
  public void startFileLogging() {
    String logsFolderPath = (customLogsFolderPath != null)
      ? customLogsFolderPath
      : FlixelRuntimeUtil.getDefaultLogsFolderPath();
    if (logsFolderPath == null) {
      return;
    }

    if (Gdx.files == null) {
      return;
    }

    FileHandle logsFolder = Gdx.files.absolute(logsFolderPath);
    logsFolder.mkdirs();

    if (canStoreLogs) {
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
      String date = now.format(formatter);

      // Delete old log files if we have more than the maximum number of log files.
      FileHandle[] logFiles = logsFolder.list();
      if (logFiles != null && logFiles.length >= maxLogFiles) {
        Arrays.sort(logFiles, Comparator.comparing(FileHandle::name));
        int toDelete = logFiles.length - maxLogFiles + 1;
        for (int i = 0; i < toDelete; i++) {
          logFiles[i].delete();
        }
      }

      FileHandle logFile = Gdx.files.absolute(logsFolderPath + "/flixel-" + date + ".log");

      setLogFileLocation(logFile);
      setFileLineConsumer(this::enqueueLogToFile);

      // Start the log writer thread.
      logWriterShutdownRequested = false;
      final FileHandle logFileForThread = logFile;
      logThread = new Thread(() -> {
        try {
          while (true) {
            String log = logQueue.poll();
            if (log != null) {
              logFileForThread.writeString(log + "\n", true);
            } else {
              synchronized (logQueueLock) {
                if (logWriterShutdownRequested) {
                  break;
                }
                try {
                  logQueueLock.wait();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            }
          }
        } catch (Exception ignored) {
          // ignore
        }
      });
      logThread.setName("FlixelGDX Log Thread");
      logThread.setDaemon(true);
      logThread.start();
    }
  }

  /**
   * Stops this logger's log file writer thread and flushes any remaining log lines. Call this during
   * game shutdown (e.g. from {@link FlixelGame#dispose()}) so that logs written during dispose are persisted.
   */
  public void stopFileLogging() {
    synchronized (logQueueLock) {
      logWriterShutdownRequested = true;
      logQueueLock.notify();
    }
    if (logThread != null && logThread.isAlive()) {
      try {
        logThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      logThread = null;
    }
    setLogFileLocation(null);
    setFileLineConsumer(null);
  }

  public void info(Object message) {
    info(defaultTag, message);
  }

  public void info(String tag, Object message) {
    outputLog(tag, evaluateMessage(message), FlixelLogLevel.INFO);
  }

  public void warn(Object message) {
    warn(defaultTag, message);
  }

  public void warn(String tag, Object message) {
    outputLog(tag, evaluateMessage(message), FlixelLogLevel.WARN);
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
    String msg = (throwable != null) ? (evaluateMessage(message) + " | Exception: " + throwable) : evaluateMessage(message);
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
    String rawMessage = evaluateMessage(message);
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

  private String evaluateMessage(Object message) {
    return message != null ? message.toString() : "null";
  }

  public String getDefaultTag() {
    return defaultTag;
  }

  public void setDefaultTag(String defaultTag) {
    this.defaultTag = defaultTag;
  }
}
