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
package org.flixelgdx.audio;

/**
 * Platform-agnostic abstraction over a single sound instance.
 *
 * <p>On JVM platforms the default implementation wraps MiniAudio ({@code MASound});
 * on TeaVM/web the implementation falls back to libGDX {@code Gdx.audio}.
 *
 * <p>Obtain instances through {@link Factory#createSound}.
 *
 * @see Factory
 */
public interface FlixelSoundBackend {

  /** Starts or resumes playback. */
  void play();

  /** Pauses playback at the current cursor position. */
  void pause();

  /** Stops playback and resets the cursor to the beginning. */
  void stop();

  /**
   * Returns whether this sound is currently playing.
   *
   * @return {@code true} if the sound is actively playing.
   */
  boolean isPlaying();

  /**
   * Returns whether this sound has reached its end.
   *
   * @return {@code true} if the cursor is at or past the end of the stream.
   */
  boolean isEnd();

  /**
   * Returns the current volume.
   *
   * @return Volume level ({@code 0} = silent, {@code 1} = default, values above {@code 1} are allowed).
   */
  float getVolume();

  /**
   * Sets the volume.
   *
   * @param volume Volume level ({@code 0} = silent, {@code 1} = default).
   */
  void setVolume(float volume);

  /**
   * Sets the pitch multiplier.
   *
   * @param pitch Pitch multiplier. Must be greater than {@code 0}. {@code 1} = default.
   */
  void setPitch(float pitch);

  /**
   * Sets the stereo pan.
   *
   * @param pan Pan value in {@code [-1, 1]}; {@code -1} = full left, {@code 0} = center, {@code 1} = full right.
   */
  void setPan(float pan);

  /**
   * Returns the current cursor position in seconds.
   *
   * @return Playback position in seconds.
   */
  float getCursorPosition();

  /**
   * Seeks to the given position.
   *
   * @param seconds Target position in seconds.
   */
  void seekTo(float seconds);

  /**
   * Returns the total length of the sound in seconds.
   *
   * @return Duration in seconds, or 0 if unknown.
   */
  float getLength();

  /**
   * Returns whether this sound is set to loop.
   *
   * @return {@code true} if the sound will loop when it reaches the end.
   */
  boolean isLooping();

  /**
   * Enables or disables looping.
   *
   * @param looping {@code true} to loop, {@code false} to play once.
   */
  void setLooping(boolean looping);

  /**
   * Sets the 3-D position of this sound for spatial audio.
   * Implementations that do not support spatial audio should ignore this call.
   *
   * @param x X position.
   * @param y Y position.
   * @param z Z position.
   */
  void setPosition(float x, float y, float z);

  /** Releases native resources held by this sound. */
  void dispose();

  /**
   * Platform-specific factory for creating sounds, groups, and effect nodes.
   *
   * <p>One instance is injected into {@link org.flixelgdx.Flixel} at startup
   * via {@code Flixel.setSoundBackendFactory(...)}, and is shared by
   * {@link FlixelAudioManager} and {@link FlixelSound}.
   */
  interface Factory {

    /**
     * Creates a new sound from a resolved file path.
     *
     * @param path Resolved (absolute or internal) path to the audio file.
     * @param flags Implementation-specific flags (typically 0).
     * @param group A group handle previously obtained from {@link #createGroup()},
     *              or {@code null} for the default group.
     * @param external {@code true} if the path is an absolute external path.
     * @return A new backend sound instance.
     */
    FlixelSoundBackend createSound(String path, short flags, Object group, boolean external);

    /**
     * Creates a new sound group. The returned object is opaque; pass it back to
     * group methods or to {@link #createSound}.
     *
     * @return A new group handle.
     */
    Object createGroup();

    /**
     * Disposes a group previously created by {@link #createGroup()}.
     *
     * @param group The group handle to dispose.
     */
    void disposeGroup(Object group);

    /**
     * Pauses all sounds in the given group.
     *
     * @param group The group handle.
     */
    void groupPause(Object group);

    /**
     * Resumes (plays) all sounds in the given group.
     *
     * @param group The group handle.
     */
    void groupPlay(Object group);

    /**
     * Sets the global master volume for the audio engine.
     *
     * @param volume Master volume in [0, 1].
     */
    void setMasterVolume(float volume);

    /** Disposes the underlying audio engine and releases all native resources. */
    void disposeEngine();

    /**
     * Pre-decodes the audio at the given path and caches the result so the next
     * {@link #createSound} call for the same path can start playback immediately with no
     * decode lag.
     *
     * <p>On platforms that support asynchronous decoding (such as TeaVM/web) this is the
     * recommended approach for music-heavy games: call this method for every track that
     * will be played together during a loading state, wait for the loading state to
     * complete, then call {@link org.flixelgdx.audio.FlixelAudioManager#play play()} in
     * the game state. All tracks will be fully decoded by then and will start from
     * position zero in perfect sync.
     *
     * <p>On platforms where decoding is synchronous (desktop, Android) this is a no-op.
     *
     * @param path The resolved internal asset path to pre-decode.
     */
    default void prewarmSound(String path) {}

    /**
     * Attaches a sound (or effect node output) to the engine endpoint.
     * Implementations that do not support an audio graph should no-op.
     *
     * @param sound The sound backend whose output to route.
     * @param outputBusIndex Output bus index (typically 0).
     */
    void attachToEngineOutput(FlixelSoundBackend sound, int outputBusIndex);

    /**
     * Creates a reverb effect node.
     *
     * @param wet Wet amount in [0, 1].
     * @return A new effect node, or a no-op stub on unsupported platforms.
     */
    EffectNode createReverbNode(float wet);

    /**
     * Creates a delay / echo effect node.
     *
     * @param delaySeconds Delay time in seconds.
     * @param decay Decay factor for the delayed signal.
     * @return A new effect node, or a no-op stub on unsupported platforms.
     */
    EffectNode createDelayNode(float delaySeconds, float decay);

    /**
     * Creates a low-pass filter effect node.
     *
     * @param cutoffHz Cutoff frequency in hertz.
     * @param order Filter order (e.g. 2 for a second-order filter).
     * @return A new effect node, or a no-op stub on unsupported platforms.
     */
    EffectNode createLowPassFilter(double cutoffHz, int order);
  }

  /**
   * An opaque handle to an audio-graph effect node (reverb, delay, low-pass, etc.).
   *
   * <p>On platforms that do not support an audio graph (e.g. TeaVM) the returned
   * instances are no-op stubs.
   */
  interface EffectNode {

    /**
     * Wires an upstream source into this node.
     *
     * @param upstream The upstream sound or node output.
     * @param bus Input bus index (typically 0).
     */
    void attachToUpstream(FlixelSoundBackend upstream, int bus);

    /**
     * Detaches this node from its input bus.
     *
     * @param bus The bus index to detach.
     */
    void detach(int bus);

    /** Releases native resources held by this effect node. */
    void dispose();
  }
}
