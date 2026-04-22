/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.FlixelUpdatable;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;
import me.stringdotjar.flixelgdx.util.signal.FlixelSignal;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Playback state and clip registration for {@link FlixelSprite} animations. Obtain a controller with
 * {@link FlixelSprite#ensureAnimation()} (or assign one directly), then call {@code sprite.ensureAnimation().loadSparrowFrames(...)},
 * {@code .playAnimation(...)}, etc. Decouples animation timing from rendering and physics.
 */
public class FlixelAnimationController implements FlixelUpdatable {

  /** The sprite that owns {@code this} animation controller. */
  @NotNull
  private final FlixelSprite owner;

  /** A map of animation names to their respective {@link Animation} instances. */
  @NotNull
  private final ObjectMap<String, Animation<FlixelFrame>> animations = new ObjectMap<>();

  /** The current state time of the animation. */
  private float stateTime;

  /** The current animation name. */
  @NotNull
  private String currentAnim = "";

  /** Whether the animation should loop. */
  private boolean looping = true;

  /** Whether the animation is paused. */
  private boolean paused;

  /** +1 forward, -1 reverse (affects how {@link #stateTime} advances). */
  private int playDirection = 1;

  /** The last dispatched frame index. */
  private int lastDispatchedFrameIndex = -1;

  /** Whether the last animation was finished. */
  private boolean lastFinished = true;

  /** Dispatched when the visible keyframe index changes. */
  @NotNull
  public final FlixelSignal<FlixelAnimationFrameSignalData> onFrameChanged = new FlixelSignal<>();

  /** Dispatched once when a non-looping clip reaches its end, or when looping is turned off at end. */
  @NotNull
  public final FlixelSignal<FlixelAnimationFrameSignalData> onAnimationFinished = new FlixelSignal<>();

  /** The current frame signal data to prevent allocation of a new object every time. */
  private FlixelAnimationFrameSignalData currentFrameSignalData = new FlixelAnimationFrameSignalData("", -1, null);

  /**
   * Creates a new animation controller for the given sprite.
   *
   * @param owner The sprite that owns {@code this} animation controller.
   */
  public FlixelAnimationController(@NotNull FlixelSprite owner) {
    this.owner = owner;
  }

  /**
   * The sprite that owns this controller. Package use includes spritemap loading helpers.
   */
  @NotNull
  public FlixelSprite getOwner() {
    return owner;
  }

  /**
   * Adobe/CreateJS spritemap plus animation index JSON. See {@link FlixelSpritemapJsonLoader#load} for file shapes.
   *
   * @param textureKey Asset key of the already-enqueued {@link FlixelGraphic}.
   * @param spritemapJsonPath Path resolved like other assets (internal or classpath).
   * @param animationJsonPath JSON with an {@code animations} object.
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSpritemapFromJson(
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    FlixelSpritemapJsonLoader.load(this, textureKey, spritemapJsonPath, animationJsonPath);
    return owner;
  }

  /**
   * Loads Sparrow XML from a path string (UTF-8). Tries {@code Gdx.files.internal} then {@code classpath}.
   *
   * @param textureKey Asset key of the {@link FlixelGraphic}.
   * @param xmlPath Path to Sparrow XML (e.g. {@code "data/hero.xml"}).
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSparrowFrames(@NotNull String textureKey, @NotNull String xmlPath) {
    FileHandle xml = FlixelSpritemapJsonLoader.resolveAssetPath(xmlPath);
    return loadSparrowFrames(textureKey, new XmlReader().parse(xml.reader("UTF-8")));
  }

  /**
   * @param textureKey Asset key of the {@link FlixelGraphic}.
   * @param xmlFile Sparrow XML file, read as UTF-8
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSparrowFrames(@NotNull String textureKey, @NotNull FileHandle xmlFile) {
    return loadSparrowFrames(textureKey, new XmlReader().parse(xmlFile.reader("UTF-8")));
  }

  /**
   * Parses Sparrow {@code SubTexture} entries and installs the result on the owner.
   *
   * @param textureKey Asset key of the {@link FlixelGraphic}.
   * @param xmlRoot Root XML element (e.g. from {@link XmlReader#parse(FileHandle)}).
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSparrowFrames(@NotNull String textureKey, @NotNull XmlReader.Element xmlRoot) {
    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(textureKey, FlixelGraphic.class);
    Texture texture;
    try {
      texture = g.requireTexture();
    } catch (IllegalStateException e) {
      texture = g.loadNow();
    }

    Array<FlixelFrame> atlasFrames = new Array<>(FlixelFrame[]::new);

    for (XmlReader.Element subTexture : xmlRoot.getChildrenByName("SubTexture")) {
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

    owner.applySparrowAtlas(g, atlasFrames);
    return owner;
  }

  /** Clears all clips and resets playback state. */
  public void clear() {
    animations.clear();
    stateTime = 0f;
    currentAnim = "";
    looping = true;
    paused = false;
    playDirection = 1;
    lastDispatchedFrameIndex = -1;
    lastFinished = true;
  }

  public void pause() {
    paused = true;
  }

  public void resume() {
    paused = false;
  }

  public boolean isPaused() {
    return paused;
  }

  /** Toggles playback direction. Time still advances by {@link #update(float)} while not paused. */
  public void reverse() {
    playDirection = -playDirection;
  }

  public float getPlayDirection() {
    return playDirection;
  }

  public void setPlayDirection(int playDirection) {
    playDirection = MathUtils.clamp(playDirection, -1, 1);
    this.playDirection = playDirection >= 0 ? 1 : -1;
  }

  /**
   * Adds an animation to the controller by prefixing the frame names.
   *
   * @param name The name of the animation.
   * @param prefix The prefix of the frame names.
   * @param frameRate The frame rate of the animation.
   * @param loop Whether the animation should loop.
   */
  public void addAnimationByPrefix(
    @NotNull String name,
    @NotNull String prefix,
    int frameRate,
    boolean loop) {
    Array<FlixelFrame> atlas = owner.getAtlasRegions();
    if (atlas == null) {
      return;
    }
    Array<FlixelFrame> clipFrames = new Array<>();
    for (FlixelFrame frame : atlas) {
      if (frame != null && frame.name != null && frame.name.startsWith(prefix)) {
        clipFrames.add(frame);
      }
    }
    if (clipFrames.size == 0) {
      return;
    }
    clipFrames.sort(Comparator.comparing(f -> f.name));
    animations.put(
      name,
      new Animation<>(
        1f / frameRate,
        clipFrames,
        loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL));
  }

  /**
   * Adds a clip from the current atlas list {@link FlixelSprite#getAtlasRegions()} using zero-based indices into
   * that list. Used by spritemap JSON and by games that know frame order after a Sparrow or spritemap load.
   *
   * @param name Clip name for {@link #playAnimation(String)}.
   * @param atlasFrameIndices Indices into the atlas list (out-of-range entries are skipped).
   * @param frameDuration libGDX frame duration in seconds (reciprocal of FPS).
   * @param loop Whether the clip loops.
   */
  public void addAnimationFromAtlas(
    @NotNull String name, @NotNull int[] atlasFrameIndices, float frameDuration, boolean loop) {
    Array<FlixelFrame> atlas = owner.getAtlasRegions();
    if (atlas == null || atlas.size == 0) {
      return;
    }
    Array<FlixelFrame> clipFrames = new Array<>();
    for (int i : atlasFrameIndices) {
      if (i >= 0 && i < atlas.size) {
        clipFrames.add(atlas.get(i));
      }
    }
    if (clipFrames.size == 0) {
      return;
    }
    animations.put(
      name,
      new Animation<>(
        frameDuration,
        clipFrames,
        loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL));
  }

  /**
   * Adds an animation to the controller by specifying the frame indices.
   *
   * @param name The name of the animation.
   * @param frameIndices The indices of the frames to add.
   * @param frameDuration The duration of each frame.
   */
  public void addAnimation(@NotNull String name, int[] frameIndices, float frameDuration) {
    FlixelFrame[][] grid = owner.getFrames();
    if (grid == null || grid.length == 0 || grid[0] == null) {
      return;
    }

    // Get the number of columns in the grid.
    int cols = grid[0].length;
    Array<FlixelFrame> animFrames = new Array<>();
    for (int index : frameIndices) {
      int row = index / cols;
      int col = index % cols;
      animFrames.add(grid[row][col]);
    }
    animations.put(name, new Animation<>(frameDuration, animFrames));
  }

  /**
   * Plays the animation with the given name. Loops and restarts the animation by default.
   *
   * @param name The name of the animation to play.
   */
  public void playAnimation(@NotNull String name) {
    playAnimation(name, true);
  }

  /**
   * Plays the animation with the given name. Restarts the animation by default.
   *
   * @param name The name of the animation to play.
   * @param loop Whether the animation should loop.
   */
  public void playAnimation(@NotNull String name, boolean loop) {
    playAnimation(name, loop, true);
  }

  /**
   * Plays the animation with the given name.
   *
   * @param name The name of the animation to play.
   * @param loop Whether the animation should loop.
   * @param forceRestart Whether the animation should restart.
   */
  public void playAnimation(@NotNull String name, boolean loop, boolean forceRestart) {
    if (currentAnim.equals(name) && !forceRestart) {
      return;
    }
    if (isAnimationFinished() || forceRestart) {
      currentAnim = name;
      looping = loop;
      stateTime = 0f;
      playDirection = 1;
      lastDispatchedFrameIndex = -1;
      lastFinished = false;
    }
  }

  public boolean isAnimationFinished() {
    Animation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) {
      return true;
    }
    return anim.isAnimationFinished(stateTime);
  }

  @Override
  public void update(float elapsed) {
    if (animations.size == 0 || paused || currentAnim.isEmpty()) {
      return;
    }
    Animation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) {
      return;
    }
    float duration = anim.getAnimationDuration();
    stateTime += elapsed * playDirection;
    if (looping) {
      if (duration > 0f) {
        stateTime = stateTime % duration;
        if (stateTime < 0f) {
          stateTime += duration;
        }
      }
    } else if (duration > 0f) {
      stateTime = MathUtils.clamp(stateTime, 0f, duration);
    }

    FlixelFrame frame = anim.getKeyFrame(stateTime, looping);
    int frameIndex = anim.getKeyFrameIndex(stateTime);
    owner.setCurrentFrameForAnimation(frame);

    if (frameIndex != lastDispatchedFrameIndex) {
      lastDispatchedFrameIndex = frameIndex;
      currentFrameSignalData.setAnimationName(currentAnim);
      currentFrameSignalData.setFrameIndex(frameIndex);
      currentFrameSignalData.setFrame(frame);
      onFrameChanged.dispatch(currentFrameSignalData);
    }

    boolean finished = anim.isAnimationFinished(stateTime);
    if (finished && !lastFinished) {
      FlixelAnimationFrameSignalData data = new FlixelAnimationFrameSignalData(currentAnim, frameIndex, frame);
      onAnimationFinished.dispatch(data);
    }
    lastFinished = finished;
  }

  @NotNull
  public ObjectMap<String, Animation<FlixelFrame>> getAnimations() {
    return animations;
  }

  public float getStateTime() {
    return stateTime;
  }

  @NotNull
  public String getCurrentAnim() {
    return currentAnim;
  }

  public boolean isLooping() {
    return looping;
  }
}
