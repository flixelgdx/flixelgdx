/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class related to {@link me.stringdotjar.flixelgdx.FlixelSprite}.
 *
 * <p>These utilities are designed to work with FlixelGDX's normal Batch-based draw flow.
 * Avoid using ShapeRenderer in core game rendering unless you control the render pipeline.
 *
 * <p>Do not call {@link #createWhitePixelTexture()} every frame or per
 * sprite instance in hot paths. That allocates new {@link Pixmap} and {@link Texture} objects and
 * will spike heap usage on most JVMs. Prefer {@link #obtainWhitePixelTexture(FlixelAssetManager)},
 * which registers a single persistent texture with the asset manager (see
 * {@link me.stringdotjar.flixelgdx.asset.FlixelDefaultAssetManager}) and reuses it for the lifetime of the game.
 *
 * <p>{@link #fill} writes solid pixels into the active frame when possible, and {@link #setBrightness} adjusts tint
 * like Animate brightness.
 */
public final class FlixelSpriteUtil {

  private FlixelSpriteUtil() {}

  private static final Vector2 TMP_V2 = new Vector2();

  /**
   * Fixed asset key for the framework-owned 1x1 white {@link Texture} registered via
   * {@link #obtainWhitePixelTexture(FlixelAssetManager)}.
   */
  public static final String WHITE_PIXEL_TEXTURE_KEY = "__flixel_internal__/white_pixel_1x1";

  private static final Object WHITE_PIXEL_LOCK = new Object();

  /**
   * Returns the shared 1x1 white {@link Texture} registered with {@code assets}. The first call
   * creates the texture, wraps it in a {@link FlixelGraphic} with {@link FlixelGraphic#setPersist(boolean)}
   * {@code true}, and registers it with {@link FlixelAssetManager#registerWrapper}.
   * Callers must not {@link Texture#dispose()} this texture; lifecycle follows the asset manager.
   *
   * @param assets Non-null manager from {@link me.stringdotjar.flixelgdx.Flixel#ensureAssets()}.
   */
  @NotNull
  public static Texture obtainWhitePixelTexture(@NotNull FlixelAssetManager assets) {
    synchronized (WHITE_PIXEL_LOCK) {
      FlixelGraphic existing = assets.peekWrapper(WHITE_PIXEL_TEXTURE_KEY, FlixelGraphic.class);
      if (existing != null) {
        return existing.require();
      }
      Texture t = createWhitePixelTexture();
      FlixelGraphic g = new FlixelGraphic(assets, WHITE_PIXEL_TEXTURE_KEY, t);
      g.setPersist(true);
      assets.registerWrapper(g);
      return g.require();
    }
  }

  /**
   * Creates a 1x1 white pixel texture.
   *
   * <p><b>Ownership:</b> Prefer {@link #obtainWhitePixelTexture(FlixelAssetManager)} so only one
   * instance exists. If you call this directly, you own the returned texture and must dispose it.
   *
   * @return The created white pixel texture.
   */
  public static Texture createWhitePixelTexture() {
    Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    px.setColor(Color.WHITE);
    px.fill();
    Texture t = new Texture(px);
    px.dispose();
    return t;
  }

  /**
   * Draws a rectangle with the given color.
   *
   * @param batch The batch to draw the rectangle on.
   * @param whitePixel The white pixel texture to use for the rectangle.
   * @param x The x position of the rectangle.
   * @param y The y position of the rectangle.
   * @param w The width of the rectangle.
   * @param h The height of the rectangle.
   * @param color The color of the rectangle.
   */
  public static void drawRect(@NotNull Batch batch,
                              @NotNull Texture whitePixel,
                              float x,
                              float y,
                              float w,
                              float h,
                              @NotNull Color color) {
    Objects.requireNonNull(batch, "The batch provided cannot be null!");
    Objects.requireNonNull(whitePixel, "The white pixel texture provided cannot be null!");
    Objects.requireNonNull(color, "The color provided cannot be null!");
    batch.setColor(color);
    batch.draw(whitePixel, x, y, w, h);
    batch.setColor(Color.WHITE);
  }

  /**
   * Draws a border around the given rectangle.
   *
   * @param batch The batch to draw the border on.
   * @param whitePixel The white pixel texture to use for the border.
   * @param x The x position of the rectangle.
   * @param y The y position of the rectangle.
   * @param w The width of the rectangle.
   * @param h The height of the rectangle.
   * @param thickness The thickness of the border.
   * @param color The color of the border.
   */
  public static void drawBorder(@NotNull Batch batch,
                                @NotNull Texture whitePixel,
                                float x,
                                float y,
                                float w,
                                float h,
                                float thickness,
                                @NotNull Color color) {
    Objects.requireNonNull(batch, "The batch provided cannot be null!");
    Objects.requireNonNull(whitePixel, "The white pixel texture provided cannot be null!");
    Objects.requireNonNull(color, "The color provided cannot be null!");
    thickness = Math.max(0f, thickness);
    if (thickness <= 0f) {
      return;
    }
    batch.setColor(color);
    // Top.
    batch.draw(whitePixel, x, y + h - thickness, w, thickness);
    // Bottom.
    batch.draw(whitePixel, x, y, w, thickness);
    // Left.
    batch.draw(whitePixel, x, y, thickness, h);
    // Right.
    batch.draw(whitePixel, x + w - thickness, y, thickness, h);
    batch.setColor(Color.WHITE);
  }

  /**
   * Creates a 2-color linear gradient {@link Pixmap}.
   *
   * <p>Caller owns the returned Pixmap and must dispose of it through {@link Pixmap#dispose()} themselves.
   *
   * @param width The width of the gradient.
   * @param height The height of the gradient.
   * @param start The start color of the gradient.
   * @param end The end color of the gradient.
   * @param horizontal Whether the gradient is horizontal.
   * @return The created gradient Pixmap.
   */
  public static Pixmap createLinearGradientPixmap(int width,
                                                  int height,
                                                  @NotNull Color start,
                                                  @NotNull Color end,
                                                  boolean horizontal) {
    Objects.requireNonNull(start, "The start color provided cannot be null!");
    Objects.requireNonNull(end, "The end color provided cannot be null!");
    width = Math.max(1, width);
    height = Math.max(1, height);
    Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);

    float r0 = start.r;
    float g0 = start.g;
    float b0 = start.b;
    float a0 = start.a;
    float r1 = end.r;
    float g1 = end.g;
    float b1 = end.b;
    float a1 = end.a;

    for (int i = 0; i < (horizontal ? width : height); i++) {
      setInterpolatedColor(pm, i, horizontal ? width : height, r0, r1, g0, g1, b0, b1, a0, a1);
      for (int j = 0; j < (horizontal ? height : width); j++) {
        pm.drawPixel(horizontal ? i : j, horizontal ? j : i);
      }
    }

    return pm;
  }

  /**
   * Creates a 2-color linear gradient {@link Texture}.
   *
   * <p>Caller owns the returned Texture and must dispose of it through {@link Texture#dispose()} themselves.
   *
   * @param width The width of the gradient.
   * @param height The height of the gradient.
   * @param start The start color of the gradient.
   * @param end The end color of the gradient.
   * @param horizontal Whether the gradient is horizontal.
   * @return The created gradient Texture.
   */
  public static Texture createLinearGradientTexture(int width,
                                                    int height,
                                                    @NotNull Color start,
                                                    @NotNull Color end,
                                                    boolean horizontal) {
    Objects.requireNonNull(start, "The start color provided cannot be null!");
    Objects.requireNonNull(end, "The end color provided cannot be null!");
    Pixmap pm = createLinearGradientPixmap(width, height, start, end, horizontal);
    Texture t = new Texture(pm);
    pm.dispose();
    return t;
  }

  private static void setInterpolatedColor(Pixmap pm, int position, int dimension,
                                    float r0, float r1, float g0, float g1,
                                    float b0, float b1, float a0, float a1) {
    float t = dimension <= 1 ? 1f : (position / (float) (dimension - 1));
    pm.setColor(
      MathUtils.lerp(r0, r1, t),
      MathUtils.lerp(g0, g1, t),
      MathUtils.lerp(b0, b1, t),
      MathUtils.lerp(a0, a1, t)
    );
  }

  /**
   * Clamps the sprite inside a world rectangle. When {@code maxX} or {@code maxY} are {@code 0} or less, the game
   * view size from {@link Flixel#getViewWidth()} / {@link Flixel#getViewHeight()} is used for that axis.
   *
   * @param sprite The sprite to bound.
   * @param minX The minimum x position.
   * @param maxX The maximum x position.
   * @param minY The minimum y position.
   * @param maxY The maximum y position.
   * @return The bounded sprite.
   */
  @NotNull
  public static FlixelSprite bound(@NotNull FlixelSprite sprite,
                                   float minX,
                                   float maxX,
                                   float minY,
                                   float maxY) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    float maxXb = maxX > 0f ? maxX : Flixel.getViewWidth();
    float maxYb = maxY > 0f ? maxY : Flixel.getViewHeight();
    float w = sprite.getWidth() * Math.abs(sprite.getScaleX());
    float h = sprite.getHeight() * Math.abs(sprite.getScaleY());
    if (sprite.getX() < minX) {
      sprite.setX(minX);
    }
    if (sprite.getY() < minY) {
      sprite.setY(minY);
    }
    if (sprite.getX() + w > maxXb) {
      sprite.setX(maxXb - w);
    }
    if (sprite.getY() + h > maxYb) {
      sprite.setY(maxYb - h);
    }
    return sprite;
  }

  /**
   * Wraps the sprite when it leaves the game view (same default max as {@link #bound} when zeros).
   *
   * @param sprite The sprite to wrap.
   * @param left Whether to wrap the sprite when it leaves the left edge of the game view.
   * @param right Whether to wrap the sprite when it leaves the right edge of the game view.
   * @param top Whether to wrap the sprite when it leaves the top edge of the game view.
   * @param bottom Whether to wrap the sprite when it leaves the bottom edge of the game view.
   * @return The wrapped sprite.
   */
  @NotNull
  public static FlixelSprite screenWrap(@NotNull FlixelSprite sprite,
                                        boolean left,
                                        boolean right,
                                        boolean top,
                                        boolean bottom) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    float maxXb = Flixel.getViewWidth();
    float maxYb = Flixel.getViewHeight();
    float w = sprite.getWidth() * Math.abs(sprite.getScaleX());
    float h = sprite.getHeight() * Math.abs(sprite.getScaleY());
    float x = sprite.getX();
    float y = sprite.getY();
    if (left && x + w < 0f) {
      sprite.setX(maxXb);
    }
    if (right && x > maxXb) {
      sprite.setX(-w);
    }
    if (top && y + h < 0f) {
      sprite.setY(maxYb);
    }
    if (bottom && y > maxYb) {
      sprite.setY(-h);
    }
    return sprite;
  }

  /**
   * Keeps the sprite inside the given camera world view ({@link FlixelCamera#getViewLeft()} etc.).
   *
   * @param sprite The sprite to bound.
   * @param camera The camera to bound the sprite to.
   * @param left Whether to bound the sprite when it leaves the left edge of the camera view.
   * @param right Whether to bound the sprite when it leaves the right edge of the camera view.
   * @param top Whether to bound the sprite when it leaves the top edge of the camera view.
   * @param bottom Whether to bound the sprite when it leaves the bottom edge of the camera view.
   * @return The bounded sprite.
   */
  @NotNull
  public static FlixelSprite cameraBound(@NotNull FlixelSprite sprite,
                                         @Nullable FlixelCamera camera,
                                         boolean left,
                                         boolean right,
                                         boolean top,
                                         boolean bottom) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    FlixelCamera cam = camera != null ? camera : Flixel.getCamera();
    float minX = cam.getViewLeft();
    float minY = cam.getViewTop();
    float maxX = cam.getViewRight();
    float maxY = cam.getViewBottom();
    float w = sprite.getWidth() * Math.abs(sprite.getScaleX());
    float h = sprite.getHeight() * Math.abs(sprite.getScaleY());
    if (left && sprite.getX() < minX) {
      sprite.setX(minX);
    }
    if (top && sprite.getY() < minY) {
      sprite.setY(minY);
    }
    if (right && sprite.getX() + w > maxX) {
      sprite.setX(maxX - w);
    }
    if (bottom && sprite.getY() + h > maxY) {
      sprite.setY(maxY - h);
    }
    return sprite;
  }

  /**
   * Wraps the sprite relative to the camera view edges.
   *
   * @param sprite The sprite to wrap.
   * @param camera The camera to wrap the sprite relative to.
   * @param left Whether to wrap the sprite when it leaves the left edge of the camera view.
   * @param right Whether to wrap the sprite when it leaves the right edge of the camera view.
   * @param top Whether to wrap the sprite when it leaves the top edge of the camera view.
   * @param bottom Whether to wrap the sprite when it leaves the bottom edge of the camera view.
   * @return The wrapped sprite.
   */
  @NotNull
  public static FlixelSprite cameraWrap(@NotNull FlixelSprite sprite,
                                        @Nullable FlixelCamera camera,
                                        boolean left,
                                        boolean right,
                                        boolean top,
                                        boolean bottom) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    FlixelCamera cam = camera != null ? camera : Flixel.getCamera();
    float minX = cam.getViewLeft();
    float minY = cam.getViewTop();
    float maxX = cam.getViewRight();
    float maxY = cam.getViewBottom();
    float w = sprite.getWidth() * Math.abs(sprite.getScaleX());
    float h = sprite.getHeight() * Math.abs(sprite.getScaleY());
    float x = sprite.getX();
    float y = sprite.getY();
    if (left && x + w < minX) {
      sprite.setX(maxX);
    }
    if (right && x > maxX) {
      sprite.setX(minX - w);
    }
    if (top && y + h < minY) {
      sprite.setY(maxY);
    }
    if (bottom && y > maxY) {
      sprite.setY(minY - h);
    }
    return sprite;
  }

  /**
   * Fills the active bitmap area with a solid color, similar to HaxeFlixel {@code FlxSpriteUtil.fill}.
   *
   * <p>When the sprite uses an <strong>owned</strong> pixmap-backed {@link Texture} ({@link FlixelSprite#hasOwnedGraphic()}),
   * every pixel in the current {@link TextureRegion} (animation frame or static region) is overwritten via
   * {@link Texture#draw(Pixmap, int, int)} without replacing the graphic.
   *
   * <p>Otherwise (shared atlas, unloaded graphic, or non-pixmap GPU texture) this falls back to
   * {@link FlixelSprite#makeGraphic(int, int, Color)} sized to the logical frame ({@link FlixelFrame#originalWidth} /
   * {@link FlixelFrame#originalHeight} when set, else region size, else object size).
   *
   * @param sprite The sprite to fill.
   * @param fillColor The color to fill with (including alpha).
   * @return {@code sprite} for chaining.
   */
  @NotNull
  public static FlixelSprite fill(@NotNull FlixelSprite sprite, @NotNull Color fillColor) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    Objects.requireNonNull(fillColor, "The fill color provided cannot be null!");
    if (tryFillActiveRegionPixels(sprite, fillColor)) {
      return sprite;
    }
    int iw = resolveFillBufferWidth(sprite);
    int ih = resolveFillBufferHeight(sprite);
    return sprite.makeGraphic(iw, ih, fillColor);
  }

  /**
   * Adjusts RGB toward white ({@code brightness = 1}) or black ({@code brightness = -1}), matching Adobe Animate style
   * brightness as described for FlixelGDX {@code FlixelSpriteUtil.setBrightness}. Alpha is preserved.
   *
   * @param sprite The sprite to set the brightness of.
   * @param brightness Clamped to {@code [-1, 1]}.
   */
  public static void setBrightness(@NotNull FlixelSprite sprite, float brightness) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    brightness = MathUtils.clamp(brightness, -1f, 1f);
    Color c = sprite.getColor();
    float r = c.r;
    float g = c.g;
    float b = c.b;
    float a = c.a;
    if (brightness >= 0f) {
      r = MathUtils.lerp(r, 1f, brightness);
      g = MathUtils.lerp(g, 1f, brightness);
      b = MathUtils.lerp(b, 1f, brightness);
    } else {
      float t = -brightness;
      r = MathUtils.lerp(r, 0f, t);
      g = MathUtils.lerp(g, 0f, t);
      b = MathUtils.lerp(b, 0f, t);
    }
    sprite.setColor(r, g, b, a);
  }

  private static boolean tryFillActiveRegionPixels(@NotNull FlixelSprite sprite, @NotNull Color fillColor) {
    if (!sprite.hasOwnedGraphic()) {
      return false;
    }
    Texture tex = sprite.getTexture();
    if (tex == null) {
      return false;
    }
    TextureData data = tex.getTextureData();
    if (!data.isPrepared()) {
      data.prepare();
    }
    if (data.getType() != TextureData.TextureDataType.Pixmap) {
      return false;
    }
    TextureRegion region = resolveActiveRegion(sprite);
    if (region == null) {
      return false;
    }
    int rw = region.getRegionWidth();
    int rh = region.getRegionHeight();
    int rx = region.getRegionX();
    int ry = region.getRegionY();
    if (rw <= 0 || rh <= 0) {
      return false;
    }
    Pixmap pm = new Pixmap(rw, rh, Pixmap.Format.RGBA8888);
    pm.setColor(fillColor);
    pm.fill();
    tex.draw(pm, rx, ry);
    pm.dispose();
    return true;
  }

  @Nullable
  private static TextureRegion resolveActiveRegion(@NotNull FlixelSprite sprite) {
    FlixelFrame anim = sprite.getCurrentFrame();
    if (anim != null) {
      return anim.getRegion();
    }
    return sprite.getRegion();
  }

  private static int resolveFillBufferWidth(@NotNull FlixelSprite sprite) {
    FlixelFrame f = sprite.getCurrentFrame();
    if (f != null && f.originalWidth > 0) {
      return f.originalWidth;
    }
    int rw = sprite.getRegionWidth();
    if (rw > 0) {
      return rw;
    }
    return Math.max(1, Math.round(sprite.getWidth()));
  }

  private static int resolveFillBufferHeight(@NotNull FlixelSprite sprite) {
    FlixelFrame f = sprite.getCurrentFrame();
    if (f != null && f.originalHeight > 0) {
      return f.originalHeight;
    }
    int rh = sprite.getRegionHeight();
    if (rh > 0) {
      return rh;
    }
    return Math.max(1, Math.round(sprite.getHeight()));
  }

  /**
   * Draws a line segment using a 1x1 texture and {@link Batch}. Does not allocate.
   *
   * @param batch The batch to draw the line on.
   * @param whitePixel The white pixel texture to use for the line.
   * @param startX The x position of the start of the line.
   * @param startY The y position of the start of the line.
   * @param endX The x position of the end of the line.
   * @param endY The y position of the end of the line.
   * @param thickness The thickness of the line.
   * @param color The color of the line.
   */
  public static void drawLine(@NotNull Batch batch,
                              @NotNull Texture whitePixel,
                              float startX,
                              float startY,
                              float endX,
                              float endY,
                              float thickness,
                              @NotNull Color color) {
    Objects.requireNonNull(batch, "The batch provided cannot be null!");
    Objects.requireNonNull(whitePixel, "The white pixel texture provided cannot be null!");
    Objects.requireNonNull(color, "The color provided cannot be null!");
    thickness = Math.max(0f, thickness);
    if (thickness <= 0f) {
      return;
    }
    float dx = endX - startX;
    float dy = endY - startY;
    float len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len <= 0.0001f) {
      return;
    }
    float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
    batch.setColor(color);
    batch.draw(whitePixel, startX, startY - thickness * 0.5f, 0f, thickness * 0.5f, len, thickness, 1f, 1f, angle, 0, 0, 1, 1, false, false);
    batch.setColor(Color.WHITE);
  }

  /**
   * Midpoint of the given sprite's hitbox in world space. Reuses {@code out} when non-null.
   *
   * @param sprite The sprite to get the midpoint of.
   * @param out The vector to store the midpoint in.
   * @return The midpoint of the sprite.
   */
  @NotNull
  public static Vector2 getMidpoint(@NotNull FlixelSprite sprite, @Nullable Vector2 out) {
    Objects.requireNonNull(sprite, "The sprite provided cannot be null!");
    Vector2 v = out != null ? out : TMP_V2;
    float w = sprite.getWidth() * Math.abs(sprite.getScaleX());
    float h = sprite.getHeight() * Math.abs(sprite.getScaleY());
    v.set(sprite.getX() + w * 0.5f, sprite.getY() + h * 0.5f);
    return v;
  }
}
