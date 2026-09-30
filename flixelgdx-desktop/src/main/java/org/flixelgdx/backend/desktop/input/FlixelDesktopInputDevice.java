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
package org.flixelgdx.backend.desktop.input;

import org.flixelgdx.backend.desktop.FlixelDesktopRunner;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.FlixelInputEventQueue;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;

/**
 * The desktop input device, driven by SDL3 events pumped from the game loop.
 *
 * <p>The {@link FlixelDesktopRunner runner} translates SDL keyboard and mouse events and posts
 * them into this device's {@link FlixelInputEventQueue}, then calls {@link #drain()} once the
 * frame's events have all been pumped. Draining runs the {@code on*} calls here, which update the
 * cached state (for {@link #isKeyPressed(int)} / pointer getters) and forward to the registered
 * {@link FlixelKeyboardListener} and {@link FlixelMouseListener} instances that the framework's
 * input managers install.
 *
 * <p>Queueing matters because one pump can hold both the press and the release of a quick click
 * or key tap. Applied immediately, the button would already be up again by the time the game
 * looked at it, so {@code justPressed} would never fire. The queue holds such a release back by
 * one frame, so the press is seen on one frame and the release on the next.
 */
public class FlixelDesktopInputDevice extends FlixelBaseInputDevice {

  /** Native SDL_Window pointer, bound by the runner once the window exists. */
  private long windowHandle;

  private final FlixelInputEventQueue events = new FlixelInputEventQueue();
  private final EventReceiver receiver = new EventReceiver();

  /** Down state per FlixelKey code; sized to cover the whole key-code range. */
  private final boolean[] keyDown = new boolean[512];

  /** Down state per mouse button. */
  private final boolean[] buttonDown = new boolean[8];

  private int mouseX;
  private int mouseY;

  /**
   * Feeds a key-down event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param flixelKey The mapped {@link FlixelKey} code.
   */
  public void onKeyDown(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = true;
    }
    dispatchKeyDown(flixelKey);
  }

  /**
   * Feeds a key-up event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param flixelKey The mapped {@link FlixelKey} code.
   */
  public void onKeyUp(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = false;
    }
    dispatchKeyUp(flixelKey);
  }

  /**
   * Feeds an OS key-repeat event from the runner, fired while a key is held after the initial
   * {@link #onKeyDown(int)}. Unlike {@link #onKeyDown(int)}, this does not touch the pressed-key
   * state array, since the key was already marked down and has not been released.
   *
   * @param flixelKey The mapped {@link FlixelKey} code.
   */
  public void onKeyRepeated(int flixelKey) {
    dispatchKeyRepeated(flixelKey);
  }

  /**
   * Feeds a typed-character event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param character The typed character.
   */
  public void onKeyTyped(char character) {
    dispatchKeyTyped(character);
  }

  /**
   * Feeds a mouse-button-down event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param button The mouse button index.
   * @param x The pointer x in pixels.
   * @param y The pointer y in pixels.
   */
  public void onMouseDown(int button, int x, int y) {
    if (button >= 0 && button < buttonDown.length) {
      buttonDown[button] = true;
    }
    mouseX = x;
    mouseY = y;
    dispatchMouseDown(button, x, y);
  }

  /**
   * Feeds a mouse-button-up event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param button The mouse button index.
   * @param x The pointer x in pixels.
   * @param y The pointer y in pixels.
   */
  public void onMouseUp(int button, int x, int y) {
    if (button >= 0 && button < buttonDown.length) {
      buttonDown[button] = false;
    }
    mouseX = x;
    mouseY = y;
    dispatchMouseUp(button, x, y);
  }

  /**
   * Feeds a mouse-move event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param x The pointer x in pixels.
   * @param y The pointer y in pixels.
   */
  public void onMouseMoved(int x, int y) {
    mouseX = x;
    mouseY = y;
    if (buttonDown[0] || buttonDown[1] || buttonDown[2]) {
      dispatchMouseDragged(x, y);
    } else {
      dispatchMouseMoved(x, y);
    }
  }

  /**
   * Feeds a scroll-wheel event.
   *
   * <p>The runner reaches this through {@link #drain()}. Call it directly only to inject input by
   * hand, since that skips the queue.
   *
   * @param amountX Horizontal scroll amount.
   * @param amountY Vertical scroll amount.
   */
  public void onScrolled(float amountX, float amountY) {
    dispatchScrolled(amountX, amountY);
  }

  /**
   * Applies every event queued since the last call and forwards it to the registered listeners.
   *
   * <p>The {@link FlixelDesktopRunner runner} calls this once per frame, after pumping SDL events
   * and before the game updates. A release whose press was applied earlier in the same call waits
   * for the next call, so a click or key tap that happened entirely within one pump still reports
   * {@code justPressed} on one frame and {@code justReleased} on the next. See
   * {@link FlixelInputEventQueue} for the details.
   */
  public void drain() {
    events.drain(receiver);
  }

  @Override
  protected void onTextInputStarted() {
    if (windowHandle != 0L) {
      SDLKeyboard.SDL_StartTextInput(windowHandle);
    }
  }

  @Override
  protected void onTextInputStopped() {
    if (windowHandle != 0L) {
      SDLKeyboard.SDL_StopTextInput(windowHandle);
    }
  }

  @Override
  protected void onTextInputAreaChanged(int x, int y, int w, int h) {
    if (windowHandle == 0L) {
      return;
    }
    // Not a per-frame call (only fires when a focused text box moves its caret), so a MemoryStack
    // allocation here is cheap enough to avoid keeping a dedicated SDL_Rect field around.
    try (MemoryStack stack = MemoryStack.stackPush()) {
      SDL_Rect.Buffer rect = SDL_Rect.malloc(1, stack);
      rect.get(0).x(x).y(y).w(w).h(h);
      SDLKeyboard.SDL_SetTextInputArea(windowHandle, rect, 0);
    }
  }

  @Override
  public boolean hasScreenKeyboard() {
    return SDLKeyboard.SDL_HasScreenKeyboardSupport();
  }

  /**
   * Binds the native SDL window handle this device requests text input and IME placement on. The
   * {@link FlixelDesktopRunner runner} calls this once the SDL window exists and before any text
   * input request is made.
   *
   * @param windowHandle The native SDL_Window pointer.
   */
  public void setWindowHandle(long windowHandle) {
    this.windowHandle = windowHandle;
  }

  @Override
  public boolean isKeyPressed(int key) {
    return key >= 0 && key < keyDown.length && keyDown[key];
  }

  @Override
  public boolean isButtonPressed(int button) {
    return button >= 0 && button < buttonDown.length && buttonDown[button];
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
    return pointer == 0 ? mouseX : 0;
  }

  @Override
  public int getY(int pointer) {
    return pointer == 0 ? mouseY : 0;
  }

  /**
   * Returns the queue that holds SDL events until the next {@link #drain()}.
   *
   * @return The event queue the runner posts into.
   */
  public FlixelInputEventQueue getEventQueue() {
    return events;
  }

  /** Applies drained events to this device's polled state and forwards them to listeners. */
  private final class EventReceiver implements FlixelInputEventQueue.Receiver {

    @Override
    public void keyDown(int keycode) {
      onKeyDown(keycode);
    }

    @Override
    public void keyUp(int keycode) {
      onKeyUp(keycode);
    }

    @Override
    public void keyRepeated(int keycode) {
      onKeyRepeated(keycode);
    }

    @Override
    public void keyTyped(char character) {
      onKeyTyped(character);
    }

    @Override
    public void mouseDown(int button, int x, int y) {
      onMouseDown(button, x, y);
    }

    @Override
    public void mouseUp(int button, int x, int y) {
      onMouseUp(button, x, y);
    }

    @Override
    public void mouseMoved(int x, int y) {
      onMouseMoved(x, y);
    }

    @Override
    public void scrolled(float amountX, float amountY) {
      onScrolled(amountX, amountY);
    }
  }
}
