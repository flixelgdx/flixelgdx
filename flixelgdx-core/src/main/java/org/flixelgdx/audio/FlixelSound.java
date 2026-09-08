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
 * {@link #addEcho}, {@link #addLowPassMuffle}, {@link #addHighPass},
 * {@link #addBandPass}, {@link #addNode}), and an {@link #onComplete} signal when
 * the sound finishes (for non-looping sounds).
 *
 * <p>Backends (miniaudio on native platforms, the Web Audio API on web) extend this class and
 * implement the abstract methods directly; all the gameplay-facing behavior
 * (fades, completion signals, effect-chain bookkeeping, persistence rules) lives here so it
 * works identically on every platform. Obtain instances from
 * {@link FlixelSoundManager#play Flixel.sound.play(...)},
 * {@link FlixelSoundManager#playMusic Flixel.sound.playMusic(...)}, or the non-playing
 * {@link FlixelSoundManager#create Flixel.sound.create(...)} escape hatch.
 *
 * <p>The effect methods return typed node handles rather than {@code this}, so holding
 * the returned reference lets you modify parameters live without rebuilding the effect chain.
 *
 * <p>{@link #isPersist() persist} controls state-switch behavior. Sounds not marked as persistent
 * are destroyed when the game switches states.
 */
public abstract class FlixelSound extends FlixelBasic {

  @Nullable
  private FlixelAsset<FlixelSoundSource> sourceAsset;

  @Nullable
  private FlixelSoundGroup group;

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

  /** Creates a new sound. Backends call this from their constructor. */
  protected FlixelSound() {
    super();
  }

  /**
   * Returns the current volume.
   *
   * @return Volume level (0 = silent, 1 = default, values above 1 are allowed).
   */
  public abstract float getVolume();

  /**
   * Sets the volume.
   *
   * @param volume Volume level (0 = silent, 1 = default, values above 1 amplify).
   * @return {@code this} for chaining.
   */
  public abstract FlixelSound setVolume(float volume);

  /**
   * Returns the pitch multiplier.
   *
   * @return Pitch multiplier; 1 = default, values above 1 raise pitch.
   */
  public abstract float getPitch();

  /**
   * Sets the pitch multiplier.
   *
   * @param pitch Pitch value; must be greater than 0.
   * @return {@code this} for chaining.
   */
  public abstract FlixelSound setPitch(float pitch);

  /**
   * Returns the stereo pan.
   *
   * @return Pan in [-1, 1]; -1 = left, 0 = center, 1 = right.
   */
  public abstract float getPan();

  /**
   * Sets the stereo pan.
   *
   * @param pan Pan value in [-1, 1].
   * @return {@code this} for chaining.
   */
  public abstract FlixelSound setPan(float pan);

  /**
   * Returns the current playback position in milliseconds.
   *
   * <p>If set while paused, the change takes effect after {@link #resume()}.
   *
   * @return Playback position in milliseconds.
   */
  public abstract float getTime();

  /**
   * Sets the playback position in milliseconds.
   *
   * @param timeMs The time to set the playback position to in milliseconds.
   * @return {@code this} for chaining.
   */
  public abstract FlixelSound setTime(float timeMs);

  /**
   * Returns the total length of the sound in milliseconds.
   *
   * @return Duration in milliseconds, or 0 if unknown.
   */
  public abstract float getLength();

  /**
   * Returns whether this sound is set to loop.
   *
   * @return {@code true} if looping is enabled.
   */
  public abstract boolean isLooped();

  /**
   * Returns whether this sound is set to loop.
   *
   * @return {@code true} if looping is enabled.
   */
  public boolean getLooped() {
    return isLooped();
  }

  /**
   * Enables or disables looping.
   *
   * @param looped {@code true} to loop, {@code false} to play once.
   * @return {@code this} for chaining.
   */
  public abstract FlixelSound setLooped(boolean looped);

  /**
   * Returns whether this sound is currently playing.
   *
   * @return {@code true} if the sound is actively playing.
   */
  public abstract boolean isPlaying();

  /**
   * Returns whether this sound is currently playing.
   *
   * @return {@code true} if the sound is actively playing.
   */
  public boolean getPlaying() {
    return isPlaying();
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
    resume();
    return this;
  }

  /**
   * Pauses the sound at its current position.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public abstract FlixelSound pause();

  /**
   * Stops the sound and resets position to 0.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound stop() {
    cancelFadeTween();
    pause();
    setTime(0f);
    return this;
  }

  /**
   * Resumes from the current position after a pause.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public abstract FlixelSound resume();

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
    fadeTween = FlixelTween.tween(this, new FlixelTweenSettings(FlixelTweenType.ONESHOT)
        .setDuration(durationSeconds)
        .addGoal(this::getVolume, to, this::setVolume));
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
    fadeTween = FlixelTween.tween(this, new FlixelTweenSettings(FlixelTweenType.ONESHOT)
        .setDuration(durationSeconds)
        .addGoal(this::getVolume, to, this::setVolume));
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
    applyPosition(x, y, 0f);
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

  /**
   * Returns whether this sound persists across state transitions.
   *
   * @return {@code true} if this sound persists when the game state changes.
   */
  public boolean isPersist() {
    return persist;
  }

  /**
   * Returns whether this sound persists across state transitions.
   *
   * @return {@code true} if this sound persists when the game state changes.
   */
  public boolean getPersist() {
    return persist;
  }

  /**
   * Sets whether this sound persists across state transitions.
   *
   * @param persist {@code true} to survive state switches.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  /**
   * Sets the group this sound belongs to, registering it as a member.
   *
   * <p>If the sound was already in a group, it is removed from the old one first.
   * This is called automatically when the sound is created; do not call it manually.
   *
   * @param group The group to join, or {@code null} to leave the current group.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound setGroup(@Nullable FlixelSoundGroup group) {
    if (this.group != null) {
      this.group.remove(this);
    }
    this.group = group;
    if (group != null) {
      group.add(this);
    }
    return this;
  }

  /**
   * Returns the group this sound belongs to, or {@code null} if not in a group.
   *
   * @return The group, or {@code null}.
   */
  @Nullable
  public FlixelSoundGroup getGroup() {
    return group;
  }

  /**
   * Attaches the backing {@link FlixelAsset} handle for the {@link FlixelSoundSource} that was
   * retained when this sound was created through {@link FlixelSoundManager}. The handle is
   * released in {@link #destroy()} so the source asset is eligible for cleanup according to the
   * active asset mode.
   *
   * @param sourceAsset The retained source handle, or {@code null} to clear it.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound setSourceAsset(@Nullable FlixelAsset<FlixelSoundSource> sourceAsset) {
    this.sourceAsset = sourceAsset;
    return this;
  }

  @Override
  public void update(float elapsed) {
    if (!active || !exists) {
      return;
    }

    if (isEnd() && !isLooped() && !completeFired) {
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
   * <p>Typed nodes ({@link FlixelReverbEffect}, {@link FlixelEchoEffect}, etc.) can be cast
   * from elements in this list if needed, though it is simpler to keep references returned by
   * {@link #addReverb}, {@link #addEcho}, and similar methods directly.
   *
   * @return The current effect chain.
   */
  public FlixelArray<FlixelSoundEffect> getEffectNodes() {
    return audioEffectNodes;
  }

  /**
   * Detaches and destroys every node in the effect chain (reverse order), then restores direct
   * sound-to-output routing. Called from {@link #destroy()}.
   */
  public void clearAudioEffectChain() {
    for (int i = audioEffectNodes.getSize() - 1; i >= 0; i--) {
      audioEffectNodes.get(i).destroy();
    }
    audioEffectNodes.clear();
    restoreDirectRouting();
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
    FlixelReverbEffect node = createReverbEffect(wetAmount);
    appendNode(node);
    return node;
  }

  /**
   * Appends a stereo delay/echo node.
   *
   * @param delaySeconds Delay time in seconds.
   * @param decay Decay factor for the delayed signal.
   * @return The attached echo node.
   */
  @NotNull
  public FlixelEchoEffect addEcho(float delaySeconds, float decay) {
    FlixelEchoEffect node = createEchoEffect(delaySeconds, decay);
    appendNode(node);
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
    FlixelLowPassEffect node = createLowPassEffect(cutoffHz, 2);
    appendNode(node);
    return node;
  }

  /**
   * Appends a 2nd-order high-pass filter (thin / airy sound).
   *
   * <p>Hold the returned node to adjust the cutoff frequency at runtime:
   *
   * <pre>{@code
   * FlixelHighPassEffect thin = sound.addHighPass(500.0);
   * thin.changeCutoff(200.0);  // now 700 Hz
   * }</pre>
   *
   * @param cutoffHz Cutoff frequency in Hz.
   * @return The attached high-pass node.
   */
  @NotNull
  public FlixelHighPassEffect addHighPass(double cutoffHz) {
    FlixelHighPassEffect node = createHighPassEffect(cutoffHz, 2);
    appendNode(node);
    return node;
  }

  /**
   * Appends a 2nd-order band-pass filter (phone / megaphone sound).
   *
   * <p>Hold the returned node to adjust cutoff or bandwidth at runtime:
   *
   * <pre>{@code
   * FlixelBandPassEffect phone = sound.addBandPass(1200.0, 1.5);
   * phone.changeQ(0.5);
   * }</pre>
   *
   * @param cutoffHz Center frequency in Hz.
   * @param q Q (quality) factor controlling the bandwidth.
   * @return The attached band-pass node.
   */
  @NotNull
  public FlixelBandPassEffect addBandPass(double cutoffHz, double q) {
    FlixelBandPassEffect node = createBandPassEffect(cutoffHz, q, 2);
    appendNode(node);
    return node;
  }

  /**
   * Appends a node by registered type ID.
   *
   * <p>Use {@link FlixelAudioNodeRegistry#register(String)} to obtain a type ID. If the backend
   * does not support the requested type, {@link FlixelSoundEffect#NOOP} is returned and silently
   * ignored.
   *
   * <p>Access parameters via {@link FlixelSoundEffect#setParam(int, float)} and
   * {@link FlixelSoundEffect#getParam(int)}.
   *
   * @param typeId A registered node type ID.
   * @param params Construction-time parameters; interpretation is type-specific.
   * @return The attached effect node.
   */
  @NotNull
  public FlixelSoundEffect addNode(int typeId, float... params) {
    FlixelSoundEffect node = createNode(typeId, params);
    appendNode(node);
    return node;
  }

  @Override
  public void destroy() {
    super.destroy();
    if (sourceAsset != null) {
      sourceAsset.release();
      sourceAsset = null;
    }
    if (group != null) {
      group.remove(this);
      group = null;
    }
    clearAudioEffectChain();
    cancelFadeTween();
    onComplete.clear();
    pause();
    setTime(0f);
    setPitch(1f);
    setPan(0f);
    setPosition(0f, 0f);
    endTimeMs = null;
    autoDestroy = false;
    persist = false;
    completeFired = false;
    disposeAudio();
  }

  /**
   * Returns {@code true} when the cursor is at or past the end of the stream.
   * Used internally by {@link #update(float)} to fire {@link #onComplete}.
   */
  protected abstract boolean isEnd();

  /**
   * Applies a 3-D position for spatial audio. Backends without spatial audio ignore this.
   * Called by {@link #setPosition(float, float)} and {@link #destroy()}.
   *
   * @param x X position.
   * @param y Y position.
   * @param z Z position.
   */
  protected abstract void applyPosition(float x, float y, float z);

  /**
   * Wires a newly created effect node into the audio graph.
   *
   * <p>When {@code upstream} is {@code null}, the node should connect to the sound source as its
   * input. When {@code upstream} is non-null, the node should connect to that preceding node's
   * output. The implementation is also responsible for routing the new node to the engine output,
   * disconnecting the old tail first if necessary. Backends without a node graph no-op.
   *
   * @param node The node to attach.
   * @param upstream The previous node in the chain, or {@code null} if this is the first node.
   */
  protected abstract void wireEffectNode(
      @NotNull FlixelSoundEffect node, @Nullable FlixelSoundEffect upstream);

  /**
   * Restores direct sound-to-output routing after the effect chain is cleared.
   * Backends without an audio graph no-op.
   */
  protected abstract void restoreDirectRouting();

  /**
   * Creates a reverb node on this sound's engine.
   *
   * @param wet Wet amount in [0, 1].
   * @return A new reverb node, or {@link FlixelReverbEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelReverbEffect createReverbEffect(float wet);

  /**
   * Creates a delay/echo node on this sound's engine.
   *
   * @param delaySeconds Delay time in seconds.
   * @param decay Decay factor for the delayed signal.
   * @return A new echo node, or {@link FlixelEchoEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelEchoEffect createEchoEffect(float delaySeconds, float decay);

  /**
   * Creates a low-pass filter node on this sound's engine.
   *
   * @param cutoffHz Cutoff frequency in hertz.
   * @param order Filter order (e.g. 2 for a second-order filter).
   * @return A new low-pass node, or {@link FlixelLowPassEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelLowPassEffect createLowPassEffect(double cutoffHz, int order);

  /**
   * Creates a high-pass filter node on this sound's engine.
   *
   * @param cutoffHz Cutoff frequency in hertz.
   * @param order Filter order (e.g. 2 for a second-order filter).
   * @return A new high-pass node, or {@link FlixelHighPassEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelHighPassEffect createHighPassEffect(double cutoffHz, int order);

  /**
   * Creates a band-pass filter node on this sound's engine.
   *
   * @param cutoffHz Center frequency in hertz.
   * @param q Q (quality) factor controlling the bandwidth.
   * @param order Filter order (e.g. 2 for a second-order filter).
   * @return A new band-pass node, or {@link FlixelBandPassEffect#NOOP} when unsupported.
   */
  @NotNull
  protected abstract FlixelBandPassEffect createBandPassEffect(double cutoffHz, double q, int order);

  /**
   * Creates an effect node for the given registered type ID.
   *
   * <p>Backends should return {@link FlixelSoundEffect#NOOP} for any type ID they do not
   * recognize, rather than throwing.
   *
   * @param typeId A registered node type ID from {@link FlixelAudioNodeRegistry}.
   * @param params Construction-time parameters; interpretation is type-specific.
   * @return A new effect node, or {@link FlixelSoundEffect#NOOP} when the type is unsupported.
   */
  @NotNull
  protected abstract FlixelSoundEffect createNode(int typeId, float[] params);

  /** Releases the backend voice's native resources. Called at the end of {@link #destroy()}. */
  protected abstract void disposeAudio();

  private void cancelFadeTween() {
    if (fadeTween != null) {
      fadeTween.cancel();
      fadeTween = null;
    }
  }

  private void appendNode(@NotNull FlixelSoundEffect node) {
    FlixelSoundEffect upstream = audioEffectNodes.isEmpty() ? null : audioEffectNodes.peek();
    audioEffectNodes.add(node);
    wireEffectNode(node, upstream);
  }
}
