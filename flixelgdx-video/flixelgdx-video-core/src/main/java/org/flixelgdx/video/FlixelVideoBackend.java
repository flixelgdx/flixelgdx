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

import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic abstraction over a single video player instance.
 *
 * <p>On desktop the implementation wraps libvlc through in-memory frame callbacks;
 * on the web it wraps a hidden HTML video element; on Android it wraps MediaCodec.
 * The decoder never owns a window or a DOM layer: every backend delivers frames
 * into a plain {@link Texture} so {@link FlixelVideo} can draw them through the
 * regular batch, respecting state draw order like any other object.
 *
 * <p>Texture orientation contract: row {@code 0} of the texture holds the top row
 * of the video image, the same orientation libGDX uses when uploading a
 * {@code Pixmap}. Drawing through the plain
 * {@code Batch.draw(Texture, x, y, width, height)} overload therefore shows the
 * frame upright.
 *
 * <p>Visible region contract: the texture may be taller or wider than the actual
 * picture because hardware decoders pad frame buffers for row alignment (a 1080p
 * H.264 stream typically decodes into a 1088 row buffer). The real picture always
 * occupies the top-left {@link #getVideoWidth()} x {@link #getVideoHeight()} region
 * of the texture; draw code must crop to it.
 *
 * <p>Obtain instances through {@link Factory#createVideo}, which is normally done
 * for you by {@link FlixelVideos#create(String)}.
 *
 * @see Factory
 * @see FlixelVideo
 */
public interface FlixelVideoBackend {

  /** Starts playback from the current position, or from the start on first call. */
  void play();

  /** Pauses playback at the current position. */
  void pause();

  /** Resumes playback after {@link #pause()}. */
  void resume();

  /** Stops playback and resets the position to the beginning. */
  void stop();

  /**
   * Returns whether the video is actively playing.
   *
   * @return {@code true} while playing, {@code false} when paused, stopped, or ended.
   */
  boolean isPlaying();

  /**
   * Returns whether playback reached the end of the stream.
   *
   * <p>Looping videos never report {@code true} here; they wrap around instead.
   *
   * @return {@code true} once a non-looping video finished.
   */
  boolean isEnd();

  /**
   * Returns whether the video is ready to produce frames.
   *
   * <p>Until the decoder has parsed the stream, {@link #getVideoWidth()} and
   * {@link #getVideoHeight()} return {@code 0} and {@link #getTexture()} returns
   * {@code null}. Readiness is reached shortly after the first {@link #play()}.
   *
   * @return {@code true} once stream metadata and the first frame are available.
   */
  boolean isReady();

  /**
   * Returns the current playback position.
   *
   * @return Position in milliseconds.
   */
  float getTime();

  /**
   * Seeks to the given playback position.
   *
   * @param timeMs Target position in milliseconds.
   */
  void setTime(float timeMs);

  /**
   * Returns the total length of the video.
   *
   * @return Duration in milliseconds, or {@code 0} if not yet known.
   */
  float getLength();

  /**
   * Returns the playback speed multiplier.
   *
   * @return Speed multiplier; {@code 1} is normal speed.
   */
  float getRate();

  /**
   * Sets the playback speed multiplier.
   *
   * @param rate Speed multiplier; must be greater than {@code 0}. {@code 1} is normal speed.
   */
  void setRate(float rate);

  /**
   * Returns whether the video restarts automatically when it reaches the end.
   *
   * @return {@code true} if looping is enabled.
   */
  boolean isLooping();

  /**
   * Enables or disables looping.
   *
   * @param looping {@code true} to loop, {@code false} to play once.
   */
  void setLooping(boolean looping);

  /**
   * Returns the audio volume of this video.
   *
   * @return Volume in {@code [0, 1]}.
   */
  float getVolume();

  /**
   * Sets the audio volume of this video.
   *
   * @param volume Volume in {@code [0, 1]}; values outside the range are clamped.
   */
  void setVolume(float volume);

  /**
   * Applies a decode quality preset.
   *
   * <p>Depending on the platform this may only take effect when playback (re)starts;
   * see {@link FlixelVideo#setQuality(FlixelVideoQuality)} for the user-facing
   * contract.
   *
   * @param quality The preset to apply (never {@code null}).
   */
  void setQuality(FlixelVideoQuality quality);

  /**
   * Returns the width of the decoded video in pixels.
   *
   * @return Decoded frame width, or {@code 0} while not {@link #isReady() ready}.
   */
  int getVideoWidth();

  /**
   * Returns the height of the decoded video in pixels.
   *
   * @return Decoded frame height, or {@code 0} while not {@link #isReady() ready}.
   */
  int getVideoHeight();

  /**
   * Per-frame pump that must run on the render thread.
   *
   * <p>This is where pending decoded frames are uploaded into the {@link Texture}
   * (asynchronously where the platform allows it), and where deferred state such
   * as loop restarts is applied. {@link FlixelBaseVideo#update(float)} calls this
   * automatically when the video is part of a {@link org.flixelgdx.FlixelState FlixelState}.
   */
  void update();

  /**
   * Returns the texture holding the most recent decoded frame.
   *
   * <p>The same texture instance is reused for the lifetime of the stream; only its
   * pixel contents change as new frames arrive. Do not dispose it yourself; the
   * backend owns it and releases it in {@link #dispose()}.
   *
   * @return The frame texture, or {@code null} before the first frame is available.
   */
  @Nullable
  Texture getTexture();

  /** Releases the decoder, the frame texture, and any native resources. */
  void dispose();

  /**
   * Platform-specific factory for creating video backends.
   *
   * <p>One instance is registered through
   * {@link FlixelVideos#setBackendFactory(FlixelVideoBackend.Factory)} by the
   * platform launcher (for example {@code FlixelVlcVideoHandler.install()} on
   * desktop) before any {@link FlixelVideo} is created.
   */
  interface Factory {

    /**
     * Creates a new video backend for the given path.
     *
     * @param path Resolved path to the video file. For internal assets this is an
     *     absolute filesystem path on JVM platforms and a URL path on the web.
     * @param external {@code true} if the path points outside the game's assets.
     * @return A new backend instance.
     */
    FlixelVideoBackend createVideo(String path, boolean external);
  }
}
