/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

/**
 * Full {@link me.stringdotjar.flixelgdx.FlixelBasic}-style contract: per-frame update and draw hooks,
 * existence and active flags, visibility, kill and revive, teardown, and libGDX {@link Disposable} /
 * {@link Pool.Poolable} hooks. Extend {@link me.stringdotjar.flixelgdx.FlixelBasic} when you want the
 * default field-based implementation, or implement this interface on your own type when you need a
 * custom base class but still want to add instances to a {@link me.stringdotjar.flixelgdx.FlixelState}
 * or {@link me.stringdotjar.flixelgdx.group.FlixelBasicGroup}.
 *
 * @see me.stringdotjar.flixelgdx.FlixelBasic
 * @see me.stringdotjar.flixelgdx.group.FlixelBasicGroup
 */
public interface IFlixelBasic extends
    FlixelUpdatable,
    FlixelDrawable,
    FlixelDestroyable,
    FlixelKillable,
    FlixelVisible,
    Disposable,
    Pool.Poolable {

  /**
   * When {@code false}, groups and states skip this instance for automatic {@link #update(float)}.
   *
   * @return The current {@code exists} flag.
   */
  boolean isExists();

  /**
   * @param exists The new {@code exists} flag.
   */
  void setExists(boolean exists);

  /**
   * When {@code false}, {@link #update(float)} is skipped even if {@link #isExists()} is {@code true}.
   *
   * @return The current {@code active} flag.
   */
  boolean isActive();

  /**
   * @param active The new {@code active} flag.
   */
  void setActive(boolean active);
}
