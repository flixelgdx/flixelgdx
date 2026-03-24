package me.stringdotjar.flixelgdx.tween.builders;

import com.badlogic.gdx.utils.Array;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelVarTween;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for {@link FlixelVarTween} (reflection-based). Requires {@link #setObject(Object)},
 * at least one {@link #addGoal(String, float)}, and {@link #setCallback}. Chain ends with {@link #start()}.
 */
public final class FlixelVarTweenBuilder extends FlixelAbstractTweenBuilder<FlixelVarTween, FlixelVarTweenBuilder> {

  private final Array<FlixelTweenSettings.FlixelTweenVarGoal> goals = new Array<>();
  private Object object;
  private FlixelVarTween.FunkinVarTweenUpdateCallback callback;

  /** Creates a new var tween builder. Use {@link FlixelTween#tween(Class, Class) FlixelTween.tween(FlixelVarTween.class, FlixelVarTweenBuilder.class)}. */
  public FlixelVarTweenBuilder() {}

  @Override
  protected FlixelVarTweenBuilder self() {
    return this;
  }

  public FlixelVarTweenBuilder setObject(@NotNull Object object) {
    this.object = object;
    return this;
  }

  public FlixelVarTweenBuilder addGoal(@NotNull String field, float value) {
    goals.add(new FlixelTweenSettings.FlixelTweenVarGoal(field, value));
    return this;
  }

  public FlixelVarTweenBuilder setCallback(@NotNull FlixelVarTween.FunkinVarTweenUpdateCallback callback) {
    this.callback = callback;
    return this;
  }

  @Override
  public FlixelVarTween start() {
    if (object == null) {
      throw new IllegalStateException("FlixelVarTween requires setObject(object) before start()");
    }
    if (callback == null) {
      throw new IllegalStateException("FlixelVarTween requires setCallback(callback) before start()");
    }
    if (goals.isEmpty()) {
      throw new IllegalStateException("FlixelVarTween requires at least one addGoal(field, value) before start()");
    }
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    for (int i = 0; i < goals.size; i++) {
      var goal = goals.get(i);
      settings.addGoal(goal.field(), goal.value());
    }
    FlixelVarTween tween = manager.obtainTween(FlixelVarTween.class, () -> new FlixelVarTween(object, settings, callback));
    tween.setTweenSettings(settings);
    tween.setTarget(object, callback);
    return (FlixelVarTween) manager.addTween(tween);
  }
}
