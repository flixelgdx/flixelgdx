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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * No-op {@link FlixelBatch} returned by {@link FlixelGraphicsManager#getBatch()} on headless and
 * pre-startup sessions.
 *
 * <p>Every operation does nothing, and every query returns a safe neutral value so callers never
 * need to null-check the result of {@link FlixelGraphicsManager#getBatch()}.
 *
 * @see FlixelGraphicsManager#getBatch()
 */
public enum FlixelUnsupportedBatch implements FlixelBatch {

  /** Shared no-op instance. */
  INSTANCE;

  private final Color color = new Color();
  private final Matrix4 projectionMatrix = new Matrix4();
  private final Matrix4 transformMatrix = new Matrix4();

  @Override
  public void begin() {}

  @Override
  public void end() {}

  @Override
  public void flush() {}

  @Override
  public void disableBlending() {}

  @Override
  public void enableBlending() {}

  @Override
  public void dispose() {}

  @Override
  public void draw(@NotNull Texture texture, float x, float y) {}

  @Override
  public void draw(@NotNull TextureRegion region, float x, float y) {}

  @Override
  public void draw(@NotNull Texture texture, float[] spriteVertices, int offset, int count) {}

  @Override
  public void draw(@NotNull TextureRegion region, float width, float height, @NotNull Affine2 transform) {}

  @Override
  public void draw(@NotNull Texture texture, float x, float y, float w, float h) {}

  @Override
  public void draw(@NotNull TextureRegion region, float x, float y, float w, float h) {}

  @Override
  public void draw(@NotNull Texture texture, float x, float y, int srcX, int srcY, int srcW, int srcH) {}

  @Override
  public void draw(@NotNull Texture texture, float x, float y, float w, float h,
      float u, float v, float u2, float v2) {}

  @Override
  public void draw(@NotNull TextureRegion region, float x, float y, float originX, float originY,
      float w, float h, float scaleX, float scaleY, float rotation) {}

  @Override
  public void draw(@NotNull TextureRegion region, float x, float y, float originX, float originY,
      float w, float h, float scaleX, float scaleY, float rotation, boolean clockwise) {}

  @Override
  public void draw(@NotNull Texture texture, float x, float y, float w, float h,
      int srcX, int srcY, int srcW, int srcH, boolean flipX, boolean flipY) {}

  @Override
  public void draw(@NotNull Texture texture, float x, float y, float originX, float originY,
      float w, float h, float scaleX, float scaleY, float rotation,
      int srcX, int srcY, int srcW, int srcH, boolean flipX, boolean flipY) {}

  @Override
  public void setColor(@NotNull Color color) {}

  @Override
  public void setColor(float r, float g, float b, float a) {}

  @Override
  public void setPackedColor(float packedColor) {}

  @Override
  public void setBlendFunction(int src, int dst) {}

  @Override
  public void setBlendFunctionSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {}

  @Override
  public void setProjectionMatrix(@NotNull Matrix4 projection) {}

  @Override
  public void setTransformMatrix(@NotNull Matrix4 transform) {}

  @Override
  public void setShader(@Nullable ShaderProgram shader) {}

  @Override
  public @NotNull Color getColor() {
    return color;
  }

  @Override
  public float getPackedColor() {
    return 0f;
  }

  @Override
  public int getBlendSrcFunc() {
    return 0;
  }

  @Override
  public int getBlendDstFunc() {
    return 0;
  }

  @Override
  public int getBlendSrcFuncAlpha() {
    return 0;
  }

  @Override
  public int getBlendDstFuncAlpha() {
    return 0;
  }

  @Override
  public @NotNull Matrix4 getProjectionMatrix() {
    return projectionMatrix;
  }

  @Override
  public @NotNull Matrix4 getTransformMatrix() {
    return transformMatrix;
  }

  @Override
  public @Nullable ShaderProgram getShader() {
    return null;
  }

  @Override
  public boolean isBlendingEnabled() {
    return false;
  }

  @Override
  public boolean isDrawing() {
    return false;
  }

  @Override
  public int getRenderCalls() {
    return 0;
  }

  @Override
  public int getTotalRenderCalls() {
    return 0;
  }
}
