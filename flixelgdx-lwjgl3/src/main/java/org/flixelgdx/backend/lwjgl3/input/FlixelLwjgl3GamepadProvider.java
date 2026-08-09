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
package org.flixelgdx.backend.lwjgl3.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.collections.FlixelIdentityMap;
import org.flixelgdx.input.gamepad.FlixelGamepad;
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.gamepad.FlixelGamepadListener;
import org.flixelgdx.input.gamepad.FlixelGamepadMapping;
import org.flixelgdx.input.gamepad.FlixelGamepadMappingResolver;
import org.flixelgdx.input.gamepad.FlixelGamepadProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Desktop (LWJGL3) {@link FlixelGamepadProvider} backed by gdx-controllers (SDL via Jamepad).
 *
 * <p>This is the transitional gamepad source the framework installs while the desktop platform
 * still runs on libGDX; a native SDL3 provider replaces it later. It wraps each gdx
 * {@link Controller} in a {@link FlixelGamepad} and forwards connect/disconnect events, so
 * {@code FlixelGamepadInputManager} never names gdx-controllers. All gamepad logic stays in the
 * manager; this class is only the raw feed.
 *
 * <p>Wrappers are cached by controller identity so the same physical pad always maps to the same
 * {@link FlixelGamepad} instance, which the manager relies on to track slots.
 *
 * <p>This class also implements {@link FlixelGamepadMappingResolver} so the launcher can install
 * it in the resolver chain. It translates the gdx-controllers SDL mapping database to
 * {@link FlixelGamepadMapping} at connect time by reading the gdx {@link ControllerMapping}
 * through {@link FlixelGamepad#getNativeHandle()}.
 *
 * <p>Vendor and product IDs are not available from the gdx-controllers API and always return
 * {@code 0}. Resolvers that need VID/PID should wait for the SDL3 provider in Phase 3.
 */
public final class FlixelLwjgl3GamepadProvider implements FlixelGamepadProvider,
    FlixelGamepadMappingResolver {

  private final FlixelIdentityMap<Controller, FlixelGdxGamepad> wrappers = new FlixelIdentityMap<>();
  private final FlixelIdentityMap<FlixelGamepadListener, ControllerListener> adapters =
      new FlixelIdentityMap<>();

  /** Creates a provider over the shared gdx-controllers registry for this session. */
  public FlixelLwjgl3GamepadProvider() {}

  @Override
  public int getGamepadCount() {
    try {
      return Controllers.getControllers().size;
    } catch (Throwable ignored) {
      return 0;
    }
  }

  @Override
  @Nullable
  public FlixelGamepad getGamepadAt(int index) {
    try {
      Array<Controller> list = Controllers.getControllers();
      if (index < 0 || index >= list.size) {
        return null;
      }
      return wrap(list.get(index));
    } catch (Throwable ignored) {
      return null;
    }
  }

  @Override
  public void addListener(@NotNull FlixelGamepadListener listener) {
    if (listener == null || adapters.get(listener) != null) {
      return;
    }
    ControllerListener adapter = new GdxListenerAdapter(listener);
    adapters.put(listener, adapter);
    try {
      Controllers.addListener(adapter);
    } catch (Throwable ignored) {
      // Some backends may not expose Controllers until fully booted.
    }
  }

  @Override
  public void removeListener(@NotNull FlixelGamepadListener listener) {
    if (listener == null) {
      return;
    }
    ControllerListener adapter = adapters.remove(listener);
    if (adapter != null) {
      try {
        Controllers.removeListener(adapter);
      } catch (Throwable ignored) {
        // Ignore.
      }
    }
  }

  @Override
  @Nullable
  public FlixelGamepadMapping resolve(@NotNull FlixelGamepad gamepad) {
    Object handle = gamepad.getNativeHandle();
    if (!(handle instanceof Controller)) {
      return null;
    }
    ControllerMapping gdxMapping = ((Controller) handle).getMapping();
    if (gdxMapping == null) {
      return null;
    }
    return translate(gdxMapping);
  }

  private FlixelGdxGamepad wrap(Controller controller) {
    FlixelGdxGamepad existing = wrappers.get(controller);
    if (existing != null) {
      return existing;
    }
    FlixelGdxGamepad created = new FlixelGdxGamepad(controller);
    wrappers.put(controller, created);
    return created;
  }

  private static FlixelGamepadMapping translate(ControllerMapping m) {
    FlixelGamepadMapping out = new FlixelGamepadMapping();
    out.registerButton(FlixelGamepadButton.A, m.buttonA);
    out.registerButton(FlixelGamepadButton.B, m.buttonB);
    out.registerButton(FlixelGamepadButton.X, m.buttonX);
    out.registerButton(FlixelGamepadButton.Y, m.buttonY);
    out.registerButton(FlixelGamepadButton.L1, m.buttonL1);
    out.registerButton(FlixelGamepadButton.R1, m.buttonR1);
    out.registerButton(FlixelGamepadButton.L2, m.buttonL2);
    out.registerButton(FlixelGamepadButton.R2, m.buttonR2);
    out.registerButton(FlixelGamepadButton.LEFT_STICK, m.buttonLeftStick);
    out.registerButton(FlixelGamepadButton.RIGHT_STICK, m.buttonRightStick);
    out.registerButton(FlixelGamepadButton.START, m.buttonStart);
    out.registerButton(FlixelGamepadButton.BACK, m.buttonBack);
    out.registerButton(FlixelGamepadButton.DPAD_UP, m.buttonDpadUp);
    out.registerButton(FlixelGamepadButton.DPAD_DOWN, m.buttonDpadDown);
    out.registerButton(FlixelGamepadButton.DPAD_LEFT, m.buttonDpadLeft);
    out.registerButton(FlixelGamepadButton.DPAD_RIGHT, m.buttonDpadRight);
    out.registerAxis(FlixelGamepadAxis.LEFT_X, m.axisLeftX);
    out.registerAxis(FlixelGamepadAxis.LEFT_Y, m.axisLeftY);
    out.registerAxis(FlixelGamepadAxis.RIGHT_X, m.axisRightX);
    out.registerAxis(FlixelGamepadAxis.RIGHT_Y, m.axisRightY);
    // On Jamepad/SDL, triggers arrive as axes 4 and 5. Register them as L2/R2 axes so the manager
    // can read trigger pressure and synthesize button state when buttonL2/R2 are UNDEFINED.
    if (m.buttonL2 == ControllerMapping.UNDEFINED) {
      out.registerAxis(FlixelGamepadAxis.L2, 4);
    }
    if (m.buttonR2 == ControllerMapping.UNDEFINED) {
      out.registerAxis(FlixelGamepadAxis.R2, 5);
    }
    return out;
  }

  /** Translates gdx controller events onto a {@link FlixelGamepadListener}. */
  private final class GdxListenerAdapter implements ControllerListener {

    private final FlixelGamepadListener delegate;

    GdxListenerAdapter(FlixelGamepadListener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void connected(Controller controller) {
      delegate.connected(wrap(controller));
    }

    @Override
    public void disconnected(Controller controller) {
      delegate.disconnected(wrap(controller));
      wrappers.remove(controller);
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonIndex) {
      return delegate.buttonDown(wrap(controller), buttonIndex);
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonIndex) {
      return delegate.buttonUp(wrap(controller), buttonIndex);
    }

    @Override
    public boolean axisMoved(Controller controller, int axisIndex, float value) {
      return delegate.axisMoved(wrap(controller), axisIndex, value);
    }
  }

  /** Wraps a single gdx {@link Controller} as a {@link FlixelGamepad}. */
  private static final class FlixelGdxGamepad implements FlixelGamepad {

    private final Controller controller;

    FlixelGdxGamepad(Controller controller) {
      this.controller = controller;
    }

    @Override
    @NotNull
    public String getName() {
      String name = controller.getName();
      return name != null ? name : "";
    }

    @Override
    public int getMinButtonIndex() {
      return controller.getMinButtonIndex();
    }

    @Override
    public int getMaxButtonIndex() {
      return controller.getMaxButtonIndex();
    }

    @Override
    public boolean getButton(int buttonIndex) {
      return controller.getButton(buttonIndex);
    }

    @Override
    public int getAxisCount() {
      return controller.getAxisCount();
    }

    @Override
    public float getAxis(int axisIndex) {
      return controller.getAxis(axisIndex);
    }

    @Override
    public boolean canVibrate() {
      return controller.canVibrate();
    }

    @Override
    public void startVibration(int durationMs, float strength) {
      controller.startVibration(durationMs, strength);
    }

    @Override
    public void cancelVibration() {
      controller.cancelVibration();
    }

    @Override
    @NotNull
    public Object getNativeHandle() {
      return controller;
    }
  }
}
