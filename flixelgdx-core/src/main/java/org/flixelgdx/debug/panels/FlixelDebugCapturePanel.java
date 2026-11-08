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
package org.flixelgdx.debug.panels;

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelPixelReadback;
import org.flixelgdx.graphics.FlixelPngEncoder;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * The capture panel of the remote debugger: it takes a picture of the game's screen on request, so the debugger
 * app (and the AI agents that drive it) can see what the game looks like.
 *
 * <p>Think of a photo booth with one chair. The app sits down and asks for a picture ("at most 800 pixels on the
 * long side, please"), the booth waits for the game to finish drawing a frame, copies that frame back from the
 * graphics card (which takes a few frames), shrinks it, wraps it in a PNG, and hands the print over. Only one
 * person can sit in the chair at a time: a request that arrives while another is open is dropped.
 *
 * <h2>What the picture shows</h2>
 *
 * <p>The picture is the <strong>finished game frame</strong>: everything the cameras and the game's own overlay
 * drew, at the size the game drew it (the render resolution when one is set, otherwise the window). It is
 * captured <em>before</em> the debugger draws its own overlay, so hitbox and selection outlines are not in it.
 * Transparent pixels are written opaque. The frame is the one drawn after the request arrived, so the picture also
 * works while the game is paused.
 *
 * <p>The longest side is shrunk to at most the {@code maxSide} the app asked for, clamped to
 * {@value FlixelDebugProtocol#CAPTURE_MIN_SIDE} to {@value FlixelDebugProtocol#CAPTURE_MAX_SIDE} pixels, with
 * nearest-neighbor sampling (see {@link FlixelPngEncoder}). The PNG is not compressed, so it is as large as the raw
 * pixels. A message may not exceed 16 MB, or the connection is closed, so the panel shrinks the picture further
 * when needed to stay under {@value #MAX_IMAGE_BYTES} bytes. That only matters for an almost square picture at the
 * largest sizes, and the {@code width} and {@code height} of the answer always say what was sent.
 *
 * <h2>When there is no answer</h2>
 *
 * <p>The panel never sends an answer for a capture that failed (the graphics backend cannot capture, the read
 * failed, or it took longer than {@value #TIMEOUT_SECONDS} seconds). The app must time out. The state payload says
 * whether the graphics backend can capture at all, so the app can hide its capture button otherwise.
 *
 * <p>Requests only happen when somebody asks, so this class allocates freely when a request runs and never in the
 * frame loop.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.capture.request(7, 640);  // Ask for picture number 7, at most 640 pixels on the long side.
 * }</pre>
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelDebugProtocol#KIND_CAPTURE
 * @see FlixelGraphicsManager#captureScene(FlixelPixelReadback)
 */
public class FlixelDebugCapturePanel extends FlixelDebugPanelBase {

  /** The most bytes of PNG one answer may hold, so the message stays well under the 16 MB limit. */
  public static final int MAX_IMAGE_BYTES = 15 * 1024 * 1024;

  /** Seconds after which an open request is given up on. */
  public static final float TIMEOUT_SECONDS = 10f;

  private final FlixelPngEncoder encoder = new FlixelPngEncoder();

  private float waited;
  private int requestId;
  private int maxSide;
  private int generation;
  private int imageLength;

  private boolean open;
  private boolean ready;
  private boolean synced;
  private boolean supported;

  /** Creates the capture panel. */
  public FlixelDebugCapturePanel() {
    super("capture", "Capture", FlixelDebugProtocol.KIND_CAPTURE);
  }

  @Override
  public void update(float elapsed) {
    boolean now = Flixel.graphics.supportsCapture();
    if (!synced || now != supported) {
      synced = true;
      supported = now;
      markChanged();
    }
    if (open && !ready) {
      waited += elapsed;
      if (waited >= TIMEOUT_SECONDS) {
        // Forget the request. A late answer from the graphics backend is ignored because of the generation.
        generation++;
        open = false;
        log().warn("The capture {} timed out and was dropped.", requestId);
      }
    }
  }

  /**
   * Asks for a picture of the next finished frame.
   *
   * <p>Nothing happens when another request is still open (its picture was not sent yet) or when the graphics
   * backend cannot capture. The picture is sent later through the pending payload mechanism.
   *
   * @param requestId A number the app chooses, sent back with the picture.
   * @param maxSide The most pixels the picture may have on its longest side, clamped to
   *     {@value FlixelDebugProtocol#CAPTURE_MIN_SIDE} to {@value FlixelDebugProtocol#CAPTURE_MAX_SIDE}.
   * @return {@code true} when a capture was started.
   */
  public boolean request(int requestId, int maxSide) {
    FlixelGraphicsManager graphics = Flixel.graphics;
    if (open || !graphics.supportsCapture()) {
      return false;
    }
    this.requestId = requestId;
    this.maxSide = Math.max(FlixelDebugProtocol.CAPTURE_MIN_SIDE,
        Math.min(FlixelDebugProtocol.CAPTURE_MAX_SIDE, maxSide));
    open = true;
    ready = false;
    waited = 0f;
    int mine = ++generation;
    graphics.captureScene((rgba, width, height, rowBytes) -> finish(mine, rgba, width, height, rowBytes));
    return true;
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeU8(FlixelDebugProtocol.CAPTURE_STATE).writeBool(supported);
  }

  @Override
  public boolean hasPendingPayload() {
    return ready;
  }

  @Override
  public void writePendingPayload(@NotNull FlixelDebugMessageWriter writer) {
    if (!ready) {
      return;
    }
    writer.writeU8(FlixelDebugProtocol.CAPTURE_IMAGE)
        .writeI32(requestId)
        .writeI32(encoder.getWidth())
        .writeI32(encoder.getHeight());
    writer.writeBytes(encoder.getBytes(), 0, imageLength);
    ready = false;
    open = false;
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    if (action == FlixelDebugProtocol.CAPTURE_REQUEST) {
      int id = reader.readI32();
      request(id, reader.readU16());
    }
  }

  @Override
  public void reset() {
    generation++;
    open = false;
    ready = false;
    synced = false;
  }

  private void finish(int mine, ByteBuffer rgba, int width, int height, int rowBytes) {
    // The callback of an old request (one that timed out or was replaced) must not answer a newer one.
    if (mine != generation || !open || ready) {
      return;
    }
    if (rgba == null) {
      log().warn("Could not read the frame for capture {}.", requestId);
      open = false;
      return;
    }
    try {
      int side = fit(width, height, maxSide);
      imageLength = encoder.encode(rgba, width, height, rowBytes, side, true);
      ready = true;
    } catch (RuntimeException e) {
      log().warn("Could not make capture {}: {}", requestId, e.getMessage());
      open = false;
    }
  }

  /**
   * Returns the longest side to use so the PNG of a picture stays under {@link #MAX_IMAGE_BYTES}.
   *
   * @param width The width of the captured frame in pixels.
   * @param height The height of the captured frame in pixels.
   * @param wanted The longest side the app asked for.
   * @return {@code wanted}, or a smaller side when the PNG would be too large to send.
   */
  public static int fit(int width, int height, int wanted) {
    int side = wanted;
    int longest = Math.max(width, height);
    while (side > 1) {
      long outW = longest > side ? Math.max(1L, (long) width * side / longest) : width;
      long outH = longest > side ? Math.max(1L, (long) height * side / longest) : height;
      long raw = (outW * 4L + 1L) * outH;
      if (raw + raw / 65535L * 5L + 1024L <= MAX_IMAGE_BYTES) {
        return side;
      }
      side = side * 9 / 10;
    }
    return 1;
  }

  private static FlixelLogger log() {
    return Flixel.log.tagged("Debugger/Capture");
  }

  /**
   * Returns whether the graphics backend can capture frames, as last reported to the app.
   *
   * @return {@code true} when requests are answered.
   */
  public boolean isSupported() {
    return supported;
  }
}
