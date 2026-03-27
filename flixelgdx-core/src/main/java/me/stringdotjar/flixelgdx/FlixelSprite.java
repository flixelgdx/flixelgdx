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
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.XmlReader;

import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;
import me.stringdotjar.flixelgdx.util.FlixelAxes;
import me.stringdotjar.flixelgdx.util.FlixelConstants;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core building block of all Flixel games. Extends {@link FlixelObject} with graphical
 * capabilities including texture rendering, animation, scaling, rotation, tinting, and flipping.
 *
 * <p>It is common to extend {@code FlixelSprite} for your own game's needs; for example, a
 * {@code SpaceShip} class may extend {@code FlixelSprite} but add additional game-specific fields.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxSprite.html">FlxSprite (HaxeFlixel)</a>
 */
public class FlixelSprite extends FlixelObject implements Pool.Poolable {

  /** Graphic backing this sprite (shared/cached wrapper around a Texture). */
  @Nullable
  protected FlixelGraphic graphic;

  /** The atlas frames used in this sprite (used for animations). */
  @Nullable
  protected Array<FlixelFrame> atlasFrames;

  /** A map that animations are stored and registered in. */
  protected final ObjectMap<String, Animation<FlixelFrame>> animations;

  /** The current frame that {@code this} sprite is currently using for drawing. */
  @Nullable
  protected FlixelFrame currentFrame;

  /** Used for updating {@code this} sprite's current animation. */
  protected float stateTime = 0;

  /** The name of the current animation playing. */
  private String currentAnim = "";

  /** Is {@code this} sprites current animation looping indefinitely? */
  private boolean looping = true;

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
  protected int facing = FlixelConstants.Graphics.FACING_RIGHT;

  /** Constructs a new FlixelSprite with default values. */
  public FlixelSprite() {
    super();
    animations = new ObjectMap<>();
  }

  /**
   * Updates {@code this} sprite.
   *
   * @param elapsed The amount of time that has passed since the last frame update.
   */
  @Override
  public void update(float elapsed) {
    super.update(elapsed);

    if (animations != null && !animations.isEmpty()) {
      Animation<FlixelFrame> anim = animations.get(currentAnim);
      if (anim != null) {
        stateTime += elapsed;
        currentFrame = anim.getKeyFrame(stateTime, looping);
        currentRegion = currentFrame;
      }
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
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class).retain();
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
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class).retain();
    Texture t = requireOrLoad(g);
    return loadGraphic(g, frameWidth, t.getHeight());
  }

  /**
   * Loads a cached graphic by key. The texture can be preloaded via {@link FlixelGraphic#queueLoad()}
   * and {@code Flixel.assets.update()} in a loading state.
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
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class).retain();
    return loadGraphic(g, frameWidth, frameHeight);
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
    animations.clear();
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
   * @return This sprite for chaining.
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
   * Loads an {@code .xml} spritesheet with {@code SubTexture} data inside of it.
   *
   * @param texture The path to the {@code .png} texture file for slicing and extracting the different frames from.
   * @param xmlFile The path to the {@code .xml} file which contains the data for each subtexture of the sparrow atlas.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadSparrowFrames(FileHandle texture, FileHandle xmlFile) {
    return loadSparrowFrames(texture.path(), new XmlReader().parse(xmlFile));
  }

  /**
   * Loads an {@code .xml} spritesheet with {@code SubTexture} data inside of it.
   *
   * @param textureKey The key of the graphic to load.
   * @param xmlFile The path to the {@code .xml} file which contains the data for each subtexture of the sparrow atlas.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadSparrowFrames(String textureKey, FileHandle xmlFile) {
    return loadSparrowFrames(textureKey, new XmlReader().parse(xmlFile));
  }

  /**
   * Loads an {@code .xml} spritesheet with {@code SubTexture} data inside of it.
   *
   * @param textureKey The key of the graphic to load.
   * @param xmlFile The {@link XmlReader.Element} data which contains the data for each subtexture of the sparrow atlas.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadSparrowFrames(String textureKey, XmlReader.Element xmlFile) {
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(textureKey, FlixelGraphic.class).retain();
    if (graphic != null) {
      graphic.release();
    }
    graphic = g;
    Texture texture = requireOrLoad(g);

    atlasFrames = new Array<>(FlixelFrame[]::new);

    for (XmlReader.Element subTexture : xmlFile.getChildrenByName("SubTexture")) {
      String name = subTexture.getAttribute("name", null);
      int x = subTexture.getInt("x");
      int y = subTexture.getInt("y");
      int width = subTexture.getInt("width");
      int height = subTexture.getInt("height");

      TextureRegion region = new TextureRegion(texture, x, y, width, height);
      FlixelFrame frame = new FlixelFrame(region);
      frame.name = name;

      if (subTexture.hasAttribute("frameX")) {
        frame.offsetX = Math.abs(subTexture.getInt("frameX"));
        frame.offsetY = Math.abs(subTexture.getInt("frameY"));
        frame.originalWidth = subTexture.getInt("frameWidth");
        frame.originalHeight = subTexture.getInt("frameHeight");
      } else {
        frame.offsetX = 0;
        frame.offsetY = 0;
        frame.originalWidth = width;
        frame.originalHeight = height;
      }

      atlasFrames.add(frame);
    }

    if (atlasFrames.size > 0) {
      currentFrame = atlasFrames.first();
      currentRegion = currentFrame;
      setSize(currentFrame.getRegionWidth(), currentFrame.getRegionHeight());
    }

    frames = null;
    animations.clear();
    return this;
  }

  /**
   * Adds an animation by looking for sub textures that start with the prefix passed down.
   *
   * @param name The name of the animation (e.g., "confirm").
   * @param prefix The prefix in the {@code .xml} file (e.g., "left confirm").
   * @param frameRate How fast the animation should play in frames-per-second. Standard is 24.
   * @param loop Should the new animation loop indefinitely?
   */
  public void addAnimationByPrefix(String name, String prefix, int frameRate, boolean loop) {
    Array<FlixelFrame> frames = new Array<>();

    if (atlasFrames == null) return;

    for (FlixelFrame frame : atlasFrames) {
      if (frame != null && frame.name != null && frame.name.startsWith(prefix)) {
        frames.add(frame);
      }
    }

    if (frames.size > 0) {
      // Ensure frames are sorted alphabetically (e.g., confirm0000, confirm0001).
      frames.sort(Comparator.comparing(f -> f.name));
      animations.put(
        name,
        new Animation<>(1f / frameRate, frames, loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL)
      );
    }
  }

  /**
   * Adds a new animation to the animations list if it doesn't exist already.
   *
   * @param name The name of the animation. This is what you'll use every time you use {@code playAnimation()}.
   * @param frameIndices An array of integers used for animation frame indices.
   * @param frameDuration How long each frame lasts for in seconds.
   */
  public void addAnimation(String name, int[] frameIndices, float frameDuration) {
    Array<FlixelFrame> animFrames = new Array<>();

    // Convert 1D indices (0, 1, 2...) to 2D grid coordinates.
    if (frames == null) return;
    int cols = frames[0].length;
    for (int index : frameIndices) {
      int row = index / cols;
      int col = index % cols;
      animFrames.add(frames[row][col]);
    }

    animations.put(name, new Animation<>(frameDuration, animFrames));
  }

  /**
   * Plays an animation {@code this} sprite has, with looping enabled by default.
   *
   * @param name The name of the animation to play.
   */
  public void playAnimation(String name) {
    playAnimation(name, true);
  }

  /**
   * Plays an animation {@code this} sprite has, if it exists.
   *
   * @param name The name of the animation to play.
   * @param loop Should this animation loop indefinitely?
   */
  public void playAnimation(String name, boolean loop) {
    playAnimation(name, loop, true);
  }

  /**
   * Plays an animation {@code this} sprite has, if it exists.
   *
   * @param name The name of the animation to play.
   * @param loop Should this animation loop indefinitely?
   * @param forceRestart Should the animation automatically restart regardless if it's playing?
   */
  public void playAnimation(String name, boolean loop, boolean forceRestart) {
    if (currentAnim.equals(name) && !forceRestart) {
      return;
    }
    if (isAnimationFinished() || forceRestart) {
      this.currentAnim = name;
      this.looping = loop;
      this.stateTime = 0;
    }
  }

  @Override
  public void draw(Batch batch) {
    if (!isOnDrawCamera()) {
      return;
    }
    if (currentFrame != null) {
      float oX = currentFrame.originalWidth / 2f;
      float oY = currentFrame.originalHeight / 2f;

      float drawX = getX() - offsetX + currentFrame.offsetX;
      float drawY = getY() - offsetY + (currentFrame.originalHeight - currentFrame.getRegionHeight() - currentFrame.offsetY);

      boolean isFlippedX = flipX || (facing == FlixelConstants.Graphics.FACING_LEFT);
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
      boolean isFlippedX = flipX || (facing == FlixelConstants.Graphics.FACING_LEFT);
      boolean isFlippedY = flipY;

      float sx = isFlippedX ? -scaleX : scaleX;
      float sy = isFlippedY ? -scaleY : scaleY;

      batch.setColor(color);
      batch.draw(
        currentRegion.getRegion(),
        getX() - offsetX,
        getY() - offsetY,
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
   * Sets how large the graphic is drawn on screen (in pixels), without changing which part of the
   * texture is used.
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
   * scale because {@link #draw} sizes that path from the frame region × scale, while hitbox
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
    setPosition(0f, 0f);
    stateTime = 0;
    currentAnim = null;
    looping = true;
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
    animations.clear();
    if (graphic != null) {
      graphic.release();
      graphic = null;
    }
  }

  public boolean isAnimationFinished() {
    Animation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) return true;
    return anim.isAnimationFinished(stateTime);
  }

  @Override
  public void reset() {
    destroy();
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

  public ObjectMap<String, Animation<FlixelFrame>> getAnimations() {
    return animations;
  }

  public Array<FlixelFrame> getAtlasRegions() {
    return atlasFrames;
  }

  public FlixelFrame getCurrentFrame() {
    return currentFrame;
  }

  public float getStateTime() {
    return stateTime;
  }

  public String getCurrentAnim() {
    return currentAnim;
  }

  public boolean isLooping() {
    return looping;
  }

  public FlixelFrame[][] getFrames() {
    return frames;
  }
}
