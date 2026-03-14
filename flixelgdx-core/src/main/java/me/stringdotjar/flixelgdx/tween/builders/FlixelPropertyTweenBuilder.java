package me.stringdotjar.flixelgdx.tween.builders;

import com.badlogic.gdx.utils.Array;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenPropertyGoal;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenPropertyGoal.FlixelTweenPropertyFloatGetter;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenPropertyGoal.FlixelTweenPropertyFloatSetter;
import me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween;

import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for {@link FlixelPropertyTween}. Use getter/setter goals via {@link #addGoal}.
 * Chain ends with {@link #start()}.
 */
public final class FlixelPropertyTweenBuilder extends FlixelAbstractTweenBuilder<FlixelPropertyTween, FlixelPropertyTweenBuilder> {

  private final Array<FlixelTweenSettings.FlixelTweenPropertyGoal> propertyGoals = new Array<>();

  /** Creates a new property tween builder. Use {@link FlixelTween#tween(Class, Class) FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class)}. */
  public FlixelPropertyTweenBuilder() {}

  @Override
  protected FlixelPropertyTweenBuilder self() {
    return this;
  }

  public FlixelPropertyTweenBuilder addGoal(@NotNull FlixelTweenPropertyFloatGetter getter, float toValue, @NotNull FlixelTweenPropertyFloatSetter setter) {
    propertyGoals.add(new FlixelTweenPropertyGoal(getter, toValue, setter));
    return this;
  }

  @Override
  public FlixelPropertyTween start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    for (int i = 0; i < propertyGoals.size; i++) {
      var goal = propertyGoals.get(i);
      settings.addGoal(goal.getter(), goal.toValue(), goal.setter());
    }
    FlixelPropertyTween tween = new FlixelPropertyTween(settings);
    return (FlixelPropertyTween) FlixelTween.getGlobalManager().addTween(tween);
  }
}
