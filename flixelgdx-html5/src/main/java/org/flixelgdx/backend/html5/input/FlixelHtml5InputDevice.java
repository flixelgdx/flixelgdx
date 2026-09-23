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
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLTextAreaElement;

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
 *
 * <h2>The text-input bridge</h2>
 *
 * <p>A browser only runs IME composition, spell-correction, and native paste handling for an
 * element that is actually focused and editable. The game canvas is neither, so this device keeps
 * a hidden {@code <textarea>} (see {@link #ensureTextInputBridge()}) that it focuses whenever
 * {@link #onTextInputStarted()} fires. Think of it like a conference's translator booth: nobody in
 * the audience sees it, but it is the one seat in the room wired into the interpreter's equipment.
 * The booth does not perform for the room itself; it exists purely so the browser's own IME and
 * text-composition pipeline has somewhere to attach. Typed and composed characters that land in the
 * bridge are drained through {@link #dispatchKeyTyped} and never rendered anywhere, so the game
 * never learns the bridge exists.
 *
 * <p>Copy, cut, select-all, and paste are deliberately left to the game's own clipboard handling
 * (through the host integration), so while the bridge is focused this device prevents the browser
 * default for those shortcuts, stopping them from also inserting or removing text in the hidden
 * field. Because the bridge only exists to intercept IME and IME-adjacent browser behavior, plain
 * typed characters are instead dispatched straight from {@code keydown} while the bridge is
 * inactive, exactly as before.
 */
public class FlixelHtml5InputDevice extends FlixelBaseInputDevice {

  private final boolean[] keyDown = new boolean[512];
  private final boolean[] buttonDown = new boolean[8];

  private HTMLCanvasElement canvas;
  private HTMLTextAreaElement textInputBridge;

  private int mouseX;
  private int mouseY;

  private boolean composing;

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
      // While the text-input bridge is focused, typed characters arrive through it instead (see
      // the input/composition listeners wired in ensureTextInputBridge()), so only editing keys
      // like Backspace and the arrows need to keep flowing from here. Ctrl/Meta shortcuts that
      // would otherwise copy, cut, select, or paste into the hidden field are swallowed, since the
      // game handles clipboard access itself.
      if (isTextInputActive()) {
        if (shouldPreventDefaultForBridge(key)) {
          event.preventDefault();
        }
      } else {
        dispatchTypedFromKeydown(key);
      }
      // The browser repeats keydown while a key is held. Route repeats to onKeyRepeated instead
      // of onKeyDown so justPressed stays true for a single frame only.
      if (key.isRepeat()) {
        onKeyRepeated(FlixelHtml5KeyMap.toFlixelKey(key.getCode()));
      } else {
        onKeyDown(FlixelHtml5KeyMap.toFlixelKey(key.getCode()));
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
   * Dispatches a typed character straight from a {@code keydown} event, used only while the
   * text-input bridge is inactive (see {@link #isTextInputActive()}). Modifier combinations such as
   * Ctrl+V still produce a one-character {@code KeyboardEvent.key} value in most browsers (for
   * example {@code "v"}), so those are skipped here to avoid feeding shortcut letters back into the
   * game as typed text. Characters outside the Basic Multilingual Plane arrive as a two-character
   * surrogate pair, which is dispatched as two separate calls to match how the text-input bridge
   * (and the desktop backend) deliver them.
   *
   * @param key The {@code keydown} event to read the typed character from.
   */
  private void dispatchTypedFromKeydown(KeyboardEvent key) {
    if (key.isCtrlKey() || key.isMetaKey() || key.isAltKey()) {
      return;
    }
    String printable = key.getKey();
    if (printable == null) {
      return;
    }
    if (printable.length() == 1) {
      onKeyTyped(printable.charAt(0));
    } else if (printable.length() == 2 && Character.isHighSurrogate(printable.charAt(0))
        && Character.isLowSurrogate(printable.charAt(1))) {
      onKeyTyped(printable.charAt(0));
      onKeyTyped(printable.charAt(1));
    }
  }

  /**
   * Lazily creates the hidden {@code <textarea>} that the browser's IME and paste handling attach
   * to, wiring its {@code input} and composition listeners once. See the class Javadoc for why this
   * bridge exists. Calling this more than once is safe; only the first call builds anything.
   */
  private void ensureTextInputBridge() {
    if (textInputBridge != null) {
      return;
    }
    HTMLDocument document = HTMLDocument.current();
    textInputBridge = (HTMLTextAreaElement) document.createElement("textarea");
    textInputBridge.setAttribute("autocapitalize", "off");
    textInputBridge.setAttribute("autocomplete", "off");
    textInputBridge.setAttribute("autocorrect", "off");
    textInputBridge.setAttribute("spellcheck", "false");
    textInputBridge.setAttribute("tabindex", "-1");
    textInputBridge.getStyle().setProperty("position", "fixed");
    textInputBridge.getStyle().setProperty("opacity", "0");
    textInputBridge.getStyle().setProperty("pointer-events", "none");
    textInputBridge.getStyle().setProperty("font-size", "16px");
    textInputBridge.getStyle().setProperty("left", "0px");
    textInputBridge.getStyle().setProperty("top", "0px");
    // A plain typed character reaches the bridge as an "input" event with nothing composing.
    // While an IME composition is in progress (composing == true), the in-between "input" events
    // only reflect an unfinished composition, so they are skipped; compositionend below is what
    // finally drains the composed text.
    textInputBridge.addEventListener("input", event -> {
      if (!composing) {
        drainTextInputBridge();
      }
    });
    textInputBridge.addEventListener("compositionstart", event -> composing = true);
    textInputBridge.addEventListener("compositionend", event -> {
      composing = false;
      drainTextInputBridge();
    });
    document.getBody().appendChild(textInputBridge);
  }

  /**
   * Dispatches every UTF-16 unit currently sitting in the text-input bridge's value through
   * {@link #onKeyTyped(char)}, then clears it. Both the plain {@code input} listener and the
   * {@code compositionend} listener in {@link #ensureTextInputBridge()} call this, and browsers do
   * not agree on which of those two fires first when an IME composition commits. That ordering does
   * not matter here: whichever listener runs first drains and clears the shared value, so the other
   * one finds nothing left to dispatch. This keeps a composed character from being typed twice
   * without needing to special-case either browser's event order.
   */
  private void drainTextInputBridge() {
    String text = textInputBridge.getValue();
    if (text.isEmpty()) {
      return;
    }
    for (int i = 0; i < text.length(); i++) {
      onKeyTyped(text.charAt(i));
    }
    textInputBridge.setValue("");
  }

  @Override
  protected void onTextInputStarted() {
    ensureTextInputBridge();
    focusPreventScroll(textInputBridge);
  }

  @Override
  protected void onTextInputStopped() {
    if (textInputBridge != null) {
      // Blurring hands focus back to the page body, which keeps the window-level keydown listener
      // working exactly as it did before text input started (keydown still bubbles up to the
      // window from whatever element, if any, is focused).
      textInputBridge.blur();
    }
  }

  @Override
  protected void onTextInputAreaChanged(int x, int y, int w, int h) {
    // The bridge only needs the caret's top-left corner to place the browser's own IME candidate
    // window; the browser sizes that window itself, so the edited area's width and height are not
    // used here.
    if (textInputBridge != null && canvas != null) {
      positionTextInputBridge(canvas, x, y, textInputBridge);
    }
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

  /**
   * Returns whether a {@code keydown} event should have its browser default prevented while the
   * text-input bridge is active: Ctrl/Meta plus C, X, V, or A (copy, cut, paste, select-all, which
   * the game handles through its own clipboard host integration instead) and Tab (which would
   * otherwise move focus away from the hidden bridge).
   *
   * @param key The {@code keydown} event.
   * @return {@code true} if the browser default should be prevented.
   */
  private static boolean shouldPreventDefaultForBridge(KeyboardEvent key) {
    if ("Tab".equals(key.getCode())) {
      return true;
    }
    if (key.isCtrlKey() || key.isMetaKey()) {
      String code = key.getCode();
      return "KeyC".equals(code) || "KeyX".equals(code) || "KeyV".equals(code) || "KeyA".equals(code);
    }
    return false;
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
   * Returns {@code false}: browser games are played with a mouse, keyboard, or gamepad, so this
   * backend has no on-screen keyboard to show while text input is active.
   *
   * @return {@code false}, always.
   */
  @Override
  public boolean hasScreenKeyboard() {
    return false;
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

  @JSBody(params = { "canvas", "areaX", "areaY", "textarea" }, script = """
      var r = canvas.getBoundingClientRect();
      var left = r.left + areaX * (r.width / canvas.width);
      var top = r.top + areaY * (r.height / canvas.height);
      textarea.style.left = Math.round(left) + 'px';
      textarea.style.top = Math.round(top) + 'px';
      """)
  private static native void positionTextInputBridge(HTMLCanvasElement canvas, int areaX, int areaY,
      HTMLTextAreaElement textarea);

  @JSBody(params = "textarea", script = """
      if (textarea && textarea.focus) { textarea.focus({ preventScroll: true }); }
      """)
  private static native void focusPreventScroll(HTMLTextAreaElement textarea);
}
