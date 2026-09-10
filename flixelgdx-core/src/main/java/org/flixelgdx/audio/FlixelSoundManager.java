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
import org.flixelgdx.FlixelGame;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFiles;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central manager for all audio. {@link FlixelSound} instances, master volume,
 * sound groups (SFX and music), and focus-based pause/resume.
 *
 * <p>Access via {@link Flixel#sound}. Supports
 * separate groups for sound effects and music, global master volume, and
 * automatic pause when the game loses focus (and resume when it regains focus).
 *
 * <p>The platform's {@link FlixelSoundFactory} powers everything: install one before
 * {@link Flixel#start} (the desktop launcher does this for you) and
 * the manager builds its groups and sounds through it. Most games only need
 * {@link #play}, {@link #playMusic}, and the volume controls; use {@link #create} when you want
 * a {@link FlixelSound} configured up front without hearing it yet.
 *
 * <p>For internal paths, sounds resolve through the asset pipeline: a loaded
 * {@link FlixelSoundSource} is used when present, otherwise the source is block-loaded first.
 * All file access goes through the {@link FlixelFiles Flixel.files} seam, so
 * audio works identically from a folder, a packaged JAR, or any custom file root.
 */
public class FlixelSoundManager implements FlixelUpdatable, FlixelDestroyable {

  /**
   * The platform-specific sound factory used when constructing the global {@link FlixelSoundManager}.
   *
   * <p>Platform launchers replace this with a real backend (for example, miniaudio on desktop or
   * the Web Audio API on HTML5) before {@link Flixel#start} runs. Leaving it as-is gives a silent
   * no-op, so audio calls never crash even if no factory is installed.
   *
   * <p>Example (in a platform launcher, before {@link Flixel#start}):
   * <pre>{@code
   * FlixelSoundManager.defaultFactory = FlixelMiniAudioFactory.create();
   * }</pre>
   */
  public static FlixelSoundFactory defaultFactory = FlixelNoopSoundFactory.INSTANCE;

  private static final String[] AUDIO_EXTENSIONS = { ".mp3", ".ogg", ".wav", ".flac" };

  private final FlixelSoundFactory factory;
  private final FlixelArray<FlixelSound> activeSounds = new FlixelArray<>(false, 8);
  private FlixelSoundGroup sfxGroup;

  private float masterVolume = 1f;

  /**
   * The current music that's playing, automatically set by {@link #playMusic}.
   */
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
    sfxGroup = factory.createGroup();
    factory.setMasterVolume(masterVolume);
  }

  /**
   * Destroys all non-persistent {@link FlixelSound} instances tracked by this manager, including
   * the current music track if it is not persistent.
   *
   * <p>Called automatically by {@link Flixel#switchState} on every state switch. Sounds whose
   * {@link FlixelSound#isPersist()} flag is set survive the switch unchanged.
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
   * state-switch cleanup and focus pausing still apply. Obtain a file handle from
   * {@link FlixelFiles Flixel.files}:
   *
   * <pre>{@code
   * FlixelSound sfx = Flixel.sound.create(Flixel.files.internal("sfx/jump.ogg"));
   * sfx.setVolume(0.8f);
   * sfx.play();
   * }</pre>
   *
   * @param file Handle to the audio file. Must not be {@code null}.
   * @return The new, idle {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound create(@NotNull FlixelFile file) {
    return create(file, null);
  }

  /**
   * Creates a sound without playing it, in the given group.
   *
   * @param file Handle to the audio file. Must not be {@code null}.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @return The new, idle {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound create(@NotNull FlixelFile file, @Nullable FlixelSoundGroup group) {
    FlixelSoundGroup targetGroup = (group != null) ? group : sfxGroup;
    FlixelSound sound = buildSound(file, targetGroup);
    sound.setManager(this);
    activeSounds.add(sound);
    return sound;
  }

  /**
   * Plays a new sound effect (SFX group) at full volume, without looping.
   *
   * <pre>{@code
   * Flixel.sound.play(Flixel.files.internal("sfx/coin.ogg"));
   * }</pre>
   *
   * @param file Handle to the audio file. Must not be {@code null}.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull FlixelFile file) {
    return play(file, 1f, false, null);
  }

  /**
   * Plays a new sound effect at the given volume.
   *
   * @param file Handle to the audio file. Must not be {@code null}.
   * @param volume Volume to play with.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull FlixelFile file, float volume) {
    return play(file, volume, false, null);
  }

  /**
   * Plays a new sound effect.
   *
   * @param file Handle to the audio file. Must not be {@code null}.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull FlixelFile file, float volume, boolean looping) {
    return play(file, volume, looping, null);
  }

  /**
   * Plays a new sound effect.
   *
   * @param file Handle to the audio file. Must not be {@code null}.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @param group Sound group, or {@code null} to use the default SFX group.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound play(@NotNull FlixelFile file, float volume, boolean looping, @Nullable FlixelSoundGroup group) {
    FlixelSoundGroup targetGroup = (group != null) ? group : sfxGroup;
    return createAndPlay(file, volume, looping, targetGroup);
  }

  /**
   * Sets and plays the current music (SFX group), looping. Stops any previous music.
   *
   * <pre>{@code
   * Flixel.sound.playMusic(Flixel.files.internal("music/theme.ogg"));
   * }</pre>
   *
   * @param file Handle to the music file. Must not be {@code null}.
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull FlixelFile file) {
    return playMusic(file, 1f, true);
  }

  /**
   * Sets and plays the current music. Stops any previous music.
   *
   * @param file Handle to the music file. Must not be {@code null}.
   * @param volume Volume.
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull FlixelFile file, float volume) {
    return playMusic(file, volume, true);
  }

  /**
   * Sets and plays the current music. Stops any previous music.
   *
   * @param file Handle to the music file. Must not be {@code null}.
   * @param volume Volume.
   * @param looping Whether to loop.
   * @return The new music {@link FlixelSound} instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull FlixelFile file, float volume, boolean looping) {
    if (music != null) {
      music.destroy();
      music = null;
    }
    music = createAndPlay(file, volume, looping, sfxGroup);
    return music;
  }

  /**
   * Builds a new {@link FlixelSound} from {@code file} without starting playback.
   *
   * <p>Internal/classpath files go through the asset manager cache using the file's path as the
   * key. Files from {@link FlixelFiles#absolute Flixel.files.absolute} or
   * {@link FlixelFiles#external Flixel.files.external} (whose {@link FlixelFile#getAbsolutePath()}
   * differs from {@link FlixelFile#getPath()}) bypass the cache and are decoded directly.
   *
   * @param file Handle to the audio file.
   * @param targetGroup The group to create the sound in.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  private FlixelSound buildSound(@NotNull FlixelFile file, @NotNull FlixelSoundGroup targetGroup) {
    String path = file.getPath();
    String absolutePath = file.getAbsolutePath();
    if (!absolutePath.equals(path)) {
      FlixelSoundBuffer buffer = FlixelSoundBuffer.read(absolutePath, file);
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
   * Builds a new {@link FlixelSound} from {@code file}, starts playback, and returns it.
   *
   * @param file Handle to the audio file.
   * @param volume The volume to play the sound at.
   * @param looping If {@code true}, the sound will loop.
   * @param targetGroup The group to play the sound in.
   * @return The new {@link FlixelSound} instance.
   */
  @NotNull
  private FlixelSound createAndPlay(
      @NotNull FlixelFile file,
      float volume,
      boolean looping,
      @NotNull FlixelSoundGroup targetGroup) {
    FlixelSound sound = buildSound(file, targetGroup);
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
   * <p>Called automatically by {@link FlixelGame} every frame
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
    if (music != null) {
      music.pause();
    }
  }

  /**
   * Resumes all sounds that were paused by {@link #pause()}. Called when the
   * game regains focus.
   */
  public void resume() {
    sfxGroup.resume();
    if (music != null) {
      music.resume();
    }
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
    factory.destroyEngine();
  }
}
