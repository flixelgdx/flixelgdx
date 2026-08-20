/**
 * Keyboard input for FlixelGDX: key codes and a stateful manager with multi-key helpers.
 *
 * <h2>Key types</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.keyboard.FlixelKeyInputManager FlixelKeyInputManager} - the
 *       manager, accessible via {@link org.flixelgdx.Flixel#keys Flixel.keys}.</li>
 *   <li>{@link org.flixelgdx.input.keyboard.FlixelKey FlixelKey} - integer constants for every
 *       key, plus utility methods.</li>
 * </ul>
 *
 * <h2>Reading keyboard input</h2>
 *
 * <p>Most game code only needs three methods:
 *
 * <pre>{@code
 * // Held this frame (true every frame the key is down).
 * if (Flixel.keys.pressed(FlixelKey.LEFT)) {
 *   player.setVelocityX(200f);
 * }
 *
 * // Edge: true only on the first frame the key goes down.
 * if (Flixel.keys.justPressed(FlixelKey.SPACE)) {
 *   player.jump();
 * }
 *
 * // Edge: true only on the first frame the key comes back up.
 * if (Flixel.keys.justReleased(FlixelKey.SHIFT_LEFT)) {
 *   sprint.stop();
 * }
 * }</pre>
 *
 * <p>Use {@link org.flixelgdx.input.keyboard.FlixelKey#ANY FlixelKey.ANY} in any of the above
 * to match <em>any</em> key:
 *
 * <pre>{@code
 * if (Flixel.keys.justPressed(FlixelKey.ANY)) {
 *   dismissTitleScreen();
 * }
 * }</pre>
 *
 * <h2>Checking multiple keys at once</h2>
 *
 * <p>{@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#anyPressed(int, int) anyPressed(...)}
 * and
 * {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#anyJustPressed(int, int) anyJustPressed(...)}
 * accept up to six keys and return true when at least one of them matches. This avoids writing a
 * long chain of {@code ||} conditions:
 *
 * <pre>{@code
 * // Accept either A/D or arrow keys for horizontal movement.
 * boolean movingRight = Flixel.keys.anyPressed(FlixelKey.D, FlixelKey.RIGHT);
 * boolean movingLeft  = Flixel.keys.anyPressed(FlixelKey.A, FlixelKey.LEFT);
 * }</pre>
 *
 * <h2>Finding which key was pressed</h2>
 *
 * <p>{@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#firstPressed() firstPressed()}
 * returns the key that has been held the longest among all currently pressed keys.
 * {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#firstJustPressed() firstJustPressed()}
 * returns the first key that transitioned to pressed this frame. Both return
 * {@link org.flixelgdx.input.keyboard.FlixelKey#NONE FlixelKey.NONE} when no matching key exists:
 *
 * <pre>{@code
 * int key = Flixel.keys.firstJustPressed();
 * if (key != FlixelKey.NONE) {
 *   rebindMap.put("jump", key);
 * }
 * }</pre>
 *
 * <h2>How state is maintained</h2>
 *
 * <p>{@link org.flixelgdx.input.keyboard.FlixelKeyInputManager FlixelKeyInputManager} implements
 * {@link org.flixelgdx.input.FlixelKeyboardListener FlixelKeyboardListener} directly and is
 * registered with the active
 * {@link org.flixelgdx.input.FlixelInputDevice FlixelInputDevice} at startup.
 * Key state is updated entirely through {@code keyDown} and {@code keyUp} event callbacks.
 * The manager deliberately does <em>not</em> rebuild its set by polling {@code Flixel.input}
 * each frame, because some backends (notably the web backend) do not expose reliable per-frame
 * polling for every key, which would silently break {@code justPressed} and {@code justReleased}
 * there. The listener is the only writer of internal key state.
 *
 * <h2>Debug UI suppression</h2>
 *
 * <p>When the active {@link org.flixelgdx.debug.FlixelDebugOverlay FlixelDebugOverlay} reports
 * that a debug UI element has keyboard focus,
 * {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#pressed(int) pressed()},
 * {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#justPressed(int) justPressed()},
 * and
 * {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager#justReleased(int) justReleased()}
 * all return {@code false} automatically. This prevents debug-console typing from leaking into
 * game controls.
 *
 * <h2>FlixelKey reference</h2>
 *
 * <p>{@link org.flixelgdx.input.keyboard.FlixelKey FlixelKey} holds integer constants for every
 * supported key (for example {@link org.flixelgdx.input.keyboard.FlixelKey#A FlixelKey.A},
 * {@link org.flixelgdx.input.keyboard.FlixelKey#SPACE FlixelKey.SPACE},
 * {@link org.flixelgdx.input.keyboard.FlixelKey#F1 FlixelKey.F1}). Two special sentinels exist:
 * {@link org.flixelgdx.input.keyboard.FlixelKey#ANY FlixelKey.ANY} ({@code -1}) matches any key
 * in the query methods, and
 * {@link org.flixelgdx.input.keyboard.FlixelKey#NONE FlixelKey.NONE} ({@code -2}) is returned
 * when no key is pressed or a query has no result. Use
 * {@link org.flixelgdx.input.keyboard.FlixelKey#toString(int) FlixelKey.toString(int)} to get a
 * readable label for display in a rebinding UI, and
 * {@link org.flixelgdx.input.keyboard.FlixelKey#fromString(String) FlixelKey.fromString(String)}
 * to read it back.
 */
package org.flixelgdx.input.keyboard;
