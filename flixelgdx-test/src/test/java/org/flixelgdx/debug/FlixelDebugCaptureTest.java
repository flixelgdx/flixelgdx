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
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.debug.feeds.FlixelDebugCapture;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelNoopGraphicsManager;
import org.flixelgdx.graphics.FlixelPixelReadback;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugCaptureTest {

  private FlixelLogger originalLog;
  private FlixelGraphicsManager originalGraphics;
  private CaptureGraphics graphics;
  private FlixelDebugCapture panel;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    originalGraphics = Flixel.graphics;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    graphics = new CaptureGraphics();
    Flixel.graphics = graphics;
    panel = new FlixelDebugCapture();
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
    Flixel.graphics = originalGraphics;
  }

  @Test
  void hasTheCaptureKindAndId() {
    assertEquals("capture", panel.getId());
    assertEquals(FlixelDebugProtocol.KIND_CAPTURE, panel.getKind());
  }

  @Test
  void theStateSaysWhetherCaptureIsSupportedAndBumpsTheRevisionWhenItChanges() {
    panel.update(0.016f);
    FlixelDebugMessageReader r = DebugTestSupport.payload(panel);
    assertEquals(FlixelDebugProtocol.CAPTURE_STATE, r.readU8());
    assertTrue(r.readBool());
    assertEquals(0, r.getRemaining());

    long revision = panel.getRevision();
    graphics.supported = false;
    panel.update(0.016f);
    assertTrue(panel.getRevision() > revision);
    r = DebugTestSupport.payload(panel);
    r.readU8();
    assertFalse(r.readBool());
  }

  @Test
  void aFinishedFrameBecomesAnOpaquePngShrunkToTheRequestedSide() throws IOException {
    panel.update(0.016f);
    assertTrue(panel.request(7, 100));
    assertEquals(1, graphics.requests);
    assertFalse(panel.hasPendingPayload(), "the pixels have not arrived yet");

    graphics.answer(640, 360, 16);

    assertTrue(panel.hasPendingPayload());
    FlixelDebugMessageReader r = pending();
    assertEquals(FlixelDebugProtocol.CAPTURE_IMAGE, r.readU8());
    assertEquals(7, r.readI32());
    assertEquals(100, r.readI32(), "the longest side is shrunk to the requested size, clamped to 64 at least");
    assertEquals(56, r.readI32());
    byte[] png = new byte[r.readBytesLength()];
    r.readBytes(png, 0, png.length);
    assertEquals(0, r.getRemaining());
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
    assertEquals(100, image.getWidth());
    assertEquals(56, image.getHeight());
    assertEquals(0xFF, image.getRGB(0, 0) >>> 24, "transparent pixels are written opaque");
    assertFalse(panel.hasPendingPayload(), "a payload is sent once");
  }

  @Test
  void theRequestedSideIsClamped() {
    panel.request(1, 5);
    graphics.answer(640, 360, 0);
    FlixelDebugMessageReader r = pending();
    r.readU8();
    r.readI32();
    assertEquals(64, r.readI32());

    panel.request(2, 60000);
    graphics.answer(4000, 1000, 0);
    r = pending();
    r.readU8();
    r.readI32();
    assertEquals(2048, r.readI32());
  }

  @Test
  void onlyOneRequestIsOpenAtATime() {
    assertTrue(panel.request(1, 256));
    assertFalse(panel.request(2, 256), "the second one is dropped");
    assertEquals(1, graphics.requests);
    graphics.answer(32, 32, 0);
    assertFalse(panel.request(3, 256), "the picture was not sent yet");
    pending();
    assertTrue(panel.request(4, 256), "the chair is free again once the picture was sent");
  }

  @Test
  void aFailedCaptureSendsNothingAndFreesTheRequest() {
    panel.request(1, 256);
    graphics.fail();
    assertFalse(panel.hasPendingPayload());
    assertTrue(panel.request(2, 256));
  }

  @Test
  void aBackendThatCannotCaptureIgnoresRequests() {
    graphics.supported = false;
    assertFalse(panel.request(1, 256));
    assertEquals(0, graphics.requests);
  }

  @Test
  void aRequestThatTakesTooLongIsDroppedAndALateAnswerIsIgnored() {
    panel.request(1, 256);
    panel.update(FlixelDebugCapture.TIMEOUT_SECONDS + 1f);
    assertTrue(panel.request(2, 256));
    FlixelPixelReadback old = graphics.callbacks.get(0);
    old.onPixels(ByteBuffer.allocate(32 * 32 * 4), 32, 32, 128);
    assertFalse(panel.hasPendingPayload(), "the answer of request 1 does not answer request 2");
    graphics.answer(32, 32, 0);
    assertTrue(panel.hasPendingPayload());
  }

  @Test
  void anAlmostSquareHugePictureIsShrunkToStayUnderTheMessageLimit() {
    int side = FlixelDebugCapture.fit(4096, 4096, FlixelDebugProtocol.CAPTURE_MAX_SIDE);
    assertTrue(side < FlixelDebugProtocol.CAPTURE_MAX_SIDE);
    assertTrue((long) side * side * 4 < FlixelDebugCapture.MAX_IMAGE_BYTES);
    assertEquals(2048, FlixelDebugCapture.fit(1920, 1080, 2048));
  }

  @Test
  void actionsFromTheAppAreDecoded() {
    DebugTestSupport.send(panel, FlixelDebugProtocol.CAPTURE_REQUEST, w -> w.writeI32(9).writeU16(300));
    assertEquals(1, graphics.requests);
    DebugTestSupport.send(panel, 77, w -> w.writeI32(1));
    assertEquals(1, graphics.requests);
  }

  private FlixelDebugMessageReader pending() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA);
    w.writeU16(7);
    panel.writePendingPayload(w);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    r.readU16();
    return r;
  }

  /** A graphics manager whose frame capture is answered by the test, when it decides to. */
  private static final class CaptureGraphics implements FlixelGraphicsManager {

    final List<FlixelPixelReadback> callbacks = new ArrayList<>();
    int requests;
    boolean supported = true;

    @Override
    public FlixelList<FlixelDisplayMode> getDisplayModes() {
      return FlixelNoopGraphicsManager.INSTANCE.getDisplayModes();
    }

    @Override
    public boolean supportsCapture() {
      return supported;
    }

    @Override
    public void captureScene(FlixelPixelReadback callback) {
      requests++;
      callbacks.add(callback);
    }

    /** Answers the latest request with transparent pixels of the given size. */
    void answer(int width, int height, int padding) {
      int rowBytes = width * 4 + padding;
      FlixelPixelReadback callback = callbacks.get(callbacks.size() - 1);
      callback.onPixels(ByteBuffer.allocate(rowBytes * height), width, height, rowBytes);
    }

    void fail() {
      callbacks.get(callbacks.size() - 1).onPixels(null, 0, 0, 0);
    }
  }
}
