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
 * method has a do-nothing default that returns
 * {@code false}, so an implementation only overrides the events it actually cares about.
 *
 * <p>Returning {@code true} from a method signals that the event was consumed and no further
 * listeners in the chain should see it. Returning {@code false} lets it propagate. Most observers
 * return {@code false} because they only watch input without blocking other listeners.
 *
 * <p>Example - a listener that reacts to the Escape key:
 *
 * <pre>{@code
 * Flixel.input.addKeyboardListener(new FlixelKeyboardListener() {
 *   public boolean keyDown(int keycode) {
 *     if (keycode == FlixelKey.ESCAPE) {
 *       openPauseMenu();
 *       return true;
 *     }
 *     return false;
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
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean keyDown(int keycode) {
    return false;
  }

  /**
   * Called once when a key is released.
   *
   * @param keycode The key that came up, as a {@link FlixelKey} code.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean keyUp(int keycode) {
    return false;
  }

  /**
   * Called when a key press produces a typed character, respecting modifiers such as Shift and
   * Caps Lock. Use this for text input rather than {@link #keyDown(int)}.
   *
   * @param character The Unicode character that was typed.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean keyTyped(char character) {
    return false;
  }
}
