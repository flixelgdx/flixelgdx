/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.animation.FlixelAnimationController;
import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core building block of all Flixel games. Extends {@link FlixelObject} with graphical
 * capabilities including texture rendering, scaling, rotation, tinting, and flipping.
 *
 * <p>Frame-based clips, Sparrow/XML atlases, and playback use a {@link FlixelAnimationController} that is
 * <strong>not</strong> allocated by default (saves memory for large sprite counts on the order of thousands of
 * extra sprites before overhead dominates). Call {@link #ensureAnimation()} or assign a controller directly
 * when you need clips, then use {@code sprite.ensureAnimation().playAnimation(...)}, {@code loadSparrowFrames(...)}, etc.
 *
 * <p>It is common to extend {@code FlixelSprite} for your own game's needs; for example, a
 * {@code SpaceShip} class may extend {@code FlixelSprite} but add additional game-specific fields.
 */
public class FlixelSprite extends FlixelObject {

  /** Graphic backing this sprite (shared/cached wrapper around a Texture). */
  @Nullable
  protected FlixelGraphic graphic;

  /** The atlas frames used in this sprite (used for animations). */
  @Nullable
  protected Array<FlixelFrame> atlasFrames;

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

  /** The currently active texture region rendered when no animation is playing. */
  @Nullable
  protected FlixelFrame currentRegion;

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

  /** Whether this sprite is smoothed when scaled. */
  protected boolean antialiasing = false;

  /** The color tint applied when drawing this sprite. */
  protected final Color color = new Color(Color.WHITE);

  /** Whether this sprite is flipped horizontally. */
  protected boolean flipX = false;

  /** Whether this sprite is flipped vertically. */
  protected boolean flipY = false;

  /** The direction this sprite is facing. Useful for automatic flipping. */
  protected int facing = FlixelObject.DirectionFlags.RIGHT;

  /** Constructs a new FlixelSprite with default values. */
  public FlixelSprite() {
    this(0, 0);
  }

  public FlixelSprite(float x,  float y) {
    this(x, y, null);
  }

  public FlixelSprite(float x, float y, String graphicAssetKey) {
    super(x, y);
    if (graphicAssetKey != null && graphicAssetKey.isEmpty()) {
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
    if (frame != null) {
      currentRegion = frame;
    }
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
    String key = assets.allocateSyntheticWrapperKey();
    FlixelGraphic g = new FlixelGraphic(assets, key, texture);
    assets.registerWrapper(g);
    graphic = g.retain();

    TextureRegion[][] regions = TextureRegion.split(texture, frameWidth, frameHeight);
    frames = wrapFrames(regions);
    currentRegion = frames[0][0];
    updateHitbox(frameWidth, frameHeight);
    return this;
  }

  /**
   * Loads a cached graphic by key. The texture can be preloaded via {@link FlixelGraphic#queueLoad()}
   * and {@code Flixel.assets.update()} in a loading state.
   *
   * <p>This method falls back to a synchronous load if the texture is not loaded yet.
   * Preloading is still strongly recommended to avoid mid-frame stalls.
   *
   * @param assetKey The key of the graphic to load.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(String assetKey) {
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class);
    Texture t = requireOrLoad(g);
    return loadGraphic(g, t.getWidth(), t.getHeight());
  }

  /**
   * Loads a cached graphic by key. The texture can be preloaded via {@link FlixelGraphic#queueLoad()}
   * and {@code Flixel.assets.update()} in a loading state.
   *
   * <p>This method falls back to a synchronous load if the texture
   * is not loaded yet. Preloading is still strongly recommended to avoid mid-frame stalls.
   *
   * @param assetKey The key of the graphic to load.
   * @param frameWidth The width of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(String assetKey, int frameWidth) {
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class);
    Texture t = requireOrLoad(g);
    return loadGraphic(g, frameWidth, t.getHeight());
  }

  /**
   * Loads a cached graphic by key. The texture can be preloaded via {@link FlixelGraphic#queueLoad()}
   * and {@link FlixelAssetManager#update()} in a loading state.
   *
   * <p>This method falls back to a synchronous load if the texture is not loaded yet.
   * Preloading is still strongly recommended to avoid mid-frame stalls.
   *
   * @param assetKey The key of the graphic to load.
   * @param frameWidth The width of the graphic.
   * @param frameHeight The height of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(String assetKey, int frameWidth, int frameHeight) {
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class);
    return loadGraphic(g, frameWidth, frameHeight);
  }

  /**
   * Loads a graphic from a {@link FlixelGraphic}.
   *
   * @param g The {@link FlixelGraphic} to load.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelGraphic g) {
    return loadGraphic(g, g.requireTexture().getWidth(), g.requireTexture().getHeight());
  }

  /**
   * Loads a graphic from a {@link FlixelGraphic}.
   *
   * @param g The {@link FlixelGraphic} to load.
   * @param frameWidth The width of the graphic.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FlixelGraphic g, int frameWidth) {
    return loadGraphic(g, frameWidth, g.requireTexture().getHeight());
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
    Texture texture = requireOrLoad(g);
    TextureRegion[][] regions = TextureRegion.split(texture, frameWidth, frameHeight);
    frames = wrapFrames(regions);
    currentRegion = frames[0][0];
    currentFrame = null;
    atlasFrames = null;
    if (animation != null) {
      animation.clear();
    }
    updateHitbox(frameWidth, frameHeight);
    return this;
  }

  @NotNull
  private static Texture requireOrLoad(@NotNull FlixelGraphic g) {
    try {
      return g.requireTexture();
    } catch (IllegalStateException e) {
      return g.loadNow();
    }
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
  public FlixelSprite makeGraphic(int width, int height, Color color) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return loadGraphic(texture, width, height);
  }

  /**
   * Installs a retained {@link FlixelGraphic} and parsed Sparrow atlas frames. Called by
   * {@link FlixelAnimationController#loadSparrowFrames(String, com.badlogic.gdx.utils.XmlReader.Element)} and
   * {@link me.stringdotjar.flixelgdx.animation.FlixelSpritemapJsonLoader#load};
   * not a general API for game code.
   *
   * @param newGraphic Graphic from {@link me.stringdotjar.flixelgdx.Flixel#ensureAssets()}{@code .obtainWrapper}(...)} (implicit retain).
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
      setSize(first.getRegionWidth(), first.getRegionHeight());
    }
  }

  @Override
  public void draw(Batch batch) {
    if (!isOnDrawCamera()) {
      return;
    }
    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.getCamera();
    float wx = getX() - cam.scroll.x * scrollX;
    float wy = getY() - cam.scroll.y * scrollY;
    if (currentFrame != null) {
      float oX = currentFrame.originalWidth / 2f;
      float oY = currentFrame.originalHeight / 2f;

      float drawX = wx - offsetX + currentFrame.offsetX;
      float drawY = wy - offsetY + (currentFrame.originalHeight - currentFrame.getRegionHeight() - currentFrame.offsetY);

      boolean isFlippedX = flipX || (facing == FlixelObject.DirectionFlags.LEFT);
      boolean isFlippedY = flipY;

      batch.setColor(color);
      batch.draw(
        currentFrame.getTexture(),
        drawX,
        drawY,
        oX - currentFrame.offsetX,
        oY - (currentFrame.originalHeight - currentFrame.getRegionHeight() - currentFrame.offsetY),
        currentFrame.getRegionWidth(),
        currentFrame.getRegionHeight(),
        isFlippedX ? -scaleX : scaleX,
        isFlippedY ? -scaleY : scaleY,
        getAngle(),
        currentFrame.getRegionX(),
        currentFrame.getRegionY(),
        currentFrame.getRegionWidth(),
        currentFrame.getRegionHeight(),
        isFlippedX,
        isFlippedY);
      batch.setColor(Color.WHITE);
    } else if (currentRegion != null) {
      boolean isFlippedX = flipX || (facing == FlixelObject.DirectionFlags.LEFT);
      boolean isFlippedY = flipY;

      float sx = isFlippedX ? -scaleX : scaleX;
      float sy = isFlippedY ? -scaleY : scaleY;

      batch.setColor(color);
      batch.draw(
        currentRegion.getRegion(),
        wx - offsetX,
        wy - offsetY,
        originX,
        originY,
        getWidth(),
        getHeight(),
        sx,
        sy,
        getAngle());
      batch.setColor(Color.WHITE);
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
    if (width <= 0 || height <= 0 || currentRegion == null) {
      return this;
    }
    int rw;
    int rh;
    if (currentFrame != null) {
      rw = currentFrame.getRegionWidth();
      rh = currentFrame.getRegionHeight();
    } else {
      rw = currentRegion.getRegionWidth();
      rh = currentRegion.getRegionHeight();
    }
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
   * <p>For textures drawn via {@link #currentRegion}, {@link #draw} uses {@code getWidth() *
   * |scaleX|} (and height), so this folds scale into {@link #setSize(float, float)} and resets
   * scale to {@code 1} to avoid double-scaling. Sparrow/atlas frames ({@link #currentFrame}) keep
   * scale because {@link #draw} sizes that path from the frame {@code region * scale}, while hitbox
   * dimensions are still set to the same effective pixel size for {@link Flixel#overlap}.
   */
  public FlixelSprite updateHitbox() {
    if (currentRegion == null) {
      return this;
    }
    float effW;
    float effH;
    if (currentFrame != null) {
      effW = Math.abs(scaleX) * currentFrame.getRegionWidth();
      effH = Math.abs(scaleY) * currentFrame.getRegionHeight();
      return updateHitbox(effW, effH);
    }
    effW = Math.abs(scaleX) * getWidth();
    effH = Math.abs(scaleY) * getHeight();
    setScale(1f, 1f);
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
    switch (axes) {
      case X -> {
        setPosition(Flixel.getViewWidth() / 2f - getWidth() / 2f, getY());
      }
      case Y -> {
        setPosition(getX(), Flixel.getViewHeight() / 2f - getHeight() / 2f);
      }
      case XY -> {
        setPosition(Flixel.getViewWidth() / 2f - getWidth() / 2f, Flixel.getViewHeight() / 2f - getHeight() / 2f);
      }
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
    antialiasing = false;
    color.set(Color.WHITE);
    flipX = false;
    flipY = false;
    setAngle(0f);
    currentFrame = null;
    currentRegion = null;
    if (atlasFrames != null) {
      atlasFrames.setSize(0);
      atlasFrames = null;
    }
    frames = null;
    if (graphic != null) {
      graphic.release();
      graphic = null;
    }
  }

  /**
   * Whether {@code this} sprite holds an owned {@link FlixelGraphic} (e.g. from {@link #makeGraphic(int, int, Color)}),
   * so CPU-side pixmap uploads are allowed without mutating a shared atlas.
   */
  public boolean hasOwnedGraphic() {
    return graphic != null && graphic.isOwned();
  }

  public Texture getGraphic() {
    return getTexture();
  }

  public Texture getTexture() {
    return currentRegion != null ? currentRegion.getTexture() : null;
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

  public boolean isAntialiasing() {
    return antialiasing;
  }

  public void setAntialiasing(boolean antialiasing) {
    this.antialiasing = antialiasing;
    Texture texture = currentRegion != null ? currentRegion.getTexture() : null;
    if (texture != null) {
      texture.setFilter(
        antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest,
        antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest
      );
    }
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

  public Color getColor() {
    return color;
  }

  public void setColor(Color tint) {
    color.set(tint);
  }

  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
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

  public boolean isFlipY() {
    return flipY;
  }

  public void setRegion(TextureRegion region) {
    currentRegion = region != null ? new FlixelFrame(region) : null;
  }

  public TextureRegion getRegion() {
    return currentRegion != null ? currentRegion.getRegion() : null;
  }

  public int getRegionWidth() {
    return currentRegion != null ? currentRegion.getRegionWidth() : 0;
  }

  public int getRegionHeight() {
    return currentRegion != null ? currentRegion.getRegionHeight() : 0;
  }

  public Array<FlixelFrame> getAtlasRegions() {
    return atlasFrames;
  }

  public FlixelFrame getCurrentFrame() {
    return currentFrame;
  }

  public FlixelFrame[][] getFrames() {
    return frames;
  }
}
