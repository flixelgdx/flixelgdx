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
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.debug.InspectorTestSupport.Row;
import org.flixelgdx.debug.InspectorTestSupport.State;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.text.FlixelText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugInspectorPanelTest {

  private FlixelLogger originalLog;
  private List<FlixelLogLevel> levels;
  private FlixelDebugObjectIds ids;
  private FlixelDebugInspectorPanel inspector;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    levels = new ArrayList<>();
    log.addSink(entry -> levels.add(entry.getLevel()));
    Flixel.log = log;
    ids = new FlixelDebugObjectIds();
    inspector = new FlixelDebugInspectorPanel(ids);
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
    Flixel.state = null;
  }

  @Test
  void sectionsFollowTheClassChainMostDerivedFirstAndSkipUnregisteredClasses() {
    inspector.register(FlixelBasic.class, "Basic", b -> b.intProp("ID", o -> o.id));
    inspector.register(FlixelObject.class, "Object", b -> b.floatProp("X", FlixelObject::getX));
    inspector.register(FlixelSprite.class, "Sprite", b -> b.floatProp("Alpha", FlixelSprite::getAlpha));
    inspector.register(Player.class, "Player", b -> b.intProp("Health", p -> p.health));
    Player player = new Player();
    inspector.select(player);

    State state = InspectorTestSupport.state(inspector);

    assertEquals(FlixelDebugProtocol.INSPECTOR_STATE, state.subtype());
    assertEquals(List.of("Player", "FlixelSprite", "FlixelObject", "FlixelBasic"), state.typeNames());
    assertEquals("Player", state.sections().get(0).displayName());
    assertEquals("Sprite", state.sections().get(1).displayName());
    assertEquals("Health", state.row(0, 0).name());
    assertEquals("ID", state.row(3, 0).name());
    assertEquals(String.valueOf(player.id), state.row(3, 0).value());
  }

  @Test
  void aSubclassOfARegisteredClassStillGetsTheSectionOfItsAncestor() {
    inspector.register(Player.class, "Player", b -> b.intProp("Health", p -> p.health));
    inspector.select(new Boss());
    assertEquals(List.of("Player"), InspectorTestSupport.state(inspector).typeNames());
  }

  @Test
  void pathIsTypeNameAndId() {
    FlixelSprite sprite = new FlixelSprite();
    inspector.select(sprite);
    State state = InspectorTestSupport.state(inspector);
    assertEquals(ids.idOf(sprite), state.selectedId());
    assertEquals("FlixelSprite#" + state.selectedId(), state.path());
  }

  @Test
  void reRegisteringAClassReplacesItsSection() {
    inspector.register(Player.class, "Player", b -> b.intProp("Old", p -> 1));
    Player player = new Player();
    inspector.select(player);
    assertEquals("Old", InspectorTestSupport.state(inspector).row(0, 0).name());

    inspector.register(Player.class, "Hero", b -> b.intProp("New", p -> 2).intProp("Extra", p -> 3));

    State state = InspectorTestSupport.state(inspector);
    assertEquals(1, state.sections().size());
    assertEquals("Hero", state.sections().get(0).displayName());
    assertEquals(2, state.sections().get(0).rows().size());
    assertEquals("New", state.row(0, 0).name());
  }

  @Test
  void theCachedChainIsRebuiltWhenARegistrationChanges() {
    inspector.register(Player.class, "Player", b -> b.intProp("Health", p -> 1));
    Player player = new Player();
    inspector.select(player);
    assertEquals(List.of("Player"), InspectorTestSupport.state(inspector).typeNames());
    assertEquals(List.of("Player"), InspectorTestSupport.state(inspector).typeNames());

    inspector.register(FlixelSprite.class, "Sprite", b -> b.floatProp("Alpha", FlixelSprite::getAlpha));
    assertEquals(List.of("Player", "FlixelSprite"), InspectorTestSupport.state(inspector).typeNames());

    assertTrue(inspector.unregister(Player.class));
    assertFalse(inspector.unregister(Player.class));
    assertEquals(List.of("FlixelSprite"), InspectorTestSupport.state(inspector).typeNames());
    assertFalse(inspector.isRegistered(Player.class));
    assertTrue(inspector.isRegistered(FlixelSprite.class));
  }

  @Test
  void onceRowsAreReadOncePerSelectionWhileOtherRowsAreReadEveryTime() {
    int[] once = new int[1];
    int[] live = new int[1];
    inspector.register(Player.class, "Player", b -> b
        .intProp("Once", p -> ++once[0]).once()
        .intProp("Live", p -> ++live[0]));
    Player player = new Player();
    inspector.select(player);

    InspectorTestSupport.state(inspector);
    InspectorTestSupport.state(inspector);
    State third = InspectorTestSupport.state(inspector);

    assertEquals(1, once[0]);
    assertEquals(3, live[0]);
    assertEquals("1", third.row(0, 0).value());
    assertEquals("3", third.row(0, 1).value());

    inspector.select(new Player());
    State next = InspectorTestSupport.state(inspector);
    assertEquals(2, once[0]);
    assertEquals("2", next.row(0, 0).value());
  }

  @Test
  void onceMustFollowARow() {
    inspector.register(Player.class, "Player", b -> {
      assertThrows(IllegalStateException.class, b::once);
      b.intProp("Health", p -> 1);
    });
  }

  @Test
  void findTargetsARelatedObjectAndHidesTheSectionWhenItReturnsNull() {
    Owner owner = new Owner();
    inspector.register(Owner.class, "Weapon", b -> b
        .find(o -> o.weapon)
        .intProp("Ammo", w -> w.ammo, (w, v) -> w.ammo = v));
    inspector.select(owner);

    assertTrue(InspectorTestSupport.state(inspector).sections().isEmpty());

    owner.weapon = new Weapon();
    owner.weapon.ammo = 12;
    State state = InspectorTestSupport.state(inspector);
    assertEquals(1, state.sections().size());
    assertEquals("12", state.row(0, 0).value());

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SET_PROPERTY,
        w -> w.writeI32(state.selectedId()).writeU16(0).writeU16(0).writeValue(30));
    assertEquals(30, owner.weapon.ammo);
  }

  @Test
  void findMustComeBeforeAnyRow() {
    inspector.register(Owner.class, "Weapon", b -> {
      b.intProp("Anything", o -> 1);
      assertThrows(IllegalStateException.class, () -> b.find(o -> o.weapon));
    });
  }

  @Test
  void aFinderThatThrowsHidesTheSection() {
    inspector.register(Owner.class, "Weapon", b -> b
        .find(o -> {
          throw new IllegalStateException("nope");
        })
        .intProp("Ammo", w -> 1));
    inspector.select(new Owner());
    assertTrue(InspectorTestSupport.state(inspector).sections().isEmpty());
  }

  @Test
  void aGetterThatThrowsBecomesAnErrorValueAndOtherRowsKeepWorking() {
    inspector.register(Player.class, "Player", b -> b
        .intProp("Bad", p -> {
          throw new IllegalStateException("boom");
        })
        .stringProp("Worse", (p, out) -> {
          out.concat("partial");
          throw new IllegalArgumentException();
        })
        .intProp("Good", p -> 7));
    inspector.select(new Player());

    State state = InspectorTestSupport.state(inspector);

    assertEquals(FlixelDebugProtocol.VALUE_ERROR, state.row(0, 0).kind());
    assertEquals("boom", state.row(0, 0).value());
    assertEquals(FlixelDebugProtocol.VALUE_ERROR, state.row(0, 1).kind());
    assertEquals("java.lang.IllegalArgumentException", state.row(0, 1).value());
    assertEquals(FlixelDebugProtocol.VALUE_INT, state.row(0, 2).kind());
    assertEquals("7", state.row(0, 2).value());
  }

  @Test
  void everyKindIsWrittenWithItsOwnValueKind() {
    Thing thing = new Thing();
    thing.flag = true;
    thing.count = 4;
    thing.big = 1L << 40;
    thing.ratio = 1.5f;
    thing.precise = 2.25;
    thing.label = "hi";
    registerThing();
    inspector.select(thing);

    State state = InspectorTestSupport.state(inspector);

    int[] kinds = {
        FlixelDebugProtocol.VALUE_BOOL, FlixelDebugProtocol.VALUE_INT, FlixelDebugProtocol.VALUE_LONG,
        FlixelDebugProtocol.VALUE_FLOAT, FlixelDebugProtocol.VALUE_DOUBLE, FlixelDebugProtocol.VALUE_STRING,
        FlixelDebugProtocol.VALUE_STRING, FlixelDebugProtocol.VALUE_INT
    };
    String[] values = { "true", "4", String.valueOf(1L << 40), "1.5", "2.25", "hi", "SECOND", "9" };
    for (int i = 0; i < kinds.length; i++) {
      assertEquals(kinds[i], state.row(0, i).kind(), "kind of row " + i);
      assertEquals(values[i], state.row(0, i).value(), "value of row " + i);
    }
    assertTrue(state.row(0, 0).editable());
    assertFalse(state.row(0, 6).editable(), "enum rows are read-only");
    assertFalse(state.row(0, 7).editable(), "rows without a setter are read-only");
  }

  @Test
  void setPropertyAppliesEachKind() {
    Thing thing = new Thing();
    registerThing();
    inspector.select(thing);
    int id = InspectorTestSupport.state(inspector).selectedId();

    set(id, 0, 0, w -> w.writeValue(true));
    set(id, 0, 1, w -> w.writeValue(42));
    set(id, 0, 2, w -> w.writeValue(1L << 40));
    set(id, 0, 3, w -> w.writeValue(2.5f));
    set(id, 0, 4, w -> w.writeValue(0.125));
    set(id, 0, 5, w -> w.writeValue("hello"));

    assertTrue(thing.flag);
    assertEquals(42, thing.count);
    assertEquals(1L << 40, thing.big);
    assertEquals(2.5f, thing.ratio);
    assertEquals(0.125, thing.precise);
    assertEquals("hello", thing.label);
    assertTrue(levels.isEmpty(), "valid edits must not log warnings");
    assertEquals("42", InspectorTestSupport.state(inspector).row(0, 1).value());
  }

  @Test
  void setPropertyConvertsBetweenNumericKinds() {
    Thing thing = new Thing();
    registerThing();
    inspector.select(thing);
    int id = InspectorTestSupport.state(inspector).selectedId();

    set(id, 0, 1, w -> w.writeValue(7.9));
    set(id, 0, 2, w -> w.writeValue(3));
    set(id, 0, 3, w -> w.writeValue(4));
    set(id, 0, 4, w -> w.writeValue(1.5f));

    assertEquals(7, thing.count);
    assertEquals(3L, thing.big);
    assertEquals(4f, thing.ratio);
    assertEquals(1.5, thing.precise);
    assertTrue(levels.isEmpty());
  }

  @Test
  void setPropertyIgnoresMismatchedKindsAndLogsAWarning() {
    Thing thing = new Thing();
    thing.flag = false;
    thing.count = 5;
    thing.label = "keep";
    registerThing();
    inspector.select(thing);
    int id = InspectorTestSupport.state(inspector).selectedId();

    set(id, 0, 0, w -> w.writeValue(1));
    set(id, 0, 1, w -> w.writeValue("text"));
    set(id, 0, 1, w -> w.writeValue(true));
    set(id, 0, 5, w -> w.writeValue(3));

    assertFalse(thing.flag);
    assertEquals(5, thing.count);
    assertEquals("keep", thing.label);
    assertEquals(4, levels.size());
    assertTrue(levels.stream().allMatch(level -> level == FlixelLogLevel.WARN));
  }

  @Test
  void setPropertyIgnoresReadOnlyRowsStaleIdsAndOutOfRangeIndices() {
    Thing thing = new Thing();
    registerThing();
    inspector.select(thing);
    int id = InspectorTestSupport.state(inspector).selectedId();

    set(id, 0, 7, w -> w.writeValue(100));
    set(id + 1, 0, 1, w -> w.writeValue(100));
    set(id, 5, 1, w -> w.writeValue(100));
    set(id, 0, 99, w -> w.writeValue(100));
    set(id, 0, 6, w -> w.writeValue("FIRST"));

    assertEquals(0, thing.count);
    assertEquals(9, thing.readOnly);
    assertEquals(Mode.SECOND, thing.mode);
  }

  @Test
  void setPropertyBeforeAnythingWasSentIsIgnored() {
    Thing thing = new Thing();
    registerThing();
    inspector.select(thing);
    int id = ids.idOf(thing);

    set(id, 0, 1, w -> w.writeValue(100));

    assertEquals(0, thing.count);
  }

  @Test
  void setPropertyTargetsTheSectionByItsSentPosition() {
    inspector.register(FlixelBasic.class, "Basic", b -> b.intProp("ID", o -> o.id));
    inspector.register(FlixelObject.class, "Object", b -> b.floatProp("X", FlixelObject::getX, FlixelObject::setX));
    FlixelObject object = new FlixelObject();
    inspector.select(object);
    State state = InspectorTestSupport.state(inspector);
    assertEquals(List.of("FlixelObject", "FlixelBasic"), state.typeNames());

    set(state.selectedId(), 0, 0, w -> w.writeValue(33f));

    assertEquals(33f, object.getX());
  }

  @Test
  void selectingAndClearingNotifiesListenersAndBumpsTheRevision() {
    List<Object> events = new ArrayList<>();
    inspector.addSelectionListener(events::add);
    FlixelSprite sprite = new FlixelSprite();

    long before = inspector.getRevision();
    inspector.select(sprite);
    assertTrue(inspector.getRevision() > before);
    inspector.select(sprite);
    inspector.clearSelection();
    inspector.clearSelection();

    assertEquals(2, events.size());
    assertSame(sprite, events.get(0));
    assertNull(events.get(1));
    assertNull(inspector.getSelected());
  }

  @Test
  void removedListenersAreNotCalled() {
    List<Object> events = new ArrayList<>();
    FlixelDebugInspectorPanel.SelectionListener listener = events::add;
    inspector.addSelectionListener(listener);
    inspector.addSelectionListener(listener);
    inspector.removeSelectionListener(listener);
    inspector.select(new FlixelSprite());
    assertTrue(events.isEmpty());
  }

  @Test
  void selectionClearsWhenTheObjectIsDestroyedOrNoLongerExists() {
    List<Object> events = new ArrayList<>();
    inspector.addSelectionListener(events::add);

    FlixelSprite destroyed = new FlixelSprite();
    inspector.select(destroyed);
    destroyed.destroy();
    inspector.update(0f);
    assertNull(inspector.getSelected());
    assertEquals(2, events.size());
    assertNull(events.get(1));

    FlixelSprite killed = new FlixelSprite();
    inspector.select(killed);
    killed.kill();
    assertNull(inspector.getSelected(), "getSelected() also drops an object that no longer exists");

    State state = InspectorTestSupport.state(inspector);
    assertEquals(FlixelDebugObjectIds.NONE, state.selectedId());
  }

  @Test
  void selectingAnObjectThatDoesNotExistDoesNothing() {
    FlixelSprite sprite = new FlixelSprite();
    sprite.kill();
    inspector.select(sprite);
    assertNull(inspector.getSelected());
  }

  @Test
  void nothingSelectedWritesAnEmptyState() {
    State state = InspectorTestSupport.state(inspector);
    assertEquals(FlixelDebugProtocol.INSPECTOR_STATE, state.subtype());
    assertEquals(-1, state.selectedId());
    assertEquals("", state.path());
    assertTrue(state.sections().isEmpty());
    assertFalse(state.hasTexture());
  }

  @Test
  void hasTextureComesFromTheOverridableHook() {
    FlixelSprite sprite = new FlixelSprite();
    inspector.select(sprite);
    assertFalse(InspectorTestSupport.state(inspector).hasTexture(), "a sprite without a texture has no thumbnail");

    FlixelDebugInspectorPanel always = new FlixelDebugInspectorPanel(ids) {
      @Override
      protected boolean hasTexture(Object object) {
        return true;
      }
    };
    always.select(sprite);
    assertTrue(InspectorTestSupport.state(always).hasTexture());
  }

  @Test
  void selectParentMovesToTheGroupThatHoldsTheObject() {
    FlixelSpriteGroup inner = new FlixelSpriteGroup();
    FlixelSpriteGroup outer = new FlixelSpriteGroup();
    FlixelSprite sprite = new FlixelSprite();
    inner.add(sprite);
    outer.add(inner);
    inspector.track(outer, "Outer");

    inspector.select(sprite);
    assertTrue(inspector.selectParent());
    assertSame(inner, inspector.getSelected());
    assertTrue(inspector.selectParent());
    assertSame(outer, inspector.getSelected());
    assertFalse(inspector.selectParent(), "a root has no parent");
    assertSame(outer, inspector.getSelected());
  }

  @Test
  void selectParentWalksTheCurrentStateTree() {
    FlixelState state = new TestState();
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite sprite = new FlixelSprite();
    group.add(sprite);
    state.add(group);
    Flixel.state = state;

    inspector.select(sprite);
    assertTrue(inspector.selectParent());
    assertSame(group, inspector.getSelected());
    assertTrue(inspector.selectParent());
    assertSame(state, inspector.getSelected());
    assertFalse(inspector.selectParent());
  }

  @Test
  void selectParentWithNothingSelectedReturnsFalse() {
    assertFalse(inspector.selectParent());
  }

  @Test
  void goToCentersTheMainCameraOnTheSelection() {
    FlixelCamera camera = new FlixelCamera(400, 300);
    Flixel.cameras.add(camera);
    try {
      FlixelSprite sprite = new FlixelSprite(1000f, 500f);
      sprite.setSize(20f, 40f);
      FlixelCamera expected = new FlixelCamera(400, 300);
      expected.focusOn(sprite.getMidpointX(), sprite.getMidpointY());

      inspector.select(sprite);

      assertTrue(inspector.goTo());
      assertEquals(expected.scrollX, camera.scrollX, 0.001f);
      assertEquals(expected.scrollY, camera.scrollY, 0.001f);
      assertNotEquals(0f, camera.scrollX);
    } finally {
      Flixel.cameras.removeValue(camera, true);
    }
  }

  @Test
  void goToDoesNothingWithoutAPositionOrASelection() {
    assertFalse(inspector.goTo());
    inspector.select(new Object());
    assertFalse(inspector.goTo());
  }

  @Test
  void trackedObjectsAreStoredLabeledAndCanBeRemoved() {
    Object manager = new Object();
    Object other = new Object();
    inspector.track(manager, "Manager");
    inspector.track(other, "Other");
    inspector.track(manager, "Renamed");

    assertEquals(2, inspector.getTrackedCount());
    assertSame(manager, inspector.getTracked(0));
    assertEquals("Renamed", inspector.getTrackedLabel(0));
    assertSame(other, inspector.getTracked(1));
    assertTrue(ids.peek(manager) > 0, "tracked objects get an id so the scene can list them");

    inspector.select(manager);
    assertTrue(inspector.untrack(manager));
    assertFalse(inspector.untrack(manager));
    assertEquals(1, inspector.getTrackedCount());
    assertSame(other, inspector.getTracked(0));
    assertNull(inspector.getSelected(), "untracking the selected object clears the selection");
    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(manager));
  }

  @Test
  void actionsSelectParentClearAndGoTo() {
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite sprite = new FlixelSprite();
    group.add(sprite);
    inspector.track(group, "Group");
    int id = ids.idOf(sprite);

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SELECT, w -> w.writeI32(id));
    assertSame(sprite, inspector.getSelected());

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SELECT_PARENT, w -> {
    });
    assertSame(group, inspector.getSelected());

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SELECT, w -> w.writeI32(987654));
    assertSame(group, inspector.getSelected(), "an unknown id is ignored");

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_GO_TO, w -> {
    });

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_CLEAR, w -> {
    });
    assertNull(inspector.getSelected());
  }

  @Test
  void laterActionsAndUnknownActionsAreIgnored() {
    FlixelSprite sprite = new FlixelSprite();
    inspector.select(sprite);

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SELECT_PATH, w -> w.writeString("Anything"));
    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_REQUEST_THUMBNAIL, w -> w.writeI32(1));
    DebugTestSupport.send(inspector, 200, w -> {
    });

    assertSame(sprite, inspector.getSelected());
  }

  @Test
  void updateBumpsTheRevisionOnlyWhileSomethingIsSelected() {
    long idle = inspector.getRevision();
    inspector.update(0.5f);
    assertEquals(idle, inspector.getRevision());

    inspector.select(new FlixelSprite());
    long selected = inspector.getRevision();
    inspector.update(0.5f);
    assertTrue(inspector.getRevision() > selected);
  }

  @Test
  void updateSweepsIdsOfDestroyedObjects() {
    FlixelSprite sprite = new FlixelSprite();
    ids.idOf(sprite);
    sprite.destroy();
    inspector.update(2f);
    assertEquals(FlixelDebugObjectIds.NONE, ids.peek(sprite));
  }

  @Test
  void builtInSectionsDescribeASprite() {
    FlixelDebugManager manager = new FlixelDebugManager();
    FlixelSprite sprite = new FlixelSprite(10f, 20f);
    manager.inspector.select(sprite);

    State state = InspectorTestSupport.state(manager.inspector);

    assertEquals(List.of("FlixelSprite", "FlixelObject", "FlixelBasic"), state.typeNames());
    assertEquals(List.of("Sprite", "Object", "Basic"),
        state.sections().stream().map(InspectorTestSupport.Section::displayName).toList());
    Row id = row(state, "Basic", "ID");
    assertEquals(String.valueOf(sprite.id), id.value());
    assertFalse(id.editable());
    Row x = row(state, "Object", "X");
    assertEquals(FlixelDebugProtocol.VALUE_FLOAT, x.kind());
    assertEquals("10.0", x.value());
    assertTrue(x.editable());
    assertEquals("#FFFFFFFF", row(state, "Sprite", "Color").value());
    assertEquals("", row(state, "Sprite", "Animation").value());
    assertTrue(row(state, "Basic", "Exists").editable());
  }

  @Test
  void builtInSectionsEditASprite() {
    FlixelDebugManager manager = new FlixelDebugManager();
    FlixelSprite sprite = new FlixelSprite(10f, 20f);
    manager.inspector.select(sprite);
    State state = InspectorTestSupport.state(manager.inspector);
    int id = state.selectedId();
    int objectSection = state.typeNames().indexOf("FlixelObject");
    int spriteSection = state.typeNames().indexOf("FlixelSprite");
    int xIndex = indexOf(state, "Object", "X");
    int colorIndex = indexOf(state, "Sprite", "Color");
    int alphaIndex = indexOf(state, "Sprite", "Alpha");

    DebugTestSupport.send(manager.inspector, FlixelDebugProtocol.INSPECTOR_SET_PROPERTY,
        w -> w.writeI32(id).writeU16(objectSection).writeU16(xIndex).writeValue(99f));
    DebugTestSupport.send(manager.inspector, FlixelDebugProtocol.INSPECTOR_SET_PROPERTY,
        w -> w.writeI32(id).writeU16(spriteSection).writeU16(colorIndex).writeValue("#FF000080"));
    DebugTestSupport.send(manager.inspector, FlixelDebugProtocol.INSPECTOR_SET_PROPERTY,
        w -> w.writeI32(id).writeU16(spriteSection).writeU16(alphaIndex).writeValue(0.5f));

    assertEquals(99f, sprite.getX());
    assertEquals(0.5f, sprite.getAlpha(), 0.001f);
    assertEquals(1f, sprite.getColor().r, 0.01f);
    assertEquals(0f, sprite.getColor().g, 0.01f);
  }

  @Test
  void builtInSectionsDescribeTextAndCamera() {
    FlixelDebugManager manager = new FlixelDebugManager();
    FlixelText text = new FlixelText(0f, 0f, 100f, "Hello");
    manager.inspector.select(text);

    State textState = InspectorTestSupport.state(manager.inspector);
    assertEquals(List.of("FlixelText", "FlixelSprite", "FlixelObject", "FlixelBasic"), textState.typeNames());
    assertEquals("Hello", row(textState, "Text", "Text").value());
    assertTrue(row(textState, "Text", "Text").editable());

    FlixelCamera camera = new FlixelCamera(400, 300);
    manager.inspector.select(camera);
    State cameraState = InspectorTestSupport.state(manager.inspector);
    assertEquals(List.of("FlixelCamera", "FlixelBasic"), cameraState.typeNames());
    assertEquals("1.0", row(cameraState, "Camera", "Zoom").value());
    assertEquals("#000000FF", row(cameraState, "Camera", "Background").value());
  }

  @Test
  void managerRegistersTheInspectorAsKindInspector() {
    FlixelDebugManager manager = new FlixelDebugManager();
    assertSame(manager.inspector, manager.get("inspector"));
    assertEquals(FlixelDebugProtocol.KIND_INSPECTOR, manager.inspector.getKind());
    assertSame(manager.ids, manager.inspector.getIds());
  }

  @Test
  void consoleCommandsClearAndMoveTheSelection() {
    FlixelDebugManager manager = new FlixelDebugManager();
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite sprite = new FlixelSprite();
    group.add(sprite);
    manager.inspector.track(group, "Group");
    manager.inspector.select(sprite);

    assertTrue(manager.console.execute("select.parent"));
    assertSame(group, manager.inspector.getSelected());
    assertTrue(manager.console.execute("select.clear"));
    assertNull(manager.inspector.getSelected());
    assertTrue(manager.console.has("select.clear"));
    assertTrue(manager.console.has("select.parent"));
  }

  private void set(int id, int section, int property,
      Consumer<FlixelDebugMessageWriter> value) {
    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SET_PROPERTY, w -> {
      w.writeI32(id).writeU16(section).writeU16(property);
      value.accept(w);
    });
  }

  private void registerThing() {
    inspector.register(Thing.class, "Thing", b -> b
        .boolProp("Flag", t -> t.flag, (t, v) -> t.flag = v)
        .intProp("Count", t -> t.count, (t, v) -> t.count = v)
        .longProp("Big", t -> t.big, (t, v) -> t.big = v)
        .floatProp("Ratio", t -> t.ratio, (t, v) -> t.ratio = v)
        .doubleProp("Precise", t -> t.precise, (t, v) -> t.precise = v)
        .stringProp("Label", (t, out) -> out.concat(t.label), (t, v) -> t.label = v.toString())
        .enumProp("Mode", t -> t.mode)
        .intProp("Read only", t -> t.readOnly));
  }

  private static Row row(State state, String sectionDisplayName, String rowName) {
    for (InspectorTestSupport.Section section : state.sections()) {
      if (section.displayName().equals(sectionDisplayName)) {
        for (Row row : section.rows()) {
          if (row.name().equals(rowName)) {
            return row;
          }
        }
      }
    }
    throw new AssertionError("No row " + sectionDisplayName + "/" + rowName);
  }

  private static int indexOf(State state, String sectionDisplayName, String rowName) {
    for (InspectorTestSupport.Section section : state.sections()) {
      if (section.displayName().equals(sectionDisplayName)) {
        for (int i = 0; i < section.rows().size(); i++) {
          if (section.rows().get(i).name().equals(rowName)) {
            return i;
          }
        }
      }
    }
    throw new AssertionError("No row " + sectionDisplayName + "/" + rowName);
  }

  private static class Mid extends FlixelSprite {
  }

  private static class Player extends Mid {

    int health = 100;
  }

  private static final class Boss extends Player {
  }

  private static final class Weapon {

    int ammo;
  }

  private static final class Owner {

    Weapon weapon;
  }

  private static final class Thing {

    long big;
    double precise;
    float ratio;
    int count;
    int readOnly = 9;
    String label = "";
    Mode mode = Mode.SECOND;
    boolean flag;
  }

  private static final class TestState extends FlixelState {
  }

  private enum Mode {
    FIRST,
    SECOND
  }
}
