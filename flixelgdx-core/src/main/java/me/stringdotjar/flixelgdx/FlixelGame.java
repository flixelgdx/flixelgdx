package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.display.FlixelCamera;
import me.stringdotjar.flixelgdx.display.FlixelState;
import me.stringdotjar.flixelgdx.logging.FlixelLogger;
import me.stringdotjar.flixelgdx.text.FlixelFontRegistry;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;
import org.fusesource.jansi.AnsiConsole;

import static me.stringdotjar.flixelgdx.signal.FlixelSignalData.UpdateSignalData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The game object used for containing the main loop and core elements of the Flixel game.
 *
 * <p>To actually use this properly, you need to create a subclass of this and override
 * the methods you want to change.
 *
 * <p>It is recommended for using this in the following way:
 *
 * <pre>{@code
 * // Create a new subclass of FlixelGame.
 * // Remember that you can override any methods to add extra functionality
 * // to the game's behavior.
 * public class MyGame extends FlixelGame {
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
 * }
 */
public abstract class FlixelGame implements ApplicationListener {

  /** The title displayed on the game's window. */
  protected String title;

  /** The size of the game's starting window position and its first camera. */
  protected Vector2 viewSize;

  /** The current window size stored in a vector object. */
  protected Vector2 windowSize;

  /** The entry point screen the game starts in (which becomes null after the game is done setting up!). */
  protected FlixelState initialScreen;

  /** The framerate of how fast the game should update and render. */
  private int framerate;

  /** Should the game use VSync to limit the framerate to the monitor's refresh rate? */
  private boolean vsync;

  /** Should the game start in fullscreen mode? */
  protected boolean fullscreen;

  /** Is the game's window currently focused? */
  private boolean isFocused = true;

  /** Is the game's window currently minimized? */
  private boolean isMinimized = false;

  /** The main stage used for rendering all screens and sprites on screen. */
  protected Stage stage;

  /** The main sprite batch used for rendering all sprites on screen. */
  protected SpriteBatch batch;

  /** The 1x1 texture used to draw the background color of the current screen. */
  protected Texture bgTexture;

  /** Where all the global cameras are stored. */
  protected SnapshotArray<FlixelCamera> cameras;

  /** The queue of logs to be written to the log file. */
  private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();

  /** The maximum number of log files FlixelGDX can store. */
  protected int maxLogFiles = 10;

  /** Can the game store logs to a text file and store them in a folder of the game's working directory? */
  private boolean canStoreLogs = true;

  /** Is the game currently closing? */
  private boolean isClosing = false;

  /** Has the game successfully shut down? */
  private boolean isClosed = false;

  /** Reusable signal data for preUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData preUpdateData = new UpdateSignalData();

  /** Reusable signal data for postUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData postUpdateData = new UpdateSignalData();

  /** Signals the log writer thread to drain the queue and exit. */
  private volatile boolean logWriterShutdownRequested = false;

  /** Lock for coordinating between log producers and the log writer thread via wait/notify. */
  private final Object logQueueLock = new Object();

  /** Reference to the log writer thread so we can wait for it to finish during dispose. */
  private Thread logThread;

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
    this.title = title;
    this.viewSize = new Vector2(width, height);
    this.windowSize = new Vector2(width, height);
    this.initialScreen = initialScreen;
    this.framerate = framerate;
    this.vsync = vsync;
    this.fullscreen = fullscreen;
  }

  @Override
  public void create() {
    configureCrashHandler(); // This should ALWAYS be called first no matter what!

    // Install Jansi's ANSI-aware output stream so that ANSI color codes render correctly in
    // terminals that don't natively support them (e.g., the Windows terminal). Skipped in the
    // IDE because the IDE console already handles ANSI codes without Jansi's help.
    if (FlixelRuntimeUtil.isRunningFromJar()) {
      AnsiConsole.systemInstall();
    }

    batch = new SpriteBatch();
    cameras = new SnapshotArray<>(FlixelCamera[]::new);
    cameras.add(new FlixelCamera((int) viewSize.x, (int) viewSize.y));
    stage = new Stage(getCamera().getViewport(), batch);

    // Set up the background color for the game.
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    bgTexture = new Texture(pixmap);
    pixmap.dispose();

    // Set up the log thread to write logs to a file.
    setupLogWriterThread();

    Flixel.switchState(initialScreen);
    initialScreen = null;
  }

  @Override
  public void resize(int width, int height) {
    FlixelCamera[] camerasArray = cameras.begin();
    for (int i = 0; i < cameras.size; i++) {
      FlixelCamera camera = camerasArray[i];
      if (camera != null) {
        camera.update(width, height, true);
      }
    }
    cameras.end();
  }

  /**
   * Updates the logic of the game's loop.
   *
   * @param elapsed The amount of time that occurred in the last frame.
   */
  public void update(float elapsed) {
    preUpdateData.set(elapsed);
    Flixel.Signals.preUpdate.dispatch(preUpdateData);

    stage.act(elapsed);
    FlixelTween.getGlobalManager().update(elapsed);

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
    FlixelCamera[] cams = cameras.begin();
    for (int i = 0; i < cameras.size; i++) {
      FlixelCamera camera = cams[i];
      if (camera == null) {
        continue;
      }
      camera.update(elapsed);
    }
    cameras.end();

    postUpdateData.set(elapsed);
    Flixel.Signals.postUpdate.dispatch(postUpdateData);
  }

  /**
   * Updates the graphics and display of the game.
   */
  public void draw() {
    Flixel.Signals.preDraw.dispatch();

    ScreenUtils.clear(Color.BLACK); // Clear the screen to refresh the screen.
    FlixelState state = Flixel.getState();

    // Loop through all cameras and draw the state/substate chain onto each camera.
    FlixelCamera[] cams = cameras.begin();
    for (int i = 0; i < cameras.size; i++) {
      FlixelCamera camera = cams[i];
      if (camera == null) {
        continue;
      }

      batch.setProjectionMatrix(camera.getCamera().combined);
      batch.begin();

      // Walk the state/substate chain. Each state is drawn only if it is the
      // active (innermost) state or if its persistentDraw flag is true.
      FlixelState current = state;
      while (current != null) {
        FlixelState sub = current.getSubState();
        boolean hasSubState = (sub != null);

        if (!hasSubState || current.persistentDraw) {
          batch.setColor(current.getBgColor());
          batch.draw(bgTexture, camera.x, camera.y, camera.getWorldWidth(), camera.getWorldHeight());
          batch.setColor(Color.WHITE);

          current.draw(batch);
        }

        current = sub;
      }

      batch.end();
    }
    cameras.end();

    stage.draw();

    Flixel.Signals.postDraw.dispatch();
  }

  @Override
  public final void render() {
    float delta = Gdx.graphics.getDeltaTime();

    windowSize.x = Gdx.graphics.getWidth();
    windowSize.y = Gdx.graphics.getHeight();

    fullscreen = Gdx.graphics.isFullscreen();

    update(delta);
    draw();
  }

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  /** Called when the user regains focus on the game's window. */
  public void onWindowFocused() {
    isFocused = true;
    Flixel.Signals.windowFocused.dispatch();
  }

  /** Called when the user loses focus on the game's window, while also not being minimized. */
  public void onWindowUnfocused() {
    if (isMinimized) {
      return;
    }
    isFocused = false;
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
    if (!isMinimized) {
      return;
    }
    isFocused = false;
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
   * Gets called when the game is closing to perform custom cleanup
   * after core resources are disposed and before the log thread shuts down, so any
   * logs written here (e.g., via {@link Flixel#info}) are persisted to the log file.
   */
  protected void close() {}

  @Override
  public final void dispose() {
    if (isClosing) {
      return;
    }
    isClosing = true;

    Flixel.Signals.preGameClose.dispatch();

    Flixel.getState().hide();
    Flixel.getState().dispose();
    stage.dispose();
    batch.dispose();
    bgTexture.dispose();

    Flixel.sound.dispose();

    FlixelFontRegistry.dispose();

    if (AnsiConsole.isInstalled()) {
      AnsiConsole.systemUninstall();
    }

    close();

    Flixel.Signals.postGameClose.dispatch();

    // Signal the log thread to drain the queue and exit. Must happen AFTER all dispose-time
    // logs are added, so they get written before the thread exits.
    synchronized (logQueueLock) {
      logWriterShutdownRequested = true;
      logQueueLock.notify();
    }

    // Wait for the log thread to flush all remaining logs to disk before marking closed.
    if (logThread != null && logThread.isAlive()) {
      try {
        logThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    isClosed = true;
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
   * Sets up the log writer thread to write logs to a file.
   */
  protected void setupLogWriterThread() {
    String path = FlixelRuntimeUtil.getWorkingDirectory();
    if (path == null) {
      return;
    }
    String logsFolder = path.substring(0, path.lastIndexOf('/')) + "/logs/";

    if (canStoreLogs) {
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
      String date = now.format(formatter);

      // Create/get the path to the logs folder, which is inside the game's working directory.
      Gdx.files.absolute(logsFolder).mkdirs();

      // Check if the logs folder has too many log files, and if so, delete the oldest ones.
      // We prune when count >= maxLogFiles so that after creating this run's log, we have at most maxLogFiles.
      FileHandle[] logFiles = Gdx.files.absolute(logsFolder).list();
      if (logFiles != null && logFiles.length >= maxLogFiles) {
        // Sort by name so we delete the oldest first (flixel-yyyy-MM-dd_HH-mm-ss.log is lexicographically ordered).
        Arrays.sort(logFiles, Comparator.comparing(FileHandle::name));
        int toDelete = logFiles.length - maxLogFiles + 1;
        for (int i = 0; i < toDelete; i++) {
          logFiles[i].delete();
        }
      }

      FileHandle logFile = Gdx.files.absolute(logsFolder + "/flixel-" + date + ".log");

      // Wire the default logger (used by Flixel.info()/warn()/error()) to also write to the file.
      FlixelLogger defaultLogger = Flixel.getLogger();
      if (defaultLogger != null) {
        defaultLogger.setLogFileLocation(logFile);
        defaultLogger.setFileLineConsumer(this::enqueueLog);
      }

      final FileHandle logFileForThread = logFile;
      logThread = new Thread(() -> {
        try {
          // Keep running until shutdown is requested AND the queue is fully drained.
          // This ensures logs added during dispose() (e.g. "Disposing...") are written.
          while (true) {
            String log = logQueue.poll();
            if (log != null) {
              logFileForThread.writeString(log + "\n", true);
            } else {
              synchronized (logQueueLock) {
                if (logWriterShutdownRequested) {
                  break;
                }
                try {
                  logQueueLock.wait();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            }
          }
        } catch (Exception ignored) {}
      });
      logThread.setName("FlixelGDX Log Thread");
      logThread.setDaemon(true);
      logThread.start();
    } else {
      FileHandle[] logFiles = Gdx.files.absolute(logsFolder).list();
      if (logFiles != null) {
        for (FileHandle logFile : logFiles) {
          logFile.delete();
        }
      }
    }
  }

  /**
   * Adds a log entry to the queue and notifies the log writer thread. Prefer this over
   * {@link #getLogQueue()} when adding logs so the writer wakes immediately instead of polling.
   */
  public void enqueueLog(String log) {
    logQueue.add(log);
    synchronized (logQueueLock) {
      logQueueLock.notify();
    }
  }

  /**
   * Gets the first camera that is part of the list. If the list is {@code null} or empty, then a new list (with a
   * default camera accordingly) is created.
   *
   * @return The first camera in the list.
   */
  public FlixelCamera getCamera() {
    Vector2 windowSize = Flixel.getWindowSize();
    if (cameras == null) {
      cameras = new SnapshotArray<>(FlixelCamera[]::new);
    }
    if (cameras.isEmpty()) {
      cameras.add(new FlixelCamera((int) windowSize.x, (int) windowSize.y));
      stage.setViewport(cameras.first().getViewport());
    }
    return cameras.first();
  }

  /**
   * Resets the camera list to contain a single default camera with the current window size as its viewport.
   */
  public void resetCameras() {
    FlixelCamera camera = new FlixelCamera((int) viewSize.x, (int) viewSize.y);
    camera.update((int) windowSize.x, (int) windowSize.y, true);
    cameras.clear();
    cameras.add(camera);
    stage.setViewport(camera.getViewport());
  }

  public String getTitle() {
    return title;
  }

  public Vector2 getViewSize() {
    return viewSize;
  }

  public Vector2 getWindowSize() {
    return windowSize;
  }

  public int getWindowWidth() {
    return (int) windowSize.x;
  }

  public  int getWindowHeight() {
    return (int) windowSize.y;
  }

  public boolean isFocused() {
    return isFocused;
  }

  public Stage getStage() {
    return stage;
  }

  public boolean canStoreLogs() {
    return canStoreLogs;
  }

  public void setCanStoreLogs(boolean canStoreLogs) {
    this.canStoreLogs = canStoreLogs;
  }

  public ConcurrentLinkedQueue<String> getLogQueue() {
    return logQueue;
  }

  public SnapshotArray<FlixelCamera> getCameras() {
    return cameras;
  }

  public SpriteBatch getBatch() {
    return batch;
  }

  public Texture getBgTexture() {
    return bgTexture;
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
