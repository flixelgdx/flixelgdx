/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.gamepad;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;

import org.jetbrains.annotations.NotNull;

/**
 * Default SDL-style mapping using {@link ControllerMapping} field names (A as south face, and so on).
 */
final class FlixelStandardGamepadMapping extends FlixelGamepadMapping {

  private final FlixelGamepadModel model;

  FlixelStandardGamepadMapping(@NotNull FlixelGamepadModel model) {
    this.model = model;
  }

  @Override
  @NotNull
  public FlixelGamepadModel getModel() {
    return model;
  }

  @Override
  public int toNativeButton(@NotNull Controller controller, int logicalButton) {
    ControllerMapping m = controller.getMapping();
    if (logicalButton == FlixelGamepadInput.A) {
      return m.buttonA;
    }
    if (logicalButton == FlixelGamepadInput.B) {
      return m.buttonB;
    }
    if (logicalButton == FlixelGamepadInput.X) {
      return m.buttonX;
    }
    if (logicalButton == FlixelGamepadInput.Y) {
      return m.buttonY;
    }
    if (logicalButton == FlixelGamepadInput.L1) {
      return m.buttonL1;
    }
    if (logicalButton == FlixelGamepadInput.R1) {
      return m.buttonR1;
    }
    if (logicalButton == FlixelGamepadInput.L2) {
      return m.buttonL2;
    }
    if (logicalButton == FlixelGamepadInput.R2) {
      return m.buttonR2;
    }
    if (logicalButton == FlixelGamepadInput.THUMBL) {
      return m.buttonLeftStick;
    }
    if (logicalButton == FlixelGamepadInput.THUMBR) {
      return m.buttonRightStick;
    }
    if (logicalButton == FlixelGamepadInput.START) {
      return m.buttonStart;
    }
    if (logicalButton == FlixelGamepadInput.SELECT) {
      return m.buttonBack;
    }
    if (logicalButton == FlixelGamepadInput.DPAD_UP) {
      return m.buttonDpadUp;
    }
    if (logicalButton == FlixelGamepadInput.DPAD_DOWN) {
      return m.buttonDpadDown;
    }
    if (logicalButton == FlixelGamepadInput.DPAD_LEFT) {
      return m.buttonDpadLeft;
    }
    if (logicalButton == FlixelGamepadInput.DPAD_RIGHT) {
      return m.buttonDpadRight;
    }
    if (logicalButton == FlixelGamepadInput.C
      || logicalButton == FlixelGamepadInput.Z
      || logicalButton == FlixelGamepadInput.CIRCLE
      || logicalButton == FlixelGamepadInput.MODE) {
      return ControllerMapping.UNDEFINED;
    }
    return ControllerMapping.UNDEFINED;
  }

  @Override
  public int toNativeAxis(@NotNull Controller controller, int logicalAxis) {
    ControllerMapping m = controller.getMapping();
    if (logicalAxis == FlixelGamepadInput.AXIS_LEFT_X) {
      return m.axisLeftX;
    }
    if (logicalAxis == FlixelGamepadInput.AXIS_LEFT_Y) {
      return m.axisLeftY;
    }
    if (logicalAxis == FlixelGamepadInput.AXIS_RIGHT_X) {
      return m.axisRightX;
    }
    if (logicalAxis == FlixelGamepadInput.AXIS_RIGHT_Y) {
      return m.axisRightY;
    }
    return ControllerMapping.UNDEFINED;
  }
}
