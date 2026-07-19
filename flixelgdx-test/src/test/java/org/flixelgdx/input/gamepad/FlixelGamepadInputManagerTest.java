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

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.ControllerPowerLevel;
import com.badlogic.gdx.utils.IntArray;

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
 * reflection so tests remain isolated from the gdx-controllers hardware layer.
 */
class FlixelGamepadInputManagerTest {

  /**
   * A {@link ControllerMapping} subclass with predictable, fixed button and axis indices for use
   * in tests. Indices are assigned sequentially starting at 0, with L2/R2 left as
   * {@link ControllerMapping#UNDEFINED} to exercise the synthetic trigger path.
   */
  private static final class TestMapping extends ControllerMapping {

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

    TestMapping() {
      super(
          0, 1, 2, 3,
          NATIVE_A, NATIVE_B, NATIVE_X, NATIVE_Y,
          NATIVE_BACK, NATIVE_START,
          NATIVE_L1, UNDEFINED,
          NATIVE_R1, UNDEFINED,
          NATIVE_LEFT_STICK, NATIVE_RIGHT_STICK,
          NATIVE_DPAD_UP, NATIVE_DPAD_DOWN, NATIVE_DPAD_LEFT, NATIVE_DPAD_RIGHT);
    }
  }

  /** Minimal {@link Controller} stub backed by a configurable boolean button array. */
  private static final class StubController implements Controller {

    private final boolean[] buttons = new boolean[TestMapping.MAX_BUTTON + 1];
    private final TestMapping mapping = new TestMapping();

    void press(int nativeButton) {
      buttons[nativeButton] = true;
    }

    void release(int nativeButton) {
      buttons[nativeButton] = false;
    }

    @Override
    public boolean getButton(int buttonCode) {
      return buttonCode >= 0 && buttonCode < buttons.length && buttons[buttonCode];
    }

    @Override
    public ControllerMapping getMapping() {
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
    public String getUniqueId() {
      return "stub-0";
    }

    @Override
    public boolean isConnected() {
      return true;
    }

    @Override
    public boolean canVibrate() {
      return false;
    }

    @Override
    public boolean isVibrating() {
      return false;
    }

    @Override
    public void startVibration(int duration, float strength) {}

    @Override
    public void cancelVibration() {}

    @Override
    public boolean supportsPlayerIndex() {
      return false;
    }

    @Override
    public int getPlayerIndex() {
      return Controller.PLAYER_IDX_UNSET;
    }

    @Override
    public void setPlayerIndex(int index) {}

    @Override
    public ControllerPowerLevel getPowerLevel() {
      return ControllerPowerLevel.POWER_UNKNOWN;
    }

    @Override
    public void addListener(ControllerListener listener) {}

    @Override
    public void removeListener(ControllerListener listener) {}
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
    // L2 mapping is UNDEFINED in TestMapping, so the synthetic trigger slot is used.
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

  private void injectController(Controller c) throws Exception {
    Field f = FlixelGamepadInputManager.class.getDeclaredField("slotController");
    f.setAccessible(true);
    ((Controller[]) f.get(manager))[0] = c;
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
    ((IntArray[]) f.get(manager))[0].add(nativeButton);
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
