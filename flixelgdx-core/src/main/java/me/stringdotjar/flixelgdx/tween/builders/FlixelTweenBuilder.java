/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.tween.FlixelTween;

/**
 * Fluent builder for creating and starting a {@link FlixelTween} of a specific type.
 * Configuration methods return the builder for chaining; the chain ends with {@link #start()},
 * which creates the tween, adds it to a {@link FlixelTweenManager}, starts it, and returns the concrete tween instance.
 *
 * @param <T> The concrete tween type (e.g. {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween}).
 */
public interface FlixelTweenBuilder<T extends FlixelTween> {

  /**
   * Builds the tween from the current configuration, adds it to the global tween manager,
   * starts it, and returns the started tween.
   *
   * @return The started tween instance.
   */
  T start();
}
