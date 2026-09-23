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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.input.keyboard.FlixelKey;

/**
 * Base {@link FlixelInputDevice} that owns listener registration and event dispatch so backends
 * only translate their platform events.
 *
 * <p>Every platform backend keeps its own keyboard, mouse, and touch listener lists and loops
 * over them the same way. This class centralizes that: it implements the
 * {@code add}/{@code remove} listener methods and exposes {@code dispatch*} helpers a backend calls
 * once it has decoded a raw platform event. A backend then only has to maintain its polled state
 * (which key or button is down, the pointer position) and forward each event through one call.
 *
 * <p>The {@code dispatch*} helpers iterate with indexed {@code for} loops and never allocate, so
 * they are safe to call from a per-frame event pump.
 */
public abstract class FlixelBaseInputDevice implements FlixelInputDevice {

  protected FlixelArray<FlixelKeyboardListener> keyboardListeners = new FlixelArray<>();
  protected FlixelArray<FlixelMouseListener> mouseListeners = new FlixelArray<>();
  protected FlixelArray<FlixelTouchListener> touchListeners = new FlixelArray<>();

  @Override
  public void addKeyboardListener(FlixelKeyboardListener listener) {
    if (listener != null && !keyboardListeners.contains(listener, true)) {
      keyboardListeners.add(listener);
    }
  }

  @Override
  public void removeKeyboardListener(FlixelKeyboardListener listener) {
    keyboardListeners.removeValue(listener, true);
  }

  @Override
  public void addMouseListener(FlixelMouseListener listener) {
    if (listener != null && !mouseListeners.contains(listener, true)) {
      mouseListeners.add(listener);
    }
  }

  @Override
  public void removeMouseListener(FlixelMouseListener listener) {
    mouseListeners.removeValue(listener, true);
  }

  @Override
  public void addTouchListener(FlixelTouchListener listener) {
    if (listener != null && !touchListeners.contains(listener, true)) {
      touchListeners.add(listener);
    }
  }

  @Override
  public void removeTouchListener(FlixelTouchListener listener) {
    touchListeners.removeValue(listener, true);
  }

  /**
   * Delivers a key-down event to every registered keyboard listener.
   *
   * @param keycode The {@link FlixelKey} code that went down.
   */
  protected void dispatchKeyDown(int keycode) {
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyDown(keycode);
    }
  }

  /**
   * Delivers a key-up event to every registered keyboard listener.
   *
   * @param keycode The {@link FlixelKey} code that came up.
   */
  protected void dispatchKeyUp(int keycode) {
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyUp(keycode);
    }
  }

  /**
   * Delivers a key-repeated event to every registered keyboard listener.
   *
   * @param keycode The {@link FlixelKey} code that is repeating.
   */
  protected void dispatchKeyRepeated(int keycode) {
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyRepeated(keycode);
    }
  }

  /**
   * Delivers a typed-character event to every registered keyboard listener.
   *
   * @param character The Unicode character that was typed.
   */
  protected void dispatchKeyTyped(char character) {
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyTyped(character);
    }
  }

  /**
   * Delivers a mouse-button-down event to every registered mouse listener.
   *
   * @param button The mouse button that went down.
   * @param x The pointer x in screen pixels from the left edge.
   * @param y The pointer y in screen pixels from the top edge.
   */
  protected void dispatchMouseDown(int button, int x, int y) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseDown(button, x, y);
    }
  }

  /**
   * Delivers a mouse-button-up event to every registered mouse listener.
   *
   * @param button The mouse button that came up.
   * @param x The pointer x in screen pixels from the left edge.
   * @param y The pointer y in screen pixels from the top edge.
   */
  protected void dispatchMouseUp(int button, int x, int y) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseUp(button, x, y);
    }
  }

  /**
   * Delivers a mouse-moved event to every registered mouse listener.
   *
   * @param x The pointer x in screen pixels from the left edge.
   * @param y The pointer y in screen pixels from the top edge.
   */
  protected void dispatchMouseMoved(int x, int y) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseMoved(x, y);
    }
  }

  /**
   * Delivers a mouse-dragged event to every registered mouse listener.
   *
   * @param x The pointer x in screen pixels from the left edge.
   * @param y The pointer y in screen pixels from the top edge.
   */
  protected void dispatchMouseDragged(int x, int y) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseDragged(x, y);
    }
  }

  /**
   * Delivers a scroll event to every registered mouse listener.
   *
   * @param amountX Horizontal scroll delta; positive scrolls right.
   * @param amountY Vertical scroll delta; positive scrolls down.
   */
  protected void dispatchScrolled(float amountX, float amountY) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).scrolled(amountX, amountY);
    }
  }

  /**
   * Delivers a touch-start event to every registered touch listener.
   *
   * @param pointer The finger index for this contact.
   * @param x The contact x in screen pixels from the left edge.
   * @param y The contact y in screen pixels from the top edge.
   */
  protected void dispatchTouched(int pointer, int x, int y) {
    for (int i = 0; i < touchListeners.getSize(); i++) {
      touchListeners.get(i).touched(pointer, x, y);
    }
  }

  /**
   * Delivers a touch-release event to every registered touch listener.
   *
   * @param pointer The finger index that was released.
   * @param x The x in screen pixels where the finger lifted.
   * @param y The y in screen pixels where the finger lifted.
   */
  protected void dispatchTouchReleased(int pointer, int x, int y) {
    for (int i = 0; i < touchListeners.getSize(); i++) {
      touchListeners.get(i).touchReleased(pointer, x, y);
    }
  }

  /**
   * Delivers a touch-drag event to every registered touch listener.
   *
   * @param pointer The finger index that moved.
   * @param x The new x in screen pixels from the left edge.
   * @param y The new y in screen pixels from the top edge.
   */
  protected void dispatchTouchDragged(int pointer, int x, int y) {
    for (int i = 0; i < touchListeners.getSize(); i++) {
      touchListeners.get(i).touchDragged(pointer, x, y);
    }
  }

  /**
   * Delivers a touch-cancel event to every registered touch listener.
   *
   * @param pointer The finger index whose contact was canceled.
   * @param x The last known x in screen pixels, or a system-provided estimate.
   * @param y The last known y in screen pixels, or a system-provided estimate.
   */
  protected void dispatchTouchCancelled(int pointer, int x, int y) {
    for (int i = 0; i < touchListeners.getSize(); i++) {
      touchListeners.get(i).touchCancelled(pointer, x, y);
    }
  }
}
