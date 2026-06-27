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
    fire.addBinding(FlixelInputBinding.key(Input.Keys.F));
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
    any.addBinding(FlixelInputBinding.key(Input.Keys.A));
    any.addBinding(FlixelInputBinding.key(Input.Keys.B));
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
    zone.addBinding(FlixelInputBinding.touchRegion(0f, 0f, 0.5f, 0.5f));
    set.add(zone);
    set.update(0f);
    assertFalse(zone.pressed());
  }

  @Test
  void analogFromKeysAndNormalizeDiagonal() {
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog move = new FlixelActionAnalog("move");
    move.addAxisBinding(FlixelAnalogAxisBinding.negXKey(Input.Keys.LEFT));
    move.addAxisBinding(FlixelAnalogAxisBinding.posXKey(Input.Keys.RIGHT));
    move.addAxisBinding(FlixelAnalogAxisBinding.negYKey(Input.Keys.DOWN));
    move.addAxisBinding(FlixelAnalogAxisBinding.posYKey(Input.Keys.UP));
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
    navigate.addAxisBinding(FlixelAnalogAxisBinding.negYKey(Input.Keys.DOWN));
    navigate.addAxisBinding(FlixelAnalogAxisBinding.posYKey(Input.Keys.UP));
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
  void negGamepadAxisYInvertsRelativeToGamepadAxisY() {
    // Simulate a gamepad axis returning a positive raw Y value (screen-space "down").
    // negGamepadAxisY should subtract that value, yielding negative Y (math-space "down"),
    // while plain gamepadAxisY would yield positive Y - the classic inversion bug.
    FlixelActionSet set = new FlixelActionSet(false) {
    };
    FlixelActionAnalog stickNeg = new FlixelActionAnalog("stickNeg");
    FlixelActionAnalog stickPlain = new FlixelActionAnalog("stickPlain");

    // Only add key bindings so we can drive Y without a real controller.
    // negYKey contributes -1 to Y, mirroring what negGamepadAxisY does for a positive raw axis.
    stickNeg.addAxisBinding(FlixelAnalogAxisBinding.negYKey(Input.Keys.S));
    stickPlain.addAxisBinding(FlixelAnalogAxisBinding.posYKey(Input.Keys.S));
    set.add(stickNeg);
    set.add(stickPlain);

    Flixel.keys.getInputProcessor().keyDown(Input.Keys.S);
    set.update(0f);

    // negYKey drives Y negative - same sign that negGamepadAxisY produces for a positive raw axis.
    assertTrue(stickNeg.getY() < 0f);
    // posYKey drives Y positive - same sign that plain gamepadAxisY produces (the inverted case).
    assertTrue(stickPlain.getY() > 0f);
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
