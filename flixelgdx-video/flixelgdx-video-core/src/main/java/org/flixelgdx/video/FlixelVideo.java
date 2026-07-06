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

import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A video that plays inside your game, drawn like any other object in a state.
 *
 * <p>{@code FlixelVideo} extends {@link IFlixelBasic}, so every implementation carries
 * the normal lifecycle flags, can be pooled, and can be added straight to a
 * {@link org.flixelgdx.FlixelState FlixelState}. Draw order follows state member order:
 * a sprite added after the video renders on top of it, exactly as with two sprites.
 *
 * <p>Create instances through {@link FlixelVideos#create(String)}, which picks the
 * platform backend registered by your launcher (libvlc on desktop, the browser's
 * decoder on the web):
 *
 * <pre>{@code
 * FlixelVideo cutscene = FlixelVideos.create("videos/intro.mp4");
 * cutscene.setSize(Flixel.getGame().getViewWidth(), Flixel.getGame().getViewHeight());
 * cutscene.setLooped(false);
 * cutscene.getOnComplete().add(v -> Flixel.switchState(new MenuState()));
 * add(cutscene);
 * cutscene.play();
 * }</pre>
 *
 * <p>All positions are in milliseconds, matching {@link org.flixelgdx.audio.FlixelSound
 * FlixelSound}. Call {@link #destroy()} when the video leaves the game for good; that
 * releases the decoder and the frame texture.
 *
 * @see FlixelVideos
 * @see FlixelVideoBackend
 */
public interface FlixelVideo extends IFlixelBasic {

  /**
   * Plays the video from the beginning.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo play();

  /**
   * Plays the video.
   *
   * @param forceRestart Should the video restart from the beginning if it is already playing?
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo play(boolean forceRestart);

  /**
   * Plays the video.
   *
   * @param forceRestart Whether to restart if the video is already playing.
   * @param startTimeMs The time to start playback at, in milliseconds.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo play(boolean forceRestart, float startTimeMs);

  /**
   * Pauses the video at its current position.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo pause();

  /**
   * Resumes from the current position after a pause.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo resume();

  /**
   * Stops the video and resets the position to the beginning.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo stop();

  /**
   * Returns whether the video is currently playing.
   *
   * @return {@code true} if the video is actively playing.
   */
  boolean isPlaying();

  /**
   * Returns whether the video has finished decoding its metadata and produced a frame.
   *
   * <p>Width, height, and length are {@code 0} until this returns {@code true}, which
   * happens shortly after the first {@link #play()}.
   *
   * @return {@code true} once the video is ready to display.
   */
  boolean isReady();

  /**
   * Returns the current playback position in milliseconds.
   *
   * @return Playback position in milliseconds.
   */
  float getTime();

  /**
   * Sets the playback position in milliseconds.
   *
   * @param timeMs The time to seek to, in milliseconds.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setTime(float timeMs);

  /**
   * Returns the total length of the video in milliseconds.
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
   * @param rate Speed multiplier; must be greater than {@code 0}. {@code 1} is normal,
   *     {@code 2} is double speed, {@code 0.5} is half speed.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setRate(float rate);

  /**
   * Returns whether this video is set to loop.
   *
   * @return {@code true} if looping is enabled.
   */
  boolean isLooped();

  /**
   * Enables or disables looping.
   *
   * @param looped {@code true} to loop, {@code false} to play once.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setLooped(boolean looped);

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
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setVolume(float volume);

  /**
   * Returns the current decode quality preset.
   *
   * @return The active quality preset.
   */
  @NotNull
  FlixelVideoQuality getQuality();

  /**
   * Sets the decode quality preset.
   *
   * <p>On the web the change applies immediately. On desktop the decoder pipeline is
   * rebuilt, so the change takes effect when playback starts or restarts; the desktop
   * backend restarts a playing video automatically and seeks back to where it was.
   *
   * @param quality The preset to apply (must not be {@code null}).
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setQuality(@NotNull FlixelVideoQuality quality);

  /**
   * Returns the width of the decoded video frame in pixels.
   *
   * @return Decoded frame width, or {@code 0} while the video is not yet ready.
   */
  int getVideoWidth();

  /**
   * Returns the height of the decoded video frame in pixels.
   *
   * @return Decoded frame height, or {@code 0} while the video is not yet ready.
   */
  int getVideoHeight();

  /**
   * Returns the texture that holds the current video frame.
   *
   * <p>Useful when you want to feed the video into your own drawing code instead of
   * relying on the default draw. The backend owns this texture; do not dispose it
   * yourself.
   *
   * @return The frame texture, or {@code null} before the first frame arrives.
   */
  @Nullable
  Texture getTexture();

  /**
   * Returns the signal dispatched once when a non-looping video reaches its end.
   *
   * @return The completion signal.
   */
  @NotNull
  FlixelSignal<Void> getOnComplete();

  /**
   * Returns whether this video auto-destroys when playback completes.
   *
   * @return {@code true} if auto-destroy is enabled.
   */
  boolean isAutoDestroy();

  /**
   * Sets whether this video auto-destroys when playback completes.
   *
   * @param autoDestroy {@code true} to destroy this video when it finishes.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setAutoDestroy(boolean autoDestroy);

  /**
   * Returns the world X position of the video's top-left corner.
   *
   * @return World X position.
   */
  float getX();

  /**
   * Returns the world Y position of the video.
   *
   * @return World Y position.
   */
  float getY();

  /**
   * Positions the video in world coordinates.
   *
   * @param x World X of the top-left corner.
   * @param y World Y of the video.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setPosition(float x, float y);

  /**
   * Returns the drawn width in pixels ({@code 0} means the decoded frame width is used).
   *
   * @return Drawn width in pixels.
   */
  float getWidth();

  /**
   * Returns the drawn height in pixels ({@code 0} means the decoded frame height is used).
   *
   * @return Drawn height in pixels.
   */
  float getHeight();

  /**
   * Sets how large the video is drawn on screen, in pixels.
   *
   * <p>This is independent of the decode {@link #setQuality(FlixelVideoQuality) quality}:
   * a HALF-quality video stretched to full screen simply looks softer.
   *
   * @param width Drawn width in pixels ({@code 0} = decoded frame width).
   * @param height Drawn height in pixels ({@code 0} = decoded frame height).
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setSize(float width, float height);

  /**
   * Returns the horizontal parallax factor ({@code 1} = follows the camera fully).
   *
   * @return Horizontal scroll factor.
   */
  float getScrollX();

  /**
   * Returns the vertical parallax factor ({@code 1} = follows the camera fully).
   *
   * @return Vertical scroll factor.
   */
  float getScrollY();

  /**
   * Sets the parallax factors, same contract as sprites. Use {@code 0, 0} to pin the
   * video to the screen regardless of camera scroll (typical for cutscenes).
   *
   * @param scrollX Horizontal scroll factor.
   * @param scrollY Vertical scroll factor.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelVideo setScrollFactor(float scrollX, float scrollY);
}
