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
package org.flixelgdx.debug.ui;

import com.sun.management.ThreadMXBean;

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.UiTestSupport.Node;
import org.flixelgdx.debug.ui.UiTestSupport.Op;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.flixelgdx.debug.ui.UiTestSupport.event;
import static org.flixelgdx.debug.ui.UiTestSupport.opened;
import static org.flixelgdx.debug.ui.UiTestSupport.patch;
import static org.flixelgdx.debug.ui.UiTestSupport.tree;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugWidgetsTest {

  private FlixelLogger originalLog;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger quiet = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    quiet.setConsoleSink(null);
    Flixel.log = quiet;
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
  }

  @Test
  void theTreeCarriesTypesIdsNamesAndOnlyChangedProperties() {
    FlixelDebugLabel title = FlixelDebugUi.heading("Enemies").setName("title");
    FlixelDebugButton kill = FlixelDebugUi.button("Kill all", null).setConfirm("Sure?").setName("kill");
    FlixelDebugPanel panel = new FlixelDebugPanel("p", "P", FlixelDebugUi.column(title, kill).setSpacing(4f));

    Node root = tree(panel);
    assertEquals(FlixelDebugProtocol.WIDGET_STACK, root.type());
    assertEquals(4f, root.props().get(FlixelDebugProtocol.PROP_SPACING));
    assertEquals(1, root.props().size(), "defaults are not sent");
    assertEquals(2, root.children().size());

    Node label = root.child(0);
    assertEquals(FlixelDebugProtocol.WIDGET_LABEL, label.type());
    assertEquals("title", label.name());
    assertEquals(title.getId(), label.id());
    assertEquals("Enemies", label.props().get(FlixelDebugProtocol.PROP_TEXT));
    assertEquals(FlixelDebugProtocol.STYLE_HEADING, label.props().get(FlixelDebugProtocol.PROP_STYLE));

    Node button = root.child(1);
    assertEquals("Sure?", button.props().get(FlixelDebugProtocol.PROP_CONFIRM));
    assertNotEquals(label.id(), button.id());
    assertTrue(label.id() > 0 && button.id() > 0);
    assertSame(kill, panel.find("kill"));
    assertSame(kill, panel.findById(kill.getId()));
  }

  @Test
  void onlyRealChangesBecomePatchesAndTheyCoalesce() {
    FlixelDebugLabel label = new FlixelDebugLabel("a");
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", label));
    assertFalse(panel.hasChanges());

    label.setText("a");
    assertFalse(panel.hasChanges(), "the same text is no change");

    FlixelString text = new FlixelString();
    text.concat("b");
    label.setText(text);
    label.setText("c");
    label.setVisible(false);
    assertTrue(panel.hasChanges());
    List<Op> ops = patch(panel);
    assertEquals(2, ops.size());
    assertEquals(new Op(label.getId(), FlixelDebugProtocol.PATCH_SET, FlixelDebugProtocol.PROP_VISIBLE, false),
        ops.get(0));
    assertEquals(new Op(label.getId(), FlixelDebugProtocol.PATCH_SET, FlixelDebugProtocol.PROP_TEXT, "c"), ops.get(1));
    assertFalse(panel.hasChanges());
  }

  @Test
  void addingOrRemovingWidgetsSendsTheWholeTreeAgain() {
    FlixelDebugStack column = FlixelDebugUi.column();
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", column));
    assertFalse(panel.needsTree());
    FlixelDebugLabel late = new FlixelDebugLabel("late");
    column.add(late);
    assertTrue(panel.needsTree());
    assertEquals(1, tree(panel).children().size());
    assertTrue(late.getId() > 0);

    column.remove(late);
    assertTrue(panel.needsTree());
    assertEquals(0, late.getId());
    assertNull(late.getPanel());
  }

  @Test
  void aWidgetMovedToAnotherContainerLeavesItsOldOne() {
    FlixelDebugLabel label = new FlixelDebugLabel("x");
    FlixelDebugStack first = FlixelDebugUi.column(label);
    FlixelDebugStack second = FlixelDebugUi.column();
    second.add(label);
    assertEquals(0, first.getChildCount());
    assertSame(second, label.getParent());
  }

  @Test
  void containersLimitTheirChildren() {
    FlixelDebugScroll scroll = new FlixelDebugScroll(new FlixelDebugLabel("a"));
    assertThrows(IllegalStateException.class, () -> scroll.add(new FlixelDebugLabel("b")));
  }

  @Test
  void aCheckboxRunsItsSetterAndShowsTheOldValueWhenTheGameRefuses() {
    boolean[] value = { false };
    boolean[] locked = { false };
    FlixelDebugCheckbox box = FlixelDebugUi.checkbox("On", () -> value[0], v -> {
      if (!locked[0]) {
        value[0] = v;
      }
    });
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", box));

    event(panel, box, FlixelDebugProtocol.EVENT_CHANGE, true);
    assertTrue(value[0]);
    assertTrue(box.getValue());
    assertTrue(panel.isUrgent());
    assertTrue(patch(panel).isEmpty(), "the app already shows the accepted value");

    locked[0] = true;
    event(panel, box, FlixelDebugProtocol.EVENT_CHANGE, false);
    assertTrue(value[0]);
    List<Op> ops = patch(panel);
    assertEquals(1, ops.size());
    assertEquals(true, ops.get(0).value(), "the refused change is undone in the app");
  }

  @Test
  void boundWidgetsFollowTheGameEveryUpdate() {
    int[] level = { 1 };
    FlixelDebugDropdown dropdown = new FlixelDebugDropdown(() -> level[0], i -> level[0] = i)
        .setItems("Easy", "Normal", "Hard");
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", dropdown));
    level[0] = 2;
    panel.update(0.016f);
    List<Op> ops = patch(panel);
    assertEquals(1, ops.size());
    assertEquals(2, ops.get(0).value());

    event(panel, dropdown, FlixelDebugProtocol.EVENT_CHANGE, 0);
    assertEquals(0, level[0]);
    event(panel, dropdown, FlixelDebugProtocol.EVENT_CHANGE, 7);
    assertEquals(0, level[0], "an index out of range is ignored");
    assertEquals(0, patch(panel).get(0).value(), "and the app is told the real value");
  }

  @Test
  void aTextBoxSubmitsAndClearsAndReportsLiveChanges() {
    List<String> submitted = new ArrayList<>();
    List<String> changed = new ArrayList<>();
    FlixelDebugTextBox box = FlixelDebugUi.textBox("Command").setClearOnSubmit(true).setLive(true)
        .onSubmit(t -> submitted.add(t.toString())).onChange(t -> changed.add(t.toString()));
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", box));

    event(panel, box, FlixelDebugProtocol.EVENT_CHANGE, "go");
    assertEquals(List.of("go"), changed);
    assertEquals("go", box.getText().toString());
    event(panel, box, FlixelDebugProtocol.EVENT_SUBMIT, "god on");
    assertEquals(List.of("god on"), submitted);
    assertEquals("", box.getText().toString());
    assertTrue(patch(panel).isEmpty(), "the app clears the box itself");

    box.setMultiline(true);
    event(panel, box, FlixelDebugProtocol.EVENT_SUBMIT, "line");
    assertEquals(1, submitted.size(), "a multiline box never submits");
  }

  @Test
  void aNumberIsClampedToItsRange() {
    double[] speed = { 5 };
    FlixelDebugNumber number = FlixelDebugUi.number(() -> speed[0], v -> speed[0] = v).setRange(0, 10);
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", number));
    event(panel, number, FlixelDebugProtocol.EVENT_CHANGE, 25.0);
    assertEquals(10.0, speed[0]);
    assertEquals(10.0, patch(panel).get(0).value());
    event(panel, number, FlixelDebugProtocol.EVENT_CHANGE, "nope");
    assertEquals(10.0, speed[0]);
  }

  @Test
  void aButtonClickRunsItsActionAndDisabledButtonsIgnoreClicks() {
    int[] clicks = new int[1];
    FlixelDebugButton button = FlixelDebugUi.button("Go", () -> clicks[0]++);
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", button));
    event(panel, button, FlixelDebugProtocol.EVENT_CLICK);
    assertEquals(1, clicks[0]);
    button.setEnabled(false);
    event(panel, button, FlixelDebugProtocol.EVENT_CLICK);
    assertEquals(1, clicks[0]);
  }

  @Test
  void aListenerThatThrowsDoesNotEscapeThePanel() {
    FlixelDebugButton button = FlixelDebugUi.button("Boom", () -> {
      throw new IllegalStateException("boom");
    });
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", button));
    event(panel, button, FlixelDebugProtocol.EVENT_CLICK);
    assertTrue(panel.isUrgent());
  }

  @Test
  void aRebuiltTableIsOnlySentWhenItsRowsChanged() {
    FlixelDebugTable table = new FlixelDebugTable().column("Name").column("HP", 60f, 0);
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", table));

    rows(table, 12);
    List<Op> ops = patch(panel);
    assertEquals(1, ops.size());
    assertEquals(FlixelDebugProtocol.PROP_ITEMS, ops.get(0).key());
    List<?> items = (List<?>) ops.get(0).value();
    assertEquals(2, items.size());
    assertEquals(List.of(7L, 0, List.of("Slime", 12), ""), items.get(0));

    assertFalse(rows(table, 12), "the same rows are no change");
    assertFalse(panel.hasChanges());
    assertTrue(rows(table, 11));
    assertEquals(1, patch(panel).size());
    assertEquals(2, table.getRowCount());
  }

  @Test
  void appendedRowsTravelAloneAndTheLimitDropsTheOldest() {
    FlixelDebugTable table = new FlixelDebugTable().column("Line").setLimit(3);
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", table));
    for (int i = 1; i <= 2; i++) {
      table.appendRow(i, 0).cell("line " + i).detail("stack " + i);
    }
    List<Op> ops = patch(panel);
    assertEquals(1, ops.size());
    assertEquals(FlixelDebugProtocol.PATCH_APPEND, ops.get(0).code());
    assertEquals(2, ((List<?>) ops.get(0).value()).size());

    for (int i = 3; i <= 5; i++) {
      table.appendRow(i, 0).cell("line " + i);
    }
    ops = patch(panel);
    assertEquals(3, ((List<?>) ops.get(0).value()).size(), "only the new rows travel");
    assertEquals(3, table.getRowCount(), "the oldest rows are dropped");

    List<?> stored = (List<?>) tree(panel).props().get(FlixelDebugProtocol.PROP_ITEMS);
    assertEquals(3, stored.size());
    assertEquals(3L, ((List<?>) stored.get(0)).get(0));
    assertEquals(5L, ((List<?>) stored.get(2)).get(0));

    for (int i = 6; i <= 20; i++) {
      table.appendRow(i, 0).cell("line " + i);
    }
    ops = patch(panel);
    assertEquals(1, ops.size());
    assertEquals(FlixelDebugProtocol.PATCH_SET, ops.get(0).code(), "too many new rows send the whole list instead");
    assertEquals(3, ((List<?>) ops.get(0).value()).size());
  }

  @Test
  void tableEventsCarryRowIds() {
    long[] selected = { -2L };
    long[] activated = { -2L };
    long[] menu = { -2L, -2L };
    FlixelDebugTable table = new FlixelDebugTable().setMenu("Remove")
        .onSelect(id -> selected[0] = id)
        .onActivate(id -> activated[0] = id)
        .onMenu((id, item) -> {
          menu[0] = id;
          menu[1] = item;
        });
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", table));
    event(panel, table, FlixelDebugProtocol.EVENT_SELECT, 9L);
    assertEquals(9L, selected[0]);
    assertEquals(9L, table.getSelected());
    event(panel, table, FlixelDebugProtocol.EVENT_SELECT, (Object) null);
    assertEquals(-1L, selected[0]);
    event(panel, table, FlixelDebugProtocol.EVENT_ACTIVATE, 4L);
    assertEquals(4L, activated[0]);
    event(panel, table, FlixelDebugProtocol.EVENT_MENU, 4L, 0);
    assertEquals(4L, menu[0]);
    event(panel, table, FlixelDebugProtocol.EVENT_MENU, 4L, 3);
    assertEquals(0L, menu[1], "an unknown menu item is ignored");
  }

  @Test
  void treeNodesCarryParentsDetailsStylesAndBadges() {
    FlixelDebugTree tree = new FlixelDebugTree();
    FlixelDebugPanel panel = new FlixelDebugPanel("p", "P", tree);
    tree.beginNodes();
    tree.node(1, -1, "PlayState").style(FlixelDebugProtocol.STYLE_EXPANDED);
    tree.node(2, 1, "Player").detail("state.0").badge("agent", FlixelDebugProtocol.TONE_SUCCESS);
    assertTrue(tree.endNodes());
    List<?> nodes = (List<?>) tree(panel).props().get(FlixelDebugProtocol.PROP_ITEMS);
    assertEquals(List.of(1L, -1L, "PlayState", "", FlixelDebugProtocol.STYLE_EXPANDED, List.of()), nodes.get(0));
    assertEquals(List.of(2L, 1L, "Player", "state.0", 0, List.of(List.of("agent", FlixelDebugProtocol.TONE_SUCCESS))),
        nodes.get(1));
  }

  @Test
  void liveSectionsReadGettersAndApplyEdits() {
    float[] speed = { 2.5f };
    FlixelDebugProperties grid = new FlixelDebugProperties().section("Player", s -> s
        .floatProp("Speed", () -> speed[0], v -> speed[0] = v)
        .stringProp("Name", out -> out.concat("Kori")));
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", grid));
    List<?> sections = (List<?>) tree(panel).props().get(FlixelDebugProtocol.PROP_SECTIONS);
    assertEquals(List.of("Player", true, List.of(List.of("Speed", 2.5f, true), List.of("Name", "Kori", false))),
        sections.get(0));

    event(panel, grid, FlixelDebugProtocol.EVENT_EDIT, 0, 0, 4.0);
    assertEquals(4f, speed[0]);
    List<Op> ops = patch(panel);
    assertEquals(1, ops.size());
    List<?> row = (List<?>) ((List<?>) ((List<?>) ((List<?>) ops.get(0).value()).get(0)).get(2)).get(0);
    assertEquals(4f, row.get(1));
  }

  @Test
  void handWrittenSectionsReportEdits() {
    int[] edits = new int[3];
    FlixelDebugProperties grid = new FlixelDebugProperties().onEdit((section, row, value) -> {
      edits[0] = section;
      edits[1] = row;
      edits[2] = value.asInt();
    });
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", grid));
    grid.beginSections();
    grid.beginSection("A", true);
    grid.row("x", 1, true);
    grid.errorRow("broken", "boom");
    grid.endSections();
    assertEquals(1, grid.getSectionCount());
    event(panel, grid, FlixelDebugProtocol.EVENT_EDIT, 0, 0, 5);
    assertArrayEquals(new int[] { 0, 0, 5 }, edits);
  }

  @Test
  void graphSamplesAreAppendedAndKeptUpToTheLimit() {
    FlixelDebugGraph graph = new FlixelDebugGraph("Frame").series("Frame", 0, "ms").series("CPU", 0, "ms")
        .setLimit(4);
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", graph));
    graph.push(16f, 3f);
    graph.push(17f, 4f);
    List<Op> ops = patch(panel);
    assertEquals(1, ops.size());
    assertEquals(FlixelDebugProtocol.PATCH_APPEND, ops.get(0).code());
    assertEquals(List.of(List.of(16f, 3f), List.of(17f, 4f)), ops.get(0).value());

    for (int i = 0; i < 10; i++) {
      graph.push(i, i);
    }
    assertEquals(4, graph.getSampleCount());
    assertEquals(9f, graph.getLatest(0));
    List<?> samples = (List<?>) tree(panel).props().get(FlixelDebugProtocol.PROP_SAMPLES);
    assertEquals(List.of(List.of(6f, 6f), List.of(7f, 7f), List.of(8f, 8f), List.of(9f, 9f)), samples);
  }

  @Test
  void canvasGesturesReachTheListenerInViewUnits() {
    List<String> calls = new ArrayList<>();
    FlixelDebugCanvas canvas =
        new FlixelDebugCanvas().setView(320f, 180f).setListener(new FlixelDebugCanvas.Listener() {
          @Override
          public void onPick(float x, float y) {
            calls.add("pick " + x + " " + y);
          }

          @Override
          public void onDrag(long id, float dx, float dy) {
            calls.add("drag " + id + " " + dx + " " + dy);
          }

          @Override
          public void onPan(float dx, float dy) {
            calls.add("pan " + dx + " " + dy);
          }

          @Override
          public void onZoom(float factor, float x, float y) {
            calls.add("zoom " + factor + " " + x + " " + y);
          }
        });
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", canvas));
    event(panel, canvas, FlixelDebugProtocol.EVENT_PICK, 10f, 20f);
    event(panel, canvas, FlixelDebugProtocol.EVENT_DRAG, 12L, 4.5f, -2f);
    event(panel, canvas, FlixelDebugProtocol.EVENT_PAN, 1f, 2f);
    event(panel, canvas, FlixelDebugProtocol.EVENT_ZOOM, 1.25f, 160f, 90f);
    event(panel, canvas, FlixelDebugProtocol.EVENT_ZOOM, -1f, 0f, 0f);
    event(panel, canvas, FlixelDebugProtocol.EVENT_PICK, Float.NaN, 0f);
    assertEquals(List.of("pick 10.0 20.0", "drag 12 4.5 -2.0", "pan 1.0 2.0", "zoom 1.25 160.0 90.0"), calls);

    canvas.beginShapes();
    canvas.shape(12, 1f, 2f, 3f, 4f, FlixelDebugProtocol.SHAPE_SELECTED, "Player #12");
    canvas.endShapes();
    List<Op> ops = patch(panel);
    assertEquals(List.of(List.of(12L, 1f, 2f, 3f, 4f, FlixelDebugProtocol.SHAPE_SELECTED, "Player #12")),
        ops.get(0).value());
  }

  @Test
  void anImageSendsItsPictureAsBytes() {
    FlixelDebugImage image = new FlixelDebugImage();
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", image));
    byte[] png = { 1, 2, 3 };
    image.setPng(png, 0, 3);
    png[0] = 9;
    Op op = patch(panel).get(0);
    assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) op.value(), "the bytes were copied");
    image.clearPng();
    assertNull(patch(panel).get(0).value());
  }

  @Test
  void theListBufferOnlyReportsRealChanges() {
    FlixelDebugListBuffer list = new FlixelDebugListBuffer();
    list.begin().writeValue("a").writeValue(1);
    assertTrue(list.end(2));
    list.begin().writeValue("a").writeValue(1);
    assertFalse(list.end(2));
    list.begin().writeValue("a").writeValue(2);
    assertTrue(list.end(2));
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    w.beginMessage(0);
    list.write(w);
    assertEquals(1 + 1 + 2 + (1 + 2 + 1) + (1 + 4), w.getSize());
  }

  @Test
  void panelRequestsQueueUpForTheApp() {
    FlixelDebugPanels panels = new FlixelDebugPanels(null);
    FlixelDebugPanel panel = panels.register(new FlixelDebugPanel("p", "P"));
    panel.show();
    panel.hide();
    for (int i = 0; i < FlixelDebugPanels.MAX_REQUESTS + 5; i++) {
      panels.toast(FlixelDebugProtocol.TONE_NORMAL, "x");
    }
    int count = 0;
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    while (panels.hasRequest()) {
      w.beginMessage(FlixelDebugProtocol.MSG_APP_REQUEST);
      panels.writeRequest(w);
      count++;
    }
    assertEquals(FlixelDebugPanels.MAX_REQUESTS, count, "the oldest requests are dropped when too many wait");
  }

  @Test
  void rebuildingUnchangedWidgetsDoesNotAllocate() {
    FlixelDebugTable table = new FlixelDebugTable().column("Name");
    FlixelDebugLabel label = new FlixelDebugLabel();
    FlixelDebugPanel panel = opened(new FlixelDebugPanel("p", "P", FlixelDebugUi.column(table, label)));
    FlixelString text = new FlixelString(32);
    FlixelDebugMessageWriter writer = new FlixelDebugMessageWriter();
    for (int i = 0; i < 200; i++) {
      frame(table, label, text, writer, panel, i);
    }
    ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    long id = Thread.currentThread().threadId();
    long before = bean.getThreadAllocatedBytes(id);
    for (int i = 0; i < 200; i++) {
      frame(table, label, text, writer, panel, i);
    }
    long used = bean.getThreadAllocatedBytes(id) - before;
    assertTrue(used < 2048, "allocated " + used + " bytes");
  }

  private static void frame(FlixelDebugTable table, FlixelDebugLabel label, FlixelString text,
      FlixelDebugMessageWriter writer, FlixelDebugPanel panel, int i) {
    table.beginRows();
    for (int r = 0; r < 20; r++) {
      table.row(r).cell("row").cell(i & 1);
    }
    table.endRows();
    label.setText(text.setLength(0).concat("frame ").concat(i & 3));
    writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_PATCH).writeU16(0);
    panel.writePatch(writer);
    writer.finish();
  }

  private static boolean rows(FlixelDebugTable table, int hp) {
    table.beginRows();
    table.row(7).cell("Slime").cell(hp);
    table.row(9, FlixelDebugProtocol.TONE_WARNING).cell("64 x 64", 4096.0).cell(3).detail("low health");
    return table.endRows();
  }
}
