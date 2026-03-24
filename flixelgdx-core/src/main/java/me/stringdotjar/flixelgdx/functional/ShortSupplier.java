package me.stringdotjar.flixelgdx.functional;

/**
 * Represents a supplier of {@code short}-valued results.
 */
@FunctionalInterface
public interface ShortSupplier {

  /**
   * Gets a short result.
   *
   * @return a result.
   */
  short getAsShort();
}

