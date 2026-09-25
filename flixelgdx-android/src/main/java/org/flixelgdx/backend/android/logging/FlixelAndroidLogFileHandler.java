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

import android.util.Log;
import org.flixelgdx.Flixel;
import org.flixelgdx.logging.FlixelLogFileHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Android {@link FlixelLogFileHandler} that writes log lines to a timestamped file and to Logcat.
 *
 * <p>Log lines are enqueued via {@link #write(String)} and drained by a background daemon thread,
 * keeping the GL thread unblocked. The queue is bounded (20,000 lines); overflow is dropped rather
 * than growing the heap. On {@link #stop()}, the thread is given five seconds to flush before it
 * is interrupted. All lines are also forwarded to {@link Log#i} so they appear in Logcat.
 */
public class FlixelAndroidLogFileHandler implements FlixelLogFileHandler {

  private static final int MAX_QUEUED_LINES = 20_000;
  private static final String LOGCAT_TAG = "FlixelGDX";

  /** Date format for the log file name, e.g. {@code flixel-2026-09-24_12-00-00.log}. */
  private static final String FILE_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

  private final BlockingQueue<String> logQueue = new ArrayBlockingQueue<>(MAX_QUEUED_LINES);
  private final Object queueLock = new Object();
  private volatile boolean shutdownRequested = false;
  private volatile boolean active = false;
  private Thread writerThread;

  @Override
  public void start(@Nullable String logsFolderPath, int maxLogFiles) {
    if (active) {
      return;
    }
    String resolvedPath = logsFolderPath != null ? logsFolderPath : getDefaultLogsFolderPath();
    if (resolvedPath == null) {
      return;
    }
    File logsFolder = new File(resolvedPath);
    logsFolder.mkdirs();
    pruneOldLogFiles(logsFolder, maxLogFiles);
    String timestamp = new SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(new Date());
    File logFile = new File(logsFolder, "flixel-" + timestamp + ".log");
    shutdownRequested = false;
    active = true;
    writerThread = new Thread(() -> {
      try {
        while (true) {
          String line;
          try {
            line = logQueue.poll(200, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
          if (line != null) {
            appendLine(logFile, line);
            continue;
          }
          if (shutdownRequested) {
            break;
          }
        }
        String rest;
        while ((rest = logQueue.poll()) != null) {
          appendLine(logFile, rest);
        }
      } catch (Exception ignored) {
        // Silently stop if the file becomes inaccessible.
      }
    });
    writerThread.setName("FlixelGDX Log Thread");
    writerThread.setDaemon(true);
    writerThread.start();
  }

  @Override
  public void stop() {
    if (!active) {
      return;
    }
    active = false;
    synchronized (queueLock) {
      shutdownRequested = true;
      queueLock.notify();
    }
    if (writerThread != null && writerThread.isAlive()) {
      try {
        writerThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      writerThread = null;
    }
  }

  @Override
  public void write(@NotNull String logLine) {
    if (!active) {
      return;
    }
    // Mirror to Logcat so developers can see logs in Android Studio without pulling files.
    Log.i(LOGCAT_TAG, logLine);
    logQueue.offer(logLine);
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  @Nullable
  public String getDefaultLogsFolderPath() {
    return Flixel.runtime.getDefaultLogsFolderPath();
  }

  private static void pruneOldLogFiles(File logsFolder, int maxLogFiles) {
    File[] existing = logsFolder.listFiles();
    if (existing == null || existing.length < maxLogFiles) {
      return;
    }
    Arrays.sort(existing, Comparator.comparing(File::getName));
    int toDelete = existing.length - maxLogFiles + 1;
    for (int i = 0; i < toDelete; i++) {
      existing[i].delete();
    }
  }

  private static void appendLine(File logFile, String line) {
    try (FileWriter writer = new FileWriter(logFile, true)) {
      writer.write(line);
      writer.write('\n');
    } catch (IOException ignored) {
      // Silently drop lines if the file becomes inaccessible.
    }
  }
}
