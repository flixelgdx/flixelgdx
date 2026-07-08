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
 * A do-nothing {@link FlixelVideoBackend} used when video playback is unavailable.
 *
 * <p>Platform factories return this backend when their native decoder cannot be set
 * up (for example, no working VLC installation on desktop). The game keeps running,
 * the video simply never becomes ready and never provides a texture, and every
 * control call is a safe no-op. Games that must react to a missing decoder can check
 * {@link FlixelVideo#isReady()} staying {@code false}.
 */
public final class FlixelUnavailableVideo implements FlixelVideoBackend {

  /** The shared instance; the backend is stateless, so one is enough for all videos. */
  @NotNull
  public static final FlixelUnavailableVideo INSTANCE = new FlixelUnavailableVideo();

  private FlixelUnavailableVideo() {}

  @Override
  public void play() {}

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public void stop() {}

  @Override
  public boolean isPlaying() {
    return false;
  }

  @Override
  public boolean isEnd() {
    return false;
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public float getTime() {
    return 0f;
  }

  @Override
  public void setTime(float timeMs) {}

  @Override
  public float getLength() {
    return 0f;
  }

  @Override
  public float getRate() {
    return 1f;
  }

  @Override
  public void setRate(float rate) {}

  @Override
  public boolean isLooping() {
    return false;
  }

  @Override
  public void setLooping(boolean looping) {}

  @Override
  public float getVolume() {
    return 1f;
  }

  @Override
  public void setVolume(float volume) {}

  @Override
  public void setQuality(@NotNull FlixelVideoQuality quality) {}

  @Override
  public int getVideoWidth() {
    return 0;
  }

  @Override
  public int getVideoHeight() {
    return 0;
  }

  @Override
  public void update() {}

  @Override
  @Nullable
  public Texture getTexture() {
    return null;
  }

  @Override
  public void dispose() {}
}
