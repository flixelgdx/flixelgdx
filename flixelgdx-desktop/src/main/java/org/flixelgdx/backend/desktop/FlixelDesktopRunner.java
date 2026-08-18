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
import org.jetbrains.annotations.NotNull;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.concurrent.locks.LockSupport;

/**
 * The desktop main loop: creates the SDL3 window, hands its native handle to bgfx for rendering,
 * then pumps events and drives the game each frame until the window closes.
 *
 * <p>This is the {@link FlixelGameRunner} the desktop launcher installs. It owns everything that
 * only makes sense once a window exists: SDL initialization, bgfx device setup, the per-frame
 * event pump (translated into the input device), and the frame timing passed to
 * {@link FlixelGame#render(float)}.
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

  private int width;
  private int height;

  private boolean vsync = true;

  /**
   * Creates a runner wired to the desktop backend objects the launcher installed.
   *
   * @param window The window wrapper to bind to the created SDL window.
   * @param input The input device to feed SDL events into.
   * @param graphics The bgfx graphics manager to initialize.
   * @param gamepads The gamepad provider to open devices on and feed connect events into.
   * @param iconManager The SDL cursor manager to dispose before SDL shuts down.
   * @param width The initial window width in pixels.
   * @param height The initial window height in pixels.
   */
  public FlixelDesktopRunner(@NotNull FlixelSdlWindow window, @NotNull FlixelDesktopInputDevice input,
      @NotNull FlixelBgfxGraphics graphics, @NotNull FlixelSdlGamepadProvider gamepads,
      @NotNull FlixelSdlMouseIconManager iconManager, int width, int height) {
    this.window = window;
    this.input = input;
    this.graphics = graphics;
    this.gamepads = gamepads;
    this.iconManager = iconManager;
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

    long windowFlags = SDLVideo.SDL_WINDOW_RESIZABLE;
    if (game.getConfig().isTransparentFramebuffer()) {
      windowFlags |= SDLVideo.SDL_WINDOW_TRANSPARENT;
    }
    windowHandle = SDLVideo.SDL_CreateWindow(game.getTitle(), width, height, windowFlags);
    if (windowHandle == 0L) {
      Flixel.error("Desktop", "The SDL window could not be created.");
      SDLInit.SDL_Quit();
      return;
    }
    SDLVideo.SDL_SetWindowPosition(windowHandle, SDLVideo.SDL_WINDOWPOS_CENTERED, SDLVideo.SDL_WINDOWPOS_CENTERED);
    window.bind(windowHandle);

    if (!initBgfx(windowHandle)) {
      SDLVideo.SDL_DestroyWindow(windowHandle);
      SDLInit.SDL_Quit();
      return;
    }

    graphics.onInitialized(width, height);
    graphics.setVSync(vsync);
    gamepads.openConnected();

    game.create();

    long lastNanos = System.nanoTime();
    try (SDL_Event event = SDL_Event.malloc()) {
      while (!window.isCloseRequested()) {
        boolean quit = pumpEvents(event, game);
        if (quit) {
          break;
        }

        if (!window.isContinuousRendering() && !window.consumeRenderRequest()) {
          Thread.yield();
          continue;
        }

        long now = System.nanoTime();
        float deltaSeconds = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        graphics.beginFrame();
        game.render(deltaSeconds);
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
  private boolean initBgfx(long windowHandle) {
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
      init.resolution(
          res -> res.width(width).height(height).reset(resetFlags).formatColor(BGFX.BGFX_TEXTURE_FORMAT_RGBA8));
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
    switch (backend) {
      case "auto", "" -> {
        return BGFX.BGFX_RENDERER_TYPE_COUNT; // Let bgfx auto-pick the best backend.
      }
      case "opengl", "gl" -> {
        return BGFX.BGFX_RENDERER_TYPE_OPENGL;
      }
      case "vulkan", "vk" -> {
        return BGFX.BGFX_RENDERER_TYPE_VULKAN;
      }
      case "metal" -> {
        return BGFX.BGFX_RENDERER_TYPE_METAL;
      }
      case "direct3d11", "d3d11" -> {
        return BGFX.BGFX_RENDERER_TYPE_DIRECT3D11;
      }
      case "direct3d12", "d3d12" -> {
        return BGFX.BGFX_RENDERER_TYPE_DIRECT3D12;
      }
      case "noop" -> {
        return BGFX.BGFX_RENDERER_TYPE_NOOP;
      }
      default -> {
        Flixel.warn("Desktop", "Unknown flixel.render.backend '" + backend
            + "'; letting bgfx auto-pick the renderer.");
        return BGFX.BGFX_RENDERER_TYPE_COUNT;
      }
    }
  }

  /**
   * Drains all pending SDL events, translating them into input-device calls and lifecycle hooks.
   *
   * @return {@code true} when a quit was requested.
   */
  private boolean pumpEvents(@NotNull SDL_Event event, @NotNull FlixelGame game) {
    while (SDLEvents.SDL_PollEvent(event)) {
      switch (event.type()) {
        case SDLEvents.SDL_EVENT_QUIT -> {
          if (!window.isAbsorbCloseRequests()) {
            return true;
          }
        }
        case SDLEvents.SDL_EVENT_WINDOW_MOVED ->
          window.onMoved(event.window().data1(), event.window().data2());
        case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED,
            SDLEvents.SDL_EVENT_WINDOW_RESIZED ->
          handleResize(game);
        case SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST -> game.onFocusLost();
        case SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED -> game.onFocusGained();
        case SDLEvents.SDL_EVENT_WINDOW_MINIMIZED -> game.onMinimized();
        case SDLEvents.SDL_EVENT_KEY_DOWN -> {
          if (!event.key().repeat()) {
            input.onKeyDown(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
          }
        }
        case SDLEvents.SDL_EVENT_KEY_UP ->
          input.onKeyUp(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
        case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN ->
          input.onMouseDown(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
        case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP ->
          input.onMouseUp(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
        case SDLEvents.SDL_EVENT_MOUSE_MOTION ->
          input.onMouseMoved((int) event.motion().x(), (int) event.motion().y());
        case SDLEvents.SDL_EVENT_MOUSE_WHEEL ->
          input.onScrolled(event.wheel().x(), event.wheel().y());
        case SDLEvents.SDL_EVENT_GAMEPAD_ADDED -> gamepads.onDeviceAdded(event.gdevice().which());
        case SDLEvents.SDL_EVENT_GAMEPAD_REMOVED -> gamepads.onDeviceRemoved(event.gdevice().which());
        default -> {
        }
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
  private static int mouseButton(int sdlButton) {
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
