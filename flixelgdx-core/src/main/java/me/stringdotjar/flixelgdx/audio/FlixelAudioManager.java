/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.util.FlixelPathsUtil;
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
   * <p>Use during {@link me.stringdotjar.flixelgdx.Flixel#resetGame()} instead
   * of {@link #destroy()} so the native backend is not torn down and re-created
   * in one frame (which can break PulseAudio and similar backends).
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
   * @param path Path to the sound (internal or resolved via {@link FlixelPathsUtil}).
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
    String resolvedPath = external ? path : FlixelPathsUtil.resolveAudioPath(path);
    Object targetGroup = (group != null) ? group : sfxGroup;
    FlixelSoundBackend backend = factory.createSound(resolvedPath, (short) 0, targetGroup, external);
    FlixelSound flixelSound = new FlixelSound(backend);
    flixelSound.setVolume(volume);
    flixelSound.setLooped(looping);
    flixelSound.play();
    return flixelSound;
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
      music.stop();
    }
    String resolvedPath = external ? path : FlixelPathsUtil.resolveAudioPath(path);
    FlixelSoundBackend backend = factory.createSound(resolvedPath, (short) 0, musicGroup, external);
    music = new FlixelSound(backend);
    music.setVolume(volume);
    music.setLooped(looping);
    music.play();
    return music;
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
