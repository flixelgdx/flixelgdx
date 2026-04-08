/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay;
import me.stringdotjar.flixelgdx.text.FlixelFontRegistry;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.util.FlixelConstants;
import me.stringdotjar.flixelgdx.util.timer.FlixelTimer;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;
import me.stringdotjar.flixelgdx.util.signal.FlixelSignalData.UpdateSignalData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The game object used for containing the main loop and core elements of the Flixel game.
 *
 * <p>To actually use this properly, you need to create a subclass of this and override
 * the methods you want to change.
 *
 * <p>It is strongly advised that you do <b>NOT</b> use this class to add the main gameplay logic to your game;
 * your code should go into your {@link FlixelState} or libGDX Screen classes instead.
 *
 * <p>It is recommended for using this in the following way:
 *
 * <pre>{@code
 * // Create a new subclass of FlixelGame.
 * // Remember that you can override any methods to add extra functionality
 * // to the game's behavior.
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame(String title, int width, int height, FlixelState initialState) {
 *     super(title, width, height, initialState);
 *   }
 * }
 * }</pre>
 *
 * Then, in a platform-specific launcher, you can create a new instance of your game and run it:
 *
 * <pre>{@code
 * // Example of how to create a new game instance and run it using the LWJGL3 launcher.
 * public class Lwjgl3Launcher {
 *
 *   public static void main(String[] args) {
 *     if (StartupHelper.startNewJvmIfRequired()) { // This handles macOS support and helps on Windows.
 *       return;
 *     }
 *
 *     MyGame game = new MyGame(
 *       "My Game",
 *       800,
 *       600,
 *       new InitialState() // The initial state the game enters when it starts!
 *     );
 *
 *     FlixelLwjgl3Launcher.launch(game);
 *   }
 * }
 * }</pre>
 */
public abstract class FlixelGame implements ApplicationListener, FlixelUpdatable, FlixelDrawable, FlixelDestroyable {

  /** The title displayed on the game's window. */
  protected String title;

  /** The size of the game's starting window position and its first camera. */
  protected Vector2 viewSize;

  /** The current window size stored in a vector object. */
  protected Vector2 windowSize;

  /**
   * Produces the root {@link FlixelState} each time {@link #create()} runs (including after {@link Flixel#resetGame()}).
   * Use {@code () -> new MyState()} for a fresh instance per session, or {@code () -> sharedState} to reuse one
   * object (its {@link FlixelState#destroy()} / {@link FlixelState#create()} lifecycle still runs via {@link Flixel#switchState}).
   */
  @NotNull
  protected Supplier<FlixelState> initialStateFactory;

  /** The framerate of how fast the game should update and render. */
  private int framerate;

  /** Should the game use VSync to limit the framerate to the monitor's refresh rate? */
  private boolean vsync;

  /** Should the game start in fullscreen mode? */
  protected boolean fullscreen;

  /** Should the game pause update calls and audio when the window loses focus or is minimized? */
  public boolean autoPause = true;

  /** Is the game's window currently focused? */
  private boolean isFocused = true;

  /** Is the game's window currently minimized? */
  private boolean isMinimized = false;

  /** The main stage used for rendering all screens and sprites on screen. */
  protected Stage stage;

  /** The main sprite batch used for rendering all sprites on screen. */
  protected SpriteBatch batch;

  /** The background color of the entire game's window (full-framebuffer clear before camera passes). */
  protected Color bgColor = new Color(Color.BLACK);

  /** 1x1 white texture used to draw solid fills (camera bg, FX); tinted via {@link SpriteBatch#setColor}. */
  protected Texture bgTexture;

  /** Where all the global cameras are stored. */
  protected Array<FlixelCamera> cameras;

  /** Is the game currently closing? */
  private boolean isClosing = false;

  /** Has the game successfully shut down? */
  private boolean isClosed = false;

  /** When true, skips gameplay/state/camera follow updates (debug pause). */
  private boolean gamePaused = false;

  /** 2D array of saved camera scroll values when the game is paused for debugging. */
  @Nullable
  private float[][] debugPauseCameraScroll;

  /** Array of saved camera zoom values when the game is paused for debugging. */
  @Nullable
  private float[] debugPauseCameraZoom;

  /** Reusable signal data for preUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData preUpdateData = new UpdateSignalData();

  /** Reusable signal data for postUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData postUpdateData = new UpdateSignalData();

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param initialScreen The initial screen to load when the game starts.
   */
  public FlixelGame(String title, FlixelState initialScreen) {
    this(title, 640, 360, initialScreen, 60, true, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialScreen The initial screen to load when the game starts.
   */
  public FlixelGame(String title, int width, int height, FlixelState initialScreen) {
    this(title, width, height, initialScreen, 60, true, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialScreen The initial screen to load when the game starts.
   * @param framerate The framerate of how fast the game should update and render.
   */
  public FlixelGame(String title, int width, int height, FlixelState initialScreen, int framerate) {
    this(title, width, height, initialScreen, framerate, true, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialScreen The initial screen to load when the game starts.
   * @param framerate The framerate of how fast the game should update and render.
   * @param vsync Should the game use VSync to limit the framerate to the monitor's refresh rate?
   */
  public FlixelGame(String title, int width, int height, FlixelState initialScreen, int framerate, boolean vsync) {
    this(title, width, height, initialScreen, framerate, vsync, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialScreen The initial screen to load when the game starts.
   * @param framerate The framerate of how fast the game should update and render.
   * @param vsync Should the game use VSync to limit the framerate to the monitor's refresh rate?
   * @param fullscreen Should the game start in fullscreen mode?
   */
  public FlixelGame(String title, int width, int height, FlixelState initialScreen, int framerate, boolean vsync, boolean fullscreen) {
    this(title, width, height, () -> initialScreen, framerate, vsync, fullscreen);
  }

  /**
   * Same as {@link #FlixelGame(String, int, int, FlixelState, int, boolean, boolean)} but supplies a new root state
   * from a factory (recommended after {@link Flixel#resetGame()} so each cold start can use {@code new MyState()}).
   *
   * @param initialStateFactory Non-null supplier; invoked from {@link #create()} to obtain the root state.
   */
  public FlixelGame(String title, int width, int height, @NotNull Supplier<FlixelState> initialStateFactory, int framerate, boolean vsync, boolean fullscreen) {
    this.title = title;
    this.viewSize = new Vector2(width, height);
    this.windowSize = new Vector2(width, height);
    this.initialStateFactory = Objects.requireNonNull(initialStateFactory, "The initial state factory cannot be null!");
    this.framerate = framerate;
    this.vsync = vsync;
    this.fullscreen = fullscreen;
  }

  /**
   * Called when the game is created. This is where you should initialize your game's resources.
   *
   * <p>This method configures the crash handler, sets up input processing, initializes the debug overlay, configures
   * the ANSI system for color output in terminals, and then switches to the initial screen.
   *
   * <p>This method is called automatically by libGDX's {@link ApplicationListener#create()} method when the game is
   * created, so it is not necessary to call this method manually in most cases. However, it can be overridden to
   * perform custom initialization before the game is created.
   *
   * @see ApplicationListener#create()
   */
  @Override
  public void create() {
    configureCrashHandler(); // This should ALWAYS be called first no matter what!

    batch = new SpriteBatch();
    cameras = new Array<>(FlixelCamera[]::new);
    cameras.add(new FlixelCamera((int) viewSize.x, (int) viewSize.y));
    stage = new Stage(getCamera().getViewport(), batch);

    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    bgTexture = new Texture(pixmap);
    pixmap.dispose();

    // Keyboard + mouse processors first on the multiplexer (scroll, etc.)
    if (Flixel.keys != null || Flixel.mouse != null) {
      InputProcessor current = Gdx.input.getInputProcessor();
      InputMultiplexer m;
      if (current instanceof InputMultiplexer multiplexer) {
        m = multiplexer;
      } else {
        m = new InputMultiplexer();
        if (current != null) {
          m.addProcessor(current);
        }
        Gdx.input.setInputProcessor(m);
      }
      int idx = 0;
      if (Flixel.keys != null) {
        m.addProcessor(idx++, Flixel.keys.getInputProcessor());
      }
      if (Flixel.mouse != null) {
        m.addProcessor(idx, Flixel.mouse.getInputProcessor());
      }
    }

    // Create the debug overlay when debug mode is enabled.
    if (Flixel.isDebugMode()) {
      FlixelDebugOverlay overlay = Flixel.createDebugOverlay();
      if (Flixel.getLogger() != null) {
        Flixel.getLogger().addLogListener(overlay.getLogListener());
      }
    }

    Flixel.switchState(initialStateFactory.get(), true, true, initialStateFactory);
  }

  @Override
  public void resize(int width, int height) {
    windowSize.x = width;
    windowSize.y = height;

    for (FlixelCamera camera : cameras) {
      camera.update(width, height, camera.centerCameraOnResize);
    }

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null) {
      debugOverlay.resize(width, height);
    }

    FlixelState state = Flixel.getState();
    if (state != null) {
      state.resize(width, height);
    }
  }

  /**
   * Updates the logic of the game's loop.
   *
   * @param elapsed The amount of time that occurred in the last frame.
   */
  @Override
  public void update(float elapsed) {
    preUpdateData.set(elapsed);
    Flixel.Signals.preUpdate.dispatch(preUpdateData);

    // Always update input first!
    if (Flixel.keys != null) {
      Flixel.keys.update();
    }
    if (Flixel.mouse != null) {
      Flixel.mouse.update();
    }

    if (!gamePaused) {
      stage.act(elapsed);
      FlixelTween.updateTweens(elapsed);
      FlixelTimer.getGlobalManager().update(elapsed * Flixel.getTimeScale());

      // Walk the state/substate chain. Each state in the chain is updated only
      // if it is the active (innermost) state or if its persistentUpdate flag is true.
      FlixelState current = Flixel.getState();
      while (current != null) {
        FlixelState sub = current.getSubState();
        boolean hasSubState = (sub != null);

        if (!hasSubState || current.persistentUpdate) {
          current.update(elapsed);
        }

        current = sub;
      }

      // Update all cameras.
      FlixelCamera[] cameraItems = cameras.items;
      for (int i = 0, n = cameras.size; i < n; i++) {
        cameraItems[i].update(elapsed);
      }
    }

    // Capture key state at end of frame so firstJustPressed/firstJustReleased work next frame.
    if (Flixel.keys != null) {
      Flixel.keys.endFrame();
    }

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null && Flixel.isDebugMode()) {
      debugOverlay.update(elapsed);
    }

    if (Flixel.mouse != null) {
      Flixel.mouse.endFrame();
    }

    postUpdateData.set(elapsed);
    Flixel.Signals.postUpdate.dispatch(postUpdateData);
  }

  /**
   * Updates the graphics and display of the game.
   *
   * @param batch The batch to use for drawing the game.
   */
  @Override
  public void draw(Batch batch) {
    Flixel.Signals.preDraw.dispatch();

    ScreenUtils.clear(bgColor); // Clear the screen to refresh the screen.
    FlixelState state = Flixel.getState();

    // Loop through all cameras and draw the state/substate chain onto each camera.
    FlixelCamera[] cameraItems = cameras.items;
    for (int ci = 0, cn = cameras.size; ci < cn; ci++) {
      FlixelCamera camera = cameraItems[ci];
      Flixel.setDrawCamera(camera);
      try {
        if (gamePaused) {
          camera.applyLibCameraTransform();
        }
        camera.getViewport().apply();
        batch.setProjectionMatrix(camera.getCamera().combined);
        batch.begin();

        camera.fill(camera.bgColor, camera.useBgAlphaBlending, 1f, batch, bgTexture);

        // Walk the state/substate chain. Each state is drawn only if it is the
        // active (innermost) state or if its persistentDraw flag is true.
        FlixelState current = state;
        while (current != null) {
          FlixelState sub = current.getSubState();
          boolean hasSubState = (sub != null);

          if (!hasSubState || current.persistentDraw) {
            current.draw(batch);
          }

          current = sub;
        }

        camera.drawFX(batch, bgTexture);

        batch.end();
      } finally {
        Flixel.setDrawCamera(null);
      }
    }

    stage.draw();

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null) {
      debugOverlay.drawBoundingBoxes(cameras.items);
      debugOverlay.draw();
    }

    Flixel.Signals.postDraw.dispatch();
  }

  /**
   * Updates the game's global and internal {@link #update(float)} and {@link #draw(Batch)} methods, with elapsed time clamped
   * to the min and max values to prevent major lag spikes.
   *
   * <p>This method is called automatically by libGDX's {@link ApplicationListener#render()} method when the game is
   * running, so it is not necessary to override this method in most cases. However, it can be overridden to
   * perform custom updating/rendering before the game is updated/rendered.
   *
   * <p>This method is kept non-final for compatibility with existing projects that want to extend to this class
   * and use its structured game loop. It's advised to override {@link #update(float)} and {@link #draw(Batch)} instead to
   * perform custom updating and rendering.
   *
   * @see #update(float)
   * @see #draw(Batch)
   * @see ApplicationListener#render()
   */
  @Override
  public void render() {
    float rawDelta = Gdx.graphics.getDeltaTime();
    float elapsed = Math.max(FlixelConstants.Graphics.MIN_ELAPSED, Math.min(rawDelta, FlixelConstants.Graphics.MAX_ELAPSED));
    Flixel.elapsed = elapsed;

    windowSize.x = Gdx.graphics.getWidth();
    windowSize.y = Gdx.graphics.getHeight();

    fullscreen = Gdx.graphics.isFullscreen();

    if (!autoPause || isFocused) {
      update(elapsed);
    }
    draw(batch);
  }

  public boolean isGamePaused() {
    return gamePaused;
  }

  /**
   * Pauses the game's update loop. This is mostly used by the debugger, although
   * you might find it useful for other purposes.
   *
   * @param gamePaused Whether the game should be paused or not.
   */
  public void setGamePaused(boolean gamePaused) {
    if (this.gamePaused == gamePaused) {
      return;
    }
    if (gamePaused) {
      snapshotCamerasForDebugPause();
      Flixel.sound.pause();
    } else {
      restoreCamerasAfterDebugPause();
      if (!autoPause || (isFocused && !isMinimized)) {
        Flixel.sound.resume();
      }
    }
    this.gamePaused = gamePaused;
  }

  private void snapshotCamerasForDebugPause() {
    if (cameras == null || cameras.size == 0) {
      debugPauseCameraScroll = null;
      debugPauseCameraZoom = null;
      return;
    }
    int n = cameras.size;
    debugPauseCameraScroll = new float[n][2];
    debugPauseCameraZoom = new float[n];
    for (int i = 0; i < n; i++) {
      FlixelCamera c = cameras.get(i);
      debugPauseCameraScroll[i][0] = c.scroll.x;
      debugPauseCameraScroll[i][1] = c.scroll.y;
      debugPauseCameraZoom[i] = c.getZoom();
    }
  }

  private void restoreCamerasAfterDebugPause() {
    if (debugPauseCameraScroll == null || debugPauseCameraZoom == null || cameras == null) {
      debugPauseCameraScroll = null;
      debugPauseCameraZoom = null;
      return;
    }
    int n = Math.min(debugPauseCameraScroll.length, Math.min(debugPauseCameraZoom.length, cameras.size));
    for (int i = 0; i < n; i++) {
      FlixelCamera c = cameras.get(i);
      float sx = debugPauseCameraScroll[i][0];
      float sy = debugPauseCameraScroll[i][1];
      c.restoreScrollAndZoom(sx, sy, debugPauseCameraZoom[i]);
    }
    debugPauseCameraScroll = null;
    debugPauseCameraZoom = null;
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  /** Called when the user regains focus on the game's window. */
  public void onWindowFocused() {
    isFocused = true;
    if (autoPause && !isMinimized && !gamePaused) {
      Flixel.sound.resume();
    }
    Flixel.Signals.windowFocused.dispatch();
  }

  /** Called when the user loses focus on the game's window, while also not being minimized. */
  public void onWindowUnfocused() {
    isFocused = false;
    if (autoPause) {
      Flixel.sound.pause();
    }
    Flixel.Signals.windowUnfocused.dispatch();
  }

  /**
   * Called when the user minimizes the game's window.
   *
   * @param iconified Whether the window is iconified (minimized) or not. This parameter is provided
   * for compatibility with the window listener in the LWJGL3 (desktop) launcher.
   */
  public void onWindowMinimized(boolean iconified) {
    isMinimized = iconified;
    isFocused = false;
    if (autoPause) {
      Flixel.sound.pause();
    }
    Flixel.Signals.windowMinimized.dispatch();
  }

  /**
   * Sets fullscreen mode for the game's window.
   *
   * @param enabled If the game's window should be in fullscreen mode.
   */
  public void setFullscreen(boolean enabled) {
    if (enabled) {
      Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    } else {
      Gdx.graphics.setWindowedMode((int) viewSize.x, (int) viewSize.y);
    }
  }

  /** Toggles fullscreen mode on or off, depending on the current state. */
  public void toggleFullscreen() {
    setFullscreen(!Flixel.isFullscreen());
  }

  /**
   * Toggles auto-pause on or off.
   *
   * @return The new value of autoPause after toggling.
   */
  public boolean toggleAutoPause() {
    autoPause = !autoPause;
    return autoPause;
  }

  /**
   * Gets called when the game is closing to perform custom cleanup
   * after core resources are disposed and before the log thread shuts down, so any
   * logs written here (e.g., via {@link Flixel#info}) are persisted to the log file.
   */
  protected void close() {}

  /** @see #destroy() */
  @Override
  public void dispose() {
    destroy();
  }

  /**
   * Destroys the game and all of its resources. Note that this closes the game entirely.
   * If you want to reset the game without closing it, use {@link #reset()} instead.
   *
   * @see #reset()
   */
  @Override
  public void destroy() {
    if (isClosing) {
      return;
    }
    isClosing = true;
    teardownSessionCore(true);
    isClosed = true;
  }

  /**
   * Tears down this session's cameras, state, stage, and batch without application-exit semantics.
   * No {@link Flixel#stopFileLogging()}, ANSI uninstall, or {@code postGameClose} signal, and does not dispose
   * {@link Flixel#assets} or {@link Flixel#sound} (callers such as {@link Flixel#resetGame()} own those). Leaves
   * {@link #isClosed()} {@code false} so the process can call {@link Flixel#initialize(FlixelGame)} again.
   */
  public void reset() {
    if (batch == null && stage == null) {
      return;
    }
    teardownSessionCore(false);
    isClosing = false;
    isClosed = false;
  }

  private void teardownSessionCore(boolean permanentShutdown) {
    if (permanentShutdown) {
      Flixel.Signals.preGameClose.dispatch();
    }

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null) {
      if (Flixel.getLogger() != null) {
        Flixel.getLogger().removeLogListener(debugOverlay.getLogListener());
      }
      debugOverlay.destroy();
      Flixel.clearDebugOverlay();
    }

    if (Flixel.getState() != null) {
      Flixel.getState().destroy();
    }
    if (stage != null) {
      stage.dispose();
      stage = null;
    }
    if (batch != null) {
      batch.dispose();
      batch = null;
    }
    if (bgTexture != null) {
      bgTexture.dispose();
      bgTexture = null;
    }

    if (permanentShutdown) {
      if (Flixel.assets != null) {
        Flixel.assets.dispose();
      }
      if (Flixel.sound != null) {
        Flixel.sound.destroy();
      }
    }

    cameras = null;
    debugPauseCameraScroll = null;
    debugPauseCameraZoom = null;
    gamePaused = false;

    FlixelFontRegistry.dispose();

    if (permanentShutdown) {
      close();

      Flixel.Signals.postGameClose.dispatch();

      Flixel.stopFileLogging();
    }
  }

  /**
   * Configures Flixel's crash handler to safely catch uncaught exceptions and gracefully close the game.
   */
  protected void configureCrashHandler() {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      String logs = FlixelRuntimeUtil.getFullExceptionMessage(throwable);
      String msg = "There was an uncaught exception on thread \"" + thread.getName() + "\"!\n" + logs;
      Flixel.error(msg);
      Flixel.showErrorAlert("Uncaught Exception", msg);
      dispose();
      // Only use Gdx.app.exit() on non-iOS platforms to avoid App Store guideline violations!
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
        Gdx.app.exit();
      }
    });
  }

  /**
   * Sets a custom folder for log files. Call before {@link #create()} so that file logging uses
   * this folder instead of the default (project root in IDE, directory containing the JAR when run from a JAR).
   *
   * @param absolutePathToLogsFolder Absolute path to the logs folder, or {@code null} to use the default.
   */
  public void setLogsFolder(String absolutePathToLogsFolder) {
    Flixel.setLogsFolder(absolutePathToLogsFolder);
  }

  /**
   * Gets the first camera that is part of the list. If the list is {@code null} or empty, then a new list (with a
   * default camera accordingly) is created.
   *
   * @return The first camera in the list.
   */
  public FlixelCamera getCamera() {
    Vector2 windowSize = Flixel.getViewSize();
    if (cameras == null) {
      cameras = new Array<>(FlixelCamera[]::new);
    }
    if (cameras.isEmpty()) {
      cameras.add(new FlixelCamera((int) windowSize.x, (int) windowSize.y));
      cameras.first().apply();
      stage.setViewport(cameras.first().getViewport());
    }
    return cameras.first();
  }

  /**
   * Resets the camera list to contain a single default camera with the current window size as its viewport.
   */
  public void resetCameras() {
    FlixelCamera camera = new FlixelCamera((int) viewSize.x, (int) viewSize.y);
    camera.update((int) windowSize.x, (int) windowSize.y, camera.centerCameraOnResize);
    cameras = new Array<>(FlixelCamera[]::new);
    cameras.add(camera);
    stage.setViewport(camera.getViewport());
  }

  public String getTitle() {
    return title;
  }

  public Vector2 getViewSize() {
    return viewSize;
  }

  public int getViewWidth() {
    return (int) viewSize.x;
  }

  public int getViewHeight() {
    return (int) viewSize.y;
  }

  public Vector2 getWindowSize() {
    return windowSize;
  }

  public int getWindowWidth() {
    return (int) windowSize.x;
  }

  public int getWindowHeight() {
    return (int) windowSize.y;
  }

  public boolean isFocused() {
    return isFocused;
  }

  public Stage getStage() {
    return stage;
  }

  public Array<FlixelCamera> getCameras() {
    return cameras;
  }

  public SpriteBatch getBatch() {
    return batch;
  }

  public Color getBgColor() {
    return bgColor;
  }

  public void setBgColor(@NotNull Color bgColor) {
    if (bgColor == null) {
      return;
    }
    this.bgColor.set(bgColor);
  }

  public boolean isMinimized() {
    return isMinimized;
  }

  public boolean isClosing() {
    return isClosing;
  }

  public boolean isClosed() {
    return isClosed;
  }

  public int getFramerate() {
    return framerate;
  }

  public void setFramerate(int framerate) {
    this.framerate = framerate;
    Gdx.graphics.setForegroundFPS(framerate);
  }

  public boolean isVsync() {
    return vsync;
  }

  public void setVsync(boolean vsync) {
    this.vsync = vsync;
    Gdx.graphics.setVSync(vsync);
  }

  public boolean isFullscreen() {
    return fullscreen;
  }

  public void setWindowSize(Vector2 newSize) {
    viewSize = newSize;
    Gdx.graphics.setWindowedMode((int) newSize.x, (int) newSize.y);
  }

}
