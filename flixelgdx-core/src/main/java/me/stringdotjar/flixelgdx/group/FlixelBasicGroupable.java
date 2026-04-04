/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import com.badlogic.gdx.utils.Pool;

import me.stringdotjar.flixelgdx.FlixelBasic;

import org.jetbrains.annotations.NotNull;

/**
 * A {@link FlixelGroupable} whose members are constrained to {@link FlixelBasic} and are always tied to a
 * mandatory {@link Pool}. {@link FlixelGroup#remove} returns instances to that pool (after {@code reset}, which
 * invokes {@link FlixelBasic#destroy}).
 *
 * <p>Use this in engine systems that assume FlixelGDX lifecycle and fields like {@code exists}, {@code active},
 * or {@code visible}. External libGDX projects that do not want to extend {@link FlixelBasic} can implement
 * {@link FlixelGroupable} directly instead.
 *
 * @param <T> The member type.
 */
public interface FlixelBasicGroupable<T extends FlixelBasic> extends FlixelGroupable<T> {

  @NotNull
  Pool<T> getMemberPool();

  /**
   * Obtains from {@link #getMemberPool()}, revives, and sets {@code active} and {@code visible}. Does not add to the
   * group. Call {@link #add} yourself after configuring the instance.
   */
  default @NotNull T obtainMember() {
    T t = getMemberPool().obtain();
    t.revive();
    t.active = true;
    t.visible = true;
    return t;
  }

  /**
   * Same as {@link #obtainMember()} but checks runtime type. If the pool produces a different class, the instance is
   * freed back to the pool and an exception is thrown. Use when your {@link Pool#newObject()} is typed to a single
   * concrete subclass (for example only {@link me.stringdotjar.flixelgdx.FlixelSprite}).
   */
  default @NotNull <C extends T> C obtainMemberAs(@NotNull Class<C> type) {
    T t = obtainMember();
    if (!type.isInstance(t)) {
      getMemberPool().free(t);
      throw new IllegalArgumentException(
        "Pool produced " + t.getClass().getName() + " but " + type.getName() + " was requested. Use a pool whose newObject() returns the expected type.");
    }
    return type.cast(t);
  }
}
