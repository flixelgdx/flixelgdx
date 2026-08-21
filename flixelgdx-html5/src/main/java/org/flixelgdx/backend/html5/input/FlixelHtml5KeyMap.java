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

import org.flixelgdx.input.keyboard.FlixelKey;

/**
 * Translates a browser {@code KeyboardEvent.code} string into a {@link FlixelKey} constant.
 *
 * <p>The browser reports each physical key twice: as {@code event.key} (the character produced,
 * which changes with layout and modifiers) and as {@code event.code} (the physical key position,
 * which does not). Games want the physical position so that, for example, the {@code W} key keeps
 * moving the player forward regardless of the user's keyboard layout, so this map keys off
 * {@code event.code}.
 *
 * <p>The lookup is a plain {@code switch} on the code string rather than a hash map. A {@code switch}
 * on constant strings compiles to an efficient jump and, just as importantly for this framework,
 * allocates nothing, so it is safe to call on every key event without generating garbage.
 */
public final class FlixelHtml5KeyMap {

  private FlixelHtml5KeyMap() {}

  /**
   * Maps a browser physical key code to its {@link FlixelKey} equivalent.
   *
   * @param code The {@code KeyboardEvent.code} value (for example {@code "KeyA"} or {@code "ArrowUp"}).
   * @return The matching {@link FlixelKey} constant, or {@link FlixelKey#UNKNOWN} when unmapped.
   */
  public static int toFlixelKey(String code) {
    if (code == null) {
      return FlixelKey.UNKNOWN;
    }
    return switch (code) {
      case "KeyA" -> FlixelKey.A;
      case "KeyB" -> FlixelKey.B;
      case "KeyC" -> FlixelKey.C;
      case "KeyD" -> FlixelKey.D;
      case "KeyE" -> FlixelKey.E;
      case "KeyF" -> FlixelKey.F;
      case "KeyG" -> FlixelKey.G;
      case "KeyH" -> FlixelKey.H;
      case "KeyI" -> FlixelKey.I;
      case "KeyJ" -> FlixelKey.J;
      case "KeyK" -> FlixelKey.K;
      case "KeyL" -> FlixelKey.L;
      case "KeyM" -> FlixelKey.M;
      case "KeyN" -> FlixelKey.N;
      case "KeyO" -> FlixelKey.O;
      case "KeyP" -> FlixelKey.P;
      case "KeyQ" -> FlixelKey.Q;
      case "KeyR" -> FlixelKey.R;
      case "KeyS" -> FlixelKey.S;
      case "KeyT" -> FlixelKey.T;
      case "KeyU" -> FlixelKey.U;
      case "KeyV" -> FlixelKey.V;
      case "KeyW" -> FlixelKey.W;
      case "KeyX" -> FlixelKey.X;
      case "KeyY" -> FlixelKey.Y;
      case "KeyZ" -> FlixelKey.Z;
      case "Digit0" -> FlixelKey.NUM_0;
      case "Digit1" -> FlixelKey.NUM_1;
      case "Digit2" -> FlixelKey.NUM_2;
      case "Digit3" -> FlixelKey.NUM_3;
      case "Digit4" -> FlixelKey.NUM_4;
      case "Digit5" -> FlixelKey.NUM_5;
      case "Digit6" -> FlixelKey.NUM_6;
      case "Digit7" -> FlixelKey.NUM_7;
      case "Digit8" -> FlixelKey.NUM_8;
      case "Digit9" -> FlixelKey.NUM_9;
      case "ArrowLeft" -> FlixelKey.LEFT;
      case "ArrowRight" -> FlixelKey.RIGHT;
      case "ArrowUp" -> FlixelKey.UP;
      case "ArrowDown" -> FlixelKey.DOWN;
      case "Space" -> FlixelKey.SPACE;
      case "Enter" -> FlixelKey.ENTER;
      case "Escape" -> FlixelKey.ESCAPE;
      case "Tab" -> FlixelKey.TAB;
      case "Backspace" -> FlixelKey.BACKSPACE;
      case "Delete" -> FlixelKey.FORWARD_DEL;
      case "ShiftLeft" -> FlixelKey.SHIFT_LEFT;
      case "ShiftRight" -> FlixelKey.SHIFT_RIGHT;
      case "ControlLeft" -> FlixelKey.CONTROL_LEFT;
      case "ControlRight" -> FlixelKey.CONTROL_RIGHT;
      case "AltLeft" -> FlixelKey.ALT_LEFT;
      case "AltRight" -> FlixelKey.ALT_RIGHT;
      case "CapsLock" -> FlixelKey.CAPS_LOCK;
      case "Home" -> FlixelKey.HOME;
      case "End" -> FlixelKey.END;
      case "PageUp" -> FlixelKey.PAGE_UP;
      case "PageDown" -> FlixelKey.PAGE_DOWN;
      case "Insert" -> FlixelKey.INSERT;
      case "Minus" -> FlixelKey.MINUS;
      case "Equal" -> FlixelKey.EQUALS;
      case "BracketLeft" -> FlixelKey.LEFT_BRACKET;
      case "BracketRight" -> FlixelKey.RIGHT_BRACKET;
      case "Backslash" -> FlixelKey.BACKSLASH;
      case "Semicolon" -> FlixelKey.SEMICOLON;
      case "Quote" -> FlixelKey.APOSTROPHE;
      case "Backquote" -> FlixelKey.GRAVE;
      case "Comma" -> FlixelKey.COMMA;
      case "Period" -> FlixelKey.PERIOD;
      case "Slash" -> FlixelKey.SLASH;
      case "ScrollLock" -> FlixelKey.SCROLL_LOCK;
      case "NumLock" -> FlixelKey.NUM_LOCK;
      case "Numpad0" -> FlixelKey.NUMPAD_0;
      case "Numpad1" -> FlixelKey.NUMPAD_1;
      case "Numpad2" -> FlixelKey.NUMPAD_2;
      case "Numpad3" -> FlixelKey.NUMPAD_3;
      case "Numpad4" -> FlixelKey.NUMPAD_4;
      case "Numpad5" -> FlixelKey.NUMPAD_5;
      case "Numpad6" -> FlixelKey.NUMPAD_6;
      case "Numpad7" -> FlixelKey.NUMPAD_7;
      case "Numpad8" -> FlixelKey.NUMPAD_8;
      case "Numpad9" -> FlixelKey.NUMPAD_9;
      case "NumpadDivide" -> FlixelKey.NUMPAD_DIVIDE;
      case "NumpadMultiply" -> FlixelKey.NUMPAD_MULTIPLY;
      case "NumpadSubtract" -> FlixelKey.NUMPAD_SUBTRACT;
      case "NumpadAdd" -> FlixelKey.NUMPAD_ADD;
      case "NumpadDecimal" -> FlixelKey.NUMPAD_DOT;
      case "NumpadEnter" -> FlixelKey.NUMPAD_ENTER;
      case "NumpadEqual" -> FlixelKey.NUMPAD_EQUALS;
      case "F1" -> FlixelKey.F1;
      case "F2" -> FlixelKey.F2;
      case "F3" -> FlixelKey.F3;
      case "F4" -> FlixelKey.F4;
      case "F5" -> FlixelKey.F5;
      case "F6" -> FlixelKey.F6;
      case "F7" -> FlixelKey.F7;
      case "F8" -> FlixelKey.F8;
      case "F9" -> FlixelKey.F9;
      case "F10" -> FlixelKey.F10;
      case "F11" -> FlixelKey.F11;
      case "F12" -> FlixelKey.F12;
      default -> FlixelKey.UNKNOWN;
    };
  }
}
