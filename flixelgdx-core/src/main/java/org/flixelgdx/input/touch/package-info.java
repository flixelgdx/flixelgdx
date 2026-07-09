/**
 * Multitouch input for mobile and touchscreen platforms.
 *
 * <p>The central class is {@link org.flixelgdx.input.touch.FlixelTouchManager FlixelTouchManager},
 * accessible via {@link org.flixelgdx.Flixel#touches Flixel.touches}. It tracks up to
 * {@link org.flixelgdx.input.touch.FlixelTouchManager#DEFAULT_MAX_POINTERS DEFAULT_MAX_POINTERS}
 * simultaneous fingers in a pre-allocated
 * {@link org.flixelgdx.input.touch.FlixelTouch FlixelTouch} array.
 *
 * <p>Each {@link org.flixelgdx.input.touch.FlixelTouch FlixelTouch} carries both screen-space
 * coordinates (top-left origin, pixels) and world-space coordinates (bottom-left origin, game
 * units), unprojected via the manager's active camera.
 */
package org.flixelgdx.input.touch;
