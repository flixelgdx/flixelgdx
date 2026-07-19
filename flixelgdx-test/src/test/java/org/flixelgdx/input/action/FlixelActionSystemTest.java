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

import com.badlogic.gdx.Input;

import org.flixelgdx.Flixel;
import org.flixelgdx.GdxHeadlessExtension;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
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
    fire.addBinding(FlixelDigitalBinding.key(Input.Keys.F));
    set.add(fire);

    Flixel.keys.getInputProcessor().keyDown(Input.Keys.F);
    set.update(0f);
    assertTrue(fire.pressed());
    assertTrue(fire.justPressed());

    set.endFrame();
    set.update(0f);
    assertTrue(fire.pressed());
    assertFalse(fire.justPressed());

    Flixel.keys.getInputProcessor().keyUp(Input.Keys.F);
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
    any.addBinding(FlixelDigitalBinding.key(Input.Keys.A));
    any.addBinding(FlixelDigitalBinding.key(Input.Keys.B));
    set.add(any);

    Flixel.keys.getInputProcessor().keyDown(Input.Keys.B);
    set.update(0f);
    assertTrue(any.pressed());
    Flixel.keys.getInputProcessor().keyUp(Input.Keys.B);
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.A);
    set.update(0f);
    assertTrue(any.pressed());
  }

  @Test
  void touchRegionBindingFalseWhenNoPointers() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital zone = new FlixelActionDigital("zone");
    zone.addBinding(FlixelDigitalBinding.touchRegion(0f, 0f, 0.5f, 0.5f));
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
    custom.addBinding(() -> active[0]);
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
    move.addBinding(FlixelAnalogBinding.negXKey(Input.Keys.LEFT));
    move.addBinding(FlixelAnalogBinding.posXKey(Input.Keys.RIGHT));
    move.addBinding(FlixelAnalogBinding.negYKey(Input.Keys.DOWN));
    move.addBinding(FlixelAnalogBinding.posYKey(Input.Keys.UP));
    set.add(move);

    Flixel.keys.getInputProcessor().keyDown(Input.Keys.RIGHT);
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.UP);
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
    navigate.addBinding(FlixelAnalogBinding.negYKey(Input.Keys.DOWN));
    navigate.addBinding(FlixelAnalogBinding.posYKey(Input.Keys.UP));
    set.add(navigate);

    // First press: flicked() fires on the first frame.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.DOWN);
    set.update(0f);
    assertTrue(navigate.flicked());

    // Held: flicked() must not fire again while the stick stays past the threshold.
    set.endFrame();
    set.update(0f);
    assertFalse(navigate.flicked());

    // Released: magnitude drops below threshold, flicked() stays false.
    Flixel.keys.getInputProcessor().keyUp(Input.Keys.DOWN);
    set.endFrame();
    set.update(0f);
    assertFalse(navigate.flicked());

    // Second press in the opposite direction: flicked() fires again.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.UP);
    set.endFrame();
    set.update(0f);
    assertTrue(navigate.flicked());
  }

  @Test
  void digitalRepeatedFiresOnPressAndAfterHoldDelay() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital scroll = new FlixelActionDigital("scroll");
    scroll.addBinding(FlixelDigitalBinding.key(Input.Keys.DOWN));
    scroll.setHoldDelay(0.5f);
    scroll.setHoldInterval(0.1f);
    set.add(scroll);

    // Initial press: repeated() fires immediately.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.DOWN);
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
    Flixel.keys.getInputProcessor().keyUp(Input.Keys.DOWN);
    set.endFrame();
    set.update(0f);
    assertFalse(scroll.held());
  }

  @Test
  void analogFlickedRepeatingFiresOnFlickAndAfterHoldDelay() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog navigate = new FlixelActionAnalog("navigate");
    navigate.addBinding(FlixelAnalogBinding.negYKey(Input.Keys.DOWN));
    navigate.setHoldDelay(0.5f);
    navigate.setHoldInterval(0.1f);
    set.add(navigate);

    // Initial flick: flickedRepeating() fires on the first frame.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.DOWN);
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
    Flixel.keys.getInputProcessor().keyUp(Input.Keys.DOWN);
    set.endFrame();
    set.update(0f);
    assertFalse(navigate.flickedRepeating());
  }

  @Test
  void digitalNamedSlotOverwritesPreviousBinding() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    jump.addBinding("keyboard", FlixelDigitalBinding.key(Input.Keys.F));
    jump.addBinding("keyboard", FlixelDigitalBinding.key(Input.Keys.G));
    set.add(jump);

    // Old key (F) must no longer fire.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.F);
    set.update(0f);
    assertFalse(jump.pressed());

    // New key (G) must fire.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.G);
    set.update(0f);
    assertTrue(jump.pressed());
  }

  @Test
  void digitalRemoveBindingBySlotStopsFiring() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    jump.addBinding("keyboard", FlixelDigitalBinding.key(Input.Keys.SPACE));
    set.add(jump);

    assertTrue(jump.removeBinding("keyboard"));
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.SPACE);
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
    FlixelDigitalBinding binding = FlixelDigitalBinding.key(Input.Keys.SPACE);
    jump.addBinding(binding);
    set.add(jump);

    assertTrue(jump.removeBinding(binding));
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.SPACE);
    set.update(0f);
    assertFalse(jump.pressed());
  }

  @Test
  void digitalClearBindingsStopsAllFiring() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionDigital jump = new FlixelActionDigital("jump");
    jump.addBinding("keyboard", FlixelDigitalBinding.key(Input.Keys.SPACE));
    jump.addBinding(FlixelDigitalBinding.key(Input.Keys.ENTER));
    set.add(jump);

    jump.clearBindings();
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.SPACE);
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.ENTER);
    set.update(0f);
    assertFalse(jump.pressed());
  }

  @Test
  void analogNamedSlotOverwritesPreviousBinding() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("left", FlixelAnalogBinding.negXKey(Input.Keys.LEFT));
    move.addBinding("left", FlixelAnalogBinding.negXKey(Input.Keys.A));
    set.add(move);

    // Old key (LEFT) must no longer contribute.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.LEFT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);

    // New key (A) must contribute.
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.A);
    set.update(0f);
    assertEquals(-1f, move.getX(), 1e-5f);
  }

  @Test
  void analogRemoveBindingBySlotStopsContributing() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("right", FlixelAnalogBinding.posXKey(Input.Keys.RIGHT));
    set.add(move);

    assertTrue(move.removeBinding("right"));
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.RIGHT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);
  }

  @Test
  void analogRemoveBindingByReferenceStopsContributing() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    FlixelAnalogBinding binding = FlixelAnalogBinding.posXKey(Input.Keys.RIGHT);
    move.addBinding(binding);
    set.add(move);

    assertTrue(move.removeBinding(binding));
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.RIGHT);
    set.update(0f);
    assertEquals(0f, move.getX(), 1e-5f);
  }

  @Test
  void analogClearBindingsStopsAllContributions() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addBinding("left", FlixelAnalogBinding.negXKey(Input.Keys.LEFT));
    move.addBinding("right", FlixelAnalogBinding.posXKey(Input.Keys.RIGHT));
    set.add(move);

    move.clearBindings();
    Flixel.keys.getInputProcessor().keyDown(Input.Keys.RIGHT);
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
