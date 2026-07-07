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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for all platform video backends.
 *
 * <p>This class handles every concern shared across backends: world position, draw
 * size, scroll factors, the completion signal, auto-destroy, quality tracking, and
 * the {@link FlixelBasic} lifecycle (pooling, {@link #update(float)}, {@link #draw}).
 * Platform subclasses only supply the media-player operations through the
 * {@code protected abstract} template methods ({@link #playMedia()},
 * {@link #updateMedia(float)}, and so on) and never have to repeat the display logic.
 *
 * <p>Instances are created by {@link FlixelVideos#create(String)}, which delegates to
 * the platform factory registered by the launcher. Game code always works through the
 * {@link FlixelVideo} interface; subclassing this type is only needed when a custom
 * media pipeline is required.
 */
public abstract class FlixelBaseVideo extends FlixelBasic implements FlixelVideo {

  /** Signal dispatched once when a non-looping video reaches its end. */
  @NotNull
  public final FlixelSignal<Void> onComplete = new FlixelSignal<>();

  @NotNull
  private FlixelVideoQuality quality = FlixelVideoQuality.FULL;

  /** World X position of the top-left corner in view coordinates. */
  public float x;

  /** World Y position of the video in view coordinates. */
  public float y;

  /** Drawn width in pixels. {@code 0} (the default) draws at the decoded frame width. */
  public float width;

  /** Drawn height in pixels. {@code 0} (the default) draws at the decoded frame height. */
  public float height;

  /** Horizontal parallax factor, same contract as sprites ({@code 1} = follows the camera). */
  public float scrollX = 1f;

  /** Vertical parallax factor, same contract as sprites ({@code 1} = follows the camera). */
  public float scrollY = 1f;

  /** When {@code true}, {@link #destroy()} is called automatically when playback completes. */
  private boolean autoDestroy;

  /** Guards {@link #onComplete} so the end of a video is only announced once. */
  private boolean completed;

  // -------------------------------------------------------------------------
  // Template methods - platform backends implement these
  // -------------------------------------------------------------------------

  /** Starts the underlying media player. */
  protected abstract void playMedia();

  /** Pauses the underlying media player at the current position. */
  protected abstract void pauseMedia();

  /** Resumes the underlying media player after a pause. */
  protected abstract void resumeMedia();

  /** Stops the underlying media player and resets its position. */
  protected abstract void stopMedia();

  /**
   * Returns whether the underlying player is actively playing.
   *
   * @return {@code true} while playing.
   */
  protected abstract boolean isMediaPlaying();

  /**
   * Returns whether the underlying player has decoded its first frame.
   *
   * @return {@code true} once the stream is ready.
   */
  protected abstract boolean isMediaReady();

  /**
   * Returns whether a non-looping stream has reached its end.
   *
   * @return {@code true} once the stream finished playing.
   */
  protected abstract boolean isMediaEnded();

  /**
   * Returns the current playback position in milliseconds.
   *
   * @return Playback position in milliseconds.
   */
  protected abstract float getMediaTime();

  /**
   * Seeks to the given playback position.
   *
   * @param timeMs Target position in milliseconds.
   */
  protected abstract void setMediaTime(float timeMs);

  /**
   * Returns the total duration of the media.
   *
   * @return Duration in milliseconds, or {@code 0} if not yet known.
   */
  protected abstract float getMediaLength();

  /**
   * Returns the current playback speed multiplier.
   *
   * @return Speed multiplier; {@code 1} is normal.
   */
  protected abstract float getMediaRate();

  /**
   * Sets the playback speed multiplier.
   *
   * @param rate Speed multiplier; must be greater than {@code 0}.
   */
  protected abstract void setMediaRate(float rate);

  /**
   * Returns whether the media is set to loop automatically.
   *
   * @return {@code true} if looping is enabled.
   */
  protected abstract boolean isMediaLooped();

  /**
   * Enables or disables automatic looping.
   *
   * @param looped {@code true} to loop, {@code false} to play once.
   */
  protected abstract void setMediaLooped(boolean looped);

  /**
   * Returns the current audio volume.
   *
   * @return Volume in {@code [0, 1]}.
   */
  protected abstract float getMediaVolume();

  /**
   * Sets the audio volume.
   *
   * @param volume Volume in {@code [0, 1]}; implementations should clamp out-of-range values.
   */
  protected abstract void setMediaVolume(float volume);

  /**
   * Applies a decode quality preset to the underlying player.
   *
   * @param quality The preset to apply.
   */
  protected abstract void applyMediaQuality(@NotNull FlixelVideoQuality quality);

  /**
   * Returns the decoded frame width in pixels.
   *
   * @return Frame width, or {@code 0} while not yet ready.
   */
  protected abstract int getMediaVideoWidth();

  /**
   * Returns the decoded frame height in pixels.
   *
   * @return Frame height, or {@code 0} while not yet ready.
   */
  protected abstract int getMediaVideoHeight();

  /**
   * Returns the texture holding the most recent decoded frame.
   *
   * @return The frame texture, or {@code null} before the first frame arrives.
   */
  @Nullable
  protected abstract Texture getMediaTexture();

  /**
   * Per-frame pump; uploads decoded frames and applies deferred state changes.
   *
   * <p>Must be called on the render thread. {@link #update(float)} calls this
   * automatically after the standard lifecycle checks.
   *
   * @param elapsed Seconds since the last frame.
   */
  protected abstract void updateMedia(float elapsed);

  /** Releases all native resources held by this backend. */
  protected abstract void disposeMedia();

  // -------------------------------------------------------------------------
  // FlixelVideo interface - implemented in terms of the template methods
  // -------------------------------------------------------------------------

  @NotNull
  @Override
  public final FlixelVideo play() {
    return play(true, 0f);
  }

  @NotNull
  @Override
  public final FlixelVideo play(boolean forceRestart) {
    return play(forceRestart, 0f);
  }

  @NotNull
  @Override
  public final FlixelVideo play(boolean forceRestart, float startTimeMs) {
    completed = false;
    playMedia();
    if (forceRestart) {
      setMediaTime(startTimeMs);
    }
    return this;
  }

  @NotNull
  @Override
  public final FlixelVideo pause() {
    pauseMedia();
    return this;
  }

  @NotNull
  @Override
  public final FlixelVideo resume() {
    resumeMedia();
    return this;
  }

  @NotNull
  @Override
  public final FlixelVideo stop() {
    completed = false;
    stopMedia();
    return this;
  }

  @Override
  public final boolean isPlaying() {
    return isMediaPlaying();
  }

  @Override
  public final boolean isReady() {
    return isMediaReady();
  }

  @Override
  public void update(float elapsed) {
    if (!active || !exists) {
      return;
    }

    updateMedia(elapsed);

    if (!completed && isMediaEnded() && !isLooped()) {
      completed = true;
      onComplete.dispatch();
      if (autoDestroy) {
        destroy();
      }
    }
  }

  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!visible || !exists) {
      return;
    }
    if (!isOnDrawCamera()) {
      return;
    }
    Texture texture = getMediaTexture();
    if (texture == null) {
      return;
    }

    int videoW = getMediaVideoWidth();
    int videoH = getMediaVideoHeight();
    float drawW = width > 0f ? width : videoW;
    float drawH = height > 0f ? height : videoH;
    if (drawW <= 0f || drawH <= 0f || videoW <= 0 || videoH <= 0) {
      return;
    }

    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
    float wx = cam.worldToViewX(x, scrollX);
    float wy = cam.worldToViewY(y, scrollY);
    if (!cam.isInView(wx, wy, drawW, drawH)) {
      return;
    }

    // Source rectangle crop: decoders often pad the texture below the visible picture
    // (codec row alignment), so only the top-left videoW x videoH region is drawn.
    batch.draw(texture, wx, wy, drawW, drawH, 0, 0, videoW, videoH, false, false);
  }

  @Override
  public void destroy() {
    super.destroy();
    onComplete.clear();
    disposeMedia();
    quality = FlixelVideoQuality.FULL;
    x = 0f;
    y = 0f;
    width = 0f;
    height = 0f;
    scrollX = 1f;
    scrollY = 1f;
    autoDestroy = false;
    completed = false;
  }

  @Nullable
  @Override
  public final Texture getTexture() {
    return getMediaTexture();
  }

  @NotNull
  @Override
  public final FlixelSignal<Void> getOnComplete() {
    return onComplete;
  }

  @Override
  public final float getTime() {
    return getMediaTime();
  }

  @NotNull
  @Override
  public final FlixelVideo setTime(float timeMs) {
    completed = false;
    setMediaTime(timeMs);
    return this;
  }

  @Override
  public final float getLength() {
    return getMediaLength();
  }

  @Override
  public final float getRate() {
    return getMediaRate();
  }

  @NotNull
  @Override
  public final FlixelVideo setRate(float rate) {
    setMediaRate(rate);
    return this;
  }

  @Override
  public final boolean isLooped() {
    return isMediaLooped();
  }

  @NotNull
  @Override
  public final FlixelVideo setLooped(boolean looped) {
    setMediaLooped(looped);
    return this;
  }

  @Override
  public final float getVolume() {
    return getMediaVolume();
  }

  @NotNull
  @Override
  public final FlixelVideo setVolume(float volume) {
    setMediaVolume(volume);
    return this;
  }

  @NotNull
  @Override
  public final FlixelVideoQuality getQuality() {
    return quality;
  }

  @NotNull
  @Override
  public final FlixelVideo setQuality(@NotNull FlixelVideoQuality quality) {
    if (quality == null) {
      throw new IllegalArgumentException("Video quality cannot be null.");
    }
    this.quality = quality;
    applyMediaQuality(quality);
    return this;
  }

  @Override
  public final int getVideoWidth() {
    return getMediaVideoWidth();
  }

  @Override
  public final int getVideoHeight() {
    return getMediaVideoHeight();
  }

  @Override
  public final boolean isAutoDestroy() {
    return autoDestroy;
  }

  @NotNull
  @Override
  public final FlixelVideo setAutoDestroy(boolean autoDestroy) {
    this.autoDestroy = autoDestroy;
    return this;
  }

  @Override
  public final float getX() {
    return x;
  }

  @Override
  public final float getY() {
    return y;
  }

  @NotNull
  @Override
  public final FlixelVideo setPosition(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  @Override
  public final float getWidth() {
    return width;
  }

  @Override
  public final float getHeight() {
    return height;
  }

  @NotNull
  @Override
  public final FlixelVideo setSize(float width, float height) {
    this.width = width;
    this.height = height;
    return this;
  }

  @Override
  public final float getScrollX() {
    return scrollX;
  }

  @Override
  public final float getScrollY() {
    return scrollY;
  }

  @NotNull
  @Override
  public final FlixelVideo setScrollFactor(float scrollX, float scrollY) {
    this.scrollX = scrollX;
    this.scrollY = scrollY;
    return this;
  }
}
