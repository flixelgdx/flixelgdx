/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.FlixelUpdatable;
import me.stringdotjar.flixelgdx.input.keyboard.FlixelKey;
import me.stringdotjar.flixelgdx.logging.FlixelDebugConsoleEntry;
import me.stringdotjar.flixelgdx.logging.FlixelLogEntry;
import me.stringdotjar.flixelgdx.logging.FlixelLogger;
import me.stringdotjar.flixelgdx.util.FlixelConstants;
import me.stringdotjar.flixelgdx.util.FlixelDebugUtil;
import me.stringdotjar.flixelgdx.util.FlixelString;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * The in-game debug overlay drawn as a <strong>separate layer</strong> on top of the entire game
 * (including over the UI stage). It has its own {@link SpriteBatch}, {@link OrthographicCamera}
 * and {@link ScreenViewport} and is completely independent of the game rendering pipeline.
 *
 * <p>The overlay displays:
 * <ul>
 *   <li>FPS and memory statistics</li>
 *   <li>Active object/member count</li>
 *   <li>A log console showing info/warn/error messages (newest at the bottom)</li>
 *   <li>A watch panel showing registered variable watches</li>
 *   <li>Optional visual debugging (bounding boxes around {@link me.stringdotjar.flixelgdx.FlixelObject} instances)</li>
 * </ul>
 *
 * <p>Toggle overlay visibility with {@link Flixel#getDebugToggleKey()} (default: {@link FlixelConstants.Debug#DEFAULT_TOGGLE_KEY}).
 * Toggle visual debug (hitboxes) with {@link Flixel#getDebugDrawToggleKey()} (default: {@link FlixelConstants.Debug#DEFAULT_DRAW_DEBUG_KEY}).
 * In debug mode, {@link Flixel#getDebugPauseKey()} (default F4) pauses the game; inspect camera with Alt+arrows, RMB pan, wheel zoom.
 */
public class FlixelDebugOverlay implements FlixelUpdatable, FlixelDestroyable, Disposable {

  private static final float[] FALLBACK_COLOR = { 1f, 0.2f, 0.2f, 0.6f };

  private static final String WATCH_PANEL_HEADER = "[#88CCFF]----------- Watch -----------";
  private static final String CONSOLE_BODY_LINE_PREFIX = "[#CCCCCC]  ";

  private final SpriteBatch batch;
  private final ShapeRenderer shapeRenderer;
  private final OrthographicCamera camera;
  private final ScreenViewport viewport;
  private final BitmapFont font;
  private final Texture whitePixel;

  private boolean visible = false;
  private boolean drawDebug = false;

  /** Prevents double-dispose if {@link #dispose()} and {@link #destroy()} are both used. */
  private boolean destroyed = false;

  private float statsTimer = 0f;

  /** Timer used to refresh watch values at a fixed rate (10 Hz). */
  private float watchRefreshTimer = 0f;

  /** Cached formatted watch lines refreshed at 10 Hz; buffers are reused across refreshes. */
  private final Array<FlixelString> cachedWatchLines = new Array<>();

  /**
   * Cached {@link FlixelDebugConsoleEntry} rendering; rebuilt at 10 Hz while the overlay is visible
   * so {@link FlixelDebugConsoleEntry#getConsoleLines()} is not hit every frame.
   */
  private final Array<CachedConsoleBlock> cachedConsoleBlocks = new Array<>();

  /** Pool of console blocks between rebuilds to avoid reallocating block objects. */
  private final Array<CachedConsoleBlock> cachedConsoleBlockPool = new Array<>();

  private final FlixelString lineFps = new FlixelString(32);
  private final FlixelString lineHeap = new FlixelString(48);
  private final FlixelString lineNative = new FlixelString(48);
  private final FlixelString lineObjects = new FlixelString(40);
  private final FlixelString lineTexVram = new FlixelString(56);
  private final FlixelString lineVisDbg = new FlixelString(48);
  private final FlixelString linePaused = new FlixelString(48);
  private final FlixelString lineCamInspect = new FlixelString(72);
  private final FlixelString lineKeybinds = new FlixelString(320);

  private int debugInspectCameraIndex;

  // Screen-space anchor for Alt+RMB pan (avoids mixing world unprojects across changing scroll).
  private int lastPanScreenX;
  private int lastPanScreenY;

  private final Vector2 panUnprojectA = new Vector2();
  private final Vector2 panUnprojectB = new Vector2();

  private boolean visDbgLineInitialized = false;
  private boolean lastDrawDebugForVisLine = false;

  private final Deque<BufferedLogLine> logBuffer = new ArrayDeque<>();
  private final Consumer<FlixelLogEntry> logListener = this::onLogEntry;

  /** Reused when formatting {@link FlixelLogEntry} lines for the log buffer. */
  private final FlixelString logMarkupScratch = new FlixelString(128);

  /** Mutable y-cursor used during watch panel rendering to avoid boxing a float. */
  private float watchDrawY;

  /** Constructs a new debug overlay. */
  public FlixelDebugOverlay() {
    batch = new SpriteBatch();
    shapeRenderer = new ShapeRenderer();
    camera = new OrthographicCamera();
    viewport = new ScreenViewport(camera);
    font = new BitmapFont();
    font.setColor(Color.WHITE);
    font.getData().markupEnabled = true;

    Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    px.setColor(Color.WHITE);
    px.fill();
    whitePixel = new Texture(px);
    px.dispose();
  }

  public Consumer<FlixelLogEntry> getLogListener() {
    return logListener;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    if (visible && !this.visible) {
      watchRefreshTimer = 0.11f;
    }
    this.visible = visible;
  }

  public void toggleVisible() {
    boolean was = visible;
    visible = !visible;
    if (visible && !was) {
      watchRefreshTimer = 0.11f;
      statsTimer = FlixelConstants.Debug.STATS_UPDATE_INTERVAL;
    }
  }

  public boolean isDrawDebug() {
    return drawDebug;
  }

  public void setDrawDebug(boolean drawDebug) {
    this.drawDebug = drawDebug;
  }

  public void toggleDrawDebug() {
    drawDebug = !drawDebug;
  }

  /**
   * Called every frame from the game loop to handle keybind input and refresh cached stats.
   *
   * @param elapsed Seconds since last frame.
   */
  @Override
  public void update(float elapsed) {
    if (Flixel.keys.justPressed(Flixel.getDebugToggleKey())) {
      toggleVisible();
    }

    if (Flixel.keys.justPressed(Flixel.getDebugDrawToggleKey())) {
      toggleDrawDebug();
    }

    if (Flixel.isDebugMode()) {
      if (Flixel.keys.justPressed(Flixel.getDebugPauseKey())) {
        Flixel.setPaused(!Flixel.isPaused());
      }
      if (Flixel.isPaused()) {
        handleInspectCameraTools();
      }
    }

    if (!visible) {
      return;
    }

    if (!visDbgLineInitialized || drawDebug != lastDrawDebugForVisLine) {
      visDbgLineInitialized = true;
      lastDrawDebugForVisLine = drawDebug;
      refreshVisDbgLine();
    }

    statsTimer += elapsed;
    watchRefreshTimer += elapsed;

    if (statsTimer >= FlixelConstants.Debug.STATS_UPDATE_INTERVAL) {
      statsTimer = 0f;
      int fps = Flixel.getFPS();
      float heapMB = Flixel.getJavaHeapUsedMegabytes();
      float nativeMB = Flixel.getNativeHeapUsedMegabytes();
      int objectCount = FlixelDebugUtil.countActiveMembers();
      float texMb = Flixel.getApproximateLoadedTextureBytes() / (1024f * 1024f);

      appendFpsLine(lineFps, fps);
      appendMbStatLine(lineHeap, "[#AAAAFF]Heap: ", heapMB);
      appendMbStatLine(lineNative, "[#AAAAFF]Native: ", nativeMB);
      appendObjectsLine(lineObjects, objectCount);
      appendMbStatLine(lineTexVram, "[#FFAA66]TexVRAM (approx.): ", texMb);
      appendPausedLine(linePaused);
      appendCamInspectLine(lineCamInspect);
      appendKeybindsLine(lineKeybinds);
    }

    // Refresh watch values at 10 Hz to avoid unnecessary per-frame allocations,
    // especially on mobile/TeaVM targets.
    if (watchRefreshTimer >= 0.1f) {
      watchRefreshTimer = 0f;

      FlixelDebugWatchManager mgr = Flixel.watch;
      if (mgr != null && !mgr.isEmpty()) {
        mgr.fillWatchLines(cachedWatchLines);
      } else {
        cachedWatchLines.clear();
      }

      rebuildCachedConsoleBlocks();
    }
  }

  private void refreshVisDbgLine() {
    lineVisDbg.clear();
    if (drawDebug) {
      lineVisDbg.concat("[#CCCCCC]VisDbg: [#00FF00]ON");
    } else {
      lineVisDbg.concat("[#CCCCCC]VisDbg: [#FF4444]OFF");
    }
  }

  private void handleInspectCameraTools() {
    if (Flixel.mouse == null) {
      return;
    }
    Array<FlixelCamera> cams = Flixel.getCamerasArray();
    if (cams == null || cams.size == 0) {
      return;
    }
    if (debugInspectCameraIndex < 0 || debugInspectCameraIndex >= cams.size) {
      debugInspectCameraIndex = 0;
    }
    boolean alt = Flixel.keys.pressed(FlixelKey.ALT_LEFT) || Flixel.keys.pressed(FlixelKey.ALT_RIGHT)
      || Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
    if (alt && Flixel.keys.justPressed(Flixel.getDebugCameraCycleLeftKey())) {
      debugInspectCameraIndex = (debugInspectCameraIndex - 1 + cams.size) % cams.size;
    }
    if (alt && Flixel.keys.justPressed(Flixel.getDebugCameraCycleRightKey())) {
      debugInspectCameraIndex = (debugInspectCameraIndex + 1) % cams.size;
    }
    FlixelCamera cam = cams.get(debugInspectCameraIndex);
    float scrollDelta = Flixel.mouse.getScrollDeltaY();
    if (scrollDelta != 0f) {
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
    if (Flixel.mouse.pressed(Flixel.getDebugCameraPanButton())) {
      int sx = Flixel.mouse.getScreenX();
      int sy = Flixel.mouse.getScreenY();
      if (Flixel.mouse.justPressed(Flixel.getDebugCameraPanButton())) {
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

  private void appendCamInspectLine(FlixelString line) {
    line.clear();
    Array<FlixelCamera> cams = Flixel.getCamerasArray();
    int n = cams != null ? cams.size : 0;
    line.concat("[#CCCCCC]Cameras: ").concat(n).concat("  Inspect: ");
    if (n == 0) {
      line.concat('-');
      return;
    }
    int idx = Math.min(debugInspectCameraIndex, n - 1) + 1;
    line.concat(idx).concat('/').concat(n);
  }

  private static void appendPausedLine(FlixelString line) {
    line.clear();
    line.concat("[#CCCCCC]Update: ");
    if (Flixel.isPaused()) {
      line.concat("[#FFAA00]PAUSED");
    } else {
      line.concat("[#00FF88]RUNNING");
    }
  }

  private static void appendKeybindsLine(FlixelString line) {
    line.clear();
    line.concat("[#AAAAAA]");
    line.concat(Input.Keys.toString(Flixel.getDebugToggleKey())).concat("=ov ");
    line.concat(Input.Keys.toString(Flixel.getDebugDrawToggleKey())).concat("=hit ");
    line.concat(Input.Keys.toString(Flixel.getDebugPauseKey())).concat("=pause ");
    line.concat("Alt+");
    line.concat(Input.Keys.toString(Flixel.getDebugCameraCycleLeftKey())).concat('/');
    line.concat(Input.Keys.toString(Flixel.getDebugCameraCycleRightKey())).concat("=cam ");
    line.concat("RMB=pan Wh=zoom");
  }

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
      block.header.clear();
      block.header.concat("[#AADDFF]<").concat(entry.getName()).concat('>');
      int n = lines.length;
      block.ensureBodyLineCount(n);
      for (int i = 0; i < n; i++) {
        block.bodies[i].clear();
        block.bodies[i].concat(CONSOLE_BODY_LINE_PREFIX).concat(lines[i]);
      }
      block.bodyCount = n;
      cachedConsoleBlocks.add(block);
    }
  }

  private static void appendLogMarkup(FlixelString line, FlixelLogEntry e) {
    line.clear();
    switch (e.level()) {
      case INFO -> line.concat("[#CCCCCC]");
      case WARN -> line.concat("[#FFFF00]");
      case ERROR -> line.concat("[#FF4444]");
    }
    line.concat('[').concat(e.level().name()).concat("] ");
    if (!e.tag().isEmpty()) {
      line.concat('[').concat(e.tag()).concat("] ");
    }
    line.concat(e.message());
  }

  private static void appendFpsLine(FlixelString line, int fps) {
    line.clear();
    line.concat("[#00FF00]FPS: ").concat(fps);
  }

  private static void appendObjectsLine(FlixelString line, int count) {
    line.clear();
    line.concat("[#FFFF00]Objects: ").concat(count);
  }

  private static void appendMbStatLine(FlixelString line, String prefix, float megabytes) {
    line.clear();
    line.concat(prefix).concatFloatRoundedOneDecimal(megabytes).concat(" MB");
  }

  /**
   * Draws bounding boxes for all visible {@link me.stringdotjar.flixelgdx.debug.FlixelDebugDrawable}
   * instances using each game camera's projection. Each object provides its own debug
   * color via {@link me.stringdotjar.flixelgdx.debug.FlixelDebugDrawable#getDebugBoundingBoxColor()}.
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
      // Ensure bounding boxes render inside each camera's split region.
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
          // Skip if the object is not projected to the current camera.
          boolean found = false;
          FlixelCamera[] list = basic.cameras;
          if (list == null || list.length == 0) { // Null/empty means the object is projected to all cameras.
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
        if (c == null || c.length < 4) c = FALLBACK_COLOR;
        shapeRenderer.setColor(c[0], c[1], c[2], c[3]);
        shapeRenderer.rect(drawable.getDebugDrawX(cam), drawable.getDebugDrawY(cam),
          drawable.getDebugWidth(), drawable.getDebugHeight());
      });
      shapeRenderer.end();
    }

    // Make sure the debug overlay UI that draws after this method isn't clipped by any other camera's viewport.
    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    viewport.apply();

    Gdx.gl.glDisable(GL20.GL_BLEND);
  }

  /**
   * Draws the overlay panels (stats, log console, watch list) on top of everything using
   * the overlay's own batch and camera. Called from
   * {@link me.stringdotjar.flixelgdx.FlixelGame#draw(Batch)} after the game stage is drawn.
   */
  public void draw() {
    if (!visible) {
      return;
    }

    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    camera.update();

    batch.setProjectionMatrix(camera.combined);
    batch.begin();

    float screenW = Gdx.graphics.getWidth();
    float screenH = Gdx.graphics.getHeight();
    float pad = 6f;
    float lineH = font.getLineHeight() + 2f;

    float y = screenH - pad;
    drawStatsPanel(pad, y, lineH);
    drawWatchPanel(screenW - pad, screenH - pad, lineH);
    drawLogConsole(pad, lineH, screenH);

    batch.end();
  }

  private float drawStatsPanel(float x, float y, float lineH) {
    drawText(lineFps, x, y);
    y -= lineH;
    drawText(lineHeap, x, y);
    y -= lineH;
    drawText(lineNative, x, y);
    y -= lineH;
    drawText(lineObjects, x, y);
    y -= lineH;
    drawText(lineTexVram, x, y);
    y -= lineH;
    drawText(linePaused, x, y);
    y -= lineH;
    drawText(lineCamInspect, x, y);
    y -= lineH;
    drawText(lineKeybinds, x, y);
    y -= lineH;
    drawText(lineVisDbg, x, y);
    return y;
  }

  private float drawWatchPanel(float rightEdge, float y, float lineH) {
    FlixelDebugWatchManager mgr = Flixel.watch;
    if (mgr == null || mgr.isEmpty() || cachedWatchLines.isEmpty()) {
      return y;
    }
    drawTextRight(WATCH_PANEL_HEADER, rightEdge, y);
    y -= lineH;

    watchDrawY = y;
    for (int i = 0; i < cachedWatchLines.size; i++) {
      FlixelString line = cachedWatchLines.get(i);
      if (line == null) {
        continue;
      }
      drawTextRight(line, rightEdge, watchDrawY);
      watchDrawY -= lineH;
    }

    return watchDrawY;
  }

  private void drawLogConsole(float x, float lineH, float screenH) {
    float consoleTop = screenH * 0.35f;
    float consoleBottom = lineH + 4f;

    // Draw custom console entries using strings rebuilt at 10 Hz in {@link #update(float)}.
    float entriesBottom = consoleTop;
    float ey = consoleTop;
    for (int bi = 0; bi < cachedConsoleBlocks.size; bi++) {
      CachedConsoleBlock block = cachedConsoleBlocks.get(bi);
      if (block == null || block.header == null || block.bodies == null) {
        continue;
      }
      drawText(block.header, x, ey);
      ey -= lineH;
      for (int li = 0; li < block.bodyCount; li++) {
        FlixelString bodyLine = block.bodies[li];
        if (bodyLine != null) {
          drawText(bodyLine, x, ey);
        }
        ey -= lineH;
        if (ey < consoleBottom) {
          break;
        }
      }
      if (ey < consoleBottom) {
        break;
      }
    }
    entriesBottom = ey;

    // Draw log entries newest-at-bottom: markup was computed once in {@link #onLogEntry}.
    synchronized (logBuffer) {
      float y = consoleBottom;
      Iterator<BufferedLogLine> it = logBuffer.descendingIterator();
      while (it.hasNext()) {
        if (y > entriesBottom) {
          break;
        }
        drawText(it.next().markup, x, y);
        y += lineH;
      }
    }
  }

  private void drawText(CharSequence markup, float x, float y) {
    font.draw(batch, markup, x, y);
  }

  private void drawTextRight(CharSequence markup, float rightEdge, float y) {
    font.draw(batch, markup, 0, y, rightEdge, Align.right, false);
  }

  private void onLogEntry(FlixelLogEntry entry) {
    synchronized (logBuffer) {
      synchronized (logMarkupScratch) {
        appendLogMarkup(logMarkupScratch, entry);
        logBuffer.addLast(new BufferedLogLine(new FlixelString(logMarkupScratch)));
      }
      while (logBuffer.size() > FlixelConstants.Debug.MAX_LOG_ENTRIES) {
        logBuffer.removeFirst();
      }
    }
  }

  /** Call from {@link me.stringdotjar.flixelgdx.FlixelGame#resize(int, int)} to keep the overlay viewport in sync. */
  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  @Override
  public void destroy() {
    if (destroyed) {
      return;
    }
    destroyed = true;
    batch.dispose();
    shapeRenderer.dispose();
    font.dispose();
    whitePixel.dispose();
  }

  @Override
  public void dispose() {
    destroy();
  }

  /** One colored line in the log console; markup is built once when the log is received. */
  private static final class BufferedLogLine {
    final FlixelString markup;

    BufferedLogLine(FlixelString markup) {
      this.markup = markup;
    }
  }

  /** Cached block for {@link FlixelDebugConsoleEntry} output (rebuilt at 10 Hz). */
  private static final class CachedConsoleBlock {
    final FlixelString header = new FlixelString(64);
    FlixelString[] bodies = new FlixelString[0];
    int bodyCount;

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
