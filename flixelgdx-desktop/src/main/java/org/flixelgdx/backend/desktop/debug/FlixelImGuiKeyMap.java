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
package org.flixelgdx.backend.desktop.debug;

import org.flixelgdx.input.keyboard.FlixelKey;

import imgui.flag.ImGuiKey;

/**
 * Translates FlixelGDX {@link FlixelKey} codes into Dear ImGui {@link ImGuiKey} codes.
 *
 * <p>The desktop runner already turns SDL scancodes into FlixelGDX key codes for the game's input
 * layer, so the debug overlay's ImGui platform backend reuses those instead of parsing SDL a second
 * time: it just needs to know which ImGui key each FlixelGDX key corresponds to. ImGui uses these
 * codes to drive keyboard navigation and text-field editing (arrows, Home/End, Backspace, and so on);
 * printable characters arrive separately as text-input events.
 *
 * <p>The lookup is a plain {@code int[]} indexed by FlixelGDX key code, filled once when the class
 * loads, so each translation is a single bounds-checked array read that never allocates. Unmapped
 * keys resolve to {@link ImGuiKey#None}.
 */
public final class FlixelImGuiKeyMap {

  /** FlixelKey-to-ImGuiKey table. Unmapped entries stay {@link ImGuiKey#None} (the array default). */
  private static final int[] TABLE = new int[512];

  static {
    // Letters, number-row digits, and numpad digits are contiguous in both key spaces, so they map
    // in order; everything else is listed explicitly.
    for (int i = 0; i < 26; i++) {
      put(FlixelKey.A + i, ImGuiKey.A + i);
    }
    for (int i = 0; i < 10; i++) {
      put(FlixelKey.NUM_0 + i, ImGuiKey._0 + i);
      put(FlixelKey.NUMPAD_0 + i, ImGuiKey.Keypad0 + i);
    }

    put(FlixelKey.F1, ImGuiKey.F1);
    put(FlixelKey.F2, ImGuiKey.F2);
    put(FlixelKey.F3, ImGuiKey.F3);
    put(FlixelKey.F4, ImGuiKey.F4);
    put(FlixelKey.F5, ImGuiKey.F5);
    put(FlixelKey.F6, ImGuiKey.F6);
    put(FlixelKey.F7, ImGuiKey.F7);
    put(FlixelKey.F8, ImGuiKey.F8);
    put(FlixelKey.F9, ImGuiKey.F9);
    put(FlixelKey.F10, ImGuiKey.F10);
    put(FlixelKey.F11, ImGuiKey.F11);
    put(FlixelKey.F12, ImGuiKey.F12);

    put(FlixelKey.ENTER, ImGuiKey.Enter);
    put(FlixelKey.ESCAPE, ImGuiKey.Escape);
    put(FlixelKey.BACKSPACE, ImGuiKey.Backspace);
    put(FlixelKey.TAB, ImGuiKey.Tab);
    put(FlixelKey.SPACE, ImGuiKey.Space);

    put(FlixelKey.LEFT, ImGuiKey.LeftArrow);
    put(FlixelKey.RIGHT, ImGuiKey.RightArrow);
    put(FlixelKey.UP, ImGuiKey.UpArrow);
    put(FlixelKey.DOWN, ImGuiKey.DownArrow);

    put(FlixelKey.INSERT, ImGuiKey.Insert);
    put(FlixelKey.FORWARD_DEL, ImGuiKey.Delete);
    put(FlixelKey.HOME, ImGuiKey.Home);
    put(FlixelKey.END, ImGuiKey.End);
    put(FlixelKey.PAGE_UP, ImGuiKey.PageUp);
    put(FlixelKey.PAGE_DOWN, ImGuiKey.PageDown);

    put(FlixelKey.MINUS, ImGuiKey.Minus);
    put(FlixelKey.EQUALS, ImGuiKey.Equal);
    put(FlixelKey.LEFT_BRACKET, ImGuiKey.LeftBracket);
    put(FlixelKey.RIGHT_BRACKET, ImGuiKey.RightBracket);
    put(FlixelKey.BACKSLASH, ImGuiKey.Backslash);
    put(FlixelKey.SEMICOLON, ImGuiKey.Semicolon);
    put(FlixelKey.APOSTROPHE, ImGuiKey.Apostrophe);
    put(FlixelKey.GRAVE, ImGuiKey.GraveAccent);
    put(FlixelKey.COMMA, ImGuiKey.Comma);
    put(FlixelKey.PERIOD, ImGuiKey.Period);
    put(FlixelKey.SLASH, ImGuiKey.Slash);

    put(FlixelKey.CAPS_LOCK, ImGuiKey.CapsLock);
    put(FlixelKey.SCROLL_LOCK, ImGuiKey.ScrollLock);
    put(FlixelKey.NUM_LOCK, ImGuiKey.NumLock);
    put(FlixelKey.PRINT_SCREEN, ImGuiKey.PrintScreen);
    put(FlixelKey.PAUSE, ImGuiKey.Pause);
    put(FlixelKey.MENU, ImGuiKey.Menu);

    put(FlixelKey.CONTROL_LEFT, ImGuiKey.LeftCtrl);
    put(FlixelKey.CONTROL_RIGHT, ImGuiKey.RightCtrl);
    put(FlixelKey.SHIFT_LEFT, ImGuiKey.LeftShift);
    put(FlixelKey.SHIFT_RIGHT, ImGuiKey.RightShift);
    put(FlixelKey.ALT_LEFT, ImGuiKey.LeftAlt);
    put(FlixelKey.ALT_RIGHT, ImGuiKey.RightAlt);
    put(FlixelKey.SYM, ImGuiKey.LeftSuper);

    put(FlixelKey.NUMPAD_DIVIDE, ImGuiKey.KeypadDivide);
    put(FlixelKey.NUMPAD_MULTIPLY, ImGuiKey.KeypadMultiply);
    put(FlixelKey.NUMPAD_SUBTRACT, ImGuiKey.KeypadSubtract);
    put(FlixelKey.NUMPAD_ADD, ImGuiKey.KeypadAdd);
    put(FlixelKey.NUMPAD_ENTER, ImGuiKey.KeypadEnter);
    put(FlixelKey.NUMPAD_DOT, ImGuiKey.KeypadDecimal);
    put(FlixelKey.NUMPAD_EQUALS, ImGuiKey.KeypadEqual);
  }

  private FlixelImGuiKeyMap() {}

  /**
   * Translates a FlixelGDX key code into its Dear ImGui key code.
   *
   * @param flixelKey The {@link FlixelKey} code to translate.
   * @return The matching {@link ImGuiKey} code, or {@link ImGuiKey#None} if the key is unmapped.
   */
  public static int toImGuiKey(int flixelKey) {
    if (flixelKey < 0 || flixelKey >= TABLE.length) {
      return ImGuiKey.None;
    }
    return TABLE[flixelKey];
  }

  /** Records a FlixelKey-to-ImGuiKey entry, ignoring out-of-range codes so loading never fails. */
  private static void put(int flixelKey, int imGuiKey) {
    if (flixelKey >= 0 && flixelKey < TABLE.length) {
      TABLE[flixelKey] = imGuiKey;
    }
  }
}
