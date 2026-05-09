/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.functional.FlixelDestroyable;
import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.FlixelState;
import me.stringdotjar.flixelgdx.functional.FlixelUpdatable;
import me.stringdotjar.flixelgdx.group.FlixelGroupable;
import me.stringdotjar.flixelgdx.input.keyboard.FlixelKey;
import me.stringdotjar.flixelgdx.logging.FlixelDebugConsoleEntry;
import me.stringdotjar.flixelgdx.logging.FlixelLogEntry;
import me.stringdotjar.flixelgdx.logging.FlixelLogLevel;
import me.stringdotjar.flixelgdx.logging.FlixelLogger;
import me.stringdotjar.flixelgdx.util.FlixelDebugUtil;
import me.stringdotjar.flixelgdx.util.FlixelString;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic <em>controller</em> for the FlixelGDX in-game debugger. This abstract class is the
 * baseline behavior that every FlixelGDX backend can rely on: it owns the visibility flags,
 * keybind handling, hitbox drawing, the pause/camera tools, and the pooled buffers that hold
 * watch values, log entries, and custom console blocks.
 *
 * <p>The class deliberately does <strong>not</strong> render any UI on its own. Doing so in the
 * core module would either pull a heavy GUI dependency into every platform (web/iOS/Android).
 * Instead, backends extend this abstract class and implement {@link #drawUI()} to render the
 * panels with whatever toolkit suits the platform.
 *
 * <p>The default install path is:
 * <ol>
 *   <li>{@link Flixel#setDebugMode(boolean)} flips debug mode on in your launcher.</li>
 *   <li>The launcher (or your code) calls {@link Flixel#setDebugOverlay(java.util.function.Supplier)}
 *       with the backend-specific factory.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.FlixelGame} constructs the overlay during {@code create}
 *       and registers it with the logger.</li>
 * </ol>
 *
 * <p>Toggle overlay visibility with {@link Flixel#getDebugToggleKey()} (default
 * {@link Keybinds#DEFAULT_TOGGLE_KEY}). Toggle visual debug (hitboxes) with
 * {@link Flixel#getDebugDrawToggleKey()} (default {@link Keybinds#DEFAULT_DRAW_DEBUG_KEY}).
 * In debug mode, {@link Flixel#getDebugPauseKey()} (default F4) pauses the game; while paused you
 * can inspect the camera with Alt+arrows, RMB pan, and the mouse wheel zoom.
 *
 * <h2>Reduced allocation rate</h2>
 *
 * <p>Strings are the easiest way to allocate yourself into stutters. To avoid that, this class:
 * <ul>
 *   <li>Caches FPS, heap, native and object counters as primitives and only refreshes them every
 *       {@value #STATS_UPDATE_INTERVAL} seconds.</li>
 *   <li>Keeps log entries as {@link BufferedLogLine} records pooled across frames; the renderer
 *       receives the level, tag, and message via {@link #copyLogBuffer(Array)} without producing
 *       any markup string.</li>
 *   <li>Refreshes watch entries at 10Hz into reusable {@link FlixelString} buffers
 *       ({@link FlixelDebugWatchManager#fillWatchEntries(Array, Array)}).</li>
 *   <li>Reuses {@link CachedConsoleBlock} instances across rebuilds.</li>
 * </ul>
 */
public abstract class FlixelDebugOverlay implements FlixelUpdatable, FlixelDestroyable, Disposable {

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

  private final ShapeRenderer shapeRenderer;

  /** Visibility flag for the renderer. Hitbox drawing is gated by {@link #drawDebug} instead. */
  private boolean visible = false;

  private boolean drawDebug = false;

  /** Prevents double-dispose if {@link #dispose()} and {@link #destroy()} are both used. */
  private boolean destroyed = false;

  protected float statsTimer = 0f;
  protected int cachedFps;
  protected float cachedHeapMegabytes;
  protected float cachedNativeMegabytes;
  protected int cachedObjectCount;

  private float perfSampleTimer = 0f;

  /** Frame-time samples in milliseconds (full real-time wall delta). */
  protected final float[] perfFrameMs = new float[PERF_HISTORY_SIZE];

  /** Java heap usage in megabytes per sample. */
  protected final float[] perfHeapMb = new float[PERF_HISTORY_SIZE];

  /** Native (GL/audio) heap usage in megabytes per sample. */
  protected final float[] perfNativeMb = new float[PERF_HISTORY_SIZE];

  /** FPS as reported by libGDX per sample. */
  protected final float[] perfFps = new float[PERF_HISTORY_SIZE];

  /** Index of the next sample to write (rolls over once the ring is full). */
  protected int perfHead = 0;

  /** Number of valid samples in each perf ring (caps at {@link #PERF_HISTORY_SIZE}). */
  protected int perfCount = 0;

  protected float watchRefreshTimer = 0f;

  /** Cached watch keys refreshed at {@value #WATCH_REFRESH_INTERVAL}s; buffers are reused across refreshes. */
  protected final Array<FlixelString> cachedWatchKeys = new Array<>();

  /** Cached watch values refreshed at {@value #WATCH_REFRESH_INTERVAL}s; buffers are reused across refreshes. */
  protected final Array<FlixelString> cachedWatchValues = new Array<>();

  protected final Array<CachedConsoleBlock> cachedConsoleBlocks = new Array<>();

  /** Pool of console blocks between rebuilds to avoid reallocating block objects. */
  private final Array<CachedConsoleBlock> cachedConsoleBlockPool = new Array<>();

  /** Latest log lines, oldest first; bounded by {@link FlixelLogger#MAX_LOG_ENTRIES}. */
  protected final Deque<BufferedLogLine> logBuffer = new ArrayDeque<>();

  /** Pool of {@link BufferedLogLine} instances reused as the buffer rolls over. */
  private final Array<BufferedLogLine> logLinePool = new Array<>();

  private final Consumer<FlixelLogEntry> logListener = this::onLogEntry;

  protected int debugInspectCameraIndex;

  /** Screen-space anchor for Alt+RMB pan (avoids mixing world unprojects across changing scroll). */
  private int lastPanScreenX;
  private int lastPanScreenY;

  private final Vector2 panUnprojectA = new Vector2();
  private final Vector2 panUnprojectB = new Vector2();

  /** Cached unproject scratch used while picking/dragging objects (kept off the per-frame allocation path). */
  private final Vector2 pickUnproject = new Vector2();

  /** World-space offset between cursor and dragged sprite's origin so it does not snap on grab. */
  private float dragOffsetX;
  private float dragOffsetY;

  /** Constructs the shared debug overlay state. Subclasses should call this before wiring platform UI. */
  protected FlixelDebugOverlay() {
    shapeRenderer = new ShapeRenderer();
  }

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
    Array<FlixelCamera> cams = Flixel.getCameras();
    int n = (cams != null) ? cams.size : 0;
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
      // Raw* so the game loop pause toggle keeps working even while an imgui text field is focused,
      // unless a backend suppresses typable keys while a command field is active (see LWJGL ImGui overlay).
      int pauseKey = Flixel.getDebugPauseKey();
      if (Flixel.keys.rawJustPressed(pauseKey) && !shouldSuppressDebugRawKeybind(pauseKey)) {
        Flixel.setPaused(!Flixel.isPaused());
      }
      if (Flixel.isPaused()) {
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
    // the overlay is visible, so opening the Performance window mid-session still shows a useful
    // history instead of an empty graph for the first several seconds.
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
      cachedFps = Flixel.getFPS();
      cachedHeapMegabytes = Flixel.getJavaHeapUsedMegabytes();
      cachedNativeMegabytes = Flixel.getNativeHeapUsedMegabytes();
      cachedObjectCount = FlixelDebugUtil.countActiveMembers();
    }

    if (watchRefreshTimer >= WATCH_REFRESH_INTERVAL) {
      watchRefreshTimer = 0f;
      refreshWatchEntries();
      rebuildCachedConsoleBlocks();
    }

    onUpdateUI(elapsed);
  }

  /**
   * Appends one sample to each performance ring buffer. The buffers are primitive {@code float[]}
   * arrays sized at {@link #PERF_HISTORY_SIZE}, so this method is allocation-free.
   *
   * @param elapsed Real wall-clock seconds since the last frame (used for the frame-time series).
   */
  protected void pushPerfSample(float elapsed) {
    int idx = perfHead;
    perfFrameMs[idx] = elapsed * 1000f;
    perfHeapMb[idx] = Flixel.getJavaHeapUsedMegabytes();
    perfNativeMb[idx] = Flixel.getNativeHeapUsedMegabytes();
    perfFps[idx] = Flixel.getFPS();
    perfHead = (idx + 1) % PERF_HISTORY_SIZE;
    if (perfCount < PERF_HISTORY_SIZE) {
      perfCount++;
    }
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

  /** Hook for subclass UI state updates that should happen every frame while the overlay is visible. */
  protected void onUpdateUI(float elapsed) {}

  private void handleToggleKeys() {
    // Use the raw* variants so the toggle keys still work even while a Dear ImGui text field
    // (for example, the debug command line) has keyboard focus and the regular justPressed
    // helpers are intentionally suppressed.
    int toggleKey = Flixel.getDebugToggleKey();
    if (Flixel.keys.rawJustPressed(toggleKey) && !shouldSuppressDebugRawKeybind(toggleKey)) {
      toggleVisible();
    }
    int drawKey = Flixel.getDebugDrawToggleKey();
    if (Flixel.keys.rawJustPressed(drawKey) && !shouldSuppressDebugRawKeybind(drawKey)) {
      toggleDrawDebug();
    }
  }

  /**
   * Backends that render a command-line {@code InputText} can override this to skip debug hotkeys for keys that would
   * normally type into that field (letters, punctuation, arrows, Enter, and so on). Return {@code false} by default so
   * {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager#rawJustPressed(int)} shortcuts keep working.
   *
   * @param keycode FlixelGDX {@link FlixelKey} or libGDX {@link Input.Keys} key code being handled by a debug binding.
   * @return {@code true} to skip handling this key for debug shortcuts this frame.
   */
  protected boolean shouldSuppressDebugRawKeybind(int keycode) {
    return false;
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
   * Hook fired after {@link #cachedConsoleBlocks} has been rebuilt. Override in renderer subclasses
   * that need to keep a parallel cache (for example a {@code String[]} for Dear ImGui).
   */
  protected void onConsoleBlocksRebuilt() {}

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
   * {@link me.stringdotjar.flixelgdx.input.mouse.FlixelMouseManager#pressed(int)} and
   * the matching {@code justPressed} / {@code justReleased} helpers will report {@code false}
   * for the game's regular input checks, and the debug camera tools / sprite picker also skip
   * their work, so clicking inside (for example) a Dear ImGui window does not bleed through
   * into the game logic. Defaults to {@code false}.
   *
   * <p>The debug overlay's own mouse-driven tools (sprite picker, camera pan) read
   * {@link me.stringdotjar.flixelgdx.input.mouse.FlixelMouseManager#rawPressed(int)} so they
   * can opt in to "ignore the suppression" while still respecting this hook for the early-exit
   * gate.
   *
   * @return {@code true} if a foreground UI element is consuming mouse input this frame.
   */
  public boolean isMouseCapturedByUI() {
    return false;
  }

  /**
   * Override to tell the framework's input layer that another UI layer is currently consuming
   * keyboard input. When this returns {@code true},
   * {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager#pressed(int)} and
   * the matching {@code justPressed} / {@code justReleased} helpers will report {@code false}
   * for the game's regular input checks, so typing in (for example) a Dear ImGui text field
   * cannot also capture game input and activate game-level actions like {@code ui_accept}.
   * Defaults to {@code false}.
   *
   * <p>The debug overlay's own toggle keys (debug overlay toggle, hitbox toggle, pause) read
   * {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager#rawJustPressed(int)}
   * so they keep working even when this returns {@code true}.
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
    Array<FlixelCamera> cams = Flixel.getCameras();
    if (cams == null || cams.size == 0) {
      return;
    }
    if (debugInspectCameraIndex < 0 || debugInspectCameraIndex >= cams.size) {
      debugInspectCameraIndex = 0;
    }
    // Use the raw* helpers throughout so the inspect camera tools keep responding while the
    // imgui debugger is focused (otherwise our own debug controls would be filtered out by the
    // input suppression we set up to protect the game's regular input).
    boolean alt = Flixel.keys.rawPressed(FlixelKey.ALT_LEFT) || Flixel.keys.rawPressed(FlixelKey.ALT_RIGHT)
      || Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
    int cycleLeft = Flixel.getDebugCameraCycleLeftKey();
    int cycleRight = Flixel.getDebugCameraCycleRightKey();
    if (alt && Flixel.keys.rawJustPressed(cycleLeft) && !shouldSuppressDebugRawKeybind(cycleLeft)) {
      debugInspectCameraIndex = (debugInspectCameraIndex - 1 + cams.size) % cams.size;
    }
    if (alt && Flixel.keys.rawJustPressed(cycleRight) && !shouldSuppressDebugRawKeybind(cycleRight)) {
      debugInspectCameraIndex = (debugInspectCameraIndex + 1) % cams.size;
    }
    FlixelCamera cam = cams.get(debugInspectCameraIndex);
    boolean uiCapturedMouse = isMouseCapturedByUI();
    float scrollDelta = Flixel.mouse.getScrollDeltaY();
    if (!uiCapturedMouse && scrollDelta != 0f) {
      float newZoom = cam.getZoom() + scrollDelta * -0.08f;
      if (newZoom < 0.05f) {
        newZoom = 0.05f;
      }
      if (newZoom > 20f) {
        newZoom = 20f;
      }
      cam.setZoom(newZoom);
    }
    cam.applyLibCameraTransform();
    if (!uiCapturedMouse && Flixel.mouse.rawPressed(Flixel.getDebugCameraPanButton())) {
      int sx = Flixel.mouse.getScreenX();
      int sy = Flixel.mouse.getScreenY();
      if (Flixel.mouse.rawJustPressed(Flixel.getDebugCameraPanButton())) {
        lastPanScreenX = sx;
        lastPanScreenY = sy;
      } else {
        panUnprojectA.set(lastPanScreenX, lastPanScreenY);
        cam.getViewport().unproject(panUnprojectA);
        panUnprojectB.set(sx, sy);
        cam.getViewport().unproject(panUnprojectB);
        cam.scroll.x -= panUnprojectB.x - panUnprojectA.x;
        cam.scroll.y -= panUnprojectB.y - panUnprojectA.y;
        lastPanScreenX = sx;
        lastPanScreenY = sy;
      }
    }
  }

  /**
   * Handles the LMB picker while the game is paused: clicks select a {@link FlixelObject} for the
   * texture inspector, drags move the selected object around in world space. Camera panning is on
   * the right mouse button (see {@link Flixel#getDebugCameraPanButton()}) so the two interactions
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

    Array<FlixelCamera> cams = Flixel.getCameras();
    if (cams == null || cams.size == 0) {
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
    cam.applyLibCameraTransform();

    // The viewport's unproject returns coordinates in *view* space (the same space the batch
    // draws into). Sprite hitboxes live in world space (their x and y fields), so we need to
    // add the camera's scroll back in plus the view margin (the offset induced by zoom that
    // FlixelCamera.worldToViewX() subtracts during draw). Without this conversion the picker would
    // feel off when the camera is scrolled or zoomed: clicks would land on the wrong sprite or miss entirely.
    pickUnproject.set(Flixel.mouse.getScreenX(), Flixel.mouse.getScreenY());
    cam.getViewport().unproject(pickUnproject);
    // View-space coordinates match FlixelSprite.draw() (worldToViewX / worldToViewY). Hit-testing
    // in view space fixes mis-picks when members use scroll factors (common in layered stages
    // and sprite groups where siblings overlap in world AABB but render at different parallax).
    float viewPickX = pickUnproject.x;
    float viewPickY = pickUnproject.y;
    float worldX = viewPickX + cam.scroll.x + cam.getViewMarginX();
    float worldY = viewPickY + cam.scroll.y + cam.getViewMarginY();

    // Use the raw* helpers so the picker keeps reading the actual mouse state (Flixel.mouse.pressed
    // is suppressed when the cursor is over an imgui window, and we still want the uncovered
    // viewport area to drive picking). The early-exit gate above already guards the imgui case.
    boolean justPressed = Flixel.mouse.rawJustPressed(Input.Buttons.LEFT);
    boolean pressed = Flixel.mouse.rawPressed(Input.Buttons.LEFT);
    boolean justReleased = Flixel.mouse.rawJustReleased(Input.Buttons.LEFT);

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
   * <p>Hidden ({@code visible == false}) and dead ({@code exists == false}) members are skipped so
   * the picker never grabs invisible UI elements or pooled corpses.
   *
   * @param cam Camera used to convert each candidate's world box into view space.
   * @param viewX View-space X from {@code viewport.unproject} (before adding scroll / margin).
   * @param viewY View-space Y from {@code viewport.unproject}.
   * @return The topmost hit, or {@code null}.
   */
  @Nullable
  private FlixelObject pickTopMostObject(@NotNull FlixelCamera cam, float viewX, float viewY) {
    FlixelState state = Flixel.getState();
    if (state == null) {
      return null;
    }
    return pickRecursive(state.getMembers(), cam, viewX, viewY);
  }

  @Nullable
  private FlixelObject pickRecursive(@Nullable SnapshotArray<?> members, @NotNull FlixelCamera cam,
                                     float viewX, float viewY) {
    if (members == null || members.size == 0) {
      return null;
    }
    Object[] items = members.begin();
    FlixelObject hit = null;
    try {
      for (int i = members.size - 1; i >= 0; i--) {
        Object o = items[i];
        if (!(o instanceof FlixelBasic basic) || !basic.exists || !basic.visible) {
          continue;
        }
        if (basic instanceof FlixelGroupable<?> group) {
          // Recurse into containers and ONLY return leaf hits. The group's own FlixelObject
          // bounds (for example FlixelSpriteGroup) span all members; testing it would steal
          // clicks from individual children.
          SnapshotArray<?> nested = group.getMembers();
          if (nested != null) {
            FlixelObject nestedHit = pickRecursive(nested, cam, viewX, viewY);
            if (nestedHit != null) {
              hit = nestedHit;
              break;
            }
          }
          continue;
        }
        if (basic instanceof FlixelObject obj && overlapsObjectInView(cam, obj, viewX, viewY)) {
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
   * View-space hit test aligned with {@link me.stringdotjar.flixelgdx.FlixelSprite#draw(Batch)}: uses world position
   * plus each object's scroll factors so parallax sprites and grouped layers match what the player sees.
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

    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

    for (FlixelCamera cam : cameras) {
      if (cam == null) {
        continue;
      }
      cam.getViewport().apply();
      shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
      shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
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
        shapeRenderer.setColor(c[0], c[1], c[2], c[3]);
        shapeRenderer.rect(drawable.getDebugDrawX(cam), drawable.getDebugDrawY(cam),
          drawable.getDebugWidth(), drawable.getDebugHeight());
      });
      shapeRenderer.end();
    }

    Gdx.gl.glDisable(GL20.GL_BLEND);
  }

  /**
   * Draws the overlay UI panels. The base implementation does nothing because the core module
   * intentionally avoids depending on a heavy GUI toolkit (Dear ImGui, scene2d.ui, etc.). Backends
   * override this to render the panels with whatever toolkit suits the platform.
   *
   * <p>Called from {@link me.stringdotjar.flixelgdx.FlixelGame#draw(com.badlogic.gdx.graphics.g2d.Batch)}
   * after the game stage and bounding boxes have been drawn.
   */
  public void draw() {
    if (!visible) {
      return;
    }
    drawUI();
  }

  /** Hook invoked from {@link #draw()} when the overlay is visible. Each platform backend implements this. */
  protected abstract void drawUI();

  /**
   * Called from {@link me.stringdotjar.flixelgdx.FlixelGame#resize(int, int)} so backends can keep
   * their renderer state in sync with the window. The base class does not need to do anything.
   *
   * @param width New window width in pixels.
   * @param height New window height in pixels.
   */
  public void resize(int width, int height) {}

  private void reclaimConsoleBlocksToPool() {
    for (int i = 0; i < cachedConsoleBlocks.size; i++) {
      cachedConsoleBlockPool.add(cachedConsoleBlocks.get(i));
    }
    cachedConsoleBlocks.clear();
  }

  private CachedConsoleBlock obtainConsoleBlock() {
    return cachedConsoleBlockPool.size > 0
      ? cachedConsoleBlockPool.pop()
      : new CachedConsoleBlock();
  }

  private void rebuildCachedConsoleBlocks() {
    reclaimConsoleBlocksToPool();
    FlixelLogger logger = Flixel.getLogger();
    if (logger == null) {
      return;
    }
    FlixelDebugConsoleEntry[] entries = logger.getConsoleEntries();
    if (entries == null || entries.length == 0) {
      return;
    }
    for (FlixelDebugConsoleEntry entry : entries) {
      if (entry == null) {
        continue;
      }
      String[] lines = entry.getConsoleLines();
      if (lines == null || lines.length == 0) {
        continue;
      }
      CachedConsoleBlock block = obtainConsoleBlock();
      block.name.clear();
      block.name.concat(entry.getName());
      int n = lines.length;
      block.ensureBodyLineCount(n);
      for (int i = 0; i < n; i++) {
        block.bodies[i].clear();
        block.bodies[i].concat(lines[i]);
      }
      block.bodyCount = n;
      cachedConsoleBlocks.add(block);
    }
    onConsoleBlocksRebuilt();
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
      BufferedLogLine line = logLinePool.size > 0 ? logLinePool.pop() : new BufferedLogLine();
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
  protected final int copyLogBuffer(@NotNull Array<BufferedLogLine> output) {
    synchronized (logBuffer) {
      int n = logBuffer.size();
      while (output.size < n) {
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
    shapeRenderer.dispose();
  }

  @Override
  public final void dispose() {
    destroy();
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

  /** Cached block for {@link FlixelDebugConsoleEntry} output (rebuilt every {@value #WATCH_REFRESH_INTERVAL}s). */
  public static final class CachedConsoleBlock {

    public final FlixelString name = new FlixelString(64);
    public FlixelString[] bodies = new FlixelString[0];
    public int bodyCount;

    void ensureBodyLineCount(int n) {
      if (bodies.length < n) {
        FlixelString[] nb = new FlixelString[n];
        System.arraycopy(bodies, 0, nb, 0, bodies.length);
        for (int i = bodies.length; i < n; i++) {
          nb[i] = new FlixelString(96);
        }
        bodies = nb;
      }
    }
  }
}
