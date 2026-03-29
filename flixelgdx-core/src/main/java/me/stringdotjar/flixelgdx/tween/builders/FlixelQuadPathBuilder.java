/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelQuadPath;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelQuadPath}. */
public final class FlixelQuadPathBuilder extends FlixelAbstractTweenBuilder<FlixelQuadPath, FlixelQuadPathBuilder> {

  private @Nullable FlixelObject target;
  private final Array<float[]> pointList = new Array<>();
  private float durationOrSpeed = 1f;
  private boolean useDuration = true;

  @Override
  protected FlixelQuadPathBuilder self() {
    return this;
  }

  public FlixelQuadPathBuilder setTarget(@Nullable FlixelObject target) {
    this.target = target;
    return this;
  }

  public FlixelQuadPathBuilder addPoint(float x, float y) {
    pointList.add(new float[] {x, y});
    return this;
  }

  public FlixelQuadPathBuilder durationOrSpeed(float v, boolean useDuration) {
    this.durationOrSpeed = v;
    this.useDuration = useDuration;
    return this;
  }

  @Override
  public FlixelQuadPath start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelQuadPath tween =
        manager.obtainTween(FlixelQuadPath.class, () -> new FlixelQuadPath(settings));
    tween.setTweenSettings(settings);
    for (int i = 0; i < pointList.size; i++) {
      float[] p = pointList.get(i);
      tween.addPoint(p[0], p[1]);
    }
    tween.setMotion(durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return (FlixelQuadPath) manager.addTween(tween);
  }
}
