/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelSprite} that renders Adobe Animate ("BTA") multi-part rigs produced by an Animate
 * texture-atlas export. The three input files ({@code spritemap1.png}, {@code spritemap1.json},
 * {@code Animation.json}) are loaded through {@link #loadSpritemapAndAnimation}, which hands them to
 * {@link FlixelAnimateRigLoader} for parsing and baking into a {@link FlixelAnimateRig}.
 *
 * <p>Rendering is fully data-driven by the rig: every draw call looks up the clip that the inherited
 * {@link FlixelAnimationController} is currently playing, grabs the keyframe at
 * {@link FlixelAnimationController#getCurrentKeyframeIndex()}, and walks the keyframe's pre-baked
 * parts back-to-front. Every part carries a fully composed {@link Affine2}, so the inner loop is a
 * single {@link Affine2#setToProduct} plus one
 * {@link SpriteBatch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, Affine2)} per
 * visible bitmap.
 *
 * <p>Position, scale, rotation, color tint, flip, origin, offset, antialiasing, scroll factor, and
 * facing all behave like a normal {@link FlixelSprite} loaded with a single graphic. The sprite's
 * hitbox matches the anchor clip's bounding box scaled by {@link #getScaleX()} / {@link #getScaleY()},
 * so {@link FlixelSprite#screenCenter()} and {@link Flixel#overlap} remain consistent with what the
 * player sees.
 *
 * <h2>Example</h2>
 * <pre>
 * FlixelAnimateSprite fas = new FlixelAnimateSprite();
 * fas.loadSpritemapAndAnimation(
 *     "path/to/atlas/spritemap1.png",
 *     "path/to/atlas/spritemap1.json",
 *     "path/to/animation/Animation.json");
 * fas.setScale(0.65f);
 * fas.setAntialiasing(true);
 * fas.updateHitbox();
 * fas.screenCenter();
 * fas.animation.playAnimation("Animation Name");
 * add(fas);
 * </pre>
 *
 * <p>{@code FlixelAnimateSprite} only draws through a {@link SpriteBatch}; if a non-sprite
 * {@link Batch} implementation is passed in, the rig path silently returns and falls back to the
 * inherited {@link FlixelSprite} draw path so simple atlas usage still works.
 */
public class FlixelAnimateSprite extends FlixelSprite {

  /**
   * The rig that drives this sprite's rendering. {@code null} until
   * {@link #loadSpritemapAndAnimation} has successfully built one (or after {@link #destroy()}).
   */
  @Nullable
  private FlixelAnimateRig rig;

  /**
   * Preallocated affine reused by {@link #draw(Batch)} so we never allocate on the hot path. At the
   * start of each draw call it is set to a translate, rotate, scale, and origin pivot composing the
   * sprite's world transform; each part's baked affine is then post-multiplied into {@link #drawAffine}
   * for the {@link SpriteBatch#draw} call.
   */
  private final Affine2 baseAffine = new Affine2();

  /**
   * Scratch affine used to hold the per-part composed matrix passed to
   * {@link SpriteBatch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, Affine2)}.
   */
  private final Affine2 drawAffine = new Affine2();

  /** Creates an empty sprite at {@code (0, 0)}. Call {@link #loadSpritemapAndAnimation} before using it. */
  public FlixelAnimateSprite() {
    super();
  }

  /**
   * Creates an empty sprite at the given world position. Call {@link #loadSpritemapAndAnimation}
   * before using it.
   *
   * @param x The x-coordinate of the sprite's position.
   * @param y The y-coordinate of the sprite's position.
   */
  public FlixelAnimateSprite(float x, float y) {
    super(x, y);
  }

  /**
   * Loads a spritemap and animation pair and installs the resulting rig. Equivalent to
   * {@link #loadSpritemapAndAnimation(String, String, String, String)} with a {@code null} anchor clip
   * name, so the first clip declared in the timeline's label layer is used as the bounding-box anchor
   * and is the one auto-played after loading.
   *
   * @param textureKey The asset key of the spritemap PNG, matching the key used by
   *   {@link FlixelSprite#loadGraphic(String)} (for example
   *   {@code "shared/images/characters/bf/spritemap1.png"}). Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If either file is missing, malformed, or not a recognized Adobe
   *   Animate texture-atlas export.
   */
  @NotNull
  public FlixelAnimateSprite loadSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    return loadSpritemapAndAnimation(textureKey, spritemapJsonPath, animationJsonPath, null);
  }

  /**
   * Loads a spritemap and animation pair and installs the resulting rig. Delegates to
   * {@link FlixelAnimateRigLoader#load}, which parses the Adobe JSON, bakes every clip and keyframe,
   * populates this sprite's {@link FlixelAnimationController} with one clip per label, and starts
   * playing the anchor clip.
   *
   * @param textureKey The asset key of the spritemap PNG, matching the key used by
   *   {@link FlixelSprite#loadGraphic(String)} (for example
   *   {@code "shared/images/characters/bf/spritemap1.png"}). Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @param anchorClipName Name of the clip whose first keyframe defines the rig's bounding box and
   *   which is auto-played after loading. Pass {@code null} (or a name that does not exist in the
   *   export) to default to the first clip declared in the timeline's label layer.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If either file is missing, malformed, or not a recognized Adobe
   *   Animate texture-atlas export.
   */
  @NotNull
  public FlixelAnimateSprite loadSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {
    FlixelAnimationController controller = ensureAnimation();
    FlixelAnimateRigLoader.load(
      this, controller, textureKey, spritemapJsonPath, animationJsonPath, anchorClipName);
    return this;
  }

  @Nullable
  public FlixelAnimateRig getRig() {
    return rig;
  }

  /**
   * When a rig is installed we render directly from its pre-baked parts and never touch
   * {@link FlixelSprite#currentRegion}, so we swallow the animation controller's per-frame "current
   * keyframe" callback. Without a rig, behaviour falls through to the normal
   * {@link FlixelSprite#setCurrentFrameForAnimation} path so the sprite still works for simple atlases.
   *
   * @param frame The frame being advanced to by {@link FlixelAnimationController}; ignored when a rig
   *   is installed.
   */
  @Override
  public void setCurrentFrameForAnimation(@Nullable FlixelFrame frame) {
    if (rig != null) {
      return;
    }
    super.setCurrentFrameForAnimation(frame);
  }

  /**
   * Rebuilds the hitbox so it exactly matches the drawn rig. With a rig installed, the hitbox is the
   * anchor bounding box scaled by {@link #getScaleX()} / {@link #getScaleY()}, so
   * {@link FlixelSprite#screenCenter()} and {@link Flixel#overlap} agree with what the player sees.
   * Without a rig (or when the sprite falls back to an atlas region), the parent behaviour is used.
   *
   * @return {@code this} sprite, matching {@link FlixelSprite#updateHitbox}.
   */
  @Override
  public @NotNull FlixelSprite updateHitbox() {
    if (rig == null || currentRegion != null) {
      return super.updateHitbox();
    }
    float effW = Math.abs(getScaleX()) * rig.anchorWidth;
    float effH = Math.abs(getScaleY()) * rig.anchorHeight;
    return updateHitbox(effW, effH);
  }

  /**
   * Toggles texture filtering on the rig's spritemap (or the inherited {@link FlixelSprite#currentRegion}'s
   * texture when no rig is installed). Without this override, {@link FlixelSprite#setAntialiasing} would
   * be a no-op on a rig sprite because we deliberately null out {@link FlixelSprite#currentRegion} once
   * the rig is in place.
   *
   * @param antialiasing {@code true} to use {@link Texture.TextureFilter#Linear}, {@code false} for
   *   {@link Texture.TextureFilter#Nearest}.
   */
  @Override
  public void setAntialiasing(boolean antialiasing) {
    this.antialiasing = antialiasing;
    Texture texture = (currentRegion != null) ? currentRegion.getTexture() : null;
    if (texture == null && rig != null && rig.atlas.size > 0) {
      FlixelFrame anyFrame = rig.atlas.first();
      if (anyFrame != null) {
        texture = anyFrame.getTexture();
      }
    }
    if (texture != null) {
      Texture.TextureFilter filter =
        antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest;
      texture.setFilter(filter, filter);
    }
  }

  /**
   * Draws the sprite. With a rig installed, walks the current clip's current keyframe and draws each
   * part through the shared {@link SpriteBatch} with a preallocated {@link Affine2}. Without a rig
   * (or with a non-sprite {@link Batch}), falls back to the inherited {@link FlixelSprite} draw path.
   *
   * <p>The rig draw composes the sprite's world transform once per frame as
   * {@code T(wx-offsetX, wy-offsetY) * T(originX, originY) * R(angle) * S(sx, sy) * T(-originX, -originY)},
   * matching {@link FlixelSprite#draw} for a single graphic. Per-part baked matrices are post-multiplied
   * into this base, so rotation, scale, flip, origin, and offset all behave identically to a regular
   * sprite.
   *
   * @param batch The active batch. The rig path requires a {@link SpriteBatch} (for
   * {@link SpriteBatch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, Affine2)});
   * any other batch falls back to the inherited single-graphic draw path.
   */
  @Override
  public void draw(Batch batch) {
    if (!visible || !isOnDrawCamera()) {
      return;
    }

    FlixelAnimateRig activeRig = rig;
    if (activeRig == null || !(batch instanceof SpriteBatch)) {
      // No rig (or batch type does not support Affine2 draws): fall back to the standard sprite path
      // so at least the inherited currentRegion / currentFrame still renders.
      super.draw(batch);
      return;
    }

    FlixelAnimationController controller = animation;
    if (controller == null) {
      return;
    }

    // Resolve the clip the controller is currently playing. Fall back to the rig's anchor clip if the
    // controller has not had a clip chosen yet (for example right after destroy-and-reload).
    String clipName = controller.getCurrentAnim();
    if (clipName == null || clipName.isEmpty()) {
      clipName = activeRig.anchorClipName;
    }
    FlixelAnimateRig.Clip clip = activeRig.getClip(clipName);
    if (clip == null) {
      return;
    }

    int keyframeIndex = controller.getCurrentKeyframeIndex();
    if (keyframeIndex < 0) {
      keyframeIndex = 0;
    } else if (keyframeIndex >= clip.keyframes.length) {
      keyframeIndex = clip.keyframes.length - 1;
    }
    FlixelAnimateRig.Keyframe keyframe = clip.keyframes[keyframeIndex];
    if (keyframe.parts.length == 0) {
      return;
    }

    // World position with scroll factor (matches FlixelSprite.draw() / FlixelObject.getDrawX()).
    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.getCamera();
    float wx = getX() - cam.scroll.x * getScrollX() - getOffsetX();
    float wy = getY() - cam.scroll.y * getScrollY() - getOffsetY();

    // Match FlixelSprite's flip-into-scale convention: a negative scale on either axis mirrors the
    // sprite around its origin, and the facing flag piles on top of the user-set flipX.
    boolean isFlippedX = flipX || (facing == FlixelObject.DirectionFlags.LEFT);
    boolean isFlippedY = flipY;
    float sx = isFlippedX ? -getScaleX() : getScaleX();
    float sy = isFlippedY ? -getScaleY() : getScaleY();
    float angle = getAngle();
    float ox = getOriginX();
    float oy = getOriginY();

    // Build the world-space base transform: T(world) * T(origin) * R * S * T(-origin). Each builder
    // call is a right-multiplication, so the resulting matrix applied to a part-local point first
    // shifts by -origin, then scales/rotates, then re-shifts by origin, then translates to the world
    // position. Identity branches are skipped so the no-rotation/no-scale fast path stays cheap.
    baseAffine.idt();
    baseAffine.translate(wx, wy);
    boolean hasOrigin = (ox != 0f) || (oy != 0f);
    if (hasOrigin) {
      baseAffine.translate(ox, oy);
    }
    if (angle != 0f) {
      baseAffine.rotate(angle);
    }
    if (sx != 1f || sy != 1f) {
      baseAffine.scale(sx, sy);
    }
    if (hasOrigin) {
      baseAffine.translate(-ox, -oy);
    }

    Color tint = getColor();
    SpriteBatch sb = (SpriteBatch) batch;
    sb.setColor(tint.r, tint.g, tint.b, tint.a);

    Array<FlixelFrame> atlas = activeRig.atlas;
    FlixelAnimateRig.Part[] parts = keyframe.parts;
    for (int i = 0; i < parts.length; i++) {
      FlixelAnimateRig.Part part = parts[i];
      if (part.atlasIndex < 0 || part.atlasIndex >= atlas.size) {
        continue;
      }
      FlixelFrame frame = atlas.get(part.atlasIndex);

      // Compose the final affine for this part. setToProduct() keeps us allocation-free and rewrites
      // all six fields in a single call. The resulting matrix acts on the quad corners (0, 0),
      // (w, 0), (w, h), (0, h) that SpriteBatch.draw() will emit, where (w, h) are the region's
      // pixel dimensions.
      drawAffine.setToProduct(baseAffine, part.local);
      sb.draw(frame.getRegion(), frame.getRegionWidth(), frame.getRegionHeight(), drawAffine);
    }

    sb.setColor(Color.WHITE);
  }

  /**
   * Installs a rig built by {@link FlixelAnimateRigLoader}. Callers must go through
   * {@link #loadSpritemapAndAnimation}. Sets the sprite's logical size to the anchor bounding box and,
   * if the user has already toggled antialiasing on, applies the matching texture filter to the rig's
   * spritemap so the setting carries across the load.
   *
   * @param installedRig The rig to install. Must not be {@code null}.
   */
  void installAnimateRig(@NotNull FlixelAnimateRig installedRig) {
    this.rig = installedRig;
    setSize(installedRig.anchorWidth, installedRig.anchorHeight);
    if (antialiasing && installedRig.atlas.size > 0) {
      FlixelFrame anyFrame = installedRig.atlas.first();
      if (anyFrame != null) {
        Texture texture = anyFrame.getTexture();
        if (texture != null) {
          texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
      }
    }
  }

  @Override
  public void destroy() {
    rig = null;
    super.destroy();
  }
}
