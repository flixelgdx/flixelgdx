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
package org.flixelgdx.input.keyboard;

import org.flixelgdx.collections.FlixelMap;

/**
 * Keyboard key codes used for {@link FlixelKeyInputManager}.
 *
 * <p>The numeric values are stable identifiers for each physical key; treat them as opaque and refer
 * to keys by name (for example {@link #SPACE} or {@link #A}). Use {@link #toString(int)} to turn a
 * code into a readable label and {@link #fromString(String)} to turn a label back into a code.
 */
public final class FlixelKey {

  private FlixelKey() {}

  public static final int NONE = -2; // In HaxeFlixel, this is -1, but we use -2 to avoid confusion with ANY.
  public static final int ANY = -1;
  public static final int NUM_0 = 7;
  public static final int NUM_1 = 8;
  public static final int NUM_2 = 9;
  public static final int NUM_3 = 10;
  public static final int NUM_4 = 11;
  public static final int NUM_5 = 12;
  public static final int NUM_6 = 13;
  public static final int NUM_7 = 14;
  public static final int NUM_8 = 15;
  public static final int NUM_9 = 16;
  public static final int A = 29;
  public static final int ALT_LEFT = 57;
  public static final int ALT_RIGHT = 58;
  public static final int APOSTROPHE = 75;
  public static final int AT = 77;
  public static final int B = 30;
  public static final int BACK = 4;
  public static final int BACKSLASH = 73;
  public static final int C = 31;
  public static final int CALL = 5;
  public static final int CAMERA = 27;
  public static final int CAPS_LOCK = 115;
  public static final int CLEAR = 28;
  public static final int COMMA = 55;
  public static final int D = 32;
  public static final int DEL = 67;
  public static final int BACKSPACE = 67;
  public static final int FORWARD_DEL = 112;
  public static final int DPAD_CENTER = 23;
  public static final int DPAD_DOWN = 20;
  public static final int DPAD_LEFT = 21;
  public static final int DPAD_RIGHT = 22;
  public static final int DPAD_UP = 19;
  public static final int CENTER = 23;
  public static final int DOWN = 20;
  public static final int LEFT = 21;
  public static final int RIGHT = 22;
  public static final int UP = 19;
  public static final int E = 33;
  public static final int ENDCALL = 6;
  public static final int ENTER = 66;
  public static final int ENVELOPE = 65;
  public static final int EQUALS = 70;
  public static final int EXPLORER = 64;
  public static final int F = 34;
  public static final int FOCUS = 80;
  public static final int G = 35;
  public static final int GRAVE = 68;
  public static final int H = 36;
  public static final int HEADSETHOOK = 79;
  public static final int HOME = 3;
  public static final int I = 37;
  public static final int J = 38;
  public static final int K = 39;
  public static final int L = 40;
  public static final int LEFT_BRACKET = 71;
  public static final int M = 41;
  public static final int MEDIA_FAST_FORWARD = 90;
  public static final int MEDIA_NEXT = 87;
  public static final int MEDIA_PLAY_PAUSE = 85;
  public static final int MEDIA_PREVIOUS = 88;
  public static final int MEDIA_REWIND = 89;
  public static final int MEDIA_STOP = 86;
  public static final int MENU = 82;
  public static final int MINUS = 69;
  public static final int MUTE = 91;
  public static final int N = 42;
  public static final int NOTIFICATION = 83;
  public static final int NUM = 78;
  public static final int O = 43;
  public static final int P = 44;
  public static final int PAUSE = 121;
  public static final int PERIOD = 56;
  public static final int PLUS = 81;
  public static final int POUND = 18;
  public static final int POWER = 26;
  public static final int PRINT_SCREEN = 120;
  public static final int Q = 45;
  public static final int R = 46;
  public static final int RIGHT_BRACKET = 72;
  public static final int S = 47;
  public static final int SCROLL_LOCK = 116;
  public static final int SEARCH = 84;
  public static final int SEMICOLON = 74;
  public static final int SHIFT_LEFT = 59;
  public static final int SHIFT_RIGHT = 60;
  public static final int SLASH = 76;
  public static final int SOFT_LEFT = 1;
  public static final int SOFT_RIGHT = 2;
  public static final int SPACE = 62;
  public static final int STAR = 17;
  public static final int SYM = 63;
  public static final int T = 48;
  public static final int TAB = 61;
  public static final int U = 49;
  public static final int UNKNOWN = 0;
  public static final int V = 50;
  public static final int VOLUME_DOWN = 25;
  public static final int VOLUME_UP = 24;
  public static final int W = 51;
  public static final int X = 52;
  public static final int Y = 53;
  public static final int Z = 54;
  public static final int META_ALT_LEFT_ON = 16;
  public static final int META_ALT_ON = 2;
  public static final int META_ALT_RIGHT_ON = 32;
  public static final int META_SHIFT_LEFT_ON = 64;
  public static final int META_SHIFT_ON = 1;
  public static final int META_SHIFT_RIGHT_ON = 128;
  public static final int META_SYM_ON = 4;
  public static final int CONTROL_LEFT = 129;
  public static final int CONTROL_RIGHT = 130;
  public static final int ESCAPE = 111;
  public static final int END = 123;
  public static final int INSERT = 124;
  public static final int PAGE_UP = 92;
  public static final int PAGE_DOWN = 93;
  public static final int PICTSYMBOLS = 94;
  public static final int SWITCH_CHARSET = 95;
  public static final int NUMPAD_0 = 144;
  public static final int NUMPAD_1 = 145;
  public static final int NUMPAD_2 = 146;
  public static final int NUMPAD_3 = 147;
  public static final int NUMPAD_4 = 148;
  public static final int NUMPAD_5 = 149;
  public static final int NUMPAD_6 = 150;
  public static final int NUMPAD_7 = 151;
  public static final int NUMPAD_8 = 152;
  public static final int NUMPAD_9 = 153;
  public static final int NUMPAD_DIVIDE = 154;
  public static final int NUMPAD_MULTIPLY = 155;
  public static final int NUMPAD_SUBTRACT = 156;
  public static final int NUMPAD_ADD = 157;
  public static final int NUMPAD_DOT = 158;
  public static final int NUMPAD_COMMA = 159;
  public static final int NUMPAD_ENTER = 160;
  public static final int NUMPAD_EQUALS = 161;
  public static final int NUMPAD_LEFT_PAREN = 162;
  public static final int NUMPAD_RIGHT_PAREN = 163;
  public static final int NUM_LOCK = 143;
  public static final int WORLD_1 = 240;
  public static final int WORLD_2 = 241;
  public static final int COLON = 243;
  public static final int F1 = 131;
  public static final int F2 = 132;
  public static final int F3 = 133;
  public static final int F4 = 134;
  public static final int F5 = 135;
  public static final int F6 = 136;
  public static final int F7 = 137;
  public static final int F8 = 138;
  public static final int F9 = 139;
  public static final int F10 = 140;
  public static final int F11 = 141;
  public static final int F12 = 142;
  public static final int F13 = 183;
  public static final int F14 = 184;
  public static final int F15 = 185;
  public static final int F16 = 186;
  public static final int F17 = 187;
  public static final int F18 = 188;
  public static final int F19 = 189;
  public static final int F20 = 190;
  public static final int F21 = 191;
  public static final int F22 = 192;
  public static final int F23 = 193;
  public static final int F24 = 194;
  public static final int MAX_KEYCODE = 255;

  /** Code-to-name lookup, indexed by key code; {@code null} where no key uses that code. */
  private static final String[] NAMES = new String[MAX_KEYCODE + 1];

  /** Name-to-code lookup, including alias names that share a code. */
  private static final FlixelMap<String, Integer> CODES = new FlixelMap<>();

  static {
    register(UNKNOWN, "UNKNOWN");
    register(SOFT_LEFT, "SOFT_LEFT");
    register(SOFT_RIGHT, "SOFT_RIGHT");
    register(HOME, "HOME");
    register(BACK, "BACK");
    register(CALL, "CALL");
    register(ENDCALL, "ENDCALL");
    register(NUM_0, "NUM_0");
    register(NUM_1, "NUM_1");
    register(NUM_2, "NUM_2");
    register(NUM_3, "NUM_3");
    register(NUM_4, "NUM_4");
    register(NUM_5, "NUM_5");
    register(NUM_6, "NUM_6");
    register(NUM_7, "NUM_7");
    register(NUM_8, "NUM_8");
    register(NUM_9, "NUM_9");
    register(STAR, "STAR");
    register(POUND, "POUND");
    register(UP, "UP");
    register(DOWN, "DOWN");
    register(LEFT, "LEFT");
    register(RIGHT, "RIGHT");
    register(DPAD_CENTER, "DPAD_CENTER");
    register(VOLUME_UP, "VOLUME_UP");
    register(VOLUME_DOWN, "VOLUME_DOWN");
    register(POWER, "POWER");
    register(CAMERA, "CAMERA");
    register(CLEAR, "CLEAR");
    register(A, "A");
    register(B, "B");
    register(C, "C");
    register(D, "D");
    register(E, "E");
    register(F, "F");
    register(G, "G");
    register(H, "H");
    register(I, "I");
    register(J, "J");
    register(K, "K");
    register(L, "L");
    register(M, "M");
    register(N, "N");
    register(O, "O");
    register(P, "P");
    register(Q, "Q");
    register(R, "R");
    register(S, "S");
    register(T, "T");
    register(U, "U");
    register(V, "V");
    register(W, "W");
    register(X, "X");
    register(Y, "Y");
    register(Z, "Z");
    register(COMMA, "COMMA");
    register(PERIOD, "PERIOD");
    register(ALT_LEFT, "ALT_LEFT");
    register(ALT_RIGHT, "ALT_RIGHT");
    register(SHIFT_LEFT, "SHIFT_LEFT");
    register(SHIFT_RIGHT, "SHIFT_RIGHT");
    register(TAB, "TAB");
    register(SPACE, "SPACE");
    register(SYM, "SYM");
    register(EXPLORER, "EXPLORER");
    register(ENVELOPE, "ENVELOPE");
    register(ENTER, "ENTER");
    register(DEL, "DEL");
    register(GRAVE, "GRAVE");
    register(MINUS, "MINUS");
    register(EQUALS, "EQUALS");
    register(LEFT_BRACKET, "LEFT_BRACKET");
    register(RIGHT_BRACKET, "RIGHT_BRACKET");
    register(BACKSLASH, "BACKSLASH");
    register(SEMICOLON, "SEMICOLON");
    register(APOSTROPHE, "APOSTROPHE");
    register(SLASH, "SLASH");
    register(AT, "AT");
    register(NUM, "NUM");
    register(HEADSETHOOK, "HEADSETHOOK");
    register(FOCUS, "FOCUS");
    register(PLUS, "PLUS");
    register(MENU, "MENU");
    register(NOTIFICATION, "NOTIFICATION");
    register(SEARCH, "SEARCH");
    register(MEDIA_PLAY_PAUSE, "MEDIA_PLAY_PAUSE");
    register(MEDIA_STOP, "MEDIA_STOP");
    register(MEDIA_NEXT, "MEDIA_NEXT");
    register(MEDIA_PREVIOUS, "MEDIA_PREVIOUS");
    register(MEDIA_REWIND, "MEDIA_REWIND");
    register(MEDIA_FAST_FORWARD, "MEDIA_FAST_FORWARD");
    register(MUTE, "MUTE");
    register(PAGE_UP, "PAGE_UP");
    register(PAGE_DOWN, "PAGE_DOWN");
    register(PICTSYMBOLS, "PICTSYMBOLS");
    register(SWITCH_CHARSET, "SWITCH_CHARSET");
    register(ESCAPE, "ESCAPE");
    register(FORWARD_DEL, "FORWARD_DEL");
    register(CAPS_LOCK, "CAPS_LOCK");
    register(SCROLL_LOCK, "SCROLL_LOCK");
    register(PRINT_SCREEN, "PRINT_SCREEN");
    register(PAUSE, "PAUSE");
    register(END, "END");
    register(INSERT, "INSERT");
    register(CONTROL_LEFT, "CONTROL_LEFT");
    register(CONTROL_RIGHT, "CONTROL_RIGHT");
    register(F1, "F1");
    register(F2, "F2");
    register(F3, "F3");
    register(F4, "F4");
    register(F5, "F5");
    register(F6, "F6");
    register(F7, "F7");
    register(F8, "F8");
    register(F9, "F9");
    register(F10, "F10");
    register(F11, "F11");
    register(F12, "F12");
    register(NUM_LOCK, "NUM_LOCK");
    register(NUMPAD_0, "NUMPAD_0");
    register(NUMPAD_1, "NUMPAD_1");
    register(NUMPAD_2, "NUMPAD_2");
    register(NUMPAD_3, "NUMPAD_3");
    register(NUMPAD_4, "NUMPAD_4");
    register(NUMPAD_5, "NUMPAD_5");
    register(NUMPAD_6, "NUMPAD_6");
    register(NUMPAD_7, "NUMPAD_7");
    register(NUMPAD_8, "NUMPAD_8");
    register(NUMPAD_9, "NUMPAD_9");
    register(NUMPAD_DIVIDE, "NUMPAD_DIVIDE");
    register(NUMPAD_MULTIPLY, "NUMPAD_MULTIPLY");
    register(NUMPAD_SUBTRACT, "NUMPAD_SUBTRACT");
    register(NUMPAD_ADD, "NUMPAD_ADD");
    register(NUMPAD_DOT, "NUMPAD_DOT");
    register(NUMPAD_COMMA, "NUMPAD_COMMA");
    register(NUMPAD_ENTER, "NUMPAD_ENTER");
    register(NUMPAD_EQUALS, "NUMPAD_EQUALS");
    register(NUMPAD_LEFT_PAREN, "NUMPAD_LEFT_PAREN");
    register(NUMPAD_RIGHT_PAREN, "NUMPAD_RIGHT_PAREN");
    register(F13, "F13");
    register(F14, "F14");
    register(F15, "F15");
    register(F16, "F16");
    register(F17, "F17");
    register(F18, "F18");
    register(F19, "F19");
    register(F20, "F20");
    register(F21, "F21");
    register(F22, "F22");
    register(F23, "F23");
    register(F24, "F24");
    register(WORLD_1, "WORLD_1");
    register(WORLD_2, "WORLD_2");
    register(COLON, "COLON");

    // Aliases: alternate names that map to a code already registered above. These resolve through
    // fromString(...) but are never returned by toString(...), which keeps one canonical label per code.
    alias("BACKSPACE", DEL);
    alias("CENTER", DPAD_CENTER);
    alias("DPAD_UP", UP);
    alias("DPAD_DOWN", DOWN);
    alias("DPAD_LEFT", LEFT);
    alias("DPAD_RIGHT", RIGHT);
  }

  /**
   * Resolves a key name (as returned by {@link #toString(int)}) back to its key code. Recognizes
   * both canonical names and the documented aliases (for example {@code "BACKSPACE"} maps to the
   * same code as {@code "DEL"}).
   *
   * @param keyname Key name from {@link #toString(int)}; case-sensitive.
   * @return The key code, or {@link #NONE} if the name is unknown.
   */
  public static int fromString(String keyname) {
    if (keyname == null || keyname.equalsIgnoreCase("NONE")) {
      return NONE;
    }
    if (keyname.equalsIgnoreCase("ANY")) {
      return ANY;
    }
    Integer code = CODES.get(keyname);
    return code != null ? code : NONE;
  }

  /**
   * Returns a readable label for the given key code, suitable for a rebinding UI or debug overlay.
   *
   * @param keycode Key code from this class.
   * @return The key's canonical name, {@code "NONE"} for {@link #NONE}, {@code "ANY"} for
   *     {@link #ANY}, or {@code "UNKNOWN"} for a code no key uses.
   */
  public static String toString(int keycode) {
    if (keycode == NONE) {
      return "NONE";
    }
    if (keycode == ANY) {
      return "ANY";
    }
    if (keycode >= 0 && keycode <= MAX_KEYCODE && NAMES[keycode] != null) {
      return NAMES[keycode];
    }
    return "UNKNOWN";
  }

  private static void register(int keycode, String name) {
    NAMES[keycode] = name;
    CODES.put(name, keycode);
  }

  private static void alias(String name, int keycode) {
    CODES.put(name, keycode);
  }
}
