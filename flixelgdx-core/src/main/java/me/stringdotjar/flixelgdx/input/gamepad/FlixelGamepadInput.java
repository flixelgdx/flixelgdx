/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.gamepad;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Logical gamepad button and axis identifiers for {@link FlixelGamepadManager}, resolved to native
 * indices through each {@link Controller#getMapping()} (gdx-controllers SDL-style layout).
 */
public final class FlixelGamepadInput {

  private FlixelGamepadInput() {}

  public static final int NONE = -2;
  public static final int ANY = -1;

  public static final int A = 96;
  public static final int B = 97;
  public static final int C = 98;
  public static final int X = 99;
  public static final int Y = 100;
  public static final int Z = 101;
  public static final int L1 = 102;
  public static final int R1 = 103;
  public static final int L2 = 104;
  public static final int R2 = 105;
  public static final int THUMBL = 106;
  public static final int THUMBR = 107;
  public static final int START = 108;
  public static final int SELECT = 109;
  public static final int MODE = 110;
  public static final int CIRCLE = 255;

  public static final int DPAD_UP = 200;
  public static final int DPAD_DOWN = 201;
  public static final int DPAD_LEFT = 202;
  public static final int DPAD_RIGHT = 203;

  public static final int AXIS_LEFT_X = 0;
  public static final int AXIS_LEFT_Y = 1;
  public static final int AXIS_RIGHT_X = 2;
  public static final int AXIS_RIGHT_Y = 3;

  /**
   * Resolves a logical button code to the native button index for the given controller.
   *
   * @param controller Controller whose {@link Controller#getMapping()} is used.
   * @param logicalButton Value from this class, except {@link #ANY} and {@link #NONE}.
   * @return Native index, or {@link ControllerMapping#UNDEFINED} when unsupported.
   */
  public static int logicalButtonToNative(@NotNull Controller controller, int logicalButton) {
    ControllerMapping m = controller.getMapping();
    if (logicalButton == A) {
      return m.buttonA;
    }
    if (logicalButton == B) {
      return m.buttonB;
    }
    if (logicalButton == X) {
      return m.buttonX;
    }
    if (logicalButton == Y) {
      return m.buttonY;
    }
    if (logicalButton == L1) {
      return m.buttonL1;
    }
    if (logicalButton == R1) {
      return m.buttonR1;
    }
    if (logicalButton == L2) {
      return m.buttonL2;
    }
    if (logicalButton == R2) {
      return m.buttonR2;
    }
    if (logicalButton == THUMBL) {
      return m.buttonLeftStick;
    }
    if (logicalButton == THUMBR) {
      return m.buttonRightStick;
    }
    if (logicalButton == START) {
      return m.buttonStart;
    }
    if (logicalButton == SELECT) {
      return m.buttonBack;
    }
    if (logicalButton == DPAD_UP) {
      return m.buttonDpadUp;
    }
    if (logicalButton == DPAD_DOWN) {
      return m.buttonDpadDown;
    }
    if (logicalButton == DPAD_LEFT) {
      return m.buttonDpadLeft;
    }
    if (logicalButton == DPAD_RIGHT) {
      return m.buttonDpadRight;
    }
    if (logicalButton == C || logicalButton == Z || logicalButton == CIRCLE || logicalButton == MODE) {
      return ControllerMapping.UNDEFINED;
    }
    return ControllerMapping.UNDEFINED;
  }

  /**
   * Resolves a logical axis constant to the native axis index for the given controller.
   *
   * @param controller Controller whose {@link Controller#getMapping()} is used.
   * @param logicalAxis One of {@link #AXIS_LEFT_X}, {@link #AXIS_LEFT_Y}, {@link #AXIS_RIGHT_X}, or {@link #AXIS_RIGHT_Y}.
   * @return Native axis index, or {@link ControllerMapping#UNDEFINED} when unsupported.
   */
  public static int logicalAxisToNative(@NotNull Controller controller, int logicalAxis) {
    ControllerMapping m = controller.getMapping();
    if (logicalAxis == AXIS_LEFT_X) {
      return m.axisLeftX;
    }
    if (logicalAxis == AXIS_LEFT_Y) {
      return m.axisLeftY;
    }
    if (logicalAxis == AXIS_RIGHT_X) {
      return m.axisRightX;
    }
    if (logicalAxis == AXIS_RIGHT_Y) {
      return m.axisRightY;
    }
    return ControllerMapping.UNDEFINED;
  }

  /**
   * Resolves a button name to a logical button code.
   *
   * @param name Human-readable name (case insensitive), for example {@code "A"} or {@code "START"}.
   * @return A logical code from this class, or {@link Input.Keys#UNKNOWN} when not recognized.
   */
  public static int fromString(@Nullable String name) {
    if (name == null) {
      return Input.Keys.UNKNOWN;
    }
    return switch (name.trim().toUpperCase(Locale.ROOT)) {
      case "A" -> A;
      case "B" -> B;
      case "C" -> C;
      case "X" -> X;
      case "Y" -> Y;
      case "Z" -> Z;
      case "L1" -> L1;
      case "R1" -> R1;
      case "L2" -> L2;
      case "R2" -> R2;
      case "THUMBL", "LEFT_THUMB", "LEFTSTICK" -> THUMBL;
      case "THUMBR", "RIGHT_THUMB", "RIGHTSTICK" -> THUMBR;
      case "START" -> START;
      case "SELECT", "BACK" -> SELECT;
      case "MODE", "GUIDE" -> MODE;
      case "CIRCLE" -> CIRCLE;
      case "DPAD_UP", "UP" -> DPAD_UP;
      case "DPAD_DOWN", "DOWN" -> DPAD_DOWN;
      case "DPAD_LEFT", "LEFT" -> DPAD_LEFT;
      case "DPAD_RIGHT", "RIGHT" -> DPAD_RIGHT;
      case "ANY" -> ANY;
      case "NONE" -> NONE;
      default -> Input.Keys.UNKNOWN;
    };
  }

  /**
   * Returns a readable English name for a logical button code.
   *
   * @param logicalButtonCode Value from this class.
   * @return Description, or {@code "?"} when unknown.
   */
  @NotNull
  public static String toString(int logicalButtonCode) {
    if (logicalButtonCode == NONE) {
      return "NONE";
    }
    if (logicalButtonCode == ANY) {
      return "ANY";
    }
    if (logicalButtonCode == A) {
      return "A";
    }
    if (logicalButtonCode == B) {
      return "B";
    }
    if (logicalButtonCode == C) {
      return "C";
    }
    if (logicalButtonCode == X) {
      return "X";
    }
    if (logicalButtonCode == Y) {
      return "Y";
    }
    if (logicalButtonCode == Z) {
      return "Z";
    }
    if (logicalButtonCode == L1) {
      return "L1";
    }
    if (logicalButtonCode == R1) {
      return "R1";
    }
    if (logicalButtonCode == L2) {
      return "L2";
    }
    if (logicalButtonCode == R2) {
      return "R2";
    }
    if (logicalButtonCode == THUMBL) {
      return "THUMBL";
    }
    if (logicalButtonCode == THUMBR) {
      return "THUMBR";
    }
    if (logicalButtonCode == START) {
      return "START";
    }
    if (logicalButtonCode == SELECT) {
      return "SELECT";
    }
    if (logicalButtonCode == MODE) {
      return "MODE";
    }
    if (logicalButtonCode == CIRCLE) {
      return "CIRCLE";
    }
    if (logicalButtonCode == DPAD_UP) {
      return "DPAD_UP";
    }
    if (logicalButtonCode == DPAD_DOWN) {
      return "DPAD_DOWN";
    }
    if (logicalButtonCode == DPAD_LEFT) {
      return "DPAD_LEFT";
    }
    if (logicalButtonCode == DPAD_RIGHT) {
      return "DPAD_RIGHT";
    }
    return "?";
  }
}
