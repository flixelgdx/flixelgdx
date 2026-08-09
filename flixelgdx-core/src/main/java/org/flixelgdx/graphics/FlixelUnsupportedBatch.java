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
package org.flixelgdx.graphics;

import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * No-op {@link FlixelBatch} returned by {@link FlixelGraphicsManager#getBatch()} on headless and
 * pre-startup sessions.
 *
 * <p>State setters remember their values so getters stay consistent, but no GPU work ever
 * happens and every render-call counter stays at zero. Callers never need to null-check the
 * result of {@link FlixelGraphicsManager#getBatch()}.
 *
 * @see FlixelGraphicsManager#getBatch()
 */
public enum FlixelUnsupportedBatch implements FlixelBatch {

  /** Shared no-op instance. */
  INSTANCE;

  private final FlixelColor color = new FlixelColor();
  private final FlixelMatrix projection = new FlixelMatrix();
  private final FlixelMatrix transform = new FlixelMatrix();

  @Nullable
  private FlixelShader shader;

  private FlixelBlendMode blendMode = FlixelBlendMode.NORMAL;

  @Override
  public void begin() {}

  @Override
  public void end() {}

  @Override
  public void flush() {}

  @Override
  public void destroy() {}

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height) {}

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
      float u, float v, float u2, float v2) {}

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {}

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation,
      boolean flipX, boolean flipY) {}

  @Override
  public void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine transform) {}

  @Override
  public int getRenderCalls() {
    return 0;
  }

  @Override
  public int getTotalRenderCalls() {
    return 0;
  }

  @NotNull
  @Override
  public FlixelColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NotNull FlixelColor color) {
    this.color.setColor(color);
  }

  @Override
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
  }

  @NotNull
  @Override
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  @Override
  public void setBlendMode(@Nullable FlixelBlendMode mode) {
    blendMode = mode != null ? mode : FlixelBlendMode.NORMAL;
  }

  @Nullable
  @Override
  public FlixelShader getShader() {
    return shader;
  }

  @Override
  public void setShader(@Nullable FlixelShader shader) {
    this.shader = shader;
  }

  @NotNull
  @Override
  public FlixelMatrix getProjection() {
    return projection;
  }

  @Override
  public void setProjection(@NotNull FlixelMatrix projection) {
    this.projection.set(projection);
  }

  @NotNull
  @Override
  public FlixelMatrix getTransform() {
    return transform;
  }

  @Override
  public void setTransform(@NotNull FlixelMatrix transform) {
    this.transform.set(transform);
  }
}
