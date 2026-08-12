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

import org.flixelgdx.input.keyboard.FlixelKey;
import org.lwjgl.sdl.SDLScancode;

import java.util.Arrays;

/**
 * Translates SDL3 physical scancodes into FlixelGDX {@link FlixelKey} codes.
 *
 * <p>SDL reports a physical key by its <em>scancode</em>: a layout-independent identifier for the
 * key's position, so the key next to the left shift is always the same scancode regardless of
 * whether the user runs a QWERTY or AZERTY layout. FlixelGDX has its own stable key codes, so this
 * class bridges the two.
 *
 * <p>The lookup is a plain {@code int[]} indexed by scancode, filled once when the class loads. Each
 * per-event call is a single bounds-checked array read, so translating input never allocates. SDL's
 * scancode constants are resolved from the native library at runtime rather than being compile-time
 * constants, which is why the table is built in a static initializer instead of a {@code switch}.
 */
public final class FlixelSdlKeyMap {

  /** Scancode-to-FlixelKey table. Entries default to {@link FlixelKey#NONE} for unmapped keys. */
  private static final int[] TABLE = new int[512];

  static {
    Arrays.fill(TABLE, FlixelKey.NONE);

    put(SDLScancode.SDL_SCANCODE_A, FlixelKey.A);
    put(SDLScancode.SDL_SCANCODE_B, FlixelKey.B);
    put(SDLScancode.SDL_SCANCODE_C, FlixelKey.C);
    put(SDLScancode.SDL_SCANCODE_D, FlixelKey.D);
    put(SDLScancode.SDL_SCANCODE_E, FlixelKey.E);
    put(SDLScancode.SDL_SCANCODE_F, FlixelKey.F);
    put(SDLScancode.SDL_SCANCODE_G, FlixelKey.G);
    put(SDLScancode.SDL_SCANCODE_H, FlixelKey.H);
    put(SDLScancode.SDL_SCANCODE_I, FlixelKey.I);
    put(SDLScancode.SDL_SCANCODE_J, FlixelKey.J);
    put(SDLScancode.SDL_SCANCODE_K, FlixelKey.K);
    put(SDLScancode.SDL_SCANCODE_L, FlixelKey.L);
    put(SDLScancode.SDL_SCANCODE_M, FlixelKey.M);
    put(SDLScancode.SDL_SCANCODE_N, FlixelKey.N);
    put(SDLScancode.SDL_SCANCODE_O, FlixelKey.O);
    put(SDLScancode.SDL_SCANCODE_P, FlixelKey.P);
    put(SDLScancode.SDL_SCANCODE_Q, FlixelKey.Q);
    put(SDLScancode.SDL_SCANCODE_R, FlixelKey.R);
    put(SDLScancode.SDL_SCANCODE_S, FlixelKey.S);
    put(SDLScancode.SDL_SCANCODE_T, FlixelKey.T);
    put(SDLScancode.SDL_SCANCODE_U, FlixelKey.U);
    put(SDLScancode.SDL_SCANCODE_V, FlixelKey.V);
    put(SDLScancode.SDL_SCANCODE_W, FlixelKey.W);
    put(SDLScancode.SDL_SCANCODE_X, FlixelKey.X);
    put(SDLScancode.SDL_SCANCODE_Y, FlixelKey.Y);
    put(SDLScancode.SDL_SCANCODE_Z, FlixelKey.Z);

    put(SDLScancode.SDL_SCANCODE_1, FlixelKey.NUM_1);
    put(SDLScancode.SDL_SCANCODE_2, FlixelKey.NUM_2);
    put(SDLScancode.SDL_SCANCODE_3, FlixelKey.NUM_3);
    put(SDLScancode.SDL_SCANCODE_4, FlixelKey.NUM_4);
    put(SDLScancode.SDL_SCANCODE_5, FlixelKey.NUM_5);
    put(SDLScancode.SDL_SCANCODE_6, FlixelKey.NUM_6);
    put(SDLScancode.SDL_SCANCODE_7, FlixelKey.NUM_7);
    put(SDLScancode.SDL_SCANCODE_8, FlixelKey.NUM_8);
    put(SDLScancode.SDL_SCANCODE_9, FlixelKey.NUM_9);
    put(SDLScancode.SDL_SCANCODE_0, FlixelKey.NUM_0);

    put(SDLScancode.SDL_SCANCODE_RETURN, FlixelKey.ENTER);
    put(SDLScancode.SDL_SCANCODE_ESCAPE, FlixelKey.ESCAPE);
    put(SDLScancode.SDL_SCANCODE_BACKSPACE, FlixelKey.BACKSPACE);
    put(SDLScancode.SDL_SCANCODE_TAB, FlixelKey.TAB);
    put(SDLScancode.SDL_SCANCODE_SPACE, FlixelKey.SPACE);
    put(SDLScancode.SDL_SCANCODE_MINUS, FlixelKey.MINUS);
    put(SDLScancode.SDL_SCANCODE_EQUALS, FlixelKey.EQUALS);
    put(SDLScancode.SDL_SCANCODE_LEFTBRACKET, FlixelKey.LEFT_BRACKET);
    put(SDLScancode.SDL_SCANCODE_RIGHTBRACKET, FlixelKey.RIGHT_BRACKET);
    put(SDLScancode.SDL_SCANCODE_BACKSLASH, FlixelKey.BACKSLASH);
    put(SDLScancode.SDL_SCANCODE_NONUSBACKSLASH, FlixelKey.BACKSLASH);
    put(SDLScancode.SDL_SCANCODE_SEMICOLON, FlixelKey.SEMICOLON);
    put(SDLScancode.SDL_SCANCODE_APOSTROPHE, FlixelKey.APOSTROPHE);
    put(SDLScancode.SDL_SCANCODE_GRAVE, FlixelKey.GRAVE);
    put(SDLScancode.SDL_SCANCODE_COMMA, FlixelKey.COMMA);
    put(SDLScancode.SDL_SCANCODE_PERIOD, FlixelKey.PERIOD);
    put(SDLScancode.SDL_SCANCODE_SLASH, FlixelKey.SLASH);
    put(SDLScancode.SDL_SCANCODE_CAPSLOCK, FlixelKey.CAPS_LOCK);

    put(SDLScancode.SDL_SCANCODE_PRINTSCREEN, FlixelKey.PRINT_SCREEN);
    put(SDLScancode.SDL_SCANCODE_SCROLLLOCK, FlixelKey.SCROLL_LOCK);
    put(SDLScancode.SDL_SCANCODE_PAUSE, FlixelKey.PAUSE);
    put(SDLScancode.SDL_SCANCODE_INSERT, FlixelKey.INSERT);
    put(SDLScancode.SDL_SCANCODE_HOME, FlixelKey.HOME);
    put(SDLScancode.SDL_SCANCODE_PAGEUP, FlixelKey.PAGE_UP);
    put(SDLScancode.SDL_SCANCODE_DELETE, FlixelKey.FORWARD_DEL);
    put(SDLScancode.SDL_SCANCODE_END, FlixelKey.END);
    put(SDLScancode.SDL_SCANCODE_PAGEDOWN, FlixelKey.PAGE_DOWN);

    put(SDLScancode.SDL_SCANCODE_RIGHT, FlixelKey.RIGHT);
    put(SDLScancode.SDL_SCANCODE_LEFT, FlixelKey.LEFT);
    put(SDLScancode.SDL_SCANCODE_DOWN, FlixelKey.DOWN);
    put(SDLScancode.SDL_SCANCODE_UP, FlixelKey.UP);

    put(SDLScancode.SDL_SCANCODE_LCTRL, FlixelKey.CONTROL_LEFT);
    put(SDLScancode.SDL_SCANCODE_RCTRL, FlixelKey.CONTROL_RIGHT);
    put(SDLScancode.SDL_SCANCODE_LSHIFT, FlixelKey.SHIFT_LEFT);
    put(SDLScancode.SDL_SCANCODE_RSHIFT, FlixelKey.SHIFT_RIGHT);
    put(SDLScancode.SDL_SCANCODE_LALT, FlixelKey.ALT_LEFT);
    put(SDLScancode.SDL_SCANCODE_RALT, FlixelKey.ALT_RIGHT);
    put(SDLScancode.SDL_SCANCODE_LGUI, FlixelKey.SYM);
    put(SDLScancode.SDL_SCANCODE_RGUI, FlixelKey.SYM);
    put(SDLScancode.SDL_SCANCODE_APPLICATION, FlixelKey.MENU);
    put(SDLScancode.SDL_SCANCODE_MENU, FlixelKey.MENU);

    put(SDLScancode.SDL_SCANCODE_NUMLOCKCLEAR, FlixelKey.NUM_LOCK);
    put(SDLScancode.SDL_SCANCODE_KP_DIVIDE, FlixelKey.NUMPAD_DIVIDE);
    put(SDLScancode.SDL_SCANCODE_KP_MULTIPLY, FlixelKey.NUMPAD_MULTIPLY);
    put(SDLScancode.SDL_SCANCODE_KP_MINUS, FlixelKey.NUMPAD_SUBTRACT);
    put(SDLScancode.SDL_SCANCODE_KP_PLUS, FlixelKey.NUMPAD_ADD);
    put(SDLScancode.SDL_SCANCODE_KP_ENTER, FlixelKey.NUMPAD_ENTER);
    put(SDLScancode.SDL_SCANCODE_KP_1, FlixelKey.NUMPAD_1);
    put(SDLScancode.SDL_SCANCODE_KP_2, FlixelKey.NUMPAD_2);
    put(SDLScancode.SDL_SCANCODE_KP_3, FlixelKey.NUMPAD_3);
    put(SDLScancode.SDL_SCANCODE_KP_4, FlixelKey.NUMPAD_4);
    put(SDLScancode.SDL_SCANCODE_KP_5, FlixelKey.NUMPAD_5);
    put(SDLScancode.SDL_SCANCODE_KP_6, FlixelKey.NUMPAD_6);
    put(SDLScancode.SDL_SCANCODE_KP_7, FlixelKey.NUMPAD_7);
    put(SDLScancode.SDL_SCANCODE_KP_8, FlixelKey.NUMPAD_8);
    put(SDLScancode.SDL_SCANCODE_KP_9, FlixelKey.NUMPAD_9);
    put(SDLScancode.SDL_SCANCODE_KP_0, FlixelKey.NUMPAD_0);
    put(SDLScancode.SDL_SCANCODE_KP_PERIOD, FlixelKey.NUMPAD_DOT);
    put(SDLScancode.SDL_SCANCODE_KP_COMMA, FlixelKey.NUMPAD_COMMA);
    put(SDLScancode.SDL_SCANCODE_KP_EQUALS, FlixelKey.NUMPAD_EQUALS);
    put(SDLScancode.SDL_SCANCODE_KP_LEFTPAREN, FlixelKey.NUMPAD_LEFT_PAREN);
    put(SDLScancode.SDL_SCANCODE_KP_RIGHTPAREN, FlixelKey.NUMPAD_RIGHT_PAREN);

    put(SDLScancode.SDL_SCANCODE_F1, FlixelKey.F1);
    put(SDLScancode.SDL_SCANCODE_F2, FlixelKey.F2);
    put(SDLScancode.SDL_SCANCODE_F3, FlixelKey.F3);
    put(SDLScancode.SDL_SCANCODE_F4, FlixelKey.F4);
    put(SDLScancode.SDL_SCANCODE_F5, FlixelKey.F5);
    put(SDLScancode.SDL_SCANCODE_F6, FlixelKey.F6);
    put(SDLScancode.SDL_SCANCODE_F7, FlixelKey.F7);
    put(SDLScancode.SDL_SCANCODE_F8, FlixelKey.F8);
    put(SDLScancode.SDL_SCANCODE_F9, FlixelKey.F9);
    put(SDLScancode.SDL_SCANCODE_F10, FlixelKey.F10);
    put(SDLScancode.SDL_SCANCODE_F11, FlixelKey.F11);
    put(SDLScancode.SDL_SCANCODE_F12, FlixelKey.F12);
    put(SDLScancode.SDL_SCANCODE_F13, FlixelKey.F13);
    put(SDLScancode.SDL_SCANCODE_F14, FlixelKey.F14);
    put(SDLScancode.SDL_SCANCODE_F15, FlixelKey.F15);
    put(SDLScancode.SDL_SCANCODE_F16, FlixelKey.F16);
    put(SDLScancode.SDL_SCANCODE_F17, FlixelKey.F17);
    put(SDLScancode.SDL_SCANCODE_F18, FlixelKey.F18);
    put(SDLScancode.SDL_SCANCODE_F19, FlixelKey.F19);
    put(SDLScancode.SDL_SCANCODE_F20, FlixelKey.F20);
    put(SDLScancode.SDL_SCANCODE_F21, FlixelKey.F21);
    put(SDLScancode.SDL_SCANCODE_F22, FlixelKey.F22);
    put(SDLScancode.SDL_SCANCODE_F23, FlixelKey.F23);
    put(SDLScancode.SDL_SCANCODE_F24, FlixelKey.F24);
  }

  private FlixelSdlKeyMap() {}

  /**
   * Translates an SDL physical scancode into its FlixelGDX key code.
   *
   * @param scancode The SDL scancode reported by a keyboard event.
   * @return The matching {@link FlixelKey} code, or {@link FlixelKey#NONE} if the key is unmapped.
   */
  public static int toFlixelKey(int scancode) {
    if (scancode < 0 || scancode >= TABLE.length) {
      return FlixelKey.NONE;
    }
    return TABLE[scancode];
  }

  /** Records a scancode-to-key entry, ignoring out-of-range scancodes so loading never fails. */
  private static void put(int scancode, int flixelKey) {
    if (scancode >= 0 && scancode < TABLE.length) {
      TABLE[scancode] = flixelKey;
    }
  }
}
