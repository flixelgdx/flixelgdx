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
 * Default {@link FlixelVideo} implementation that wraps a platform
 * {@link FlixelVideoBackend}.
 *
 * <p>This is the class {@link FlixelVideos#create(String)} returns on every platform.
 * It extends {@link FlixelBasic} for the standard lifecycle flags and pooling support,
 * pumps the backend once per frame from {@link #update(float)}, and draws the frame
 * texture through the regular batch with camera awareness and culling.
 *
 * <p>Games normally interact with the {@link FlixelVideo} interface; subclassing this
 * type is only needed when a custom draw or update behavior is wanted on top of an
 * existing backend.
 */
public class FlixelBaseVideo extends FlixelBasic implements FlixelVideo {

  /** Signal dispatched once when a non-looping video reaches its end. */
  @NotNull
  public final FlixelSignal<Void> onComplete = new FlixelSignal<>();

  @NotNull
  private final FlixelVideoBackend video;

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

  /** When true, {@link #destroy()} is called automatically when playback completes. */
  private boolean autoDestroy;

  /** Guards {@link #onComplete} so the end of a video is only announced once. */
  private boolean completed;

  /**
   * Creates a video wrapping the given backend. Prefer {@link FlixelVideos#create(String)};
   * this exists for tests and advanced embedding.
   *
   * @param video The platform-specific video backend to wrap (must not be null).
   */
  public FlixelBaseVideo(@NotNull FlixelVideoBackend video) {
    super();
    this.video = video;
  }

  @NotNull
  @Override
  public FlixelVideo play() {
    return play(true, 0f);
  }

  @NotNull
  @Override
  public FlixelVideo play(boolean forceRestart) {
    return play(forceRestart, 0f);
  }

  @NotNull
  @Override
  public FlixelVideo play(boolean forceRestart, float startTimeMs) {
    completed = false;
    video.play();
    if (forceRestart) {
      video.setTime(startTimeMs);
    }
    return this;
  }

  @NotNull
  @Override
  public FlixelVideo pause() {
    video.pause();
    return this;
  }

  @NotNull
  @Override
  public FlixelVideo resume() {
    video.resume();
    return this;
  }

  @NotNull
  @Override
  public FlixelVideo stop() {
    completed = false;
    video.stop();
    return this;
  }

  @Override
  public boolean isPlaying() {
    return video.isPlaying();
  }

  @Override
  public boolean isReady() {
    return video.isReady();
  }

  @Override
  public void update(float elapsed) {
    if (!active || !exists) {
      return;
    }

    video.update();

    if (!completed && video.isEnd() && !video.isLooping()) {
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
    Texture texture = video.getTexture();
    if (texture == null) {
      return;
    }

    int videoW = video.getVideoWidth();
    int videoH = video.getVideoHeight();
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
    video.dispose();
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

  /**
   * Returns the underlying video backend for advanced use. Prefer the
   * {@link FlixelVideo} API when possible.
   *
   * @return The wrapped backend instance.
   */
  @NotNull
  public FlixelVideoBackend getBackend() {
    return video;
  }

  @Nullable
  @Override
  public Texture getTexture() {
    return video.getTexture();
  }

  @NotNull
  @Override
  public FlixelSignal<Void> getOnComplete() {
    return onComplete;
  }

  @Override
  public float getTime() {
    return video.getTime();
  }

  @NotNull
  @Override
  public FlixelVideo setTime(float timeMs) {
    completed = false;
    video.setTime(timeMs);
    return this;
  }

  @Override
  public float getLength() {
    return video.getLength();
  }

  @Override
  public float getRate() {
    return video.getRate();
  }

  @NotNull
  @Override
  public FlixelVideo setRate(float rate) {
    video.setRate(rate);
    return this;
  }

  @Override
  public boolean isLooped() {
    return video.isLooping();
  }

  @NotNull
  @Override
  public FlixelVideo setLooped(boolean looped) {
    video.setLooping(looped);
    return this;
  }

  @Override
  public float getVolume() {
    return video.getVolume();
  }

  @NotNull
  @Override
  public FlixelVideo setVolume(float volume) {
    video.setVolume(volume);
    return this;
  }

  @NotNull
  @Override
  public FlixelVideoQuality getQuality() {
    return quality;
  }

  @NotNull
  @Override
  public FlixelVideo setQuality(@NotNull FlixelVideoQuality quality) {
    if (quality == null) {
      throw new IllegalArgumentException("Video quality cannot be null.");
    }
    this.quality = quality;
    video.setQuality(quality);
    return this;
  }

  @Override
  public int getVideoWidth() {
    return video.getVideoWidth();
  }

  @Override
  public int getVideoHeight() {
    return video.getVideoHeight();
  }

  @Override
  public boolean isAutoDestroy() {
    return autoDestroy;
  }

  @NotNull
  @Override
  public FlixelVideo setAutoDestroy(boolean autoDestroy) {
    this.autoDestroy = autoDestroy;
    return this;
  }

  @Override
  public float getX() {
    return x;
  }

  @Override
  public float getY() {
    return y;
  }

  @NotNull
  @Override
  public FlixelVideo setPosition(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  @Override
  public float getWidth() {
    return width;
  }

  @Override
  public float getHeight() {
    return height;
  }

  @NotNull
  @Override
  public FlixelVideo setSize(float width, float height) {
    this.width = width;
    this.height = height;
    return this;
  }

  @Override
  public float getScrollX() {
    return scrollX;
  }

  @Override
  public float getScrollY() {
    return scrollY;
  }

  @NotNull
  @Override
  public FlixelVideo setScrollFactor(float scrollX, float scrollY) {
    this.scrollX = scrollX;
    this.scrollY = scrollY;
    return this;
  }
}
