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
package org.flixelgdx;

import org.flixelgdx.animation.FlixelAnimationController;
import org.flixelgdx.animation.FlixelSpritemapJsonLoader;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.FlixelColorable;
import org.flixelgdx.functional.FlixelShaderable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.util.FlixelAxes;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelDirectionFlags;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core building block of all FlixelGDX games. Extends {@link FlixelObject} with graphical
 * capabilities: texture rendering, frame grids, Sparrow XML atlases, scaling, rotation, tinting,
 * flipping, blend modes, per-sprite shaders, and scissor clip rects.
 *
 * <h2>Loading graphics</h2>
 * <p>Use {@link #loadGraphic(String)} for asset-managed textures (the texture is cached and
 * reference-counted by {@link org.flixelgdx.asset.FlixelAssetManager FlixelAssetManager}).
 * {@link #makeGraphic(int, int, FlixelColor)} generates a solid-color rectangle on the fly and
 * owns the resulting texture. For Sparrow XML atlases, call
 * {@link #ensureAnimation()}{@code .addSparrowAtlas(...)} instead of loading the texture directly.
 *
 * <h2>Rendering model</h2>
 * <p>Each frame the sprite converts its world position through the active camera to a screen
 * coordinate, then adds {@link #offsetX}/{@link #offsetY} to get the final draw point. Offset
 * shifts the visible artwork relative to the hitbox without moving the physics body - useful for
 * centering asymmetric artwork on a hitbox. {@link #originX}/{@link #originY} sets the pivot used
 * for rotation and scale; call {@link #setOriginCenter()} after every hitbox resize to keep the
 * pivot centered. {@link #scaleX}/{@link #scaleY} multiply each drawn frame's pixel dimensions.
 *
 * <h2>Facing and flip</h2>
 * <p>{@link #facing} automatically mirrors the sprite on X when set to {@link FlixelDirectionFlags#LEFT},
 * so directional sprites do not need a manual flip call on every direction change. {@link #flipX}
 * and {@link #flipY} stack on top of facing for additional control.
 *
 * <h2>Animation</h2>
 * <p>The {@link FlixelAnimationController} is {@code null} by default to save memory for large
 * sprite counts. Call {@link #ensureAnimation()} to create it lazily, then register clips:
 * <pre>{@code
 * FlixelSprite sprite = new FlixelSprite(0, 0, "player.png");
 * sprite.ensureAnimation().add("walk", new int[]{0, 1, 2, 3}, 12, true);
 * sprite.ensureAnimation().playAnimation("walk");
 * }</pre>
 *
 * <p>It is common to extend {@code FlixelSprite} for your own game's needs; for example, a
 * {@code SpaceShip} class may extend {@code FlixelSprite} but add additional game-specific fields.
 *
 * @see FlixelObject
 * @see FlixelAnimationController
 */
public class FlixelSprite extends FlixelObject implements FlixelAntialiasable, FlixelColorable, FlixelShaderable {

  /** Shared scratch rectangle for clip rect bounds; reused across all sprite draw calls. */
  private static final FlixelRect tempClipBounds = new FlixelRect();

  /** Graphic backing this sprite (shared/cached wrapper around a texture). */
  @Nullable
  protected FlixelGraphic graphic;

  /** The atlas frames used in this sprite (used for animations). */
  @Nullable
  protected FlixelArray<FlixelFrame> atlasFrames;

  /**
   * Extra {@link FlixelGraphic} handles retained when additional spritesheets are merged onto this
   * sprite, so those atlases' textures stay loaded for the sprite's lifetime. The primary graphic is
   * still tracked by the {@link #graphic} field; this list only holds graphics appended <em>after</em>
   * the initial load, for example through {@link #mergeSparrowAtlas} or an appended Animate rig atlas.
   * Lazily allocated to avoid the per-instance footprint for sprites that only ever load one atlas.
   */
  @Nullable
  protected FlixelArray<FlixelGraphic> secondaryGraphics;

  /**
   * Heavy controller object for handling animations. {@code null} until {@link #ensureAnimation()} or assigned directly.
   */
  @Nullable
  public FlixelAnimationController animation;

  /** The current frame that {@code this} sprite is currently using for drawing. */
  @Nullable
  protected FlixelFrame currentFrame;

  /**
   * Where all the image frames are stored. This is also where the main image is stored when using
   * {@link #loadGraphic(FlixelFile)}.
   */
  @Nullable
  protected FlixelFrame[][] frames;

  /** Horizontal scale factor. {@code 1} = normal size. */
  protected float scaleX = 1f;

  /** Vertical scale factor. {@code 1} = normal size. */
  protected float scaleY = 1f;

  /** X component of the rotation/scale origin point. */
  protected float originX = 0f;

  /** Y component of the rotation/scale origin point. */
  protected float originY = 0f;

  /** The offset from the sprite's position to its graphic. */
  protected float offsetX = 0f;

  /** The offset from the sprite's position to its graphic. */
  protected float offsetY = 0f;

  /** The color tint applied when drawing this sprite. */
  protected final FlixelColor color = new FlixelColor(FlixelColor.WHITE);

  /**
   * Blending mode, functions similarly to Photoshop or Gimp, e.g. "multiply", "screen", etc.
   * Defaults to {@link FlixelBlendMode#NORMAL}, which draws with the usual {@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA}
   * blend function and costs nothing extra.
   */
  @NotNull
  private FlixelBlendMode blendMode = FlixelBlendMode.NORMAL;

  /** The direction this sprite is facing. Useful for automatic flipping. */
  protected int facing = FlixelDirectionFlags.RIGHT;

  /**
   * X offset of the clip rectangle's left edge, in screen pixels from the sprite's drawn left edge.
   * Active only when {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(float, float, float, float)}.
   */
  private float clipRectX;

  /**
   * Y offset of the clip rectangle's bottom edge, in screen pixels from the sprite's drawn bottom edge.
   * Active only when {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(float, float, float, float)}.
   */
  private float clipRectY;

  /**
   * Width of the visible clip rectangle, in screen pixels. Active only when
   * {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(float, float, float, float)}.
   */
  private float clipRectWidth;

  /**
   * Height of the visible clip rectangle, in screen pixels. Active only when
   * {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(float, float, float, float)}.
   */
  private float clipRectHeight;

  /**
   * The shader applied to this sprite individually, or {@code null} for no per-sprite effect.
   *
   * <p>Set via {@link #setShader(FlixelShader)}. Prefer keeping this {@code null} unless you
   * specifically need a per-sprite effect; each unique shader in draw order costs a GPU batch
   * flush. See {@link org.flixelgdx.functional.FlixelShaderable FlixelShaderable} for the
   * full performance breakdown.
   */
  @Nullable
  private FlixelShader spriteShader;

  /** Whether this sprite is smoothed when scaled. */
  protected boolean antialiasing = false;

  /** Whether this sprite is flipped horizontally. */
  protected boolean flipX = false;

  /** Whether this sprite is flipped vertically. */
  protected boolean flipY = false;

  /**
   * Whether a clip rectangle is active.
   *
   * <p>Set from either {@link #setClipRect(float, float, float, float)}, and cleared by
   * {@link #clearClipRect()}.
   */
  private boolean clipRectEnabled;

  /** Constructs a new FlixelSprite with default values. */
  public FlixelSprite() {
    this(0, 0);
  }

  /**
   * Constructs a new sprite at the given position.
   *
   * @param x The X coordinate to place the new sprite at.
   * @param y The X coordinate to place the new sprite at.
   */
  public FlixelSprite(float x, float y) {
    this(x, y, null);
  }

  /**
   * Constructs a new sprite at the given position with a loaded graphic.
   *
   * @param x The X coordinate to place the new sprite at.
   * @param y The X coordinate to place the new sprite at.
   */
  public FlixelSprite(float x, float y, String graphicAssetKey) {
    super(x, y);
    if (graphicAssetKey != null && !graphicAssetKey.isEmpty()) {
      loadGraphic(graphicAssetKey);
    }
  }

  /**
   * Returns the existing controller or creates and assigns a new {@link FlixelAnimationController}
   * for {@code this} sprite.
   */
  @NotNull
  public FlixelAnimationController ensureAnimation() {
    if (animation == null) {
      animation = new FlixelAnimationController(this);
    }
    return animation;
  }

  /**
   * Updates {@code this} sprite.
   *
   * @param elapsed The amount of time that has passed since the last frame update.
   */
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (animation != null) {
      animation.update(elapsed);
    }
  }

  /**
   * Called by {@link FlixelAnimationController} when the displayed keyframe changes.
   *
   * @param frame The frame to draw, or {@code null} to leave static graphic unchanged.
   */
  public void setCurrentFrameForAnimation(@Nullable FlixelFrame frame) {
    currentFrame = frame;
  }

  /**
   * Clears the active Sparrow / atlas / animation display frame. {@link #draw} will draw nothing
   * until a frame is set again (e.g. by {@link FlixelAnimationController} or {@link #applySparrowAtlas}).
   */
  public void clearAnimationDisplayFrame() {
    currentFrame = null;
  }

  /**
   * Load's a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param file A handle to the {@code .png} to load onto {@code this} sprite.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelFile file) {
    return loadGraphic(file.getPath());
  }

  /**
   * Load's a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param file A handle to the {@code .png} to load onto {@code this} sprite.
   * @param frameWidth How wide the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelFile file, int frameWidth) {
    return loadGraphic(file.getPath(), frameWidth);
  }

  /**
   * Load's a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param file A handle to the {@code .png} to load onto {@code this} sprite.
   * @param frameWidth How wide the sprite should be.
   * @param frameHeight How tall the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelFile file, int frameWidth, int frameHeight) {
    return loadGraphic(file.getPath(), frameWidth, frameHeight);
  }

  /**
   * Loads a texture and automatically resizes the size of {@code this} sprite.
   *
   * <p>This backs {@link #makeGraphic(int, int, FlixelColor)} and other in-memory texture sources, so it
   * is {@code protected} rather than public: game code loads art through the file and asset-key
   * overloads, keeping backend texture types out of the public surface, while subclasses can still
   * override it to guard or specialize in-memory loading.
   *
   * @param texture The texture to load onto {@code this} sprite (owned by caller).
   * @param frameWidth How wide the sprite should be.
   * @param frameHeight How tall the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  protected FlixelSprite loadGraphic(FlixelTexture texture, int frameWidth, int frameHeight) {
    if (graphic != null) {
      graphic.release();
    }
    FlixelAssetManager assets = Flixel.assets;
    String key = assets.allocateSyntheticKey();
    FlixelGraphic g = new FlixelGraphic(assets, key, texture);
    assets.register(g);
    graphic = g.retain();

    frames = splitFrames(texture, frameWidth, frameHeight);
    currentFrame = frames[0][0];
    updateHitbox(frameWidth, frameHeight);
    setAntialiasing(antialiasing);
    return this;
  }

  /**
   * Loads a cached graphic by key. Queue the asset with {@link FlixelAssetManager#load(String)} in
   * a loading state to avoid synchronous stalls on the first frame.
   *
   * @param assetKey The key of the graphic to load.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(String assetKey) {
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(assetKey).retain().get();
    FlixelTexture t = g.getTexture();
    return loadGraphic(g, t.getWidth(), t.getHeight());
  }

  /**
   * Loads a cached graphic by key. Queue the asset with {@link FlixelAssetManager#load(String)} in
   * a loading state to avoid synchronous stalls on the first frame.
   *
   * @param assetKey The key of the graphic to load.
   * @param frameWidth The width of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(String assetKey, int frameWidth) {
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(assetKey).retain().get();
    FlixelTexture t = g.getTexture();
    return loadGraphic(g, frameWidth, t.getHeight());
  }

  /**
   * Loads a cached graphic by key. Queue the asset with {@link FlixelAssetManager#load(String)} in
   * a loading state to avoid synchronous stalls on the first frame.
   *
   * @param assetKey The key of the graphic to load.
   * @param frameWidth The width of the graphic.
   * @param frameHeight The height of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(String assetKey, int frameWidth, int frameHeight) {
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(assetKey).retain().get();
    return loadGraphic(g, frameWidth, frameHeight);
  }

  /**
   * Loads a graphic from a {@link FlixelGraphic}.
   *
   * @param g The {@link FlixelGraphic} to load.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelGraphic g) {
    return loadGraphic(g, g.getTexture().getWidth(), g.getTexture().getHeight());
  }

  /**
   * Loads a graphic from a {@link FlixelGraphic}.
   *
   * @param g The {@link FlixelGraphic} to load.
   * @param frameWidth The width of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelGraphic g, int frameWidth) {
    return loadGraphic(g, frameWidth, g.getTexture().getHeight());
  }

  /**
   * Loads a graphic from a {@link FlixelGraphic}.
   *
   * @param g The {@link FlixelGraphic} to load.
   * @param frameWidth The width of the graphic.
   * @param frameHeight The height of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelGraphic g, int frameWidth, int frameHeight) {
    if (graphic != null) {
      graphic.release();
    }
    graphic = g;
    FlixelTexture texture = g.getTexture();
    frames = splitFrames(texture, frameWidth, frameHeight);
    currentFrame = frames[0][0];
    atlasFrames = null;
    if (animation != null) {
      animation.clear();
    }
    updateHitbox(frameWidth, frameHeight);
    setAntialiasing(antialiasing);
    return this;
  }

  /** Cuts a texture into a grid of equally sized frames, row by row from the top-left. */
  private static FlixelFrame[][] splitFrames(FlixelTexture texture, int frameWidth, int frameHeight) {
    int cols = Math.max(1, texture.getWidth() / Math.max(1, frameWidth));
    int rows = Math.max(1, texture.getHeight() / Math.max(1, frameHeight));
    FlixelFrame[][] out = new FlixelFrame[rows][];
    for (int i = 0; i < rows; i++) {
      FlixelFrame[] rowFrames = new FlixelFrame[cols];
      for (int j = 0; j < cols; j++) {
        rowFrames[j] = new FlixelFrame(texture, j * frameWidth, i * frameHeight, frameWidth, frameHeight);
      }
      out[i] = rowFrames;
    }
    return out;
  }

  /**
   * Creates a solid color rectangular texture on the fly.
   *
   * @param width The width of the graphic.
   * @param height The height of the graphic.
   * @param color The color of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite makeGraphic(int width, int height, @NotNull FlixelColor color) {
    FlixelImage image = new FlixelImage(width, height);
    image.fill(color);
    return loadGraphic(Flixel.graphics.createTexture(image), width, height);
  }

  /**
   * Installs a retained {@link FlixelGraphic} and parsed Sparrow atlas frames. Called by
   * {@link FlixelAnimationController#addSparrowFrames(String)} and
   * {@link FlixelSpritemapJsonLoader#load}, not a general API for game code.
   *
   * @param newGraphic Graphic from {@link Flixel#assets}{@code .get(...)} with
   *     {@code retain()} already called.
   * @param parsedFrames Frames built from the XML (which may be empty).
   */
  public void applySparrowAtlas(@NotNull FlixelGraphic newGraphic, @NotNull FlixelArray<FlixelFrame> parsedFrames) {
    if (graphic != null) {
      graphic.release();
    }
    graphic = newGraphic;
    atlasFrames = parsedFrames;
    frames = null;
    if (animation != null) {
      animation.clear();
    }
    if (parsedFrames.getSize() > 0) {
      FlixelFrame first = parsedFrames.first();
      setCurrentFrameForAnimation(first);
      // Size to the untrimmed source frame, not the trimmed region, so the hitbox and debug box
      // frame the artwork. Playing an animation re-snaps this to that clip's own source frame, so
      // this is just a sensible default for the very first frame.
      setSize(first.originalWidth, first.originalHeight);
      setOriginCenter();
      newGraphic.getTexture().setSmooth(antialiasing);
    }
  }

  /**
   * Merges parsed Sparrow atlas frames onto this sprite's existing atlas instead of replacing it.
   *
   * <p>Where {@link #applySparrowAtlas} swaps in a fresh atlas and clears the sprite's clips, this
   * <em>appends</em> {@code parsedFrames} to whatever atlas the sprite already has (creating one when
   * it had none) and retains {@code newGraphic} as a {@link #retainSecondaryGraphic secondary graphic}
   * so its texture stays loaded. That lets a single sprite carry frames from more than one sheet,
   * which is what {@link FlixelAnimationController#addSparrowFrames(String)} builds on. The currently
   * displayed frame and the registered clips are left untouched, so a sprite already showing a rig
   * clip or another atlas keeps rendering exactly as before; play one of the newly registered clips to
   * show the merged art.
   *
   * @param newGraphic The graphic backing {@code parsedFrames}, already retained by its loader.
   * @param parsedFrames The frames to append, which may be empty.
   */
  public void mergeSparrowAtlas(
      @NotNull FlixelGraphic newGraphic, @NotNull FlixelArray<FlixelFrame> parsedFrames) {
    retainSecondaryGraphic(newGraphic);
    if (atlasFrames == null) {
      atlasFrames = parsedFrames;
    } else {
      atlasFrames.addAll(parsedFrames);
    }
  }

  /**
   * Retains an additional {@link FlixelGraphic} so its texture stays loaded until this sprite is
   * destroyed, and propagates the sprite's current antialiasing setting onto the new graphic's
   * texture so an appended atlas matches the visual filter of the original.
   *
   * <p>The graphic is assumed to have already been retained by the caller (typically via
   * {@link org.flixelgdx.asset.FlixelAssetManager#get(String) FlixelAssetManager.get(...)} followed
   * by {@link org.flixelgdx.asset.FlixelAsset#retain() retain()}), so this method only stores the
   * reference and does not call {@link FlixelGraphic#retain()} again. This is an advanced hook used
   * by atlas-merging code such as {@link FlixelAnimationController#addSparrowFrames(String)} and
   * the Animate rig loader; most game code never calls it directly.
   *
   * @param graphic The graphic to retain for the sprite's lifetime. Must not be {@code null}.
   */
  public void retainSecondaryGraphic(@NotNull FlixelGraphic graphic) {
    if (secondaryGraphics == null) {
      secondaryGraphics = new FlixelArray<>(2);
    }
    secondaryGraphics.add(graphic);

    if (graphic.isLoaded()) {
      graphic.getTexture().setSmooth(antialiasing);
    }
  }

  /**
   * Renders this sprite for the current camera pass.
   *
   * <p>The pipeline runs in order: visibility and camera-assignment check, per-frame null guard,
   * world-to-screen coordinate transform, rotation-aware AABB cull against the camera viewport,
   * blend-mode isolation (flush before and after when non-NORMAL), per-sprite shader switch,
   * trim-aware inset calculation for Sparrow atlas frames, optional scissor push, and finally the
   * batch draw call with scale, rotation, and flip applied around {@link #originX}/{@link #originY}.
   *
   * <p>Override this to add custom rendering on top of the default sprite, but call
   * {@code super.draw(batch)} to keep the standard pipeline intact.
   *
   * @param batch The batch to draw into.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!visible) {
      return;
    }
    if (!isOnDrawCamera()) {
      return;
    }
    FlixelFrame f = currentFrame;
    if (f == null) {
      return;
    }

    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
    float wx = cam.worldToViewX(getX(), scrollX);
    float wy = cam.worldToViewY(getY(), scrollY);

    float drawLeft = wx + offsetX;
    float drawBottom = wy + offsetY;
    // Use the actual graphic dimensions for culling rather than the hitbox, since the hitbox may
    // have been shrunk independently (e.g. via setSize()) while the visible sprite remains larger.
    float cullW = f.originalWidth * Math.abs(scaleX);
    float cullH = f.originalHeight * Math.abs(scaleY);
    float angle = getAngle();
    if (angle != 0f) {
      float cos = Math.abs(FlixelMath.cosDeg(angle));
      float sin = Math.abs(FlixelMath.sinDeg(angle));
      float rotW = cos * cullW + sin * cullH;
      float rotH = sin * cullW + cos * cullH;
      drawLeft -= (rotW - cullW) * 0.5f;
      drawBottom -= (rotH - cullH) * 0.5f;
      cullW = rotW;
      cullH = rotH;
    }
    if (!cam.isInView(drawLeft, drawBottom, cullW, cullH)) {
      return;
    }

    // Non-NORMAL blend modes need this sprite's geometry isolated in its own batch flush, since the
    // blend state applies to everything the GPU draws until it's restored below. The batch flushes
    // pending geometry internally when the mode changes, so no explicit flush is needed here.
    boolean blending = blendMode != FlixelBlendMode.NORMAL;
    if (blending) {
      batch.setBlendMode(blendMode);
    }

    // Switch the batch to this sprite's custom shader before drawing. batch.setShader() flushes
    // pending geometry internally before switching, so no explicit flush is needed.
    if (spriteShader != null) {
      if (spriteShader.getProgram() != null && batch.getShader() != spriteShader.getProgram()) {
        batch.setShader(spriteShader.getProgram());
        spriteShader.applyUniforms();
      }
    }

    int srcW = f.originalWidth;
    int srcH = f.originalHeight;
    int regW = f.getRegionWidth();
    int regH = f.getRegionHeight();

    boolean isFlippedX = flipX || (facing == FlixelDirectionFlags.LEFT);
    boolean isFlippedY = flipY;

    // Place the trimmed region inside its untrimmed source box, then anchor that box at the
    // sprite's position. Mirroring is computed around the source box (not the trimmed region) so a
    // left-facing pose lines up with its right-facing counterpart.
    int insetX = FlixelFrame.regionInsetX(srcW, regW, f.offsetX, isFlippedX);
    int insetY = FlixelFrame.regionInsetY(srcH, regH, f.offsetY, isFlippedY);

    float drawX = wx + offsetX + insetX + srcW * (scaleX - 1) * 0.5f;
    float drawY = wy + offsetY + insetY + srcH * (scaleY - 1) * 0.5f;

    // Rotate/scale around the source box's center, expressed relative to the region's bottom-left
    // corner (the origin that the batch.draw(...) overload below measures from).
    float originXParam = srcW / 2f - insetX;
    float originYParam = srcH / 2f - insetY;

    boolean clipEnabled = clipRectEnabled;
    if (clipEnabled) {
      // Flush before changing scissor state so previously batched sprites are not retroactively clipped.
      batch.flush();
      float clipViewX = wx + offsetX + clipRectX;
      float clipViewY = wy + offsetY + clipRectY;
      cam.getViewport().projectToScissor(clipViewX, clipViewY, clipRectWidth, clipRectHeight, tempClipBounds);
      Flixel.graphics.setScissor(
          Math.round(tempClipBounds.x), Math.round(tempClipBounds.y),
          Math.round(tempClipBounds.width), Math.round(tempClipBounds.height));
    }

    batch.setColor(color);
    batch.draw(
        f,
        drawX,
        drawY,
        originXParam,
        originYParam,
        regW,
        regH,
        scaleX,
        scaleY,
        getAngle(),
        isFlippedX,
        isFlippedY);
    batch.setColor(FlixelColor.WHITE);

    if (clipEnabled) {
      batch.flush();
      Flixel.graphics.clearScissor();
    }

    if (spriteShader != null) {
      batch.setShader(null);
    }

    if (blending) {
      batch.flush(); // Commit this sprite under the special blend state.
      batch.setBlendMode(FlixelBlendMode.NORMAL);
    }
  }

  /**
   * Sets how large the graphic is drawn on screen (in pixels), without changing which part of the texture is used.
   *
   * <p>This adjusts {@link #setScale(float, float)} so the full current frame/region maps to the
   * given size. It does <em>not</em> change the frame's source region bounds inside the texture;
   * the drawable size in this class comes from {@link #getWidth()}/{@link #getHeight()} and scale
   * in {@link #draw}.
   *
   * @param width The drawn width in pixels (must be {@code > 0}).
   * @param height The drawn height in pixels (must be {@code > 0}).
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite setGraphicSize(int width, int height) {
    if (width <= 0 || height <= 0 || currentFrame == null) {
      return this;
    }
    int rw = currentFrame.originalWidth;
    int rh = currentFrame.originalHeight;
    if (rw <= 0 || rh <= 0) {
      return this;
    }
    setScale(width / (float) rw, height / (float) rh);
    updateHitbox();
    return this;
  }

  /**
   * Sets the hitbox to match the on-screen graphic.
   *
   * <p>{@link #draw} sizes every frame from {@link FlixelFrame#originalWidth}/{@link FlixelFrame#originalHeight}
   * and scale separately, so the hitbox is set to the frame's untrimmed source size times
   * {@code |scale|} so the box frames the whole drawn artwork (matching HaxeFlixel's
   * {@code frameWidth}/{@code frameHeight}), not just the trimmed pixels.
   */
  public FlixelSprite updateHitbox() {
    if (currentFrame == null) {
      return this;
    }
    float effW = Math.abs(scaleX) * currentFrame.originalWidth;
    float effH = Math.abs(scaleY) * currentFrame.originalHeight;
    return updateHitbox(effW, effH);
  }

  /**
   * Updates the hitbox of {@code this} sprite to the size of the given width and height.
   *
   * @param width The width of the hitbox.
   * @param height The height of the hitbox.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite updateHitbox(float width, float height) {
    setSize(width, height);
    setOriginCenter();
    return this;
  }

  /**
   * Centers {@code this} sprite on the screen.
   *
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite screenCenter() {
    return screenCenter(FlixelAxes.XY);
  }

  /**
   * Centers {@code this} sprite on the screen.
   *
   * @param axes The axes to center on.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite screenCenter(FlixelAxes axes) {
    float halfWidth = getWidth() / 2f;
    float halfHeight = getHeight() / 2f;
    float halfViewWidth = Flixel.getVisibleWidth() / 2f;
    float halfViewHeight = Flixel.getVisibleHeight() / 2f;
    switch (axes) {
      case X -> setPosition(halfViewWidth - halfWidth, getY());
      case Y -> setPosition(getX(), halfViewHeight - halfHeight);
      case XY -> setPosition(halfViewWidth - halfWidth, halfViewHeight - halfHeight);
    }
    return this;
  }

  @Override
  public void destroy() {
    super.destroy();
    if (animation != null) {
      animation.clear();
      animation = null;
    }
    scaleX = 1f;
    scaleY = 1f;
    originX = 0f;
    originY = 0f;
    offsetX = 0f;
    offsetY = 0f;
    spriteShader = null;
    blendMode = FlixelBlendMode.NORMAL;
    antialiasing = false;
    color.set(FlixelColor.WHITE);
    flipX = false;
    flipY = false;
    setAngle(0f);
    currentFrame = null;
    clipRectX = 0;
    clipRectY = 0;
    clipRectWidth = 0;
    clipRectHeight = 0;
    clipRectEnabled = false;
    if (atlasFrames != null) {
      atlasFrames.setSize(0);
      atlasFrames = null;
    }
    frames = null;
    if (secondaryGraphics != null) {
      // Balance every retain from merged sheets (get() + retain()); the primary graphic below is
      // released separately through its own field.
      for (int i = 0; i < secondaryGraphics.getSize(); i++) {
        FlixelGraphic g = secondaryGraphics.get(i);
        if (g != null) {
          g.release();
        }
      }
      secondaryGraphics.clear();
      secondaryGraphics = null;
    }
    if (graphic != null) {
      graphic.release();
      graphic = null;
    }
  }

  /**
   * Assigns a shader that is applied to this sprite individually when it is drawn.
   *
   * <p>Each unique shader transition in draw order flushes the GPU vertex buffer before the new
   * shader takes over. Consecutive sprites that share the same {@link FlixelShader} instance batch
   * together for free. If you mix many different shaders across sprites in a single camera,
   * performance may drop noticeably on weak devices. Giving players the option to disable sprite
   * shaders is strongly recommended.
   *
   * <p>The shader is NOT owned by this sprite. Call {@link FlixelShader#destroy()} yourself when
   * the shader is no longer needed. Pass {@code null} to remove the current shader.
   *
   * <p>If you need a full-scene effect (post-processing applied to everything a camera sees),
   * prefer {@link org.flixelgdx.FlixelCamera#setShader(FlixelShader) FlixelCamera.setShader()}
   * instead, as it captures the entire scene into a single FBO and applies the shader once, with
   * no per-sprite flush cost.
   *
   * @param shader The shader to apply when drawing this sprite, or {@code null} to remove it.
   */
  @Override
  public void setShader(@Nullable FlixelShader shader) {
    this.spriteShader = shader;
  }

  /**
   * Returns the shader currently assigned to this sprite, or {@code null} if none is set.
   *
   * @return The active per-sprite {@link FlixelShader}, or {@code null}.
   */
  @Nullable
  @Override
  public FlixelShader getShader() {
    return spriteShader;
  }

  /**
   * Whether {@code this} sprite holds an owned {@link FlixelGraphic} (e.g. from {@link #makeGraphic(int, int, FlixelColor)}),
   * so CPU-side pixmap uploads are allowed without mutating a shared atlas.
   */
  public boolean hasOwnedGraphic() {
    return graphic != null && graphic.isOwned();
  }

  /**
   * Returns the {@link FlixelGraphic} currently loaded onto this sprite, or {@code null} if no
   * graphic has been loaded. The graphic is reference-counted; do not call {@link FlixelGraphic#release()}
   * on the returned instance unless you called {@link FlixelGraphic#retain()} on it yourself.
   *
   * @return The active graphic, or {@code null}.
   */
  @Nullable
  public FlixelGraphic getGraphic() {
    return graphic;
  }

  /**
   * Returns the backing {@link FlixelTexture} of the current frame, or {@code null} if no frame is
   * loaded. The texture is owned by the sprite's {@link FlixelGraphic}; do not destroy it directly.
   *
   * @return The current frame's texture, or {@code null}.
   */
  public FlixelTexture getTexture() {
    return currentFrame != null ? currentFrame.getTexture() : null;
  }

  public float getScaleX() {
    return scaleX;
  }

  public float getScaleY() {
    return scaleY;
  }

  /**
   * Sets both {@link #scaleX} and {@link #scaleY} to the same value. For non-uniform scale use
   * {@link #setScale(float, float)} instead.
   *
   * @param scaleXY Scale multiplier applied to both axes.
   */
  public void setScale(float scaleXY) {
    scaleX = scaleY = scaleXY;
  }

  /**
   * Sets horizontal and vertical scale independently.
   *
   * <p>Negative values flip the drawn image on the corresponding axis, but prefer
   * {@link #setFlipX(boolean)} / {@link #setFlipY(boolean)} for explicit mirroring so intent is
   * clear. Call {@link #updateHitbox()} afterward if the hitbox should match the new drawn size.
   *
   * @param scaleX Horizontal scale multiplier.
   * @param scaleY Vertical scale multiplier.
   */
  public void setScale(float scaleX, float scaleY) {
    this.scaleX = scaleX;
    this.scaleY = scaleY;
  }

  public void setScaleX(float scaleX) {
    this.scaleX = scaleX;
  }

  public void setScaleY(float scaleY) {
    this.scaleY = scaleY;
  }

  public float getOriginX() {
    return originX;
  }

  public float getOriginY() {
    return originY;
  }

  /**
   * Sets the pivot point used for rotation and scale, measured from the sprite's bottom-left draw
   * corner in pixels. Default is {@code (0, 0)}, which rotates and scales around the bottom-left
   * corner. Call {@link #setOriginCenter()} to rotate around the center instead.
   *
   * @param originX Horizontal pivot offset in pixels.
   * @param originY Vertical pivot offset in pixels.
   */
  public void setOrigin(float originX, float originY) {
    this.originX = originX;
    this.originY = originY;
  }

  /**
   * Sets the rotation and scale pivot to the center of the current hitbox.
   *
   * <p>Call this after every hitbox resize (for example after {@link #updateHitbox()} or
   * {@link #setSize(float, float)}) to keep the pivot centered - it is not updated automatically
   * when the hitbox changes.
   */
  public void setOriginCenter() {
    originX = getWidth() / 2f;
    originY = getHeight() / 2f;
  }

  public float getOffsetX() {
    return offsetX;
  }

  public void setOffsetX(float offsetX) {
    this.offsetX = offsetX;
  }

  public float getOffsetY() {
    return offsetY;
  }

  public void setOffsetY(float offsetY) {
    this.offsetY = offsetY;
  }

  /**
   * Sets both graphic offset components at once.
   *
   * <p>Offset shifts the visible artwork relative to the hitbox position without moving the physics
   * body. A common use is to nudge artwork that does not perfectly align with its hitbox after
   * trimming (for example, centering a 64x64 sprite on a 32x32 hitbox: {@code setOffset(-16, -16)}).
   *
   * @param x Horizontal offset in pixels.
   * @param y Vertical offset in pixels.
   */
  public void setOffset(float x, float y) {
    this.offsetX = x;
    this.offsetY = y;
  }

  @Override
  public boolean isAntialiasing() {
    return antialiasing;
  }

  /** Returns whether linear texture filtering (antialiasing) is enabled for this sprite. */
  public boolean getAntialiasing() {
    return antialiasing;
  }

  @Override
  public void setAntialiasing(boolean antialiasing) {
    this.antialiasing = antialiasing;
    FlixelTexture texture = currentFrame != null ? currentFrame.getTexture() : null;
    if (texture != null) {
      texture.setSmooth(antialiasing);
    }
    if (secondaryGraphics != null) {
      for (FlixelGraphic g : secondaryGraphics) {
        g.getTexture().setSmooth(antialiasing);
      }
    }
  }

  @Override
  public void toggleAntialiasing() {
    setAntialiasing(!isAntialiasing());
  }

  /**
   * Returns the alpha component of this sprite's tint color, in the range {@code [0, 1]}.
   * This is sugar for {@link #getColor()}{@code .a}; both refer to the same underlying value.
   *
   * @return Alpha transparency, where {@code 0} is fully transparent and {@code 1} is fully opaque.
   */
  public float getAlpha() {
    return color.a;
  }

  /**
   * Returns the current facing direction. The value is one of the {@link FlixelDirectionFlags}
   * constants; default is {@link FlixelDirectionFlags#RIGHT}.
   *
   * @return Current facing direction flag.
   */
  public int getFacing() {
    return facing;
  }

  /**
   * Sets the direction this sprite faces. When set to {@link FlixelDirectionFlags#LEFT}, the draw
   * pipeline automatically mirrors the graphic on the X axis as if {@link #flipX} were {@code true},
   * so directional characters do not need a manual flip on every direction change. {@link #flipX}
   * and {@link #flipY} stack on top of this for additional control.
   *
   * @param facing A {@link FlixelDirectionFlags} constant - typically {@link FlixelDirectionFlags#LEFT}
   *     or {@link FlixelDirectionFlags#RIGHT}.
   */
  public void setFacing(int facing) {
    this.facing = facing;
  }

  /**
   * Returns the current blend mode. Never {@code null} - defaults to {@link FlixelBlendMode#NORMAL}.
   *
   * @return The active blend mode.
   */
  @NotNull
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  /**
   * Sets the blend mode applied when this sprite is drawn.
   *
   * <p>Some modes (for example {@link FlixelBlendMode#LIGHTEN} and {@link FlixelBlendMode#DARKEN})
   * require capabilities not present on every backend; the active graphics backend falls back to
   * {@link FlixelBlendMode#NORMAL} when it cannot honor a mode. All non-NORMAL modes force a GPU
   * batch flush before and after this sprite, which can reduce draw-call batching efficiency when
   * many sprites use different blend modes in the same draw pass. Passing {@code null} is safe and
   * resets to {@link FlixelBlendMode#NORMAL}.
   *
   * @param blendMode The blend mode to apply, or {@code null} to reset to NORMAL.
   */
  public void setBlendMode(FlixelBlendMode blendMode) {
    this.blendMode = blendMode == null ? FlixelBlendMode.NORMAL : blendMode;
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public FlixelColor getColor() {
    return color;
  }

  /** {@inheritDoc} */
  @Override
  public void setColor(@NotNull FlixelColor tint) {
    color.set(tint);
  }

  /**
   * Sets the tint color by individual RGBA components, each in the range {@code [0, 1]}.
   *
   * @param r Red channel.
   * @param g Green channel.
   * @param b Blue channel.
   * @param a Alpha channel ({@code 0} = transparent, {@code 1} = opaque).
   */
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
  }

  /** {@inheritDoc} */
  @Override
  public float getShakeX() {
    return offsetX;
  }

  /** {@inheritDoc} */
  @Override
  public float getShakeY() {
    return offsetY;
  }

  /** {@inheritDoc} */
  @Override
  public void setShake(float x, float y) {
    offsetX = x;
    offsetY = y;
  }

  /**
   * Sets the alpha component of this sprite's tint color. Values are expected in {@code [0, 1]}.
   * Equivalent to {@code getColor().a = a}. RGB channels are unchanged.
   *
   * @param a New alpha value, where {@code 0} is fully transparent and {@code 1} is fully opaque.
   */
  public void setAlpha(float a) {
    color.a = a;
  }

  /**
   * Toggles the flip state on the requested axes using XOR, so calling this twice with the same
   * arguments returns to the original orientation.
   *
   * <p>Unlike {@link #setFlipX(boolean)}/{@link #setFlipY(boolean)}, which force a specific state,
   * this is a toggle, which is useful for one-shot direction reversal without tracking current state:
   * <pre>{@code
   * sprite.flip(true, false); // now mirrored on X
   * sprite.flip(true, false); // back to normal
   * }</pre>
   *
   * @param x {@code true} to toggle the horizontal flip.
   * @param y {@code true} to toggle the vertical flip.
   */
  public void flip(boolean x, boolean y) {
    flipX ^= x;
    flipY ^= y;
  }

  public boolean isFlipX() {
    return flipX;
  }

  public boolean getFlipX() {
    return flipX;
  }

  public void setFlipX(boolean flipX) {
    this.flipX = flipX;
  }

  public boolean isFlipY() {
    return flipY;
  }

  public boolean getFlipY() {
    return flipY;
  }

  public void setFlipY(boolean flipY) {
    this.flipY = flipY;
  }

  /**
   * Replaces the currently displayed frame directly, bypassing the frame grid and animation
   * system. Useful for one-off sprites that source a frame from an existing atlas without going
   * through {@link #loadGraphic(String)} or {@link #applySparrowAtlas(FlixelGraphic, FlixelArray)}.
   * Pass {@code null} to clear the displayed frame.
   *
   * @param region The frame to display, or {@code null} to stop rendering.
   */
  public void setRegion(FlixelFrame region) {
    currentFrame = region;
  }

  public FlixelFrame getFrame() {
    return currentFrame;
  }

  public FlixelFrame getRegion() {
    return currentFrame;
  }

  public int getRegionWidth() {
    return currentFrame != null ? currentFrame.getRegionWidth() : 0;
  }

  public int getRegionHeight() {
    return currentFrame != null ? currentFrame.getRegionHeight() : 0;
  }

  public FlixelArray<FlixelFrame> getAtlasRegions() {
    return atlasFrames;
  }

  public @Nullable FlixelFrame getCurrentFrame() {
    return currentFrame;
  }

  public FlixelFrame[][] getFrames() {
    return frames;
  }

  public boolean isClipRectEnabled() {
    return clipRectEnabled;
  }

  /**
   * Sets the clip rectangle in screen-pixel space relative to the sprite's drawn position, and enables clipping.
   *
   * <p>Only the region inside the rectangle is drawn; pixels outside are discarded by the GPU
   * scissor. Coordinates are in the same units as {@link #getWidth()}/{@link #getHeight()} - that
   * is, they already account for scale, so {@code x=0, y=0} anchors to the drawn bottom-left
   * corner and {@code width=getWidth()} covers the full drawn width regardless of scale.
   *
   * <p>For example, to show only the left half of a sprite regardless of its current scale:
   * <pre>{@code
   * sprite.setClipRect(0, 0, sprite.getWidth() * 0.5f, sprite.getHeight());
   * // Slide the window right by 10 px later:
   * sprite.changeClipRectX(10);
   * // Remove clipping:
   * sprite.clearClipRect();
   * }</pre>
   *
   * @param x Left edge of the visible region, in screen pixels from the sprite's drawn left edge.
   * @param y Bottom edge of the visible region, in screen pixels from the sprite's drawn bottom edge.
   * @param width Width of the visible region, in screen pixels.
   * @param height Height of the visible region, in screen pixels.
   */
  public void setClipRect(float x, float y, float width, float height) {
    clipRectX = x;
    clipRectY = y;
    clipRectWidth = FlixelMath.clamp(width, 0, getWidth());
    clipRectHeight = FlixelMath.clamp(height, 0, getHeight());
    clipRectEnabled = true;
  }

  /** Disables the clip rectangle and resets all clip values to zero. */
  public void clearClipRect() {
    clipRectX = 0;
    clipRectY = 0;
    clipRectWidth = 0;
    clipRectHeight = 0;
    clipRectEnabled = false;
  }

  public float getClipRectX() {
    return clipRectX;
  }

  public void setClipRectX(float clipRectX) {
    this.clipRectX = clipRectX;
  }

  public void changeClipRectX(float clipRectX) {
    this.clipRectX += clipRectX;
  }

  public float getClipRectY() {
    return clipRectY;
  }

  public void setClipRectY(float clipRectY) {
    this.clipRectY = clipRectY;
  }

  public void changeClipRectY(float clipRectY) {
    this.clipRectY += clipRectY;
  }

  public float getClipRectWidth() {
    return clipRectWidth;
  }

  public void setClipRectWidth(float clipRectWidth) {
    this.clipRectWidth = FlixelMath.clamp(clipRectWidth, 0, getWidth());
  }

  public void changeClipRectWidth(float clipRectWidth) {
    setClipRectWidth(this.clipRectWidth + clipRectWidth);
  }

  public float getClipRectHeight() {
    return clipRectHeight;
  }

  public void setClipRectHeight(float clipRectHeight) {
    this.clipRectHeight = FlixelMath.clamp(clipRectHeight, 0, getHeight());
  }

  public void changeClipRectHeight(float clipRectHeight) {
    setClipRectHeight(this.clipRectHeight + clipRectHeight);
  }
}
