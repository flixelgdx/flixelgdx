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
import org.flixelgdx.debug.InspectorTestSupport.Custom;
import org.flixelgdx.debug.panels.FlixelDebugCustomPanel;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugCustomPanelTest {

  private FlixelLogger originalLog;
  private List<FlixelLogLevel> levels;

  private int enemies = 5;
  private float aggression = 0.5f;
  private boolean paused;
  private String label = "boss";
  private int kills;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    levels = new ArrayList<>();
    log.addSink(entry -> levels.add(entry.getLevel()));
    Flixel.log = log;
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
  }

  private FlixelDebugCustomPanel panel() {
    return new FlixelDebugCustomPanel("ai", "Enemy AI", p -> p
        .intProp("Active enemies", () -> enemies)
        .floatProp("Aggression", () -> aggression, v -> aggression = v)
        .boolProp("Paused", () -> paused, v -> paused = v)
        .stringProp("Label", out -> out.concat(label), v -> label = v.toString())
        .action("Kill all", () -> kills++)
        .action("Reset", () -> enemies = 0));
  }

  @Test
  void writesRowsThenButtons() {
    FlixelDebugCustomPanel panel = panel();

    Custom custom = InspectorTestSupport.custom(panel);

    assertEquals(FlixelDebugProtocol.KIND_CUSTOM, panel.getKind());
    assertEquals("ai", panel.getId());
    assertEquals("Enemy AI", panel.getDisplayName());
    assertEquals(4, custom.rows().size());
    assertEquals("Active enemies", custom.rows().get(0).name());
    assertEquals(FlixelDebugProtocol.VALUE_INT, custom.rows().get(0).kind());
    assertEquals("5", custom.rows().get(0).value());
    assertFalse(custom.rows().get(0).editable());
    assertEquals("0.5", custom.rows().get(1).value());
    assertTrue(custom.rows().get(1).editable());
    assertEquals(FlixelDebugProtocol.VALUE_BOOL, custom.rows().get(2).kind());
    assertEquals("boss", custom.rows().get(3).value());
    assertEquals(List.of("Kill all", "Reset"), custom.buttons());
    assertEquals(List.of(0, 1), custom.buttonIndices());
  }

  @Test
  void rowsReadLiveValues() {
    FlixelDebugCustomPanel panel = panel();
    enemies = 11;
    assertEquals("11", InspectorTestSupport.custom(panel).rows().get(0).value());
  }

  @Test
  void pressRunsTheButtonWithThatIndex() {
    FlixelDebugCustomPanel panel = panel();

    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_PRESS, w -> w.writeU16(0));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_PRESS, w -> w.writeU16(0));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_PRESS, w -> w.writeU16(1));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_PRESS, w -> w.writeU16(77));

    assertEquals(2, kills);
    assertEquals(0, enemies);
  }

  @Test
  void aButtonThatThrowsIsLoggedAndDoesNotEscape() {
    FlixelDebugCustomPanel panel = new FlixelDebugCustomPanel("x", "X", p -> p.action("Boom", () -> {
      throw new IllegalStateException("boom");
    }));

    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_PRESS, w -> w.writeU16(0));

    assertEquals(List.of(FlixelLogLevel.ERROR), levels);
  }

  @Test
  void setPropertyUsesTheRowIndexAndConvertsByKind() {
    FlixelDebugCustomPanel panel = panel();

    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(1).writeValue(0.25f));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(2).writeValue(true));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(3).writeValue("minion"));
    assertEquals(0.25f, aggression);
    assertTrue(paused);
    assertEquals("minion", label);

    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(1).writeValue(2));
    assertEquals(2f, aggression, "any numeric kind is converted");
    assertTrue(levels.isEmpty());
  }

  @Test
  void setPropertyIgnoresMismatchesReadOnlyRowsAndBadIndices() {
    FlixelDebugCustomPanel panel = panel();

    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(2).writeValue(1));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(0).writeValue(100));
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, w -> w.writeU16(50).writeValue(100));

    assertFalse(paused);
    assertEquals(5, enemies);
    assertEquals(1, levels.size(), "only the kind mismatch is warned about");
  }

  @Test
  void aGetterThatThrowsBecomesAnErrorRow() {
    FlixelDebugCustomPanel panel = new FlixelDebugCustomPanel("x", "X", p -> p
        .intProp("Bad", () -> {
          throw new IllegalStateException("boom");
        })
        .intProp("Good", () -> 1));

    Custom custom = InspectorTestSupport.custom(panel);

    assertEquals(FlixelDebugProtocol.VALUE_ERROR, custom.rows().get(0).kind());
    assertEquals("boom", custom.rows().get(0).value());
    assertEquals("1", custom.rows().get(1).value());
  }

  @Test
  void onceRowsAreReadAgainAfterRefresh() {
    int[] reads = new int[1];
    FlixelDebugCustomPanel panel =
        new FlixelDebugCustomPanel("x", "X", p -> p.intProp("Once", () -> ++reads[0]).once());

    InspectorTestSupport.custom(panel);
    InspectorTestSupport.custom(panel);
    assertEquals(1, reads[0]);

    panel.refresh();
    assertEquals("2", InspectorTestSupport.custom(panel).rows().get(0).value());
  }

  @Test
  void changesBumpTheRevision() {
    FlixelDebugCustomPanel panel = panel();
    long before = panel.getRevision();
    panel.update(0.5f);
    assertTrue(panel.getRevision() > before);

    before = panel.getRevision();
    DebugTestSupport.send(panel, FlixelDebugProtocol.CUSTOM_PRESS, w -> w.writeU16(0));
    assertTrue(panel.getRevision() > before);
  }

  @Test
  void registersOnTheManagerAndRowsCanBeAddedLater() {
    FlixelDebugManager manager = new FlixelDebugManager();
    FlixelDebugCustomPanel panel = manager.register(new FlixelDebugCustomPanel("late", "Late"));
    assertSame(panel, manager.get("late", FlixelDebugCustomPanel.class));
    assertEquals(0, panel.getBuilder().getRowCount());

    panel.getBuilder().intProp("Added", () -> 3).action("Go", () -> kills++);

    Custom custom = InspectorTestSupport.custom(panel);
    assertEquals(1, custom.rows().size());
    assertEquals(1, custom.buttons().size());
  }
}
