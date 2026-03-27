/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.tween.FlixelEase;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.FlixelTweenManager;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;
import org.jetbrains.annotations.Nullable;

/**
 * Base for fluent tween builders using the recursive generic pattern (CRTP). Holds common
 * configuration (duration, ease, type, delays, callbacks) and applies it to a
 * {@link FlixelTweenSettings} instance. Use {@link FlixelTween#tween(Class, Class)} with the
 * concrete builder class so the return type has the correct type-specific methods.
 *
 * @param <T> The tween type (e.g. {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween}).
 * @param <B> The concrete builder type (e.g. {@link FlixelPropertyTweenBuilder}), for fluent return types.
 */
public abstract class FlixelAbstractTweenBuilder<T extends FlixelTween, B extends FlixelAbstractTweenBuilder<T, B>> implements FlixelTweenBuilder<T> {

  protected float duration = 1f;
  protected FlixelTweenType type = FlixelTweenType.ONESHOT;
  protected FlixelEase.FunkinEaseFunction ease = FlixelEase::linear;
  protected FlixelTweenManager manager = FlixelTween.getGlobalManager();
  protected float startDelay;
  protected float loopDelay;
  protected float framerate;
  protected FlixelEase.FunkinEaseStartCallback onStart;
  protected FlixelEase.FunkinEaseUpdateCallback onUpdate;
  protected FlixelEase.FunkinEaseCompleteCallback onComplete;

  /**
   * Returns {@code this} as the concrete builder type for fluent chaining.
   */
  protected abstract B self();

  /**
   * Applies the current common configuration to the given settings instance.
   */
  protected void applyTo(FlixelTweenSettings settings) {
    settings.setDuration(duration);
    settings.setType(type);
    settings.setEase(ease);
    settings.setStartDelay(startDelay);
    settings.setLoopDelay(loopDelay);
    settings.setFramerate(framerate);
    settings.setOnStart(onStart);
    settings.setOnUpdate(onUpdate);
    settings.setOnComplete(onComplete);
  }

  public B setDuration(float duration) {
    this.duration = duration;
    return self();
  }

  public B setType(FlixelTweenType type) {
    this.type = type;
    return self();
  }

  public B setEase(FlixelEase.FunkinEaseFunction ease) {
    this.ease = ease;
    return self();
  }

  public B setManager(FlixelTweenManager manager) {
    this.manager = manager;
    return self();
  }

  public B setStartDelay(float startDelay) {
    this.startDelay = startDelay;
    return self();
  }

  public B setLoopDelay(float loopDelay) {
    this.loopDelay = loopDelay;
    return self();
  }

  public B setFramerate(float framerate) {
    this.framerate = framerate;
    return self();
  }

  public B setOnStart(@Nullable FlixelEase.FunkinEaseStartCallback onStart) {
    this.onStart = onStart;
    return self();
  }

  public B setOnUpdate(@Nullable FlixelEase.FunkinEaseUpdateCallback onUpdate) {
    this.onUpdate = onUpdate;
    return self();
  }

  public B setOnComplete(@Nullable FlixelEase.FunkinEaseCompleteCallback onComplete) {
    this.onComplete = onComplete;
    return self();
  }
}
