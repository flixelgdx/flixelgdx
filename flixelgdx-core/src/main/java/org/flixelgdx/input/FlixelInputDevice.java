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
package org.flixelgdx.input;

import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.mouse.FlixelMouseButton;

/**
 * The low-level input backend: the one interface each platform implements so the framework can read
 * the keyboard and pointer without naming a specific windowing library. Reached through
 * {@link org.flixelgdx.Flixel#input Flixel.input}.
 *
 * <p>Input arrives two complementary ways. Framework managers (keyboard, mouse, touch) <b>listen</b>:
 * they register a {@link FlixelKeyboardListener}, {@link FlixelMouseListener}, or
 * {@link FlixelTouchListener} with the device so the backend delivers each event to them as it
 * happens. Game code typically <b>polls</b> instead: it asks "is this key down right now?" each
 * frame through {@link #isKeyPressed(int)} and the pointer position getters. The two views describe
 * the same hardware.
 *
 * <p>A safe default ({@link FlixelNoopInputDevice}) is installed before startup, so
 * {@code Flixel.input} is never {@code null}; on headless or not-yet-initialized sessions its polls
 * report "nothing pressed" and it drops any listeners handed to it. The real desktop, mobile, and
 * web backends replace it at launch. Every method has a neutral default here, so a backend only
 * implements what its platform actually supports.
 *
 * <p>Screen coordinates are in pixels with the origin at the top-left corner: X grows to the right
 * and Y grows downward. This matches raw window coordinates and is <i>not</i> the same as FlixelGDX
 * world coordinates, whose origin is bottom-left; convert through a {@code FlixelCamera} when you
 * need world space.
 *
 * <p>Example:
 *
 * <pre>{@code
 * if (Flixel.input.isKeyPressed(FlixelKey.SPACE)) {
 *   charge();
 * }
 * int mouseX = Flixel.input.getX();
 * }</pre>
 *
 * @see org.flixelgdx.Flixel#input
 * @see FlixelKeyboardListener
 * @see FlixelMouseListener
 * @see FlixelTouchListener
 */
public interface FlixelInputDevice {

  /**
   * @param key The {@link FlixelKey} code to test.
   * @return {@code true} when that key is held down this instant. Defaults to {@code false}.
   */
  default boolean isKeyPressed(int key) {
    return false;
  }

  /**
   * @param button The {@link FlixelMouseButton} code to test.
   * @return {@code true} when that mouse button is held down this instant. Defaults to {@code false}.
   */
  default boolean isButtonPressed(int button) {
    return false;
  }

  /**
   * @return The pointer's horizontal position in screen pixels from the left edge, or {@code 0} when
   *     unknown. Equivalent to {@link #getX(int)} with pointer {@code 0}.
   */
  default int getX() {
    return 0;
  }

  /**
   * @return The pointer's vertical position in screen pixels from the top edge, or {@code 0} when
   *     unknown. Equivalent to {@link #getY(int)} with pointer {@code 0}.
   */
  default int getY() {
    return 0;
  }

  /**
   * Returns the horizontal position of a specific pointer, for multitouch.
   *
   * @param pointer The pointer (finger) index, where {@code 0} is the first finger or the mouse.
   * @return The horizontal position in screen pixels from the left edge, or {@code 0} when unknown.
   */
  default int getX(int pointer) {
    return 0;
  }

  /**
   * Returns the vertical position of a specific pointer, for multitouch.
   *
   * @param pointer The pointer (finger) index, where {@code 0} is the first finger or the mouse.
   * @return The vertical position in screen pixels from the top edge, or {@code 0} when unknown.
   */
  default int getY(int pointer) {
    return 0;
  }

  /**
   * Registers a listener to receive keyboard events.
   *
   * @param listener The listener to add. Ignored when {@code null}.
   */
  default void addKeyboardListener(FlixelKeyboardListener listener) {}

  /**
   * Removes a previously registered keyboard listener. Does nothing if the listener is not registered.
   *
   * @param listener The listener to remove.
   */
  default void removeKeyboardListener(FlixelKeyboardListener listener) {}

  /**
   * Registers a listener to receive mouse events.
   *
   * @param listener The listener to add. Ignored when {@code null}.
   */
  default void addMouseListener(FlixelMouseListener listener) {}

  /**
   * Removes a previously registered mouse listener. Does nothing if the listener is not registered.
   *
   * @param listener The listener to remove.
   */
  default void removeMouseListener(FlixelMouseListener listener) {}

  /**
   * Registers a listener to receive touch events.
   *
   * @param listener The listener to add. Ignored when {@code null}.
   */
  default void addTouchListener(FlixelTouchListener listener) {}

  /**
   * Removes a previously registered touch listener. Does nothing if the listener is not registered.
   *
   * @param listener The listener to remove.
   */
  default void removeTouchListener(FlixelTouchListener listener) {}
}
