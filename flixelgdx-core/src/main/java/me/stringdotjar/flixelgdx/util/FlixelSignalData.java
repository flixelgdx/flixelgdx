/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelState;

/**
 * Convenience class for holding all signal data types used in the default signals stored in
 * the global {@link Flixel} manager class.
 *
 * <p>{@link UpdateSignalData} is a mutable, reusable class rather than a record because it is
 * dispatched every frame. Allocating a new object 120 times per second (pre+post, and assuming the FPS is 60)
 * adds GC pressure that causes frame stutters. Signal handlers must not hold a reference to the data
 * object past the callback return.
 */
public final class FlixelSignalData {

  /**
   * Mutable carrier for per-frame update data. Reuse the same instance across frames to
   * avoid GC pressure. Do NOT store a reference to this object; read values during the
   * callback only.
   */
  public static final class UpdateSignalData {
    private float elapsed;

    public UpdateSignalData() {}

    public UpdateSignalData(float elapsed) {
      this.elapsed = elapsed;
    }

    public float elapsed() {
      return elapsed;
    }

    public void set(float elapsed) {
      this.elapsed = elapsed;
    }
  }

  public record StateSwitchSignalData(FlixelState state) {}

  private FlixelSignalData() {}
}
