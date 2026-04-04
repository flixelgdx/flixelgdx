/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util.timer;

import org.jetbrains.annotations.NotNull;

/**
 * Callback invoked when a {@link FlixelTimer} completes a loop or the whole run.
 */
@FunctionalInterface
public interface FlixelTimerListener {

  /**
   * Called when a {@link FlixelTimer} completes a loop or the whole run.
   *
   * @param timer The timer that completed.
   */
  void onComplete(@NotNull FlixelTimer timer);
}
