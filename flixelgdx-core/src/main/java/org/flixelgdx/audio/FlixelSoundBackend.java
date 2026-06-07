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
     * <p>This is an internal hook called automatically by
     * {@link org.flixelgdx.asset.FlixelDefaultAssetManager} whenever a
     * {@link org.flixelgdx.audio.FlixelSoundSource} is enqueued on the web platform.
     * Do not call it directly; load audio through {@code Flixel.assets.load(path)} instead.
     *
     * <p>On platforms where decoding is synchronous (desktop, Android) this is a no-op.
     *
     * @param path The resolved internal asset path to pre-decode.
     */
    default void prewarmSound(String path) {}

    /**
     * Returns {@code true} while at least one {@link #prewarmSound} decode is still in progress.
     *
     * <p>On platforms where decoding is synchronous (desktop, Android) this always returns
     * {@code false}. On the web platform, {@link org.flixelgdx.asset.FlixelAssetManager#update()}
     * consults this method and continues returning {@code false} until all pending decodes
     * resolve, keeping the loading-state loop alive until audio is truly ready to play.
     *
     * @return {@code true} if one or more background decodes have not yet completed.
     */
    default boolean isPrewarmPending() {
      return false;
    }

    /**
     * Attaches a sound directly to the engine endpoint, bypassing any effect
     * chain. Used to restore direct routing after clearing effects.
     * Implementations that do not support an audio graph should no-op.
     *
     * @param sound The sound backend whose output to route.
     * @param outputBusIndex Output bus index (typically 0).
     */
    void attachToEngineOutput(FlixelSoundBackend sound, int outputBusIndex);

    /**
     * Attaches the tail effect node in a chain to the engine endpoint so that
     * processed audio reaches the output. Must be called each time a new node
     * is appended to the chain.
     * Implementations that do not support an audio graph should no-op.
     *
     * @param node The tail effect node whose output to route.
     * @param outputBusIndex Output bus index (typically 0).
     */
    void attachEffectToEngineOutput(EffectNode node, int outputBusIndex);

    /**
     * Sound-aware overload of {@link #attachEffectToEngineOutput(EffectNode, int)}.
     *
     * <p>Backends that route through sound groups (e.g. MiniAudio) should override this to
     * connect the tail node through the sound's group rather than directly to the engine
     * endpoint, preserving group-level pause and resume behavior. The default implementation
     * delegates to the two-argument form and ignores the sound.
     *
     * @param node The tail effect node whose output to route.
     * @param outputBusIndex Output bus index (typically 0).
     * @param sound The sound whose effect chain is being finalized.
     */
    default void attachEffectToEngineOutput(EffectNode node, int outputBusIndex,
        FlixelSoundBackend sound) {
      attachEffectToEngineOutput(node, outputBusIndex);
    }

    /**
     * Creates a reverb effect node.
     *
     * @param wet Wet amount in [0, 1].
     * @return A new reverb node, or a no-op stub on unsupported platforms.
     */
    ReverbNode createReverbNode(float wet);

    /**
     * Creates a delay / echo effect node.
     *
     * @param delaySeconds Delay time in seconds.
     * @param decay Decay factor for the delayed signal.
     * @return A new echo node, or a no-op stub on unsupported platforms.
     */
    EchoNode createDelayNode(float delaySeconds, float decay);

    /**
     * Creates a low-pass filter effect node.
     *
     * @param cutoffHz Cutoff frequency in hertz.
     * @param order Filter order (e.g. 2 for a second-order filter).
     * @return A new low-pass node, or a no-op stub on unsupported platforms.
     */
    LowPassNode createLowPassFilter(double cutoffHz, int order);
  }

  /**
   * An opaque handle to an audio-graph effect node (reverb, delay, low-pass, etc.).
   *
   * <p>On platforms that do not support an audio graph the returned instances are no-op stubs.
   * Typed subtypes ({@link ReverbNode}, {@link EchoNode}, {@link LowPassNode}) expose
   * live parameter setters for the effects that support them.
   */
  interface EffectNode {

    /**
     * Wires a sound source into this node's input.
     *
     * @param upstream The upstream sound backend.
     * @param bus Input bus index (typically 0).
     */
    void attachToUpstream(FlixelSoundBackend upstream, int bus);

    /**
     * Wires another effect node into this node's input, allowing effects to be
     * chained together (e.g. reverb feeding into a low-pass filter).
     *
     * @param upstream The upstream effect node.
     * @param bus Input bus index (typically 0).
     */
    void attachToUpstreamNode(EffectNode upstream, int bus);

    /**
     * Detaches this node from its input bus.
     *
     * @param bus The bus index to detach.
     */
    void detach(int bus);

    /** Releases native resources held by this effect node. */
    void dispose();
  }

  /**
   * A live-controllable reverb effect node.
   *
   * <p>All setters take effect immediately without rebuilding the audio graph.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * ReverbNode reverb = sound.addReverb(0.4f);
   * // Later, when the player enters a cave:
   * reverb.setRoomSize(0.9f);
   * reverb.setWet(0.7f);
   * }</pre>
   */
  interface ReverbNode extends EffectNode {

    /** No-op sentinel returned when no sound factory is available. */
    ReverbNode NOOP = new ReverbNode() {
      public void attachToUpstream(FlixelSoundBackend u, int b) {}

      public void attachToUpstreamNode(EffectNode u, int b) {}

      public void detach(int b) {}

      public void dispose() {}

      public void setWet(float v) {}

      public void setDry(float v) {}

      public void setRoomSize(float v) {}

      public void setDamping(float v) {}

      public void setWidth(float v) {}

      public void setFrozen(boolean v) {}
    };

    /**
     * Sets the wet (processed) signal level.
     *
     * @param wet Level in [0, 1].
     */
    void setWet(float wet);

    /**
     * Sets the dry (unprocessed) signal level.
     *
     * @param dry Level in [0, 1].
     */
    void setDry(float dry);

    /**
     * Sets the simulated room size.
     *
     * @param size Room size in [0, 1]; larger values produce longer reverb tails.
     */
    void setRoomSize(float size);

    /**
     * Sets the high-frequency damping amount.
     *
     * @param damping Damping in [0, 1]; higher values absorb treble faster.
     */
    void setDamping(float damping);

    /**
     * Sets the stereo width of the reverb tail.
     *
     * @param width Width in [0, 1]; 0 is mono, 1 is full stereo spread.
     */
    void setWidth(float width);

    /**
     * Freezes or unfreezes the reverb tail.
     *
     * <p>When frozen the tail recirculates indefinitely, producing an infinite-sustain effect.
     *
     * @param frozen {@code true} to freeze, {@code false} for normal decay.
     */
    void setFrozen(boolean frozen);
  }

  /**
   * A delay / echo effect node.
   *
   * <p>Delay time and decay are fixed at construction on platforms backed by MiniAudio.
   * To change them, dispose the current node and add a new one via
   * {@link FlixelSound#addEcho(float, float)}.
   */
  interface EchoNode extends EffectNode {

    /** No-op sentinel returned when no sound factory is available. */
    EchoNode NOOP = new EchoNode() {
      public void attachToUpstream(FlixelSoundBackend u, int b) {}

      public void attachToUpstreamNode(EffectNode u, int b) {}

      public void detach(int b) {}

      public void dispose() {}
    };
  }

  /**
   * A live-controllable low-pass filter effect node.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * LowPassNode muffle = sound.addLowPassMuffle(8000.0);
   * // Smoothly tighten the filter as the player goes deeper underground:
   * muffle.setCutoff(2000.0);
   * }</pre>
   */
  interface LowPassNode extends EffectNode {

    /** No-op sentinel returned when no sound factory is available. */
    LowPassNode NOOP = new LowPassNode() {
      public void attachToUpstream(FlixelSoundBackend u, int b) {}

      public void attachToUpstreamNode(EffectNode u, int b) {}

      public void detach(int b) {}

      public void dispose() {}

      public void setCutoff(double v) {}
    };

    /**
     * Sets the filter cutoff frequency.
     *
     * <p>Frequencies above the cutoff are progressively attenuated.
     *
     * @param hz Cutoff frequency in hertz; must be positive and below the Nyquist frequency.
     */
    void setCutoff(double hz);
  }
}
