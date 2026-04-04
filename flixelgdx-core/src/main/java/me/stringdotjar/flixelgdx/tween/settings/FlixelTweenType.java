/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.settings;

/** Enum containing all different tween types that can determine the behavior of a tween. */
public enum FlixelTweenType {

  /** Stops and removes itself from the manager when it finishes. */
  ONESHOT,

  /** Stops when finished but remains in the manager. Can be reused multiple times. */
  PERSIST,

  /** Like {@link #PERSIST} but plays once in reverse; does not remove on finish. */
  BACKWARD,

  /** Restarts immediately when it finishes. {@code onComplete} is called every cycle. */
  LOOPING,

  /** Like {@link #LOOPING} but every second run is in reverse. {@code onComplete} is called every cycle. */
  PINGPONG;

  /** True for LOOPING and PINGPONG (tween restarts and may flip direction). */
  public boolean isLooping() {
    return this == LOOPING || this == PINGPONG;
  }

  /** True if this type plays in reverse (initial direction for {@link #BACKWARD}). Toggled each cycle for {@link #PINGPONG}. */
  public boolean isBackward() {
    return this == BACKWARD;
  }

  /** True only for {@link #ONESHOT}: tween is removed from the manager when it finishes. */
  public boolean removeOnFinish() {
    return this == ONESHOT;
  }
}
