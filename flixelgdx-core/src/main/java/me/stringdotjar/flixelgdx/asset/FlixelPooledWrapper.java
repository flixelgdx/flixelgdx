/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * Pooled wrapper registered with {@link FlixelAssetManager#registerWrapper(FlixelPooledWrapper)} for
 * {@link #clearNonPersist()} lifecycle.
 */
public interface FlixelPooledWrapper {

  @NotNull
  String getAssetKey();

  /**
   * @return The wrapper class used to look up a {@link FlixelWrapperFactory} (typically the concrete
   *   wrapper class, e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}.class).
   */
  @NotNull
  default Class<?> wrapperRegistrationClass() {
    return getClass();
  }

  boolean isOwned();
}
