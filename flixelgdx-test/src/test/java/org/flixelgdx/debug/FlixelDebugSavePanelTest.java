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
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.debug.panels.FlixelDebugSavePanel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.save.FlixelSave;
import org.flixelgdx.save.FlixelSaveHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugSavePanelTest {

  private FlixelLogger originalLog;
  private FlixelSave originalSave;
  private MemoryHandler storage;
  private FlixelSave save;
  private FlixelDebugSavePanel panel;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    originalSave = Flixel.save;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    storage = new MemoryHandler();
    save = new FlixelSave(storage);
    save.bind("Game", "slot1");
    Flixel.save = save;
    panel = new FlixelDebugSavePanel();
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
    Flixel.save = originalSave;
  }

  @Test
  void valuesAreFlattenedIntoDottedPathsWithTheirKinds() {
    FlixelMap<String, Object> player = new FlixelMap<>();
    player.put("name", "Ada \"the\" Great\n");
    player.put("coins", 120);
    player.put("xp", 5_000_000_000L);
    player.put("speed", 2.5f);
    player.put("luck", 0.25);
    player.put("alive", true);
    player.put("pets", new FlixelMap<String, Object>());
    save.data.put("player", player);
    save.data.put("achievements", new FlixelArray<Object>());
    save.data.put("nothing", null);

    Payload payload = payload();

    assertEquals(
        List.of("achievements", "nothing", "player.alive", "player.coins", "player.luck", "player.name",
            "player.pets", "player.speed", "player.xp"),
        payload.paths());
    assertEquals("[]", payload.row("achievements").display);
    assertEquals("null", payload.row("nothing").display);
    assertEquals("true", payload.row("player.alive").display);
    assertEquals(FlixelDebugProtocol.VALUE_BOOL, payload.row("player.alive").kind);
    assertEquals("120", payload.row("player.coins").display);
    assertEquals(FlixelDebugProtocol.VALUE_INT, payload.row("player.coins").kind);
    assertEquals("0.25", payload.row("player.luck").display);
    assertEquals(FlixelDebugProtocol.VALUE_DOUBLE, payload.row("player.luck").kind);
    assertEquals("\"Ada \\\"the\\\" Great\\n\"", payload.row("player.name").display);
    assertEquals(FlixelDebugProtocol.VALUE_STRING, payload.row("player.name").kind);
    assertEquals("{}", payload.row("player.pets").display);
    assertEquals("2.5", payload.row("player.speed").display);
    assertEquals(FlixelDebugProtocol.VALUE_FLOAT, payload.row("player.speed").kind);
    assertEquals("5000000000", payload.row("player.xp").display);
    assertEquals(FlixelDebugProtocol.VALUE_LONG, payload.row("player.xp").kind);
  }

  @Test
  void listElementsAreNamedByIndexAndNestedContainersAreFlattened() {
    FlixelArray<Object> items = new FlixelArray<>();
    FlixelMap<String, Object> sword = new FlixelMap<>();
    sword.put("name", "sword");
    items.add(sword);
    items.add("shield");
    save.data.put("items", items);

    assertEquals(List.of("items.0.name", "items.1"), payload().paths());
  }

  @Test
  void numericKeysSortByValueAndComeBeforeTheRestWhichSortWithoutRegardToCase() {
    for (String key : new String[] { "10", "9", "2", "banana", "Apple", "apple", "cherry", "100" }) {
      save.data.put(key, 1);
    }
    assertEquals(List.of("2", "9", "10", "100", "Apple", "apple", "banana", "cherry"), payload().paths());
  }

  @Test
  void atMostThreeHundredRowsAreSentAndTheStatusSaysSo() {
    for (int i = 0; i < 400; i++) {
      save.data.put("k" + i, i);
    }
    Payload payload = payload();
    assertEquals(300, payload.rows.size());
    assertEquals(300, panel.getRowCount());
    assertTrue(payload.status.contains("300+"), payload.status);
  }

  @Test
  void valuesThatMatchTheStoredSaveAreNotUnsaved() {
    save.data.put("coins", 7);
    save.data.put("name", "Ada");
    save.flush();

    Payload payload = payload();

    assertFalse(payload.row("coins").unsaved, "7 is stored as 7.0, which is the same number");
    assertFalse(payload.row("name").unsaved);
    assertEquals(0, panel.getUnsavedCount());
    assertTrue(payload.removed.isEmpty());
  }

  @Test
  void newAndChangedValuesAreUnsavedUntilTheyAreWritten() {
    save.data.put("coins", 7);
    save.flush();

    save.data.put("coins", 8);
    save.data.put("gems", 1);
    Payload payload = payload();

    assertTrue(payload.row("coins").unsaved);
    assertTrue(payload.row("gems").unsaved);
    assertEquals(2, panel.getUnsavedCount());

    assertTrue(panel.saveNow());
    payload = payload();
    assertFalse(payload.row("coins").unsaved);
    assertFalse(payload.row("gems").unsaved);
    assertEquals(0, panel.getUnsavedCount());
  }

  @Test
  void aSaveThatWasFlushedByTheGameStopsBeingUnsavedOnTheNextRefresh() {
    save.data.put("coins", 7);
    assertTrue(payload().row("coins").unsaved);

    save.flush();
    panel.update(1.1f);

    assertFalse(payload().row("coins").unsaved);
  }

  @Test
  void removedPathsAreListedUntilTheSaveIsWritten() {
    save.data.put("coins", 7);
    save.data.put("gems", 3);
    save.flush();

    save.data.remove("gems");
    Payload payload = payload();
    assertEquals(List.of("gems"), payload.removed);
    assertNull(payload.row("gems"));

    panel.saveNow();
    assertTrue(payload().removed.isEmpty());
  }

  @Test
  void anUnboundSaveShowsEverythingAsUnsaved() {
    FlixelSave loose = new FlixelSave(storage);
    loose.data.put("a", 1);
    panel.setSave(loose);

    Payload payload = payload();

    assertTrue(payload.row("a").unsaved);
    assertTrue(payload.status.contains("not bound"), payload.status);
    assertFalse(panel.saveNow());
    assertTrue(payload().isError);
  }

  @Test
  void withoutASaveThePanelIsEmptyAndActionsReportAnError() {
    Flixel.save = null;
    Payload payload = payload();
    assertTrue(payload.rows.isEmpty());
    assertTrue(payload.status.contains("no save"), payload.status);

    assertFalse(panel.set("a", "1"));
    assertTrue(payload().isError);
    assertFalse(panel.resetKey("a"));
    assertFalse(panel.saveNow());
    assertFalse(panel.resetAll());
  }

  @Test
  void aStringCanBeTypedWithOrWithoutQuotesAndEscapesWorkInsideQuotes() {
    save.data.put("name", "Ada");

    assertTrue(panel.set("name", "Grace"));
    assertEquals("Grace", save.data.get("name"));
    assertTrue(panel.set("name", "\"Linus\""));
    assertEquals("Linus", save.data.get("name"));
    assertTrue(panel.set("name", "\"say \\\"hi\\\"\\n\""));
    assertEquals("say \"hi\"\n", save.data.get("name"));
    assertTrue(panel.set("name", "  spaced  "));
    assertEquals("  spaced  ", save.data.get("name"), "unquoted text is kept exactly");
    assertTrue(panel.set("name", "\"\""));
    assertEquals("", save.data.get("name"));
  }

  @Test
  void aBooleanAcceptsOnlyTrueOrFalse() {
    save.data.put("music", true);

    assertTrue(panel.set("music", "false"));
    assertEquals(Boolean.FALSE, save.data.get("music"));
    assertTrue(panel.set("music", " TRUE "));
    assertEquals(Boolean.TRUE, save.data.get("music"));
    assertFalse(panel.set("music", "yes"));
    assertEquals(Boolean.TRUE, save.data.get("music"));
    assertTrue(payload().isError);
  }

  @Test
  void numbersKeepTheirTypeAndRejectWhatDoesNotFit() {
    save.data.put("i", 1);
    save.data.put("l", 1L);
    save.data.put("f", 1f);
    save.data.put("d", 1.0);

    assertTrue(panel.set("i", "42"));
    assertEquals(42, save.data.get("i"));
    assertFalse(panel.set("i", "1.5"));
    assertFalse(panel.set("i", "99999999999"));
    assertFalse(panel.set("i", "abc"));
    assertEquals(42, save.data.get("i"));

    assertTrue(panel.set("l", "99999999999"));
    assertEquals(99_999_999_999L, save.data.get("l"));

    assertTrue(panel.set("f", "2.5"));
    assertEquals(2.5f, save.data.get("f"));
    assertFalse(panel.set("f", "NaN"));

    assertTrue(panel.set("d", "-0.125"));
    assertEquals(-0.125, save.data.get("d"));
    assertFalse(panel.set("d", "Infinity"));
    assertTrue(panel.set("d", "7"));
    assertEquals(7.0, save.data.get("d"), "a decimal accepts a whole number and stays a double");
  }

  @Test
  void aSetNeedsAnExistingPathToAPlainValue() {
    FlixelMap<String, Object> player = new FlixelMap<>();
    player.put("coins", 1);
    save.data.put("player", player);
    save.data.put("nothing", null);

    assertFalse(panel.set("missing", "1"));
    assertFalse(panel.set("player.missing", "1"));
    assertFalse(panel.set("player", "1"), "a whole map cannot be overwritten with a value");
    assertFalse(panel.set("nothing", "1"), "a null has no type to keep");
    assertFalse(panel.set("player.coins.deeper", "1"));
    assertEquals(1, player.get("coins"));
    assertTrue(panel.set("player.coins", "2"));
    assertEquals(2, player.get("coins"));
  }

  @Test
  void listElementsAndKeysWithDotsCanBeEdited() {
    FlixelArray<Object> scores = new FlixelArray<>();
    scores.add(10);
    scores.add(20);
    save.data.put("scores", scores);
    FlixelMap<String, Object> files = new FlixelMap<>();
    files.put("a.txt", "x");
    save.data.put("files", files);

    assertTrue(panel.set("scores.1", "25"));
    assertEquals(25, scores.get(1));
    assertFalse(panel.set("scores.2", "1"));
    assertFalse(panel.set("scores.x", "1"));
    assertTrue(panel.set("files.a.txt", "y"));
    assertEquals("y", files.get("a.txt"));
  }

  @Test
  void pathsAndValuesLongerThanTwoHundredCharactersAreRejected() {
    save.data.put("name", "Ada");
    String longText = "x".repeat(201);
    assertFalse(panel.set("name", longText));
    assertFalse(panel.set(longText, "x"));
    assertFalse(panel.resetKey(longText));
    assertEquals("Ada", save.data.get("name"));
    assertTrue(panel.set("name", "x".repeat(200)));
  }

  @Test
  void resetKeyRemovesTheValueAndShiftsListElements() {
    FlixelMap<String, Object> player = new FlixelMap<>();
    player.put("coins", 1);
    player.put("gems", 2);
    save.data.put("player", player);
    FlixelArray<Object> list = new FlixelArray<>();
    list.add("a");
    list.add("b");
    list.add("c");
    save.data.put("list", list);

    assertTrue(panel.resetKey("player.coins"));
    assertFalse(player.containsKey("coins"));
    assertEquals(2, player.get("gems"));
    assertTrue(panel.resetKey("list.0"));
    assertEquals(2, list.getSize());
    assertEquals("b", list.get(0));
    assertTrue(panel.resetKey("player"), "a whole branch can be removed too");
    assertFalse(save.data.containsKey("player"));
    assertFalse(panel.resetKey("player"));
    assertFalse(panel.resetKey("nope.nope"));
  }

  @Test
  void saveNowWritesToStorageAndReportsFailure() {
    save.data.put("coins", 5);
    assertTrue(panel.saveNow());
    assertTrue(storage.files.get("Game.slot1").contains("\"coins\":5"));
    assertFalse(payload().isError);
    assertTrue(payload().status.contains("Saved"));

    storage.failWrites = true;
    assertFalse(panel.saveNow());
    Payload payload = payload();
    assertTrue(payload.isError);
    assertTrue(payload.status.contains("Could not write"), payload.status);
  }

  @Test
  void resetAllErasesTheDataAndTheStoredSave() {
    save.data.put("coins", 5);
    save.flush();
    assertTrue(storage.files.containsKey("Game.slot1"));

    assertTrue(panel.resetAll());

    assertTrue(save.data.isEmpty());
    assertFalse(storage.files.containsKey("Game.slot1"));
    assertTrue(payload().rows.isEmpty());
  }

  @Test
  void actionsArriveThroughThePanelProtocol() {
    save.data.put("name", "Ada");
    save.data.put("coins", 1);
    save.data.put("gone", true);

    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_SET, w -> w.writeString("coins").writeString("77"));
    assertEquals(77, save.data.get("coins"));
    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_RESET_KEY, w -> w.writeString("gone"));
    assertFalse(save.data.containsKey("gone"));
    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_SAVE_NOW, w -> {
    });
    assertNotNull(storage.files.get("Game.slot1"));
    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_RESET_ALL, w -> {
    });
    assertTrue(save.data.isEmpty());
    DebugTestSupport.send(panel, 99, w -> w.writeString("ignored"));
  }

  @Test
  void theStatusShowsTheActionResultAndThenGoesBackToTheSummary() {
    save.data.put("coins", 1);
    panel.set("coins", "oops");
    Payload failed = payload();
    assertTrue(failed.isError);
    assertTrue(failed.status.contains("coins"), failed.status);

    panel.update(FlixelDebugSavePanel.STATUS_SECONDS + 0.1f);
    Payload summary = payload();
    assertFalse(summary.isError);
    assertTrue(summary.status.contains("Game.slot1"), summary.status);
    assertTrue(summary.status.contains("1 values"), summary.status);
  }

  @Test
  void aFailingStorageShowsAnErrorSummary() {
    save.data.put("coins", 1);
    storage.failWrites = true;
    save.flush();
    panel.refresh();
    assertTrue(payload().isError);
  }

  @Test
  void theRevisionOnlyMovesWhenTheContentChanges() {
    save.data.put("coins", 1);
    panel.update(1.1f);
    long revision = panel.getRevision();

    panel.update(1.1f);
    assertEquals(revision, panel.getRevision());

    save.data.put("coins", 2);
    panel.update(1.1f);
    assertTrue(panel.getRevision() > revision);
  }

  @Test
  void theEditedSaveCanBeSwappedAndRestored() {
    FlixelSave other = new FlixelSave(storage);
    other.bind("Other", null);
    other.data.put("x", 1);
    panel.setSave(other);
    assertEquals(List.of("x"), payload().paths());
    panel.setSave(null);
    save.data.put("y", 1);
    assertEquals(List.of("y"), payload().paths());
  }

  @Test
  void entryZeroIsAlwaysFlixelSaveAndAddedSavesFollowInOrder() {
    FlixelSave settings = bound("Game", "settings");
    FlixelSave profile = bound("Game", null);

    panel.add(settings);
    panel.add(profile);

    assertEquals(3, panel.getSaveCount());
    assertSame(save, panel.getSaveAt(0));
    assertSame(settings, panel.getSaveAt(1));
    assertSame(profile, panel.getSaveAt(2));
    assertNull(panel.getSaveAt(3));
    assertNull(panel.getSaveAt(-1));
    assertEquals(0, panel.getSelectedIndex());
    assertSame(save, panel.getSave(), "adding a save does not switch to it");
    Payload payload = payload();
    assertEquals(List.of("Game/slot1", "Game/settings", "Game"), payload.saves);
    assertEquals(0, payload.selected);
  }

  @Test
  void addingASaveTwiceOrAddingFlixelSaveDoesNothing() {
    FlixelSave settings = bound("Game", "settings");
    panel.add(settings);
    panel.add(settings);
    panel.add(save);
    assertEquals(2, panel.getSaveCount());
    assertEquals(List.of("Game/slot1", "Game/settings"), payload().saves);
  }

  @Test
  void entryZeroFollowsFlixelSaveWhenTheGameReplacesIt() {
    save.data.put("old", 1);
    FlixelSave replacement = bound("Game", "slot2");
    replacement.data.put("new", 1);

    assertEquals(List.of("old"), payload().paths());
    Flixel.save = replacement;
    Payload payload = payload();
    assertEquals(List.of("new"), payload.paths());
    assertEquals(List.of("Game/slot2"), payload.saves);

    Flixel.save = null;
    payload = payload();
    assertEquals(List.of("(no save)"), payload.saves);
    assertTrue(payload.rows.isEmpty());
  }

  @Test
  void aSaveThatBecomesFlixelSaveIsListedOnlyOnce() {
    FlixelSave profile = bound("Game", "profile1");
    panel.add(profile);
    Flixel.save = profile;
    assertEquals(1, panel.getSaveCount());
    assertEquals(List.of("Game/profile1"), payload().saves);
  }

  @Test
  void selectSwitchesTheEditedSaveAndRefreshesAtOnce() {
    save.data.put("a", 1);
    FlixelSave profile = bound("Game", "profile1");
    profile.data.put("b", 2);
    panel.add(profile);
    panel.refresh();
    long revision = panel.getRevision();

    assertTrue(panel.select(1));
    assertSame(profile, panel.getSave());
    assertEquals(1, panel.getSelectedIndex());
    assertTrue(panel.getRevision() > revision);
    Payload payload = decode();
    assertEquals(List.of("b"), payload.paths());
    assertEquals(1, payload.selected);
    assertTrue(payload.status.contains("Game.profile1"), payload.status);
    assertTrue(panel.set("b", "3"));
    assertEquals(3, profile.data.get("b"));

    assertFalse(panel.select(2));
    assertFalse(panel.select(-1));
    assertEquals(1, panel.getSelectedIndex());

    assertTrue(panel.select(0));
    assertSame(save, panel.getSave());
    assertEquals(List.of("a"), payload().paths());
  }

  @Test
  void theSelectedSaveIsComparedWithItsOwnStorage() {
    save.data.put("coins", 1);
    save.flush();
    FlixelSave profile = bound("Game", "profile1");
    profile.data.put("coins", 1);
    panel.add(profile);

    assertFalse(payload().row("coins").unsaved);
    panel.select(1);
    assertTrue(decode().row("coins").unsaved, "the profile was never written");
  }

  @Test
  void setSaveAddsTheSaveAndSelectsIt() {
    FlixelSave profile = bound("Game", "profile1");
    panel.setSave(profile);
    assertEquals(2, panel.getSaveCount());
    assertEquals(1, panel.getSelectedIndex());
    panel.setSave(profile);
    assertEquals(2, panel.getSaveCount());

    panel.setSave(null);
    assertEquals(0, panel.getSelectedIndex());
    assertEquals(2, panel.getSaveCount(), "going back to Flixel.save keeps the list");
  }

  @Test
  void removingTheSelectedSaveGoesBackToEntryZero() {
    FlixelSave settings = bound("Game", "settings");
    FlixelSave profile = bound("Game", "profile1");
    panel.add(settings);
    panel.add(profile);
    panel.select(2);

    assertTrue(panel.remove(settings));
    assertSame(profile, panel.getSave());
    assertEquals(1, panel.getSelectedIndex(), "the index moves down with the list");

    assertTrue(panel.remove(profile));
    assertSame(save, panel.getSave());
    assertEquals(0, panel.getSelectedIndex());
    assertFalse(panel.remove(profile));
    assertFalse(panel.remove(save), "Flixel.save cannot be taken off the list");
    Payload payload = decode();
    assertEquals(List.of("Game/slot1"), payload.saves);
    assertEquals(0, payload.selected);
  }

  @Test
  void aDestroyedSaveIsShownAsNotBoundAndCanStillBeSelected() {
    FlixelSave profile = bound("Game", "profile1");
    profile.data.put("a", 1);
    panel.add(profile);
    profile.destroy();

    assertTrue(panel.select(1));
    Payload payload = decode();
    assertEquals(List.of("Game/slot1", "(not bound)"), payload.saves);
    assertTrue(payload.rows.isEmpty());
    assertTrue(payload.status.contains("not bound"), payload.status);
    assertFalse(panel.saveNow());
  }

  @Test
  void theSelectActionPicksASave() {
    FlixelSave profile = bound("Game", "profile1");
    panel.add(profile);

    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_SELECT, w -> w.writeU16(1));
    assertSame(profile, panel.getSave());
    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_SELECT, w -> w.writeU16(9));
    assertSame(profile, panel.getSave(), "an index out of range is ignored");
    DebugTestSupport.send(panel, FlixelDebugProtocol.SAVE_SELECT, w -> w.writeU16(0));
    assertSame(save, panel.getSave());
  }

  @Test
  void theListOnlyChangesTheRevisionWhenALabelChanges() {
    FlixelSave profile = new FlixelSave(storage);
    panel.add(profile);
    panel.update(1.1f);
    long revision = panel.getRevision();
    panel.update(1.1f);
    assertEquals(revision, panel.getRevision());

    profile.bind("Game", "profile1");
    panel.update(1.1f);
    assertTrue(panel.getRevision() > revision);
    assertEquals(List.of("Game/slot1", "Game/profile1"), decode().saves);
  }

  @Test
  void resetClearsTheRows() {
    save.data.put("coins", 1);
    panel.refresh();
    panel.reset();
    assertEquals(0, panel.getUnsavedCount());
    Payload payload = decode();
    assertTrue(payload.rows.isEmpty());
    assertEquals("", payload.status);
  }

  private FlixelSave bound(String name, @Nullable String slot) {
    FlixelSave other = new FlixelSave(storage);
    other.bind(name, slot);
    return other;
  }

  /** Refreshes the panel the way its one second timer would, then decodes what it would send. */
  private Payload payload() {
    panel.refresh();
    return decode();
  }

  private Payload decode() {
    FlixelDebugMessageReader r = DebugTestSupport.payload(panel);
    Payload payload = new Payload();
    int count = r.readCount();
    for (int i = 0; i < count; i++) {
      Row row = new Row();
      row.path = DebugTestSupport.str(r);
      row.display = DebugTestSupport.str(r);
      row.kind = r.readU8();
      row.unsaved = r.readBool();
      payload.rows.add(row);
    }
    int removed = r.readCount();
    for (int i = 0; i < removed; i++) {
      payload.removed.add(DebugTestSupport.str(r));
    }
    payload.status = DebugTestSupport.str(r);
    payload.isError = r.readBool();
    int saves = r.readCount();
    for (int i = 0; i < saves; i++) {
      payload.saves.add(DebugTestSupport.str(r));
    }
    payload.selected = r.readU16();
    assertEquals(0, r.getRemaining());
    return payload;
  }

  private static final class Row {

    String path;
    String display;
    int kind;
    boolean unsaved;
  }

  private static final class Payload {

    final List<Row> rows = new ArrayList<>();
    final List<String> removed = new ArrayList<>();
    final List<String> saves = new ArrayList<>();
    String status;
    int selected;
    boolean isError;

    List<String> paths() {
      List<String> out = new ArrayList<>();
      for (Row row : rows) {
        out.add(row.path);
      }
      return out;
    }

    @Nullable
    Row row(String path) {
      for (Row row : rows) {
        if (row.path.equals(path)) {
          return row;
        }
      }
      return null;
    }
  }

  /** Keeps saves in memory so tests can read what was written. */
  private static final class MemoryHandler implements FlixelSaveHandler {

    final Map<String, String> files = new HashMap<>();
    boolean failWrites;

    @Nullable
    @Override
    public String read(@NotNull String name) {
      return files.get(name);
    }

    @Override
    public boolean write(@NotNull String name, @NotNull String data) {
      if (failWrites) {
        return false;
      }
      files.put(name, data);
      return true;
    }

    @Override
    public boolean delete(@NotNull String name) {
      files.remove(name);
      return true;
    }
  }
}
