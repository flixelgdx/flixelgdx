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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link FlixelGamepadInputManager#firstPressed(int)},
 * {@link FlixelGamepadInputManager#firstJustPressed(int)}, and
 * {@link FlixelGamepadInputManager#firstJustReleased(int)}.
 *
 * <p>Internal state (slotController, currentButtons, previousButtons, pressedOrder) is injected via
 * reflection so tests remain isolated from any real controller hardware.
 */
class FlixelGamepadInputManagerTest {

  /**
   * Predictable, fixed button and axis indices for tests, with L2/R2 left
   * {@link FlixelControllerMapping#UNDEFINED} to exercise the synthetic trigger path.
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

    static FlixelControllerMapping create() {
      FlixelControllerMapping m = new FlixelControllerMapping();
      m.axisLeftX = 0;
      m.axisLeftY = 1;
      m.axisRightX = 2;
      m.axisRightY = 3;
      m.buttonA = NATIVE_A;
      m.buttonB = NATIVE_B;
      m.buttonX = NATIVE_X;
      m.buttonY = NATIVE_Y;
      m.buttonBack = NATIVE_BACK;
      m.buttonStart = NATIVE_START;
      m.buttonL1 = NATIVE_L1;
      m.buttonR1 = NATIVE_R1;
      m.buttonLeftStick = NATIVE_LEFT_STICK;
      m.buttonRightStick = NATIVE_RIGHT_STICK;
      m.buttonDpadUp = NATIVE_DPAD_UP;
      m.buttonDpadDown = NATIVE_DPAD_DOWN;
      m.buttonDpadLeft = NATIVE_DPAD_LEFT;
      m.buttonDpadRight = NATIVE_DPAD_RIGHT;
      // buttonL2 and buttonR2 stay UNDEFINED to drive the synthetic trigger path.
      return m;
    }
  }

  /** Minimal {@link FlixelController} stub backed by a configurable boolean button array. */
  private static final class StubController implements FlixelController {

    private final boolean[] buttons = new boolean[TestMapping.MAX_BUTTON + 1];
    private final FlixelControllerMapping mapping = TestMapping.create();

    @Override
    public boolean getButton(int buttonCode) {
      return buttonCode >= 0 && buttonCode < buttons.length && buttons[buttonCode];
    }

    @Override
    public FlixelControllerMapping getMapping() {
      return mapping;
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
    public String getName() {
      return "StubController";
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
    StubController stub = new StubController();
    injectController(stub);
  }

  @Test
  void firstPressedReturnsNoneWhenDisabled() {
    manager.enabled = false;
    assertEquals(FlixelGamepadInput.NONE, manager.firstPressed(0));
  }

  @Test
  void firstPressedReturnsNoneForNegativeId() {
    assertEquals(FlixelGamepadInput.NONE, manager.firstPressed(-1));
  }

  @Test
  void firstPressedReturnsNoneWhenIdExceedsActiveCount() {
    assertEquals(FlixelGamepadInput.NONE, manager.firstPressed(1));
  }

  @Test
  void firstPressedReturnsNoneWhenNothingHeld() {
    assertEquals(FlixelGamepadInput.NONE, manager.firstPressed(0));
  }

  @Test
  void firstPressedReturnsChronologicallyFirstButton() throws Exception {
    // B was pressed first, then A.
    addToPressedOrder(TestMapping.NATIVE_B);
    addToPressedOrder(TestMapping.NATIVE_A);
    setCurrent(TestMapping.NATIVE_A);
    setCurrent(TestMapping.NATIVE_B);

    assertEquals(FlixelGamepadInput.B, manager.firstPressed(0));
  }

  @Test
  void firstPressedReturnsSingleHeldButton() throws Exception {
    addToPressedOrder(TestMapping.NATIVE_Y);
    setCurrent(TestMapping.NATIVE_Y);

    assertEquals(FlixelGamepadInput.Y, manager.firstPressed(0));
  }

  @Test
  void firstJustPressedReturnsNoneWhenDisabled() {
    manager.enabled = false;
    assertEquals(FlixelGamepadInput.NONE, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsNoneWhenNothingChanged() throws Exception {
    setCurrent(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_A);

    assertEquals(FlixelGamepadInput.NONE, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsButtonThatTransitionedToPressed() throws Exception {
    setCurrent(TestMapping.NATIVE_A);
    // Previous is false by default.

    assertEquals(FlixelGamepadInput.A, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedSkipsButtonAlreadyHeldLastFrame() throws Exception {
    // B was held last frame; X is newly pressed.
    setCurrent(TestMapping.NATIVE_B);
    setPrevious(TestMapping.NATIVE_B);
    setCurrent(TestMapping.NATIVE_X);

    assertEquals(FlixelGamepadInput.X, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsSyntheticL2WhenTriggerCrossesThreshold() throws Exception {
    // L2 mapping is UNDEFINED in TestMapping, so the synthetic trigger slot is used.
    int syntheticL = syntheticTriggerL();
    setCurrent(syntheticL);

    assertEquals(FlixelGamepadInput.L2, manager.firstJustPressed(0));
  }

  @Test
  void firstJustPressedReturnsSyntheticR2WhenTriggerCrossesThreshold() throws Exception {
    int syntheticR = syntheticTriggerR();
    setCurrent(syntheticR);

    assertEquals(FlixelGamepadInput.R2, manager.firstJustPressed(0));
  }

  @Test
  void firstJustReleasedReturnsNoneWhenDisabled() {
    manager.enabled = false;
    assertEquals(FlixelGamepadInput.NONE, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsNoneWhenNothingChanged() throws Exception {
    setCurrent(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_A);

    assertEquals(FlixelGamepadInput.NONE, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsButtonThatTransitionedToReleased() throws Exception {
    // previous = pressed, current = released.
    setPrevious(TestMapping.NATIVE_B);

    assertEquals(FlixelGamepadInput.B, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedSkipsButtonStillHeld() throws Exception {
    // A is held both frames; B was released.
    setCurrent(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_A);
    setPrevious(TestMapping.NATIVE_B);

    assertEquals(FlixelGamepadInput.B, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsSyntheticL2WhenTriggerDropsBelowThreshold() throws Exception {
    int syntheticL = syntheticTriggerL();
    setPrevious(syntheticL);

    assertEquals(FlixelGamepadInput.L2, manager.firstJustReleased(0));
  }

  @Test
  void firstJustReleasedReturnsSyntheticR2WhenTriggerDropsBelowThreshold() throws Exception {
    int syntheticR = syntheticTriggerR();
    setPrevious(syntheticR);

    assertEquals(FlixelGamepadInput.R2, manager.firstJustReleased(0));
  }

  private void injectController(FlixelController c) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("slotController");
    f.setAccessible(true);
    ((FlixelController[]) f.get(manager))[0] = c;
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
