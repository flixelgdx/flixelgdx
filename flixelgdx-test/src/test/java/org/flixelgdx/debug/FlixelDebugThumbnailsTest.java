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
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.debug.ReadbackTestSupport.FakeTexture;
import org.flixelgdx.debug.ReadbackTestSupport.ReadbackGraphics;
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugThumbnailsTest {

  private FlixelLogger originalLog;
  private FlixelGraphicsManager originalGraphics;
  private ReadbackGraphics graphics;
  private FlixelDebugThumbnails thumbnails;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    originalGraphics = Flixel.graphics;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    graphics = new ReadbackGraphics();
    Flixel.graphics = graphics;
    thumbnails = new FlixelDebugThumbnails();
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
    Flixel.graphics = originalGraphics;
  }

  @Test
  void aFinishedRequestBecomesAPngPayloadShrunkToTheMaximumSide() throws IOException {
    FakeTexture texture = new FakeTexture(600, 300);
    assertTrue(thumbnails.request(42, texture, 0, 0, 600, 300));
    assertFalse(thumbnails.hasPending(), "the pixels have not arrived yet");
    assertEquals(0, graphics.lastX);
    assertEquals(600, graphics.lastWidth);

    graphics.answer(256 - (600 * 4) % 256);

    assertTrue(thumbnails.hasPending());
    FlixelDebugMessageReader r = write(FlixelDebugProtocol.TEXTURES_THUMBNAIL);
    assertEquals(FlixelDebugProtocol.TEXTURES_THUMBNAIL, r.readU8());
    assertEquals(42, r.readI32());
    assertEquals(256, r.readI32());
    assertEquals(128, r.readI32());
    byte[] png = new byte[r.readBytesLength()];
    r.readBytes(png, 0, png.length);
    assertEquals(0, r.getRemaining());
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
    assertEquals(256, image.getWidth());
    assertEquals(128, image.getHeight());
    assertEquals(0xFF, image.getRGB(0, 0) >>> 24);
    assertFalse(thumbnails.hasPending(), "a payload is sent once");
  }

  @Test
  void aSmallTextureKeepsItsSize() throws IOException {
    thumbnails.request(1, new FakeTexture(16, 8), 0, 0, 16, 8);
    graphics.answer(0);
    FlixelDebugMessageReader r = write(FlixelDebugProtocol.INSPECTOR_THUMBNAIL);
    r.readU8();
    r.readI32();
    assertEquals(16, r.readI32());
    assertEquals(8, r.readI32());
  }

  @Test
  void aRequestForTheSameIdIsIgnoredWhileItIsOpenAndAfterItIsReady() {
    FakeTexture texture = new FakeTexture(8, 8);
    assertTrue(thumbnails.request(5, texture, 0, 0, 8, 8));
    assertFalse(thumbnails.request(5, texture, 0, 0, 8, 8));
    graphics.answer(0);
    assertFalse(thumbnails.request(5, texture, 0, 0, 8, 8));
    assertEquals(1, graphics.requests);
    write(1);
    assertTrue(thumbnails.request(5, texture, 0, 0, 8, 8), "after it was sent a new request is fine");
  }

  @Test
  void onlyAFewRequestsCanBeOpenAtOnce() {
    FakeTexture texture = new FakeTexture(8, 8);
    for (int i = 0; i < FlixelDebugThumbnails.MAX_PENDING; i++) {
      assertTrue(thumbnails.request(100 + i, texture, 0, 0, 8, 8));
    }
    assertFalse(thumbnails.request(999, texture, 0, 0, 8, 8));
    assertEquals(FlixelDebugThumbnails.MAX_PENDING, graphics.requests, "the extra request reads nothing");
    assertEmpty(write(FlixelDebugProtocol.TEXTURES_THUMBNAIL), 999);
  }

  @Test
  void aFailedReadIsAnsweredWithAnEmptyThumbnailAndFreesTheSlot() {
    FakeTexture texture = new FakeTexture(8, 8);
    thumbnails.request(7, texture, 0, 0, 8, 8);
    graphics.fail();
    assertEmpty(write(FlixelDebugProtocol.TEXTURES_THUMBNAIL), 7);
    assertFalse(thumbnails.hasPending());
    assertTrue(thumbnails.request(7, texture, 0, 0, 8, 8));
  }

  @Test
  void aFailedPreviewSendsNothing() {
    thumbnails.requestPreview(7, new FakeTexture(8, 8), 0, 0, 8, 8);
    graphics.fail();
    assertFalse(thumbnails.hasPending(), "previews are only for the listener");
  }

  @Test
  void aBackendThatCannotReadPixelsAnswersWithAnEmptyThumbnail() {
    graphics.supported = false;
    assertFalse(thumbnails.request(1, new FakeTexture(8, 8), 0, 0, 8, 8));
    assertEquals(0, graphics.requests);
    assertEmpty(write(FlixelDebugProtocol.TEXTURES_THUMBNAIL), 1);
    graphics.supported = true;
    assertFalse(thumbnails.request(2, new FakeTexture(8, 8), 0, 0, 0, 8), "an empty area is rejected too");
    assertEmpty(write(FlixelDebugProtocol.TEXTURES_THUMBNAIL), 2);
    assertFalse(thumbnails.hasPending());
  }

  @Test
  void emptyAnswersCannotPileUp() {
    graphics.supported = false;
    for (int i = 0; i < 100; i++) {
      thumbnails.reject(i);
    }
    int sent = 0;
    while (thumbnails.hasPending()) {
      write(FlixelDebugProtocol.TEXTURES_THUMBNAIL);
      sent++;
    }
    assertTrue(sent < 100, "extra empty answers are dropped");
  }

  @Test
  void clearDropsWaitingAndOpenRequests() {
    FakeTexture texture = new FakeTexture(8, 8);
    thumbnails.request(1, texture, 0, 0, 8, 8);
    graphics.answer(0);
    thumbnails.request(2, texture, 0, 0, 8, 8);
    thumbnails.clear();
    assertFalse(thumbnails.hasPending());

    graphics.answer(0);
    assertFalse(thumbnails.hasPending(), "a read that finishes after clear() is thrown away");
  }

  @Test
  void theInspectorRequestsTheFrameRegionOfTheSelectedSprite() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    FlixelDebugInspector inspector = new FlixelDebugInspector(ids);
    FakeTexture atlas = new FakeTexture(512, 512);
    FlixelSprite sprite = new FlixelSprite();
    sprite.setRegion(new FlixelFrame(atlas, 64, 32, 40, 20));
    inspector.select(sprite);

    assertTrue(InspectorTestSupport.state(inspector).hasTexture());
    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_REQUEST_THUMBNAIL,
        w -> w.writeI32(ids.idOf(sprite)));

    assertEquals(1, graphics.requests);
    assertEquals(atlas, graphics.lastTexture);
    assertEquals(64, graphics.lastX);
    assertEquals(32, graphics.lastY);
    assertEquals(40, graphics.lastWidth);
    assertEquals(20, graphics.lastHeight);

    graphics.answer(0);
    assertTrue(inspector.hasPendingPayload());
    FlixelDebugMessageWriter writer = new FlixelDebugMessageWriter(64);
    writer.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA);
    writer.writeU16(3);
    inspector.writePendingPayload(writer);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(writer.finish());
    r.readU8();
    r.readU16();
    assertEquals(FlixelDebugProtocol.INSPECTOR_THUMBNAIL, r.readU8());
    assertEquals(ids.idOf(sprite), r.readI32());
    assertEquals(40, r.readI32());
    assertEquals(20, r.readI32());
    assertFalse(inspector.hasPendingPayload());
  }

  @Test
  void theInspectorOnlyOffersThumbnailsWhenThePixelsCanBeRead() {
    FlixelDebugInspector inspector = new FlixelDebugInspector(new FlixelDebugObjectIds());
    FlixelSprite sprite = new FlixelSprite();
    sprite.setRegion(new FlixelFrame(new FakeTexture(8, 8)));
    inspector.select(sprite);
    assertTrue(InspectorTestSupport.state(inspector).hasTexture());

    graphics.supported = false;
    assertFalse(InspectorTestSupport.state(inspector).hasTexture());
  }

  @Test
  void aThumbnailRequestForAnObjectWithoutAFrameOrAnUnknownIdGetsAnEmptyAnswer() {
    FlixelDebugObjectIds ids = new FlixelDebugObjectIds();
    FlixelDebugInspector inspector = new FlixelDebugInspector(ids);
    FlixelSprite plain = new FlixelSprite();
    inspector.select(plain);
    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_REQUEST_THUMBNAIL, w -> w.writeI32(ids.idOf(plain)));
    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_REQUEST_THUMBNAIL, w -> w.writeI32(9999));
    assertEquals(0, graphics.requests);
    assertEmpty(writePending(inspector), ids.idOf(plain));
    assertEmpty(writePending(inspector), 9999);
    assertFalse(inspector.hasPendingPayload());
  }

  private static FlixelDebugMessageReader writePending(FlixelDebugInspector inspector) {
    assertTrue(inspector.hasPendingPayload());
    FlixelDebugMessageWriter writer = new FlixelDebugMessageWriter(64);
    writer.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA);
    writer.writeU16(3);
    inspector.writePendingPayload(writer);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(writer.finish());
    r.readU8();
    r.readU16();
    return r;
  }

  private static void assertEmpty(FlixelDebugMessageReader r, int id) {
    r.readU8();
    assertEquals(id, r.readI32());
    assertEquals(0, r.readI32(), "an empty thumbnail has no width");
    assertEquals(0, r.readI32(), "an empty thumbnail has no height");
    assertEquals(0, r.readBytesLength(), "an empty thumbnail has no PNG");
    assertEquals(0, r.getRemaining());
  }

  private FlixelDebugMessageReader write(int subtype) {
    FlixelDebugMessageWriter writer = new FlixelDebugMessageWriter(1024);
    writer.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA);
    writer.writeU16(3);
    thumbnails.write(writer, subtype);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(writer.finish());
    r.readU8();
    r.readU16();
    return r;
  }
}
