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

import java.util.Locale;

/**
 * Logical gamepad button and axis identifiers for {@link FlixelGamepadInputManager}, resolved to
 * native indices through the {@link FlixelGamepadMapping} produced by the manager's resolver chain
 * at connect time.
 *
 * <h2>Important note on cross-platform button layouts</h2>
 *
 * <p>The desktop backend uses SDL with a large controller database, so many USB pads are remapped
 * to the same logical layout. The web backend reads the browser Gamepad API and always exposes the
 * W3C standard face-button indices through a fixed mapping. If a device reports non-standard button
 * ordering but the browser still labels the mapping as standard, logical {@code A} and {@code Y}
 * can disagree with what you see on desktop for the same physical pad. That is a platform and
 * driver limitation. Games that need perfect parity can offer a remap screen or branch on the
 * running platform and {@link FlixelGamepad#getName()}.
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
   * Logical constant for left trigger (L2) pressure, as returned by
   * {@link FlixelGamepadInputManager#getTriggerL(int)}.
   */
  public static final int AXIS_TRIGGER_L = 4;

  /**
   * Logical constant for right trigger (R2) pressure, as returned by
   * {@link FlixelGamepadInputManager#getTriggerR(int)}.
   */
  public static final int AXIS_TRIGGER_R = 5;

  /**
   * Resolves a logical button code to the native button index for the given mapping.
   *
   * @param mapping The mapping to look up against; must not be {@code null}.
   * @param logicalButton Value from this class, except {@link #ANY} and {@link #NONE}.
   * @return Native index, or {@link FlixelGamepadMapping#UNDEFINED} when unsupported.
   */
  public static int logicalButtonToNative(@NotNull FlixelGamepadMapping mapping, int logicalButton) {
    if (logicalButton == A) {
      return mapping.getButtonIndex(FlixelGamepadButton.A);
    }
    if (logicalButton == B) {
      return mapping.getButtonIndex(FlixelGamepadButton.B);
    }
    if (logicalButton == C) {
      return mapping.getButtonIndex(FlixelGamepadButton.C);
    }
    if (logicalButton == X) {
      return mapping.getButtonIndex(FlixelGamepadButton.X);
    }
    if (logicalButton == Y) {
      return mapping.getButtonIndex(FlixelGamepadButton.Y);
    }
    if (logicalButton == Z) {
      return mapping.getButtonIndex(FlixelGamepadButton.Z);
    }
    if (logicalButton == L1) {
      return mapping.getButtonIndex(FlixelGamepadButton.L1);
    }
    if (logicalButton == R1) {
      return mapping.getButtonIndex(FlixelGamepadButton.R1);
    }
    if (logicalButton == L2) {
      return mapping.getButtonIndex(FlixelGamepadButton.L2);
    }
    if (logicalButton == R2) {
      return mapping.getButtonIndex(FlixelGamepadButton.R2);
    }
    if (logicalButton == THUMBL) {
      return mapping.getButtonIndex(FlixelGamepadButton.LEFT_STICK);
    }
    if (logicalButton == THUMBR) {
      return mapping.getButtonIndex(FlixelGamepadButton.RIGHT_STICK);
    }
    if (logicalButton == START) {
      return mapping.getButtonIndex(FlixelGamepadButton.START);
    }
    if (logicalButton == SELECT) {
      return mapping.getButtonIndex(FlixelGamepadButton.BACK);
    }
    if (logicalButton == MODE) {
      return mapping.getButtonIndex(FlixelGamepadButton.MODE);
    }
    if (logicalButton == DPAD_UP) {
      return mapping.getButtonIndex(FlixelGamepadButton.DPAD_UP);
    }
    if (logicalButton == DPAD_DOWN) {
      return mapping.getButtonIndex(FlixelGamepadButton.DPAD_DOWN);
    }
    if (logicalButton == DPAD_LEFT) {
      return mapping.getButtonIndex(FlixelGamepadButton.DPAD_LEFT);
    }
    if (logicalButton == DPAD_RIGHT) {
      return mapping.getButtonIndex(FlixelGamepadButton.DPAD_RIGHT);
    }
    return FlixelGamepadMapping.UNDEFINED;
  }

  /**
   * Resolves a logical axis constant to the native axis index for the given mapping.
   *
   * <p>Stick axes ({@link #AXIS_LEFT_X}, {@link #AXIS_LEFT_Y}, {@link #AXIS_RIGHT_X},
   * {@link #AXIS_RIGHT_Y}) are resolved through the mapping's axis table. Trigger axes
   * ({@link #AXIS_TRIGGER_L}, {@link #AXIS_TRIGGER_R}) are handled separately by
   * {@link FlixelGamepadInputManager} and return {@link FlixelGamepadMapping#UNDEFINED} here.
   *
   * @param mapping The mapping to look up against; must not be {@code null}.
   * @param logicalAxis One of the {@code AXIS_*} constants in this class.
   * @return Native axis index, or {@link FlixelGamepadMapping#UNDEFINED} when unsupported.
   */
  public static int logicalAxisToNative(@NotNull FlixelGamepadMapping mapping, int logicalAxis) {
    if (logicalAxis == AXIS_LEFT_X) {
      return mapping.getAxisIndex(FlixelGamepadAxis.LEFT_X);
    }
    if (logicalAxis == AXIS_LEFT_Y) {
      return mapping.getAxisIndex(FlixelGamepadAxis.LEFT_Y);
    }
    if (logicalAxis == AXIS_RIGHT_X) {
      return mapping.getAxisIndex(FlixelGamepadAxis.RIGHT_X);
    }
    if (logicalAxis == AXIS_RIGHT_Y) {
      return mapping.getAxisIndex(FlixelGamepadAxis.RIGHT_Y);
    }
    return FlixelGamepadMapping.UNDEFINED;
  }

  /**
   * Resolves a button name to a logical button code.
   *
   * @param name Human-readable name (case-insensitive), for example {@code "A"} or {@code "START"}.
   * @return A logical code from this class, or {@link #NONE} when not recognized.
   */
  public static int fromString(@Nullable String name) {
    if (name == null) {
      return NONE;
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
      default -> NONE;
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
