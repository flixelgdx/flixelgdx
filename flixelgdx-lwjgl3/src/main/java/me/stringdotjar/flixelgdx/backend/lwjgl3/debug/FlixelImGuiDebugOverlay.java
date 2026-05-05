/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import org.lwjgl.glfw.GLFW;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay;
import me.stringdotjar.flixelgdx.logging.FlixelLogLevel;

/**
 * Dear ImGui based debug overlay for the LWJGL3 backend.
 *
 * <h2>Wiring</h2>
 *
 * <p>{@code FlixelLwjgl3Launcher} registers this class as the default debug overlay
 * factory when launching in {@link me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode#DEBUG DEBUG}
 * mode. You can also register it manually with {@link Flixel#setDebugOverlay(java.util.function.Supplier)}
 * before {@link Flixel#initialize}.
 *
 * <p>Initialization (creating the imgui context, hooking GLFW, and uploading the OpenGL
 * resources) happens lazily on the first {@link #draw()} call. That keeps construction
 * cheap and only pays the cost when debug mode actually activates.
 *
 * <h2>Default layout</h2>
 *
 * <p>On first launch (and after {@code Reset Layout}) the windows are tiled into a fixed template
 * derived from the viewport work area: a left column for Stats, Performance, and Log; a right
 * column for Watch, Controls, and Texture Inspector; a Console strip in the middle gap above the
 * command line; and a full-width Command Line along the bottom edge. Resizing or toggling
 * fullscreen recomputes the same template on the next frame so panels track the window.
 * Once you move or resize a window, Dear ImGui remembers the change until the next forced layout.
 * Window positions are not persisted across runs because we explicitly disable imgui's
 * {@code imgui.ini} file (the framework does not own a writable directory on every platform).
 *
 * <h2>Input behavior</h2>
 *
 * <p>The imgui-java GLFW backend installs callbacks that <em>chain</em> with the existing
 * libGDX callbacks (using {@code installCallbacks=true}). Both libGDX and imgui see every
 * key, mouse, and scroll event, so {@link Flixel#getDebugToggleKey() F2}, {@link Flixel#getDebugDrawToggleKey() F3},
 * and {@link Flixel#getDebugPauseKey() F4} keep working even when an imgui window has focus.
 * When the cursor is hovering an imgui window we suppress the camera pan/zoom and the
 * sprite picker (via {@link #isMouseCapturedByUI()}) so scrolling inside an imgui list
 * does not also zoom the inspected camera or grab a sprite by accident.
 */
public class FlixelImGuiDebugOverlay extends FlixelDebugOverlay {

  // Component-per-channel color constants. Dear ImGui's textColored takes either an ImVec4 or four
  // floats; the four-float overload keeps us off the ImVec4 allocation path entirely.
  private static final float[] COLOR_INFO   = { 0.85f, 0.85f, 0.85f, 1f };
  private static final float[] COLOR_WARN   = { 1.00f, 0.80f, 0.20f, 1f };
  private static final float[] COLOR_ERROR  = { 1.00f, 0.30f, 0.30f, 1f };
  private static final float[] COLOR_KEY    = { 0.55f, 0.85f, 1.00f, 1f };
  private static final float[] COLOR_VALUE  = { 0.95f, 0.95f, 0.95f, 1f };
  private static final float[] COLOR_HEADER = { 0.65f, 0.85f, 1.00f, 1f };
  private static final float[] COLOR_OK     = { 0.30f, 0.95f, 0.55f, 1f };
  private static final float[] COLOR_PAUSED = { 1.00f, 0.70f, 0.20f, 1f };
  private static final float[] COLOR_HINT   = { 0.65f, 0.65f, 0.65f, 1f };

  private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
  private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

  private boolean imguiInitialized = false;
  private boolean imguiShutdown = false;

  // Snapshot of the log buffer taken once per frame to avoid holding logBuffer during draw.
  private final Array<BufferedLogLine> logSnapshot = new Array<>();

  // Watch caches mirroring cachedWatchKeys / cachedWatchValues as java String.
  private String[] watchKeyStr = new String[0];
  private String[] watchValueStr = new String[0];
  private int watchCount;

  // Console caches mirroring cachedConsoleBlocks as String arrays.
  private String[] consoleNameStr = new String[0];
  private String[][] consoleBodyStrs = new String[0][];
  private int[] consoleBodyCounts = new int[0];
  private int consoleBlockCount;

  // Window visibility flags (toggled from the debug menu).
  private final ImBoolean showStatsWindow = new ImBoolean(true);
  private final ImBoolean showPerformanceWindow = new ImBoolean(true);
  private final ImBoolean showControlsWindow = new ImBoolean(true);
  private final ImBoolean showWatchWindow = new ImBoolean(true);
  private final ImBoolean showLogWindow = new ImBoolean(true);
  private final ImBoolean showConsoleWindow = new ImBoolean(true);
  private final ImBoolean showCommandWindow = new ImBoolean(true);
  private final ImBoolean showTextureWindow = new ImBoolean(false);

  // Log level filters. Toggled in the log window's menu bar.
  private final ImBoolean logShowInfo = new ImBoolean(true);
  private final ImBoolean logShowWarn = new ImBoolean(true);
  private final ImBoolean logShowError = new ImBoolean(true);
  private final ImBoolean logAutoScroll = new ImBoolean(true);

  // When true, request the next draw to send the log scroll to the bottom regardless of logAutoScroll.
  private boolean scrollLogToBottom = false;

  /**
   * One-shot flag that swaps the layout condition to {@link ImGuiCond#Always} for the next frame
   * (used the first time the overlay is shown and after the user runs {@code Reset Layout}). Reset
   * back to false after the override frame so manual window moves stick afterwards.
   */
  private boolean forceLayoutNextFrame = true;

  // Per-window default rectangles (work-area coordinates). Filled by computeDefaultLayoutRects()
  // each frame so resize and fullscreen keep the template sane; applyWindowLayout() consumes them
  // right before each begin().
  private float layoutStatsX, layoutStatsY, layoutStatsW, layoutStatsH;
  private float layoutPerfX, layoutPerfY, layoutPerfW, layoutPerfH;
  private float layoutLogX, layoutLogY, layoutLogW, layoutLogH;
  private float layoutWatchX, layoutWatchY, layoutWatchW, layoutWatchH;
  private float layoutControlsX, layoutControlsY, layoutControlsW, layoutControlsH;
  private float layoutTextureX, layoutTextureY, layoutTextureW, layoutTextureH;
  private float layoutConsoleX, layoutConsoleY, layoutConsoleW, layoutConsoleH;
  private float layoutCommandX, layoutCommandY, layoutCommandW, layoutCommandH;

  private final ImString commandInputBuffer = new ImString(256);
  private int commandHistoryCursor = -1;
  private boolean focusCommandLine = false;

  private float textureViewerZoom = 1f;
  // Reused float buffer fed to ImGui.sliderFloat() so the zoom control stays allocation-free.
  private final float[] textureViewerZoomBuf = new float[1];

  private String keybindToggleLabel;
  private String keybindHitboxLabel;
  private String keybindPauseLabel;
  private String keybindCycleLeftLabel;
  private String keybindCycleRightLabel;
  private int cachedToggleKey = -1;
  private int cachedHitboxKey = -1;
  private int cachedPauseKey = -1;
  private int cachedCycleLeftKey = -1;
  private int cachedCycleRightKey = -1;

  @Override
  public void draw() {
    if (!isVisible() || imguiShutdown) {
      return;
    }
    if (!imguiInitialized) {
      initImGui();
      if (!imguiInitialized) {
        // Initialization could not complete (no GLFW handle yet, for example). Skip rendering
        // and try again on the next frame so we never call into Dear ImGui without setup.
        return;
      }
    }
    super.draw();
  }

  @Override
  protected void drawUI() {
    if (!imguiInitialized || imguiShutdown) {
      return;
    }
    snapshotLogBuffer();

    imGuiGl3.newFrame();
    imGuiGlfw.newFrame();
    ImGui.newFrame();

    // Passthrough dockspace covers the whole viewport with a transparent central node so the
    // game keeps rendering through the empty space between/around docked windows. Without the
    // PassthruCentralNode flag the dockspace fills the screen with the imgui background color,
    // which is the "gray screen" symptom you would otherwise see after toggling the overlay.
    ImGui.dockSpaceOverViewport(0, ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);

    // Compute the default rectangles from the viewport work area each frame. The actual position
    // hints are applied per-window inside drawXxxWindow() right before its begin() so each call
    // pairs with its own begin (otherwise back-to-back setNextWindowPos calls overwrite each
    // other and only the last one sticks).
    computeDefaultLayoutRects();

    drawMainMenuBar();
    drawStatsWindow();
    drawPerformanceWindow();
    drawControlsWindow();
    drawWatchWindow();
    drawLogWindow();
    drawConsoleWindow();
    drawCommandWindow();
    drawTextureWindow();

    // The forced-layout pass only lasts one frame; subsequent frames use FirstUseEver so the
    // user's manual window moves stick.
    if (forceLayoutNextFrame) {
      forceLayoutNextFrame = false;
    }

    ImGui.render();
    imGuiGl3.renderDrawData(ImGui.getDrawData());

    if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
      long backupContext = GLFW.glfwGetCurrentContext();
      ImGui.updatePlatformWindows();
      ImGui.renderPlatformWindowsDefault();
      GLFW.glfwMakeContextCurrent(backupContext);
    }
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    if (!imguiInitialized || imguiShutdown) {
      return;
    }
    // Dear ImGui's display size must track the framebuffer explicitly; relying only on GLFW
    // callbacks can leave panels mis-scaled after libGDX toggles fullscreen or resizes the
    // backing window.
    ImGui.getIO().setDisplaySize(width, height);
    forceLayoutNextFrame = true;
  }

  /**
   * Tears down the Dear ImGui resources in the safe order: GL3 renderer first (which detaches
   * GL state), then the GLFW backend (which removes its callback chain), and finally the imgui
   * context itself. Each call is wrapped in its own try/catch because the LWJGL window is
   * already partway through its own destruction by the time libGDX invokes
   * {@link com.badlogic.gdx.ApplicationListener#dispose()}, and a stray crash here was triggering
   * the framework's uncaught-exception handler with the audio system still alive (the symptom:
   * "OK" closes the alert but music keeps playing).
   */
  @Override
  public void destroy() {
    if (!imguiShutdown && imguiInitialized) {
      imguiShutdown = true;
      try {
        imGuiGl3.shutdown();
      } catch (Throwable t) {
        // Ignore.
      }
      try {
        imGuiGlfw.shutdown();
      } catch (Throwable t) {
        // Ignore.
      }
      try {
        ImGui.destroyContext();
      } catch (Throwable t) {
        // Ignore.
      }
    }
    super.destroy();
  }

  @Override
  public boolean isMouseCapturedByUI() {
    return imguiInitialized && ImGui.getIO().getWantCaptureMouse();
  }

  /**
   * Reports keyboard capture by routing through Dear ImGui's {@code WantCaptureKeyboard} flag.
   * Imgui sets this flag whenever any of its widgets has keyboard focus (text input, modal
   * popup, drag spinner, etc.), so returning that value here prevents a game's input
   * from capturing and processing input while the user is typing in the command console or
   * interacting with another widget.
   *
   * <p>Note that the debug overlay's own toggle keys (F2 by default, etc.) read raw input from
   * {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager#rawJustPressed(int)},
   * so they continue to work even while a text field is focused.
   */
  @Override
  public boolean isKeyboardCapturedByUI() {
    return imguiInitialized && ImGui.getIO().getWantCaptureKeyboard();
  }

  @Override
  protected void onWatchEntriesRefreshed() {
    int n = cachedWatchKeys.size;
    if (watchKeyStr.length < n) {
      watchKeyStr = new String[Math.max(n, watchKeyStr.length * 2)];
      watchValueStr = new String[watchKeyStr.length];
    }
    for (int i = 0; i < n; i++) {
      watchKeyStr[i] = cachedWatchKeys.get(i).toString();
      watchValueStr[i] = cachedWatchValues.get(i).toString();
    }
    watchCount = n;
  }

  @Override
  protected void onConsoleBlocksRebuilt() {
    int n = cachedConsoleBlocks.size;
    if (consoleNameStr.length < n) {
      String[] nb = new String[Math.max(n, consoleNameStr.length * 2)];
      String[][] nbb = new String[nb.length][];
      int[] nc = new int[nb.length];
      consoleNameStr = nb;
      consoleBodyStrs = nbb;
      consoleBodyCounts = nc;
    }
    for (int i = 0; i < n; i++) {
      CachedConsoleBlock block = cachedConsoleBlocks.get(i);
      consoleNameStr[i] = block.name.toString();
      int bodyN = block.bodyCount;
      String[] bodies = consoleBodyStrs[i];
      if (bodies == null || bodies.length < bodyN) {
        bodies = new String[Math.max(bodyN, 4)];
        consoleBodyStrs[i] = bodies;
      }
      for (int b = 0; b < bodyN; b++) {
        bodies[b] = block.bodies[b].toString();
      }
      consoleBodyCounts[i] = bodyN;
    }
    consoleBlockCount = n;
  }

  @Override
  protected void onLogEntryAppended(BufferedLogLine line) {
    if (logAutoScroll.get()) {
      scrollLogToBottom = true;
    }
  }

  private void initImGui() {
    long windowHandle = resolveGlfwWindowHandle();
    if (windowHandle == 0L) {
      // Without a GLFW window handle there is no way to set imgui up; bail and try again next
      // draw so the overlay gracefully degrades instead of hard-crashing.
      Flixel.warn("FlixelImGuiDebugOverlay", "Could not resolve a GLFW window handle; skipping imgui init.");
      return;
    }

    ImGui.createContext();
    ImGuiIO io = ImGui.getIO();
    io.setIniFilename(null);
    io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
    io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

    ImFontAtlas fonts = io.getFonts();
    fonts.addFontDefault();
    fonts.build();

    imGuiGlfw.init(windowHandle, true);
    imGuiGl3.init(resolveGlslVersion());

    imguiInitialized = true;
    forceRefreshOnNextUpdate();
  }

  private static long resolveGlfwWindowHandle() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics graphics)) {
      return 0L;
    }
    return graphics.getWindow().getWindowHandle();
  }

  /**
   * Picks a GLSL version string compatible with the OpenGL context libGDX created. macOS only
   * exposes core profiles (>= 3.2), so we always feed it {@code "#version 150"}; on Linux/Windows
   * the same string works against the default libGDX 3.2 context as well.
   */
  private static String resolveGlslVersion() {
    return "#version 150";
  }

  /**
   * Fills the per-window layout rectangles from the main viewport work area (already excludes
   * the menu bar). The goal is a non-overlapping template that matches the stock FlixelGDX
   * debugger: one narrow column on the left (Stats, Performance, Log), one on the right
   * (Watch, Controls, Texture Inspector), a console strip above the command line in the gap
   * between the columns, and a full-width command bar at the bottom.
   */
  private void computeDefaultLayoutRects() {
    ImGuiViewport viewport = ImGui.getMainViewport();
    float ox = viewport.getWorkPosX();
    float oy = viewport.getWorkPosY();
    float workW = viewport.getWorkSizeX();
    float workH = viewport.getWorkSizeY();

    final float gap = 8f;
    float cmdH = 76f;
    float consH = 124f;

    // Side columns stay within a stable pixel band so ultra-wide monitors do not stretch panels.
    float colW = Math.min(Math.max(workW * 0.175f, 220f), 338f);
    // Keep both columns plus the middle console strip inside the work area on small windows.
    colW = Math.min(colW, Math.max(200f, (workW - 3f * gap) * 0.48f));

    float yCmd = oy + workH - cmdH;
    float yCons = yCmd - consH - gap;
    float availH = yCons - oy - gap;
    // If the work area is very short (small window or low resolution), shrink the bottom chrome
    // first so the column stacks still have room without overlapping the console strip.
    while (availH < 200f && (consH > 64f || cmdH > 52f)) {
      if (consH > 64f) {
        consH -= 10f;
      } else {
        cmdH -= 6f;
      }
      yCmd = oy + workH - cmdH;
      yCons = yCmd - consH - gap;
      availH = yCons - oy - gap;
    }
    availH = Math.max(96f, availH);

    // Left stack: Stats, Performance, Log (same X, stacked top to bottom).
    float statsH = Math.min(172f, Math.max(96f, availH * 0.20f));
    float perfH = Math.min(320f, Math.max(120f, availH * 0.38f));
    float logH = availH - statsH - perfH - 2f * gap;
    if (logH < 100f) {
      float need = 100f - logH;
      float fromPerf = Math.min(need, Math.max(0f, perfH - 110f));
      perfH -= fromPerf;
      need -= fromPerf;
      if (need > 0f) {
        float fromStats = Math.min(need, Math.max(0f, statsH - 88f));
        statsH -= fromStats;
      }
      logH = availH - statsH - perfH - 2f * gap;
    }

    float used = statsH + perfH + logH + 2f * gap;
    if (used > availH && used > 1f) {
      float scale = (availH - 2f * gap) / (statsH + perfH + logH);
      statsH *= scale;
      perfH *= scale;
      logH = availH - statsH - perfH - 2f * gap;
    }

    // Right stack: Watch, Controls, Texture Inspector.
    float watchH = statsH;
    float controlsH = Math.min(360f, Math.max(168f, availH * 0.34f));
    float textureH = availH - watchH - controlsH - 2f * gap;
    if (textureH < 120f) {
      float need = 120f - textureH;
      float fromControls = Math.min(need, Math.max(0f, controlsH - 150f));
      controlsH -= fromControls;
      textureH = availH - watchH - controlsH - 2f * gap;
    }

    float rightX = ox + workW - colW;
    float midX = ox + colW + gap;
    float midW = workW - 2f * colW - 2f * gap;
    midW = Math.max(100f, midW);

    layoutStatsX = ox;
    layoutStatsY = oy;
    layoutStatsW = colW;
    layoutStatsH = statsH;

    layoutPerfX = ox;
    layoutPerfY = oy + statsH + gap;
    layoutPerfW = colW;
    layoutPerfH = perfH;

    layoutLogX = ox;
    layoutLogY = oy + statsH + gap + perfH + gap;
    layoutLogW = colW;
    layoutLogH = logH;

    layoutWatchX = rightX;
    layoutWatchY = oy;
    layoutWatchW = colW;
    layoutWatchH = watchH;

    layoutControlsX = rightX;
    layoutControlsY = oy + watchH + gap;
    layoutControlsW = colW;
    layoutControlsH = controlsH;

    layoutTextureX = rightX;
    layoutTextureY = oy + watchH + gap + controlsH + gap;
    layoutTextureW = colW;
    layoutTextureH = textureH;

    layoutConsoleX = midX;
    layoutConsoleY = yCons;
    layoutConsoleW = midW;
    layoutConsoleH = consH;

    layoutCommandX = ox;
    layoutCommandY = yCmd;
    layoutCommandW = workW;
    layoutCommandH = cmdH;
  }

  /**
   * Returns the imgui condition flag to use for the next window's default position / size hint.
   * On the very first frame after a launch (or after {@code Reset Layout}), this returns
   * {@link ImGuiCond#Always} so the windows snap to their default rectangles even if they were
   * remembered at a previous position. After that frame it goes back to
   * {@link ImGuiCond#FirstUseEver} so the user's manual moves stick afterwards.
   */
  private int nextLayoutCond() {
    return forceLayoutNextFrame ? ImGuiCond.Always : ImGuiCond.FirstUseEver;
  }

  /**
   * Applies the default position and size for a single window. Must be called from inside a
   * {@code drawXxxWindow()} method right before the corresponding {@link ImGui#begin}.
   *
   * @param x Default top-left X in viewport coordinates.
   * @param y Default top-left Y in viewport coordinates.
   * @param w Default width in pixels.
   * @param h Default height in pixels.
   */
  private void applyWindowLayout(float x, float y, float w, float h) {
    int cond = nextLayoutCond();
    ImGui.setNextWindowPos(x, y, cond);
    ImGui.setNextWindowSize(w, h, cond);
  }

  private void snapshotLogBuffer() {
    copyLogBuffer(logSnapshot);
  }

  private void drawMainMenuBar() {
    if (!ImGui.beginMainMenuBar()) {
      return;
    }
    if (ImGui.beginMenu("Debug")) {
      ImGui.menuItem("Stats", null, showStatsWindow);
      ImGui.menuItem("Performance", null, showPerformanceWindow);
      ImGui.menuItem("Controls", null, showControlsWindow);
      ImGui.menuItem("Watch", null, showWatchWindow);
      ImGui.menuItem("Log", null, showLogWindow);
      ImGui.menuItem("Console", null, showConsoleWindow);
      ImGui.menuItem("Command Line", null, showCommandWindow);
      ImGui.menuItem("Texture Inspector", null, showTextureWindow);
      ImGui.separator();
      if (ImGui.menuItem("Reset Layout")) {
        forceLayoutNextFrame = true;
      }
      if (ImGui.menuItem("Hide Overlay")) {
        setVisible(false);
      }
      ImGui.endMenu();
    }
    if (ImGui.beginMenu("Game")) {
      boolean drawDebug = isDrawDebug();
      if (ImGui.menuItem("Show Hitboxes", null, drawDebug)) {
        toggleDrawDebug();
      }
      boolean paused = Flixel.isPaused();
      if (ImGui.menuItem("Pause Game", null, paused)) {
        Flixel.setPaused(!paused);
      }
      if (ImGui.menuItem("Reset State")) {
        Flixel.resetState();
      }
      ImGui.endMenu();
    }
    ImGui.endMainMenuBar();
  }

  private void drawStatsWindow() {
    if (!showStatsWindow.get()) {
      return;
    }
    applyWindowLayout(layoutStatsX, layoutStatsY, layoutStatsW, layoutStatsH);
    if (!ImGui.begin("Stats", showStatsWindow)) {
      ImGui.end();
      return;
    }
    drawStatRow("FPS", cachedFps);
    drawStatRow("Heap (MB)", cachedHeapMegabytes);
    drawStatRow("Native (MB)", cachedNativeMegabytes);
    drawStatRow("Active members", cachedObjectCount);

    ImGui.separator();
    boolean paused = Flixel.isPaused();
    text(COLOR_KEY, "Update");
    ImGui.sameLine();
    if (paused) {
      text(COLOR_PAUSED, "PAUSED");
    } else {
      text(COLOR_OK, "RUNNING");
    }

    Array<FlixelCamera> cams = Flixel.getCameras();
    int camCount = cams != null ? cams.size : 0;
    int inspect = getInspectCameraIndex();
    text(COLOR_KEY, "Cameras");
    ImGui.sameLine();
    if (camCount == 0) {
      text(COLOR_VALUE, "0 (none)");
    } else {
      text(COLOR_VALUE, (inspect + 1) + " / " + camCount);
    }
    ImGui.end();
  }

  private void drawStatRow(String label, int value) {
    text(COLOR_KEY, label);
    ImGui.sameLine();
    text(COLOR_VALUE, Integer.toString(value));
  }

  private void drawStatRow(String label, float value) {
    text(COLOR_KEY, label);
    ImGui.sameLine();
    text(COLOR_VALUE, formatOneDecimal(value));
  }

  /**
   * Real-time graphs of the perf ring buffers owned by {@link FlixelDebugOverlay}. Each graph is
   * a simple {@link ImGui#plotLines(String, float[], int)} call against the parent's primitive
   * sample arrays, so we never copy the data into a temporary buffer.
   */
  private void drawPerformanceWindow() {
    if (!showPerformanceWindow.get()) {
      return;
    }
    applyWindowLayout(layoutPerfX, layoutPerfY, layoutPerfW, layoutPerfH);
    if (!ImGui.begin("Performance", showPerformanceWindow)) {
      ImGui.end();
      return;
    }
    int count = getPerfCount();
    // ImGui.plotLines reads count floats from arr starting at offset, wrapping around the
    // end of the array. When the ring buffer is FULL (count == PERF_HISTORY_SIZE), the oldest
    // sample lives at perfHead (= the next write index), so we use perfHead as the offset.
    // When the ring buffer is NOT yet full, samples live at indices 0..count-1 (perfHead == count
    // points at the empty tail of the array), so the correct read offset is 0.
    int offset = (count < PERF_HISTORY_SIZE) ? 0 : getPerfHead();
    if (count == 0) {
      text(COLOR_HINT, "Collecting samples...");
      ImGui.end();
      return;
    }

    float graphWidth = ImGui.getContentRegionAvailX();
    float graphHeight = 60f;

    // Float.MAX_VALUE asks Dear ImGui to auto-scale each graph from the sample min/max. Passing
    // 0f, 0f pins both ends at zero, which produces flat lines whenever every sample is > 0
    // (FPS, heap, frame time, etc.).
    float scaleAutoMin = Float.MAX_VALUE;
    float scaleAutoMax = Float.MAX_VALUE;

    // FPS plot.
    text(COLOR_KEY, "FPS");
    ImGui.sameLine();
    text(COLOR_VALUE, Integer.toString(Math.round(latestSample(getPerfFps()))));
    ImGui.plotLines("##fps", getPerfFps(), count, offset, "", scaleAutoMin, scaleAutoMax, graphWidth, graphHeight);

    // Frame time plot.
    text(COLOR_KEY, "Frame (ms)");
    ImGui.sameLine();
    text(COLOR_VALUE, formatOneDecimal(latestSample(getPerfFrameMs())));
    ImGui.plotLines("##frame", getPerfFrameMs(), count, offset, "", scaleAutoMin, scaleAutoMax, graphWidth, graphHeight);

    // Java heap plot.
    text(COLOR_KEY, "Heap (MB)");
    ImGui.sameLine();
    text(COLOR_VALUE, formatOneDecimal(latestSample(getPerfHeapMb())));
    ImGui.plotLines("##heap", getPerfHeapMb(), count, offset, "", scaleAutoMin, scaleAutoMax, graphWidth, graphHeight);

    // Native heap plot. Skip if libGDX always returns zero on this platform; an entirely-flat zero
    // line just wastes vertical space.
    float nativePeek = latestSample(getPerfNativeMb());
    if (nativePeek > 0f) {
      text(COLOR_KEY, "Native (MB)");
      ImGui.sameLine();
      text(COLOR_VALUE, formatOneDecimal(nativePeek));
      ImGui.plotLines("##native", getPerfNativeMb(), count, offset, "", scaleAutoMin, scaleAutoMax, graphWidth, graphHeight);
    }
    ImGui.end();
  }

  /** Returns the most-recent sample written into {@code buffer}, accounting for ring rollover. */
  private float latestSample(float[] buffer) {
    int count = getPerfCount();
    if (count == 0 || buffer.length == 0) {
      return 0f;
    }
    int head = getPerfHead();
    int last = (head - 1 + buffer.length) % buffer.length;
    return buffer[last];
  }

  private void drawControlsWindow() {
    if (!showControlsWindow.get()) {
      return;
    }
    applyWindowLayout(layoutControlsX, layoutControlsY, layoutControlsW, layoutControlsH);
    if (!ImGui.begin("Controls", showControlsWindow)) {
      ImGui.end();
      return;
    }

    boolean drawDebug = isDrawDebug();
    if (ImGui.checkbox("Show hitboxes", drawDebug)) {
      toggleDrawDebug();
    }
    boolean paused = Flixel.isPaused();
    if (ImGui.checkbox("Pause game loop", paused)) {
      Flixel.setPaused(!paused);
    }

    ImGui.separator();
    text(COLOR_HEADER, "Keybinds");
    refreshKeybindLabelsIfNeeded();
    drawKeybindRow("Toggle overlay", keybindToggleLabel);
    drawKeybindRow("Toggle hitboxes", keybindHitboxLabel);
    drawKeybindRow("Pause", keybindPauseLabel);
    drawKeybindRow("Cycle camera left", keybindCycleLeftLabel);
    drawKeybindRow("Cycle camera right", keybindCycleRightLabel);
    drawKeybindRow("Pan camera (paused)", "Right Mouse drag");
    drawKeybindRow("Move sprite (paused)", "Left Mouse drag");
    drawKeybindRow("Inspect sprite (paused)", "Left Mouse click");
    drawKeybindRow("Zoom camera (paused)", "Mouse wheel");

    ImGui.separator();
    if (ImGui.button("Reset zoom on inspected camera")) {
      Array<FlixelCamera> cams = Flixel.getCameras();
      if (cams != null && cams.size > 0) {
        FlixelCamera cam = cams.get(getInspectCameraIndex());
        cam.setZoom(1f);
      }
    }
    ImGui.sameLine();
    if (ImGui.button("Toggle Texture Inspector")) {
      showTextureWindow.set(!showTextureWindow.get());
    }
    ImGui.end();
  }

  private void drawKeybindRow(String label, String value) {
    text(COLOR_KEY, label);
    ImGui.sameLine();
    text(COLOR_VALUE, value);
  }

  /**
   * Rebuilds the cached keybind label strings only when the underlying key codes have changed.
   * {@link Input.Keys#toString(int)} allocates a fresh {@link String} each call, so this caching
   * step keeps the controls panel allocation-free on the steady-state path.
   */
  private void refreshKeybindLabelsIfNeeded() {
    int t = Flixel.getDebugToggleKey();
    int h = Flixel.getDebugDrawToggleKey();
    int p = Flixel.getDebugPauseKey();
    int cl = Flixel.getDebugCameraCycleLeftKey();
    int cr = Flixel.getDebugCameraCycleRightKey();
    if (t != cachedToggleKey || keybindToggleLabel == null) {
      keybindToggleLabel = Input.Keys.toString(t);
      cachedToggleKey = t;
    }
    if (h != cachedHitboxKey || keybindHitboxLabel == null) {
      keybindHitboxLabel = Input.Keys.toString(h);
      cachedHitboxKey = h;
    }
    if (p != cachedPauseKey || keybindPauseLabel == null) {
      keybindPauseLabel = Input.Keys.toString(p);
      cachedPauseKey = p;
    }
    if (cl != cachedCycleLeftKey || keybindCycleLeftLabel == null) {
      keybindCycleLeftLabel = "Alt + " + Input.Keys.toString(cl);
      cachedCycleLeftKey = cl;
    }
    if (cr != cachedCycleRightKey || keybindCycleRightLabel == null) {
      keybindCycleRightLabel = "Alt + " + Input.Keys.toString(cr);
      cachedCycleRightKey = cr;
    }
  }

  private void drawWatchWindow() {
    if (!showWatchWindow.get()) {
      return;
    }
    applyWindowLayout(layoutWatchX, layoutWatchY, layoutWatchW, layoutWatchH);
    if (!ImGui.begin("Watch", showWatchWindow)) {
      ImGui.end();
      return;
    }
    if (watchCount == 0) {
      ImGui.textDisabled("No watches registered. Use Flixel.watch.add(...) to track values.");
      ImGui.end();
      return;
    }
    int flags = ImGuiTableFlags.RowBg | ImGuiTableFlags.Borders | ImGuiTableFlags.SizingStretchProp;
    if (ImGui.beginTable("watch_table", 2, flags)) {
      ImGui.tableSetupColumn("Name");
      ImGui.tableSetupColumn("Value");
      ImGui.tableHeadersRow();
      for (int i = 0; i < watchCount; i++) {
        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        text(COLOR_KEY, watchKeyStr[i]);
        ImGui.tableNextColumn();
        text(COLOR_VALUE, watchValueStr[i]);
      }
      ImGui.endTable();
    }
    ImGui.end();
  }

  private void drawLogWindow() {
    if (!showLogWindow.get()) {
      return;
    }
    applyWindowLayout(layoutLogX, layoutLogY, layoutLogW, layoutLogH);
    int flags = ImGuiWindowFlags.MenuBar;
    if (!ImGui.begin("Log", showLogWindow, flags)) {
      ImGui.end();
      return;
    }

    if (ImGui.beginMenuBar()) {
      ImGui.menuItem("Info", null, logShowInfo);
      ImGui.menuItem("Warn", null, logShowWarn);
      ImGui.menuItem("Error", null, logShowError);
      ImGui.separator();
      ImGui.menuItem("Auto-scroll", null, logAutoScroll);
      ImGui.endMenuBar();
    }

    if (ImGui.beginChild("log_scroll", 0, 0, false, ImGuiWindowFlags.HorizontalScrollbar)) {
      for (int i = 0; i < logSnapshot.size; i++) {
        BufferedLogLine line = logSnapshot.get(i);
        if (line == null || !isLogLevelVisible(line.level)) {
          continue;
        }
        float[] color = colorForLevel(line.level);
        if (line.tagStr.isEmpty()) {
          text(color, "[" + line.level.name() + "] " + line.messageStr);
        } else {
          text(color, "[" + line.level.name() + "] [" + line.tagStr + "] " + line.messageStr);
        }
      }
      if (scrollLogToBottom || (logAutoScroll.get() && ImGui.getScrollY() >= ImGui.getScrollMaxY() - 1f)) {
        ImGui.setScrollHereY(1f);
      }
      scrollLogToBottom = false;
    }
    ImGui.endChild();
    ImGui.end();
  }

  private void drawConsoleWindow() {
    if (!showConsoleWindow.get()) {
      return;
    }
    if (consoleBlockCount == 0) {
      // Hide automatically when there is nothing to show, but keep it discoverable via the menu.
      return;
    }
    applyWindowLayout(layoutConsoleX, layoutConsoleY, layoutConsoleW, layoutConsoleH);
    if (!ImGui.begin("Console", showConsoleWindow)) {
      ImGui.end();
      return;
    }
    for (int i = 0; i < consoleBlockCount; i++) {
      String name = consoleNameStr[i];
      if (name == null) {
        continue;
      }
      if (ImGui.collapsingHeader(name)) {
        String[] bodies = consoleBodyStrs[i];
        int n = consoleBodyCounts[i];
        ImGui.indent();
        for (int b = 0; b < n; b++) {
          if (bodies != null && bodies[b] != null) {
            text(COLOR_VALUE, bodies[b]);
          }
        }
        ImGui.unindent();
      }
    }
    ImGui.end();
  }

  /**
   * Renders the runtime command line. Pressing Enter routes the input through
   * {@link Flixel#debug Flixel.debug.executeCommand(...)}, so the same parser is shared with
   * code-driven invocations.
   */
  private void drawCommandWindow() {
    if (!showCommandWindow.get()) {
      return;
    }
    applyWindowLayout(layoutCommandX, layoutCommandY, layoutCommandW, layoutCommandH);
    if (!ImGui.begin("Command Line", showCommandWindow)) {
      ImGui.end();
      return;
    }

    text(COLOR_HINT, "Enter a command and press Enter. Type \"help\" for a list.");
    ImGui.pushItemWidth(-100f);
    int inputFlags = ImGuiInputTextFlags.EnterReturnsTrue
      | ImGuiInputTextFlags.CallbackHistory;
    if (focusCommandLine) {
      ImGui.setKeyboardFocusHere();
      focusCommandLine = false;
    }
    boolean submitted = ImGui.inputText("##cmd", commandInputBuffer, inputFlags);
    ImGui.popItemWidth();
    ImGui.sameLine();
    boolean clickedRun = ImGui.button("Run");

    // Manual history navigation since the imgui-java callback signature varies between versions.
    if (ImGui.isItemFocused()) {
      if (ImGui.isKeyPressed(GLFW.GLFW_KEY_UP)) {
        navigateHistory(-1);
      } else if (ImGui.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
        navigateHistory(+1);
      }
    }

    if (submitted || clickedRun) {
      String line = commandInputBuffer.get();
      if (line != null && !line.isBlank()) {
        Flixel.info("FlixelDebug", "> " + line);
        Flixel.debug.executeCommand(line);
      }
      commandInputBuffer.set("");
      commandHistoryCursor = -1;
      focusCommandLine = true;
    }
    ImGui.end();
  }

  /**
   * Walks the persistent command history. {@code direction} should be {@code -1} to go to an
   * older entry (Up arrow) or {@code +1} to step toward the newest. Wrapping is intentionally
   * disabled so the user can clear the field by pressing Down past the newest.
   */
  private void navigateHistory(int direction) {
    Array<String> history = Flixel.debug.getCommandHistory();
    if (history.size == 0) {
      return;
    }
    if (commandHistoryCursor < 0) {
      commandHistoryCursor = history.size;
    }
    int next = commandHistoryCursor + direction;
    if (next < 0) {
      next = 0;
    } else if (next >= history.size) {
      next = history.size;
    }
    commandHistoryCursor = next;
    if (next == history.size) {
      commandInputBuffer.set("");
    } else {
      commandInputBuffer.set(history.get(next));
    }
  }

  /**
   * Renders the texture inspector for the currently selected sprite (set by the LMB picker).
   * For atlas-backed sprites this shows the entire backing texture, not just the active frame,
   * so you can see what other graphics share the same atlas page.
   */
  private void drawTextureWindow() {
    if (!showTextureWindow.get()) {
      return;
    }
    applyWindowLayout(layoutTextureX, layoutTextureY, layoutTextureW, layoutTextureH);
    if (!ImGui.begin("Texture Inspector", showTextureWindow)) {
      ImGui.end();
      return;
    }

    FlixelObject inspected = Flixel.debug.getInspectedSprite();
    if (inspected == null) {
      text(COLOR_HINT, "Click a sprite while paused (left mouse button) to inspect its texture.");
      ImGui.end();
      return;
    }

    text(COLOR_KEY, "Type");
    ImGui.sameLine();
    text(COLOR_VALUE, inspected.getClass().getSimpleName());
    text(COLOR_KEY, "Position");
    ImGui.sameLine();
    text(COLOR_VALUE, "(" + formatOneDecimal(inspected.getX()) + ", "
      + formatOneDecimal(inspected.getY()) + ")");
    text(COLOR_KEY, "Size");
    ImGui.sameLine();
    text(COLOR_VALUE, "(" + formatOneDecimal(inspected.getWidth()) + ", "
      + formatOneDecimal(inspected.getHeight()) + ")");

    if (!(inspected instanceof FlixelSprite sprite)) {
      ImGui.separator();
      text(COLOR_HINT, "This object does not have a texture (only FlixelSprite subclasses do).");
      ImGui.end();
      return;
    }

    Texture texture = sprite.getTexture();
    if (texture == null) {
      ImGui.separator();
      text(COLOR_HINT, "Sprite has no texture loaded.");
      ImGui.end();
      return;
    }

    int handle = texture.getTextureObjectHandle();
    int texW = texture.getWidth();
    int texH = texture.getHeight();
    text(COLOR_KEY, "Texture");
    ImGui.sameLine();
    text(COLOR_VALUE, texW + " x " + texH + " (gl=" + handle + ")");

    ImGui.separator();
    // imgui-java's sliderFloat overload that mutates a float[1] is the only allocation-free way to
    // get the new value back. Reuse the same buffer field every frame instead of allocating one.
    textureViewerZoomBuf[0] = textureViewerZoom;
    if (ImGui.sliderFloat("Zoom", textureViewerZoomBuf, 0.25f, 8f, "%.2fx")) {
      textureViewerZoom = textureViewerZoomBuf[0];
    }

    if (ImGui.beginChild("texscroll", 0, 0, true, ImGuiWindowFlags.HorizontalScrollbar)) {
      // Pass the GL handle directly. ImGui treats v=0 as the top of the image and libGDX uploads
      // pixmaps so that texel (0, 0) is the top-left, so default UVs render upright.
      ImGui.image(handle, texW * textureViewerZoom, texH * textureViewerZoom);
    }
    ImGui.endChild();
    ImGui.end();
  }

  /** Renders {@code message} colored by the supplied {@code [r, g, b, a]} tuple. */
  private static void text(float[] color, String message) {
    ImGui.textColored(color[0], color[1], color[2], color[3], message);
  }

  private boolean isLogLevelVisible(FlixelLogLevel level) {
    return switch (level) {
      case INFO -> logShowInfo.get();
      case WARN -> logShowWarn.get();
      case ERROR -> logShowError.get();
    };
  }

  private static float[] colorForLevel(FlixelLogLevel level) {
    return switch (level) {
      case INFO -> COLOR_INFO;
      case WARN -> COLOR_WARN;
      case ERROR -> COLOR_ERROR;
    };
  }

  /**
   * Formats {@code value} with one decimal place. Allocates a small {@link String} but only runs
   * twice in the stats panel each time the cached stats refresh (every half-second by default),
   * so the cost is negligible.
   */
  private static String formatOneDecimal(float value) {
    int whole = (int) value;
    int tenths = Math.abs((int) ((value - whole) * 10f));
    return whole + "." + tenths;
  }
}
