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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.FlixelTouchListener;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.Touch;
import org.teavm.jso.dom.events.TouchEvent;
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
 * managers through the {@link FlixelKeyboardListener}, {@link FlixelMouseListener}, and
 * {@link FlixelTouchListener} lists.
 *
 * <p>Keyboard events are bound to the page window rather than the canvas because a canvas does not
 * receive keyboard focus by default; pointer and touch events are bound to the canvas so their
 * coordinates can be translated into canvas space.
 */
public class FlixelHtml5InputDevice implements FlixelInputDevice {

  private final boolean[] keyDown = new boolean[512];
  private final boolean[] buttonDown = new boolean[8];

  private final FlixelArray<FlixelKeyboardListener> keyboardListeners = new FlixelArray<>();
  private final FlixelArray<FlixelMouseListener> mouseListeners = new FlixelArray<>();
  private final FlixelArray<FlixelTouchListener> touchListeners = new FlixelArray<>();

  private HTMLCanvasElement canvas;

  private int mouseX;
  private int mouseY;

  /**
   * Attaches DOM event listeners so browser input reaches the framework.
   *
   * <p>Called once during startup after the canvas exists. Keyboard listeners live on the window
   * so keys are captured without the canvas needing focus; pointer and touch listeners live on the
   * canvas so their coordinates map cleanly into canvas space.
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
      // The browser repeats keydown while a key is held; ignore repeats so justPressed stays true
      // for a single frame only.
      if (!key.isRepeat()) {
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

    canvas.addEventListener("touchstart", event -> dispatchTouch((TouchEvent) event, TouchPhase.START));
    canvas.addEventListener("touchend", event -> dispatchTouch((TouchEvent) event, TouchPhase.END));
    canvas.addEventListener("touchmove", event -> dispatchTouch((TouchEvent) event, TouchPhase.MOVE));
    canvas.addEventListener("touchcancel", event -> dispatchTouch((TouchEvent) event, TouchPhase.CANCEL));
  }

  private void onKeyDown(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = true;
    }
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyDown(flixelKey);
    }
  }

  private void onKeyUp(int flixelKey) {
    if (flixelKey >= 0 && flixelKey < keyDown.length) {
      keyDown[flixelKey] = false;
    }
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyUp(flixelKey);
    }
  }

  private void onKeyTyped(char character) {
    for (int i = 0; i < keyboardListeners.getSize(); i++) {
      keyboardListeners.get(i).keyTyped(character);
    }
  }

  private void onMouseDown(int button, int x, int y) {
    if (button >= 0 && button < buttonDown.length) {
      buttonDown[button] = true;
    }
    mouseX = x;
    mouseY = y;
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseDown(button, x, y);
    }
  }

  private void onMouseUp(int button, int x, int y) {
    if (button >= 0 && button < buttonDown.length) {
      buttonDown[button] = false;
    }
    mouseX = x;
    mouseY = y;
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).mouseUp(button, x, y);
    }
  }

  private void onMouseMoved(int x, int y) {
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

  private void onScrolled(float amountX, float amountY) {
    for (int i = 0; i < mouseListeners.getSize(); i++) {
      mouseListeners.get(i).scrolled(amountX, amountY);
    }
  }

  /**
   * Forwards the changed touches of a browser touch event to the touch listeners.
   *
   * <p>The touch's position in the event's changed-touches list is used as the pointer index, which
   * keeps single-finger use on pointer zero and lets a few simultaneous fingers map to higher
   * pointers without any per-frame allocation.
   *
   * @param event The browser touch event.
   * @param phase Which lifecycle phase (start, move, end, cancel) fired.
   */
  private void dispatchTouch(TouchEvent event, TouchPhase phase) {
    event.preventDefault();
    for (int i = 0; i < event.getChangedTouches().getLength(); i++) {
      Touch touch = event.getChangedTouches().get(i);
      int x = canvasX(canvas, (int) touch.getClientX());
      int y = canvasY(canvas, (int) touch.getClientY());
      for (int j = 0; j < touchListeners.getSize(); j++) {
        FlixelTouchListener listener = touchListeners.get(j);
        switch (phase) {
          case START -> listener.touched(i, x, y);
          case MOVE -> listener.touchDragged(i, x, y);
          case END -> listener.touchReleased(i, x, y);
          case CANCEL -> listener.touchCancelled(i, x, y);
        }
      }
    }
  }

  /**
   * Remaps a browser mouse button index to its {@link org.flixelgdx.input.mouse.FlixelMouseButton}
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

  @JSBody(params = { "canvas", "clientX" },
      script = "var r = canvas.getBoundingClientRect();"
          + "return Math.round((clientX - r.left) * (canvas.width / r.width));")
  private static native int canvasX(HTMLCanvasElement canvas, int clientX);

  @JSBody(params = { "canvas", "clientY" },
      script = "var r = canvas.getBoundingClientRect();"
          + "return Math.round((clientY - r.top) * (canvas.height / r.height));")
  private static native int canvasY(HTMLCanvasElement canvas, int clientY);

  /** The lifecycle phase of a browser touch event, used to pick the listener callback to fire. */
  private enum TouchPhase {
    START,
    MOVE,
    END,
    CANCEL
  }
}
