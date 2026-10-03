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
package org.flixelgdx.input.mouse;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.debug.FlixelDebugOverlay;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.input.FlixelInputManager;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.math.FlixelVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mouse and pointer polling with screen/world coordinates. Access via {@code Flixel.mouse} after
 * {@link Flixel#start(FlixelGame, FlixelGameRunner)}.
 *
 * <h2>Scroll wheel deltas</h2>
 * <p>
 * {@link FlixelMouseListener#scrolled(float, float)} supplies {@code amountX} and {@code amountY}.
 * This manager <strong>accumulates</strong> them into {@link #getScrollDeltaX()} and
 * {@link #getScrollDeltaY()} until {@link #endFrame()}.
 * </p>
 * <ul>
 *   <li><strong>{@link #getScrollDeltaX()}</strong>: horizontal scroll (e.g. trackpad sideways,
 *       some Shift+wheel setups). Not the usual mouse-wheel up/down axis. This is typically used by
 *       trackpad users for horizontal scrolling.</li>
 *   <li><strong>{@link #getScrollDeltaY()}</strong>: vertical scroll. This is what you normally use
 *       for standard wheel up/down. You'll most likely use this a lot of the time.</li>
 * </ul>
 * <p>
 * Values are <strong>deltas</strong>, not fixed {@code -1}/{@code 1}: magnitude varies by device (notched
 * wheel vs trackpad, high-resolution wheels). The <strong>sign</strong> indicates direction, but which sign
 * means "up" vs "down" can depend on the backend/OS; verify on your target platform if
 * input feels inverted.
 * </p>
 * <p>
 * Read accumulated scroll <strong>after</strong> {@link #update()} and <strong>before</strong>
 * {@link #endFrame()} clears it (same timing as other per-frame input you consume in your game loop).
 * </p>
 */
public class FlixelMouseInputManager implements FlixelInputManager, FlixelMouseListener {

  private static final int MAX_BUTTON = 4;

  @NotNull
  public FlixelMouseIconManager icons = FlixelNoopMouseIconManager.INSTANCE;

  private int screenX;
  private int screenY;

  private float worldX;
  private float worldY;

  private float scrollDeltaX;
  private float scrollDeltaY;

  private final boolean[] justPressed = new boolean[MAX_BUTTON + 1];
  private final boolean[] justReleased = new boolean[MAX_BUTTON + 1];
  private final boolean[] prevPressed = new boolean[MAX_BUTTON + 1];

  @Nullable
  private FlixelCamera worldCamera;

  private final FlixelVector tmpUnproject = new FlixelVector();
  private final FlixelVector tmpProject = new FlixelVector();

  /** When {@code false}, all queries return inactive state. */
  public boolean enabled = true;

  /** Creates a new mouse input manager with all buttons in the released state. */
  public FlixelMouseInputManager() {}

  /**
   * Replaces the active {@link FlixelMouseIconManager}, for example with an LWJGL3 or web backend.
   * Pass {@code null} to force the shared no-op implementation.
   *
   * @param iconManager The {@link FlixelMouseIconManager} to implement.
   */
  public void setMouseIconManager(@Nullable FlixelMouseIconManager iconManager) {
    icons = iconManager != null ? iconManager : FlixelNoopMouseIconManager.INSTANCE;
  }

  @Override
  public void scrolled(float amountX, float amountY) {
    scrollDeltaX += amountX;
    scrollDeltaY += amountY;
  }

  /** Call once per frame at the start of the game update (with {@link Flixel#keys}). */
  @Override
  public void update() {
    if (!enabled) {
      return;
    }
    screenX = Flixel.input.getX();
    screenY = Flixel.input.getY();
    for (int i = 0; i <= MAX_BUTTON; i++) {
      boolean cur = Flixel.input.isButtonPressed(i);
      justPressed[i] = cur && !prevPressed[i];
      justReleased[i] = !cur && prevPressed[i];
    }
    recomputeWorld();
  }

  private void recomputeWorld() {
    FlixelCamera cam = resolveCamera();
    if (cam == null) {
      worldX = screenX;
      worldY = screenY;
      return;
    }
    tmpUnproject.set(screenX, screenY);
    cam.unproject(tmpUnproject);
    worldX = tmpUnproject.x;
    worldY = tmpUnproject.y;
  }

  @Nullable
  private FlixelCamera resolveCamera() {
    return worldCamera != null ? worldCamera : safeGetDefaultCamera();
  }

  @Nullable
  private static FlixelCamera safeGetDefaultCamera() {
    try {
      return Flixel.cameras.first();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Sets the camera for world coordinates; {@code null} uses {@link Flixel#cameras}.
   *
   * @param worldCamera The camera to use for world-coordinate unprojection, or {@code null} to use the default.
   */
  public void setWorldCamera(@Nullable FlixelCamera worldCamera) {
    this.worldCamera = worldCamera;
  }

  @Nullable
  public FlixelCamera getWorldCamera() {
    return worldCamera;
  }

  /**
   * Call at end of frame after game logic (with
   * {@link FlixelKeyInputManager#endFrame()}). Resets
   * {@link #getScrollDeltaX()} and {@link #getScrollDeltaY()} to zero for the next frame.
   */
  @Override
  public void endFrame() {
    for (int i = 0; i <= MAX_BUTTON; i++) {
      prevPressed[i] = Flixel.input.isButtonPressed(i);
    }
    scrollDeltaX = 0f;
    scrollDeltaY = 0f;
  }

  public int getScreenX() {
    return screenX;
  }

  public int getScreenY() {
    return screenY;
  }

  public float getWorldX() {
    return worldX;
  }

  public float getWorldY() {
    return worldY;
  }

  /**
   * Returns the mouse X position in the world coordinates of the given camera.
   *
   * @param cam The camera used to unproject screen coordinates.
   * @return The unprojected world X coordinate.
   */
  public float getWorldX(@NotNull FlixelCamera cam) {
    tmpUnproject.set(screenX, screenY);
    cam.unproject(tmpUnproject);
    return tmpUnproject.x;
  }

  /**
   * Returns the mouse Y position in the world coordinates of the given camera.
   *
   * @param cam The camera used to unproject screen coordinates.
   * @return The unprojected world Y coordinate.
   */
  public float getWorldY(@NotNull FlixelCamera cam) {
    tmpUnproject.set(screenX, screenY);
    cam.unproject(tmpUnproject);
    return tmpUnproject.y;
  }

  /**
   * Returns {@code true} when the current platform can move the pointer from code. Desktop returns
   * {@code true}; web, Android, and iOS return {@code false}, and every {@code set...} position
   * method on this manager is then a no-op.
   *
   * @return {@code true} if the {@code setScreen...} and {@code setWorld...} methods move the pointer.
   */
  public boolean supportsSetPosition() {
    return Flixel.input.supportsPointerWarp();
  }

  /**
   * Moves the pointer horizontally to the given game-screen X, keeping the current Y.
   *
   * <p>The coordinate is in the same space {@link #getScreenX()} reports. This only works on
   * desktop; on web and mobile it does nothing and the tracked position does not change (see
   * {@link #supportsSetPosition()}).
   *
   * @param x The target X in screen pixels from the left edge.
   */
  public void setScreenX(float x) {
    setScreenPosition(x, screenY);
  }

  /**
   * Moves the pointer vertically to the given game-screen Y, keeping the current X.
   *
   * <p>The coordinate is in the same space {@link #getScreenY()} reports. This only works on
   * desktop; on web and mobile it does nothing and the tracked position does not change (see
   * {@link #supportsSetPosition()}).
   *
   * @param y The target Y in screen pixels from the top edge.
   */
  public void setScreenY(float y) {
    setScreenPosition(screenX, y);
  }

  /**
   * Moves the pointer to the given game-screen position.
   *
   * <p>Think of it as picking up the mouse and putting it down somewhere else on the desk: the
   * position you read back this frame is already the new one. Coordinates are in the same space
   * {@link #getScreenX()} and {@link #getScreenY()} report. This only works on desktop; browsers
   * and mobile systems cannot move the pointer, so on web, Android, and iOS this does nothing and
   * the tracked position does not change (see {@link #supportsSetPosition()}).
   *
   * <pre>{@code
   * // Snap the cursor back to the middle of a 1280x720 window.
   * if (Flixel.mouse.supportsSetPosition()) {
   *   Flixel.mouse.setScreenPosition(640f, 360f);
   * }
   * }</pre>
   *
   * @param x The target X in screen pixels from the left edge.
   * @param y The target Y in screen pixels from the top edge.
   */
  public void setScreenPosition(float x, float y) {
    if (!enabled || !Flixel.input.supportsPointerWarp()) {
      return;
    }
    Flixel.input.warpPointer(Math.round(x), Math.round(y));
    screenX = Flixel.input.getX();
    screenY = Flixel.input.getY();
    recomputeWorld();
  }

  /**
   * Moves the pointer horizontally to the given world X, keeping the current screen Y.
   *
   * <p>The coordinate is in the same space {@link #getWorldX()} reports, converted through the
   * world camera. Desktop only; see {@link #setScreenPosition(float, float)}.
   *
   * @param x The target X in world coordinates.
   */
  public void setWorldX(float x) {
    setWorldX(x, resolveCamera());
  }

  /**
   * Moves the pointer horizontally to the given world X of a specific camera, keeping the current
   * screen Y. Desktop only; see {@link #setScreenPosition(float, float)}.
   *
   * @param x The target X in world coordinates of {@code cam}.
   * @param cam The camera used to project world coordinates onto the screen, or {@code null} to
   *     treat world coordinates as screen coordinates.
   */
  public void setWorldX(float x, @Nullable FlixelCamera cam) {
    if (cam == null) {
      setScreenX(x);
      return;
    }
    tmpProject.set(x, getWorldY(cam));
    cam.project(tmpProject);
    setScreenPosition(tmpProject.x, screenY);
  }

  /**
   * Moves the pointer vertically to the given world Y, keeping the current screen X.
   *
   * <p>The coordinate is in the same space {@link #getWorldY()} reports, converted through the
   * world camera. Desktop only; see {@link #setScreenPosition(float, float)}.
   *
   * @param y The target Y in world coordinates.
   */
  public void setWorldY(float y) {
    setWorldY(y, resolveCamera());
  }

  /**
   * Moves the pointer vertically to the given world Y of a specific camera, keeping the current
   * screen X. Desktop only; see {@link #setScreenPosition(float, float)}.
   *
   * @param y The target Y in world coordinates of {@code cam}.
   * @param cam The camera used to project world coordinates onto the screen, or {@code null} to
   *     treat world coordinates as screen coordinates.
   */
  public void setWorldY(float y, @Nullable FlixelCamera cam) {
    if (cam == null) {
      setScreenY(y);
      return;
    }
    tmpProject.set(getWorldX(cam), y);
    cam.project(tmpProject);
    setScreenPosition(screenX, tmpProject.y);
  }

  /**
   * Moves the pointer to the given world position.
   *
   * <p>Handy for snapping the cursor onto an object, like dropping a pin on a map. Coordinates are
   * in the same space {@link #getWorldX()} and {@link #getWorldY()} report, converted through the
   * world camera (the one from {@link #getWorldCamera()}, or the default camera). This only works
   * on desktop; on web, Android, and iOS it does nothing (see {@link #supportsSetPosition()}).
   *
   * <pre>{@code
   * // Snap the cursor to the center of a button sprite.
   * Flixel.mouse.setWorldPosition(button.getX() + button.getWidth() / 2f,
   *     button.getY() + button.getHeight() / 2f);
   * }</pre>
   *
   * @param x The target X in world coordinates.
   * @param y The target Y in world coordinates.
   */
  public void setWorldPosition(float x, float y) {
    setWorldPosition(x, y, resolveCamera());
  }

  /**
   * Moves the pointer to the given world position of a specific camera. Desktop only; see
   * {@link #setWorldPosition(float, float)}.
   *
   * @param x The target X in world coordinates of {@code cam}.
   * @param y The target Y in world coordinates of {@code cam}.
   * @param cam The camera used to project world coordinates onto the screen, or {@code null} to
   *     treat world coordinates as screen coordinates.
   */
  public void setWorldPosition(float x, float y, @Nullable FlixelCamera cam) {
    if (cam == null) {
      setScreenPosition(x, y);
      return;
    }
    tmpProject.set(x, y);
    cam.project(tmpProject);
    setScreenPosition(tmpProject.x, tmpProject.y);
  }

  /**
   * Sum of horizontal scroll amounts received this frame via {@link FlixelMouseListener#scrolled(float, float)}
   * {@code amountX} (not cleared until {@link #endFrame()}). Use for sideways scroll; for typical wheel
   * up/down use {@link #getScrollDeltaY()}.
   *
   * @return The accumulated horizontal scroll delta for the current frame.
   */
  public float getScrollDeltaX() {
    return scrollDeltaX;
  }

  /**
   * Sum of vertical scroll amounts received this frame via {@link FlixelMouseListener#scrolled(float, float)}
   * {@code amountY} (not cleared until {@link #endFrame()}). Sign and magnitude are device-dependent; see
   * class Javadoc.
   *
   * @return The accumulated vertical scroll delta for the current frame.
   */
  public float getScrollDeltaY() {
    return scrollDeltaY;
  }

  /**
   * Returns whether the given mouse button is held down. Returns {@code false} while the active
   * debug overlay reports that another UI layer (typically the imgui debugger) is capturing
   * mouse input, so clicking inside an imgui window cannot leak into game logic.
   *
   * @param button Button index (e.g. {@code 0} for left mouse button).
   * @return {@code true} if the button is pressed and input is enabled and not suppressed by UI.
   */
  public boolean pressed(int button) {
    return enabled && !isCapturedByDebugUI() && button >= 0 && button <= MAX_BUTTON
        && Flixel.input.isButtonPressed(button);
  }

  /**
   * Returns whether the given mouse button was just pressed this frame. Returns {@code false}
   * while the debug UI reports that another UI layer is capturing mouse input.
   *
   * @param button Button index.
   * @return {@code true} if the button was just pressed and input is enabled and not suppressed.
   */
  public boolean justPressed(int button) {
    return enabled && !isCapturedByDebugUI() && button >= 0 && button <= MAX_BUTTON && justPressed[button];
  }

  /**
   * Returns whether the given mouse button was just released this frame. Returns {@code false}
   * while the debug UI reports that another UI layer is capturing mouse input.
   *
   * @param button Button index.
   * @return {@code true} if the button was just released and input is enabled and not suppressed.
   */
  public boolean justReleased(int button) {
    return enabled && !isCapturedByDebugUI() && button >= 0 && button <= MAX_BUTTON && justReleased[button];
  }

  /**
   * Returns {@code true} when the active {@link FlixelDebugOverlay} reports that another UI
   * layer is consuming mouse input. Used by {@link #pressed(int)}, {@link #justPressed(int)},
   * and {@link #justReleased(int)} to suppress game-level input while the cursor is over a
   * debug UI panel.
   */
  private static boolean isCapturedByDebugUI() {
    return Flixel.debug != null && Flixel.debug.overlay.isMouseCapturedByUI();
  }

  /**
   * Returns {@code true} when the current mouse world position falls within the bounds of the given object.
   *
   * @param obj The object whose bounding box is tested against.
   * @return {@code true} if the mouse is inside {@code obj}'s axis-aligned bounding box.
   */
  public boolean overlap(@NotNull FlixelPositional obj) {
    float x = getWorldX();
    float y = getWorldY();
    return x >= obj.getX()
        && x <= obj.getX() + obj.getWidth()
        && y >= obj.getY()
        && y <= obj.getY() + obj.getHeight();
  }
}
