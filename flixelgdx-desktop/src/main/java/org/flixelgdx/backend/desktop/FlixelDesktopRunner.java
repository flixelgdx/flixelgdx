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
package org.flixelgdx.backend.desktop;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.desktop.graphics.FlixelBgfxGraphics;
import org.flixelgdx.backend.desktop.input.FlixelDesktopInputDevice;
import org.flixelgdx.backend.desktop.input.FlixelSdlGamepadProvider;
import org.flixelgdx.backend.desktop.input.FlixelSdlKeyMap;
import org.flixelgdx.backend.desktop.input.FlixelSdlMouseIconManager;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.concurrent.locks.LockSupport;

/**
 * The desktop main loop: creates the SDL3 window, hands its native handle to bgfx for rendering,
 * then pumps events and drives the game each frame until the window closes.
 *
 * <p>This is the {@link FlixelGameRunner} the desktop launcher installs. It owns everything that
 * only makes sense once a window exists: SDL initialization, bgfx device setup, the per-frame
 * event pump (translated into the input device), and the frame timing passed to the game loop.
 * In continuous mode the loop polls SDL events every frame; in non-continuous mode it blocks on
 * {@code SDL_WaitEvent} so the thread is parked rather than spinning while the game is idle.
 */
public class FlixelDesktopRunner implements FlixelGameRunner {

  private static final long SPIN_MARGIN_NANOS = 1_500_000L;

  private long windowHandle;

  /** Minimum nanoseconds per frame derived from the game's framerate, or {@code 0} for uncapped. */
  private long targetFrameNanos;

  /** Absolute {@link System#nanoTime()} target the current frame should not finish before. */
  private long frameDeadlineNanos;

  @NotNull
  private final FlixelSdlWindow window;

  @NotNull
  private final FlixelDesktopInputDevice input;

  @NotNull
  private final FlixelBgfxGraphics graphics;

  @NotNull
  private final FlixelSdlGamepadProvider gamepads;

  @NotNull
  private final FlixelSdlMouseIconManager iconManager;

  @NotNull
  private final FlixelDesktopHostIntegration host;

  private int width;
  private int height;

  private boolean vsync = true;

  /**
   * Creates a new desktop runner wired to the given platform components.
   *
   * @param window The SDL window implementation.
   * @param input The desktop input device.
   * @param graphics The bgfx graphics implementation.
   * @param gamepads The SDL gamepad provider.
   * @param iconManager The mouse icon manager.
   * @param host The desktop host integration.
   * @param width Initial window width in pixels.
   * @param height Initial window height in pixels.
   */
  public FlixelDesktopRunner(@NotNull FlixelSdlWindow window, @NotNull FlixelDesktopInputDevice input,
      @NotNull FlixelBgfxGraphics graphics, @NotNull FlixelSdlGamepadProvider gamepads,
      @NotNull FlixelSdlMouseIconManager iconManager, @NotNull FlixelDesktopHostIntegration host,
      int width, int height) {
    this.window = window;
    this.input = input;
    this.graphics = graphics;
    this.gamepads = gamepads;
    this.iconManager = iconManager;
    this.host = host;
    this.width = width;
    this.height = height;
  }

  @Override
  public void run(@NotNull FlixelGame game) {
    vsync = game.isVsync();
    int framerate = game.getFramerate();
    targetFrameNanos = framerate > 0 ? 1_000_000_000L / framerate : 0L;

    if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO | SDLInit.SDL_INIT_EVENTS | SDLInit.SDL_INIT_GAMEPAD)) {
      Flixel.error("Desktop", "SDL_Init failed; cannot open a window.");
      return;
    }

    boolean transparentFramebuffer = game.getConfig().isTransparentFramebuffer();
    long windowFlags = SDLVideo.SDL_WINDOW_RESIZABLE;
    if (transparentFramebuffer) {
      windowFlags |= SDLVideo.SDL_WINDOW_TRANSPARENT;
      // On X11, SDL3 uses XMatchVisualInfo for the window visual when no OpenGL flag is
      // present, which may return a visual that is not in the GLX visual list. bgfx then
      // cannot create a compatible alpha-capable context, so the compositor sees a 24-bit
      // window and renders transparent areas as black. Adding SDL_WINDOW_OPENGL forces SDL3
      // to use glXChooseFBConfig (with GLX_ALPHA_SIZE=8) for visual selection, giving bgfx
      // a 32-bit RGBA-compatible window without SDL3 creating any GL context of its own.
      //
      // This is temporary, as I need to battle test the framework on different hardware.
      // Linux is so fucking annoying to deal with bro. :wilted_flower:
      String driver = SDLVideo.SDL_GetCurrentVideoDriver();
      if ("x11".equals(driver)) {
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_ALPHA_SIZE, 8);
        windowFlags |= SDLVideo.SDL_WINDOW_OPENGL;
      }
    }
    windowHandle = SDLVideo.SDL_CreateWindow(game.getTitle(), width, height, windowFlags);
    if (windowHandle == 0L) {
      Flixel.error("Desktop", "The SDL window could not be created.");
      SDLInit.SDL_Quit();
      return;
    }
    SDLVideo.SDL_SetWindowPosition(windowHandle, SDLVideo.SDL_WINDOWPOS_CENTERED, SDLVideo.SDL_WINDOWPOS_CENTERED);
    window.bind(windowHandle);

    if (!initBgfx(windowHandle, transparentFramebuffer)) {
      SDLVideo.SDL_DestroyWindow(windowHandle);
      SDLInit.SDL_Quit();
      return;
    }

    graphics.onInitialized(width, height, transparentFramebuffer);
    graphics.setVSync(vsync);
    gamepads.openConnected();

    // The debug overlay's command line needs SDL text-input events (which carry composed characters,
    // separate from raw key events). Only debug builds have that overlay, so keep text input off
    // otherwise to avoid triggering an IME where it is not wanted.
    if (Flixel.isDebugMode()) {
      SDLKeyboard.SDL_StartTextInput(windowHandle);
    }

    refreshMonitors(); // Fill in the monitors at startup.
    game.create();
    // Ensure at least one frame is drawn immediately after create, even when the game switches to
    // non-continuous rendering inside its create() call.
    graphics.requestRendering();

    long lastNanos = System.nanoTime();
    try (SDL_Event event = SDL_Event.malloc()) {
      while (!window.isCloseRequested()) {
        boolean quit;
        if (graphics.isContinuousRendering()) {
          quit = pumpEvents(event, game);
        } else {
          // Block the thread until SDL delivers any event. This eliminates the busy-spin
          // that would otherwise burn CPU while the game is unfocused and not updating.
          SDLEvents.SDL_WaitEvent(event);
          quit = dispatchEvent(event, game);
          if (!quit) {
            quit = pumpEvents(event, game);
          }
          // lastNanos is intentionally NOT reset here. MAX_ELAPSED clamps the first-frame delta
          // spike from a long park, and subsequent frames need the real elapsed time so that
          // animations and movement advance at the correct rate.
          //
          // Only draw when a frame was explicitly requested. Any SDL event wakes SDL_WaitEvent,
          // but most (mouse motion, window moves, display notifications) should not cause a
          // render. Input events and explicit requestRendering() calls set the flag via
          // dispatchEvent or game code; everything else just loops back to sleep.
          if (!quit && !graphics.consumeRenderRequest()) {
            continue;
          }
        }

        if (quit) {
          break;
        }

        long now = System.nanoTime();
        float deltaSeconds = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        graphics.beginFrame();
        float elapsed = game.advanceTime(deltaSeconds);
        game.update(elapsed);
        game.draw(game.getBatch());
        game.endFrame();
        graphics.endFrame();

        limitFrameRate();
      }
    }

    game.destroy();
    gamepads.dispose();
    iconManager.dispose();
    BGFX.bgfx_shutdown();
    SDLVideo.SDL_DestroyWindow(windowHandle);
    SDLInit.SDL_Quit();
  }

  /**
   * Waits until the game's target frame period has elapsed, holding the frame rate at the cap.
   * Does nothing when the framerate is uncapped.
   *
   * <p>The cap is paced against an absolute deadline that advances by exactly one frame period each
   * call, rather than by measuring elapsed time and sleeping the remainder. That keeps rounding
   * error from accumulating, so the average rate lands on the target instead of drifting below it.
   * The wait itself is a hybrid: it sleeps ({@link LockSupport#parkNanos}) for the bulk of the
   * remaining time, then busy-spins the final {@link #SPIN_MARGIN_NANOS} to absorb the scheduler's
   * late wakeups. If a frame runs long enough to miss the deadline entirely, the deadline resyncs to
   * now so the limiter does not then rush a burst of catch-up frames.
   */
  private void limitFrameRate() {
    if (targetFrameNanos <= 0L) {
      return;
    }
    long now = System.nanoTime();
    if (frameDeadlineNanos == 0L) {
      frameDeadlineNanos = now;
    }
    frameDeadlineNanos += targetFrameNanos;
    if (frameDeadlineNanos <= now) {
      // We are already past the deadline (a slow frame); resync and render the next one immediately.
      frameDeadlineNanos = now;
      return;
    }
    waitUntil(frameDeadlineNanos);
  }

  /**
   * Blocks the calling thread until {@link System#nanoTime()} reaches {@code deadlineNanos}, sleeping
   * most of the way and spinning the last {@link #SPIN_MARGIN_NANOS} for precision.
   *
   * @param deadlineNanos The {@link System#nanoTime()} timestamp to wait for.
   */
  private static void waitUntil(long deadlineNanos) {
    long remaining = deadlineNanos - System.nanoTime();
    while (remaining > SPIN_MARGIN_NANOS) {
      LockSupport.parkNanos(remaining - SPIN_MARGIN_NANOS);
      remaining = deadlineNanos - System.nanoTime();
    }
    while (System.nanoTime() < deadlineNanos) {
      Thread.onSpinWait();
    }
  }

  /** Initializes bgfx with the SDL window's native handle. */
  private boolean initBgfx(long windowHandle, boolean transparentFramebuffer) {
    long nativeWindow = FlixelSdlNativeHandle.windowHandle(windowHandle);
    long nativeDisplay = FlixelSdlNativeHandle.displayHandle(windowHandle);
    if (nativeWindow == 0L) {
      Flixel.error("Desktop", "Could not resolve the native window handle for bgfx.");
      return false;
    }
    try (BGFXInit init = BGFXInit.calloc()) {
      BGFX.bgfx_init_ctor(init);
      init.type(resolveRendererType());
      int resetFlags = vsync ? BGFX.BGFX_RESET_VSYNC : BGFX.BGFX_RESET_NONE;
      if (transparentFramebuffer) {
        resetFlags |= BGFX.BGFX_RESET_TRANSPARENT_BACKBUFFER;
      }
      int finalResetFlags = resetFlags;
      init.resolution(
          res -> res.width(width).height(height).reset(finalResetFlags).formatColor(BGFX.BGFX_TEXTURE_FORMAT_RGBA8));
      init.platformData(pd -> pd.nwh(nativeWindow).ndt(nativeDisplay));
      if (!BGFX.bgfx_init(init)) {
        Flixel.error("Desktop", "bgfx could not be initialized.");
        return false;
      }
    }
    return true;
  }

  /**
   * Resolves the bgfx renderer backend to request at initialization.
   *
   * <p>By default, bgfx auto-picks the best backend for the platform. Some systems have a driver
   * that bgfx would prefer but that crashes or misbehaves (a common example is Mesa's Intel Vulkan
   * driver on Linux). The {@code flixel.render.backend} system property forces a specific backend
   * so those machines can fall back to a working one without a code change, for example:
   *
   * <pre>{@code
   * java -Dflixel.render.backend=opengl -jar mygame.jar
   * }</pre>
   *
   * <p>Recognized values are {@code auto} (the default), {@code opengl}, {@code vulkan},
   * {@code metal}, {@code direct3d11}, {@code direct3d12}, and {@code noop}. An unrecognized value
   * is ignored with a warning and auto-selection is used.
   *
   * @return The bgfx renderer type constant to hand to {@link BGFXInit#type(int)}.
   */
  private static int resolveRendererType() {
    String backend = System.getProperty("flixel.render.backend", "auto").trim().toLowerCase();
    if (backend.isEmpty() || backend.equals("auto")) {
      return BGFX.BGFX_RENDERER_TYPE_COUNT; // Let bgfx auto-pick the best backend.
    }
    if (backend.equals(FlixelGraphicsApi.OpenGL.getId().toLowerCase())) {
      return BGFX.BGFX_RENDERER_TYPE_OPENGL;
    }
    if (backend.equals(FlixelGraphicsApi.Vulkan.getId().toLowerCase())) {
      return BGFX.BGFX_RENDERER_TYPE_VULKAN;
    }
    if (backend.equals(FlixelGraphicsApi.Metal.getId().toLowerCase())) {
      return BGFX.BGFX_RENDERER_TYPE_METAL;
    }
    if (backend.equals(FlixelGraphicsApi.Direct3D11.getId().toLowerCase())) {
      return BGFX.BGFX_RENDERER_TYPE_DIRECT3D11;
    }
    if (backend.equals(FlixelGraphicsApi.Direct3D12.getId().toLowerCase())) {
      return BGFX.BGFX_RENDERER_TYPE_DIRECT3D12;
    }
    if (backend.equals(FlixelGraphicsApi.Noop.getId().toLowerCase())) {
      return BGFX.BGFX_RENDERER_TYPE_NOOP;
    }
    Flixel.warn("Desktop", "Unknown flixel.render.backend '" + backend
        + "'; letting bgfx auto-pick the renderer.");
    return BGFX.BGFX_RENDERER_TYPE_COUNT;
  }

  /**
   * Drains all pending SDL events by polling, translating each one into input-device calls and
   * lifecycle hooks. Used in continuous-rendering mode; for the blocking non-continuous path see
   * the main loop which calls {@link #dispatchEvent} after {@code SDL_WaitEvent}.
   *
   * @return {@code true} when a quit was requested.
   */
  private boolean pumpEvents(@NotNull SDL_Event event, @NotNull FlixelGame game) {
    while (SDLEvents.SDL_PollEvent(event)) {
      if (dispatchEvent(event, game)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Translates a single SDL event into the appropriate input-device call or lifecycle hook.
   *
   * @return {@code true} when the event signals a quit.
   */
  private boolean dispatchEvent(@NotNull SDL_Event event, @NotNull FlixelGame game) {
    switch (event.type()) {
      case SDLEvents.SDL_EVENT_QUIT -> {
        if (!window.isAbsorbCloseRequests()) {
          return true;
        }
      }
      case SDLEvents.SDL_EVENT_WINDOW_MOVED ->
        window.onMoved(event.window().data1(), event.window().data2());
      case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED,
          SDLEvents.SDL_EVENT_WINDOW_RESIZED -> {
        handleResize(game);
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST -> game.onFocusLost();
      case SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED -> game.onFocusGained();
      case SDLEvents.SDL_EVENT_WINDOW_MINIMIZED -> game.onMinimized();
      case SDLEvents.SDL_EVENT_KEY_DOWN -> {
        if (!event.key().repeat()) {
          input.onKeyDown(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
          graphics.requestRendering();
        }
      }
      case SDLEvents.SDL_EVENT_KEY_UP -> {
        input.onKeyUp(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_TEXT_INPUT -> {
        // Composed text (letters, punctuation, IME output) arrives here as UTF-8, separate from the
        // physical key events above. Feed each character to listeners so the debug command line and
        // any future text fields can read typed input.
        String textInput = event.text().textString();
        if (textInput != null) {
          for (int i = 0; i < textInput.length(); i++) {
            input.onKeyTyped(textInput.charAt(i));
          }
        }
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN -> {
        input.onMouseDown(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP -> {
        input.onMouseUp(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_MOUSE_MOTION ->
        input.onMouseMoved((int) event.motion().x(), (int) event.motion().y());
      case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> {
        input.onScrolled(event.wheel().x(), event.wheel().y());
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_GAMEPAD_ADDED -> {
        gamepads.onDeviceAdded(event.gdevice().which());
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_GAMEPAD_REMOVED -> {
        gamepads.onDeviceRemoved(event.gdevice().which());
        graphics.requestRendering();
      }
      case SDLEvents.SDL_EVENT_DISPLAY_ADDED,
          SDLEvents.SDL_EVENT_DISPLAY_REMOVED,
          SDLEvents.SDL_EVENT_DISPLAY_MOVED,
          SDLEvents.SDL_EVENT_DISPLAY_CURRENT_MODE_CHANGED ->
        refreshMonitors();
      default -> {
      }
    }
    return false;
  }

  private void handleResize(@NotNull FlixelGame game) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer w = stack.mallocInt(1);
      IntBuffer h = stack.mallocInt(1);
      SDLVideo.SDL_GetWindowSizeInPixels(windowHandle, w, h);
      width = Math.max(1, w.get(0));
      height = Math.max(1, h.get(0));
    }
    graphics.onResize(width, height);
    game.resize(width, height);
  }

  /** Maps an SDL mouse button (1=left, 2=middle, 3=right) to the framework's 0-based index. */
  private int mouseButton(int sdlButton) {
    if (sdlButton == SDLMouse.SDL_BUTTON_LEFT) {
      return 0;
    }
    if (sdlButton == SDLMouse.SDL_BUTTON_RIGHT) {
      return 1;
    }
    if (sdlButton == SDLMouse.SDL_BUTTON_MIDDLE) {
      return 2;
    }
    return Math.max(0, sdlButton - 1);
  }

  private void refreshMonitors() {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      host.monitors.clear();

      IntBuffer displaysBuf = SDLVideo.SDL_GetDisplays();

      if (displaysBuf != null) {
        int monitorCount = displaysBuf.limit();
        int primaryId = SDLVideo.SDL_GetPrimaryDisplay();

        for (int i = 0; i < monitorCount; i++) {
          int displayId = displaysBuf.get(i);
          String name = SDLVideo.SDL_GetDisplayName(displayId);
          boolean isPrimary = (displayId == primaryId);

          SDL_Rect bounds = SDL_Rect.malloc(stack);
          SDLVideo.SDL_GetDisplayBounds(displayId, bounds);

          SDL_DisplayMode mode = SDLVideo.SDL_GetCurrentDisplayMode(displayId);
          float refreshRate = mode != null ? mode.refresh_rate() : 0.0f;

          FlixelSdlMonitor monitor = new FlixelSdlMonitor(name != null ? name : "Unknown", bounds.x(), bounds.y(),
              bounds.w(), bounds.h(), refreshRate, isPrimary);
          host.monitors.add(monitor);
        }
      }
    }
  }

  @NotNull
  public FlixelSdlWindow getWindow() {
    return window;
  }

  @NotNull
  public FlixelDesktopInputDevice getInput() {
    return input;
  }

  @NotNull
  public FlixelBgfxGraphics getGraphics() {
    return graphics;
  }

  @NotNull
  public FlixelSdlGamepadProvider getGamepads() {
    return gamepads;
  }

  @NotNull
  public FlixelSdlMouseIconManager getIconManager() {
    return iconManager;
  }

  @NotNull
  public FlixelDesktopHostIntegration getHost() {
    return host;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isVsync() {
    return vsync;
  }
}
