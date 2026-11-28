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
package org.flixelgdx;

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.flixelgdx.testing.RecordingBatch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelManagersTest {

  private FlixelManagers managers;
  private StringBuilder log;

  @BeforeEach
  void setUp() {
    managers = new FlixelManagers();
    log = new StringBuilder();
  }

  @Test
  void hooksRunFromTheLowestOrderToTheHighest() {
    managers.add(new Recorder("c", 10, log));
    managers.add(new Recorder("a", -10, log));
    managers.add(new Recorder("b", 0, log));
    managers.update(0.1f);
    assertEquals("a.update b.update c.update ", log.toString());
  }

  @Test
  void managersWithTheSameOrderRunInTheOrderTheyWereAdded() {
    managers.add(new Recorder("first", 5, log));
    managers.add(new Recorder("second", 5, log));
    managers.add(new Recorder("third", 5, log));
    managers.endFrame();
    assertEquals("first.endFrame second.endFrame third.endFrame ", log.toString());
  }

  @Test
  void destroyRunsFromTheHighestOrderToTheLowest() {
    managers.add(new Recorder("a", -1, log));
    managers.add(new Recorder("b", 0, log));
    managers.add(new Recorder("c", 1, log));
    managers.create();
    log.setLength(0);
    managers.destroy();
    assertEquals("c.destroy b.destroy a.destroy ", log.toString());
  }

  @Test
  void theOrderIsReadOnlyWhenTheManagerIsAdded() {
    Recorder moving = new Recorder("moving", 0, log);
    managers.add(moving);
    managers.add(new Recorder("fixed", 1, log));
    moving.order = 5;
    managers.update(0f);
    assertEquals("moving.update fixed.update ", log.toString());
  }

  @Test
  void addingTheSameManagerTwiceDoesNothing() {
    Recorder recorder = new Recorder("a", 0, log);
    managers.add(recorder);
    managers.add(recorder);
    assertEquals(1, managers.getSize());
    managers.update(0f);
    assertEquals("a.update ", log.toString());
  }

  @Test
  void createRunsOnceUntilTheManagersAreDestroyed() {
    managers.add(new Recorder("a", 0, log));
    assertFalse(managers.isCreated());
    managers.create();
    managers.create();
    assertTrue(managers.isCreated());
    assertEquals("a.create ", log.toString());
  }

  @Test
  void destroyDoesNothingWhenTheManagersWereNotCreated() {
    managers.add(new Recorder("a", 0, log));
    managers.destroy();
    assertEquals("", log.toString());
  }

  @Test
  void managersStayRegisteredAfterDestroySoTheyAreCreatedAgain() {
    managers.add(new Recorder("a", 0, log));
    managers.create();
    managers.destroy();
    managers.create();
    assertEquals("a.create a.destroy a.create ", log.toString());
    assertEquals(1, managers.getSize());
  }

  @Test
  void aManagerAddedToARunningGameIsCreatedRightAway() {
    managers.create();
    managers.add(new Recorder("late", 0, log));
    assertEquals("late.create ", log.toString());
  }

  @Test
  void aManagerAddedBeforeTheGameStartsIsNotCreatedYet() {
    managers.add(new Recorder("early", 0, log));
    assertEquals("", log.toString());
  }

  @Test
  void aManagerRemovedFromARunningGameIsDestroyedRightAway() {
    Recorder recorder = new Recorder("a", 0, log);
    managers.add(recorder);
    managers.create();
    log.setLength(0);
    managers.remove(recorder);
    assertEquals("a.destroy ", log.toString());
    assertFalse(managers.contains(recorder));
    assertEquals(0, managers.getSize());
  }

  @Test
  void aManagerRemovedBeforeTheGameStartsIsNotDestroyed() {
    Recorder recorder = new Recorder("a", 0, log);
    managers.add(recorder);
    managers.remove(recorder);
    assertEquals("", log.toString());
    assertEquals(0, managers.getSize());
  }

  @Test
  void removingAManagerThatIsNotRegisteredDoesNothing() {
    managers.remove(new Recorder("a", 0, log));
    assertEquals(0, managers.getSize());
  }

  @Test
  void pausableManagersSkipTheUpdateHooksWhileThePauseIsOn() {
    managers.add(new Recorder("game", 0, log));
    Recorder input = new Recorder("input", 1, log);
    input.pausable = false;
    managers.add(input);
    managers.preUpdate(0f, true);
    managers.update(0f, true);
    managers.postUpdate(0f, true);
    assertEquals("input.preUpdate input.update input.postUpdate ", log.toString());
  }

  @Test
  void everyManagerUpdatesWhileThePauseIsOff() {
    managers.add(new Recorder("game", 0, log));
    managers.preUpdate(0f, false);
    managers.update(0f, false);
    managers.postUpdate(0f, false);
    assertEquals("game.preUpdate game.update game.postUpdate ", log.toString());
  }

  @Test
  void drawAndEventHooksStillReachPausableManagers() {
    managers.add(new Recorder("game", 0, log));
    managers.onPaused(true);
    managers.postDraw(new RecordingBatch());
    assertEquals("game.onPaused(true) game.postDraw ", log.toString());
  }

  @Test
  void everyHookReachesTheManagerWithItsArguments() {
    Recorder recorder = new Recorder("m", 0, log);
    managers.add(recorder);
    RecordingBatch batch = new RecordingBatch();
    FlixelCamera camera = new FlixelCamera(10, 10);
    StateSwitchSignalData data = new StateSwitchSignalData(null);

    managers.create();
    managers.beginFrame();
    managers.preUpdate(0.5f, false);
    managers.update(0.5f, false);
    managers.postUpdate(0.5f, false);
    managers.preDraw(batch);
    managers.drawCamera(camera, batch);
    managers.drawOverlay(batch);
    managers.postDraw(batch);
    managers.endFrame();
    managers.resize(640, 360);
    managers.onFocusLost();
    managers.onFocusGained();
    managers.onMinimized();
    managers.onSuspended();
    managers.onPaused(false);
    managers.preStateSwitch(data);
    managers.onStateSwitch(data);
    managers.postStateSwitch(data);
    managers.destroy();

    assertEquals(
        "m.create m.beginFrame m.preUpdate m.update m.postUpdate m.preDraw m.drawCamera m.drawOverlay m.postDraw "
            + "m.endFrame m.resize(640x360) m.onFocusLost m.onFocusGained m.onMinimized m.onSuspended "
            + "m.onPaused(false) m.preStateSwitch m.onStateSwitch m.postStateSwitch m.destroy ",
        log.toString());
    assertEquals(0.5f, recorder.lastElapsed);
    assertSame(batch, recorder.lastBatch);
    assertSame(camera, recorder.lastCamera);
    assertSame(data, recorder.lastData);
  }

  @Test
  void aSourceSlotAlwaysReachesTheCurrentManager() {
    Recorder[] holder = { new Recorder("old", 0, log) };
    managers.add(() -> holder[0], 0);
    managers.update(0f);
    holder[0] = new Recorder("new", 0, log);
    managers.update(0f);
    assertEquals("old.update new.update ", log.toString());
  }

  @Test
  void aSourceThatSuppliesNullIsSkipped() {
    managers.add(() -> null, 0);
    managers.add(new Recorder("a", 1, log));
    managers.update(0f);
    assertEquals("a.update ", log.toString());
  }

  @Test
  void aSourceSlotUsesTheOrderItWasAddedWith() {
    Recorder sourced = new Recorder("sourced", 100, log);
    managers.add(new Recorder("fixed", 0, log));
    managers.add(() -> sourced, -1);
    managers.update(0f);
    assertEquals("sourced.update fixed.update ", log.toString());
  }

  @Test
  void addingTheSameSourceTwiceDoesNothing() {
    Supplier<Recorder> source = () -> new Recorder("a", 0, log);
    managers.add(source, 0);
    managers.add(source, 0);
    assertEquals(1, managers.getSize());
    assertTrue(managers.contains(source));
  }

  @Test
  void removingASourceDestroysTheManagerItSupplies() {
    Recorder recorder = new Recorder("a", 0, log);
    Supplier<Recorder> source = () -> recorder;
    managers.add(source, 0);
    managers.create();
    managers.remove(source);
    assertEquals("a.create a.destroy ", log.toString());
    assertFalse(managers.contains(source));
  }

  @Test
  void aManagerAddedDuringAHookJoinsAfterThatHookFinishes() {
    Recorder late = new Recorder("late", -100, log);
    managers.add(new Recorder("adder", 0, log) {
      @Override
      public void update(float elapsed) {
        super.update(elapsed);
        managers.add(late);
      }
    });
    managers.update(0f);
    assertTrue(managers.contains(late));
    assertEquals(2, managers.getSize());
    log.setLength(0);
    managers.endFrame();
    assertEquals("late.endFrame adder.endFrame ", log.toString());
  }

  @Test
  void aManagerRemovedDuringAHookIsSkippedForTheRestOfIt() {
    Recorder second = new Recorder("second", 1, log);
    managers.add(new Recorder("first", 0, log) {
      @Override
      public void update(float elapsed) {
        super.update(elapsed);
        managers.remove(second);
      }
    });
    managers.add(second);
    managers.update(0f);
    assertEquals("first.update ", log.toString());
    assertEquals(1, managers.getSize());
  }

  @Test
  void nullArgumentsAreRejected() {
    assertThrows(NullPointerException.class, () -> managers.add((FlixelManagers.Manager) null));
    assertThrows(NullPointerException.class, () -> managers.add(null, 0));
    assertThrows(NullPointerException.class, () -> managers.remove((FlixelManagers.Manager) null));
    assertThrows(NullPointerException.class, () -> managers.remove((Supplier<FlixelManagers.Manager>) null));
    assertFalse(managers.contains((FlixelManagers.Manager) null));
    assertFalse(managers.contains((Supplier<FlixelManagers.Manager>) null));
  }

  @Test
  void theDefaultsOfTheManagerInterfaceDoNothing() {
    FlixelManagers.Manager manager = new FlixelManagers.Manager() {
    };
    assertEquals(FlixelManagers.Manager.ORDER_DEFAULT, manager.getOrder());
    assertTrue(manager.isPausable());
    managers.add(manager);
    managers.create();
    managers.update(0f);
    managers.drawOverlay(new RecordingBatch());
    managers.destroy();
    assertEquals(1, managers.getSize());
  }

  static class Recorder implements FlixelManagers.Manager {

    final String name;
    final StringBuilder log;
    int order;
    float lastElapsed;
    FlixelBatch lastBatch;
    FlixelCamera lastCamera;
    StateSwitchSignalData lastData;
    boolean pausable = true;

    Recorder(String name, int order, StringBuilder log) {
      this.name = name;
      this.order = order;
      this.log = log;
    }

    private void record(String hook) {
      log.append(name).append('.').append(hook).append(' ');
    }

    @Override
    public int getOrder() {
      return order;
    }

    @Override
    public boolean isPausable() {
      return pausable;
    }

    @Override
    public void create() {
      record("create");
    }

    @Override
    public void preUpdate(float elapsed) {
      lastElapsed = elapsed;
      record("preUpdate");
    }

    @Override
    public void update(float elapsed) {
      lastElapsed = elapsed;
      record("update");
    }

    @Override
    public void postUpdate(float elapsed) {
      lastElapsed = elapsed;
      record("postUpdate");
    }

    @Override
    public void preDraw(@NotNull FlixelBatch batch) {
      lastBatch = batch;
      record("preDraw");
    }

    @Override
    public void drawCamera(@NotNull FlixelCamera camera, @NotNull FlixelBatch batch) {
      lastCamera = camera;
      lastBatch = batch;
      record("drawCamera");
    }

    @Override
    public void drawOverlay(@NotNull FlixelBatch batch) {
      lastBatch = batch;
      record("drawOverlay");
    }

    @Override
    public void postDraw(@NotNull FlixelBatch batch) {
      lastBatch = batch;
      record("postDraw");
    }

    @Override
    public void beginFrame() {
      record("beginFrame");
    }

    @Override
    public void endFrame() {
      record("endFrame");
    }

    @Override
    public void resize(int width, int height) {
      record("resize(" + width + "x" + height + ")");
    }

    @Override
    public void onFocusLost() {
      record("onFocusLost");
    }

    @Override
    public void onFocusGained() {
      record("onFocusGained");
    }

    @Override
    public void onMinimized() {
      record("onMinimized");
    }

    @Override
    public void onSuspended() {
      record("onSuspended");
    }

    @Override
    public void onPaused(boolean paused) {
      record("onPaused(" + paused + ")");
    }

    @Override
    public void preStateSwitch(@NotNull StateSwitchSignalData data) {
      lastData = data;
      record("preStateSwitch");
    }

    @Override
    public void onStateSwitch(@NotNull StateSwitchSignalData data) {
      lastData = data;
      record("onStateSwitch");
    }

    @Override
    public void postStateSwitch(@NotNull StateSwitchSignalData data) {
      lastData = data;
      record("postStateSwitch");
    }

    @Override
    public void destroy() {
      record("destroy");
    }
  }
}
