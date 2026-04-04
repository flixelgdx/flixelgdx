/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import org.jetbrains.annotations.NotNull;

/**
 * Helpers to register clips from libGDX {@link TextureAtlas} and related sources.
 */
public final class FlixelAnimationSources {

  private FlixelAnimationSources() {}

  /**
   * Adds an animation using named regions from an atlas (order preserved).
   *
   * @param controller The animation controller to add the animation to
   * @param name The name of the animation
   * @param atlas The texture atlas to get the frames from
   * @param regionNames The names of the regions to get the frames from
   * @param frameDuration Seconds per frame.
   * @param loop Whether to loop the animation
   */
  public static void addFromTextureAtlas(
    @NotNull FlixelAnimationController controller,
    @NotNull String name,
    @NotNull TextureAtlas atlas,
    @NotNull String[] regionNames,
    float frameDuration,
    boolean loop) {
    Array<FlixelFrame> frames = new Array<>(regionNames.length);
    for (String regionName : regionNames) {
      if (atlas.findRegion(regionName) != null) {
        frames.add(new FlixelFrame(atlas.findRegion(regionName)));
      }
    }
    if (frames.size == 0) {
      return;
    }
    Animation<FlixelFrame> anim =
      new Animation<>(
        frameDuration,
        frames,
        loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
    controller.getAnimations().put(name, anim);
  }
}
