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

import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

/**
 * The desktop window, wrapping the SDL3 window the {@link FlixelDesktopRunner} created.
 *
 * <p>Exposes the window controls game code reaches through {@link org.flixelgdx.Flixel#window
 * Flixel.window}: title, size, position, fullscreen, decoration, focus, opacity, and closing.
 * The continuous-rendering hooks let the framework idle the loop while the window is unfocused;
 * the runner reads that flag via {@link #isContinuousRendering()} and {@link #consumeRenderRequest()}.
 *
 * <p>Window position is cached locally rather than queried from SDL on each read. The cache is
 * initialized when the window is bound and updated both from set calls and from
 * {@code SDL_EVENT_WINDOW_MOVED} events (user drag), so reads are always fast and
 * {@link #setX(int)}/{@link #setY(int)} never query SDL for the axis they are not changing.
 */
public class FlixelSdlWindow implements FlixelWindow {

  /** The SDL window handle, or {@code 0} before the runner creates one. */
  private long handle;

  private int cachedX;
  private int cachedY;

  private boolean closeRequested;
  private boolean absorbCloseRequests;

  /**
   * Binds this wrapper to the SDL window created by the runner and seeds the position cache.
   *
   * @param handle The SDL window handle.
   */
  void bind(long handle) {
    this.handle = handle;
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer x = stack.mallocInt(1);
      IntBuffer y = stack.mallocInt(1);
      SDLVideo.SDL_GetWindowPosition(handle, x, y);
      cachedX = x.get(0);
      cachedY = y.get(0);
    }
  }

  /**
   * Updates the cached position when the OS moves the window (for example after a user drag).
   * The runner calls this on {@code SDL_EVENT_WINDOW_MOVED}.
   *
   * @param x New X position in screen coordinates.
   * @param y New Y position in screen coordinates.
   */
  void onMoved(int x, int y) {
    cachedX = x;
    cachedY = y;
  }

  /** Returns {@code true} once {@link #close()} has been called, so the runner can exit the loop. */
  boolean isCloseRequested() {
    return closeRequested;
  }

  @Override
  public void close() {
    closeRequested = true;
  }

  @Override
  public String getTitle() {
    return handle != 0L ? SDLVideo.SDL_GetWindowTitle(handle) : "";
  }

  @Override
  public void setTitle(String title) {
    if (handle != 0L && title != null) {
      SDLVideo.SDL_SetWindowTitle(handle, title);
    }
  }

  @Override
  public int getWidth() {
    return querySize(true);
  }

  @Override
  public int getHeight() {
    return querySize(false);
  }

  @Override
  public int getBackBufferWidth() {
    return querySizeInPixels(true);
  }

  @Override
  public int getBackBufferHeight() {
    return querySizeInPixels(false);
  }

  @Override
  public void setSize(int width, int height) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowSize(handle, width, height);
    }
  }

  @Override
  public boolean isFullscreen() {
    return handle != 0L && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_FULLSCREEN) != 0L;
  }

  @Override
  public void setFullscreen(FlixelDisplayMode mode) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowFullscreen(handle, true);
    }
  }

  @Override
  public void setWindowed(int width, int height) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowFullscreen(handle, false);
      SDLVideo.SDL_SetWindowSize(handle, width, height);
    }
  }

  @Override
  public boolean supportsFullscreen() {
    return true;
  }

  @Override
  public boolean supportsOpacity() {
    return true;
  }

  @Override
  public float getOpacity() {
    return handle != 0L ? SDLVideo.SDL_GetWindowOpacity(handle) : 1f;
  }

  @Override
  public void setOpacity(float opacity) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowOpacity(handle, opacity);
    }
  }

  @Override
  public void setX(int x) {
    if (handle != 0L) {
      cachedX = x;
      SDLVideo.SDL_SetWindowPosition(handle, cachedX, cachedY);
    }
  }

  @Override
  public void setY(int y) {
    if (handle != 0L) {
      cachedY = y;
      SDLVideo.SDL_SetWindowPosition(handle, cachedX, cachedY);
    }
  }

  @Override
  public void setPosition(int x, int y) {
    if (handle != 0L) {
      cachedX = x;
      cachedY = y;
      SDLVideo.SDL_SetWindowPosition(handle, x, y);
    }
  }

  @Override
  public void changeX(int deltaX) {
    setPosition(cachedX + deltaX, cachedY);
  }

  @Override
  public void changeY(int deltaY) {
    setPosition(cachedX, cachedY + deltaY);
  }

  @Override
  public boolean supportsDecorated() {
    return true;
  }

  @Override
  public void setDecorated(boolean decorated) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowBordered(handle, decorated);
    }
  }

  @Override
  public boolean isDecorated() {
    return handle == 0L || (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_BORDERLESS) == 0L;
  }

  @Override
  public boolean supportsBringToForeground() {
    return true;
  }

  @Override
  public void bringToForeground() {
    if (handle != 0L) {
      SDLVideo.SDL_RaiseWindow(handle);
    }
  }

  @Override
  public boolean isFocused() {
    return handle != 0L && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_INPUT_FOCUS) != 0L;
  }

  @Override
  public boolean supportsFloating() {
    return true;
  }

  @Override
  public void setFloating(boolean floating) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowAlwaysOnTop(handle, floating);
    }
  }

  @Override
  public boolean isFloating() {
    return handle != 0L && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_ALWAYS_ON_TOP) != 0L;
  }

  @Override
  public void setResizable(boolean resizable) {
    if (handle != 0L) {
      SDLVideo.SDL_SetWindowResizable(handle, resizable);
    }
  }

  @Override
  public boolean isResizable() {
    return handle != 0L && (SDLVideo.SDL_GetWindowFlags(handle) & SDLVideo.SDL_WINDOW_RESIZABLE) != 0L;
  }

  @Override
  public boolean supportsAbsorbCloseRequests() {
    return true;
  }

  @Override
  public void setAbsorbCloseRequests(boolean absorb) {
    absorbCloseRequests = absorb;
  }

  @Override
  public boolean isAbsorbCloseRequests() {
    return absorbCloseRequests;
  }

  @Override
  public int getX() {
    return cachedX;
  }

  @Override
  public int getY() {
    return cachedY;
  }

  private int querySize(boolean wantWidth) {
    if (handle == 0L) {
      return 0;
    }
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer w = stack.mallocInt(1);
      IntBuffer h = stack.mallocInt(1);
      SDLVideo.SDL_GetWindowSize(handle, w, h);
      return wantWidth ? w.get(0) : h.get(0);
    }
  }

  private int querySizeInPixels(boolean wantWidth) {
    if (handle == 0L) {
      return 0;
    }
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer w = stack.mallocInt(1);
      IntBuffer h = stack.mallocInt(1);
      SDLVideo.SDL_GetWindowSizeInPixels(handle, w, h);
      return wantWidth ? w.get(0) : h.get(0);
    }
  }
}
