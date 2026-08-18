/**
 * Multitouch input for mobile and touchscreen platforms, tracking up to ten simultaneous fingers
 * with pre-allocated, zero-garbage state objects.
 *
 * <h2>Key types</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.touch.FlixelTouchManager FlixelTouchManager} - the manager,
 *       accessible via {@link org.flixelgdx.Flixel#touches Flixel.touches}.</li>
 *   <li>{@link org.flixelgdx.input.touch.FlixelTouch FlixelTouch} - a snapshot of one finger's
 *       position and edge state for the current frame. Instances are pre-allocated by the manager;
 *       do not create them yourself.</li>
 * </ul>
 *
 * <h2>Enabling touch input</h2>
 *
 * <p>{@link org.flixelgdx.input.touch.FlixelTouchManager#enabled FlixelTouchManager.enabled}
 * starts as {@code false}. Mobile backend launchers enable it automatically when a game is
 * compiled for Android or iOS. On desktop or web, set it manually if your game supports a
 * touchscreen monitor:
 *
 * <pre>{@code
 * Flixel.touches.enabled = true;
 * }</pre>
 *
 * <h2>Reading per-finger state</h2>
 *
 * <p>The manager tracks fingers in the pre-allocated
 * {@link org.flixelgdx.input.touch.FlixelTouchManager#list list} array. Slot {@code list[0]}
 * always corresponds to pointer index 0 (the first finger to touch the screen in the current
 * gesture), slot {@code list[1]} to index 1, and so on. Reading the public fields directly is the
 * fastest way to access state:
 *
 * <pre>{@code
 * FlixelTouch first = Flixel.touches.list[0];
 *
 * if (first.justPressed()) {
 *   spawnEffect(first.worldX, first.worldY);
 * }
 * if (first.dragging()) {
 *   moveCamera(first.worldX - prevX, first.worldY - prevY);
 * }
 * if (first.justReleased()) {
 *   finishGesture();
 * }
 * }</pre>
 *
 * <h2>Checking any finger</h2>
 *
 * <p>When you do not need to know which finger triggered an event, the manager's convenience
 * methods scan all active pointers at once:
 *
 * <pre>{@code
 * if (Flixel.touches.anyJustPressed()) {
 *   Flixel.haptics.vibrate(30);
 * }
 * if (Flixel.touches.anyPressed()) {
 *   activateTouchEffect();
 * }
 * int count = Flixel.touches.count(); // How many fingers are down right now.
 * }</pre>
 *
 * <h2>Rectangle hit tests</h2>
 *
 * <p>A common pattern is detecting whether a finger is inside a UI button or game area. The
 * manager provides both screen-space and world-space variants for "currently inside" and "just
 * touched":
 *
 * <pre>{@code
 * // True every frame at least one finger is inside the button in world space.
 * if (Flixel.touches.touchingWorld(button.getX(), button.getY(),
 *         button.getWidth(), button.getHeight())) {
 *   button.setHighlighted(true);
 * }
 *
 * // True only on the single frame a finger first lands inside the button.
 * if (Flixel.touches.justTouchedWorld(button.getX(), button.getY(),
 *         button.getWidth(), button.getHeight())) {
 *   onButtonTapped();
 * }
 *
 * // Screen-space equivalents (top-left origin, Y increases down).
 * if (Flixel.touches.touchingScreen(0, 0, half, Flixel.window.getHeight())) {
 *   moveRight();
 * }
 * }</pre>
 *
 * <h2>Coordinate systems</h2>
 *
 * <p>Each {@link org.flixelgdx.input.touch.FlixelTouch FlixelTouch} carries two sets of
 * coordinates.
 *
 * <p><b>Screen coordinates</b>
 * ({@link org.flixelgdx.input.touch.FlixelTouch#screenX FlixelTouch.screenX},
 * {@link org.flixelgdx.input.touch.FlixelTouch#screenY FlixelTouch.screenY}) are in pixels with
 * the origin at the top-left corner: X increases right, Y increases down. These match raw window
 * coordinates.
 *
 * <p><b>World coordinates</b>
 * ({@link org.flixelgdx.input.touch.FlixelTouch#worldX FlixelTouch.worldX},
 * {@link org.flixelgdx.input.touch.FlixelTouch#worldY FlixelTouch.worldY}) are unprojected
 * through the active {@link org.flixelgdx.FlixelCamera FlixelCamera} and use the standard
 * FlixelGDX top-left origin: X increases right, Y increases down. These match the positions of
 * game objects in the scene. By default, the first camera in
 * {@link org.flixelgdx.Flixel#cameras Flixel.cameras} is used; call
 * {@link org.flixelgdx.input.touch.FlixelTouchManager#setWorldCamera(org.flixelgdx.FlixelCamera)
 * setWorldCamera(...)} to override.
 *
 * <h2>Touch cancellation</h2>
 *
 * <p>Some platforms cancel an active touch without delivering a release event, for example when an
 * incoming phone call interrupts a game session. The
 * {@link org.flixelgdx.input.touch.FlixelTouch#justCancelled() justCancelled()} flag is set for
 * one frame in those cases. Treat it like a release: clear any state associated with that pointer.
 *
 * <h2>Maximum pointer count</h2>
 *
 * <p>The default limit is
 * {@link org.flixelgdx.input.touch.FlixelTouchManager#DEFAULT_MAX_POINTERS DEFAULT_MAX_POINTERS}
 * ({@code 10}), which covers the maximum simultaneous touches supported by virtually all Android
 * hardware. Raise or lower it with
 * {@link org.flixelgdx.input.touch.FlixelTouchManager#setMaxPointers(int) setMaxPointers(int)};
 * existing live touch state is preserved for pointers within the new limit.
 */
package org.flixelgdx.input.touch;
