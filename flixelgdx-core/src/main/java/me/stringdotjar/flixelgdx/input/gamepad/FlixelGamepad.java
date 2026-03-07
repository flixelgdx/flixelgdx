package me.stringdotjar.flixelgdx.input.gamepad;

import com.badlogic.gdx.Input;

import me.stringdotjar.flixelgdx.input.keyboard.FlixelKey;

/**
 * Gamepad button codes copied from libGDX {@link Input.Keys} BUTTON_* values for backwards compatibility.
 * Use for gamepad/controller button checks; use {@link FlixelKey} for keyboard.
 */
public final class FlixelGamepad {

  private FlixelGamepad() {}

  public static final int NONE = -2; // In HaxeFlixel, this is -1, but we use -2 to avoid confusion with ANY.
  public static final int ANY = -1;
  public static final int A = 96;
  public static final int B = 97;
  public static final int C = 98;
  public static final int X = 99;
  public static final int Y = 100;
  public static final int Z = 101;
  public static final int L1 = 102;
  public static final int R1 = 103;
  public static final int L2 = 104;
  public static final int R2 = 105;
  public static final int THUMBL = 106;
  public static final int THUMBR = 107;
  public static final int START = 108;
  public static final int SELECT = 109;
  public static final int MODE = 110;
  public static final int CIRCLE = 255;

  /**
   * Resolves a button name (as returned by {@link Input.Keys#toString(int)}) to a button code.
   *
   * @param name Button name from {@link Input.Keys#toString(int)}.
   * @return The button code, or -1 if not found.
   */
  public static int fromString(String name) {
    return Input.Keys.valueOf(name);
  }

  /**
   * Returns a human-readable string for the given gamepad button code.
   *
   * @param buttonCode Button code from this class (same values as Input.Keys BUTTON_*).
   * @return Human-readable button name, or null if unknown.
   */
  public static String toString(int buttonCode) {
    return Input.Keys.toString(buttonCode);
  }
}
