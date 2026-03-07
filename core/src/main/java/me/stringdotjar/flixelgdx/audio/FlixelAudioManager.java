package me.stringdotjar.flixelgdx.audio;

import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;
import me.stringdotjar.flixelgdx.util.FlixelPathsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central manager for all audio: FlixelSound instances, master volume, sound groups (SFX and Music),
 * and focus-based pause/resume.
 *
 * <p>Access via {@link me.stringdotjar.flixelgdx.Flixel#sound}. Supports separate groups for
 * sound effects and music, global master volume, and automatic pause when the game loses focus
 * (and resume when it regains focus).
 */
public final class FlixelAudioManager {

  private final MiniAudio engine;
  private final MAGroup sfxGroup;
  private final MAGroup musicGroup;

  private float masterVolume = 1f;
  private FlixelSound music;

  private final List<FlixelSound> activeSounds = new ArrayList<>();
  private final Set<FlixelSound> pausedByFocus = new HashSet<>();

  public FlixelAudioManager() {
    engine = new MiniAudio();
    sfxGroup = engine.createGroup();
    musicGroup = engine.createGroup();
  }

  /** Returns the underlying MiniAudio engine for advanced use and asset loading. */
  @NotNull
  public MiniAudio getEngine() {
    return engine;
  }

  /** Group for sound effects. Use for playing sounds or custom sounds that should be categorized as SFX. */
  @NotNull
  public MAGroup getSfxGroup() {
    return sfxGroup;
  }

  /** Group for music. Used by {@link #playMusic}. */
  @NotNull
  public MAGroup getMusicGroup() {
    return musicGroup;
  }

  /**
   * Returns the default group used when no group is specified (SFX group).
   * Provided for backward compatibility with code that expects a single "sounds" group.
   */
  @NotNull
  public MAGroup getSoundsGroup() {
    return sfxGroup;
  }

  @Nullable
  public FlixelSound getMusic() {
    return music;
  }

  /** Master volume in 0-1. Values > 1 are clamped to 1 when applied to the engine. */
  public float getMasterVolume() {
    return masterVolume;
  }

  /**
   * Sets the global master volume applied to all sounds.
   *
   * @param volume New master volume (values &gt; 1 are clamped to 1).
   */
  public void setMasterVolume(float volume) {
    float clamped = volume > 1f ? 1f : volume;
    engine.setMasterVolume(clamped);
    masterVolume = clamped;
  }

  /**
   * Plays a new sound effect (SFX group).
   *
   * @param path Path to the sound (internal or resolved via {@link FlixelPathsUtil}).
   * @return The new FlixelSound instance.
   */
  @NotNull
  public FlixelSound playSound(@NotNull String path) {
    return playSound(path, 1f, false, null, false);
  }

  /** @see #playSound(String, float, boolean, MAGroup, boolean) */
  @NotNull
  public FlixelSound playSound(@NotNull String path, float volume) {
    return playSound(path, volume, false, null, false);
  }

  /** @see #playSound(String, float, boolean, MAGroup, boolean) */
  @NotNull
  public FlixelSound playSound(@NotNull String path, float volume, boolean looping) {
    return playSound(path, volume, looping, null, false);
  }

  /** @see #playSound(String, float, boolean, MAGroup, boolean) */
  @NotNull
  public FlixelSound playSound(@NotNull String path, float volume, boolean looping, @Nullable MAGroup group) {
    return playSound(path, volume, looping, group, false);
  }

  /**
   * Plays a new sound effect.
   *
   * @param path Path to the sound.
   * @param volume Volume to play with.
   * @param looping Whether to loop.
   * @param group Sound group, or null to use the default SFX group.
   * @param external If true, path is used as-is (for external files, e.g. mobile).
   * @return The new FlixelSound instance.
   */
  @NotNull
  public FlixelSound playSound(@NotNull String path, float volume, boolean looping, @Nullable MAGroup group, boolean external) {
    String resolvedPath = external ? path : FlixelPathsUtil.resolveAudioPath(path);
    MAGroup targetGroup = (group != null) ? group : sfxGroup;
    MASound sound = engine.createSound(resolvedPath, (short) 0, targetGroup, external);
    FlixelSound flixelSound = new FlixelSound(sound);
    flixelSound.setVolume(volume);
    flixelSound.setLooped(looping);
    flixelSound.play();
    registerSound(flixelSound);
    return flixelSound;
  }

  /**
   * Sets and plays the current music (music group). Stops any previous music.
   *
   * @param path Path to the music file.
   * @return The new music FlixelSound instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull String path) {
    return playMusic(path, 1f, true, false);
  }

  /** @see #playMusic(String, float, boolean, boolean) */
  @NotNull
  public FlixelSound playMusic(@NotNull String path, float volume) {
    return playMusic(path, volume, true, false);
  }

  /** @see #playMusic(String, float, boolean, boolean) */
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
   * @param external If true, path is used as-is (e.g. for mobile external storage).
   * @return The new music FlixelSound instance.
   */
  @NotNull
  public FlixelSound playMusic(@NotNull String path, float volume, boolean looping, boolean external) {
    if (music != null) {
      music.stop();
    }
    String resolvedPath = external ? path : FlixelPathsUtil.resolveAudioPath(path);
    MASound sound = engine.createSound(resolvedPath, (short) 0, musicGroup, external);
    music = new FlixelSound(sound);
    music.setVolume(volume);
    music.setLooped(looping);
    music.play();
    registerSound(music);
    return music;
  }

  private void registerSound(@NotNull FlixelSound flixelSound) {
    activeSounds.add(flixelSound);
  }

  /**
   * Pauses all currently playing sounds. Used when the game loses focus or is minimized.
   * Only sounds that were playing are paused; they can be resumed with {@link #resume()}.
   */
  public void pause() {
    pausedByFocus.clear();
    for (FlixelSound s : activeSounds) {
      if (s.isPlaying()) {
        s.pause();
        pausedByFocus.add(s);
      }
    }
  }

  /**
   * Resumes all sounds that were paused by {@link #pause()}. Called when the game regains focus.
   */
  public void resume() {
    for (FlixelSound s : pausedByFocus) {
      s.resume();
    }
    pausedByFocus.clear();
  }

  /**
   * Disposes the current music (if any), all groups, and the engine. Call once when the game is
   * shutting down. After this, the manager must not be used.
   */
  public void dispose() {
    if (music != null) {
      music.dispose();
      music = null;
    }
    activeSounds.clear();
    pausedByFocus.clear();
    sfxGroup.dispose();
    musicGroup.dispose();
    engine.dispose();
  }
}
