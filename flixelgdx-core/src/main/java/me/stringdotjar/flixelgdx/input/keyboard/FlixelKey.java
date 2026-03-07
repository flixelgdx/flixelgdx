package me.stringdotjar.flixelgdx.input.keyboard;

import com.badlogic.gdx.Input;

import me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepad;

/**
 * Keyboard key codes copied from libGDX {@link Input.Keys} (gamepad buttons are in {@link FlixelGamepad}).
 * Values match Input.Keys for backwards compatibility.
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

  /**
   * Resolves a key name (as returned by {@link Input.Keys#toString(int)}) to a key code.
   *
   * @param keyname Key name from {@link Input.Keys#toString(int)}.
   * @return The key code, or -1 if not found.
   */
  public static int fromString(String keyname) {
    return Input.Keys.valueOf(keyname);
  }

  /**
   * Returns a human-readable string for the given key code.
   *
   * @param keycode Key code from this class (same values as {@link Input.Keys}).
   * @return Human-readable key name, or null if unknown.
   */
  public static String toString(int keycode) {
    return Input.Keys.toString(keycode);
  }
}
