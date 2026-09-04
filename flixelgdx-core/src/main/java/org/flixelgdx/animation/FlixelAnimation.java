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
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * A time-indexed sequence of key frames: give it a running state time and it hands back the
 * frame to show.
 *
 * <p>This is the timing core under sprite animations. It knows nothing about textures; the
 * key-frame type is generic (usually {@link org.flixelgdx.graphics.FlixelFrame FlixelFrame}),
 * so rigs and other systems can animate any payload.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelAnimation<FlixelFrame> run = new FlixelAnimation<>(0.1f, runFrames);
 * run.setPlayMode(FlixelAnimation.PlayMode.LOOP);
 * FlixelFrame current = run.getKeyFrame(stateTime);
 * }</pre>
 *
 * @param <T> The key-frame payload type.
 */
public class FlixelAnimation<T> {

  private float frameDuration;

  private final T @NotNull [] keyFrames;

  @NotNull
  private PlayMode playMode = PlayMode.NORMAL;

  /**
   * Creates an animation.
   *
   * @param frameDuration Seconds each frame is shown.
   * @param keyFrames The ordered frames; the array is used as-is, not copied.
   */
  public FlixelAnimation(float frameDuration, T @NotNull [] keyFrames) {
    this.frameDuration = frameDuration;
    this.keyFrames = keyFrames;
  }

  /**
   * Creates an animation from a {@link FlixelArray}, copying its frames into a right-sized array.
   *
   * @param frameDuration Seconds each frame is shown.
   * @param keyFrames The ordered frames; copied to the exact size.
   */
  public FlixelAnimation(float frameDuration, @NotNull FlixelArray<T> keyFrames) {
    this(frameDuration, keyFrames, PlayMode.NORMAL);
  }

  /**
   * Creates an animation from a {@link FlixelArray} with an explicit play mode.
   *
   * @param frameDuration Seconds each frame is shown.
   * @param keyFrames The ordered frames; copied to the exact size.
   * @param playMode How the frames are walked over time.
   */
  public FlixelAnimation(float frameDuration, @NotNull FlixelArray<T> keyFrames, @NotNull PlayMode playMode) {
    this.frameDuration = frameDuration;
    this.keyFrames = Arrays.copyOf(keyFrames.getItems(), keyFrames.getSize());
    this.playMode = playMode;
  }

  /**
   * Returns the frame to display at the given time under the current play mode.
   *
   * @param stateTime Seconds since the animation started.
   * @return The frame to display.
   */
  public T getKeyFrame(float stateTime) {
    return keyFrames[getKeyFrameIndex(stateTime)];
  }

  /**
   * Returns the index of the frame to display at the given time.
   *
   * @param stateTime Seconds since the animation started.
   * @return The frame index in {@code [0, frameCount)}.
   */
  public int getKeyFrameIndex(float stateTime) {
    int count = keyFrames.length;
    if (count <= 1 || frameDuration <= 0f) {
      return 0;
    }
    int rawIndex = (int) (stateTime / frameDuration);
    return switch (playMode) {
      case NORMAL -> Math.min(count - 1, rawIndex);
      case REVERSED -> Math.max(count - 1 - rawIndex, 0);
      case LOOP -> rawIndex % count;
      case LOOP_REVERSED -> count - 1 - (rawIndex % count);
      case LOOP_PINGPONG -> {
        int cycle = rawIndex % (count * 2 - 2);
        yield cycle < count ? cycle : count * 2 - 2 - cycle;
      }
    };
  }

  /**
   * Returns whether a non-looping animation has shown its last frame.
   *
   * @param stateTime Seconds since the animation started.
   * @return {@code true} when past the final frame (always {@code false} for looping modes).
   */
  public boolean isAnimationFinished(float stateTime) {
    if (playMode == PlayMode.LOOP || playMode == PlayMode.LOOP_REVERSED || playMode == PlayMode.LOOP_PINGPONG) {
      return false;
    }
    return keyFrames.length - 1 < (int) (stateTime / frameDuration);
  }

  /**
   * Returns the total duration of one pass through all frames, in seconds.
   */
  public float getAnimationDuration() {
    return keyFrames.length * frameDuration;
  }

  /**
   * Returns the ordered key frames backing this animation. Treat as read-only.
   */
  public T @NotNull [] getKeyFrames() {
    return keyFrames;
  }

  /**
   * Returns the duration in seconds each frame is shown.
   */
  public float getFrameDuration() {
    return frameDuration;
  }

  /**
   * Changes the per-frame duration, effectively re-timing the animation.
   *
   * @param frameDuration Seconds each frame is shown.
   */
  public void setFrameDuration(float frameDuration) {
    this.frameDuration = frameDuration;
  }

  /**
   * Returns the current play mode.
   */
  @NotNull
  public PlayMode getPlayMode() {
    return playMode;
  }

  /**
   * Switches how the animation walks its frames.
   *
   * @param playMode The new play mode.
   */
  public void setPlayMode(@NotNull PlayMode playMode) {
    this.playMode = playMode;
  }

  /** The ways an animation can walk its frame sequence over time. */
  public enum PlayMode {
    /** Play forward once and hold the last frame. */
    NORMAL,
    /** Play backward once and hold the first frame. */
    REVERSED,
    /** Play forward and restart at the beginning forever. */
    LOOP,
    /** Play backward and restart at the end forever. */
    LOOP_REVERSED,
    /** Bounce back and forth between the first and last frames forever. */
    LOOP_PINGPONG
  }
}
