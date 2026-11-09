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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelIntArray;
import org.flixelgdx.debug.feeds.FlixelDebugFeed;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelPngEncoder;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Makes small PNG thumbnails of textures for the debugger app, one request at a time.
 *
 * <p>Think of a photo booth with a queue. The app drops a request in ("a picture of texture 12, please"), the
 * booth asks the graphics card for the pixels (which takes a few frames), shrinks the picture to at most
 * {@value #MAX_SIDE} pixels on its longest side, wraps it in a PNG, and puts the finished print in an outbox. The
 * panel hands prints to the network agent one by one through
 * {@link FlixelDebugFeed#writePendingPayload(FlixelDebugMessageWriter)}.
 *
 * <p>Pixels come from {@link FlixelGraphicsManager#readPixels}. A backend that cannot read textures (see
 * {@link FlixelGraphicsManager#supportsReadPixels()}) answers every request with an empty thumbnail, and the app
 * should not offer thumbnails. Requests only happen when a person asks for one, so this class allocates freely when
 * a request runs and never in the frame loop.
 *
 * <p>Every {@linkplain #request(int, FlixelTexture, int, int, int, int) request} gets exactly one answer, so a tool
 * waiting for it never waits for nothing. When the picture cannot be made (the backend cannot read pixels, the
 * texture is compressed, the read fails, or too many requests are open), the answer is an <strong>empty
 * thumbnail</strong>: a width and height of {@code 0} and no PNG bytes. A feed that has nothing to take a picture of
 * answers with {@link #reject(int)}.
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelPngEncoder
 */
public final class FlixelDebugThumbnails {

  /** The most pixels a thumbnail has on its longest side. */
  public static final int MAX_SIDE = 256;

  /** The most requests that can wait or be unsent at once; extra requests get an empty thumbnail. */
  public static final int MAX_PENDING = 8;

  /** The most empty answers that can wait to be sent; extra ones are dropped so a flood of requests cannot pile up. */
  private static final int MAX_EMPTY = 32;

  private static final byte[] NO_PNG = new byte[0];

  private final FlixelPngEncoder encoder = new FlixelPngEncoder();
  private final FlixelArray<Print> ready = new FlixelArray<>();
  private final FlixelIntArray inFlight = new FlixelIntArray();
  private final FlixelIntArray inFlightQueued = new FlixelIntArray();
  private Listener listener;

  /** Creates an empty queue. */
  public FlixelDebugThumbnails() {}

  /**
   * Asks for a thumbnail of part of a texture.
   *
   * <p>The request is always answered once. A request for an id whose read is already running or whose picture is
   * already waiting shares that answer. When too many requests are open, the area is empty, or the graphics backend
   * cannot read textures, the answer is an empty thumbnail (see {@link #reject(int)}).
   *
   * @param id The id to report back with the picture (a texture id or an object id).
   * @param texture The texture to read.
   * @param x Left edge of the area in pixels.
   * @param y Top edge of the area in pixels.
   * @param width Width of the area in pixels.
   * @param height Height of the area in pixels.
   * @return {@code true} when a read was started.
   */
  public boolean request(int id, @NotNull FlixelTexture texture, int x, int y, int width, int height) {
    return start(id, texture, x, y, width, height, true);
  }

  /**
   * Starts reading a thumbnail only for the {@linkplain #setListener(Listener) listener}, such as a preview in a
   * debugger panel. It is not queued for {@link #write(FlixelDebugMessageWriter, int)}.
   *
   * @param id The id the thumbnail is reported with.
   * @param texture The texture to read.
   * @param x The left edge of the region to read, in pixels.
   * @param y The top edge of the region to read, in pixels.
   * @param width The width of the region, in pixels.
   * @param height The height of the region, in pixels.
   * @return {@code true} when the read started; {@code false} when one for the same id is already running, the
   *   picture is waiting to be sent, too many are open, or the graphics backend cannot read pixels.
   */
  public boolean requestPreview(int id, @NotNull FlixelTexture texture, int x, int y, int width, int height) {
    return start(id, texture, x, y, width, height, false);
  }

  /**
   * Answers a request with an empty thumbnail: the id with a width and height of {@code 0} and no PNG bytes.
   *
   * <p>A feed calls this when it has nothing to take a picture of, for example an unknown id or an object without a
   * texture, so the app learns right away that no picture is coming instead of waiting for one. The listener is not
   * called.
   *
   * @param id The id the request asked for.
   */
  public void reject(int id) {
    int empty = 0;
    for (int i = 0, n = ready.getSize(); i < n; i++) {
      if (ready.get(i).png.length == 0) {
        empty++;
      }
    }
    if (empty < MAX_EMPTY) {
      ready.add(new Print(id, 0, 0, NO_PNG));
    }
  }

  /**
   * Returns whether a finished thumbnail is waiting to be handed out.
   *
   * @return {@code true} if at least one thumbnail is ready.
   */
  public boolean hasPending() {
    return ready.getSize() > 0;
  }

  /**
   * Writes the oldest finished thumbnail and removes it from the queue.
   *
   * <p>The payload is the subtype byte, then {@code i32 id}, {@code i32 width}, {@code i32 height}, and the PNG as
   * {@code bytes}. Does nothing when no thumbnail is waiting.
   *
   * @param writer The writer positioned right after the panel index.
   * @param subtype The {@code THUMBNAIL} subtype of the panel's kind.
   */
  public void write(@NotNull FlixelDebugMessageWriter writer, int subtype) {
    if (ready.getSize() == 0) {
      return;
    }
    Print print = ready.removeIndex(0);
    writer.writeU8(subtype).writeI32(print.id).writeI32(print.width).writeI32(print.height);
    writer.writeBytes(print.png);
  }

  /** Forgets every waiting thumbnail. Reads that are still in flight finish and are then thrown away. */
  public void clear() {
    ready.clear();
    inFlight.clear();
    inFlightQueued.clear();
  }

  private boolean start(int id, FlixelTexture texture, int x, int y, int width, int height, boolean queued) {
    FlixelGraphicsManager graphics = Flixel.graphics;
    if (!graphics.supportsReadPixels() || width <= 0 || height <= 0) {
      if (queued) {
        reject(id);
      }
      return false;
    }
    int running = inFlight.indexOf(id);
    if (running >= 0) {
      // A read for the same id is already running, so this request is dropped. The running read is still queued
      // when this request wanted that, so a tool that asks while a preview is being read gets its answer.
      if (queued) {
        inFlightQueued.set(running, 1);
      }
      return false;
    }
    if (findReady(id) >= 0) {
      return false; // The picture is already waiting to be sent, which answers this request too.
    }
    if (inFlight.getSize() + ready.getSize() >= MAX_PENDING) {
      if (queued) {
        reject(id);
      }
      return false;
    }
    inFlight.add(id);
    inFlightQueued.add(queued ? 1 : 0);
    graphics.readPixels(texture, x, y, width, height, (rgba, w, h, rowBytes) -> finish(id, rgba, w, h, rowBytes));
    return true;
  }

  private void finish(int id, ByteBuffer rgba, int width, int height, int rowBytes) {
    int slot = inFlight.indexOf(id);
    if (slot < 0) {
      return; // The queue was cleared while the read was running.
    }
    inFlight.removeIndex(slot);
    boolean queued = inFlightQueued.removeIndex(slot) != 0;
    FlixelLogger log = Flixel.log.tagged("Debugger/Thumbnails");
    if (rgba == null) {
      log.warn("Could not read the pixels for thumbnail {}.", id);
      if (queued) {
        reject(id);
      }
      return;
    }
    try {
      int length = encoder.encode(rgba, width, height, rowBytes, MAX_SIDE);
      if (listener != null) {
        listener.onThumbnail(id, encoder.getWidth(), encoder.getHeight(), encoder.getBytes(), length);
      }
      if (queued) {
        ready.add(new Print(id, encoder.getWidth(), encoder.getHeight(), Arrays.copyOf(encoder.getBytes(), length)));
      }
    } catch (RuntimeException e) {
      log.warn("Could not make thumbnail {}: {}", id, e.getMessage());
      if (queued) {
        reject(id);
      }
    }
  }

  private int findReady(int id) {
    for (int i = 0, n = ready.getSize(); i < n; i++) {
      if (ready.get(i).id == id) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the listener that hears about every finished thumbnail.
   *
   * @return The listener, or {@code null} for none.
   */
  public Listener getListener() {
    return listener;
  }

  /**
   * Sets a listener that hears about every finished thumbnail, queued or not. A debugger panel uses it to show a
   * preview.
   *
   * @param listener The listener, or {@code null} for none.
   */
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  private record Print(int id, int width, int height, byte[] png) {
  }

  /**
   * Hears about finished thumbnails.
   */
  @FunctionalInterface
  public interface Listener {

    /**
     * Receives a finished thumbnail.
     *
     * @param id The id it was requested with.
     * @param width The width of the PNG in pixels.
     * @param height The height of the PNG in pixels.
     * @param png The array that holds the PNG file; only valid during the call, so copy what you keep.
     * @param length How many bytes of the array the PNG uses.
     */
    void onThumbnail(int id, int width, int height, byte @NotNull [] png, int length);
  }
}
