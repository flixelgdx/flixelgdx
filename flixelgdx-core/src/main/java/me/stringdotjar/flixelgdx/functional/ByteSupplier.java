package me.stringdotjar.flixelgdx.functional;

/**
 * Represents a supplier of {@code byte}-valued results.
 */
@FunctionalInterface
public interface ByteSupplier {

  /**
   * Gets a byte result.
   *
   * @return a result.
   */
  byte getAsByte();
}

