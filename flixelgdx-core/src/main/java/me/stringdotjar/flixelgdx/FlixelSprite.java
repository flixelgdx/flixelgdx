package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.XmlReader;
import me.stringdotjar.flixelgdx.display.FlixelCamera;
import me.stringdotjar.flixelgdx.util.FlixelConstants;

import java.util.Comparator;

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

  /** The texture image that {@code this} sprite uses. */
  protected Texture texture;

  /** The hitbox used for collision detection and angling. */
  protected Rectangle hitbox;

  /** The cameras that {@code this} sprite is projected onto. */
  protected FlixelCamera[] cameras;

  /** The atlas regions used in this sprite (used for animations). */
  protected Array<TextureAtlas.AtlasRegion> atlasRegions;

  /** A map that animations are stored and registered in. */
  protected final ObjectMap<String, Animation<TextureRegion>> animations;

  /** The current frame that {@code this} sprite is on in its animation (if one is playing). */
  protected TextureAtlas.AtlasRegion currentFrame;

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
  protected TextureRegion[][] frames;

  /** The currently active texture region rendered when no animation is playing. */
  protected TextureRegion currentRegion;

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

  public FlixelSprite() {
    super();
    animations = new ObjectMap<>();
    cameras = new FlixelCamera[]{Flixel.getCamera()};
  }

  /**
   * Updates {@code this} sprite.
   *
   * @param elapsed The amount of time that has passed since the last frame update.
   */
  @Override
  public void update(float elapsed) {
    if (moves) {
      updateMotion(elapsed);
    }

    if (animations != null && !animations.isEmpty()) {
      Animation<TextureRegion> anim = animations.get(currentAnim);
      if (anim != null) {
        stateTime += elapsed;
        currentFrame = (TextureAtlas.AtlasRegion) anim.getKeyFrame(stateTime, looping);
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
    Texture texture = new Texture(path);
    return loadGraphic(texture, texture.getWidth(), texture.getHeight());
  }

  /**
   * Load's a texture and automatically resizes the size of {@code this} sprite.
   *
   * @param path The directory of the {@code .png} to load onto {@code this} sprite.
   * @param frameWidth How wide the sprite should be.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadGraphic(FileHandle path, int frameWidth) {
    Texture texture = new Texture(path);
    return loadGraphic(texture, frameWidth, texture.getHeight());
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
    return loadGraphic(new Texture(path), frameWidth, frameHeight);
  }

  public FlixelSprite loadGraphic(Texture texture, int frameWidth, int frameHeight) {
    this.texture = texture;
    frames = TextureRegion.split(texture, frameWidth, frameHeight);
    currentRegion = frames[0][0];
    setSize(frameWidth, frameHeight);
    setOriginCenter();
    return this;
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
   * @param texture The path to the {@code .png} texture file for slicing and extracting the
   * different frames from.
   * @param xmlFile The path to the {@code .xml} file which contains the data for each subtexture of
   * the sparrow atlas.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadSparrowFrames(FileHandle texture, FileHandle xmlFile) {
    return loadSparrowFrames(new Texture(texture), new XmlReader().parse(xmlFile));
  }

  /**
   * Loads an {@code .xml} spritesheet with {@code SubTexture} data inside of it.
   *
   * @param texture The {@code .png} texture file for slicing and extracting the different frames
   * from.
   * @param xmlFile The {@link XmlReader.Element} data which contains the data for each subtexture
   * of the sparrow atlas.
   * @return {@code this} sprite for chaining.
   */
  public FlixelSprite loadSparrowFrames(Texture texture, XmlReader.Element xmlFile) {
    atlasRegions = new Array<>(AtlasRegion[]::new);

    for (XmlReader.Element subTexture : xmlFile.getChildrenByName("SubTexture")) {
      String name = subTexture.getAttribute("name");
      int x = subTexture.getInt("x");
      int y = subTexture.getInt("y");
      int width = subTexture.getInt("width");
      int height = subTexture.getInt("height");

      this.texture = texture;
      TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion(texture, x, y, width, height);
      region.name = name;

      if (subTexture.hasAttribute("frameX")) {
        region.offsetX = Math.abs(subTexture.getInt("frameX"));
        region.offsetY = Math.abs(subTexture.getInt("frameY"));
        region.originalWidth = subTexture.getInt("frameWidth");
        region.originalHeight = subTexture.getInt("frameHeight");
      } else {
        region.offsetX = 0;
        region.offsetY = 0;
        region.originalWidth = width;
        region.originalHeight = height;
      }

      atlasRegions.add(region);
    }

    if (atlasRegions.size > 0) {
      currentFrame = atlasRegions.first();
      currentRegion = currentFrame;
      setSize(currentFrame.getRegionWidth(), currentFrame.getRegionHeight());
    }

    currentRegion = atlasRegions.first();
    setSize(currentRegion.getRegionWidth(), currentRegion.getRegionHeight());
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
    Array<TextureAtlas.AtlasRegion> frames = new Array<>();

    for (TextureAtlas.AtlasRegion region : atlasRegions) {
      if (region.name.startsWith(prefix)) {
        frames.add(region);
      }
    }

    if (frames.size > 0) {
      // Ensure frames are sorted alphabetically (e.g., confirm0000, confirm0001).
      frames.sort(Comparator.comparing(o -> o.name));

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
    Array<TextureRegion> animFrames = new Array<>();

    // Convert 1D indices (0, 1, 2...) to 2D grid coordinates.
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
        currentRegion,
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

  @Override
  public void destroy() {
    reset();
  }

  public boolean isAnimationFinished() {
    Animation<TextureRegion> anim = animations.get(currentAnim);
    if (anim == null) return true;
    return anim.isAnimationFinished(stateTime);
  }

  @Override
  public void reset() {
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
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    if (currentFrame != null) {
      currentFrame.getTexture().dispose();
      currentFrame = null;
    }
    currentRegion = null;
    if (atlasRegions != null) {
      for (int i = atlasRegions.size - 1; i >= 0; i--) {
        var region = atlasRegions.items[i];
        if (region != null) {
          region.getTexture().dispose();
        }
      }
      atlasRegions.setSize(0);
      atlasRegions = null;
    }
    if (frames != null) {
      for (int i = frames.length - 1; i >= 0; i--) {
        var frame = frames[i];
        if (frame != null) {
          for (TextureRegion region : frame) {
            if (region != null) {
              region.getTexture().dispose();
            }
          }
        }
      }
      frames = null;
    }
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
    if (texture != null) {
      texture.setFilter(
        antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest,
        antialiasing ? Texture.TextureFilter.Linear : Texture.TextureFilter.Nearest
      );
    }
  }

  public Texture getGraphic() {
    return texture;
  }

  public void setGraphic(Texture texture) {
    this.texture = texture;
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
    currentRegion = region;
  }

  public TextureRegion getRegion() {
    return currentRegion;
  }

  public int getRegionWidth() {
    return currentRegion != null ? currentRegion.getRegionWidth() : 0;
  }

  public int getRegionHeight() {
    return currentRegion != null ? currentRegion.getRegionHeight() : 0;
  }

  public ObjectMap<String, Animation<TextureRegion>> getAnimations() {
    return animations;
  }

  public Array<TextureAtlas.AtlasRegion> getAtlasRegions() {
    return atlasRegions;
  }

  public FlixelCamera[] getCameras() {
    return cameras;
  }

  public TextureAtlas.AtlasRegion getCurrentFrame() {
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

  public TextureRegion[][] getFrames() {
    return frames;
  }
}
