package me.stringdotjar.flixelgdx.util;

/**
 * Core class for holding static classes with values which do not change. It is NOT RECOMMENDED to
 * store things like {@link java.util.ArrayList}'s, as they can still be modified even if they are
 * initialized as {@code final}.
 */
public final class FlixelConstants {

  /**
   * Values for graphics throughout the game.
   */
  public static final class Graphics {

    // For sprite facing directions.
    public static final int FACING_LEFT = 0x0001;
    public static final int FACING_RIGHT = 0x0010;
    public static final int FACING_UP = 0x0100;
    public static final int FACING_DOWN = 0x1000;

    private Graphics() {}
  }

  /**
   * ASCII color code constants for text in the console.
   */
  public static final class AsciiCodes {

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\033[0;1m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private AsciiCodes() {}
  }

  private FlixelConstants() {}
}
