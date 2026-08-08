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

/**
 * The per-device translation table from a standard gamepad layout to a controller's own raw button
 * and axis indices.
 *
 * <p>Every physical pad reports its buttons and sticks at different numeric indices. This mapping is
 * how the framework turns a friendly, layout-agnostic idea like "the bottom face button" into the
 * exact index that <i>this</i> controller uses for it. Game code never touches these fields directly;
 * it works with the logical constants in {@link FlixelGamepadInput}, and {@link FlixelGamepadInputManager}
 * resolves them through this mapping.
 *
 * <p>Any field left as {@link #UNDEFINED} means this controller does not report that input. A common
 * example is triggers: on the desktop backend L2 and R2 arrive as analog axes, so {@link #buttonL2}
 * and {@link #buttonR2} stay {@code UNDEFINED} and the manager synthesizes button state from the
 * axis value instead.
 *
 * <p>Each backend fills this in from its native controller data; the fields are left public and
 * writable so a backend can populate them cheaply without a wide constructor.
 *
 * @see FlixelController#getMapping()
 * @see FlixelGamepadInput
 */
public class FlixelControllerMapping {

  /** Value used for any button or axis this controller does not expose. */
  public static final int UNDEFINED = -1;

  /** Native index of the bottom face button (A on Xbox, Cross on PlayStation). */
  public int buttonA = UNDEFINED;

  /** Native index of the right face button (B on Xbox, Circle on PlayStation). */
  public int buttonB = UNDEFINED;

  /** Native index of the left face button (X on Xbox, Square on PlayStation). */
  public int buttonX = UNDEFINED;

  /** Native index of the top face button (Y on Xbox, Triangle on PlayStation). */
  public int buttonY = UNDEFINED;

  /** Native index of the left shoulder bumper (L1 / LB). */
  public int buttonL1 = UNDEFINED;

  /** Native index of the right shoulder bumper (R1 / RB). */
  public int buttonR1 = UNDEFINED;

  /** Native index of the left trigger as a button (L2 / LT), or {@link #UNDEFINED} when it is an axis. */
  public int buttonL2 = UNDEFINED;

  /** Native index of the right trigger as a button (R2 / RT), or {@link #UNDEFINED} when it is an axis. */
  public int buttonR2 = UNDEFINED;

  /** Native index of the left stick click (L3). */
  public int buttonLeftStick = UNDEFINED;

  /** Native index of the right stick click (R3). */
  public int buttonRightStick = UNDEFINED;

  /** Native index of the Start button. */
  public int buttonStart = UNDEFINED;

  /** Native index of the Back / Select button. */
  public int buttonBack = UNDEFINED;

  /** Native index of the D-pad up direction. */
  public int buttonDpadUp = UNDEFINED;

  /** Native index of the D-pad down direction. */
  public int buttonDpadDown = UNDEFINED;

  /** Native index of the D-pad left direction. */
  public int buttonDpadLeft = UNDEFINED;

  /** Native index of the D-pad right direction. */
  public int buttonDpadRight = UNDEFINED;

  /** Native index of the left stick horizontal axis. */
  public int axisLeftX = UNDEFINED;

  /** Native index of the left stick vertical axis. */
  public int axisLeftY = UNDEFINED;

  /** Native index of the right stick horizontal axis. */
  public int axisRightX = UNDEFINED;

  /** Native index of the right stick vertical axis. */
  public int axisRightY = UNDEFINED;

  /** Creates a mapping with every entry {@link #UNDEFINED}; a backend fills in what it supports. */
  public FlixelControllerMapping() {}
}
