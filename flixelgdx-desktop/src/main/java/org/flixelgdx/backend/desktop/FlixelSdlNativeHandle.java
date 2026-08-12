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

import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;

/**
 * Extracts the OS-native window and display handles from an SDL3 window so bgfx can render into it.
 *
 * <p>bgfx does not open its own window; it renders into one that already exists. To do that it needs
 * two raw pointers: the native window handle (bgfx calls it {@code nwh}) and, on some systems, the
 * native display handle ({@code ndt}). SDL owns the window, so it is the one that knows these values,
 * and it exposes them through its per-window property store. This class asks SDL which windowing
 * system is active ({@link SDLVideo#SDL_GetCurrentVideoDriver()}) and reads the matching properties.
 *
 * <p>The exact handles differ per platform:
 * <ul>
 *   <li><b>X11 (Linux):</b> the window is an integer XID; the display is a pointer to the X display.</li>
 *   <li><b>Wayland (Linux):</b> both the surface and the display are pointers.</li>
 *   <li><b>Windows:</b> the window is an {@code HWND} pointer; no separate display handle is needed.</li>
 *   <li><b>macOS (Cocoa):</b> the window is an {@code NSWindow} pointer; no separate display handle.</li>
 * </ul>
 */
final class FlixelSdlNativeHandle {

  private FlixelSdlNativeHandle() {}

  /**
   * Returns the native window handle (bgfx {@code nwh}) for the given SDL window.
   *
   * @param sdlWindow The SDL window handle from {@code SDL_CreateWindow}.
   * @return The OS-native window handle, or {@code 0} if it could not be resolved.
   */
  static long windowHandle(long sdlWindow) {
    int props = SDLVideo.SDL_GetWindowProperties(sdlWindow);
    if (props == 0) {
      return 0L;
    }
    String driver = driver();
    if ("x11".equals(driver)) {
      return SDLProperties.SDL_GetNumberProperty(props, SDLVideo.SDL_PROP_WINDOW_X11_WINDOW_NUMBER, 0L);
    }
    if ("wayland".equals(driver)) {
      return SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER, 0L);
    }
    if ("windows".equals(driver)) {
      return SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
    }
    if ("cocoa".equals(driver)) {
      return SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_COCOA_WINDOW_POINTER, 0L);
    }
    return 0L;
  }

  /**
   * Returns the native display handle (bgfx {@code ndt}) for the given SDL window, or {@code 0} on
   * platforms that do not use one (Windows, macOS).
   *
   * @param sdlWindow The SDL window handle from {@code SDL_CreateWindow}.
   * @return The OS-native display handle, or {@code 0} when none applies.
   */
  static long displayHandle(long sdlWindow) {
    int props = SDLVideo.SDL_GetWindowProperties(sdlWindow);
    if (props == 0) {
      return 0L;
    }
    String driver = driver();
    if ("x11".equals(driver)) {
      return SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_X11_DISPLAY_POINTER, 0L);
    }
    if ("wayland".equals(driver)) {
      return SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER, 0L);
    }
    return 0L;
  }

  /** Returns the active SDL video driver name (for example {@code "x11"}), or an empty string. */
  private static String driver() {
    String driver = SDLVideo.SDL_GetCurrentVideoDriver();
    return driver != null ? driver : "";
  }
}
