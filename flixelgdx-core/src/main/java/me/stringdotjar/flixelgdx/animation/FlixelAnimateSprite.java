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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.group.FlixelSpriteGroup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelSprite}-compatible sprite that renders Adobe Animate ("BTA") multi-part rigs produced
 * by an Animate texture-atlas export. The three input files ({@code spritemap1.png},
 * {@code spritemap1.json}, {@code Animation.json}) are loaded through
 * {@link #loadSpritemapAndAnimation}, which hands them to {@link FlixelAnimateRigLoader} for parsing
 * and baking into a {@link FlixelAnimateRig}.
 *
 * <p>Rendering is fully data-driven by the rig: every draw call looks up the clip that the inherited
 * {@link FlixelAnimationController} is currently playing, grabs the keyframe at
 * {@link FlixelAnimationController#getCurrentKeyframeIndex()}, and walks the keyframe's pre-baked
 * parts back-to-front. Every part carries a fully composed {@link Affine2}, so the inner loop is a
 * single {@link Affine2#mul} plus one {@link SpriteBatch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion,
 * float, float, Affine2)} per visible bitmap.
 *
 * <p>Group position, scale, color tint, camera scroll factor, and draw-offset behave like a normal
 * {@link FlixelSprite}. The sprite's hitbox matches the anchor clip's bounding box scaled by
 * {@link #getScaleX()} / {@link #getScaleY()}, so {@link FlixelSprite#screenCenter()} and
 * {@link Flixel#overlap} remain consistent with what the player sees.
 *
 * <h2>Example</h2>
 * <pre>
 * FlixelAnimateSprite fas = new FlixelAnimateSprite();
 * fas.loadSpritemapAndAnimation(
 *     "path/to/atlas/spritemap1.png",
 *     "path/to/atlas/spritemap1.json",
 *     "path/to/animation/Animation.json");
 * fas.setScale(0.65f);
 * fas.updateHitbox();
 * fas.screenCenter();
 * fas.animation.playAnimation("Animation Name");
 * add(fas);
 * </pre>
 *
 * <p>An {@link FlixelAnimateSprite} only draws through a {@link SpriteBatch}; if a non-sprite
 * {@link Batch} implementation is passed in, {@link #draw} silently returns.
 */
public class FlixelAnimateSprite extends FlixelSpriteGroup {

  /**
   * The rig that drives this sprite's rendering; {@code null} until
   * {@link #loadSpritemapAndAnimation} has successfully built one (or after {@link #destroy()}).
   */
  @Nullable
  private FlixelAnimateRig rig;

  /**
   * Preallocated affine reused by {@link #draw(Batch)} so we never allocate on the hot path. At the
   * start of each draw call it is reset to a translate/scale that represents the sprite's world
   * position, then each part's baked affine is post-multiplied into it for the {@link SpriteBatch#draw}
   * call and the result is undone before the next part.
   */
  private final Affine2 drawAffine = new Affine2();

  /**
   * Scratch affine that holds the translate-and-scale produced from the sprite's world position and
   * scale. Copied into {@link #drawAffine} before each part is composited.
   */
  private final Affine2 baseAffine = new Affine2();

  /** Creates an empty sprite with default group capacity. Call {@link #loadSpritemapAndAnimation}
   * before using it. */
  public FlixelAnimateSprite() {
    super();
  }

  /**
   * Creates a new empty animate sprite sized for a specific group capacity and rotation configuration.
   * Forwarded directly to the parent group constructor because the rig is orthogonal to the group's
   * rotation/wheel behaviour.
   *
   * @param maxSize Maximum number of group members, or {@code 0} for unlimited.
   * @param rotationRadius Radius used by {@link FlixelSpriteGroup.RotationMode#WHEEL}.
   * @param rotation Initial group angle in degrees.
   */
  public FlixelAnimateSprite(int maxSize, float rotationRadius, float rotation) {
    super(maxSize, rotationRadius, rotation);
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

  /**
   * @return The rig that drives this sprite's rendering, or {@code null} if no rig has been loaded yet.
   */
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
    if (rig == null) {
      return super.updateHitbox();
    }
    if (currentRegion != null) {
      return super.updateHitbox();
    }
    float effW = Math.abs(getScaleX()) * rig.anchorWidth;
    float effH = Math.abs(getScaleY()) * rig.anchorHeight;
    return updateHitbox(effW, effH);
  }

  /**
   * Draws the sprite. When a rig is installed, walks the current clip's current keyframe and draws
   * each part through the shared {@link SpriteBatch} with a preallocated {@link Affine2}. When no
   * rig is installed, falls back to {@link FlixelSpriteGroup}'s normal member/frame draw path.
   *
   * @param batch The active batch. Must be a {@link SpriteBatch} for the rig path (required for
   * {@link SpriteBatch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, Affine2)}).
   */
  @Override
  public void draw(Batch batch) {
    if (!visible || !isOnDrawCamera()) {
      return;
    }
    FlixelAnimateRig activeRig = rig;
    if (activeRig == null) {
      super.draw(batch);
      return;
    }
    if (!(batch instanceof SpriteBatch)) {
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

    Array<FlixelFrame> atlas = activeRig.atlas;
    SpriteBatch sb = (SpriteBatch) batch;

    // Compose the sprite's world translation and scale once per frame. Per-part matrices are already
    // in anchor-local, libGDX Y-up space, so this is the only world-space math left to do.
    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.getCamera();
    float wx = getX() - cam.scroll.x * getScrollX() - getOffsetX();
    float wy = getY() - cam.scroll.y * getScrollY() - getOffsetY();
    float sx = getScaleX();
    float sy = getScaleY();

    baseAffine.idt();
    baseAffine.m02 = wx;
    baseAffine.m12 = wy;
    if (sx != 1f || sy != 1f) {
      baseAffine.m00 = sx;
      baseAffine.m11 = sy;
    }

    Color tint = getColor();
    sb.setColor(tint.r, tint.g, tint.b, tint.a);

    FlixelAnimateRig.Part[] parts = keyframe.parts;
    for (int i = 0; i < parts.length; i++) {
      FlixelAnimateRig.Part part = parts[i];
      if (part.atlasIndex < 0 || part.atlasIndex >= atlas.size) {
        continue;
      }
      FlixelFrame frame = atlas.get(part.atlasIndex);

      // Compose the final affine for this part. Using setToProduct keeps us allocation-free and rewrites
      // all six fields in a single call. The resulting matrix acts on the quad corners (0, 0), (w, 0),
      // (w, h), (0, h) that SpriteBatch.draw() will emit, where (w, h) are the region's pixel dimensions.
      drawAffine.setToProduct(baseAffine, part.local);
      sb.draw(frame.getRegion(), frame.getRegionWidth(), frame.getRegionHeight(), drawAffine);
    }

    sb.setColor(Color.WHITE);
  }

  /**
   * Installs a rig built by {@link FlixelAnimateRigLoader}. Callers must go through
   * {@link #loadSpritemapAndAnimation}. This method itself does not size the hitbox; the loader does
   * that after clearing the interim display frame set by
   * {@link FlixelSprite#applySparrowAtlas(me.stringdotjar.flixelgdx.graphics.FlixelGraphic,
   * com.badlogic.gdx.utils.Array)}.
   *
   * @param installedRig The rig to install. Must not be {@code null}.
   */
  void installAnimateRig(@NotNull FlixelAnimateRig installedRig) {
    this.rig = installedRig;
    setSize(installedRig.anchorWidth, installedRig.anchorHeight);
  }

  /**
   * Reports a bounding rectangle for overlap/culling queries. With a rig installed and no explicit
   * members, the sprite's own position and (possibly scaled) hitbox are returned; otherwise the parent
   * group bounds are used.
   *
   * @param out Optional scratch rectangle to reuse; if {@code null}, a new one is allocated.
   * @return The bounds rectangle, filled in place.
   */
  @Override
  public Rectangle getBounds(@Nullable Rectangle out) {
    if (members.size == 0 && rig != null) {
      if (out == null) {
        out = new Rectangle();
      }
      out.set(getX(), getY(), getWidth(), getHeight());
      return out;
    }
    return super.getBounds(out);
  }

  @Override
  public void destroy() {
    rig = null;
    super.destroy();
  }
}
