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

import org.flixelgdx.collections.FlixelIntArray;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FlixelGamepadInputManager#firstPressed(int)},
 * {@link FlixelGamepadInputManager#firstJustPressed(int)}, and
 * {@link FlixelGamepadInputManager#firstJustReleased(int)}.
 *
 * <p>Internal state (slotGamepads, slotMappings, currentButtons, previousButtons, pressedOrder) is
 * injected via reflection so tests remain isolated from any real controller hardware.
 */
class FlixelGamepadInputManagerTest {

  /**
   * Predictable, fixed button and axis indices for tests, with L2/R2 left unmapped to exercise
   * the synthetic trigger path.
   */
  private static final class TestMapping {

    static final int NATIVE_A = 0;
    static final int NATIVE_B = 1;
    static final int NATIVE_X = 2;
    static final int NATIVE_Y = 3;
    static final int NATIVE_L1 = 4;
    static final int NATIVE_R1 = 5;
    static final int NATIVE_BACK = 6;
    static final int NATIVE_START = 7;
    static final int NATIVE_LEFT_STICK = 8;
    static final int NATIVE_RIGHT_STICK = 9;
    static final int NATIVE_DPAD_UP = 10;
    static final int NATIVE_DPAD_DOWN = 11;
    static final int NATIVE_DPAD_LEFT = 12;
    static final int NATIVE_DPAD_RIGHT = 13;
    static final int MAX_BUTTON = 13;

    private TestMapping() {}

    static FlixelGamepadMapping create() {
      FlixelGamepadMapping m = new FlixelGamepadMapping();
      m.registerAxis(FlixelGamepadAxis.LEFT_X, 0);
      m.registerAxis(FlixelGamepadAxis.LEFT_Y, 1);
      m.registerAxis(FlixelGamepadAxis.RIGHT_X, 2);
      m.registerAxis(FlixelGamepadAxis.RIGHT_Y, 3);
      m.register(FlixelGamepadButton.A, NATIVE_A);
      m.register(FlixelGamepadButton.B, NATIVE_B);
      m.register(FlixelGamepadButton.X, NATIVE_X);
      m.register(FlixelGamepadButton.Y, NATIVE_Y);
      m.register(FlixelGamepadButton.BACK, NATIVE_BACK);
      m.register(FlixelGamepadButton.START, NATIVE_START);
      m.register(FlixelGamepadButton.L1, NATIVE_L1);
      m.register(FlixelGamepadButton.R1, NATIVE_R1);
      m.register(FlixelGamepadButton.LEFT_STICK, NATIVE_LEFT_STICK);
      m.register(FlixelGamepadButton.RIGHT_STICK, NATIVE_RIGHT_STICK);
      m.register(FlixelGamepadButton.DPAD_UP, NATIVE_DPAD_UP);
      m.register(FlixelGamepadButton.DPAD_DOWN, NATIVE_DPAD_DOWN);
      m.register(FlixelGamepadButton.DPAD_LEFT, NATIVE_DPAD_LEFT);
      m.register(FlixelGamepadButton.DPAD_RIGHT, NATIVE_DPAD_RIGHT);
      // L2 and R2 are intentionally not registered to drive the synthetic trigger path.
      return m;
    }
  }

  /** Minimal {@link FlixelGamepad} stub backed by a configurable boolean button array. */
  private static final class StubGamepad implements FlixelGamepad {

    private final boolean[] buttons = new boolean[TestMapping.MAX_BUTTON + 1];

    @Override
    public boolean getButton(int buttonCode) {
      return buttonCode >= 0 && buttonCode < buttons.length && buttons[buttonCode];
    }

    @Override
    public int getMinButtonIndex() {
      return 0;
    }

    @Override
    public int getMaxButtonIndex() {
      return TestMapping.MAX_BUTTON;
    }

    @Override
    public int getAxisCount() {
      return 6;
    }

    @Override
    public float getAxis(int axisCode) {
      return 0f;
    }

    @Override
    public @NotNull String getName() {
      return "StubGamepad";
    }

    @Override
    public boolean canVibrate() {
      return false;
    }

    @Override
    public void startVibration(int duration, float strength) {}

    @Override
    public void cancelVibration() {}
  }

  private FlixelGamepadInputManager manager;

  @BeforeEach
  void setUp() throws Exception {
    manager = new FlixelGamepadInputManager();
    manager.enabled = true;
    manager.numActiveGamepads = 1;
    StubGamepad stub = new StubGamepad();
    injectGamepad(stub);
    injectMapping(TestMapping.create());
  }

  @Test
  void firstPressedReturnsNoneWhenDisabled() {
    manager.enabled = false;
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(0));
  }

  @Test
  void firstPressedReturnsNoneForNegativeId() {
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(-1));
  }

  @Test
  void firstPressedReturnsNoneWhenIdExceedsActiveCount() {
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(1));
  }

  @Test
  void firstPressedReturnsNoneWhenNothingHeld() {
    assertEquals(FlixelGamepadButton.NONE, manager.firstPressed(0));
  }

  @Test
  void firstPressedReturnsChronologicallyFirstButton() throws Exception {
    // B was pressed first, then A.
    addToPressedOrder(TestMapping.NATIVE_B);
    addToPressedOrder(TestMapping.NATIVE_A);
    setCurrent(TestMapping.NATIVE_A);
    setCurrent(TestMapping.NATIVE_B);

    assertEquals(FlixelGamepadButton.B, manager.firstPressed(0));
  }

  @Test
  void firstPressedReturnsSingleHeldButton() throws Exception {
    addToPressedOrder(TestMapping.NATIVE_Y);
    setCurrent(TestMapping.NATIVE_Y);

    assertEquals(FlixelGamepadButton.Y, manager.firstPressed(0));
  }

  @Test
  void firstJustPressedReturnsNoneWhenDisabled() {
    manager.enabled = false;
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsNoneWhenNothingChanged() throws Exception {
    setCurrent(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_A);

    assertEquals(FlixelGamepadButton.NONE, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsButtonThatTransitionedToPressed() throws Exception {
    setCurrent(TestMapping.NATIVE_A);
    // Previous is false by default.

    assertEquals(FlixelGamepadButton.A, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedSkipsButtonAlreadyHeldLastFrame() throws Exception {
    // B was held last frame; X is newly pressed.
    setCurrent(TestMapping.NATIVE_B);
    setPrevious(TestMapping.NATIVE_B);
    setCurrent(TestMapping.NATIVE_X);

    assertEquals(FlixelGamepadButton.X, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsSyntheticL2WhenTriggerCrossesThreshold() throws Exception {
    // L2 is not registered in TestMapping, so the synthetic trigger slot is used.
    int syntheticL = syntheticTriggerL();
    setCurrent(syntheticL);

    assertEquals(FlixelGamepadButton.L2, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsSyntheticR2WhenTriggerCrossesThreshold() throws Exception {
    int syntheticR = syntheticTriggerR();
    setCurrent(syntheticR);

    assertEquals(FlixelGamepadButton.R2, manager.firstJustPressed(0));
  }

  @Test
  void firstJustReleasedReturnsNoneWhenDisabled() {
    manager.enabled = false;
    assertEquals(FlixelGamepadButton.NONE, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsNoneWhenNothingChanged() throws Exception {
    setCurrent(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_A);

    assertEquals(FlixelGamepadButton.NONE, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsButtonThatTransitionedToReleased() throws Exception {
    // previous = pressed, current = released.
    setPrevious(TestMapping.NATIVE_B);

    assertEquals(FlixelGamepadButton.B, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedSkipsButtonStillHeld() throws Exception {
    // A is held both frames; B was released.
    setCurrent(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_B);

    assertEquals(FlixelGamepadButton.B, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsSyntheticL2WhenTriggerDropsBelowThreshold() throws Exception {
    int syntheticL = syntheticTriggerL();
    setPrevious(syntheticL);

    assertEquals(FlixelGamepadButton.L2, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsSyntheticR2WhenTriggerDropsBelowThreshold() throws Exception {
    int syntheticR = syntheticTriggerR();
    setPrevious(syntheticR);

    assertEquals(FlixelGamepadButton.R2, manager.firstJustReleased(0));
  }

  @Test
  void customButtonIsPollableEndToEnd() throws Exception {
    // The point of unifying on FlixelGamepadButton: a button the framework never heard of can be
    // minted, registered in a mapping, and then polled through the exact same calls as A/B/X/Y.
    FlixelGamepadButton turbo = FlixelGamepadButton.of("Turbo");
    FlixelGamepadMapping custom = new FlixelGamepadMapping();
    custom.register(turbo, TestMapping.NATIVE_A);
    injectMapping(custom);

    setCurrent(TestMapping.NATIVE_A);

    assertTrue(manager.pressed(0, turbo), "A custom button must be readable once mapped.");
    assertEquals(turbo, manager.firstJustPressed(0),
        "firstJustPressed must resolve a native index back to its custom button.");
  }

  private void injectGamepad(FlixelGamepad g) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("slotGamepads");
    f.setAccessible(true);
    ((FlixelGamepad[]) f.get(manager))[0] = g;
  }

  private void injectMapping(FlixelGamepadMapping m) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("slotMappings");
    f.setAccessible(true);
    ((FlixelGamepadMapping[]) f.get(manager))[0] = m;
  }

  private void setCurrent(int button) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("currentButtons");
    f.setAccessible(true);
    ((boolean[][]) f.get(manager))[0][button] = true;
  }

  private void setPrevious(int button) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("previousButtons");
    f.setAccessible(true);
    ((boolean[][]) f.get(manager))[0][button] = true;
  }

  private void addToPressedOrder(int nativeButton) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("pressedOrder");
    f.setAccessible(true);
    ((FlixelIntArray[]) f.get(manager))[0].add(nativeButton);
  }

  private static int syntheticTriggerL() throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("SYNTHETIC_TRIGGER_L");
    f.setAccessible(true);
    return f.getInt(null);
  }

  private static int syntheticTriggerR() throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("SYNTHETIC_TRIGGER_R");
    f.setAccessible(true);
    return f.getInt(null);
  }
}
