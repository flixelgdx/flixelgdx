/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.group.FlixelSpriteGroup;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link FlixelSpriteGroup} for <strong>Adobe Animate / BTA-style</strong> texture atlases: {@code spritemapX.json}
 * (with {@code ATLAS.SPRITES}) plus {@code Animation.json} with a symbol dictionary ({@code SD}).
 *
 * <p>When the animation JSON has {@code SD.S}, the loader composes <strong>every</strong> {@code ASI} (bitmap) using
 * {@code main SI.MX} × nested {@code SI.MX} × {@code ASI.MX} (same idea as
 * <a href="https://github.com/Dot-Stuff/flxanimate">flxanimate</a>), instead of a single “largest cell”
 * per frame. Pass {@code me/.../spritemap1.png} as the {@code textureKey} and
 * {@code me/.../spritemap1.json} + {@code me/.../Animation.json} as the paths.
 *
 * <p>Group position, scale, color, scroll, and offset behave like a normal {@link FlixelSprite}. The rig is
 * drawn with {@link SpriteBatch} and {@link Affine2} (requires a {@code SpriteBatch} for this path).
 */
public class FlixelAnimateSprite extends FlixelSpriteGroup {

  @Nullable
  private FlixelBtaCompositing bta;

  private final Matrix3 m3Base = new Matrix3();
  private final Matrix3 m3PartM = new Matrix3();
  private final Matrix3 m3Combined = new Matrix3();
  private final Matrix3 m3Scale = new Matrix3();
  private final Affine2 drawAffine = new Affine2();

  public FlixelAnimateSprite() {
    super();
  }

  /**
   * Creates a new Adobe Animate / BTA-style sprite with the given maximum size, rotation radius, and rotation.
   * 
   * @param maxSize Maximum number of child sprites, or {@code 0} for unlimited.
   * @param rotationRadius Radius for {@link RotationMode#WHEEL}.
   * @param rotation Initial group angle in degrees.
   */
  public FlixelAnimateSprite(int maxSize, float rotationRadius, float rotation) {
    super(maxSize, rotationRadius, rotation);
  }

  /**
   * @return BTA multi-part data after a successful {@link #loadSpritemapAndAnimation} with {@code SD}, else
   * {@code null}.
   */
  @Nullable
  public FlixelBtaCompositing getBtaCompositing() {
    return bta;
  }

  @Override
  public void setCurrentFrameForAnimation(@Nullable FlixelFrame frame) {
    if (bta != null) {
      return;
    }
    super.setCurrentFrameForAnimation(frame);
  }

  @Override
  public void draw(Batch batch) {
    if (!isVisible() || !isOnDrawCamera()) {
      return;
    }
    if (bta == null) {
      super.draw(batch);
      return;
    }
    FlixelAnimationController an = animation;
    if (an == null) {
      return;
    }
    String name = an.getCurrentAnim();
    if (name == null || name.isEmpty()) {
      return;
    }
    FlixelBtaCompositing.NamedClip clip = bta.getClip(name);
    if (clip == null) {
      return;
    }
    int fi = an.getCurrentKeyframeIndex();
    if (fi < 0 || fi >= clip.keyframes.length) {
      fi = 0;
    }
    FlixelBtaCompositing.Keyframe kf = clip.keyframes[fi];
    Array<FlixelFrame> atlas = getAtlasRegions();
    if (atlas == null) {
      return;
    }
    if (!(batch instanceof SpriteBatch)) {
      return;
    }
    SpriteBatch sb = (SpriteBatch) batch;
    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.getCamera();
    float wx = getX() - cam.scroll.x * getScrollX() - getOffsetX();
    float wy = getY() - cam.scroll.y * getScrollY() - getOffsetY();

    m3Base.idt();
    m3Base.val[Matrix3.M02] = wx;
    m3Base.val[Matrix3.M12] = wy;
    if (getScaleX() != 1f || getScaleY() != 1f) {
      m3Scale.setToScaling(getScaleX(), getScaleY());
      m3Base.mul(m3Scale);
    }
    Color c = getColor();
    for (int i = 0; i < kf.parts.length; i++) {
      FlixelBtaCompositing.Part p = kf.parts[i];
      if (p.atlasIndex < 0 || p.atlasIndex >= atlas.size) {
        continue;
      }
      FlixelFrame f = atlas.get(p.atlasIndex);
      m3PartM.set(p.world);
      m3Combined.set(m3Base);
      m3Combined.mul(m3PartM);
      drawAffine.set(m3Combined);
      batch.setColor(c.r, c.g, c.b, c.a);
      sb.draw(f.getRegion(), 0f, 0f, drawAffine);
    }
    batch.setColor(Color.WHITE);
  }

  void installBtaCompositing(@NotNull FlixelBtaCompositing comp, float hitboxWidth, float hitboxHeight) {
    this.bta = comp;
    clearAnimationDisplayFrame();
    setSize(hitboxWidth, hitboxHeight);
    updateHitbox();
  }

  @Override
  public void destroy() {
    bta = null;
    super.destroy();
  }

  @Override
  public Rectangle getBounds(@Nullable Rectangle out) {
    if (members.size == 0 && bta != null) {
      if (out == null) {
        out = new Rectangle();
      }
      out.set(getX(), getY(), getWidth(), getHeight());
      return out;
    }
    return super.getBounds(out);
  }

  /**
   * Loads the spritemap and animation JSON files and installs the BTA compositing.
   * 
   * @param textureKey Asset key for the PNG (same as {@link FlixelSprite#loadGraphic(String)}), e.g. matching
   *   {@code spritemap1.png} for a BF export.
   * @param spritemapJsonPath e.g. {@code .../spritemap1.json}
   * @param animationJsonPath e.g. {@code .../Animation.json}
   * @return {@code this} sprite for chaining.
   */
  @NotNull
  public FlixelAnimateSprite loadSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    FlixelAnimationController controller = ensureAnimation();
    FlixelSpritemapJsonLoader.load(
      controller, textureKey, spritemapJsonPath, animationJsonPath);
    return this;
  }

}
