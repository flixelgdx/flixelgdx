/**
 * Debug overlay, watch panel, and command console for FlixelGDX.
 *
 * <p>Everything is accessed through {@link org.flixelgdx.Flixel#debug Flixel.debug}, which is
 * the {@link org.flixelgdx.debug.FlixelDebugManager FlixelDebugManager} created automatically
 * when {@link org.flixelgdx.Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner) Flixel.start(...)}
 * runs. The overlay is only shown in debug or test runtime modes; in release builds it is
 * replaced by a no-op so calls cost nothing in shipping code.
 *
 * <h2>Toggling the overlay</h2>
 * <pre>{@code
 * Flixel.debug.toggleVisible();    // Show or hide the overlay panel.
 * Flixel.debug.setDrawDebug(true); // Show hitbox outlines on all FlixelObjects.
 * }</pre>
 *
 * <h2>Watching values</h2>
 * <p>The watch panel shows live key-value pairs you register from game code. Values are
 * re-read every frame, so they always reflect the current state:
 *
 * <pre>{@code
 * Flixel.watch.add("FPS", Flixel.graphics::getFps);
 * Flixel.watch.add("Player X", player::getX);
 * }</pre>
 *
 * <h2>Custom console commands</h2>
 * <p>Register commands through the manager and execute them from the overlay console or from
 * code:
 *
 * <pre>{@code
 * Flixel.debug.registerCommand("god", args -> {
 *   player.invincible = !player.invincible;
 *   Flixel.info("God mode: " + player.invincible);
 * });
 *
 * // Trigger programmatically:
 * Flixel.debug.executeCommand("god");
 * }</pre>
 *
 * <h2>Overlay key binding</h2>
 * <p>The toggle key can be changed on the overlay object itself:
 *
 * <pre>{@code
 * Flixel.debug.overlay.toggleKey = FlixelKey.BACKSLASH;
 * }</pre>
 *
 * @see org.flixelgdx.debug.FlixelDebugManager
 * @see org.flixelgdx.debug.FlixelDebugOverlay
 * @see org.flixelgdx.debug.FlixelDebugWatchManager
 */
package org.flixelgdx.debug;
