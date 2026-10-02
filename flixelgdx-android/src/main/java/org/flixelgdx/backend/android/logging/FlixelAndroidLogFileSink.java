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
package org.flixelgdx.backend.android.logging;

import org.flixelgdx.Flixel;
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogFileSink;
import org.flixelgdx.logging.FlixelLogFormat;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * A {@link FlixelLogFileSink} that writes log lines to a timestamped file in the app's logs folder.
 *
 * <p>Think of a restaurant with a pass-through window: the game (the waiter) quickly drops each
 * finished line into a slot, and a background writer thread (the cook) carries them to the file at its
 * own pace, so a slow disk never makes a frame late.
 *
 * <p>Here is how it works:
 * <ul>
 *   <li>{@link #write(FlixelLogEntry)} formats the line with
 *       {@link FlixelLogFormat#appendDetailed(FlixelString, FlixelLogEntry)} straight into a slot of a
 *       preallocated ring, so no {@link String} is created for the line. The ring holds
 *       {@link #DEFAULT_CAPACITY} lines unless {@link #setCapacity(int)} says otherwise.</li>
 *   <li>When the ring is full, new lines are dropped (never blocking the game) and counted. The writer
 *       thread then records {@code [N log lines were dropped because the queue was full]} in the file.</li>
 *   <li>A daemon thread named {@code FlixelGDX Log Thread} copies the queued lines out while holding the
 *       lock, then does all the file input and output with the lock released. It keeps one UTF-8
 *       {@link BufferedWriter} open for the whole session and flushes it when the queue runs dry, right
 *       after a batch that contains an {@link FlixelLogLevel#ERROR}, and on {@link #close()}.</li>
 *   <li>Input and output errors never reach the game. If the file becomes unusable, the sink silently
 *       discards the remaining lines.</li>
 * </ul>
 *
 * <p>Log files are named {@code flixel-yyyy-MM-dd_HH-mm-ss.log}. When the folder already holds the
 * maximum number of such files, the oldest ones are deleted first. Only files that start with
 * {@code flixel-} and end with {@code .log} are ever deleted; every other file is left alone.
 *
 * <p>Install it from the launcher before the game starts:
 *
 * <pre>{@code
 * Flixel.log.setFileSink(new FlixelAndroidLogFileSink());
 * }</pre>
 */
public class FlixelAndroidLogFileSink implements FlixelLogFileSink {

  /** The default number of lines the queue can hold before new lines are dropped. */
  public static final int DEFAULT_CAPACITY = 1024;

  private static final long JOIN_MS = 5000L;

  private static final int SLOT_CHARS = 128;
  private static final int BATCH_CHARS = 8192;

  private static final String FILE_PREFIX = "flixel-";
  private static final String FILE_SUFFIX = ".log";
  private static final String FILE_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

  private long dropped;
  private long totalDropped;

  private int capacity = DEFAULT_CAPACITY;
  private int head;
  private int tail;
  private int count;
  private FlixelString[] slots;
  private WriterThread writer;
  private String logFilePath;
  private final Object lock = new Object();

  private volatile boolean open;
  private boolean errorPending;

  @Override
  public synchronized void open(@Nullable String logsFolderPath, int maxLogFiles) {
    if (open) {
      return;
    }
    String path = logsFolderPath != null ? logsFolderPath : getDefaultLogsFolderPath();
    if (path == null) {
      return;
    }

    BufferedWriter out;
    File file;
    try {
      File folder = new File(path);
      folder.mkdirs();
      if (!folder.isDirectory()) {
        return;
      }
      pruneOldLogs(folder, maxLogFiles);
      String stamp = new SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(new Date());
      file = new File(folder, FILE_PREFIX + stamp + FILE_SUFFIX);
      out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
    } catch (IOException | RuntimeException e) {
      return;
    }

    WriterThread w = new WriterThread(out);
    synchronized (lock) {
      if (slots == null || slots.length != capacity) {
        slots = new FlixelString[capacity];
        for (int i = 0; i < capacity; i++) {
          slots[i] = new FlixelString(SLOT_CHARS);
        }
      }
      head = 0;
      tail = 0;
      count = 0;
      dropped = 0L;
      totalDropped = 0L;
      errorPending = false;
      logFilePath = file.getAbsolutePath();
      writer = w;
      open = true;
    }
    w.start();
  }

  @Override
  public void write(@NotNull FlixelLogEntry entry) {
    if (!open) {
      return;
    }
    synchronized (lock) {
      if (!open) {
        return;
      }
      if (count == slots.length) {
        dropped++;
        totalDropped++;
        return;
      }
      FlixelString slot = slots[tail];
      slot.clear();
      FlixelLogFormat.appendDetailed(slot, entry);
      Throwable t = entry.getThrowable();
      if (t != null) {
        FlixelLogFormat.appendThrowable(slot, t);
      }
      tail++;
      if (tail == slots.length) {
        tail = 0;
      }
      count++;
      if (entry.getLevel() == FlixelLogLevel.ERROR) {
        errorPending = true;
      }
      if (count == 1) {
        lock.notifyAll();
      }
    }
  }

  @Override
  public synchronized void close() {
    WriterThread w;
    synchronized (lock) {
      if (!open) {
        return;
      }
      open = false;
      w = writer;
      lock.notifyAll();
    }
    try {
      w.join(JOIN_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    synchronized (lock) {
      // A writer that did not finish in time is told to give up the next time it looks.
      if (writer == w) {
        writer = null;
      }
    }
  }

  private static void pruneOldLogs(@NotNull File folder, int maxLogFiles) {
    File[] files = folder.listFiles((dir, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
    if (files == null) {
      return;
    }
    // The file names hold the date, so sorting by name sorts from the oldest to the newest.
    Arrays.sort(files);
    int toDelete = files.length - Math.max(maxLogFiles, 1) + 1;
    for (int i = 0; i < toDelete; i++) {
      if (files[i].isFile()) {
        files[i].delete();
      }
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
   * Returns the absolute path of the file that the current (or most recent) session writes to.
   *
   * @return The path, or {@code null} if the sink has never been opened.
   */
  @Nullable
  public String getLogFilePath() {
    synchronized (lock) {
      return logFilePath;
    }
  }

  /**
   * Returns how many lines the queue can hold before new lines are dropped.
   *
   * @return The queue capacity in lines.
   */
  public int getCapacity() {
    synchronized (lock) {
      return capacity;
    }
  }

  /**
   * Sets how many lines the queue can hold before new lines are dropped.
   *
   * <p>The queue is allocated when the sink is opened, so a new capacity only takes effect the next time
   * {@link #open(String, int)} runs. Every slot is allocated up front, so keep the number reasonable.
   *
   * @param capacity The queue capacity in lines. Values below {@code 1} become {@code 1}.
   */
  public void setCapacity(int capacity) {
    synchronized (lock) {
      this.capacity = Math.max(capacity, 1);
    }
  }

  /**
   * Returns how many lines were dropped because the queue was full during the current (or most recent)
   * session.
   *
   * @return The number of dropped lines.
   */
  public long getDroppedCount() {
    synchronized (lock) {
      return totalDropped;
    }
  }

  /**
   * The background thread that moves queued lines from the ring into the log file.
   *
   * <p>It owns the {@link BufferedWriter} and its own scratch buffers, so a thread that is slow to stop
   * can never interfere with the buffers of a sink that has been opened again.
   */
  private final class WriterThread extends Thread {

    private char[] batch = new char[BATCH_CHARS];
    private final BufferedWriter out;
    private final FlixelString note = new FlixelString(64);

    WriterThread(@NotNull BufferedWriter out) {
      super("FlixelGDX Log Thread");
      this.out = out;
      setDaemon(true);
    }

    @Override
    public void run() {
      boolean dirty = false;
      boolean failed = false;
      while (true) {
        int n = 0;
        long lost = 0L;
        boolean flushNow = false;
        boolean stop;
        synchronized (lock) {
          boolean interrupted = false;
          try {
            while (count == 0 && dropped == 0L && open && writer == this && !dirty) {
              lock.wait();
            }
          } catch (InterruptedException e) {
            interrupted = true;
            open = false;
          }
          boolean current = writer == this;
          stop = interrupted || !open || !current;
          if (current) {
            n = takeLines();
            lost = dropped;
            dropped = 0L;
            flushNow = errorPending;
            errorPending = false;
          }
        }

        if (!failed) {
          try {
            if (n > 0) {
              out.write(batch, 0, n);
              dirty = true;
            }
            if (lost > 0L) {
              note.clear();
              note.concat('[');
              note.concat(lost);
              note.concat(" log lines were dropped because the queue was full]");
              note.concat('\n');
              out.write(note.charBuffer().getItems(), 0, note.length());
              dirty = true;
            }
            if (flushNow || stop || (n == 0 && lost == 0L && dirty)) {
              out.flush();
              dirty = false;
            }
          } catch (IOException | RuntimeException e) {
            failed = true;
            dirty = false;
          }
        }
        if (stop) {
          break;
        }
      }
      try {
        out.close();
      } catch (IOException | RuntimeException ignored) {
        // Nothing more can be done if the file cannot be closed.
      }
    }

    /**
     * Copies every queued line into {@link #batch}, each followed by a line break, and empties the
     * ring. The caller must hold the sink's lock.
     *
     * @return The number of characters in {@link #batch}.
     */
    private int takeLines() {
      int cap = slots.length;
      int total = 0;
      for (int i = 0, idx = head; i < count; i++) {
        total += slots[idx].length() + 1;
        idx++;
        if (idx == cap) {
          idx = 0;
        }
      }
      if (batch.length < total) {
        batch = new char[Math.max(total, batch.length * 2)];
      }
      int n = 0;
      for (int i = 0; i < count; i++) {
        FlixelString slot = slots[head];
        int len = slot.length();
        System.arraycopy(slot.charBuffer().getItems(), 0, batch, n, len);
        n += len;
        batch[n++] = '\n';
        head++;
        if (head == cap) {
          head = 0;
        }
      }
      count = 0;
      return n;
    }
  }
}
