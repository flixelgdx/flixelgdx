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
package org.flixelgdx.input.mouse;

/**
 * Mouse button codes for {@link FlixelMouseInputManager} and the pointer events on
 * {@link org.flixelgdx.input.FlixelMouseListener FlixelMouseListener}.
 *
 * <p>These are plain integer constants so they cost nothing and read clearly at the call site, for
 * example {@code Flixel.mouse.pressed(FlixelMouseButton.LEFT)}.
 */
public final class FlixelMouseButton {

  /** The primary (usually left) mouse button. */
  public static final int LEFT = 0;

  /** The secondary (usually right) mouse button. */
  public static final int RIGHT = 1;

  /** The middle mouse button, typically the scroll-wheel click. */
  public static final int MIDDLE = 2;

  /** The "back" side button found on many mice. */
  public static final int BACK = 3;

  /** The "forward" side button found on many mice. */
  public static final int FORWARD = 4;

  private FlixelMouseButton() {}
}
