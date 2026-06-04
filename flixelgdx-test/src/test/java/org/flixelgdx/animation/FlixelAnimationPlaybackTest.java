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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.FlixelSprite;
import org.flixelgdx.graphics.FlixelFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two pure playback decisions in {@link FlixelAnimationController} that back the
 * non-looping-frame and replay fixes. They are split into static helpers precisely so they can be
 * checked here without registering real clips (which would need a GPU texture).
 */
class FlixelAnimationPlaybackTest {

  // A 3-frame clip at 0.1s per frame: total duration 0.3s.

  @Test
  void finishedNonLoopingClipHoldsItsLastFrame() {
    // Past the end, a non-looping clip must stay on the last index, not snap back to the first.
    assertEquals(2, FlixelAnimationController.keyframeIndex(0.35f, 0.1f, 3, false));
    assertEquals(2, FlixelAnimationController.keyframeIndex(100f, 0.1f, 3, false));
    // Mid-clip indices are unaffected.
    assertEquals(0, FlixelAnimationController.keyframeIndex(0.05f, 0.1f, 3, false));
    assertEquals(1, FlixelAnimationController.keyframeIndex(0.15f, 0.1f, 3, false));
  }

  @Test
  void loopingClipWrapsBackIntoRange() {
    assertEquals(0, FlixelAnimationController.keyframeIndex(0.35f, 0.1f, 3, true)); // index 3 -> 0
    assertEquals(1, FlixelAnimationController.keyframeIndex(0.45f, 0.1f, 3, true)); // index 4 -> 1
  }

  @Test
  void degenerateClipsReturnZeroSafely() {
    assertEquals(0, FlixelAnimationController.keyframeIndex(1f, 0f, 3, false));
    assertEquals(0, FlixelAnimationController.keyframeIndex(1f, 0.1f, 0, false));
  }

  @Test
  void replayRulesAllowReplayingAFinishedClip() {
    // A forced call always restarts.
    assertTrue(FlixelAnimationController.shouldRestart(true, true, false));
    // Switching to a different clip restarts.
    assertTrue(FlixelAnimationController.shouldRestart(false, false, false));
    // Same clip, but it already finished: restart (this is the "can't play again" fix).
    assertTrue(FlixelAnimationController.shouldRestart(false, true, true));
    // Same clip still mid-play and not forced: leave it running.
    assertFalse(FlixelAnimationController.shouldRestart(false, true, false));
  }

  @Test
  void playingAndUpdatingAnObjectBackedClipDoesNotThrow() {
    // Clips registered through addAnimationByPrefix use a default libGDX Array, whose backing store
    // is an Object[]. getKeyFrames() is typed FlixelFrame[], so casting the whole array (rather than
    // each element) throws a ClassCastException at runtime. Empty TextureRegions keep this GPU-free.
    FlixelSprite sprite = new FlixelSprite();
    FlixelAnimationController controller = sprite.ensureAnimation();

    Array<FlixelFrame> frames = new Array<>();
    for (int i = 0; i < 3; i++) {
      frames.add(new FlixelFrame(new TextureRegion()));
    }
    controller.getAnimations().put("test", new Animation<>(0.1f, frames, Animation.PlayMode.NORMAL));

    assertDoesNotThrow(() -> {
      controller.playAnimation("test", false);
      controller.update(0.05f);
      controller.update(0.5f); // advance past the end of the clip
    });
    assertNotNull(sprite.getCurrentFrame());
  }
}
