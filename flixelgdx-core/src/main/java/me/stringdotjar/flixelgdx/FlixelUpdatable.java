package me.stringdotjar.flixelgdx;

/**
 * Interface for Flixel-based objects that can be updated.
 *
 * @see FlixelBasic
 */
public interface FlixelUpdatable {

  /**
   * Updates the object for the given elapsed time.
   *
   * @param elapsed The elapsed time since the last frame update.
   */
  void update(float elapsed);
}
