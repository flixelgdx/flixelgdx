package me.stringdotjar.flixelgdx.debug;

import com.badlogic.gdx.Gdx;
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
import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.FlixelUpdatable;
import me.stringdotjar.flixelgdx.display.FlixelCamera;
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

  /** Cached formatted watch lines refreshed at 10 Hz to avoid per-frame allocations. */
  private final Array<String> cachedWatchLines = new Array<>();

  /**
   * Cached {@link FlixelDebugConsoleEntry} rendering; rebuilt at 10 Hz while the overlay is visible
   * so {@link FlixelDebugConsoleEntry#getConsoleLines()} is not hit every frame.
   */
  private final Array<CachedConsoleBlock> cachedConsoleBlocks = new Array<>();

  private String cachedFpsText = "";
  private String cachedHeapText = "";
  private String cachedNativeText = "";
  private String cachedObjectsText = "";
  private String cachedVisDbgText = "[#CCCCCC]VisDbg: [#FF4444]OFF";

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

    if (!visible) {
      return;
    }

    if (!visDbgLineInitialized || drawDebug != lastDrawDebugForVisLine) {
      visDbgLineInitialized = true;
      lastDrawDebugForVisLine = drawDebug;
      cachedVisDbgText = drawDebug
        ? "[#CCCCCC]VisDbg: [#00FF00]ON"
        : "[#CCCCCC]VisDbg: [#FF4444]OFF";
    }

    statsTimer += elapsed;
    watchRefreshTimer += elapsed;

    if (statsTimer >= FlixelConstants.Debug.STATS_UPDATE_INTERVAL) {
      statsTimer = 0f;
      int fps = Flixel.getFPS();
      float heapMB = Flixel.getJavaHeapUsedMegabytes();
      float nativeMB = Flixel.getNativeHeapUsedMegabytes();
      int objectCount = FlixelDebugUtil.countActiveMembers();

      cachedFpsText = "[#00FF00]FPS: " + fps;
      cachedHeapText = "[#AAAAFF]Heap: " + formatOneDecimal(heapMB) + " MB";
      cachedNativeText = "[#AAAAFF]Native: " + formatOneDecimal(nativeMB) + " MB";
      cachedObjectsText = "[#FFFF00]Objects: " + objectCount;
    }

    // Refresh watch values at 10 Hz to avoid unnecessary per-frame allocations,
    // especially on mobile/TeaVM targets.
    if (watchRefreshTimer >= 0.1f) {
      watchRefreshTimer = 0f;
      cachedWatchLines.clear();

      FlixelDebugWatchManager mgr = Flixel.watch;
      if (mgr != null && !mgr.isEmpty()) {
        mgr.forEach((name, value) -> {
          cachedWatchLines.add("[#88CCFF]" + name + ":[#FFFFFF] " + value);
        });
      }

      rebuildCachedConsoleBlocks();
    }
  }

  private void rebuildCachedConsoleBlocks() {
    cachedConsoleBlocks.clear();
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
      CachedConsoleBlock block = new CachedConsoleBlock();
      block.headerMarkup = "[#AADDFF]<" + entry.getName() + ">";
      int n = lines.size();
      block.bodyMarkups = new String[n];
      for (int i = 0; i < n; i++) {
        block.bodyMarkups[i] = CONSOLE_BODY_LINE_PREFIX + lines.get(i);
      }
      cachedConsoleBlocks.add(block);
    }
  }

  private String buildLogMarkup(FlixelLogEntry e) {
    synchronized (logMarkupScratch) {
      logMarkupScratch.setLength(0);
      switch (e.level()) {
        case INFO -> logMarkupScratch.append("[#CCCCCC]");
        case WARN -> logMarkupScratch.append("[#FFFF00]");
        case ERROR -> logMarkupScratch.append("[#FF4444]");
      }
      logMarkupScratch.append('[').append(e.level().name()).append("] ");
      if (!e.tag().isEmpty()) {
        logMarkupScratch.append('[').append(e.tag()).append("] ");
      }
      logMarkupScratch.append(e.message());
      return logMarkupScratch.toString();
    }
  }

  private static String formatOneDecimal(float value) {
    // Avoid String.format/Formatter allocations.
    float rounded = Math.round(value * 10f) / 10f;
    String s = Float.toString(rounded);
    int dot = s.indexOf('.');
    if (dot < 0) {
      return s + ".0";
    }
    // Ensure exactly one decimal digit.
    int decimals = s.length() - dot - 1;
    if (decimals == 0) {
      return s + "0";
    }
    if (decimals > 1) {
      return s.substring(0, dot + 2);
    }
    return s;
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
        if (drawable instanceof FlixelSprite sprite) {
          // Skip if the object is not projected to the current camera.
          boolean found = false;
          if (sprite.cameras == null) { // Null means the object is projected to all cameras.
            found = true;
          } else {
            for (FlixelCamera c : sprite.cameras) {
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
        shapeRenderer.rect(drawable.getDebugX(), drawable.getDebugY(),
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
   * {@link me.stringdotjar.flixelgdx.FlixelGame#draw()} after the game stage is drawn.
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
    drawText(cachedFpsText, x, y);
    y -= lineH;
    drawText(cachedHeapText, x, y);
    y -= lineH;
    drawText(cachedNativeText, x, y);
    y -= lineH;
    drawText(cachedObjectsText, x, y);
    y -= lineH;
    drawText(cachedVisDbgText, x, y);
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
      String line = cachedWatchLines.get(i);
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
      if (block == null || block.headerMarkup == null || block.bodyMarkups == null) {
        continue;
      }
      drawText(block.headerMarkup, x, ey);
      ey -= lineH;
      for (String bodyLine : block.bodyMarkups) {
        drawText(bodyLine, x, ey);
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

  private void drawText(String markup, float x, float y) {
    font.draw(batch, markup, x, y);
  }

  private void drawTextRight(String markup, float rightEdge, float y) {
    font.draw(batch, markup, 0, y, rightEdge, Align.right, false);
  }

  private void onLogEntry(FlixelLogEntry entry) {
    synchronized (logBuffer) {
      logBuffer.addLast(new BufferedLogLine(buildLogMarkup(entry)));
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
    final String markup;

    BufferedLogLine(String markup) {
      this.markup = markup;
    }
  }

  /** Cached block for {@link FlixelDebugConsoleEntry} output (rebuilt at 10 Hz). */
  private static final class CachedConsoleBlock {
    String headerMarkup;
    String[] bodyMarkups;
  }
}
