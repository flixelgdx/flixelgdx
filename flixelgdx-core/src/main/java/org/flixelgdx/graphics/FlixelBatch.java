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

import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The 2D sprite batch: collects textured quads and submits them to the GPU in as few draw calls
 * as possible.
 *
 * <p>Every {@link org.flixelgdx.functional.FlixelDrawable FlixelDrawable} in the framework renders
 * through the shared batch returned by {@link FlixelGraphicsManager#getBatch()}. The batch is
 * implemented by the active graphics backend, so game code drawing through this interface runs
 * unchanged on every platform.
 *
 * <p>Drawing happens between {@link #begin()} and {@link #end()}. Quads drawn with the same
 * texture, blend mode, and shader are merged into one GPU submission; switching any of those
 * forces a flush, so group draws by texture where you can (a texture atlas does this for you).
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelBatch batch = Flixel.graphics.getBatch();
 * batch.begin();
 * batch.setColor(FlixelColor.WHITE);
 * batch.draw(frame, x, y, frame.getRegionWidth(), frame.getRegionHeight());
 * batch.end();
 * }</pre>
 */
public interface FlixelBatch extends FlixelDestroyable {

  /**
   * Starts a drawing session. Must be called before any {@code draw} call, and must be paired
   * with {@link #end()}. Resets the per-session render call counter.
   */
  void begin();

  /** Ends the drawing session, flushing any remaining quads to the GPU. */
  void end();

  /** Immediately submits everything batched so far without ending the session. */
  void flush();

  /**
   * Draws a full texture stretched into the given rectangle.
   *
   * @param texture The texture to draw.
   * @param x Left edge in world units.
   * @param y Bottom edge in world units.
   * @param width Drawn width in world units.
   * @param height Drawn height in world units.
   */
  void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height);

  /**
   * Draws a rectangle of a texture selected by normalized coordinates.
   *
   * @param texture The texture to draw.
   * @param x Left edge in world units.
   * @param y Bottom edge in world units.
   * @param width Drawn width in world units.
   * @param height Drawn height in world units.
   * @param u Left texture coordinate in {@code [0, 1]}.
   * @param v Top texture coordinate in {@code [0, 1]}.
   * @param u2 Right texture coordinate in {@code [0, 1]}.
   * @param v2 Bottom texture coordinate in {@code [0, 1]}.
   */
  void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
      float u, float v, float u2, float v2);

  /**
   * Draws a frame (texture region) into the given rectangle.
   *
   * @param frame The frame to draw.
   * @param x Left edge in world units.
   * @param y Bottom edge in world units.
   * @param width Drawn width in world units.
   * @param height Drawn height in world units.
   */
  void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height);

  /**
   * Draws a frame with scaling, rotation, and mirroring around an origin point.
   *
   * <p>This is the workhorse overload sprites render through. The origin is measured from the
   * quad's bottom-left corner in unscaled pixels; scaling and rotation happen around it.
   *
   * @param frame The frame to draw.
   * @param x Left edge of the unscaled quad in world units.
   * @param y Bottom edge of the unscaled quad in world units.
   * @param originX Origin x, relative to {@code x}.
   * @param originY Origin y, relative to {@code y}.
   * @param width Unscaled quad width.
   * @param height Unscaled quad height.
   * @param scaleX Horizontal scale factor around the origin.
   * @param scaleY Vertical scale factor around the origin.
   * @param rotation Rotation in degrees, counter-clockwise around the origin.
   * @param flipX {@code true} to mirror horizontally.
   * @param flipY {@code true} to mirror vertically.
   */
  void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation,
      boolean flipX, boolean flipY);

  /**
   * Draws a frame through an arbitrary affine transform, used by rigged and skewed rendering.
   *
   * @param frame The frame to draw.
   * @param width Quad width before the transform.
   * @param height Quad height before the transform.
   * @param transform Affine transform applied to the quad's corners.
   */
  void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine transform);

  /**
   * Returns the number of GPU draw submissions issued by this batch since the last
   * {@link #begin()} call. This counter resets to zero at the start of each session.
   *
   * @return Per-frame render call count.
   */
  int getRenderCalls();

  /**
   * Returns the cumulative number of GPU draw submissions issued by this batch since it was
   * created. Unlike {@link #getRenderCalls()}, this value never resets.
   *
   * @return Total render call count since construction.
   */
  int getTotalRenderCalls();

  /**
   * Returns the tint applied to subsequent draws.
   *
   * @return The live tint color; treat as read-only and use {@link #setColor(FlixelColor)} to change it.
   */
  @NotNull
  FlixelColor getColor();

  /**
   * Sets the tint color multiplied into every following draw.
   *
   * @param color The tint; copied into the batch's own color.
   */
  void setColor(@NotNull FlixelColor color);

  /**
   * Sets the tint color multiplied into every following draw.
   *
   * @param r Red component in {@code [0, 1]}.
   * @param g Green component in {@code [0, 1]}.
   * @param b Blue component in {@code [0, 1]}.
   * @param a Alpha component in {@code [0, 1]}.
   */
  void setColor(float r, float g, float b, float a);

  /**
   * @return The blend mode applied to subsequent draws. Defaults to {@link FlixelBlendMode#NORMAL}.
   */
  @NotNull
  FlixelBlendMode getBlendMode();

  /**
   * Switches the blend mode for subsequent draws, flushing pending quads first when it changes.
   *
   * @param mode The blend mode; {@code null} resets to {@link FlixelBlendMode#NORMAL}.
   */
  void setBlendMode(@Nullable FlixelBlendMode mode);

  /**
   * @return The custom shader in effect, or {@code null} when the backend's default sprite shader is active.
   */
  @Nullable
  FlixelShader getShader();

  /**
   * Switches the shader used for subsequent draws, flushing pending quads first when it changes.
   *
   * @param shader The shader to draw with, or {@code null} to restore the default sprite shader.
   */
  void setShader(@Nullable FlixelShader shader);

  /**
   * Returns the projection matrix mapping world units to the screen.
   *
   * @return The live projection matrix; use {@link #setProjection(FlixelMatrix)} to change it.
   */
  @NotNull
  FlixelMatrix getProjection();

  /**
   * Sets the projection matrix, flushing pending quads first. Cameras call this before drawing
   * their view.
   *
   * @param projection The new projection matrix; copied.
   */
  void setProjection(@NotNull FlixelMatrix projection);
}
