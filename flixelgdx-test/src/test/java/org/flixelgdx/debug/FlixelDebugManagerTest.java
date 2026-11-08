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

import org.flixelgdx.debug.feeds.FlixelDebugFeed;
import org.flixelgdx.debug.panels.FlixelDebugDefaultPanels;
import org.flixelgdx.debug.panels.FlixelDebugSavePanel;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugPanels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugManagerTest {

  private FlixelDebugManager manager;

  @BeforeEach
  void setUp() {
    manager = new FlixelDebugManager();
  }

  @Test
  void feedsAreListedInProtocolOrder() {
    String[] ids = {
        "stats", "logs", "watches", "trackers", "textures", "controls", "console", "scene", "inspector", "save",
        "viewport", "capture", "input" };
    int[] kinds = {
        FlixelDebugProtocol.KIND_STATS, FlixelDebugProtocol.KIND_LOGS, FlixelDebugProtocol.KIND_WATCHES,
        FlixelDebugProtocol.KIND_TRACKERS, FlixelDebugProtocol.KIND_TEXTURES, FlixelDebugProtocol.KIND_CONTROLS,
        FlixelDebugProtocol.KIND_CONSOLE, FlixelDebugProtocol.KIND_SCENE, FlixelDebugProtocol.KIND_INSPECTOR,
        FlixelDebugProtocol.KIND_SAVE, FlixelDebugProtocol.KIND_VIEWPORT, FlixelDebugProtocol.KIND_CAPTURE,
        FlixelDebugProtocol.KIND_INPUT };
    assertEquals(ids.length, manager.getFeedCount());
    for (int i = 0; i < ids.length; i++) {
      FlixelDebugFeed feed = manager.getFeed(i);
      assertEquals(ids[i], feed.getId());
      assertEquals(kinds[i], feed.getKind());
      assertSame(feed, manager.getFeed(ids[i]));
    }
    assertSame(manager.stats, manager.getFeed("stats"));
    assertSame(manager.input, manager.getFeed("input"));
    assertNull(manager.getFeed("nope"));
  }

  @Test
  void builtInPanelsAreOrdinaryRegisteredPanels() {
    String[] ids = {
        FlixelDebugDefaultPanels.STATS, FlixelDebugDefaultPanels.PERFORMANCE, FlixelDebugDefaultPanels.LOGS,
        FlixelDebugDefaultPanels.SCENE, FlixelDebugDefaultPanels.VIEWPORT, FlixelDebugDefaultPanels.INSPECTOR,
        FlixelDebugDefaultPanels.TEXTURES, FlixelDebugDefaultPanels.SAVE, FlixelDebugDefaultPanels.WATCHES,
        FlixelDebugDefaultPanels.CONTROLS, FlixelDebugDefaultPanels.TRACKERS, FlixelDebugDefaultPanels.CONSOLE };
    FlixelDebugPanels panels = manager.panels;
    assertEquals(ids.length, panels.getCount());
    for (int i = 0; i < ids.length; i++) {
      FlixelDebugPanel panel = panels.get(i);
      assertEquals(ids[i], panel.getId());
      assertSame(panels, panel.getOwner());
      assertSame(manager, panel.getManager());
      assertFalse(panel.isOpen(), "panels start closed");
      assertTrue(panel.getContent() != null, ids[i] + " has content");
    }
    assertTrue(panels.get(FlixelDebugDefaultPanels.SAVE) instanceof FlixelDebugSavePanel);
  }

  @Test
  void aBuiltInPanelCanBeReplacedInPlace() {
    FlixelDebugPanels panels = manager.panels;
    int index = panels.indexOf(FlixelDebugDefaultPanels.SAVE);
    FlixelDebugPanel old = panels.get(FlixelDebugDefaultPanels.SAVE);
    long revision = panels.getRevision();
    FlixelDebugPanel mine = new FlixelDebugPanel(FlixelDebugDefaultPanels.SAVE, "My save", new FlixelDebugLabel("hi"));
    assertSame(mine, panels.replace(mine));
    assertEquals(index, panels.indexOf(FlixelDebugDefaultPanels.SAVE));
    assertSame(mine, panels.get(FlixelDebugDefaultPanels.SAVE));
    assertNull(old.getOwner());
    assertNotEquals(revision, panels.getRevision());
  }

  @Test
  void panelsCanBeAddedFoundRemovedAndCleared() {
    FlixelDebugPanels panels = manager.panels;
    FlixelDebugPanel mine = panels.register(new FlixelDebugPanel("game.enemies", "Enemies"));
    assertSame(mine, panels.get("game.enemies"));
    assertSame(mine, panels.get("game.enemies", FlixelDebugPanel.class));
    assertNull(panels.get("game.enemies", FlixelDebugSavePanel.class));
    assertThrows(IllegalArgumentException.class, () -> panels.register(new FlixelDebugPanel("game.enemies", "X")));

    assertSame(mine, panels.unregister("game.enemies"));
    assertNull(panels.get("game.enemies"));
    assertNull(panels.unregister("game.enemies"));

    assertTrue(panels.unregister(FlixelDebugDefaultPanels.TRACKERS) != null);
    assertEquals(-1, panels.indexOf(FlixelDebugDefaultPanels.TRACKERS));

    panels.clear();
    assertEquals(0, panels.getCount());
    FlixelDebugDefaultPanels.addTo(manager);
    assertEquals(12, panels.getCount());
  }

  @Test
  void addingDefaultsKeepsAGamesOwnPanelWithABuiltInId() {
    FlixelDebugPanels panels = manager.panels;
    panels.clear();
    FlixelDebugPanel mine = panels.register(new FlixelDebugPanel(FlixelDebugDefaultPanels.LOGS, "My logs"));
    FlixelDebugDefaultPanels.addTo(manager);
    assertSame(mine, panels.get(FlixelDebugDefaultPanels.LOGS));
    assertEquals(12, panels.getCount());
  }

  @Test
  void movingAPanelChangesTheListOrder() {
    FlixelDebugPanels panels = manager.panels;
    assertTrue(panels.move(FlixelDebugDefaultPanels.CONSOLE, 0));
    assertEquals(FlixelDebugDefaultPanels.CONSOLE, panels.get(0).getId());
    assertFalse(panels.move("nope", 0));
  }

  @Test
  void updateReachesOnlyEnabledFeedsAndOpenPanels() {
    CountingPanel open = manager.panels.register(new CountingPanel("open"));
    CountingPanel closed = manager.panels.register(new CountingPanel("closed"));
    CountingPanel always = manager.panels.register(new CountingPanel("always"));
    always.setUpdatedWhileClosed(true);
    open.setOpen(true);
    manager.watches.setEnabled(false);
    manager.update(0.016f);
    assertEquals(1, open.updates);
    assertEquals(0, closed.updates);
    assertEquals(1, always.updates);
  }

  @Test
  void aPanelThatThrowsDoesNotStopTheOthers() {
    FlixelDebugPanel broken = manager.panels.register(new FlixelDebugPanel("broken", "Broken") {
      @Override
      public void update(float elapsed) {
        throw new IllegalStateException("boom");
      }
    });
    CountingPanel after = manager.panels.register(new CountingPanel("after"));
    broken.setOpen(true);
    after.setOpen(true);
    manager.update(0.016f);
    assertEquals(1, after.updates);
  }

  @Test
  void disposeResetsEveryFeedAndClosesEveryPanel() {
    FlixelDebugPanel panel = manager.panels.get(0);
    panel.setOpen(true);
    manager.watches.addInt("x", () -> 1);
    manager.dispose();
    assertEquals(0, manager.watches.getCount());
    assertFalse(panel.isOpen());
  }

  private static final class CountingPanel extends FlixelDebugPanel {

    int updates;

    CountingPanel(String id) {
      super(id, id);
    }

    @Override
    public void update(float elapsed) {
      super.update(elapsed);
      updates++;
    }
  }
}
