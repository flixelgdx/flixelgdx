/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.jvm.logging;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import me.stringdotjar.flixelgdx.logging.FlixelLogFileHandler;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * JVM implementation of {@link FlixelLogFileHandler} that writes log lines to a
 * timestamped {@code .log} file on a dedicated daemon thread.
 *
 * <p>Log lines are enqueued via {@link #write(String)} and drained by the
 * background thread, keeping game-thread latency to a minimum. The queue is bounded (default 20,000
 * lines). When it is full, new lines are dropped so a log storm cannot grow the heap without limit.
 * On {@link #stop()}, the thread is given up to five seconds to flush remaining
 * lines before it is interrupted.
 *
 * <p>This handler is intended for desktop and other JVM-based backends (LWJGL3,
 * Android, iOS). It should <strong>not</strong> be used on TeaVM/web, where
 * threading and host-filesystem access are unavailable.
 *
 * @see FlixelLogFileHandler
 */
public class FlixelJvmLogFileHandler implements FlixelLogFileHandler {

  private static final int MAX_QUEUED_LINES = 20_000;

  private static final DateTimeFormatter FILE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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

    String resolvedPath = (logsFolderPath != null) ? logsFolderPath : getDefaultLogsFolderPath();
    if (resolvedPath == null || Gdx.files == null) {
      return;
    }

    FileHandle logsFolder = Gdx.files.absolute(resolvedPath);
    logsFolder.mkdirs();

    pruneOldLogFiles(logsFolder, maxLogFiles);

    String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
    FileHandle logFile = Gdx.files.absolute(resolvedPath + "/flixel-" + timestamp + ".log");

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
            logFile.writeString(line + "\n", true);
            continue;
          }
          if (shutdownRequested) {
            break;
          }
        }
        String rest;
        while ((rest = logQueue.poll()) != null) {
          logFile.writeString(rest + "\n", true);
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
    if (!active || logLine == null) {
      return;
    }
    if (!logQueue.offer(logLine)) {
      // The queue is full, so we drop the log line.
    }
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  @Nullable
  public String getDefaultLogsFolderPath() {
    return FlixelRuntimeUtil.getDefaultLogsFolderPath();
  }

  /**
   * Deletes the oldest log files when the folder already contains at least
   * {@code maxLogFiles} entries, making room for the new file.
   *
   * @param logsFolder The directory containing log files.
   * @param maxLogFiles The maximum number of files to retain.
   */
  private static void pruneOldLogFiles(FileHandle logsFolder, int maxLogFiles) {
    FileHandle[] existing = logsFolder.list();
    if (existing == null || existing.length < maxLogFiles) {
      return;
    }
    Arrays.sort(existing, Comparator.comparing(FileHandle::name));
    int toDelete = existing.length - maxLogFiles + 1;
    for (int i = 0; i < toDelete; i++) {
      existing[i].delete();
    }
  }
}
