/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.Input;

import me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadInput;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable description of one source for a {@link FlixelActionDigital}. Factory methods are allocation-free after the
 * binding is stored; do not create new bindings inside {@link me.stringdotjar.flixelgdx.FlixelState#update(float)}.
 *
 * <h2>Kinds</h2>
 *
 * <ul>
 *   <li>{@link FlixelInputBindingKind#KEY} via {@link #key(int)} uses {@link me.stringdotjar.flixelgdx.Flixel#keys}.</li>
 *   <li>{@link FlixelInputBindingKind#GAMEPAD_BUTTON} via {@link #gamepadButton(int, int)} uses logical codes from
 *       {@link me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadInput}; slot {@link #GAMEPAD_SLOT_ANY} maps to
 *       {@code anyPressed}.</li>
 *   <li>{@link FlixelInputBindingKind#POINTER_BUTTON} via {@link #pointerButton(int, int)} uses {@link me.stringdotjar.flixelgdx.Flixel#mouse}
 *       for {@link #POINTER_MOUSE}, else {@link com.badlogic.gdx.Gdx#input} multitouch.</li>
 *   <li>{@link FlixelInputBindingKind#TOUCH_REGION} via {@link #touchRegion(float, float, float, float)} polls normalized
 *       screen rects each frame (mobile-style zones).</li>
 * </ul>
 */
public final class FlixelInputBinding {

  /** Use with {@link #gamepadButton(int, int)} when any active slot should count. */
  public static final int GAMEPAD_SLOT_ANY = -1;

  /** Use with {@link #pointerButton(int, int)} for the primary mouse pointer (left button typical). */
  public static final int POINTER_MOUSE = -1;

  public final FlixelInputBindingKind kind;

  /** Keycode, logical gamepad button, pointer index, or unused. */
  public final int a;

  /** Gamepad slot, mouse button, or unused. */
  public final int b;

  /** Normalized touch region min X (0..1), only for {@link FlixelInputBindingKind#TOUCH_REGION}. */
  public final float normX;

  /** Normalized touch region min Y (0..1). */
  public final float normY;

  /** Normalized touch region width (0..1). */
  public final float normW;

  /** Normalized touch region height (0..1). */
  public final float normH;

  private FlixelInputBinding(
      FlixelInputBindingKind kind,
      int a,
      int b,
      float normX,
      float normY,
      float normW,
      float normH) {
    this.kind = kind;
    this.a = a;
    this.b = b;
    this.normX = normX;
    this.normY = normY;
    this.normW = normW;
    this.normH = normH;
  }

  /**
   * Keyboard key binding.
   *
   * @param keycode libGDX keycode (for example {@link com.badlogic.gdx.Input.Keys#SPACE}).
   * @return Binding instance.
   */
  @NotNull
  public static FlixelInputBinding key(int keycode) {
    return new FlixelInputBinding(FlixelInputBindingKind.KEY, keycode, 0, 0f, 0f, 0f, 0f);
  }

  /**
   * Gamepad button binding.
   *
   * @param gamepadSlot {@code 0..FlixelGamepadManager#MAX_GAMEPADS}-{@code 1}, or {@link #GAMEPAD_SLOT_ANY}.
   * @param logicalButton Value from {@link FlixelGamepadInput} (for example {@link FlixelGamepadInput#A}).
   * @return Binding instance.
   */
  @NotNull
  public static FlixelInputBinding gamepadButton(int gamepadSlot, int logicalButton) {
    return new FlixelInputBinding(FlixelInputBindingKind.GAMEPAD_BUTTON, logicalButton, gamepadSlot, 0f, 0f, 0f, 0f);
  }

  /**
   * Mouse or multitouch pointer button binding.
   *
   * @param pointer {@link #POINTER_MOUSE} for mouse, else multitouch pointer index.
   * @param button libGDX button (for example {@link Input.Buttons#LEFT}).
   * @return Binding instance.
   */
  @NotNull
  public static FlixelInputBinding pointerButton(int pointer, int button) {
    return new FlixelInputBinding(FlixelInputBindingKind.POINTER_BUTTON, pointer, button, 0f, 0f, 0f, 0f);
  }

  /**
   * Screen region in normalized coordinates (fraction of back buffer width and height).
   *
   * @param normX Left edge in 0..1.
   * @param normY Top edge in 0..1 (same orientation as libGDX screen Y).
   * @param normW Width in 0..1.
   * @param normH Height in 0..1.
   * @return Binding instance.
   */
  @NotNull
  public static FlixelInputBinding touchRegion(float normX, float normY, float normW, float normH) {
    return new FlixelInputBinding(FlixelInputBindingKind.TOUCH_REGION, 0, 0, normX, normY, normW, normH);
  }
}
