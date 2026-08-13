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

import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugOverlay;
import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelDrawable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.group.FlixelBasicGroup;
import org.flixelgdx.input.action.FlixelActionSets;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.text.FlixelFontRegistry;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelRuntimeUtil;
import org.flixelgdx.util.FlixelShader;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.flixelgdx.util.save.FlixelSave;
import org.flixelgdx.util.signal.FlixelSignalData.UpdateSignalData;
import org.flixelgdx.util.timer.FlixelTimer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The main game class that drives the update-render loop and owns the core framework components.
 *
 * <p>Extend this class and hand an instance to the platform launcher. The launcher installs the
 * appropriate backend, wires up the runner, and calls {@link #create()} once the rendering surface
 * is ready. After that, it calls {@link #render(float)} every frame, which dispatches
 * {@link #update(float)} and then {@link #draw(FlixelBatch)} in that order.
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #create()} - called once when the rendering surface is ready. Initializes input,
 *       cameras, the batch, the debug overlay, and the initial state.</li>
 *   <li>{@link #render(float)} - called every frame with the raw wall-clock delta. Dispatches
 *       update, then draw. Do not override this; override {@link #update(float)} or
 *       {@link #draw(FlixelBatch)} instead.</li>
 *   <li>{@link #destroy()} - called when the game closes. Releases all framework resources. Call
 *       {@link Flixel#exit()} to close the window; calling {@code destroy()} alone only releases
 *       resources without terminating the process.</li>
 * </ol>
 *
 * <h2>States vs. the game class</h2>
 *
 * <p>The game class is the permanent container. The active {@link FlixelState} is the changeable
 * scene. Put all gameplay logic, UI, and entity management in your states, and use
 * {@link Flixel#switchState(FlixelState)} to move between them. Override methods here only for
 * behavior that applies for the entire lifetime of the process: a persistent HUD, a global
 * post-processing shader, custom focus handling, and so on.
 *
 * <h2>Cameras</h2>
 *
 * <p>The active camera list lives in {@link Flixel#cameras}. On startup, {@link #create()} adds
 * one camera sized to match the initial window dimensions from {@link Config}. Every
 * camera in the list is drawn in order each frame. Use {@link #resetCameras()} to restore the
 * single-camera default, or manipulate {@link Flixel#cameras} directly for split-screen or
 * minimap setups.
 *
 * <h2>Global overlay</h2>
 *
 * <p>Any object added via {@link #add(IFlixelBasic)} is placed in a private group that renders on
 * top of all game cameras each frame. The overlay uses its own camera whose scroll is always zero,
 * so members placed at {@code (x, y)} always appear at those design-resolution coordinates
 * regardless of what the active game camera is doing. Enable and disable the overlay with
 * {@link #enableGlobalOverlay(boolean)}. The overlay is completely separate from
 * {@link org.flixelgdx.debug.FlixelDebugOverlay} and does not appear in debug mode unless you
 * explicitly enable it.
 *
 * <h2>Signals</h2>
 *
 * <p>The framework emits signals each frame through {@link Flixel.Signals}:
 * <ul>
 *   <li>{@link Flixel.Signals#preUpdate} / {@link Flixel.Signals#postUpdate} - fired before and
 *       after the entire update pass.</li>
 *   <li>{@link Flixel.Signals#preDraw} / {@link Flixel.Signals#postDraw} - fired before and
 *       after the entire draw pass.</li>
 *   <li>{@link Flixel.Signals#windowFocused} / {@link Flixel.Signals#windowUnfocused} - fired
 *       when the window gains or loses focus.</li>
 *   <li>{@link Flixel.Signals#preGameClose} / {@link Flixel.Signals#postGameClose} - fired at
 *       the start and end of {@link #destroy()}.</li>
 * </ul>
 *
 * <h2>Global shaders</h2>
 *
 * <p>Use {@link #addGlobalShader(FlixelShader)} to apply a post-processing shader to the combined
 * output of every game camera in a single full-screen pass. Multiple shaders chain automatically
 * via ping-pong render targets so each pass feeds the next without re-rendering the scene. The
 * global overlay is always drawn after the shader chain and is never affected by it.
 *
 * <h2>Auto-pause</h2>
 *
 * <p>When {@link #autoPause} is {@code true} (the default), audio is paused and the update loop
 * suspends whenever the game window loses focus. Both resume automatically when focus returns. Set
 * {@link #autoPause} to {@code false} to keep the game running in the background. Note that on
 * mobile the audio will keep playing if {@link #autoPause} is {@code false}.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame() {
 *     super(
 *       new Config.Builder("My Game")
 *           .company("My Studio")
 *           .size(1280, 720)
 *           .build(),
 *       () -> new MenuState()
 *     );
 *   }
 * }
 *
 * // In your platform's launcher (i.e. desktop):
 * FlixelDesktopLauncher.launch(new MyGame());
 * }</pre>
 *
 * @see Config
 * @see FlixelState
 * @see Flixel
 */
public abstract class FlixelGame implements FlixelUpdatable, FlixelDrawable, FlixelDestroyable {

  private static final int FLOATS_PER_CAMERA_BACKDROP = 5;

  /**
   * Produces the root {@link FlixelState} each time {@link #create()} runs. Use
   * {@code () -> new MyState()} for a fresh instance per session, or {@code () -> sharedState} to
   * reuse one object (its {@link FlixelState#destroy()} and {@link FlixelState#create()} lifecycle
   * still runs via {@link Flixel#switchState}).
   */
  @NotNull
  protected Supplier<FlixelState> initialStateFactory;

  @NotNull
  private final Config config;

  /** The main batch used for rendering all sprites on screen. */
  protected FlixelBatch batch;

  /** The background color of the entire game's window (full-framebuffer clear before camera passes). */
  protected FlixelColor bgColor = new FlixelColor(FlixelColor.BLACK);

  /** Shared 1x1 white pixel frame used to draw solid fills (camera bg, FX). */
  protected FlixelFrame bgPixel;

  /** Convenience reference to the global {@link Flixel#cameras} list (the single source of truth). */
  protected final FlixelArray<FlixelCamera> cameras = Flixel.cameras;

  /** The camera used to render the global overlay. Not registered in {@link Flixel#cameras}. */
  @Nullable
  private FlixelCamera overlayCamera;

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

  /** FlixelArray of saved camera zoom values when the game is paused for debugging. */
  @Nullable
  private float[] debugPauseCameraZoom;

  /** Reusable signal data for preUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData preUpdateData = new UpdateSignalData();

  /** Reusable signal data for postUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData postUpdateData = new UpdateSignalData();

  /** Orthographic projection matrix reused each frame for the render-target composite pass. */
  private final FlixelMatrix fboOrtho = new FlixelMatrix();

  /**
   * Camera dimensions the current {@link #fboOrtho} matrix was last built for.
   * -1 means uninitialized; any change triggers a rebuild and a re-upload to the batch.
   */
  private int fboOrthoW = -1;

  private int fboOrthoH = -1;

  /**
   * Ordered list of shaders applied to all game cameras as a group before the global overlay is
   * drawn. Shaders are run in insertion order; two or more shaders chain via ping-pong render
   * targets so each pass feeds the next without re-rendering the scene.
   *
   * <p>Managed via {@link #addGlobalShader(FlixelShader)} and
   * {@link #removeGlobalShader(FlixelShader)}.
   */
  private final FlixelArray<FlixelShader> globalShaders = new FlixelArray<>();

  /**
   * Primary scene render target for the global shader pass.
   * Created on the first {@link #addGlobalShader} call and recreated on window resize.
   * Null when {@link #globalShaders} is empty.
   */
  @Nullable
  private FlixelRenderTarget sceneFboA;

  /**
   * Secondary scene render target used only when two or more global shaders are active.
   * Acts as the ping-pong target so each shader reads from one target and writes to the other.
   * Null when fewer than two shaders are present.
   */
  @Nullable
  private FlixelRenderTarget sceneFboB;

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

  /**
   * When {@code true}, the launcher requests an alpha-capable framebuffer so
   * {@link FlixelWindow#setTransparencyActive(boolean)} can composite with the desktop.
   *
   * <p>Set {@code false} before launch only for drivers or projects that must keep a strictly
   * opaque default framebuffer.
   *
   * <p><b>WARNING</b>: This can cause some minor performance issues on low-end devices, so only
   * enable this at launch time if you truly need to!
   */
  public boolean transparentFramebufferRequested = false;

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

  /** Prevents re-entrant fullscreen transitions from resize callbacks on desktop backends. */
  private boolean fullscreenChangeInProgress = false;

  /**
   * When {@code true}, {@link Flixel#state} was sent {@link FlixelState#onFocusLost()} for a paired
   * app or window pause and {@link FlixelState#onFocusGained()} has not yet been dispatched. Used so
   * duplicate callbacks (such as minimize plus focus lost) only run state hooks once.
   */
  private boolean stateLifecyclePauseDispatched;

  /**
   * When true, the update loop will cycle every frame. This is primarily used by
   * {@link #onFocusGained()} and {@link #onFocusLost()} for auto-pausing.
   */
  private boolean shouldUpdate = true;

  /**
   * Last value passed to {@link #applyBackdropForDesktopTransparency(boolean)}; used by
   * {@link FlixelWindow#isTransparencyActive() FlixelWindow.isTransparencyActive()}.
   */
  private boolean desktopTransparencyActive;

  private boolean desktopTransparencyRestoreSnapshotValid;

  /**
   * Creates a new game instance with a default 640x360 window, 60 fps, and VSync enabled.
   *
   * @param title The title of the game's window.
   * @param initialState The initial state to load when the game starts.
   */
  public FlixelGame(String title, FlixelState initialState) {
    this(new Config.Builder(title).build(), initialState);
  }

  /**
   * Creates a new game instance configured entirely by the supplied {@link Config}.
   *
   * <p>Use this when you need to set company name, version, a custom framerate, or any other
   * option beyond the simple title-and-state shorthand:
   *
   * <pre>{@code
   * super(
   *   new Config.Builder("My Game")
   *     .company("My Studio")
   *     .version("1.0.0")
   *     .size(1280, 720)
   *     .build(),
   *   () -> new MenuState()
   * );
   * }</pre>
   *
   * @param config The configuration that supplies all startup settings.
   * @param initialState The initial state to load when the game starts.
   */
  public FlixelGame(@NotNull Config config, FlixelState initialState) {
    this(config, () -> initialState);
  }

  /**
   * Creates a new game instance with a 4-parameter shorthand: title, window size, and initial state.
   * All other settings use their defaults (60 fps, VSync on, windowed).
   *
   * <p>For anything beyond these four parameters, prefer {@link #FlixelGame(Config, FlixelState)}
   * with a {@link Config} instead.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialState The initial state to load when the game starts.
   */
  public FlixelGame(String title, int width, int height, FlixelState initialState) {
    this(new Config.Builder(title).size(width, height).build(), initialState);
  }

  /**
   * Creates a new game instance configured entirely by the supplied {@link Config}, using a
   * factory that produces the initial state.
   *
   * <p>This is the primary constructor that all others delegate to. Use {@code () -> new MyState()} for a
   * fresh instance each session, or {@code () -> sharedState} to reuse one object whose
   * {@link FlixelState#destroy()} and {@link FlixelState#create()} lifecycle still runs on each
   * {@link Flixel#switchState} call.
   *
   * @param config The configuration that supplies all startup settings.
   * @param initialStateFactory A factory that produces the initial state to load when the game starts.
   */
  public FlixelGame(@NotNull Config config, @NotNull Supplier<FlixelState> initialStateFactory) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.initialStateFactory = Objects.requireNonNull(initialStateFactory, "initialStateFactory cannot be null");
  }

  /**
   * Called when the game is created. This is where you should initialize your game's resources.
   *
   * <p>This method configures the crash handler, sets up input processing, initializes the debug overlay, configures
   * the ANSI system for color output in terminals, and then switches to the initial state.
   *
   * <p>This method is called automatically by the platform runner once the backend surface is
   * ready, so it is not necessary to call it manually in most cases. However, it can be overridden
   * to perform custom initialization when the game is created.
   */
  public void create() {
    configureCrashHandler(); // This should ALWAYS be called first no matter what!

    // Deferred to here (rather than earlier) since compressed-texture support depends on the
    // graphics backend, which is only guaranteed to be running once create() is reached.
    Flixel.assets.setCompressedTexturesEnabled(true);

    isClosed = false;
    isClosing = false;
    stateLifecyclePauseDispatched = false;

    batch = Flixel.graphics.getBatch();
    cameras.clear();
    cameras.add(new FlixelCamera(config.getWidth(), config.getHeight()));
    overlayCamera = new FlixelCamera(config.getWidth(), config.getHeight());
    overlayGroup = new FlixelBasicGroup<>(IFlixelBasic[]::new) {
    };

    bgPixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);

    // Register keyboard, mouse, and touch listeners with the input device.
    if (Flixel.keys != null) {
      Flixel.input.addKeyboardListener(Flixel.keys);
    }
    if (Flixel.mouse != null) {
      Flixel.input.addMouseListener(Flixel.mouse);
    }
    if (Flixel.touches != null) {
      Flixel.input.addTouchListener(Flixel.touches);
    }

    // Create the debug overlay when debug mode is enabled.
    if (Flixel.isDebugMode()) {
      FlixelDebugOverlay overlay = Flixel.createDebugOverlay();
      if (Flixel.log != null) {
        Flixel.log.addLogListener(overlay.getLogListener());
      }
    }

    Flixel.switchState(initialStateFactory.get(), true, true, true, initialStateFactory);
  }

  /**
   * Called by the platform runner when the game's drawable surface changes size.
   *
   * @param width The new surface width in pixels.
   * @param height The new surface height in pixels.
   */
  public void resize(int width, int height) {
    for (FlixelCamera camera : cameras) {
      camera.update(width, height, camera.centerCameraOnResize);
    }
    if (overlayCamera != null && overlayEnabled) {
      overlayCamera.update(width, height, overlayCamera.centerCameraOnResize);
    }

    if (Flixel.debug != null) {
      Flixel.debug.overlay.resize(width, height);
    }

    FlixelState state = Flixel.state;
    if (state != null) {
      state.resize(width, height);
    }

    if (!globalShaders.isEmpty()) {
      initSceneFbos(globalShaders.getSize() > 1);
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
    if (Flixel.touches != null) {
      Flixel.touches.update();
    }
    if (Flixel.gamepads != null) {
      Flixel.gamepads.update();
    }
    FlixelActionSets.update(elapsed);

    if (!gamePaused && shouldUpdate) {
      FlixelTween.updateTweens(elapsed);
      FlixelTimer.getGlobalManager().update(elapsed);

      // Walk the state/substate chain. Each state in the chain is updated only
      // if it is the active (innermost) state or if its persistentUpdate flag is true.
      FlixelState current = Flixel.state;
      while (current != null) {
        FlixelState sub = current.getSubState();
        boolean hasSubState = (sub != null);

        if (!hasSubState || current.persistentUpdate) {
          current.update(elapsed);
        }

        current = sub;
      }

      if (Flixel.sound != null) {
        Flixel.sound.update(elapsed);
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

    if (Flixel.debug != null && Flixel.isDebugMode()) {
      Flixel.debug.overlay.update(elapsed);
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

    Flixel.graphics.clear(bgColor.r, bgColor.g, bgColor.b, bgColor.a); // Clear the screen to refresh it.
    FlixelState state = Flixel.state;

    int totalRenderCallsBefore = batch.getTotalRenderCalls();

    boolean useGlobalFbo = !globalShaders.isEmpty() && sceneFboA != null;
    if (useGlobalFbo) {
      sceneFboA.begin();
      Flixel.graphics.clear(0f, 0f, 0f, 0f);
    }

    // Loop through all cameras and draw the state/substate chain onto each camera.
    FlixelCamera[] cameraItems = cameras.getItems();
    for (int ci = 0, cn = cameras.getSize(); ci < cn; ci++) {
      FlixelCamera camera = cameraItems[ci];
      Flixel.graphics.beginCameraPass();
      Flixel.setDrawCamera(camera);
      try {
        if (gamePaused) {
          camera.applyCameraTransform();
        }
        camera.applyViewport();

        FlixelShader cameraShader = camera.getShader();
        if (cameraShader != null) {
          camera.getFbo().begin();
          Flixel.graphics.clear(0f, 0f, 0f, 0f);
        }

        batch.setProjection(camera.getCombinedMatrix());
        batch.begin();

        camera.fill(camera.bgColor, camera.useBgAlphaBlending, 1f, batch, bgPixel);

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

        camera.drawFX(batch, bgPixel);

        batch.end();

        // Safety reset: per-sprite shader draws restore themselves inline, but reset here as a
        // backstop so the next camera pass always starts with the default batch shader.
        batch.setShader(null);

        if (cameraShader != null) {
          // Ending the camera's render target returns drawing to the global target (when active)
          // or the screen, since render targets nest.
          camera.getFbo().end();
          camera.applyViewport();
          if (camera.width != fboOrthoW || camera.height != fboOrthoH) {
            fboOrthoW = camera.width;
            fboOrthoH = camera.height;
            fboOrtho.setToOrtho2D(0, 0, fboOrthoW, fboOrthoH);
          }
          batch.setProjection(fboOrtho);
          batch.setShader(cameraShader.getProgram());
          batch.begin();
          cameraShader.applyUniforms();
          drawFullTarget(batch, camera.getFbo(), camera.width, camera.height);
          batch.end();
          batch.setShader(null);
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
      Flixel.graphics.beginCameraPass();
      Flixel.setDrawCamera(overlayCamera);
      try {
        if (gamePaused) {
          overlayCamera.applyCameraTransform();
        }
        overlayCamera.applyViewport();
        batch.setProjection(overlayCamera.getCombinedMatrix());
        batch.begin();
        overlayGroup.draw(batch);
        batch.end();
      } finally {
        Flixel.setDrawCamera(null);
      }
    }

    frameRenderCalls = batch.getTotalRenderCalls() - totalRenderCallsBefore;

    if (Flixel.debug != null) {
      Flixel.debug.overlay.drawBoundingBoxes(cameras.getItems());
      Flixel.debug.overlay.draw();
    }

    if (!desktopTransparencyActive && transparentFramebufferRequested
        && Flixel.host.getPlatform() == FlixelPlatform.Desktop) {
      Flixel.graphics.forceOpaqueAlpha();
    }

    Flixel.Signals.postDraw.dispatch();
  }

  /**
   * Draws a render target's whole texture into the given rectangle, flipping it vertically when
   * the backend stores the target upside down.
   */
  private static void drawFullTarget(FlixelBatch batch, FlixelRenderTarget target, float width, float height) {
    if (target.isFlipped()) {
      batch.draw(target.getTexture(), 0, 0, width, height, 0f, 1f, 1f, 0f);
    } else {
      batch.draw(target.getTexture(), 0, 0, width, height);
    }
  }

  /**
   * Runs one frame: {@link #update(float)} then {@link #draw(FlixelBatch)}, with the elapsed time
   * clamped to the min and max values to prevent major lag spikes.
   *
   * <p>This method is called automatically by the platform runner every frame with the raw
   * wall-clock time since the previous frame.
   *
   * <p>You should not (and cannot) override this method. You are encouraged to override either
   * {@link #update(float)} or {@link #draw(FlixelBatch)} instead, as they separate logic
   * and drawing correctly.
   *
   * @param rawDeltaSeconds The raw time since the last frame, in seconds.
   * @see #update(float)
   * @see #draw(FlixelBatch)
   */
  public final void render(float rawDeltaSeconds) {
    float rawClamped = Math.max(Flixel.MIN_ELAPSED, Math.min(rawDeltaSeconds, Flixel.MAX_ELAPSED));
    float elapsed = rawClamped * Flixel.timeScale;
    Flixel.rawElapsed = rawClamped;
    Flixel.elapsed = elapsed;

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
    if (Flixel.touches != null) {
      Flixel.touches.endFrame();
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
    if (cameras.getSize() == 0) {
      debugPauseCameraScroll = null;
      debugPauseCameraZoom = null;
      return;
    }
    int n = cameras.getSize();
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
    int n = Math.min(debugPauseCameraScroll.length, Math.min(debugPauseCameraZoom.length, cameras.getSize()));
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
  public final void pause() {
    onFocusLost();
  }

  /**
   * Do not override this method. Override {@link #onFocusGained()} instead.
   */
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
      Flixel.window.setContinuousRendering(false);
      shouldUpdate = false;
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
    if (autoPause) {
      shouldUpdate = true;
      if (!gamePaused) {
        Flixel.sound.resume();
        Flixel.window.setContinuousRendering(true);
        Flixel.window.requestRendering();
      }
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
   * {@link Flixel.Signals#windowMinimized}.
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
    boolean currentFullscreen = Flixel.window.isFullscreen();
    if (enabled == currentFullscreen || fullscreenChangeInProgress) {
      return;
    }
    fullscreenChangeInProgress = true;
    try {
      if (enabled) {
        Flixel.window.setFullscreen(Flixel.graphics.getDisplayMode());
      } else {
        Flixel.window.setWindowed(config.getWidth(), config.getHeight());
      }
    } finally {
      fullscreenChangeInProgress = false;
    }
  }

  /** Toggles fullscreen mode on or off, depending on the current state. */
  public void toggleFullscreen() {
    setFullscreen(!Flixel.window.isFullscreen());
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
    initSceneFbos(needsPingPong || globalShaders.getSize() > 1);
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
        initSceneFbos(globalShaders.getSize() > 1);
      }
    }
    return removed;
  }

  /** Creates (or recreates) the scene render targets used by the global shader chain. */
  private void initSceneFbos(boolean needPingPong) {
    disposeSceneFbos();
    int w = Flixel.graphics.getBackBufferWidth();
    int h = Flixel.graphics.getBackBufferHeight();
    sceneFboA = Flixel.graphics.createRenderTarget(w, h);
    if (needPingPong) {
      sceneFboB = Flixel.graphics.createRenderTarget(w, h);
    }
  }

  /** Releases the scene render targets. */
  private void disposeSceneFbos() {
    if (sceneFboA != null) {
      sceneFboA.destroy();
      sceneFboA = null;
    }
    if (sceneFboB != null) {
      sceneFboB.destroy();
      sceneFboB = null;
    }
  }

  /**
   * Composites the scene render target to the screen by running it through the global shader
   * chain. When more than one shader is present the passes ping-pong between {@link #sceneFboA}
   * and {@link #sceneFboB} so each shader reads from one texture and writes to the other.
   */
  private void applyGlobalShaderChain() {
    int w = Flixel.graphics.getBackBufferWidth();
    int h = Flixel.graphics.getBackBufferHeight();
    boolean usingA = true;
    FlixelRenderTarget src = sceneFboA;
    int n = globalShaders.getSize();

    for (int i = 0; i < n; i++) {
      FlixelShader gs = globalShaders.get(i);
      boolean isLast = (i == n - 1);

      if (w != fboOrthoW || h != fboOrthoH) {
        fboOrthoW = w;
        fboOrthoH = h;
        fboOrtho.setToOrtho2D(0, 0, w, h);
      }
      batch.setProjection(fboOrtho);
      batch.setShader(gs.getProgram());

      if (!isLast) {
        FlixelRenderTarget dst = usingA ? sceneFboB : sceneFboA;
        dst.begin();
        Flixel.graphics.clear(0f, 0f, 0f, 0f);
        batch.begin();
        gs.applyUniforms();
        drawFullTarget(batch, src, w, h);
        batch.end();
        dst.end();
        src = dst;
        usingA = !usingA;
      } else {
        batch.begin();
        gs.applyUniforms();
        drawFullTarget(batch, src, w, h);
        batch.end();
      }
    }
    batch.setShader(null);
  }

  /**
   * Destroys the game and all of its resources.
   *
   * <p>Note that this doesn't close the game entirely, it just disposes
   * of the game's resources. If you want to close the entire game, use {@link Flixel#exit()}.
   */
  @Override
  public void destroy() {
    if (isClosing) {
      return;
    }
    isClosing = true;

    Flixel.setDrawCamera(null);

    Flixel.Signals.preGameClose.dispatch();

    if (Flixel.debug != null) {
      if (Flixel.log != null) {
        Flixel.log.removeLogListener(Flixel.debug.overlay.getLogListener());
      }
      Flixel.debug.overlay.destroy();
      Flixel.clearDebugOverlay();
    }

    if (Flixel.gamepads != null) {
      Flixel.gamepads.detach();
    }

    FlixelTween.cancelActiveTweens();
    FlixelTween.clearTweenPools();
    FlixelTween.resetRegistry();
    FlixelTimer.cancelAll();

    if (Flixel.state != null) {
      Flixel.state.destroy();
    }
    // The batch is owned by the graphics backend, not the game, so it is not destroyed here.
    batch = null;
    disposeSceneFbos();
    globalShaders.clear();
    fboOrthoW = -1;
    fboOrthoH = -1;
    // bgPixel is a shared, persistent asset owned by the asset manager; do not destroy it here.
    bgPixel = null;

    if (Flixel.assets != null) {
      Flixel.assets.destroy();
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
   * Supplies the framework crash response to the active platform backend via
   * {@link org.flixelgdx.backend.FlixelRuntimeDevice#setCrashHandler}.
   *
   * <p>The callback logs the exception, shows an error alert, tears down game resources, and exits
   * the process on platforms where that is permitted. Each backend installs the handler using
   * whatever mechanism its runtime provides, so this method makes no assumptions about Java threads
   * or any other platform-specific API.
   */
  protected void configureCrashHandler() {
    Flixel.runtime.setCrashHandler((thread, throwable) -> {
      String logs = FlixelRuntimeUtil.getFullExceptionMessage(throwable);
      String threadName = thread != null ? thread.getName() : "unknown";
      String msg = "There was an uncaught exception on thread \"" + threadName + "\"!\n" + logs;
      Flixel.error(msg);
      Flixel.alert.error("Uncaught Exception", msg);
      destroy();
      // Only quit on non-iOS platforms to avoid App Store guideline violations!
      if (Flixel.host.getPlatform() != FlixelPlatform.iOS) {
        Flixel.exit();
      }
    });
  }

  /**
   * Resets the camera list to contain a single default camera with the current window size as its viewport.
   */
  public void resetCameras() {
    FlixelCamera camera = new FlixelCamera(config.getWidth(), config.getHeight());
    camera.update(Flixel.graphics.getBackBufferWidth(), Flixel.graphics.getBackBufferHeight(),
        camera.centerCameraOnResize);
    cameras.clear();
    cameras.add(camera);
    // The debug-pause snapshot refers to cameras that no longer exist after this reset,
    // so discard it. restoreCamerasAfterDebugPause() already handles null gracefully.
    debugPauseCameraScroll = null;
    debugPauseCameraZoom = null;
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

  public FlixelArray<FlixelCamera> getCameras() {
    return cameras;
  }

  @NotNull
  public FlixelBatch getBatch() {
    return batch;
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

  public FlixelColor getBgColor() {
    return bgColor;
  }

  public void setBgColor(@NotNull FlixelColor bgColor) {
    if (bgColor == null) {
      return;
    }
    this.bgColor.set(bgColor);
  }

  public boolean isTransparentFramebufferRequested() {
    return transparentFramebufferRequested;
  }

  /** Returns whether an alpha-capable framebuffer was requested at launch. */
  public boolean getTransparentFramebufferRequested() {
    return transparentFramebufferRequested;
  }

  /**
   * @return {@code true} after {@link #applyBackdropForDesktopTransparency(boolean)} was called with {@code true}.
   */
  public boolean isTransparencyActive() {
    return desktopTransparencyActive;
  }

  /** Returns {@code true} after desktop transparency was applied via {@link #applyBackdropForDesktopTransparency(boolean)}. */
  public boolean getTransparencyActive() {
    return desktopTransparencyActive;
  }

  /**
   * Updates global and per-camera backdrop drawing for desktop compositing. Called from
   * {@link FlixelWindow FlixelWindow}. When desktop see-through is off but the window
   * was created with a transparent-capable framebuffer, {@link FlixelDrawable#draw} also forces
   * framebuffer alpha to {@code 1} after rendering so tinted sprites do not composite through the real desktop.
   *
   * @param active {@code true} for transparent clears and camera fills. {@code false} restores colors
   *     captured the first time transparency was enabled this session (then clears that cache), or opaque black
   *     if transparency was never enabled.
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
    FlixelCamera[] camItems = cameras.getItems();
    for (int i = 0, n = cameras.getSize(); i < n; i++) {
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
    int n = cameras.getSize();
    ensureDesktopTransparencyCameraSnapshotCapacity(n);
    FlixelCamera[] camItems = n == 0 ? null : cameras.getItems();
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
      bgColor.set(FlixelColor.BLACK);
    }
    FlixelCamera[] camItems = cameras.getItems();
    int n = cameras.getSize();
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
        cam.bgColor.set(FlixelColor.BLACK);
      }
    }
  }

  private void clearDesktopTransparencyRestoreSnapshot() {
    desktopTransparencyRestoreSnapshotValid = false;
    desktopTransparencyRestoreCameraCount = 0;
    Arrays.fill(desktopTransparencyRestoreGameRgba, 0f);
    Arrays.fill(desktopTransparencyRestoreCamerasPacked, 0f);
  }

  public String getTitle() {
    return config.getTitle();
  }

  public String getCompany() {
    return config.getCompany();
  }

  public String getVersion() {
    return config.getVersion();
  }

  @NotNull
  public Config getConfig() {
    return config;
  }

  public boolean isGamePaused() {
    return gamePaused;
  }

  /** Returns whether the game is currently paused. */
  public boolean getGamePaused() {
    return gamePaused;
  }

  public boolean isClosing() {
    return isClosing;
  }

  /** Returns whether the game is in the process of closing. */
  public boolean getClosing() {
    return isClosing;
  }

  public boolean isClosed() {
    return isClosed;
  }

  /** Returns whether the game window has fully closed. */
  public boolean getClosed() {
    return isClosed;
  }

  public int getFramerate() {
    return config.getFramerate();
  }

  public boolean isVsync() {
    return config.isVsync();
  }

  public boolean getVsync() {
    return config.isVsync();
  }

  public boolean isFullscreen() {
    return config.isFullscreen();
  }

  public boolean getFullscreen() {
    return config.isFullscreen();
  }

  public int getInitialWidth() {
    return config.getWidth();
  }

  public int getInitialHeight() {
    return config.getHeight();
  }

  public boolean isGlobalOverlayEnabled() {
    return overlayEnabled;
  }

  /** Returns whether the global overlay camera is enabled. */
  public boolean getGlobalOverlayEnabled() {
    return overlayEnabled;
  }

  public boolean getShouldUpdate() {
    return isShouldUpdate();
  }

  public boolean isShouldUpdate() {
    return shouldUpdate;
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

  /**
   * Immutable startup configuration for a {@link FlixelGame}.
   *
   * <p>Build one via {@link Builder}, pass it to your {@link FlixelGame} constructor, and the
   * framework reads it once at startup. No property can change after {@link Builder#build()} returns,
   * so there is never any ambiguity between what was configured and what the game is running with.
   *
   * <p>Most properties have sensible defaults (640x360, 60 fps, VSync on, windowed), so you only
   * need to set what differs. Two properties deserve special attention:
   *
   * <ul>
   *   <li>{@code title} - the text shown in the game window's title bar. Required; pass it to the
   *       {@link Builder} constructor.</li>
   *   <li>{@code company} - the studio or organization name. Strongly recommended whenever the game
   *       uses {@link FlixelSave}. The save system combines it with the title to build the
   *       OS-specific data directory ({@code %APPDATA%\Company\Title\saves\} on Windows,
   *       {@code ~/Library/Application Support/Company/Title/saves/} on macOS,
   *       {@code $XDG_DATA_HOME/Company/Title/saves/} on Linux). Calling
   *       {@link FlixelSave#bind(String, String)} without a company name is an error.</li>
   * </ul>
   *
   * <pre>{@code
   * new FlixelGame.Config.Builder("My Game")
   *     .company("My Studio")
   *     .version("1.0.0")
   *     .size(1280, 720)
   *     .build()
   * }</pre>
   *
   * @see FlixelGame
   * @see Builder
   */
  public static final class Config {

    @NotNull
    private final String title;

    @NotNull
    private final String company;

    @NotNull
    private final String version;

    private final int width;

    private final int height;

    private final int framerate;

    private final boolean vsync;

    private final boolean fullscreen;

    private Config(@NotNull Builder builder) {
      this.title = builder.title;
      this.company = builder.company;
      this.version = builder.version;
      this.width = builder.width;
      this.height = builder.height;
      this.framerate = builder.framerate;
      this.vsync = builder.vsync;
      this.fullscreen = builder.fullscreen;
    }

    @NotNull
    public String getTitle() {
      return title;
    }

    @NotNull
    public String getCompany() {
      return company;
    }

    @NotNull
    public String getVersion() {
      return version;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public int getFramerate() {
      return framerate;
    }

    public boolean isVsync() {
      return vsync;
    }

    public boolean getVsync() {
      return vsync;
    }

    public boolean isFullscreen() {
      return fullscreen;
    }

    public boolean getFullscreen() {
      return fullscreen;
    }

    /**
     * Fluent builder for {@link Config}.
     *
     * <p>The game title is required and must be supplied to the constructor. Everything else
     * defaults to a safe value and can be set in any order before calling {@link #build()}.
     *
     * <p>The same builder instance must not be reused after {@link #build()} is called; create a
     * new one instead.
     *
     * <pre>{@code
     * FlixelGame.Config config = new FlixelGame.Config.Builder("My Game")
     *     .company("My Studio")
     *     .version("1.0.0")
     *     .size(1280, 720)
     *     .framerate(144)
     *     .vsync(false)
     *     .build();
     * }</pre>
     */
    public static final class Builder {

      @NotNull
      private final String title;

      @NotNull
      private String company = "";

      @NotNull
      private String version = "";

      private int width = 640;

      private int height = 360;

      private int framerate = 60;

      private boolean vsync = true;

      private boolean fullscreen = false;

      /**
       * Creates a builder for a game with the given window title.
       *
       * @param title The title to display in the game window's title bar. Must not be null or empty.
       * @throws IllegalArgumentException if {@code title} is null or empty.
       */
      public Builder(@NotNull String title) {
        if (title == null || title.isEmpty()) {
          throw new IllegalArgumentException("Game title cannot be null or empty.");
        }
        this.title = title;
      }

      /**
       * Sets the company or studio name. Used by {@link FlixelSave} to build the OS-specific save
       * directory.
       *
       * @param company The company or studio name.
       * @return This builder, for chaining.
       */
      @NotNull
      public Builder company(@NotNull String company) {
        this.company = company != null ? company : "";
        return this;
      }

      /**
       * Sets the game version string (for example {@code "1.0.0"} or {@code "2.3.1-beta"}).
       *
       * @param version The version string.
       * @return This builder, for chaining.
       */
      @NotNull
      public Builder version(@NotNull String version) {
        this.version = version != null ? version : "";
        return this;
      }

      /**
       * Sets the starting window size and the dimensions of the first camera.
       *
       * @param width The width in pixels.
       * @param height The height in pixels.
       * @return This builder, for chaining.
       */
      @NotNull
      public Builder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
      }

      /**
       * Sets the target update and render framerate.
       *
       * @param framerate Frames per second.
       * @return This builder, for chaining.
       */
      @NotNull
      public Builder framerate(int framerate) {
        this.framerate = framerate;
        return this;
      }

      /**
       * Controls whether VSync is requested at startup.
       *
       * @param vsync {@code true} to cap rendering to the monitor's refresh rate.
       * @return This builder, for chaining.
       */
      @NotNull
      public Builder vsync(boolean vsync) {
        this.vsync = vsync;
        return this;
      }

      /**
       * Controls whether the game starts in fullscreen mode.
       *
       * @param fullscreen {@code true} to start fullscreen.
       * @return This builder, for chaining.
       */
      @NotNull
      public Builder fullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return this;
      }

      /**
       * Builds the immutable {@link Config} from the values set on this builder.
       *
       * @return A new, immutable config instance.
       */
      @NotNull
      public Config build() {
        return new Config(this);
      }
    }
  }
}
