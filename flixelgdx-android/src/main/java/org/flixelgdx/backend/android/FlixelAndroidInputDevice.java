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
package org.flixelgdx.backend.android;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.jetbrains.annotations.NotNull;

/**
 * Android input device skeleton: a lock-free ring buffer that decouples the UI thread (producer)
 * from the GL thread (consumer).
 *
 * <p>Touch events (down, move, up, cancel for all pointers) and raw key events are posted from the
 * UI thread via {@link View#setOnTouchListener} and a key listener, and dispatched to registered
 * listeners on the GL thread by calling {@link #drain()}.
 *
 * <p>The ring buffer uses parallel primitive arrays so no objects are allocated per event. The
 * event-type enum doubles as an integer constant for zero-GC storage. To add new event types
 * (full key map, gamepads, text input) in a later task: define additional {@code TYPE_*} constants,
 * extend the parallel arrays by the maximum width you need, and dispatch the new events in
 * {@link #drain()}.
 *
 * <p>Coordinates are back-buffer pixels (the touch position is scaled from the view size to
 * the back-buffer size by the pointer-event handlers).
 */
public class FlixelAndroidInputDevice extends FlixelBaseInputDevice {

  // Event type constants stored in the type slot of each ring-buffer entry.
  private static final int TYPE_TOUCH_DOWN = 0;
  private static final int TYPE_TOUCH_UP = 1;
  private static final int TYPE_TOUCH_MOVE = 2;
  private static final int TYPE_TOUCH_CANCEL = 3;
  private static final int TYPE_KEY_DOWN = 4;
  private static final int TYPE_KEY_UP = 5;

  /**
   * Ring-buffer capacity; must be a power of two. 1024 events is enough to absorb a burst
   * without dropping any under normal gameplay, while keeping heap cost low.
   */
  private static final int CAPACITY = 1024;
  private static final int MASK = CAPACITY - 1;

  // Parallel primitive arrays for the ring buffer - no allocation per event.
  private final int[] types = new int[CAPACITY];
  private final int[] pointers = new int[CAPACITY];
  private final int[] xs = new int[CAPACITY];
  private final int[] ys = new int[CAPACITY];
  private final int[] keyCodes = new int[CAPACITY];

  /**
   * Write head (UI thread). Only one writer exists, so no CAS is needed here; volatile
   * ensures the value is visible to the GL thread reader.
   */
  private volatile int head = 0;

  /**
   * Read head (GL thread). Only one reader exists.
   */
  private int tail = 0;

  /**
   * Scale factor from view pixels to back-buffer pixels (width).
   * Updated by the runner on each surface change.
   */
  private volatile float scaleX = 1f;

  /**
   * Scale factor from view pixels to back-buffer pixels (height).
   * Updated by the runner on each surface change.
   */
  private volatile float scaleY = 1f;

  /**
   * Updates the coordinate scale based on the current view and back-buffer dimensions.
   * Call this from the GL thread whenever the surface size changes.
   *
   * @param viewW View width in logical pixels.
   * @param viewH View height in logical pixels.
   * @param bufW Back-buffer width in physical pixels.
   * @param bufH Back-buffer height in physical pixels.
   */
  public void setScale(int viewW, int viewH, int bufW, int bufH) {
    scaleX = viewW > 0 ? (float) bufW / viewW : 1f;
    scaleY = viewH > 0 ? (float) bufH / viewH : 1f;
  }

  /**
   * Returns a {@link View.OnTouchListener} that posts touch events into the ring buffer.
   * Attach this to the GL surface view once.
   *
   * @return A touch listener safe to call from the UI thread.
   */
  @NotNull
  public View.OnTouchListener createTouchListener() {
    return (v, event) -> {
      int action = event.getActionMasked();
      int actionIndex = event.getActionIndex();
      switch (action) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
          postTouch(TYPE_TOUCH_DOWN, event.getPointerId(actionIndex),
              (int) (event.getX(actionIndex) * scaleX),
              (int) (event.getY(actionIndex) * scaleY));
          break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
          postTouch(TYPE_TOUCH_UP, event.getPointerId(actionIndex),
              (int) (event.getX(actionIndex) * scaleX),
              (int) (event.getY(actionIndex) * scaleY));
          break;
        case MotionEvent.ACTION_MOVE:
          for (int i = 0, n = event.getPointerCount(); i < n; i++) {
            postTouch(TYPE_TOUCH_MOVE, event.getPointerId(i),
                (int) (event.getX(i) * scaleX),
                (int) (event.getY(i) * scaleY));
          }
          break;
        case MotionEvent.ACTION_CANCEL:
          for (int i = 0, n = event.getPointerCount(); i < n; i++) {
            postTouch(TYPE_TOUCH_CANCEL, event.getPointerId(i),
                (int) (event.getX(i) * scaleX),
                (int) (event.getY(i) * scaleY));
          }
          break;
        default:
          break;
      }
      return true;
    };
  }

  /**
   * Returns a {@link View.OnKeyListener} that posts raw key-down and key-up events into the
   * ring buffer using the Android keycode as-is.
   *
   * <p>A future task will map Android keycodes to {@link org.flixelgdx.input.keyboard.FlixelKey}
   * values; this skeleton forwards the raw code so the ring-buffer design is complete and callers
   * are not blocked.
   *
   * @return A key listener safe to call from the UI thread.
   */
  @NotNull
  public View.OnKeyListener createKeyListener() {
    return (v, keyCode, event) -> {
      int type = event.getAction() == KeyEvent.ACTION_DOWN ? TYPE_KEY_DOWN : TYPE_KEY_UP;
      postKey(type, keyCode);
      return false;
    };
  }

  /**
   * Drains all pending events from the ring buffer and dispatches them to registered listeners.
   * Call this once per frame on the GL thread, before the game update.
   */
  public void drain() {
    int h = head;
    while (tail != h) {
      int slot = tail & MASK;
      int type = types[slot];
      int pointer = pointers[slot];
      int x = xs[slot];
      int y = ys[slot];
      int keyCode = keyCodes[slot];
      tail++;
      switch (type) {
        case TYPE_TOUCH_DOWN:
          dispatchTouched(pointer, x, y);
          break;
        case TYPE_TOUCH_UP:
          dispatchTouchReleased(pointer, x, y);
          break;
        case TYPE_TOUCH_MOVE:
          dispatchTouchDragged(pointer, x, y);
          break;
        case TYPE_TOUCH_CANCEL:
          dispatchTouchCancelled(pointer, x, y);
          break;
        case TYPE_KEY_DOWN:
          dispatchKeyDown(keyCode);
          break;
        case TYPE_KEY_UP:
          dispatchKeyUp(keyCode);
          break;
        default:
          break;
      }
    }
  }

  /**
   * Writes one touch event slot into the ring buffer. Drops the event if the buffer is full;
   * this is preferable to blocking the UI thread.
   */
  private void postTouch(int type, int pointer, int x, int y) {
    int h = head;
    if ((h - tail) >= CAPACITY) {
      return; // Buffer full; drop this event.
    }
    int slot = h & MASK;
    types[slot] = type;
    pointers[slot] = pointer;
    xs[slot] = x;
    ys[slot] = y;
    head = h + 1;
  }

  /**
   * Writes one key event slot into the ring buffer.
   */
  private void postKey(int type, int keyCode) {
    int h = head;
    if ((h - tail) >= CAPACITY) {
      return;
    }
    int slot = h & MASK;
    types[slot] = type;
    keyCodes[slot] = keyCode;
    head = h + 1;
  }
}
