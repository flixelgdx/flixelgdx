package me.stringdotjar.flixelgdx.tween.type;

import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectSet;

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.util.FlixelConstants;
import me.stringdotjar.flixelgdx.util.FlixelReflectUtil;

import java.lang.reflect.Field;

/**
 * Tween type for tweening specific fields on an object using reflection.
 * 
 * <p>Although it is slightly slower than a {@link FlixelPropertyTween}, this type is here
 * just in case you need it and for convenience.
 */
public class FlixelVarTween extends FlixelTween {

  /** The object to tween. */
  protected Object object;

  /** The initial values of the fields being tweened. */
  protected final ObjectFloatMap<String> initialValues = new ObjectFloatMap<>();

  /** Reusable map for computed tween values, cleared and repopulated each frame. */
  protected final ObjectFloatMap<String> currentValues = new ObjectFloatMap<>();

  /**
   * Maps each goal field name to its target value.
   */
  protected final ObjectFloatMap<String> goalValues = new ObjectFloatMap<>();

  /**
   * Cache of the fields being tweened for faster access so they aren't accessed every time the
   * {@link #start()} method is called.
   */
  protected Field[] fieldsCache = null;

  /** The update callback for {@code this} tween to change the objects values every update. */
  protected FlixelVarTween.FunkinVarTweenUpdateCallback updateCallback;

  /**
   * Constructs a new object tween using reflection.
   *
   * <p>Note that this does NOT add the tween to the global manager, it just assigns its main
   * values. That's it. If you wish to create a tween to automatically start working, you might want
   * to see {@link FlixelTween#tween(Object object, FlixelTweenSettings tweenSettings,
   * FunkinVarTweenUpdateCallback updateCallback)}.
   *
   * @param object The object to tween values.
   * @param settings The settings that configure and determine how the tween should animate and last for.
   * @param updateCallback Callback function for updating the objects values when the tween updates.
   */
  public FlixelVarTween(Object object, FlixelTweenSettings settings, FunkinVarTweenUpdateCallback updateCallback) {
    super(settings);
    this.object = object;
    this.updateCallback = updateCallback;
  }

  @Override
  public FlixelTween start() {
    super.start();

    if (tweenSettings == null) {
      return this;
    }

    var goals = tweenSettings.getGoals();
    if (goals == null || goals.isEmpty()) {
      return this;
    }

    if (fieldsCache == null) {
      fieldsCache = FlixelReflectUtil.getAllFieldsAsArray(object.getClass());
    }

    ObjectSet<String> floatFieldIds = new ObjectSet<>(fieldsCache.length);
    for (Field f : fieldsCache) {
      if (f != null && f.getType() == float.class) {
        String name = f.getName();
        if (name != null && !name.isEmpty()) {
          floatFieldIds.add(name);
        }
      }
    }

    final Field[] fields = fieldsCache;
    final Object target = object;
    tweenSettings.forEachGoal((fieldName, value) -> {
      if (!floatFieldIds.contains(fieldName)) {
        String message = "Field \"" + fieldName + "\" does not exist on the given object or is not a float field.";
        throw new RuntimeException(message);
      }
      for (Field field : fields) {
        if (!field.getName().equals(fieldName)) {
          continue;
        }
        try {
          if (!field.trySetAccessible()) {
            continue;
          }
          initialValues.put(fieldName, field.getFloat(target));
          goalValues.put(fieldName, value);
          break;
        } catch (IllegalAccessException e) {
        }
      }
    });

    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (updateCallback == null || goalValues.isEmpty()) {
      return;
    }
    currentValues.clear();
    for (ObjectFloatMap.Entry<String> entry : goalValues.entries()) {
      float startValue = initialValues.get(entry.key, 0f);
      float newValue = startValue + (entry.value - startValue) * scale;
      currentValues.put(entry.key, newValue);
    }
    if (currentValues.size > 0) {
      updateCallback.update(currentValues);
    }
  }

  @Override
  public void reset() {
    super.reset();
    fieldsCache = null;
    goalValues.clear();
  }

  @Override
  public void resetBasic() {
    super.resetBasic();
    initialValues.clear();
    currentValues.clear();
  }

  /** Callback interface for changing an objects values when a var tween updates its values. */
  @FunctionalInterface
  public interface FunkinVarTweenUpdateCallback {

    /**
     * A callback method that is called when the tween updates its values during its tweening (or
     * animating) process.
     *
     * @param values The new current values of the fields being tweened during the animation.
     */
    void update(ObjectFloatMap<String> values);
  }
}
