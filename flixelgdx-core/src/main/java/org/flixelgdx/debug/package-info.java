/**
 * Debugger panels and commands for FlixelGDX.
 *
 * <p>The debugger is a set of <strong>panels</strong>. Each panel (a
 * {@link org.flixelgdx.debug.FlixelDebugPanel FlixelDebugPanel}) samples one kind of data from the
 * running game and encodes it for the remote debugger app, which connects over a WebSocket and works the
 * same on every platform. See {@code DEBUG_PROTOCOL.md} at the repository root for the wire format.
 *
 * <p>Everything is accessed through {@link org.flixelgdx.Flixel#debug Flixel.debug}, which is
 * the {@link org.flixelgdx.debug.FlixelDebugManager FlixelDebugManager} created automatically
 * when {@link org.flixelgdx.Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner) Flixel.start(...)}
 * runs. Panels are only updated in debug runtime mode, so calls cost nothing in shipping code.
 *
 * <h2>Pausing, stepping, and hitboxes</h2>
 * <pre>{@code
 * Flixel.debug.controls.setPaused(true);   // Freeze the game. F3 does the same.
 * Flixel.debug.controls.stepFrame();       // Run exactly one frame while paused.
 * Flixel.debug.controls.setHitboxes(true); // Outline every object. F2 does the same.
 * }</pre>
 *
 * <h2>Watching values</h2>
 * <p>The watch panel shows live key-value pairs you register from game code. Values are
 * read each time the app asks, so they always reflect the current state, and the app receives their
 * real types:
 *
 * <pre>{@code
 * Flixel.debug.watches.addInt("FPS", Flixel.graphics::getFps);
 * Flixel.debug.watches.addFloat("Player X", player::getX);
 * Flixel.debug.watches.add("State", () -> machine.getName());
 * }</pre>
 *
 * <h2>Custom console commands</h2>
 * <p>Register commands through the console panel and run them from the app or from code:
 *
 * <pre>{@code
 * Flixel.debug.console.register("god", "god [on|off]", "Makes the player invincible.", args -> {
 *   player.invincible = args.getBoolean(0, !player.invincible);
 *   Flixel.info("God mode: " + player.invincible);
 * });
 *
 * // Trigger programmatically:
 * Flixel.debug.console.execute("god");
 * }</pre>
 *
 * <h2>Inspecting objects</h2>
 * <p>The inspector panel shows the properties of one selected object. It shows one section for every registered
 * class in the object's family tree, most specific class first, so a {@code Player} that extends
 * {@code FlixelSprite} shows its own section and then the sprite, object, and basic sections. The framework
 * registers sections for its own classes. Add one for yours:
 *
 * <pre>{@code
 * Flixel.debug.inspector.register(Player.class, "Player", b -> b
 *     .intProp("Health", Player::getHealth, Player::setHealth)  // With a setter, the row is editable.
 *     .floatProp("Speed", Player::getSpeed)                     // Without one, it is read-only.
 *     .boolProp("On ground", Player::isOnGround)
 *     .enumProp("State", Player::getState)
 *     .intProp("Id", Player::getId).once());                    // Read once when the player is selected.
 *
 * Flixel.debug.inspector.select(player);   // Or click it in the app.
 * }</pre>
 *
 * <p>There is one builder method per value type ({@code boolProp}, {@code intProp}, {@code longProp},
 * {@code floatProp}, {@code doubleProp}, {@code stringProp}), so a lambda is never ambiguous. Reading values
 * never allocates, and a getter that throws shows its message in the app instead of a value. See
 * {@link org.flixelgdx.debug.inspector.FlixelSectionBuilder FlixelSectionBuilder} for all the options.
 *
 * <h2>Custom panels</h2>
 * <p>For a panel that is not about one object, use a
 * {@link org.flixelgdx.debug.FlixelDebugCustomPanel FlixelDebugCustomPanel}. Describe its rows and buttons with
 * the same style of builder, except the getters take no arguments:
 *
 * <pre>{@code
 * Flixel.debug.register(new FlixelDebugCustomPanel("ai", "Enemy AI", p -> p
 *     .intProp("Active enemies", () -> enemies.countLiving())
 *     .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)
 *     .action("Kill all", enemies::killAll)));
 * }</pre>
 *
 * <p>When you need a panel with a completely different payload, extend
 * {@link org.flixelgdx.debug.FlixelDebugPanelBase FlixelDebugPanelBase} and register it with
 * {@link org.flixelgdx.debug.FlixelDebugManager#register(org.flixelgdx.debug.FlixelDebugPanel)}.
 *
 * @see org.flixelgdx.debug.FlixelDebugManager
 * @see org.flixelgdx.debug.FlixelDebugPanel
 */
package org.flixelgdx.debug;
