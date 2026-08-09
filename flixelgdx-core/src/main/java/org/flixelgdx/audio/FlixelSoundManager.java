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

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central manager for all audio. {@link FlixelSound} instances, master volume,
 * sound groups (SFX and music), and focus-based pause/resume.
 *
 * <p>Access via {@link org.flixelgdx.Flixel#sound Flixel.sound}. Supports
 * separate groups for sound effects and music, global master volume, and
 * automatic pause when the game loses focus (and resume when it regains focus).
 *
 * <p>The platform's {@link FlixelSoundFactory} powers everything: install one before
 * {@link org.flixelgdx.Flixel#start Flixel.start} (the desktop launcher does this for you) and
 * the manager builds its groups and sounds through it. Most games only need
 * {@link #play}, {@link #playMusic}, and the volume controls; use {@link #create} when you want
 * a {@link FlixelSound} configured up front without hearing it yet.
 *
 * <p>For internal paths, sounds resolve through the asset pipeline: a loaded
 * {@link FlixelSoundSource} is used when present, otherwise the source is block-loaded first.
 * All file access goes through the {@link org.flixelgdx.file.FlixelFiles Flixel.files} seam, so
 * audio works identically from a folder, a packaged JAR, or any custom file root.
 */
public class FlixelSoundManager implements FlixelUpdatable, FlixelDestroyable {

  private static final String[] AUDIO_EXTENSIONS = { ".mp3", ".ogg", ".wav", ".flac" };

  private final FlixelSoundFactory factory;
  private final FlixelArray<FlixelSound> activeSounds = new FlixelArray<>(false, 8);
  private FlixelSoundGroup sfxGroup;
  private FlixelSoundGroup musicGroup;

  private float masterVolume = 1f;

  @Nullable
  public FlixelSound music;

  /**
   * Constructs a new audio manager using the given backend factory.
   *
   * @param factory The platform-specific sound factory.
   */
  public FlixelSoundManager(@NotNull FlixelSoundFactory factory) {
    this.factory = factory;
    sfxGroup = factory.createGroup();
    musicGroup = factory.createGroup();
    FlixelSoundSourceLoader loader = new FlixelSoundSourceLoader();
    for (String ext : AUDIO_EXTENSIONS) {
      Flixel.assets.registerLoader(ext, loader);
    }
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
      music.destroy();
      music = null;
    }
    for (int i = 0; i < activeSounds.getSize(); i++) {
      FlixelSound s = activeSounds.get(i);
      if (s.isExists()) {
        s.destroy();
      }
    }
    activeSounds.clear();
    if (sfxGroup != null) {
      sfxGroup.destroy();
    }
    if (musicGroup != null) {
      musicGroup.destroy();
    }
    sfxGroup = factory.createGroup();
    musicGroup = factory.createGroup();
    factory.setMasterVolume(masterVolume);
  }

  /**
   * Destroys all non-persistent {@link FlixelSound} instances tracked by this manager, including
   * the current music track if it is not persistent.
   *
   * <p>Called automatically by {@link org.flixelgdx.Flixel#switchState Flixel.switchState} on every state switch
   * when the asset mode is {@link org.flixelgdx.asset.FlixelAssetMode#STANDARD FlixelAssetMode.STANDARD} or
   * {@link org.flixelgdx.asset.FlixelAssetMode#AGGRESSIVE FlixelAssetMode.AGGRESSIVE}. Sounds whose {@link FlixelSound#isPersist()}
   * flag is set survive the switch unchanged.
   *
   * <p>Sounds that were already destroyed (for example, via {@link FlixelSound#setAutoDestroy}) are
   * pruned from the tracking list without being double-destroyed.
   */
  public void clearNonPersist() {
    for (int i = activeSounds.getSize() - 1; i >= 0; i--) {
      FlixelSound s = activeSounds.get(i);
      if (!s.isExists()) {
        activeSounds.removeIndex(i);
      } else if (!s.isPersist()) {
        s.destroy();
        activeSounds.removeIndex(i);
      }
    }
    if (music != null && !music.isPersist()) {
      music.destroy();
      music = null;
    }
  }

  /**
   * Returns the underlying backend factory for advanced use.
   *
   * @return The sound factory powering this manager.
   */
  @NotNull
  public FlixelSoundFactory getFactory() {
    return factory;
  }

  /**
   * Returns the SFX group. Use for playing sounds or custom sounds
   * that should be categorised as SFX.
   *
   * @return The SFX group.
   */
  @NotNull
  public FlixelSoundGroup getSfxGroup() {
    return sfxGroup;
  }

  /**
   * Returns the music group. Used by {@link #playMusic}.
   *
   * @return The music group.
   */
  @NotNull
  public FlixelSoundGroup getMusicGroup() {
    return musicGroup;
  }

  /**
   * Returns the default group used when no group is specified (SFX group).
   *
   * @return The SFX group.
   */
  @NotNull
  public FlixelSoundGroup getSoundsGroup() {
    return sfxGroup;
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
   * Creates a sound without playing it: the escape hatch for configuring a {@link FlixelSound}
   * (volume, effects, looping) before its first {@link FlixelSound#play()}.
   *
   * <p>The sound joins the SFX group and is tracked by this manager like any played sound, so
   * state-switch cleanup and focus pausing still apply.
   *
   * @param path Internal asset key / path.
   * @return The new, idle {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound create(@NotNull String path) {
    return create(path, null, false);
  }

  /**
   * Creates a sound without playing it, in the given group.
   *
   * @param path Internal asset key / path.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @return The new, idle {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound create(@NotNull String path, @Nullable FlixelSoundGroup group) {
    return create(path, group, false);
  }

  /**
   * Creates a sound without playing it.
   *
   * @param path Internal asset key / path, or an absolute path when {@code external} is {@code true}.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @param external If {@code true}, the path is read from the absolute file root.
   * @return The new, idle {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound create(@NotNull String path, @Nullable FlixelSoundGroup group, boolean external) {
    FlixelSoundGroup targetGroup = (group != null) ? group : sfxGroup;
    FlixelSound sound = buildSound(path, external, targetGroup);
    sound.setManager(this);
    activeSounds.add(sound);
    return sound;
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
  public FlixelSound play(@NotNull String path, float volume, boolean looping, @Nullable FlixelSoundGroup group) {
    return play(path, volume, looping, group, false);
  }

  /**
   * Plays a new sound effect.
   *
   * @param path Path to the sound.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @param external If {@code true}, the path is read from the absolute file root.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull String path, float volume, boolean looping,
      @Nullable FlixelSoundGroup group, boolean external) {
    FlixelSoundGroup targetGroup = (group != null) ? group : sfxGroup;
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
   * @param external If {@code true}, the path is read from the absolute file root.
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
   * Builds a new {@link FlixelSound} for {@code path} without starting playback.
   *
   * <p>When {@code external} is {@code false}, reads or synchronously loads a
   * {@link FlixelSoundSource} through the asset manager and retains its handle for the sound's
   * lifetime. External paths read the file bytes from
   * {@link org.flixelgdx.file.FlixelFiles#absolute Flixel.files.absolute} directly.
   *
   * @param path The path to the sound file.
   * @param external If {@code true}, the path is read from the absolute file root.
   * @param targetGroup The group to create the sound in.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  private FlixelSound buildSound(@NotNull String path, boolean external, @NotNull FlixelSoundGroup targetGroup) {
    if (external) {
      FlixelSoundBuffer buffer = FlixelSoundBuffer.read(path, Flixel.files.absolute(path));
      return factory.createSound(buffer, targetGroup);
    }
    FlixelAssetManager assets = Flixel.assets;
    if (!assets.isLoaded(path)) {
      assets.load(path);
      assets.finishLoadingAsset(path);
    }
    FlixelAsset<FlixelSoundSource> sourceHandle = assets.<FlixelSoundSource>get(path).retain();
    FlixelSound sound = sourceHandle.get().create(targetGroup);
    sound.setSourceAsset(sourceHandle);
    return sound;
  }

  /**
   * Builds a new {@link FlixelSound} for {@code path}, starts playback, and returns it.
   *
   * @param path The path to the sound file.
   * @param external If {@code true}, the path is read from the absolute file root.
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
      @NotNull FlixelSoundGroup targetGroup) {
    FlixelSound sound = buildSound(path, external, targetGroup);
    sound.setManager(this);
    sound.setVolume(volume);
    sound.setLooped(looping);
    sound.play();
    activeSounds.add(sound);
    return sound;
  }

  /**
   * Ticks all active sounds so that {@link FlixelSound#onComplete} fires and
   * {@link FlixelSound#setAutoDestroy auto-destroy} is honored.
   *
   * <p>Called automatically by {@link org.flixelgdx.FlixelGame FlixelGame} every frame
   * inside the game-update block; do not call this manually.
   *
   * <p>Sounds whose {@link FlixelSound#isExists() exists} flag is {@code false} (e.g.
   * because they auto-destroyed inside their own {@code update()}) are pruned from the
   * tracking list during this pass.
   *
   * @param elapsed Time in seconds since the last frame.
   */
  @Override
  public void update(float elapsed) {
    for (int i = activeSounds.getSize() - 1; i >= 0; i--) {
      FlixelSound s = activeSounds.get(i);
      if (s.isExists()) {
        s.update(elapsed);
      }
      if (!s.isExists()) {
        activeSounds.removeIndex(i);
      }
    }
  }

  /**
   * Pauses all currently playing sounds. Used when the game loses focus or
   * is minimized. Only sounds that were playing are paused; they can be
   * resumed with {@link #resume()}.
   */
  public void pause() {
    sfxGroup.pause();
    musicGroup.pause();
  }

  /**
   * Resumes all sounds that were paused by {@link #pause()}. Called when the
   * game regains focus.
   */
  public void resume() {
    sfxGroup.resume();
    musicGroup.resume();
  }

  @Override
  public void destroy() {
    if (music != null) {
      music.destroy();
      music = null;
    }
    for (int i = 0; i < activeSounds.getSize(); i++) {
      FlixelSound s = activeSounds.get(i);
      if (s.isExists()) {
        s.destroy();
      }
    }
    activeSounds.clear();
    sfxGroup.destroy();
    musicGroup.destroy();
    factory.destroyEngine();
  }
}
