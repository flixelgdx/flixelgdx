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
package org.flixelgdx.backend.desktop.logging;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelCharArray;
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogFileSink;
import org.flixelgdx.logging.FlixelLogFormat;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Writes log lines to a timestamped {@code flixel-yyyy-MM-dd_HH-mm-ss.log} file on a background thread.
 *
 * <p>Think of this sink as a mail room with a fixed number of pigeonholes. The game thread drops each
 * formatted line into the next free pigeonhole (a reused {@link FlixelString}, so no new text objects
 * are created per line) and goes straight back to the game. A daemon thread named
 * {@code FlixelGDX Log Thread} empties the pigeonholes into one file that stays open for the whole
 * session. If the game logs faster than the disk can keep up and every pigeonhole is full, new lines
 * are dropped instead of growing memory without limit, and the file gets a line such as
 * {@code [12 log lines were dropped because the queue was full]} right before the lines that follow.
 *
 * <p>The file is flushed whenever the queue drains, right after an ERROR line, and when the sink is
 * closed. Input and output errors stop file logging quietly, so a full disk never crashes the game.
 *
 * <p>When the logs folder is pruned, only files whose names start with {@code flixel-} and end with
 * {@code .log} are ever deleted. Other files in the same folder are never touched.
 *
 * <p>Install it from the desktop launcher and then start logging to a file:
 * <pre>{@code
 * Flixel.log.setFileSink(new FlixelJvmLogFileSink());
 * Flixel.log.startFileLogging();
 * }</pre>
 *
 * <p>This class is for JVM platforms. It is not meant for the web, where threads and the host file
 * system are not available.
 */
public class FlixelJvmLogFileSink implements FlixelLogFileSink {

  /** The number of lines that can wait for the writer thread when no capacity is given. */
  public static final int DEFAULT_CAPACITY = 1024;

  private static final long JOIN_MS = 5000L;
  private static final long WAIT_MS = 200L;

  private static final int SLOT_CHARS = 192;

  private static final String FILE_PREFIX = "flixel-";
  private static final String FILE_SUFFIX = ".log";

  private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private long droppedTotal;

  private final int capacity;
  private int head;
  private int count;
  private int pendingDropped;
  private final FlixelString[] slots;
  private final int[] dropsBefore;
  private final boolean[] flushAfter;
  private final Object lock = new Object();
  private final FlixelString marker = new FlixelString(80);
  private String logFilePath;
  private Thread writerThread;

  private boolean closing;
  private volatile boolean open;

  /** Creates a sink with room for {@link #DEFAULT_CAPACITY} queued lines. */
  public FlixelJvmLogFileSink() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates a sink with room for the given number of queued lines.
   *
   * @param capacity The number of lines that can wait for the writer thread. Values below 1 are
   *   raised to 1. When all slots are in use, new lines are dropped.
   */
  public FlixelJvmLogFileSink(int capacity) {
    this.capacity = Math.max(1, capacity);
    slots = new FlixelString[this.capacity];
    for (int i = 0; i < this.capacity; i++) {
      slots[i] = new FlixelString(SLOT_CHARS);
    }
    dropsBefore = new int[this.capacity];
    flushAfter = new boolean[this.capacity];
  }

  @Override
  public synchronized void open(@Nullable String logsFolderPath, int maxLogFiles) {
    if (open) {
      return;
    }

    String resolvedPath = (logsFolderPath != null) ? logsFolderPath : getDefaultLogsFolderPath();
    if (resolvedPath == null) {
      return;
    }

    File logsFolder = new File(resolvedPath);
    logsFolder.mkdirs();
    pruneOldLogFiles(logsFolder, Math.max(1, maxLogFiles));

    File logFile = new File(logsFolder, FILE_PREFIX + LocalDateTime.now().format(FILE_DATE_FORMAT) + FILE_SUFFIX);
    BufferedWriter writer;
    try {
      writer = Files.newBufferedWriter(
          logFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException | RuntimeException e) {
      return;
    }

    synchronized (lock) {
      head = 0;
      count = 0;
      pendingDropped = 0;
      droppedTotal = 0L;
      closing = false;
      open = true;
    }
    logFilePath = logFile.getAbsolutePath();

    Thread thread = new Thread(() -> runWriter(writer), "FlixelGDX Log Thread");
    thread.setDaemon(true);
    writerThread = thread;
    thread.start();
  }

  @Override
  public synchronized void close() {
    Thread thread;
    synchronized (lock) {
      open = false;
      closing = true;
      lock.notifyAll();
      thread = writerThread;
      writerThread = null;
    }
    if (thread != null && thread.isAlive()) {
      try {
        thread.join(JOIN_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void write(@NotNull FlixelLogEntry entry) {
    synchronized (lock) {
      if (!open) {
        return;
      }
      if (count == capacity) {
        droppedTotal++;
        pendingDropped++;
        return;
      }
      int index = (head + count) % capacity;
      FlixelString slot = slots[index];
      slot.clear();
      FlixelLogFormat.appendDetailed(slot, entry);
      Throwable t = entry.getThrowable();
      if (t != null) {
        FlixelLogFormat.appendThrowable(slot, t);
      }
      dropsBefore[index] = pendingDropped;
      pendingDropped = 0;
      flushAfter[index] = entry.getLevel() == FlixelLogLevel.ERROR;
      count++;
      lock.notifyAll();
    }
  }

  @Override
  public boolean isOpen() {
    return open;
  }

  @Override
  @Nullable
  public String getDefaultLogsFolderPath() {
    return Flixel.runtime.getDefaultLogsFolderPath();
  }

  /**
   * Returns the absolute path of the log file that is open now, or that was opened last.
   *
   * @return The path, or {@code null} if no file has been opened yet.
   */
  @Nullable
  public String getLogFilePath() {
    return logFilePath;
  }

  /**
   * Returns how many lines can wait for the writer thread at once.
   *
   * @return The number of queue slots.
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Returns how many lines were dropped because the queue was full since the file was last opened.
   *
   * @return The number of dropped lines.
   */
  public long getDroppedCount() {
    synchronized (lock) {
      return droppedTotal;
    }
  }

  /**
   * Deletes the oldest {@code flixel-*.log} files so that, once the new file is created, at most
   * {@code maxLogFiles} of them remain. Files with any other name are never deleted.
   */
  private static void pruneOldLogFiles(File logsFolder, int maxLogFiles) {
    File[] existing = logsFolder.listFiles(
        (dir, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
    if (existing == null || existing.length < maxLogFiles) {
      return;
    }
    Arrays.sort(existing);
    int toDelete = existing.length - maxLogFiles + 1;
    for (int i = 0; i < toDelete; i++) {
      existing[i].delete();
    }
  }

  /**
   * The body of the writer thread. It moves queued lines into the file until the sink is closed and
   * the queue is empty, or until an input or output error happens.
   */
  private void runWriter(BufferedWriter writer) {
    try {
      while (true) {
        int start;
        int n;
        int trailingDrops = 0;
        synchronized (lock) {
          while (count == 0 && !closing) {
            try {
              lock.wait(WAIT_MS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              closing = true;
            }
          }
          start = head;
          n = count;
          if (n == 0) {
            trailingDrops = pendingDropped;
            pendingDropped = 0;
          }
        }

        if (n == 0) {
          if (trailingDrops > 0) {
            writeMarker(writer, trailingDrops);
          }
          break;
        }

        // Slots from start to start + n are owned by this thread until head moves past them, so
        // they can be read here without holding the lock.
        for (int i = 0; i < n; i++) {
          int index = (start + i) % capacity;
          int dropped = dropsBefore[index];
          if (dropped > 0) {
            writeMarker(writer, dropped);
          }
          FlixelCharArray chars = slots[index].charBuffer();
          writer.write(chars.getItems(), 0, chars.getSize());
          writer.write('\n');
          if (flushAfter[index]) {
            writer.flush();
          }
        }
        writer.flush();

        synchronized (lock) {
          head = (head + n) % capacity;
          count -= n;
        }
      }
    } catch (IOException | RuntimeException e) {
      // Stop quietly if the file becomes inaccessible. Logging must never crash the game.
      synchronized (lock) {
        open = false;
        closing = true;
        count = 0;
      }
    } finally {
      try {
        writer.close();
      } catch (IOException ignored) {
        // Nothing else can be done with a file that will not close.
      }
    }
  }

  private void writeMarker(BufferedWriter writer, int dropped) throws IOException {
    marker.clear();
    marker.concat('[');
    marker.concat(dropped);
    marker.concat(" log lines were dropped because the queue was full]");
    marker.concat('\n');
    FlixelCharArray chars = marker.charBuffer();
    writer.write(chars.getItems(), 0, chars.getSize());
  }
}
