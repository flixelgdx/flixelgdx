package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.loader.MASoundLoader;
import me.stringdotjar.flixelgdx.audio.FlixelSound;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;
import me.stringdotjar.flixelgdx.util.FlixelPathsUtil;
import me.stringdotjar.flixelgdx.backend.FlixelAlerter;
import me.stringdotjar.flixelgdx.display.FlixelCamera;
import me.stringdotjar.flixelgdx.display.FlixelState;
import me.stringdotjar.flixelgdx.logging.FlixelLogMode;
import me.stringdotjar.flixelgdx.logging.FlixelLogger;
import me.stringdotjar.flixelgdx.signal.FlixelSignal;
import me.stringdotjar.flixelgdx.signal.FlixelSignalData.UpdateSignalData;
import me.stringdotjar.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Properties;

/**
 * Global manager and utility class for Flixel.
 *
 * <p>This is where you want to do the main things, like switching screens, playing sounds/music, etc.
 */
public final class Flixel {

  /** The current {@code FlixelState} being displayed. */
  private static FlixelState state;

  /** The main audio object used to create, */
  private static MiniAudio engine;

  /** The global asset manager used to preload and cache assets. */
  private static AssetManager assetManager;

  /** Should the game use antialiasing globally? */
  private static boolean antialiasing = false;

  /** The audio group for all sound effects, including the current music. */
  private static MAGroup soundsGroup;

  /** The sound for playing music throughout the game. */
  private static FlixelSound music;

  /** The current master volume that is set. */
  private static float masterVolume = 1;

  /** The static instance used to access the core elements of the game. */
  private static FlixelGame game;

  /** The system to use for displaying alert notifications to the user. */
  private static FlixelAlerter alerter;

  /** Has the global manager been initialized yet? */
  private static boolean initialized = false;

  /** The default logger used by {@link #info}, {@link #warn}, and {@link #error}. */
  private static FlixelLogger defaultLogger;

  /** System used to detect where a log comes from when a log is created. **/
  private static FlixelStackTraceProvider stackTraceProvider;

  /**
   * Initializes the global manager.
   *
   * <p>This can only be called once. If attempted to be executed again, the game will throw an
   * exception.
   *
   * @param gameInstance The instance of the game to use.
   * @param alertSystem The system to use for displaying alert notifications to the user.
   * @param stackTraceProviderSystem The system to use for providing stack traces on logs.
   * @throws IllegalStateException If Flixel has already been initialized.
   */
  public static void initialize(@NotNull FlixelGame gameInstance,
                                @NotNull FlixelAlerter alertSystem,
                                @NotNull FlixelStackTraceProvider stackTraceProviderSystem) {
    if (initialized) {
      throw new IllegalStateException("Flixel has already been initialized!");
    }
    game = gameInstance;
    alerter = alertSystem;
    stackTraceProvider = stackTraceProviderSystem;
    defaultLogger = new FlixelLogger(FlixelLogMode.SIMPLE);

    assetManager = new AssetManager();

    // Set up the game's global audio system.
    engine = new MiniAudio();
    soundsGroup = engine.createGroup();
    assetManager.setLoader(MASound.class, new MASoundLoader(engine, assetManager.getFileHandleResolver()));

    initialized = true;
  }

  /**
   * Sets the current screen to the provided screen.
   *
   * @param newState The new {@code FlixelState} to set as the current screen.
   */
  public static void switchState(FlixelState newState) {
    Signals.preStateSwitch.dispatch(new StateSwitchSignalData(newState));
    if (!initialized) {
      throw new IllegalStateException("Flixel has not been initialized yet!");
    }
    if (newState == null) {
      throw new IllegalArgumentException("New state cannot be null!");
    }
    if (state != null) {
      state.hide();
      state.dispose();
    }
    game.resetCameras();
    state = newState;
    state.create();
    Signals.postStateSwitch.dispatch(new StateSwitchSignalData(newState));
  }

  /**
   * Plays a new sound effect.
   *
   * <p>When you want to play a sound externally, outside the assets folder, you can use a {@link
   * FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * Flixel.playSound(FlixelPathsUtil.external("your/path/here").path());
   * }</pre>
   *
   * @param path The path to load the sound from. Note that if you're loading an external sound
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @return The new sound instance.
   */
  public static FlixelSound playSound(String path) {
    return playSound(path, 1, false, null, false);
  }

  /**
   * Plays a new sound effect.
   *
   * <p>When you want to play a sound externally, outside the assets folder, you can use a {@link
   * FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * Flixel.playSound(FlixelPathsUtil.external("your/path/here").path(), 1);
   * }</pre>
   *
   * @param path The path to load the sound from. Note that if you're loading an external sound
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new sound with.
   * @return The new sound instance.
   */
  public static FlixelSound playSound(String path, float volume) {
    return playSound(path, volume, false, null, false);
  }

  /**
   * Plays a new sound effect.
   *
   * <p>When you want to play a sound externally, outside the assets folder, you can use a {@link
   * FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * Flixel.playSound(FlixelPathsUtil.external("your/path/here").path(), 1, false);
   * }</pre>
   *
   * @param path The path to load the sound from. Note that if you're loading an external sound
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new sound with.
   * @param looping Should the new sound loop indefinitely?
   * @return The new sound instance.
   */
  public static FlixelSound playSound(String path, float volume, boolean looping) {
    return playSound(path, volume, looping, null, false);
  }

  /**
   * Plays a new sound effect.
   *
   * <p>When you want to play a sound externally, outside the assets folder, you can use a {@link
   * FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * // If null is passed down for the group, then the default sound group will be used.
   * Flixel.playSound(FlixelPathsUtil.external("your/path/here").path(), 1, false, null);
   * }</pre>
   *
   * @param path The path to load the sound from. Note that if you're loading an external sound
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new sound with.
   * @param looping Should the new sound loop indefinitely?
   * @param group The sound group to add the new sound to. If {@code null} is passed down, then the
   * default sound group will be used.
   * @return The new sound instance.
   */
  public static FlixelSound playSound(String path, float volume, boolean looping, MAGroup group) {
    return playSound(path, volume, looping, group, false);
  }

  /**
   * Plays a new sound effect.
   *
   * <p>When you want to play a sound externally, outside the assets folder, you can use a {@link
   * FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * // If null is passed down for the group, then the default sound group will be used.
   * // For the boolean attribute "external", you only should make it true for mobile builds,
   * // otherwise just simply leave it be or make it "false" for other platforms like desktop.
   * Flixel.playSound(FlixelPathsUtil.external("your/path/here").path(), 1, false, null, true);
   * }</pre>
   *
   * @param path The path to load the sound from. Note that if you're loading an external sound
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new sound with.
   * @param looping Should the new sound loop indefinitely?
   * @param group The sound group to add the new sound to. If {@code null} is passed down, then the
   * default sound group will be used.
   * @param external Should this sound be loaded externally? (This is only for mobile platforms!)
   * @return The new sound instance.
   */
  public static FlixelSound playSound(@NotNull String path, float volume, boolean looping, MAGroup group, boolean external) {
    String resolvedPath = external ? path : FlixelPathsUtil.resolveAudioPath(path);
    MASound sound = engine.createSound(resolvedPath, (short) 0, (group != null) ? group : soundsGroup, external);
    FlixelSound flixelSound = new FlixelSound(sound);
    flixelSound.setVolume(volume);
    flixelSound.setLooped(looping);
    flixelSound.play();
    return flixelSound;
  }

  /**
   * Sets the current music playing for the entire game.
   *
   * <p>When you want to play music located externally, outside the assets folder, you can use a
   * {@link FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * Flixel.playMusic(FlixelPathsUtil.external("your/path/here").path());
   * }</pre>
   *
   * @param path The path to load the music from. Note that if you're loading an external sound file
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @return The new music sound instance.
   */
  public static FlixelSound playMusic(String path) {
    return playMusic(path, 1, true, false);
  }

  /**
   * Sets the current music playing for the entire game.
   *
   * <p>When you want to play music located externally, outside the assets folder, you can use a
   * {@link FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * Flixel.playMusic(FlixelPathsUtil.external("your/path/here").path(), 1);
   * }</pre>
   *
   * @param path The path to load the music from. Note that if you're loading an external sound file
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new music with.
   * @return The new music sound instance.
   */
  public static FlixelSound playMusic(String path, float volume) {
    return playMusic(path, volume, true, false);
  }

  /**
   * Sets the current music playing for the entire game.
   *
   * <p>When you want to play music located externally, outside the assets folder, you can use a
   * {@link FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * Flixel.playMusic(FlixelPathsUtil.external("your/path/here").path(), 1, false);
   * }</pre>
   *
   * @param path The path to load the music from. Note that if you're loading an external sound file
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new music with.
   * @param looping Should the new music loop indefinitely?
   * @return The new music sound instance.
   */
  public static FlixelSound playMusic(String path, float volume, boolean looping) {
    return playMusic(path, volume, looping, false);
  }

  /**
   * Sets the current music playing for the entire game.
   *
   * <p>When you want to play music located externally, outside the assets folder, you can use a
   * {@link FileHandle} like so:
   *
   * <pre>{@code
   * // Notice how it uses the FlixelPathsUtil class provided by Flixel'.
   * // For the boolean attribute "external", you only should make it true for mobile builds,
   * // otherwise just simply leave it be or make it "false" for other platforms like desktop.
   * Flixel.playMusic(FlixelPathsUtil.external("your/path/here").path(), 1, false, true);
   * }</pre>
   *
   * @param path The path to load the music from. Note that if you're loading an external sound file
   * outside the game's assets, you should use {@link FileHandle}; otherwise, just pass down a
   * regular string (without {@code assets/} at the beginning).
   * @param volume The volume to play the new music with.
   * @param looping Should the new music loop indefinitely?
   * @param external Should this music be loaded externally? (This is only for mobile platforms!)
   * @return The new music sound instance.
   */
  public static FlixelSound playMusic(String path, float volume, boolean looping, boolean external) {
    if (music != null) {
      music.stop();
    }
    String resolvedPath = external ? path : FlixelPathsUtil.resolveAudioPath(path);
    MASound sound = engine.createSound(resolvedPath, (short) 0, soundsGroup, external);
    music = new FlixelSound(sound);
    music.setVolume(volume);
    music.setLooped(looping);
    music.play();
    return music;
  }

  /**
   * Sets the game master/global volume, which is automatically applied to all current sounds.
   *
   * @param volume The new master volume to set.
   */
  public static void setMasterVolume(float volume) {
    engine.setMasterVolume(!(volume > 1.0) ? volume : 1.0f);
    masterVolume = volume;
  }

  /**
   * Shows an info alert notification to the user.
   *
   * @param title The title of the alert.
   * @param message The message of the alert.
   */
  public static void showInfoAlert(String title, String message) {
    alerter.showInfoAlert(title, message);
  }

  /**
   * Shows a warning alert notification to the user.
   *
   * @param title The title of the alert.
   * @param message The message of the alert.
   */
  public static void showWarningAlert(String title, String message) {
    alerter.showWarningAlert(title, message);
  }

  /**
   * Shows an error alert notification to the user.
   *
   * @param title The title of the alert.
   * @param message The message of the alert.
   */
  public static void showErrorAlert(String title, String message) {
    alerter.showErrorAlert(title, message);
  }

  public static boolean keyPressed(int key) {
    return Gdx.input.isKeyPressed(key);
  }

  public static boolean keyJustPressed(int key) {
    return Gdx.input.isKeyJustPressed(key);
  }

  public static void info(Object message) {
    info(defaultLogger.getDefaultTag(), message);
  }

  public static void info(String tag, Object message) {
    defaultLogger.info(tag, message);
  }

  public static void warn(Object message) {
    warn(defaultLogger.getDefaultTag(), message);
  }

  public static void warn(String tag, Object message) {
    defaultLogger.warn(tag, message);
  }

  public static void error(String message) {
    error(defaultLogger.getDefaultTag(), message, null);
  }

  public static void error(String tag, Object message) {
    error(tag, message, null);
  }

  public static void error(String tag, Object message, Throwable throwable) {
    defaultLogger.error(tag, message, throwable);
  }

  public static void setLogger(@NotNull FlixelLogger logger) {
    defaultLogger = logger;
  }

  public static void setDefaultLogTag(@NotNull String tag) {
    defaultLogger.setDefaultTag(tag);
  }

  public static FlixelAlerter getAlerter() {
    return alerter;
  }

  public static FlixelLogger getLogger() {
    return defaultLogger;
  }

  public static FlixelLogMode getLogMode() {
    return defaultLogger.getLogMode();
  }

  public static FlixelGame getGame() {
    return game;
  }

  public static Stage getStage() {
    return game.stage;
  }

  public static FlixelState getState() {
    return state;
  }

  public static FlixelSound getMusic() {
    return music;
  }

  public static Vector2 getWindowSize() {
    return game.viewSize;
  }

  public static int getWindowWidth() {
    return (int) game.viewSize.x;
  }

  public static int getWindowHeight() {
    return (int) game.viewSize.y;
  }

  public static MiniAudio getAudioEngine() {
    return engine;
  }

  public static float getMasterVolume() {
    return masterVolume;
  }

  public static AssetManager getAssetManager() {
    return assetManager;
  }

  public static MAGroup getSoundsGroup() {
    return soundsGroup;
  }

  public static float getElapsed() {
    return Gdx.graphics.getDeltaTime();
  }

  public static FlixelCamera getCamera() {
    return game.getCamera();
  }

  public static boolean isFullscreen() {
    return Gdx.graphics.isFullscreen();
  }

  public static ApplicationType getPlatform() {
    return Gdx.app.getType();
  }

  public static FlixelStackTraceProvider getStackTraceProvider() {
    return stackTraceProvider;
  }

  public static String getVersion() {
    try (InputStream in = Flixel.class.getResourceAsStream("version.properties")) {
      if (in != null) {
        Properties p = new Properties();
        p.load(in);
        String v = p.getProperty("version");
        if (v != null && !v.isEmpty()) return v;
      }
    } catch (Exception ignored) {}
    return "Unknown";
  }

  public static void setLogMode(@NotNull FlixelLogMode mode) {
    defaultLogger.setLogMode(mode);
  }

  public static boolean globalAntialiasing() {
    return antialiasing;
  }

  public static void setAntialiasing(boolean enabled) {
    if (enabled == antialiasing) {
      return;
    }
    antialiasing = enabled;

    if (state == null) {
      return;
    }

    var members = state.getMembers();
    var mbrs = members.begin();
    for (int i = 0; i < members.size; i++) {
      var member = mbrs[i];
      if (member == null) {
        continue;
      }
      if (member instanceof FlixelSprite sprite) {
        sprite.setAntialiasing(enabled);
      }
    }
  }

  /**
   * Contains all the global events that get dispatched when something happens in the game.
   *
   * <p>This includes anything from the screen being switched, the game updating every frame, and
   * just about everything you can think of.
   *
   * <p>IMPORTANT DETAIL!: Anything with the {@code pre} and {@code post} prefixes always mean the
   * same thing. If a signal has {@code pre}, then the signal gets ran BEFORE any functionality is
   * executed, and {@code post} means AFTER all functionality was executed.
   */
  public static final class Signals {

    public static final FlixelSignal<UpdateSignalData> preUpdate = new FlixelSignal<>();
    public static final FlixelSignal<UpdateSignalData> postUpdate = new FlixelSignal<>();
    public static final FlixelSignal<Void> preDraw = new FlixelSignal<>();
    public static final FlixelSignal<Void> postDraw = new FlixelSignal<>();
    public static final FlixelSignal<StateSwitchSignalData> preStateSwitch = new FlixelSignal<>();
    public static final FlixelSignal<StateSwitchSignalData> postStateSwitch = new FlixelSignal<>();
    public static final FlixelSignal<Void> preGameClose = new FlixelSignal<>();
    public static final FlixelSignal<Void> postGameClose = new FlixelSignal<>();
    public static final FlixelSignal<Void> windowFocused = new FlixelSignal<>();
    public static final FlixelSignal<Void> windowUnfocused = new FlixelSignal<>();
    public static final FlixelSignal<Void> windowMinimized = new FlixelSignal<>();

    private Signals() {}
  }

  private Flixel() {}
}
