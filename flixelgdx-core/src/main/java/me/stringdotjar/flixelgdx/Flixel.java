package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;

import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.loader.MASoundLoader;
import games.rednblack.miniaudio.MiniAudio;
import me.stringdotjar.flixelgdx.audio.FlixelAudioManager;
import me.stringdotjar.flixelgdx.audio.FlixelSound;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;
import me.stringdotjar.flixelgdx.backend.FlixelAlerter;
import me.stringdotjar.flixelgdx.display.FlixelCamera;
import me.stringdotjar.flixelgdx.display.FlixelState;
import me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager;
import me.stringdotjar.flixelgdx.logging.FlixelLogMode;
import me.stringdotjar.flixelgdx.logging.FlixelLogger;
import me.stringdotjar.flixelgdx.signal.FlixelSignal;
import me.stringdotjar.flixelgdx.signal.FlixelSignalData.UpdateSignalData;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Properties;

/**
 * Global manager and utility class for Flixel.
 *
 * <p>Use this for switching screens, and use {@link #sound} for playing sounds and music
 * ({@code Flixel.sound.playSound()}, {@code Flixel.sound.playMusic()}, etc.).
 */
public final class Flixel {

  /** The current {@code FlixelState} being displayed. */
  private static FlixelState state;

  /** Central audio manager: use {@code Flixel.sound.play()}, {@code Flixel.sound.playMusic()}, etc. */
  public static FlixelAudioManager sound;

  /** Keyboard input manager: use {@code Flixel.keys.keyPressed()}, {@code Flixel.keys.keyJustPressed()}, etc. */
  public static FlixelKeyInputManager keys;

  /** The global asset manager used to preload and cache assets. */
  private static AssetManager assetManager;

  /** Should the game use antialiasing globally? */
  private static boolean antialiasing = false;

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

    sound = new FlixelAudioManager();
    assetManager.setLoader(MASound.class, new MASoundLoader(sound.getEngine(), assetManager.getFileHandleResolver()));

    keys = new FlixelKeyInputManager();

    initialized = true;
  }

  /**
   * Sets the current screen to the provided screen, and clears all active tweens by default.
   * 
   * @param newState The new {@code FlixelState} to set as the current screen.
   */
  public static void switchState(FlixelState newState) {
    switchState(newState, true);
  }

  /**
   * Sets the current screen to the provided screen.
   *
   * @param newState The new {@code FlixelState} to set as the current screen.
   * @param clearTweens Whether to clear all active tweens.
   */
  public static void switchState(FlixelState newState, boolean clearTweens) {
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
    if (clearTweens) {
      FlixelTween.getGlobalManager()
        .getActiveTweens()
        .forEach(tween -> tween.cancel());
      FlixelTween.getGlobalManager()
        .getTweenPool()
        .clear();
    }
    game.resetCameras();
    state = newState;
    state.create();
    Signals.postStateSwitch.dispatch(new StateSwitchSignalData(newState));
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
    return sound.getMusic();
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
    return sound.getEngine();
  }

  public static float getMasterVolume() {
    return sound.getMasterVolume();
  }

  public static AssetManager getAssetManager() {
    return assetManager;
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

    // Apply antialiasing to all sprites in the current state.
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
