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

import org.flixelgdx.FlixelSprite;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
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
  void playingAndUpdatingAClipDoesNotThrow() {
    // Build a clip from frames backed by a stub texture, so playback and updates run with no GPU.
    FlixelSprite sprite = new FlixelSprite();
    FlixelAnimationController controller = sprite.ensureAnimation();

    FlixelTexture texture = new StubTexture();
    FlixelArray<FlixelFrame> frames = new FlixelArray<>();
    for (int i = 0; i < 3; i++) {
      frames.add(new FlixelFrame(texture));
    }
    controller.getAnimations().put("test", new FlixelAnimation<>(0.1f, frames, FlixelAnimation.PlayMode.NORMAL));

    assertDoesNotThrow(() -> {
      controller.playAnimation("test", false);
      controller.update(0.05f);
      controller.update(0.5f); // advance past the end of the clip
    });
    assertNotNull(sprite.getCurrentFrame());
  }

  /** A GPU-free {@link FlixelTexture} stub so frames can exist without a graphics backend. */
  private static final class StubTexture implements FlixelTexture {

    private boolean smooth;

    @Override
    public long getHandle() {
      return 0L;
    }

    @Override
    public int getWidth() {
      return 1;
    }

    @Override
    public int getHeight() {
      return 1;
    }

    @Override
    public boolean isSmooth() {
      return smooth;
    }

    @Override
    public void setSmooth(boolean smooth) {
      this.smooth = smooth;
    }

    @Override
    public void update(int x, int y, FlixelImage image) {}

    @Override
    public void destroy() {}
  }
}
