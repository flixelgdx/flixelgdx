package me.stringdotjar.flixelgdx.util;

/**
 * Utility class for various math related functions used in FlixelGDX.
 */
public final class FlixelMathUtil {

  /**
   * Rounds a float value to a specified number of decimal places.
   *
   * @param value The float value to round.
   * @param decimalPlaces The number of decimal places to round to.
   * @return The rounded float value.
   */
  public static float round(float value, int decimalPlaces) {
    float scale = (float) Math.pow(10, decimalPlaces);
    return Math.round(value * scale) / scale;
  }

  private FlixelMathUtil() {}
}
