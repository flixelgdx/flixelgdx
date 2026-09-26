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
package org.flixelgdx.backend.android.input;

import android.view.KeyEvent;
import org.flixelgdx.input.keyboard.FlixelKey;

import java.util.Arrays;

/**
 * Translates Android {@code KeyEvent.KEYCODE_*} values into {@link FlixelKey} codes.
 *
 * <p>Most Android keycode values happen to be numerically identical to the matching
 * {@link FlixelKey} constant (both originate from the same Android heritage), so the vast
 * majority of entries in the lookup table are identity mappings. The handful of exceptions
 * (Ctrl keys, the cursor-movement Home/End keys, and a few others) are listed explicitly.
 * Any unmapped keycode returns {@link FlixelKey#UNKNOWN}.
 *
 * <p>The lookup is a plain {@code int[]} indexed by Android keycode, filled once at class
 * load time. Each per-event call is a single bounds-checked array read, so translating input
 * never allocates.
 *
 * <p>Example:
 *
 * <pre>{@code
 * int flixelKey = FlixelAndroidKeyMap.toFlixelKey(event.getKeyCode());
 * if (flixelKey != FlixelKey.UNKNOWN) {
 *   device.postKeyDown(flixelKey);
 * }
 * }</pre>
 *
 * @see FlixelKey
 */
public final class FlixelAndroidKeyMap {

  /** Table size covers all Android keycodes that have a {@link FlixelKey} counterpart. */
  private static final int TABLE_SIZE = 256;

  /**
   * Keycode-to-FlixelKey table. Entries default to {@link FlixelKey#UNKNOWN} for unmapped keys.
   */
  private static final int[] TABLE = new int[TABLE_SIZE];

  static {
    Arrays.fill(TABLE, FlixelKey.UNKNOWN);

    // Hardware navigation: BACK is consumed by the game loop.
    put(KeyEvent.KEYCODE_BACK, FlixelKey.BACK);

    // D-pad / arrow keys.
    put(KeyEvent.KEYCODE_DPAD_UP, FlixelKey.DPAD_UP);
    put(KeyEvent.KEYCODE_DPAD_DOWN, FlixelKey.DPAD_DOWN);
    put(KeyEvent.KEYCODE_DPAD_LEFT, FlixelKey.DPAD_LEFT);
    put(KeyEvent.KEYCODE_DPAD_RIGHT, FlixelKey.DPAD_RIGHT);
    put(KeyEvent.KEYCODE_DPAD_CENTER, FlixelKey.DPAD_CENTER);

    // Volume keys are intentionally left unmapped; the launcher does not consume them.

    // Digits 0-9.
    put(KeyEvent.KEYCODE_0, FlixelKey.NUM_0);
    put(KeyEvent.KEYCODE_1, FlixelKey.NUM_1);
    put(KeyEvent.KEYCODE_2, FlixelKey.NUM_2);
    put(KeyEvent.KEYCODE_3, FlixelKey.NUM_3);
    put(KeyEvent.KEYCODE_4, FlixelKey.NUM_4);
    put(KeyEvent.KEYCODE_5, FlixelKey.NUM_5);
    put(KeyEvent.KEYCODE_6, FlixelKey.NUM_6);
    put(KeyEvent.KEYCODE_7, FlixelKey.NUM_7);
    put(KeyEvent.KEYCODE_8, FlixelKey.NUM_8);
    put(KeyEvent.KEYCODE_9, FlixelKey.NUM_9);

    // Letters A-Z. Android KEYCODE_A..KEYCODE_Z values match FlixelKey.A..Z exactly.
    put(KeyEvent.KEYCODE_A, FlixelKey.A);
    put(KeyEvent.KEYCODE_B, FlixelKey.B);
    put(KeyEvent.KEYCODE_C, FlixelKey.C);
    put(KeyEvent.KEYCODE_D, FlixelKey.D);
    put(KeyEvent.KEYCODE_E, FlixelKey.E);
    put(KeyEvent.KEYCODE_F, FlixelKey.F);
    put(KeyEvent.KEYCODE_G, FlixelKey.G);
    put(KeyEvent.KEYCODE_H, FlixelKey.H);
    put(KeyEvent.KEYCODE_I, FlixelKey.I);
    put(KeyEvent.KEYCODE_J, FlixelKey.J);
    put(KeyEvent.KEYCODE_K, FlixelKey.K);
    put(KeyEvent.KEYCODE_L, FlixelKey.L);
    put(KeyEvent.KEYCODE_M, FlixelKey.M);
    put(KeyEvent.KEYCODE_N, FlixelKey.N);
    put(KeyEvent.KEYCODE_O, FlixelKey.O);
    put(KeyEvent.KEYCODE_P, FlixelKey.P);
    put(KeyEvent.KEYCODE_Q, FlixelKey.Q);
    put(KeyEvent.KEYCODE_R, FlixelKey.R);
    put(KeyEvent.KEYCODE_S, FlixelKey.S);
    put(KeyEvent.KEYCODE_T, FlixelKey.T);
    put(KeyEvent.KEYCODE_U, FlixelKey.U);
    put(KeyEvent.KEYCODE_V, FlixelKey.V);
    put(KeyEvent.KEYCODE_W, FlixelKey.W);
    put(KeyEvent.KEYCODE_X, FlixelKey.X);
    put(KeyEvent.KEYCODE_Y, FlixelKey.Y);
    put(KeyEvent.KEYCODE_Z, FlixelKey.Z);

    // Punctuation and symbols.
    put(KeyEvent.KEYCODE_COMMA, FlixelKey.COMMA);
    put(KeyEvent.KEYCODE_PERIOD, FlixelKey.PERIOD);
    put(KeyEvent.KEYCODE_MINUS, FlixelKey.MINUS);
    put(KeyEvent.KEYCODE_EQUALS, FlixelKey.EQUALS);
    put(KeyEvent.KEYCODE_LEFT_BRACKET, FlixelKey.LEFT_BRACKET);
    put(KeyEvent.KEYCODE_RIGHT_BRACKET, FlixelKey.RIGHT_BRACKET);
    put(KeyEvent.KEYCODE_BACKSLASH, FlixelKey.BACKSLASH);
    put(KeyEvent.KEYCODE_SEMICOLON, FlixelKey.SEMICOLON);
    put(KeyEvent.KEYCODE_APOSTROPHE, FlixelKey.APOSTROPHE);
    put(KeyEvent.KEYCODE_SLASH, FlixelKey.SLASH);
    put(KeyEvent.KEYCODE_AT, FlixelKey.AT);
    put(KeyEvent.KEYCODE_GRAVE, FlixelKey.GRAVE);
    put(KeyEvent.KEYCODE_PLUS, FlixelKey.PLUS);

    // Modifier keys.
    put(KeyEvent.KEYCODE_ALT_LEFT, FlixelKey.ALT_LEFT);
    put(KeyEvent.KEYCODE_ALT_RIGHT, FlixelKey.ALT_RIGHT);
    put(KeyEvent.KEYCODE_SHIFT_LEFT, FlixelKey.SHIFT_LEFT);
    put(KeyEvent.KEYCODE_SHIFT_RIGHT, FlixelKey.SHIFT_RIGHT);
    // KEYCODE_CTRL_LEFT = 113, but FlixelKey.CONTROL_LEFT = 129 (values differ).
    put(KeyEvent.KEYCODE_CTRL_LEFT, FlixelKey.CONTROL_LEFT);
    // KEYCODE_CTRL_RIGHT = 114, but FlixelKey.CONTROL_RIGHT = 130 (values differ).
    put(KeyEvent.KEYCODE_CTRL_RIGHT, FlixelKey.CONTROL_RIGHT);
    put(KeyEvent.KEYCODE_CAPS_LOCK, FlixelKey.CAPS_LOCK);
    put(KeyEvent.KEYCODE_SCROLL_LOCK, FlixelKey.SCROLL_LOCK);

    // Whitespace and navigation keys.
    put(KeyEvent.KEYCODE_TAB, FlixelKey.TAB);
    put(KeyEvent.KEYCODE_SPACE, FlixelKey.SPACE);
    put(KeyEvent.KEYCODE_ENTER, FlixelKey.ENTER);
    put(KeyEvent.KEYCODE_DEL, FlixelKey.DEL);
    put(KeyEvent.KEYCODE_FORWARD_DEL, FlixelKey.FORWARD_DEL);
    put(KeyEvent.KEYCODE_ESCAPE, FlixelKey.ESCAPE);

    // Cursor-movement Home/End: KEYCODE_MOVE_HOME = 122, KEYCODE_MOVE_END = 123.
    // FlixelKey.HOME = 3 (the Android hardware Home button), so these are explicit overrides.
    put(KeyEvent.KEYCODE_MOVE_HOME, FlixelKey.HOME);
    put(KeyEvent.KEYCODE_MOVE_END, FlixelKey.END);
    put(KeyEvent.KEYCODE_INSERT, FlixelKey.INSERT);
    put(KeyEvent.KEYCODE_PAGE_UP, FlixelKey.PAGE_UP);
    put(KeyEvent.KEYCODE_PAGE_DOWN, FlixelKey.PAGE_DOWN);
    // KEYCODE_PRINT_SCREEN does not exist in Android's KeyEvent; omitted intentionally.
    put(KeyEvent.KEYCODE_BREAK, FlixelKey.PAUSE);
    put(KeyEvent.KEYCODE_NUM_LOCK, FlixelKey.NUM_LOCK);
    put(KeyEvent.KEYCODE_MENU, FlixelKey.MENU);

    // Function keys F1-F12 (and higher where Android defines them).
    put(KeyEvent.KEYCODE_F1, FlixelKey.F1);
    put(KeyEvent.KEYCODE_F2, FlixelKey.F2);
    put(KeyEvent.KEYCODE_F3, FlixelKey.F3);
    put(KeyEvent.KEYCODE_F4, FlixelKey.F4);
    put(KeyEvent.KEYCODE_F5, FlixelKey.F5);
    put(KeyEvent.KEYCODE_F6, FlixelKey.F6);
    put(KeyEvent.KEYCODE_F7, FlixelKey.F7);
    put(KeyEvent.KEYCODE_F8, FlixelKey.F8);
    put(KeyEvent.KEYCODE_F9, FlixelKey.F9);
    put(KeyEvent.KEYCODE_F10, FlixelKey.F10);
    put(KeyEvent.KEYCODE_F11, FlixelKey.F11);
    put(KeyEvent.KEYCODE_F12, FlixelKey.F12);

    // Numpad.
    put(KeyEvent.KEYCODE_NUMPAD_0, FlixelKey.NUMPAD_0);
    put(KeyEvent.KEYCODE_NUMPAD_1, FlixelKey.NUMPAD_1);
    put(KeyEvent.KEYCODE_NUMPAD_2, FlixelKey.NUMPAD_2);
    put(KeyEvent.KEYCODE_NUMPAD_3, FlixelKey.NUMPAD_3);
    put(KeyEvent.KEYCODE_NUMPAD_4, FlixelKey.NUMPAD_4);
    put(KeyEvent.KEYCODE_NUMPAD_5, FlixelKey.NUMPAD_5);
    put(KeyEvent.KEYCODE_NUMPAD_6, FlixelKey.NUMPAD_6);
    put(KeyEvent.KEYCODE_NUMPAD_7, FlixelKey.NUMPAD_7);
    put(KeyEvent.KEYCODE_NUMPAD_8, FlixelKey.NUMPAD_8);
    put(KeyEvent.KEYCODE_NUMPAD_9, FlixelKey.NUMPAD_9);
    put(KeyEvent.KEYCODE_NUMPAD_DIVIDE, FlixelKey.NUMPAD_DIVIDE);
    put(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, FlixelKey.NUMPAD_MULTIPLY);
    put(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, FlixelKey.NUMPAD_SUBTRACT);
    put(KeyEvent.KEYCODE_NUMPAD_ADD, FlixelKey.NUMPAD_ADD);
    put(KeyEvent.KEYCODE_NUMPAD_DOT, FlixelKey.NUMPAD_DOT);
    put(KeyEvent.KEYCODE_NUMPAD_COMMA, FlixelKey.NUMPAD_COMMA);
    put(KeyEvent.KEYCODE_NUMPAD_ENTER, FlixelKey.NUMPAD_ENTER);
    put(KeyEvent.KEYCODE_NUMPAD_EQUALS, FlixelKey.NUMPAD_EQUALS);
    put(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN, FlixelKey.NUMPAD_LEFT_PAREN);
    put(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN, FlixelKey.NUMPAD_RIGHT_PAREN);
  }

  private FlixelAndroidKeyMap() {}

  /**
   * Translates an Android keycode into its {@link FlixelKey} code.
   *
   * @param keyCode The Android {@code KeyEvent.KEYCODE_*} value.
   * @return The matching {@link FlixelKey} code, or {@link FlixelKey#UNKNOWN} if the key is
   *     not mapped.
   */
  public static int toFlixelKey(int keyCode) {
    if (keyCode < 0 || keyCode >= TABLE_SIZE) {
      return FlixelKey.UNKNOWN;
    }
    return TABLE[keyCode];
  }

  /** Stores a keycode-to-FlixelKey entry; silently ignores out-of-range keycodes. */
  private static void put(int keyCode, int flixelKey) {
    if (keyCode >= 0 && keyCode < TABLE_SIZE) {
      TABLE[keyCode] = flixelKey;
    }
  }
}
