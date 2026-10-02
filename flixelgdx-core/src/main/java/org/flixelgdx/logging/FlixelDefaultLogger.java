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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default {@link FlixelLogger}: it formats each message once and hands it to a console sink, a file
 * sink, and any number of extra sinks.
 *
 * <p>Think of one root logger as a post office and every {@link #tagged(String) tagged} logger as a
 * mail slot in the front door of that office. Each slot stamps letters with its own label (the tag),
 * but all of them feed the same sorting room (the level, mode, sinks, and file settings). Setting the
 * level on any slot changes it for the whole office, which is why the setters of a tagged logger
 * forward to its root, and only {@link #setTag(String)} is private to a slot.
 *
 * <p>Logging is designed to cost almost nothing when the level filters a message out, and to create no
 * garbage when it does not. One lock guards a reused {@link FlixelLogEntry} and a reused text buffer, so
 * it is safe to log from any thread. A sink that logs while it is being called (or a message whose
 * {@code toString()} logs) is detected: the nested message is written as a plain line to standard
 * error, and anything nested deeper than that is dropped.
 *
 * <p>File logging is controlled through {@link #setLogsFolder(String)}, {@link #setCanStoreLogs(boolean)},
 * and {@link #setMaxLogFiles(int)}, followed by {@link #startFileLogging()} and, when the game shuts
 * down, {@link #stopFileLogging()}.
 *
 * <p>Example:
 * <pre>{@code
 * FlixelDefaultLogger root = new FlixelDefaultLogger(FlixelLogMode.DETAILED);
 * root.setLevel(FlixelLogLevel.INFO);
 * FlixelLogger ai = root.tagged("AI");
 * ai.info("Path found in {} steps", 12);
 * }</pre>
 */
public final class FlixelDefaultLogger implements FlixelLogger {

  private int depth;
  private int maxLogFiles = 10;

  private volatile FlixelLogLevel level = FlixelLogLevel.DEBUG;
  private volatile String tag = "";

  private FlixelDefaultLogger root;
  private Object lock;
  private FlixelLogMode mode;
  private String logsFolder;
  private FlixelLogSink consoleSink;
  private FlixelLogFileSink fileSink;
  private FlixelLogSiteResolver siteResolver = FlixelLogSiteMarker.INSTANCE;
  private FlixelArray<FlixelLogSink> extraSinks;
  private FlixelString text;
  private FlixelString fallback;
  private FlixelLogEntry entry;

  private boolean canStoreLogs = true;

  /** Creates a root logger that uses {@link FlixelLogMode#SIMPLE} and prints plain text to the console. */
  public FlixelDefaultLogger() {
    this(FlixelLogMode.SIMPLE);
  }

  /**
   * Creates a root logger that prints plain text to the console.
   *
   * @param mode The format of the console output. {@code null} means {@link FlixelLogMode#SIMPLE}.
   */
  public FlixelDefaultLogger(@Nullable FlixelLogMode mode) {
    this.root = this;
    this.lock = new Object();
    this.mode = mode != null ? mode : FlixelLogMode.SIMPLE;
    this.consoleSink = new FlixelPlainConsoleSink();
    this.extraSinks = new FlixelArray<>(FlixelLogSink[]::new);
    this.text = new FlixelString(512);
    this.fallback = new FlixelString(128);
    this.entry = new FlixelLogEntry(text);
  }

  private FlixelDefaultLogger(@NotNull FlixelDefaultLogger root, @NotNull String tag) {
    this.root = root;
    this.tag = tag;
  }

  @Override
  public boolean isEnabled(@NotNull FlixelLogLevel level) {
    return level.getSeverity() >= root.level.getSeverity();
  }

  @Override
  public void log(
      @NotNull FlixelLogLevel level,
      @Nullable Object message,
      int argc,
      @Nullable Object a1,
      @Nullable Object a2,
      @Nullable Object a3,
      @Nullable Object[] varargs) {
    if (!isEnabled(level)) {
      FlixelLogSiteMarker.clear();
      return;
    }
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      if (r.depth > 1) {
        FlixelLogSiteMarker.clear();
        return;
      }
      if (r.depth == 1) {
        r.depth = 2;
        try {
          r.writeFallback(tag, level, message, argc, a1, a2, a3, varargs);
        } finally {
          r.depth = 1;
        }
        return;
      }
      r.depth = 1;
      try {
        r.dispatch(tag, level, message, argc, a1, a2, a3, varargs);
      } finally {
        r.depth = 0;
        r.entry.throwable = null;
        FlixelLogSiteMarker.clear();
      }
    }
  }

  /**
   * Returns a logger that prints the given tag on every message.
   *
   * <p>The returned logger owns only its tag. Its level, mode, sinks, call site resolver, and file
   * settings all belong to this logger's root, so changing any of them on the child changes them for
   * the root and every other tagged logger.
   *
   * @param tag The tag to print, or {@code null} for none.
   * @return A new logger that shares everything except its tag.
   */
  @Override
  @NotNull
  public FlixelLogger tagged(@Nullable String tag) {
    return new FlixelDefaultLogger(root, tag != null ? tag : "");
  }

  @Override
  public void addExtraSink(@Nullable FlixelLogSink sink) {
    if (sink == null) {
      return;
    }
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.extraSinks.add(sink);
    }
  }

  @Override
  public void removeExtraSink(@Nullable FlixelLogSink sink) {
    if (sink == null) {
      return;
    }
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.extraSinks.removeValue(sink, true);
    }
  }

  @Override
  public void startFileLogging() {
    FlixelLogFileSink sink;
    String folder;
    int max;
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      if (r.fileSink == null || !r.canStoreLogs) {
        return;
      }
      sink = r.fileSink;
      folder = r.logsFolder;
      max = r.maxLogFiles;
    }
    sink.open(folder, max);
  }

  @Override
  public void stopFileLogging() {
    FlixelLogFileSink sink;
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      sink = r.fileSink;
    }
    if (sink != null) {
      sink.close();
    }
  }

  private void dispatch(
      String callerTag,
      FlixelLogLevel lvl,
      Object message,
      int argc,
      Object a1,
      Object a2,
      Object a3,
      Object[] varargs) {
    text.clear();
    Throwable thrown = format(text, message, argc, a1, a2, a3, varargs);
    entry.reset(System.currentTimeMillis(), lvl, mode, callerTag, thrown, siteResolver);

    FlixelLogSink console = consoleSink;
    if (console != null) {
      send(console);
    }
    FlixelLogFileSink file = fileSink;
    if (file != null && file.isOpen()) {
      send(file);
    }
    for (int i = 0; i < extraSinks.getSize(); i++) {
      FlixelLogSink extra = extraSinks.get(i);
      if (extra != null) {
        send(extra);
      }
    }
  }

  private void send(FlixelLogSink sink) {
    try {
      sink.write(entry);
    } catch (RuntimeException e) {
      System.err.println("[FlixelLogger] A log sink threw an exception: " + e);
    }
  }

  private void writeFallback(
      String callerTag,
      FlixelLogLevel lvl,
      Object message,
      int argc,
      Object a1,
      Object a2,
      Object a3,
      Object[] varargs) {
    fallback.clear();
    fallback.concat('[');
    fallback.concat(lvl.name());
    fallback.concat(']');
    fallback.concat(' ');
    if (!callerTag.isEmpty()) {
      fallback.concat('[');
      fallback.concat(callerTag);
      fallback.concat(']');
      fallback.concat(' ');
    }
    format(fallback, message, argc, a1, a2, a3, varargs);
    System.err.println(fallback);
  }

  /**
   * Fills {@code out} with the formatted message and returns the exception that the entry should carry.
   *
   * @return The trailing {@link Throwable} that no placeholder consumed, or {@code null}.
   */
  @Nullable
  private static Throwable format(
      FlixelString out, Object message, int argc, Object a1, Object a2, Object a3, Object[] varargs) {
    int n;
    if (varargs != null) {
      n = varargs.length;
    } else {
      n = Math.max(0, Math.min(argc, 3));
    }

    int used = 0;
    if (message instanceof CharSequence) {
      CharSequence fmt = (CharSequence) message;
      int len = fmt.length();
      for (int i = 0; i < len; i++) {
        char c = fmt.charAt(i);
        if (c == '{' && i + 1 < len && fmt.charAt(i + 1) == '}') {
          if (used < n) {
            append(out, argAt(used++, a1, a2, a3, varargs));
          } else {
            out.concat('{');
            out.concat('}');
          }
          i++;
        } else {
          out.concat(c);
        }
      }
    } else {
      append(out, message);
    }

    Throwable thrown = null;
    if (used < n && argAt(n - 1, a1, a2, a3, varargs) instanceof Throwable) {
      thrown = (Throwable) argAt(n - 1, a1, a2, a3, varargs);
      n--;
    }
    for (int i = used; i < n; i++) {
      out.concat(' ');
      append(out, argAt(i, a1, a2, a3, varargs));
    }
    return thrown;
  }

  private static Object argAt(int i, Object a1, Object a2, Object a3, Object[] varargs) {
    if (varargs != null) {
      return varargs[i];
    }
    return i == 0 ? a1 : (i == 1 ? a2 : a3);
  }

  private static void append(FlixelString out, Object value) {
    if (value == null) {
      out.concat("null");
    } else if (value instanceof CharSequence) {
      out.concat((CharSequence) value);
    } else if (value instanceof Integer) {
      out.concat(((Integer) value).intValue());
    } else if (value instanceof Long) {
      out.concat(((Long) value).longValue());
    } else if (value instanceof Float) {
      out.concat(((Float) value).floatValue());
    } else if (value instanceof Double) {
      out.concat(((Double) value).doubleValue());
    } else if (value instanceof Boolean) {
      out.concat(((Boolean) value).booleanValue());
    } else if (value instanceof Character) {
      out.concat(((Character) value).charValue());
    } else if (value instanceof Short) {
      out.concat(((Short) value).shortValue());
    } else if (value instanceof Byte) {
      out.concat(((Byte) value).byteValue());
    } else {
      out.concat(value);
    }
  }

  @Override
  @NotNull
  public FlixelLogLevel getLevel() {
    return root.level;
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the level of its root, which every tagged logger shares.
   */
  @Override
  public void setLevel(@Nullable FlixelLogLevel level) {
    root.level = level != null ? level : FlixelLogLevel.DEBUG;
  }

  @Override
  @NotNull
  public FlixelLogMode getMode() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.mode;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the mode of its root, which every tagged logger shares.
   */
  @Override
  public void setMode(@Nullable FlixelLogMode mode) {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.mode = mode != null ? mode : FlixelLogMode.SIMPLE;
    }
  }

  @Override
  @NotNull
  public String getTag() {
    return tag;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Unlike the other setters, this only changes the tag of this logger, never the tag of its root
   * or of other tagged loggers.
   */
  @Override
  public void setTag(@Nullable String tag) {
    this.tag = tag != null ? tag : "";
  }

  @Override
  @Nullable
  public FlixelLogSink getConsoleSink() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.consoleSink;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the sink of its root, which every tagged logger shares.
   */
  @Override
  public void setConsoleSink(@Nullable FlixelLogSink sink) {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.consoleSink = sink;
    }
  }

  @Override
  @Nullable
  public FlixelLogFileSink getFileSink() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.fileSink;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the sink of its root, which every tagged logger shares.
   */
  @Override
  public void setFileSink(@Nullable FlixelLogFileSink sink) {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.fileSink = sink;
    }
  }

  @Override
  @Nullable
  public FlixelLogSiteResolver getSiteResolver() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.siteResolver;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the resolver of its root, which every tagged logger shares.
   */
  @Override
  public void setSiteResolver(@Nullable FlixelLogSiteResolver resolver) {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.siteResolver = resolver;
    }
  }

  @Override
  @Nullable
  public String getLogsFolder() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.logsFolder;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the folder of its root, which every tagged logger shares.
   */
  @Override
  public void setLogsFolder(@Nullable String absolutePathToLogsFolder) {
    String path = absolutePathToLogsFolder;
    if (path != null && path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.logsFolder = (path == null || path.isEmpty()) ? null : path;
    }
  }

  @Override
  public int getMaxLogFiles() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.maxLogFiles;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the setting of its root, which every tagged logger shares.
   */
  @Override
  public void setMaxLogFiles(int maxLogFiles) {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.maxLogFiles = maxLogFiles;
    }
  }

  @Override
  public boolean canStoreLogs() {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      return r.canStoreLogs;
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>On a tagged logger, this changes the setting of its root, which every tagged logger shares.
   */
  @Override
  public void setCanStoreLogs(boolean canStoreLogs) {
    FlixelDefaultLogger r = root;
    synchronized (r.lock) {
      r.canStoreLogs = canStoreLogs;
    }
  }
}
