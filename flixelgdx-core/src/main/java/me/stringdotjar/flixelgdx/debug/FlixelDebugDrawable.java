package me.stringdotjar.flixelgdx.debug;

import me.stringdotjar.flixelgdx.FlixelObject;

/**
 * Interface for objects that can draw a debug bounding box in the
 * {@link me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay}. Any class that
 * implements this will automatically appear when visual debug drawing is enabled,
 * without being hard-coded into the overlay.
 *
 * <p>{@link FlixelObject} implements this by default, using collision state to
 * pick an appropriate color. Custom objects (including Box2D bodies) can implement
 * this interface to provide their own debug visualization.
 */
public interface FlixelDebugDrawable {

  /** X position of the bounding box in world space. */
  float getDebugX();

  /** Y position of the bounding box in world space. */
  float getDebugY();

  /** Width of the bounding box in world pixels. */
  float getDebugWidth();

  /** Height of the bounding box in world pixels. */
  float getDebugHeight();

  /**
   * Returns the RGBA color for this object's debug bounding box as a 4-element
   * array: {@code [r, g, b, a]}. Implementations should return a cached array
   * rather than allocating every frame.
   */
  float[] getDebugBoundingBoxColor();
}
