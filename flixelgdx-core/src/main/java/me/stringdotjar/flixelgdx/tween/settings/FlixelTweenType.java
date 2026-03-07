package me.stringdotjar.flixelgdx.tween.settings;

/** Enum containing all different tween types that can determine */
public enum FlixelTweenType {

  /** Will stop and remove itself from the manager when it finishes. */
  ONESHOT,
  /** Will stop when it finishes but remain in the manager. */
  PERSIST,
  /** Will play tween in reverse direction. */
  BACKWARD,
  /** Will restart immediately when it finishes. */
  LOOPING,
  /** "To and from", will play tween hither and thither. Also loops indefinitely. */
  PINGPONG;
}
