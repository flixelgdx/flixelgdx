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
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.collections.FlixelObjectIntMap;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.json.FlixelJson;
import org.flixelgdx.json.FlixelJsonValue;
import org.flixelgdx.math.FlixelAffine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Loader that converts a pair of Adobe Animate texture-atlas JSON files plus a spritemap PNG into a
 * {@link FlixelAnimateRig}, then installs that rig on a {@link FlixelAnimateSprite}.
 *
 * <h2>Input format</h2>
 * <p>Adobe Animate's built-in texture atlas export produces three companion files:
 * <ol>
 *   <li>{@code spritemap1.png} - a single packed texture holding every unique bitmap slice of the rig.</li>
 *   <li>{@code spritemap1.json} - a map describing where each named bitmap lives on the PNG
 *   ({@code ATLAS.SPRITES[].SPRITE}: {@code name}, {@code x}, {@code y}, {@code w}, {@code h}).</li>
 *   <li>{@code Animation.json} - timeline data. The loader supports two Adobe export shapes that share
 *   the same {@link FlixelAnimateSprite#addSpritemapAndAnimation} entry point:
 *   <ul>
 *     <li><strong>Symbol / nested rig</strong> (common with the "Better Texture Atlas" extension and
 *     complex characters): {@code AN.TL.L} contains exactly one <strong>main</strong> layer whose
 *     {@code FR} entries each hold a root symbol instance ({@code E.SI}). Other layers are
 *     <strong>label</strong> layers; any {@code FR} with an {@code N} (name) field becomes a playable
 *     clip with start index {@code I} and duration {@code DU}. {@code SD.S} is the symbol dictionary:
 *     each symbol has nested timelines that may contain further {@code SI} references or leaf
 *     {@code ASI} atlas instances (with {@code MX} or {@code M3D} matrices).</li>
 *     <li><strong>Document timeline</strong> (stock Animate export for many single-scene effects): the
 *     main layer's {@code FR} entries reference bitmaps directly via {@code E.ASI} (no root {@code SI},
 *     and often no {@code SD} block at all). Matrices are usually {@code M3D} (16 floats, column-major
 *     {@code 4x4}). If there are no label layers with {@code N}, the loader creates one clip spanning
 *     the whole timeline, named from {@code AN.SN} when present, otherwise {@code AN.N}, otherwise
 *     {@code "animation"}.</li>
 *   </ul>
 *   The metadata block {@code MD.FRT} carries the authoring frame rate in frames per second when present.
 *   </li>
 * </ol>
 *
 * <h2>Matrix convention</h2>
 * Flash stores affines as six-element arrays {@code [a, b, c, d, tx, ty]} representing
 * {@code x' = a*x + c*y + tx}, {@code y' = b*x + d*y + ty}. A {@link FlixelAffine} uses the fields
 * {@code m00, m01, m02, m10, m11, m12} with {@code x' = m00*x + m01*y + m02},
 * {@code y' = m10*x + m11*y + m12}, so the packing is:
 * <pre>
 *   m00 = a;   m01 = c;   m02 = tx;
 *   m10 = b;   m11 = d;   m12 = ty;
 * </pre>
 *
 * <h2>Coordinate space</h2>
 * Adobe Animate uses Y-down pixel space (the top-left of a bitmap is {@code (0, 0)}), which is exactly
 * the space the FlixelGDX renderer draws in. No coordinate flips are needed: a frame is drawn with its
 * top-left at the supplied local origin, so the loader only slides the composed Flash-world coordinates
 * so that the anchor bounding box's top-left corner sits at the sprite's origin. The per-part bake is a
 * simple {@code anchorShift * P_flash}, where {@code anchorShift = translate(-anchorMinX, -anchorMinY)}.
 * Parts packed rotated 90 degrees clockwise are un-rotated by swapping the affine's two linear columns
 * (see {@link #bakePartAffine}).
 *
 * <h2>Layer z-order</h2>
 * In Flash (and in Adobe Animate's exported JSON), the <strong>first</strong> layer in {@code TL.L} is
 * drawn on top, and the last layer is drawn on the bottom. The loader walks layers in reverse so that
 * the resulting {@link FlixelAnimateRig.Keyframe#parts} array is already in back-to-front order and the
 * draw path can iterate forward without any extra bookkeeping. Elements within a single layer's
 * {@code E} array keep their declared order.
 *
 * <h2>Symbol-instance loop modes</h2>
 * Every {@code SI} (symbol instance) carries an {@code FF} ("first frame") and an {@code LP} ("loop
 * parameter") field. {@code FF} offsets where the child symbol's timeline starts; {@code LP} controls
 * what happens when the parent's tick walks past the child symbol's last frame. The loader honors
 * the three Flash modes ({@code "loop"}, {@code "playonce"}, {@code "singleframe"}, also accepted in
 * abbreviated form as {@code "LP"}, {@code "PO"}, {@code "SF"}) so that, for example, a single-frame
 * faceplate stays put while the surrounding rig animates instead of "leaking" frames from a different
 * sub-clip.
 */
final class FlixelAnimateRigLoader {

  /**
   * Maximum depth for symbol recursion. A deeper graph is almost certainly a cycle, so the loader bails
   * out rather than overflowing the Java stack.
   */
  private static final int MAX_NEST = 8;

  /** Scratch affine used by {@link #matrixFromFlashMx} to avoid allocating during parsing. */
  private final FlixelAffine scratchMx = new FlixelAffine();

  /** Shared identity template for resetting {@link FlixelAffine} instances cheaply. */
  private static final FlixelAffine IDENTITY = new FlixelAffine();

  /**
   * Cache of {@code SN -> total timeline length} for every visited symbol. Used to evaluate the loop
   * mode ({@code LP}) of a child symbol instance without re-walking its layer/frame tree on every
   * keyframe of every clip.
   */
  private final FlixelMap<String, Integer> symbolDurations = new FlixelMap<>();

  /**
   * Loads the given spritemap/animation pair, builds a fully baked {@link FlixelAnimateRig}, and installs
   * it on {@code sprite}.
   *
   * <p>Equivalent to calling {@link #load(FlixelAnimateSprite, FlixelAnimationController, String, String, String, String)}
   * with a {@code null} {@code anchorClipName}, which means the first clip in the timeline's label layer is used
   * as the anchor.
   *
   * @param sprite The {@link FlixelAnimateSprite} that will own the rig. Must not be {@code null}.
   * @param controller The sprite's animation controller (used to register clip durations for timing).
   *   Must not be {@code null}.
   * @param textureKey The asset key of the already-enqueued spritemap {@link FlixelGraphic}
   *   (for example {@code "shared/images/characters/bf/spritemap1.png"}). Must not be {@code null}.
   * @param spritemapJsonPath The resolver-relative path to {@code spritemap1.json}. Must not be {@code null}.
   * @param animationJsonPath The resolver-relative path to {@code Animation.json}. Must not be {@code null}.
   * @throws IllegalArgumentException If any of the three files is missing, malformed, or fails a
   *   structural precondition (for example, the spritemap has zero sprites, {@code AN.TL.L} is missing
   *   a recognizable main layer, or a symbol-based export has an empty {@code SD.S} block).
   */
  static void load(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    load(sprite, controller, textureKey, spritemapJsonPath, animationJsonPath, null);
  }

  /**
   * Loads the given spritemap/animation pair, builds a fully baked {@link FlixelAnimateRig}, and installs
   * it on {@code sprite}. On success the sprite is resized to the anchor clip's bounding box, its
   * {@link FlixelAnimationController} is populated with one clip per label, and the anchor clip is the one
   * being played.
   *
   * @param sprite The {@link FlixelAnimateSprite} that will own the rig. Must not be {@code null}.
   * @param controller The sprite's animation controller (used to register clip durations for timing).
   *   Must not be {@code null}.
   * @param textureKey The asset key of the already-enqueued spritemap {@link FlixelGraphic}. Must not be
   *   {@code null}.
   * @param spritemapJsonPath The resolver-relative path to {@code spritemap1.json}. Must not be {@code null}.
   * @param animationJsonPath The resolver-relative path to {@code Animation.json}. Must not be {@code null}.
   * @param anchorClipName The name of the clip whose first keyframe defines the rig's bounding box and
   *   which is auto-played after loading. Pass {@code null} (or an unmatched name) to fall back to the
   *   first clip in the timeline's label layer.
   * @throws IllegalArgumentException If any of the three files is missing, malformed, or fails a
   *   structural precondition (for example: the spritemap has zero sprites, {@code AN.TL.L} is missing
   *   a recognizable main layer, or a symbol-based export has an empty {@code SD.S} block).
   */
  static void load(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {
    Objects.requireNonNull(sprite, "sprite cannot be null");
    Objects.requireNonNull(controller, "controller cannot be null");
    Objects.requireNonNull(textureKey, "textureKey cannot be null");
    Objects.requireNonNull(spritemapJsonPath, "spritemapJsonPath cannot be null");
    Objects.requireNonNull(animationJsonPath, "animationJsonPath cannot be null");
    new FlixelAnimateRigLoader().loadInternal(
        sprite, controller, textureKey, spritemapJsonPath, animationJsonPath, anchorClipName);
  }

  /**
   * Appends an additional Adobe Animate texture-atlas export onto a sprite that already has a rig
   * installed. The new atlas's frames are appended to the existing {@link FlixelAnimateRig#atlas},
   * its clips are baked using the existing rig's anchor space (so the body stays pinned to the same
   * world position when game code switches between atlases), and the new clip names are registered
   * on the same {@link FlixelAnimationController}. Mirrors the multi-atlas character workflow used
   * by the original {@code flxanimate} (and Friday Night Funkin') for characters whose animations
   * are split across multiple Animate exports (for example, Pico's basic singing animations versus
   * his playable miss animations).
   *
   * <p>Clip-name collisions silently overwrite the previously registered clip on both the rig and
   * the controller (matching {@link FlixelMap#put} semantics), allowing later loads to act as
   * costume / behavior overrides.
   *
   * @param sprite The sprite to append onto. Must already have a rig (for example the first triple loaded through
   *   {@link FlixelAnimateSprite#addSpritemapAndAnimation}). Must not be {@code null}.
   * @param controller The sprite's animation controller (used to register the new clip durations
   *   for timing). Must not be {@code null}.
   * @param textureKey The asset key of the appended spritemap {@link FlixelGraphic}. Must not be {@code null}.
   * @param spritemapJsonPath The resolver-relative path to the appended spritemap JSON. Must not be {@code null}.
   * @param animationJsonPath The resolver-relative path to the appended animation JSON. Must not be {@code null}.
   * @throws IllegalStateException If {@code sprite} has no rig installed.
   * @throws IllegalArgumentException If any of the three files is missing, malformed, or fails a
   *   structural precondition.
   */
  static void append(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    Objects.requireNonNull(sprite, "sprite cannot be null");
    Objects.requireNonNull(controller, "controller cannot be null");
    Objects.requireNonNull(textureKey, "textureKey cannot be null");
    Objects.requireNonNull(spritemapJsonPath, "spritemapJsonPath cannot be null");
    Objects.requireNonNull(animationJsonPath, "animationJsonPath cannot be null");
    FlixelAnimateRig existing = sprite.getRig();
    if (existing == null) {
      throw new IllegalStateException(
          "Cannot append to a FlixelAnimateSprite that has no rig installed; call "
              + "addSpritesheetAndAnimation(...) first to establish the anchor coordinate space.");
    }
    new FlixelAnimateRigLoader().appendInternal(
        sprite, controller, existing, textureKey, spritemapJsonPath, animationJsonPath);
  }

  private void loadInternal(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {

    // Read and parse both JSON files up-front. The JSON reader owns no file handles after this call.
    String spritemapText = FlixelSpritemapJsonLoader.readUtf8Text(
        FlixelSpritemapJsonLoader.resolveAssetPath(spritemapJsonPath));
    String animationText = FlixelSpritemapJsonLoader.readUtf8Text(
        FlixelSpritemapJsonLoader.resolveAssetPath(animationJsonPath));
    FlixelJsonValue spritemapRoot = FlixelJson.parse(spritemapText);
    FlixelJsonValue animationRoot = FlixelJson.parse(animationText);

    // Obtain the backing texture. If the asset has not been preloaded, fall back to a synchronous load.
    FlixelGraphic graphic = Flixel.assets.<FlixelGraphic>get(textureKey).retain().get();
    FlixelTexture texture = graphic.getTexture();

    // Build the atlas region list and the "ATLAS name -> region index" lookup shared by every ASI reference.
    FlixelObjectIntMap<String> nameToIndex = new FlixelObjectIntMap<>();
    FlixelArray<FlixelFrame> atlas = FlixelSpritemapJsonLoader.parseAtlasSprites(spritemapRoot, texture, nameToIndex);
    if (atlas.getSize() == 0) {
      throw new IllegalArgumentException("Spritemap JSON produced zero atlas regions.");
    }

    // Install the graphic + atlas on the sprite now so FlixelSprite.applySparrowAtlas() can tear down any
    // previous graphic and reset the animation controller before we register new clips on it.
    sprite.applySparrowAtlas(graphic, atlas);

    // Parse the AN/SD structure once, then bake every clip and its keyframes.
    ParsedAnimation parsed = ParsedAnimation.parse(animationRoot);
    float fps = parsed.framesPerSecond;
    if (fps <= 0f) {
      fps = 24f;
    }

    // Compute the anchor-clip bounding box in Flash Y-down world space. This is required up-front so that
    // every part in every clip can be baked into the same rig-local coordinate system.
    float[] anchorBox = new float[] {
        Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
    };
    int anchorClipIndex = pickAnchorClipIndex(parsed.clipDefs, anchorClipName);
    computeAnchorBbox(parsed, anchorClipIndex, nameToIndex, atlas, anchorBox);
    if (anchorBox[0] > anchorBox[2] || anchorBox[1] > anchorBox[3]) {
      // Degenerate case (no parts in the anchor clip). Fall back to a 1x1 rectangle so the sprite still
      // has a valid hitbox and the draw path does not divide by zero.
      anchorBox[0] = 0f;
      anchorBox[1] = 0f;
      anchorBox[2] = 1f;
      anchorBox[3] = 1f;
    }
    float anchorMinX = anchorBox[0];
    float anchorMinY = anchorBox[1];
    float anchorWidth = anchorBox[2] - anchorMinX;
    float anchorHeight = anchorBox[3] - anchorMinY;

    FlixelMap<String, FlixelAnimateRig.Clip> clips = new FlixelMap<>();
    bakeClipsInto(parsed, fps, atlas, nameToIndex, anchorMinX, anchorMinY, controller, clips);

    String resolvedAnchorName = parsed.clipDefs.get(anchorClipIndex).name;
    FlixelAnimateRig rig = new FlixelAnimateRig(
        atlas, clips, resolvedAnchorName, anchorMinX, anchorMinY, anchorWidth, anchorHeight);
    sprite.installAnimateRig(rig);

    // applySparrowAtlas() set a default currentFrame/region on the sprite. Now that the rig is installed,
    // clear them so the rig's draw path takes over and the hitbox is sized from the anchor bbox.
    sprite.clearAnimationDisplayFrame();
    sprite.updateHitbox();

    // Start the anchor clip so the sprite has a visible pose even before game code calls playAnimation.
    controller.play(resolvedAnchorName, true, true);
  }

  /**
   * Appends an additional Adobe Animate texture-atlas onto an existing rig. Reuses {@code existing}'s
   * anchor coordinate system so newly baked parts sit in the same anchor-local space as the rig's
   * original parts, which is what keeps the character's body pinned to the same on-screen position
   * when game code switches between atlases.
   *
   * <p>The new spritemap's frames are appended to {@link FlixelAnimateRig#atlas} (which is shared
   * by reference with the owning sprite, so {@link org.flixelgdx.FlixelSprite#getAtlasRegions() FlixelSprite.getAtlasRegions()}
   * keeps working) and the new label-layer clips are added to {@link FlixelAnimateRig#clips}. Clip
   * names that collide with existing ones are silently overwritten on both the rig and the controller,
   * matching {@link FlixelMap#put}.
   *
   * @param sprite The owning sprite (used to retain the appended {@link FlixelGraphic}).
   * @param controller The animation controller to register the new clips on.
   * @param existing The rig that the new atlas is being appended to. Must already be installed on
   *   {@code sprite}.
   * @param textureKey Asset key for the appended PNG.
   * @param spritemapJsonPath Resolver path for the appended spritemap JSON.
   * @param animationJsonPath Resolver path for the appended animation JSON.
   */
  private void appendInternal(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull FlixelAnimateRig existing,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath) {
    String spritemapText = FlixelSpritemapJsonLoader.readUtf8Text(
        FlixelSpritemapJsonLoader.resolveAssetPath(spritemapJsonPath));
    String animationText = FlixelSpritemapJsonLoader.readUtf8Text(
        FlixelSpritemapJsonLoader.resolveAssetPath(animationJsonPath));
    FlixelJsonValue spritemapRoot = FlixelJson.parse(spritemapText);
    FlixelJsonValue animationRoot = FlixelJson.parse(animationText);

    FlixelGraphic graphic = Flixel.assets.<FlixelGraphic>get(textureKey).retain().get();
    FlixelTexture texture = graphic.getTexture();

    // Parse the new atlas with a fresh local lookup, then offset every entry so the indices point
    // into the merged atlas (existing rig frames first, appended frames after). This lets the bake
    // path reuse a single FlixelArray<FlixelFrame> without ever discriminating between "old" and "new"
    // frames at runtime.
    FlixelObjectIntMap<String> localNameToIndex = new FlixelObjectIntMap<>();
    FlixelArray<FlixelFrame> newAtlasFrames =
        FlixelSpritemapJsonLoader.parseAtlasSprites(spritemapRoot, texture, localNameToIndex);
    if (newAtlasFrames.getSize() == 0) {
      throw new IllegalArgumentException("Appended spritemap JSON produced zero atlas regions.");
    }
    int atlasOffset = existing.atlas.getSize();
    existing.atlas.addAll(newAtlasFrames);

    FlixelObjectIntMap<String> nameToIndex = new FlixelObjectIntMap<>(localNameToIndex.getSize());
    for (FlixelObjectIntMap.Entry<String> e : localNameToIndex.entries()) {
      nameToIndex.put(e.key, e.value + atlasOffset);
    }

    // Retain the appended graphic on the sprite so it does not get unloaded out from under us. Done
    // through the sprite (rather than directly on the asset wrapper), so the retained count is balanced
    // against the sprite's own destroy() / applySparrowAtlas() lifecycle.
    sprite.retainSecondaryGraphic(graphic);

    // Bake the appended clips into the existing rig's anchor coordinate system. Reusing the original
    // anchor minX/minY/height ensures the body's pivot point in Flash space lands on the same spot
    // in anchor-local space across both atlases, so the character does not visually jump when game
    // code switches between an idle clip on atlas A and a miss clip on atlas B.
    ParsedAnimation parsed = ParsedAnimation.parse(animationRoot);
    float fps = parsed.framesPerSecond;
    if (fps <= 0f) {
      fps = 24f;
    }

    bakeClipsInto(
        parsed,
        fps,
        existing.atlas,
        nameToIndex,
        existing.anchorMinX,
        existing.anchorMinY,
        controller,
        existing.clips);
  }

  /**
   * Bakes every clip in {@code parsed} into {@code clipsOut} and registers each clip's duration with
   * {@code controller}. Shared by both the initial-load and append paths, so the per-clip baking
   * algorithm stays in one place. Each keyframe's parts are stored back-to-front (layers iterated in
   * reverse during {@link #visitSymbol}), so the draw path can iterate forward with no extra work.
   *
   * <p>Clip definitions carry their absolute start tick on the main timeline. The baker looks up the
   * main-layer {@code FR} that owns each absolute tick rather than assuming a 1:1 alignment between
   * the label layer and the main layer, since that alignment only holds for the simplest exports.
   *
   * @param parsed The parsed animation layout (timelines and symbol dictionary).
   * @param fps Authoring frame rate (for the registered clip's per-frame duration).
   * @param atlas The merged atlas list to look frames up from. {@link FlixelAnimateRig.Part#atlasIndex}
   *   on every baked part will index into this array.
   * @param nameToIndex Sprite name to atlas index lookup, already offset to point into {@code atlas}.
   * @param anchorMinX Anchor bounding-box minimum X in Flash Y-down world space.
   * @param anchorMinY Anchor bounding-box minimum Y in Flash Y-down world space.
   * @param controller The controller to register clip durations on (one
   *   {@link FlixelAnimation} per clip name).
   * @param clipsOut The map to populate. Existing entries with the same name are overwritten.
   */
  private void bakeClipsInto(
      @NotNull ParsedAnimation parsed,
      float fps,
      @NotNull FlixelArray<FlixelFrame> atlas,
      @NotNull FlixelObjectIntMap<String> nameToIndex,
      float anchorMinX,
      float anchorMinY,
      @NotNull FlixelAnimationController controller,
      @NotNull FlixelMap<String, FlixelAnimateRig.Clip> clipsOut) {
    FlixelArray<RawPart> scratchRaw = new FlixelArray<>(32);
    for (int clipIndex = 0; clipIndex < parsed.clipDefs.getSize(); clipIndex++) {
      ClipDef clip = parsed.clipDefs.get(clipIndex);
      if (clip.duration < 1) {
        continue;
      }

      FlixelAnimateRig.Keyframe[] kfs = new FlixelAnimateRig.Keyframe[clip.duration];
      for (int t = 0; t < clip.duration; t++) {
        scratchRaw.clear();
        int absoluteTick = clip.startTick + t;
        FlixelJsonValue mainFrame = findMainFrameAt(parsed.mainFrames, absoluteTick);
        if (mainFrame != null) {
          int frameLocalTime = absoluteTick - readIntOr(mainFrame, "I", 0);
          collectKeyframeParts(parsed, mainFrame, frameLocalTime, nameToIndex, scratchRaw);
        }

        FlixelAnimateRig.Part[] parts = new FlixelAnimateRig.Part[scratchRaw.getSize()];
        for (int p = 0; p < scratchRaw.getSize(); p++) {
          RawPart raw = scratchRaw.get(p);
          FlixelFrame frame = atlas.get(raw.atlasIndex);
          FlixelAnimateRig.Part part = new FlixelAnimateRig.Part(raw.atlasIndex);
          bakePartAffine(part.local, raw.flashMatrix, frame.rotated, anchorMinX, anchorMinY);
          parts[p] = part;
        }
        kfs[t] = new FlixelAnimateRig.Keyframe(parts);
      }
      clipsOut.put(clip.name, new FlixelAnimateRig.Clip(clip.name, kfs));

      // Register the clip with the animation controller so getCurrentKeyframeIndex() advances over
      // time. The actual frame indices are irrelevant (the rig draw path ignores them), but
      // FlixelAnimation requires at least one entry, so feed it a duplicate of atlas[0] per tick. We
      // register with loop=false, so the backing Animation's PlayMode is NORMAL; runtime looping is
      // controlled entirely by FlixelAnimationController.playAnimation(...) and its own looping
      // flag, and registering as NORMAL guarantees that a non-looping clip's last keyframe
      // stays put instead of snapping back to the first when stateTime reaches the clip's duration.
      int[] dummyFrames = new int[clip.duration];
      controller.addFromAtlas(clip.name, dummyFrames, 1f / fps, false);
    }
  }

  /**
   * Selects which clip should define the anchor-space bounding box. When {@code requestedName} is
   * non-{@code null} and matches a clip definition's name, that clip is chosen; otherwise the first
   * clip in {@code clipDefs} is used.
   *
   * @param clipDefs The clip definitions gathered by {@link ParsedAnimation#parse}.
   * @param requestedName Caller-supplied anchor clip name, or {@code null} to default to the first clip.
   * @return The index into {@code clipDefs} whose name is the chosen anchor clip.
   */
  private static int pickAnchorClipIndex(
      @NotNull FlixelArray<ClipDef> clipDefs, @Nullable String requestedName) {
    if (requestedName != null && !requestedName.isEmpty()) {
      for (int i = 0; i < clipDefs.getSize(); i++) {
        if (requestedName.equals(clipDefs.get(i).name)) {
          return i;
        }
      }
    }
    return 0;
  }

  /**
   * Walks the anchor clip's first keyframe to compute the axis-aligned bounding box of every bitmap in
   * Flash Y-down world space, storing the result as {@code [minX, minY, maxX, maxY]} in {@code out}.
   *
   * @param parsed The parsed animation layout (timelines and symbol dictionary).
   * @param anchorClipIndex The index of the anchor clip in {@code parsed.clipDefs}.
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup.
   * @param atlas The atlas frames.
   * @param out The output bounding box.
   */
  private void computeAnchorBbox(
      @NotNull ParsedAnimation parsed,
      int anchorClipIndex,
      @NotNull FlixelObjectIntMap<String> nameToIndex,
      @NotNull FlixelArray<FlixelFrame> atlas,
      @NotNull float[] out) {
    ClipDef clip = parsed.clipDefs.get(anchorClipIndex);
    FlixelJsonValue mainFrame = findMainFrameAt(parsed.mainFrames, clip.startTick);
    if (mainFrame == null) {
      return;
    }
    int frameLocalTime = clip.startTick - readIntOr(mainFrame, "I", 0);
    FlixelArray<RawPart> tmp = new FlixelArray<>(16);
    collectKeyframeParts(parsed, mainFrame, frameLocalTime, nameToIndex, tmp);
    for (int i = 0; i < tmp.getSize(); i++) {
      RawPart p = tmp.get(i);
      FlixelFrame frame = atlas.get(p.atlasIndex);
      // Use logical (unrotated) dimensions in Flash local space regardless of atlas packing.
      float w = frame.originalWidth;
      float h = frame.originalHeight;
      accumulateTransformedCorner(p.flashMatrix, 0f, 0f, out);
      accumulateTransformedCorner(p.flashMatrix, w, 0f, out);
      accumulateTransformedCorner(p.flashMatrix, w, h, out);
      accumulateTransformedCorner(p.flashMatrix, 0f, h, out);
    }
  }

  /**
   * Returns the main-layer {@code FR} entry whose tick range covers {@code absoluteTick}, or
   * {@code null} when no FR matches (an intentional gap in the timeline).
   *
   * @param mainFrames The main-layer FR list, in declaration order.
   * @param absoluteTick The tick on the main timeline to look up.
   * @return The matching {@link FlixelJsonValue} FR, or {@code null} if no FR covers this tick.
   */
  @Nullable
  private static FlixelJsonValue findMainFrameAt(@NotNull FlixelArray<FlixelJsonValue> mainFrames, int absoluteTick) {
    for (int i = 0; i < mainFrames.getSize(); i++) {
      FlixelJsonValue fr = mainFrames.get(i);
      int frI = readIntOr(fr, "I", 0);
      int frDu = readIntOr(fr, "DU", 1);
      if (absoluteTick >= frI && absoluteTick < frI + frDu) {
        return fr;
      }
    }
    return null;
  }

  /**
   * Expands {@code out} ({@code [minX, minY, maxX, maxY]}) to include the Flash-world position of the
   * local-space point {@code (x, y)} transformed by {@code m}.
   *
   * @param m The affine transformation matrix.
   * @param x The x-coordinate of the point to transform.
   * @param y The y-coordinate of the point to transform.
   * @param out The output bounding box.
   */
  private static void accumulateTransformedCorner(@NotNull FlixelAffine m, float x, float y, @NotNull float[] out) {
    float tx = m.m00 * x + m.m01 * y + m.m02;
    float ty = m.m10 * x + m.m11 * y + m.m12;
    if (tx < out[0]) {
      out[0] = tx;
    }
    if (ty < out[1]) {
      out[1] = ty;
    }
    if (tx > out[2]) {
      out[2] = tx;
    }
    if (ty > out[3]) {
      out[3] = ty;
    }
  }

  /**
   * Resolves the root symbol referenced by the main-layer keyframe at time {@code frameTime} and recurses
   * into its timeline when {@link ParsedAnimation#usesSymbolGraph()} is {@code true}; otherwise the caller
   * collects direct {@code ASI} instances from the same {@code FR}. The output is ordered back-to-front
   * for nested rigs (layers iterated in reverse during {@link #visitSymbol}); direct timelines keep the
   * declaration order of each {@code FR}'s {@code E} array.
   *
   * @param parsed The parsed animation layout (timelines and symbol dictionary).
   * @param mainFrame The main-layer {@code FR} entry for the clip being baked. For symbol rigs it must
   *   contain an {@code SI} in its {@code E} array. For document timelines it lists {@code ASI} entries.
   * @param frameTime The clip-local tick being baked (0-indexed, less than the clip's {@code DU}).
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup.
   * @param out The list that receives one {@link RawPart} per visible bitmap. Cleared by the caller.
   */
  private void collectKeyframeParts(
      @NotNull ParsedAnimation parsed,
      @NotNull FlixelJsonValue mainFrame,
      int frameTime,
      @NotNull FlixelObjectIntMap<String> nameToIndex,
      @NotNull FlixelArray<RawPart> out) {
    FlixelJsonValue elements = mainFrame.get("E");
    if (elements == null || !elements.isArray() || elements.getSize() == 0) {
      return;
    }

    if (parsed.usesSymbolGraph) {
      // The main layer holds exactly one SI per keyframe, which is the root symbol for the nested rig.
      FlixelJsonValue firstElement = elements.get(0);
      FlixelJsonValue rootSi = firstElement != null ? firstElement.get("SI") : null;
      if (rootSi == null) {
        return;
      }
      FlixelJsonValue rootSnNode = rootSi.get("SN");
      if (rootSnNode == null) {
        return;
      }

      FlixelAffine rootMatrix = new FlixelAffine();
      matrixFromFlashMxOrM3d(rootSi.get("MX"), rootSi.get("M3D"), rootMatrix);

      // Apply the root SI's FF (first frame) and LP (loop mode) the same way visitSymbol handles
      // nested child SIs. Without this, the FF offset on the root symbol instance is ignored and
      // every clip that references the same symbol with a non-zero FF (such as danceRight when
      // FF=15) always starts sampling from frame 0 instead of the intended starting frame.
      String rootSymName = rootSnNode.asString();
      int rootFirstFrame = readIntOr(rootSi, "FF", 0);
      String rootLoopMode = readStringOr(rootSi, "LP", "loop");
      int rootSymDuration = computeSymbolDuration(parsed.symbolsByName, rootSymName);
      int rootLocalTime = computeChildLocalTime(rootLoopMode, rootFirstFrame, frameTime, rootSymDuration);

      visitSymbol(parsed.symbolsByName, nameToIndex, rootSymName, rootLocalTime, rootMatrix, out, 0);
      return;
    }

    // Bitmaps are attached directly as ASI entries (no symbol graph).
    collectDirectAsiElements(elements, nameToIndex, out);
  }

  /**
   * Collects every {@code ASI} on a main-timeline {@code FR} in JSON order. Used when Animate exported
   * the scene as flat atlas instances instead of a root {@code SI} + {@code SD.S} graph.
   *
   * @param elements The {@code E} array from a timeline {@code FR}. Must not be {@code null}.
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup.
   * @param out Receives one {@link RawPart} per {@code ASI} instance in declaration order.
   */
  private void collectDirectAsiElements(
      @NotNull FlixelJsonValue elements,
      @NotNull FlixelObjectIntMap<String> nameToIndex,
      @NotNull FlixelArray<RawPart> out) {
    for (int ei = 0; ei < elements.getSize(); ei++) {
      FlixelJsonValue element = elements.get(ei);
      FlixelJsonValue asi = element.get("ASI");
      if (asi == null) {
        continue;
      }
      FlixelJsonValue nameNode = asi.get("N");
      if (nameNode == null) {
        continue;
      }
      int atlasIndex = resolveAtlasIndex(nameNode.asString(), nameToIndex);
      if (atlasIndex < 0) {
        continue;
      }
      RawPart part = new RawPart(atlasIndex);
      matrixFromFlashMxOrM3d(asi.get("MX"), asi.get("M3D"), part.flashMatrix);
      out.add(part);
    }
  }

  /**
   * Recursively walks the timeline of the symbol named {@code symbolName} at local time {@code localTime}.
   * For each visited {@code ASI} instance, appends a {@link RawPart} to {@code out}; for each visited
   * {@code SI} instance, descends with an updated world matrix and local time.
   *
   * <p>Layers are iterated in reverse so that Flash's "first layer is on top" convention produces a
   * back-to-front list.
   *
   * @param symbolsByName The {@code SD.S} lookup keyed on {@code SN}.
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup, for resolving {@code ASI.N}.
   * @param symbolName The {@code SN} to resolve.
   * @param localTime The current symbol's timeline tick (0-indexed).
   * @param worldMatrix The accumulated Flash-world matrix down to this symbol. Not mutated by callees.
   * @param out Receives one entry per leaf {@code ASI}.
   * @param depth Current recursion depth; used only to guard against cyclic graphs.
   */
  private void visitSymbol(
      @NotNull FlixelMap<String, FlixelJsonValue> symbolsByName,
      @NotNull FlixelObjectIntMap<String> nameToIndex,
      @NotNull String symbolName,
      int localTime,
      @NotNull FlixelAffine worldMatrix,
      @NotNull FlixelArray<RawPart> out,
      int depth) {
    if (depth > MAX_NEST) {
      return;
    }
    FlixelJsonValue symbol = symbolsByName.get(symbolName);
    if (symbol == null) {
      return;
    }
    FlixelJsonValue timeline = symbol.get("TL");
    if (timeline == null) {
      return;
    }
    FlixelJsonValue layers = timeline.get("L");
    if (layers == null || !layers.isArray() || layers.getSize() == 0) {
      return;
    }

    // Walk layers back-to-front (last in JSON = deepest; first in JSON = topmost). FlixelJsonValue
    // supports random access, so no snapshot into a fixed-order array is needed.
    for (int layerIdx = layers.getSize() - 1; layerIdx >= 0; layerIdx--) {
      FlixelJsonValue layer = layers.get(layerIdx);
      FlixelJsonValue frames = layer.get("FR");
      if (frames == null || !frames.isArray()) {
        continue;
      }

      // Find the FR range that covers this layer's current tick.
      FlixelJsonValue activeFrame = null;
      int frameOffsetStart = 0;
      for (int fi = 0; fi < frames.getSize(); fi++) {
        FlixelJsonValue fr = frames.get(fi);
        int startIndex = fr.getInt("I", 0);
        int frameDuration = fr.getInt("DU", 0);
        if (localTime >= startIndex && localTime < startIndex + frameDuration) {
          activeFrame = fr;
          frameOffsetStart = startIndex;
          break;
        }
      }
      if (activeFrame == null) {
        continue;
      }
      int frameLocalTime = localTime - frameOffsetStart;

      FlixelJsonValue elements = activeFrame.get("E");
      if (elements == null || !elements.isArray()) {
        continue;
      }

      // Elements within a single FR are visited forward, so their declared order is preserved in the
      // output list. Callers use that order as within-layer z-order.
      for (int ei = 0; ei < elements.getSize(); ei++) {
        FlixelJsonValue element = elements.get(ei);
        FlixelJsonValue asi = element.get("ASI");
        if (asi != null) {
          FlixelJsonValue nameNode = asi.get("N");
          if (nameNode == null) {
            continue;
          }
          int atlasIndex = resolveAtlasIndex(nameNode.asString(), nameToIndex);
          if (atlasIndex < 0) {
            continue;
          }
          RawPart part = new RawPart(atlasIndex);
          matrixFromFlashMxOrM3d(asi.get("MX"), asi.get("M3D"), scratchMx);
          part.flashMatrix.set(worldMatrix).mul(scratchMx);
          out.add(part);
          continue;
        }

        FlixelJsonValue si = element.get("SI");
        if (si == null) {
          continue;
        }
        FlixelJsonValue childSnNode = si.get("SN");
        if (childSnNode == null) {
          continue;
        }
        String childSymbolName = childSnNode.asString();

        // FF (first frame) shifts the child's starting tick; used by looping sub-animations that begin
        // part-way through their own timeline. LP (loop parameter) controls how the child's timeline
        // is sampled when the parent's tick walks past the child's last frame:
        //   LP "loop"        -> wrap with modulo (default).
        //   LP "playonce"    -> clamp to the child's last frame.
        //   LP "singleframe" -> always show frame FF, ignoring the parent's tick entirely.
        // Adobe Animate exports these as the long form; some third-party exports use the abbreviated
        // form ("LP", "PO", "SF"). We accept both spellings.
        int firstFrame = readIntOr(si, "FF", 0);
        String loopMode = readStringOr(si, "LP", "loop");

        FlixelAffine childWorld = new FlixelAffine().set(worldMatrix);
        matrixFromFlashMxOrM3d(si.get("MX"), si.get("M3D"), scratchMx);
        childWorld.mul(scratchMx);

        int childSymDuration = computeSymbolDuration(symbolsByName, childSymbolName);
        int childLocalTime = computeChildLocalTime(loopMode, firstFrame, frameLocalTime, childSymDuration);

        // Recurse with the LP-corrected tick.
        visitSymbol(symbolsByName, nameToIndex, childSymbolName, childLocalTime, childWorld, out, depth + 1);
      }
    }
  }

  /**
   * Returns the tick that should be sampled inside a child symbol's timeline given Flash's
   * {@code LP} (loop mode) and {@code FF} (first frame) parameters on the parent's symbol instance.
   *
   * @param loopMode The {@code LP} string. Accepted spellings are {@code "loop"}/{@code "LP"} (default),
   *   {@code "playonce"}/{@code "PO"}, and {@code "singleframe"}/{@code "SF"}. Anything else is treated
   *   as {@code "loop"}.
   * @param firstFrame The {@code FF} field, the offset within the child's timeline at which this
   *   instance starts displaying.
   * @param frameLocalTime The number of ticks since the parent's enclosing {@code FR} began.
   * @param childSymDuration The total length (in ticks) of the child symbol's longest layer; assumed
   *   to be at least one. Pass the value returned by {@link #computeSymbolDuration}.
   * @return The tick to pass into the recursive {@link #visitSymbol} call.
   */
  private static int computeChildLocalTime(
      @NotNull String loopMode, int firstFrame, int frameLocalTime, int childSymDuration) {
    if (childSymDuration <= 0) {
      return Math.max(firstFrame, 0);
    }
    if (isLoopMode(loopMode, "singleframe", "SF")) {
      int t = firstFrame % childSymDuration;
      return t < 0 ? t + childSymDuration : t;
    }
    int raw = frameLocalTime + firstFrame;
    if (isLoopMode(loopMode, "playonce", "PO")) {
      if (raw < 0) {
        return 0;
      }
      return Math.min(raw, childSymDuration - 1);
    }
    int wrapped = raw % childSymDuration;
    return wrapped < 0 ? wrapped + childSymDuration : wrapped;
  }

  /**
   * Case-insensitive equality check against either spelling of an LP value.
   *
   * @param loopMode The loop mode to check.
   * @param longForm The long form of the loop mode.
   * @param shortForm The short form of the loop mode.
   * @return {@code true} if the loop mode is either the long form or the short form, {@code false} otherwise.
   */
  private static boolean isLoopMode(@NotNull String loopMode, @NotNull String longForm, @NotNull String shortForm) {
    Objects.requireNonNull(loopMode, "Loop mode cannot be null.");
    Objects.requireNonNull(longForm, "Long form cannot be null.");
    Objects.requireNonNull(shortForm, "Short form cannot be null.");
    return longForm.equalsIgnoreCase(loopMode) || shortForm.equalsIgnoreCase(loopMode);
  }

  /**
   * Returns the total timeline length (in ticks) of a symbol, computed as the max of {@code I + DU}
   * across every {@code FR} entry on every layer. The result is cached, so repeated lookups during clip
   * baking are constant-time.
   *
   * @param symbolsByName The {@code SD.S} lookup keyed on {@code SN}.
   * @param symbolName The symbol whose duration to compute.
   * @return The duration in ticks; always at least one even for empty/missing symbols, so the loop
   *   modes can divide safely.
   */
  private int computeSymbolDuration(
      @NotNull FlixelMap<String, FlixelJsonValue> symbolsByName, @NotNull String symbolName) {
    Integer cached = symbolDurations.get(symbolName);
    if (cached != null) {
      return cached;
    }
    int dur = 1;
    FlixelJsonValue symbol = symbolsByName.get(symbolName);
    if (symbol != null) {
      FlixelJsonValue timeline = symbol.get("TL");
      FlixelJsonValue layers = (timeline != null) ? timeline.get("L") : null;
      if (layers != null && layers.isArray()) {
        for (int layerIdx = 0; layerIdx < layers.getSize(); layerIdx++) {
          FlixelJsonValue frames = layers.get(layerIdx).get("FR");
          if (frames == null || !frames.isArray()) {
            continue;
          }
          for (int fi = 0; fi < frames.getSize(); fi++) {
            FlixelJsonValue fr = frames.get(fi);
            int start = readIntOr(fr, "I", 0);
            int frameDuration = readIntOr(fr, "DU", 1);
            int end = start + frameDuration;
            if (end > dur) {
              dur = end;
            }
          }
        }
      }
    }
    symbolDurations.put(symbolName, dur);
    return dur;
  }

  /**
   * Reads a numeric field from a JSON object. Returns {@code dflt} when the field is absent or not numeric.
   *
   * @param obj The JSON object to read from.
   * @param key The field name.
   * @param dflt The fallback value.
   * @return The field's integer value, or {@code dflt}.
   */
  private static int readIntOr(@NotNull FlixelJsonValue obj, @NotNull String key, int dflt) {
    FlixelJsonValue v = obj.get(key);
    if (v == null || v.getKind() != FlixelJsonValue.Kind.NUMBER) {
      return dflt;
    }
    return v.asInt();
  }

  /**
   * Reads a string field from a JSON object. Returns {@code dflt} when the field is absent or not
   * a string.
   *
   * @param obj The JSON object to read from.
   * @param key The field name.
   * @param dflt The fallback value.
   * @return The field's string value, or {@code dflt}.
   */
  @NotNull
  private static String readStringOr(@NotNull FlixelJsonValue obj, @NotNull String key, @NotNull String dflt) {
    FlixelJsonValue v = obj.get(key);
    if (v == null || v.getKind() != FlixelJsonValue.Kind.STRING) {
      return dflt;
    }
    return v.asString();
  }

  /**
   * Resolves an {@code ASI.N} reference against the {@code ATLAS.SPRITES} name map. FNF exports sometimes
   * store the name as a plain integer string (for example {@code "0"}), a zero-padded name
   * ({@code "1.000"}), or with a trailing extension; this method tries the common variants before
   * giving up.
   *
   * @param name The raw {@code ASI.N} string.
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup.
   * @return The atlas index, or {@code -1} if no variant matches.
   */
  private static int resolveAtlasIndex(@NotNull String name, @NotNull FlixelObjectIntMap<String> nameToIndex) {
    if (name.isEmpty()) {
      return -1;
    }
    int direct = nameToIndex.get(name, -1);
    if (direct != -1) {
      return direct;
    }
    int dot = name.indexOf('.');
    if (dot > 0) {
      int trimmed = nameToIndex.get(name.substring(0, dot), -1);
      if (trimmed != -1) {
        return trimmed;
      }
    }
    try {
      int asInt = (int) Double.parseDouble(name);
      int byInt = nameToIndex.get(String.valueOf(asInt), -1);
      if (byInt != -1) {
        return byInt;
      }
    } catch (NumberFormatException ignored) {
      // The original name was not numeric; nothing more to try.
    }
    return -1;
  }

  /**
   * Converts Flash's {@code MX} (six values) or {@code M3D} (16 values, column-major 4x4) into a
   * {@link FlixelAffine}. {@code M3D} is preferred when present and long enough; otherwise {@code MX} is used.
   *
   * @param mx Optional {@code MX} array ({@code [a, b, c, d, tx, ty]}).
   * @param m3d Optional {@code M3D} array (16 floats).
   * @param out Destination affine; never reallocated.
   */
  private static void matrixFromFlashMxOrM3d(@Nullable FlixelJsonValue mx, @Nullable FlixelJsonValue m3d,
      @NotNull FlixelAffine out) {
    if (m3d != null && m3d.isArray() && m3d.getSize() >= 16) {
      matrixFromFlashM3d(m3d, out);
      return;
    }
    matrixFromFlashMx(mx, out);
  }

  /**
   * Converts a column-major Flash / Animate {@code 4x4} matrix into a 2D {@link FlixelAffine}. The upper-left
   * {@code 2x2} carries scale and rotation; translation is {@code m[12], m[13]}.
   *
   * @param m3d The {@code M3D} JSON array with at least 16 entries. Must not be {@code null}.
   * @param out The destination; always written to.
   */
  private static void matrixFromFlashM3d(@NotNull FlixelJsonValue m3d, @NotNull FlixelAffine out) {
    out.m00 = m3d.get(0).asFloat();
    out.m01 = m3d.get(4).asFloat();
    out.m02 = m3d.get(12).asFloat();
    out.m10 = m3d.get(1).asFloat();
    out.m11 = m3d.get(5).asFloat();
    out.m12 = m3d.get(13).asFloat();
  }

  /**
   * Converts a Flash {@code [a, b, c, d, tx, ty]} matrix into a {@link FlixelAffine}, in-place. The
   * Flash row-major convention is unpacked into {@link FlixelAffine}'s {@code m00, m01, m02, m10, m11, m12}
   * fields without any temporary objects.
   *
   * @param mx The {@code MX} JSON array. If {@code null} or shorter than six elements, {@code out} is
   * reset to identity.
   * @param out The destination; always written to, never reallocated.
   */
  private static void matrixFromFlashMx(@Nullable FlixelJsonValue mx, @NotNull FlixelAffine out) {
    if (mx == null || !mx.isArray() || mx.getSize() < 6) {
      out.set(IDENTITY);
      return;
    }
    float a = mx.get(0).asFloat();
    float b = mx.get(1).asFloat();
    float c = mx.get(2).asFloat();
    float d = mx.get(3).asFloat();
    float tx = mx.get(4).asFloat();
    float ty = mx.get(5).asFloat();
    out.m00 = a;
    out.m01 = c;
    out.m02 = tx;
    out.m10 = b;
    out.m11 = d;
    out.m12 = ty;
  }

  /**
   * Bakes the final draw-ready affine for a single part.
   *
   * <p>Adobe Animate authors in Y-down pixel space with each bitmap's origin at its top-left corner,
   * which is exactly the space the Y-down renderer draws in. The only work left is therefore to slide
   * the anchor bounding box's top-left corner onto the sprite origin, so the result equals
   * {@code anchorShift * P_flash}, where {@code anchorShift = translate(-anchorMinX, -anchorMinY)}.
   *
   * <p>For a part that was packed rotated 90 degrees clockwise in the atlas, Adobe stores the sprite
   * sideways: the atlas footprint is {@code origH} pixels wide and {@code origW} pixels tall, so the
   * draw call produces a quad in {@code [0, origH] x [0, origW]} local space. Composing the un-rotation
   * with the identical Y-down bitmap and world spaces reduces to swapping the affine's two linear
   * columns; no dimension or sign terms survive.
   *
   * <p>Exposed as package-private so the geometry can be unit-tested without a GPU texture.
   *
   * @param out The destination affine; overwritten.
   * @param flashWorld The accumulated Flash-world matrix for this part.
   * @param rotated Whether this part was packed rotated 90 degrees clockwise in the atlas.
   * @param anchorMinX The minimum X of the anchor bounding box in Flash-world space.
   * @param anchorMinY The minimum Y of the anchor bounding box in Flash-world space.
   */
  static void bakePartAffine(
      @NotNull FlixelAffine out,
      @NotNull FlixelAffine flashWorld,
      boolean rotated,
      float anchorMinX,
      float anchorMinY) {
    float p00 = flashWorld.m00;
    float p01 = flashWorld.m01;
    float p02 = flashWorld.m02;
    float p10 = flashWorld.m10;
    float p11 = flashWorld.m11;
    float p12 = flashWorld.m12;

    if (rotated) {
      // Un-rotating the 90-degrees-CW packing in Y-down space swaps the two linear columns.
      out.m00 = p01;
      out.m01 = p00;
      out.m02 = p02 - anchorMinX;
      out.m10 = p11;
      out.m11 = p10;
      out.m12 = p12 - anchorMinY;
    } else {
      // Flash bitmap space is already Y-down with a top-left origin, so only the anchor shift remains.
      out.m00 = p00;
      out.m01 = p01;
      out.m02 = p02 - anchorMinX;
      out.m10 = p10;
      out.m11 = p11;
      out.m12 = p12 - anchorMinY;
    }
  }

  /**
   * Mutable carrier used while the loader is walking symbol timelines. Allocated per visible bitmap at
   * load time only; the draw path reads the baked {@link FlixelAnimateRig.Part} instead.
   */
  private static final class RawPart {
    final int atlasIndex;
    @NotNull
    final FlixelAffine flashMatrix = new FlixelAffine();

    RawPart(int atlasIndex) {
      this.atlasIndex = atlasIndex;
    }
  }

  /**
   * A single named animation clip pulled from any label layer. Carries the absolute start tick and
   * duration in the main timeline so the baker can sample the right main-layer {@code FR} per tick.
   *
   * @param name The name of the animation clip, never null.
   * @param startTick The absolute start tick of the clip in the main timeline.
   * @param duration The duration of the clip in ticks.
   * @see FlixelAnimateRigLoader#findMainFrameAt
   */
  private record ClipDef(
      @NotNull String name,
      int startTick,
      int duration) {
  }

  /**
   * Parsed form of the {@code AN} / optional {@code SD} / optional {@code MD} blocks. Captured in one
   * pass so the clip baker can iterate without re-walking the JSON tree.
   *
   * @param clipDefs Every named clip discovered across every non-main layer, in order of appearance. Some Adobe
   * Animate exports (like Darnell) split labels across multiple layers (one for the high-level pose
   * names and one for shorter sub-segments such as "Left Flame Loop"); the loader treats them all
   * as first-class clips so users can play either kind by name.
   *
   * <p>When the timeline has no label layers, a single clip is synthesized that spans the main
   * layer's full range (see {@link #deriveDefaultClipName}).
   * @param mainFrames Main-layer {@code FR} entries, in declaration order.
   * @param symbolsByName {@code SD.S} lookup keyed on {@code SN}. Empty for flat document exports.
   * @param framesPerSecond Authoring frame rate, copied from {@code MD.FRT} (or the 24 fps default).
   * @param usesSymbolGraph {@code true} when the main layer uses root {@code E.SI} entries and {@link #symbolsByName()} defines
   * nested timelines. {@code false} for direct {@code E.ASI} document timelines.
   */
  private record ParsedAnimation(@NotNull FlixelArray<ClipDef> clipDefs,
      @NotNull FlixelArray<FlixelJsonValue> mainFrames,
      @NotNull FlixelMap<String, FlixelJsonValue> symbolsByName, float framesPerSecond,
      boolean usesSymbolGraph) {
    @NotNull
    static ParsedAnimation parse(@NotNull FlixelJsonValue animationRoot) {
      FlixelJsonValue an = animationRoot.get("AN");
      FlixelJsonValue tl = (an != null) ? an.get("TL") : null;
      FlixelJsonValue layers = (tl != null) ? tl.get("L") : null;
      if (layers == null || !layers.isArray()) {
        throw new IllegalArgumentException("Animation JSON is missing \"AN.TL.L\".");
      }

      FlixelJsonValue mainFrameList = findSymbolRootMainFrameList(layers);
      boolean usesSymbolGraph = mainFrameList != null;
      if (mainFrameList == null) {
        mainFrameList = findDirectAsiMainFrameList(layers);
      }
      if (mainFrameList == null) {
        throw new IllegalArgumentException(
            "Animation JSON does not have a recognizable main layer. Expected either a timeline whose "
                + "keyframes use a root symbol instance (\"E\".\"SI\"), or a flat export whose keyframes "
                + "list atlas sprite instances (\"E\".\"ASI\").");
      }

      FlixelArray<ClipDef> clipDefs = new FlixelArray<>();
      for (int layerIdx = 0; layerIdx < layers.getSize(); layerIdx++) {
        FlixelJsonValue frs = layers.get(layerIdx).get("FR");
        if (frs == null || !frs.isArray() || frs == mainFrameList) {
          continue;
        }
        for (int fi = 0; fi < frs.getSize(); fi++) {
          FlixelJsonValue fr = frs.get(fi);
          FlixelJsonValue n = fr.get("N");
          if (n == null || n.getKind() != FlixelJsonValue.Kind.STRING) {
            continue;
          }
          int startTick = readIntOr(fr, "I", 0);
          int duration = readIntOr(fr, "DU", 1);
          if (duration < 1) {
            continue;
          }
          clipDefs.add(new ClipDef(n.asString(), startTick, duration));
        }
      }

      FlixelArray<FlixelJsonValue> mainFrames = new FlixelArray<>();
      for (int fi = 0; fi < mainFrameList.getSize(); fi++) {
        mainFrames.add(mainFrameList.get(fi));
      }
      if (mainFrames.getSize() == 0) {
        throw new IllegalArgumentException("Animation JSON main layer contains zero keyframes.");
      }

      if (clipDefs.getSize() == 0) {
        if (an == null) {
          throw new IllegalArgumentException(
              "Animation JSON has no named clips and no \"AN\" block to derive a default clip name from.");
        }
        clipDefs.add(
            new ClipDef(deriveDefaultClipName(an), 0, computeExclusiveTimelineEnd(mainFrameList)));
      }

      FlixelMap<String, FlixelJsonValue> symbolsByName = new FlixelMap<>();
      FlixelJsonValue sd = animationRoot.get("SD");
      if (sd != null) {
        FlixelJsonValue s = sd.get("S");
        if (s != null && s.isArray()) {
          for (int si = 0; si < s.getSize(); si++) {
            FlixelJsonValue sym = s.get(si);
            FlixelJsonValue sn = sym.get("SN");
            if (sn != null) {
              symbolsByName.put(sn.asString(), sym);
            }
          }
        }
      }

      if (usesSymbolGraph && symbolsByName.getSize() == 0) {
        throw new IllegalArgumentException(
            "Animation JSON uses root symbol instances (\"E\".\"SI\") but \"SD.S\" is empty or missing.");
      }

      float fps = 24f;
      FlixelJsonValue md = animationRoot.get("MD");
      if (md != null) {
        FlixelJsonValue frt = md.get("FRT");
        if (frt != null && frt.getKind() == FlixelJsonValue.Kind.NUMBER) {
          fps = frt.asFloat();
        }
      }

      return new ParsedAnimation(clipDefs, mainFrames, symbolsByName, fps, usesSymbolGraph);
    }

    /**
     * Chooses a beginner-friendly default clip name for document exports with no label layers.
     * Prefers {@code AN.SN}, then {@code AN.N}, then {@code "animation"}.
     *
     * @param an The {@code AN} object from the animation JSON. Must not be {@code null}.
     * @return A non-empty clip name.
     */
    @NotNull
    private static String deriveDefaultClipName(@NotNull FlixelJsonValue an) {
      FlixelJsonValue sn = an.get("SN");
      if (sn != null && sn.getKind() == FlixelJsonValue.Kind.STRING) {
        String s = sn.asString().trim();
        if (!s.isEmpty()) {
          return s;
        }
      }
      FlixelJsonValue n = an.get("N");
      if (n != null && n.getKind() == FlixelJsonValue.Kind.STRING) {
        String s = n.asString().trim();
        if (!s.isEmpty()) {
          return s;
        }
      }
      return "animation";
    }

    /**
     * Returns the exclusive end tick of the main timeline (max of every {@code I + DU}). The clip
     * duration in ticks is this value when the clip starts at {@code 0}.
     *
     * @param mainFrameList The main layer's {@code FR} array. Must not be {@code null}.
     * @return At least {@code 1}.
     */
    private static int computeExclusiveTimelineEnd(@NotNull FlixelJsonValue mainFrameList) {
      int maxExclusiveEnd = 0;
      for (int fi = 0; fi < mainFrameList.getSize(); fi++) {
        FlixelJsonValue fr = mainFrameList.get(fi);
        int i = readIntOr(fr, "I", 0);
        int du = readIntOr(fr, "DU", 1);
        int end = i + du;
        if (end > maxExclusiveEnd) {
          maxExclusiveEnd = end;
        }
      }
      return Math.max(1, maxExclusiveEnd);
    }

    /**
     * Finds the layer whose first {@code FR} begins with a root {@code E.SI}. The first matching layer
     * in declaration order wins (Better Texture Atlas / nested symbol rigs).
     *
     * @param layers The layers to search. Must not be {@code null}.
     * @return That layer's {@code FR} array, or {@code null}.
     */
    @Nullable
    private static FlixelJsonValue findSymbolRootMainFrameList(@NotNull FlixelJsonValue layers) {
      for (int layerIdx = 0; layerIdx < layers.getSize(); layerIdx++) {
        FlixelJsonValue frs = layers.get(layerIdx).get("FR");
        if (frs == null || !frs.isArray() || frs.getSize() == 0) {
          continue;
        }
        FlixelJsonValue firstFrame = frs.get(0);
        FlixelJsonValue firstE = firstFrame.get("E");
        if (firstE == null || !firstE.isArray() || firstE.getSize() == 0) {
          continue;
        }
        FlixelJsonValue firstEv = firstE.get(0);
        if (firstEv == null || firstEv.get("SI") == null) {
          continue;
        }
        return frs;
      }
      return null;
    }

    /**
     * Finds the first layer that places at least one {@code E.ASI} on the timeline, which stock Adobe
     * Animate often uses for a flat sequence of bitmaps without a symbol dictionary.
     *
     * @param layers The layers to search. Must not be {@code null}.
     * @return That layer's {@code FR} array, or {@code null}.
     */
    @Nullable
    private static FlixelJsonValue findDirectAsiMainFrameList(@NotNull FlixelJsonValue layers) {
      for (int layerIdx = 0; layerIdx < layers.getSize(); layerIdx++) {
        FlixelJsonValue frs = layers.get(layerIdx).get("FR");
        if (frs == null || !frs.isArray() || frs.getSize() == 0) {
          continue;
        }
        for (int fi = 0; fi < frs.getSize(); fi++) {
          FlixelJsonValue stageElements = frs.get(fi).get("E");
          if (stageElements == null || !stageElements.isArray()) {
            continue;
          }
          for (int ei = 0; ei < stageElements.getSize(); ei++) {
            if (stageElements.get(ei).get("ASI") != null) {
              return frs;
            }
          }
        }
      }
      return null;
    }
  }
}
