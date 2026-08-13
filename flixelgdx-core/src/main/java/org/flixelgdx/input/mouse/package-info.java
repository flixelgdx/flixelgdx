/**
 * Mouse and pointer input for FlixelGDX: button queries, screen and world coordinates, scroll
 * deltas, and pluggable OS cursor styling.
 *
 * <h2>Key types</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.mouse.FlixelMouseInputManager FlixelMouseInputManager} - the
 *       main manager, on {@link org.flixelgdx.Flixel#mouse Flixel.mouse}.</li>
 *   <li>{@link org.flixelgdx.input.mouse.FlixelMouseButton FlixelMouseButton} - integer constants
 *       for the five standard mouse buttons.</li>
 *   <li>{@link org.flixelgdx.input.mouse.FlixelMouseIconManager FlixelMouseIconManager} - interface
 *       for changing the OS cursor at runtime, reachable via
 *       {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#icons Flixel.mouse.icons}.</li>
 *   <li>{@link org.flixelgdx.input.mouse.FlixelMouseCursor FlixelMouseCursor} - enumeration of
 *       preset system cursor shapes.</li>
 *   <li>{@link org.flixelgdx.input.mouse.FlixelNoopMouseIconManager FlixelNoopMouseIconManager} -
 *       the safe default installed before a backend provides its own implementation.</li>
 * </ul>
 *
 * <h2>Reading button state</h2>
 *
 * <pre>{@code
 * // Held this frame.
 * if (Flixel.mouse.pressed(FlixelMouseButton.LEFT)) {
 *   drag(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY());
 * }
 *
 * // Edge: true only on the first frame the button goes down.
 * if (Flixel.mouse.justPressed(FlixelMouseButton.RIGHT)) {
 *   openContextMenu(Flixel.mouse.getScreenX(), Flixel.mouse.getScreenY());
 * }
 *
 * // Edge: true only on the first frame the button comes back up.
 * if (Flixel.mouse.justReleased(FlixelMouseButton.LEFT)) {
 *   finishSelection();
 * }
 * }</pre>
 *
 * <p>The five button constants are
 * {@link org.flixelgdx.input.mouse.FlixelMouseButton#LEFT FlixelMouseButton.LEFT},
 * {@link org.flixelgdx.input.mouse.FlixelMouseButton#RIGHT FlixelMouseButton.RIGHT},
 * {@link org.flixelgdx.input.mouse.FlixelMouseButton#MIDDLE FlixelMouseButton.MIDDLE},
 * {@link org.flixelgdx.input.mouse.FlixelMouseButton#BACK FlixelMouseButton.BACK}, and
 * {@link org.flixelgdx.input.mouse.FlixelMouseButton#FORWARD FlixelMouseButton.FORWARD}.
 *
 * <h2>Coordinates: screen space versus world space</h2>
 *
 * <p>The manager tracks two coordinate systems for the current pointer position.
 *
 * <p><b>Screen space</b>
 * ({@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getScreenX() getScreenX()},
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getScreenY() getScreenY()}) is in
 * pixels with the origin at the top-left corner of the window: X increases right, Y increases
 * down. Use this for UI elements that are positioned in screen space.
 *
 * <p><b>World space</b>
 * ({@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getWorldX() getWorldX()},
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getWorldY() getWorldY()}) is the
 * result of unprojecting the screen position through the active
 * {@link org.flixelgdx.FlixelCamera FlixelCamera} and uses the standard FlixelGDX origin at the
 * bottom-left: X increases right, Y increases up. Use this to test whether the cursor is over a
 * game object.
 *
 * <p>By default, the first camera in {@link org.flixelgdx.Flixel#cameras Flixel.cameras} is used
 * for the world conversion. Call
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#setWorldCamera(org.flixelgdx.FlixelCamera)
 * setWorldCamera(...)} to override. Passing a camera directly to
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getWorldX(org.flixelgdx.FlixelCamera)
 * getWorldX(camera)} gives a one-off unproject without changing the stored camera.
 *
 * <p>For a quick "is the cursor overlapping this object?" check, use
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#overlap(org.flixelgdx.functional.FlixelPositional)
 * overlap(...)}:
 *
 * <pre>{@code
 * if (Flixel.mouse.overlap(button)) {
 *   button.setHighlighted(true);
 * }
 * }</pre>
 *
 * <h2>Scroll wheel and trackpad scroll</h2>
 *
 * <p>Scroll amounts are accumulated within each frame and exposed as deltas through
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getScrollDeltaX() getScrollDeltaX()}
 * (horizontal) and
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#getScrollDeltaY() getScrollDeltaY()}
 * (vertical). Both are cleared by
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#endFrame() endFrame()}.
 *
 * <p>Values are <em>deltas</em>, not fixed steps: magnitude varies by device and OS (notched
 * wheel, high-resolution wheel, trackpad). The sign indicates direction, but which sign means
 * "scroll up" versus "scroll down" can differ between backends and operating systems. Add an
 * option in your game to toggle scroll direction for your users.
 *
 * <pre>{@code
 * float scroll = Flixel.mouse.getScrollDeltaY();
 * if (scroll != 0f) {
 *   camera.zoom(1f + scroll * 0.1f);
 * }
 * }</pre>
 *
 * <h2>Debug UI suppression</h2>
 *
 * <p>When the active {@link org.flixelgdx.debug.FlixelDebugOverlay FlixelDebugOverlay} reports
 * that a UI panel is capturing mouse input,
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#pressed(int) pressed()},
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#justPressed(int) justPressed()}, and
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#justReleased(int) justReleased()}
 * return {@code false} automatically. This prevents clicks inside a debug panel from triggering
 * game behavior at the same time. Use the {@code raw} variants to opt out of this suppression:
 *
 * <pre>{@code
 * // Always responds, even when a debug panel is open.
 * if (Flixel.mouse.rawJustPressed(FlixelMouseButton.LEFT)) {
 *   debugTool.pick(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY());
 * }
 * }</pre>
 *
 * <h2>Cursor styling</h2>
 *
 * <p>Games that want to change the OS cursor at runtime use
 * {@link org.flixelgdx.input.mouse.FlixelMouseInputManager#icons Flixel.mouse.icons}, which holds
 * the active {@link org.flixelgdx.input.mouse.FlixelMouseIconManager FlixelMouseIconManager}. Each
 * backend installs its own implementation at startup. Before any backend is installed,
 * {@link org.flixelgdx.input.mouse.FlixelNoopMouseIconManager FlixelNoopMouseIconManager} is
 * active and silently ignores all calls.
 *
 * <pre>{@code
 * // Show a text-cursor when the user hovers over an input field.
 * Flixel.mouse.icons.setCursor(FlixelMouseCursor.IBEAM);
 *
 * // Restore the default arrow when the user leaves.
 * Flixel.mouse.icons.resetCursor();
 *
 * // Use a custom game-themed cursor (desktop only).
 * Flixel.mouse.icons.setCustomCursor(cursorGraphic, 0, 0);
 * }</pre>
 *
 * <p>{@link org.flixelgdx.input.mouse.FlixelMouseCursor FlixelMouseCursor} lists all available
 * preset shapes. Note that not all shapes are available on every platform, as some operating systems
 * differ with icon management and may not provide a full cursor set.
 */
package org.flixelgdx.input.mouse;
