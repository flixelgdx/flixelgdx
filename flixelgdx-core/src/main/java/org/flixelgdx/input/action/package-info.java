/**
 * Rebindable logical input actions that sit above the raw device managers and support Steam Input.
 *
 * <h2>Why use actions instead of direct key checks</h2>
 *
 * <p>Writing {@code Flixel.keys.pressed(FlixelKey.SPACE)} everywhere works for a prototype, but it
 * becomes a problem the moment you add controller support, player-configurable bindings, or Steam
 * Input. Every hardcoded key reference has to be tracked down and updated.
 *
 * <p>An action inverts this relationship: game code asks whether an abstract control (for example
 * {@code jump}) is active, and the action figures out which physical inputs map to it. Changing a
 * binding only requires updating the action's configuration in one place. The gameplay code
 * ({@code if (jump.justPressed()) player.jump()}) stays unchanged.
 *
 * <h2>Key types</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.action.FlixelAction} - base class. Holds the action name, hold-repeat timing, an optional
 *       callback, and an {@code active} flag.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionDigital} - boolean action. True when any bound
 *       {@link org.flixelgdx.input.action.FlixelDigitalBinding} fires, or when Steam digital input matches the name.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionAnalog} - 2D vector action. Sums bound {@link org.flixelgdx.input.action.FlixelAnalogBinding}
 *       contributors plus optional Steam analog input, then clamps the result to a unit vector.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelDigitalBinding} - connects a key, mouse button, or gamepad button to a
 *       {@link org.flixelgdx.input.action.FlixelActionDigital}.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelAnalogBinding} - connects a key half or gamepad axis to one component of a
 *       {@link org.flixelgdx.input.action.FlixelActionAnalog}.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionSet} - groups actions together, drives their update/endFrame cycle,
 *       and holds the optional Steam reader.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionSets} - global registry; {@link org.flixelgdx.FlixelGame FlixelGame}
 *       calls {@link org.flixelgdx.input.action.FlixelActionSets#update(float) update()} and
 *       {@link org.flixelgdx.input.action.FlixelActionSets#endFrameAll() endFrameAll()} automatically each frame.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelSteamActionReader} - interface for reading Steam Input state each frame.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelSteamActionReaders} - static factory; {@link org.flixelgdx.input.action.FlixelSteamActionReaders#EMPTY}
 *       is the safe default when Steam is not active.</li>
 * </ul>
 *
 * <h2>Minimal setup</h2>
 *
 * <p>Subclass {@link org.flixelgdx.input.action.FlixelActionSet}, create actions in the constructor, add bindings, and call
 * {@link org.flixelgdx.input.action.FlixelActionSet#add(FlixelAction) add(...)} for each action. The set registers itself
 * with {@link org.flixelgdx.input.action.FlixelActionSets} automatically, so the game loop advances it every frame.
 *
 * <pre>{@code
 * public class PlayerControls extends FlixelActionSet {
 *
 *   public final FlixelActionDigital jump;
 *   public final FlixelActionAnalog  move;
 *
 *   public PlayerControls() {
 *     // Boolean action: Space or gamepad A triggers "jump".
 *     jump = new FlixelActionDigital("jump");
 *     jump.addBinding("kb",  FlixelDigitalBinding.key(FlixelKey.SPACE));
 *     jump.addBinding("pad", FlixelDigitalBinding.gamepadButton(0, FlixelGamepadButton.A));
 *     add(jump);
 *
 *     // 2D vector action: WASD/arrow keys or the left stick controls "move".
 *     move = new FlixelActionAnalog("move");
 *     move.addBinding("negX",   FlixelAnalogBinding.negXKey(FlixelKey.A));
 *     move.addBinding("posX",   FlixelAnalogBinding.posXKey(FlixelKey.D));
 *     move.addBinding("negY",   FlixelAnalogBinding.negYKey(FlixelKey.S));
 *     move.addBinding("posY",   FlixelAnalogBinding.posYKey(FlixelKey.W));
 *     move.addBinding("stickX", FlixelAnalogBinding.gamepadAxisX(0, FlixelGamepadAxis.LEFT_X));
 *     move.addBinding("stickY", FlixelAnalogBinding.gamepadAxisY(0, FlixelGamepadAxis.LEFT_Y));
 *     add(move);
 *   }
 * }
 * }</pre>
 *
 * <p>Then, in your game state:
 *
 * <pre>{@code
 * private PlayerControls controls;
 *
 * @Override
 * public void create() {
 *   controls = new PlayerControls();
 * }
 *
 * @Override
 * public void update(float elapsed) {
 *   super.update(elapsed);
 *   if (controls.jump.justPressed()) {
 *     player.jump();
 *   }
 *   player.setVelocityX(controls.move.getX() * 200f);
 *   player.setVelocityY(controls.move.getY() * 200f);
 * }
 *
 * @Override
 * public void destroy() {
 *   controls.destroy(); // Unregister from FlixelActionSets.
 *   super.destroy();
 * }
 * }</pre>
 *
 * <h2>Reading a digital action</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionDigital#pressed() pressed()} - true every frame the action is active.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionDigital#justPressed() justPressed()} - true only on the <em>first</em>
 *       frame the action becomes active.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionDigital#justReleased() justReleased()} - true only on the
 *       <em>first</em> frame the action becomes inactive.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionDigital#held() held()} - autorepeat: fires on the initial press, waits
 *       {@link org.flixelgdx.input.action.FlixelAction#getHoldDelay() holdDelay} seconds, then fires again every
 *       {@link org.flixelgdx.input.action.FlixelAction#getHoldInterval() holdInterval} seconds while held. Useful for
 *       navigating menus by holding a directional button.</li>
 * </ul>
 *
 * <h2>Reading an analog action</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionAnalog#getX() getX()} / {@link org.flixelgdx.input.action.FlixelActionAnalog#getY() getY()} -
 *       the normalized 2D vector for this frame, clamped to a maximum length of 1 so diagonal
 *       keyboard input does not exceed unit speed.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionAnalog#getPrevX() getPrevX()} / {@link org.flixelgdx.input.action.FlixelActionAnalog#getPrevY()
 *       getPrevY()} - the vector from the previous frame, useful for computing delta movement.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionAnalog#flickedRepeating() flickedRepeating()} - autorepeat equivalent
 *       of {@link org.flixelgdx.input.action.FlixelActionDigital#held() held()} for directional navigation (fires once on
 *       initial deflection, then holds after the delay).</li>
 * </ul>
 *
 * <h2>Hold-repeat timing</h2>
 *
 * <p>{@link org.flixelgdx.input.action.FlixelAction#getHoldDelay() holdDelay} and
 * {@link org.flixelgdx.input.action.FlixelAction#getHoldInterval() holdInterval} control the autorepeat rhythm used by
 * {@link org.flixelgdx.input.action.FlixelActionDigital#held() held()} and
 * {@link org.flixelgdx.input.action.FlixelActionAnalog#flickedRepeating() flickedRepeating()}. Defaults are 0.5 seconds for
 * the initial delay and 0.1 seconds between repeats. Adjust them before the game loop starts:
 *
 * <pre>{@code
 * menuNavigate.setHoldDelay(0.4f);     // 400 ms before the first repeat...
 * menuNavigate.setHoldInterval(0.08f); // ...then fire every 80 ms.
 * }</pre>
 *
 * <h2>Frame order</h2>
 *
 * <p>{@link org.flixelgdx.FlixelGame FlixelGame} drives action updates in this order each frame:
 *
 * <ol>
 *   <li>{@link org.flixelgdx.Flixel#keys Flixel.keys}{@code .update()},
 *       {@link org.flixelgdx.Flixel#mouse Flixel.mouse}{@code .update()},
 *       {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}{@code .update()}.</li>
 *   <li>{@link org.flixelgdx.input.action.FlixelActionSets#update(float) FlixelActionSets.update(elapsed)} - actions read the
 *       hardware snapshot written in step 1.</li>
 *   <li>{@link org.flixelgdx.FlixelState#update(float) FlixelState.update(elapsed)} runs now,
 *       seeing a fully consistent input snapshot for the current frame.</li>
 *   <li>After rendering: {@code keys.endFrame()}, {@code mouse.endFrame()},
 *       {@code gamepads.endFrame()}, then
 *       {@link org.flixelgdx.input.action.FlixelActionSets#endFrameAll() FlixelActionSets.endFrameAll()} clears
 *       {@code justPressed} and {@code justReleased} flags so they are accurate next frame.</li>
 * </ol>
 *
 * <h2>Steam Input</h2>
 *
 * <p>Ship a Steam Input manifest (a {@code .vdf} file under the {@code steam_input} layout) and
 * keep action {@link org.flixelgdx.input.action.FlixelAction#getName() names} in sync with the manifest entries. At runtime,
 * set {@link org.flixelgdx.input.action.FlixelActionSet#steamReader steamReader} on each action set to an implementation of
 * {@link org.flixelgdx.input.action.FlixelSteamActionReader} that reads Steam Input state each frame (for example, backed by
 * steamworks4j). The action's digital/analog value merges hardware bindings and Steam Input
 * together each frame.
 *
 * <p>Before Steam is initialized (or when shipping without Steam), assign the no-op reader to
 * satisfy the field without crashes:
 *
 * <pre>{@code
 * controls.steamReader = FlixelSteamActionReaders.EMPTY;
 * }</pre>
 */
package org.flixelgdx.input.action;
