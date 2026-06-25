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

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.Comparator;

/**
 * Playback state and clip registration for {@link FlixelSprite} animations. Obtain a controller with
 * {@link FlixelSprite#ensureAnimation()} (or assign one directly), then call {@code sprite.ensureAnimation().loadSparrowFrames(...)},
 * {@code .playAnimation(...)}, etc. Decouples animation timing from rendering and physics.
 *
 * <p>Optionally link a {@link FlixelAnimationStateMachine} via {@link #setStateMachine} so the
 * machine is ticked automatically inside {@link #update(float)} - no separate machine update call
 * needed in your game loop.
 */
public class FlixelAnimationController implements FlixelUpdatable {

  /** The sprite that owns {@code this} animation controller. */
  @NotNull
  private final FlixelSprite owner;

  /** A map of animation names to their respective {@link Animation} instances. */
  @NotNull
  private final ObjectMap<String, Animation<FlixelFrame>> animations = new ObjectMap<>();

  /**
   * Per-animation pixel offsets applied to the owner when a clip starts.
   *
   * <p>Each entry is a {@code {x, y}} pair. Empty until {@link #addOffset(String, float, float)} is used,
   * which keeps the feature opt-in and out of the way of manual {@link FlixelSprite#setOffset} calls.
   */
  @NotNull
  private final ObjectMap<String, float[]> animationOffsets = new ObjectMap<>();

  /**
   * Optional state machine to update automatically alongside this controller. When non-null,
   * {@link #update(float)} ticks the machine so callers only need to update the sprite.
   */
  @Nullable
  private FlixelAnimationStateMachine stateMachine;

  /** The current state time of the animation. */
  private float stateTime;

  /** The current animation name. */
  @NotNull
  private String currentAnim = "";

  /** +1 forward, -1 reverse (affects how {@link #stateTime} advances). */
  private int playDirection = 1;

  /** The last dispatched frame index. */
  private int lastDispatchedFrameIndex = -1;

  /** Dispatched when the visible keyframe index changes. */
  @NotNull
  public final FlixelSignal<FlixelAnimationFrameSignalData> onFrameChanged = new FlixelSignal<>();

  /** Dispatched once when a non-looping clip reaches its end, or when looping is turned off at end. */
  @NotNull
  public final FlixelSignal<FlixelAnimationFrameSignalData> onAnimationFinished = new FlixelSignal<>();

  /** The current frame signal data to prevent allocation of a new object every time. */
  private final FlixelAnimationFrameSignalData currentFrameSignalData =
      new FlixelAnimationFrameSignalData("", -1, null);

  /** Whether the animation should loop. */
  private boolean looping = true;

  /** Whether the animation is paused. */
  private boolean paused;

  /** Whether the last animation was finished. */
  private boolean lastFinished = true;

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
   * Loads a Sparrow atlas from a single base path, inferring the conventional {@code .png} and
   * {@code .xml} file names shared by a Sparrow export. For example, passing
   * {@code "shared/images/foo"} loads the texture from
   * {@code "shared/images/foo.png"} and the atlas data from
   * {@code "shared/images/foo.xml"}.
   *
   * <pre>{@code
   * sprite.ensureAnimation().loadSparrowFrames("shared/images/foo");
   * }</pre>
   *
   * @param path The base path, without a file extension, shared by the PNG and XML pair.
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSparrowFrames(@NotNull String path) {
    return loadSparrowFrames(path + ".png", path + ".xml");
  }

  /**
   * Overload of {@link #loadSparrowFrames(String)} that accepts the base path as a {@link FileHandle}
   * instead of a path string.
   *
   * @param path The base path handle, without a file extension, shared by the PNG and XML pair.
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSparrowFrames(@NotNull FileHandle path) {
    return loadSparrowFrames(path.path());
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
    String text = FlixelSpritemapJsonLoader.readUtf8Text(xml);
    return loadSparrowFrames(textureKey, new XmlReader().parse(new StringReader(text)));
  }

  /**
   * @param textureKey Asset key of the {@link FlixelGraphic}.
   * @param xmlFile Sparrow XML file, read as UTF-8 (optional BOM stripped)
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSparrowFrames(@NotNull String textureKey, @NotNull FileHandle xmlFile) {
    String text = FlixelSpritemapJsonLoader.readUtf8Text(xmlFile);
    return loadSparrowFrames(textureKey, new XmlReader().parse(new StringReader(text)));
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

    Array<FlixelFrame> atlasFrames = parseSparrowFrames(texture, xmlRoot);
    owner.applySparrowAtlas(g, atlasFrames);
    return owner;
  }

  /**
   * Merges a Sparrow XML atlas onto the owning sprite from a single base path, inferring the
   * conventional {@code .png} and {@code .xml} file names shared by a Sparrow export.
   *
   * <p>Unlike {@link #loadSparrowFrames(String)}, which replaces the sprite's atlas and clears its
   * clips, this <em>appends</em> the sheet's frames to whatever atlas the sprite already has, so one
   * sprite can carry frames from several sheets. It works on any {@link FlixelSprite}, not just an
   * Adobe Animate rig sprite: a plain sprite can mix two Sparrow sheets, and a
   * {@link FlixelAnimateSprite} can carry Sparrow clips alongside its baked rig. Register the merged
   * clips the usual way after this call, then play one to show the merged art.
   *
   * <pre>{@code
   * FlixelSprite sprite = new FlixelSprite();
   * sprite.ensureAnimation().addSparrowAtlas("shared/images/characters/Pico_Censored");
   * sprite.animation.addAnimationByPrefix("swearLeft", "pico swear left", 24, false);
   * sprite.animation.playAnimation("swearLeft");
   * }</pre>
   *
   * @param path The base path, without a file extension, shared by the PNG and XML pair.
   * @return The owning sprite for chaining.
   * @see #addSparrowAtlas(String, String)
   */
  @NotNull
  public FlixelSprite addSparrowAtlas(@NotNull String path) {
    return addSparrowAtlas(path + ".png", path + ".xml");
  }

  /**
   * Overload of {@link #addSparrowAtlas(String)} that accepts the base path as a {@link FileHandle}.
   *
   * @param path The base path handle, without a file extension, shared by the PNG and XML pair.
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite addSparrowAtlas(@NotNull FileHandle path) {
    return addSparrowAtlas(path.path());
  }

  /**
   * Merges a Sparrow XML atlas onto the owning sprite from an explicit texture key and XML path. See
   * {@link #addSparrowAtlas(String)} for the typical base-path form and the merge contract.
   *
   * @param textureKey The asset key of the Sparrow PNG. Must not be {@code null}.
   * @param xmlPath The path to the Sparrow XML. Must not be {@code null}.
   * @return The owning sprite for chaining.
   * @throws IllegalArgumentException If either file is missing or malformed.
   */
  @NotNull
  public FlixelSprite addSparrowAtlas(@NotNull String textureKey, @NotNull String xmlPath) {
    FileHandle xml = FlixelSpritemapJsonLoader.resolveAssetPath(xmlPath);
    String text = FlixelSpritemapJsonLoader.readUtf8Text(xml);
    XmlReader.Element xmlRoot = new XmlReader().parse(new StringReader(text));

    FlixelGraphic g = Flixel.ensureAssets().obtainWrapper(textureKey, FlixelGraphic.class);
    Texture texture;
    try {
      texture = g.requireTexture();
    } catch (IllegalStateException notLoaded) {
      texture = g.loadNow();
    }

    Array<FlixelFrame> parsed = parseSparrowFrames(texture, xmlRoot);
    owner.mergeSparrowAtlas(g, parsed);
    return owner;
  }

  /**
   * Parses Sparrow {@code SubTexture} entries into a fresh frame list without installing them on any
   * sprite.
   *
   * <p>This is the shared parsing core behind {@link #loadSparrowFrames(String, XmlReader.Element)}.
   * Keeping it separate lets callers that need to <em>merge</em> a Sparrow sheet into an existing
   * atlas (for example {@link #addSparrowAtlas(String)}) reuse the exact same region math without
   * going through {@link FlixelSprite#applySparrowAtlas}, which replaces the sprite's atlas and
   * clears its clips.
   *
   * @param texture The backing texture the regions are cut from.
   * @param xmlRoot The root {@code TextureAtlas} element of a Sparrow XML.
   * @return A newly allocated list of frames, one per valid {@code SubTexture}.
   */
  @NotNull
  public static Array<FlixelFrame> parseSparrowFrames(
      @NotNull Texture texture, @NotNull XmlReader.Element xmlRoot) {
    int texW = texture.getWidth();
    int texH = texture.getHeight();
    Array<FlixelFrame> atlasFrames = new Array<>(FlixelFrame[]::new);

    Array<XmlReader.Element> subTextures = xmlRoot.getChildrenByName("SubTexture");
    for (int i = 0; i < subTextures.size; i++) {
      XmlReader.Element subTexture = subTextures.get(i);

      // Sparrow always supplies x/y/width/height, but a hand-edited or truncated atlas might not.
      // Defaulting to 0 keeps a single broken entry from throwing and aborting the whole load.
      int x = subTexture.getInt("x", 0);
      int y = subTexture.getInt("y", 0);
      int width = subTexture.getInt("width", 0);
      int height = subTexture.getInt("height", 0);

      // A zero-area or off-texture region cannot be rendered; drop it rather than emit garbage UVs.
      if (width <= 0 || height <= 0 || x < 0 || y < 0 || x >= texW || y >= texH) {
        continue;
      }

      // Clamp regions that spill past the texture edge so a slightly oversized rectangle still draws.
      if (x + width > texW) {
        width = texW - x;
      }
      if (y + height > texH) {
        height = texH - y;
      }

      TextureRegion region = new TextureRegion(texture, x, y, width, height);
      FlixelFrame frame = new FlixelFrame(region);

      String name = subTexture.getAttribute("name", null);
      // Prefix animations look frames up by name, so a missing name gets a stable synthetic one
      // instead of a null that would break sorting and lookups later.
      frame.name = (name != null && !name.isEmpty()) ? name : "frame" + i;

      if (subTexture.hasAttribute("frameX") || subTexture.hasAttribute("frameY")) {
        // Sparrow stores frameX/frameY as negative offsets; negate them to get the trim offset
        // measured from the source frame's top-left corner.
        frame.offsetX = -subTexture.getInt("frameX", 0);
        frame.offsetY = -subTexture.getInt("frameY", 0);
        int sourceWidth = subTexture.getInt("frameWidth", width);
        int sourceHeight = subTexture.getInt("frameHeight", height);
        // The source box can never be smaller than the trimmed region it contains; guard against
        // malformed atlases that would otherwise place art outside its own frame.
        frame.originalWidth = Math.max(sourceWidth, width);
        frame.originalHeight = Math.max(sourceHeight, height);
      } else {
        frame.offsetX = 0;
        frame.offsetY = 0;
        frame.originalWidth = width;
        frame.originalHeight = height;
      }

      atlasFrames.add(frame);
    }

    return atlasFrames;
  }

  /** Clears all clips, per-animation offsets, and resets playback state. */
  public void clear() {
    animations.clear();
    animationOffsets.clear();
    stateTime = 0f;
    currentAnim = "";
    looping = true;
    paused = false;
    playDirection = 1;
    lastDispatchedFrameIndex = -1;
    lastFinished = true;
  }

  /**
   * Registers a per-animation pixel offset, applied to the owning sprite's
   * {@link FlixelSprite#setOffset(float, float) offset} every time that clip starts.
   *
   * <p>A Sparrow atlas keeps each frame planted within its own untrimmed source box, but different
   * clips are usually authored on differently sized canvases, so their anchors do not line up when
   * you switch between them. Nudge each clip by a hand-tuned amount so they all share a common ground
   * line. The offset is <em>subtracted</em> from the draw position, so positive {@code x} moves the
   * graphic left and positive {@code y} moves it up (matching {@link FlixelSprite#setOffset}).
   *
   * <p>The feature is opt-in. Until the first {@code addOffset} call the controller never touches the
   * sprite's offset; afterwards it owns it, and playing a clip with no registered offset resets the
   * offset to {@code (0, 0)}. Avoid mixing manual {@link FlixelSprite#setOffset} with this API.
   *
   * <pre>{@code
   * var anim = sprite.ensureAnimation();
   * anim.addAnimationByPrefix("idle", "BF idle dance", 24, true);
   * anim.addAnimationByPrefix("singLEFT", "BF NOTE LEFT", 24, false);
   * anim.addOffset("idle", 0, 0);
   * anim.addOffset("singLEFT", 12, -6);
   * anim.playAnimation("idle"); // offset snaps to (0, 0)
   * }</pre>
   *
   * @param name The animation name, as registered with one of the {@code addAnimation*} methods.
   * @param x Horizontal offset in pixels (positive moves the graphic left).
   * @param y Vertical offset in pixels (positive moves the graphic up).
   */
  public void addOffset(@NotNull String name, float x, float y) {
    float[] offset = animationOffsets.get(name);
    if (offset == null) {
      animationOffsets.put(name, new float[] { x, y });
    } else {
      offset[0] = x;
      offset[1] = y;
    }
    // If the offset for the clip currently playing changed, reflect it right away.
    if (currentAnim.equals(name)) {
      owner.setOffset(x, y);
    }
  }

  /**
   * Removes a previously registered per-animation offset.
   *
   * @param name The animation name to clear the offset for.
   */
  public void removeOffset(@NotNull String name) {
    animationOffsets.remove(name);
  }

  /** Removes every registered per-animation offset without affecting the registered clips. */
  public void clearOffsets() {
    animationOffsets.clear();
  }

  /**
   * @param name The animation name to check.
   * @return {@code true} if a per-animation offset is registered for {@code name}.
   */
  public boolean hasOffset(@NotNull String name) {
    return animationOffsets.containsKey(name);
  }

  /** Equivalent to {@link #centerOffsets(boolean)} with {@code adjustPosition} set to {@code false}. */
  public void centerOffsets() {
    centerOffsets(false);
  }

  /**
   * Centers the owning sprite's hitbox within its current frame's untrimmed source box.
   *
   * <p>This is useful when a trimmed Sparrow frame draws smaller than the hitbox set by
   * {@link FlixelSprite#updateHitbox(float, float)} (or vice versa), since the art would otherwise sit
   * off-center inside the box.
   *
   * <p>This is a one-shot computation from the currently displayed {@link FlixelFrame}, independent
   * of the per-animation {@link #addOffset(String, float, float) registry}; it does not read or write
   * {@link #animationOffsets}. Calling it after a clip with a registered offset will overwrite that
   * offset until the next {@link #playAnimation} call reapplies it.
   *
   * @param adjustPosition Whether to also shift the sprite's position so its origin stays anchored
   *     to the frame's center, matching HaxeFlixel's optional position correction.
   */
  public void centerOffsets(boolean adjustPosition) {
    FlixelFrame frame = owner.getCurrentFrame();
    if (frame == null) {
      return;
    }
    owner.setOffset(
        centeredOffset(frame.originalWidth, owner.getWidth()),
        centeredOffset(frame.originalHeight, owner.getHeight()));
    if (adjustPosition) {
      owner.setPosition(
          owner.getX() + owner.getOriginX() - frame.originalWidth * 0.5f,
          owner.getY() + owner.getOriginY() - frame.originalHeight * 0.5f);
    }
  }

  /**
   * Pure offset math for {@link #centerOffsets(boolean)}, split out so it can be unit tested without
   * a GPU-backed {@link FlixelFrame}.
   *
   * @param sourceSize The frame's untrimmed source size on this axis ({@code originalWidth} or
   *     {@code originalHeight}).
   * @param hitboxSize The sprite's hitbox size on this axis ({@link FlixelSprite#getWidth()} or
   *     {@link FlixelSprite#getHeight()}).
   * @return The offset that centers {@code hitboxSize} within {@code sourceSize}.
   */
  static float centeredOffset(float sourceSize, float hitboxSize) {
    return (sourceSize - hitboxSize) * 0.5f;
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
    Animation<FlixelFrame> anim = animations.get(name);
    if (anim == null) {
      // Unknown clip name: leave whatever is currently displayed untouched instead of blanking out.
      return;
    }
    // Keep playing only when this exact clip is already mid-play and the caller did not force a
    // restart. A clip that has finished (or a different clip) always (re)starts, so a non-looping
    // clip can be replayed once it ends and switching clips never silently no-ops.
    if (!shouldRestart(forceRestart, currentAnim.equals(name), isAnimationFinished())) {
      return;
    }

    currentAnim = name;
    looping = loop;
    stateTime = 0f;
    playDirection = 1;
    lastDispatchedFrameIndex = -1;
    lastFinished = false;

    // Show this clip's first keyframe immediately so a freshly played animation does not flash the
    // previous frame for a tick, and size the sprite to the clip's source frame.
    Object[] keyframes = anim.getKeyFrames();
    if (keyframes.length > 0) {
      owner.setCurrentFrameForAnimation((FlixelFrame) keyframes[0]);
      // Sparrow/atlas characters: snap the hitbox to this clip's untrimmed frame so the debug box
      // and collision bounds frame whichever animation is playing. Grid-frame sprites keep their
      // hitbox untouched, since it may be a deliberately customized collision box.
      if (owner.getAtlasRegions() != null) {
        owner.updateHitbox();
      }
    }
    applyAnimationOffset(name);
  }

  /**
   * Pure decision for whether a {@code playAnimation} call should (re)start the clip, split out so it
   * can be unit tested. A clip restarts when the caller forces it, when it is a different clip than
   * the one playing, or when the current clip has already finished; only a still-playing same clip
   * without a force is left alone.
   *
   * @param forceRestart Whether the caller forced a restart.
   * @param sameClip Whether the requested clip is the one already current.
   * @param finished Whether the current clip has finished.
   * @return {@code true} if playback should (re)start from frame zero.
   */
  static boolean shouldRestart(boolean forceRestart, boolean sameClip, boolean finished) {
    return forceRestart || !sameClip || finished;
  }

  /**
   * Applies the registered {@link #addOffset(String, float, float) per-animation offset} for
   * {@code name} to the owning sprite. Does nothing when no offsets are registered, so sprites that
   * never opt into the feature keep whatever {@link FlixelSprite#setOffset} value they were given.
   * Once at least one offset exists, playing a clip with no registered offset resets the sprite back
   * to {@code (0, 0)} so a previous clip's nudge does not leak forward.
   *
   * @param name The animation that is starting.
   */
  private void applyAnimationOffset(@NotNull String name) {
    if (animationOffsets.size == 0) {
      return;
    }
    float[] offset = animationOffsets.get(name);
    if (offset != null) {
      owner.setOffset(offset[0], offset[1]);
    } else {
      owner.setOffset(0f, 0f);
    }
  }

  public boolean isAnimationFinished() {
    Animation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) {
      return true;
    }
    float duration = anim.getAnimationDuration();
    return !looping && duration > 0f && stateTime >= duration;
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

    int frameIndex = computeKeyframeIndex(anim);
    // Pick the displayed frame by the same index the controller reports elsewhere, rather than
    // anim.getKeyFrame(stateTime, looping). That libGDX call honors the clip's REGISTERED PlayMode,
    // so a clip registered to loop but played non-looping wraps back to frame 0 when it finishes;
    // indexing the keyframes directly keeps a finished non-looping clip parked on its last frame.
    // Typed FlixelFrame[] but may be an Object[] at runtime, so cast the element, not the array.
    Object[] keyframes = anim.getKeyFrames();
    if (keyframes.length == 0) {
      return;
    }
    FlixelFrame frame = (FlixelFrame) keyframes[frameIndex];
    owner.setCurrentFrameForAnimation(frame);

    if (frameIndex != lastDispatchedFrameIndex) {
      lastDispatchedFrameIndex = frameIndex;
      currentFrameSignalData.setAnimationName(currentAnim);
      currentFrameSignalData.setFrameIndex(frameIndex);
      currentFrameSignalData.setFrame(frame);
      onFrameChanged.dispatch(currentFrameSignalData);
    }

    boolean finished = !looping && duration > 0f && stateTime >= duration;
    if (finished && !lastFinished) {
      FlixelAnimationFrameSignalData data = new FlixelAnimationFrameSignalData(currentAnim, frameIndex, frame);
      onAnimationFinished.dispatch(data);
    }
    lastFinished = finished;

    if (stateMachine != null) {
      stateMachine.update(elapsed);
    }
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

  /**
   * Zero-based key index in the current clip. Computed from {@link #getStateTime()} and the
   * controller's {@link #isLooping() looping} flag rather than from the underlying libGDX
   * {@link Animation}'s {@code PlayMode}, so the index always matches what {@link #update(float)}
   * actually displays no matter how the clip was registered.
   *
   * <p>For non-looping playback that has already finished, this returns the <strong>last</strong>
   * keyframe index ({@code keyframeCount - 1}). For looping playback, it returns the wrapped index
   * inside {@code [0, keyframeCount)}. Useful for multi-part BTA/texture-atlas characters driven
   * outside a single-frame texture (for example {@link FlixelAnimateSprite}).
   *
   * @return The current keyframe index. Always {@code >= 0}.
   */
  public int getCurrentKeyframeIndex() {
    if (currentAnim.isEmpty()) {
      return 0;
    }
    Animation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) {
      return 0;
    }
    return computeKeyframeIndex(anim);
  }

  /**
   * Returns the keyframe index for {@code anim} at the current {@link #stateTime}, honouring the
   * controller's runtime {@link #looping} flag. This deliberately bypasses
   * {@link Animation#getKeyFrameIndex(float)} (which uses the {@link Animation#getPlayMode()
   * registered PlayMode}) so that:
   * <ul>
   *   <li>Non-looping playback at the end of the clip returns the <strong>last</strong> keyframe
   *   instead of wrapping back to the first (the latter is what libGDX's {@link Animation.PlayMode#LOOP
   *   LOOP} does when {@code stateTime == duration}, and is the source of the "first frame appears at
   *   the end" bug for clips registered with {@code loop = true} but played with looping disabled).</li>
   *   <li>Looping playback always returns an in-range index, even right at the wrap point.</li>
   * </ul>
   *
   * @param anim The clip to read the keyframe index from. Must not be {@code null}.
   * @return A keyframe index in {@code [0, keyframeCount - 1]}, or {@code 0} for degenerate clips.
   */
  private int computeKeyframeIndex(@NotNull Animation<FlixelFrame> anim) {
    Object[] keyframes = anim.getKeyFrames();
    return keyframeIndex(stateTime, anim.getFrameDuration(), keyframes.length, looping);
  }

  /**
   * Pure keyframe-index math, split out from {@link #computeKeyframeIndex} so it can be unit tested
   * without a GPU texture. Non-looping playback past the end returns the <strong>last</strong> index
   * ({@code total - 1}), never wrapping back to {@code 0}; looping playback wraps into range.
   *
   * @param stateTime Elapsed playback time in seconds.
   * @param frameDuration Seconds per keyframe.
   * @param total Number of keyframes in the clip.
   * @param looping Whether playback wraps.
   * @return A keyframe index in {@code [0, total - 1]}, or {@code 0} for degenerate clips.
   */
  static int keyframeIndex(float stateTime, float frameDuration, int total, boolean looping) {
    if (total <= 0 || frameDuration <= 0f) {
      return 0;
    }
    int idx = (int) (stateTime / frameDuration);
    if (looping) {
      idx %= total;
      if (idx < 0) {
        idx += total;
      }
      return idx;
    }
    if (idx < 0) {
      return 0;
    }
    if (idx >= total) {
      return total - 1;
    }
    return idx;
  }

  public boolean isLooping() {
    return looping;
  }

  /**
   * @return The linked {@link FlixelAnimationStateMachine}, or {@code null} if none is set.
   */
  @Nullable
  public FlixelAnimationStateMachine getStateMachine() {
    return stateMachine;
  }

  /**
   * Links a {@link FlixelAnimationStateMachine} to this controller so it is ticked automatically by
   * {@link #update(float)}. Pass {@code null} to detach a previously linked machine.
   *
   * <p>This is the recommended way to drive an FSM: link it once and update only the sprite each
   * frame. The machine still works independently if you prefer to call its own
   * {@link FlixelAnimationStateMachine#update(float)} by hand - just do not link it here.
   *
   * <pre>{@code
   * var fsm = new FlixelAnimationStateMachine(player);
   * player.ensureAnimation().setStateMachine(fsm);
   * // From now on player.update(elapsed) advances both the controller and the FSM.
   * }</pre>
   *
   * @param stateMachine The machine to auto-tick, or {@code null} to clear the link.
   */
  public void setStateMachine(@Nullable FlixelAnimationStateMachine stateMachine) {
    this.stateMachine = stateMachine;
  }
}
