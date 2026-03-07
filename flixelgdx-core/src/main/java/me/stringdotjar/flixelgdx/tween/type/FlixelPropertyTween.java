package me.stringdotjar.flixelgdx.tween.type;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

/**
 * Tween type for animating values via getter/setter pairs (property goals) rather than
 * reflection. Use this when you need setter side effects (e.g. bounds updates, listeners) to
 * run on every interpolated step. Configure with {@link FlixelTweenSettings#addGoal}.
 *
 * <p>Note that this is faster than {@link FlixelVarTween} since it does not use reflection to access the fields.
 * It is recommended to use this when you need setter side effects (e.g. bounds updates, listeners) to
 * run on every interpolated step, the {@code FlixelVarTween} type is there for convenience.
 */
public class FlixelPropertyTween extends FlixelTween {

  /**
   * Cached property goals captured at {@link #start()} to avoid re-allocating the list every
   * frame inside {@link #updateTweenValues()}.
   */
  protected Array<FlixelTweenSettings.FlixelTweenPropertyGoal> cachedPropertyGoals = new Array<>();

  /**
   * Initial values of each property goal, captured from their getter at {@link #start()}, indexed
   * parallel to {@link #cachedPropertyGoals}.
   */
  protected FloatArray propertyGoalStartValues = new FloatArray();

  /**
   * Constructs a new property tween with the given settings. Property goals must be added via
   * {@link FlixelTweenSettings#addGoal} before starting.
   *
   * @param settings The settings that configure and determine how the tween should animate.
   */
  public FlixelPropertyTween(FlixelTweenSettings settings) {
    super(settings);
  }

  @Override
  public FlixelTween start() {
    super.start();

    if (tweenSettings == null) {
      return this;
    }

    var propertyGoals = tweenSettings.getPropertyGoals();
    if (propertyGoals == null || propertyGoals.isEmpty()) {
      return this;
    }

    cachedPropertyGoals.clear();
    propertyGoalStartValues.clear();
    for (int i = 0; i < propertyGoals.size; i++) {
      var goal = propertyGoals.get(i);
      if (goal == null) {
        continue;
      }
      cachedPropertyGoals.add(goal);
      propertyGoalStartValues.add(goal.getter().get());
    }

    return this;
  }

  @Override
  protected void updateTweenValues() {
    for (int i = 0; i < cachedPropertyGoals.size; i++) {
      float startValue = propertyGoalStartValues.get(i);
      var goal = cachedPropertyGoals.get(i);
      if (goal == null) {
        continue;
      }
      float newValue = startValue + (goal.toValue() - startValue) * scale;
      goal.setter().set(newValue);
    }
  }

  @Override
  public void reset() {
    super.reset();
    cachedPropertyGoals.clear();
    propertyGoalStartValues.clear();
  }
}
