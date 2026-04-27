/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable, pre-resolved multi-part rig for an Adobe Animate (BTA/"Better Texture Atlas") character. Built by
 * {@link FlixelAnimateRigLoader} and consumed by {@link FlixelAnimateSprite}.
 *
 * <h2>Data shape</h2>
 * The rig is a dictionary of named {@link Clip clips} (for example {@code "Idle"}, {@code "Left"}). Each clip
 * holds an array of {@link Keyframe keyframes}, one per visible frame of the animation. Each keyframe holds
 * an ordered array of {@link Part parts}, which are the individual bitmap slices that must be drawn in
 * back-to-front order to produce the final composited image for that moment in time.
 *
 * <p>Each {@link Part} stores:
 * <ul>
 *   <li>An integer index into {@link #atlas} selecting which bitmap slice to draw.</li>
 *   <li>A fully baked {@link Affine2} that already contains (a) the Flash {@code MX} matrix chain from
 *   the root symbol down to the leaf bitmap, (b) a Y-axis flip that converts Adobe Animate's Y-down
 *   bitmap space into libGDX's Y-up texture-region space, and (c) an anchor translation that shifts the
 *   whole rig so the anchor-clip bounding box starts at {@code (0, 0)}.</li>
 * </ul>
 *
 * <p>Because every matrix is baked at load time, the per-frame draw path is reduced to one translate, one
 * scale, and one affine concatenation per visible bitmap.
 *
 * <h2>Anchor space</h2>
 * The <strong>anchor clip</strong> defines the bounding box used to size the
 * sprite's hitbox: parts are pre-shifted so the union of their rectangles on anchor-clip frame {@code 0}
 * fits exactly inside {@code (0, 0)} to {@code (anchorWidth, anchorHeight)}. All other clips are baked
 * in the same coordinate space, so switching clips does not move the rig relative to the sprite's hitbox.
 *
 * <h2>Merging multiple atlases</h2>
 * A rig can grow at runtime by merging additional Adobe Animate exports through
 * {@link FlixelAnimateRigLoader#append(FlixelAnimateSprite, FlixelAnimationController, String, String, String)}.
 * Appended atlases share the original rig's anchor space (preserved on the rig as
 * {@link #anchorMinX} / {@link #anchorMinY} / {@link #anchorHeight}), so a character's body stays
 * visually pinned to the same world position when game code switches between, say, an "idle" atlas
 * and a separate "miss" atlas. Frames from the appended atlas are appended to {@link #atlas} and the
 * appended clip names are added to {@link #clips}, with later entries overwriting earlier ones on
 * name collisions.
 */
public final class FlixelAnimateRig {

  /**
   * The shared atlas regions indexed by {@link Part#atlasIndex}. This is the same list installed on the
   * owning {@link me.stringdotjar.flixelgdx.FlixelSprite} through
   * {@link me.stringdotjar.flixelgdx.FlixelSprite#applySparrowAtlas}, so callers may look up a region
   * either way.
   */
  @NotNull
  public final Array<FlixelFrame> atlas;

  /** Map of clip name (for example {@code "Idle"}) to its baked {@link Clip}. */
  @NotNull
  public final ObjectMap<String, Clip> clips;

  /**
   * Name of the clip whose frame-zero bounding box defines the rig's anchor-space rectangle. Usually the
   * string {@code "Idle"} if present, otherwise the first clip declared on the label layer.
   */
  @NotNull
  public final String anchorClipName;

  /** Width in pixels of the anchor-clip bounding box. Used to size the owning sprite's hitbox. */
  public final float anchorWidth;

  /** Height in pixels of the anchor-clip bounding box. Used to size the owning sprite's hitbox. */
  public final float anchorHeight;

  /**
   * Minimum X of the anchor clip's bounding box in <strong>Flash Y-down world space</strong>. Stored
   * so that subsequent atlases merged through
   * {@link FlixelAnimateRigLoader#append(FlixelAnimateSprite, FlixelAnimationController, String, String, String)}
   * can bake their parts into the same anchor-local coordinate system as the first load. Internal
   * use only; game code should not depend on this value.
   */
  final float anchorMinX;

  /**
   * Minimum Y of the anchor clip's bounding box in <strong>Flash Y-down world space</strong>. See
   * {@link #anchorMinX} for why this is preserved on the rig.
   */
  final float anchorMinY;

  /**
   * Creates a new rig. Called by {@link FlixelAnimateRigLoader} after all parts have been baked.
   *
   * @param atlas The shared atlas region list that {@link Part#atlasIndex} indexes into. Must not be {@code null}.
   * @param clips The map of clip name to {@link Clip}. Ownership transfers to the rig. Must not be {@code null}.
   * @param anchorClipName The name of the anchor clip (for example {@code "Idle"}). Must not be {@code null}.
   * @param anchorMinX The X-coordinate of the anchor bounding box's top-left corner in Flash Y-down
   *   world space. Used by the loader to bake additional atlases into the same coordinate system.
   * @param anchorMinY The Y-coordinate of the anchor bounding box's top-left corner in Flash Y-down
   *   world space. Used by the loader to bake additional atlases into the same coordinate system.
   * @param anchorWidth The width of the anchor bounding box in pixels.
   * @param anchorHeight The height of the anchor bounding box in pixels.
   */
  FlixelAnimateRig(
      @NotNull Array<FlixelFrame> atlas,
      @NotNull ObjectMap<String, Clip> clips,
      @NotNull String anchorClipName,
      float anchorMinX,
      float anchorMinY,
      float anchorWidth,
      float anchorHeight) {
    Objects.requireNonNull(atlas, "atlas cannot be null");
    Objects.requireNonNull(clips, "clips cannot be null");
    Objects.requireNonNull(anchorClipName, "anchorClipName cannot be null");
    this.atlas = atlas;
    this.clips = clips;
    this.anchorClipName = anchorClipName;
    this.anchorMinX = anchorMinX;
    this.anchorMinY = anchorMinY;
    this.anchorWidth = anchorWidth;
    this.anchorHeight = anchorHeight;
  }

  /**
   * Looks up a clip by its label-layer name (for example {@code "Idle"}, {@code "Left Miss"}).
   *
   * @param name The clip name, matching the {@code "N"} field on the label layer of {@code Animation.json}.
   *   Must not be {@code null}.
   * @return The {@link Clip}, or {@code null} if no clip with that name exists in this rig.
   */
  @Nullable
  public Clip getClip(@NotNull String name) {
    return clips.get(name);
  }

  /**
   * Returns the live map of clip name to {@link Clip}. Callers must treat the returned map as read-only;
   * mutating it invalidates the rig.
   *
   * @return The internal clip map. Never {@code null}.
   */
  @NotNull
  public ObjectMap<String, Clip> getClips() {
    return clips;
  }

  /**
   * A single bitmap draw call inside a {@link Keyframe}. Immutable at runtime: the affine is filled
   * once by the loader and only read afterwards, so instances are safe to share.
   */
  public static final class Part {

    /** Index into {@link FlixelAnimateRig#atlas}. Always in range when the rig was built successfully. */
    public final int atlasIndex;

    /**
     * The composed affine to pass to {@link com.badlogic.gdx.graphics.g2d.SpriteBatch#draw(TextureRegion,
     * float, float, Affine2)} (after the sprite's world translate and scale are applied on top).
     *
     * <p>Mathematically this is {@code anchorShift * flipRig * P_flash * flipBitmap}, where {@code P_flash}
     * is the product of {@code MX} matrices walked from the root symbol down to the leaf bitmap.
     */
    @NotNull
    public final Affine2 local = new Affine2();

    /**
     * Creates a part with the given atlas index. The affine starts as identity and is filled by the
     * loader; the {@code final} reference is kept so callers may store preallocated parts in arrays
     * without additional null checks.
     *
     * @param atlasIndex Index into {@link FlixelAnimateRig#atlas}.
     */
    Part(int atlasIndex) {
      this.atlasIndex = atlasIndex;
    }
  }

  /**
   * A single visible frame of a clip. Parts are stored in <strong>back-to-front draw order</strong>: the
   * first element is drawn first (appears behind), the last element is drawn last (appears on top). The
   * loader has already applied Flash's "first layer in the list is on top" z-order convention by
   * iterating layers in reverse before filling this array.
   */
  public static final class Keyframe {

    /** Per-bitmap draw list, in back-to-front order. Never contains {@code null}. */
    @NotNull
    public final Part[] parts;

    /**
     * Creates a keyframe with the given parts array. Ownership transfers; the array must not be modified
     * after construction.
     *
     * @param parts Ordered list of parts to draw for this frame.
     */
    Keyframe(@NotNull Part[] parts) {
      this.parts = parts;
    }
  }

  /**
   * A named animation clip, for example {@code "Idle"} or {@code "Left Miss"}. Each index into
   * {@link #keyframes} corresponds to one tick of the main timeline at the authoring frame rate stored
   * on the parent {@link FlixelAnimateRig}'s owner.
   */
  public static final class Clip {

    /** The clip's label-layer name. Matches the key used in {@link FlixelAnimateRig#getClip}. */
    @NotNull
    public final String name;

    /** Per-tick keyframes. Length equals the label-layer {@code DU} (duration) for this clip. */
    @NotNull
    public final Keyframe[] keyframes;

    /**
     * Creates a clip with the given keyframes. Ownership transfers.
     *
     * @param name The clip name.
     * @param keyframes The ordered keyframes; length must be greater than zero.
     */
    Clip(@NotNull String name, @NotNull Keyframe[] keyframes) {
      this.name = name;
      this.keyframes = keyframes;
    }
  }
}
