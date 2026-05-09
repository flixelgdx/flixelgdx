/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Default {@link FlixelHostIntegration} used on platforms without desktop shell integration.
 */
public enum FlixelNoopHostIntegration implements FlixelHostIntegration {

  /** Shared no-op instance. */
  INSTANCE;

  @Override
  public void sendDesktopNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message");
  }

  @Override
  public void requestUserAttention() {}

  @Override
  public boolean supportsDesktopNotification() {
    return false;
  }
}
