package me.stringdotjar.flixelgdx.functional;

/**
 * Represents a supplier of {@code float}-valued results.
 */
@FunctionalInterface
public interface FloatSupplier {

  /**
   * Gets a float result.
   *
   * @return a result.
   */
  float getAsFloat();
}

