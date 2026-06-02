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
package org.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * Pooled wrapper registered through {@link org.flixelgdx.asset.FlixelAssetManager} ({@code registerWrapper}) for
 * {@link org.flixelgdx.asset.FlixelAssetManager#clearNonPersist()} lifecycle.
 */
public interface FlixelPooledWrapper {

  /**
   * Gets and returns the asset key associated with {@code this} wrapper.
   *
   * @return The asset key.
   */
  @NotNull
  String getAssetKey();

  /**
   * @return The wrapper class used to look up a {@link FlixelWrapperFactory} (typically the concrete
   *   wrapper class, e.g. {@link org.flixelgdx.graphics.FlixelGraphic}.class).
   */
  @NotNull
  default Class<?> wrapperRegistrationClass() {
    return getClass();
  }

  /**
   * Whether this wrapper holds a dedicated resource the pool must dispose (for example {@link org.flixelgdx.graphics.FlixelGraphic}
   * with a pixmap or caller texture). This is structural, not the same as {@link FlixelAsset#isPersist()}; see
   * {@link org.flixelgdx.graphics.FlixelGraphic} and {@link FlixelAssetManager#getGlobalPersist()}.
   *
   * @return {@code true} if the wrapper is owned, {@code false} otherwise.
   */
  boolean isOwned();
}
