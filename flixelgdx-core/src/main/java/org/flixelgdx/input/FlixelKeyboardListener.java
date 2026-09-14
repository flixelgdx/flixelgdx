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

/**
 * Receives keyboard events as they happen each frame.
 *
 * <p>Register an implementation with {@link FlixelInputDevice#addKeyboardListener} and the active
 * backend will deliver key presses, releases, and typed characters to it as events arrive. Every
 * method has a do-nothing default, so an implementation only overrides the events it actually
 * cares about. Listeners observe input; they do not block one another, so every registered
 * listener sees every event.
 *
 * <p>Example - a listener that reacts to the Escape key:
 *
 * <pre>{@code
 * Flixel.input.addKeyboardListener(new FlixelKeyboardListener() {
 *   @Override
 *   public void keyDown(int keycode) {
 *     if (keycode == FlixelKey.ESCAPE) {
 *       openPauseMenu();
 *     }
 *   }
 * });
 * }</pre>
 *
 * @see FlixelInputDevice#addKeyboardListener(FlixelKeyboardListener)
 * @see FlixelMouseListener
 * @see FlixelTouchListener
 */
public interface FlixelKeyboardListener {

  /**
   * Called once when a key is first pressed down.
   *
   * @param keycode The key that went down, as a {@link FlixelKey} code.
   */
  default void keyDown(int keycode) {}

  /**
   * Called once when a key is released.
   *
   * @param keycode The key that came up, as a {@link FlixelKey} code.
   */
  default void keyUp(int keycode) {}

  /**
   * Called when a key press produces a typed character, respecting modifiers such as Shift and
   * Caps Lock. Use this for text input rather than {@link #keyDown(int)}.
   *
   * @param character The Unicode character that was typed.
   */
  default void keyTyped(char character) {}
}
