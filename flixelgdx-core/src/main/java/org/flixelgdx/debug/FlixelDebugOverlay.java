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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelState;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelDrawable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.logging.FlixelLogEntry;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelDebugUtil;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Platform-agnostic <em>controller</em> for the FlixelGDX in-game debugger. This abstract class is the
 * baseline behavior that every FlixelGDX backend can rely on: it owns the visibility flags,
 * keybind handling, hitbox drawing, the pause/camera tools, and the pooled buffers that hold
 * watch values, log entries, and custom tracker blocks.
 *
 * <p>The class deliberately does <strong>not</strong> render any UI on its own. Doing so in the
 * core module would pull a heavy GUI dependency into every platform (web/iOS/Android).
 * Instead, backends extend this abstract class and implement {@link #drawUI()} to render the
 * panels with whatever toolkit suits the platform.
 *
 * <p>The default install path is:
 * <ol>
 *   <li>{@link Flixel#setDebugMode(boolean)} flips debug mode on in your launcher.</li>
 *   <li>The launcher (or your code) calls {@link Flixel#setDebugOverlay(java.util.function.Supplier)}
 *       with the backend-specific factory.</li>
 *   <li>{@link org.flixelgdx.FlixelGame FlixelGame} constructs the overlay during {@code create}
 *       and registers it with the logger.</li>
 * </ol>
 *
 * <p>Toggle overlay visibility with {@link #toggleKey} (default {@link Keybinds#DEFAULT_TOGGLE_KEY}).
 * Toggle visual debug (hitboxes) with {@link #drawDebugKey} (default {@link Keybinds#DEFAULT_DRAW_DEBUG_KEY}).
 * In debug mode, {@link #pauseKey} (default F4) pauses the game; while paused you can inspect the camera
 * with Alt+arrows ({@link #cameraCycleLeftKey} / {@link #cameraCycleRightKey}), {@link #cameraPanButton} pan,
 * and the mouse wheel zoom.
 *
 * <h2>Reduced allocation rate</h2>
 *
 * <p>Strings are the easiest way to allocate yourself into stutters. To avoid that, this class:
 * <ul>
 *   <li>Caches FPS, heap, native and object counters as primitives and only refreshes them every
 *       {@value #STATS_UPDATE_INTERVAL} seconds.</li>
 *   <li>Keeps log entries as {@link BufferedLogLine} records pooled across frames; the renderer
 *       receives the level, tag, and message via {@link #copyLogBuffer(FlixelArray)} without producing
 *       any markup string.</li>
 *   <li>Refreshes watch entries at 10Hz into reusable {@link FlixelString} buffers
 *       ({@link FlixelDebugWatchManager#fillWatchEntries(FlixelArray, FlixelArray)}).</li>
 *   <li>Reuses {@link CachedTrackerBlock} instances across rebuilds.</li>
 * </ul>
 */
public abstract class FlixelDebugOverlay implements FlixelUpdatable, FlixelDestroyable {

  /** Seconds between automatic refreshes of cached primitive stats while the overlay is visible. */
  protected static final float STATS_UPDATE_INTERVAL = 0.5f;

  /** Seconds between watch buffer rebuilds while the overlay is visible. */
  protected static final float WATCH_REFRESH_INTERVAL = 0.1f;

  /** Seconds between samples written into the performance ring buffers. */
  protected static final float PERF_SAMPLE_INTERVAL = 0.05f;

  /**
   * Number of samples retained in each performance ring buffer. {@value} samples at
   * {@value #PERF_SAMPLE_INTERVAL}s gives a rolling window of about six seconds.
   */
  public static final int PERF_HISTORY_SIZE = 120;

  /** Fallback color used when a {@link FlixelDebugDrawable} returns a {@code null} or undersized array. */
  private static final float[] FALLBACK_BOUNDING_BOX_COLOR = { 1f, 0.2f, 0.2f, 0.6f };

  /** Key that toggles overlay visibility. Set to {@link FlixelKey#NONE} to disable. */
  public int toggleKey = Keybinds.DEFAULT_TOGGLE_KEY;

  /** Key that toggles bounding-box (hitbox) drawing. Set to {@link FlixelKey#NONE} to disable. */
  public int drawDebugKey = Keybinds.DEFAULT_DRAW_DEBUG_KEY;

  /** Key that pauses/unpauses the game loop (debug mode only). Set to {@link FlixelKey#NONE} to disable. */
  public int pauseKey = Keybinds.DEFAULT_PAUSE_KEY;

  /** Key that cycles the debug inspect camera left while paused (with Alt). Set to {@link FlixelKey#NONE} to disable. */
  public int cameraCycleLeftKey = Keybinds.DEFAULT_DEBUG_CAMERA_CYCLE_LEFT;

  /** Key that cycles the debug inspect camera right while paused (with Alt). Set to {@link FlixelKey#NONE} to disable. */
  public int cameraCycleRightKey = Keybinds.DEFAULT_DEBUG_CAMERA_CYCLE_RIGHT;

  /** Mouse button used to pan the debug camera while paused. Set to a negative value to disable. */
  public int cameraPanButton = FlixelMouseButton.RIGHT;

  protected float statsTimer = 0f;
  protected int cachedFps;
  protected float cachedHeapMegabytes;
  protected float cachedNativeMegabytes;
  protected int cachedObjectCount;
  protected int cachedAssetCount;

  /**
   * Total render-call count snapshotted at the start of each {@link #draw()} call, after the
   * game's batch has been ended for the frame. Sums the framework's own batch and
   * any batches registered via {@link FlixelDebugManager#trackBatch(FlixelBatch)}.
   */
  protected int cachedRenderCalls;

  private float perfSampleTimer = 0f;

  /** Frame-time samples in milliseconds (full real-time wall delta). */
  protected final float[] perfFrameMs = new float[PERF_HISTORY_SIZE];

  /** Java heap usage in megabytes per sample. */
  protected final float[] perfHeapMb = new float[PERF_HISTORY_SIZE];

  /** Native (GL/audio) heap usage in megabytes per sample. */
  protected final float[] perfNativeMb = new float[PERF_HISTORY_SIZE];

  /** FPS per sample, as reported by the graphics backend. */
  protected final float[] perfFps = new float[PERF_HISTORY_SIZE];

  /**
   * Render-call count per sample. Sampled in {@link #pushPerfSample(float)} after the
   * previous frame's batch has been ended but before the next frame's {@code begin()} resets the
   * counter, so it always reflects a complete frame even when the overlay is hidden.
   */
  protected final float[] perfRenderCalls = new float[PERF_HISTORY_SIZE];

  /** Index of the next sample to write (rolls over once the ring is full). */
  protected int perfHead = 0;

  /** Number of valid samples in each perf ring (caps at {@link #PERF_HISTORY_SIZE}). */
  protected int perfCount = 0;

  protected float watchRefreshTimer = 0f;

  /** Cached watch keys refreshed at {@value #WATCH_REFRESH_INTERVAL}s; buffers are reused across refreshes. */
  protected final FlixelArray<FlixelString> cachedWatchKeys = new FlixelArray<>();

  /** Cached watch values refreshed at {@value #WATCH_REFRESH_INTERVAL}s; buffers are reused across refreshes. */
  protected final FlixelArray<FlixelString> cachedWatchValues = new FlixelArray<>();

  protected final FlixelArray<CachedTrackerBlock> cachedTrackerBlocks = new FlixelArray<>();

  /** Pool of tracker blocks between rebuilds to avoid reallocating block objects. */
  private final FlixelArray<CachedTrackerBlock> cachedTrackerBlockPool = new FlixelArray<>();

  /** Latest log lines, oldest first; bounded by {@link FlixelLogger#MAX_LOG_ENTRIES}. */
  protected final Deque<BufferedLogLine> logBuffer = new ArrayDeque<>();

  /** Pool of {@link BufferedLogLine} instances reused as the buffer rolls over. */
  private final FlixelArray<BufferedLogLine> logLinePool = new FlixelArray<>();

  private final Consumer<FlixelLogEntry> logListener = this::onLogEntry;

  protected int debugInspectCameraIndex;

  /** Screen-space anchor for Alt+RMB pan (avoids mixing world unprojects across changing scroll). */
  private int lastPanScreenX;
  private int lastPanScreenY;

  private final FlixelColor boundingBoxColor = new FlixelColor();

  private final FlixelVector panUnprojectA = new FlixelVector();
  private final FlixelVector panUnprojectB = new FlixelVector();

  /** Cached unproject scratch used while picking/dragging objects (kept off the per-frame allocation path). */
  private final FlixelVector pickUnproject = new FlixelVector();

  /** World-space offset between cursor and dragged sprite's origin so it does not snap on grab. */
  private float dragOffsetX;
  private float dragOffsetY;

  /** Visibility flag for the renderer. Hitbox drawing is gated by {@link #drawDebug} instead. */
  private boolean visible = false;

  private boolean drawDebug = false;

  /** Guards against {@link #destroy()} running its teardown more than once. */
  private boolean destroyed = false;

  /** Constructs the shared debug overlay state. Subclasses should call this before wiring platform UI. */
  protected FlixelDebugOverlay() {}

  public final Consumer<FlixelLogEntry> getLogListener() {
    return logListener;
  }

  public final boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    if (visible && !this.visible) {
      forceRefreshOnNextUpdate();
    }
    this.visible = visible;
  }

  public void toggleVisible() {
    visible = !visible;
    if (visible) {
      forceRefreshOnNextUpdate();
    }
  }

  public final boolean isDrawDebug() {
    return drawDebug;
  }

  public void setDrawDebug(boolean drawDebug) {
    this.drawDebug = drawDebug;
  }

  public void toggleDrawDebug() {
    drawDebug = !drawDebug;
  }

  /** Returns the camera currently selected by Alt+arrow cycling, clamped to a valid index. */
  public final int getInspectCameraIndex() {
    FlixelArray<FlixelCamera> cams = Flixel.cameras;
    int n = (cams != null) ? cams.getSize() : 0;
    if (n == 0) {
      return -1;
    }
    if (debugInspectCameraIndex < 0 || debugInspectCameraIndex >= n) {
      debugInspectCameraIndex = 0;
    }
    return debugInspectCameraIndex;
  }

  /**
   * Schedules the next {@link #update(float)} to refresh stats and watch buffers immediately. Useful
   * when the overlay was just shown or when a backend wants the very first frame to display fresh data.
   */
  protected final void forceRefreshOnNextUpdate() {
    statsTimer = STATS_UPDATE_INTERVAL;
    watchRefreshTimer = WATCH_REFRESH_INTERVAL + 0.01f;
  }

  /**
   * Called every frame from the game loop to handle keybind input and refresh cached stats. Subclasses
   * that need to update their own UI state should override {@link #onUpdateUI(float)} rather than this
   * method, so the input/state handling stays consistent across backends.
   *
   * @param elapsed Seconds since the last frame.
   */
  @Override
  public void update(float elapsed) {
    handleToggleKeys();

    if (Flixel.isDebugMode()) {
      // The keyboard manager already reports the pause key as not pressed while an imgui text field
      // has focus (isKeyboardCapturedByUI), so a plain justPressed does not fire mid-typing.
      if (Flixel.keys.justPressed(pauseKey)) {
        Flixel.game.setGamePaused(!Flixel.game.isGamePaused());
      }
      if (Flixel.game.isGamePaused()) {
        handleInspectCameraTools();
        handleSpritePicker();
      } else {
        // Make sure we never leave a half-finished drag in place when the user un-pauses.
        if (Flixel.debug != null) {
          Flixel.debug.setDraggedSprite(null);
        }
      }
    }

    // Performance ring buffers are sampled whenever the game runs in debug mode, not only while
    // the overlay is visible, so opening a panel mid-session still shows a useful history instead
    // of an empty graph for the first several seconds.
    if (Flixel.isDebugMode()) {
      perfSampleTimer += elapsed;
      if (perfSampleTimer >= PERF_SAMPLE_INTERVAL) {
        perfSampleTimer = 0f;
        pushPerfSample(elapsed);
      }
    }

    if (!visible) {
      return;
    }

    statsTimer += elapsed;
    watchRefreshTimer += elapsed;

    if (statsTimer >= STATS_UPDATE_INTERVAL) {
      statsTimer = 0f;
      cachedFps = Flixel.graphics.getFps();
      cachedHeapMegabytes = Flixel.runtime.getJavaHeap() / (1024f * 1024f);
      cachedNativeMegabytes = Flixel.runtime.getNativeHeap() / (1024f * 1024f);
      cachedObjectCount = FlixelDebugUtil.countActiveMembers();
      cachedAssetCount = Flixel.assets != null ? Flixel.assets.getLoadedAssetCount() : 0;
    }

    if (watchRefreshTimer >= WATCH_REFRESH_INTERVAL) {
      watchRefreshTimer = 0f;
      refreshWatchEntries();
      rebuildCachedTrackerBlocks();
    }

    onUpdateUI(elapsed);
  }

  /**
   * Appends one sample to each performance ring buffer. The buffers are primitive {@code float[]}
   * arrays sized at {@link #PERF_HISTORY_SIZE}, so this method is allocation-free.
   *
   * <p>The frame-time series always reads {@link Flixel#getRawElapsed()} so it reflects actual
   * wall-clock time regardless of the active {@link Flixel#timeScale}.
   *
   * @param elapsed Scaled elapsed time passed by the update loop (kept for subclass compatibility).
   */
  protected void pushPerfSample(float elapsed) {
    int idx = perfHead;
    perfFrameMs[idx] = Flixel.getRawElapsed() * 1000f;
    perfHeapMb[idx] = Flixel.runtime.getJavaHeap() / (1024f * 1024f);
    perfNativeMb[idx] = Flixel.runtime.getNativeHeap() / (1024f * 1024f);
    perfFps[idx] = Flixel.graphics.getFps();
    perfRenderCalls[idx] = sampleRenderCallsNow();
    perfHead = (idx + 1) % PERF_HISTORY_SIZE;
    if (perfCount < PERF_HISTORY_SIZE) {
      perfCount++;
    }
  }

  /**
   * Reads the current total render-call count from the framework batch and any user-registered
   * batches. Safe to call from {@link #pushPerfSample(float)} in {@link #update(float)} because
   * the game loop ends the previous frame's batch before calling {@code update}, so the counter
   * reflects the just-completed frame and has not yet been reset by the next {@code begin()}.
   */
  private int sampleRenderCallsNow() {
    int total = 0;
    if (Flixel.game != null) {
      total += Flixel.game.getFrameRenderCalls();
    }
    FlixelDebugManager mgr = Flixel.debug;
    if (mgr != null) {
      FlixelArray<FlixelBatch> extra = mgr.getTrackedBatches();
      for (int i = 0, n = extra.getSize(); i < n; i++) {
        FlixelBatch b = extra.get(i);
        if (b != null) {
          total += b.getRenderCalls();
        }
      }
    }
    return total;
  }

  /** Returns the index immediately after the latest sample (where the next write will go). */
  public final int getPerfHead() {
    return perfHead;
  }

  /** Returns the number of valid samples in each perf series. Caps at {@link #PERF_HISTORY_SIZE}. */
  public final int getPerfCount() {
    return perfCount;
  }

  public final float[] getPerfFrameMs() {
    return perfFrameMs;
  }

  public final float[] getPerfHeapMb() {
    return perfHeapMb;
  }

  public final float[] getPerfNativeMb() {
    return perfNativeMb;
  }

  public final float[] getPerfFps() {
    return perfFps;
  }

  public final float[] getPerfRenderCalls() {
    return perfRenderCalls;
  }

  /** Hook for subclass UI state updates that should happen every frame while the overlay is visible. */
  protected void onUpdateUI(float elapsed) {}

  private void handleToggleKeys() {
    // The overlay's own input reaches Dear ImGui through a dedicated platform listener, so the
    // keyboard manager can suppress these toggles while a debug text field is focused
    // (isKeyboardCapturedByUI) and they still work any other time.
    if (Flixel.keys.justPressed(toggleKey)) {
      toggleVisible();
    }
    if (Flixel.keys.justPressed(drawDebugKey)) {
      toggleDrawDebug();
    }
  }

  private void refreshWatchEntries() {
    FlixelDebugWatchManager mgr = Flixel.watch;
    if (mgr != null && !mgr.isEmpty()) {
      mgr.fillWatchEntries(cachedWatchKeys, cachedWatchValues);
    } else {
      cachedWatchKeys.clear();
      cachedWatchValues.clear();
    }
    onWatchEntriesRefreshed();
  }

  /**
   * Hook fired after {@link #cachedWatchKeys} and {@link #cachedWatchValues} have been refilled. Override
   * in renderer subclasses that need to keep a parallel cache (for example a {@code String[]} for
   * Dear ImGui calls that only accept {@link String}).
   */
  protected void onWatchEntriesRefreshed() {}

  /**
   * Hook fired after {@link #cachedTrackerBlocks} has been rebuilt. Override in renderer subclasses
   * that need to keep a parallel cache (for example a {@code String[]} for Dear ImGui).
   */
  protected void onTrackerBlocksRebuilt() {}

  /**
   * Hook fired right after a new {@link FlixelLogEntry} has been pushed into {@link #logBuffer}.
   * The {@code line} argument is the pooled buffer that was just populated. Override in renderer
   * subclasses that need to mirror the entry into a parallel cache. Must not retain the reference;
   * the buffer may be recycled once the buffer rolls over.
   *
   * @param line The pooled buffer that was just populated.
   */
  protected void onLogEntryAppended(BufferedLogLine line) {}

  /**
   * Override to tell the framework's input layer that another UI layer (typically the imgui
   * debug overlay) is currently capturing the mouse. When this returns {@code true},
   * {@link FlixelMouseInputManager#pressed(int) FlixelMouseInputManager.pressed(int)} and
   * the matching {@code justPressed} / {@code justReleased} helpers will report {@code false}
   * for the game's regular input checks, and the debug camera tools / sprite picker also skip
   * their work, so clicking inside (for example) a Dear ImGui window does not bleed through
   * into the game logic. Defaults to {@code false}.
   *
   * <p>The overlay's own mouse tools (sprite picker, camera pan) use the regular
   * {@link FlixelMouseInputManager#pressed(int) FlixelMouseInputManager.pressed(int)} helpers, which
   * already report {@code false} while the cursor is over a debug panel, so a click there never grabs
   * a sprite or pans the camera.
   *
   * @return {@code true} if a foreground UI element is consuming mouse input this frame.
   */
  public boolean isMouseCapturedByUI() {
    return false;
  }

  /**
   * Override to tell the framework's input layer that another UI layer is currently consuming
   * keyboard input. When this returns {@code true},
   * {@link FlixelKeyInputManager#pressed(int) FlixelKeyInputManager.pressed(int)} and
   * the matching {@code justPressed} / {@code justReleased} helpers will report {@code false}
   * for the game's regular input checks, so typing in (for example) a Dear ImGui text field
   * cannot also capture game input and activate game-level actions like {@code ui_accept}.
   * Defaults to {@code false}.
   *
   * <p>The debug overlay's own toggle keys use the regular {@code justPressed} helpers, so they are
   * suppressed while a debug text field is focused and respond normally the rest of the time.
   *
   * @return {@code true} if a foreground UI element is consuming keyboard input this frame.
   */
  public boolean isKeyboardCapturedByUI() {
    return false;
  }

  private void handleInspectCameraTools() {
    if (Flixel.mouse == null) {
      return;
    }
    FlixelArray<FlixelCamera> cams = Flixel.cameras;
    if (cams == null || cams.getSize() == 0) {
      return;
    }
    if (debugInspectCameraIndex < 0 || debugInspectCameraIndex >= cams.getSize()) {
      debugInspectCameraIndex = 0;
    }
    // Alt is read straight off the input device so it still registers while the keyboard manager is
    // suppressing game input; the camera-cycle keys use the regular justPressed helper, which keeps
    // the arrow keys editing text (instead of cycling cameras) while the command line is focused.
    boolean alt = Flixel.input.isKeyPressed(FlixelKey.ALT_LEFT) || Flixel.input.isKeyPressed(FlixelKey.ALT_RIGHT);
    if (alt && Flixel.keys.justPressed(cameraCycleLeftKey)) {
      debugInspectCameraIndex = (debugInspectCameraIndex - 1 + cams.getSize()) % cams.getSize();
    }
    if (alt && Flixel.keys.justPressed(cameraCycleRightKey)) {
      debugInspectCameraIndex = (debugInspectCameraIndex + 1) % cams.getSize();
    }

    FlixelCamera cam = cams.get(debugInspectCameraIndex);
    boolean uiCapturedMouse = isMouseCapturedByUI();
    float scrollDelta = Flixel.mouse.getScrollDeltaY();
    if (!uiCapturedMouse && scrollDelta != 0f) {
      float newZoom = cam.getZoom() + scrollDelta * 0.08f;
      if (newZoom < 0.05f) {
        newZoom = 0.05f;
      }
      if (newZoom > 20f) {
        newZoom = 20f;
      }
      cam.setZoom(newZoom);
    }
    cam.applyCameraTransform();

    if (!uiCapturedMouse && Flixel.mouse.pressed(cameraPanButton)) {
      int sx = Flixel.mouse.getScreenX();
      int sy = Flixel.mouse.getScreenY();
      if (!Flixel.mouse.justPressed(cameraPanButton)) {
        panUnprojectA.set(lastPanScreenX, lastPanScreenY);
        cam.unproject(panUnprojectA);
        panUnprojectB.set(sx, sy);
        cam.unproject(panUnprojectB);
        cam.scrollX -= panUnprojectB.x - panUnprojectA.x;
        cam.scrollY -= panUnprojectB.y - panUnprojectA.y;
      }
      lastPanScreenX = sx;
      lastPanScreenY = sy;
    }
  }

  /**
   * Handles the LMB picker while the game is paused: clicks select a {@link FlixelObject} for the
   * texture inspector, drags move the selected object around in world space. Camera panning is on
   * the right mouse button (see {@link #cameraPanButton}) so the two interactions
   * never fight over the same gesture.
   *
   * <p>Driven from {@link #update(float)} only when the game is paused. Skipped when the cursor is
   * over a UI window (to avoid accidentally moving sprites while clicking inside Dear ImGui).
   */
  private void handleSpritePicker() {
    if (Flixel.mouse == null || Flixel.debug == null) {
      return;
    }
    if (isMouseCapturedByUI()) {
      // While the UI is grabbing the mouse, drop any in-flight drag so we don't keep pulling the
      // sprite when the user actually wants to interact with a window.
      if (Flixel.debug.getDraggedSprite() != null) {
        Flixel.debug.setDraggedSprite(null);
      }
      return;
    }

    FlixelArray<FlixelCamera> cams = Flixel.cameras;
    if (cams == null || cams.getSize() == 0) {
      return;
    }
    int idx = getInspectCameraIndex();
    if (idx < 0) {
      return;
    }
    FlixelCamera cam = cams.get(idx);
    // Make sure the camera matrix reflects the latest scroll / zoom values before we
    // unproject. handleInspectCameraTools also calls this (it runs first when both are active),
    // but calling it here too is cheap and guarantees correctness if the call order ever shifts.
    cam.applyCameraTransform();

    // The viewport's unproject returns coordinates in VIEW space (the same space the batch draws into).
    pickUnproject.set(Flixel.mouse.getScreenX(), Flixel.mouse.getScreenY());
    cam.unproject(pickUnproject);

    float viewPickX = pickUnproject.x;
    float viewPickY = pickUnproject.y;
    float worldX = viewPickX + cam.scrollX + cam.getViewMarginX();
    float worldY = viewPickY + cam.scrollY + cam.getViewMarginY();

    // The early-exit gate above already returned when the cursor is over a debug panel, so here the
    // regular mouse helpers report the real state and drive picking over the uncovered viewport.
    boolean justPressed = Flixel.mouse.justPressed(FlixelMouseButton.LEFT);
    boolean pressed = Flixel.mouse.pressed(FlixelMouseButton.LEFT);
    boolean justReleased = Flixel.mouse.justReleased(FlixelMouseButton.LEFT);

    FlixelObject dragged = Flixel.debug.getDraggedSprite();

    if (justPressed) {
      FlixelObject hit = pickTopMostObject(cam, viewPickX, viewPickY);
      if (hit != null) {
        Flixel.debug.setDraggedSprite(hit);
        Flixel.debug.setInspectedSprite(hit);
        dragOffsetX = worldX - hit.getX();
        dragOffsetY = worldY - hit.getY();
      } else {
        Flixel.debug.setDraggedSprite(null);
      }
      return;
    }

    if (pressed && dragged != null && dragged.exists) {
      dragged.setX(worldX - dragOffsetX);
      dragged.setY(worldY - dragOffsetY);
      return;
    }

    if (justReleased && dragged != null) {
      Flixel.debug.setDraggedSprite(null);
    }
  }

  /**
   * Returns the topmost {@link FlixelObject} whose axis-aligned bounds contain the supplied
   * view-space point (same space as {@link FlixelCamera#worldToViewX(float, float)} /
   * {@link FlixelCamera#worldToViewY(float, float)} for each object), or {@code null} if nothing
   * was hit. Topmost is defined as "rendered last": the recursive walk mirrors the draw order,
   * so the first matching member encountered from back to front wins.
   *
   * <p>The search covers the active state and all of its open substates. Substates are
   * searched first because they render on top of their parent. A parent state's members are
   * included only when the parent has no substate, or when {@link FlixelState#persistentDraw}
   * is {@code true}, mirroring the actual draw chain.
   *
   * <p>Only objects assigned to {@code cam} are eligible. An object with a {@code null} or
   * empty camera list is treated as assigned to all cameras (the default).
   *
   * <p>Hidden ({@code visible == false}) and dead ({@code exists == false}) members are skipped so
   * the picker never grabs invisible UI elements or pooled corpses.
   *
   * @param cam Camera used to convert each candidate's world box into view space, and to filter
   *     objects that are not assigned to it.
   * @param viewX View-space X from {@code viewport.unproject} (before adding scroll / margin).
   * @param viewY View-space Y from {@code viewport.unproject}.
   * @return The topmost hit, or {@code null}.
   */
  @Nullable
  private FlixelObject pickTopMostObject(@NotNull FlixelCamera cam, float viewX, float viewY) {
    FlixelState state = Flixel.state;
    if (state == null) {
      return null;
    }
    return pickFromStateChain(state, cam, viewX, viewY);
  }

  /**
   * Walks the state chain starting at {@code state}, searching substates first (deepest renders on
   * top) and falling back to the parent only when it has no substate or when
   * {@link FlixelState#persistentDraw} is {@code true}.
   */
  @Nullable
  private FlixelObject pickFromStateChain(@NotNull FlixelState state, @NotNull FlixelCamera cam,
      float viewX, float viewY) {
    FlixelState sub = state.getSubState();
    if (sub != null) {
      FlixelObject hit = pickFromStateChain(sub, cam, viewX, viewY);
      if (hit != null) {
        return hit;
      }
    }
    if (sub == null || state.persistentDraw) {
      return pickRecursive(state.getMembers(), cam, viewX, viewY);
    }
    return null;
  }

  @Nullable
  private FlixelObject pickRecursive(@Nullable FlixelArray<?> members, @NotNull FlixelCamera cam,
      float viewX, float viewY) {
    if (members == null || members.getSize() == 0) {
      return null;
    }
    Object[] items = members.begin();
    FlixelObject hit = null;
    try {
      for (int i = members.getSize() - 1; i >= 0; i--) {
        Object o = items[i];
        if (!(o instanceof FlixelBasic basic) || !basic.exists || !basic.visible) {
          continue;
        }
        if (basic instanceof FlixelGroupable<?> group) {
          // Recurse into containers and ONLY return leaf hits. The group's own FlixelObject
          // bounds (for example FlixelSpriteGroup) span all members; testing it would steal
          // clicks from individual children.
          FlixelArray<?> nested = group.getMembers();
          if (nested != null) {
            FlixelObject nestedHit = pickRecursive(nested, cam, viewX, viewY);
            if (nestedHit != null) {
              hit = nestedHit;
              break;
            }
          }
          continue;
        }
        if (basic instanceof FlixelObject obj && isAssignedToCamera(basic, cam)
            && overlapsObjectInView(cam, obj, viewX, viewY)) {
          hit = obj;
          break;
        }
      }
    } finally {
      members.end();
    }
    return hit;
  }

  /**
   * Returns {@code true} if {@code basic} should be rendered by {@code cam}. An object with a
   * {@code null} or empty camera list renders to all cameras (the default).
   */
  private static boolean isAssignedToCamera(@NotNull FlixelBasic basic, @NotNull FlixelCamera cam) {
    FlixelCamera[] list = basic.cameras;
    if (list == null || list.length == 0) {
      return true;
    }
    for (FlixelCamera c : list) {
      if (c == cam) {
        return true;
      }
    }
    return false;
  }

  /**
   * View-space hit test aligned with {@link FlixelDrawable#draw(FlixelBatch) FlixelSprite.draw(Batch)}:
   * uses world position plus each object's scroll factors so parallax sprites and grouped layers match
   * what the player sees.
   */
  private static boolean overlapsObjectInView(@NotNull FlixelCamera cam, @NotNull FlixelObject obj,
      float viewX, float viewY) {
    float vx = cam.worldToViewX(obj.getX(), obj.getScrollX());
    float vy = cam.worldToViewY(obj.getY(), obj.getScrollY());
    float w = obj.getWidth();
    float h = obj.getHeight();
    return viewX >= vx && viewX <= vx + w && viewY >= vy && viewY <= vy + h;
  }

  /**
   * Draws bounding boxes for all visible {@link FlixelDebugDrawable} instances using each camera's
   * projection. Each object provides its own debug color via
   * {@link FlixelDebugDrawable#getDebugBoundingBoxColor()}. Disabled when {@link #isDrawDebug()} is false.
   *
   * @param cameras The game camera array.
   */
  public void drawBoundingBoxes(FlixelCamera[] cameras) {
    if (!drawDebug) {
      return;
    }

    FlixelBatch batch = Flixel.graphics.getBatch();
    FlixelFrame whitePixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);

    for (FlixelCamera cam : cameras) {
      if (cam == null) {
        continue;
      }
      Flixel.graphics.beginCameraPass();
      cam.applyViewport();
      batch.setProjection(cam.getCombinedMatrix());
      batch.begin();
      // Scale the outline thickness by the inverse zoom so it stays a constant width on screen and
      // does not thin out to nothing when the camera is zoomed out.
      final float thickness = Math.max(1f, 1f / Math.max(0.0001f, cam.getZoom()));
      FlixelDebugUtil.forEachDebugDrawable(drawable -> {
        if (drawable == null) {
          return;
        }
        if (drawable instanceof FlixelBasic basic) {
          // Skip if the object is not projected to the current camera. A null/empty list means
          // the object is projected to all cameras.
          boolean found = false;
          FlixelCamera[] list = basic.cameras;
          if (list == null || list.length == 0) {
            found = true;
          } else {
            for (FlixelCamera c : list) {
              if (c == cam) {
                found = true;
                break;
              }
            }
          }
          if (!found) {
            return;
          }
        }
        float[] c = drawable.getDebugBoundingBoxColor();
        if (c == null || c.length < 4) {
          c = FALLBACK_BOUNDING_BOX_COLOR;
        }
        boundingBoxColor.set(c[0], c[1], c[2], c[3]);
        FlixelSpriteUtil.drawBorder(batch, whitePixel, drawable.getDebugDrawX(cam), drawable.getDebugDrawY(cam),
            drawable.getDebugWidth(), drawable.getDebugHeight(), thickness, boundingBoxColor);
      });
      batch.end();
    }
  }

  /**
   * Draws the overlay UI panels. The base implementation does nothing because the core module
   * intentionally avoids depending on a heavy GUI toolkit (Dear ImGui, scene2d.ui, etc.). Backends
   * override this to render the panels with whatever toolkit suits the platform.
   *
   * <p>Called from {@link FlixelDrawable#draw(FlixelBatch) FlixelGame.draw(Batch)}
   * after the game stage and bounding boxes have been drawn.
   */
  public void draw() {
    if (!visible) {
      return;
    }
    snapshotRenderCalls();
    // The debug UI draws in screen space, so give it its own render pass rather than inheriting the
    // last camera's projection.
    Flixel.graphics.beginCameraPass();
    drawUI();
  }

  /**
   * Sums {@code renderCalls} from the framework's own batch and all user-registered batches.
   * Called from {@link #draw()} after the game's batch has been ended for the frame, so the
   * count reflects the full just-completed frame rather than a partial in-progress one.
   */
  private void snapshotRenderCalls() {
    int total = 0;
    if (Flixel.game != null) {
      total += Flixel.game.getFrameRenderCalls();
    }
    FlixelDebugManager mgr = Flixel.debug;
    if (mgr != null) {
      FlixelArray<FlixelBatch> extra = mgr.getTrackedBatches();
      for (int i = 0, n = extra.getSize(); i < n; i++) {
        FlixelBatch b = extra.get(i);
        if (b != null) {
          total += b.getRenderCalls();
        }
      }
    }
    cachedRenderCalls = total;
  }

  /** Hook invoked from {@link #draw()} when the overlay is visible. Each platform backend implements this. */
  protected abstract void drawUI();

  /**
   * Called from {@link org.flixelgdx.FlixelGame#resize(int, int)} so backends can keep
   * their renderer state in sync with the window. The base class does not need to do anything.
   *
   * @param width New window width in pixels.
   * @param height New window height in pixels.
   */
  public void resize(int width, int height) {}

  private void reclaimTrackerBlocksToPool() {
    for (int i = 0; i < cachedTrackerBlocks.getSize(); i++) {
      cachedTrackerBlockPool.add(cachedTrackerBlocks.get(i));
    }
    cachedTrackerBlocks.clear();
  }

  private CachedTrackerBlock obtainTrackerBlock() {
    return cachedTrackerBlockPool.getSize() > 0
        ? cachedTrackerBlockPool.pop()
        : new CachedTrackerBlock();
  }

  private void rebuildCachedTrackerBlocks() {
    reclaimTrackerBlocksToPool();
    if (Flixel.debug == null) {
      return;
    }
    FlixelArray<FlixelDebugTrackerEntry> entries = Flixel.debug.getTrackerEntries();
    if (entries == null || entries.getSize() == 0) {
      return;
    }
    for (int e = 0; e < entries.getSize(); e++) {
      FlixelDebugTrackerEntry entry = entries.get(e);
      if (entry == null) {
        continue;
      }
      FlixelMap<String, String> values = entry.getTrackedValues();
      if (values == null || values.getSize() == 0) {
        continue;
      }
      CachedTrackerBlock block = obtainTrackerBlock();
      block.name.clear();
      block.name.concat(entry.getName());
      int n = values.getSize();
      block.ensurePairCount(n);
      // FlixelMap reuses its entries iterator, so this loop stays allocation-free.
      int i = 0;
      for (FlixelMap.Entry<String, String> pair : values.entries()) {
        block.keys[i].clear();
        block.keys[i].concat(pair.key != null ? pair.key : "");
        block.values[i].clear();
        block.values[i].concat(pair.value != null ? pair.value : "");
        i++;
      }
      block.pairCount = i;
      cachedTrackerBlocks.add(block);
    }
    onTrackerBlocksRebuilt();
  }

  private void onLogEntry(FlixelLogEntry entry) {
    if (entry == null) {
      return;
    }
    synchronized (logBuffer) {
      while (logBuffer.size() >= FlixelLogger.MAX_LOG_ENTRIES) {
        BufferedLogLine old = logBuffer.removeFirst();
        logLinePool.add(old);
      }
      BufferedLogLine line = logLinePool.getSize() > 0 ? logLinePool.pop() : new BufferedLogLine();
      line.set(entry);
      logBuffer.addLast(line);
      onLogEntryAppended(line);
    }
  }

  /**
   * Copies the current log buffer into {@code output}, oldest first, reusing existing
   * {@link BufferedLogLine} slots in {@code output} so the renderer does not have to allocate.
   *
   * @param output Destination array. Cleared (resized) to match the current buffer size.
   * @return The number of log lines written.
   */
  protected final int copyLogBuffer(@NotNull FlixelArray<BufferedLogLine> output) {
    synchronized (logBuffer) {
      int n = logBuffer.size();
      while (output.getSize() < n) {
        output.add(new BufferedLogLine());
      }
      output.setSize(n);
      int i = 0;
      for (BufferedLogLine src : logBuffer) {
        BufferedLogLine dst = output.get(i++);
        dst.copyFrom(src);
      }
      return n;
    }
  }

  @Override
  public void destroy() {
    if (destroyed) {
      return;
    }
    destroyed = true;
  }

  /** Default key codes for the debug overlay. */
  public static final class Keybinds {

    public static final int DEFAULT_TOGGLE_KEY = FlixelKey.F2;
    public static final int DEFAULT_DRAW_DEBUG_KEY = FlixelKey.F3;
    public static final int DEFAULT_PAUSE_KEY = FlixelKey.F4;
    public static final int DEFAULT_DEBUG_CAMERA_CYCLE_LEFT = FlixelKey.LEFT;
    public static final int DEFAULT_DEBUG_CAMERA_CYCLE_RIGHT = FlixelKey.RIGHT;

    private Keybinds() {}
  }

  /**
   * Buffered, markup-free copy of a {@link FlixelLogEntry}. Stored in {@link #logBuffer} and pooled to
   * avoid per-log allocations when the overlay's renderer reads log lines.
   *
   * <p>{@link #tagStr} and {@link #messageStr} are snapshots taken from the {@link #tag} and {@link #message}
   * buffers after each {@link #set(FlixelLogEntry)} so renderers that require {@link String} (such as Dear ImGui)
   * do not call {@link FlixelString#toString()} every frame. They match the copied buffer text, not a live
   * view of any caller-owned {@link CharSequence}.
   */
  public static final class BufferedLogLine {

    public FlixelLogLevel level = FlixelLogLevel.INFO;
    public final FlixelString tag = new FlixelString(32);
    public final FlixelString message = new FlixelString(192);

    /** Stable {@link String} reference for the tag, refreshed whenever {@link #set(FlixelLogEntry)} runs. */
    public String tagStr = "";

    /** Stable {@link String} reference for the message, refreshed whenever {@link #set(FlixelLogEntry)} runs. */
    public String messageStr = "";

    void set(FlixelLogEntry entry) {
      level = entry.level();
      String t = entry.tag() != null ? entry.tag() : "";
      tag.clear();
      tag.concat(t);
      tagStr = tag.toString();
      String m = entry.message() != null ? entry.message() : "";
      message.clear();
      message.concat(m);
      messageStr = message.toString();
    }

    void copyFrom(BufferedLogLine other) {
      level = other.level;
      tag.set(other.tag);
      message.set(other.message);
      tagStr = other.tagStr;
      messageStr = other.messageStr;
    }
  }

  /** Cached block for a {@link FlixelDebugTrackerEntry} (rebuilt every {@value #WATCH_REFRESH_INTERVAL}s). */
  public static final class CachedTrackerBlock {

    public final FlixelString name = new FlixelString(64);
    public FlixelString[] keys = new FlixelString[0];
    public FlixelString[] values = new FlixelString[0];
    public int pairCount;

    void ensurePairCount(int n) {
      if (keys.length < n) {
        FlixelString[] nk = new FlixelString[n];
        FlixelString[] nv = new FlixelString[n];
        System.arraycopy(keys, 0, nk, 0, keys.length);
        System.arraycopy(values, 0, nv, 0, values.length);
        for (int i = keys.length; i < n; i++) {
          nk[i] = new FlixelString(48);
          nv[i] = new FlixelString(64);
        }
        keys = nk;
        values = nv;
      }
    }
  }
}
