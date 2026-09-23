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

import org.flixelgdx.Flixel;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.mouse.FlixelMouseButton;

/**
 * The low-level input backend: the one interface each platform implements so the framework can read
 * the keyboard and pointer without naming a specific windowing library. Reached through
 * {@link Flixel#input}.
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
 * and Y grows downward. This matches raw window coordinates; FlixelGDX world coordinates share the
 * same top-left origin and downward Y but are offset and scaled by the camera, so convert through a
 * {@code FlixelCamera} when you need world space.
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
 * @see Flixel#input
 * @see FlixelKeyboardListener
 * @see FlixelMouseListener
 * @see FlixelTouchListener
 */
public interface FlixelInputDevice {

  /**
   * Returns {@code true} when the given key is held down this instant, or {@code false} by default.
   *
   * @param key The {@link FlixelKey} code to test.
   * @return {@code true} if the key is currently held down.
   */
  default boolean isKeyPressed(int key) {
    return false;
  }

  /**
   * Returns {@code true} when the given mouse button is held down this instant, or {@code false} by default.
   *
   * @param button The {@link FlixelMouseButton} code to test.
   * @return {@code true} if the button is currently held down.
   */
  default boolean isButtonPressed(int button) {
    return false;
  }

  /**
   * Returns the pointer's horizontal position in screen pixels from the left edge, or {@code 0} when unknown.
   *
   * <p>Equivalent to {@link #getX(int)} with pointer {@code 0}.
   *
   * @return The horizontal position in screen pixels from the left edge, or {@code 0} when unknown.
   */
  default int getX() {
    return 0;
  }

  /**
   * Returns the pointer's vertical position in screen pixels from the top edge, or {@code 0} when unknown.
   *
   * <p>Equivalent to {@link #getY(int)} with pointer {@code 0}.
   *
   * @return The vertical position in screen pixels from the top edge, or {@code 0} when unknown.
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

  /**
   * Requests that the platform start delivering typed text through
   * {@link FlixelKeyboardListener#keyTyped(char)} and, on platforms that have one, show the
   * on-screen keyboard. Does nothing by default.
   *
   * <p>A UI text box typically calls this when it gains focus, and {@link #stopTextInput()} when it
   * loses it:
   *
   * <pre>{@code
   * void focus() {
   *   Flixel.input.startTextInput();
   * }
   *
   * void blur() {
   *   Flixel.input.stopTextInput();
   * }
   * }</pre>
   */
  default void startTextInput() {}

  /**
   * Releases one previous {@link #startTextInput()} request. Does nothing by default.
   *
   * <p>A UI text box typically calls this when it loses focus, pairing it with
   * {@link #startTextInput()} on focus. See {@link #startTextInput()} for a usage example.
   */
  default void stopTextInput() {}

  /**
   * Returns {@code true} when text input is currently active, or {@code false} by default.
   *
   * @return {@code true} if at least one {@link #startTextInput()} request is still outstanding.
   */
  default boolean isTextInputActive() {
    return false;
  }

  /**
   * Tells the platform where text is currently being edited, so it can place the IME candidate
   * window or an on-screen keyboard next to the caret. Does nothing by default.
   *
   * <p>A text box typically calls this whenever the caret moves while focused:
   *
   * <pre>{@code
   * void onCaretMoved(int caretX, int caretY, int caretHeight) {
   *   Flixel.input.setTextInputArea(caretX, caretY, 1, caretHeight);
   * }
   * }</pre>
   *
   * @param x The left edge of the edited text, in window pixels from the left edge.
   * @param y The top edge of the edited text, in window pixels from the top edge.
   * @param width The width of the edited text area, in pixels.
   * @param height The height of the edited text area, in pixels.
   */
  default void setTextInputArea(int x, int y, int width, int height) {}

  /**
   * Returns {@code true} when the platform shows an on-screen keyboard while text input is active,
   * or {@code false} by default.
   *
   * @return {@code true} if the platform has an on-screen keyboard.
   */
  default boolean hasScreenKeyboard() {
    return false;
  }
}
