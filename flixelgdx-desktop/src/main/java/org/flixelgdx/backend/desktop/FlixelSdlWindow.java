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
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.util.FlixelColor;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * The desktop window, wrapping the SDL3 window the {@link FlixelDesktopRunner} created.
 *
 * <p>Exposes the window controls game code reaches through {@link org.flixelgdx.Flixel#window
 * Flixel.window}: title, size, position, fullscreen, decoration, focus, opacity, and closing.
 *
 * <p>Window position is cached locally rather than queried from SDL on each read. The cache is
 * initialized when the window is bound and updated both from set calls and from
 * {@code SDL_EVENT_WINDOW_MOVED} events (user drag), so reads are always fast and
 * {@link #setX(int)}/{@link #setY(int)} never query SDL for the axis they are not changing.
 */
public class FlixelSdlWindow implements FlixelWindow {

  /** Number of floats stored per camera: r, g, b, a, useBgAlphaBlending (1 = true, 0 = false). */
  private static final int FLOATS_PER_CAMERA = 5;

  /** The SDL window handle, or {@code 0} before the runner creates one. */
  private long handle;

  private int cachedX;
  private int cachedY;
  private int savedCameraCount;

  private float[] gameRgba = new float[4];
  private float[] camerasPacked = new float[20];

  private boolean closeRequested;
  private boolean absorbCloseRequests;
  private boolean transparencyActive;
  private boolean transparencySnapshotValid;

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
  public void setTransparencyActive(boolean active) {
    transparencyActive = active;
    if (active) {
      captureSnapshot();
      applyTransparencyBackdropOnly();
    } else {
      restoreBackdrop();
      clearSnapshot();
    }
  }

  @Override
  public boolean isTransparencyActive() {
    return transparencyActive;
  }

  @Override
  public void applyTransparencyBackdropOnly() {
    Flixel.game.getBgColor().a = 0f;
    FlixelCamera[] camItems = Flixel.cameras.getItems();
    for (int i = 0, n = Flixel.cameras.getSize(); i < n; i++) {
      FlixelCamera cam = camItems[i];
      if (cam == null) {
        continue;
      }
      cam.useBgAlphaBlending = true;
      cam.bgColor.a = 0f;
    }
  }

  @Override
  public void resetTransparency() {
    transparencyActive = false;
    transparencySnapshotValid = false;
    savedCameraCount = 0;
    Arrays.fill(gameRgba, 0f);
    Arrays.fill(camerasPacked, 0f);
  }

  private void captureSnapshot() {
    if (transparencySnapshotValid) {
      return;
    }
    gameRgba[0] = Flixel.game.getBgColor().r;
    gameRgba[1] = Flixel.game.getBgColor().g;
    gameRgba[2] = Flixel.game.getBgColor().b;
    gameRgba[3] = Flixel.game.getBgColor().a;
    int n = Flixel.cameras.getSize();
    ensureCapacity(n);
    FlixelCamera[] camItems = n == 0 ? null : Flixel.cameras.getItems();
    for (int i = 0; i < n; i++) {
      FlixelCamera cam = camItems[i];
      int o = i * FLOATS_PER_CAMERA;
      if (cam == null) {
        camerasPacked[o] = 0f;
        camerasPacked[o + 1] = 0f;
        camerasPacked[o + 2] = 0f;
        camerasPacked[o + 3] = 1f;
        camerasPacked[o + 4] = 0f;
        continue;
      }
      camerasPacked[o] = cam.bgColor.r;
      camerasPacked[o + 1] = cam.bgColor.g;
      camerasPacked[o + 2] = cam.bgColor.b;
      camerasPacked[o + 3] = cam.bgColor.a;
      camerasPacked[o + 4] = cam.useBgAlphaBlending ? 1f : 0f;
    }
    savedCameraCount = n;
    transparencySnapshotValid = true;
  }

  private void restoreBackdrop() {
    if (transparencySnapshotValid) {
      Flixel.game.getBgColor().r = gameRgba[0];
      Flixel.game.getBgColor().g = gameRgba[1];
      Flixel.game.getBgColor().b = gameRgba[2];
      Flixel.game.getBgColor().a = gameRgba[3];
    } else {
      Flixel.game.getBgColor().set(FlixelColor.BLACK);
    }
    FlixelCamera[] camItems = Flixel.cameras.getItems();
    int n = Flixel.cameras.getSize();
    for (int i = 0; i < n; i++) {
      FlixelCamera cam = camItems[i];
      if (cam == null) {
        continue;
      }
      if (transparencySnapshotValid && i < savedCameraCount) {
        int o = i * FLOATS_PER_CAMERA;
        cam.bgColor.r = camerasPacked[o];
        cam.bgColor.g = camerasPacked[o + 1];
        cam.bgColor.b = camerasPacked[o + 2];
        cam.bgColor.a = camerasPacked[o + 3];
        cam.useBgAlphaBlending = camerasPacked[o + 4] != 0f;
      } else {
        cam.useBgAlphaBlending = false;
        cam.bgColor.set(FlixelColor.BLACK);
      }
    }
  }

  private void clearSnapshot() {
    transparencySnapshotValid = false;
    savedCameraCount = 0;
    Arrays.fill(gameRgba, 0f);
    Arrays.fill(camerasPacked, 0f);
  }

  private void ensureCapacity(int cameraCount) {
    int need = cameraCount * FLOATS_PER_CAMERA;
    if (camerasPacked.length < need) {
      camerasPacked = new float[Math.max(need, camerasPacked.length * 2)];
    }
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
