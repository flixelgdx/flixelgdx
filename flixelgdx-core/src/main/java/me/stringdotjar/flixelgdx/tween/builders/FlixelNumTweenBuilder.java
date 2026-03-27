/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelNumTween;
import org.jetbrains.annotations.Nullable;

/**
 * Fluent builder for {@link FlixelNumTween}. Requires {@link #from(float)}, {@link #to(float)},
 * and {@link #setCallback}. {@link #start()} throws if any of these are missing.
 */
public final class FlixelNumTweenBuilder extends FlixelAbstractTweenBuilder<FlixelNumTween, FlixelNumTweenBuilder> {

  private float from = Float.NaN;
  private float to = Float.NaN;
  private FlixelNumTween.FlixelNumTweenUpdateCallback callback;

  /** Creates a new num tween builder. Use {@link FlixelTween#tween(Class, Class) FlixelTween.tween(FlixelNumTween.class, FlixelNumTweenBuilder.class)}. */
  public FlixelNumTweenBuilder() {}

  @Override
  protected FlixelNumTweenBuilder self() {
    return this;
  }

  public FlixelNumTweenBuilder from(float from) {
    this.from = from;
    return this;
  }

  public FlixelNumTweenBuilder to(float to) {
    this.to = to;
    return this;
  }

  public FlixelNumTweenBuilder setCallback(@Nullable FlixelNumTween.FlixelNumTweenUpdateCallback callback) {
    this.callback = callback;
    return this;
  }

  @Override
  public FlixelNumTween start() {
    if (Float.isNaN(from)) {
      throw new IllegalStateException("FlixelNumTween requires from(float) before start()");
    }
    if (Float.isNaN(to)) {
      throw new IllegalStateException("FlixelNumTween requires to(float) before start()");
    }
    if (callback == null) {
      throw new IllegalStateException("FlixelNumTween requires setCallback(callback) before start()");
    }
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelNumTween tween = new FlixelNumTween(from, to, settings, callback);
    return (FlixelNumTween) FlixelTween.getGlobalManager().addTween(tween);
  }
}
