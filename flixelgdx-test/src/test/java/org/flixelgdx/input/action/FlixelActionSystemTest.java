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
package org.flixelgdx.input.action;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelActionSystemTest {

  private FlixelKeyInputManager savedKeys;

  @BeforeEach
  void saveKeys() {
    savedKeys = Flixel.keys;
    Flixel.keys = new FlixelKeyInputManager();
    FlixelActionSets.clearRegistryForTests();
  }

  @AfterEach
  void restoreKeys() {
    Flixel.keys = savedKeys;
    FlixelActionSets.clearRegistryForTests();
  }

  @Test
  void digitalJustPressedFollowsEndFrameOrdering() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital fire = new FlixelActionDigital("fire");
    fire.addBinding("key", FlixelDigitalBinding.key(FlixelKey.F));
    set.add(fire);

    Flixel.keys.keyDown(FlixelKey.F);
    set.update(0f);
    assertTrue(fire.pressed());
    assertTrue(fire.justPressed());

    set.endFrame();
    set.update(0f);
    assertTrue(fire.pressed());
    assertFalse(fire.justPressed());

    Flixel.keys.keyUp(FlixelKey.F);
    set.update(0f);
    assertFalse(fire.pressed());
    assertTrue(fire.justReleased());

    set.endFrame();
    set.update(0f);
    assertFalse(fire.justReleased());
  }

  @Test
  void overlappingOrBindings() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital any = new FlixelActionDigital("any");
    any.addBinding("a", FlixelDigitalBinding.key(FlixelKey.A));
    any.addBinding("b", FlixelDigitalBinding.key(FlixelKey.B));
    set.add(any);

    Flixel.keys.keyDown(FlixelKey.B);
    set.update(0f);
    assertTrue(any.pressed());
    Flixel.keys.keyUp(FlixelKey.B);
    Flixel.keys.keyDown(FlixelKey.A);
    set.update(0f);
    assertTrue(any.pressed());
  }

  @Test
  void touchRegionBindingFalseWhenNoPointers() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital zone = new FlixelActionDigital("zone");
    zone.addBinding("touch", FlixelDigitalBinding.touchRegion(0f, 0f, 0.5f, 0.5f));
    set.add(zone);
    set.update(0f);
    assertFalse(zone.pressed());
  }

  @Test
  void customLambdaBindingEvaluated() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital custom = new FlixelActionDigital("custom");
    boolean[] active = { true };
    custom.addBinding("lambda", () -> active[0]);
    set.add(custom);

    set.update(0f);
    assertTrue(custom.pressed());

    active[0] = false;
    set.update(0f);
    assertFalse(custom.pressed());
  }

  @Test
  void analogFromKeysAndNormalizeDiagonal() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("negX", FlixelAnalogBinding.negXKey(FlixelKey.LEFT));
    move.addBinding("posX", FlixelAnalogBinding.posXKey(FlixelKey.RIGHT));
    move.addBinding("negY", FlixelAnalogBinding.negYKey(FlixelKey.DOWN));
    move.addBinding("posY", FlixelAnalogBinding.posYKey(FlixelKey.UP));
    set.add(move);

    Flixel.keys.keyDown(FlixelKey.RIGHT);
    Flixel.keys.keyDown(FlixelKey.UP);
    set.update(0f);
    float len = (float) Math.sqrt(move.getX() * move.getX() + move.getY() * move.getY());
    assertEquals(1f, len, 1e-5f);
  }

  @Test
  void registryRegistersOnConstructAndClearsOnDestroy() {
    FlixelActionSet set = new FlixelActionSet(true) {
    };
    assertEquals(1, FlixelActionSets.registeredCountForTests());
    set.destroy();
    assertEquals(0, FlixelActionSets.registeredCountForTests());
  }

  @Test
  void analogFlickedFiresOncePerDeflection() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog navigate = new FlixelActionAnalog("navigate");
    navigate.addBinding("down", FlixelAnalogBinding.negYKey(FlixelKey.DOWN));
    navigate.addBinding("up", FlixelAnalogBinding.posYKey(FlixelKey.UP));
    set.add(navigate);

    // First press: flicked() fires on the first frame.
    Flixel.keys.keyDown(FlixelKey.DOWN);
    set.update(0f);
    assertTrue(navigate.flicked());

    // Held: flicked() must not fire again while the stick stays past the threshold.
    set.endFrame();
    set.update(0f);
    assertFalse(navigate.flicked());

    // Released: magnitude drops below threshold, flicked() stays false.
    Flixel.keys.keyUp(FlixelKey.DOWN);
    set.endFrame();
    set.update(0f);
    assertFalse(navigate.flicked());

    // Second press in the opposite direction: flicked() fires again.
    Flixel.keys.keyDown(FlixelKey.UP);
    set.endFrame();
    set.update(0f);
    assertTrue(navigate.flicked());
  }

  @Test
  void digitalRepeatedFiresOnPressAndAfterHoldDelay() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital scroll = new FlixelActionDigital("scroll");
    scroll.addBinding("key", FlixelDigitalBinding.key(FlixelKey.DOWN));
    scroll.setHoldDelay(0.5f);
    scroll.setHoldInterval(0.1f);
    set.add(scroll);

    // Initial press: repeated() fires immediately.
    Flixel.keys.keyDown(FlixelKey.DOWN);
    set.update(0f);
    assertTrue(scroll.held());

    // Still held but before holdDelay: repeated() does not fire again.
    set.endFrame();
    set.update(0.3f);
    assertFalse(scroll.held());

    // Still held, holdDelay elapsed: first hold-repeat fires.
    set.endFrame();
    set.update(0.25f);
    assertTrue(scroll.held());

    // Still held, holdInterval not yet elapsed: no repeat.
    set.endFrame();
    set.update(0.04f);
    assertFalse(scroll.held());

    // holdInterval elapsed: repeat fires again.
    set.endFrame();
    set.update(0.06f);
    assertTrue(scroll.held());

    // Released: no repeat.
    Flixel.keys.keyUp(FlixelKey.DOWN);
    set.endFrame();
    set.update(0f);
    assertFalse(scroll.held());
  }

  @Test
  void analogFlickedRepeatingFiresOnFlickAndAfterHoldDelay() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog navigate = new FlixelActionAnalog("navigate");
    navigate.addBinding("down", FlixelAnalogBinding.negYKey(FlixelKey.DOWN));
    navigate.setHoldDelay(0.5f);
    navigate.setHoldInterval(0.1f);
    set.add(navigate);

    // Initial flick: flickedRepeating() fires on the first frame.
    Flixel.keys.keyDown(FlixelKey.DOWN);
    set.update(0f);
    assertTrue(navigate.flickedRepeating());

    // Held but before holdDelay: no repeat.
    set.endFrame();
    set.update(0.3f);
    assertFalse(navigate.flickedRepeating());

    // holdDelay elapsed: first hold-repeat fires.
    set.endFrame();
    set.update(0.25f);
    assertTrue(navigate.flickedRepeating());

    // Released (magnitude drops below threshold): no repeat.
    Flixel.keys.keyUp(FlixelKey.DOWN);
    set.endFrame();
    set.update(0f);
    assertFalse(navigate.flickedRepeating());
  }

  @Test
  void digitalNamedSlotOverwritesPreviousBinding() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    jump.addBinding("keyboard", FlixelDigitalBinding.key(FlixelKey.F));
    jump.addBinding("keyboard", FlixelDigitalBinding.key(FlixelKey.G));
    set.add(jump);

    // Old key (F) must no longer fire.
    Flixel.keys.keyDown(FlixelKey.F);
    set.update(0f);
    assertFalse(jump.pressed());

    // New key (G) must fire.
    Flixel.keys.keyDown(FlixelKey.G);
    set.update(0f);
    assertTrue(jump.pressed());
  }

  @Test
  void digitalRemoveBindingBySlotStopsFiring() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    jump.addBinding("keyboard", FlixelDigitalBinding.key(FlixelKey.SPACE));
    set.add(jump);

    assertTrue(jump.removeBinding("keyboard"));
    Flixel.keys.keyDown(FlixelKey.SPACE);
    set.update(0f);
    assertFalse(jump.pressed());
  }

  @Test
  void digitalRemoveNonExistentSlotReturnsFalse() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    set.add(jump);
    assertFalse(jump.removeBinding("keyboard"));
  }

  @Test
  void digitalRemoveBindingByReferenceStopsFiring() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    FlixelDigitalBinding binding = FlixelDigitalBinding.key(FlixelKey.SPACE);
    jump.addBinding("key", binding);
    set.add(jump);

    assertTrue(jump.removeBinding(binding));
    Flixel.keys.keyDown(FlixelKey.SPACE);
    set.update(0f);
    assertFalse(jump.pressed());
  }

  @Test
  void digitalClearBindingsStopsAllFiring() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    jump.addBinding("keyboard", FlixelDigitalBinding.key(FlixelKey.SPACE));
    jump.addBinding("enter", FlixelDigitalBinding.key(FlixelKey.ENTER));
    set.add(jump);

    jump.clearBindings();
    Flixel.keys.keyDown(FlixelKey.SPACE);
    Flixel.keys.keyDown(FlixelKey.ENTER);
    set.update(0f);
    assertFalse(jump.pressed());
  }

  @Test
  void analogNamedSlotOverwritesPreviousBinding() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("left", FlixelAnalogBinding.negXKey(FlixelKey.LEFT));
    move.addBinding("left", FlixelAnalogBinding.negXKey(FlixelKey.A));
    set.add(move);

    // Old key (LEFT) must no longer contribute.
    Flixel.keys.keyDown(FlixelKey.LEFT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);

    // New key (A) must contribute.
    Flixel.keys.keyDown(FlixelKey.A);
    set.update(0f);
    assertEquals(-1f, move.getX(), 1e-5f);
  }

  @Test
  void analogRemoveBindingBySlotStopsContributing() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("right", FlixelAnalogBinding.posXKey(FlixelKey.RIGHT));
    set.add(move);

    assertTrue(move.removeBinding("right"));
    Flixel.keys.keyDown(FlixelKey.RIGHT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);
  }

  @Test
  void analogRemoveBindingByReferenceStopsContributing() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    FlixelAnalogBinding binding = FlixelAnalogBinding.posXKey(FlixelKey.RIGHT);
    move.addBinding("right", binding);
    set.add(move);

    assertTrue(move.removeBinding(binding));
    Flixel.keys.keyDown(FlixelKey.RIGHT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);
  }

  @Test
  void analogClearBindingsStopsAllContributions() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("left", FlixelAnalogBinding.negXKey(FlixelKey.LEFT));
    move.addBinding("right", FlixelAnalogBinding.posXKey(FlixelKey.RIGHT));
    set.add(move);

    move.clearBindings();
    Flixel.keys.keyDown(FlixelKey.RIGHT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);
  }

  @Test
  void steamReaderMergesDigital() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    set.steamReader = new FlixelSteamActionReader() {
      @Override
      public boolean getDigital(String actionName) {
        return "jump".equals(actionName);
      }

      @Override
      public float getAnalogX(String actionName) {
        return 0f;
      }

      @Override
      public float getAnalogY(String actionName) {
        return 0f;
      }
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    set.add(jump);
    set.update(0f);
    assertTrue(jump.pressed());
  }
}
