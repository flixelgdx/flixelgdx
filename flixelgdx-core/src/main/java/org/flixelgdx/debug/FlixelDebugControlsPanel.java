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
import org.flixelgdx.FlixelGame;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelDebugUtil;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The remote control of the debugger: pause the game, step one frame, show hitboxes, and tune the
 * debugger itself.
 *
 * <p>Think of it as the remote for a video player. The pause button freezes the picture, the step button
 * moves it forward by exactly one frame, and the other buttons change how the picture is shown
 * (hitboxes) or how often the debugger looks at it (the sample rate). The remote works from the
 * debugger app, from the console, and from the keyboard, and it keeps working when no app is connected.
 *
 * <p>Default hotkeys, all configurable:
 * <ul>
 *   <li>F3 pauses and resumes the game ({@link #setPauseKey(int)}).</li>
 *   <li>F2 shows and hides hitboxes ({@link #setHitboxesKey(int)}).</li>
 * </ul>
 * F1 and F4 are reserved for future debugger features.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.controls.setPaused(true);
 * Flixel.debug.controls.stepFrame();      // The next frame runs, then the game stays paused.
 * Flixel.debug.controls.setHitboxes(true);
 * Flixel.debug.controls.setPauseKey(FlixelKey.P);
 * }</pre>
 *
 * @see FlixelDebugProtocol#KIND_CONTROLS
 */
public class FlixelDebugControlsPanel extends FlixelDebugPanelBase {

  /** The sample rate in Hz that the stats panel uses until it is changed. */
  public static final int DEFAULT_RATE = 20;

  /** The lowest allowed sample rate in Hz. */
  public static final int MIN_RATE = 1;

  /** The highest allowed sample rate in Hz. */
  public static final int MAX_RATE = 30;

  private static final float[] FALLBACK_BOUNDING_BOX_COLOR = { 1f, 0.2f, 0.2f, 0.6f };

  private final FlixelColor boundingBoxColor = new FlixelColor();
  private final Consumer<FlixelDebugDrawable> hitboxDrawer = this::drawHitbox;

  @Nullable
  private FlixelBatch hitboxBatch;

  @Nullable
  private FlixelFrame hitboxPixel;

  @Nullable
  private FlixelCamera hitboxCamera;

  @Nullable
  private FlixelLogLevel savedLogLevel;

  private String pauseKeyName = FlixelKey.toString(FlixelKey.F3);
  private String hitboxesKeyName = FlixelKey.toString(FlixelKey.F2);

  private float hitboxThickness;

  private int pauseKey = FlixelKey.F3;
  private int hitboxesKey = FlixelKey.F2;
  private int rate = DEFAULT_RATE;
  private int logLevel = FlixelDebugProtocol.LEVEL_DEFAULT;
  private int syncedRate = -1;
  private int syncedLogLevel = -1;

  private boolean hitboxes;
  private boolean freeCam;
  private boolean syncedPaused;
  private boolean syncedHitboxes;
  private boolean syncedFreeCam;

  /** Creates the controls panel. */
  public FlixelDebugControlsPanel() {
    super("controls", "Controls", FlixelDebugProtocol.KIND_CONTROLS);
  }

  @Override
  public void update(float elapsed) {
    FlixelKeyInputManager keys = Flixel.keys;
    if (keys != null) {
      if (pauseKey != FlixelKey.NONE && keys.justPressed(pauseKey)) {
        setPaused(!isPaused());
      }
      if (hitboxesKey != FlixelKey.NONE && keys.justPressed(hitboxesKey)) {
        setHitboxes(!hitboxes);
      }
    }
    boolean paused = isPaused();
    if (paused != syncedPaused || hitboxes != syncedHitboxes || freeCam != syncedFreeCam
        || rate != syncedRate || logLevel != syncedLogLevel) {
      syncedPaused = paused;
      syncedHitboxes = hitboxes;
      syncedFreeCam = freeCam;
      syncedRate = rate;
      syncedLogLevel = logLevel;
      markChanged();
    }
  }

  /**
   * Lets the paused game run exactly one more frame.
   *
   * @return {@code true} when a frame was scheduled, or {@code false} when the game is not paused.
   */
  public boolean stepFrame() {
    FlixelGame game = Flixel.game;
    if (game == null || !game.isGamePaused()) {
      return false;
    }
    game.stepFrame();
    return true;
  }

  /**
   * Draws an outline around every visible {@link FlixelDebugDrawable} when hitboxes are on.
   *
   * <p>The game calls this once per frame after the scene has been drawn. Each object chooses its own
   * outline color through {@link FlixelDebugDrawable#getDebugBoundingBoxColor()}. It does nothing while
   * {@link #isHitboxes()} is {@code false}, and it does not allocate.
   *
   * @param cameras The game's cameras. Empty slots are skipped.
   */
  public void drawBoundingBoxes(@NotNull FlixelCamera[] cameras) {
    if (!hitboxes) {
      return;
    }
    FlixelBatch batch = Flixel.graphics.getBatch();
    hitboxBatch = batch;
    hitboxPixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);
    try {
      for (int i = 0; i < cameras.length; i++) {
        FlixelCamera cam = cameras[i];
        if (cam == null) {
          continue;
        }
        Flixel.graphics.beginCameraPass();
        cam.applyViewport();
        batch.setProjection(cam.getCombinedMatrix());
        batch.begin();
        // Scale the outline by the inverse zoom so it keeps a constant width on screen.
        hitboxThickness = Math.max(1f, 1f / Math.max(0.0001f, cam.getZoom()));
        hitboxCamera = cam;
        FlixelDebugUtil.forEachDebugDrawable(hitboxDrawer);
        batch.end();
      }
    } finally {
      hitboxBatch = null;
      hitboxPixel = null;
      hitboxCamera = null;
    }
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeBool(isPaused())
        .writeBool(hitboxes)
        .writeBool(freeCam)
        .writeU8(rate)
        .writeU8(logLevel);
    int count = 0;
    writer.beginArray();
    if (pauseKey != FlixelKey.NONE) {
      writer.writeString(pauseKeyName).writeString("Pause or resume the game");
      count++;
    }
    if (hitboxesKey != FlixelKey.NONE) {
      writer.writeString(hitboxesKeyName).writeString("Show or hide hitboxes");
      count++;
    }
    writer.endArray(count);
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    switch (action) {
      case FlixelDebugProtocol.CONTROLS_SET_PAUSED -> setPaused(reader.readBool());
      case FlixelDebugProtocol.CONTROLS_STEP -> stepFrame();
      case FlixelDebugProtocol.CONTROLS_SET_HITBOXES -> setHitboxes(reader.readBool());
      case FlixelDebugProtocol.CONTROLS_SET_FREECAM -> setFreeCam(reader.readBool());
      case FlixelDebugProtocol.CONTROLS_SET_RATE -> setRate(reader.readU8());
      case FlixelDebugProtocol.CONTROLS_SET_LOG_LEVEL -> setLogLevel(reader.readU8());
      default -> {
      }
    }
  }

  @Override
  public void reset() {
    hitboxes = false;
    freeCam = false;
    rate = DEFAULT_RATE;
    setLogLevel(FlixelDebugProtocol.LEVEL_DEFAULT);
    markChanged();
  }

  private void drawHitbox(FlixelDebugDrawable drawable) {
    FlixelCamera cam = hitboxCamera;
    FlixelBatch batch = hitboxBatch;
    FlixelFrame pixel = hitboxPixel;
    if (drawable == null || cam == null || batch == null || pixel == null) {
      return;
    }
    // An object is only outlined on cameras it is drawn on. An empty camera list means "every camera
    // whose defaultDrawTarget is true".
    if (drawable instanceof FlixelBasic basic) {
      FlixelCamera[] list = basic.cameras;
      boolean found = false;
      if (list == null || list.length == 0) {
        found = cam.defaultDrawTarget;
      } else {
        for (int i = 0; i < list.length; i++) {
          if (list[i] == cam) {
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
    FlixelSpriteUtil.drawBorder(batch, pixel, drawable.getDebugDrawX(cam), drawable.getDebugDrawY(cam),
        drawable.getDebugWidth(), drawable.getDebugHeight(), hitboxThickness, boundingBoxColor);
  }

  /**
   * Converts a protocol log level code into a logger level.
   *
   * @param code A {@code LEVEL_*} constant of {@link FlixelDebugProtocol}.
   * @return The matching level, or {@code null} for {@link FlixelDebugProtocol#LEVEL_DEFAULT} and any
   *   unknown code.
   */
  @Nullable
  public static FlixelLogLevel toLogLevel(int code) {
    return switch (code) {
      case FlixelDebugProtocol.LEVEL_DEBUG -> FlixelLogLevel.DEBUG;
      case FlixelDebugProtocol.LEVEL_INFO -> FlixelLogLevel.INFO;
      case FlixelDebugProtocol.LEVEL_WARN -> FlixelLogLevel.WARN;
      case FlixelDebugProtocol.LEVEL_ERROR -> FlixelLogLevel.ERROR;
      default -> null;
    };
  }

  /**
   * Returns whether the game is paused.
   *
   * @return {@code true} when a game exists and its update loop is paused.
   */
  public boolean isPaused() {
    FlixelGame game = Flixel.game;
    return game != null && game.isGamePaused();
  }

  /**
   * Pauses or resumes the game. Does nothing when no game is running.
   *
   * @param paused {@code true} to pause.
   */
  public void setPaused(boolean paused) {
    FlixelGame game = Flixel.game;
    if (game != null) {
      game.setGamePaused(paused);
    }
  }

  public boolean isHitboxes() {
    return hitboxes;
  }

  public void setHitboxes(boolean hitboxes) {
    this.hitboxes = hitboxes;
  }

  public boolean isFreeCam() {
    return freeCam;
  }

  /**
   * Turns the free camera flag on or off. The flag only records the request; the free camera itself is
   * driven by a separate feature that reads it.
   *
   * @param freeCam {@code true} to request the free camera.
   */
  public void setFreeCam(boolean freeCam) {
    this.freeCam = freeCam;
  }

  public int getRate() {
    return rate;
  }

  /**
   * Sets how many times per second the stats panel samples.
   *
   * @param rate The rate in Hz. Values outside {@value #MIN_RATE} to {@value #MAX_RATE} are clamped.
   */
  public void setRate(int rate) {
    this.rate = Math.max(MIN_RATE, Math.min(MAX_RATE, rate));
  }

  public int getLogLevel() {
    return logLevel;
  }

  /**
   * Overrides the minimum level of {@link Flixel#log}, or restores the game's own level.
   *
   * <p>The first override remembers the level the logger had, and
   * {@link FlixelDebugProtocol#LEVEL_DEFAULT} puts it back.
   *
   * @param level A {@code LEVEL_*} constant of {@link FlixelDebugProtocol}. Unknown codes are ignored.
   */
  public void setLogLevel(int level) {
    if (level == FlixelDebugProtocol.LEVEL_DEFAULT) {
      FlixelLogLevel saved = savedLogLevel;
      if (saved != null) {
        Flixel.log.setLevel(saved);
      }
      savedLogLevel = null;
      logLevel = FlixelDebugProtocol.LEVEL_DEFAULT;
      return;
    }
    FlixelLogLevel target = toLogLevel(level);
    if (target == null) {
      return;
    }
    if (savedLogLevel == null) {
      savedLogLevel = Flixel.log.getLevel();
    }
    Flixel.log.setLevel(target);
    logLevel = level;
  }

  public int getPauseKey() {
    return pauseKey;
  }

  /**
   * Sets the key that pauses and resumes the game.
   *
   * @param pauseKey A {@link FlixelKey} code, or {@link FlixelKey#NONE} to disable the hotkey.
   */
  public void setPauseKey(int pauseKey) {
    this.pauseKey = pauseKey;
    pauseKeyName = FlixelKey.toString(pauseKey);
    markChanged();
  }

  public int getHitboxesKey() {
    return hitboxesKey;
  }

  /**
   * Sets the key that shows and hides hitboxes.
   *
   * @param hitboxesKey A {@link FlixelKey} code, or {@link FlixelKey#NONE} to disable the hotkey.
   */
  public void setHitboxesKey(int hitboxesKey) {
    this.hitboxesKey = hitboxesKey;
    hitboxesKeyName = FlixelKey.toString(hitboxesKey);
    markChanged();
  }
}
