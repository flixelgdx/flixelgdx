/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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
