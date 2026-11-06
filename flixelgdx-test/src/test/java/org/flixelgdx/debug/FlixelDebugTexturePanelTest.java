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
import org.flixelgdx.FlixelState;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.asset.FlixelBaseAssetManager;
import org.flixelgdx.debug.ReadbackTestSupport.FakeTexture;
import org.flixelgdx.debug.ReadbackTestSupport.ReadbackGraphics;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugTexturePanelTest {

  private FlixelLogger originalLog;
  private FlixelAssetManager originalAssets;
  private FlixelGraphicsManager originalGraphics;
  private FlixelBaseAssetManager assets;
  private ReadbackGraphics graphics;
  private FlixelDebugObjectIds ids;
  private FlixelDebugTexturePanel panel;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    originalAssets = Flixel.assets;
    originalGraphics = Flixel.graphics;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    assets = new FlixelBaseAssetManager();
    Flixel.assets = assets;
    graphics = new ReadbackGraphics();
    Flixel.graphics = graphics;
    ids = new FlixelDebugObjectIds();
    panel = new FlixelDebugTexturePanel(ids);
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
    Flixel.assets = originalAssets;
    Flixel.graphics = originalGraphics;
    Flixel.state = null;
  }

  @Test
  void anEmptyPanelWritesAnEmptyListAndZeroTotals() {
    panel.refresh();
    List<Entry> entries = new ArrayList<>();
    FlixelDebugMessageReader r = list(entries);
    assertTrue(entries.isEmpty());
    assertEquals(0, r.readI32());
    assertEquals(0L, r.readI64());
    assertEquals(0, r.readI32());
    assertEquals(0, r.getRemaining());
  }

  @Test
  void everyLoadedGraphicIsListedWithItsSizeFormatAndMemory() {
    FakeTexture hero = new FakeTexture(64, 32);
    FakeTexture atlas = new FakeTexture(1024, 512, 600_000L, "BC7");
    addOwned(hero);
    addOwned(atlas);

    panel.refresh();

    List<Entry> entries = new ArrayList<>();
    FlixelDebugMessageReader r = list(entries);
    assertEquals(2, entries.size());
    Entry a = byWidth(entries, 64);
    Entry b = byWidth(entries, 1024);
    assertEquals(32, a.height);
    assertEquals("RGBA8", a.format);
    assertEquals(64L * 32 * 4, a.bytes);
    assertEquals("BC7", b.format);
    assertEquals(600_000L, b.bytes);
    assertEquals(ids.idOf(hero), a.id, "the id comes from the shared registry");
    assertTrue(a.name.startsWith("flixel://synthetic/"));
    assertFalse(a.duplicate);
    assertEquals(2, r.readI32());
    assertEquals(64L * 32 * 4 + 600_000L, r.readI64());
    assertEquals(0, r.readI32());
    assertEquals(0, r.getRemaining());
    assertEquals(2, panel.getTextureCount());
    assertEquals(64L * 32 * 4 + 600_000L, panel.getTotalBytes());
  }

  @Test
  void aTextureSharedByTwoHandlesIsListedOnce() {
    FakeTexture shared = new FakeTexture(8, 8);
    addOwned(shared);
    addOwned(shared);
    panel.refresh();
    assertEquals(1, panel.getTextureCount());
  }

  @Test
  void usersAreTheExistingSpritesShowingTheTextureIncludingThoseInGroupsAndSubStates() {
    FakeTexture sheet = new FakeTexture(16, 16);
    FakeTexture other = new FakeTexture(16, 16);
    addOwned(sheet);
    addOwned(other);
    FlixelState state = new TestState();
    state.add(sprite(sheet));
    state.add(sprite(sheet));
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    group.add(sprite(sheet));
    group.add(sprite(other));
    state.add(group);
    FlixelSprite dead = sprite(sheet);
    dead.kill();
    state.add(dead);
    state.add(new FlixelSprite());
    Flixel.state = state;

    panel.refresh();

    assertEquals(3, panel.getUsers(indexOf(sheet)));
    assertEquals(1, panel.getUsers(indexOf(other)));
  }

  @Test
  void twoTexturesLoadedFromTheSameFileAreFlaggedAsDuplicates() {
    FakeTexture first = new FakeTexture(8, 8);
    FakeTexture second = new FakeTexture(8, 8);
    FakeTexture unique = new FakeTexture(8, 8);
    assets.register(new PathGraphic(assets, "a.png", "assets/hero.png", first));
    assets.register(new PathGraphic(assets, "b.png", "assets/hero.png", second));
    assets.register(new PathGraphic(assets, "c.png", "assets/other.png", unique));

    panel.refresh();

    List<Entry> entries = new ArrayList<>();
    FlixelDebugMessageReader r = list(entries);
    assertEquals(3, entries.size());
    int flagged = 0;
    for (Entry entry : entries) {
      if (entry.duplicate) {
        flagged++;
        assertTrue(entry.name.equals("a.png") || entry.name.equals("b.png"));
      }
    }
    assertEquals(2, flagged);
    assertEquals(3, r.readI32());
    r.readI64();
    assertEquals(2, r.readI32(), "the duplicate count");
    assertEquals(2, panel.getDuplicateCount());
  }

  @Test
  void graphicsThatAreNotLoadedAreSkipped() {
    assets.register(new PathGraphic(assets, "missing.png", "missing.png", null));
    panel.refresh();
    assertEquals(0, panel.getTextureCount());
  }

  @Test
  void theListIsOnlyRebuiltAboutOnceASecondAndTheRevisionOnlyMovesWhenSomethingChanged() {
    FakeTexture texture = new FakeTexture(8, 8);
    addOwned(texture);
    panel.update(0.016f);
    assertEquals(1, panel.getTextureCount(), "the first update fills the list");
    long revision = panel.getRevision();

    addOwned(new FakeTexture(4, 4));
    panel.update(0.5f);
    assertEquals(1, panel.getTextureCount(), "half a second later nothing was rebuilt");

    panel.update(0.6f);
    assertEquals(2, panel.getTextureCount());
    assertTrue(panel.getRevision() > revision);

    long settled = panel.getRevision();
    panel.update(1.1f);
    assertEquals(settled, panel.getRevision(), "an identical list does not bump the revision");

    Flixel.state = new TestState();
    Flixel.state.add(sprite(texture));
    panel.update(1.1f);
    assertTrue(panel.getRevision() > settled, "a changed user count does");
  }

  @Test
  void idsOfTexturesThatLeftTheListAreReleased() {
    FakeTexture keep = new FakeTexture(8, 8);
    FakeTexture drop = new FakeTexture(8, 8);
    FlixelGraphic keepGraphic = addOwned(keep);
    addOwned(drop);
    panel.refresh();
    int dropId = ids.peek(drop);
    assertTrue(dropId != FlixelDebugObjectIds.NONE);

    assets.unload(keepGraphic.getPath());
    assets.clear();
    addOwned(keep);
    panel.refresh();

    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(drop));
    assertNull(ids.get(dropId));
    assertTrue(ids.peek(keep) != FlixelDebugObjectIds.NONE);
  }

  @Test
  void aThumbnailRequestByTextureIdReadsTheWholeTexture() {
    FakeTexture texture = new FakeTexture(40, 20);
    addOwned(texture);
    panel.refresh();

    DebugTestSupport.send(panel, FlixelDebugProtocol.TEXTURES_REQUEST_THUMBNAIL, w -> w.writeI32(ids.peek(texture)));

    assertEquals(1, graphics.requests);
    assertEquals(texture, graphics.lastTexture);
    assertEquals(0, graphics.lastX);
    assertEquals(40, graphics.lastWidth);
    assertEquals(20, graphics.lastHeight);
    assertFalse(panel.hasPendingPayload());

    graphics.answer(0);
    assertTrue(panel.hasPendingPayload());
    FlixelDebugMessageWriter writer = new FlixelDebugMessageWriter(64);
    writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_DATA);
    writer.writeU16(4);
    panel.writePendingPayload(writer);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(writer.finish());
    r.readU8();
    r.readU16();
    assertEquals(FlixelDebugProtocol.TEXTURES_THUMBNAIL, r.readU8());
    assertEquals(ids.peek(texture), r.readI32());
    assertEquals(40, r.readI32());
    assertEquals(20, r.readI32());
    assertTrue(r.readBytesLength() > 0);
    assertFalse(panel.hasPendingPayload());
  }

  @Test
  void aThumbnailRequestForAnUnknownIdIsIgnored() {
    panel.refresh();
    DebugTestSupport.send(panel, FlixelDebugProtocol.TEXTURES_REQUEST_THUMBNAIL, w -> w.writeI32(12345));
    assertEquals(0, graphics.requests);
    assertFalse(panel.requestThumbnail(12345));
  }

  @Test
  void resetEmptiesTheListAndReleasesTheIds() {
    FakeTexture texture = new FakeTexture(8, 8);
    addOwned(texture);
    panel.refresh();
    panel.reset();
    assertEquals(0, panel.getTextureCount());
    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(texture));
    panel.update(0.016f);
    assertEquals(1, panel.getTextureCount(), "the list fills again after a reset");
  }

  @Test
  void unknownActionsAreIgnored() {
    DebugTestSupport.send(panel, 99, w -> w.writeI32(1));
    assertEquals(0, graphics.requests);
  }

  private FlixelGraphic addOwned(FlixelTexture texture) {
    FlixelGraphic graphic = new FlixelGraphic(assets, assets.allocateSyntheticKey(), texture);
    assets.register(graphic);
    return graphic;
  }

  private int indexOf(FlixelTexture texture) {
    for (int i = 0; i < panel.getTextureCount(); i++) {
      if (panel.getTexture(i) == texture) {
        return i;
      }
    }
    throw new AssertionError("Texture is not listed.");
  }

  private static FlixelSprite sprite(FlixelTexture texture) {
    FlixelSprite sprite = new FlixelSprite();
    sprite.setRegion(new FlixelFrame(texture));
    return sprite;
  }

  private static Entry byWidth(List<Entry> entries, int width) {
    for (Entry entry : entries) {
      if (entry.width == width) {
        return entry;
      }
    }
    throw new AssertionError("No entry with width " + width);
  }

  /** Decodes the list payload into the entries and returns a reader positioned at the totals. */
  private FlixelDebugMessageReader list(List<Entry> out) {
    FlixelDebugMessageReader r = DebugTestSupport.payload(panel);
    assertEquals(FlixelDebugProtocol.TEXTURES_LIST, r.readU8());
    int count = r.readCount();
    for (int i = 0; i < count; i++) {
      Entry entry = new Entry();
      entry.id = r.readI32();
      entry.name = DebugTestSupport.str(r);
      entry.width = r.readI32();
      entry.height = r.readI32();
      entry.format = DebugTestSupport.str(r);
      entry.bytes = r.readI64();
      entry.users = r.readI32();
      entry.duplicate = r.readBool();
      out.add(entry);
    }
    return r;
  }

  private static final class Entry {

    long bytes;
    String name;
    String format;
    int id;
    int width;
    int height;
    int users;
    boolean duplicate;
  }

  /** A graphic that pretends to be loaded from a file, so the panel's duplicate check can be tested. */
  private static final class PathGraphic extends FlixelGraphic {

    private final String resolved;
    private final FlixelTexture texture;

    PathGraphic(FlixelAssetManager assets, String path, String resolved, FlixelTexture texture) {
      super(assets, path, null);
      this.resolved = resolved;
      this.texture = texture;
    }

    @Override
    public boolean isLoaded() {
      return texture != null;
    }

    @NotNull
    @Override
    public FlixelTexture getTexture() {
      if (texture == null) {
        throw new IllegalStateException("not loaded");
      }
      return texture;
    }

    @NotNull
    @Override
    public String getResolvedPath() {
      return resolved;
    }
  }

  private static final class TestState extends FlixelState {
  }
}
