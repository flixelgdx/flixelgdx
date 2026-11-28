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
package org.flixelgdx;

import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.backend.FlixelAlerter;
import org.flixelgdx.backend.FlixelBootManager;
import org.flixelgdx.backend.FlixelHaptics;
import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelProfiler;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.remote.FlixelDebugEndpoint;
import org.flixelgdx.debug.remote.FlixelDebugTransport;
import org.flixelgdx.file.FlixelFiles;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelOverlayManager;
import org.flixelgdx.graphics.FlixelRenderManager;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.flixelgdx.locale.FlixelLocalization;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.save.FlixelSave;
import org.flixelgdx.save.FlixelSaveHandler;
import org.flixelgdx.testing.FlixelMemoryFiles;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Supplier;

public final class FlixelHeadlessExtension
    implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(FlixelHeadlessExtension.class);

  @Override
  public synchronized void beforeAll(ExtensionContext context) {
    if (!(Flixel.files instanceof FlixelMemoryFiles)) {
      Flixel.files = new FlixelMemoryFiles();
    }
    context.getStore(NAMESPACE).put(Globals.class, Globals.capture());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    restore(context);
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    context.getStore(NAMESPACE).put(Globals.class, Globals.capture());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    restore(context);
  }

  private static void restore(ExtensionContext context) {
    Globals globals = context.getStore(NAMESPACE).remove(Globals.class, Globals.class);
    if (globals != null) {
      globals.restore();
    }
  }

  private static final class Globals {

    private float timeScale;
    private FlixelLogger log;
    private FlixelGame game;
    private FlixelConfig config;
    private FlixelBootManager boot;
    private FlixelAlerter alert;
    private FlixelState state;
    private Supplier<FlixelState> currentStateFactory;
    private FlixelKeyInputManager keys;
    private FlixelSoundManager sound;
    private FlixelAssetManager assets;
    private FlixelDebugManager debug;
    private FlixelSave save;
    private FlixelLocalization locale;
    private FlixelMouseInputManager mouse;
    private FlixelTouchManager touches;
    private FlixelGamepadInputManager gamepads;
    private FlixelWindow window;
    private FlixelGraphicsManager graphics;
    private FlixelRenderManager render;
    private FlixelCamera.Manager cameraManager;
    private FlixelOverlayManager overlay;
    private FlixelHostIntegration host;
    private FlixelHaptics haptics;
    private FlixelProfiler profiler;
    private FlixelInputDevice input;
    private FlixelFiles files;
    private FlixelRuntimeDevice runtime;
    private FlixelArray<FlixelCamera> cameras;
    private FlixelCamera drawCamera;
    private FlixelSaveHandler saveHandler;
    private FlixelDebugTransport debugTransport;
    private FlixelDebugEndpoint debugEndpoint;

    private boolean applyAntialiasingOnStateAdd;
    private boolean autoPause;

    static Globals capture() {
      Globals g = new Globals();
      g.timeScale = Flixel.timeScale;
      g.log = Flixel.log;
      g.game = Flixel.game;
      g.config = Flixel.config;
      g.boot = Flixel.boot;
      g.alert = Flixel.alert;
      g.state = Flixel.state;
      g.currentStateFactory = Flixel.currentStateFactory;
      g.keys = Flixel.keys;
      g.sound = Flixel.sound;
      g.assets = Flixel.assets;
      g.debug = Flixel.debug;
      g.save = Flixel.save;
      g.locale = Flixel.locale;
      g.mouse = Flixel.mouse;
      g.touches = Flixel.touches;
      g.gamepads = Flixel.gamepads;
      g.window = Flixel.window;
      g.graphics = Flixel.graphics;
      g.render = Flixel.render;
      g.cameraManager = Flixel.cameraManager;
      g.overlay = Flixel.overlay;
      g.host = Flixel.host;
      g.haptics = Flixel.haptics;
      g.profiler = Flixel.profiler;
      g.input = Flixel.input;
      g.files = Flixel.files;
      g.runtime = Flixel.runtime;
      g.cameras = new FlixelArray<>(FlixelCamera[]::new);
      g.cameras.addAll(Flixel.cameras);
      g.drawCamera = Flixel.getDrawCamera();
      g.saveHandler = FlixelSave.defaultHandler;
      g.debugTransport = FlixelDebugManager.defaultTransport;
      g.debugEndpoint = FlixelDebugManager.defaultEndpoint;
      g.applyAntialiasingOnStateAdd = Flixel.applyAntialiasingOnStateAdd;
      g.autoPause = Flixel.autoPause;
      return g;
    }

    void restore() {
      Flixel.timeScale = timeScale;
      Flixel.log = log;
      Flixel.game = game;
      Flixel.config = config;
      Flixel.boot = boot;
      Flixel.alert = alert;
      Flixel.state = state;
      Flixel.currentStateFactory = currentStateFactory;
      Flixel.keys = keys;
      Flixel.sound = sound;
      Flixel.assets = assets;
      Flixel.debug = debug;
      Flixel.save = save;
      Flixel.locale = locale;
      Flixel.mouse = mouse;
      Flixel.touches = touches;
      Flixel.gamepads = gamepads;
      Flixel.window = window;
      Flixel.graphics = graphics;
      Flixel.render = render;
      Flixel.cameraManager = cameraManager;
      Flixel.overlay = overlay;
      Flixel.host = host;
      Flixel.haptics = haptics;
      Flixel.profiler = profiler;
      Flixel.input = input;
      Flixel.files = files;
      Flixel.runtime = runtime;
      Flixel.cameras.clear();
      Flixel.cameras.addAll(cameras);
      Flixel.setDrawCamera(drawCamera);
      FlixelSave.defaultHandler = saveHandler;
      FlixelDebugManager.defaultTransport = debugTransport;
      FlixelDebugManager.defaultEndpoint = debugEndpoint;
      Flixel.applyAntialiasingOnStateAdd = applyAntialiasingOnStateAdd;
      Flixel.autoPause = autoPause;
    }
  }
}
