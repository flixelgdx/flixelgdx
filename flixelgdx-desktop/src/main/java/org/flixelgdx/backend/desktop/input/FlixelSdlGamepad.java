/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.backend.desktop.input;

import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLGamepad;

/**
 * A single SDL3 gamepad, wrapping one opened {@code SDL_Gamepad} handle.
 *
 * <p>SDL already normalizes every controller it recognizes to a standard layout (the same button
 * and axis numbering for an Xbox pad, a DualShock, a Switch Pro controller, and so on), so the
 * native indices this reports are SDL's standard {@code SDL_GAMEPAD_BUTTON_*} and
 * {@code SDL_GAMEPAD_AXIS_*} values. {@link FlixelSdlGamepadProvider} pairs that with one fixed
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadMapping FlixelGamepadMapping}, which is why every
 * SDL pad shares a single mapping rather than needing a per-device database lookup.
 *
 * <p>Axis values come back from SDL as signed 16-bit integers; sticks span the full range and
 * triggers span zero to the maximum. Both are scaled here into the {@code [-1, 1]} range the
 * framework expects.
 */
public class FlixelSdlGamepad implements FlixelGamepad {

  /** SDL reports axes as Sint16; this is the divisor that scales them into [-1, 1]. */
  private static final float AXIS_SCALE = 32767f;

  /** The opened {@code SDL_Gamepad} handle. */
  private final long handle;

  /** The SDL joystick instance id, used to match connect and disconnect events. */
  private final int instanceId;

  @NotNull
  private final String name;

  private final int vendorId;
  private final int productId;

  FlixelSdlGamepad(long handle, int instanceId) {
    this.handle = handle;
    this.instanceId = instanceId;
    String n = SDLGamepad.SDL_GetGamepadName(handle);
    this.name = n != null ? n : "Gamepad";
    this.vendorId = SDLGamepad.SDL_GetGamepadVendor(handle) & 0xFFFF;
    this.productId = SDLGamepad.SDL_GetGamepadProduct(handle) & 0xFFFF;
  }

  /** @return The SDL joystick instance id this wrapper was opened for. */
  int getInstanceId() {
    return instanceId;
  }

  /** Closes the underlying SDL gamepad handle. */
  void close() {
    SDLGamepad.SDL_CloseGamepad(handle);
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getVendorId() {
    return vendorId;
  }

  @Override
  public int getProductId() {
    return productId;
  }

  @Override
  public int getMinButtonIndex() {
    return 0;
  }

  @Override
  public int getMaxButtonIndex() {
    return SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT - 1;
  }

  @Override
  public boolean getButton(int buttonIndex) {
    return SDLGamepad.SDL_GetGamepadButton(handle, buttonIndex);
  }

  @Override
  public int getAxisCount() {
    return SDLGamepad.SDL_GAMEPAD_AXIS_COUNT;
  }

  @Override
  public float getAxis(int axisIndex) {
    float value = SDLGamepad.SDL_GetGamepadAxis(handle, axisIndex) / AXIS_SCALE;
    if (value < -1f) {
      return -1f;
    }
    return Math.min(value, 1f);
  }

  @Override
  public boolean canVibrate() {
    return true;
  }

  @Override
  public void startVibration(int durationMs, float strength) {
    int magnitude = (int) (clamp01(strength) * 0xFFFF);
    SDLGamepad.SDL_RumbleGamepad(handle, (short) magnitude, (short) magnitude, durationMs);
  }

  @Override
  public void cancelVibration() {
    SDLGamepad.SDL_RumbleGamepad(handle, (short) 0, (short) 0, 0);
  }

  @Nullable
  @Override
  public Object getNativeHandle() {
    return handle;
  }

  private static float clamp01(float v) {
    if (v < 0f) {
      return 0f;
    }
    return Math.min(v, 1f);
  }
}
