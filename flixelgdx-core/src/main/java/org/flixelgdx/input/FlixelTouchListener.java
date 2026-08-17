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

/**
 * Receives touch events as they happen each frame.
 *
 * <p>Register an implementation with {@link FlixelInputDevice#addTouchListener} and the active
 * backend will deliver finger contacts, releases, drags, and cancellations to it as events arrive.
 * Every method has a do-nothing default that returns
 * {@code false}, so an implementation only overrides the events it cares about.
 *
 * <p>Returning {@code true} from a method signals that the event was consumed and no further
 * listeners in the chain should see it. Returning {@code false} lets it propagate.
 *
 * <p>Each simultaneous finger contact carries its own {@code pointer} index starting at {@code 0}
 * for the first finger down. Indices are reused once a finger lifts, so always track state per
 * pointer rather than assuming a fixed mapping to fingers. Screen coordinates use the top-left
 * origin: X grows right, Y grows down.
 *
 * @see FlixelInputDevice#addTouchListener(FlixelTouchListener)
 * @see FlixelKeyboardListener
 * @see FlixelMouseListener
 */
public interface FlixelTouchListener {

  /**
   * Called when a finger first contacts the screen.
   *
   * @param pointer The finger index for this contact (0 = first finger down).
   * @param x The horizontal contact position in screen pixels from the left edge.
   * @param y The vertical contact position in screen pixels from the top edge.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touched(int pointer, int x, int y) {
    return false;
  }

  /**
   * Called when a finger lifts off the screen.
   *
   * @param pointer The finger index that was released.
   * @param x The horizontal position in screen pixels where the finger lifted.
   * @param y The vertical position in screen pixels where the finger lifted.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchReleased(int pointer, int x, int y) {
    return false;
  }

  /**
   * Called when a finger moves while in contact with the screen.
   *
   * @param pointer The finger index that moved.
   * @param x The new horizontal position in screen pixels from the left edge.
   * @param y The new vertical position in screen pixels from the top edge.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchDragged(int pointer, int x, int y) {
    return false;
  }

  /**
   * Called when an active touch is canceled by the system, for example when the OS overlays a
   * notification or the app loses focus mid-gesture.
   *
   * <p>Treat this like {@link #touchReleased} but without valid end coordinates. Release any state
   * associated with the given pointer.
   *
   * @param pointer The finger index whose contact was canceled.
   * @param x The last known horizontal position in screen pixels, or a system-provided estimate.
   * @param y The last known vertical position in screen pixels, or a system-provided estimate.
   * @return {@code true} to consume the event, {@code false} to pass it on.
   */
  default boolean touchCancelled(int pointer, int x, int y) {
    return false;
  }
}
