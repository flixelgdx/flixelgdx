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
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.jetbrains.annotations.NotNull;

/**
 * A garbage-free queue that holds keyboard and mouse events until the game loop is ready for them.
 *
 * <p>Some platforms deliver input whenever they like instead of when the game asks for it. A
 * backend on such a platform posts each raw event here as it arrives, then calls
 * {@link #drain(Receiver)} once at the start of every frame to hand the queued events to a
 * {@link Receiver} in their original order. Think of it like a mail slot: letters can drop through
 * at any hour, but the household only opens them together at breakfast.
 *
 * <h2>Why a press and its release are split across frames</h2>
 *
 * <p>The {@link FlixelKeyInputManager} and {@link FlixelMouseInputManager} detect
 * {@code justPressed} and {@code justReleased} by comparing what is held this frame with what was
 * held last frame. If a quick click (or a key tap) delivered both its press and its release between
 * two frames, the button would look released on both frames and the click would vanish. To stop
 * that, {@link #drain(Receiver)} stops just before a release whose matching press was delivered
 * earlier in the same drain. The release, and every event after it, stays queued for the next
 * frame. The result is that a same-frame click always reports {@code justPressed} on one frame and
 * {@code justReleased} on the following frame, and events never change order.
 *
 * <h2>Coalescing</h2>
 *
 * <p>Pointer movement and scrolling can arrive far more often than frames are drawn. A move posted
 * right after another queued move replaces that move's position, and a scroll posted right after
 * another queued scroll is added to it, so a long stall cannot fill the queue with movement alone.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // In the platform's event callback, whenever it fires:
 * queue.postMouseDown(FlixelMouseButton.LEFT, x, y);
 *
 * // At the start of each frame, before the game updates:
 * queue.drain(receiver);
 * }</pre>
 *
 * <p>This class is not thread safe. Post and drain from the same thread.
 */
public class FlixelInputEventQueue {

  /** The capacity used by {@link #FlixelInputEventQueue()}, in events. */
  public static final int DEFAULT_CAPACITY = 1024;

  private static final int TYPE_KEY_DOWN = 0;
  private static final int TYPE_KEY_UP = 1;
  private static final int TYPE_KEY_REPEATED = 2;
  private static final int TYPE_KEY_TYPED = 3;
  private static final int TYPE_MOUSE_DOWN = 4;
  private static final int TYPE_MOUSE_UP = 5;
  private static final int TYPE_MOUSE_MOVED = 6;
  private static final int TYPE_SCROLLED = 7;

  /** How many distinct keys one drain remembers, so their releases can wait a frame. */
  private static final int MAX_TRACKED_KEYS = 32;

  private final int[] types;
  private final int[] codes;
  private final int[] xs;
  private final int[] ys;
  private final int[] keysPressedThisDrain = new int[MAX_TRACKED_KEYS];
  private final int mask;
  private int head;
  private int tail;

  /** Creates a queue holding up to {@link #DEFAULT_CAPACITY} events. */
  public FlixelInputEventQueue() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates a queue holding up to {@code capacity} events.
   *
   * @param capacity The maximum number of pending events. Must be a positive power of two.
   * @throws IllegalArgumentException If {@code capacity} is not a positive power of two.
   */
  public FlixelInputEventQueue(int capacity) {
    if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
      throw new IllegalArgumentException("Capacity must be a positive power of two, got " + capacity + ".");
    }
    types = new int[capacity];
    codes = new int[capacity];
    xs = new int[capacity];
    ys = new int[capacity];
    mask = capacity - 1;
  }

  /**
   * Queues a key press.
   *
   * @param key The {@link FlixelKey} code that went down.
   * @return {@code true} if the event was queued, or {@code false} if the queue was full.
   */
  public boolean postKeyDown(int key) {
    return post(TYPE_KEY_DOWN, key, 0, 0);
  }

  /**
   * Queues a key release.
   *
   * @param key The {@link FlixelKey} code that came up.
   * @return {@code true} if the event was queued, or {@code false} if the queue was full.
   */
  public boolean postKeyUp(int key) {
    return post(TYPE_KEY_UP, key, 0, 0);
  }

  /**
   * Queues an automatic key repeat for a key that is being held.
   *
   * @param key The {@link FlixelKey} code that is repeating.
   * @return {@code true} if the event was queued, or {@code false} if the queue was full.
   */
  public boolean postKeyRepeated(int key) {
    return post(TYPE_KEY_REPEATED, key, 0, 0);
  }

  /**
   * Queues one typed UTF-16 character.
   *
   * @param character The character that was typed.
   * @return {@code true} if the event was queued, or {@code false} if the queue was full.
   */
  public boolean postKeyTyped(char character) {
    return post(TYPE_KEY_TYPED, character, 0, 0);
  }

  /**
   * Queues a mouse button press.
   *
   * @param button The {@link FlixelMouseButton} code that went down.
   * @param x The pointer position in screen pixels from the left edge.
   * @param y The pointer position in screen pixels from the top edge.
   * @return {@code true} if the event was queued, or {@code false} if the queue was full.
   */
  public boolean postMouseDown(int button, int x, int y) {
    return post(TYPE_MOUSE_DOWN, button, x, y);
  }

  /**
   * Queues a mouse button release.
   *
   * @param button The {@link FlixelMouseButton} code that came up.
   * @param x The pointer position in screen pixels from the left edge.
   * @param y The pointer position in screen pixels from the top edge.
   * @return {@code true} if the event was queued, or {@code false} if the queue was full.
   */
  public boolean postMouseUp(int button, int x, int y) {
    return post(TYPE_MOUSE_UP, button, x, y);
  }

  /**
   * Queues pointer movement, replacing the position of a move that is still the newest queued event.
   *
   * <p>The queue does not decide whether the movement is a drag; the {@link Receiver} gets
   * {@link Receiver#mouseMoved(int, int)} and checks its own button state.
   *
   * @param x The pointer position in screen pixels from the left edge.
   * @param y The pointer position in screen pixels from the top edge.
   * @return {@code true} if the event was queued or merged, or {@code false} if the queue was full.
   */
  public boolean postMouseMoved(int x, int y) {
    if (head != tail) {
      int last = (head - 1) & mask;
      if (types[last] == TYPE_MOUSE_MOVED) {
        xs[last] = x;
        ys[last] = y;
        return true;
      }
    }
    return post(TYPE_MOUSE_MOVED, 0, x, y);
  }

  /**
   * Queues a scroll, adding it to a scroll that is still the newest queued event.
   *
   * @param amountX The horizontal scroll delta.
   * @param amountY The vertical scroll delta.
   * @return {@code true} if the event was queued or merged, or {@code false} if the queue was full.
   */
  public boolean postScrolled(float amountX, float amountY) {
    if (head != tail) {
      int last = (head - 1) & mask;
      if (types[last] == TYPE_SCROLLED) {
        xs[last] = Float.floatToRawIntBits(Float.intBitsToFloat(xs[last]) + amountX);
        ys[last] = Float.floatToRawIntBits(Float.intBitsToFloat(ys[last]) + amountY);
        return true;
      }
    }
    return post(TYPE_SCROLLED, 0, Float.floatToRawIntBits(amountX), Float.floatToRawIntBits(amountY));
  }

  /**
   * Delivers queued events to {@code receiver} in the order they were posted.
   *
   * <p>Call this once per frame, before the game updates. Draining stops just before the release of
   * a key or mouse button that was pressed earlier in the same drain; that release and everything
   * after it are delivered by the next call. See the class documentation for why.
   *
   * @param receiver The object that applies each event and forwards it to listeners.
   */
  public void drain(@NotNull Receiver receiver) {
    int pressedButtons = 0;
    int pressedKeyCount = 0;
    while (tail != head) {
      int slot = tail & mask;
      int type = types[slot];
      int code = codes[slot];
      if (type == TYPE_MOUSE_UP && isTrackedButton(code) && (pressedButtons & (1 << code)) != 0) {
        return;
      }
      if (type == TYPE_KEY_UP && containsKey(code, pressedKeyCount)) {
        return;
      }
      tail++;
      int x = xs[slot];
      int y = ys[slot];
      switch (type) {
        case TYPE_KEY_DOWN -> {
          if (pressedKeyCount < MAX_TRACKED_KEYS) {
            keysPressedThisDrain[pressedKeyCount++] = code;
          }
          receiver.keyDown(code);
        }
        case TYPE_KEY_UP -> receiver.keyUp(code);
        case TYPE_KEY_REPEATED -> receiver.keyRepeated(code);
        case TYPE_KEY_TYPED -> receiver.keyTyped((char) code);
        case TYPE_MOUSE_DOWN -> {
          if (isTrackedButton(code)) {
            pressedButtons |= 1 << code;
          }
          receiver.mouseDown(code, x, y);
        }
        case TYPE_MOUSE_UP -> receiver.mouseUp(code, x, y);
        case TYPE_MOUSE_MOVED -> receiver.mouseMoved(x, y);
        case TYPE_SCROLLED -> receiver.scrolled(Float.intBitsToFloat(x), Float.intBitsToFloat(y));
        default -> {
        }
      }
    }
  }

  /** Discards every queued event without delivering it. */
  public void clear() {
    tail = head;
  }

  private boolean post(int type, int code, int x, int y) {
    if (head - tail > mask) {
      return false;
    }
    int slot = head & mask;
    types[slot] = type;
    codes[slot] = code;
    xs[slot] = x;
    ys[slot] = y;
    head++;
    return true;
  }

  private boolean containsKey(int key, int count) {
    for (int i = 0; i < count; i++) {
      if (keysPressedThisDrain[i] == key) {
        return true;
      }
    }
    return false;
  }

  private static boolean isTrackedButton(int button) {
    return button >= 0 && button < Integer.SIZE;
  }

  /**
   * Returns how many events are waiting to be drained.
   *
   * @return The number of queued events.
   */
  public int getSize() {
    return head - tail;
  }

  public boolean isEmpty() {
    return head == tail;
  }

  public int getCapacity() {
    return mask + 1;
  }

  /**
   * Receives the events handed out by {@link FlixelInputEventQueue#drain(Receiver)}.
   *
   * <p>A backend usually implements this privately: each callback updates the backend's polled
   * state (which key or button is down, where the pointer is) and then forwards the event to the
   * registered {@link FlixelKeyboardListener} and {@link FlixelMouseListener} objects. The queue
   * only calls {@link #mouseMoved(int, int)} for movement, so the receiver decides whether that
   * movement is a drag. The queue never calls {@link #mouseDragged(int, int)}.
   */
  public interface Receiver extends FlixelKeyboardListener, FlixelMouseListener {
  }
}
