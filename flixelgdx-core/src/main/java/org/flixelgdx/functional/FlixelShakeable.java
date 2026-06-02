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
package org.flixelgdx.functional;

import com.badlogic.gdx.Gdx;
import org.flixelgdx.tween.settings.FlixelShakeUnit;

/**
 * Something {@link org.flixelgdx.tween.type.FlixelShakeTween} can jitter and restore without caring whether
 * the underlying channel is a sprite graphic offset, a world position, or a desktop window position.
 *
 * <p>Implementations choose what X and Y mean: {@link org.flixelgdx.FlixelSprite} uses graphic
 * {@linkplain org.flixelgdx.FlixelSprite#getOffsetX() offset}; {@link org.flixelgdx.FlixelObject}
 * uses world {@linkplain org.flixelgdx.functional.FlixelPositional position};
 * {@link org.flixelgdx.backend.window.FlixelWindow} uses window placement in screen coordinates.
 *
 * @see org.flixelgdx.tween.type.FlixelShakeTween
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
   * Width used to scale {@link FlixelShakeUnit#FRACTION} on the X axis.
   * Values {@code 0} or less mean the tween treats intensity like plain pixels on that axis.
   *
   * @return Reference width for fractional shake, or non-positive to fall back.
   */
  default float getShakeWidth() {
    return 0f;
  }

  /**
   * Height used to scale {@link FlixelShakeUnit#FRACTION} on the Y axis.
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
