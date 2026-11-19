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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.signal.FlixelSignal;
import org.flixelgdx.xml.FlixelXml;
import org.flixelgdx.xml.FlixelXmlElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * Playback state and clip registration for {@link FlixelSprite} animations. Obtain a controller with
 * {@link FlixelSprite#ensureAnimation()} (or assign one directly), then call {@code sprite.ensureAnimation().addSparrowFrames(...)},
 * {@code .play(...)}, etc. Decouples animation timing from rendering and physics.
 *
 * <p>Optionally link a {@link FlixelAnimationStateMachine} via {@link #setStateMachine} so the
 * machine is ticked automatically inside {@link #update(float)} - no separate machine update call
 * would be needed in your game loop.
 *
 * <h2>Mixing in a Sparrow atlas</h2>
 * A character can also carry Sparrow XML clips on the same body via
 * {@link FlixelAnimationController#addSparrowFrames(FlixelFile)}, allowing a single sprite to contain
 * multiple Sparrow sheets with unique animations.
 */
public class FlixelAnimationController implements FlixelUpdatable {

  /** The sprite that owns {@code this} animation controller. */
  @NotNull
  private final FlixelSprite owner;

  /** A map of animation names to their respective {@link FlixelAnimation} instances. */
  @NotNull
  private final FlixelMap<String, FlixelAnimation<FlixelFrame>> animations = new FlixelMap<>();

  /**
   * Per-animation pixel offsets applied to the owner when a clip starts.
   *
   * <p>Each entry is a {@code {x, y}} pair. Empty until {@link #addOffset(String, float, float)} is used,
   * which keeps the feature opt-in and out of the way of manual {@link FlixelSprite#setOffset} calls.
   */
  @NotNull
  private final FlixelMap<String, float[]> animationOffsets = new FlixelMap<>();

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
   *
   * @return The sprite that created and owns this controller; never {@code null}.
   */
  @NotNull
  public FlixelSprite getOwner() {
    return owner;
  }

  /**
   * Adobe/CreateJS spritemap plus animation index JSON. See {@link FlixelSpritemapJsonLoader#load} for file shapes.
   *
   * @param textureFile Handle to the already-enqueued spritemap PNG.
   * @param spritemapJsonFile Handle to the spritemap JSON.
   * @param animationJsonFile Handle to the JSON with an {@code animations} object.
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite loadSpritemapFromJson(
      @NotNull FlixelFile textureFile,
      @NotNull FlixelFile spritemapJsonFile,
      @NotNull FlixelFile animationJsonFile) {
    FlixelSpritemapJsonLoader.load(
        this, textureFile.getPath(), spritemapJsonFile.getPath(), animationJsonFile.getPath());
    return owner;
  }

  /**
   * Loads or merges a Sparrow atlas onto the owning sprite from a base path handle, inferring the
   * conventional {@code .png} and {@code .xml} file names shared by a Sparrow export. For example,
   * passing a handle to {@code "shared/images/foo"} loads the texture from
   * {@code "shared/images/foo.png"} and the atlas data from {@code "shared/images/foo.xml"}.
   *
   * <p>When the sprite has no atlas yet, this installs the sheet as the primary atlas, sizes the
   * sprite to the first frame, and clears any previously registered clips. When the sprite already
   * has an atlas (either from a prior call or from an Adobe Animate rig), this appends the new
   * sheet's frames without disturbing the existing ones, so a single sprite can carry clips from
   * multiple sheets.
   *
   * <pre>{@code
   * FlixelFile base = Flixel.files.internal("shared/images/foo");
   * sprite.ensureAnimation().addSparrowFrames(base);
   * sprite.animation.addByPrefix("idle", "idle", 24, true);
   * sprite.animation.play("idle");
   * }</pre>
   *
   * @param path Handle to the base path, without a file extension, shared by the PNG and XML pair.
   * @return The owning sprite for chaining.
   * @see #addSparrowFrames(FlixelFile, FlixelFile)
   */
  @NotNull
  public FlixelSprite addSparrowFrames(@NotNull FlixelFile path) {
    String basePath = path.getPath();
    return addSparrowFrames(
        Flixel.files.internal(basePath + ".png"),
        FlixelSpritemapJsonLoader.resolveAssetPath(basePath + ".xml"));
  }

  /**
   * Loads or merges a Sparrow atlas onto the owning sprite from an explicit texture file and XML file.
   * See {@link #addSparrowFrames(FlixelFile)} for the full load/merge contract.
   *
   * @param textureFile Handle to the Sparrow PNG. Must not be {@code null}.
   * @param xmlFile Handle to the Sparrow XML file. Must not be {@code null}.
   * @return The owning sprite for chaining.
   * @throws IllegalArgumentException If either file is missing or malformed.
   */
  @NotNull
  public FlixelSprite addSparrowFrames(@NotNull FlixelFile textureFile, @NotNull FlixelFile xmlFile) {
    String text = FlixelSpritemapJsonLoader.readUtf8Text(xmlFile);
    return addSparrowFrames(textureFile, FlixelXml.parse(text));
  }

  /**
   * Overload of {@link #addSparrowFrames(FlixelFile, FlixelFile)} that accepts a pre-parsed XML root.
   *
   * @param textureFile Handle to the Sparrow PNG. Must not be {@code null}.
   * @param xmlRoot The root {@code TextureAtlas} element of a Sparrow XML. Must not be {@code null}.
   * @return The owning sprite for chaining.
   */
  @NotNull
  public FlixelSprite addSparrowFrames(@NotNull FlixelFile textureFile, @NotNull FlixelXmlElement xmlRoot) {
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(textureFile.getPath()).retain().get();
    FlixelTexture texture = g.getTexture();

    FlixelArray<FlixelFrame> parsed = parseSparrowFrames(texture, xmlRoot);
    FlixelArray<FlixelFrame> existing = owner.getAtlasRegions();
    if (existing == null || existing.isEmpty()) {
      owner.applySparrowAtlas(g, parsed);
    } else {
      owner.mergeSparrowAtlas(g, parsed);
    }
    return owner;
  }

  /**
   * Parses Sparrow {@code SubTexture} entries into a fresh frame list without installing them on any
   * sprite.
   *
   * <p>This is the shared parsing core used by {@link #addSparrowFrames(FlixelFile, FlixelXmlElement)}.
   * Keeping it separate lets callers inspect or transform the frame list before passing it to
   * {@link FlixelSprite#applySparrowAtlas} or {@link FlixelSprite#mergeSparrowAtlas}.
   *
   * @param texture The backing texture the regions are cut from.
   * @param xmlRoot The root {@code TextureAtlas} element of a Sparrow XML.
   * @return A newly allocated list of frames, one per valid {@code SubTexture}.
   */
  @NotNull
  public static FlixelArray<FlixelFrame> parseSparrowFrames(
      @NotNull FlixelTexture texture, @NotNull FlixelXmlElement xmlRoot) {
    int texW = texture.getWidth();
    int texH = texture.getHeight();
    FlixelArray<FlixelFrame> atlasFrames = new FlixelArray<>(FlixelFrame[]::new);

    FlixelArray<FlixelXmlElement> subTextures = xmlRoot.getChildrenByName("SubTexture");
    for (int i = 0; i < subTextures.getSize(); i++) {
      FlixelXmlElement subTexture = subTextures.get(i);

      // Sparrow always supplies x/y/width/height, but a hand-edited or truncated atlas might not.
      // Defaulting to 0 keeps a single broken entry from throwing and aborting the whole load.
      int x = subTexture.getIntAttribute("x", 0);
      int y = subTexture.getIntAttribute("y", 0);
      int width = subTexture.getIntAttribute("width", 0);
      int height = subTexture.getIntAttribute("height", 0);

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

      FlixelFrame frame = new FlixelFrame(texture, x, y, width, height);

      String name = subTexture.getAttribute("name", null);
      // Prefix animations look frames up by name, so a missing name gets a stable synthetic one
      // instead of a null that would break sorting and lookups later.
      frame.name = (name != null && !name.isEmpty()) ? name : "frame" + i;

      if (subTexture.hasAttribute("frameX") || subTexture.hasAttribute("frameY")) {
        // Sparrow stores frameX/frameY as negative offsets; negate them to get the trim offset
        // measured from the source frame's top-left corner.
        frame.offsetX = -subTexture.getIntAttribute("frameX", 0);
        frame.offsetY = -subTexture.getIntAttribute("frameY", 0);
        int sourceWidth = subTexture.getIntAttribute("frameWidth", width);
        int sourceHeight = subTexture.getIntAttribute("frameHeight", height);
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
   * graphic left and positive {@code y} moves it up.
   *
   * <p>The feature is opt-in. Until the first {@code addOffset} call the controller never touches the
   * sprite's offset; afterwards it owns it, and playing a clip with no registered offset resets the
   * offset to {@code (0, 0)}. Avoid mixing manual {@link FlixelSprite#setOffset} with this API.
   *
   * <pre>{@code
   * var anim = sprite.ensureAnimation();
   * anim.addByPrefix("idle", "BF idle dance", 24, true);
   * anim.addByPrefix("singLEFT", "BF NOTE LEFT", 24, false);
   * anim.addOffset("idle", 0, 0);
   * anim.addOffset("singLEFT", -12, 6);
   * anim.play("idle"); // offset snaps to (0, 0)
   * }</pre>
   *
   * @param name The animation name, as registered with {@code add(...)}, {@code addByPrefix(...)},
   *     {@code addByIndices(...)}, or {@code addFromAtlas(...)}.
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
   * Returns {@code true} if a per-animation offset is registered for the given name.
   *
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
   * offset until the next {@link #play} call reapplies it.
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

  /** Pauses animation playback so the current frame is held until {@link #resume()} is called. */
  public void pause() {
    paused = true;
  }

  /** Resumes animation playback after a previous {@link #pause()} call. */
  public void resume() {
    paused = false;
  }

  /**
   * Returns {@code true} when animation playback is currently paused.
   *
   * @return {@code true} when playback is paused, {@code false} when it is running.
   */
  public boolean isPaused() {
    return paused;
  }

  /** Toggles playback direction. Time still advances by {@link #update(float)} while not paused. */
  public void reverse() {
    playDirection = -playDirection;
  }

  /**
   * Returns the current play direction: {@code 1} for forward, {@code -1} for reversed.
   *
   * @return {@code 1} when playing forward, {@code -1} when playing in reverse.
   */
  public float getPlayDirection() {
    return playDirection;
  }

  /**
   * Sets the play direction for animation playback.
   *
   * @param playDirection {@code 1} for forward, {@code -1} for reversed; clamped to those values.
   */
  public void setPlayDirection(int playDirection) {
    playDirection = FlixelMath.clamp(playDirection, -1, 1);
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
  public void addByPrefix(
      @NotNull String name,
      @NotNull String prefix,
      int frameRate,
      boolean loop) {
    FlixelArray<FlixelFrame> atlas = owner.getAtlasRegions();
    if (atlas == null) {
      return;
    }
    FlixelArray<FlixelFrame> clipFrames = new FlixelArray<>();
    for (FlixelFrame frame : atlas) {
      if (frame != null && frame.name != null && frame.name.startsWith(prefix)) {
        clipFrames.add(frame);
      }
    }
    if (clipFrames.getSize() == 0) {
      return;
    }
    clipFrames.sort(Comparator.comparing(f -> f.name));
    animations.put(
        name,
        new FlixelAnimation<>(
            1f / frameRate,
            clipFrames,
            loop ? FlixelAnimation.PlayMode.LOOP : FlixelAnimation.PlayMode.NORMAL));
  }

  /**
   * Adds a clip from the current atlas list {@link FlixelSprite#getAtlasRegions()} using zero-based indices into
   * that list. Used by spritemap JSON and by games that know frame order after a Sparrow or spritemap load.
   *
   * @param name Clip name for {@link #play(String)}.
   * @param atlasFrameIndices Indices into the atlas list (out-of-range entries are skipped).
   * @param frameDuration Frame duration in seconds (reciprocal of FPS).
   * @param loop Whether the clip loops.
   */
  public void addFromAtlas(
      @NotNull String name, @NotNull int[] atlasFrameIndices, float frameDuration, boolean loop) {
    FlixelArray<FlixelFrame> atlas = owner.getAtlasRegions();
    if (atlas == null || atlas.getSize() == 0) {
      return;
    }
    FlixelArray<FlixelFrame> clipFrames = new FlixelArray<>();
    for (int i : atlasFrameIndices) {
      if (i >= 0 && i < atlas.getSize()) {
        clipFrames.add(atlas.get(i));
      }
    }
    if (clipFrames.getSize() == 0) {
      return;
    }
    animations.put(
        name,
        new FlixelAnimation<>(
            frameDuration,
            clipFrames,
            loop ? FlixelAnimation.PlayMode.LOOP : FlixelAnimation.PlayMode.NORMAL));
  }

  /**
   * Adds a looping clip built from frame numbers in the sprite's own frame list.
   *
   * @param name The name of the clip, used later with {@link #play(String)}.
   * @param frames Frame numbers to play, in order. See {@link #add(String, int[], int, boolean)}.
   * @param frameRate How many frames the clip shows per second.
   * @see #add(String, int[], int, boolean)
   */
  public void add(@NotNull String name, @NotNull int[] frames, int frameRate) {
    add(name, frames, frameRate, true);
  }

  /**
   * Adds a clip built from frame numbers in the sprite's own frame list.
   *
   * <p>Think of the sprite's frames as a numbered deck of cards: this method picks cards out of the
   * deck by number and lays them down in the order you list them. Where the deck comes from depends
   * on how the sprite was loaded:
   * <ul>
   *   <li>A sprite sheet loaded with {@link FlixelSprite#loadGraphic(FlixelFile, int, int)} is cut
   *   into a grid, and its frames are numbered left to right, then top to bottom, starting at
   *   {@code 0}.</li>
   *   <li>A sprite with an atlas (Sparrow XML or a spritemap) numbers its frames in the order they
   *   appear in {@link FlixelSprite#getAtlasRegions()}. Most atlas clips are easier to build by name
   *   with {@link #addByPrefix(String, String, int, boolean)} or
   *   {@link #addByIndices(String, String, int[], String, int, boolean)} instead.</li>
   * </ul>
   *
   * <p>Frame numbers may repeat, which is handy for holding a pose. Numbers that do not exist are
   * skipped with a warning, and if none of them exist no clip is registered.
   *
   * <pre>{@code
   * sprite.loadGraphic(Flixel.files.internal("images/player.png"), 32, 32);
   * sprite.ensureAnimation().add("idle", new int[] {0, 1}, 4, true);
   * sprite.animation.add("walk", new int[] {2, 3, 4, 5}, 10, true);
   * sprite.animation.play("walk");
   * }</pre>
   *
   * @param name The name of the clip, used later with {@link #play(String)}.
   * @param frames Frame numbers to play, in order.
   * @param frameRate How many frames the clip shows per second.
   * @param loop Whether the clip loops when played with {@link #play(String)}.
   */
  public void add(@NotNull String name, @NotNull int[] frames, int frameRate, boolean loop) {
    FlixelArray<FlixelFrame> atlas = owner.getAtlasRegions();
    FlixelFrame[][] grid = owner.getFrames();
    FlixelArray<FlixelFrame> clipFrames = new FlixelArray<>(frames.length);
    for (int index : frames) {
      FlixelFrame frame = (atlas != null) ? atlasFrame(atlas, index) : gridFrame(grid, index);
      if (frame != null) {
        clipFrames.add(frame);
      } else {
        Flixel.log.warn("Animation '{}' skipped frame {}, which does not exist on this sprite.", name, index);
      }
    }
    register(name, clipFrames, frameRate, loop);
  }

  /**
   * Adds a clip from atlas frames picked by number, for frames named without a postfix.
   *
   * @param name The name of the clip, used later with {@link #play(String)}.
   * @param prefix The text every frame name in the clip starts with.
   * @param indices Frame numbers to play, in order.
   * @param frameRate How many frames the clip shows per second.
   * @param loop Whether the clip loops when played with {@link #play(String)}.
   * @see #addByIndices(String, String, int[], String, int, boolean)
   */
  public void addByIndices(
      @NotNull String name, @NotNull String prefix, @NotNull int[] indices, int frameRate, boolean loop) {
    addByIndices(name, prefix, indices, "", frameRate, loop);
  }

  /**
   * Adds a clip from atlas frames picked by the number in their names.
   *
   * <p>Atlas exporters name each frame as a prefix followed by a number, such as
   * {@code "BF idle dance0000"}, {@code "BF idle dance0001"}, and so on, sometimes with a postfix
   * after the number. Where {@link #addByPrefix(String, String, int, boolean)} grabs every frame
   * that shares a prefix, this method grabs only the numbers you list, in the order you list them.
   * That lets you reuse part of a sequence, play it backwards, or hold a frame by repeating it.
   *
   * <p>A frame matches number {@code n} when its name is exactly {@code prefix}, then digits whose
   * value is {@code n}, then {@code postfix}. Leading zeros are ignored, so index {@code 3} matches
   * both {@code "idle3"} and {@code "idle0003"}. Frames with extra text between the prefix and the
   * number (like {@code "idle-alt0003"}) never match, so similar prefixes do not bleed into each
   * other. Numbers with no matching frame are skipped with a warning, and if none match no clip is
   * registered.
   *
   * <pre>{@code
   * var anim = sprite.ensureAnimation();
   * anim.addSparrowFrames(Flixel.files.internal("images/gf"));
   * // Split one dance sequence into two halves that alternate on each beat.
   * anim.addByIndices("danceLeft", "GF Dancing Beat", new int[] {0, 1, 2, 3, 4, 5, 6, 7}, "", 24, false);
   * anim.addByIndices("danceRight", "GF Dancing Beat", new int[] {8, 9, 10, 11, 12, 13, 14}, "", 24, false);
   * anim.play("danceLeft");
   * }</pre>
   *
   * @param name The name of the clip, used later with {@link #play(String)}.
   * @param prefix The text every frame name in the clip starts with.
   * @param indices Frame numbers to play, in order.
   * @param postfix The text every frame name in the clip ends with, or {@code ""} for none.
   * @param frameRate How many frames the clip shows per second.
   * @param loop Whether the clip loops when played with {@link #play(String)}.
   */
  public void addByIndices(
      @NotNull String name,
      @NotNull String prefix,
      @NotNull int[] indices,
      @NotNull String postfix,
      int frameRate,
      boolean loop) {
    FlixelArray<FlixelFrame> atlas = owner.getAtlasRegions();
    if (atlas == null) {
      Flixel.log.warn("Animation '{}' needs an atlas, but this sprite has none.", name);
      return;
    }
    FlixelArray<FlixelFrame> clipFrames = new FlixelArray<>(indices.length);
    for (int index : indices) {
      FlixelFrame frame = findNumberedFrame(atlas, prefix, index, postfix);
      if (frame != null) {
        clipFrames.add(frame);
      } else {
        Flixel.log.warn("Animation '{}' found no frame named '{}' + {} + '{}'.", name, prefix, index, postfix);
      }
    }
    register(name, clipFrames, frameRate, loop);
  }

  /**
   * Reads the frame number out of an atlas frame name shaped like {@code prefix + digits + postfix}.
   *
   * <p>This is the matching rule behind {@link #addByIndices(String, String, int[], String, int, boolean)},
   * split out so it can be unit tested without a texture.
   *
   * @param frameName The atlas frame name to read, or {@code null}.
   * @param prefix The text the name must start with.
   * @param postfix The text the name must end with.
   * @return The number between the prefix and postfix, or {@code -1} when the name does not have
   *     that shape (missing prefix or postfix, no digits, non-digit characters, or a number too large
   *     for an {@code int}).
   */
  static int frameNumber(@Nullable String frameName, @NotNull String prefix, @NotNull String postfix) {
    if (frameName == null || !frameName.startsWith(prefix) || !frameName.endsWith(postfix)) {
      return -1;
    }
    int start = prefix.length();
    int end = frameName.length() - postfix.length();
    if (end <= start) {
      return -1;
    }
    int value = 0;
    for (int i = start; i < end; i++) {
      char c = frameName.charAt(i);
      if (c < '0' || c > '9') {
        return -1;
      }
      int digit = c - '0';
      if (value > (Integer.MAX_VALUE - digit) / 10) {
        return -1;
      }
      value = value * 10 + digit;
    }
    return value;
  }

  /** Returns the first atlas frame whose name is {@code prefix + index + postfix}, or {@code null}. */
  @Nullable
  private static FlixelFrame findNumberedFrame(
      @NotNull FlixelArray<FlixelFrame> atlas, @NotNull String prefix, int index, @NotNull String postfix) {
    if (index < 0) {
      return null;
    }
    for (int i = 0; i < atlas.getSize(); i++) {
      FlixelFrame frame = atlas.get(i);
      if (frame != null && frameNumber(frame.name, prefix, postfix) == index) {
        return frame;
      }
    }
    return null;
  }

  /** Returns the atlas frame at {@code index}, or {@code null} when it is out of range. */
  @Nullable
  private static FlixelFrame atlasFrame(@NotNull FlixelArray<FlixelFrame> atlas, int index) {
    return (index >= 0 && index < atlas.getSize()) ? atlas.get(index) : null;
  }

  /** Returns grid frame {@code index}, counted left to right then top to bottom, or {@code null}. */
  @Nullable
  private static FlixelFrame gridFrame(@Nullable FlixelFrame[][] grid, int index) {
    if (grid == null || grid.length == 0 || grid[0] == null || grid[0].length == 0 || index < 0) {
      return null;
    }
    int cols = grid[0].length;
    int row = index / cols;
    if (row >= grid.length || grid[row] == null) {
      return null;
    }
    return grid[row][index % cols];
  }

  /** Registers a clip under {@code name}, or does nothing when {@code clipFrames} is empty. */
  private void register(
      @NotNull String name, @NotNull FlixelArray<FlixelFrame> clipFrames, int frameRate, boolean loop) {
    if (clipFrames.isEmpty()) {
      return;
    }
    animations.put(
        name,
        new FlixelAnimation<>(
            1f / frameRate,
            clipFrames,
            loop ? FlixelAnimation.PlayMode.LOOP : FlixelAnimation.PlayMode.NORMAL));
  }

  /**
   * Plays the animation with the given name. Loops and restarts the animation by default.
   *
   * @param name The name of the animation to play.
   */
  public void play(@NotNull String name) {
    play(name, true);
  }

  /**
   * Plays the animation with the given name. Restarts the animation by default.
   *
   * @param name The name of the animation to play.
   * @param loop Whether the animation should loop.
   */
  public void play(@NotNull String name, boolean loop) {
    play(name, loop, true);
  }

  /**
   * Plays the animation with the given name.
   *
   * @param name The name of the animation to play.
   * @param loop Whether the animation should loop.
   * @param forceRestart Whether the animation should restart.
   */
  public void play(@NotNull String name, boolean loop, boolean forceRestart) {
    FlixelAnimation<FlixelFrame> anim = animations.get(name);
    if (anim == null) {
      // Unknown clip name: leave whatever is currently displayed untouched instead of blanking out.
      return;
    }
    // Keep playing only when this exact clip is already mid-play and the caller did not force a
    // restart. A clip that has finished (or a different clip) always (re)starts, so a non-looping
    // clip can be replayed once it ends and switching clips never silently no-ops.
    if (!shouldRestart(forceRestart, currentAnim.equals(name), isFinished())) {
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
    applyOffset(name);
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
  private void applyOffset(@NotNull String name) {
    if (animationOffsets.getSize() == 0) {
      return;
    }
    float[] offset = animationOffsets.get(name);
    if (offset != null) {
      owner.setOffset(offset[0], offset[1]);
    } else {
      owner.setOffset(0f, 0f);
    }
  }

  /**
   * Returns {@code true} when the current non-looping animation has played through all frames.
   *
   * @return {@code true} when the animation has finished, {@code false} when it is still running or looping.
   */
  public boolean isFinished() {
    FlixelAnimation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) {
      return true;
    }
    float duration = anim.getAnimationDuration();
    return !looping && duration > 0f && stateTime >= duration;
  }

  @Override
  public void update(float elapsed) {
    if (animations.getSize() == 0 || paused || currentAnim.isEmpty()) {
      return;
    }
    FlixelAnimation<FlixelFrame> anim = animations.get(currentAnim);
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
      stateTime = FlixelMath.clamp(stateTime, 0f, duration);
    }

    int frameIndex = computeKeyframeIndex(anim);
    // Pick the displayed frame by the same index the controller reports elsewhere, rather than
    // anim.getKeyFrame(stateTime, looping). That call honors the clip's REGISTERED PlayMode,
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
  public FlixelMap<String, FlixelAnimation<FlixelFrame>> getAnimations() {
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
   * controller's {@link #isLooping() looping} flag rather than from the underlying
   * {@link FlixelAnimation}'s {@code PlayMode}, so the index always matches what {@link #update(float)}
   * actually displays no matter how the clip was registered.
   *
   * <p>For non-looping playback that has already finished, this returns the <strong>last</strong>
   * keyframe index ({@code keyframeCount - 1}). For looping playback, it returns the wrapped index
   * inside {@code [0, keyframeCount)}. Useful for multipart BTA/texture-atlas characters driven
   * outside a single-frame texture (for example {@link FlixelAnimateSprite}).
   *
   * @return The current keyframe index. Always {@code >= 0}.
   */
  public int getCurrentKeyframeIndex() {
    if (currentAnim.isEmpty()) {
      return 0;
    }
    FlixelAnimation<FlixelFrame> anim = animations.get(currentAnim);
    if (anim == null) {
      return 0;
    }
    return computeKeyframeIndex(anim);
  }

  /**
   * Returns the keyframe index for {@code anim} at the current {@link #stateTime}, honoring the
   * controller's runtime {@link #looping} flag. This deliberately bypasses
   * {@link FlixelAnimation#getKeyFrameIndex(float)} (which uses the {@link FlixelAnimation#getPlayMode()
   * registered PlayMode}) so that:
   * <ul>
   *   <li>Non-looping playback at the end of the clip returns the <strong>last</strong> keyframe
   *   instead of wrapping back to the first (the latter is what {@link FlixelAnimation.PlayMode#LOOP
   *   LOOP} does when {@code stateTime == duration}, and is the source of the "first frame appears at
   *   the end" bug for clips registered with {@code loop = true} but played with looping disabled).</li>
   *   <li>Looping playback always returns an in-range index, even right at the wrap point.</li>
   * </ul>
   *
   * @param anim The clip to read the keyframe index from. Must not be {@code null}.
   * @return A keyframe index in {@code [0, keyframeCount - 1]}, or {@code 0} for degenerate clips.
   */
  private int computeKeyframeIndex(@NotNull FlixelAnimation<FlixelFrame> anim) {
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

  /**
   * Returns whether the current animation is set to loop.
   *
   * @return {@code true} when the current animation repeats, {@code false} when it plays once and stops.
   */
  public boolean isLooping() {
    return looping;
  }

  /**
   * Returns the linked {@link FlixelAnimationStateMachine}, or {@code null} if none is set.
   *
   * @return The linked state machine, or {@code null} when none has been attached.
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
   * {@link FlixelAnimationStateMachine#update(float)} by hand, just do not link it here.
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
