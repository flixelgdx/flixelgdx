/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.effect.MADelayNode;
import games.rednblack.miniaudio.effect.MAReverbNode;
import games.rednblack.miniaudio.filter.MALowPassFilter;
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
 * Flixel sound object that wraps an {@link MASound} from the miniaudio library.
 *
 * <p>Provides volume, pitch, pan, play/pause/stop/resume, fade-in/fade-out, position (time), optional
 * miniaudio graph effects ({@link #addReverb}, {@link #addEcho}, {@link #addLowPassMuffle},
 * {@link #attachCustomNode}), and an {@link #onComplete} signal when the sound finishes (for non-looping sounds).
 *
 * <p>This class implements {@link FlixelAsset}{@code <MASound>} for a shared refcount / {@code persist}
 * contract. {@link #persist} controls whether this {@code FlixelSound} is treated as long-lived in game state
 * (e.g. not killed on substate switches). It is separate from {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager#clearNonPersist()},
 * which clears <em>pooled</em> typed handles and wrappers on the global asset manager—not miniaudio instances created
 * directly from a {@link com.badlogic.gdx.files.FileHandle}. Use {@link #retain()} / {@link #release()} if you mirror
 * pooled-asset semantics for sounds you manage manually.
 *
 * @see me.stringdotjar.flixelgdx.asset.FlixelAssetManager#resolveAudioPath(String)
 */
public class FlixelSound extends FlixelBasic implements FlixelAsset<MASound> {

  private static final float SEC_TO_MS = 1000f;
  private static final float MS_TO_SEC = 1f / SEC_TO_MS;

  @NotNull
  private final String assetKey;

  /** The underlying miniaudio sound. Use {@link #getMASound()} for external access. */
  @NotNull
  private final MASound sound;

  /** The manager that {@code this} sound is a member of. */
  @Nullable
  private FlixelAudioManager manager;

  private int refCount;

  /** Cached pitch (MASound has no getPitch). */
  private float pitch = 1f;

  /** Cached pan (MASound has no getPan). */
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

  /** Tail-ordered effect nodes: each attaches to the previous node or to {@link #sound}. */
  private final Array<MANode> audioEffectNodes = new Array<>(4);

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
   * Creates a Flixel sound wrapping the given MASound.
   *
   * @param sound The miniaudio sound to wrap (must not be null).
   */
  public FlixelSound(@NotNull MASound sound) {
    super();
    this.sound = sound;
    this.assetKey = "__flixel_sound__/" + ID;
  }

  /**
   * Returns the underlying MASound for advanced use. Prefer the FlixelSound API when possible.
   *
   * @return The wrapped MASound instance.
   */
  @NotNull
  public MASound getMASound() {
    return sound;
  }

  /**
   * Returns the manager that {@code this} sound is a member of.
   *
   * @return The manager that {@code this} sound is a member of, or {@code null} if it is not a member of any manager.
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
  public Class<MASound> getType() {
    return MASound.class;
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
    // Sound is created eagerly in constructors; nothing to queue on the libGDX AssetManager.
  }

  @NotNull
  @Override
  public MASound require() {
    return sound;
  }

  @NotNull
  @Override
  public MASound loadNow() {
    return sound;
  }

  /** Volume in [0, 1] (values > 1 are allowed for louder). */
  public float getVolume() {
    return sound.getVolume();
  }

  /** Sets volume (0 = silent, 1 = default, >1 = louder). */
  public FlixelSound setVolume(float volume) {
    sound.setVolume(volume);
    return this;
  }

  /** Pitch multiplier; 1 = default, >1 = higher. */
  public float getPitch() {
    return pitch;
  }

  /** Sets pitch; must be > 0. */
  public FlixelSound setPitch(float pitch) {
    this.pitch = pitch;
    sound.setPitch(pitch);
    return this;
  }

  /** Pan in [-1, 1]; -1 = left, 0 = center, 1 = right. */
  public float getPan() {
    return pan;
  }

  /** Sets pan in [-1, 1]. */
  public FlixelSound setPan(float pan) {
    this.pan = pan;
    sound.setPan(pan);
    return this;
  }

  /**
   * Current playback position in milliseconds.
   *
   * <p>If set while paused, the change takes effect after {@link #resume()}.
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

  public float getLength() {
    return sound.getLength() * SEC_TO_MS;
  }

  public boolean isLooped() {
    return sound.isLooping();
  }

  public FlixelSound setLooped(boolean looped) {
    sound.setLooping(looped);
    return this;
  }

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

  /** Pauses the sound. */
  @NotNull
  public FlixelSound pause() {
    sound.pause();
    return this;
  }

  /** Stops the sound and resets position to 0. */
  @NotNull
  public FlixelSound stop() {
    cancelFadeTween();
    sound.stop();
    return this;
  }

  /** Resumes from the current position after a pause. */
  @NotNull
  public FlixelSound resume() {
    sound.play();
    return this;
  }

  /** Stops playback at this position (ms). Null = play to end. */
  @Nullable
  public Float getEndTime() {
    return endTimeMs;
  }

  /** Sets the position (ms) at which to stop. Null = play to end. */
  public FlixelSound setEndTime(@Nullable Float endTimeMs) {
    this.endTimeMs = endTimeMs;
    return this;
  }

  /** Fades in from 0 to 1 over the given duration (seconds). */
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

  /** Fades out to 0 over the given duration (seconds). */
  @NotNull
  public FlixelSound fadeOut(float durationSeconds) {
    return fadeOut(durationSeconds, 0f);
  }

  /**
   * Fades volume to {@code to} over {@code durationSeconds}.
   *
   * @param durationSeconds Fade duration in seconds.
   * @param to Target volume (typically 0).
   * @return this, for chaining.
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

  /** The tween used for fade-in/fade-out, if any. */
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

  /** X position in world coordinates (for proximity/panning). */
  public float getX() {
    return x;
  }

  /** Y position in world coordinates (for proximity/panning). */
  public float getY() {
    return y;
  }

  /** Sets world position for proximity/panning. */
  public FlixelSound setPosition(float x, float y) {
    this.x = x;
    this.y = y;
    sound.setPosition(x, y, 0f);
    return this;
  }

  public boolean isAutoDestroy() {
    return autoDestroy;
  }

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
   * Detaches and disposes every node in {@link #audioEffectNodes} (reverse order). Called from
   * {@link #reset()} and {@link #destroy()}.
   */
  public void clearAudioEffectChain() {
    MiniAudio mini = Flixel.getAudioEngine();
    for (int i = audioEffectNodes.size - 1; i >= 0; i--) {
      MANode n = audioEffectNodes.get(i);
      n.detach(0);
      n.dispose();
    }
    audioEffectNodes.clear();
    // Effects replace the default sound -> endpoint route; restore it when the chain is empty.
    mini.attachToEngineOutput(sound, 0);
  }

  /**
   * Appends a reverb node with the given wet amount in {@code [0, 1]} (dry is set to {@code 1 - wet}).
   * Build effect chains in load/setup code, not every frame.
   */
  @NotNull
  public FlixelSound addReverb(float wetAmount) {
    MiniAudio engine = Flixel.getAudioEngine();
    MAReverbNode rev = new MAReverbNode(engine);
    float w = Math.max(0f, Math.min(1f, wetAmount));
    rev.setWet(w);
    rev.setDry(1f - w);
    attachEffectNode(rev);
    return this;
  }

  /**
   * Appends a stereo delay/echo node (miniaudio {@link MADelayNode}).
   *
   * @param delaySeconds Delay time in seconds.
   * @param decay Decay factor for the delayed signal.
   */
  @NotNull
  public FlixelSound addEcho(float delaySeconds, float decay) {
    MADelayNode node = new MADelayNode(Flixel.getAudioEngine(), delaySeconds, decay);
    attachEffectNode(node);
    return this;
  }

  /**
   * Appends a 2nd-order low-pass filter (muffled / distant sound).
   *
   * @param cutoffHz Cutoff frequency in Hz.
   */
  @NotNull
  public FlixelSound addLowPassMuffle(double cutoffHz) {
    MALowPassFilter lp = new MALowPassFilter(Flixel.getAudioEngine(), cutoffHz, 2);
    attachEffectNode(lp);
    return this;
  }

  /**
   * Expert escape hatch: append any {@link MANode} to the effect chain. {@code this} sound disposes the node
   * when {@link #clearAudioEffectChain} runs unless you remove it yourself first.
   */
  @NotNull
  public FlixelSound attachCustomNode(@NotNull MANode node) {
    attachEffectNode(node);
    return this;
  }

  private void attachEffectNode(@NotNull MANode node) {
    MiniAudio mini = Flixel.getAudioEngine();
    MANode upstream = audioEffectNodes.size == 0 ? sound : audioEffectNodes.peek();
    node.attachToThisNode(upstream, 0);
    audioEffectNodes.add(node);
    // Wiring upstream -> node removes the previous output attachment (e.g. sound or prior tail -> endpoint).
    mini.attachToEngineOutput(node, 0);
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

  private static MASound createSoundForHandle(@NotNull FileHandle path) {
    String resolvedPath = FlixelPathsUtil.resolveAudioPath(path.path());
    return Flixel.getAudioEngine().createSound(resolvedPath, (short) 0, Flixel.sound.getSfxGroup(), false);
  }
}
