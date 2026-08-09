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

  /** Name-to-code lookup, including alias names that map to the same code as a canonical key. */
  private static final FlixelMap<String, Integer> CODES = new FlixelMap<>();

  static {
    NAMES[UNKNOWN] = "UNKNOWN";
    CODES.put("UNKNOWN", UNKNOWN);
    NAMES[SOFT_LEFT] = "SOFT_LEFT";
    CODES.put("SOFT_LEFT", SOFT_LEFT);
    NAMES[SOFT_RIGHT] = "SOFT_RIGHT";
    CODES.put("SOFT_RIGHT", SOFT_RIGHT);
    NAMES[HOME] = "HOME";
    CODES.put("HOME", HOME);
    NAMES[BACK] = "BACK";
    CODES.put("BACK", BACK);
    NAMES[CALL] = "CALL";
    CODES.put("CALL", CALL);
    NAMES[ENDCALL] = "ENDCALL";
    CODES.put("ENDCALL", ENDCALL);
    NAMES[NUM_0] = "NUM_0";
    CODES.put("NUM_0", NUM_0);
    NAMES[NUM_1] = "NUM_1";
    CODES.put("NUM_1", NUM_1);
    NAMES[NUM_2] = "NUM_2";
    CODES.put("NUM_2", NUM_2);
    NAMES[NUM_3] = "NUM_3";
    CODES.put("NUM_3", NUM_3);
    NAMES[NUM_4] = "NUM_4";
    CODES.put("NUM_4", NUM_4);
    NAMES[NUM_5] = "NUM_5";
    CODES.put("NUM_5", NUM_5);
    NAMES[NUM_6] = "NUM_6";
    CODES.put("NUM_6", NUM_6);
    NAMES[NUM_7] = "NUM_7";
    CODES.put("NUM_7", NUM_7);
    NAMES[NUM_8] = "NUM_8";
    CODES.put("NUM_8", NUM_8);
    NAMES[NUM_9] = "NUM_9";
    CODES.put("NUM_9", NUM_9);
    NAMES[STAR] = "STAR";
    CODES.put("STAR", STAR);
    NAMES[POUND] = "POUND";
    CODES.put("POUND", POUND);
    NAMES[UP] = "UP";
    CODES.put("UP", UP);
    NAMES[DOWN] = "DOWN";
    CODES.put("DOWN", DOWN);
    NAMES[LEFT] = "LEFT";
    CODES.put("LEFT", LEFT);
    NAMES[RIGHT] = "RIGHT";
    CODES.put("RIGHT", RIGHT);
    NAMES[DPAD_CENTER] = "DPAD_CENTER";
    CODES.put("DPAD_CENTER", DPAD_CENTER);
    NAMES[VOLUME_UP] = "VOLUME_UP";
    CODES.put("VOLUME_UP", VOLUME_UP);
    NAMES[VOLUME_DOWN] = "VOLUME_DOWN";
    CODES.put("VOLUME_DOWN", VOLUME_DOWN);
    NAMES[POWER] = "POWER";
    CODES.put("POWER", POWER);
    NAMES[CAMERA] = "CAMERA";
    CODES.put("CAMERA", CAMERA);
    NAMES[CLEAR] = "CLEAR";
    CODES.put("CLEAR", CLEAR);
    NAMES[A] = "A";
    CODES.put("A", A);
    NAMES[B] = "B";
    CODES.put("B", B);
    NAMES[C] = "C";
    CODES.put("C", C);
    NAMES[D] = "D";
    CODES.put("D", D);
    NAMES[E] = "E";
    CODES.put("E", E);
    NAMES[F] = "F";
    CODES.put("F", F);
    NAMES[G] = "G";
    CODES.put("G", G);
    NAMES[H] = "H";
    CODES.put("H", H);
    NAMES[I] = "I";
    CODES.put("I", I);
    NAMES[J] = "J";
    CODES.put("J", J);
    NAMES[K] = "K";
    CODES.put("K", K);
    NAMES[L] = "L";
    CODES.put("L", L);
    NAMES[M] = "M";
    CODES.put("M", M);
    NAMES[N] = "N";
    CODES.put("N", N);
    NAMES[O] = "O";
    CODES.put("O", O);
    NAMES[P] = "P";
    CODES.put("P", P);
    NAMES[Q] = "Q";
    CODES.put("Q", Q);
    NAMES[R] = "R";
    CODES.put("R", R);
    NAMES[S] = "S";
    CODES.put("S", S);
    NAMES[T] = "T";
    CODES.put("T", T);
    NAMES[U] = "U";
    CODES.put("U", U);
    NAMES[V] = "V";
    CODES.put("V", V);
    NAMES[W] = "W";
    CODES.put("W", W);
    NAMES[X] = "X";
    CODES.put("X", X);
    NAMES[Y] = "Y";
    CODES.put("Y", Y);
    NAMES[Z] = "Z";
    CODES.put("Z", Z);
    NAMES[COMMA] = "COMMA";
    CODES.put("COMMA", COMMA);
    NAMES[PERIOD] = "PERIOD";
    CODES.put("PERIOD", PERIOD);
    NAMES[ALT_LEFT] = "ALT_LEFT";
    CODES.put("ALT_LEFT", ALT_LEFT);
    NAMES[ALT_RIGHT] = "ALT_RIGHT";
    CODES.put("ALT_RIGHT", ALT_RIGHT);
    NAMES[SHIFT_LEFT] = "SHIFT_LEFT";
    CODES.put("SHIFT_LEFT", SHIFT_LEFT);
    NAMES[SHIFT_RIGHT] = "SHIFT_RIGHT";
    CODES.put("SHIFT_RIGHT", SHIFT_RIGHT);
    NAMES[TAB] = "TAB";
    CODES.put("TAB", TAB);
    NAMES[SPACE] = "SPACE";
    CODES.put("SPACE", SPACE);
    NAMES[SYM] = "SYM";
    CODES.put("SYM", SYM);
    NAMES[EXPLORER] = "EXPLORER";
    CODES.put("EXPLORER", EXPLORER);
    NAMES[ENVELOPE] = "ENVELOPE";
    CODES.put("ENVELOPE", ENVELOPE);
    NAMES[ENTER] = "ENTER";
    CODES.put("ENTER", ENTER);
    NAMES[DEL] = "DEL";
    CODES.put("DEL", DEL);
    NAMES[GRAVE] = "GRAVE";
    CODES.put("GRAVE", GRAVE);
    NAMES[MINUS] = "MINUS";
    CODES.put("MINUS", MINUS);
    NAMES[EQUALS] = "EQUALS";
    CODES.put("EQUALS", EQUALS);
    NAMES[LEFT_BRACKET] = "LEFT_BRACKET";
    CODES.put("LEFT_BRACKET", LEFT_BRACKET);
    NAMES[RIGHT_BRACKET] = "RIGHT_BRACKET";
    CODES.put("RIGHT_BRACKET", RIGHT_BRACKET);
    NAMES[BACKSLASH] = "BACKSLASH";
    CODES.put("BACKSLASH", BACKSLASH);
    NAMES[SEMICOLON] = "SEMICOLON";
    CODES.put("SEMICOLON", SEMICOLON);
    NAMES[APOSTROPHE] = "APOSTROPHE";
    CODES.put("APOSTROPHE", APOSTROPHE);
    NAMES[SLASH] = "SLASH";
    CODES.put("SLASH", SLASH);
    NAMES[AT] = "AT";
    CODES.put("AT", AT);
    NAMES[NUM] = "NUM";
    CODES.put("NUM", NUM);
    NAMES[HEADSETHOOK] = "HEADSETHOOK";
    CODES.put("HEADSETHOOK", HEADSETHOOK);
    NAMES[FOCUS] = "FOCUS";
    CODES.put("FOCUS", FOCUS);
    NAMES[PLUS] = "PLUS";
    CODES.put("PLUS", PLUS);
    NAMES[MENU] = "MENU";
    CODES.put("MENU", MENU);
    NAMES[NOTIFICATION] = "NOTIFICATION";
    CODES.put("NOTIFICATION", NOTIFICATION);
    NAMES[SEARCH] = "SEARCH";
    CODES.put("SEARCH", SEARCH);
    NAMES[MEDIA_PLAY_PAUSE] = "MEDIA_PLAY_PAUSE";
    CODES.put("MEDIA_PLAY_PAUSE", MEDIA_PLAY_PAUSE);
    NAMES[MEDIA_STOP] = "MEDIA_STOP";
    CODES.put("MEDIA_STOP", MEDIA_STOP);
    NAMES[MEDIA_NEXT] = "MEDIA_NEXT";
    CODES.put("MEDIA_NEXT", MEDIA_NEXT);
    NAMES[MEDIA_PREVIOUS] = "MEDIA_PREVIOUS";
    CODES.put("MEDIA_PREVIOUS", MEDIA_PREVIOUS);
    NAMES[MEDIA_REWIND] = "MEDIA_REWIND";
    CODES.put("MEDIA_REWIND", MEDIA_REWIND);
    NAMES[MEDIA_FAST_FORWARD] = "MEDIA_FAST_FORWARD";
    CODES.put("MEDIA_FAST_FORWARD", MEDIA_FAST_FORWARD);
    NAMES[MUTE] = "MUTE";
    CODES.put("MUTE", MUTE);
    NAMES[PAGE_UP] = "PAGE_UP";
    CODES.put("PAGE_UP", PAGE_UP);
    NAMES[PAGE_DOWN] = "PAGE_DOWN";
    CODES.put("PAGE_DOWN", PAGE_DOWN);
    NAMES[PICTSYMBOLS] = "PICTSYMBOLS";
    CODES.put("PICTSYMBOLS", PICTSYMBOLS);
    NAMES[SWITCH_CHARSET] = "SWITCH_CHARSET";
    CODES.put("SWITCH_CHARSET", SWITCH_CHARSET);
    NAMES[ESCAPE] = "ESCAPE";
    CODES.put("ESCAPE", ESCAPE);
    NAMES[FORWARD_DEL] = "FORWARD_DEL";
    CODES.put("FORWARD_DEL", FORWARD_DEL);
    NAMES[CAPS_LOCK] = "CAPS_LOCK";
    CODES.put("CAPS_LOCK", CAPS_LOCK);
    NAMES[SCROLL_LOCK] = "SCROLL_LOCK";
    CODES.put("SCROLL_LOCK", SCROLL_LOCK);
    NAMES[PRINT_SCREEN] = "PRINT_SCREEN";
    CODES.put("PRINT_SCREEN", PRINT_SCREEN);
    NAMES[PAUSE] = "PAUSE";
    CODES.put("PAUSE", PAUSE);
    NAMES[END] = "END";
    CODES.put("END", END);
    NAMES[INSERT] = "INSERT";
    CODES.put("INSERT", INSERT);
    NAMES[CONTROL_LEFT] = "CONTROL_LEFT";
    CODES.put("CONTROL_LEFT", CONTROL_LEFT);
    NAMES[CONTROL_RIGHT] = "CONTROL_RIGHT";
    CODES.put("CONTROL_RIGHT", CONTROL_RIGHT);
    NAMES[F1] = "F1";
    CODES.put("F1", F1);
    NAMES[F2] = "F2";
    CODES.put("F2", F2);
    NAMES[F3] = "F3";
    CODES.put("F3", F3);
    NAMES[F4] = "F4";
    CODES.put("F4", F4);
    NAMES[F5] = "F5";
    CODES.put("F5", F5);
    NAMES[F6] = "F6";
    CODES.put("F6", F6);
    NAMES[F7] = "F7";
    CODES.put("F7", F7);
    NAMES[F8] = "F8";
    CODES.put("F8", F8);
    NAMES[F9] = "F9";
    CODES.put("F9", F9);
    NAMES[F10] = "F10";
    CODES.put("F10", F10);
    NAMES[F11] = "F11";
    CODES.put("F11", F11);
    NAMES[F12] = "F12";
    CODES.put("F12", F12);
    NAMES[NUM_LOCK] = "NUM_LOCK";
    CODES.put("NUM_LOCK", NUM_LOCK);
    NAMES[NUMPAD_0] = "NUMPAD_0";
    CODES.put("NUMPAD_0", NUMPAD_0);
    NAMES[NUMPAD_1] = "NUMPAD_1";
    CODES.put("NUMPAD_1", NUMPAD_1);
    NAMES[NUMPAD_2] = "NUMPAD_2";
    CODES.put("NUMPAD_2", NUMPAD_2);
    NAMES[NUMPAD_3] = "NUMPAD_3";
    CODES.put("NUMPAD_3", NUMPAD_3);
    NAMES[NUMPAD_4] = "NUMPAD_4";
    CODES.put("NUMPAD_4", NUMPAD_4);
    NAMES[NUMPAD_5] = "NUMPAD_5";
    CODES.put("NUMPAD_5", NUMPAD_5);
    NAMES[NUMPAD_6] = "NUMPAD_6";
    CODES.put("NUMPAD_6", NUMPAD_6);
    NAMES[NUMPAD_7] = "NUMPAD_7";
    CODES.put("NUMPAD_7", NUMPAD_7);
    NAMES[NUMPAD_8] = "NUMPAD_8";
    CODES.put("NUMPAD_8", NUMPAD_8);
    NAMES[NUMPAD_9] = "NUMPAD_9";
    CODES.put("NUMPAD_9", NUMPAD_9);
    NAMES[NUMPAD_DIVIDE] = "NUMPAD_DIVIDE";
    CODES.put("NUMPAD_DIVIDE", NUMPAD_DIVIDE);
    NAMES[NUMPAD_MULTIPLY] = "NUMPAD_MULTIPLY";
    CODES.put("NUMPAD_MULTIPLY", NUMPAD_MULTIPLY);
    NAMES[NUMPAD_SUBTRACT] = "NUMPAD_SUBTRACT";
    CODES.put("NUMPAD_SUBTRACT", NUMPAD_SUBTRACT);
    NAMES[NUMPAD_ADD] = "NUMPAD_ADD";
    CODES.put("NUMPAD_ADD", NUMPAD_ADD);
    NAMES[NUMPAD_DOT] = "NUMPAD_DOT";
    CODES.put("NUMPAD_DOT", NUMPAD_DOT);
    NAMES[NUMPAD_COMMA] = "NUMPAD_COMMA";
    CODES.put("NUMPAD_COMMA", NUMPAD_COMMA);
    NAMES[NUMPAD_ENTER] = "NUMPAD_ENTER";
    CODES.put("NUMPAD_ENTER", NUMPAD_ENTER);
    NAMES[NUMPAD_EQUALS] = "NUMPAD_EQUALS";
    CODES.put("NUMPAD_EQUALS", NUMPAD_EQUALS);
    NAMES[NUMPAD_LEFT_PAREN] = "NUMPAD_LEFT_PAREN";
    CODES.put("NUMPAD_LEFT_PAREN", NUMPAD_LEFT_PAREN);
    NAMES[NUMPAD_RIGHT_PAREN] = "NUMPAD_RIGHT_PAREN";
    CODES.put("NUMPAD_RIGHT_PAREN", NUMPAD_RIGHT_PAREN);
    NAMES[F13] = "F13";
    CODES.put("F13", F13);
    NAMES[F14] = "F14";
    CODES.put("F14", F14);
    NAMES[F15] = "F15";
    CODES.put("F15", F15);
    NAMES[F16] = "F16";
    CODES.put("F16", F16);
    NAMES[F17] = "F17";
    CODES.put("F17", F17);
    NAMES[F18] = "F18";
    CODES.put("F18", F18);
    NAMES[F19] = "F19";
    CODES.put("F19", F19);
    NAMES[F20] = "F20";
    CODES.put("F20", F20);
    NAMES[F21] = "F21";
    CODES.put("F21", F21);
    NAMES[F22] = "F22";
    CODES.put("F22", F22);
    NAMES[F23] = "F23";
    CODES.put("F23", F23);
    NAMES[F24] = "F24";
    CODES.put("F24", F24);
    NAMES[WORLD_1] = "WORLD_1";
    CODES.put("WORLD_1", WORLD_1);
    NAMES[WORLD_2] = "WORLD_2";
    CODES.put("WORLD_2", WORLD_2);
    NAMES[COLON] = "COLON";

    CODES.put("COLON", COLON);
    CODES.put("BACKSPACE", DEL);
    CODES.put("CENTER", DPAD_CENTER);
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
}
