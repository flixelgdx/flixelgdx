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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.animation.FlixelAnimationController;
import org.flixelgdx.animation.FlixelSpritemapJsonLoader;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.FlixelColorable;
import org.flixelgdx.functional.FlixelShaderable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.util.FlixelAxes;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelDirectionFlags;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core building block of all FlixelGDX games. Extends {@link FlixelObject} with graphical
 * capabilities including texture rendering, scaling, rotation, tinting, and flipping.
 *
 * <p>Frame-based clips, Sparrow/XML atlases, and playback use a {@link FlixelAnimationController} that is
 * <strong>not</strong> allocated by default (saves memory for large sprite counts on the order of thousands of
 * extra sprites before overhead dominates). Call {@link #ensureAnimation()} or assign a controller directly
 * when you need clips, then use {@code sprite.ensureAnimation().addSparrowAtlas(...)}, {@code .playAnimation(...)}, etc.
 *
 * <p>It is common to extend {@code FlixelSprite} for your own game's needs; for example, a
 * {@code SpaceShip} class may extend {@code FlixelSprite} but add additional game-specific fields.
 */
public class FlixelSprite extends FlixelObject implements FlixelAntialiasable, FlixelColorable, FlixelShaderable {

  /** Graphic backing this sprite (shared/cached wrapper around a Texture). */
  @Nullable
  protected FlixelGraphic graphic;

  /** The atlas frames used in this sprite (used for animations). */
  @Nullable
  protected Array<FlixelFrame> atlasFrames;

  /**
   * Extra {@link FlixelGraphic} handles retained when additional spritesheets are merged onto this
   * sprite, so those atlases' textures stay loaded for the sprite's lifetime. The primary graphic is
   * still tracked by the {@link #graphic} field; this list only holds graphics appended <em>after</em>
   * the initial load, for example through {@link #mergeSparrowAtlas} or an appended Animate rig atlas.
   * Lazily allocated to avoid the per-instance footprint for sprites that only ever load one atlas.
   */
  @Nullable
  protected Array<FlixelGraphic> secondaryGraphics;

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
   * {@link #loadGraphic(FileHandle)}.
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
  protected final Color color = new Color(Color.WHITE);

  /**
   * Blending mode, functions similarly to Photoshop or Gimp, e.g. "multiply", "screen", etc.
   * Defaults to {@link FlixelBlendMode#NORMAL}, which draws with the usual {@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA}
   * blend function and costs nothing extra.
   */
  @NotNull
  private FlixelBlendMode blendMode = FlixelBlendMode.NORMAL;

  @Nullable
  private static ShaderProgram premultipliedShader;
  @Nullable
  private static ShaderProgram whiteMixShader;

  /** Shared scratch rectangle for clip rect bounds; reused across all sprite draw calls. */
  private static final Rectangle tempClipBounds = new Rectangle();

  /** Shared scratch rectangle for scissor pixel coordinates; reused across all sprite draw calls. */
  private static final Rectangle tempScissors = new Rectangle();

  private static boolean blendMinMaxChecked;
  private static boolean blendMinMaxSupported;

  /** The direction this sprite is facing. Useful for automatic flipping. */
  protected int facing = FlixelDirectionFlags.RIGHT;

  /**
   * X offset of the clip rectangle's left edge, in screen pixels from the sprite's drawn left edge.
   * Active only when {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(int, int, int, int)}.
   */
  private int clipRectX;

  /**
   * Y offset of the clip rectangle's bottom edge, in screen pixels from the sprite's drawn bottom edge.
   * Active only when {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(int, int, int, int)}.
   */
  private int clipRectY;

  /**
   * Width of the visible clip rectangle, in screen pixels. Active only when
   * {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(int, int, int, int)}.
   */
  private int clipRectWidth;

  /**
   * Height of the visible clip rectangle, in screen pixels. Active only when
   * {@link #clipRectEnabled} is {@code true}; see {@link #setClipRect(int, int, int, int)}.
   */
  private int clipRectHeight;

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

  /** Whether a clip rectangle is active; set via {@link #setClipRect(int, int, int, int)} and cleared by {@link #clearClipRect()}. */
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
   * Returns the existing controller or creates and assigns a new {@link FlixelAnimationController} for {@code this} sprite.
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
   * @param path The directory of the {@code .png} to load onto {@code this} sprite.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FileHandle path) {
    return loadGraphic(path.path());
  }

  /**
   * Load's a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param path The directory of the {@code .png} to load onto {@code this} sprite.
   * @param frameWidth How wide the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FileHandle path, int frameWidth) {
    return loadGraphic(path.path(), frameWidth);
  }

  /**
   * Load's a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param path The directory of the {@code .png} to load onto {@code this} sprite.
   * @param frameWidth How wide the sprite should be.
   * @param frameHeight How tall the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FileHandle path, int frameWidth, int frameHeight) {
    return loadGraphic(path.path(), frameWidth, frameHeight);
  }

  /**
   * Loads a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param texture The texture to load onto {@code this} sprite (owned by caller).
   * @param frameWidth How wide the sprite should be.
   * @param frameHeight How tall the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(Texture texture, int frameWidth, int frameHeight) {
    if (graphic != null) {
      graphic.release();
    }
    FlixelAssetManager assets = Flixel.ensureAssets();
    String key = assets.allocateSyntheticKey();
    FlixelGraphic g = new FlixelGraphic(assets, key, texture);
    assets.register(g);
    graphic = g.retain();

    TextureRegion[][] regions = TextureRegion.split(texture, frameWidth, frameHeight);
    frames = wrapFrames(regions);
    currentFrame = frames[0][0];
    updateHitbox(frameWidth, frameHeight);
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
    FlixelGraphic g = Flixel.ensureAssets().<FlixelGraphic>get(assetKey).retain().get();
    Texture t = g.getTexture();
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
    FlixelGraphic g = Flixel.ensureAssets().<FlixelGraphic>get(assetKey).retain().get();
    Texture t = g.getTexture();
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
    FlixelGraphic g = Flixel.ensureAssets().<FlixelGraphic>get(assetKey).retain().get();
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
    Texture texture = g.getTexture();
    TextureRegion[][] regions = TextureRegion.split(texture, frameWidth, frameHeight);
    frames = wrapFrames(regions);
    currentFrame = frames[0][0];
    atlasFrames = null;
    if (animation != null) {
      animation.clear();
    }
    updateHitbox(frameWidth, frameHeight);
    return this;
  }

  private static FlixelFrame[][] wrapFrames(TextureRegion[][] regions) {
    FlixelFrame[][] out = new FlixelFrame[regions.length][];
    for (int i = 0; i < regions.length; i++) {
      TextureRegion[] row = regions[i];
      FlixelFrame[] rowFrames = new FlixelFrame[row.length];
      for (int j = 0; j < row.length; j++) {
        rowFrames[j] = new FlixelFrame(row[j]);
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
  public FlixelSprite makeGraphic(int width, int height, @NotNull Color color) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return loadGraphic(texture, width, height);
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
    return makeGraphic(width, height, color.getGdxColor());
  }

  /**
   * Installs a retained {@link FlixelGraphic} and parsed Sparrow atlas frames. Called by
   * {@link FlixelAnimationController#addSparrowFrames(String, com.badlogic.gdx.utils.XmlReader.Element)} and
   * {@link FlixelSpritemapJsonLoader#load}, not a general API for game code.
   *
   * @param newGraphic Graphic from {@link Flixel#ensureAssets() Flixel.ensureAssets()}{@code .get}(...) with
   *     {@code retain()} already called.
   * @param parsedFrames Frames built from the XML (which may be empty).
   */
  public void applySparrowAtlas(@NotNull FlixelGraphic newGraphic, @NotNull Array<FlixelFrame> parsedFrames) {
    if (graphic != null) {
      graphic.release();
    }
    graphic = newGraphic;
    atlasFrames = parsedFrames;
    frames = null;
    if (animation != null) {
      animation.clear();
    }
    if (parsedFrames.size > 0) {
      FlixelFrame first = parsedFrames.first();
      setCurrentFrameForAnimation(first);
      // Size to the untrimmed source frame, not the trimmed region, so the hitbox and debug box
      // frame the artwork. Playing an animation re-snaps this to that clip's own source frame, so
      // this is just a sensible default for the very first frame.
      setSize(first.originalWidth, first.originalHeight);
      setOriginCenter();
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
      @NotNull FlixelGraphic newGraphic, @NotNull Array<FlixelFrame> parsedFrames) {
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
      secondaryGraphics = new Array<>(2);
    }
    secondaryGraphics.add(graphic);

    if (antialiasing && graphic.isLoaded()) {
      graphic.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }
  }

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
      float cos = Math.abs(MathUtils.cosDeg(angle));
      float sin = Math.abs(MathUtils.sinDeg(angle));
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

    // Non-NORMAL blend modes need this sprite's geometry isolated in its own batch flush, since
    // the blend function/equation (and, for LIGHTEN/DARKEN, the shader) applies to everything the
    // GPU draws until it's restored below.
    boolean blending = blendMode != FlixelBlendMode.NORMAL;
    ShaderProgram previousShader = null;
    if (blending) {
      previousShader = batch.getShader();
      applyBlendMode(batch, blendMode);
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
    boolean clipPushed = false;
    if (clipEnabled) {
      // Flush before changing scissor state so previously batched sprites are not retroactively clipped.
      batch.flush();
      float clipScreenX = wx + offsetX + clipRectX;
      float clipScreenY = wy + offsetY + clipRectY;
      float clipScreenW = clipRectWidth;
      float clipScreenH = clipRectHeight;
      tempClipBounds.set(clipScreenX, clipScreenY, clipScreenW, clipScreenH);
      ScissorStack.calculateScissors(
          cam.getCamera(),
          cam.getViewport().getScreenX(), cam.getViewport().getScreenY(),
          cam.getViewport().getScreenWidth(), cam.getViewport().getScreenHeight(),
          batch.getTransformMatrix(), tempClipBounds, tempScissors);
      clipPushed = ScissorStack.pushScissors(tempScissors);
      if (!clipPushed) {
        if (spriteShader != null) {
          batch.setShader(null);
        }
        if (blending) {
          resetBlendMode(batch, previousShader);
        }
        return;
      }
    }

    batch.setColor(color);
    batch.draw(
        f.getTexture(),
        drawX,
        drawY,
        originXParam,
        originYParam,
        regW,
        regH,
        scaleX,
        scaleY,
        getAngle(),
        f.getRegionX(),
        f.getRegionY(),
        regW,
        regH,
        isFlippedX,
        isFlippedY);
    batch.setColor(Color.WHITE);

    if (clipEnabled && clipPushed) {
      batch.flush();
      ScissorStack.popScissors();
    }

    if (spriteShader != null) {
      batch.setShader(null);
    }

    if (blending) {
      batch.flush(); // Commit this sprite under the special blend state.
      resetBlendMode(batch, previousShader);
    }
  }

  /**
   * Sets how large the graphic is drawn on screen (in pixels), without changing which part of the texture is used.
   *
   * <p>This adjusts {@link #setScale(float, float)} so the full current frame/region maps to the
   * given size. It does <em>not</em> change {@link TextureRegion} bounds: {@code
   * TextureRegion#setRegionWidth}/{@code setRegionHeight} only resize the <strong>source</strong>
   * rectangle inside the texture (UVs), which crops or re-samples texels; the drawable size in
   * this class comes from {@link #getWidth()}/{@link #getHeight()} and scale in {@link #draw}.
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
    float halfViewWidth = Flixel.getWidth() / 2f;
    float halfViewHeight = Flixel.getHeight() / 2f;
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
    color.set(Color.WHITE);
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
      for (int i = 0; i < secondaryGraphics.size; i++) {
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
   * Whether {@code this} sprite holds an owned {@link FlixelGraphic} (e.g. from {@link #makeGraphic(int, int, Color)}),
   * so CPU-side pixmap uploads are allowed without mutating a shared atlas.
   */
  public boolean hasOwnedGraphic() {
    return graphic != null && graphic.isOwned();
  }

  @Nullable
  public FlixelGraphic getGraphic() {
    return graphic;
  }

  public Texture getTexture() {
    return currentFrame != null ? currentFrame.getTexture() : null;
  }

  public float getScaleX() {
    return scaleX;
  }

  public float getScaleY() {
    return scaleY;
  }

  public void setScale(float scaleXY) {
    scaleX = scaleY = scaleXY;
  }

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

  public void setOrigin(float originX, float originY) {
    this.originX = originX;
    this.originY = originY;
  }

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
    Texture texture = currentFrame != null ? currentFrame.getTexture() : null;
    if (texture != null) {
      texture.setFilter(
          antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest,
          antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest);
    }
  }

  @Override
  public void toggleAntialiasing() {
    setAntialiasing(!isAntialiasing());
  }

  public float getAlpha() {
    return color.a;
  }

  public int getFacing() {
    return facing;
  }

  public void setFacing(int facing) {
    this.facing = facing;
  }

  @NotNull
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  public void setBlendMode(FlixelBlendMode blendMode) {
    this.blendMode = blendMode == null ? FlixelBlendMode.NORMAL : blendMode;
    if (!isBlendMinMaxSupported()) {
      if (blendMode == FlixelBlendMode.LIGHTEN || blendMode == FlixelBlendMode.DARKEN) {
        Flixel.warn("FlixelSprite", blendMode
            + " blend mode requires OpenGL ES 3.0, which is not available on this device. Falling back to NORMAL.");
        this.blendMode = FlixelBlendMode.NORMAL;
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getColor() {
    return Color.rgba8888(color);
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public Color getGdxColor() {
    return color;
  }

  /** {@inheritDoc} */
  @Override
  public void setColor(@NotNull Color tint) {
    color.set(tint);
  }

  /** {@inheritDoc} */
  @Override
  public void setColor(@NotNull FlixelColor tint) {
    color.set(tint.getGdxColor());
  }

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

  public void setAlpha(float a) {
    color.a = a;
  }

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

  public void setRegion(TextureRegion region) {
    currentFrame = region != null ? new FlixelFrame(region) : null;
  }

  public FlixelFrame getFrame() {
    return currentFrame;
  }

  public TextureRegion getRegion() {
    return currentFrame != null ? currentFrame.getRegion() : null;
  }

  public int getRegionWidth() {
    return currentFrame != null ? currentFrame.getRegionWidth() : 0;
  }

  public int getRegionHeight() {
    return currentFrame != null ? currentFrame.getRegionHeight() : 0;
  }

  public Array<FlixelFrame> getAtlasRegions() {
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
   * is, they already account for scale - so {@code x=0, y=0} anchors to the drawn bottom-left
   * corner and {@code width=(int)getWidth()} covers the full drawn width regardless of scale.
   *
   * <p>For example, to show only the left half of a sprite regardless of its current scale:
   * <pre>{@code
   * sprite.setClipRect(0, 0, (int)(sprite.getWidth() * 0.5f), (int)sprite.getHeight());
   * // Slide the window right by 10 px later:
   * sprite.setClipRectX(10);
   * // Remove clipping:
   * sprite.clearClipRect();
   * }</pre>
   *
   * <p>Disable clipping with {@link #clearClipRect()}.
   *
   * @param x Left edge of the visible region, in screen pixels from the sprite's drawn left edge.
   * @param y Bottom edge of the visible region, in screen pixels from the sprite's drawn bottom edge.
   * @param width Width of the visible region, in screen pixels.
   * @param height Height of the visible region, in screen pixels.
   */
  public void setClipRect(int x, int y, int width, int height) {
    clipRectX = x;
    clipRectY = y;
    clipRectWidth = Math.max(0, width);
    clipRectHeight = Math.max(0, height);
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

  public int getClipRectX() {
    return clipRectX;
  }

  public void setClipRectX(int clipRectX) {
    this.clipRectX = clipRectX;
  }

  public void changeClipRectX(int clipRectX) {
    this.clipRectX += clipRectX;
  }

  public int getClipRectY() {
    return clipRectY;
  }

  public void setClipRectY(int clipRectY) {
    this.clipRectY = clipRectY;
  }

  public void changeClipRectY(int clipRectY) {
    this.clipRectY += clipRectY;
  }

  public int getClipRectWidth() {
    return clipRectWidth;
  }

  public void setClipRectWidth(int clipRectWidth) {
    this.clipRectWidth = Math.max(0, clipRectWidth);
  }

  public void changeClipRectWidth(int clipRectWidth) {
    setClipRectWidth(this.clipRectWidth + clipRectWidth);
  }

  public int getClipRectHeight() {
    return clipRectHeight;
  }

  public void setClipRectHeight(int clipRectHeight) {
    this.clipRectHeight = Math.max(0, clipRectHeight);
  }

  public void changeClipRectHeight(int clipRectHeight) {
    setClipRectHeight(this.clipRectHeight + clipRectHeight);
  }

  private void applyBlendMode(FlixelBatch batch, FlixelBlendMode mode) {
    switch (mode) {
      case ADD -> batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
      case MULTIPLY -> {
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ZERO);
        batch.setShader(getWhiteMixShader());
      }
      case SCREEN -> {
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
        batch.setShader(getPremultipliedShader());
      }
      case SUBTRACT -> {
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        batch.setShader(getPremultipliedShader());
        Gdx.gl.glBlendEquation(GL20.GL_FUNC_REVERSE_SUBTRACT);
      }
      case LIGHTEN -> {
        if (isBlendMinMaxSupported()) {
          batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
          batch.setShader(getPremultipliedShader());
          setBlendEquationMinMax(GL30.GL_MAX);
        }
      }
      case DARKEN -> {
        if (isBlendMinMaxSupported()) {
          batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
          batch.setShader(getWhiteMixShader());
          setBlendEquationMinMax(GL30.GL_MIN);
        }
      }
      case NORMAL -> {
        // Do nothing.
      }
    }
  }

  private void resetBlendMode(FlixelBatch batch, @Nullable ShaderProgram previousShader) {
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
    batch.setShader(previousShader);
  }

  private void setBlendEquationMinMax(int mode) {
    Gdx.gl.glBlendEquation(mode);
  }

  private static ShaderProgram getPremultipliedShader() {
    if (premultipliedShader == null) {
      premultipliedShader = compileBlendShader(FlixelBlendMode.PREMULTIPLIED_FRAGMENT_SHADER);
    }
    return premultipliedShader;
  }

  private static ShaderProgram getWhiteMixShader() {
    if (whiteMixShader == null) {
      whiteMixShader = compileBlendShader(FlixelBlendMode.WHITE_MIX_FRAGMENT_SHADER);
    }
    return whiteMixShader;
  }

  private static ShaderProgram compileBlendShader(String fragmentSource) {
    ShaderProgram.pedantic = false;
    return new ShaderProgram(FlixelBlendMode.BLEND_VERTEX_SHADER, fragmentSource);
  }

  private static boolean isBlendMinMaxSupported() {
    if (!blendMinMaxChecked) {
      blendMinMaxChecked = true;
      if (Gdx.graphics.isGL30Available()) {
        blendMinMaxSupported = true;
      } else {
        String extensions = Gdx.gl.glGetString(GL20.GL_EXTENSIONS);
        blendMinMaxSupported = extensions != null && extensions.contains("GL_EXT_blend_minmax");
      }
    }
    return blendMinMaxSupported;
  }
}
