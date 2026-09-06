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
package org.flixelgdx.backend;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.util.FlixelColor;

import java.util.Arrays;

/**
 * Manages the snapshot-and-restore logic for desktop compositing transparency.
 *
 * <p>When transparency is activated, the game's background color and every camera's backdrop are
 * forced to fully transparent so the desktop shows through. When deactivated, the colors captured
 * just before activation are restored, or opaque black is used if transparency was never on.
 *
 * <p>Each backend that supports desktop compositing owns one instance, returned by
 * {@link FlixelWindow#getTransparency()}. Game code uses
 * {@link FlixelWindow#setTransparencyActive(boolean)} rather than calling this directly.
 *
 * @see FlixelWindow#setTransparencyActive(boolean)
 */
public class FlixelWindowTransparency {

  /**
   * No-op implementation returned by {@link org.flixelgdx.backend.FlixelWindow#getTransparency()}
   * on platforms that do not support desktop compositing. All methods do nothing and
   * {@link #isActive()} always returns {@code false}.
   */
  public static final FlixelWindowTransparency NOOP = new FlixelWindowTransparency() {
    @Override
    public void apply(boolean active) {}

    @Override
    public void applyBackdropOnly() {}

    @Override
    public boolean isActive() {
      return false;
    }

    @Override
    public void reset() {}
  };

  /** Number of floats stored per camera: r, g, b, a, useBgAlphaBlending (1 = true, 0 = false). */
  private static final int FLOATS_PER_CAMERA = 5;

  private final float[] gameRgba = new float[4];
  private float[] camerasPacked = new float[20];

  private int savedCameraCount;
  private boolean active;
  private boolean snapshotValid;

  /** Creates a new, initially inactive transparency state manager. */
  public FlixelWindowTransparency() {}

  /**
   * Enables or disables desktop compositing transparency.
   *
   * <p>When {@code true}, clears and camera backdrop fills are forced to alpha zero. When
   * {@code false}, restores backdrop colors captured the first time transparency was enabled this
   * session, or falls back to opaque black if it was never enabled.
   *
   * @param active {@code true} to composite with the desktop through alpha.
   */
  public void apply(boolean active) {
    this.active = active;
    if (active) {
      capture();
      applyBackdropOnly();
    } else {
      restoreBackdrop();
      clearSnapshot();
    }
  }

  /**
   * Applies transparent clears and camera fills without touching the restore snapshot.
   * Used after {@link org.flixelgdx.FlixelGame#resetCameras()} while transparency stays enabled.
   */
  public void applyBackdropOnly() {
    if (Flixel.game == null) {
      return;
    }
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

  /**
   * Returns {@code true} when desktop compositing transparency is currently on.
   *
   * @return {@code true} when transparency is active.
   */
  public boolean isActive() {
    return active;
  }

  /** Resets all state. Called when the game shuts down. */
  public void reset() {
    active = false;
    snapshotValid = false;
    savedCameraCount = 0;
    Arrays.fill(gameRgba, 0f);
    Arrays.fill(camerasPacked, 0f);
  }

  private void capture() {
    if (snapshotValid) {
      return;
    }
    if (Flixel.game == null) {
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
    snapshotValid = true;
  }

  private void restoreBackdrop() {
    if (Flixel.game == null) {
      return;
    }
    if (snapshotValid) {
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
      if (snapshotValid && i < savedCameraCount) {
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
    snapshotValid = false;
    savedCameraCount = 0;
    Arrays.fill(gameRgba, 0f);
    Arrays.fill(camerasPacked, 0f);
  }

  private void ensureCapacity(int cameraCount) {
    int need = cameraCount * FLOATS_PER_CAMERA;
    if (camerasPacked.length >= need) {
      return;
    }
    camerasPacked = new float[Math.max(need, camerasPacked.length * 2)];
  }
}
