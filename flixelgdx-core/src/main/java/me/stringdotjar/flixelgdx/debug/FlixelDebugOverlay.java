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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
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

  /** Cached formatted watch lines refreshed at 10 Hz; builders are reused across refreshes. */
  private final Array<StringBuilder> cachedWatchLines = new Array<>();

  /**
   * Cached {@link FlixelDebugConsoleEntry} rendering; rebuilt at 10 Hz while the overlay is visible
   * so {@link FlixelDebugConsoleEntry#getConsoleLines()} is not hit every frame.
   */
  private final Array<CachedConsoleBlock> cachedConsoleBlocks = new Array<>();

  /** Pool of console blocks between rebuilds to avoid reallocating block objects. */
  private final Array<CachedConsoleBlock> cachedConsoleBlockPool = new Array<>();

  private final StringBuilder lineFps = new StringBuilder(32);
  private final StringBuilder lineHeap = new StringBuilder(48);
  private final StringBuilder lineNative = new StringBuilder(48);
  private final StringBuilder lineObjects = new StringBuilder(40);
  private final StringBuilder lineTexVram = new StringBuilder(56);
  private final StringBuilder lineVisDbg = new StringBuilder(48);
  private final StringBuilder linePaused = new StringBuilder(48);
  private final StringBuilder lineCamInspect = new StringBuilder(72);
  private final StringBuilder lineKeybinds = new StringBuilder(320);

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

  /** Reused when formatting {@link FlixelLogEntry} lines for the log buffer (one allocation per new log line). */
  private final StringBuilder logMarkupScratch = new StringBuilder(128);

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
        mgr.fillWatchLineBuilders(cachedWatchLines);
      } else {
        cachedWatchLines.clear();
      }

      rebuildCachedConsoleBlocks();
    }
  }

  private void refreshVisDbgLine() {
    lineVisDbg.setLength(0);
    if (drawDebug) {
      lineVisDbg.append("[#CCCCCC]VisDbg: [#00FF00]ON");
    } else {
      lineVisDbg.append("[#CCCCCC]VisDbg: [#FF4444]OFF");
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

  private void appendCamInspectLine(StringBuilder out) {
    out.setLength(0);
    Array<FlixelCamera> cams = Flixel.getCamerasArray();
    int n = cams != null ? cams.size : 0;
    out.append("[#CCCCCC]Cameras: ").append(n).append("  Inspect: ");
    if (n == 0) {
      out.append('-');
      return;
    }
    int idx = Math.min(debugInspectCameraIndex, n - 1) + 1;
    out.append(idx).append('/').append(n);
  }

  private static void appendPausedLine(StringBuilder out) {
    out.setLength(0);
    out.append("[#CCCCCC]Update: ");
    if (Flixel.isPaused()) {
      out.append("[#FFAA00]PAUSED");
    } else {
      out.append("[#00FF88]RUNNING");
    }
  }

  private static void appendKeybindsLine(StringBuilder out) {
    out.setLength(0);
    out.append("[#AAAAAA]");
    out.append(Input.Keys.toString(Flixel.getDebugToggleKey())).append("=ov ");
    out.append(Input.Keys.toString(Flixel.getDebugDrawToggleKey())).append("=hit ");
    out.append(Input.Keys.toString(Flixel.getDebugPauseKey())).append("=pause ");
    out.append("Alt+");
    out.append(Input.Keys.toString(Flixel.getDebugCameraCycleLeftKey())).append('/');
    out.append(Input.Keys.toString(Flixel.getDebugCameraCycleRightKey())).append("=cam ");
    out.append("RMB=pan Wh=zoom");
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
    List<FlixelDebugConsoleEntry> entries = logger.getConsoleEntries();
    if (entries == null || entries.isEmpty()) {
      return;
    }
    for (FlixelDebugConsoleEntry entry : entries) {
      List<String> lines = entry.getConsoleLines();
      if (lines == null || lines.isEmpty()) {
        continue;
      }
      CachedConsoleBlock block = obtainConsoleBlock();
      StringBuilder header = block.header;
      header.setLength(0);
      header.append("[#AADDFF]<").append(entry.getName()).append('>');
      int n = lines.size();
      block.ensureBodyLineCount(n);
      for (int i = 0; i < n; i++) {
        StringBuilder body = block.bodies[i];
        body.setLength(0);
        body.append(CONSOLE_BODY_LINE_PREFIX).append(lines.get(i));
      }
      block.bodyCount = n;
      cachedConsoleBlocks.add(block);
    }
  }

  private static void appendLogMarkup(StringBuilder out, FlixelLogEntry e) {
    out.setLength(0);
    switch (e.level()) {
      case INFO -> out.append("[#CCCCCC]");
      case WARN -> out.append("[#FFFF00]");
      case ERROR -> out.append("[#FF4444]");
    }
    out.append('[').append(e.level().name()).append("] ");
    if (!e.tag().isEmpty()) {
      out.append('[').append(e.tag()).append("] ");
    }
    out.append(e.message());
  }

  private static void appendFpsLine(StringBuilder out, int fps) {
    out.setLength(0);
    out.append("[#00FF00]FPS: ").append(fps);
  }

  private static void appendObjectsLine(StringBuilder out, int count) {
    out.setLength(0);
    out.append("[#FFFF00]Objects: ").append(count);
  }

  private static void appendMbStatLine(StringBuilder out, String prefix, float megabytes) {
    out.setLength(0);
    out.append(prefix);
    appendOneDecimal(out, megabytes);
    out.append(" MB");
  }

  /**
   * Appends {@code value} rounded to one decimal place (no {@link String#format} / substring churn).
   */
  private static void appendOneDecimal(StringBuilder out, float value) {
    float rounded = Math.round(value * 10f) / 10f;
    String s = Float.toString(rounded);
    int dot = s.indexOf('.');
    if (dot < 0) {
      out.append(s).append(".0");
      return;
    }
    int decimals = s.length() - dot - 1;
    if (decimals == 0) {
      out.append(s).append('0');
    } else if (decimals > 1) {
      out.append(s, 0, dot + 2);
    } else {
      out.append(s);
    }
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
      StringBuilder line = cachedWatchLines.get(i);
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
        StringBuilder bodyLine = block.bodies[li];
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
        drawText(it.next().markupBuilder, x, y);
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
        StringBuilder lineBuf = new StringBuilder(logMarkupScratch.length());
        lineBuf.append(logMarkupScratch);
        logBuffer.addLast(new BufferedLogLine(lineBuf));
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
    final StringBuilder markupBuilder;

    BufferedLogLine(StringBuilder markupBuilder) {
      this.markupBuilder = markupBuilder;
    }
  }

  /** Cached block for {@link FlixelDebugConsoleEntry} output (rebuilt at 10 Hz). */
  private static final class CachedConsoleBlock {
    final StringBuilder header = new StringBuilder(64);
    StringBuilder[] bodies = new StringBuilder[0];
    int bodyCount;

    void ensureBodyLineCount(int n) {
      if (bodies.length < n) {
        StringBuilder[] nb = new StringBuilder[n];
        System.arraycopy(bodies, 0, nb, 0, bodies.length);
        for (int i = bodies.length; i < n; i++) {
          nb[i] = new StringBuilder(96);
        }
        bodies = nb;
      }
    }
  }
}
