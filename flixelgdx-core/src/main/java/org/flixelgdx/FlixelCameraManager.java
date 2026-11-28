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

import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The manager that keeps {@link Flixel#cameras} and the overlay camera in step with the game.
 *
 * <p>Think of it as the camera crew: it sets up the default camera when the game starts, moves
 * every camera each frame (follow, shake, flash, fade), refits them when the window changes size,
 * and puts a fresh default camera in place on every state switch. It is registered in
 * {@link Flixel#managers} at {@link FlixelManagers.Manager#ORDER_CAMERAS} and reached through
 * {@link Flixel#cameraManager}.
 *
 * <p>It also owns the overlay camera, which draws the global overlay on top of every game camera.
 * The overlay camera is never part of {@link Flixel#cameras} and its scroll is always zero, so
 * anything drawn with it lands at the same design-resolution coordinates no matter what the game
 * cameras do.
 *
 * <p>Example:
 * <pre>{@code
 * // Go back to a single default camera, for example after a split-screen section.
 * Flixel.cameraManager.reset();
 * }</pre>
 */
public class FlixelCameraManager implements FlixelManagers.Manager {

  /** The camera that draws the global overlay. Created in {@link #create()}. */
  @Nullable
  private FlixelCamera overlayCamera;

  /** The scroll of every camera when the game was paused, restored when it resumes. */
  private float @Nullable [][] pauseScroll;

  /** The zoom of every camera when the game was paused, restored when it resumes. */
  private float @Nullable [] pauseZoom;

  /** Creates the default camera and the overlay camera, both sized to the design resolution. */
  @Override
  public void create() {
    Flixel.cameras.clear();
    Flixel.cameras.add(new FlixelCamera(Flixel.config.getWidth(), Flixel.config.getHeight()));
    overlayCamera = new FlixelCamera(Flixel.config.getWidth(), Flixel.config.getHeight());
  }

  /**
   * Replaces every camera with a single default camera that fits the current window.
   *
   * <p>{@link #onStateSwitch(StateSwitchSignalData)} calls this on every state switch, so each
   * state starts with one fresh camera.
   */
  public void reset() {
    FlixelCamera camera = new FlixelCamera(Flixel.config.getWidth(), Flixel.config.getHeight());
    camera.update(Flixel.graphics.getBackBufferWidth(), Flixel.graphics.getBackBufferHeight(),
        camera.centerCameraOnResize);
    Flixel.cameras.clear();
    Flixel.cameras.add(camera);
    // The pause snapshot refers to cameras that no longer exist, so drop it.
    pauseScroll = null;
    pauseZoom = null;
    if (Flixel.window.isTransparencyActive()) {
      Flixel.window.applyTransparencyBackdropOnly();
    }
  }

  /**
   * Moves every camera (follow, shake, flash, fade) right after the state updated.
   *
   * @param elapsed The scaled seconds since the last frame.
   */
  @Override
  public void postUpdate(float elapsed) {
    for (int i = 0, n = Flixel.cameras.getSize(); i < n; i++) {
      Flixel.cameras.get(i).update(elapsed);
    }
    if (overlayCamera != null) {
      overlayCamera.update(elapsed);
    }
  }

  /**
   * Refits every camera and the overlay camera to the new surface size.
   *
   * @param width The new surface width in pixels.
   * @param height The new surface height in pixels.
   */
  @Override
  public void resize(int width, int height) {
    for (int i = 0, n = Flixel.cameras.getSize(); i < n; i++) {
      FlixelCamera camera = Flixel.cameras.get(i);
      camera.update(width, height, camera.centerCameraOnResize);
    }
    if (overlayCamera != null) {
      overlayCamera.update(width, height, overlayCamera.centerCameraOnResize);
    }
  }

  /**
   * Remembers where every camera was when the game pauses, and puts them back when it resumes.
   *
   * <p>While the game is paused the debugger may move the cameras around, so this keeps the game's
   * view unchanged once it resumes. When the debugger's free camera is on, it owns the cameras and
   * restores them itself, so nothing is saved.
   *
   * @param paused {@code true} when the game was just paused, {@code false} when it was resumed.
   */
  @Override
  public void onPaused(boolean paused) {
    if (paused) {
      snapshot();
    } else {
      restore();
    }
  }

  /**
   * Puts a single fresh default camera in place through {@link #reset()}, after the old state was
   * destroyed and before the new one is created.
   *
   * @param data Holds the state that was just destroyed.
   */
  @Override
  public void onStateSwitch(@NotNull StateSwitchSignalData data) {
    reset();
  }

  /** Destroys every camera and the overlay camera when the game closes. */
  @Override
  public void destroy() {
    for (int i = 0, n = Flixel.cameras.getSize(); i < n; i++) {
      Flixel.cameras.get(i).destroy();
    }
    Flixel.cameras.clear();
    if (overlayCamera != null) {
      overlayCamera.destroy();
      overlayCamera = null;
    }
    pauseScroll = null;
    pauseZoom = null;
  }

  private void snapshot() {
    int n = Flixel.cameras.getSize();
    if (n == 0 || isFreeCamActive()) {
      pauseScroll = null;
      pauseZoom = null;
      return;
    }
    pauseScroll = new float[n][2];
    pauseZoom = new float[n];
    for (int i = 0; i < n; i++) {
      FlixelCamera camera = Flixel.cameras.get(i);
      pauseScroll[i][0] = camera.scrollX;
      pauseScroll[i][1] = camera.scrollY;
      pauseZoom[i] = camera.getZoom();
    }
  }

  private void restore() {
    if (pauseScroll == null || pauseZoom == null || isFreeCamActive()) {
      pauseScroll = null;
      pauseZoom = null;
      return;
    }
    int n = Math.min(pauseScroll.length, Math.min(pauseZoom.length, Flixel.cameras.getSize()));
    for (int i = 0; i < n; i++) {
      Flixel.cameras.get(i).restoreScrollAndZoom(pauseScroll[i][0], pauseScroll[i][1], pauseZoom[i]);
    }
    pauseScroll = null;
    pauseZoom = null;
  }

  private static boolean isFreeCamActive() {
    return Flixel.debug != null && Flixel.debug.controls.isFreeCamActive();
  }

  @Override
  public int getOrder() {
    return ORDER_CAMERAS;
  }

  @NotNull
  @Override
  public String getName() {
    return "flixel.cameras";
  }

  /**
   * Returns the camera that draws the global overlay. It is never part of {@link Flixel#cameras}.
   *
   * @return The overlay camera, or {@code null} before the game was created.
   */
  @Nullable
  public FlixelCamera getOverlayCamera() {
    return overlayCamera;
  }
}
