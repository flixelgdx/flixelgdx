/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.Input;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.GdxHeadlessExtension;
import me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager;

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
    FlixelActionSet set = new FlixelActionSet(false) {};
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
    FlixelActionSet set = new FlixelActionSet(false) {};
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
    FlixelActionSet set = new FlixelActionSet(false) {};
    FlixelActionDigital zone = new FlixelActionDigital("zone");
    zone.addBinding(FlixelInputBinding.touchRegion(0f, 0f, 0.5f, 0.5f));
    set.add(zone);
    set.update(0f);
    assertFalse(zone.pressed());
  }

  @Test
  void analogFromKeysAndNormalizeDiagonal() {
    FlixelActionSet set = new FlixelActionSet(false) {};
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
    FlixelActionSet set = new FlixelActionSet(true) {};
    assertEquals(1, FlixelActionSets.registeredCountForTests());
    set.destroy();
    assertEquals(0, FlixelActionSets.registeredCountForTests());
  }

  @Test
  void steamReaderMergesDigital() {
    FlixelActionSet set = new FlixelActionSet(false) {};
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
