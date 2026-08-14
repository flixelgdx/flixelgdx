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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.gamepad.FlixelGamepadListener;
import org.flixelgdx.input.gamepad.FlixelGamepadMapping;
import org.flixelgdx.input.gamepad.FlixelGamepadMappingResolver;
import org.flixelgdx.input.gamepad.FlixelGamepadProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLGamepad;

import java.nio.IntBuffer;

/**
 * The desktop gamepad feed, backed by SDL3.
 *
 * <p>SDL recognizes controllers through its built-in community mapping database and presents them
 * all in one standard layout, so this provider hands {@link FlixelGamepadInputManager} a stable
 * {@link FlixelSdlGamepad} per physical device and a single shared {@link FlixelGamepadMapping}.
 * Because that standard layout is the same for every SDL pad, this class also serves as the
 * {@link FlixelGamepadMappingResolver}: it returns the same mapping for any gamepad the manager
 * asks about.
 *
 * <p>The desktop runner drives the lifecycle: it calls {@link #openConnected()} once SDL is up to
 * pick up pads that were already plugged in, and forwards SDL's add and remove events to
 * {@link #onDeviceAdded(int)} and {@link #onDeviceRemoved(int)} as they arrive. All gamepad logic
 * (slot assignment, press tracking, dead zones) stays in the manager; this class is only the raw
 * feed plus the mapping.
 */
public class FlixelSdlGamepadProvider implements FlixelGamepadProvider, FlixelGamepadMappingResolver {

  /** Connected pads in a stable order; the manager tracks slots by instance identity. */
  private final FlixelArray<FlixelSdlGamepad> gamepads = new FlixelArray<>();

  private final FlixelArray<FlixelGamepadListener> listeners = new FlixelArray<>();

  /** The single standard mapping shared by every SDL gamepad. */
  private final FlixelGamepadMapping standardMapping = buildStandardMapping();

  /** Opens every gamepad that is already connected. Call once after SDL has been initialized. */
  public void openConnected() {
    IntBuffer ids = SDLGamepad.SDL_GetGamepads();
    if (ids == null) {
      return;
    }
    while (ids.hasRemaining()) {
      onDeviceAdded(ids.get());
    }
  }

  /**
   * Handles an SDL "gamepad added" event by opening the device and announcing it.
   *
   * @param instanceId The SDL joystick instance id from the event.
   */
  public void onDeviceAdded(int instanceId) {
    if (!SDLGamepad.SDL_IsGamepad(instanceId) || indexOf(instanceId) >= 0) {
      return;
    }
    long handle = SDLGamepad.SDL_OpenGamepad(instanceId);
    if (handle == 0L) {
      return;
    }
    FlixelSdlGamepad gamepad = new FlixelSdlGamepad(handle, instanceId);
    gamepads.add(gamepad);
    for (int i = 0; i < listeners.getSize(); i++) {
      listeners.get(i).connected(gamepad);
    }
  }

  /**
   * Handles an SDL "gamepad removed" event by announcing and closing the device.
   *
   * @param instanceId The SDL joystick instance id from the event.
   */
  public void onDeviceRemoved(int instanceId) {
    int index = indexOf(instanceId);
    if (index < 0) {
      return;
    }
    FlixelSdlGamepad gamepad = gamepads.get(index);
    gamepads.removeIndex(index);
    for (int i = 0; i < listeners.getSize(); i++) {
      listeners.get(i).disconnected(gamepad);
    }
    gamepad.close();
  }

  /** Closes every open gamepad. Call during shutdown. */
  public void dispose() {
    for (int i = 0; i < gamepads.getSize(); i++) {
      gamepads.get(i).close();
    }
    gamepads.clear();
    listeners.clear();
  }

  @Override
  public int getGamepadCount() {
    return gamepads.getSize();
  }

  @Nullable
  @Override
  public FlixelGamepad getGamepadAt(int index) {
    return index >= 0 && index < gamepads.getSize() ? gamepads.get(index) : null;
  }

  @Override
  public void addListener(@NotNull FlixelGamepadListener listener) {
    if (!listeners.contains(listener, true)) {
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(@NotNull FlixelGamepadListener listener) {
    listeners.removeValue(listener, true);
  }

  @Nullable
  @Override
  public FlixelGamepadMapping resolve(@NotNull FlixelGamepad gamepad) {
    return gamepad instanceof FlixelSdlGamepad ? standardMapping : null;
  }

  /** Returns the index of the pad with the given instance id, or {@code -1} when not present. */
  private int indexOf(int instanceId) {
    for (int i = 0; i < gamepads.getSize(); i++) {
      if (gamepads.get(i).getInstanceId() == instanceId) {
        return i;
      }
    }
    return -1;
  }

  /** Builds the fixed mapping from SDL's standard layout to the framework's logical tokens. */
  private static FlixelGamepadMapping buildStandardMapping() {
    FlixelGamepadMapping mapping = new FlixelGamepadMapping();
    mapping.registerButton(FlixelGamepadButton.A, SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH);
    mapping.registerButton(FlixelGamepadButton.B, SDLGamepad.SDL_GAMEPAD_BUTTON_EAST);
    mapping.registerButton(FlixelGamepadButton.X, SDLGamepad.SDL_GAMEPAD_BUTTON_WEST);
    mapping.registerButton(FlixelGamepadButton.Y, SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH);
    mapping.registerButton(FlixelGamepadButton.BACK, SDLGamepad.SDL_GAMEPAD_BUTTON_BACK);
    mapping.registerButton(FlixelGamepadButton.MODE, SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE);
    mapping.registerButton(FlixelGamepadButton.START, SDLGamepad.SDL_GAMEPAD_BUTTON_START);
    mapping.registerButton(FlixelGamepadButton.LEFT_STICK, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK);
    mapping.registerButton(FlixelGamepadButton.RIGHT_STICK, SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK);
    mapping.registerButton(FlixelGamepadButton.L1, SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER);
    mapping.registerButton(FlixelGamepadButton.R1, SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER);
    mapping.registerButton(FlixelGamepadButton.DPAD_UP, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP);
    mapping.registerButton(FlixelGamepadButton.DPAD_DOWN, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN);
    mapping.registerButton(FlixelGamepadButton.DPAD_LEFT, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT);
    mapping.registerButton(FlixelGamepadButton.DPAD_RIGHT, SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT);

    mapping.registerAxis(FlixelGamepadAxis.LEFT_X, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX);
    mapping.registerAxis(FlixelGamepadAxis.LEFT_Y, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY);
    mapping.registerAxis(FlixelGamepadAxis.RIGHT_X, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX);
    mapping.registerAxis(FlixelGamepadAxis.RIGHT_Y, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY);
    mapping.registerAxis(FlixelGamepadAxis.L2, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER);
    mapping.registerAxis(FlixelGamepadAxis.R2, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER);
    return mapping;
  }

  @NotNull
  public FlixelGamepadMapping getStandardMapping() {
    return standardMapping;
  }
}
