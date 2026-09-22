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
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.desktop.graphics.FlixelBgfxGraphics;
import org.flixelgdx.backend.desktop.input.FlixelDesktopInputDevice;
import org.flixelgdx.backend.desktop.input.FlixelSdlGamepadProvider;
import org.flixelgdx.backend.desktop.input.FlixelSdlKeyMap;
import org.flixelgdx.backend.desktop.input.FlixelSdlMouseIconManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXInit;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.locks.LockSupport;

/**
 * The desktop main loop: creates the SDL3 window, hands its native handle to bgfx for rendering,
 * then pumps events and drives the game each frame until the window closes.
 *
 * <p>This is the {@link FlixelGameRunner} the desktop launcher installs. It owns everything that
 * only makes sense once a window exists: SDL initialization, bgfx device setup, the per-frame
 * event pump (translated into the input device), and the frame timing passed to the game loop.
 */
public class FlixelDesktopRunner implements FlixelGameRunner {

  private static final long SPIN_MARGIN_NANOS = 1_500_000L;

  private long windowHandle;

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

  /**
   * Resource paths for the window icon set, ordered smallest to largest (for example
   * {@code "icons/icon16.png"}, {@code "icons/icon32.png"}, {@code "icons/icon256.png"}).
   * May be {@code null} when no icons were requested.
   */
  @Nullable
  private final String[] iconPaths;

  private int width;
  private int height;

  private boolean vsync = true;

  /**
   * Creates a new desktop runner wired to the given platform components, with a set of window icons.
   *
   * @param window The SDL window implementation.
   * @param input The desktop input device.
   * @param graphics The bgfx graphics implementation.
   * @param gamepads The SDL gamepad provider.
   * @param iconManager The mouse icon manager.
   * @param host The desktop host integration.
   * @param width Initial window width in pixels.
   * @param height Initial window height in pixels.
   * @param iconPaths Resource paths for the window icons, ordered from smallest to largest.
   *                  Each path is resolved from the Java resources root (for example
   *                  {@code "icons/icon16.png"} loads {@code /icons/icon16.png} from the
   *                  classpath). May be {@code null} to set no icon.
   */
  public FlixelDesktopRunner(@NotNull FlixelSdlWindow window, @NotNull FlixelDesktopInputDevice input,
      @NotNull FlixelBgfxGraphics graphics, @NotNull FlixelSdlGamepadProvider gamepads,
      @NotNull FlixelSdlMouseIconManager iconManager, @NotNull FlixelDesktopHostIntegration host,
      int width, int height, @Nullable String[] iconPaths) {
    this.window = window;
    this.input = input;
    this.graphics = graphics;
    this.gamepads = gamepads;
    this.iconManager = iconManager;
    this.host = host;
    this.width = width;
    this.height = height;
    this.iconPaths = iconPaths;
  }

  @Override
  public void run(@NotNull FlixelGame game) {
    FlixelConfig config = Flixel.config;

    vsync = config.isVsync();
    graphics.setTargetFps(config.getFramerate());

    if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO | SDLInit.SDL_INIT_EVENTS | SDLInit.SDL_INIT_GAMEPAD)) {
      Flixel.error("Desktop", "SDL_Init failed; cannot open a window.");
      return;
    }

    boolean transparentFramebuffer = config.isTransparentFramebuffer();
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
    if (config.isFullscreen()) {
      windowFlags |= SDLVideo.SDL_WINDOW_FULLSCREEN;
    }
    windowHandle = SDLVideo.SDL_CreateWindow(config.getTitle(), width, height, windowFlags);
    if (windowHandle == 0L) {
      Flixel.error("Desktop", "The SDL window could not be created.");
      SDLInit.SDL_Quit();
      return;
    }
    SDLVideo.SDL_SetWindowPosition(windowHandle, SDLVideo.SDL_WINDOWPOS_CENTERED, SDLVideo.SDL_WINDOWPOS_CENTERED);
    window.bind(windowHandle);
    applyWindowIcons(windowHandle);

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

    long lastNanos = System.nanoTime();
    try (SDL_Event event = SDL_Event.malloc()) {
      while (!window.isCloseRequested()) {
        if (pumpEvents(event, game)) {
          break;
        }

        long now = System.nanoTime();
        float deltaSeconds = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        graphics.beginFrame();
        float elapsed = game.advanceTime(deltaSeconds);
        game.update(elapsed);
        game.draw(graphics.getBatch());
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
    int fps = graphics.getTargetFps();
    if (fps <= 0) {
      frameDeadlineNanos = 0L;
      return;
    }
    long targetNanos = 1_000_000_000L / fps;
    long now = System.nanoTime();
    if (frameDeadlineNanos == 0L) {
      frameDeadlineNanos = now;
    }
    frameDeadlineNanos += targetNanos;
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

  /**
   * Loads each icon in {@link #iconPaths} and hands the resulting SDL surfaces to
   * {@code SDL_SetWindowIcon()}.
   *
   * <p>SDL3 only accepts one surface per call, so this iterates over every path. The surface
   * returned by SDL_CreateSurfaceFrom() must stay alive until after SDL_SetWindowIcon() returns,
   * at which point SDL has copied what it needs and the surface can be destroyed. Each iteration
   * therefore follows a create-use-destroy pattern inside the loop.
   *
   * <p>If {@link #iconPaths} is {@code null} or empty the method returns immediately.
   *
   * @param wnd The SDL window handle to apply icons to.
   */
  private void applyWindowIcons(long wnd) {
    if (iconPaths == null) {
      return;
    }
    for (String path : iconPaths) {
      SDL_Surface surface = null;
      try {
        FlixelImage image = loadIconImage(path);
        if (image == null) {
          Flixel.warn("Desktop", "Window icon could not be decoded: " + path);
          continue;
        }
        // SDL_CreateSurfaceFrom requires the pixel buffer to remain valid for the lifetime
        // of the surface. The image owns a Java-managed direct ByteBuffer; we pass it directly.
        ByteBuffer pixels = image.getPixels();
        int w = image.getWidth();
        int h = image.getHeight();
        // stb_image always decodes to RGBA order; SDL_PIXELFORMAT_RGBA32 matches that layout.
        surface = SDLSurface.SDL_CreateSurfaceFrom(w, h, SDLPixels.SDL_PIXELFORMAT_RGBA32, pixels, w * 4);
        if (surface == null) {
          Flixel.warn("Desktop", "SDL_CreateSurfaceFrom failed for icon: " + path);
          continue;
        }
        SDLVideo.SDL_SetWindowIcon(wnd, surface);
      } finally {
        if (surface != null) {
          SDLSurface.SDL_DestroySurface(surface);
        }
      }
    }
  }

  /**
   * Reads a resource by path and decodes it into a {@link FlixelImage}.
   *
   * <p>The path is resolved from the Java classpath root: {@code "icons/icon16.png"} maps to
   * {@code /icons/icon16.png} on the classpath. This matches the convention used by other
   * framework loaders (such as the shader loader in {@code FlixelBgfxGraphics}).
   *
   * @param path The classpath-relative resource path (no leading slash).
   * @return The decoded image, or {@code null} when the resource was not found or could not
   *         be decoded.
   */
  @Nullable
  private FlixelImage loadIconImage(String path) {
    byte[] bytes;
    try (InputStream in = FlixelDesktopRunner.class.getResourceAsStream("/" + path)) {
      if (in == null) {
        return null;
      }
      bytes = in.readAllBytes();
    } catch (IOException e) {
      return null;
    }
    // Wrap the raw bytes in a direct buffer so the decoder can read them natively.
    ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
    try {
      encoded.put(bytes).flip();
      return graphics.decodeImage(encoded);
    } finally {
      MemoryUtil.memFree(encoded);
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
      case SDLEvents.SDL_EVENT_KEY_UP -> input.onKeyUp(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
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
      }
      case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN ->
        input.onMouseDown(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
      case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP ->
        input.onMouseUp(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
      case SDLEvents.SDL_EVENT_MOUSE_MOTION ->
        input.onMouseMoved((int) event.motion().x(), (int) event.motion().y());
      case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> input.onScrolled(event.wheel().x(), event.wheel().y());
      case SDLEvents.SDL_EVENT_GAMEPAD_ADDED -> gamepads.onDeviceAdded(event.gdevice().which());
      case SDLEvents.SDL_EVENT_GAMEPAD_REMOVED -> gamepads.onDeviceRemoved(event.gdevice().which());
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

          // Obtain the display modes of each monitor.
          FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();
          PointerBuffer modesBuf = SDLVideo.SDL_GetFullscreenDisplayModes(displayId);
          if (modesBuf != null) {
            for (int j = 0; j < modesBuf.limit(); j++) {
              SDL_DisplayMode sdlMode = SDL_DisplayMode.create(modesBuf.get(j));
              int bpp = (sdlMode.format() >> 8) & 0xFF;
              displayModes.add(new FlixelDisplayMode(sdlMode.w(), sdlMode.h(), (int) sdlMode.refresh_rate(), bpp));
            }
          }

          FlixelSdlMonitor monitor = new FlixelSdlMonitor(name != null ? name : "Unknown", displayModes,
              bounds.x(), bounds.y(), bounds.w(), bounds.h(), refreshRate, isPrimary);
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
