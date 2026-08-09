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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelFrame;
import org.jetbrains.annotations.NotNull;

/**
 * Helpers to register clips from explicit frame lists.
 *
 * <p>Most games register clips through {@link FlixelAnimationController} directly (from a
 * Sparrow atlas, a spritemap, or a frame grid). This class is a small convenience for building
 * a clip from a set of {@link FlixelFrame}s you already hold, without an intermediate atlas
 * type.
 */
public final class FlixelAnimationSources {

  private FlixelAnimationSources() {}

  /**
   * Adds an animation from an ordered set of frames.
   *
   * @param controller The animation controller to add the animation to.
   * @param name The name of the animation.
   * @param frames The frames, in play order. Order is preserved.
   * @param frameDuration Seconds per frame.
   * @param loop Whether to loop the animation.
   */
  public static void addFromFrames(
      @NotNull FlixelAnimationController controller,
      @NotNull String name,
      @NotNull FlixelArray<FlixelFrame> frames,
      float frameDuration,
      boolean loop) {
    if (frames.getSize() == 0) {
      return;
    }
    FlixelAnimation<FlixelFrame> anim = new FlixelAnimation<>(
        frameDuration,
        frames,
        loop ? FlixelAnimation.PlayMode.LOOP : FlixelAnimation.PlayMode.NORMAL);
    controller.getAnimations().put(name, anim);
  }
}
