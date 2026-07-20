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
package org.flixelgdx.tween.settings;

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

  /** Returns true for LOOPING and PINGPONG tween types. */
  public boolean getLooping() {
    return this == LOOPING || this == PINGPONG;
  }

  /** True if this type plays in reverse (initial direction for {@link #BACKWARD}). Toggled each cycle for {@link #PINGPONG}. */
  public boolean isBackward() {
    return this == BACKWARD;
  }

  /** Returns true if this tween type plays in reverse. */
  public boolean getBackward() {
    return this == BACKWARD;
  }

  /** True only for {@link #ONESHOT}: tween is removed from the manager when it finishes. */
  public boolean removeOnFinish() {
    return this == ONESHOT;
  }
}
