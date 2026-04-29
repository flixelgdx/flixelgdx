/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.badlogic.gdx.utils.Disposable;

/**
 * Central manager for all audio. {@link FlixelSound} instances, master volume,
 * sound groups (SFX and music), and focus-based pause/resume.
 *
 * <p>Access via {@link me.stringdotjar.flixelgdx.Flixel#sound}. Supports
 * separate groups for sound effects and music, global master volume, and
 * automatic pause when the game loses focus (and resume when it regains focus).
 *
 * <p>For internal paths, {@link #play} and {@link #playMusic} resolve audio through
 * {@link Flixel#ensureAssets()}, which uses a loaded {@link FlixelSoundSource} when present,
 * otherwise enqueue and block-load that source before creating a {@link FlixelSound}.
 * External paths still bypass the asset manager and hit the backend directly.
 */
public class FlixelAudioManager implements FlixelDestroyable, Disposable {

  private final FlixelSoundBackend.Factory factory;
  private Object sfxGroup;
  private Object musicGroup;

  private float masterVolume = 1f;
  private FlixelSound music;

  /**
   * Constructs a new audio manager using the given backend factory.
   *
   * @param factory The platform-specific sound backend factory.
   */
  public FlixelAudioManager(@NotNull FlixelSoundBackend.Factory factory) {
    this.factory = factory;
    sfxGroup = factory.createGroup();
    musicGroup = factory.createGroup();
  }

  /**
   * Stops session audio and rebuilds SFX and music groups on the existing engine.
   *
   * <p>Use this instead of {@link #destroy()} so the native backend is not torn down and re-created
   * in one frame (which can break PulseAudio and similar backends) unless you know for sure you don't want to
   * use the audio system anymore.
   */
  public void resetSession() {
    if (music != null) {
      music.dispose();
      music = null;
    }
    if (sfxGroup != null) {
      factory.disposeGroup(sfxGroup);
    }
    if (musicGroup != null) {
      factory.disposeGroup(musicGroup);
    }
    sfxGroup = factory.createGroup();
    musicGroup = factory.createGroup();
    factory.setMasterVolume(masterVolume);
  }

  /**
   * Returns the underlying backend factory for advanced use.
   *
   * @return The backend factory powering this manager.
   */
  @NotNull
  public FlixelSoundBackend.Factory getFactory() {
    return factory;
  }

  /**
   * Returns the SFX group handle. Use for playing sounds or custom sounds
   * that should be categorised as SFX.
   *
   * @return The SFX group handle.
   */
  @NotNull
  public Object getSfxGroup() {
    return sfxGroup;
  }

  /**
   * Returns the music group handle. Used by {@link #playMusic}.
   *
   * @return The music group handle.
   */
  @NotNull
  public Object getMusicGroup() {
    return musicGroup;
  }

  /**
   * Returns the default group used when no group is specified (SFX group).
   *
   * @return The SFX group handle.
   */
  @NotNull
  public Object getSoundsGroup() {
    return sfxGroup;
  }

  /**
   * Returns the currently playing music, or {@code null} if none.
   *
   * @return The current music sound, or {@code null}.
   */
  @Nullable
  public FlixelSound getMusic() {
    return music;
  }

  /**
   * Returns the current master volume.
   *
   * @return Master volume in [0, 1].
   */
  public float getMasterVolume() {
    return masterVolume;
  }

  /**
   * Sets the global master volume applied to all sounds.
   *
   * @param volume New master volume (values outside [0, 1] are clamped).
   * @return The clamped master volume.
   */
  public float setMasterVolume(float volume) {
    float clamped = Math.max(0f, Math.min(1f, volume));
    factory.setMasterVolume(clamped);
    masterVolume = clamped;
    return clamped;
  }

  /**
   * Changes the global master volume by the given amount.
   *
   * @param amount The amount to change the master volume by.
   * @return The new master volume.
   */
  public float changeMasterVolume(float amount) {
    return setMasterVolume(masterVolume + amount);
  }

  /**
   * Plays a new sound effect (SFX group).
   *
   * @param path Internal asset key / path, or external path when {@code external} is {@code true}.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull String path) {
    return play(path, 1f, false, null, false);
  }

  /**
   * Plays a new sound effect.
   *
   * @param path Path to the sound.
   * @param volume Volume to play with.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull String path, float volume) {
    return play(path, volume, false, null, false);
  }

  /**
   * Plays a new sound effect.
   *
   * @param path Path to the sound.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull String path, float volume, boolean looping) {
    return play(path, volume, looping, null, false);
  }

  /**
   * Plays a new sound effect.
   *
   * @param path Path to the sound.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull String path, float volume, boolean looping, @Nullable Object group) {
    return play(path, volume, looping, group, false);
  }

  /**
   * Plays a new sound effect.
   *
   * @param path Path to the sound.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @param external If {@code true}, the path is used as-is (for external files).
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull String path, float volume, boolean looping,
                           @Nullable Object group, boolean external) {
    Object targetGroup = (group != null) ? group : sfxGroup;
    return createAndPlaySoundFromPath(path, external, volume, looping, targetGroup);
  }

  /**
   * Sets and plays the current music (music group). Stops any previous music.
   *
   * @param path Path to the music file.
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull String path) {
    return playMusic(path, 1f, true, false);
  }

  /**
   * Sets and plays the current music. Stops any previous music.
   *
   * @param path Path to the music file.
   * @param volume Volume.
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull String path, float volume) {
    return playMusic(path, volume, true, false);
  }

  /**
   * Sets and plays the current music. Stops any previous music.
   *
   * @param path Path to the music file.
   * @param volume Volume.
   * @param looping Whether to loop.
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull String path, float volume, boolean looping) {
    return playMusic(path, volume, looping, false);
  }

  /**
   * Sets and plays the current music. Stops any previous music.
   *
   * @param path Path to the music file.
   * @param volume Volume.
   * @param looping Whether to loop.
   * @param external If {@code true}, the path is used as-is (e.g. for mobile external storage).
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull String path, float volume, boolean looping, boolean external) {
    if (music != null) {
      music.destroy();
      music = null;
    }
    music = createAndPlaySoundFromPath(path, external, volume, looping, musicGroup);
    return music;
  }

  /**
   * Builds a new {@link FlixelSound} for {@code path}, starts playback, and returns it.
   *
   * <p>When {@code external} is {@code false}, uses {@link Flixel#ensureAssets()} to read or
   * synchronously load a {@link FlixelSoundSource} for {@code path}, then {@link FlixelSoundSource#create(Object)}
   * with {@code targetGroup}. External paths keep the previous behavior: direct backend creation from {@code path}.
   *
   * @param path The path to the sound file.
   * @param external If {@code true}, the path is used as-is (for external files).
   * @param volume The volume to play the sound at.
   * @param looping If {@code true}, the sound will loop.
   * @param targetGroup The group to play the sound in.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  private FlixelSound createAndPlaySoundFromPath(
    @NotNull String path,
    boolean external,
    float volume,
    boolean looping,
    @NotNull Object targetGroup
  ) {
    if (external) {
      FlixelSoundBackend backend = factory.createSound(path, (short) 0, targetGroup, true);
      FlixelSound s = new FlixelSound(backend);
      s.setVolume(volume);
      s.setLooped(looping);
      s.play();
      return s;
    }
    FlixelAssetManager assets = Flixel.ensureAssets();
    if (!assets.isLoaded(path, FlixelSoundSource.class)) {
      assets.load(path, FlixelSoundSource.class);
      assets.finishLoadingAsset(path);
    }
    FlixelSoundSource source = assets.get(path, FlixelSoundSource.class);
    FlixelSound flixelSound = source.create(targetGroup);
    flixelSound.setVolume(volume);
    flixelSound.setLooped(looping);
    flixelSound.play();
    return flixelSound;
  }

  /**
   * Pauses all currently playing sounds. Used when the game loses focus or
   * is minimized. Only sounds that were playing are paused; they can be
   * resumed with {@link #resume()}.
   */
  public void pause() {
    factory.groupPause(sfxGroup);
    factory.groupPause(musicGroup);
  }

  /**
   * Resumes all sounds that were paused by {@link #pause()}. Called when the
   * game regains focus.
   */
  public void resume() {
    factory.groupPlay(sfxGroup);
    factory.groupPlay(musicGroup);
  }

  @Override
  public void destroy() {
    if (music != null) {
      music.dispose();
      music = null;
    }
    factory.disposeGroup(sfxGroup);
    factory.disposeGroup(musicGroup);
    factory.disposeEngine();
  }

  @Override
  public void dispose() {
    destroy();
  }
}
