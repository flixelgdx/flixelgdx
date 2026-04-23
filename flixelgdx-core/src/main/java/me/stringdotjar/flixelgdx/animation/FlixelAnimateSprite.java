/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.math.Rectangle;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.group.FlixelSpriteGroup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link FlixelSpriteGroup} intended for <strong>Adobe Animate / BTA-style</strong> spritemap + JSON
 * pipelines (FNF and similar). It behaves like a normal {@link FlixelSprite} for position, scale,
 * rotation, alpha, color, flip, and facing: those setters follow {@link FlixelSpriteGroup} rules and
 * propagate to any member parts when a multi-part compositor is used.
 *
 * <p>Today, loading a spritemap plus animation uses {@link FlixelSpritemapJsonLoader} on this object’s
 * {@link #ensureAnimation() animation} and draws the <strong>single-rig</strong> frame selection on the
 * group root (or the “largest cell” fallback when a symbol dictionary is present). Future work may
 * attach one {@link FlixelSprite} per layer for a true compositor; until then, an empty group still
 * shows the root atlas the loader applied via {@link FlixelSprite#applySparrowAtlas}.
 *
 * <p>Do <strong>not</strong> use {@link #loadGraphic} or {@link #makeGraphic} on groups: those are
 * disabled on {@link FlixelSpriteGroup}. Pre-load the texture with your asset key, then call
 * {@link #loadSpritemapAndAnimation(String, String, String)}.
 */
public class FlixelAnimateSprite extends FlixelSpriteGroup {

  /**
   * Creates a sprite with no member cap and default group rotation parameters (same as
   * {@link FlixelSpriteGroup#FlixelSpriteGroup()}).
   */
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
   * When this group has no child sprites, the inherited {@link FlixelSpriteGroup#getBounds} would
   * return a zero-size rect. Use the root sprite’s size (set by the atlas) so
   * {@code screenCenter} and bounds queries match a {@link FlixelSprite} after
   * {@link #loadSpritemapAndAnimation}.
   */
  @Override
  public Rectangle getBounds(@Nullable Rectangle out) {
    if (members.size == 0) {
      if (out == null) {
        out = new Rectangle();
      }
      out.set(getX(), getY(), getWidth(), getHeight());
      return out;
    }
    return super.getBounds(out);
  }

  /**
   * Reads spritemap and animation JSON, applies atlas frames, and registers named clips. Ensures
   * {@link #animation} is non-null, then calls {@link FlixelSpritemapJsonLoader#load}.
   *
   * <p><b>Precondition:</b> {@code textureKey} must already be known to
   * {@link me.stringdotjar.flixelgdx.Flixel#ensureAssets()} (the PNG is loadable for that key).
   * 
   * <p><b>Postcondition:</b> this sprite’s graphic and {@link me.stringdotjar.flixelgdx.graphics.FlixelFrame}
   * data match the files; you may call {@link FlixelSprite#updateHitbox()} and
   * {@link FlixelAnimationController#playAnimation(String)} on {@link #animation} (after
   * {@link #ensureAnimation()}) on the result.
   * 
   * <p><b>On failure</b> the loader throw. This sprite’s prior graphic is unchanged if the
   * throw occurs before a successful install.
   *
   * @param textureKey Asset key for the shared texture (same as with a plain {@link FlixelSprite}).
   * @param spritemapJsonPath Path to spritemap JSON (e.g. {@code shared/.../spritemap1.json}).
   * @param animationJsonPath Path to animation JSON (e.g. {@code shared/.../Animation.json}).
   * @return {@code this} for chaining.
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
