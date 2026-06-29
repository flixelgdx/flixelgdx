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
package org.flixelgdx.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.util.FlixelDirectionFlags;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link FlixelSprite} that renders Adobe Animate texture-atlas rigs. The three input files
 * ({@code spritemap1.png}, {@code spritemap1.json}, {@code Animation.json}) are loaded through
 * {@link #addSpritemapAndAnimation}. The first call builds a rig with {@link FlixelAnimateRigLoader};
 * further calls merge additional Adobe exports into that rig automatically. Both nested symbol exports
 * (Better Texture Atlas style with {@code E.SI} + {@code SD.S}) and flat document timelines (direct
 * {@code E.ASI} keyframes) use the same API.
 *
 * <p>Rendering is fully data-driven by the rig: every draw call looks up the clip that the inherited
 * {@link FlixelAnimationController} is currently playing, grabs the keyframe at
 * {@link FlixelAnimationController#getCurrentKeyframeIndex()}, and walks the keyframe's pre-baked
 * parts back-to-front. Every part carries a fully composed {@link Affine2}, so the inner loop is a
 * single {@link Affine2#setToProduct} plus one
 * {@link Batch#draw(TextureRegion, float, float, Affine2)} per
 * visible bitmap.
 *
 * <p>Position, scale, rotation, color tint, flip, origin, offset, antialiasing, scroll factor, and
 * facing all behave like a normal {@link FlixelSprite} loaded with a single graphic. The sprite's
 * hitbox matches the anchor clip's bounding box scaled by {@link #getScaleX()} / {@link #getScaleY()},
 * so {@link FlixelSprite#screenCenter()} and {@link Flixel#overlap} remain consistent with what the
 * player sees.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * FlixelAnimateSprite fas = new FlixelAnimateSprite();
 * fas.addSpritemapAndAnimation(
 *     "path/to/atlas/spritemap1.png",
 *     "path/to/atlas/spritemap1.json",
 *     "path/to/animation/Animation.json");
 * fas.setScale(0.65f);
 * fas.setAntialiasing(true);
 * fas.updateHitbox();
 * fas.screenCenter();
 * fas.animation.playAnimation("Animation Name");
 * add(fas);
 *
 * // If you group all of your atlas files in individual folders, you can
 * // also just provide a path to a folder and the paths will be automatically
 * // loaded for you!
 * FlixelAnimateSprite.defaultSpritemapFileName = "customSpritemapName";
 * FlixelAnimateSprite.defaultAnimationFileName = "customAnimationName";
 *
 * FlixelAnimateSprite fas = new FlixelAnimateSprite();
 * fas.addSpritemapAndAnimation("path/to/atlas/folder");
 * }
 * </pre>
 *
 * <h2>Merging multiple atlases</h2>
 * Call {@link #addSpritemapAndAnimation} again with another export triple. Subsequent loads append frames to the
 * shared atlas, bake clips into the existing anchor space (the body stays pinned when you switch atlases),
 * and register clip names on the same {@link FlixelAnimationController#playAnimation} path. Names from a later sheet
 * override earlier registrations on collisions.
 *
 * <h2>Mixing in a Sparrow atlas</h2>
 * A character can also carry Sparrow XML clips on the same body via
 * {@link FlixelAnimationController#addSparrowFrames(String)}. Rig clips keep rendering from the baked
 * rig; clips registered against the merged Sparrow frames render through the standard frame path, and
 * the sprite picks the right one per clip automatically.
 *
 * @see #addSpritemapAndAnimation(String)
 * @see #addSpritemapAndAnimation(String, String, String)
 * @see FlixelAnimationController#addSparrowFrames(String)
 * @see #defaultSpritemapFileName
 * @see #defaultAnimationFileName
 */
public class FlixelAnimateSprite extends FlixelSprite {

  /**
   * The default file name for every spritemap {@code .png} / {@code .json}
   * loaded with {@link #addSpritemapAndAnimation(String)}. Note that you shouldn't
   * include an extension, as it's already handled for you. Default value is {@code "spritemap1"}.
   */
  @NotNull
  public static String defaultSpritemapFileName = "spritemap1";

  /**
   * The default file name for every animation {@code .json} data
   * loaded with {@link #addSpritemapAndAnimation(String)}. When using this, you don't need to
   * include {@code .json} at the end, as the loader does it for you. Default value is {@code "Animation"}.
   */
  @NotNull
  public static String defaultAnimationFileName = "Animation";

  /**
   * The rig that drives this sprite's rendering. {@code null} until
   * {@link #addSpritemapAndAnimation} has successfully built one (or after {@link #destroy()}).
   */
  @Nullable
  private FlixelAnimateRig rig;

  /**
   * Preallocated affine reused by {@link #draw(FlixelBatch)} so we never allocate on the hot path. At the
   * start of each draw call it is set to a translate, rotate, scale, and origin pivot composing the
   * sprite's world transform; each part's baked affine is then post-multiplied into {@link #drawAffine}
   * for the {@link Batch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, Affine2)} call.
   */
  @NotNull
  private final Affine2 baseAffine = new Affine2();

  /**
   * Scratch affine used to hold the per-part composed matrix passed to
   * {@link Batch#draw(com.badlogic.gdx.graphics.g2d.TextureRegion, float, float, Affine2)}.
   */
  @NotNull
  private final Affine2 drawAffine = new Affine2();

  /** Creates an empty sprite at {@code (0, 0)}. Call {@link #addSpritemapAndAnimation} before using BTA rigs. */
  public FlixelAnimateSprite() {
    super();
  }

  /**
   * Creates an empty sprite at the given world position. Call {@link #addSpritemapAndAnimation}
   * before using it.
   *
   * @param x The x-coordinate of the sprite's position.
   * @param y The y-coordinate of the sprite's position.
   */
  public FlixelAnimateSprite(float x, float y) {
    super(x, y);
  }

  /**
   * Adds an Adobe Animate texture atlas from a single provided path.
   *
   * <p>This method is very useful if you have an exact location where all three core parts of
   * an atlas are stored, and you just want to provide the path to it. Note that you can set the
   * default file names of the default spritemap and animation data if you know what it's going
   * to be every time!
   *
   * @param path The directory where all three core files are stored. Must be a directory.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If the provided path isn't a real directory.
   * @throws NullPointerException If either {@link #defaultSpritemapFileName} or {@link #defaultAnimationFileName}
   *     are {@code null}.
   * @see #defaultSpritemapFileName
   * @see #defaultAnimationFileName
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(String path) {
    Objects.requireNonNull(defaultSpritemapFileName, "defaultSpritemapName cannot be null.");
    Objects.requireNonNull(defaultAnimationFileName, "defaultAnimationName cannot be null.");
    String pngPath = path + "/" + defaultSpritemapFileName + ".png";
    String spritemapJsonPath = path + "/" + defaultSpritemapFileName + ".json";
    String animationJsonPath = path + "/" + defaultAnimationFileName + ".json";
    if (!Gdx.files.internal(pngPath).exists()
        || !Gdx.files.internal(spritemapJsonPath).exists()
        || !Gdx.files.internal(animationJsonPath).exists()) {
      throw new IllegalArgumentException(
          "The provided path is either not a real folder, one of the required files is missing, or doesn't exist.");
    }
    return addSpritemapAndAnimation(pngPath, spritemapJsonPath, animationJsonPath);
  }

  /**
   * Adds an Adobe Animate texture atlas from a single provided directory handle.
   *
   * <p>Equivalent to {@link #addSpritemapAndAnimation(String)}, but accepts a libGDX {@link FileHandle}
   * for callers that already resolved the directory through a file resolver instead of holding a raw path.
   *
   * @param path The directory handle where all three core files are stored. Must be a directory.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If the provided path isn't a real directory.
   * @throws NullPointerException If either {@link #defaultSpritemapFileName} or {@link #defaultAnimationFileName}
   *     are {@code null}.
   * @see #defaultSpritemapFileName
   * @see #defaultAnimationFileName
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(@NotNull FileHandle path) {
    return addSpritemapAndAnimation(pathOf(path, "path"));
  }

  /**
   * Adds an Adobe Animate texture atlas ({@code PNG} plus spritemap JSON and {@code Animation.json}). If
   * this sprite has no rig yet, {@link FlixelAnimateRigLoader#load} builds one and starts the anchor
   * clip. If a rig is already installed, the same triple is merged with {@link FlixelAnimateRigLoader#append}.
   * The {@code anchorClipName} argument is read only on the first successful load; it is ignored on merges.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    return addSpritemapAndAnimation(textureKey, spritemapJsonPath, animationJsonPath, null);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts {@code textureKey}
   * as a {@link FileHandle} instead of a path string.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    return addSpritemapAndAnimation(pathOf(textureKey, "textureKey"), spritemapJsonPath, animationJsonPath);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts {@code spritemapJsonPath}
   * as a {@link FileHandle} instead of a path string.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull String animationJsonPath) {
    return addSpritemapAndAnimation(textureKey, pathOf(spritemapJsonPath, "spritemapJsonPath"), animationJsonPath);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts {@code animationJsonPath}
   * as a {@link FileHandle} instead of a path string.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull FileHandle animationJsonPath) {
    return addSpritemapAndAnimation(textureKey, spritemapJsonPath, pathOf(animationJsonPath, "animationJsonPath"));
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts {@code textureKey}
   * and {@code spritemapJsonPath} as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull String animationJsonPath) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"), pathOf(spritemapJsonPath, "spritemapJsonPath"), animationJsonPath);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts {@code textureKey}
   * and {@code animationJsonPath} as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull FileHandle animationJsonPath) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"), spritemapJsonPath, pathOf(animationJsonPath, "animationJsonPath"));
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts {@code spritemapJsonPath}
   * and {@code animationJsonPath} as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull FileHandle animationJsonPath) {
    return addSpritemapAndAnimation(
        textureKey, pathOf(spritemapJsonPath, "spritemapJsonPath"), pathOf(animationJsonPath, "animationJsonPath"));
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String)} that accepts all three core
   * parameters as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull FileHandle animationJsonPath) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"),
        pathOf(spritemapJsonPath, "spritemapJsonPath"),
        pathOf(animationJsonPath, "animationJsonPath"));
  }

  /**
   * Adds an Adobe Animate texture atlas with an explicit anchor clip on the first load.
   *
   * <p>When no rig exists yet, {@code anchorClipName} selects the clip whose first keyframe defines the hitbox
   * anchor and which autoplays after load; pass {@code null} (or a non-matching name) to use the first clip
   * from the timeline (for document exports with no labels, this is the synthesized default clip). When merging
   * into an existing rig, {@code anchorClipName} is ignored.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {
    FlixelAnimationController controller = ensureAnimation();
    if (rig == null) {
      FlixelAnimateRigLoader.load(
          this, controller, textureKey, spritemapJsonPath, animationJsonPath, anchorClipName);
    } else {
      FlixelAnimateRigLoader.append(this, controller, textureKey, spritemapJsonPath, animationJsonPath);
    }
    return this;
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts
   * {@code textureKey} as a {@link FileHandle} instead of a path string.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"), spritemapJsonPath, animationJsonPath, anchorClipName);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts
   * {@code spritemapJsonPath} as a {@link FileHandle} instead of a path string.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        textureKey, pathOf(spritemapJsonPath, "spritemapJsonPath"), animationJsonPath, anchorClipName);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts
   * {@code animationJsonPath} as a {@link FileHandle} instead of a path string.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull FileHandle animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        textureKey, spritemapJsonPath, pathOf(animationJsonPath, "animationJsonPath"), anchorClipName);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts
   * {@code textureKey} and {@code spritemapJsonPath} as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"),
        pathOf(spritemapJsonPath, "spritemapJsonPath"),
        animationJsonPath,
        anchorClipName);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts
   * {@code textureKey} and {@code animationJsonPath} as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull FileHandle animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"),
        spritemapJsonPath,
        pathOf(animationJsonPath, "animationJsonPath"),
        anchorClipName);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts
   * {@code spritemapJsonPath} and {@code animationJsonPath} as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull String textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull FileHandle animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        textureKey,
        pathOf(spritemapJsonPath, "spritemapJsonPath"),
        pathOf(animationJsonPath, "animationJsonPath"),
        anchorClipName);
  }

  /**
   * Overload of {@link #addSpritemapAndAnimation(String, String, String, String)} that accepts all
   * three core parameters as {@link FileHandle}s instead of path strings.
   *
   * @param textureKey The asset key of the spritemap PNG, as a file handle. Must not be {@code null}.
   * @param spritemapJsonPath The path to the spritemap JSON, as a file handle. Must not be {@code null}.
   * @param animationJsonPath The path to the animation JSON, as a file handle. Must not be {@code null}.
   * @param anchorClipName Anchor clip name for the initial load only; ignored when appending. May be {@code null}.
   * @return {@code this} sprite, for chaining.
   * @throws IllegalArgumentException If any file is missing, malformed, or not a recognized Adobe Animate export.
   */
  @NotNull
  public FlixelAnimateSprite addSpritemapAndAnimation(
      @NotNull FileHandle textureKey,
      @NotNull FileHandle spritemapJsonPath,
      @NotNull FileHandle animationJsonPath,
      @Nullable String anchorClipName) {
    return addSpritemapAndAnimation(
        pathOf(textureKey, "textureKey"),
        pathOf(spritemapJsonPath, "spritemapJsonPath"),
        pathOf(animationJsonPath, "animationJsonPath"),
        anchorClipName);
  }

  /**
   * Resolves a {@link FileHandle} into the path string the rest of {@code addSpritemapAndAnimation}
   * overloads operate on. Asset-manager lookups (the spritemap PNG) and direct JSON reads both resolve
   * a plain path through {@code Gdx.files}, so converting up front lets every {@link FileHandle} overload
   * delegate straight into the existing {@code String} pipeline without duplicating loading logic.
   *
   * @param handle The file handle to resolve. Must not be {@code null}.
   * @param paramName The parameter name to report if {@code handle} is {@code null}.
   * @return The handle's path, as returned by {@link FileHandle#path()}.
   */
  @NotNull
  private static String pathOf(@NotNull FileHandle handle, @NotNull String paramName) {
    Objects.requireNonNull(handle, paramName + " cannot be null.");
    return handle.path();
  }

  @Nullable
  public FlixelAnimateRig getRig() {
    return rig;
  }

  /**
   * Routes the controller's per-frame "current keyframe" callback to the right renderer.
   *
   * <p>When the clip that is playing is rig-backed we render directly from its pre-baked parts and
   * never touch {@link FlixelSprite#currentRegion}, so we swallow the callback (and clear any leftover
   * Sparrow frame so switching from a Sparrow clip to a rig clip does not flash stale art). For a
   * Sparrow or simple-atlas clip the callback falls through to the normal
   * {@link FlixelSprite#setCurrentFrameForAnimation} path, which is what lets a Sparrow sheet merged
   * with {@link FlixelAnimationController#addSparrowFrames(String)} share the same sprite as the rig.
   *
   * @param frame The frame being advanced to by {@link FlixelAnimationController}; ignored while a
   *   rig clip is playing.
   */
  @Override
  public void setCurrentFrameForAnimation(@Nullable FlixelFrame frame) {
    if (isCurrentClipRig()) {
      currentFrame = null;
      return;
    }
    super.setCurrentFrameForAnimation(frame);
  }

  /**
   * Whether the clip the controller is currently playing is backed by the installed rig.
   *
   * <p>A clip is rig-backed only when a rig is installed and the clip name matches one of the rig's
   * baked clips. Clips registered against merged Sparrow frames (or any non-rig atlas) are not
   * rig-backed and therefore render through {@link FlixelSprite#currentRegion}.
   *
   * @return {@code true} if the current clip should be drawn from the rig.
   */
  private boolean isCurrentClipRig() {
    FlixelAnimationController controller = animation;
    String name = (controller != null) ? controller.getCurrentAnim() : "";
    boolean rigHasClip = rig != null && !name.isEmpty() && rig.getClip(name) != null;
    return useRigClip(rig != null, rigHasClip);
  }

  /**
   * Pure decision for whether the rig draw path should be used, split out so it can be unit tested
   * without a GPU-backed rig. The rig path is used only when a rig is installed and it actually holds
   * the clip being played.
   *
   * @param hasRig Whether a rig is installed.
   * @param rigHasClip Whether the rig holds the clip currently playing.
   * @return {@code true} if the rig draw path should be used.
   */
  static boolean useRigClip(boolean hasRig, boolean rigHasClip) {
    return hasRig && rigHasClip;
  }

  /**
   * Rebuilds the hitbox so it exactly matches the drawn rig. With a rig installed, the hitbox is the
   * anchor bounding box scaled by {@link #getScaleX()} / {@link #getScaleY()}, so
   * {@link FlixelSprite#screenCenter()} and {@link Flixel#overlap} agree with what the player sees.
   * Without a rig the parent behavior is used.
   *
   * <p>Unlike {@link FlixelSprite#updateHitbox}, this method does <strong>not</strong> reset
   * {@link #setScale(float)} back to {@code 1}. The rig's part affines are baked at anchor-local size,
   * so the absolute scale must remain on the sprite for the {@link #draw(FlixelBatch)} matrix chain to
   * size the visible rig correctly. The {@link #draw(FlixelBatch)} method is fully aware of this and uses
   * {@link #getOriginX()} / {@link #getOriginY()} together with {@code |scaleX|} / {@code |scaleY|}
   * so the visible rig still coincides with the hitbox.
   *
   * @return {@code this} sprite, matching {@link FlixelSprite#updateHitbox}.
   */
  @Override
  public @NotNull FlixelSprite updateHitbox() {
    // A Sparrow clip merged onto this sprite sizes its hitbox from the frame, like a normal sprite;
    // only a rig clip uses the rig's anchor bounding box.
    if (!isCurrentClipRig()) {
      return super.updateHitbox();
    }
    FlixelAnimateRig activeRig = rig;
    float effW = Math.abs(getScaleX()) * activeRig.anchorWidth;
    float effH = Math.abs(getScaleY()) * activeRig.anchorHeight;
    return updateHitbox(effW, effH);
  }

  /**
   * Sets how large the rig is drawn on screen (in pixels) without changing the underlying anchor
   * data. With a rig installed this divides the requested dimensions by the rig's anchor bounding
   * box to derive the matching {@link #setScale(float, float)}, then calls {@link #updateHitbox()} so
   * the hitbox follows the new visual size. Without a rig, the parent behavior is used so plain
   * atlas usage continues to work.
   *
   * <p>This is the recommended entry point when game code needs a character to occupy a fixed pixel
   * size regardless of the resolution of the source export, because callers do not have to compute
   * the anchor dimensions themselves or chain {@code setScale + updateHitbox} manually.
   *
   * @param width The drawn width in pixels (must be {@code > 0}).
   * @param height The drawn height in pixels (must be {@code > 0}).
   * @return {@code this} sprite for chaining.
   */
  @Override
  public FlixelSprite setGraphicSize(int width, int height) {
    FlixelAnimateRig activeRig = rig;
    if (activeRig == null) {
      return super.setGraphicSize(width, height);
    }
    if (width <= 0 || height <= 0) {
      return this;
    }
    float aw = activeRig.anchorWidth;
    float ah = activeRig.anchorHeight;
    if (aw <= 0f || ah <= 0f) {
      return this;
    }
    setScale(width / aw, height / ah);
    updateHitbox();
    return this;
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
    Texture.TextureFilter filter =
        antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest;

    // Without a rig, mirror FlixelSprite.setAntialiasing() exactly: filter only the current region's
    // texture so plain atlas usage stays cheap.
    if (rig == null) {
      Texture texture = (currentRegion != null) ? currentRegion.getTexture() : null;
      if (texture != null) {
        texture.setFilter(filter, filter);
      }
      return;
    }

    // With a rig installed (potentially with multiple merged atlases), every atlas brings its own
    // backing texture and currentRegion is null. Walk the rig's atlas list and set the filter on
    // every unique texture. Adjacent frames usually share the same texture, so we dedupe by
    // identity to avoid hammering glTexParameteri on the same texture once per region.
    Texture lastTexture = null;
    Array<FlixelFrame> atlas = rig.atlas;
    for (int i = 0; i < atlas.size; i++) {
      FlixelFrame frame = atlas.get(i);
      if (frame == null) {
        continue;
      }
      Texture texture = frame.getTexture();
      if (texture == null || texture == lastTexture) {
        continue;
      }
      texture.setFilter(filter, filter);
      lastTexture = texture;
    }
  }

  /**
   * Draws the sprite. With a rig installed, walks the current clip's current keyframe and draws each
   * part through the shared {@link org.flixelgdx.graphics.FlixelSpriteBatch FlixelSpriteBatch} with a preallocated
   * {@link Affine2}. Without a rig (or with a non-sprite {@link Batch}), falls back to the inherited
   * {@link FlixelSprite} draw path.
   *
   * <p>The rig draw composes the sprite's world transform once per frame as
   * {@code T(wx-offsetX, wy-offsetY) * T(originX, originY) * R(angle) * S(sx, sy) * T(-originX/|sx|, -originY/|sy|)}.
   * The asymmetric origin (scaled-world units before the scale, anchor-local units after the scale) is
   * what makes the rig's visible bounding box exactly coincide with the hitbox after
   * {@link #updateHitbox()} for any non-unit scale, while still pivoting rotations around the visual
   * center. Per-part baked matrices are post-multiplied into this base, so rotation, scale, flip,
   * origin, and offset all behave identically to a regular {@link FlixelSprite}.
   *
   * @param batch The active batch.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!visible || !isOnDrawCamera()) {
      return;
    }

    FlixelAnimateRig activeRig = rig;
    if (activeRig == null) {
      super.draw(batch);
      return;
    }

    FlixelAnimationController controller = animation;
    if (controller == null) {
      return;
    }

    // Resolve the clip the controller is currently playing. Fall back to the rig's anchor clip if the
    // controller has not had a clip chosen yet (for example, right after destroy-and-reload).
    String clipName = controller.getCurrentAnim();
    if (clipName == null || clipName.isEmpty()) {
      clipName = activeRig.anchorClipName;
    }
    FlixelAnimateRig.Clip clip = activeRig.getClip(clipName);
    if (clip == null) {
      // Not a rig clip: a Sparrow clip merged via addSparrowAtlas() (or any non-rig atlas clip)
      // renders through the standard frame path off currentFrame / currentRegion.
      super.draw(batch);
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
    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
    float wx = cam.worldToViewX(getX(), scrollX);
    float wy = cam.worldToViewY(getY(), scrollY);

    // Conservative culling using the anchor-clip hitbox. Multi-part rigs may extend beyond this box
    // but it avoids drawing entirely off-screen rigs. Per-part culling is not done here.
    float rigCullW = getWidth() * Math.abs(scaleX);
    float rigCullH = getHeight() * Math.abs(scaleY);
    float rigLeft = wx - getOffsetX();
    float rigBottom = wy - getOffsetY();
    float rigAngle = getAngle();
    if (rigAngle != 0f) {
      float cos = Math.abs(MathUtils.cosDeg(rigAngle));
      float sin = Math.abs(MathUtils.sinDeg(rigAngle));
      float rotW = cos * rigCullW + sin * rigCullH;
      float rotH = sin * rigCullW + cos * rigCullH;
      rigLeft -= (rotW - rigCullW) * 0.5f;
      rigBottom -= (rotH - rigCullH) * 0.5f;
      rigCullW = rotW;
      rigCullH = rotH;
    }
    if (!cam.isInView(rigLeft, rigBottom, rigCullW, rigCullH)) {
      return;
    }

    FlixelShader activeShader = getShader();
    if (activeShader != null
        && activeShader.getProgram() != null
        && batch.getShader() != activeShader.getProgram()) {
      batch.setShader(activeShader.getProgram());
      activeShader.applyUniforms();
    }

    // Match FlixelSprite's flip-into-scale convention: a negative scale on either axis mirrors the
    // sprite around its origin, and the facing flag piles on top of the user-set flipX.
    boolean isFlippedX = flipX || (facing == FlixelDirectionFlags.LEFT);
    boolean isFlippedY = flipY;
    float sx = isFlippedX ? -getScaleX() : getScaleX();
    float sy = isFlippedY ? -getScaleY() : getScaleY();
    float angle = getAngle();
    float ox = getOriginX();
    float oy = getOriginY();

    // Origin/scale unit conversion: FlixelSprite stores originX/originY in the same coordinate space as
    // getWidth()/getHeight() (i.e. scaled-world units, since updateHitbox() folds the absolute scale
    // into the size and then re-centers the origin). The rig's part affines, however, place each part
    // inside an anchor-local rectangle of size (anchorWidth, anchorHeight). To pivot rotation and
    // scaling around the visual center, we therefore translate by +origin in scaled-world units AFTER
    // the scale, but by -origin/|scale| in anchor-local units BEFORE the scale. The two translations
    // collapse to the no-op pair (+origin, -origin) when |scale| == 1, so the unit-scale path stays
    // identical to FlixelSprite's standard transform chain.
    float absSx = Math.abs(sx);
    float absSy = Math.abs(sy);
    float anchorOriginX = (absSx != 0f) ? ox / absSx : 0f;
    float anchorOriginY = (absSy != 0f) ? oy / absSy : 0f;
    boolean hasOrigin = (ox != 0f) || (oy != 0f);
    boolean hasAnchorOrigin = (anchorOriginX != 0f) || (anchorOriginY != 0f);

    // Build the world-space base transform:
    //   T(world) * T(origin_world) * R * S * T(-origin_anchor)
    // Each builder call is a right-multiplication, so the resulting matrix applied to a part-local
    // (anchor-local) point first shifts by -origin_anchor, then scales/rotates, then re-shifts by
    // +origin_world, then translates to the world position. Identity branches are skipped so the
    // no-rotation/no-scale fast path stays cheap.
    baseAffine.idt();
    baseAffine.translate(wx - getOffsetX(), wy - getOffsetY());
    if (hasOrigin) {
      baseAffine.translate(ox, oy);
    }
    if (angle != 0f) {
      baseAffine.rotate(angle);
    }
    if (sx != 1f || sy != 1f) {
      baseAffine.scale(sx, sy);
    }
    if (hasAnchorOrigin) {
      baseAffine.translate(-anchorOriginX, -anchorOriginY);
    }

    batch.setColor(getGdxColor());

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
      // (w, 0), (w, h), (0, h) that Batch.draw() will emit, where (w, h) are the region's
      // pixel dimensions.
      drawAffine.setToProduct(baseAffine, part.local);
      batch.draw(frame.getRegion(), frame.getRegionWidth(), frame.getRegionHeight(), drawAffine);
    }

    batch.setColor(Color.WHITE);

    if (activeShader != null) {
      batch.setShader(null);
    }
  }

  /**
   * Installs a rig built by {@link FlixelAnimateRigLoader}. Callers normally use
   * {@link #addSpritemapAndAnimation}. Sets the sprite's logical size to the anchor bounding box and,
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
