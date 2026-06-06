package org.flixelgdx.util;

/**
 * Bit flags for collision and touch sides.
 */
public final class FlixelDirectionFlags {

  // Facing flags.
  public static final int NONE = 0x0000;
  public static final int LEFT = 0x0001;
  public static final int RIGHT = 0x0010;
  public static final int UP = 0x0100;
  public static final int DOWN = 0x1000;
  public static final int ANY = LEFT | RIGHT | UP | DOWN;

  // Collision flags.
  public static final int FLOOR = DOWN;
  public static final int CEILING = UP;
  public static final int WALL = LEFT | RIGHT;

  private FlixelDirectionFlags() {}
}
