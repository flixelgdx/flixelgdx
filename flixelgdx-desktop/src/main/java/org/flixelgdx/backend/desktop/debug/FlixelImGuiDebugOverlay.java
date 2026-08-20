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
package org.flixelgdx.backend.desktop.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.backend.desktop.graphics.FlixelBgfxGraphics;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugOverlay;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.logging.FlixelLogLevel;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXStats;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiInputTextCallbackData;
import imgui.ImGuiStyle;
import imgui.ImGuiViewport;
import imgui.callback.ImGuiInputTextCallback;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

/**
 * Dear ImGui based debug overlay for the desktop (bgfx + SDL3) backend.
 *
 * <h2>Wiring</h2>
 *
 * <p>{@code FlixelDesktopLauncher} registers this class as the default debug overlay factory when
 * launching in {@link org.flixelgdx.backend.FlixelRuntimeMode#DEBUG DEBUG} mode. You can also
 * register it manually with {@link Flixel#setDebugOverlay(Supplier)} before the game starts.
 *
 * <p>Initialization (creating the Dear ImGui context, uploading the font atlas as a bgfx texture, and
 * registering the SDL input listener) happens lazily on the first {@link #draw()} call, so
 * construction stays cheap and the cost is only paid once debug mode actually shows the overlay.
 *
 * <h2>Rendering and input</h2>
 *
 * <p>Unlike a typical Dear ImGui integration, nothing here talks to a graphics API directly. Panels
 * are submitted through {@link FlixelImGuiBgfxRenderer}, which turns Dear ImGui's draw lists into bgfx
 * draw calls, and input arrives through {@link FlixelImGuiSdlInput}, which forwards the desktop
 * backend's SDL events into Dear ImGui. Both are bespoke because the project ships its own bgfx and
 * SDL3 stack instead of the OpenGL and GLFW backends imgui-java bundles.
 *
 * <p>Window positions are not persisted across runs: the {@code imgui.ini} file is disabled because
 * the framework does not own a writable directory on every platform.
 *
 * <h2>Extending the overlay</h2>
 *
 * <p>Because Dear ImGui is exposed as an API dependency, games can add their own panels by
 * subclassing this overlay and overriding {@link #onDrawImGui()}; any standard {@code ImGui.*} call is
 * valid there.
 */
public class FlixelImGuiDebugOverlay extends FlixelDebugOverlay {

  private static final String TAG = "FlixelImGuiDebugOverlay";

  // Component-per-channel color constants. Colored labels use pushStyleColor(ImGuiCol.Text, ...) plus
  // textUnformatted so dynamic strings are never passed through printf-style formatting.
  private static final float[] COLOR_INFO = { 0.85f, 0.85f, 0.85f, 1f };
  private static final float[] COLOR_WARN = { 1.00f, 0.80f, 0.20f, 1f };
  private static final float[] COLOR_ERROR = { 1.00f, 0.30f, 0.30f, 1f };
  private static final float[] COLOR_DEBUG = { 0.30f, 0.30f, 1.0f, 1f };
  private static final float[] COLOR_KEY = { 0.9f, 0.2f, 0.2f, 1f };
  private static final float[] COLOR_VALUE = { 0.95f, 0.95f, 0.95f, 1f };
  private static final float[] COLOR_HEADER = { 0.9f, 0.2f, 0.2f, 1f };
  private static final float[] COLOR_OK = { 0.30f, 0.95f, 0.55f, 1f };
  private static final float[] COLOR_PAUSED = { 1.00f, 0.70f, 0.20f, 1f };
  private static final float[] COLOR_HINT = { 0.65f, 0.65f, 0.65f, 1f };

  // Widget color constants (red theme replacing the default ImGui blue).
  private static final float[] COLOR_TITLE_BG = { 0.45f, 0.07f, 0.07f, 1f };
  private static final float[] COLOR_TITLE_BG_ACTIVE = { 0.72f, 0.10f, 0.10f, 1f };
  private static final float[] COLOR_TITLE_BG_COLLAPSED = { 0.30f, 0.05f, 0.05f, 0.75f };
  private static final float[] COLOR_FRAME_BG = { 0.28f, 0.05f, 0.05f, 0.54f };
  private static final float[] COLOR_FRAME_BG_HOVERED = { 0.50f, 0.08f, 0.08f, 0.40f };
  private static final float[] COLOR_FRAME_BG_ACTIVE = { 0.62f, 0.10f, 0.10f, 0.67f };
  private static final float[] COLOR_CHECK_MARK = { 1.00f, 0.60f, 0.60f, 1f };
  private static final float[] COLOR_BUTTON = { 0.65f, 0.10f, 0.10f, 0.60f };
  private static final float[] COLOR_BUTTON_HOVERED = { 0.80f, 0.15f, 0.15f, 1f };
  private static final float[] COLOR_BUTTON_ACTIVE = { 0.92f, 0.20f, 0.20f, 1f };
  private static final float[] COLOR_COLLAPSING_HEADER = { 0.55f, 0.08f, 0.08f, 0.31f };
  private static final float[] COLOR_COLLAPSING_HEADER_HOVERED = { 0.68f, 0.11f, 0.11f, 0.80f };
  private static final float[] COLOR_COLLAPSING_HEADER_ACTIVE = { 0.78f, 0.13f, 0.13f, 1f };
  private static final float[] COLOR_RESIZE_GRIP = { 0.65f, 0.10f, 0.10f, 0.20f };
  private static final float[] COLOR_RESIZE_GRIP_HOVERED = { 0.80f, 0.15f, 0.15f, 0.67f };
  private static final float[] COLOR_RESIZE_GRIP_ACTIVE = { 0.92f, 0.20f, 0.20f, 0.95f };
  private static final float[] COLOR_SLIDER_GRAB = { 0.92f, 0.20f, 0.20f, 0.95f };
  private static final float[] COLOR_SLIDER_GRAB_ACTIVE = { 0.92f, 0.30f, 0.30f, 0.95f };

  /** Empty-state copy for the Watch panel. */
  private static final String WATCH_EMPTY_HINT = "No watches registered. Use Flixel.watch.add(...) to track values.";

  /** Empty-state copy for the Tracker panel. */
  private static final String TRACKER_EMPTY_HINT =
      "No trackers registered. Use Flixel.debug.addTrackerEntry(...) to show grouped values here.";

  /**
   * Number of unique bgfx time-series graphs, used to size the per-series Y-axis scale array.
   * Indices: 0 CPU submit, 1 GPU, 2 Wait submit, 3 Wait render, 4 GPU memory.
   * CPU frame and draw calls are omitted because they duplicate the core perf ring buffers.
   */
  private static final int BGFX_GRAPH_COUNT = 5;

  private final FlixelImGuiSdlInput imguiInput = new FlixelImGuiSdlInput();
  private final FlixelBgfxStatsSampler statsSampler = new FlixelBgfxStatsSampler();

  private FlixelBgfxGraphics graphics;
  private FlixelImGuiBgfxRenderer renderer;

  // Snapshot of the log buffer taken once per frame to avoid holding logBuffer during draw.
  private final FlixelArray<FlixelDebugOverlay.BufferedLogLine> logSnapshot = new FlixelArray<>();

  // Window visibility flags (toggled from the debug menu).
  private final ImBoolean showStatsWindow = new ImBoolean(true);
  private final ImBoolean showPerformanceWindow = new ImBoolean(true);
  private final ImBoolean showControlsWindow = new ImBoolean(true);
  private final ImBoolean showWatchWindow = new ImBoolean(true);
  private final ImBoolean showLogWindow = new ImBoolean(true);
  private final ImBoolean showTrackerWindow = new ImBoolean(true);
  private final ImBoolean showCommandWindow = new ImBoolean(true);
  private final ImBoolean showTextureWindow = new ImBoolean(false);

  // Log level filters. Toggled in the log window's menu bar.
  private final ImBoolean logShowInfo = new ImBoolean(true);
  private final ImBoolean logShowWarn = new ImBoolean(true);
  private final ImBoolean logShowError = new ImBoolean(true);
  private final ImBoolean logShowDebug = new ImBoolean(true);
  private final ImBoolean logAutoScroll = new ImBoolean(true);

  private final ImString commandInputBuffer = new ImString(256);

  /**
   * Dear ImGui consumes Up/Down on a focused {@code InputText} for caret movement before the game
   * sees them. {@link ImGuiInputTextFlags#CallbackHistory} runs inside the widget so command history
   * still works while the field stays focused.
   */
  private final ImGuiInputTextCallback commandHistoryCallback = new ImGuiInputTextCallback() {
    @Override
    public void accept(ImGuiInputTextCallbackData data) {
      if (!data.hasEventFlag(ImGuiInputTextFlags.CallbackHistory)) {
        return;
      }
      int key = data.getEventKey();
      if (key == ImGuiKey.UpArrow) {
        applyHistoryKeyInInputCallback(data, -1);
      } else if (key == ImGuiKey.DownArrow) {
        applyHistoryKeyInInputCallback(data, 1);
      }
    }
  };

  /** Last non-empty UTF-8 bytes from the command field (ImGui may clear the buffer when Run is pressed). */
  private final byte[] commandLineUtf8Scratch = new byte[512];

  // Reused float buffers fed to ImGui.sliderFloat() so the controls stay allocation-free.
  private final float[] textureViewerZoomBuf = new float[1];
  private final float[] timeScaleSliderBuf = new float[1];
  private final float[] overlayUpdateRateBuf = new float[1];

  // Watch caches mirroring cachedWatchKeys / cachedWatchValues as java String.
  private String[] watchKeyStr = new String[0];
  private String[] watchValueStr = new String[0];

  // Tracker caches mirroring cachedTrackerBlocks as String arrays.
  private String[] trackerNameStr = new String[0];
  private String[][] trackerKeyStrs = new String[0][];
  private String[][] trackerValueStrs = new String[0][];
  private int[] trackerPairCounts = new int[0];

  private String keybindToggleLabel;
  private String keybindHitboxLabel;
  private String keybindPauseLabel;
  private String keybindCycleLeftLabel;
  private String keybindCycleRightLabel;

  private int watchCount;
  private int trackerBlockCount;
  private int commandHistoryCursor = -1;
  private int commandLineUtf8ScratchLen;
  private int cachedToggleKey = -1;
  private int cachedHitboxKey = -1;
  private int cachedPauseKey = -1;
  private int cachedCycleLeftKey = -1;
  private int cachedCycleRightKey = -1;

  // Per-window default rectangles (work-area coordinates). Filled by computeDefaultLayoutRects()
  // each frame; applyWindowLayout() consumes them right before each begin().
  private float layoutStatsX;
  private float layoutStatsY;
  private float layoutStatsW;
  private float layoutStatsH;
  private float layoutPerfX;
  private float layoutPerfY;
  private float layoutPerfW;
  private float layoutPerfH;
  private float layoutLogX;
  private float layoutLogY;
  private float layoutLogW;
  private float layoutLogH;
  private float layoutWatchX;
  private float layoutWatchY;
  private float layoutWatchW;
  private float layoutWatchH;
  private float layoutControlsX;
  private float layoutControlsY;
  private float layoutControlsW;
  private float layoutControlsH;
  private float layoutTextureX;
  private float layoutTextureY;
  private float layoutTextureW;
  private float layoutTextureH;
  private float layoutTrackerX;
  private float layoutTrackerY;
  private float layoutTrackerW;
  private float layoutTrackerH;
  private float layoutCommandX;
  private float layoutCommandY;
  private float layoutCommandW;
  private float layoutCommandH;
  private float bgfxSampleTimer = 0f;
  private float overlayUpdateRate = 20f;
  private float textureViewerZoom = 1f;

  // Per-series Y-axis scale maxima for the core performance graphs. Each grows immediately when a new
  // peak appears and decays slowly toward the ring-buffer max so the graphs stay stable.
  private float perfScaleMaxFps = 1f;
  private float perfScaleMaxFrameMs = 1f;
  private float perfScaleMaxHeapMb = 1f;
  private float perfScaleMaxNativeMb = 1f;
  private float perfScaleMaxRenderCalls = 1f;

  /** Per-series Y-axis scale maxima for the bgfx render graphs, one per graph. */
  private final float[] bgfxScaleMax = new float[BGFX_GRAPH_COUNT];

  private boolean imguiInitialized;
  private boolean imguiShutdown;
  private boolean scrollLogToBottom;

  /**
   * One-shot flag that swaps the layout condition to {@link ImGuiCond#Always} for the next frame
   * (used the first time the overlay is shown and after {@code Reset Layout}).
   */
  private boolean forceLayoutNextFrame = true;

  /** When true, the Texture Inspector uses {@link ImGuiCond#Always} once so it snaps under Controls. */
  private boolean textureInspectorSnapNextFrame;

  /** Previous frame's Texture Inspector visibility (edge-detect open). */
  private boolean textureInspectorOpenPrev;

  private boolean focusCommandLine;

  /** When true, the next {@link #drawUI()} pass clears the Dear ImGui IO input queues once. */
  private boolean sanitizeImGuiInputBeforeNextDraw;

  @Override
  public void draw() {
    if (!isVisible() || imguiShutdown) {
      return;
    }
    if (!imguiInitialized) {
      initImGui();
      if (!imguiInitialized) {
        // Initialization could not complete (no bgfx graphics yet, for example). Skip rendering and
        // try again next frame so the overlay degrades gracefully instead of crashing.
        return;
      }
    }
    super.draw();
  }

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    if (!imguiInitialized || imguiShutdown) {
      return;
    }
    ImGui.getIO().setDisplaySize(width, height);
    forceLayoutNextFrame = true;
  }

  /**
   * Tears down the Dear ImGui resources in a safe order: deactivate input (which clears queued IO
   * while the context is still valid) and unregister the SDL listener, destroy the renderer's font
   * texture, then destroy the context. Each step is guarded because desktop shutdown is already partway
   * through when this runs.
   */
  @Override
  public void destroy() {
    if (!imguiShutdown && imguiInitialized) {
      imguiShutdown = true;
      try {
        imguiInput.setActive(false);
        if (Flixel.input != null) {
          Flixel.input.removeKeyboardListener(imguiInput);
          Flixel.input.removeMouseListener(imguiInput);
        }
      } catch (Throwable t) {
        // Ignore.
      }
      try {
        renderer.dispose();
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
  public void setVisible(boolean visible) {
    boolean wasVisible = isVisible();
    super.setVisible(visible);
    onVisibilityChanged(wasVisible, isVisible());
  }

  @Override
  public void toggleVisible() {
    boolean wasVisible = isVisible();
    super.toggleVisible();
    onVisibilityChanged(wasVisible, isVisible());
  }

  @Override
  public boolean isMouseCapturedByUI() {
    if (!isVisible() || !imguiInitialized) {
      return false;
    }
    return ImGui.getIO().getWantCaptureMouse();
  }

  @Override
  public boolean isKeyboardCapturedByUI() {
    if (!isVisible() || !imguiInitialized) {
      return false;
    }
    return ImGui.getIO().getWantCaptureKeyboard();
  }

  @Override
  protected void drawUI() {
    if (!imguiInitialized || imguiShutdown) {
      return;
    }
    snapshotLogBuffer();
    bgfxSampleTimer += Flixel.getRawElapsed();
    float bgfxSampleInterval = 1f / overlayUpdateRate;
    if (bgfxSampleTimer >= bgfxSampleInterval) {
      bgfxSampleTimer -= bgfxSampleInterval;
      statsSampler.sample(graphics.getBgfxStats());
    }

    if (sanitizeImGuiInputBeforeNextDraw) {
      sanitizeImGuiInputBeforeNextDraw = false;
      ImGuiIO io = ImGui.getIO();
      io.clearInputKeys();
      io.clearEventsQueue();
      io.clearInputMouse();
    }

    imguiInput.newFrame(graphics.getBackBufferWidth(), graphics.getBackBufferHeight(), Flixel.getRawElapsed());
    ImGui.newFrame();

    // Passthrough dockspace covers the whole viewport with a transparent central node so the game
    // keeps rendering through the empty space between and around docked windows.
    ImGui.dockSpaceOverViewport(0, ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);

    drawMainMenuBar();
    computeDefaultLayoutRects();

    if (forceLayoutNextFrame && showTextureWindow.get()) {
      textureInspectorSnapNextFrame = true;
    }
    boolean texInspectorOpen = showTextureWindow.get();
    if (texInspectorOpen && !textureInspectorOpenPrev) {
      textureInspectorSnapNextFrame = true;
    }
    textureInspectorOpenPrev = texInspectorOpen;

    drawStatsWindow();
    drawPerformanceWindow();
    drawWatchWindow();
    drawControlsWindow();
    drawLogWindow();
    drawTrackerWindow();
    drawCommandWindow();
    drawTextureWindow();
    onDrawImGui();

    // The forced-layout pass only lasts one frame; later frames use FirstUseEver so manual moves stick.
    if (forceLayoutNextFrame) {
      forceLayoutNextFrame = false;
    }

    ImGui.render();
    renderer.render(ImGui.getDrawData());
    updateImGuiCursor();
  }

  @Override
  protected void onWatchEntriesRefreshed() {
    int n = cachedWatchKeys.getSize();
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
  protected void onTrackerBlocksRebuilt() {
    int n = cachedTrackerBlocks.getSize();
    if (trackerNameStr.length < n) {
      trackerNameStr = new String[Math.max(n, trackerNameStr.length * 2)];
      trackerKeyStrs = new String[trackerNameStr.length][];
      trackerValueStrs = new String[trackerNameStr.length][];
      trackerPairCounts = new int[trackerNameStr.length];
    }
    for (int i = 0; i < n; i++) {
      CachedTrackerBlock block = cachedTrackerBlocks.get(i);
      trackerNameStr[i] = block.name.toString();
      int pairN = block.pairCount;
      String[] keys = trackerKeyStrs[i];
      String[] vals = trackerValueStrs[i];
      if (keys == null || keys.length < pairN) {
        keys = new String[Math.max(pairN, 4)];
        trackerKeyStrs[i] = keys;
      }
      if (vals == null || vals.length < pairN) {
        vals = new String[Math.max(pairN, 4)];
        trackerValueStrs[i] = vals;
      }
      for (int p = 0; p < pairN; p++) {
        keys[p] = block.keys[p].toString();
        vals[p] = block.values[p].toString();
      }
      trackerPairCounts[i] = pairN;
    }
    trackerBlockCount = n;
  }

  @Override
  protected void onLogEntryAppended(BufferedLogLine line) {
    if (logAutoScroll.get()) {
      scrollLogToBottom = true;
    }
  }

  /**
   * Hook for subclasses that want to add custom Dear ImGui content to the overlay.
   *
   * <p>Called once per frame from {@link #drawUI()} after all built-in panels have been submitted but
   * before {@code ImGui.render()}. The Dear ImGui frame is fully open, so any standard {@code ImGui.*}
   * call is valid here. Because Dear ImGui is immediate mode, calling {@code ImGui.begin()} with the
   * title of an existing panel appends to it instead of creating a new window, so one override can
   * both add new windows and inject rows into the built-in panels.
   */
  protected void onDrawImGui() {}

  private void initImGui() {
    if (!(Flixel.graphics instanceof FlixelBgfxGraphics bgfx)) {
      Flixel.warn(TAG, "Desktop graphics are not bgfx; the ImGui debug overlay will not run.");
      imguiShutdown = true;
      return;
    }
    graphics = bgfx;
    renderer = new FlixelImGuiBgfxRenderer(bgfx);

    ImGui.createContext();
    ImGuiIO io = ImGui.getIO();
    io.setIniFilename(null);
    // Keyboard navigation is intentionally left off: with it on, focusing any overlay window makes
    // Dear ImGui report WantCaptureKeyboard, which would suppress the F2/F3/F4 toggle keys (they now
    // go through the normal input path). Mouse drives the overlay; the toggles stay reliable.
    io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

    applyStyle();

    ImFontAtlas fonts = io.getFonts();
    fonts.addFontDefault();
    ImInt fontWidth = new ImInt();
    ImInt fontHeight = new ImInt();
    ByteBuffer pixels = fonts.getTexDataAsRGBA32(fontWidth, fontHeight);
    long texId = renderer.init(pixels, fontWidth.get(), fontHeight.get());
    if (texId == -1) {
      Flixel.warn(TAG, "Could not upload the ImGui font atlas; the debug overlay is disabled.");
      ImGui.destroyContext();
      imguiShutdown = true;
      return;
    }
    fonts.setTexID(texId);

    Flixel.input.addKeyboardListener(imguiInput);
    Flixel.input.addMouseListener(imguiInput);

    imguiInitialized = true;
    imguiInput.setActive(isVisible());
    forceRefreshOnNextUpdate();
    setOverlayUpdateRate(overlayUpdateRate);
  }

  /** Applies the red-themed Dear ImGui style, replacing the default blue accents. */
  private static void applyStyle() {
    ImGuiStyle style = ImGui.getStyle();
    setStyleColor(style, ImGuiCol.TitleBg, COLOR_TITLE_BG);
    setStyleColor(style, ImGuiCol.TitleBgActive, COLOR_TITLE_BG_ACTIVE);
    setStyleColor(style, ImGuiCol.TitleBgCollapsed, COLOR_TITLE_BG_COLLAPSED);
    setStyleColor(style, ImGuiCol.FrameBg, COLOR_FRAME_BG);
    setStyleColor(style, ImGuiCol.FrameBgHovered, COLOR_FRAME_BG_HOVERED);
    setStyleColor(style, ImGuiCol.FrameBgActive, COLOR_FRAME_BG_ACTIVE);
    setStyleColor(style, ImGuiCol.CheckMark, COLOR_CHECK_MARK);
    setStyleColor(style, ImGuiCol.Button, COLOR_BUTTON);
    setStyleColor(style, ImGuiCol.ButtonHovered, COLOR_BUTTON_HOVERED);
    setStyleColor(style, ImGuiCol.ButtonActive, COLOR_BUTTON_ACTIVE);
    setStyleColor(style, ImGuiCol.Header, COLOR_COLLAPSING_HEADER);
    setStyleColor(style, ImGuiCol.HeaderHovered, COLOR_COLLAPSING_HEADER_HOVERED);
    setStyleColor(style, ImGuiCol.HeaderActive, COLOR_COLLAPSING_HEADER_ACTIVE);
    setStyleColor(style, ImGuiCol.ResizeGrip, COLOR_RESIZE_GRIP);
    setStyleColor(style, ImGuiCol.ResizeGripHovered, COLOR_RESIZE_GRIP_HOVERED);
    setStyleColor(style, ImGuiCol.ResizeGripActive, COLOR_RESIZE_GRIP_ACTIVE);
    setStyleColor(style, ImGuiCol.SliderGrab, COLOR_SLIDER_GRAB);
    setStyleColor(style, ImGuiCol.SliderGrabActive, COLOR_SLIDER_GRAB_ACTIVE);
    style.setWindowRounding(8f);
    style.setChildRounding(6f);
    style.setFrameRounding(4f);
    style.setPopupRounding(6f);
    style.setScrollbarRounding(6f);
    style.setGrabRounding(4f);
    style.setTabRounding(6f);
  }

  private static void setStyleColor(ImGuiStyle style, int target, float[] color) {
    style.setColor(target, color[0], color[1], color[2], color[3]);
  }

  /**
   * Runs when the overlay visibility actually changes. Dear ImGui does not receive {@link #drawUI()}
   * while hidden, so the input backend is toggled and, on show, a one-shot IO sanitize is scheduled so
   * keys typed while hidden do not flush into a text field.
   */
  private void onVisibilityChanged(boolean wasVisible, boolean nowVisible) {
    if (wasVisible == nowVisible || !imguiInitialized) {
      return;
    }
    imguiInput.setActive(nowVisible);
    if (nowVisible) {
      sanitizeImGuiInputBeforeNextDraw = true;
    } else if (Flixel.mouse != null) {
      Flixel.mouse.icons.resetCursor();
    }
  }

  /**
   * Fills the per-window layout rectangles from the main viewport work area. Call only after
   * {@link ImGui#beginMainMenuBar()} / {@link ImGui#endMainMenuBar()} for this frame so work
   * coordinates exclude the menu bar.
   */
  private void computeDefaultLayoutRects() {
    ImGuiViewport viewport = ImGui.getMainViewport();
    float ox = viewport.getWorkPosX();
    float oy = viewport.getWorkPosY();
    float workW = viewport.getWorkSizeX();
    float workBottom = viewport.getWorkPosY() + viewport.getWorkSizeY();
    if (oy <= viewport.getPosY() + 0.5f) {
      oy = viewport.getPosY() + ImGui.getFrameHeight() + ImGui.getStyle().getFramePadding().y + 2f;
    }
    float workH = workBottom - oy;

    final float gap = 8f;
    float cmdH = 76f;

    float colW = Math.min(Math.max(workW * 0.175f, 220f), 338f);
    colW = Math.min(colW, Math.max(200f, (workW - 3f * gap) * 0.48f));

    float rightX = ox + workW - colW;
    float logW = Math.min(colW * 1.75f, Math.max(colW, rightX - gap - ox));

    float yCmd = oy + workH - cmdH;
    float availH = yCmd - oy - gap;
    while (availH < 200f && cmdH > 52f) {
      cmdH -= 6f;
      yCmd = oy + workH - cmdH;
      availH = yCmd - oy - gap;
    }
    availH = Math.max(96f, availH);

    float statsH = Math.min(172f, Math.max(96f, availH * 0.19f));
    float perfH = Math.min(380f, Math.max(148f, availH * 0.43f));
    float logHBase = availH - statsH - perfH - 2f * gap;
    if (logHBase < 100f) {
      float need = 100f - logHBase;
      float fromPerf = Math.min(need, Math.max(0f, perfH - 128f));
      perfH -= fromPerf;
      need -= fromPerf;
      if (need > 0f) {
        float fromStats = Math.min(need, Math.max(0f, statsH - 88f));
        statsH -= fromStats;
      }
      logHBase = availH - statsH - perfH - 2f * gap;
    }

    float usedLeft = statsH + perfH + logHBase + 2f * gap;
    if (usedLeft > availH && usedLeft > 1f) {
      float scale = (availH - 2f * gap) / (statsH + perfH + logHBase);
      statsH *= scale;
      perfH *= scale;
      logHBase = availH - statsH - perfH - 2f * gap;
    }

    float logSlotTop = oy + statsH + gap + perfH + gap;
    float logSlotBottom = oy + availH;
    float logSlotH = Math.max(0f, logSlotBottom - logSlotTop);
    float logH = Math.min(logHBase * 1.35f, logSlotH);

    float watchH = Math.min(statsH * 1.25f, availH * 0.28f);
    final float minTextureStripe = 96f;
    final float minControlsH = 400f;
    float controlsTarget = Math.min(540f, Math.max(minControlsH, availH * 0.46f));
    float maxControlsFit = availH - watchH - 2f * gap - minTextureStripe;
    float controlsH = Math.min(controlsTarget, maxControlsFit);
    if (controlsH < 260f) {
      controlsH = Math.min(maxControlsFit, Math.max(220f, availH * 0.38f));
    }

    float textureHBase = availH - watchH - controlsH - 2f * gap;
    if (textureHBase < 72f) {
      float need = 72f - textureHBase;
      float fromControls = Math.min(need, Math.max(0f, controlsH - minControlsH));
      controlsH -= fromControls;
      need -= fromControls;
      if (need > 0f) {
        float fromWatch = Math.min(need, Math.max(0f, watchH - 56f));
        watchH -= fromWatch;
      }
      textureHBase = availH - watchH - controlsH - 2f * gap;
    }

    float rightColMaxW = workW - logW - gap;
    float watchW = Math.min(colW * 1.4f, rightColMaxW);
    float controlsW = Math.min(Math.max(watchW, colW * 1.25f), rightColMaxW);

    float textureTop = oy + watchH + gap + controlsH + gap;
    float textureBottomLimit = oy + availH + 120;
    float maxTextureH = Math.max(72f, textureBottomLimit - textureTop);
    float textureW = Math.min(colW * 1.8f, workW - colW - gap);
    float textureHDesired = Math.min(480f, Math.max(140f, textureHBase * 1.75f));
    float textureH = Math.min(textureHDesired, maxTextureH);

    layoutStatsX = ox;
    layoutStatsY = oy;
    layoutStatsW = colW;
    layoutStatsH = statsH;

    layoutPerfX = ox;
    layoutPerfY = oy + statsH + gap;
    layoutPerfW = colW;
    layoutPerfH = perfH;

    layoutLogX = ox;
    layoutLogY = logSlotTop + (logSlotH - logH) * 0.5f;
    layoutLogW = logW;
    layoutLogH = logH;

    layoutWatchX = ox + workW - watchW;
    layoutWatchY = oy;
    layoutWatchW = watchW;
    layoutWatchH = watchH;

    layoutControlsX = ox + workW - controlsW;
    layoutControlsY = oy + watchH + gap;
    layoutControlsW = controlsW;
    layoutControlsH = controlsH;

    layoutTextureX = ox + workW - textureW;
    layoutTextureY = textureTop;
    layoutTextureW = textureW;
    layoutTextureH = textureH;

    layoutTrackerX = ox + workW - controlsW;
    layoutTrackerY = textureTop;
    layoutTrackerW = controlsW;
    layoutTrackerH = Math.max(72f, textureHBase);

    layoutCommandX = ox;
    layoutCommandY = yCmd;
    layoutCommandW = workW;
    layoutCommandH = cmdH;
  }

  /** Returns the imgui condition flag to use for the next window's default position / size hint. */
  private int nextLayoutCond() {
    return forceLayoutNextFrame ? ImGuiCond.Always : ImGuiCond.FirstUseEver;
  }

  private void applyWindowLayout(float x, float y, float w, float h) {
    applyWindowLayout(x, y, w, h, nextLayoutCond());
  }

  private void applyWindowLayout(float x, float y, float w, float h, int cond) {
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
      ImGui.menuItem("Tracker", null, showTrackerWindow);
      ImGui.menuItem("Watch", null, showWatchWindow);
      ImGui.menuItem("Log", null, showLogWindow);
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
      boolean paused = Flixel.game.isGamePaused();
      if (ImGui.menuItem("Pause Game", null, paused)) {
        Flixel.game.setGamePaused(!paused);
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
    drawStatRow("Assets loaded", cachedAssetCount);
    drawStatRow("Render calls", cachedRenderCalls);

    ImGui.separator();
    boolean paused = Flixel.game.isGamePaused();
    text(COLOR_KEY, "Update");
    ImGui.sameLine();
    if (paused) {
      text(COLOR_PAUSED, "PAUSED");
    } else {
      text(COLOR_OK, "RUNNING");
    }

    FlixelArray<FlixelCamera> cams = Flixel.cameras;
    int camCount = cams != null ? cams.getSize() : 0;
    int inspect = getInspectCameraIndex();
    text(COLOR_KEY, "Cameras");
    ImGui.sameLine();
    if (camCount == 0) {
      text(COLOR_VALUE, "0 (none)");
    } else {
      text(COLOR_VALUE, (inspect + 1) + " / " + camCount);
      FlixelCamera cam = cams.get(inspect);
      drawStatRow("  Scroll X", cam.scrollX);
      drawStatRow("  Scroll Y", cam.scrollY);
      drawStatRow("  Zoom", cam.getZoom());
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
   * Merged Performance panel: real-time graphs of the platform-agnostic ring buffers and bgfx render
   * statistics, organized into collapsing sections. The Core section (FPS, frame time, draw calls)
   * starts open; Memory, GPU Timing, Submission, and Resources start collapsed.
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

    text(COLOR_KEY, "Graphics API");
    ImGui.sameLine();
    text(COLOR_VALUE, graphics.getApi().toString());
    text(COLOR_KEY, "Back buffer");
    ImGui.sameLine();
    text(COLOR_VALUE, graphics.getBackBufferWidth() + " x " + graphics.getBackBufferHeight());

    BGFXStats stats = graphics.getBgfxStats();
    int bgfxCount = statsSampler.getCount();
    int bgfxOffset = statsSampler.getPlotOffset();
    // Read conversion factors directly from the live stats so they stay current even when the
    // sampler is throttled; fall back to the sampler's last known values when stats is null.
    long cpuFreq = stats != null ? stats.cpuTimerFreq() : 0;
    long gpuFreq = stats != null ? stats.gpuTimerFreq() : 0;
    double cpuToMs = cpuFreq > 0 ? 1000.0 / cpuFreq : statsSampler.getCpuToMs();
    double gpuToMs = gpuFreq > 0 ? 1000.0 / gpuFreq : statsSampler.getGpuToMs();
    float graphWidth = ImGui.getContentRegionAvailX();
    float graphHeight = 60f;
    float graphHeightSmall = 48f;
    int count = getPerfCount();
    int offset = (count < PERF_HISTORY_SIZE) ? 0 : getPerfHead();

    if (ImGui.collapsingHeader("Core", ImGuiTreeNodeFlags.DefaultOpen)) {
      if (count == 0) {
        text(COLOR_HINT, "Collecting samples...");
      } else {
        perfScaleMaxFps = Math.max(ringMax(getPerfFps(), count) * 1.15f, perfScaleMaxFps * 0.997f);
        perfScaleMaxFrameMs = Math.max(ringMax(getPerfFrameMs(), count) * 1.15f, perfScaleMaxFrameMs * 0.997f);
        perfScaleMaxRenderCalls =
            Math.max(ringMax(getPerfRenderCalls(), count) * 1.15f, perfScaleMaxRenderCalls * 0.997f);

        text(COLOR_KEY, "FPS");
        ImGui.sameLine();
        text(COLOR_VALUE, Integer.toString(Math.round(latestSample(getPerfFps()))));
        ImGui.plotLines("##fps", getPerfFps(), count, offset, "", 0f, perfScaleMaxFps, graphWidth, graphHeight);

        text(COLOR_KEY, "Frame (ms)");
        ImGui.sameLine();
        text(COLOR_VALUE, formatOneDecimal(latestSample(getPerfFrameMs())));
        ImGui.plotLines("##frame", getPerfFrameMs(), count, offset, "", 0f, perfScaleMaxFrameMs, graphWidth,
            graphHeight);

        text(COLOR_KEY, "Draw calls");
        ImGui.sameLine();
        text(COLOR_VALUE, Integer.toString(Math.round(latestSample(getPerfRenderCalls()))));
        ImGui.plotLines("##rendercalls", getPerfRenderCalls(), count, offset, "", 0f, perfScaleMaxRenderCalls,
            graphWidth,
            graphHeight);
      }
    }

    if (ImGui.collapsingHeader("Memory")) {
      if (count > 0) {
        perfScaleMaxHeapMb = Math.max(ringMax(getPerfHeapMb(), count) * 1.15f, perfScaleMaxHeapMb * 0.997f);
        perfScaleMaxNativeMb = Math.max(ringMax(getPerfNativeMb(), count) * 1.15f, perfScaleMaxNativeMb * 0.997f);

        text(COLOR_KEY, "Heap (MB)");
        ImGui.sameLine();
        text(COLOR_VALUE, formatOneDecimal(latestSample(getPerfHeapMb())));
        ImGui.plotLines("##heap", getPerfHeapMb(), count, offset, "", 0f, perfScaleMaxHeapMb, graphWidth, graphHeight);

        float nativePeek = latestSample(getPerfNativeMb());
        if (nativePeek > 0f) {
          text(COLOR_KEY, "Native (MB)");
          ImGui.sameLine();
          text(COLOR_VALUE, formatOneDecimal(nativePeek));
          ImGui.plotLines("##native", getPerfNativeMb(), count, offset, "", 0f, perfScaleMaxNativeMb, graphWidth,
              graphHeight);
        }
      }
      if (bgfxCount > 0) {
        if (statsSampler.latest(statsSampler.getGpuMemoryMb()) > 0f) {
          drawBgfxGraph(4, "GPU (MB)", statsSampler.getGpuMemoryMb(), bgfxCount, bgfxOffset, graphWidth,
              graphHeightSmall,
              false);
        } else {
          text(COLOR_HINT, "GPU memory tracking unavailable for this graphics API.");
        }
      }
      if (stats != null) {
        ImGui.separator();
        drawByteRow("GPU memory used", stats.gpuMemoryUsed());
        drawByteRow("GPU memory max", stats.gpuMemoryMax());
        drawByteRow("Texture memory", stats.textureMemoryUsed());
        drawByteRow("Render target memory", stats.rtMemoryUsed());
        drawStatRow("Transient VB (KB)", stats.transientVbUsed() / 1024);
        drawStatRow("Transient IB (KB)", stats.transientIbUsed() / 1024);
      }
    }

    if (ImGui.collapsingHeader("GPU Timing")) {
      if (bgfxCount == 0) {
        text(COLOR_HINT, "Collecting samples...");
      } else {
        drawBgfxGraph(0, "CPU submit (ms)", statsSampler.getCpuSubmitMs(), bgfxCount, bgfxOffset, graphWidth,
            graphHeightSmall, true);
        drawBgfxGraph(1, "GPU (ms)", statsSampler.getGpuMs(), bgfxCount, bgfxOffset, graphWidth, graphHeightSmall,
            true);
        drawBgfxGraph(2, "Wait submit (ms)", statsSampler.getWaitSubmitMs(), bgfxCount, bgfxOffset, graphWidth,
            graphHeightSmall, true);
        drawBgfxGraph(3, "Wait render (ms)", statsSampler.getWaitRenderMs(), bgfxCount, bgfxOffset, graphWidth,
            graphHeightSmall, true);
      }
      if (stats != null) {
        ImGui.separator();
        drawStatRow("CPU frame (ms)", (float) (stats.cpuTimeFrame() * cpuToMs));
        drawStatRow("CPU submit (ms)", (float) ((stats.cpuTimeEnd() - stats.cpuTimeBegin()) * cpuToMs));
        if (gpuToMs > 0.0) {
          drawStatRow("GPU (ms)", (float) ((stats.gpuTimeEnd() - stats.gpuTimeBegin()) * gpuToMs));
        } else {
          text(COLOR_KEY, "GPU (ms)");
          ImGui.sameLine();
          text(COLOR_VALUE, "n/a");
        }
        drawStatRow("Wait submit (ms)", (float) (stats.waitSubmit() * cpuToMs));
        drawStatRow("Wait render (ms)", (float) (stats.waitRender() * cpuToMs));
      }
    }

    if (stats != null) {
      if (ImGui.collapsingHeader("Submission")) {
        drawStatRow("Peak draw calls", stats.numDrawCallsPeak());
        drawStatRow("Compute calls", stats.numCompute());
        drawStatRow("Blit calls", stats.numBlit());
        drawStatRow("Triangles", stats.numPrims(BGFX.BGFX_TOPOLOGY_TRI_LIST));
        drawStatRow("Views", stats.numViews());
        drawStatRow("Encoders", stats.numEncoders());
        drawStatRow("GPU latency", stats.maxGpuLatency());
      }
      if (ImGui.collapsingHeader("Resources")) {
        drawStatRow("Textures", stats.numTextures());
        drawStatRow("Shaders", stats.numShaders());
        drawStatRow("Programs", stats.numPrograms());
        drawStatRow("Uniforms", stats.numUniforms());
        drawStatRow("Frame buffers", stats.numFrameBuffers());
        drawStatRow("Vertex buffers", stats.numVertexBuffers());
        drawStatRow("Index buffers", stats.numIndexBuffers());
        drawStatRow("Dynamic VB", stats.numDynamicVertexBuffers());
        drawStatRow("Dynamic IB", stats.numDynamicIndexBuffers());
      }
    }

    ImGui.end();
  }

  /**
   * Draws one labeled bgfx graph with a live value readout.
   *
   * @param index The per-series scale slot (0 to {@link #BGFX_GRAPH_COUNT} - 1).
   * @param label The row label.
   * @param ring The ring buffer to plot.
   * @param count The number of valid samples.
   * @param offset The read offset of the oldest sample.
   * @param width The graph width in pixels.
   * @param height The graph height in pixels.
   * @param oneDecimal Whether to format the live value with one decimal ({@code true}) or as a
   *     rounded integer ({@code false}).
   */
  private void drawBgfxGraph(int index, String label, float[] ring, int count, int offset,
      float width, float height, boolean oneDecimal) {
    float latest = statsSampler.latest(ring);
    bgfxScaleMax[index] = Math.max(ringMax(ring, count) * 1.15f, bgfxScaleMax[index] * 0.997f);
    if (bgfxScaleMax[index] < 1e-4f) {
      bgfxScaleMax[index] = 1e-4f;
    }
    text(COLOR_KEY, label);
    ImGui.sameLine();
    text(COLOR_VALUE, oneDecimal ? formatOneDecimal(latest) : Integer.toString(Math.round(latest)));
    ImGui.plotLines("##bgfx" + index, ring, count, offset, "", 0f, bgfxScaleMax[index], width, height);
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
    boolean paused = Flixel.game.isGamePaused();
    if (ImGui.checkbox("Pause game loop", paused)) {
      Flixel.game.setGamePaused(!paused);
    }

    ImGui.separator();
    text(COLOR_HEADER, "Time scale");
    timeScaleSliderBuf[0] = Flixel.timeScale;
    ImGui.pushItemWidth(-100f);
    if (ImGui.sliderFloat("##timescale", timeScaleSliderBuf, 0.1f, 4.0f, "%.2fx")) {
      Flixel.timeScale = timeScaleSliderBuf[0];
    }
    ImGui.sameLine();
    if (ImGui.button("Reset##timescale")) {
      Flixel.timeScale = 1f;
    }
    ImGui.popItemWidth();

    ImGui.separator();
    text(COLOR_HEADER, "Update rate (Hz)");
    overlayUpdateRateBuf[0] = overlayUpdateRate;
    ImGui.pushItemWidth(-100f);
    if (ImGui.sliderFloat("##updaterate", overlayUpdateRateBuf, 1f, 30f, "%.0f Hz")) {
      setOverlayUpdateRate(overlayUpdateRateBuf[0]);
    }
    ImGui.sameLine();
    if (ImGui.button("Reset##updaterate")) {
      setOverlayUpdateRate(20f);
    }
    ImGui.popItemWidth();

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
      FlixelArray<FlixelCamera> cams = Flixel.cameras;
      if (cams != null && cams.getSize() > 0) {
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
   * {@link FlixelKey#toString(int)} allocates a fresh {@link String} each call, so this caching keeps
   * the controls panel allocation-free on the steady-state path.
   */
  private void refreshKeybindLabelsIfNeeded() {
    int t = this.toggleKey;
    int h = this.drawDebugKey;
    int p = this.pauseKey;
    int cl = this.cameraCycleLeftKey;
    int cr = this.cameraCycleRightKey;
    if (t != cachedToggleKey || keybindToggleLabel == null) {
      keybindToggleLabel = FlixelKey.toString(t);
      cachedToggleKey = t;
    }
    if (h != cachedHitboxKey || keybindHitboxLabel == null) {
      keybindHitboxLabel = FlixelKey.toString(h);
      cachedHitboxKey = h;
    }
    if (p != cachedPauseKey || keybindPauseLabel == null) {
      keybindPauseLabel = FlixelKey.toString(p);
      cachedPauseKey = p;
    }
    if (cl != cachedCycleLeftKey || keybindCycleLeftLabel == null) {
      keybindCycleLeftLabel = "Alt + " + FlixelKey.toString(cl);
      cachedCycleLeftKey = cl;
    }
    if (cr != cachedCycleRightKey || keybindCycleRightLabel == null) {
      keybindCycleRightLabel = "Alt + " + FlixelKey.toString(cr);
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
      ImGui.textDisabled(WATCH_EMPTY_HINT);
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
      ImGui.menuItem("Debug", null, logShowDebug);
      ImGui.separator();
      ImGui.menuItem("Auto-scroll", null, logAutoScroll);
      ImGui.endMenuBar();
    }

    if (ImGui.beginChild("log_scroll", 0, 0, false, ImGuiWindowFlags.HorizontalScrollbar)) {
      for (int i = 0; i < logSnapshot.getSize(); i++) {
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

  /**
   * Renders the Tracker panel: a collapsible header per registered
   * {@link org.flixelgdx.debug.FlixelDebugTrackerEntry FlixelDebugTrackerEntry}, each with a
   * {@code name -> value} table like the Watch panel. When no trackers are registered it stays visible
   * with a hint, mirroring the Watch panel's empty state.
   */
  private void drawTrackerWindow() {
    if (!showTrackerWindow.get()) {
      return;
    }
    applyWindowLayout(layoutTrackerX, layoutTrackerY, layoutTrackerW, layoutTrackerH);
    if (!ImGui.begin("Tracker", showTrackerWindow)) {
      ImGui.end();
      return;
    }
    if (trackerBlockCount == 0) {
      ImGui.textDisabled(TRACKER_EMPTY_HINT);
      ImGui.end();
      return;
    }
    int tableFlags = ImGuiTableFlags.RowBg | ImGuiTableFlags.Borders | ImGuiTableFlags.SizingStretchProp;
    for (int i = 0; i < trackerBlockCount; i++) {
      String name = trackerNameStr[i];
      if (name == null) {
        continue;
      }
      if (ImGui.collapsingHeader(name, ImGuiTreeNodeFlags.DefaultOpen)) {
        String[] keys = trackerKeyStrs[i];
        String[] vals = trackerValueStrs[i];
        int n = trackerPairCounts[i];
        ImGui.pushID(i);
        if (ImGui.beginTable("tracker_table", 2, tableFlags)) {
          ImGui.tableSetupColumn("Name");
          ImGui.tableSetupColumn("Value");
          ImGui.tableHeadersRow();
          for (int p = 0; p < n; p++) {
            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            text(COLOR_KEY, keys != null && keys[p] != null ? keys[p] : "");
            ImGui.tableNextColumn();
            text(COLOR_VALUE, vals != null && vals[p] != null ? vals[p] : "");
          }
          ImGui.endTable();
        }
        ImGui.popID();
      }
    }
    ImGui.end();
  }

  /**
   * Renders the runtime command line. Pressing Enter routes the input through
   * {@code Flixel.debug.executeCommand(...)}, so the same parser is shared with code-driven
   * invocations.
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
    int inputFlags = ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.CallbackHistory;
    if (focusCommandLine) {
      ImGui.setKeyboardFocusHere();
      focusCommandLine = false;
    }
    boolean submitted = ImGui.inputText("##cmd", commandInputBuffer, inputFlags, commandHistoryCallback);
    String commandSnapshot = commandInputBuffer.get();
    ImGui.popItemWidth();
    ImGui.sameLine();
    boolean clickedRun = ImGui.button("Run");

    updateCommandLineScratchAfterInput(clickedRun);

    if (submitted || clickedRun) {
      String line = commandSnapshot != null ? commandSnapshot.trim() : "";
      if (line.isEmpty() && clickedRun && commandLineUtf8ScratchLen > 0) {
        line = decodeCommandLineScratchUtf8();
      }
      if (!line.isEmpty()) {
        Flixel.info("FlixelDebug", "> " + line);
        Flixel.debug.executeCommand(line);
        commandLineUtf8ScratchLen = 0;
      }
      commandInputBuffer.set("");
      commandHistoryCursor = -1;
      focusCommandLine = true;
    }
    ImGui.end();
  }

  /**
   * Renders the texture inspector for the currently selected sprite (set by the LMB picker). For
   * atlas-backed sprites this shows the entire backing texture, not just the active frame, so you can
   * see what other graphics share the same atlas page.
   */
  private void drawTextureWindow() {
    if (!showTextureWindow.get()) {
      return;
    }
    int layoutCond = textureInspectorSnapNextFrame ? ImGuiCond.Always : nextLayoutCond();
    if (textureInspectorSnapNextFrame) {
      textureInspectorSnapNextFrame = false;
    }
    applyWindowLayout(layoutTextureX, layoutTextureY, layoutTextureW, layoutTextureH, layoutCond);
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
    text(COLOR_VALUE, "(" + formatOneDecimal(inspected.getX()) + ", " + formatOneDecimal(inspected.getY()) + ")");
    text(COLOR_KEY, "Size");
    ImGui.sameLine();
    text(COLOR_VALUE, "(" + formatOneDecimal(inspected.getWidth()) + ", " + formatOneDecimal(inspected.getHeight())
        + ")");

    if (!(inspected instanceof FlixelSprite sprite)) {
      ImGui.separator();
      text(COLOR_HINT, "This object does not have a texture (only FlixelSprite subclasses do).");
      ImGui.end();
      return;
    }

    FlixelTexture texture = sprite.getTexture();
    if (texture == null) {
      ImGui.separator();
      text(COLOR_HINT, "Sprite has no texture loaded.");
      ImGui.end();
      return;
    }

    long handle = texture.getHandle();
    int texW = texture.getWidth();
    int texH = texture.getHeight();
    text(COLOR_KEY, "Texture");
    ImGui.sameLine();
    text(COLOR_VALUE, texW + " x " + texH + " (bgfx=" + handle + ")");

    ImGui.separator();
    textureViewerZoomBuf[0] = textureViewerZoom;
    if (ImGui.sliderFloat("Zoom", textureViewerZoomBuf, 0.25f, 8f, "%.2fx")) {
      textureViewerZoom = textureViewerZoomBuf[0];
    }

    if (ImGui.beginChild("texscroll", 0, 0, true, ImGuiWindowFlags.HorizontalScrollbar)) {
      // Pass the bgfx texture handle as the Dear ImGui texture id; the bgfx renderer binds it back.
      // Framework textures store texel (0, 0) at the top-left, matching ImGui's default UVs, so the
      // image renders upright.
      ImGui.image(handle, texW * textureViewerZoom, texH * textureViewerZoom);
    }
    ImGui.endChild();
    ImGui.end();
  }

  /**
   * Refreshes the UTF-8 scratch from the live ImGui buffer when it has content; when empty, keeps the
   * previous scratch if {@code clickedRun} is true so Run can still read the last typed line.
   */
  private void updateCommandLineScratchAfterInput(boolean clickedRun) {
    byte[] src = commandInputBuffer.getData();
    int cap = Math.min(src.length, commandLineUtf8Scratch.length);
    int i = 0;
    for (; i < cap && src[i] != 0; i++) {
      commandLineUtf8Scratch[i] = src[i];
    }
    if (i > 0) {
      commandLineUtf8ScratchLen = i;
    } else if (!clickedRun) {
      commandLineUtf8ScratchLen = 0;
    }
  }

  private String decodeCommandLineScratchUtf8() {
    return new String(commandLineUtf8Scratch, 0, commandLineUtf8ScratchLen, StandardCharsets.UTF_8).trim();
  }

  /**
   * Walks the persistent command history from an {@code InputText} history callback. {@code direction}
   * is {@code -1} for Up (older) or {@code +1} for Down (newer). Wrapping is disabled so Down past the
   * newest clears the buffer.
   */
  private void applyHistoryKeyInInputCallback(ImGuiInputTextCallbackData data, int direction) {
    FlixelArray<String> history = Flixel.debug.getCommandHistory();
    if (history.getSize() == 0) {
      return;
    }
    if (commandHistoryCursor < 0) {
      commandHistoryCursor = history.getSize();
    }
    int next = commandHistoryCursor + direction;
    if (next < 0) {
      next = 0;
    } else if (next > history.getSize()) {
      next = history.getSize();
    }
    commandHistoryCursor = next;
    String line = next == history.getSize() ? "" : history.get(next);
    data.deleteChars(0, data.getBufTextLen());
    if (!line.isEmpty()) {
      data.insertChars(0, line);
    }
    int end = data.getBufTextLen();
    data.setSelectionStart(end);
    data.setSelectionEnd(end);
    data.setCursorPos(end);
  }

  private boolean isLogLevelVisible(FlixelLogLevel level) {
    return switch (level) {
      case INFO -> logShowInfo.get();
      case WARN -> logShowWarn.get();
      case ERROR -> logShowError.get();
      case DEBUG -> logShowDebug.get();
    };
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

  private static float[] colorForLevel(FlixelLogLevel level) {
    return switch (level) {
      case INFO -> COLOR_INFO;
      case WARN -> COLOR_WARN;
      case ERROR -> COLOR_ERROR;
      case DEBUG -> COLOR_DEBUG;
    };
  }

  /**
   * Formats {@code value} with one decimal place. Allocates a small {@link String}, but only when a
   * value is actually shown, so the cost is negligible.
   */
  private static String formatOneDecimal(float value) {
    int whole = (int) value;
    int tenths = Math.abs((int) ((value - whole) * 10f));
    return whole + "." + tenths;
  }

  /**
   * Returns the maximum value across the first {@code count} elements of {@code buf} without
   * allocating a sorted copy. Returns {@code 0} when {@code count} is zero.
   */
  private static float ringMax(float[] buf, int count) {
    float m = 0f;
    for (int i = 0; i < count; i++) {
      if (buf[i] > m) {
        m = buf[i];
      }
    }
    return m;
  }

  /** Draws a {@code label} plus a byte count formatted as megabytes, or "n/a" for a negative total. */
  private void drawByteRow(String label, long bytes) {
    text(COLOR_KEY, label);
    ImGui.sameLine();
    if (bytes < 0) {
      text(COLOR_VALUE, "n/a");
    } else {
      text(COLOR_VALUE, formatOneDecimal(bytes / (1024f * 1024f)) + " MB");
    }
  }

  /**
   * Renders {@code message} colored by the supplied {@code [r, g, b, a]} tuple. Uses
   * {@link ImGui#textUnformatted(String)} so a literal {@code %} is not interpreted as a printf format
   * sequence by the native Dear ImGui binding.
   */
  private static void text(float[] color, String message) {
    ImGui.pushStyleColor(ImGuiCol.Text, color[0], color[1], color[2], color[3]);
    ImGui.textUnformatted(message != null ? message : "");
    ImGui.popStyleColor();
  }

  /**
   * Applies the cursor that Dear ImGui requested for this frame. Only takes effect when ImGui owns
   * the mouse or explicitly wants a non-arrow cursor (resize grips, text fields, etc.); otherwise
   * the call is skipped so the game's own cursor choice is not overwritten while the cursor is over
   * the game viewport.
   */
  private void updateImGuiCursor() {
    if (Flixel.mouse == null) {
      return;
    }
    int cursor = ImGui.getMouseCursor();
    if (cursor != ImGuiMouseCursor.Arrow || ImGui.getIO().getWantCaptureMouse()) {
      Flixel.mouse.icons.setCursor(imguiCursorToFlixel(cursor));
    }
  }

  private static FlixelMouseCursor imguiCursorToFlixel(int imguiCursor) {
    return switch (imguiCursor) {
      case ImGuiMouseCursor.TextInput -> FlixelMouseCursor.IBEAM;
      case ImGuiMouseCursor.ResizeAll -> FlixelMouseCursor.ALL_RESIZE;
      case ImGuiMouseCursor.ResizeNS -> FlixelMouseCursor.VERTICAL_RESIZE;
      case ImGuiMouseCursor.ResizeEW -> FlixelMouseCursor.HORIZONTAL_RESIZE;
      case ImGuiMouseCursor.ResizeNESW -> FlixelMouseCursor.NORTH_EAST_SOUTH_WEST_RESIZE;
      case ImGuiMouseCursor.ResizeNWSE -> FlixelMouseCursor.NORTH_WEST_SOUTH_EAST_RESIZE;
      case ImGuiMouseCursor.Hand -> FlixelMouseCursor.HAND;
      case ImGuiMouseCursor.NotAllowed -> FlixelMouseCursor.NOT_ALLOWED;
      case ImGuiMouseCursor.None -> FlixelMouseCursor.NONE;
      default -> FlixelMouseCursor.ARROW;
    };
  }

  /**
   * Returns the current data update rate for this overlay in updates per second.
   *
   * <p>This rate governs how often the graphs sample new data, how often the Watch and Tracker
   * panels refresh their values, and how often the Stats panel updates its counters. The value is
   * always within the range {@code [1, 30]} Hz.
   *
   * @return The update rate in Hertz.
   */
  public float getOverlayUpdateRate() {
    return overlayUpdateRate;
  }

  /**
   * Sets the data update rate for this overlay, in updates per second. Affects graphs, the Watch
   * panel, the Tracker panel, and the Stats panel.
   *
   * <p>The value is clamped to {@code [1, 30]} Hz. Lower values reduce CPU overhead from the debug
   * overlay; higher values give smoother, more reactive graphs and watch readings.
   *
   * @param hz The desired rate in Hertz. Values below 1 are raised to 1; values above 30 are
   *     lowered to 30.
   */
  public void setOverlayUpdateRate(float hz) {
    overlayUpdateRate = Math.max(1f, Math.min(30f, hz));
    float interval = 1f / overlayUpdateRate;
    perfSampleInterval = interval;
    watchRefreshInterval = interval;
    statsUpdateInterval = interval;
  }
}
