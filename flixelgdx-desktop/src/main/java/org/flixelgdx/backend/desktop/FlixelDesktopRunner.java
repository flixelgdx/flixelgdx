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
import org.flixelgdx.backend.desktop.input.FlixelSdlKeyMap;
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

/**
 * The desktop main loop: creates the SDL3 window, hands its native handle to bgfx for rendering,
 * then pumps events and drives the game each frame until the window closes.
 *
 * <p>This is the {@link FlixelGameRunner} the desktop launcher installs. It owns everything that
 * only makes sense once a window exists: SDL initialization, bgfx device setup, the per-frame
 * event pump (translated into the input device), and the frame timing passed to
 * {@link FlixelGame#render(float)}.
 */
public final class FlixelDesktopRunner implements FlixelGameRunner {

  @NotNull
  private final FlixelSdlWindow window;

  @NotNull
  private final FlixelDesktopInputDevice input;

  @NotNull
  private final FlixelBgfxGraphics graphics;

  private long windowHandle;
  private int width;
  private int height;

  /**
   * Creates a runner wired to the desktop backend objects the launcher installed.
   *
   * @param window The window wrapper to bind to the created SDL window.
   * @param input The input device to feed SDL events into.
   * @param graphics The bgfx graphics manager to initialize.
   * @param width The initial window width in pixels.
   * @param height The initial window height in pixels.
   */
  public FlixelDesktopRunner(@NotNull FlixelSdlWindow window, @NotNull FlixelDesktopInputDevice input,
      @NotNull FlixelBgfxGraphics graphics, int width, int height) {
    this.window = window;
    this.input = input;
    this.graphics = graphics;
    this.width = width;
    this.height = height;
  }

  @Override
  public void run(@NotNull FlixelGame game) {
    if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO | SDLInit.SDL_INIT_EVENTS | SDLInit.SDL_INIT_GAMEPAD)) {
      Flixel.error("Desktop", "SDL_Init failed; cannot open a window.");
      return;
    }

    windowHandle = SDLVideo.SDL_CreateWindow(game.getTitle(), width, height, SDLVideo.SDL_WINDOW_RESIZABLE);
    if (windowHandle == 0L) {
      Flixel.error("Desktop", "The SDL window could not be created.");
      SDLInit.SDL_Quit();
      return;
    }
    window.bind(windowHandle);

    if (!initBgfx(windowHandle)) {
      SDLVideo.SDL_DestroyWindow(windowHandle);
      SDLInit.SDL_Quit();
      return;
    }
    graphics.onInitialized(width, height);

    game.create();

    long lastNanos = System.nanoTime();
    try (SDL_Event event = SDL_Event.malloc()) {
      while (!window.isCloseRequested()) {
        boolean quit = pumpEvents(event, game);
        if (quit) {
          break;
        }

        long now = System.nanoTime();
        float deltaSeconds = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        graphics.beginFrame();
        game.render(deltaSeconds);
        graphics.endFrame();
      }
    }

    game.destroy();
    BGFX.bgfx_shutdown();
    SDLVideo.SDL_DestroyWindow(windowHandle);
    SDLInit.SDL_Quit();
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
      init.type(BGFX.BGFX_RENDERER_TYPE_COUNT); // Let bgfx auto-pick the best backend.
      init.resolution(res -> res.width(width).height(height).reset(BGFX.BGFX_RESET_VSYNC));
      init.platformData(pd -> pd.nwh(nativeWindow).ndt(nativeDisplay));
      if (!BGFX.bgfx_init(init)) {
        Flixel.error("Desktop", "bgfx could not be initialized.");
        return false;
      }
    }
    return true;
  }

  /**
   * Drains all pending SDL events, translating them into input-device calls and lifecycle hooks.
   *
   * @return {@code true} when a quit was requested.
   */
  private boolean pumpEvents(@NotNull SDL_Event event, @NotNull FlixelGame game) {
    while (SDLEvents.SDL_PollEvent(event)) {
      int type = event.type();
      if (type == SDLEvents.SDL_EVENT_QUIT) {
        return true;
      } else if (type == SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED
          || type == SDLEvents.SDL_EVENT_WINDOW_RESIZED) {
        handleResize(game);
      } else if (type == SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST) {
        game.onFocusLost();
      } else if (type == SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED) {
        game.onFocusGained();
      } else if (type == SDLEvents.SDL_EVENT_WINDOW_MINIMIZED) {
        game.onMinimized();
      } else if (type == SDLEvents.SDL_EVENT_KEY_DOWN) {
        if (!event.key().repeat()) {
          input.onKeyDown(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
        }
      } else if (type == SDLEvents.SDL_EVENT_KEY_UP) {
        input.onKeyUp(FlixelSdlKeyMap.toFlixelKey(event.key().scancode()));
      } else if (type == SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN) {
        input.onMouseDown(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
      } else if (type == SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP) {
        input.onMouseUp(mouseButton(event.button().button()), (int) event.button().x(), (int) event.button().y());
      } else if (type == SDLEvents.SDL_EVENT_MOUSE_MOTION) {
        input.onMouseMoved((int) event.motion().x(), (int) event.motion().y());
      } else if (type == SDLEvents.SDL_EVENT_MOUSE_WHEEL) {
        input.onScrolled(event.wheel().x(), event.wheel().y());
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
}
