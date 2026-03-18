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
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.FlixelUpdatable;
import me.stringdotjar.flixelgdx.display.FlixelCamera;
import me.stringdotjar.flixelgdx.logging.FlixelDebugConsoleEntry;
import me.stringdotjar.flixelgdx.logging.FlixelLogEntry;
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

  private final float[] BOUNDINGBOX_COLOR_NORMAL = { 1f, 0.2f, 0.2f, 0.6f };
  private final float[] BOUNDINGBOX_COLOR_IMMOVABLE = { 0.2f, 0.9f, 0.2f, 0.6f };
  private final float[] BOUNDINGBOX_COLOR_NO_COLLISION = { 0.2f, 0.4f, 1f, 0.6f };

  private final SpriteBatch batch;
  private final ShapeRenderer shapeRenderer;
  private final OrthographicCamera camera;
  private final ScreenViewport viewport;
  private final BitmapFont font;
  private final Texture whitePixel;

  private boolean visible = false;
  private boolean drawDebug = false;

  private float statsTimer = 0f;

  private String cachedFpsText = "";
  private String cachedHeapText = "";
  private String cachedNativeText = "";
  private String cachedObjectsText = "";

  private final Deque<FlixelLogEntry> logBuffer = new ArrayDeque<>();
  private final Consumer<FlixelLogEntry> logListener = this::onLogEntry;

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
    this.visible = visible;
  }

  public void toggleVisible() {
    visible = !visible;
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

    statsTimer += elapsed;
    if (statsTimer >= FlixelConstants.Debug.STATS_UPDATE_INTERVAL) {
      statsTimer = 0f;
      int fps = Flixel.getFPS();
      float heapMB = Flixel.getJavaHeapUsedMegabytes();
      float nativeMB = Flixel.getNativeHeapUsedMegabytes();
      int objectCount = FlixelDebugUtil.countActiveMembers();

      cachedFpsText = "[#00FF00]FPS: " + fps;
      cachedHeapText = "[#AAAAFF]Heap: " + String.format("%.1f", heapMB) + " MB";
      cachedNativeText = "[#AAAAFF]Native: " + String.format("%.1f", nativeMB) + " MB";
      cachedObjectsText = "[#FFFF00]Objects: " + objectCount;
    }
  }

  /**
   * Draws bounding boxes for all visible {@link me.stringdotjar.flixelgdx.FlixelObject} instances
   * using each game camera's projection. Called from
   * {@link me.stringdotjar.flixelgdx.FlixelGame#draw()} when visual debug is enabled.
   *
   * @param cameras The game camera array.
   */
  public void drawBoundingBoxes(SnapshotArray<FlixelCamera> cameras) {
    if (!drawDebug) {
      return;
    }

    float[] c = BOUNDINGBOX_COLOR_NORMAL;

    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

    FlixelCamera[] cams = cameras.begin();
    for (int ci = 0; ci < cameras.size; ci++) {
      FlixelCamera cam = cams[ci];
      if (cam == null) {
        continue;
      }
      shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
      shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
      FlixelDebugUtil.forEachVisibleObject(obj -> {
        shapeRenderer.setColor(c[0], c[1], c[2], c[3]);
        shapeRenderer.rect(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
      });
      shapeRenderer.end();
    }
    cameras.end();

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
    drawLogConsole(pad, lineH, screenW, screenH);

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
    drawText("[#CCCCCC]VisDbg: " + (drawDebug ? "[#00FF00]ON" : "[#FF4444]OFF"), x, y);
    return y;
  }

  private float drawWatchPanel(float rightEdge, float y, float lineH) {
    FlixelDebugWatchManager mgr = Flixel.watch;
    if (mgr == null || mgr.isEmpty()) {
      return y;
    }
    drawTextRight("[#88CCFF]----------- Watch -----------", rightEdge, y);
    y -= lineH;

    watchDrawY = y;
    mgr.forEach((name, value) -> {
      drawTextRight("[#88CCFF]" + name + ":[#FFFFFF] " + value, rightEdge, watchDrawY);
      watchDrawY -= lineH;
    });

    return watchDrawY;
  }

  private void drawLogConsole(float x, float lineH, float screenW, float screenH) {
    float consoleTop = screenH * 0.35f;
    float consoleBottom = lineH + 4f;

    // Draw custom console entries from the top of the console area downward.
    float entriesBottom = consoleTop;
    if (Flixel.getLogger() != null) {
      float ey = consoleTop;
      List<FlixelDebugConsoleEntry> entries = Flixel.getLogger().getConsoleEntries();
      for (FlixelDebugConsoleEntry entry : entries) {
        List<String> lines = entry.getConsoleLines();
        if (lines == null || lines.isEmpty()) {
          continue;
        }
        drawText("[#AADDFF]<" + entry.getName() + ">", x, ey);
        ey -= lineH;
        for (String line : lines) {
          drawText("[#CCCCCC]  " + line, x, ey);
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
    }

    // Draw log entries newest-at-bottom: iterate from newest to oldest, drawing upward
    // from the bottom of the screen. This ensures the latest log is always visible.
    synchronized (logBuffer) {
      float y = consoleBottom;
      Iterator<FlixelLogEntry> it = logBuffer.descendingIterator();
      while (it.hasNext()) {
        if (y > entriesBottom) break;
        FlixelLogEntry entry = it.next();
        String colorTag = switch (entry.level()) {
          case INFO -> "[#CCCCCC]";
          case WARN -> "[#FFFF00]";
          case ERROR -> "[#FF4444]";
        };
        drawText(colorTag + entry.toString(), x, y);
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
      logBuffer.addLast(entry);
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
    batch.dispose();
    shapeRenderer.dispose();
    font.dispose();
    whitePixel.dispose();
  }

  @Override
  public void dispose() {
    destroy();
  }
}
