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
package org.flixelgdx.backend.html5.input;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.gamepad.FlixelGamepadListener;
import org.flixelgdx.input.gamepad.FlixelGamepadMapping;
import org.flixelgdx.input.gamepad.FlixelGamepadMappingResolver;
import org.flixelgdx.input.gamepad.FlixelGamepadProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Discovers and tracks controllers through the browser Gamepad API.
 *
 * <p>This is the web counterpart of the desktop SDL gamepad provider. The browser fires
 * {@code gamepadconnected} and {@code gamepaddisconnected} events as controllers come and go, so
 * this provider listens for those and keeps a live list of {@link FlixelWebGamepad} wrappers. Per
 * frame, the framework's gamepad manager polls those wrappers for button and axis state, exactly as
 * it does on desktop.
 *
 * <p>It also acts as its own {@link FlixelGamepadMappingResolver}, handing back a single shared
 * "standard layout" mapping for every controller. The Gamepad API already normalizes common
 * controllers to a fixed button and axis order, so one mapping covers them without per-device
 * lookups.
 */
public class FlixelWebGamepadProvider implements FlixelGamepadProvider, FlixelGamepadMappingResolver {

  @NotNull
  private final FlixelArray<FlixelWebGamepad> gamepads = new FlixelArray<>();

  @NotNull
  private final FlixelArray<FlixelGamepadListener> listeners = new FlixelArray<>();

  @NotNull
  private final FlixelGamepadMapping standardMapping = buildStandardMapping();

  /**
   * Installs the browser connect and disconnect listeners so the provider learns about controllers.
   * Called once during startup.
   */
  public void attach() {
    installGamepadEvents(this::onConnected, this::onDisconnected);
  }

  @Override
  public int getGamepadCount() {
    return gamepads.getSize();
  }

  @Override
  @Nullable
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

  @Override
  @Nullable
  public FlixelGamepadMapping resolve(@NotNull FlixelGamepad gamepad) {
    return gamepad instanceof FlixelWebGamepad ? standardMapping : null;
  }

  /**
   * Handles a browser connect event by wrapping the new controller and notifying listeners.
   *
   * @param index The browser slot index of the connected controller.
   */
  private void onConnected(int index) {
    FlixelWebGamepad gamepad = new FlixelWebGamepad(index);
    gamepads.add(gamepad);
    for (int i = 0; i < listeners.getSize(); i++) {
      listeners.get(i).connected(gamepad);
    }
  }

  /**
   * Handles a browser disconnect event by removing the matching controller and notifying listeners.
   *
   * @param index The browser slot index of the disconnected controller.
   */
  private void onDisconnected(int index) {
    for (int i = 0; i < gamepads.getSize(); i++) {
      FlixelWebGamepad gamepad = gamepads.get(i);
      if (gamepad.getIndex() == index) {
        gamepads.removeIndex(i);
        for (int j = 0; j < listeners.getSize(); j++) {
          listeners.get(j).disconnected(gamepad);
        }
        return;
      }
    }
  }

  /**
   * Builds the shared "standard layout" mapping used for every browser controller.
   *
   * @return The standard-layout mapping.
   */
  private static FlixelGamepadMapping buildStandardMapping() {
    FlixelGamepadMapping mapping = new FlixelGamepadMapping();
    mapping.registerButton(FlixelGamepadButton.A, 0);
    mapping.registerButton(FlixelGamepadButton.B, 1);
    mapping.registerButton(FlixelGamepadButton.X, 2);
    mapping.registerButton(FlixelGamepadButton.Y, 3);
    mapping.registerButton(FlixelGamepadButton.L1, 4);
    mapping.registerButton(FlixelGamepadButton.R1, 5);
    mapping.registerButton(FlixelGamepadButton.L2, 6);
    mapping.registerButton(FlixelGamepadButton.R2, 7);
    mapping.registerButton(FlixelGamepadButton.BACK, 8);
    mapping.registerButton(FlixelGamepadButton.START, 9);
    mapping.registerButton(FlixelGamepadButton.LEFT_STICK, 10);
    mapping.registerButton(FlixelGamepadButton.RIGHT_STICK, 11);
    mapping.registerButton(FlixelGamepadButton.DPAD_UP, 12);
    mapping.registerButton(FlixelGamepadButton.DPAD_DOWN, 13);
    mapping.registerButton(FlixelGamepadButton.DPAD_LEFT, 14);
    mapping.registerButton(FlixelGamepadButton.DPAD_RIGHT, 15);
    mapping.registerButton(FlixelGamepadButton.MODE, 16);

    mapping.registerAxis(FlixelGamepadAxis.LEFT_X, 0);
    mapping.registerAxis(FlixelGamepadAxis.LEFT_Y, 1);
    mapping.registerAxis(FlixelGamepadAxis.RIGHT_X, 2);
    mapping.registerAxis(FlixelGamepadAxis.RIGHT_Y, 3);
    return mapping;
  }

  @JSBody(params = { "onConnect", "onDisconnect" },
      script = "window.addEventListener('gamepadconnected', function(e) { onConnect.accept(e.gamepad.index); });"
          + "window.addEventListener('gamepaddisconnected', function(e) { onDisconnect.accept(e.gamepad.index); });")
  private static native void installGamepadEvents(IndexCallback onConnect, IndexCallback onDisconnect);

  /** Receives a controller slot index from a browser gamepad connect or disconnect event. */
  @JSFunctor
  private interface IndexCallback extends JSObject {
    void accept(int index);
  }
}
