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
package org.flixelgdx.debug.panels;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.debug.FlixelDebugDrawable;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugPicker;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelDebugUtil;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The remote control of the debugger: pause the game, step one frame, show hitboxes, pick objects, fly a free
 * camera, and tune the debugger itself.
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
 *   <li>F4 turns the free camera on and off ({@link #setFreeCamKey(int)}).</li>
 *   <li>Alt plus a left click selects the topmost object under the mouse in the inspector.</li>
 * </ul>
 * F1 is reserved for a future debugger feature.
 *
 * <h2>Selecting and outlines</h2>
 *
 * <p>An Alt click picks the object that is drawn at the mouse (see {@link FlixelDebugPicker}) and selects it on
 * the {@linkplain FlixelDebugManager#inspector inspector}. The click is taken from the game, so the game does not
 * also react to it. The selected object is always outlined in yellow with a thicker line, even with hitboxes off.
 * Hitboxes use the object's own debug color (red for solid objects by default, see
 * {@link FlixelObject#setDebugBoundingBoxColor(float[])} to give one object its own color).
 *
 * <h2>Free camera</h2>
 *
 * <p>The free camera lets you fly around the level without touching the game. While it is on, the main camera
 * stops following its target, and these controls move it:
 * <ul>
 *   <li>W, A, S, D or the arrow keys pan ({@value #DEFAULT_FREE_CAM_SPEED} screen pixels per second, so it feels
 *       the same at any zoom). Shift makes it four times faster.</li>
 *   <li>The mouse wheel zooms by a factor of {@value #ZOOM_STEP} per notch, and Q and E zoom out and in. The zoom
 *       stays between {@value #MIN_ZOOM} and {@value #MAX_ZOOM}.</li>
 *   <li>Dragging with the right mouse button pans.</li>
 * </ul>
 * It runs on real time, so it works while the game is paused. The keys, the right button, and the wheel are
 * hidden from the game while it is on (see {@link FlixelKeyInputManager#block(int)}), so the player does not move
 * while you fly. Turning it off puts the camera's scroll, zoom, scroll bounds, and follow target back exactly as
 * they were.
 *
 * <p>Code (and the debugger app, through the viewport panel) can fly the camera too: {@link #panFreeCam(float, float)}
 * moves it, {@link #zoomFreeCam(float, float, float)} zooms around a point that stays still, and
 * {@link #setFreeCamIndex(int)} chooses another camera to fly than the main one. The keys, the wheel, and the right
 * button use the same two methods, so every way of flying behaves the same.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.controls.setPaused(true);
 * Flixel.debug.controls.stepFrame();      // The next frame runs, then the game stays paused.
 * Flixel.debug.controls.setHitboxes(true);
 * Flixel.debug.controls.setPauseKey(FlixelKey.P);
 * Flixel.debug.controls.goTo(1200f, 300f); // Turns the free camera on and looks at that spot.
 * Flixel.debug.controls.panFreeCam(0f, -50f); // Looks a little higher.
 * Flixel.debug.controls.setFreeCam(false); // Back to the game's camera.
 * }</pre>
 *
 * @see FlixelDebugProtocol#KIND_CONTROLS
 * @see FlixelDebugPicker
 */
public class FlixelDebugControlsPanel extends FlixelDebugPanelBase {

  /** The sample rate in Hz that the stats panel uses until it is changed. */
  public static final int DEFAULT_RATE = 20;

  /** The lowest allowed sample rate in Hz. */
  public static final int MIN_RATE = 1;

  /** The highest allowed sample rate in Hz. */
  public static final int MAX_RATE = 30;

  /** The free camera's pan speed in screen pixels per second until it is changed. */
  public static final float DEFAULT_FREE_CAM_SPEED = 600f;

  /** How much faster the free camera pans while Shift is held. */
  public static final float FAST_FACTOR = 4f;

  /** The zoom factor of one mouse wheel notch (or one step of the Q and E keys). */
  public static final float ZOOM_STEP = 1.25f;

  /** The lowest zoom the free camera allows. */
  public static final float MIN_ZOOM = 0.1f;

  /** The highest zoom the free camera allows. */
  public static final float MAX_ZOOM = 10f;

  /** How many zoom steps per second the Q and E keys apply. */
  public static final float ZOOM_KEY_STEPS_PER_SECOND = 3f;

  private static final float[] FALLBACK_BOUNDING_BOX_COLOR = { 1f, 0.2f, 0.2f, 0.6f };

  /** The most zoom steps one frame of wheel input can apply, which keeps a runaway device from flinging the zoom. */
  private static final float MAX_WHEEL_STEPS = 8f;

  /** The keys the free camera takes from the game while it is on. */
  private static final int[] FREE_CAM_KEYS = {
      FlixelKey.W, FlixelKey.A, FlixelKey.S, FlixelKey.D,
      FlixelKey.UP, FlixelKey.LEFT, FlixelKey.DOWN, FlixelKey.RIGHT,
      FlixelKey.Q, FlixelKey.E, FlixelKey.SHIFT_LEFT, FlixelKey.SHIFT_RIGHT
  };

  private final FlixelColor boundingBoxColor = new FlixelColor(FlixelColor.WHITE);
  private final FlixelColor selectionColor = new FlixelColor(1f, 0.85f, 0.2f, 1f);
  private final FlixelVector scratchPoint = new FlixelVector();
  private final FlixelDebugPicker picker = new FlixelDebugPicker();
  private final FlixelDebugInspectorPanel inspector;
  private final Consumer<FlixelDebugDrawable> hitboxDrawer = this::drawHitbox;

  @Nullable
  private FlixelBatch hitboxBatch;

  @Nullable
  private FlixelFrame hitboxPixel;

  @Nullable
  private FlixelCamera hitboxCamera;

  @Nullable
  private Object hitboxSelected;

  @Nullable
  private FlixelLogLevel savedLogLevel;

  @Nullable
  private FlixelCamera freeCamCamera;

  @Nullable
  private FlixelPositional savedTarget;

  @Nullable
  private FlixelRect savedDeadzone;

  private FlixelCamera.FollowStyle savedStyle = FlixelCamera.FollowStyle.LOCKON;

  private String pauseKeyName = FlixelKey.toString(FlixelKey.F3);
  private String hitboxesKeyName = FlixelKey.toString(FlixelKey.F2);
  private String freeCamKeyName = FlixelKey.toString(FlixelKey.F4);

  private float hitboxThickness;
  private float freeCamSpeed = DEFAULT_FREE_CAM_SPEED;
  private float savedScrollX;
  private float savedScrollY;
  private float savedZoom;
  private float savedFollowLerp;
  private float savedMinScrollX;
  private float savedMaxScrollX;
  private float savedMinScrollY;
  private float savedMaxScrollY;
  private float dragX;
  private float dragY;

  private int pauseKey = FlixelKey.F3;
  private int hitboxesKey = FlixelKey.F2;
  private int freeCamKey = FlixelKey.F4;
  private int freeCamIndex;
  private int rate = DEFAULT_RATE;
  private int logLevel = FlixelDebugProtocol.LEVEL_DEFAULT;
  private int syncedRate = -1;
  private int syncedLogLevel = -1;

  private boolean hitboxes;
  private boolean freeCam;
  private boolean freeCamActive;
  private boolean dragging;
  private boolean syncedPaused;
  private boolean syncedHitboxes;
  private boolean syncedFreeCam;

  /**
   * Creates the controls panel.
   *
   * @param inspector The inspector that picked objects are selected on and whose selection is outlined.
   */
  public FlixelDebugControlsPanel(@NotNull FlixelDebugInspectorPanel inspector) {
    super("controls", "Controls", FlixelDebugProtocol.KIND_CONTROLS);
    this.inspector = inspector;
  }

  /**
   * Handles the input the debugger owns: the free camera and Alt click picking.
   *
   * <p>The game calls this once per frame after it has read the input devices and before the game logic runs,
   * so a click or key the debugger takes is already hidden when the game looks. It does not allocate.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  public void updateInput(float elapsed) {
    FlixelKeyInputManager keys = Flixel.keys;
    FlixelMouseInputManager mouse = Flixel.mouse;
    if (freeCam) {
      FlixelCamera wanted = freeCamTarget();
      if (freeCamActive && wanted != freeCamCamera) {
        // The camera list was rebuilt (for example by a new state), so fly the new camera instead.
        leaveFreeCam();
      }
      if (!freeCamActive) {
        enterFreeCam();
      }
    }
    if (freeCamActive && keys != null && mouse != null) {
      runFreeCam(elapsed, keys, mouse);
    }
    if (keys != null && mouse != null && mouse.rawJustPressed(FlixelMouseButton.LEFT)
        && (keys.rawPressed(FlixelKey.ALT_LEFT) || keys.rawPressed(FlixelKey.ALT_RIGHT))) {
      mouse.consume(FlixelMouseButton.LEFT);
      Object hit = pickAtMouse();
      if (hit != null) {
        inspector.select(hit);
      }
    }
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
      if (freeCamKey != FlixelKey.NONE && keys.justPressed(freeCamKey)) {
        setFreeCam(!freeCam);
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
   * Finds the topmost object drawn under the mouse, without selecting it.
   *
   * @return The object, or {@code null} when nothing is drawn there or there is no mouse manager.
   * @see FlixelDebugPicker#pickAt(float, float)
   */
  @Nullable
  public Object pickAtMouse() {
    FlixelMouseInputManager mouse = Flixel.mouse;
    return mouse != null ? picker.pickAt(mouse.getScreenX(), mouse.getScreenY()) : null;
  }

  /**
   * Turns the free camera on if needed and centers the flown camera on a world point.
   *
   * <p>The flown camera is the main camera unless {@link #setFreeCamIndex(int)} chose another one.
   *
   * <p>Turning the free camera on first is what keeps the move from being undone by the camera's follow target on
   * the next frame. Turn the free camera off to go back to the game's camera.
   *
   * @param worldX The world X to center on.
   * @param worldY The world Y to center on.
   * @return {@code true} when the camera moved. It is {@code false} when there is no camera.
   */
  public boolean goTo(float worldX, float worldY) {
    if (Flixel.cameras.getSize() == 0) {
      return false;
    }
    setFreeCam(true);
    FlixelCamera cam = freeCamCamera;
    if (!freeCamActive || cam == null) {
      return false;
    }
    cam.focusOn(worldX, worldY);
    return true;
  }

  /**
   * Turns the free camera on if needed and moves the flown camera by a distance in view space.
   *
   * <p>One view unit is one world unit for an object with a scroll factor of {@code 1}, so the distance does not
   * depend on the window size. A positive {@code dx} moves the camera right, which makes the world slide left on
   * the screen. To drag the world with a finger, pass the finger's movement with the sign flipped. The keyboard
   * and the right mouse button of the free camera go through this method too.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Flixel.debug.controls.panFreeCam(100f, 0f); // Look 100 units further right.
   * }</pre>
   *
   * @param dx How far to move right, in view units. Negative moves left.
   * @param dy How far to move down, in view units. Negative moves up.
   * @return {@code true} when the camera moved. It is {@code false} when there is no camera or a distance is not
   *     a finite number.
   */
  public boolean panFreeCam(float dx, float dy) {
    if (!Float.isFinite(dx) || !Float.isFinite(dy)) {
      return false;
    }
    setFreeCam(true);
    FlixelCamera cam = freeCamCamera;
    if (!freeCamActive || cam == null) {
      return false;
    }
    cam.scrollX += dx;
    cam.scrollY += dy;
    return true;
  }

  /**
   * Turns the free camera on if needed and zooms the flown camera, keeping one point of the screen still.
   *
   * <p>Think of pinching a map on a phone: the spot between your fingers stays under your fingers while
   * everything else grows or shrinks around it. The anchor is that spot, given in the camera's view space as it is
   * before the zoom ({@code 0, 0} is the top left corner of what the camera shows). Pass the center of the view to
   * zoom the way the mouse wheel does. The zoom stays between {@value #MIN_ZOOM} and {@value #MAX_ZOOM}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelCamera cam = Flixel.cameras.first();
   * float centerX = cam.getWorldWidth() / cam.getZoom() / 2f;
   * float centerY = cam.getWorldHeight() / cam.getZoom() / 2f;
   * Flixel.debug.controls.zoomFreeCam(2f, centerX, centerY); // Twice as close, around the center.
   * }</pre>
   *
   * @param factor How much to multiply the zoom by. {@code 2} zooms in twice as close, {@code 0.5} zooms out.
   * @param anchorViewX The horizontal position of the point that stays still, in view space.
   * @param anchorViewY The vertical position of the point that stays still, in view space.
   * @return {@code true} when the zoom changed. It is {@code false} when there is no camera, the factor is not a
   *     positive finite number, an anchor is not finite, or the zoom was already at its limit.
   */
  public boolean zoomFreeCam(float factor, float anchorViewX, float anchorViewY) {
    if (!(factor > 0f) || !Float.isFinite(factor) || !Float.isFinite(anchorViewX) || !Float.isFinite(anchorViewY)) {
      return false;
    }
    setFreeCam(true);
    FlixelCamera cam = freeCamCamera;
    if (!freeCamActive || cam == null) {
      return false;
    }
    float before = cam.getZoom();
    float after = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, before * factor));
    if (after == before) {
      return false;
    }
    // The world point under the anchor, for an object with a scroll factor of 1 (see worldToViewX(...)).
    float worldX = anchorViewX + cam.scrollX + cam.getViewMarginX();
    float worldY = anchorViewY + cam.scrollY + cam.getViewMarginY();
    cam.setZoom(after);
    // The same spot on the screen now has a view position scaled by the zoom change. Scroll so the world point
    // is drawn there again.
    float ratio = before / after;
    cam.scrollX = worldX - anchorViewX * ratio - cam.getViewMarginX();
    cam.scrollY = worldY - anchorViewY * ratio - cam.getViewMarginY();
    return true;
  }

  /**
   * Draws an outline around every visible {@link FlixelDebugDrawable} when hitboxes are on, and around the
   * selected object always.
   *
   * <p>The game calls this once per frame after the scene has been drawn. Each object chooses its own
   * outline color through {@link FlixelDebugDrawable#getDebugBoundingBoxColor()}. The selected object gets a
   * thicker yellow outline on top. It does nothing while {@link #isHitboxes()} is {@code false} and nothing is
   * selected, and it does not allocate.
   *
   * @param cameras The game's cameras. Empty slots are skipped.
   */
  public void drawBoundingBoxes(@NotNull FlixelCamera[] cameras) {
    Object selected = inspector.getSelected();
    boolean outline = selected instanceof FlixelObject || selected instanceof FlixelDebugDrawable;
    if (!hitboxes && !outline) {
      return;
    }
    FlixelBatch batch = Flixel.graphics.getBatch();
    hitboxBatch = batch;
    hitboxPixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);
    hitboxSelected = outline ? selected : null;
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
        if (hitboxes) {
          FlixelDebugUtil.forEachDebugDrawable(hitboxDrawer);
        }
        if (outline) {
          drawSelection(selected, cam);
        }
        batch.end();
      }
    } finally {
      hitboxBatch = null;
      hitboxPixel = null;
      hitboxCamera = null;
      hitboxSelected = null;
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
    if (freeCamKey != FlixelKey.NONE) {
      writer.writeString(freeCamKeyName).writeString("Turn the free camera on or off");
      count++;
    }
    writer.writeString("Alt + Click").writeString("Select the object under the mouse");
    writer.writeString("W A S D / Arrows").writeString("Move the free camera");
    writer.writeString("Wheel / Q / E").writeString("Zoom the free camera");
    writer.writeString("Shift").writeString("Move the free camera 4 times faster");
    writer.writeString("Right Drag").writeString("Pan the free camera");
    count += 5;
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
    setFreeCam(false);
    freeCamIndex = 0;
    rate = DEFAULT_RATE;
    setLogLevel(FlixelDebugProtocol.LEVEL_DEFAULT);
    markChanged();
  }

  private void drawHitbox(FlixelDebugDrawable drawable) {
    FlixelCamera cam = hitboxCamera;
    FlixelBatch batch = hitboxBatch;
    FlixelFrame pixel = hitboxPixel;
    if (drawable == null || cam == null || batch == null || pixel == null || drawable == hitboxSelected) {
      return;
    }
    // An object is only outlined on cameras it is drawn on. An empty camera list means "every camera
    // whose defaultDrawTarget is true".
    if (drawable instanceof FlixelBasic basic && !FlixelDebugPicker.isDrawnOn(basic, cam)) {
      return;
    }
    float[] c = drawable.getDebugBoundingBoxColor();
    if (c == null || c.length < 4) {
      c = FALLBACK_BOUNDING_BOX_COLOR;
    }
    boundingBoxColor.set(c[0], c[1], c[2], c[3]);
    FlixelSpriteUtil.drawBorder(batch, pixel, drawable.getDebugDrawX(cam), drawable.getDebugDrawY(cam),
        drawable.getDebugWidth(), drawable.getDebugHeight(), hitboxThickness, boundingBoxColor);
  }

  private void drawSelection(Object selected, FlixelCamera cam) {
    FlixelBatch batch = hitboxBatch;
    FlixelFrame pixel = hitboxPixel;
    if (batch == null || pixel == null) {
      return;
    }
    if (selected instanceof FlixelBasic basic && (!basic.isExists() || !FlixelDebugPicker.isDrawnOn(basic, cam))) {
      return;
    }
    float thickness = hitboxThickness * 2f;
    if (selected instanceof FlixelObject object) {
      // Outline what is drawn, which is what an Alt click hits, not only the hitbox.
      if (picker.computeBounds(object, cam)) {
        FlixelSpriteUtil.drawBorder(batch, pixel, picker.getBoundsLeft(), picker.getBoundsTop(),
            picker.getBoundsWidth(), picker.getBoundsHeight(), thickness, selectionColor);
      }
    } else if (selected instanceof FlixelDebugDrawable drawable) {
      FlixelSpriteUtil.drawBorder(batch, pixel, drawable.getDebugDrawX(cam), drawable.getDebugDrawY(cam),
          drawable.getDebugWidth(), drawable.getDebugHeight(), thickness, selectionColor);
    }
  }

  /** Returns the camera the free camera should fly: the chosen one, or the main camera when it does not exist. */
  @Nullable
  private FlixelCamera freeCamTarget() {
    int size = Flixel.cameras.getSize();
    if (size == 0) {
      return null;
    }
    return Flixel.cameras.get(freeCamIndex < size ? freeCamIndex : 0);
  }

  private void enterFreeCam() {
    FlixelCamera cam = freeCamTarget();
    if (cam == null) {
      return;
    }
    freeCamCamera = cam;
    savedScrollX = cam.scrollX;
    savedScrollY = cam.scrollY;
    savedZoom = cam.getZoom();
    savedTarget = cam.target;
    savedStyle = cam.style;
    savedFollowLerp = cam.followLerp;
    savedDeadzone = cam.deadzone;
    savedMinScrollX = cam.minScrollX;
    savedMaxScrollX = cam.maxScrollX;
    savedMinScrollY = cam.minScrollY;
    savedMaxScrollY = cam.maxScrollY;
    cam.target = null;
    // A free camera must be able to leave the level, so the scroll bounds are lifted until it is turned off.
    cam.setScrollBounds(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
    FlixelKeyInputManager keys = Flixel.keys;
    if (keys != null) {
      for (int i = 0; i < FREE_CAM_KEYS.length; i++) {
        keys.block(FREE_CAM_KEYS[i]);
      }
    }
    FlixelMouseInputManager mouse = Flixel.mouse;
    if (mouse != null) {
      mouse.setBlocked(FlixelMouseButton.RIGHT, true);
      mouse.setScrollBlocked(true);
    }
    dragging = false;
    freeCamActive = true;
    markChanged();
  }

  private void leaveFreeCam() {
    FlixelCamera cam = freeCamCamera;
    freeCamActive = false;
    freeCamCamera = null;
    dragging = false;
    FlixelKeyInputManager keys = Flixel.keys;
    if (keys != null) {
      for (int i = 0; i < FREE_CAM_KEYS.length; i++) {
        keys.unblock(FREE_CAM_KEYS[i]);
      }
    }
    FlixelMouseInputManager mouse = Flixel.mouse;
    if (mouse != null) {
      mouse.setBlocked(FlixelMouseButton.RIGHT, false);
      mouse.setScrollBlocked(false);
    }
    if (cam == null) {
      return;
    }
    cam.target = savedTarget;
    cam.style = savedStyle;
    cam.followLerp = savedFollowLerp;
    cam.setScrollBounds(savedMinScrollX, savedMaxScrollX, savedMinScrollY, savedMaxScrollY);
    cam.restoreScrollAndZoom(savedScrollX, savedScrollY, savedZoom);
    // Restoring the zoom rebuilds the dead zone when there is a target, so put the original one back.
    cam.deadzone = savedDeadzone;
    savedTarget = null;
    savedDeadzone = null;
    markChanged();
  }

  private void runFreeCam(float elapsed, FlixelKeyInputManager keys, FlixelMouseInputManager mouse) {
    FlixelCamera cam = freeCamCamera;
    if (cam == null) {
      return;
    }
    // Something else (a state reset, for example) may have given the camera a target again.
    cam.target = null;

    float perPixelX = worldPerPixelX(cam);
    float perPixelY = worldPerPixelY(cam);
    boolean fast = keys.rawPressed(FlixelKey.SHIFT_LEFT) || keys.rawPressed(FlixelKey.SHIFT_RIGHT);

    float moveX = 0f;
    float moveY = 0f;
    if (keys.rawPressed(FlixelKey.A) || keys.rawPressed(FlixelKey.LEFT)) {
      moveX -= 1f;
    }
    if (keys.rawPressed(FlixelKey.D) || keys.rawPressed(FlixelKey.RIGHT)) {
      moveX += 1f;
    }
    if (keys.rawPressed(FlixelKey.W) || keys.rawPressed(FlixelKey.UP)) {
      moveY -= 1f;
    }
    if (keys.rawPressed(FlixelKey.S) || keys.rawPressed(FlixelKey.DOWN)) {
      moveY += 1f;
    }
    float pixels = freeCamSpeed * (fast ? FAST_FACTOR : 1f) * elapsed;
    if (moveX != 0f && moveY != 0f) {
      // Keep diagonal movement as fast as straight movement.
      pixels *= 0.70710678f;
    }
    if (moveX != 0f || moveY != 0f) {
      panFreeCam(moveX * pixels * perPixelX, moveY * pixels * perPixelY);
    }

    if (mouse.rawPressed(FlixelMouseButton.RIGHT)) {
      float x = mouse.getScreenX();
      float y = mouse.getScreenY();
      if (dragging && (x != dragX || y != dragY)) {
        // Dragging grabs the world, so it moves the opposite way to the camera.
        panFreeCam(-(x - dragX) * perPixelX, -(y - dragY) * perPixelY);
      }
      dragX = x;
      dragY = y;
      dragging = true;
    } else {
      dragging = false;
    }

    // A positive wheel delta scrolls down, which zooms out.
    float steps = -mouse.getRawScrollDeltaY();
    steps = Math.max(-MAX_WHEEL_STEPS, Math.min(MAX_WHEEL_STEPS, steps));
    if (keys.rawPressed(FlixelKey.E)) {
      steps += ZOOM_KEY_STEPS_PER_SECOND * elapsed;
    }
    if (keys.rawPressed(FlixelKey.Q)) {
      steps -= ZOOM_KEY_STEPS_PER_SECOND * elapsed;
    }
    if (steps != 0f) {
      float zoom = cam.getZoom();
      zoomFreeCam((float) Math.pow(ZOOM_STEP, steps), cam.getWorldWidth() / zoom * 0.5f,
          cam.getWorldHeight() / zoom * 0.5f);
    }
  }

  /** Returns how many world units one screen pixel covers horizontally, which depends on the zoom and window size. */
  private float worldPerPixelX(FlixelCamera cam) {
    scratchPoint.set(0f, 0f);
    cam.unproject(scratchPoint);
    float x0 = scratchPoint.x;
    scratchPoint.set(100f, 0f);
    cam.unproject(scratchPoint);
    float value = (scratchPoint.x - x0) / 100f;
    return value > 0f && value < Float.POSITIVE_INFINITY ? value : 1f / cam.getZoom();
  }

  /** Returns how many world units one screen pixel covers vertically. */
  private float worldPerPixelY(FlixelCamera cam) {
    scratchPoint.set(0f, 0f);
    cam.unproject(scratchPoint);
    float y0 = scratchPoint.y;
    scratchPoint.set(0f, 100f);
    cam.unproject(scratchPoint);
    float value = (scratchPoint.y - y0) / 100f;
    return value > 0f && value < Float.POSITIVE_INFINITY ? value : 1f / cam.getZoom();
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

  /**
   * Returns whether the free camera was asked for.
   *
   * <p>This is {@code true} from the moment it is requested, even if no camera existed yet to fly. See
   * {@link #isFreeCamActive()} for whether a camera is flying right now.
   *
   * @return {@code true} when the free camera is on.
   */
  public boolean isFreeCam() {
    return freeCam;
  }

  /**
   * Turns the free camera on or off.
   *
   * <p>Turning it on saves the flown camera's scroll, zoom, scroll bounds, and follow target, stops following,
   * and hides the free camera keys from the game. Turning it off restores all of that exactly. When there is no
   * camera yet, the request is remembered and the free camera starts as soon as one exists.
   *
   * @param freeCam {@code true} to fly the camera chosen with {@link #setFreeCamIndex(int)} (the main camera by
   *     default).
   */
  public void setFreeCam(boolean freeCam) {
    this.freeCam = freeCam;
    if (freeCam) {
      if (!freeCamActive) {
        enterFreeCam();
      }
    } else if (freeCamActive) {
      leaveFreeCam();
    }
  }

  /**
   * Returns whether the free camera is flying a camera right now.
   *
   * @return {@code true} when a camera is under the free camera's control.
   */
  public boolean isFreeCamActive() {
    return freeCamActive;
  }

  public int getFreeCamIndex() {
    return freeCamIndex;
  }

  /**
   * Chooses which camera the free camera flies.
   *
   * <p>The index is a position in {@link Flixel#cameras}, and {@code 0} (the main camera) is the default. When the
   * free camera is flying another camera right now, that camera is restored exactly first and the new one takes
   * over. While the index is past the end of the camera list, the main camera is flown instead.
   *
   * @param freeCamIndex A position in {@link Flixel#cameras}. Negative values are raised to {@code 0}.
   */
  public void setFreeCamIndex(int freeCamIndex) {
    int index = Math.max(0, freeCamIndex);
    if (index == this.freeCamIndex) {
      return;
    }
    boolean flying = freeCamActive;
    if (flying) {
      leaveFreeCam();
    }
    this.freeCamIndex = index;
    if (flying && freeCam) {
      enterFreeCam();
    }
  }

  public float getFreeCamSpeed() {
    return freeCamSpeed;
  }

  /**
   * Sets how fast the free camera pans.
   *
   * @param freeCamSpeed Screen pixels per second at normal speed. Values below {@code 1} are raised to {@code 1}.
   */
  public void setFreeCamSpeed(float freeCamSpeed) {
    this.freeCamSpeed = Math.max(1f, freeCamSpeed);
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

  public int getFreeCamKey() {
    return freeCamKey;
  }

  /**
   * Sets the key that turns the free camera on and off.
   *
   * @param freeCamKey A {@link FlixelKey} code, or {@link FlixelKey#NONE} to disable the hotkey.
   */
  public void setFreeCamKey(int freeCamKey) {
    this.freeCamKey = freeCamKey;
    freeCamKeyName = FlixelKey.toString(freeCamKey);
    markChanged();
  }

  /**
   * Returns the picker this panel uses for Alt clicks.
   *
   * @return The picker.
   */
  @NotNull
  public FlixelDebugPicker getPicker() {
    return picker;
  }
}
