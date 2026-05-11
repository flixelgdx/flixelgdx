/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import org.jetbrains.annotations.NotNull;

/**
 * Optional bridge for Steam Input (or similar): digital and analog values keyed by the same logical names as
 * {@link FlixelAction#getName()}. Assign to {@link FlixelActionSet#steamReader}; {@link FlixelActionDigital} and
 * {@link FlixelActionAnalog} query it every {@link FlixelActionSet#update(float)} alongside physical bindings.
 *
 * <p>Implementations usually live outside {@code flixelgdx-core} (steamworks4j, native JNI). Keep methods allocation-free
 * and fast; they run for every registered action set each frame.
 *
 * <p>Manifest action names in {@code steam_input_manifest.vdf} (see resource under this package) should match
 * {@link FlixelAction#getName()} so Steam overlay and your UI use one vocabulary.
 */
public interface FlixelSteamActionReader {

  /**
   * Whether the named digital Steam action is active this frame.
   *
   * @param actionName Logical action name (matches VDF and {@link FlixelAction#getName()}).
   * @return {@code true} when active; default implementations may return {@code false}.
   */
  boolean getDigital(@NotNull String actionName);

  /**
   * Analog X for the named vector action, typically in {@code -1..1}.
   *
   * @param actionName Logical action name.
   * @return X component.
   */
  float getAnalogX(@NotNull String actionName);

  /**
   * Analog Y for the named vector action, typically in {@code -1..1}.
   *
   * @param actionName Logical action name.
   * @return Y component.
   */
  float getAnalogY(@NotNull String actionName);
}
