/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base type for a named logical control inside a {@link FlixelActionSet}.
 *
 * <p>Concrete types are {@link FlixelActionDigital} (boolean, many {@link FlixelInputBinding}s OR'd together) and
 * {@link FlixelActionAnalog} (2D vector from keys and gamepad axes, optional Steam vector merge). Subclasses are updated
 * by the owning set's {@link FlixelActionSet#update(float)} and finalized by {@link FlixelActionSet#endFrame()}.
 *
 * <p>The {@link #getName()} string is used for Steam Input alignment and logging; keep names stable if players rebind or
 * use cloud profiles.
 */
public abstract class FlixelAction {

  @NotNull
  private final String name;

  @Nullable
  FlixelActionSet owner;

  /** Optional edge callback; assign once to avoid per-frame allocations. */
  @Nullable
  public Runnable callback;

  /** When {@code false}, this action stays inactive and reads false or zero. */
  public boolean active = true;

  protected FlixelAction(@Nullable String name) {
    this.name = name != null ? name : "";
  }

  /**
   * Logical name for Steam manifest alignment and debugging.
   *
   * @return Non-null name string.
   */
  @NotNull
  public String getName() {
    return name;
  }

  @Nullable
  public FlixelActionSet getOwner() {
    return owner;
  }

  void setOwner(@Nullable FlixelActionSet set) {
    this.owner = set;
  }

  abstract void updateAction(float elapsed);

  abstract void endFrameAction();

  abstract void resetAction();
}
