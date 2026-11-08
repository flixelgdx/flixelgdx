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
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.input.FlixelBaseInputDevice;
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

  private int injectedX;
  private int injectedY;
  private int realX;
  private int realY;
  private int screenX;
  private int screenY;

  private float worldX;
  private float worldY;

  private float injectedScrollX;
  private float injectedScrollY;

  private float scrollDeltaX;
  private float scrollDeltaY;

  private final boolean[] justPressed = new boolean[MAX_BUTTON + 1];
  private final boolean[] justReleased = new boolean[MAX_BUTTON + 1];
  private final boolean[] prevPressed = new boolean[MAX_BUTTON + 1];

  /** Buttons whose current press was taken by a tool; cleared when the button is released. */
  private final boolean[] consumed = new boolean[MAX_BUTTON + 1];

  /** Buttons a tool keeps from the game until it unblocks them. */
  private final boolean[] blocked = new boolean[MAX_BUTTON + 1];

  private final boolean[] injected = new boolean[MAX_BUTTON + 1];

  @Nullable
  private FlixelCamera worldCamera;

  private final FlixelVector tmpUnproject = new FlixelVector();
  private final FlixelVector tmpProject = new FlixelVector();

  /** When {@code false}, all queries return inactive state. */
  public boolean enabled = true;

  private boolean scrollBlocked;

  private boolean injectedPointer;

  private boolean forwarding;

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
    if (forwarding) {
      // This is an injected scroll coming back through the device. It was already counted in injectScroll(...).
      return;
    }
    scrollDeltaX += amountX;
    scrollDeltaY += amountY;
  }

  /** Call once per frame at the start of the game update (with {@link Flixel#keys}). */
  @Override
  public void update() {
    if (!enabled) {
      return;
    }
    if (injectedPointer && (Flixel.input.getX() != realX || Flixel.input.getY() != realY)) {
      // The real mouse moved, so the person at the keyboard takes the pointer back.
      injectedPointer = false;
    }
    screenX = injectedPointer ? injectedX : Flixel.input.getX();
    screenY = injectedPointer ? injectedY : Flixel.input.getY();
    for (int i = 0; i <= MAX_BUTTON; i++) {
      boolean cur = isDown(i);
      justPressed[i] = cur && !prevPressed[i];
      justReleased[i] = !cur && prevPressed[i];
      if (consumed[i] && !cur) {
        // The tool that took this press is done with it; the release belongs to that press, so the game
        // does not see it either.
        consumed[i] = false;
        justReleased[i] = false;
      }
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
      prevPressed[i] = isDown(i);
    }
    scrollDeltaX = 0f;
    scrollDeltaY = 0f;
    injectedScrollX = 0f;
    injectedScrollY = 0f;
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
  public void setScreenX(int x) {
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
  public void setScreenY(int y) {
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
      setScreenX((int) x);
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
      setScreenY((int) y);
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
    return (scrollBlocked ? 0f : scrollDeltaX) + injectedScrollX;
  }

  /**
   * Sum of vertical scroll amounts received this frame via {@link FlixelMouseListener#scrolled(float, float)}
   * {@code amountY} (not cleared until {@link #endFrame()}). Sign and magnitude are device-dependent; see
   * class Javadoc.
   *
   * @return The accumulated vertical scroll delta for the current frame.
   */
  public float getScrollDeltaY() {
    return (scrollBlocked ? 0f : scrollDeltaY) + injectedScrollY;
  }

  /**
   * Returns the vertical scroll of this frame even when scrolling is {@linkplain #setScrollBlocked(boolean) blocked}.
   *
   * <p>This is for tools that take the wheel away from the game. Game code should use {@link #getScrollDeltaY()}.
   *
   * @return The accumulated vertical scroll delta for the current frame.
   */
  public float getRawScrollDeltaY() {
    return scrollDeltaY;
  }

  /**
   * Returns whether the given mouse button is held down.
   *
   * @param button Button index (e.g. {@code 0} for left mouse button).
   * @return {@code true} if the button is pressed and input is enabled.
   */
  public boolean pressed(int button) {
    return rawPressed(button) && !hidden(button);
  }

  /**
   * Returns whether the given mouse button was just pressed this frame.
   *
   * @param button Button index.
   * @return {@code true} if the button was just pressed and input is enabled.
   */
  public boolean justPressed(int button) {
    return rawJustPressed(button) && !hidden(button);
  }

  /**
   * Returns whether the given mouse button was just released this frame.
   *
   * @param button Button index.
   * @return {@code true} if the button was just released and input is enabled.
   */
  public boolean justReleased(int button) {
    return rawJustReleased(button) && !hidden(button);
  }

  /**
   * Returns whether the given mouse button is held down, even when a tool hides it from the game.
   *
   * <p>This is for tools such as the debugger. Game code should use {@link #pressed(int)}.
   *
   * @param button Button index.
   * @return {@code true} if the button is pressed and input is enabled.
   * @see #consume(int)
   * @see #setBlocked(int, boolean)
   */
  public boolean rawPressed(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && isDown(button);
  }

  /**
   * Returns whether the given mouse button was just pressed this frame, even when a tool hides it from the game.
   *
   * @param button Button index.
   * @return {@code true} if the button was just pressed and input is enabled.
   * @see #rawPressed(int)
   */
  public boolean rawJustPressed(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && justPressed[button];
  }

  /**
   * Returns whether the given mouse button was just released this frame, even when a tool hides it from the game.
   *
   * <p>The release of a press that was {@linkplain #consume(int) consumed} is not reported, because
   * {@link #update()} drops it.
   *
   * @param button Button index.
   * @return {@code true} if the button was just released and input is enabled.
   * @see #rawPressed(int)
   */
  public boolean rawJustReleased(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && justReleased[button];
  }

  /**
   * Hides the current press of a button from the game, so a tool can use a click without the game also reacting
   * to it.
   *
   * <p>The game sees the button as released until the player lets go of it, and it never sees the matching
   * release. Call this after {@link #update()} and before the game's own update reads the mouse. It only has an
   * effect while the button is held. {@link #rawPressed(int)} and the other {@code raw...} methods are not affected.
   *
   * @param button Button index.
   */
  public void consume(int button) {
    if (button >= 0 && button <= MAX_BUTTON && isDown(button)) {
      consumed[button] = true;
    }
  }

  /**
   * Hides a button from the game, or shows it again.
   *
   * <p>Unlike {@link #consume(int)} this lasts until it is undone, which suits a tool that owns a button for a
   * while (the debugger's free camera owns the right button). A tool that blocks a button must unblock it again.
   *
   * @param button Button index.
   * @param blocked {@code true} to hide the button from the game.
   */
  public void setBlocked(int button, boolean blocked) {
    if (button >= 0 && button <= MAX_BUTTON) {
      this.blocked[button] = blocked;
    }
  }

  /**
   * Returns whether a button is hidden from the game by {@link #setBlocked(int, boolean)}.
   *
   * @param button Button index.
   * @return {@code true} if the button is blocked.
   */
  public boolean isBlocked(int button) {
    return button >= 0 && button <= MAX_BUTTON && blocked[button];
  }

  /**
   * Hides the scroll wheel from the game, or shows it again.
   *
   * <p>While blocked, {@link #getScrollDeltaX()} and {@link #getScrollDeltaY()} return {@code 0}. The raw scroll
   * stays available through {@link #getRawScrollDeltaY()}.
   *
   * @param scrollBlocked {@code true} to hide the wheel from the game.
   */
  public void setScrollBlocked(boolean scrollBlocked) {
    this.scrollBlocked = scrollBlocked;
  }

  public boolean isScrollBlocked() {
    return scrollBlocked;
  }

  private boolean hidden(int button) {
    return !injected[button] && (consumed[button] || blocked[button]);
  }

  private boolean isDown(int button) {
    return injected[button] || Flixel.input.isButtonPressed(button);
  }

  /**
   * Moves the pointer on behalf of a tool, so the game sees the mouse at that spot.
   *
   * <p>From the next {@link #update()} on, {@link #getScreenX()}, {@link #getWorldX()}, and
   * {@link #getWorldX(FlixelCamera)} (and the matching Y methods) report the injected position instead of the
   * real one, and every {@link FlixelMouseListener} gets a {@code mouseMoved} (or {@code mouseDragged} while an
   * injected button is held). The injected position stays until {@link #releaseInjected()} or until the real
   * mouse moves, whichever comes first.
   *
   * @param x The horizontal position in window pixels.
   * @param y The vertical position in window pixels.
   */
  public void injectPosition(int x, int y) {
    if (!injectedPointer) {
      realX = Flixel.input.getX();
      realY = Flixel.input.getY();
    }
    injectedPointer = true;
    injectedX = x;
    injectedY = y;
    if (Flixel.input instanceof FlixelBaseInputDevice device) {
      if (anyInjected()) {
        device.injectMouseDragged(x, y);
      } else {
        device.injectMouseMoved(x, y);
      }
    }
  }

  /**
   * Presses or releases a mouse button on behalf of a tool, so the game sees a real click.
   *
   * <p>The button shows up in {@link #pressed(int)}, {@link #justPressed(int)}, {@link #justReleased(int)}, and
   * every {@link FlixelMouseListener}, and it is never hidden by {@link #setBlocked(int, boolean)} or
   * {@link #consume(int)}. "Just" detection works like it does for a real button, so release the button in a later
   * frame than the press. Pointer position comes from {@link #injectPosition(int, int)}.
   *
   * @param button The {@link FlixelMouseButton} code.
   * @param down {@code true} to press, {@code false} to release.
   */
  public void injectButton(int button, boolean down) {
    if (button < 0 || button > MAX_BUTTON || injected[button] == down) {
      return;
    }
    injected[button] = down;
    if (Flixel.input instanceof FlixelBaseInputDevice device) {
      int x = injectedPointer ? injectedX : Flixel.input.getX();
      int y = injectedPointer ? injectedY : Flixel.input.getY();
      if (down) {
        device.injectMouseDown(button, x, y);
      } else {
        device.injectMouseUp(button, x, y);
      }
    }
  }

  /**
   * Scrolls the mouse wheel on behalf of a tool.
   *
   * <p>The amount is added to {@link #getScrollDeltaY()} for the current frame, even while scrolling is
   * {@linkplain #setScrollBlocked(boolean) blocked}, and every {@link FlixelMouseListener} gets
   * {@code scrolled(...)}. A positive amount scrolls down.
   *
   * @param amountX The horizontal scroll amount.
   * @param amountY The vertical scroll amount.
   */
  public void injectScroll(float amountX, float amountY) {
    injectedScrollX += amountX;
    injectedScrollY += amountY;
    if (Flixel.input instanceof FlixelBaseInputDevice device) {
      forwarding = true;
      try {
        device.injectScrolled(amountX, amountY);
      } finally {
        forwarding = false;
      }
    }
  }

  /**
   * Releases every injected button and hands the pointer position back to the real mouse.
   *
   * <p>A tool calls this when it goes away, so a button it held down does not stay stuck.
   */
  public void releaseInjected() {
    for (int i = 0; i <= MAX_BUTTON; i++) {
      injectButton(i, false);
    }
    injectedPointer = false;
  }

  /**
   * Returns whether a tool is holding a mouse button down through {@link #injectButton(int, boolean)}.
   *
   * @param button The button to check.
   * @return {@code true} while the injected press has not been released.
   */
  public boolean isInjected(int button) {
    return button >= 0 && button <= MAX_BUTTON && injected[button];
  }

  private boolean anyInjected() {
    for (int i = 0; i <= MAX_BUTTON; i++) {
      if (injected[i]) {
        return true;
      }
    }
    return false;
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
