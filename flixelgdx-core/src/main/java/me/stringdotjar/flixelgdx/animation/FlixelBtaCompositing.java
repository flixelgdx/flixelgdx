/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.ObjectMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pre-resolved BTA/Animate multi-part rig: each named clip holds one composited “frame”
 * (all bitmap instances) per tick, with affines in normalized character space.
 */
public final class FlixelBtaCompositing {

  @NotNull private final ObjectMap<String, NamedClip> clips;
  @NotNull private final String anchorClipName;
  public final float anchorWidth;
  public final float anchorHeight;

  public FlixelBtaCompositing(
      @NotNull ObjectMap<String, NamedClip> clips,
      @NotNull String anchorClipName,
      float anchorWidth,
      float anchorHeight) {
    this.clips = clips;
    this.anchorClipName = anchorClipName;
    this.anchorWidth = anchorWidth;
    this.anchorHeight = anchorHeight;
  }

  @Nullable
  public NamedClip getClip(@NotNull String name) {
    return clips.get(name);
  }

  @NotNull
  public String getAnchorClipName() {
    return anchorClipName;
  }

  public static final class Part {
    public final int atlasIndex;
    @NotNull public final Affine2 world = new Affine2();

    public Part(int atlasIndex) {
      this.atlasIndex = atlasIndex;
    }
  }

  public static final class Keyframe {
    @NotNull public final Part[] parts;

    public Keyframe(@NotNull Part[] parts) {
      this.parts = parts;
    }
  }

  public static final class NamedClip {
    @NotNull public final String name;
    @NotNull public final Keyframe[] keyframes;
    public final float hitboxWidth;
    public final float hitboxHeight;

    public NamedClip(
        @NotNull String name,
        @NotNull Keyframe[] keyframes,
        float hitboxWidth,
        float hitboxHeight) {
      this.name = name;
      this.keyframes = keyframes;
      this.hitboxWidth = hitboxWidth;
      this.hitboxHeight = hitboxHeight;
    }
  }
}
