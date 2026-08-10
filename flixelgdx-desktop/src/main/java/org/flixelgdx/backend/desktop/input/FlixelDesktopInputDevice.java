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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.FlixelTouchListener;

/**
 * The desktop input device, driven by SDL3 events pumped from the game loop.
 *
 * <p>The {@link org.flixelgdx.backend.desktop.FlixelDesktopRunner runner} translates SDL keyboard
 * and mouse events into the {@code on*} calls here, which update the cached state (for
 * {@link #isKeyPressed(int)} / pointer getters) and forward to the registered
 * {@link FlixelKeyboardListener} and {@link FlixelMouseListener} instances that the framework's
 * input managers install.
 */
public final class FlixelDesktopInputDevice implements FlixelInputDevice {

  /** Down state per FlixelKey code; sized to cover the whole key-code range. */
  private final boolean[] keyDown = new boolean[512];

  /** Down state per mouse button. */
  private final boolean[] buttonDown = new boolean[8];

  private final FlixelArray<FlixelKeyboardListener> keyboardListeners = new FlixelArray<>();
  private final FlixelArray<FlixelMouseListener> mouseListeners = new FlixelArray<>();
  private final FlixelArray<FlixelTouchListener> touchListeners = new FlixelArray<>();

  private int mouseX;
  private int mouseY;

  /**
   * Feeds a key-down event from the runner.
   *
   * @param flixelKey The mapped {@link org.flixelgdx.input.keyboard.FlixelKey FlixelKey} code.
   */
  public void onKeyDown(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = true;
    }
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyDown(flixelKey);
    }
  }

  /**
   * Feeds a key-up event from the runner.
   *
   * @param flixelKey The mapped {@link org.flixelgdx.input.keyboard.FlixelKey FlixelKey} code.
   */
  public void onKeyUp(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = false;
    }
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyUp(flixelKey);
    }
  }

  /**
   * Feeds a typed-character event from the runner.
   *
   * @param character The typed character.
   */
  public void onKeyTyped(char character) {
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyTyped(character);
    }
  }

  /**
   * Feeds a mouse-button-down event from the runner.
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
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseDown(button, x, y);
    }
  }

  /**
   * Feeds a mouse-button-up event from the runner.
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
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseUp(button, x, y);
    }
  }

  /**
   * Feeds a mouse-move event from the runner.
   *
   * @param x The pointer x in pixels.
   * @param y The pointer y in pixels.
   */
  public void onMouseMoved(int x, int y) {
    mouseX = x;
    mouseY = y;
    boolean dragging = buttonDown[0] || buttonDown[1] || buttonDown[2];
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      if (dragging) {
        mouseListeners.get(i).mouseDragged(x, y);
      } else {
        mouseListeners.get(i).mouseMoved(x, y);
      }
    }
  }

  /**
   * Feeds a scroll-wheel event from the runner.
   *
   * @param amountX Horizontal scroll amount.
   * @param amountY Vertical scroll amount.
   */
  public void onScrolled(float amountX, float amountY) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).scrolled(amountX, amountY);
    }
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
}
