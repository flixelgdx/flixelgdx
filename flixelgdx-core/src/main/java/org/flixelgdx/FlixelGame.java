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
package org.flixelgdx;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import org.flixelgdx.debug.FlixelDebugOverlay;
import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelDrawable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelSpriteBatch;
import org.flixelgdx.group.FlixelBasicGroup;
import org.flixelgdx.input.FlixelInputProcessorManager;
import org.flixelgdx.input.action.FlixelActionSets;
import org.flixelgdx.text.FlixelFontRegistry;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.util.FlixelRuntimeUtil;
import org.flixelgdx.util.FlixelShader;
import org.flixelgdx.util.signal.FlixelSignalData.UpdateSignalData;
import org.flixelgdx.util.timer.FlixelTimer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The game object used for containing the main loop and core elements of the Flixel game.
 *
 * <p>To actually use this properly, you need to create a subclass of this and override
 * the methods you want to change.
 *
 * <p>It is strongly advised that you do <b>NOT</b> use this class to add the main gameplay logic to your game;
 * your code should go into your {@link FlixelState}s or libGDX {@link com.badlogic.gdx.Screen} implementations instead.
 *
 * <p>It is recommended for using this in the following way:
 *
 * <pre>{@code
 * // Create a new subclass of FlixelGame.
 * // Remember that you can override any methods to add extra functionality
 * // to the game's behavior.
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame() {
 *     super("My Game Title", 640, 360, new InitialState());
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
 *     FlixelLwjgl3Launcher.launch(new MyGame());
 *   }
 * }
 * }</pre>
 */
public abstract class FlixelGame implements ApplicationListener, FlixelUpdatable, FlixelDrawable, FlixelDestroyable {

  private static final int FLOATS_PER_CAMERA_BACKDROP = 5;

  /** The title displayed on the game's window. */
  protected String title;

  /** The size of the game's starting window position and its first camera. */
  protected Vector2 viewSize;

  /** The current window size stored in a vector object. */
  protected Vector2 windowSize;

  /**
   * Produces the root {@link FlixelState} each time {@link #create()} runs. Use {@code () -> new MyState()} for a fresh
   * instance per session, or {@code () -> sharedState} to reuse one object
   * (its {@link FlixelState#destroy()} / {@link FlixelState#create()} lifecycle still runs via {@link Flixel#switchState}).
   */
  @NotNull
  protected Supplier<FlixelState> initialStateFactory;

  /** The framerate of how fast the game should update and render. */
  private int framerate;

  /** The main batch used for rendering all sprites on screen. */
  protected FlixelBatch batch;

  /** The background color of the entire game's window (full-framebuffer clear before camera passes). */
  protected Color bgColor = new Color(Color.BLACK);

  /** 1x1 white texture used to draw solid fills (camera bg, FX); tinted via {@link FlixelSpriteBatch#setColor(Color)}. */
  protected Texture bgTexture;

  /** Convenience reference to the global {@link Flixel#cameras} list (the single source of truth). */
  protected final Array<FlixelCamera> cameras = Flixel.cameras;

  /** The camera used to render the global overlay. Not registered in {@link Flixel#cameras}. */
  @Nullable
  private FlixelCamera overlayCamera;

  /**
   * A standard single-texture batch used only for the post-processing composite pass when a
   * {@link FlixelCamera} has a {@link FlixelShader} assigned. Created lazily on first use and
   * disposed in {@link #destroy()}.
   */
  @Nullable
  private SpriteBatch compositeBatch;

  /** The member group for the global overlay. Updated and drawn when the overlay is enabled. */
  @Nullable
  private FlixelBasicGroup<IFlixelBasic> overlayGroup;

  /**
   * Total render calls issued by the framework {@link FlixelBatch} during the most recently
   * completed draw pass, summed across all camera loops. Derived from the delta of
   * {@link FlixelBatch#getTotalRenderCalls()} so multiple begin/end cycles within a single frame
   * do not erase earlier cameras' counts.
   */
  private int frameRenderCalls;

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

  /** Orthographic projection matrix reused each frame for the FBO composite pass. */
  private final Matrix4 fboOrtho = new Matrix4();

  /**
   * Camera dimensions the current {@link #fboOrtho} matrix was last built for.
   * -1 means uninitialized; any change triggers a rebuild and a re-upload to the composite batch.
   */
  private int fboOrthoW = -1;

  private int fboOrthoH = -1;

  /**
   * Ordered list of shaders applied to all game cameras as a group before the global overlay is
   * drawn. Shaders are run in insertion order; two or more shaders chain via ping-pong FBOs so
   * each pass feeds the next without re-rendering the scene.
   *
   * <p>Managed via {@link #addGlobalShader(FlixelShader)} and
   * {@link #removeGlobalShader(FlixelShader)}.
   */
  private final Array<FlixelShader> globalShaders = new Array<>();

  /**
   * Primary scene framebuffer for the global shader pass.
   * Created on the first {@link #addGlobalShader} call and recreated on window resize.
   * Null when {@link #globalShaders} is empty.
   */
  @Nullable
  private FrameBuffer sceneFboA;

  /**
   * Secondary scene framebuffer used only when two or more global shaders are active.
   * Acts as the ping-pong target so each shader reads from one FBO and writes to the other.
   * Null when fewer than two shaders are present.
   */
  @Nullable
  private FrameBuffer sceneFboB;

  @Nullable
  private TextureRegion sceneFboRegionA;

  @Nullable
  private TextureRegion sceneFboRegionB;

  /**
   * {@code r, g, b, a} of {@link #bgColor} captured the first time desktop transparency is enabled
   * this session. Cleared when transparency is turned off.
   */
  private final float[] desktopTransparencyRestoreGameRgba = new float[4];

  /**
   * Packed per-camera backdrop data: {@code r, g, b, a, useBgAlphaBlending ? 1f : 0f} for each camera index.
   * Reused across toggles to avoid allocations.
   */
  private float[] desktopTransparencyRestoreCamerasPacked = new float[20];

  private int desktopTransparencyRestoreCameraCount;

  /** Should the game use VSync to limit the framerate to the monitor's refresh rate? */
  private boolean vsync;

  /** Should the game start in fullscreen mode? */
  protected boolean fullscreen;

  /**
   * When {@code true}, the LWJGL3 launcher requests an alpha-capable framebuffer so
   * {@link org.flixelgdx.backend.window.FlixelWindow#setTransparencyActive(boolean) FlixelWindow.setTransparencyActive(boolean)}
   * can composite with the desktop.
   *
   * <p>Set {@code false} before launch only for drivers or projects that must keep a strictly opaque default framebuffer.
   *
   * <p><b>WARNING</b>: This can cause some minor performance issues on low-end PCs, so only enable this at launch time
   * if you truly need to!
   */
  protected boolean transparentFramebufferRequested = false;

  /** Should the game pause audio when the application goes to the background? */
  public boolean autoPause = true;

  /** Is the game currently closing? */
  private boolean isClosing = false;

  /** Has the game successfully shut down? */
  private boolean isClosed = false;

  /** When true, skips gameplay/state/camera follow updates (debug pause). */
  private boolean gamePaused = false;

  /** When true, the global overlay group is updated and drawn on top of all game cameras each frame. */
  private boolean overlayEnabled;

  /** When true, calls {@link #destroy()} and {@link #create()}, which resets the game. */
  protected volatile boolean resetRequested = false;

  /** Prevents re-entrant fullscreen transitions from resize callbacks on desktop backends. */
  private boolean fullscreenChangeInProgress = false;

  /**
   * When {@code true}, {@link Flixel#getState()} was sent {@link FlixelState#pause()} for a paired app or window pause
   * and {@link FlixelState#resume()} has not yet been dispatched. Used so duplicate callbacks (such as minimize plus
   * focus lost) only run state hooks once.
   */
  private boolean stateLifecyclePauseDispatched;

  /**
   * Last value passed to {@link #applyBackdropForDesktopTransparency(boolean)}; used by
   * {@link org.flixelgdx.backend.window.FlixelWindow#isTransparencyActive() FlixelWindow.isTransparencyActive()}.
   */
  private boolean desktopTransparencyActive;

  private boolean desktopTransparencyRestoreSnapshotValid;

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param initialState The initial state to load when the game starts.
   */
  public FlixelGame(String title, FlixelState initialState) {
    this(title, 640, 360, initialState, 60, true, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialState The initial state to load when the game starts.
   */
  public FlixelGame(String title, int width, int height, FlixelState initialState) {
    this(title, width, height, initialState, 60, true, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialState The initial state to load when the game starts.
   * @param framerate The framerate of how fast the game should update and render.
   */
  public FlixelGame(String title, int width, int height, FlixelState initialState, int framerate) {
    this(title, width, height, initialState, framerate, true, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialState The initial state to load when the game starts.
   * @param framerate The framerate of how fast the game should update and render.
   * @param vsync Should the game use Vsync to limit the framerate to the monitor's refresh rate?
   */
  public FlixelGame(String title, int width, int height, FlixelState initialState, int framerate, boolean vsync) {
    this(title, width, height, initialState, framerate, vsync, false);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialState The initial state to load when the game starts.
   * @param framerate The framerate of how fast the game should update and render.
   * @param vsync Should the game use Vsync to limit the framerate to the monitor's refresh rate?
   * @param fullscreen Should the game start in fullscreen mode?
   */
  public FlixelGame(String title, int width, int height, FlixelState initialState, int framerate, boolean vsync,
      boolean fullscreen) {
    this(title, width, height, () -> initialState, framerate, vsync, fullscreen);
  }

  /**
   * Creates a new game instance with the details specified.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialStateFactory The initial state to load when the game starts as a supplier factory.
   * @param framerate The framerate of how fast the game should update and render.
   * @param vsync Should the game use Vsync to limit the framerate to the monitor's refresh rate?
   * @param fullscreen Should the game start in fullscreen mode?
   */
  public FlixelGame(String title, int width, int height, @NotNull Supplier<FlixelState> initialStateFactory,
      int framerate, boolean vsync, boolean fullscreen) {
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
   * the ANSI system for color output in terminals, and then switches to the initial state.
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

    // Deferred to here (rather than the asset manager's constructor) since the KTX2 loader
    // queries Gdx.gl for supported texture formats, and no GL context exists until create() runs.
    Flixel.assets.enableCompressedTextures();

    isClosed = false;
    isClosing = false;
    stateLifecyclePauseDispatched = false;

    batch = new FlixelSpriteBatch();
    cameras.clear();
    cameras.add(new FlixelCamera((int) viewSize.x, (int) viewSize.y));
    overlayCamera = new FlixelCamera((int) viewSize.x, (int) viewSize.y);
    overlayGroup = new FlixelBasicGroup<>(IFlixelBasic[]::new) {
    };

    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    bgTexture = new Texture(pixmap);
    pixmap.dispose();

    // Keyboard + mouse processors first on the multiplexer (scroll, etc.)
    FlixelInputProcessorManager keysMgr = Flixel.keys;
    FlixelInputProcessorManager mouseMgr = Flixel.mouse;
    if (keysMgr != null || mouseMgr != null) {
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
      if (keysMgr != null) {
        m.addProcessor(idx++, keysMgr.getInputProcessor());
      }
      if (mouseMgr != null) {
        m.addProcessor(idx, mouseMgr.getInputProcessor());
      }
    }

    // Create the debug overlay when debug mode is enabled.
    if (Flixel.isDebugMode()) {
      FlixelDebugOverlay overlay = Flixel.createDebugOverlay();
      if (Flixel.log != null) {
        Flixel.log.addLogListener(overlay.getLogListener());
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
    if (overlayCamera != null && overlayEnabled) {
      overlayCamera.update(width, height, overlayCamera.centerCameraOnResize);
    }

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null) {
      debugOverlay.resize(width, height);
    }

    FlixelState state = Flixel.state;
    if (state != null) {
      state.resize(width, height);
    }

    if (!globalShaders.isEmpty()) {
      initSceneFbos(globalShaders.size > 1);
    }
  }

  /**
   * Updates the logic of the game loop.
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
    if (Flixel.gamepads != null) {
      Flixel.gamepads.update();
    }
    FlixelActionSets.updateAll(elapsed);

    if (!gamePaused) {
      FlixelTween.updateTweens(elapsed);
      FlixelTimer.getGlobalManager().update(elapsed * Flixel.timeScale);

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
      for (FlixelCamera camera : cameras) {
        camera.update(elapsed);
      }

      if (overlayGroup != null && overlayEnabled) {
        overlayGroup.update(elapsed);
        if (overlayCamera != null) {
          overlayCamera.update(elapsed);
        }
      }
    }

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null && Flixel.isDebugMode()) {
      debugOverlay.update(elapsed);
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
  public void draw(@NotNull FlixelBatch batch) {
    Flixel.Signals.preDraw.dispatch();

    ScreenUtils.clear(bgColor); // Clear the screen to refresh it.
    FlixelState state = Flixel.getState();

    int totalRenderCallsBefore = batch.getTotalRenderCalls();

    boolean useGlobalFbo = !globalShaders.isEmpty() && sceneFboA != null;
    if (useGlobalFbo) {
      sceneFboA.begin();
      Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

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

        FlixelShader cameraShader = camera.getShader();
        if (cameraShader != null) {
          camera.getFbo().begin();
          Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
          Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }

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

        // Safety reset: per-sprite shader draws restore themselves inline, but reset here as a
        // backstop so the next camera pass always starts with the default batch shader.
        batch.setShader(null);

        if (cameraShader != null) {
          camera.getFbo().end();
          if (useGlobalFbo) {
            // camera.getFbo().end() restores GL framebuffer to 0 (the screen).
            Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, sceneFboA.getFramebufferHandle());
          }
          camera.getViewport().apply();
          // FlixelSpriteBatch.flush() leaves the active GL texture unit at the last
          // slot it bound (e.g. unit 2 after drawing 3 atlases). SpriteBatch.flush()
          // calls Texture.bind() with no unit argument, so it binds the FBO texture
          // to whatever unit is still active, not unit 0.
          Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
          if (compositeBatch == null) {
            compositeBatch = new SpriteBatch();
          }
          if (camera.width != fboOrthoW || camera.height != fboOrthoH) {
            fboOrthoW = camera.width;
            fboOrthoH = camera.height;
            fboOrtho.setToOrtho2D(0, 0, fboOrthoW, fboOrthoH);
            compositeBatch.setProjectionMatrix(fboOrtho);
          }
          if (compositeBatch.getShader() != cameraShader.getProgram()) {
            compositeBatch.setShader(cameraShader.getProgram());
          }
          compositeBatch.begin();
          cameraShader.applyUniforms();
          compositeBatch.draw(camera.getFboRegion(), 0, 0, camera.width, camera.height);
          compositeBatch.end();
        }
      } finally {
        Flixel.setDrawCamera(null);
      }
    }

    if (useGlobalFbo) {
      sceneFboA.end();
      applyGlobalShaderChain();
    }

    if (overlayCamera != null && overlayGroup != null && overlayEnabled) {
      Flixel.setDrawCamera(overlayCamera);
      try {
        if (gamePaused) {
          overlayCamera.applyLibCameraTransform();
        }
        overlayCamera.getViewport().apply();
        batch.setProjectionMatrix(overlayCamera.getCamera().combined);
        batch.begin();
        overlayGroup.draw(batch);
        batch.end();
      } finally {
        Flixel.setDrawCamera(null);
      }
    }

    frameRenderCalls = batch.getTotalRenderCalls() - totalRenderCallsBefore;

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null) {
      debugOverlay.drawBoundingBoxes(cameras.items);
      debugOverlay.draw();
    }

    squashFramebufferAlpha();

    Flixel.Signals.postDraw.dispatch();
  }

  /**
   * When the window was created with a transparent-capable framebuffer but desktop see-through is off, the compositor
   * still blends using framebuffer alpha. Sprite draws that write alpha < 1 would incorrectly show the real desktop.
   * This clears only the alpha channel to {@code 1} over the full framebuffer after all rendering.
   */
  private void squashFramebufferAlpha() {
    if (desktopTransparencyActive || !transparentFramebufferRequested) {
      return;
    }
    if (Gdx.app.getType() != Application.ApplicationType.Desktop) {
      return;
    }
    GL20 gl = Gdx.gl;
    boolean scissorWasOn = gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
    if (scissorWasOn) {
      gl.glDisable(GL20.GL_SCISSOR_TEST);
    }
    gl.glColorMask(false, false, false, true);
    gl.glClearColor(0f, 0f, 0f, 1f);
    gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    gl.glColorMask(true, true, true, true);
    if (scissorWasOn) {
      gl.glEnable(GL20.GL_SCISSOR_TEST);
    }
  }

  /**
   * Updates the game's global and internal {@link #update(float)} and {@link FlixelDrawable#draw(FlixelBatch)} methods, with elapsed time clamped
   * to the min and max values to prevent major lag spikes.
   *
   * <p>This method is called automatically by libGDX's {@link ApplicationListener#render()} method when the game is
   * running, so it is not necessary to override this method in most cases. However, it can be overridden to
   * perform custom updating/rendering before the game is updated/rendered.
   *
   * <p>You should not (and cannot) override this method. You are encouraged to override either {@link #update(float)}
   * or {@link FlixelDrawable#draw(FlixelBatch)} instead, as they separate logic and rendering correctly.
   *
   * @see #update(float)
   * @see FlixelDrawable#draw(FlixelBatch)
   * @see ApplicationListener#render()
   */
  @Override
  public final void render() {
    float rawDelta = Gdx.graphics != null ? Gdx.graphics.getDeltaTime() : Flixel.MIN_ELAPSED;
    float elapsed = Math.max(Flixel.MIN_ELAPSED, Math.min(rawDelta, Flixel.MAX_ELAPSED));
    Flixel.elapsed = elapsed;

    windowSize.x = Gdx.graphics.getWidth();
    windowSize.y = Gdx.graphics.getHeight();
    fullscreen = Gdx.graphics.isFullscreen();

    update(elapsed);
    draw(batch);

    // Finalize input frame AFTER user update hooks run, so justPressed()/justReleased() checks
    // in subclasses (typically placed after super.update(elapsed)) stay valid this frame.
    if (Flixel.keys != null) {
      Flixel.keys.endFrame();
    }
    if (Flixel.mouse != null) {
      Flixel.mouse.endFrame();
    }
    if (Flixel.gamepads != null) {
      Flixel.gamepads.endFrame();
    }
    FlixelActionSets.endFrameAll();
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
      Flixel.sound.resume();
    }
    this.gamePaused = gamePaused;
  }

  private void snapshotCamerasForDebugPause() {
    if (cameras.size == 0) {
      debugPauseCameraScroll = null;
      debugPauseCameraZoom = null;
      return;
    }
    int n = cameras.size;
    debugPauseCameraScroll = new float[n][2];
    debugPauseCameraZoom = new float[n];
    for (int i = 0; i < n; i++) {
      FlixelCamera c = cameras.get(i);
      debugPauseCameraScroll[i][0] = c.scrollX;
      debugPauseCameraScroll[i][1] = c.scrollY;
      debugPauseCameraZoom[i] = c.getZoom();
    }
  }

  private void restoreCamerasAfterDebugPause() {
    if (debugPauseCameraScroll == null || debugPauseCameraZoom == null) {
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

  /**
   * Do not override this method. Override {@link #onFocusLost()} instead.
   */
  @Override
  public final void pause() {
    onFocusLost();
  }

  /**
   * Do not override this method. Override {@link #onFocusGained()} instead.
   */
  @Override
  public final void resume() {
    onFocusGained();
  }

  /**
   * Called when the game window loses focus or the application goes to the background.
   *
   * <p>On mobile and web this fires when the OS sends the application to the background.
   * On desktop it fires when the game window loses focus or is minimized (focus loss always
   * arrives before minimize, so this is called once for both events).
   *
   * <p>The default implementation pauses audio and stops continuous rendering when
   * {@link #autoPause} is {@code true}, then notifies the active state. Duplicate calls
   * without an intervening {@link #onFocusGained()} are silently ignored.
   *
   * <p>Override this to add custom behavior when focus is lost:
   *
   * <pre>{@code
   * @Override
   * protected void onFocusLost() {
   *   super.onFocusLost();
   *   myVideo.pause();
   * }
   * }</pre>
   *
   * @see #onFocusGained()
   * @see #onMinimized()
   * @see Flixel.Signals#windowUnfocused
   */
  public void onFocusLost() {
    if (stateLifecyclePauseDispatched) {
      return;
    }
    stateLifecyclePauseDispatched = true;
    FlixelState state = Flixel.state;
    if (state != null) {
      state.onFocusLost();
    }
    if (autoPause) {
      Flixel.sound.pause();
      Gdx.graphics.setContinuousRendering(false);
    }
    Flixel.Signals.windowUnfocused.dispatch();
  }

  /**
   * Called when the game window regains focus or the application returns to the foreground.
   *
   * <p>On mobile and web this fires when the OS brings the application back to the foreground.
   * On desktop it fires when the game window gains focus, including when the window is
   * restored from being minimized.
   *
   * <p>The default implementation resumes audio and re-enables continuous rendering when
   * {@link #autoPause} is {@code true}, then notifies the active state. Calls that arrive
   * without a prior {@link #onFocusLost()} are silently ignored.
   *
   * <p>Override this to add custom behavior when focus returns:
   *
   * <pre>{@code
   * @Override
   * protected void onFocusGained() {
   *   super.onFocusGained();
   *   myVideo.resume();
   * }
   * }</pre>
   *
   * @see #onFocusLost()
   * @see Flixel.Signals#windowFocused
   */
  public void onFocusGained() {
    if (!stateLifecyclePauseDispatched) {
      return;
    }
    stateLifecyclePauseDispatched = false;
    FlixelState state = Flixel.state;
    if (state != null) {
      state.onFocusGained();
    }
    if (autoPause && !gamePaused) {
      Flixel.sound.resume();
      Gdx.graphics.setContinuousRendering(true);
      Gdx.graphics.requestRendering();
    }
    Flixel.Signals.windowFocused.dispatch();
  }

  /**
   * Called when the desktop window is minimized (iconified).
   *
   * <p>This is a desktop-only event and is never called on mobile or web platforms.
   * On most operating systems, focus loss fires first so {@link #onFocusLost()} already
   * handles audio and rendering pausing before this is called.
   *
   * <p>The default implementation notifies the active state and dispatches
   * {@link Flixel.Signals#windowMinimized}. Override to add custom behavior:
   *
   * <pre>{@code
   * @Override
   * protected void onMinimized() {
   *   super.onMinimized();
   *   saveQuickAutoSave();
   * }
   * }</pre>
   *
   * @see #onFocusLost()
   * @see #onFocusGained()
   * @see Flixel.Signals#windowMinimized
   */
  public void onMinimized() {
    FlixelState state = Flixel.state;
    if (state != null) {
      state.onMinimized();
    }
    Flixel.Signals.windowMinimized.dispatch();
  }

  /**
   * Sets fullscreen mode for the game's window.
   *
   * @param enabled If the game's window should be in fullscreen mode.
   */
  public void setFullscreen(boolean enabled) {
    boolean currentFullscreen = Gdx.graphics.isFullscreen();
    if (enabled == currentFullscreen || fullscreenChangeInProgress) {
      fullscreen = currentFullscreen;
      return;
    }
    fullscreenChangeInProgress = true;
    try {
      if (enabled) {
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
      } else {
        Gdx.graphics.setWindowedMode((int) viewSize.x, (int) viewSize.y);
      }
    } finally {
      fullscreenChangeInProgress = false;
      fullscreen = Gdx.graphics.isFullscreen();
    }
  }

  /** Toggles fullscreen mode on or off, depending on the current state. */
  public void toggleFullscreen() {
    setFullscreen(!Gdx.graphics.isFullscreen());
  }

  /**
   * Toggles auto-pause on or off.
   *
   * @return The new value of auto-pause after toggling.
   */
  public boolean toggleAutoPause() {
    autoPause = !autoPause;
    return autoPause;
  }

  /** @see #destroy() */
  @Override
  public final void dispose() {
    destroy();
  }

  /**
   * Adds a shader to the global post-processing chain applied to all game cameras together.
   *
   * <p>Unlike per-camera shaders (see {@link FlixelCamera#setShader(FlixelShader)}), a global
   * shader captures the combined output of every game camera into a single full-screen
   * framebuffer and applies the effect in one pass. This means barrel distortion, scanlines,
   * and similar effects align correctly across camera boundaries. The global overlay (debug
   * FPS display, etc.) is drawn after the global composite and is always excluded.
   *
   * <p>Shaders added with this method run in insertion order. When more than one shader is
   * present they chain via ping-pong framebuffers so each pass feeds the next without
   * re-rendering the scene.
   *
   * <p><b>Performance note:</b> Every global shader adds a full-screen framebuffer pass per
   * frame. On weaker or integrated-graphics hardware this can have a meaningful impact on
   * frame budget. It is strongly recommended to expose a graphics settings option in your
   * game so players can disable shader effects. A common pattern is to call
   * {@link #removeGlobalShader(FlixelShader)} and {@link FlixelCamera#setShader(FlixelShader)
   * camera.setShader(null)} when the player turns shaders off, and re-add them when turned
   * back on.
   *
   * <p>Adding the same shader instance more than once is a no-op.
   *
   * @param shader The shader to append to the global chain.
   */
  public void addGlobalShader(FlixelShader shader) {
    if (globalShaders.contains(shader, true)) {
      return;
    }
    boolean needsPingPong = !globalShaders.isEmpty();
    globalShaders.add(shader);
    initSceneFbos(needsPingPong || globalShaders.size > 1);
  }

  /**
   * Removes a shader from the global post-processing chain.
   *
   * <p>If the chain becomes empty as a result, the scene framebuffers are released immediately.
   * Removing a shader that was never added is a no-op.
   *
   * @param shader The shader to remove.
   * @return {@code true} if the shader was found and removed, {@code false} otherwise.
   */
  public boolean removeGlobalShader(FlixelShader shader) {
    boolean removed = globalShaders.removeValue(shader, true);
    if (removed) {
      if (globalShaders.isEmpty()) {
        disposeSceneFbos();
      } else {
        initSceneFbos(globalShaders.size > 1);
      }
    }
    return removed;
  }

  /** Creates (or recreates) the scene framebuffers used by the global shader chain. */
  private void initSceneFbos(boolean needPingPong) {
    disposeSceneFbos();
    int w = Gdx.graphics.getBackBufferWidth();
    int h = Gdx.graphics.getBackBufferHeight();
    sceneFboA = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
    sceneFboRegionA = new TextureRegion(sceneFboA.getColorBufferTexture());
    sceneFboRegionA.flip(false, true);
    if (needPingPong) {
      sceneFboB = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
      sceneFboRegionB = new TextureRegion(sceneFboB.getColorBufferTexture());
      sceneFboRegionB.flip(false, true);
    }
  }

  /** Releases the scene framebuffers and clears the region references. */
  private void disposeSceneFbos() {
    if (sceneFboA != null) {
      sceneFboA.dispose();
      sceneFboA = null;
      sceneFboRegionA = null;
    }
    if (sceneFboB != null) {
      sceneFboB.dispose();
      sceneFboB = null;
      sceneFboRegionB = null;
    }
  }

  /**
   * Composites the scene framebuffer to the screen by running it through the global shader chain.
   * When more than one shader is present the passes ping-pong between {@link #sceneFboA} and
   * {@link #sceneFboB} so each shader reads from one texture and writes to the other.
   */
  private void applyGlobalShaderChain() {
    if (compositeBatch == null) {
      compositeBatch = new SpriteBatch();
    }
    int w = Gdx.graphics.getBackBufferWidth();
    int h = Gdx.graphics.getBackBufferHeight();
    boolean usingA = true;
    TextureRegion src = sceneFboRegionA;
    int n = globalShaders.size;

    for (int i = 0; i < n; i++) {
      FlixelShader gs = globalShaders.get(i);
      boolean isLast = (i == n - 1);

      Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

      if (w != fboOrthoW || h != fboOrthoH) {
        fboOrthoW = w;
        fboOrthoH = h;
        fboOrtho.setToOrtho2D(0, 0, w, h);
        compositeBatch.setProjectionMatrix(fboOrtho);
      }
      if (compositeBatch.getShader() != gs.getProgram()) {
        compositeBatch.setShader(gs.getProgram());
      }

      if (!isLast) {
        FrameBuffer dst = usingA ? sceneFboB : sceneFboA;
        TextureRegion dstRegion = usingA ? sceneFboRegionB : sceneFboRegionA;
        dst.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        compositeBatch.begin();
        gs.applyUniforms();
        compositeBatch.draw(src, 0, 0, w, h);
        compositeBatch.end();
        dst.end();
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        src = dstRegion;
        usingA = !usingA;
      } else {
        compositeBatch.begin();
        gs.applyUniforms();
        compositeBatch.draw(src, 0, 0, w, h);
        compositeBatch.end();
      }
    }
  }

  /**
   * Destroys the game and all of its resources.
   *
   * <p>Note that this doesn't close the game entirely; it just disposes
   * of the game's resources. If you want to close the entire game, use libGDX's {@link Application#exit()}.
   */
  @Override
  public void destroy() {
    if (isClosing) {
      return;
    }
    isClosing = true;

    Flixel.setDrawCamera(null);

    Flixel.Signals.preGameClose.dispatch();

    FlixelDebugOverlay debugOverlay = Flixel.getDebugOverlay();
    if (debugOverlay != null) {
      if (Flixel.log != null) {
        Flixel.log.removeLogListener(debugOverlay.getLogListener());
      }
      debugOverlay.destroy();
      Flixel.clearDebugOverlay();
    }

    InputProcessor processor = Gdx.input.getInputProcessor();
    if (processor instanceof InputMultiplexer multiplexer) {
      multiplexer.clear();
    }

    if (Flixel.gamepads != null) {
      Flixel.gamepads.detach();
    }

    FlixelTween.cancelActiveTweens();
    FlixelTween.clearTweenPools();
    FlixelTween.resetRegistry();
    FlixelTimer.cancelAll();

    if (Flixel.getState() != null) {
      Flixel.getState().destroy();
    }
    if (batch != null) {
      batch.dispose();
      batch = null;
    }
    disposeSceneFbos();
    globalShaders.clear();

    if (compositeBatch != null) {
      compositeBatch.dispose();
      compositeBatch = null;
      fboOrthoW = -1;
      fboOrthoH = -1;
    }
    if (bgTexture != null) {
      bgTexture.dispose();
      bgTexture = null;
    }

    if (Flixel.assets != null) {
      Flixel.assets.dispose();
      Flixel.assets = null;
    }
    if (Flixel.sound != null) {
      if (Flixel.initialized) {
        Flixel.sound.destroy();
      } else {
        Flixel.sound.resetSession();
      }
    }

    for (FlixelCamera camera : cameras) {
      camera.destroy();
    }
    cameras.clear();
    if (overlayGroup != null) {
      overlayGroup.destroy();
      overlayGroup = null;
    }
    if (overlayCamera != null) {
      overlayCamera.destroy();
      overlayCamera = null;
    }
    overlayEnabled = false;
    debugPauseCameraScroll = null;
    debugPauseCameraZoom = null;
    gamePaused = false;
    stateLifecyclePauseDispatched = false;

    FlixelFontRegistry.dispose();

    Flixel.Signals.postGameClose.dispatch();

    // Stop file logging after the whole game closes so that way any logs made can be stored!
    Flixel.log.stopFileLogging();

    isClosed = true;
  }

  /**
   * Configures the framework's crash handler to safely catch uncaught exceptions and gracefully close the game.
   */
  protected void configureCrashHandler() {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      String logs = FlixelRuntimeUtil.getFullExceptionMessage(throwable);
      String msg = "There was an uncaught exception on thread \"" + thread.getName() + "\"!\n" + logs;
      Flixel.error(msg);
      Flixel.showErrorAlert("Uncaught Exception", msg);
      destroy();
      // Only use Gdx.app.exit() on non-iOS platforms to avoid App Store guideline violations!
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
        Gdx.app.exit();
      }
    });
  }

  /**
   * Resets the camera list to contain a single default camera with the current window size as its viewport.
   */
  public void resetCameras() {
    FlixelCamera camera = new FlixelCamera((int) viewSize.x, (int) viewSize.y);
    camera.update((int) windowSize.x, (int) windowSize.y, camera.centerCameraOnResize);
    cameras.clear();
    cameras.add(camera);
    if (desktopTransparencyActive) {
      applyDesktopTransparencyBackdropOnly();
    }
  }

  /**
   * Adds a member to the global overlay group so it is updated and drawn on top of all game cameras while the overlay
   * is enabled.
   *
   * <p>The overlay must be enabled via {@link #enableGlobalOverlay(boolean)} or {@link #toggleGlobalOverlay()} for
   * added members to actually appear. This is safe to call even when the overlay is disabled.
   *
   * <p>Example usage:
   * <pre>{@code
   * fpsCounter = new FlixelText();
   * add(fpsCounter);
   * enableGlobalOverlay(true);
   * }</pre>
   *
   * @param basic The object to add to the overlay group.
   */
  public void add(@NotNull IFlixelBasic basic) {
    if (overlayGroup != null) {
      overlayGroup.add(basic);
      if (basic instanceof FlixelAntialiasable b && Flixel.applyAntialiasingOnStateAdd) {
        b.setAntialiasing(Flixel.isAntialiasing());
      }
    }
  }

  /**
   * Removes a member from the global overlay group.
   *
   * @param basic The object to remove from the overlay group.
   */
  public void remove(@NotNull IFlixelBasic basic) {
    if (overlayGroup != null) {
      overlayGroup.remove(basic);
    }
  }

  /**
   * Enables or disables the global overlay. When disabled, the overlay group is neither updated nor drawn, making it
   * zero-cost on the frame budget.
   *
   * @param enabled Whether the overlay should be active.
   */
  public void enableGlobalOverlay(boolean enabled) {
    overlayEnabled = enabled;
  }

  /**
   * Toggles the global overlay on if it is currently off, and off if it is currently on.
   *
   * @return The new enabled state after toggling.
   */
  public boolean toggleGlobalOverlay() {
    overlayEnabled = !overlayEnabled;
    return overlayEnabled;
  }

  public String getTitle() {
    return title;
  }

  public Vector2 getSize() {
    return viewSize;
  }

  public int getWidth() {
    return (int) viewSize.x;
  }

  public int getHeight() {
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

  public Array<FlixelCamera> getCameras() {
    return cameras;
  }

  @NotNull
  public FlixelBatch getBatch() {
    return batch;
  }

  @Nullable
  public SpriteBatch getCompositeBatch() {
    return compositeBatch;
  }

  /**
   * Returns the total number of {@link FlixelBatch} render calls issued during the most recently
   * completed frame, summed across all camera passes. This value is not reset by intermediate
   * begin/end cycles, so it correctly reflects the full per-frame cost when multiple cameras
   * are active.
   *
   * @return Per-frame render call count from the last completed draw pass.
   */
  public int getFrameRenderCalls() {
    return frameRenderCalls;
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

  public boolean isTransparentFramebufferRequested() {
    return transparentFramebufferRequested;
  }

  /**
   * Requests an alpha-capable GLFW framebuffer on LWJGL3 before the desktop launcher runs. Default {@code true}. Set {@code false}
   * if you must avoid framebuffer alpha (some drivers) or never want desktop compositing. When {@code false}, toggling
   * {@link org.flixelgdx.backend.window.FlixelWindow#setTransparencyActive(boolean) FlixelWindow.setTransparencyActive(boolean)} only affects drawing, not true desktop bleed-through.
   *
   * @param transparentFramebufferRequested {@code false} to force an opaque default framebuffer at launch.
   */
  public void setTransparentFramebufferRequested(boolean transparentFramebufferRequested) {
    this.transparentFramebufferRequested = transparentFramebufferRequested;
  }

  /**
   * @return {@code true} after {@link #applyBackdropForDesktopTransparency(boolean)} was called with {@code true}.
   */
  public boolean isTransparencyActive() {
    return desktopTransparencyActive;
  }

  /**
   * Updates global and per-camera backdrop drawing for desktop compositing. Called from
   * {@link org.flixelgdx.backend.window.FlixelWindow FlixelWindow}. When desktop see-through is off but the GLFW window
   * was created with a transparent-capable framebuffer, {@link FlixelDrawable#draw} also forces framebuffer alpha to {@code 1} after
   * rendering so tinted sprites do not composite through the real desktop.
   *
   * @param active {@code true} for transparent clears and camera fills; {@code false} restores colors
   * captured the first time transparency was enabled this session (then clears that cache), or opaque black
   * if transparency was never enabled.
   */
  public void applyBackdropForDesktopTransparency(boolean active) {
    desktopTransparencyActive = active;
    if (active) {
      captureDesktopTransparency();
      applyDesktopTransparencyBackdropOnly();
      return;
    }
    restoreDesktopTransparencyBackdrop();
    clearDesktopTransparencyRestoreSnapshot();
  }

  /**
   * Applies transparent full-window clear and per-camera backdrop without touching the restore snapshot.
   * Used after {@link #resetCameras()} while transparency stays enabled.
   */
  private void applyDesktopTransparencyBackdropOnly() {
    bgColor.a = 0f;
    FlixelCamera[] camItems = cameras.items;
    for (int i = 0, n = cameras.size; i < n; i++) {
      FlixelCamera cam = camItems[i];
      if (cam == null) {
        continue;
      }
      cam.useBgAlphaBlending = true;
      cam.bgColor.a = 0f;
    }
  }

  private void captureDesktopTransparency() {
    if (desktopTransparencyRestoreSnapshotValid) {
      return;
    }
    float[] g = desktopTransparencyRestoreGameRgba;
    g[0] = bgColor.r;
    g[1] = bgColor.g;
    g[2] = bgColor.b;
    g[3] = bgColor.a;
    int n = cameras.size;
    ensureDesktopTransparencyCameraSnapshotCapacity(n);
    FlixelCamera[] camItems = n == 0 ? null : cameras.items;
    float[] p = desktopTransparencyRestoreCamerasPacked;
    for (int i = 0; i < n; i++) {
      FlixelCamera cam = camItems[i];
      int o = i * FLOATS_PER_CAMERA_BACKDROP;
      if (cam == null) {
        p[o] = 0f;
        p[o + 1] = 0f;
        p[o + 2] = 0f;
        p[o + 3] = 1f;
        p[o + 4] = 0f;
        continue;
      }
      p[o] = cam.bgColor.r;
      p[o + 1] = cam.bgColor.g;
      p[o + 2] = cam.bgColor.b;
      p[o + 3] = cam.bgColor.a;
      p[o + 4] = cam.useBgAlphaBlending ? 1f : 0f;
    }
    desktopTransparencyRestoreCameraCount = n;
    desktopTransparencyRestoreSnapshotValid = true;
  }

  private void ensureDesktopTransparencyCameraSnapshotCapacity(int cameraCount) {
    int need = cameraCount * FLOATS_PER_CAMERA_BACKDROP;
    if (desktopTransparencyRestoreCamerasPacked.length >= need) {
      return;
    }
    desktopTransparencyRestoreCamerasPacked =
        new float[Math.max(need, desktopTransparencyRestoreCamerasPacked.length * 2)];
  }

  private void restoreDesktopTransparencyBackdrop() {
    float[] g = desktopTransparencyRestoreGameRgba;
    if (desktopTransparencyRestoreSnapshotValid) {
      bgColor.r = g[0];
      bgColor.g = g[1];
      bgColor.b = g[2];
      bgColor.a = g[3];
    } else {
      bgColor.set(Color.BLACK);
    }
    FlixelCamera[] camItems = cameras.items;
    int n = cameras.size;
    int saved = desktopTransparencyRestoreCameraCount;
    float[] p = desktopTransparencyRestoreCamerasPacked;
    for (int i = 0; i < n; i++) {
      FlixelCamera cam = camItems[i];
      if (cam == null) {
        continue;
      }
      if (desktopTransparencyRestoreSnapshotValid && i < saved) {
        int o = i * FLOATS_PER_CAMERA_BACKDROP;
        cam.bgColor.r = p[o];
        cam.bgColor.g = p[o + 1];
        cam.bgColor.b = p[o + 2];
        cam.bgColor.a = p[o + 3];
        cam.useBgAlphaBlending = p[o + 4] != 0f;
      } else {
        cam.useBgAlphaBlending = false;
        cam.bgColor.set(Color.BLACK);
      }
    }
  }

  private void clearDesktopTransparencyRestoreSnapshot() {
    desktopTransparencyRestoreSnapshotValid = false;
    desktopTransparencyRestoreCameraCount = 0;
    Arrays.fill(desktopTransparencyRestoreGameRgba, 0f);
    Arrays.fill(desktopTransparencyRestoreCamerasPacked, 0f);
  }

  public boolean isGamePaused() {
    return gamePaused;
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

  public boolean isGlobalOverlayEnabled() {
    return overlayEnabled;
  }

  /**
   * Returns the private {@link FlixelCamera} used to render the global overlay.
   *
   * <p>This camera is never registered in {@link Flixel#cameras}, so it is not affected by state
   * code or camera resets. Its scroll is always zero, which means overlay members placed at
   * position {@code (x, y)} always appear at those same design-resolution coordinates regardless
   * of what the active game camera is doing.
   *
   * @return The overlay camera, or {@code null} if {@link #create()} has not yet run.
   */
  @Nullable
  public FlixelCamera getOverlayCamera() {
    return overlayCamera;
  }

  /**
   * Returns the {@link FlixelBasicGroup} that holds all members added via {@link #add(IFlixelBasic)}.
   *
   * @return The overlay group, or {@code null} if {@link #create()} has not yet run.
   */
  @Nullable
  public FlixelBasicGroup<IFlixelBasic> getOverlayGroup() {
    return overlayGroup;
  }
}
