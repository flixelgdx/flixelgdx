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
package org.flixelgdx.debug.feeds;

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogSink;
import org.flixelgdx.logging.FlixelLogSite;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The log feed of the remote debugger: it remembers the most recent log lines so the debugger app and the logs
 * panel can show them.
 *
 * <p>Think of it as a conveyor belt with room for {@value #MAX_LINES} boxes. Every log message from
 * {@code Flixel.log} puts a new box on the belt, and once the belt is full the oldest box falls off the
 * end. The network agent grabs all boxes that arrived since its last visit with
 * {@link #write(FlixelDebugMessageWriter)}, or the whole belt with
 * {@link #writeFirst(FlixelDebugMessageWriter)} right after the app subscribes. A panel reads the boxes with
 * {@link #visitSince(long, LineVisitor)}, keeping track of its own position on the belt.
 *
 * <p>The logger reuses its entry object for every message, so this feed copies each message into a
 * pooled line object that is created once and then recycled. Messages longer than
 * {@value FlixelDebugProtocol#MAX_LOG_MESSAGE_CHARS} characters are cut and end with
 * {@value #CUT_OFF}. The feed receives log messages only while it is attached to the logger, which the
 * manager does when the game starts in debug mode.
 *
 * <p>Logging is allowed from any thread, so the line buffer is guarded by a lock. Reading and writing it
 * from the game thread is still allocation free once the belt has filled for the first time.
 *
 * @see FlixelDebugProtocol#KIND_LOGS
 */
public class FlixelDebugLogs extends FlixelDebugFeedBase {

  /** How many log lines the feed keeps. */
  public static final int MAX_LINES = 500;

  /** The text appended to a message that was shortened. */
  public static final String CUT_OFF = " (cut off)";

  /** A message that starts with this text is flagged as an uncaught exception. */
  public static final String UNCAUGHT_PREFIX = "There was an uncaught exception";

  private static final int MAX_STACK_CHARS = 8000;
  private static final int MAX_FRAMES = 32;
  private static final int MAX_CAUSES = 8;

  private final long startNanos = System.nanoTime();

  private long nextSeq = 1L;
  private long sentSeq;

  private final Object lock = new Object();
  private final Line[] lines = new Line[MAX_LINES];
  private final FlixelLogSink sink = this::onLog;

  private int head;
  private int count;
  private int clears;

  private boolean attached;

  /** Creates the log feed. It does not receive messages until {@link #attach()} is called. */
  public FlixelDebugLogs() {
    super("logs", FlixelDebugProtocol.KIND_LOGS);
  }

  /**
   * Starts receiving messages from {@link Flixel#log}. Calling it again while attached does nothing.
   */
  public void attach() {
    if (!attached) {
      attached = true;
      Flixel.log.addSink(sink);
    }
  }

  /** Stops receiving messages from {@link Flixel#log}. Calling it while detached does nothing. */
  public void detach() {
    if (attached) {
      attached = false;
      Flixel.log.removeSink(sink);
    }
  }

  /** Removes every stored line. */
  public void clear() {
    synchronized (lock) {
      count = 0;
      sentSeq = nextSeq - 1L;
      clears++;
    }
  }

  /**
   * Shows every stored line that is newer than a sequence number to a visitor, oldest first.
   *
   * <p>A panel keeps the sequence number of the newest line it has seen and passes it back next time, so it only
   * receives new lines. The text arguments are reused buffers that are only valid during the call. Logging from other
   * threads waits while the visitor runs, so keep it quick. Nothing is allocated.
   *
   * @param afterSeq The newest sequence number already seen, or {@code 0} for every stored line.
   * @param visitor Receives each line.
   * @return The sequence number of the newest stored line, to pass in next time.
   */
  public long visitSince(long afterSeq, @NotNull LineVisitor visitor) {
    synchronized (lock) {
      long newest = nextSeq - 1L;
      int n = (int) Math.max(0L, Math.min(count, newest - afterSeq));
      int start = head - n;
      if (start < 0) {
        start += MAX_LINES;
      }
      for (int i = 0; i < n; i++) {
        Line line = lines[(start + i) % MAX_LINES];
        visitor.visit(line.seq, line.time, line.level, line.uncaught, line.tag, line.callSite, line.message,
            line.stack);
      }
      return newest;
    }
  }

  /**
   * Returns whether lines arrived since the last time they were written.
   *
   * @return {@code true} when {@link #write(FlixelDebugMessageWriter)} would write at least one line.
   */
  public boolean hasNewLines() {
    synchronized (lock) {
      return nextSeq - 1L > sentSeq;
    }
  }

  /**
   * Writes the lines that arrived since the previous write, then remembers that they were sent.
   *
   * <p>The payload has the backlog flag set to {@code false}. If the feed dropped older lines before they
   * could be sent, they are simply missing.
   *
   * @param writer The writer positioned right after the feed index.
   */
  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    synchronized (lock) {
      writeLines(writer, false, sentSeq);
    }
  }

  /**
   * Writes every stored line, oldest first, with the backlog flag set to {@code true}.
   *
   * <p>The agent uses it for the first message after the app subscribes. It also counts as a write, so the next
   * call to {@link #write(FlixelDebugMessageWriter)} only sends lines that arrive afterwards.
   *
   * @param writer The writer positioned right after the feed index.
   */
  @Override
  public void writeFirst(@NotNull FlixelDebugMessageWriter writer) {
    synchronized (lock) {
      writeLines(writer, true, 0L);
    }
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    if (action == FlixelDebugProtocol.LOGS_CLEAR) {
      clear();
    }
  }

  @Override
  public void reset() {
    clear();
  }

  private void writeLines(FlixelDebugMessageWriter writer, boolean backlog, long afterSeq) {
    long newest = nextSeq - 1L;
    int n = (int) Math.min(count, newest - afterSeq);
    writer.writeBool(backlog);
    writer.beginArray();
    int start = head - n;
    if (start < 0) {
      start += MAX_LINES;
    }
    for (int i = 0; i < n; i++) {
      Line line = lines[(start + i) % MAX_LINES];
      writer.writeI64(line.seq)
          .writeF64(line.time)
          .writeU8(line.level)
          .writeBool(line.uncaught)
          .writeString(line.tag)
          .writeString(line.callSite)
          .writeString(line.message)
          .writeString(line.stack);
    }
    writer.endArray(n);
    sentSeq = newest;
  }

  private void onLog(FlixelLogEntry entry) {
    synchronized (lock) {
      Line line = lines[head];
      if (line == null) {
        line = new Line();
        lines[head] = line;
      }
      line.seq = nextSeq++;
      line.time = (System.nanoTime() - startNanos) / 1_000_000_000.0;
      line.level = entry.getLevel().getSeverity();

      line.tag.clear();
      line.tag.concat(entry.getTag());

      line.message.clear();
      CharSequence text = entry.getMessage();
      int length = text.length();
      if (length > FlixelDebugProtocol.MAX_LOG_MESSAGE_CHARS) {
        for (int i = 0; i < FlixelDebugProtocol.MAX_LOG_MESSAGE_CHARS; i++) {
          line.message.concat(text.charAt(i));
        }
        line.message.concat(CUT_OFF);
      } else {
        line.message.concat(text);
      }
      line.uncaught = startsWith(text, UNCAUGHT_PREFIX);

      line.callSite.clear();
      FlixelLogSite site = entry.site();
      if (site.getLine() > 0) {
        line.callSite.concat(site.getFileName());
        line.callSite.concat(':');
        line.callSite.concat(site.getLine());
      }

      line.stack.clear();
      Throwable thrown = entry.getThrowable();
      if (thrown != null) {
        appendStack(line.stack, thrown);
      }

      head = (head + 1) % MAX_LINES;
      if (count < MAX_LINES) {
        count++;
      }
      markChanged();
    }
  }

  private static boolean startsWith(CharSequence text, String prefix) {
    int n = prefix.length();
    if (text.length() < n) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      if (text.charAt(i) != prefix.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private static void appendStack(FlixelString out, Throwable thrown) {
    Throwable current = thrown;
    for (int depth = 0; current != null && depth < MAX_CAUSES; depth++) {
      if (depth > 0) {
        out.concat("Caused by: ");
      }
      out.concat(current.getClass().getName());
      String message = current.getMessage();
      if (message != null) {
        out.concat(": ");
        out.concat(message);
      }
      out.concat('\n');
      StackTraceElement[] trace = current.getStackTrace();
      int frames = Math.min(trace.length, MAX_FRAMES);
      for (int i = 0; i < frames; i++) {
        if (out.length() > MAX_STACK_CHARS) {
          out.concat("...");
          return;
        }
        StackTraceElement element = trace[i];
        out.concat("\tat ");
        out.concat(element.getClassName());
        out.concat('.');
        out.concat(element.getMethodName());
        out.concat('(');
        String file = element.getFileName();
        out.concat(file != null ? file : "Unknown Source");
        if (element.getLineNumber() > 0) {
          out.concat(':');
          out.concat(element.getLineNumber());
        }
        out.concat(')');
        out.concat('\n');
      }
      if (trace.length > frames) {
        out.concat("\t... ");
        out.concat(trace.length - frames);
        out.concat(" more\n");
      }
      Throwable cause = current.getCause();
      current = cause == current ? null : cause;
    }
  }

  /**
   * Returns the sink that fills this feed, so it can be attached to a logger other than
   * {@link Flixel#log}.
   *
   * @return The sink, never {@code null}.
   */
  @NotNull
  public FlixelLogSink getSink() {
    return sink;
  }

  /**
   * Returns how many lines are stored right now.
   *
   * @return A count from 0 to {@value #MAX_LINES}.
   */
  public int getLineCount() {
    synchronized (lock) {
      return count;
    }
  }

  /**
   * Returns the sequence number of the newest line, or 0 when no line was ever stored.
   *
   * @return The newest sequence number.
   */
  public long getNewestSeq() {
    synchronized (lock) {
      return nextSeq - 1L;
    }
  }

  /**
   * Returns how many times the lines were cleared, so a panel can notice a clear that someone else made.
   *
   * @return The clear count.
   */
  public int getClears() {
    synchronized (lock) {
      return clears;
    }
  }

  public boolean isAttached() {
    return attached;
  }

  /**
   * Receives the stored lines of a {@link FlixelDebugLogs} feed, one call per line.
   */
  @FunctionalInterface
  public interface LineVisitor {

    /**
     * Receives one line. The text arguments are reused buffers that are only valid during the call.
     *
     * @param seq The line's sequence number, increasing by one per message.
     * @param time Seconds since the game started.
     * @param level The level: {@link FlixelDebugProtocol#LEVEL_DEBUG} up to {@link FlixelDebugProtocol#LEVEL_ERROR}.
     * @param uncaught Whether the line reports an uncaught exception.
     * @param tag The logger's tag.
     * @param callSite The file and line that logged it, such as {@code Main.java:10}, or empty.
     * @param message The message.
     * @param stack The stack trace, or empty.
     */
    void visit(long seq, double time, int level, boolean uncaught, @NotNull CharSequence tag,
        @NotNull CharSequence callSite, @NotNull CharSequence message, @NotNull CharSequence stack);
  }

  private static final class Line {

    long seq;
    double time;

    final FlixelString tag = new FlixelString(32);
    final FlixelString callSite = new FlixelString(32);
    final FlixelString message = new FlixelString(192);
    final FlixelString stack = new FlixelString(0);

    int level;

    boolean uncaught;
  }
}
