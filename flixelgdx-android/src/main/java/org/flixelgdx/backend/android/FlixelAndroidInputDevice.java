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

import android.app.Activity;
import android.content.Context;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import org.flixelgdx.backend.android.input.FlixelAndroidKeyMap;
import org.flixelgdx.backend.android.input.FlixelAndroidGamepadProvider;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Android input device: a lock-free SPSC ring buffer that decouples the UI thread (producer)
 * from the GL thread (consumer).
 *
 * <p>Touch events (all pointers), key events, and IME-committed characters are posted from the
 * UI thread and dispatched to registered listeners on the GL thread by {@link #drain()}. The ring
 * buffer uses parallel primitive arrays so no objects are allocated per event.
 *
 * <p>Touch pointer 0 is also mirrored as a mouse emulation: pointer-0 down fires
 * {@code mouseDown(LEFT)}, moves fire {@code mouseDragged}, and releases fire
 * {@code mouseUp(LEFT)}. This lets mouse-only UI code work on a touch screen without any changes
 * in game code.
 *
 * <p>Coordinates are always in back-buffer pixels (view coordinates scaled by
 * {@link #setScale(int, int, int, int)}).
 *
 * <p>Call {@link #setSurfaceAndActivity(FlixelAndroidSurfaceView, Activity)} once from the
 * launcher so that {@link #onTextInputStarted()} and {@link #onTextInputStopped()} can show
 * and hide the soft keyboard.
 *
 * @see FlixelAndroidSurfaceView
 * @see FlixelAndroidKeyMap
 */
public class FlixelAndroidInputDevice extends FlixelBaseInputDevice {

  // Event type constants stored in the type slot of each ring-buffer entry.
  static final int TYPE_TOUCH_DOWN = 0;
  static final int TYPE_TOUCH_UP = 1;
  static final int TYPE_TOUCH_MOVE = 2;
  static final int TYPE_TOUCH_CANCEL = 3;
  static final int TYPE_KEY_DOWN = 4;
  static final int TYPE_KEY_UP = 5;
  /** Committed IME or hardware-keyboard character; the char value lives in {@code keyCodes[slot]}. */
  static final int TYPE_CHAR_INPUT = 6;

  /**
   * Ring-buffer capacity; must be a power of two. 1024 events absorbs a burst without dropping
   * any under normal gameplay while keeping heap cost low.
   */
  private static final int CAPACITY = 1024;
  private static final int MASK = CAPACITY - 1;

  /**
   * Maximum pointer count tracked for multitouch positions. Matches Android's practical limit
   * of 10 simultaneous contacts.
   */
  private static final int MAX_POINTERS = 10;

  // Parallel primitive arrays for the ring buffer - no allocation per event.
  private final int[] types = new int[CAPACITY];
  private final int[] pointers = new int[CAPACITY];
  private final int[] xs = new int[CAPACITY];
  private final int[] ys = new int[CAPACITY];
  private final int[] keyCodes = new int[CAPACITY];

  /**
   * Write head (UI thread). Only one writer exists, so no CAS is needed; volatile ensures
   * the value is visible to the GL thread reader.
   */
  private volatile int head = 0;

  /**
   * Read head (GL thread). Only one reader exists; volatile so the UI thread sees freed slots
   * when it checks whether the buffer is full.
   */
  private volatile int tail = 0;

  /**
   * Scale factor from view pixels to back-buffer pixels (width). Updated by the runner on each
   * surface change.
   */
  private volatile float scaleX = 1f;

  /**
   * Scale factor from view pixels to back-buffer pixels (height). Updated by the runner on each
   * surface change.
   */
  private volatile float scaleY = 1f;

  // GL-thread-only state (no thread safety needed; drain() and the getters all run on GL thread).

  /** Per-pointer X positions in back-buffer pixels, indexed by pointer ID. */
  private final int[] pointerXs = new int[MAX_POINTERS];

  /** Per-pointer Y positions in back-buffer pixels, indexed by pointer ID. */
  private final int[] pointerYs = new int[MAX_POINTERS];

  /**
   * Key-pressed state. Index is a {@link FlixelKey} code; {@code true} when the key is held.
   */
  private final boolean[] keysDown = new boolean[FlixelKey.MAX_KEYCODE + 1];

  /** Current X of the mouse cursor emulated from pointer 0. */
  private int mouseX;

  /** Current Y of the mouse cursor emulated from pointer 0. */
  private int mouseY;

  /** Whether the left mouse button is currently pressed (pointer 0 is down). */
  private boolean mouseDown;

  // References set by the launcher so the text-input methods can reach the platform.

  @Nullable
  private volatile FlixelAndroidSurfaceView surfaceView;

  @Nullable
  private volatile Activity activity;

  /**
   * Gamepad provider that receives gamepad button and axis events routed from the key and motion
   * listeners. Set by the launcher after the provider is created.
   */
  @Nullable
  private volatile FlixelAndroidGamepadProvider gamepadProvider;

  /**
   * Updates the coordinate scale based on the current view and back-buffer dimensions. Call this
   * from the GL thread whenever the surface size changes.
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
   * Stores the surface view and activity so text-input methods can show and hide the IME.
   * Call this from the launcher after both objects exist.
   *
   * @param view The game's custom surface view.
   * @param act The host activity.
   */
  public void setSurfaceAndActivity(@NotNull FlixelAndroidSurfaceView view,
      @NotNull Activity act) {
    this.surfaceView = view;
    this.activity = act;
  }

  /**
   * Sets the gamepad provider that receives routed button and axis events. Set this from the
   * launcher after the provider is created, before the first frame.
   *
   * @param provider The gamepad provider, or {@code null} to clear it.
   */
  public void setGamepadProvider(@Nullable FlixelAndroidGamepadProvider provider) {
    this.gamepadProvider = provider;
  }

  /**
   * Returns a {@link View.OnTouchListener} that posts touch events into the ring buffer.
   * Attach this to the surface view once.
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
   * Returns a {@link View.OnKeyListener} that routes hardware keyboard events through the ring
   * buffer and gamepad button events to the gamepad provider.
   *
   * <p>The BACK key is translated to {@link FlixelKey#BACK} and consumed (returns {@code true})
   * so the activity does not finish on Back. Volume keys are passed through unconsumed.
   * Gamepad button events are forwarded to the active {@link FlixelAndroidGamepadProvider} and
   * consumed. For hardware keyboard events, typed Unicode characters are also forwarded through
   * the char-input path so {@link org.flixelgdx.input.FlixelKeyboardListener#keyTyped(char)} fires.
   *
   * @return A key listener safe to call from the UI thread.
   */
  @NotNull
  public View.OnKeyListener createKeyListener() {
    return (v, keyCode, event) -> {
      // Volume keys: never consume so the system handles them.
      if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
          || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
        return false;
      }

      // Route gamepad button events to the provider.
      InputDevice device = event.getDevice();
      if (device != null) {
        int sources = device.getSources();
        if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
          FlixelAndroidGamepadProvider gp = gamepadProvider;
          if (gp != null) {
            gp.onKeyEvent(keyCode, event);
          }
          return true; // Consume so the event does not propagate further.
        }
      }

      // Map the Android keycode to a FlixelKey code.
      int flixelKey = FlixelAndroidKeyMap.toFlixelKey(keyCode);
      if (flixelKey != FlixelKey.UNKNOWN) {
        int type = event.getAction() == KeyEvent.ACTION_DOWN ? TYPE_KEY_DOWN : TYPE_KEY_UP;
        postKey(type, flixelKey);

        // Forward typed character for KEY_DOWN events on hardware keyboards. The IME path
        // (BaseInputConnection.commitText) handles soft-keyboard characters separately.
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          int uniChar = event.getUnicodeChar();
          if (uniChar > 0) {
            postChar((char) uniChar);
          }
        }
      }

      // Consume the BACK key so the activity does not finish.
      return keyCode == KeyEvent.KEYCODE_BACK;
    };
  }

  /**
   * Returns a {@link View.OnGenericMotionListener} that forwards joystick axis events to the
   * active {@link FlixelAndroidGamepadProvider}. Attach this to the surface view once.
   *
   * @return A motion listener safe to call from the UI thread.
   */
  @NotNull
  public View.OnGenericMotionListener createGenericMotionListener() {
    return (v, event) -> {
      if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
          && event.getAction() == MotionEvent.ACTION_MOVE) {
        FlixelAndroidGamepadProvider gp = gamepadProvider;
        if (gp != null) {
          gp.onMotionEvent(event);
        }
        return true;
      }
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
          if (pointer >= 0 && pointer < MAX_POINTERS) {
            pointerXs[pointer] = x;
            pointerYs[pointer] = y;
          }
          dispatchTouched(pointer, x, y);
          if (pointer == 0) {
            mouseX = x;
            mouseY = y;
            mouseDown = true;
            dispatchMouseDown(FlixelMouseButton.LEFT, x, y);
          }
          break;
        case TYPE_TOUCH_UP:
          if (pointer >= 0 && pointer < MAX_POINTERS) {
            pointerXs[pointer] = x;
            pointerYs[pointer] = y;
          }
          dispatchTouchReleased(pointer, x, y);
          if (pointer == 0) {
            mouseX = x;
            mouseY = y;
            mouseDown = false;
            dispatchMouseUp(FlixelMouseButton.LEFT, x, y);
          }
          break;
        case TYPE_TOUCH_MOVE:
          if (pointer >= 0 && pointer < MAX_POINTERS) {
            pointerXs[pointer] = x;
            pointerYs[pointer] = y;
          }
          dispatchTouchDragged(pointer, x, y);
          if (pointer == 0) {
            mouseX = x;
            mouseY = y;
            dispatchMouseDragged(x, y);
          }
          break;
        case TYPE_TOUCH_CANCEL:
          if (pointer >= 0 && pointer < MAX_POINTERS) {
            pointerXs[pointer] = x;
            pointerYs[pointer] = y;
          }
          dispatchTouchCancelled(pointer, x, y);
          if (pointer == 0) {
            mouseX = x;
            mouseY = y;
            mouseDown = false;
            dispatchMouseUp(FlixelMouseButton.LEFT, x, y);
          }
          break;
        case TYPE_KEY_DOWN:
          if (keyCode >= 0 && keyCode <= FlixelKey.MAX_KEYCODE) {
            keysDown[keyCode] = true;
          }
          dispatchKeyDown(keyCode);
          break;
        case TYPE_KEY_UP:
          if (keyCode >= 0 && keyCode <= FlixelKey.MAX_KEYCODE) {
            keysDown[keyCode] = false;
          }
          dispatchKeyUp(keyCode);
          break;
        case TYPE_CHAR_INPUT:
          dispatchKeyTyped((char) keyCode);
          break;
        default:
          break;
      }
    }
  }

  @Override
  public boolean isKeyPressed(int key) {
    return key >= 0 && key <= FlixelKey.MAX_KEYCODE && keysDown[key];
  }

  @Override
  public boolean isButtonPressed(int button) {
    return button == FlixelMouseButton.LEFT && mouseDown;
  }

  @Override
  public int getX() {
    return mouseX;
  }

  @Override
  public int getY() {
    return mouseY;
  }

  @Override
  public int getX(int pointer) {
    return (pointer >= 0 && pointer < MAX_POINTERS) ? pointerXs[pointer] : 0;
  }

  @Override
  public int getY(int pointer) {
    return (pointer >= 0 && pointer < MAX_POINTERS) ? pointerYs[pointer] : 0;
  }

  @Override
  public boolean hasScreenKeyboard() {
    return true;
  }

  /**
   * Shows the soft keyboard and requests focus on the surface view by posting to the UI thread.
   * Called by {@link #startTextInput()} on the 0-to-1 transition.
   */
  @Override
  protected void onTextInputStarted() {
    FlixelAndroidSurfaceView sv = surfaceView;
    Activity act = activity;
    if (sv == null || act == null) {
      return;
    }
    sv.post(() -> {
      sv.requestFocus();
      InputMethodManager imm =
          (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.showSoftInput(sv, InputMethodManager.SHOW_IMPLICIT);
      }
    });
  }

  /**
   * Hides the soft keyboard by posting to the UI thread. Called by {@link #stopTextInput()} on
   * the 1-to-0 transition.
   */
  @Override
  protected void onTextInputStopped() {
    FlixelAndroidSurfaceView sv = surfaceView;
    Activity act = activity;
    if (sv == null || act == null) {
      return;
    }
    sv.post(() -> {
      InputMethodManager imm =
          (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.hideSoftInputFromWindow(sv.getWindowToken(), 0);
      }
    });
  }

  /**
   * Posts one touch event slot into the ring buffer. Drops the event when the buffer is full;
   * this is preferable to blocking the UI thread.
   */
  void postTouch(int type, int pointer, int x, int y) {
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
   * Posts one key event slot into the ring buffer. The {@code flixelKey} parameter must already
   * be a {@link FlixelKey} code (after mapping from the Android keycode).
   */
  void postKey(int type, int flixelKey) {
    int h = head;
    if ((h - tail) >= CAPACITY) {
      return;
    }
    int slot = h & MASK;
    types[slot] = type;
    keyCodes[slot] = flixelKey;
    head = h + 1;
  }

  /**
   * Posts a typed character into the ring buffer. Called from the IME connection for committed
   * text and from the key listener for hardware keyboard input.
   *
   * @param c The Unicode character to forward to keyboard listeners.
   */
  void postChar(char c) {
    int h = head;
    if ((h - tail) >= CAPACITY) {
      return;
    }
    int slot = h & MASK;
    types[slot] = TYPE_CHAR_INPUT;
    keyCodes[slot] = c;
    head = h + 1;
  }
}
