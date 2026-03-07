package me.stringdotjar.flixelgdx.audio;

import games.rednblack.miniaudio.MASound;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.signal.FlixelSignal;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Flixel sound object that wraps an {@link MASound} from the miniaudio library and exposes an API
 * consistent with HaxeFlixel's {@code FlxSound}.
 *
 * <p>Provides volume, pitch, pan, play/pause/stop/resume, fade-in/fade-out, position (time), and
 * an {@link #onComplete} signal when the sound finishes (for non-looping sounds).
 *
 * @see <a href="https://api.haxeflixel.com/flixel/sound/FlxSound.html">FlxSound (HaxeFlixel)</a>
 */
public class FlixelSound extends FlixelBasic {

  private static final float SEC_TO_MS = 1000f;
  private static final float MS_TO_SEC = 1f / SEC_TO_MS;

  private static final ConcurrentHashMap<String, String> audioPathCache = new ConcurrentHashMap<>();

  /** The underlying miniaudio sound. Use {@link #getMASound()} for external access. */
  @NotNull
  private final MASound sound;

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

  /** Signal dispatched when the sound reaches its end (non-looping). */
  @NotNull
  public final FlixelSignal<Void> onComplete = new FlixelSignal<>();

  /**
   * Creates a new Flixel sound wrapping the given file path.
   * 
   * @param path The path to the sound file.
   */
  public FlixelSound(@NotNull FileHandle path) {
    this(Flixel.getAudioEngine().createSound(path.path(), (short) 0, null, false));
  }

  /**
   * Creates a Flixel sound wrapping the given MASound.
   *
   * @param sound The miniaudio sound to wrap (must not be null).
   */
  public FlixelSound(@NotNull MASound sound) {
    super();
    this.sound = sound;
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

  /** Volume in [0, 1] (values > 1 are allowed for louder). */
  public float getVolume() {
    return sound.getVolume();
  }

  /** Sets volume (0 = silent, 1 = default, >1 = louder). */
  public void setVolume(float volume) {
    sound.setVolume(volume);
  }

  /** Pitch multiplier; 1 = default, >1 = higher. */
  public float getPitch() {
    return pitch;
  }

  /** Sets pitch; must be > 0. */
  public void setPitch(float pitch) {
    this.pitch = pitch;
    sound.setPitch(pitch);
  }

  /** Pan in [-1, 1]; -1 = left, 0 = center, 1 = right. */
  public float getPan() {
    return pan;
  }

  /** Sets pan in [-1, 1]. */
  public void setPan(float pan) {
    this.pan = pan;
    sound.setPan(pan);
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

  public void setLooped(boolean looped) {
    sound.setLooping(looped);
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
  public void setEndTime(@Nullable Float endTimeMs) {
    this.endTimeMs = endTimeMs;
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
    fadeTween = FlixelTween.tween(settings);
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
    fadeTween = FlixelTween.tween(settings);
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
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
    sound.setPosition(x, y, 0f);
  }

  public boolean isAutoDestroy() {
    return autoDestroy;
  }

  public void setAutoDestroy(boolean autoDestroy) {
    this.autoDestroy = autoDestroy;
  }

  public boolean isPersist() {
    return persist;
  }

  public void setPersist(boolean persist) {
    this.persist = persist;
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

  @Override
  public void destroy() {
    cancelFadeTween();
    onComplete.clear();
    super.destroy();
  }

  /**
   * Disposes the underlying MASound and destroys this wrapper. Call when the sound is no longer
   * needed. After dispose, this instance must not be used.
   */
  public void dispose() {
    cancelFadeTween();
    onComplete.clear();
    sound.dispose();
    destroy();
  }

  public static ConcurrentHashMap<String, String> getAudioPathCache() {
    return audioPathCache;
  }

  /**
   * Extracts the path to the audio file and converts it to an absolute path
   * so MiniAudio can open it and play it.
   * 
   * @param path The path to the audio file.
   * @return The absolute path to the audio file.
   */
  public static String extractAudioPath(@NotNull String path) {
    FileHandle handle = Gdx.files.absolute(path);
    if (handle.file().exists()) {
      return handle.file().getAbsolutePath();
    }
    // Asset is inside a JAR, copy it out to a temp file so MiniAudio can open it.
    String ext = path.contains(".") ? path.substring(path.lastIndexOf('.')) : "";
    try {
      File temp = File.createTempFile("flixelaudio_", ext);
      temp.deleteOnExit();
      handle.copyTo(new FileHandle(temp));
      return temp.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract audio asset from JAR: " + path, e);
    }
  }
}
