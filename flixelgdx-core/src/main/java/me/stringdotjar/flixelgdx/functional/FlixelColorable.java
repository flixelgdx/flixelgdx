/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

import com.badlogic.gdx.graphics.Color;

import me.stringdotjar.flixelgdx.util.FlixelColor;

import org.jetbrains.annotations.NotNull;

/**
 * Something with a tint you can read or write as either a libGDX {@link Color} or a
 * {@link FlixelColor} wrapper. Implementations should keep both APIs in sync on the same backing RGBA.
 *
 * @see me.stringdotjar.flixelgdx.FlixelSprite
 */
public interface FlixelColorable {

  /**
   * @return Packed RGBA8888 tint (see {@link Color#rgba8888(Color)}).
   */
  int getColor();

  /**
   * @return The backing libGDX color (often mutable). Must not be {@code null}.
   */
  @NotNull
  Color getGdxColor();

  /**
   * Copies RGBA from {@code color} into this tint.
   *
   * @param color The color to copy. Must not be {@code null}.
   */
  void setColor(@NotNull Color color);

  /**
   * Copies RGBA from {@code color} into this tint.
   *
   * @param color The wrapper to copy from. Must not be {@code null}.
   */
  void setColor(@NotNull FlixelColor color);
}
