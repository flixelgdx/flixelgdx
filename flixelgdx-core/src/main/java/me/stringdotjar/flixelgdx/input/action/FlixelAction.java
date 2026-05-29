/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
