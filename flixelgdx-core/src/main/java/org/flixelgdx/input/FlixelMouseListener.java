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

import org.flixelgdx.input.mouse.FlixelMouseButton;

/**
 * Receives mouse events as they happen each frame.
 *
 * <p>Register an implementation with {@link FlixelInputDevice#addMouseListener} and the active
 * backend will deliver button presses, releases, movement, dragging, and scroll wheel events to it
 * as events arrive. Every method has a do-nothing default
 * that returns {@code false}, so an implementation only overrides the events it cares about.
 *
 * <p>Returning {@code true} from a method signals that the event was consumed and no further
 * listeners in the chain should see it. Returning {@code false} lets it propagate.
 *
 * <p>Screen coordinates use the top-left origin: X grows right, Y grows down. Convert to world
 * space through a {@link org.flixelgdx.FlixelCamera FlixelCamera} when needed.
 *
 * @see FlixelInputDevice#addMouseListener(FlixelMouseListener)
 * @see FlixelKeyboardListener
 * @see FlixelTouchListener
 */
public interface FlixelMouseListener {

  /**
   * Called when a mouse button is pressed down.
   *
   * @param button The button that went down, as a {@link FlixelMouseButton} code.
   * @param x The horizontal cursor position in screen pixels from the left edge.
   * @param y The vertical cursor position in screen pixels from the top edge.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean mouseDown(int button, int x, int y) {
    return false;
  }

  /**
   * Called when a mouse button is released.
   *
   * @param button The button that came up, as a {@link FlixelMouseButton} code.
   * @param x The horizontal cursor position in screen pixels from the left edge.
   * @param y The vertical cursor position in screen pixels from the top edge.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean mouseUp(int button, int x, int y) {
    return false;
  }

  /**
   * Called when the mouse moves without any button held down. Only fires on platforms with a
   * hovering pointer (desktop and web); never on pure touch devices.
   *
   * @param x The horizontal cursor position in screen pixels from the left edge.
   * @param y The vertical cursor position in screen pixels from the top edge.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean mouseMoved(int x, int y) {
    return false;
  }

  /**
   * Called when the mouse moves while a button is held down.
   *
   * @param x The horizontal cursor position in screen pixels from the left edge.
   * @param y The vertical cursor position in screen pixels from the top edge.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean mouseDragged(int x, int y) {
    return false;
  }

  /**
   * Called when the mouse wheel or a trackpad gesture scrolls.
   *
   * <p>Values are deltas, not fixed -1/1 increments: magnitude varies by device (notched wheel vs
   * trackpad, high-resolution scroll). Sign indicates direction, though which sign means "up" vs
   * "down" can depend on the backend and OS.
   *
   * @param amountX Horizontal scroll delta; positive scrolls right.
   * @param amountY Vertical scroll delta; positive scrolls down.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean scrolled(float amountX, float amountY) {
    return false;
  }
}
