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
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.keyboard.FlixelKey;

/**
 * The desktop input device, driven by SDL3 events pumped from the game loop.
 *
 * <p>The {@link FlixelDesktopRunner runner} translates SDL keyboard
 * and mouse events into the {@code on*} calls here, which update the cached state (for
 * {@link #isKeyPressed(int)} / pointer getters) and forward to the registered
 * {@link FlixelKeyboardListener} and {@link FlixelMouseListener} instances that the framework's
 * input managers install.
 */
public class FlixelDesktopInputDevice extends FlixelBaseInputDevice {

  /** Down state per FlixelKey code; sized to cover the whole key-code range. */
  private final boolean[] keyDown = new boolean[512];

  /** Down state per mouse button. */
  private final boolean[] buttonDown = new boolean[8];

  private int mouseX;
  private int mouseY;

  /**
   * Feeds a key-down event from the runner.
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
   * Feeds a key-up event from the runner.
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
   * Feeds a typed-character event from the runner.
   *
   * @param character The typed character.
   */
  public void onKeyTyped(char character) {
    dispatchKeyTyped(character);
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
    dispatchMouseDown(button, x, y);
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
    dispatchMouseUp(button, x, y);
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
    if (buttonDown[0] || buttonDown[1] || buttonDown[2]) {
      dispatchMouseDragged(x, y);
    } else {
      dispatchMouseMoved(x, y);
    }
  }

  /**
   * Feeds a scroll-wheel event from the runner.
   *
   * @param amountX Horizontal scroll amount.
   * @param amountY Vertical scroll amount.
   */
  public void onScrolled(float amountX, float amountY) {
    dispatchScrolled(amountX, amountY);
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
}
