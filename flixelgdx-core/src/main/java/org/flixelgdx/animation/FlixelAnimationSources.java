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

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.graphics.FlixelFrame;
import org.jetbrains.annotations.NotNull;

/**
 * Helpers to register clips from libGDX {@link TextureAtlas} and related sources.
 */
public final class FlixelAnimationSources {

  private FlixelAnimationSources() {}

  /**
   * Adds an animation using named regions from an atlas (order preserved).
   *
   * @param controller The animation controller to add the animation to.
   * @param name The name of the animation.
   * @param atlas The texture atlas to get the frames from.
   * @param regionNames The names of the regions to get the frames from.
   * @param frameDuration Seconds per frame.
   * @param loop Whether to loop the animation.
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
