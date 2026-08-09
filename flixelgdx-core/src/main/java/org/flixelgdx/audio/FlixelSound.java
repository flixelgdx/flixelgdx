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

import org.flixelgdx.FlixelBasic;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.tween.settings.FlixelTweenType;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One playable sound instance, implemented by the platform's audio backend.
 *
 * <p>Provides volume, pitch, pan, play/pause/stop/resume, fade-in/fade-out,
 * position (time), optional audio-graph effects ({@link #addReverb},
 * {@link #addEcho}, {@link #addLowPassMuffle}, {@link #attachCustomNode}),
 * and an {@link #onComplete} signal when the sound finishes (for non-looping sounds).
 *
 * <p>Backends (miniaudio on native platforms, the Web Audio API on web) extend this class and
 * fill in the small set of {@code backend*} primitives; all the gameplay-facing behavior
 * (fades, completion signals, effect-chain bookkeeping, persistence rules) lives here so it
 * works identically on every platform. Obtain instances from
 * {@link FlixelSoundManager#play Flixel.sound.play(...)},
 * {@link FlixelSoundManager#playMusic Flixel.sound.playMusic(...)}, or the non-playing
 * {@link FlixelSoundManager#create Flixel.sound.create(...)} escape hatch.
 *
 * <p>The effect methods return typed node handles rather than {@code this}, so holding
 * the returned reference lets you modify parameters live without rebuilding the effect chain.
 *
 * <p>This class implements {@link FlixelAsset}{@code <FlixelSound>} for a refcount contract:
 * each instance {@link #retain()}s once on construction, and {@link #destroy()}
 * {@link #release()}s to balance it. Use extra {@link #retain()} / {@link #release()} for
 * advanced sharing. {@link #isPersist() persist} controls state-switch behavior.
 */
public abstract class FlixelSound extends FlixelBasic implements FlixelAsset<FlixelSound> {

  private static final float SEC_TO_MS = 1000f;
  private static final float MS_TO_SEC = 1f / SEC_TO_MS;

  @NotNull
  private final String path;

  @Nullable
  private FlixelSoundManager manager;

  @Nullable
  private FlixelAsset<FlixelSoundSource> sourceAsset;

  private int refCount;

  /** Cached pitch (some backends have no pitch getter). */
  private float pitch = 1f;

  /** Cached pan (some backends have no pan getter). */
  private float pan = 0f;

  /** World x position for proximity/panning. */
  private float x;

  /** World y position for proximity/panning. */
  private float y;

  /** If set, playback stops at this position in milliseconds. */
  @Nullable
  private Float endTimeMs;

  /** Current fade tween, so it can be canceled when starting a new fade. */
  @Nullable
  private FlixelTween fadeTween;

  /** Tail-ordered effect nodes attached to the audio graph. */
  private final FlixelArray<FlixelSoundEffect> audioEffectNodes = new FlixelArray<>(4);

  /** Signal dispatched when the sound reaches its end (non-looping). */
  @NotNull
  public final FlixelSignal<Void> onComplete = new FlixelSignal<>();

  /** When true, {@link #destroy()} is called when the sound finishes (non-looping). */
  private boolean autoDestroy;

  /** When true, this sound is not automatically destroyed on state switch. */
  private boolean persist;

  /** Guards {@link #onComplete} so it fires at most once per play session. */
  private boolean completeFired;

  /**
   * Creates a new sound. Backends call this from their constructor.
   */
  protected FlixelSound() {
    super();
    this.path = "__flixel_sound__/" + ID;
    retain();
  }

  /**
   * Returns the manager that {@code this} sound is a member of.
   *
   * @return The manager, or {@code null} if not assigned to one.
   */
  @Nullable
  public FlixelSoundManager getManager() {
    return manager;
  }

  /**
   * Sets the manager that {@code this} sound is a member of.
   *
   * @param manager The manager to set.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound setManager(@Nullable FlixelSoundManager manager) {
    this.manager = manager;
    return this;
  }

  /**
   * Attaches the backing {@link FlixelAsset} handle for the {@link FlixelSoundSource} that was
   * retained when this sound was created through {@link FlixelSoundManager}. The handle is
   * released in {@link #destroy()} so the source asset is eligible for cleanup according to the
   * active {@link org.flixelgdx.asset.FlixelAssetMode FlixelAssetMode}.
   *
   * @param sourceAsset The retained source handle, or {@code null} to clear it.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound setSourceAsset(@Nullable FlixelAsset<FlixelSoundSource> sourceAsset) {
    this.sourceAsset = sourceAsset;
    return this;
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  /** Returns {@code this}, since the sound is its own handle. */
  @NotNull
  @Override
  public FlixelSound get() {
    return this;
  }

  @Override
  public boolean isLoaded() {
    return true;
  }

  /** Returns whether this sound's audio data has been loaded. */
  public boolean getLoaded() {
    return true;
  }

  @Override
  public int getRefCount() {
    return refCount;
  }

  @NotNull
  @Override
  public FlixelSound retain() {
    refCount++;
    return this;
  }

  @NotNull
  @Override
  public FlixelSound release() {
    if (refCount <= 0) {
      refCount = 0;
      return this;
    }
    refCount--;
    return this;
  }

  /**
   * Returns the current volume.
   *
   * @return Volume level (0 = silent, 1 = default, values above 1 are allowed).
   */
  public float getVolume() {
    return backendGetVolume();
  }

  /**
   * Sets the volume.
   *
   * @param volume Volume level (0 = silent, 1 = default, values above 1 amplify).
   * @return {@code this} for chaining.
   */
  public FlixelSound setVolume(float volume) {
    backendSetVolume(volume);
    return this;
  }

  /**
   * Returns the cached pitch multiplier.
   *
   * @return Pitch multiplier; 1 = default, values above 1 raise pitch.
   */
  public float getPitch() {
    return pitch;
  }

  /**
   * Sets the pitch multiplier.
   *
   * @param pitch Pitch value; must be greater than 0.
   * @return {@code this} for chaining.
   */
  public FlixelSound setPitch(float pitch) {
    this.pitch = pitch;
    backendSetPitch(pitch);
    return this;
  }

  /**
   * Returns the cached pan value.
   *
   * @return Pan in [-1, 1]; -1 = left, 0 = center, 1 = right.
   */
  public float getPan() {
    return pan;
  }

  /**
   * Sets the stereo pan.
   *
   * @param pan Pan value in [-1, 1].
   * @return {@code this} for chaining.
   */
  public FlixelSound setPan(float pan) {
    this.pan = pan;
    backendSetPan(pan);
    return this;
  }

  /**
   * Returns the current playback position in milliseconds.
   *
   * <p>If set while paused, the change takes effect after {@link #resume()}.
   *
   * @return Playback position in milliseconds.
   */
  public float getTime() {
    return backendGetCursor() * SEC_TO_MS;
  }

  /**
   * Sets the playback position in milliseconds.
   *
   * @param timeMs The time to set the playback position to in milliseconds.
   * @return {@code this} for chaining.
   */
  public FlixelSound setTime(float timeMs) {
    backendSeek(timeMs * MS_TO_SEC);
    return this;
  }

  /**
   * Returns the total length of the sound in milliseconds.
   *
   * @return Duration in milliseconds, or 0 if unknown.
   */
  public float getLength() {
    return backendGetLength() * SEC_TO_MS;
  }

  /**
   * Returns whether this sound is set to loop.
   *
   * @return {@code true} if looping is enabled.
   */
  public boolean isLooped() {
    return backendIsLooping();
  }

  /** Returns whether this sound is set to loop. */
  public boolean getLooped() {
    return backendIsLooping();
  }

  /**
   * Enables or disables looping.
   *
   * @param looped {@code true} to loop, {@code false} to play once.
   * @return {@code this} for chaining.
   */
  public FlixelSound setLooped(boolean looped) {
    backendSetLooping(looped);
    return this;
  }

  /**
   * Returns whether this sound is currently playing.
   *
   * @return {@code true} if the sound is actively playing.
   */
  public boolean isPlaying() {
    return backendIsPlaying();
  }

  /** Returns whether this sound is currently playing. */
  public boolean getPlaying() {
    return backendIsPlaying();
  }

  /**
   * Plays the sound from the beginning.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound play() {
    return play(true, 0f);
  }

  /**
   * Plays the sound.
   *
   * @param forceRestart Should the sound be restarted if it is already playing?
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound play(boolean forceRestart) {
    return play(forceRestart, 0f);
  }

  /**
   * Plays the sound.
   *
   * @param forceRestart Whether to restart the sound if it is already playing.
   * @param startTimeMs The time to start the sound at in milliseconds.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound play(boolean forceRestart, float startTimeMs) {
    cancelFadeTween();
    completeFired = false;
    if (forceRestart) {
      setTime(startTimeMs);
    }
    backendPlay();
    return this;
  }

  /**
   * Pauses the sound at its current position.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound pause() {
    backendPause();
    return this;
  }

  /**
   * Stops the sound and resets position to 0.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound stop() {
    cancelFadeTween();
    backendStop();
    return this;
  }

  /**
   * Resumes from the current position after a pause.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound resume() {
    backendPlay();
    return this;
  }

  /**
   * Returns the position (in milliseconds) at which playback will stop, or
   * {@code null} if the sound will play to the end.
   *
   * @return End time in milliseconds, or {@code null}.
   */
  @Nullable
  public Float getEndTime() {
    return endTimeMs;
  }

  /**
   * Sets the position (ms) at which to stop. {@code null} means play to the end.
   *
   * @param endTimeMs End time in milliseconds, or {@code null}.
   * @return {@code this} for chaining.
   */
  public FlixelSound setEndTime(@Nullable Float endTimeMs) {
    this.endTimeMs = endTimeMs;
    return this;
  }

  /**
   * Fades in from 0 to 1 over the given duration (seconds).
   *
   * @param durationSeconds Fade duration in seconds.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound fadeIn(float durationSeconds) {
    return fadeIn(durationSeconds, 0f, 1f);
  }

  /**
   * Fades volume from {@code from} to {@code to} over {@code durationSeconds}.
   *
   * @param durationSeconds Fade duration in seconds.
   * @param from Start volume.
   * @param to End volume.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound fadeIn(float durationSeconds, float from, float to) {
    cancelFadeTween();
    setVolume(from);
    FlixelTweenSettings settings = new FlixelTweenSettings(FlixelTweenType.ONESHOT)
        .setDuration(durationSeconds)
        .addGoal(this::getVolume, to, this::setVolume);
    fadeTween = FlixelTween.tween(this, settings);
    return this;
  }

  /**
   * Fades out to 0 over the given duration (seconds).
   *
   * @param durationSeconds Fade duration in seconds.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound fadeOut(float durationSeconds) {
    return fadeOut(durationSeconds, 0f);
  }

  /**
   * Fades volume to {@code to} over {@code durationSeconds}.
   *
   * @param durationSeconds Fade duration in seconds.
   * @param to Target volume (typically 0).
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound fadeOut(float durationSeconds, float to) {
    cancelFadeTween();
    FlixelTweenSettings settings = new FlixelTweenSettings(FlixelTweenType.ONESHOT)
        .setDuration(durationSeconds)
        .addGoal(this::getVolume, to, this::setVolume);
    fadeTween = FlixelTween.tween(this, settings);
    return this;
  }

  /**
   * Returns the tween used for fade-in/fade-out, if any.
   *
   * @return The active fade tween, or {@code null}.
   */
  @Nullable
  public FlixelTween getFadeTween() {
    return fadeTween;
  }

  private void cancelFadeTween() {
    if (fadeTween != null) {
      fadeTween.cancel();
      fadeTween = null;
    }
  }

  /**
   * Returns the X position in world coordinates (for proximity/panning).
   *
   * @return World X position.
   */
  public float getX() {
    return x;
  }

  /**
   * Returns the Y position in world coordinates (for proximity/panning).
   *
   * @return World Y position.
   */
  public float getY() {
    return y;
  }

  /**
   * Sets world position for proximity/panning.
   *
   * @param x World X coordinate.
   * @param y World Y coordinate.
   * @return {@code this} for chaining.
   */
  public FlixelSound setPosition(float x, float y) {
    this.x = x;
    this.y = y;
    backendSetPosition(x, y, 0f);
    return this;
  }

  /**
   * Returns whether this sound auto-destroys when playback completes.
   *
   * @return {@code true} if auto-destroy is enabled.
   */
  public boolean isAutoDestroy() {
    return autoDestroy;
  }

  /** Returns whether this sound auto-destroys when playback completes. */
  public boolean getAutoDestroy() {
    return autoDestroy;
  }

  /**
   * Sets whether this sound auto-destroys when playback completes.
   *
   * @param autoDestroy {@code true} to enable auto-destroy.
   * @return {@code this} for chaining.
   */
  public FlixelSound setAutoDestroy(boolean autoDestroy) {
    this.autoDestroy = autoDestroy;
    return this;
  }

  @Override
  public boolean isPersist() {
    return persist;
  }

  /** Returns whether this sound persists across state transitions. */
  public boolean getPersist() {
    return persist;
  }

  @NotNull
  @Override
  public FlixelSound setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  @Override
  public void update(float elapsed) {
    if (!active || !exists) {
      return;
    }

    if (backendIsEnd() && !backendIsLooping() && !completeFired) {
      completeFired = true;
      onComplete.dispatch();
      if (autoDestroy) {
        destroy();
      }
      return;
    }

    if (endTimeMs != null && getTime() >= endTimeMs) {
      stop();
      onComplete.dispatch();
      if (autoDestroy) {
        destroy();
      }
    }
  }

  /**
   * Returns the list of effect nodes currently attached to this sound's audio graph, in
   * chain order (index 0 is closest to the sound source, last index is closest to the output).
   *
   * <p>Typed nodes ({@link FlixelReverbEffect}, {@link FlixelEchoEffect},
   * {@link FlixelLowPassEffect}) can be cast from elements in this list if needed,
   * though it is simpler to keep references returned by {@link #addReverb},
   * {@link #addEcho}, and {@link #addLowPassMuffle} directly.
   *
   * @return A read-only view of the effect chain.
   */
  public FlixelArray<FlixelSoundEffect> getEffectNodes() {
    return audioEffectNodes;
  }

  /**
   * Detaches and destroys every node in the effect chain (reverse order).
   * Called from {@link #destroy()}.
   */
  public void clearAudioEffectChain() {
    for (int i = audioEffectNodes.getSize() - 1; i >= 0; i--) {
      FlixelSoundEffect n = audioEffectNodes.get(i);
      n.detach(0);
      n.destroy();
    }
    audioEffectNodes.clear();
    backendRestoreDirectRouting();
  }

  /**
   * Appends a reverb node with the given wet amount in {@code [0, 1]}
   * (dry is set to {@code 1 - wet}). Build effect chains in load/setup code, not every frame.
   *
   * <p>Hold the returned node to adjust reverb parameters at runtime without rebuilding
   * the chain:
   *
   * <pre>{@code
   * FlixelReverbEffect reverb = sound.addReverb(0.4f);
   * // Later, on entering a cave:
   * reverb.setRoomSize(0.9f);
   * reverb.setWet(0.7f);
   * }</pre>
   *
   * @param wetAmount Wet signal level in [0, 1].
   * @return The attached reverb node. Hold this reference to modify parameters later.
   */
  @NotNull
  public FlixelReverbEffect addReverb(float wetAmount) {
    FlixelReverbEffect node = backendCreateReverb(wetAmount);
    attachEffectNode(node);
    return node;
  }

  /**
   * Appends a stereo delay/echo node.
   *
   * <p>Delay time and decay are fixed at construction. To change them, call
   * {@link #clearAudioEffectChain()} and rebuild, or destroy the specific node and add a new one.
   *
   * @param delaySeconds Delay time in seconds.
   * @param decay Decay factor for the delayed signal.
   * @return The attached echo node.
   */
  @NotNull
  public FlixelEchoEffect addEcho(float delaySeconds, float decay) {
    FlixelEchoEffect node = backendCreateEcho(delaySeconds, decay);
    attachEffectNode(node);
    return node;
  }

  /**
   * Appends a 2nd-order low-pass filter (muffled / distant sound).
   *
   * <p>Hold the returned node to adjust the cutoff frequency at runtime:
   *
   * <pre>{@code
   * FlixelLowPassEffect muffle = sound.addLowPassMuffle(8000.0);
   * // Tighten the filter as the player moves deeper:
   * muffle.setCutoff(2000.0);
   * }</pre>
   *
   * @param cutoffHz Cutoff frequency in Hz.
   * @return The attached low-pass node. Hold this reference to adjust cutoff later.
   */
  @NotNull
  public FlixelLowPassEffect addLowPassMuffle(double cutoffHz) {
    FlixelLowPassEffect node = backendCreateLowPass(cutoffHz, 2);
    attachEffectNode(node);
    return node;
  }

  /**
   * Expert escape hatch: append any effect node to the chain. {@code this}
   * sound destroys the node when {@link #clearAudioEffectChain()} runs unless
   * you remove it yourself first.
   *
   * @param node The effect node to attach.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound attachCustomNode(@NotNull FlixelSoundEffect node) {
    attachEffectNode(node);
    return this;
  }

  private void attachEffectNode(@NotNull FlixelSoundEffect node) {
    if (audioEffectNodes.getSize() == 0) {
      node.attachToUpstreamSound(this, 0);
    } else {
      node.attachToUpstreamNode(audioEffectNodes.peek(), 0);
    }
    audioEffectNodes.add(node);
    backendRouteTailToOutput(node);
  }

  @Override
  public void destroy() {
    release();
    if (sourceAsset != null) {
      sourceAsset.release();
      sourceAsset = null;
    }
    super.destroy();
    clearAudioEffectChain();
    cancelFadeTween();
    onComplete.clear();
    backendStop();
    pitch = 1f;
    pan = 0f;
    backendSetPitch(1f);
    backendSetPan(0f);
    x = 0f;
    y = 0f;
    backendSetPosition(0f, 0f, 0f);
    endTimeMs = null;
    autoDestroy = false;
    persist = false;
    completeFired = false;
    backendDispose();
  }

  /** Starts or resumes playback on the backend. */
  protected abstract void backendPlay();

  /** Pauses playback at the current cursor position. */
  protected abstract void backendPause();

  /** Stops playback and resets the cursor to the beginning. */
  protected abstract void backendStop();

  /**
   * @return {@code true} if the backend voice is actively playing.
   */
  protected abstract boolean backendIsPlaying();

  /**
   * @return {@code true} if the cursor is at or past the end of the stream.
   */
  protected abstract boolean backendIsEnd();

  /**
   * @return The backend volume ({@code 0} = silent, {@code 1} = default).
   */
  protected abstract float backendGetVolume();

  /**
   * Applies a volume to the backend voice.
   *
   * @param volume Volume level ({@code 0} = silent, {@code 1} = default).
   */
  protected abstract void backendSetVolume(float volume);

  /**
   * Applies a pitch multiplier to the backend voice.
   *
   * @param pitch Pitch multiplier; must be greater than {@code 0}.
   */
  protected abstract void backendSetPitch(float pitch);

  /**
   * Applies a stereo pan to the backend voice.
   *
   * @param pan Pan value in {@code [-1, 1]}.
   */
  protected abstract void backendSetPan(float pan);

  /**
   * @return The current cursor position in seconds.
   */
  protected abstract float backendGetCursor();

  /**
   * Seeks the backend voice.
   *
   * @param seconds Target position in seconds.
   */
  protected abstract void backendSeek(float seconds);

  /**
   * @return The total sound length in seconds, or {@code 0} when unknown.
   */
  protected abstract float backendGetLength();

  /**
   * @return {@code true} when the backend voice loops at the end.
   */
  protected abstract boolean backendIsLooping();

  /**
   * Enables or disables backend looping.
   *
   * @param looping {@code true} to loop.
   */
  protected abstract void backendSetLooping(boolean looping);

  /**
   * Applies a 3-D position for spatial audio. Backends without spatial audio ignore this.
   *
   * @param x X position.
   * @param y Y position.
   * @param z Z position.
   */
  protected abstract void backendSetPosition(float x, float y, float z);

  /** Releases the backend voice's native resources. Called at the end of {@link #destroy()}. */
  protected abstract void backendDispose();

  /**
   * Creates a reverb node on this sound's engine.
   *
   * @param wet Wet amount in [0, 1].
   * @return A new reverb node, or {@link FlixelReverbEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelReverbEffect backendCreateReverb(float wet);

  /**
   * Creates a delay / echo node on this sound's engine.
   *
   * @param delaySeconds Delay time in seconds.
   * @param decay Decay factor for the delayed signal.
   * @return A new echo node, or {@link FlixelEchoEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelEchoEffect backendCreateEcho(float delaySeconds, float decay);

  /**
   * Creates a low-pass filter node on this sound's engine.
   *
   * @param cutoffHz Cutoff frequency in hertz.
   * @param order Filter order (e.g. 2 for a second-order filter).
   * @return A new low-pass node, or {@link FlixelLowPassEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelLowPassEffect backendCreateLowPass(double cutoffHz, int order);

  /**
   * Routes the chain's tail node to the engine output so processed audio is audible. Called
   * each time a node is appended. Backends without an audio graph no-op.
   *
   * @param tail The current tail of the effect chain.
   */
  protected abstract void backendRouteTailToOutput(@NotNull FlixelSoundEffect tail);

  /**
   * Restores direct sound-to-output routing after the effect chain is cleared. Backends
   * without an audio graph no-op.
   */
  protected abstract void backendRestoreDirectRouting();
}
