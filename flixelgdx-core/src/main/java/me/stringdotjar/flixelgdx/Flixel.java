/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;

import games.rednblack.miniaudio.MiniAudio;
import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.asset.FlixelDefaultAssetManager;
import me.stringdotjar.flixelgdx.audio.FlixelAudioManager;
import me.stringdotjar.flixelgdx.audio.FlixelSound;
import me.stringdotjar.flixelgdx.backend.alert.FlixelAlerter;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelUnsupportedReflectionHandler;
import me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode;
import me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay;
import me.stringdotjar.flixelgdx.debug.FlixelDebugWatchManager;
import me.stringdotjar.flixelgdx.group.FlixelGroupable;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;
import me.stringdotjar.flixelgdx.util.FlixelConstants;
import me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager;
import me.stringdotjar.flixelgdx.logging.FlixelLogMode;
import me.stringdotjar.flixelgdx.logging.FlixelLogger;
import me.stringdotjar.flixelgdx.signal.FlixelSignal;
import me.stringdotjar.flixelgdx.signal.FlixelSignalData.UpdateSignalData;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.builders.FlixelNumTweenBuilder;
import me.stringdotjar.flixelgdx.tween.builders.FlixelPropertyTweenBuilder;
import me.stringdotjar.flixelgdx.tween.builders.FlixelVarTweenBuilder;
import me.stringdotjar.flixelgdx.tween.type.FlixelNumTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelVarTween;
import me.stringdotjar.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Global manager and utility class for Flixel.
 *
 * <p>Use this for switching screens, and use {@link #sound} for playing sounds and music
 * ({@code Flixel.sound.playSound()}, {@code Flixel.sound.playMusic()}, etc.).
 * Preload assets with {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager} and
 * {@link me.stringdotjar.flixelgdx.asset.FlixelAsset}.
 */
public final class Flixel {

  /** The current {@code FlixelState} being displayed. */
  private static FlixelState state;

  /** Keyboard input manager. Use {@code Flixel.keys.pressed(key)}, {@code Flixel.keys.justPressed(key)}, etc. */
  @NotNull
  public static FlixelKeyInputManager keys;

  /** Central audio manager. Use {@code Flixel.sound.play()}, {@code Flixel.sound.playMusic()}, etc. */
  @NotNull
  public static FlixelAudioManager sound;

  /** Central asset manager. Use this for loading, caching and managing assets. */
  @NotNull
  public static FlixelAssetManager assets;

  /** The debug watch manager. Access via {@code Flixel.watch.add(...)}, {@code Flixel.watch.remove(...)}, etc. */
  @NotNull
  public static FlixelDebugWatchManager watch;

  /** The default logger used by {@link #info}, {@link #warn}, and {@link #error}. */
  @NotNull
  public static FlixelLogger log;

  /** Global reflection service. Use {@code Flixel.reflect.hasField(target, fieldName)}, etc. */
  @NotNull
  public static FlixelReflection reflect = new FlixelUnsupportedReflectionHandler();

  /** Should the game use antialiasing globally? */
  private static boolean antialiasing = false;

  /** The static instance used to access the core elements of the game. */
  @NotNull
  private static FlixelGame game;

  /** The camera currently being drawn in {@link FlixelGame#draw()}, or {@code null} if not in a camera pass. */
  @Nullable
  private static FlixelCamera drawCamera;

  /** The system to use for displaying alert notifications to the user. */
  @NotNull
  private static FlixelAlerter alerter;

  /** Has the global manager been initialized yet? */
  private static boolean initialized = false;

  /** System used to detect where a log comes from when a log is created. **/
  @NotNull
  private static FlixelStackTraceProvider stackTraceProvider;

  /** Whether the game is running in debug mode. Can only be set once from the launcher. */
  private static boolean debugMode = false;

  /** Guard that ensures {@link #setDebugMode(boolean)} is only called once. */
  private static boolean debugModeSet = false;

  /** The runtime mode (TEST, DEBUG, RELEASE) set by the launcher. */
  private static FlixelRuntimeMode runtimeMode = FlixelRuntimeMode.RELEASE;

  /** Guard that ensures {@link #setRuntimeMode(FlixelRuntimeMode)} is only called once. */
  private static boolean runtimeModeSet = false;

  /** The capped elapsed time for the current frame. Set by {@link FlixelGame} after clamping the raw libGDX delta. */
  protected static float elapsed = 0f;

  /**
   * World bounds used by {@link #overlap} and {@link #collide} for broad-phase culling.
   * Format: {@code [x, y, width, height]}. Defaults to a very large area.
   */
  private static final float[] worldBounds = { -10000f, -10000f, 20000f, 20000f };

  /** Current key used to toggle the debug overlay. */
  private static int debugToggleKey = FlixelConstants.Debug.DEFAULT_TOGGLE_KEY;

  /** Current key used to toggle visual debug (bounding boxes). */
  private static int debugDrawToggleKey = FlixelConstants.Debug.DEFAULT_DRAW_DEBUG_KEY;

  /**
   * Factory used to create the debug overlay when the game starts. Developers can replace
   * this with their own subclass via {@link #setDebugOverlay(Supplier)} before the game
   * starts (i.e. in the launcher, before {@link FlixelGame#create()} runs).
   */
  private static Supplier<FlixelDebugOverlay> debugOverlayFactory = FlixelDebugOverlay::new;

  /** The active debug overlay instance, created by {@link FlixelGame} during startup. */
  private static FlixelDebugOverlay debugOverlay;

  /**
   * Initializes the entire Flixel system.
   *
   * <p>This gets called BEFORE {@link FlixelGame#create()} is executed.
   * It sets up every core system that Flixel needs to work, such as {@link FlixelAssetManager},
   * audio system, key input manager,
   * logger, backend systems for different platforms, and more.
   *
   * <p>This can only be called once. If attempted to be executed again, the game will throw an {@link IllegalStateException}.
   *
   * @param gameInstance The {@link FlixelGame} instance to use.
   * @throws IllegalStateException If Flixel has already been initialized.
   */
  public static void initialize(@NotNull FlixelGame gameInstance) {
    if (initialized) {
      throw new IllegalStateException("Flixel has already been initialized!");
    }

    // Set the game and backend systems.
    game = gameInstance;
    if (alerter == null) {
      throw new IllegalStateException("Flixel alerter not set. Call Flixel.setAlerter(...) before Flixel.initialize(...).");
    }
    if (stackTraceProvider == null) {
      throw new IllegalStateException("Flixel stack trace provider not set. Call Flixel.setStackTraceProvider(...) before Flixel.initialize(...).");
    }

    // Initialize the core systems.
    keys = new FlixelKeyInputManager();
    sound = new FlixelAudioManager();
    watch = new FlixelDebugWatchManager();
    log = new FlixelLogger(FlixelLogMode.SIMPLE);
    if (assets == null) {
      assets = new FlixelDefaultAssetManager();
    }
    if (assets instanceof FlixelDefaultAssetManager dam) {
      dam.ensureMiniAudioLoader();
    }

    // Register default tween types.
    FlixelTween.getGlobalManager()
      .registerTweenType(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class, () -> new FlixelPropertyTween(null))
      .registerTweenType(FlixelVarTween.class, FlixelVarTweenBuilder.class, () -> new FlixelVarTween(null, null, null))
      .registerTweenType(FlixelNumTween.class, FlixelNumTweenBuilder.class, () -> new FlixelNumTween(0, 0, null, null));

    initialized = true;
  }

  /**
   * Sets the system used for displaying alert notifications to the user.
   *
   * <p>This must be set before {@link #initialize(FlixelGame)}. Calling it after initialization
   * throws an exception.
   */
  public static void setAlerter(@NotNull FlixelAlerter alertSystem) {
    if (initialized) {
      throw new IllegalStateException("Cannot change alerter after Flixel has been initialized.");
    }
    if (alertSystem == null) {
      throw new IllegalArgumentException("Alert system cannot be null.");
    }
    alerter = alertSystem;
  }

  /**
   * Sets the system used for providing stack traces on logs.
   *
   * <p>This must be set before {@link #initialize(FlixelGame)}. Calling it after initialization
   * throws an exception.
   */
  public static void setStackTraceProvider(@NotNull FlixelStackTraceProvider provider) {
    if (initialized) {
      throw new IllegalStateException("Cannot change stack trace provider after Flixel has been initialized.");
    }
    if (provider == null) {
      throw new IllegalArgumentException("Stack trace provider cannot be null.");
    }
    stackTraceProvider = provider;
  }

  /**
   * Sets the current state to the provided state, triggers garbage collection and
   * clears all active tweens by default.
   *
   * @param newState The new {@code FlixelState} to set as the current state.
   */
  public static void switchState(FlixelState newState) {
    switchState(newState, true, true);
  }

  /**
   * Sets the current state to the provided state and triggers Java's garbage collector for memory cleanup.
   *
   * @param newState The new {@code FlixelState} to set as the current state.
   * @param clearTweens Should all active tweens be cancelled and their pools be cleared?
   */
  public static void switchState(FlixelState newState, boolean clearTweens) {
    switchState(newState, clearTweens, true);
  }

  /**
   * Sets the current state to the provided state.
   *
   * @param newState The new {@code FlixelState} to set as the current state.
   * @param clearTweens Should all active tweens be cancelled and their pools be cleared?
   * @param triggerGC Should Java's garbage collector be triggered for memory cleanup?
   */
  public static void switchState(FlixelState newState, boolean clearTweens, boolean triggerGC) {
    Signals.preStateSwitch.dispatch(new StateSwitchSignalData(state));
    if (!initialized) {
      throw new IllegalStateException("Flixel has not been initialized yet!");
    }
    if (newState == null) {
      throw new IllegalArgumentException("New state cannot be null!");
    }
    if (triggerGC) {
      System.gc();
    }
    if (state != null) {
      state.destroy();
    }

    if (assets != null) {
      assets.clearNonPersist();
    }
    if (clearTweens) {
      FlixelTween.getGlobalManager()
        .getActiveTweens()
        .forEach(tween -> tween.cancel());
      FlixelTween.getGlobalManager().clearPools();
    }
    game.resetCameras();
    state = newState;
    state.create();
    Signals.postStateSwitch.dispatch(new StateSwitchSignalData(state));
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
    info(log.getDefaultTag(), message);
  }

  public static void info(String tag, Object message) {
    log.info(tag, message);
  }

  public static void warn(Object message) {
    warn(log.getDefaultTag(), message);
  }

  public static void warn(String tag, Object message) {
    log.warn(tag, message);
  }

  public static void error(String message) {
    error(log.getDefaultTag(), message, null);
  }

  public static void error(String tag, Object message) {
    error(tag, message, null);
  }

  public static void error(String tag, Object message, Throwable throwable) {
    log.error(tag, message, throwable);
  }

  public static void setLogger(@NotNull FlixelLogger logger) {
    log = logger;
  }

  public static void setDefaultLogTag(@NotNull String tag) {
    if (log != null) {
      log.setDefaultTag(tag);
    }
  }

  /**
   * Sets the folder where log files are stored, or {@code null} for default
   * (project root in IDE, next to JAR when run from JAR).
   *
   * @param absolutePathToLogsFolder The absolute path to the logs folder, or {@code null} for default.
   */
  public static void setLogsFolder(@Nullable String absolutePathToLogsFolder) {
    if (log != null) {
      log.setLogsFolder(absolutePathToLogsFolder);
    }
  }

  /** Returns the custom logs folder path, or {@code null} if using the default. */
  public static String getLogsFolder() {
    return log != null ? log.getLogsFolder() : null;
  }

  /** Enables or disables writing logs to a file when {@link #startFileLogging()} is called. */
  public static void setCanStoreLogs(boolean canStoreLogs) {
    if (log != null) {
      log.setCanStoreLogs(canStoreLogs);
    }
  }

  /** Returns whether file logging is enabled for the default logger. */
  public static boolean canStoreLogs() {
    return log != null && log.canStoreLogs();
  }

  /** Sets the maximum number of log files to keep. */
  public static void setMaxLogFiles(int maxLogFiles) {
    if (log != null) {
      log.setMaxLogFiles(maxLogFiles);
    }
  }

  /** Returns the maximum number of log files to keep. If no logger is set, returns 0. */
  public static int getMaxLogFiles() {
    return log != null ? log.getMaxLogFiles() : 0;
  }

  /** Starts file logging for the default logger (uses its current canStoreLogs and maxLogFiles). */
  public static void startFileLogging() {
    if (log != null) {
      log.startFileLogging();
    }
  }

  /** Stops the default logger's file writer thread; call during game shutdown. */
  public static void stopFileLogging() {
    if (log != null) {
      log.stopFileLogging();
    }
  }

  public static FlixelAlerter getAlerter() {
    return alerter;
  }

  public static FlixelLogger getLogger() {
    return log;
  }

  public static FlixelLogMode getLogMode() {
    return log != null ? log.getLogMode() : FlixelLogMode.SIMPLE;
  }

  /**
   * Ensures {@link #assets} is available for embedded libGDX usage.
   *
   * <p>If Flixel has not been initialized yet, this creates a default asset manager on first use.
   * Note that audio loaders are only registered once the global audio system is initialized.
   */
  @NotNull
  public static FlixelAssetManager ensureAssets() {
    if (assets == null) {
      assets = new FlixelDefaultAssetManager();
    }
    return assets;
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
    return game.windowSize;
  }

  public static int getWindowWidth() {
    return (int) game.windowSize.x;
  }

  public static int getWindowHeight() {
    return (int) game.windowSize.y;
  }

  public static Vector2 getViewSize() {
    return game.viewSize;
  }

  public static int getViewWidth() {
    return (int) game.viewSize.x;
  }

  public static int getViewHeight() {
    return (int) game.viewSize.y;
  }

  public static MiniAudio getAudioEngine() {
    return sound.getEngine();
  }

  public static float getMasterVolume() {
    return sound.getMasterVolume();
  }

  /**
   * Returns the capped elapsed time (in seconds) for the current frame. This value is clamped
   * between {@link me.stringdotjar.flixelgdx.util.FlixelConstants.Graphics#MIN_ELAPSED} and
   * {@link me.stringdotjar.flixelgdx.util.FlixelConstants.Graphics#MAX_ELAPSED} by
   * {@link FlixelGame} each frame.
   */
  public static float getElapsed() {
    return elapsed;
  }

  /**
   * Sets whether the game is running in debug mode. This may only be called <strong>once</strong>,
   * typically from the platform launcher before the game starts. A second call throws an
   * {@link IllegalStateException}.
   *
   * @param enabled {@code true} to enable debug mode.
   * @throws IllegalStateException If called more than once.
   */
  public static void setDebugMode(boolean enabled) {
    if (debugModeSet) {
      throw new IllegalStateException("Debug mode can only be set once (from the launcher).");
    }
    debugMode = enabled;
    debugModeSet = true;
  }

  public static boolean isDebugMode() {
    return debugMode;
  }

  /**
   * Sets the runtime mode for the game. This may only be called <strong>once</strong>, typically
   * from the platform launcher before the game starts. A second call throws an
   * {@link IllegalStateException}.
   *
   * @param mode The {@link FlixelRuntimeMode} to set.
   * @throws IllegalStateException If called more than once.
   */
  public static void setRuntimeMode(@NotNull FlixelRuntimeMode mode) {
    if (runtimeModeSet) {
      throw new IllegalStateException("Runtime mode can only be set once (from the launcher).");
    }
    runtimeMode = mode;
    runtimeModeSet = true;
  }

  /** Returns the current runtime mode. Defaults to {@link FlixelRuntimeMode#RELEASE}. */
  public static FlixelRuntimeMode getRuntimeMode() {
    return runtimeMode;
  }

  /**
   * Returns the key used to toggle the debug overlay visibility.
   *
   * @see me.stringdotjar.flixelgdx.input.keyboard.FlixelKey
   */
  public static int getDebugToggleKey() {
    return debugToggleKey;
  }

  /**
   * Changes the key used to toggle the debug overlay visibility.
   *
   * @param key A key constant from {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKey}.
   */
  public static void setDebugToggleKey(int key) {
    debugToggleKey = key;
  }

  /**
   * Returns the key used to toggle visual debug (bounding box drawing) on/off.
   *
   * @see me.stringdotjar.flixelgdx.input.keyboard.FlixelKey
   */
  public static int getDebugDrawToggleKey() {
    return debugDrawToggleKey;
  }

  /**
   * Changes the key used to toggle visual debug (bounding box drawing) on/off.
   *
   * @param key A key constant from {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKey}.
   */
  public static void setDebugDrawToggleKey(int key) {
    debugDrawToggleKey = key;
  }

  /**
   * Sets a factory that produces the {@link FlixelDebugOverlay} used when debug mode is
   * enabled. This can be called either in the launcher (before the game starts) or in the
   * {@link FlixelGame#create()} method itself.
   *
   * <p>A factory is used instead of a new instance directly for timing, so that way the
   * debug overlay can be set even before GL context is created.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.setDebugOverlay(MyCustomOverlay::new);
   * }</pre>
   *
   * @param factory A supplier that creates a new {@link FlixelDebugOverlay} (or subclass).
   */
  public static void setDebugOverlay(@NotNull Supplier<FlixelDebugOverlay> factory) {
    debugOverlayFactory = factory;
  }

  /**
   * Returns the active debug overlay instance, or {@code null} when debug mode is off
   * or the overlay has not been created yet.
   */
  public static FlixelDebugOverlay getDebugOverlay() {
    return debugOverlay;
  }

  /**
   * Creates the debug overlay using the registered factory. Called internally by
   * {@link FlixelGame} during startup when debug mode is enabled.
   */
  protected static FlixelDebugOverlay createDebugOverlay() {
    debugOverlay = debugOverlayFactory.get();
    return debugOverlay;
  }

  /**
   * Clears the active debug overlay reference after it has been disposed.
   * {@link FlixelGame#dispose()} calls {@link me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay#destroy()}
   * first; this method only nulls the static handle to avoid double-dispose.
   */
  protected static void clearDebugOverlay() {
    debugOverlay = null;
  }

  /** Returns the Java heap memory currently in use, in bytes. */
  public static long getJavaHeapUsedBytes() {
    Runtime rt = Runtime.getRuntime();
    return rt.totalMemory() - rt.freeMemory();
  }

  /** Returns the Java heap memory currently in use, in megabytes. */
  public static float getJavaHeapUsedMegabytes() {
    return getJavaHeapUsedBytes() / (1024f * 1024f);
  }

  /** Returns the Java heap memory currently in use, in gigabytes. */
  public static float getJavaHeapUsedGigabytes() {
    return getJavaHeapUsedBytes() / (1024f * 1024f * 1024f);
  }

  /** Returns the total Java heap memory allocated by the JVM, in bytes. */
  public static long getJavaHeapTotalBytes() {
    return Runtime.getRuntime().totalMemory();
  }

  /** Returns the total Java heap memory allocated by the JVM, in megabytes. */
  public static float getJavaHeapTotalMegabytes() {
    return getJavaHeapTotalBytes() / (1024f * 1024f);
  }

  /** Returns the total Java heap memory allocated by the JVM, in gigabytes. */
  public static float getJavaHeapTotalGigabytes() {
    return getJavaHeapTotalBytes() / (1024f * 1024f * 1024f);
  }

  /** Returns the maximum Java heap memory available to the JVM, in bytes. */
  public static long getJavaHeapMaxBytes() {
    return Runtime.getRuntime().maxMemory();
  }

  /** Returns the maximum Java heap memory available to the JVM, in megabytes. */
  public static float getJavaHeapMaxMegabytes() {
    return getJavaHeapMaxBytes() / (1024f * 1024f);
  }

  /** Returns the maximum Java heap memory available to the JVM, in gigabytes. */
  public static float getJavaHeapMaxGigabytes() {
    return getJavaHeapMaxBytes() / (1024f * 1024f * 1024f);
  }

  /**
   * Returns an estimate of the native/VRAM heap usage in bytes as reported by libGDX.
   * This is not available on all platforms and may return {@code 0} when unsupported.
   */
  public static long getNativeHeapUsedBytes() {
    return Gdx.app != null ? Gdx.app.getNativeHeap() : 0L;
  }

  /** Returns the native/VRAM heap usage in megabytes. */
  public static float getNativeHeapUsedMegabytes() {
    return getNativeHeapUsedBytes() / (1024f * 1024f);
  }

  /** Returns the native/VRAM heap usage in gigabytes. */
  public static float getNativeHeapUsedGigabytes() {
    return getNativeHeapUsedBytes() / (1024f * 1024f * 1024f);
  }

  /** Returns the current frames-per-second as reported by the graphics backend. */
  public static int getFPS() {
    return Gdx.graphics != null ? Gdx.graphics.getFramesPerSecond() : 0;
  }

  public static FlixelCamera getCamera() {
    return game.getCamera();
  }

  public static FlixelCamera[] getCameras() {
    return game.getCameras().items;
  }

  public static Array<FlixelCamera> getCamerasArray() {
    return game.getCameras();
  }

  public static void addCamera(FlixelCamera camera) {
    game.getCameras().add(camera);
  }

  /**
   * The camera currently being drawn in {@link FlixelGame#draw()}, or {@code null} if not in a camera pass.
   */
  @Nullable
  public static FlixelCamera getDrawCamera() {
    return drawCamera;
  }

  protected static void setDrawCamera(@Nullable FlixelCamera camera) {
    drawCamera = camera;
  }

  /**
   * Whether something with the given {@code cameras} list should render during the current draw pass.
   * {@code null} or an empty array means all cameras; otherwise, the object is drawn only if {@link #getDrawCamera()}
   * is reference-equal to an entry.
   */
  public static boolean isOnDrawCamera(@Nullable FlixelCamera[] cameras) {
    FlixelCamera active = drawCamera;
    if (active == null) {
      return true;
    }
    if (cameras == null || cameras.length == 0) {
      return true;
    }
    for (FlixelCamera c : cameras) {
      if (c == active) {
        return true;
      }
    }
    return false;
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

  /**
   * Sets the runtime reflection service implementation.
   *
   * @param reflection Reflection service to expose as {@link #reflect}.
   */
  public static void setReflection(FlixelReflection reflection) {
    if (reflection == null) {
      throw new IllegalArgumentException("Reflection service cannot be null.");
    }
    Flixel.reflect = reflection;
  }

  public static String getVersion() {
    try (InputStream in = Flixel.class.getResourceAsStream("version.properties")) {
      if (in != null) {
        Properties p = new Properties();
        p.load(in);
        String v = p.getProperty("version");
        if (v != null && !v.isEmpty()) return v;
      }
    } catch (Exception ignored) {
      // Ignored.
    }
    return "Unknown";
  }

  public static void setLogMode(@NotNull FlixelLogMode mode) {
    if (log != null) {
      log.setLogMode(mode);
    }
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

  public static World getWorld() {
    if (game == null) {
      return null;
    }
    return game.getWorld();
  }

  /**
   * Sets the gravity of the global Box2D world. Convenience overload that
   * sets horizontal gravity to {@code 0}.
   *
   * @param gravity Vertical gravity in m/s² (negative = down in most setups).
   */
  public static void setGravity(float gravity) {
    setGravity(0, gravity);
  }

  /**
   * Sets the gravity of the global Box2D world.
   *
   * @param gravityX Horizontal gravity in m/s².
   * @param gravityY Vertical gravity in m/s².
   */
  public static void setGravity(float gravityX, float gravityY) {
    if (game != null) {
      game.setGravity(gravityX, gravityY);
    }
  }

  /**
   * Returns the current gravity of the Box2D world, or {@code (0,0)} if no world exists.
   */
  public static Vector2 getGravity() {
    if (game != null) {
      return game.getGravity();
    }
    return new Vector2(0, 0);
  }

  /**
   * Returns the world bounds used for collision broad-phase culling.
   * The returned array is {@code [x, y, width, height]}.
   */
  public static float[] getWorldBounds() {
    return worldBounds;
  }

  /**
   * Sets the world bounds used for collision culling.
   *
   * @param x Left edge of the world.
   * @param y Top edge of the world.
   * @param width Width of the world in pixels.
   * @param height Height of the world in pixels.
   */
  public static void setWorldBounds(float x, float y, float width, float height) {
    worldBounds[0] = x;
    worldBounds[1] = y;
    worldBounds[2] = width;
    worldBounds[3] = height;
  }

  /**
   * Checks for overlaps between two objects or groups. Can be called with
   * any combination of single {@link FlixelObject}s and {@link FlixelGroupable}s.
   *
   * <p>This is modeled after
   * <a href="https://api.haxeflixel.com/flixel/FlxG.html#overlap">FlxG.overlap</a>.
   *
   * @param objectOrGroup1 First object or group (may be {@code null} to use the current state).
   * @param objectOrGroup2 Second object or group (may be {@code null} to use the current state).
   * @param notifyCallback Called for each overlapping pair. May be {@code null}.
   * @param processCallback If provided, must return {@code true} for the pair to count as overlapping.
   * Pass {@code null} for simple AABB overlap.
   * @return {@code true} if any overlaps were detected.
   */
  public static boolean overlap(@Nullable FlixelBasic objectOrGroup1,
                                @Nullable FlixelBasic objectOrGroup2,
                                @Nullable BiConsumer<FlixelObject, FlixelObject> notifyCallback,
                                @Nullable BiFunction<FlixelObject, FlixelObject, Boolean> processCallback) {
    if (objectOrGroup1 == null) objectOrGroup1 = state;
    if (objectOrGroup2 == null) objectOrGroup2 = state;
    if (objectOrGroup1 == null || objectOrGroup2 == null) return false;
    return overlapInternal(objectOrGroup1, objectOrGroup2, notifyCallback, processCallback);
  }

  /**
   * Shorthand for {@link #overlap(FlixelBasic, FlixelBasic, BiConsumer, BiFunction)}
   * with no callbacks.
   */
  public static boolean overlap(@Nullable FlixelBasic objectOrGroup1, @Nullable FlixelBasic objectOrGroup2) {
    return overlap(objectOrGroup1, objectOrGroup2, null, null);
  }

  /**
   * Checks for overlaps and separates colliding objects. Equivalent to calling
   * {@link #overlap} with {@link FlixelObject#separate} as the process callback.
   *
   * <p>This is modeled after <a href="https://api.haxeflixel.com/flixel/FlxG.html#collide">FlxG.collide</a>.
   *
   * @param objectOrGroup1 First object or group.
   * @param objectOrGroup2 Second object or group.
   * @param notifyCallback Called for each pair that was separated. May be {@code null}.
   * @return {@code true} if any objects were separated.
   */
  public static boolean collide(@Nullable FlixelBasic objectOrGroup1,
                                @Nullable FlixelBasic objectOrGroup2,
                                @Nullable BiConsumer<FlixelObject, FlixelObject> notifyCallback) {
    return overlap(objectOrGroup1, objectOrGroup2, notifyCallback, FlixelObject::separate);
  }

  /**
   * Shorthand for {@link #collide(FlixelBasic, FlixelBasic, BiConsumer)} with
   * no {@code notifyCallback}.
   */
  public static boolean collide(@Nullable FlixelBasic objectOrGroup1,
                                @Nullable FlixelBasic objectOrGroup2) {
    return collide(objectOrGroup1, objectOrGroup2, null);
  }

  private static boolean overlapInternal(FlixelBasic obj1, FlixelBasic obj2,
                                         BiConsumer<FlixelObject, FlixelObject> notifyCallback,
                                         BiFunction<FlixelObject, FlixelObject, Boolean> processCallback) {
    boolean result = false;

    if (obj1 instanceof FlixelGroupable<?> group1) {
      Array<? extends FlixelBasic> members = (Array<? extends FlixelBasic>) group1.getMembers();
      for (FlixelBasic member : members) {
        if (member != null && member.exists) {
          result |= overlapInternal(member, obj2, notifyCallback, processCallback);
        }
      }
      return result;
    }

    if (obj2 instanceof FlixelGroupable<?> group2) {
      Array<? extends FlixelBasic> members = (Array<? extends FlixelBasic>) group2.getMembers();
      for (FlixelBasic member : members) {
        if (member != null && member.exists) {
          result |= overlapInternal(obj1, member, notifyCallback, processCallback);
        }
      }
      return result;
    }

    if (!(obj1 instanceof FlixelObject fo1) || !(obj2 instanceof FlixelObject fo2)) return false;
    if (obj1 == obj2) return false;
    if (!fo1.exists || !fo2.exists) return false;

    boolean overlaps = fo1.getX() < fo2.getX() + fo2.getWidth()
      && fo1.getX() + fo1.getWidth() > fo2.getX()
      && fo1.getY() < fo2.getY() + fo2.getHeight()
      && fo1.getY() + fo1.getHeight() > fo2.getY();

    if (!overlaps) {
      return false;
    }

    if (processCallback != null) {
      if (!processCallback.apply(fo1, fo2)) {
        return false;
      }
    }

    if (notifyCallback != null) {
      notifyCallback.accept(fo1, fo2);
    }

    return true;
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
