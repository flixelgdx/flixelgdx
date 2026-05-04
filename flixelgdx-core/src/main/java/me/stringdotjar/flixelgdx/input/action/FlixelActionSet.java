/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.FlixelDestroyable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups logical {@link FlixelAction} instances (digital and analog) and advances them on the same frame
 * contract as {@link me.stringdotjar.flixelgdx.input.FlixelInputManager}. Actions read {@link me.stringdotjar.flixelgdx.Flixel#keys},
 * {@link me.stringdotjar.flixelgdx.Flixel#mouse}, {@link me.stringdotjar.flixelgdx.Flixel#gamepads}, and {@code Gdx.input}
 * during {@link #update(float)}. This class is <strong>not</strong> an {@link com.badlogic.gdx.InputProcessor}: you do not add
 * it to {@link com.badlogic.gdx.InputMultiplexer}. Framework keyboard and mouse processors stay the single libGDX entry
 * points for those devices.
 *
 * <h2>Lifecycle (normal games)</h2>
 *
 * <ol>
 *   <li>Construct a subclass (or this type) after {@link me.stringdotjar.flixelgdx.Flixel#initialize}.</li>
 *   <li>In the subclass constructor, create {@link FlixelActionDigital} / {@link FlixelActionAnalog} instances,
 *       call {@link #add(FlixelAction)} for each, and configure {@link FlixelInputBinding} / {@link FlixelAnalogAxisBinding}.</li>
 *   <li>By default the set registers with {@link FlixelActionSets}; {@link me.stringdotjar.flixelgdx.FlixelGame} calls
 *       {@link FlixelActionSets#updateAll(float)} after {@code Flixel.gamepads.update()} and {@link FlixelActionSets#endFrameAll()}
 *       after keys, mouse, and gamepads {@code endFrame()} in {@code render()}.</li>
 *   <li>From {@link me.stringdotjar.flixelgdx.FlixelState#update(float)} (or similar), read {@code jump.justPressed()},
 *       {@code move.getX()}, etc.</li>
 *   <li>When the screen or mode ends, call {@link #destroy()} so the set unregisters and clears members.</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * public class PlayerControls extends FlixelActionSet {
 * 
 *   public final FlixelActionDigital jump;
 *   public final FlixelActionAnalog move;
 *
 *   public PlayerControls() {
 *     jump = new FlixelActionDigital("jump");
 *     jump.addBinding(FlixelInputBinding.key(FlixelKey.SPACE));
 *     jump.addBinding(FlixelInputBinding.gamepadButton(0, FlixelGamepadInput.A));
 *     add(jump);
 *
 *     move = new FlixelActionAnalog("move");
 *     move.addAxisBinding(FlixelAnalogAxisBinding.negXKey(FlixelKey.A));
 *     move.addAxisBinding(FlixelAnalogAxisBinding.posXKey(FlixelKey.D));
 *     move.addAxisBinding(FlixelAnalogAxisBinding.gamepadAxisX(0, FlixelGamepadInput.AXIS_LEFT_X));
 *     move.addAxisBinding(FlixelAnalogAxisBinding.gamepadAxisY(0, FlixelGamepadInput.AXIS_LEFT_Y));
 *     add(move);
 *   }
 * }
 * }</pre>
 *
 * <p>Then, in a state for your game:
 *
 * <pre>{@code
 * public class PlayState extends FlixelState {
 *
 *   private final PlayerControls controls = new PlayerControls();
 *
 *   @Override
 *   public void update(float elapsed) {
 *     if (controls.jump.justPressed()) {
 *       // fire jump once this frame
 *     }
 *     float mx = controls.move.getX();
 *     float my = controls.move.getY();
 *   }
 *
 *   @Override
 *   public void destroy() {
 *     controls.destroy();
 *     super.destroy();
 *   }
 * }
 * }</pre>
 *
 * <h2>Steam Input</h2>
 *
 * <p>Set {@link #steamReader} to an implementation that reads Steam Input API (for example via steamworks4j). Action
 * {@link FlixelAction#getName()} strings should match your in-game actions manifest. Use {@link FlixelSteamActionReaders#EMPTY}
 * when Steam is unavailable.
 *
 * <h2>Tests</h2>
 *
 * <p>Pass {@code false} to {@link #FlixelActionSet(boolean)} from an anonymous subclass so the set does not register globally,
 * then call {@link #update(float)} and {@link #endFrame()} yourself.
 */
public class FlixelActionSet implements FlixelDestroyable {

  protected final Array<FlixelAction> members;

  /**
   * Optional Steam Input bridge; when non-null, digital and analog actions also read these values
   * each {@link #update(float)}. Implement outside core with steamworks4j or similar.
   */
  @Nullable
  public FlixelSteamActionReader steamReader;

  private final boolean registerForGlobalLifecycle;

  /**
   * Registers this set with {@link FlixelActionSets} for automatic {@link #update(float)} and {@link #endFrame()}.
   */
  public FlixelActionSet() {
    this(true);
  }

  /**
   * @param registerForGlobalLifecycle When {@code true}, registers with {@link FlixelActionSets}. Tests may pass
   *   {@code false} and call {@link #update(float)} / {@link #endFrame()} manually.
   */
  protected FlixelActionSet(boolean registerForGlobalLifecycle) {
    this.members = new Array<>(16);
    this.registerForGlobalLifecycle = registerForGlobalLifecycle;
    if (registerForGlobalLifecycle) {
      FlixelActionSets.register(this);
    }
  }

  /**
   * Adds an action to this set and assigns ownership for Steam name resolution and lifecycle.
   *
   * @param action Non-null action instance.
   */
  protected void add(@NotNull FlixelAction action) {
    members.add(action);
    action.setOwner(this);
  }

  /**
   * Steps all member actions. Called from {@link FlixelActionSets#updateAll(float)} after gamepads update.
   *
   * @param elapsed Frame delta in seconds.
   */
  public void update(float elapsed) {
    for (int i = 0, n = members.size; i < n; i++) {
      members.get(i).updateAction(elapsed);
    }
  }

  /**
   * Finalizes edge detection for digital and analog actions. Called from {@link FlixelActionSets#endFrameAll()}
   * after keys, mouse, and gamepads {@code endFrame()}.
   */
  public void endFrame() {
    for (int i = 0, n = members.size; i < n; i++) {
      members.get(i).endFrameAction();
    }
  }

  @Override
  public void destroy() {
    if (registerForGlobalLifecycle) {
      FlixelActionSets.unregister(this);
    }
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelAction a = members.get(i);
      a.setOwner(null);
      a.resetAction();
    }
    members.clear();
    steamReader = null;
  }
}
