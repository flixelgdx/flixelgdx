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
 * Receives raw input events (key presses, pointer taps, mouse movement, and scrolling) as they
 * happen, rather than by polling once per frame.
 *
 * <p>Think of this as the framework's own event listener for the keyboard and pointer. The active
 * platform backend (reached through {@link org.flixelgdx.Flixel#input Flixel.input}) delivers each
 * event to the processor that was handed to it, so this interface is how FlixelGDX stays aware of
 * input without ever naming a specific windowing library.
 *
 * <p>Every method has a do-nothing default that reports the event as unhandled, so an implementation
 * only overrides the handful of events it actually cares about. A method returns {@code true} to say
 * "I consumed this event; stop passing it along" and {@code false} to let it continue to the next
 * processor (see {@link FlixelInputMultiplexer}). Most trackers return {@code false} because they
 * only observe input; they do not want to block anyone else from seeing it.
 *
 * <p>Example: a processor that only reacts to the space bar being pressed.
 *
 * <pre>{@code
 * FlixelInputProcessor jumpListener = new FlixelInputProcessor() {
 *   public boolean keyDown(int keycode) {
 *     if (keycode == FlixelKey.SPACE) {
 *       jump();
 *       return true; // consume it so nothing else treats space as its own.
 *     }
 *     return false;
 *   }
 * };
 * }</pre>
 *
 * @see FlixelInputMultiplexer
 * @see FlixelInputDevice
 */
public interface FlixelInputProcessor {

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
   * Called when a key press produces a typed character (respecting modifiers such as Shift).
   *
   * @param character The Unicode character that was typed.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean keyTyped(char character) {
    return false;
  }

  /**
   * Called when a finger or mouse button first touches the screen.
   *
   * @param screenX The horizontal position in screen pixels, measured from the left.
   * @param screenY The vertical position in screen pixels, measured from the top.
   * @param pointer The pointer (finger) index, always {@code 0} for a mouse.
   * @param button The mouse button as a {@link FlixelMouseButton} code, or {@code 0} for touch.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchDown(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  /**
   * Called when a finger or mouse button is lifted from the screen.
   *
   * @param screenX The horizontal position in screen pixels, measured from the left.
   * @param screenY The vertical position in screen pixels, measured from the top.
   * @param pointer The pointer (finger) index, always {@code 0} for a mouse.
   * @param button The mouse button as a {@link FlixelMouseButton} code, or {@code 0} for touch.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchUp(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  /**
   * Called when an in-progress touch is cancelled by the system (for example, an incoming call on
   * mobile), so it never becomes a {@link #touchUp(int, int, int, int)}.
   *
   * @param screenX The horizontal position in screen pixels, measured from the left.
   * @param screenY The vertical position in screen pixels, measured from the top.
   * @param pointer The pointer (finger) index, always {@code 0} for a mouse.
   * @param button The mouse button as a {@link FlixelMouseButton} code, or {@code 0} for touch.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  /**
   * Called when a finger or held mouse button moves across the screen.
   *
   * @param screenX The horizontal position in screen pixels, measured from the left.
   * @param screenY The vertical position in screen pixels, measured from the top.
   * @param pointer The pointer (finger) index, always {@code 0} for a mouse.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchDragged(int screenX, int screenY, int pointer) {
    return false;
  }

  /**
   * Called when the mouse moves without any button held down. Only fires on platforms that have a
   * hovering pointer (desktop and web), never on pure touch devices.
   *
   * @param screenX The horizontal position in screen pixels, measured from the left.
   * @param screenY The vertical position in screen pixels, measured from the top.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean mouseMoved(int screenX, int screenY) {
    return false;
  }

  /**
   * Called when the mouse wheel or a trackpad gesture scrolls.
   *
   * @param amountX The horizontal scroll amount; positive scrolls right.
   * @param amountY The vertical scroll amount; positive scrolls down.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean scrolled(float amountX, float amountY) {
    return false;
  }
}
