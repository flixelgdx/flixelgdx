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
package org.flixelgdx.backend.html5.input;

import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;

/**
 * The web input device, driven by browser DOM events on the game canvas.
 *
 * <p>This is the web counterpart of the desktop input device. Where desktop pumps SDL events from
 * its loop, the browser pushes events to registered listeners on its own schedule, so this class
 * attaches DOM handlers once in {@link #attach(HTMLCanvasElement)} and lets them run whenever the
 * browser fires them. Each handler updates the same cached state the desktop device keeps (for
 * {@link #isKeyPressed(int)} and the pointer getters) and forwards to the framework's input
 * managers through the {@link FlixelKeyboardListener} and {@link FlixelMouseListener} lists.
 *
 * <p>Browser games are played with a mouse, keyboard, or gamepad, so this device does not listen
 * for touch events. On a touch screen, taps still reach the game because the browser falls back to
 * its own mouse emulation for elements with no touch listeners.
 *
 * <p>Keyboard events are bound to the page window rather than the canvas because a canvas does not
 * receive keyboard focus by default; pointer events are bound to the canvas so their coordinates
 * can be translated into canvas space.
 */
public class FlixelHtml5InputDevice extends FlixelBaseInputDevice {

  private final boolean[] keyDown = new boolean[512];
  private final boolean[] buttonDown = new boolean[8];

  private HTMLCanvasElement canvas;

  private int mouseX;
  private int mouseY;

  /**
   * Attaches DOM event listeners so browser input reaches the framework.
   *
   * <p>Called once during startup after the canvas exists. Keyboard listeners live on the window
   * so keys are captured without the canvas needing focus; pointer listeners live on the canvas so
   * their coordinates map cleanly into canvas space.
   *
   * @param canvas The canvas the game renders into.
   */
  public void attach(HTMLCanvasElement canvas) {
    this.canvas = canvas;

    Window.current().addEventListener("keydown", event -> {
      KeyboardEvent key = (KeyboardEvent) event;
      if (shouldSwallow(key.getCode())) {
        event.preventDefault();
      }
      // The browser repeats keydown while a key is held. Route repeats to onKeyRepeated instead
      // of onKeyDown so justPressed stays true for a single frame only.
      if (key.isRepeat()) {
        onKeyRepeated(FlixelHtml5KeyMap.toFlixelKey(key.getCode()));
      } else {
        onKeyDown(FlixelHtml5KeyMap.toFlixelKey(key.getCode()));
      }
      String printable = key.getKey();
      if (printable != null && printable.length() == 1) {
        onKeyTyped(printable.charAt(0));
      }
    });
    Window.current().addEventListener("keyup", event -> {
      KeyboardEvent key = (KeyboardEvent) event;
      onKeyUp(FlixelHtml5KeyMap.toFlixelKey(key.getCode()));
    });

    canvas.addEventListener("mousedown", event -> {
      MouseEvent mouse = (MouseEvent) event;
      onMouseDown(mapButton(mouse.getButton()), canvasX(canvas, mouse.getClientX()),
          canvasY(canvas, mouse.getClientY()));
    });
    canvas.addEventListener("mouseup", event -> {
      MouseEvent mouse = (MouseEvent) event;
      onMouseUp(mapButton(mouse.getButton()), canvasX(canvas, mouse.getClientX()), canvasY(canvas, mouse.getClientY()));
    });
    canvas.addEventListener("mousemove", event -> {
      MouseEvent mouse = (MouseEvent) event;
      onMouseMoved(canvasX(canvas, mouse.getClientX()), canvasY(canvas, mouse.getClientY()));
    });
    canvas.addEventListener("wheel", event -> {
      WheelEvent wheel = (WheelEvent) event;
      onScrolled((float) wheel.getDeltaX(), (float) wheel.getDeltaY());
    });
    // Suppress the right-click menu so games can use the right mouse button.
    canvas.addEventListener("contextmenu", Event::preventDefault);
  }

  private void onKeyDown(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = true;
    }
    dispatchKeyDown(flixelKey);
  }

  private void onKeyUp(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = false;
    }
    dispatchKeyUp(flixelKey);
  }

  private void onKeyRepeated(int flixelKey) {
    dispatchKeyRepeated(flixelKey);
  }

  private void onKeyTyped(char character) {
    dispatchKeyTyped(character);
  }

  private void onMouseDown(int button, int x, int y) {
    if (button >= 0 && button < buttonDown.length) {
      buttonDown[button] = true;
    }
    mouseX = x;
    mouseY = y;
    dispatchMouseDown(button, x, y);
  }

  private void onMouseUp(int button, int x, int y) {
    if (button >= 0 && button < buttonDown.length) {
      buttonDown[button] = false;
    }
    mouseX = x;
    mouseY = y;
    dispatchMouseUp(button, x, y);
  }

  private void onMouseMoved(int x, int y) {
    mouseX = x;
    mouseY = y;
    if (buttonDown[0] || buttonDown[1] || buttonDown[2]) {
      dispatchMouseDragged(x, y);
    } else {
      dispatchMouseMoved(x, y);
    }
  }

  private void onScrolled(float amountX, float amountY) {
    dispatchScrolled(amountX, amountY);
  }

  /**
   * Remaps a browser mouse button index to its {@link FlixelMouseButton}
   * equivalent. The browser orders middle and right buttons as {@code 1} and {@code 2}; the
   * framework uses {@code 1} for right and {@code 2} for middle, so those two are swapped.
   *
   * @param browserButton The {@code MouseEvent.button} value.
   * @return The framework mouse button index.
   */
  private static int mapButton(short browserButton) {
    return switch (browserButton) {
      case 0 -> 0;
      case 1 -> 2;
      case 2 -> 1;
      default -> browserButton;
    };
  }

  /**
   * Returns whether a key should have its default browser action swallowed. Only keys that would
   * otherwise scroll the page (space and the arrow keys) are swallowed, so shortcuts like refresh
   * and the developer tools keep working.
   *
   * @param code The {@code KeyboardEvent.code}.
   * @return {@code true} if the browser default should be prevented.
   */
  private static boolean shouldSwallow(String code) {
    return "Space".equals(code)
        || "ArrowLeft".equals(code)
        || "ArrowRight".equals(code)
        || "ArrowUp".equals(code)
        || "ArrowDown".equals(code);
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

  @JSBody(params = { "canvas", "clientX" }, script = """
      var r = canvas.getBoundingClientRect();
      return Math.round((clientX - r.left) * (canvas.width / r.width));
      """)
  private static native int canvasX(HTMLCanvasElement canvas, int clientX);

  @JSBody(params = { "canvas", "clientY" }, script = """
      var r = canvas.getBoundingClientRect();
      return Math.round((clientY - r.top) * (canvas.height / r.height));
      """)
  private static native int canvasY(HTMLCanvasElement canvas, int clientY);
}
