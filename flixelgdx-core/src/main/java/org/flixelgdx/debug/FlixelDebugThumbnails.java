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
 * {@link FlixelDebugPanel#writePendingPayload(FlixelDebugMessageWriter)}.
 *
 * <p>Pixels come from {@link FlixelGraphicsManager#readPixels}. A backend that cannot read textures (see
 * {@link FlixelGraphicsManager#supportsReadPixels()}) makes every request quietly do nothing, and the app should
 * not offer thumbnails. Requests only happen when a person asks for one, so this class allocates freely when a
 * request runs and never in the frame loop.
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelPngEncoder
 */
public final class FlixelDebugThumbnails {

  /** The most pixels a thumbnail has on its longest side. */
  public static final int MAX_SIDE = 256;

  /** The most requests that can wait or be unsent at once; extra requests are dropped. */
  public static final int MAX_PENDING = 8;

  private final FlixelPngEncoder encoder = new FlixelPngEncoder();
  private final FlixelArray<Print> ready = new FlixelArray<>();
  private final FlixelIntArray inFlight = new FlixelIntArray();

  /** Creates an empty queue. */
  public FlixelDebugThumbnails() {}

  /**
   * Asks for a thumbnail of part of a texture.
   *
   * <p>Nothing happens when a thumbnail for the same id is already waiting, when too many requests are open, or
   * when the graphics backend cannot read textures.
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
    FlixelGraphicsManager graphics = Flixel.graphics;
    if (!graphics.supportsReadPixels() || width <= 0 || height <= 0) {
      return false;
    }
    if (inFlight.indexOf(id) >= 0 || findReady(id) >= 0 || inFlight.getSize() + ready.getSize() >= MAX_PENDING) {
      return false;
    }
    inFlight.add(id);
    graphics.readPixels(texture, x, y, width, height, (rgba, w, h, rowBytes) -> finish(id, rgba, w, h, rowBytes));
    return true;
  }

  /**
   * Returns whether a finished thumbnail waits to be sent.
   *
   * @return {@code true} when {@link #write(FlixelDebugMessageWriter, int)} has something to write.
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
  }

  private void finish(int id, ByteBuffer rgba, int width, int height, int rowBytes) {
    int slot = inFlight.indexOf(id);
    if (slot < 0) {
      return; // The queue was cleared while the read was running.
    }
    inFlight.removeIndex(slot);
    FlixelLogger log = Flixel.log.tagged("FlixelDebug");
    if (rgba == null) {
      log.warn("Could not read the pixels for thumbnail {}.", id);
      return;
    }
    try {
      int length = encoder.encode(rgba, width, height, rowBytes, MAX_SIDE);
      ready.add(new Print(id, encoder.getWidth(), encoder.getHeight(), Arrays.copyOf(encoder.getBytes(), length)));
    } catch (RuntimeException e) {
      log.warn("Could not make thumbnail {}: {}", id, e.getMessage());
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

  /** One finished thumbnail. */
  private record Print(int id, int width, int height, byte[] png) {
  }
}
