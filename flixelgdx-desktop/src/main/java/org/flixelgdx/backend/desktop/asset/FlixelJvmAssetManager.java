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
package org.flixelgdx.backend.jvm.asset;

import org.flixelgdx.asset.FlixelBaseAssetManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The shared JVM asset manager: the {@link FlixelBaseAssetManager} pipeline with stage-one
 * loads running on background worker threads.
 *
 * <p>Desktop and Android both use this implementation. Queued loads (file reads, image and
 * audio decoding) run on a small daemon thread pool; the main thread finishes each asset
 * (GPU uploads, cache registration) inside {@link #update()}, so main-thread-only systems are
 * never touched from a worker.
 *
 * <p>Platforms without threads (web) provide their own manager instead; nothing here is
 * required by the interface contract.
 */
public class FlixelJvmAssetManager extends FlixelBaseAssetManager {

  /** Worker count is capped: asset loading is I/O-bound and two workers keep the queue moving. */
  private static final int MAX_WORKERS = 2;

  @NotNull
  private final ExecutorService workers;

  /**
   * Creates a manager with its own daemon worker pool.
   */
  public FlixelJvmAssetManager() {
    ThreadFactory factory = new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(@NotNull Runnable runnable) {
        Thread thread = new Thread(runnable, "flixel-asset-loader-" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      }
    };
    workers = Executors.newFixedThreadPool(Math.min(MAX_WORKERS, Runtime.getRuntime().availableProcessors()), factory);
  }

  @Override
  protected void submitLoad(@NotNull PendingLoad task) {
    workers.execute(task::run);
  }

  @Override
  protected void waitForLoads() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    workers.shutdownNow();
  }
}
