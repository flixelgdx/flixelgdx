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
package org.flixelgdx.testing;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RecordingBatch implements FlixelBatch {

  public final FlixelArray<Call> calls = new FlixelArray<>(Call[]::new);
  private final FlixelColor color = new FlixelColor(FlixelColor.WHITE);
  private final FlixelMatrix projection = new FlixelMatrix();
  private final FlixelMatrix transform = new FlixelMatrix();
  private FlixelBlendMode blendMode;
  private FlixelShader shader;
  public int begins;
  public int ends;
  public int flushes;

  @Override
  public void begin() {
    begins++;
  }

  @Override
  public void end() {
    ends++;
  }

  @Override
  public void flush() {
    flushes++;
  }

  private void record(@Nullable FlixelTexture texture, @Nullable FlixelFrame frame, float x, float y, float w, float h,
      float u, float v, float u2, float v2, float rotation) {
    calls.add(new Call(texture, frame, x, y, w, h, u, v, u2, v2, rotation, new FlixelColor(color)));
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height) {
    record(texture, null, x, y, width, height, 0f, 0f, 1f, 1f, 0f);
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
      float u, float v, float u2, float v2) {
    record(texture, null, x, y, width, height, u, v, u2, v2, 0f);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {
    record(null, frame, x, y, width, height, 0f, 0f, 1f, 1f, 0f);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation, boolean flipX, boolean flipY) {
    record(null, frame, x, y, width * scaleX, height * scaleY, 0f, 0f, 1f, 1f, rotation);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine transform) {
    record(null, frame, 0f, 0f, width, height, 0f, 0f, 1f, 1f, 0f);
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float @NotNull [] vertices, int offset, int count) {
    record(texture, null, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f);
  }

  @Nullable
  public Call last() {
    return calls.isEmpty() ? null : calls.peek();
  }

  @Override
  public void destroy() {}

  @Override
  public int getRenderCalls() {
    return calls.getSize();
  }

  @Override
  public int getTotalRenderCalls() {
    return calls.getSize();
  }

  @Override
  public FlixelColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NotNull FlixelColor c) {
    color.set(c);
  }

  @Override
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
  }

  @Override
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  @Override
  public void setBlendMode(@Nullable FlixelBlendMode mode) {
    blendMode = mode;
  }

  @Override
  public FlixelShader getShader() {
    return shader;
  }

  @Override
  public void setShader(@Nullable FlixelShader s) {
    shader = s;
  }

  @Override
  public FlixelMatrix getProjection() {
    return projection;
  }

  @Override
  public void setProjection(@NotNull FlixelMatrix p) {
    projection.set(p);
  }

  @Override
  public FlixelMatrix getTransform() {
    return transform;
  }

  @Override
  public void setTransform(@NotNull FlixelMatrix t) {
    transform.set(t);
  }

  public record Call(@Nullable FlixelTexture texture, @Nullable FlixelFrame frame, float x, float y, float w, float h,
      float u, float v, float u2, float v2, float rotation, FlixelColor color) {
  }
}
