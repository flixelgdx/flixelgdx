package me.stringdotjar.flixelgdx.tween.settings;

/** Enum containing all different tween types that can determine the behavior of a tween. */
public enum FlixelTweenType {

  /** Stops and removes itself from the manager when it finishes. */
  ONESHOT,
  /** Stops when finished but remains in the manager (can be restarted). */
  PERSIST,
  /** Like PERSIST but plays once in reverse; does not remove on finish. */
  BACKWARD,
  /** Restarts immediately when it finishes; onComplete is called every cycle. */
  LOOPING,
  /** Like LOOPING but every second run is in reverse; onComplete is called every cycle. */
  PINGPONG;

  /** True for LOOPING and PINGPONG (tween restarts and may flip direction). */
  public boolean isLooping() {
    return this == LOOPING || this == PINGPONG;
  }

  /** True if this type plays in reverse (initial direction for BACKWARD; toggled each cycle for PINGPONG). */
  public boolean isBackward() {
    return this == BACKWARD;
  }

  /** True only for ONESHOT: tween is removed from the manager when it finishes. */
  public boolean removeOnFinish() {
    return this == ONESHOT;
  }
}
