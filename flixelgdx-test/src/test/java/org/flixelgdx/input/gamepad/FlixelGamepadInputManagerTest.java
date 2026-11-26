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
package org.flixelgdx.input.gamepad;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelGamepadInputManagerTest {

  private Pads provider;
  private FlixelGamepadInputManager manager;
  private Pad pad;

  @BeforeEach
  void setUp() {
    provider = new Pads();
    manager = new FlixelGamepadInputManager();
    manager.setGamepadProvider(provider);
    manager.addMappingResolver(g -> mapping());
    pad = provider.plug(new Pad("Test Pad"));
    manager.update();
  }

  private static FlixelGamepadMapping mapping() {
    FlixelGamepadMapping m = new FlixelGamepadMapping();
    m.registerButton(FlixelGamepadButton.A, 0);
    m.registerButton(FlixelGamepadButton.B, 1);
    m.registerButton(FlixelGamepadButton.X, 2);
    m.registerAxis(FlixelGamepadAxis.LEFT_X, 0);
    m.registerAxis(FlixelGamepadAxis.LEFT_Y, 1);
    m.registerAxis(FlixelGamepadAxis.RIGHT_X, 2);
    m.registerAxis(FlixelGamepadAxis.RIGHT_Y, 3);
    m.registerAxis(FlixelGamepadAxis.L2, 4);
    m.registerAxis(FlixelGamepadAxis.R2, 5);
    return m;
  }

  private void frame() {
    manager.endFrame();
    manager.update();
  }

  @Test
  void buttonsGoThroughPressedJustPressedAndJustReleased() {
    pad.buttons[0] = true;
    frame();
    assertTrue(manager.pressed(0, FlixelGamepadButton.A));
    assertTrue(manager.justPressed(0, FlixelGamepadButton.A));
    assertTrue(manager.anyPressed(FlixelGamepadButton.A));
    assertTrue(manager.anyJustPressed(FlixelGamepadButton.A));
    assertTrue(manager.pressed(0, FlixelGamepadButton.ANY));
    assertTrue(manager.justPressed(0, FlixelGamepadButton.ANY));
    assertEquals(FlixelGamepadButton.A, manager.firstPressed(0));
    assertEquals(FlixelGamepadButton.A, manager.firstJustPressed(0));
    assertTrue(manager.anyInput());
    assertEquals(0, manager.getFirstActiveGamepadId());

    frame();
    assertFalse(manager.justPressed(0, FlixelGamepadButton.A));
    assertFalse(manager.justPressed(0, FlixelGamepadButton.ANY));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(0));

    pad.buttons[0] = false;
    frame();
    assertTrue(manager.justReleased(0, FlixelGamepadButton.A));
    assertTrue(manager.justReleased(0, FlixelGamepadButton.ANY));
    assertTrue(manager.anyJustReleased(FlixelGamepadButton.A));
    assertEquals(FlixelGamepadButton.A, manager.firstJustReleased(0));
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(0));
    assertFalse(manager.anyPressed(FlixelGamepadButton.A));
    assertFalse(manager.anyInput());
    assertEquals(-1, manager.getFirstActiveGamepadId());
    assertNull(manager.getFirstActiveGamepad());

    frame();
    assertFalse(manager.justReleased(0, FlixelGamepadButton.ANY));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustReleased(0));
  }

  @Test
  void unmappedButtonsAndBadIdsReadAsReleased() {
    pad.buttons[7] = true;
    frame();
    assertFalse(manager.pressed(0, FlixelGamepadButton.X));
    assertFalse(manager.justPressed(0, FlixelGamepadButton.X));
    assertFalse(manager.justReleased(0, FlixelGamepadButton.X));
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(0), "an unmapped native button has no name");
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(0));
    assertFalse(manager.pressed(-1, FlixelGamepadButton.A));
    assertFalse(manager.justPressed(5, FlixelGamepadButton.A));
    assertFalse(manager.justReleased(5, FlixelGamepadButton.A));
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(5));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(5));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustReleased(5));
    assertEquals(0f, manager.getAxis(5, FlixelGamepadAxis.LEFT_X));
    assertFalse(manager.canVibrate(5));
  }

  @Test
  void axesRespectTheDeadZone() {
    pad.axes[0] = 0.1f;
    pad.axes[3] = -0.8f;
    frame();
    assertEquals(0.1f, manager.getAxis(0, FlixelGamepadAxis.LEFT_X), 1e-6f);
    assertTrue(manager.anyMovedXAxis());
    assertTrue(manager.anyMovedYAxis());
    assertTrue(manager.pressed(0, FlixelGamepadButton.ANY), "a moved stick counts as input");

    manager.globalDeadZone = 0.2f;
    assertEquals(0f, manager.getAxis(0, FlixelGamepadAxis.LEFT_X));
    assertFalse(manager.anyMovedXAxis());
    assertEquals(-0.8f, manager.getAxis(0, FlixelGamepadAxis.RIGHT_Y), 1e-6f);

    pad.axes[3] = 0f;
    pad.axes[1] = 0.5f;
    pad.axes[2] = 0.5f;
    frame();
    assertTrue(manager.anyMovedXAxis(), "the right stick counts for the x axis too");
    assertTrue(manager.anyMovedYAxis());
    assertEquals(0f, manager.getAxis(0, FlixelGamepadAxis.L2));
  }

  @Test
  void analogTriggersBecomeButtonsPastHalfway() {
    pad.axes[4] = 0.7f;
    pad.axes[5] = 0.3f;
    frame();
    assertEquals(0.7f, manager.getTriggerL(0), 1e-6f);
    assertEquals(0.3f, manager.getTriggerR(0), 1e-6f);
    assertTrue(manager.pressed(0, FlixelGamepadButton.L2));
    assertFalse(manager.pressed(0, FlixelGamepadButton.R2));
    assertTrue(manager.justPressed(0, FlixelGamepadButton.L2));
    assertEquals(FlixelGamepadButton.L2, manager.firstPressed(0));
    assertEquals(FlixelGamepadButton.L2, manager.firstJustPressed(0));
    assertTrue(manager.anyInput());

    pad.axes[4] = 0f;
    pad.axes[5] = 0.9f;
    frame();
    assertTrue(manager.justReleased(0, FlixelGamepadButton.L2));
    assertEquals(FlixelGamepadButton.L2, manager.firstJustReleased(0));
    assertEquals(FlixelGamepadButton.R2, manager.firstJustPressed(0));
    assertEquals(FlixelGamepadButton.R2, manager.firstPressed(0));
    assertTrue(manager.pressed(0, FlixelGamepadButton.ANY));

    pad.axes[5] = 0f;
    frame();
    assertEquals(FlixelGamepadButton.R2, manager.firstJustReleased(0));
  }

  @Test
  void digitalTriggersUseTheirButtons() {
    manager.addMappingResolver(g -> {
      FlixelGamepadMapping m = new FlixelGamepadMapping();
      m.registerButton(FlixelGamepadButton.L2, 6);
      m.registerButton(FlixelGamepadButton.R2, 7);
      return m;
    });
    Pad second = provider.plug(new Pad("Digital"));
    second.buttons[6] = true;
    frame();
    assertEquals(1f, manager.getTriggerL(1));
    assertEquals(0f, manager.getTriggerR(1));
    assertTrue(manager.pressed(1, FlixelGamepadButton.L2));
    assertEquals(FlixelGamepadButton.L2, manager.firstJustPressed(1));
  }

  @Test
  void connectAndDisconnectSignalsFire() {
    AtomicInteger connected = new AtomicInteger(-1);
    AtomicInteger disconnected = new AtomicInteger(-1);
    manager.deviceConnected.add(e -> {
      connected.set(e.gamepadId());
      assertNotNull(e.model());
    });
    manager.deviceDisconnected.add(e -> disconnected.set(e.gamepadId()));
    provider.plug(new Pad("Second"));
    manager.update();
    assertEquals(1, connected.get());
    assertEquals(FlixelGamepadModel.UNKNOWN, manager.getModel(-1));
    assertNotNull(manager.getModel(1));

    pad.buttons[0] = true;
    frame();
    provider.unplug(0);
    manager.update();
    assertEquals(0, disconnected.get());
    assertSame(provider.pads[0], manager.gamepadAt(0));
    assertFalse(manager.pressed(0, FlixelGamepadButton.A), "a new pad in the slot starts released");
  }

  @Test
  void devicesWrapASlot() {
    assertNull(manager.getById(0));
    assertNull(manager.getById(99));
    FlixelGamepadDevice d = manager.ensureDevice(0);
    assertSame(d, manager.ensureDevice(0));
    assertSame(d, manager.getById(0));
    assertThrows(IllegalArgumentException.class, () -> manager.ensureDevice(-1));
    assertEquals(0, d.getId());
    assertTrue(d.isConnected());
    assertNotNull(d.getModel());

    pad.buttons[0] = true;
    pad.axes[0] = 0.5f;
    pad.axes[1] = -0.5f;
    pad.axes[4] = 1f;
    pad.axes[5] = 1f;
    frame();
    assertTrue(d.pressed(FlixelGamepadButton.A));
    assertTrue(d.justPressed(FlixelGamepadButton.A));
    assertFalse(d.justReleased(FlixelGamepadButton.A));
    assertEquals(0.5f, d.getXAxis(), 1e-6f);
    assertEquals(-0.5f, d.getYAxis(), 1e-6f);
    assertEquals(0.5f, d.getAxis(FlixelGamepadAxis.LEFT_X), 1e-6f);
    assertEquals(1f, d.getTriggerL(), 1e-6f);
    assertEquals(1f, d.getTriggerR(), 1e-6f);
    assertSame(d, manager.getFirstActiveGamepad());

    FlixelGamepadDevice[] out = new FlixelGamepadDevice[4];
    assertEquals(1, manager.getActiveGamepads(out));
    assertSame(d, out[0]);
    assertEquals(0, manager.getActiveGamepads(null));
    int[] ids = new int[4];
    assertEquals(1, manager.getActiveGamepadIds(ids));
    assertEquals(0, manager.getActiveGamepadIds(null));
  }

  @Test
  void vibrationIsClampedAndOnlySentToPadsThatSupportIt() {
    FlixelGamepadDevice d = manager.ensureDevice(0);
    assertFalse(d.canVibrate());
    d.vibrate(1f);
    assertEquals(0, pad.vibrations);

    pad.rumble = true;
    assertTrue(d.canVibrate());
    d.vibrate(0.5f);
    assertEquals(500, pad.lastMs);
    assertEquals(1f, pad.lastLeft);
    d.vibrate(2f, 0.25f);
    assertEquals(1f, pad.lastLeft, "intensity is clamped to one");
    d.vibrate(-1f, 0.5f, 1f);
    assertEquals(0f, pad.lastLeft);
    assertEquals(0.5f, pad.lastRight);
    d.stopVibration();
    assertEquals(1, pad.cancels);
    manager.vibrate(5, 1f);
    manager.stopVibration(5);
  }

  @Test
  void aDisabledManagerReportsNothing() {
    pad.buttons[0] = true;
    frame();
    manager.enabled = false;
    manager.update();
    assertFalse(manager.pressed(0, FlixelGamepadButton.A));
    assertFalse(manager.anyPressed(FlixelGamepadButton.A));
    assertFalse(manager.anyJustPressed(FlixelGamepadButton.A));
    assertFalse(manager.anyJustReleased(FlixelGamepadButton.A));
    assertFalse(manager.anyInput());
    assertFalse(manager.anyMovedXAxis());
    assertFalse(manager.anyMovedYAxis());
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(0));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(0));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustReleased(0));
    assertEquals(0f, manager.getAxis(0, FlixelGamepadAxis.LEFT_X));
    assertFalse(manager.canVibrate(0));
    manager.vibrate(0, 1f);
    manager.stopVibration(0);
  }

  @Test
  void resolversArePickedNewestFirstAndCanBeRemoved() {
    FlixelGamepadMappingResolver none = g -> null;
    manager.addMappingResolver(none);
    provider.plug(new Pad("Second"));
    manager.update();
    assertEquals(0, manager.mappingAt(1).getButtonIndex(FlixelGamepadButton.A), "a null answer falls through");
    manager.removeMappingResolver(none);
    assertNull(manager.mappingAt(-1));
    assertNull(manager.gamepadAt(99));
    assertThrows(NullPointerException.class, () -> manager.addMappingResolver(null));
    assertThrows(NullPointerException.class, () -> manager.setGamepadProvider(null));
  }

  @Test
  void withoutResolversEveryPadGetsAnEmptyMapping() {
    FlixelGamepadInputManager plain = new FlixelGamepadInputManager();
    plain.setGamepadProvider(provider);
    plain.update();
    assertNotNull(plain.mappingAt(0));
    assertEquals(FlixelGamepadMapping.UNDEFINED, plain.mappingAt(0).getButtonIndex(FlixelGamepadButton.A));
  }

  @Test
  void resetForgetsEveryPad() {
    pad.buttons[0] = true;
    frame();
    manager.ensureDevice(0);
    manager.reset();
    assertNull(manager.gamepadAt(0));
    assertNull(manager.getById(0));
    assertFalse(manager.anyInput());
  }

  @Test
  void nothingHeldMeansNoFirstButton() {
    frame();
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(0));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(0));
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustReleased(0));
  }

  @Test
  void firstPressedFollowsTheOrderButtonsWerePressedIn() {
    pad.buttons[1] = true;
    frame();
    pad.buttons[0] = true;
    frame();
    assertEquals(FlixelGamepadButton.B, manager.firstPressed(0),
        "B went down first, even though A has the lower index");

    pad.buttons[1] = false;
    frame();
    assertEquals(FlixelGamepadButton.A, manager.firstPressed(0));
  }

  @Test
  void firstJustPressedSkipsButtonsHeldSinceLastFrame() {
    pad.buttons[1] = true;
    frame();
    pad.buttons[2] = true;
    frame();
    assertEquals(FlixelGamepadButton.X, manager.firstJustPressed(0));
  }

  @Test
  void firstJustReleasedSkipsButtonsStillHeld() {
    pad.buttons[0] = true;
    pad.buttons[1] = true;
    frame();
    pad.buttons[1] = false;
    frame();
    assertEquals(FlixelGamepadButton.B, manager.firstJustReleased(0));
  }

  @Test
  void customButtonsCanBeReadOnceMapped() {
    FlixelGamepadButton turbo = FlixelGamepadButton.of("Turbo");
    manager.addMappingResolver(g -> {
      FlixelGamepadMapping m = new FlixelGamepadMapping();
      m.registerButton(turbo, 3);
      return m;
    });
    Pad custom = provider.plug(new Pad("Custom"));
    custom.buttons[3] = true;
    frame();
    assertTrue(manager.pressed(1, turbo));
    assertEquals(turbo, manager.firstJustPressed(1), "a native button resolves back to its custom name");
  }

  private static final class Pads implements FlixelGamepadProvider {

    final Pad[] pads = new Pad[4];
    int count;

    Pad plug(Pad p) {
      pads[count++] = p;
      return p;
    }

    void unplug(int index) {
      pads[index] = new Pad("Replacement");
    }

    @Override
    public int getGamepadCount() {
      return count;
    }

    @Nullable
    @Override
    public FlixelGamepad getGamepadAt(int index) {
      return pads[index];
    }
  }

  private static final class Pad implements FlixelGamepad {

    final boolean[] buttons = new boolean[8];
    final float[] axes = new float[6];
    final String name;
    float lastLeft;
    float lastRight;
    int lastMs;
    int vibrations;
    int cancels;

    boolean rumble;

    Pad(String name) {
      this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
      return name;
    }

    @Override
    public int getMinButtonIndex() {
      return 0;
    }

    @Override
    public int getMaxButtonIndex() {
      return buttons.length - 1;
    }

    @Override
    public boolean getButton(int buttonIndex) {
      return buttons[buttonIndex];
    }

    @Override
    public int getAxisCount() {
      return axes.length;
    }

    @Override
    public float getAxis(int axisIndex) {
      return axes[axisIndex];
    }

    @Override
    public boolean canVibrate() {
      return rumble;
    }

    @Override
    public void startVibration(int durationMs, float leftIntensity, float rightIntensity) {
      vibrations++;
      lastMs = durationMs;
      lastLeft = leftIntensity;
      lastRight = rightIntensity;
    }

    @Override
    public void cancelVibration() {
      cancels++;
    }
  }
}
