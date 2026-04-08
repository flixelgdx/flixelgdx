/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.asset.FlixelAsset;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;
import me.stringdotjar.flixelgdx.util.FlixelPathsUtil;
import me.stringdotjar.flixelgdx.util.signal.FlixelSignal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * Flixel sound object that wraps a platform-specific {@link FlixelSoundBackend}.
 *
 * <p>Provides volume, pitch, pan, play/pause/stop/resume, fade-in/fade-out,
 * position (time), optional audio-graph effects ({@link #addReverb},
 * {@link #addEcho}, {@link #addLowPassMuffle}, {@link #attachCustomNode}),
 * and an {@link #onComplete} signal when the sound finishes (for non-looping
 * sounds).
 *
 * <p>This class implements {@link FlixelAsset}{@code <FlixelSoundBackend>} for
 * a shared refcount / {@code persist} contract. {@link #persist} controls
 * whether this {@code FlixelSound} is treated as long-lived in game state (e.g.
 * not killed on substate switches). Use {@link #retain()} / {@link #release()}
 * if you mirror pooled-asset semantics for sounds you manage manually.
 *
 * @see me.stringdotjar.flixelgdx.asset.FlixelAssetManager#resolveAudioPath(String)
 */
public class FlixelSound extends FlixelBasic implements FlixelAsset<FlixelSoundBackend> {

  private static final float SEC_TO_MS = 1000f;
  private static final float MS_TO_SEC = 1f / SEC_TO_MS;

  @NotNull
  private final String assetKey;

  @NotNull
  private final FlixelSoundBackend sound;

  @Nullable
  private FlixelAudioManager manager;

  private int refCount;

  /** Cached pitch (some backends have no getPitch). */
  private float pitch = 1f;

  /** Cached pan (some backends have no getPan). */
  private float pan = 0f;

  /** World x position for proximity/panning. */
  private float x;

  /** World y position for proximity/panning. */
  private float y;

  /** If set, playback stops at this position in milliseconds. */
  @Nullable
  private Float endTimeMs;

  /** When true, {@link #destroy()} is called when the sound finishes (non-looping). */
  private boolean autoDestroy;

  /** When true, this sound is not automatically destroyed on state switch. */
  private boolean persist;

  /** Current fade tween, so it can be cancelled when starting a new fade. */
  @Nullable
  private FlixelTween fadeTween;

  /** Tail-ordered effect nodes attached to the audio graph. */
  private final Array<FlixelSoundBackend.EffectNode> audioEffectNodes = new Array<>(4);

  /** Signal dispatched when the sound reaches its end (non-looping). */
  @NotNull
  public final FlixelSignal<Void> onComplete = new FlixelSignal<>();

  /**
   * Creates a new Flixel sound wrapping the given file path.
   *
   * @param path The path to the sound file.
   */
  public FlixelSound(@NotNull FileHandle path) {
    this(createSoundForHandle(path));
  }

  /**
   * Creates a Flixel sound wrapping the given backend.
   *
   * @param sound The platform-specific sound backend to wrap (must not be null).
   */
  public FlixelSound(@NotNull FlixelSoundBackend sound) {
    super();
    this.sound = sound;
    this.assetKey = "__flixel_sound__/" + ID;
  }

  /**
   * Returns the underlying sound backend for advanced use. Prefer the
   * {@code FlixelSound} API when possible.
   *
   * @return The wrapped backend instance.
   */
  @NotNull
  public FlixelSoundBackend getBackend() {
    return sound;
  }

  /**
   * Returns the manager that {@code this} sound is a member of.
   *
   * @return The manager, or {@code null} if not assigned to one.
   */
  @Nullable
  public FlixelAudioManager getManager() {
    return manager;
  }

  /**
   * Sets the manager that {@code this} sound is a member of.
   *
   * @param manager The manager to set.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound setManager(@Nullable FlixelAudioManager manager) {
    this.manager = manager;
    return this;
  }

  @NotNull
  @Override
  public String getAssetKey() {
    return assetKey;
  }

  @NotNull
  @Override
  public Class<FlixelSoundBackend> getType() {
    return FlixelSoundBackend.class;
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
    refCount--;
    if (refCount < 0) {
      refCount = 0;
    }
    return this;
  }

  @Override
  public void queueLoad() {
    // Sound is created eagerly in constructors, nothing to queue.
  }

  @NotNull
  @Override
  public FlixelSoundBackend require() {
    return sound;
  }

  @NotNull
  @Override
  public FlixelSoundBackend loadNow() {
    return sound;
  }

  /**
   * Returns the current volume.
   *
   * @return Volume level (0 = silent, 1 = default, values above 1 are allowed).
   */
  public float getVolume() {
    return sound.getVolume();
  }

  /**
   * Sets the volume.
   *
   * @param volume Volume level (0 = silent, 1 = default, values above 1 amplify).
   * @return {@code this} for chaining.
   */
  public FlixelSound setVolume(float volume) {
    sound.setVolume(volume);
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
    sound.setPitch(pitch);
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
    sound.setPan(pan);
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
    return sound.getCursorPosition() * SEC_TO_MS;
  }

  /**
   * Sets the playback position in milliseconds.
   *
   * @param timeMs The time to set the playback position to in milliseconds.
   * @return {@code this} for chaining.
   */
  public FlixelSound setTime(float timeMs) {
    sound.seekTo(timeMs * MS_TO_SEC);
    return this;
  }

  /**
   * Returns the total length of the sound in milliseconds.
   *
   * @return Duration in milliseconds, or 0 if unknown.
   */
  public float getLength() {
    return sound.getLength() * SEC_TO_MS;
  }

  /**
   * Returns whether this sound is set to loop.
   *
   * @return {@code true} if looping is enabled.
   */
  public boolean isLooped() {
    return sound.isLooping();
  }

  /**
   * Enables or disables looping.
   *
   * @param looped {@code true} to loop, {@code false} to play once.
   * @return {@code this} for chaining.
   */
  public FlixelSound setLooped(boolean looped) {
    sound.setLooping(looped);
    return this;
  }

  /**
   * Returns whether this sound is currently playing.
   *
   * @return {@code true} if the sound is actively playing.
   */
  public boolean isPlaying() {
    return sound.isPlaying();
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
    if (forceRestart) {
      setTime(startTimeMs);
    }
    sound.play();
    return this;
  }

  /**
   * Pauses the sound at its current position.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound pause() {
    sound.pause();
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
    sound.stop();
    return this;
  }

  /**
   * Resumes from the current position after a pause.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound resume() {
    sound.play();
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
    sound.setPosition(x, y, 0f);
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

  @Override
  public boolean isPersist() {
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
    if (!active || !exists || sound == null) {
      return;
    }

    if (sound.isEnd() && !sound.isLooping()) {
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
   * Detaches and disposes every node in the effect chain (reverse order).
   * Called from {@link #destroy()}.
   */
  public void clearAudioEffectChain() {
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
    for (int i = audioEffectNodes.size - 1; i >= 0; i--) {
      FlixelSoundBackend.EffectNode n = audioEffectNodes.get(i);
      n.detach(0);
      n.dispose();
    }
    audioEffectNodes.clear();
    if (factory != null) {
      factory.attachToEngineOutput(sound, 0);
    }
  }

  /**
   * Appends a reverb node with the given wet amount in {@code [0, 1]}
   * (dry is set to {@code 1 - wet}). Build effect chains in load/setup
   * code, not every frame.
   *
   * @param wetAmount Wet signal level in [0, 1].
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound addReverb(float wetAmount) {
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
    if (factory == null) return this;
    FlixelSoundBackend.EffectNode node = factory.createReverbNode(wetAmount);
    attachEffectNode(node);
    return this;
  }

  /**
   * Appends a stereo delay/echo node.
   *
   * @param delaySeconds Delay time in seconds.
   * @param decay Decay factor for the delayed signal.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound addEcho(float delaySeconds, float decay) {
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
    if (factory == null) return this;
    FlixelSoundBackend.EffectNode node = factory.createDelayNode(delaySeconds, decay);
    attachEffectNode(node);
    return this;
  }

  /**
   * Appends a 2nd-order low-pass filter (muffled / distant sound).
   *
   * @param cutoffHz Cutoff frequency in Hz.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound addLowPassMuffle(double cutoffHz) {
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
    if (factory == null) return this;
    FlixelSoundBackend.EffectNode node = factory.createLowPassFilter(cutoffHz, 2);
    attachEffectNode(node);
    return this;
  }

  /**
   * Expert escape hatch: append any effect node to the chain. {@code this}
   * sound disposes the node when {@link #clearAudioEffectChain()} runs unless
   * you remove it yourself first.
   *
   * @param node The effect node to attach.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelSound attachCustomNode(@NotNull FlixelSoundBackend.EffectNode node) {
    attachEffectNode(node);
    return this;
  }

  private void attachEffectNode(@NotNull FlixelSoundBackend.EffectNode node) {
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
    FlixelSoundBackend upstream = audioEffectNodes.size == 0
      ? sound
      : null;
    if (upstream != null) {
      node.attachToUpstream(upstream, 0);
    }
    audioEffectNodes.add(node);
    if (factory != null) {
      factory.attachToEngineOutput(sound, 0);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    clearAudioEffectChain();
    cancelFadeTween();
    onComplete.clear();
    sound.stop();
    pitch = 1f;
    pan = 0f;
    sound.setPitch(1f);
    sound.setPan(0f);
    x = 0f;
    y = 0f;
    sound.setPosition(0f, 0f, 0f);
    endTimeMs = null;
    autoDestroy = false;
    persist = false;
  }

  private static FlixelSoundBackend createSoundForHandle(@NotNull FileHandle path) {
    String resolvedPath = FlixelPathsUtil.resolveAudioPath(path.path());
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
    Object sfxGroup = Flixel.sound != null ? Flixel.sound.getSfxGroup() : null;
    return factory.createSound(resolvedPath, (short) 0, sfxGroup, false);
  }
}
