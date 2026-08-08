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
import org.flixelgdx.input.gamepad.FlixelController;
import org.flixelgdx.input.gamepad.FlixelControllerListener;
import org.flixelgdx.input.gamepad.FlixelControllerMapping;
import org.flixelgdx.input.gamepad.FlixelControllerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Desktop (LWJGL3) {@link FlixelControllerProvider} backed by gdx-controllers (SDL via Jamepad).
 *
 * <p>This is the transitional controller source the framework installs while the desktop platform
 * still runs on libGDX; a native SDL3 provider replaces it later. It wraps each gdx
 * {@link Controller} in a {@link FlixelController} and forwards connect/disconnect events, so
 * {@code FlixelGamepadInputManager} never names gdx-controllers. All gamepad logic stays in the
 * manager; this class is only the raw feed.
 *
 * <p>Wrappers are cached by controller identity so the same physical pad always maps to the same
 * {@link FlixelController} instance, which the manager relies on to track slots.
 */
public final class FlixelLwjgl3ControllerProvider implements FlixelControllerProvider {

  private final FlixelIdentityMap<Controller, FlixelGdxController> wrappers = new FlixelIdentityMap<>();
  private final FlixelIdentityMap<FlixelControllerListener, ControllerListener> adapters =
      new FlixelIdentityMap<>();

  /** Creates a provider over the shared gdx-controllers registry for this session. */
  public FlixelLwjgl3ControllerProvider() {}

  @Override
  public int getControllerCount() {
    try {
      return Controllers.getControllers().size;
    } catch (Throwable ignored) {
      return 0;
    }
  }

  @Override
  @Nullable
  public FlixelController getControllerAt(int index) {
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
  public void addListener(@NotNull FlixelControllerListener listener) {
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
  public void removeListener(@NotNull FlixelControllerListener listener) {
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

  private FlixelGdxController wrap(Controller controller) {
    FlixelGdxController existing = wrappers.get(controller);
    if (existing != null) {
      return existing;
    }
    FlixelGdxController created = new FlixelGdxController(controller);
    wrappers.put(controller, created);
    return created;
  }

  /** Translates gdx controller events onto a {@link FlixelControllerListener}. */
  private final class GdxListenerAdapter implements ControllerListener {

    private final FlixelControllerListener delegate;

    GdxListenerAdapter(FlixelControllerListener delegate) {
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

  /** Wraps a single gdx {@link Controller} as a {@link FlixelController}. */
  private static final class FlixelGdxController implements FlixelController {

    private final Controller controller;

    @Nullable
    private FlixelControllerMapping mapping;

    FlixelGdxController(Controller controller) {
      this.controller = controller;
    }

    @Override
    @NotNull
    public String getName() {
      String name = controller.getName();
      return name != null ? name : "";
    }

    @Override
    @NotNull
    public FlixelControllerMapping getMapping() {
      if (mapping == null) {
        mapping = translate(controller.getMapping());
      }
      return mapping;
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

    private static FlixelControllerMapping translate(ControllerMapping m) {
      FlixelControllerMapping out = new FlixelControllerMapping();
      out.buttonA = m.buttonA;
      out.buttonB = m.buttonB;
      out.buttonX = m.buttonX;
      out.buttonY = m.buttonY;
      out.buttonL1 = m.buttonL1;
      out.buttonR1 = m.buttonR1;
      out.buttonL2 = m.buttonL2;
      out.buttonR2 = m.buttonR2;
      out.buttonLeftStick = m.buttonLeftStick;
      out.buttonRightStick = m.buttonRightStick;
      out.buttonStart = m.buttonStart;
      out.buttonBack = m.buttonBack;
      out.buttonDpadUp = m.buttonDpadUp;
      out.buttonDpadDown = m.buttonDpadDown;
      out.buttonDpadLeft = m.buttonDpadLeft;
      out.buttonDpadRight = m.buttonDpadRight;
      out.axisLeftX = m.axisLeftX;
      out.axisLeftY = m.axisLeftY;
      out.axisRightX = m.axisRightX;
      out.axisRightY = m.axisRightY;
      return out;
    }
  }
}
