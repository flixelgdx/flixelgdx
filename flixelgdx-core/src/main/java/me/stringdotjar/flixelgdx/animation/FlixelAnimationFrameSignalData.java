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
package me.stringdotjar.flixelgdx.animation;

import me.stringdotjar.flixelgdx.graphics.FlixelFrame;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Payload for animation frame / completion signals.
 *
 * <p>Note this is a class and not a record to prevent allocation of a new object every time.
 */
public class FlixelAnimationFrameSignalData {

  @NotNull
  private String animationName;
  private int frameIndex;
  @Nullable
  private FlixelFrame frame;

  public FlixelAnimationFrameSignalData(@NotNull String animationName, int frameIndex, @Nullable FlixelFrame frame) {
    if (animationName == null) {
      throw new IllegalArgumentException("animationName");
    }
    this.animationName = animationName;
    this.frameIndex = frameIndex;
    this.frame = frame;
  }

  @NotNull
  public String getAnimationName() {
    return animationName;
  }

  public int getFrameIndex() {
    return frameIndex;
  }

  @Nullable
  public FlixelFrame getFrame() {
    return frame;
  }

  public void setAnimationName(@NotNull String animationName) {
    this.animationName = animationName;
  }

  public void setFrameIndex(int frameIndex) {
    this.frameIndex = frameIndex;
  }

  public void setFrame(@Nullable FlixelFrame frame) {
    this.frame = frame;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FlixelAnimationFrameSignalData that = (FlixelAnimationFrameSignalData) o;

    if (frameIndex != that.frameIndex) return false;
    if (!animationName.equals(that.animationName)) return false;
    return frame != null ? frame.equals(that.frame) : that.frame == null;
  }

  @Override
  public int hashCode() {
    int result = animationName.hashCode();
    result = 31 * result + frameIndex;
    result = 31 * result + (frame != null ? frame.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FlixelAnimationFrameSignalData(animationName=" + animationName +
           ", frameIndex=" + frameIndex +
           ", frame=" + frame + ")";
  }
}
