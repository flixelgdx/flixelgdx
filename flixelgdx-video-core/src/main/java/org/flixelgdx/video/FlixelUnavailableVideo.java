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
package org.flixelgdx.video;

import com.badlogic.gdx.graphics.Texture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A do-nothing {@link FlixelVideo} used when video playback is unavailable.
 *
 * <p>Platform factories return this backend when their native decoder cannot be set
 * up (for example, no working VLC installation on desktop). The game keeps running;
 * the video simply never becomes ready and never provides a texture, and every
 * control call is a safe no-op. Games that must react to a missing decoder can check
 * {@link FlixelVideo#isReady()} staying {@code false}.
 */
public final class FlixelUnavailableVideo extends FlixelVideo {

  public FlixelUnavailableVideo() {
    super();
  }

  @Override
  protected void playMedia() {}

  @Override
  protected void pauseMedia() {}

  @Override
  protected void resumeMedia() {}

  @Override
  protected void stopMedia() {}

  @Override
  protected boolean isMediaPlaying() {
    return false;
  }

  @Override
  protected boolean isMediaReady() {
    return false;
  }

  @Override
  protected boolean isMediaEnded() {
    return false;
  }

  @Override
  protected float getMediaTime() {
    return 0f;
  }

  @Override
  protected void setMediaTime(float timeMs) {}

  @Override
  protected float getMediaLength() {
    return 0f;
  }

  @Override
  protected float getMediaRate() {
    return 1f;
  }

  @Override
  protected void setMediaRate(float rate) {}

  @Override
  protected boolean isMediaLooped() {
    return false;
  }

  @Override
  protected void setMediaLooped(boolean looped) {}

  @Override
  protected float getMediaVolume() {
    return 1f;
  }

  @Override
  protected void setMediaVolume(float volume) {}

  @Override
  protected void applyMediaQuality(@NotNull FlixelVideoQuality quality) {}

  @Override
  protected int getMediaVideoWidth() {
    return 0;
  }

  @Override
  protected int getMediaVideoHeight() {
    return 0;
  }

  @Nullable
  @Override
  protected Texture getMediaTexture() {
    return null;
  }

  @Override
  protected void updateMedia(float elapsed) {}

  @Override
  protected void disposeMedia() {}
}
