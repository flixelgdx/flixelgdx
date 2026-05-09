/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

import com.badlogic.gdx.Gdx;

/**
 * Something {@link me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween} can jitter and restore without caring whether
 * the underlying channel is a sprite graphic offset, a world position, or a desktop window position.
 *
 * <p>Implementations choose what X and Y mean: {@link me.stringdotjar.flixelgdx.FlixelSprite} uses graphic
 * {@linkplain me.stringdotjar.flixelgdx.FlixelSprite#getOffsetX() offset}; {@link me.stringdotjar.flixelgdx.FlixelObject}
 * uses world {@linkplain me.stringdotjar.flixelgdx.functional.FlixelPositional position};
 * {@link me.stringdotjar.flixelgdx.backend.window.FlixelWindow} uses window placement in screen coordinates.
 *
 * @see me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween
 */
public interface FlixelShakeable {

  /**
   * Current shake channel X (for example graphic offset or world X).
   *
   * @return Shake X before jitter is applied.
   */
  float getShakeX();

  /**
   * Current shake channel Y.
   *
   * @return Shake Y before jitter is applied.
   */
  float getShakeY();

  /**
   * Sets the shake channel to absolute coordinates (typically base plus jitter).
   *
   * @param x New shake X.
   * @param y New shake Y.
   */
  void setShake(float x, float y);

  /**
   * Width used to scale {@link me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween.ShakeUnit#FRACTION} on the X axis.
   * Values {@code 0} or less mean the tween treats intensity like plain pixels on that axis.
   *
   * @return Reference width for fractional shake, or non-positive to fall back.
   */
  default float getShakeWidth() {
    return 0f;
  }

  /**
   * Height used to scale {@link me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween.ShakeUnit#FRACTION} on the Y axis.
   * Values {@code 0} or less mean the tween treats intensity like plain pixels on that axis.
   *
   * @return Reference height for fractional shake, or non-positive to fall back.
   */
  default float getShakeHeight() {
    return 0f;
  }

  /**
   * Convenience default for fullscreen targets: uses the back buffer size when libGDX graphics is available.
   *
   * @return Positive width in pixels, or {@code 0f} if unknown.
   */
  default float shakeFractionWidthFromGraphics() {
    if (Gdx.graphics == null) {
      return 0f;
    }
    int w = Gdx.graphics.getBackBufferWidth();
    return w > 0 ? w : 0f;
  }

  /**
   * Convenience default for fullscreen targets: uses the back buffer size when libGDX graphics is available.
   *
   * @return Positive height in pixels, or {@code 0f} if unknown.
   */
  default float shakeFractionHeightFromGraphics() {
    if (Gdx.graphics == null) {
      return 0f;
    }
    int h = Gdx.graphics.getBackBufferHeight();
    return h > 0 ? h : 0f;
  }
}
