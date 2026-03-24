package me.stringdotjar.flixelgdx.util;

import me.stringdotjar.flixelgdx.input.keyboard.FlixelKey;

// TODO: Move all constants into their respective classes instead of having this class.

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

    /** Minimum allowed elapsed time (seconds) per frame to avoid zero-delta issues. */
    public static final float MIN_ELAPSED = 0.000001f;

    /** Maximum allowed elapsed time (seconds) per frame to cap extreme lag spikes. */
    public static final float MAX_ELAPSED = 0.1f;

    private Graphics() {}
  }

  /**
   * Direction flags used for collision detection and touch sensing, matching
   * <a href="https://api.haxeflixel.com/flixel/util/FlxDirectionFlags.html">FlxDirectionFlags</a>.
   * The same bit patterns are reused for facing directions in {@link Graphics}.
   */
  public static final class Physics {

    public static final int NONE = 0x0000;
    public static final int LEFT = 0x0001;
    public static final int RIGHT = 0x0010;
    public static final int UP = 0x0100;
    public static final int DOWN = 0x1000;
    public static final int ANY = LEFT | RIGHT | UP | DOWN;

    public static final int FLOOR = DOWN;
    public static final int CEILING = UP;
    public static final int WALL = LEFT | RIGHT;

    /**
     * Maximum number of pixels two objects can intersect before separation gives up.
     *
     * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#SEPARATE_BIAS">FlxObject.SEPARATE_BIAS</a>
     */
    public static final float SEPARATE_BIAS = 4f;

    /** Default pixels-per-meter ratio used by Box2D helpers. */
    public static final float PIXELS_PER_METER = 100f;

    /** Box2D velocity solver iterations per step. */
    public static final int VELOCITY_ITERATIONS = 6;

    /** Box2D position solver iterations per step. */
    public static final int POSITION_ITERATIONS = 2;

    private Physics() {}
  }

  /**
   * Constants used by the debug overlay and diagnostic systems.
   */
  public static final class Debug {

    /** Default key used to toggle the debug overlay visibility. */
    public static final int DEFAULT_TOGGLE_KEY = FlixelKey.F2;

    /** Default key used to toggle visual debug (bounding boxes) on/off. */
    public static final int DEFAULT_DRAW_DEBUG_KEY = FlixelKey.F3;

    /** The maximum number of log entries the debug console keeps in its buffer. */
    public static final int MAX_LOG_ENTRIES = 200;

    /** Stats update interval in seconds (how often FPS/memory stats refresh). */
    public static final float STATS_UPDATE_INTERVAL = 0.5f;

    private Debug() {}
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
