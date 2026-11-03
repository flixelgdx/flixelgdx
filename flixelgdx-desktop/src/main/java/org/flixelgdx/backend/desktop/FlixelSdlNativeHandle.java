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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLMetal;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;

import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.GpuSurface;
import io.github.stringdotjar.jwebgpu.GpuSurfaceDescriptor;
import io.github.stringdotjar.jwebgpu.GpuSurfaceSourceMetalLayer;
import io.github.stringdotjar.jwebgpu.GpuSurfaceSourceWaylandSurface;
import io.github.stringdotjar.jwebgpu.GpuSurfaceSourceWindowsHWND;
import io.github.stringdotjar.jwebgpu.GpuSurfaceSourceXlibWindow;

/**
 * Turns an SDL window into a WebGPU surface by reading the window's native handles.
 *
 * <p>WebGPU draws to a native window through a "surface source" that names the platform's own window objects.
 * SDL3 exposes those objects as window properties, and this class picks the right pair for the video driver SDL
 * chose: an X11 display and window, a Wayland display and surface, a Win32 window and instance, or the
 * {@code CAMetalLayer} of a Metal view on macOS.
 *
 * <p>On macOS the window must have been created with {@code SDL_WINDOW_METAL}, because SDL only hands out Metal
 * views to such windows.
 */
final class FlixelSdlNativeHandle {

  private long metalView;

  FlixelSdlNativeHandle() {}

  /**
   * Creates the WebGPU surface for a window.
   *
   * @param instance The WebGPU instance that owns the surface.
   * @param sdlWindow The SDL window handle.
   * @return The surface, or {@code null} when the window system is not supported or its handles could not be
   *     read.
   */
  @Nullable
  GpuSurface createSurface(@NotNull GpuInstance instance, long sdlWindow) {
    int props = SDLVideo.SDL_GetWindowProperties(sdlWindow);
    if (props == 0) {
      return null;
    }
    GpuSurfaceDescriptor descriptor = new GpuSurfaceDescriptor();
    descriptor.label = "flixel window";
    String driver = driver();
    switch (driver) {
      case "x11" -> {
        GpuSurfaceSourceXlibWindow source = new GpuSurfaceSourceXlibWindow();
        source.display = SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_X11_DISPLAY_POINTER, 0L);
        source.window = SDLProperties.SDL_GetNumberProperty(props, SDLVideo.SDL_PROP_WINDOW_X11_WINDOW_NUMBER, 0L);
        if (source.display == 0L || source.window == 0L) {
          return null;
        }
        descriptor.nextInChain = source;
      }
      case "wayland" -> {
        GpuSurfaceSourceWaylandSurface source = new GpuSurfaceSourceWaylandSurface();
        source.display = SDLProperties.SDL_GetPointerProperty(props,
            SDLVideo.SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER, 0L);
        source.surface = SDLProperties.SDL_GetPointerProperty(props,
            SDLVideo.SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER, 0L);
        if (source.display == 0L || source.surface == 0L) {
          return null;
        }
        descriptor.nextInChain = source;
      }
      case "windows" -> {
        GpuSurfaceSourceWindowsHWND source = new GpuSurfaceSourceWindowsHWND();
        source.hwnd = SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
        source.hinstance = SDLProperties.SDL_GetPointerProperty(props,
            SDLVideo.SDL_PROP_WINDOW_WIN32_INSTANCE_POINTER, 0L);
        if (source.hwnd == 0L) {
          return null;
        }
        descriptor.nextInChain = source;
      }
      case "cocoa" -> {
        metalView = SDLMetal.SDL_Metal_CreateView(sdlWindow);
        long layer = metalView != 0L ? SDLMetal.SDL_Metal_GetLayer(metalView) : 0L;
        if (layer == 0L) {
          return null;
        }
        GpuSurfaceSourceMetalLayer source = new GpuSurfaceSourceMetalLayer();
        source.layer = layer;
        descriptor.nextInChain = source;
      }
      default -> {
        return null;
      }
    }
    return instance.createSurface(descriptor);
  }

  /** Releases the Metal view created for the window, if there is one. Safe to call more than once. */
  void destroy() {
    if (metalView != 0L) {
      SDLMetal.SDL_Metal_DestroyView(metalView);
      metalView = 0L;
    }
  }

  /**
   * Returns the name of the video driver SDL is using, such as {@code x11}, {@code wayland}, {@code windows}
   * or {@code cocoa}.
   *
   * @return The driver name, or an empty string when none is active.
   */
  @NotNull
  static String driver() {
    String driver = SDLVideo.SDL_GetCurrentVideoDriver();
    return driver != null ? driver : "";
  }
}
