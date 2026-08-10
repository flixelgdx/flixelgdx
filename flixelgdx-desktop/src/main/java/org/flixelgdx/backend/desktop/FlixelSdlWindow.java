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
 * Flixel.window}: title, size, fullscreen, and closing. The continuous-rendering hooks let the
 * framework idle the loop while the window is unfocused; the runner reads that flag.
 */
public final class FlixelSdlWindow implements FlixelWindow {

  /** The SDL window handle, or {@code 0} before the runner creates one. */
  private long handle;

  private boolean continuousRendering = true;
  private boolean closeRequested;

  /**
   * Binds this wrapper to the SDL window created by the runner.
   *
   * @param handle The SDL window handle.
   */
  void bind(long handle) {
    this.handle = handle;
  }

  /**
   * @return {@code true} once {@link #close()} has been called, so the runner can exit the loop.
   */
  boolean isCloseRequested() {
    return closeRequested;
  }

  /**
   * @return Whether continuous rendering is currently on.
   */
  boolean isContinuousRendering() {
    return continuousRendering;
  }

  @Override
  public void setContinuousRendering(boolean continuous) {
    this.continuousRendering = continuous;
  }

  @Override
  public void requestRendering() {
    // The desktop loop always renders the next frame, so this is a no-op here.
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
    return SDLVideo.SDL_GetWindowOpacity(handle);
  }

  @Override
  public void setOpacity(float opacity) {
    SDLVideo.SDL_SetWindowOpacity(handle, opacity);
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
