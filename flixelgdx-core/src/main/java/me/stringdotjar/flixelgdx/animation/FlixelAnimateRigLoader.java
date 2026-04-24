/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.graphics.FlixelFrame;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loader that converts a pair of Adobe Animate (BTA) JSON files plus a spritemap PNG into a
 * {@link FlixelAnimateRig}, then installs that rig on a {@link FlixelAnimateSprite}.
 *
 * <h2>Input format</h2>
 * <p>Adobe Animate's "Texture Atlas" export (also known in the Friday Night Funkin' community as "BTA"
 * or "Better Texture Atlas") produces three files per character:
 * <ol>
 *   <li>{@code spritemap1.png} - a single packed texture holding every unique bitmap slice of the rig.</li>
 *   <li>{@code spritemap1.json} - a map describing where each named bitmap lives on the PNG
 *   ({@code ATLAS.SPRITES[].SPRITE}: {@code name}, {@code x}, {@code y}, {@code w}, {@code h}).</li>
 *   <li>{@code Animation.json} - the scene graph, composed of three top-level blocks:
 *   <ul>
 *     <li>{@code AN.TL.L} - the main timeline's list of layers. One layer contains labelled frames
 *     ({@code FR[i].N}) that name each clip along with its start index {@code I} and duration
 *     {@code DU}. Another layer contains exactly one matching {@code FR} per clip whose {@code E}
 *     element holds a root symbol instance ({@code SI}).</li>
 *     <li>{@code SD.S} - the symbol dictionary. Every named symbol ({@code "BF Head default"},
 *     {@code "bf face default"}, ...) has its own timeline of layers and frames, mirroring the
 *     {@code AN.TL.L} shape, which may in turn reference other symbols or leaf atlas sprite
 *     instances ({@code ASI}).</li>
 *     <li>{@code MD.FRT} - the authoring frame rate in frames per second.</li>
 *   </ul>
 *   </li>
 * </ol>
 *
 * <h2>Matrix convention</h2>
 * Flash stores affines as six-element arrays {@code [a, b, c, d, tx, ty]} representing
 * {@code x' = a*x + c*y + tx}, {@code y' = b*x + d*y + ty}. libGDX's {@link Affine2} uses the fields
 * {@code m00, m01, m02, m10, m11, m12} with {@code x' = m00*x + m01*y + m02},
 * {@code y' = m10*x + m11*y + m12}, so the packing is:
 * <pre>
 *   m00 = a;   m01 = c;   m02 = tx;
 *   m10 = b;   m11 = d;   m12 = ty;
 * </pre>
 *
 * <h2>Coordinate flip</h2>
 * Adobe Animate uses Y-down pixel space (the top-left of a bitmap is {@code (0, 0)}). libGDX's
 * {@link com.badlogic.gdx.graphics.g2d.SpriteBatch} draws a {@link TextureRegion} with its bottom-left
 * at the supplied local origin when a Y-up projection is active (which is the FlixelGDX default). The
 * loader bakes two Y-flips into every part so the draw path can stay a simple
 * {@code translate * scale * part}:
 * <ol>
 *   <li>A <strong>per-bitmap flip</strong> {@code [1, 0, 0; 0, -1, regionHeight]} that turns libGDX's
 *   local-space Y-up rectangle into Adobe's Y-down rectangle, applied on the <em>right</em> so that
 *   the existing {@code MX} chain keeps interpreting its input as Flash-local.</li>
 *   <li>A <strong>rig-wide flip</strong> {@code [1, 0, -anchorMinX; 0, -1, anchorMinY + anchorHeight]}
 *   that turns Flash-world coordinates back into Y-up world coordinates after all Flash matrices have
 *   been composed, and simultaneously shifts the anchor bounding box so its bottom-left corner sits at
 *   the sprite's origin.</li>
 * </ol>
 *
 * <h2>Layer z-order</h2>
 * In Flash (and in Adobe Animate's exported JSON), the <strong>first</strong> layer in {@code TL.L} is
 * drawn on top, and the last layer is drawn on the bottom. The loader walks layers in reverse so that
 * the resulting {@link FlixelAnimateRig.Keyframe#parts} array is already in back-to-front order and the
 * draw path can iterate forward without any extra bookkeeping. Elements within a single layer's
 * {@code E} array keep their declared order.
 */
final class FlixelAnimateRigLoader {

  /**
   * Maximum depth for symbol recursion. A deeper graph is almost certainly a cycle, so the loader bails
   * out rather than overflowing the Java stack.
   */
  private static final int MAX_NEST = 8;

  /** Scratch affine used by {@link #matrixFromFlashMx} to avoid allocating during parsing. */
  private final Affine2 scratchMx = new Affine2();

  /** Shared identity template for resetting {@link Affine2} instances cheaply. */
  private static final Affine2 IDENTITY = new Affine2();

  /**
   * Loads the given spritemap/animation pair, builds a fully baked {@link FlixelAnimateRig}, and installs
   * it on {@code sprite}. Equivalent to calling
   * {@link #load(FlixelAnimateSprite, FlixelAnimationController, String, String, String, String)} with a
   * {@code null} {@code anchorClipName}, which means the first clip in the timeline's label layer is used
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
   *   structural precondition (for example: the spritemap has zero sprites, {@code AN.TL.L} is missing
   *   a label or a main layer, or {@code SD.S} is empty).
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
   *   a label or a main layer, or {@code SD.S} is empty).
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

  private void loadInternal(
      @NotNull FlixelAnimateSprite sprite,
      @NotNull FlixelAnimationController controller,
      @NotNull String textureKey,
      @NotNull String spritemapJsonPath,
      @NotNull String animationJsonPath,
      @Nullable String anchorClipName) {

    // Read and parse both JSON files up-front. libGDX's JsonReader owns no file handles after this call.
    String spritemapText = FlixelSpritemapJsonLoader.readUtf8Text(
      FlixelSpritemapJsonLoader.resolveAssetPath(spritemapJsonPath));
    String animationText = FlixelSpritemapJsonLoader.readUtf8Text(
      FlixelSpritemapJsonLoader.resolveAssetPath(animationJsonPath));
    JsonValue spritemapRoot = new JsonReader().parse(spritemapText);
    JsonValue animationRoot = new JsonReader().parse(animationText);

    // Obtain the backing texture. If the asset has not been preloaded, fall back to a synchronous load.
    FlixelGraphic graphic = Flixel.ensureAssets().obtainWrapper(textureKey, FlixelGraphic.class);
    Texture texture;
    try {
      texture = graphic.requireTexture();
    } catch (IllegalStateException notLoaded) {
      texture = graphic.loadNow();
    }

    // Build the atlas region list and the "ATLAS name -> region index" lookup shared by every ASI reference.
    ObjectMap<String, Integer> nameToIndex = new ObjectMap<>();
    Array<FlixelFrame> atlas = FlixelSpritemapJsonLoader.parseAtlasSprites(spritemapRoot, texture, nameToIndex);
    if (atlas.size == 0) {
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
    int anchorClipIndex = pickAnchorClipIndex(parsed.labelFrames, anchorClipName);
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

    // Bake every clip. Each keyframe's parts are stored back-to-front already (layers iterated in reverse),
    // so the draw path can iterate forward with no extra work.
    ObjectMap<String, FlixelAnimateRig.Clip> clips = new ObjectMap<>();
    Array<RawPart> scratchRaw = new Array<>(32);
    for (int clipIndex = 0; clipIndex < parsed.labelFrames.size; clipIndex++) {
      JsonValue label = parsed.labelFrames.get(clipIndex);
      String clipName = label.get("N").asString();
      int duration = label.getInt("DU");
      if (duration < 1) {
        continue;
      }
      JsonValue mainFrame = parsed.mainFrames.get(clipIndex);

      FlixelAnimateRig.Keyframe[] kfs = new FlixelAnimateRig.Keyframe[duration];
      for (int t = 0; t < duration; t++) {
        scratchRaw.clear();
        collectKeyframeParts(parsed, mainFrame, t, nameToIndex, scratchRaw);

        FlixelAnimateRig.Part[] parts = new FlixelAnimateRig.Part[scratchRaw.size];
        for (int p = 0; p < scratchRaw.size; p++) {
          RawPart raw = scratchRaw.get(p);
          FlixelFrame frame = atlas.get(raw.atlasIndex);
          FlixelAnimateRig.Part part = new FlixelAnimateRig.Part(raw.atlasIndex);
          bakePartAffine(part.local, raw.flashMatrix, frame.getRegionHeight(), anchorMinX, anchorMinY, anchorHeight);
          parts[p] = part;
        }
        kfs[t] = new FlixelAnimateRig.Keyframe(parts);
      }
      clips.put(clipName, new FlixelAnimateRig.Clip(clipName, kfs));

      // Register the clip with the animation controller so getCurrentKeyframeIndex() advances over time.
      // The actual frame indices are irrelevant (the draw path ignores them), but libGDX's Animation
      // requires at least one entry, so feed it a duplicate of atlas[0] per tick.
      int[] dummyFrames = new int[duration];
      controller.addAnimationFromAtlas(clipName, dummyFrames, 1f / fps, true);
    }

    String resolvedAnchorName = parsed.labelFrames.get(anchorClipIndex).get("N").asString();
    FlixelAnimateRig rig = new FlixelAnimateRig(atlas, clips, resolvedAnchorName, anchorWidth, anchorHeight);
    sprite.installAnimateRig(rig);

    // applySparrowAtlas() set a default currentFrame/region on the sprite. Now that the rig is installed,
    // clear them so the rig's draw path takes over and the hitbox is sized from the anchor bbox.
    sprite.clearAnimationDisplayFrame();
    sprite.updateHitbox();

    // Start the anchor clip so the sprite has a visible pose even before game code calls playAnimation.
    controller.playAnimation(resolvedAnchorName, true, true);
  }

  /**
   * Selects which clip should define the anchor-space bounding box. When {@code requestedName} is
   * non-{@code null} and matches a clip's {@code "N"} field, that clip is chosen; otherwise the first
   * clip in {@code labelFrames} is used.
   *
   * @param labelFrames The label-layer frames as returned by {@link ParsedAnimation#parse}.
   * @param requestedName Caller-supplied anchor clip name, or {@code null} to default to the first clip.
   * @return The index into {@code labelFrames} whose {@code "N"} field is the chosen anchor clip name.
   */
  private static int pickAnchorClipIndex(
      @NotNull Array<JsonValue> labelFrames, @Nullable String requestedName) {
    if (requestedName != null && !requestedName.isEmpty()) {
      for (int i = 0; i < labelFrames.size; i++) {
        JsonValue name = labelFrames.get(i).get("N");
        if (name != null && requestedName.equals(name.asString())) {
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
   * @param anchorClipIndex The index of the anchor clip in {@code parsed.mainFrames}.
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup.
   * @param atlas The atlas frames.
   * @param out The output bounding box.
   */
  private void computeAnchorBbox(
      @NotNull ParsedAnimation parsed,
      int anchorClipIndex,
      @NotNull ObjectMap<String, Integer> nameToIndex,
      @NotNull Array<FlixelFrame> atlas,
      @NotNull float[] out) {
    JsonValue mainFrame = parsed.mainFrames.get(anchorClipIndex);
    Array<RawPart> tmp = new Array<>(16);
    collectKeyframeParts(parsed, mainFrame, 0, nameToIndex, tmp);
    for (int i = 0; i < tmp.size; i++) {
      RawPart p = tmp.get(i);
      FlixelFrame frame = atlas.get(p.atlasIndex);
      float w = frame.getRegionWidth();
      float h = frame.getRegionHeight();
      accumulateTransformedCorner(p.flashMatrix, 0f, 0f, out);
      accumulateTransformedCorner(p.flashMatrix, w, 0f, out);
      accumulateTransformedCorner(p.flashMatrix, w, h, out);
      accumulateTransformedCorner(p.flashMatrix, 0f, h, out);
    }
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
  private static void accumulateTransformedCorner(@NotNull Affine2 m, float x, float y, @NotNull float[] out) {
    float tx = m.m00 * x + m.m01 * y + m.m02;
    float ty = m.m10 * x + m.m11 * y + m.m12;
    if (tx < out[0]) out[0] = tx;
    if (ty < out[1]) out[1] = ty;
    if (tx > out[2]) out[2] = tx;
    if (ty > out[3]) out[3] = ty;
  }

  /**
   * Resolves the root symbol referenced by the main-layer keyframe at time {@code frameTime} and recurses
   * into its timeline, collecting every {@code ASI} (atlas sprite instance) encountered along the way. The
   * output is ordered back-to-front: later entries should be drawn on top.
   *
   * @param parsed The parsed animation layout (timelines and symbol dictionary).
   * @param mainFrame The main-layer {@code FR} entry for the clip being baked. Must contain an {@code SI}
   *   in its {@code E} array pointing at the clip's root symbol. Must not be {@code null}.
   * @param frameTime The clip-local tick being baked (0-indexed, less than the clip's {@code DU}).
   * @param nameToIndex The {@code ATLAS.SPRITES} name-to-index lookup.
   * @param out The list that receives one {@link RawPart} per visible bitmap. Cleared by the caller.
   */
  private void collectKeyframeParts(
      @NotNull ParsedAnimation parsed,
      @NotNull JsonValue mainFrame,
      int frameTime,
      @NotNull ObjectMap<String, Integer> nameToIndex,
      @NotNull Array<RawPart> out) {
    JsonValue elements = mainFrame.get("E");
    if (elements == null || !elements.isArray() || elements.size == 0) {
      return;
    }

    // The main layer holds exactly one SI per clip, which is the root symbol for that clip. Grab it and
    // descend with its MX as the initial world transform.
    JsonValue firstElement = elements.child;
    if (firstElement == null) {
      firstElement = elements.get(0);
    }
    JsonValue rootSi = firstElement != null ? firstElement.get("SI") : null;
    if (rootSi == null) {
      return;
    }
    JsonValue rootSnNode = rootSi.get("SN");
    if (rootSnNode == null) {
      return;
    }

    Affine2 rootMatrix = new Affine2();
    matrixFromFlashMx(rootSi.get("MX"), rootMatrix);

    visitSymbol(parsed.symbolsByName, nameToIndex, rootSnNode.asString(), frameTime, rootMatrix, out, 0);
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
      @NotNull ObjectMap<String, JsonValue> symbolsByName,
      @NotNull ObjectMap<String, Integer> nameToIndex,
      @NotNull String symbolName,
      int localTime,
      @NotNull Affine2 worldMatrix,
      @NotNull Array<RawPart> out,
      int depth) {
    if (depth > MAX_NEST) {
      return;
    }
    JsonValue symbol = symbolsByName.get(symbolName);
    if (symbol == null) {
      return;
    }
    JsonValue timeline = symbol.get("TL");
    if (timeline == null) {
      return;
    }
    JsonValue layers = timeline.get("L");
    if (layers == null || !layers.isArray() || layers.size == 0) {
      return;
    }

    // Build a fixed-order layer array so the loop can walk it in reverse. JsonValue's sibling list is a
    // singly-linked chain with no random access, so we have to snapshot it once.
    int layerCount = 0;
    for (JsonValue l = layers.child; l != null; l = l.next) {
      layerCount++;
    }
    JsonValue[] layerArr = new JsonValue[layerCount];
    int li = 0;
    for (JsonValue l = layers.child; l != null; l = l.next) {
      layerArr[li++] = l;
    }

    // Walk layers back-to-front (last in JSON = deepest; first in JSON = topmost).
    for (int layerIdx = layerArr.length - 1; layerIdx >= 0; layerIdx--) {
      JsonValue layer = layerArr[layerIdx];
      JsonValue frames = layer.get("FR");
      if (frames == null || !frames.isArray()) {
        continue;
      }

      // Find the FR range that covers this layer's current tick.
      JsonValue activeFrame = null;
      int frameOffsetStart = 0;
      for (JsonValue fr = frames.child; fr != null; fr = fr.next) {
        int startIndex = fr.getInt("I");
        int frameDuration = fr.getInt("DU");
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

      JsonValue elements = activeFrame.get("E");
      if (elements == null || !elements.isArray()) {
        continue;
      }

      // Elements within a single FR are visited forward so their declared order is preserved in the
      // output list. Callers use that order as within-layer z-order.
      for (JsonValue element = elements.child; element != null; element = element.next) {
        JsonValue asi = element.get("ASI");
        if (asi != null) {
          JsonValue nameNode = asi.get("N");
          if (nameNode == null) {
            continue;
          }
          int atlasIndex = resolveAtlasIndex(nameNode.asString(), nameToIndex);
          if (atlasIndex < 0) {
            continue;
          }
          RawPart part = new RawPart(atlasIndex);
          matrixFromFlashMx(asi.get("MX"), scratchMx);
          part.flashMatrix.set(worldMatrix).mul(scratchMx);
          out.add(part);
          continue;
        }

        JsonValue si = element.get("SI");
        if (si == null) {
          continue;
        }
        JsonValue childSnNode = si.get("SN");
        if (childSnNode == null) {
          continue;
        }

        // FF (first frame) shifts the child's starting tick; used by looping sub-animations that begin
        // part-way through their own timeline.
        int firstFrame = 0;
        JsonValue ffNode = si.get("FF");
        if (ffNode != null && ffNode.isNumber()) {
          firstFrame = ffNode.asInt();
        }

        Affine2 childWorld = new Affine2().set(worldMatrix);
        matrixFromFlashMx(si.get("MX"), scratchMx);
        childWorld.mul(scratchMx);

        int childLocalTime = frameLocalTime + firstFrame;
        int sizeBefore = out.size;
        visitSymbol(symbolsByName, nameToIndex, childSnNode.asString(), childLocalTime, childWorld, out, depth + 1);
        if (out.size == sizeBefore) {
          // The child timeline has no element at the computed local time. Fall back first to the parent's
          // raw local time and then to tick zero so a statically-posed child still renders something.
          visitSymbol(symbolsByName, nameToIndex, childSnNode.asString(), localTime, childWorld, out, depth + 1);
        }
        if (out.size == sizeBefore) {
          visitSymbol(symbolsByName, nameToIndex, childSnNode.asString(), 0, childWorld, out, depth + 1);
        }
      }
    }
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
  private static int resolveAtlasIndex(@NotNull String name, @NotNull ObjectMap<String, Integer> nameToIndex) {
    if (name.isEmpty()) {
      return -1;
    }
    Integer direct = nameToIndex.get(name);
    if (direct != null) {
      return direct;
    }
    int dot = name.indexOf('.');
    if (dot > 0) {
      Integer trimmed = nameToIndex.get(name.substring(0, dot));
      if (trimmed != null) {
        return trimmed;
      }
    }
    try {
      int asInt = (int) Double.parseDouble(name);
      Integer byInt = nameToIndex.get(String.valueOf(asInt));
      if (byInt != null) {
        return byInt;
      }
    } catch (NumberFormatException ignored) {
      // The original name was not numeric; nothing more to try.
    }
    return -1;
  }

  /**
   * Converts a Flash {@code [a, b, c, d, tx, ty]} matrix into a libGDX {@link Affine2}, in-place. The
   * Flash row-major convention is unpacked into {@link Affine2}'s {@code m00, m01, m02, m10, m11, m12}
   * fields without any temporary objects.
   *
   * @param mx The {@code MX} JSON array. If {@code null} or shorter than six elements, {@code out} is
   * reset to identity.
   * @param out The destination; always written to, never reallocated.
   */
  private static void matrixFromFlashMx(@Nullable JsonValue mx, @NotNull Affine2 out) {
    if (mx == null || !mx.isArray() || mx.size < 6) {
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
   * <p>The returned matrix equals {@code anchorShift * flipRig * P_flash * flipBitmap}, where
   * {@code P_flash} is the caller-supplied Flash-world matrix, {@code flipBitmap} converts libGDX
   * Y-up local bitmap coordinates to Flash Y-down, and {@code anchorShift * flipRig} converts the
   * resulting Flash-world point back to libGDX Y-up while sliding the anchor bounding box's
   * bottom-left corner onto the sprite origin.
   *
   * @param out The destination affine; overwritten.
   * @param flashWorld The accumulated Flash-world matrix for this part.
   * @param regionHeight The height of the part's texture region in pixels (used by {@code flipBitmap}).
   * @param anchorMinX The minimum X of the anchor bounding box in Flash-world space.
   * @param anchorMinY The minimum Y of the anchor bounding box in Flash-world space.
   * @param anchorHeight The height of the anchor bounding box.
   */
  private static void bakePartAffine(
      @NotNull Affine2 out,
      @NotNull Affine2 flashWorld,
      float regionHeight,
      float anchorMinX,
      float anchorMinY,
      float anchorHeight) {
    // flipBitmap = [1, 0, 0; 0, -1, regionHeight].
    // After flashWorld * flipBitmap, the libGDX-local top-left of the region maps to where Flash
    // expects the bitmap's top-left to sit.
    float p00 = flashWorld.m00;
    float p01 = flashWorld.m01;
    float p02 = flashWorld.m02;
    float p10 = flashWorld.m10;
    float p11 = flashWorld.m11;
    float p12 = flashWorld.m12;

    float fw00 = p00;
    float fw01 = -p01;
    float fw02 = p01 * regionHeight + p02;
    float fw10 = p10;
    float fw11 = -p11;
    float fw12 = p11 * regionHeight + p12;

    // anchorShift * flipRig = [1, 0, -anchorMinX; 0, -1, anchorMinY + anchorHeight] applied on the left.
    float shiftY = anchorMinY + anchorHeight;
    out.m00 = fw00;
    out.m01 = fw01;
    out.m02 = fw02 - anchorMinX;
    out.m10 = -fw10;
    out.m11 = -fw11;
    out.m12 = shiftY - fw12;
  }

  /**
   * Mutable carrier used while the loader is walking symbol timelines. Allocated per visible bitmap at
   * load time only; the draw path reads the baked {@link FlixelAnimateRig.Part} instead.
   */
  private static final class RawPart {
    final int atlasIndex;
    @NotNull final Affine2 flashMatrix = new Affine2();

    RawPart(int atlasIndex) {
      this.atlasIndex = atlasIndex;
    }
  }

  /**
   * Parsed form of the {@code AN} / {@code SD} / {@code MD} blocks. Captured in one pass so the clip
   * baker can iterate without re-walking the JSON tree.
   */
  private static final class ParsedAnimation {
    /** Label-layer {@code FR} entries (every entry has a {@code "N"} clip name). */
    @NotNull final Array<JsonValue> labelFrames;

    /** Main-layer {@code FR} entries, aligned one-to-one with {@link #labelFrames}. */
    @NotNull final Array<JsonValue> mainFrames;

    /** {@code SD.S} lookup keyed on {@code SN}. */
    @NotNull final ObjectMap<String, JsonValue> symbolsByName;

    /** Authoring frame rate, copied from {@code MD.FRT} (or the 24 fps default). */
    final float framesPerSecond;

    private ParsedAnimation(
        @NotNull Array<JsonValue> labelFrames,
        @NotNull Array<JsonValue> mainFrames,
        @NotNull ObjectMap<String, JsonValue> symbolsByName,
        float framesPerSecond) {
      this.labelFrames = labelFrames;
      this.mainFrames = mainFrames;
      this.symbolsByName = symbolsByName;
      this.framesPerSecond = framesPerSecond;
    }

    @NotNull
    static ParsedAnimation parse(@NotNull JsonValue animationRoot) {
      JsonValue an = animationRoot.get("AN");
      JsonValue tl = (an != null) ? an.get("TL") : null;
      JsonValue layers = (tl != null) ? tl.get("L") : null;
      if (layers == null || !layers.isArray()) {
        throw new IllegalArgumentException("Animation JSON is missing \"AN.TL.L\".");
      }

      // The label layer holds one FR per clip, each with a "N" clip name. The main layer holds the root
      // symbol instance for each clip. Flash exports commonly label these "Layer_3" and "Layer_1" but
      // third-party exporters are inconsistent, so we detect them by structure.
      JsonValue labelFrameList = findLabelFrameList(layers);
      JsonValue mainFrameList = findMainFrameList(layers);
      if (labelFrameList == null || mainFrameList == null) {
        throw new IllegalArgumentException(
          "Animation JSON does not have a recognizable label layer (FR entries with N) and main layer "
            + "(FR entries with E.SI pointing at a root symbol).");
      }

      Array<JsonValue> labelFrames = new Array<>();
      for (JsonValue fr = labelFrameList.child; fr != null; fr = fr.next) {
        if (fr.get("N") != null) {
          labelFrames.add(fr);
        }
      }
      Array<JsonValue> mainFrames = new Array<>();
      for (JsonValue fr = mainFrameList.child; fr != null; fr = fr.next) {
        mainFrames.add(fr);
      }
      if (labelFrames.size == 0) {
        throw new IllegalArgumentException("Animation JSON label layer contains zero named clips.");
      }
      if (labelFrames.size != mainFrames.size) {
        throw new IllegalArgumentException(
          "Animation JSON label layer has " + labelFrames.size + " clips but main layer has "
            + mainFrames.size + " keyframes.");
      }

      ObjectMap<String, JsonValue> symbolsByName = new ObjectMap<>();
      JsonValue sd = animationRoot.get("SD");
      if (sd != null) {
        JsonValue s = sd.get("S");
        if (s != null && s.isArray()) {
          for (JsonValue sym = s.child; sym != null; sym = sym.next) {
            JsonValue sn = sym.get("SN");
            if (sn != null) {
              symbolsByName.put(sn.asString(), sym);
            }
          }
        }
      }
      if (symbolsByName.size == 0) {
        throw new IllegalArgumentException("Animation JSON \"SD.S\" is empty or missing; rig has no symbols.");
      }

      float fps = 24f;
      JsonValue md = animationRoot.get("MD");
      if (md != null) {
        JsonValue frt = md.get("FRT");
        if (frt != null && frt.isNumber()) {
          fps = frt.asFloat();
        }
      }

      return new ParsedAnimation(labelFrames, mainFrames, symbolsByName, fps);
    }

    /**
     * Finds the layer whose {@code FR} array contains entries with {@code "N"} (clip name) fields. The
     * first matching layer in declaration order is returned.
     * 
     * @param layers The layers to search. Must not be {@code null}.
     * @return The first layer whose {@code FR} array contains entries with {@code "N"} (clip name) fields, or 
     *   {@code null} if no such layer is found.
     */
    @Nullable
    private static JsonValue findLabelFrameList(@NotNull JsonValue layers) {
      for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
        JsonValue frs = layer.get("FR");
        if (frs == null || !frs.isArray() || frs.size == 0) {
          continue;
        }
        for (JsonValue fr = frs.child; fr != null; fr = fr.next) {
          if (fr.get("N") != null) {
            return frs;
          }
        }
      }
      return null;
    }

    /**
     * Finds the layer whose first {@code FR} has an {@code E.SI} pointing at a root symbol. The first
     * matching layer in declaration order is returned.
     * 
     * @param layers The layers to search. Must not be {@code null}.
     * @return The first layer whose first {@code FR} has an {@code E.SI} pointing at a root symbol, or 
     *   {@code null} if no such layer is found.
     */
    @Nullable
    private static JsonValue findMainFrameList(@NotNull JsonValue layers) {
      for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
        JsonValue frs = layer.get("FR");
        if (frs == null || !frs.isArray() || frs.size == 0) {
          continue;
        }
        JsonValue firstFrame = frs.child;
        if (firstFrame == null) {
          continue;
        }
        JsonValue firstE = firstFrame.get("E");
        if (firstE == null || !firstE.isArray() || firstE.size == 0) {
          continue;
        }
        JsonValue firstEv = firstE.child;
        if (firstEv == null || firstEv.get("SI") == null) {
          continue;
        }
        return frs;
      }
      return null;
    }
  }
}
