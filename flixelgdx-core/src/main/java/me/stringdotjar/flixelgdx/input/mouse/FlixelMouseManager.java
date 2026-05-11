/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.mouse;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay;
import me.stringdotjar.flixelgdx.input.FlixelInputProcessorManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mouse and pointer polling with screen/world coordinates. Access via {@code Flixel.mouse} after
 * {@link me.stringdotjar.flixelgdx.Flixel#initialize(me.stringdotjar.flixelgdx.FlixelGame)}.
 *
 * <h2>Scroll wheel deltas</h2>
 * <p>
 * {@link InputProcessor#scrolled(float, float)} supplies {@code amountX} and {@code amountY} from libGDX.
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
public class FlixelMouseManager implements FlixelInputProcessorManager {

  private static final int MAX_BUTTON = 4;

  @NotNull
  private FlixelMouseIconManager iconManager = FlixelNoopMouseIconManager.INSTANCE;

  public boolean enabled = true;

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

  private final Vector2 tmpUnproject = new Vector2();

  private final InputProcessor inputProcessor = new InputProcessor() {
    @Override
    public boolean keyDown(int keycode) {
      return false;
    }

    @Override
    public boolean keyUp(int keycode) {
      return false;
    }

    @Override
    public boolean keyTyped(char character) {
      return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
      return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
      return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
      return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
      return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
      return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
      scrollDeltaX += amountX;
      scrollDeltaY += amountY;
      return false;
    }
  };

  public FlixelMouseManager() {}

  /**
   * Replaces the active {@link FlixelMouseIconManager}, for example with an LWJGL3 or web backend.
   * Pass {@code null} to force the shared no-op implementation.
   *
   * @param iconManager The {@link FlixelMouseIconManager} to implement.
   */
  public void setMouseIconManager(@Nullable FlixelMouseIconManager iconManager) {
    this.iconManager = iconManager != null ? iconManager : FlixelNoopMouseIconManager.INSTANCE;
  }

  /**
   * @return Native cursor integration for this session (never null).
   */
  @NotNull
  public FlixelMouseIconManager icons() {
    return iconManager;
  }

  @NotNull
  public InputProcessor getInputProcessor() {
    return inputProcessor;
  }

  /** Call once per frame at the start of the game update (with {@link Flixel#keys}). */
  public void update() {
    if (!enabled) {
      return;
    }
    screenX = Gdx.input.getX();
    screenY = Gdx.input.getY();
    for (int i = 0; i <= MAX_BUTTON; i++) {
      boolean cur = Gdx.input.isButtonPressed(i);
      justPressed[i] = cur && !prevPressed[i];
      justReleased[i] = !cur && prevPressed[i];
    }
    recomputeWorld();
  }

  private void recomputeWorld() {
    FlixelCamera cam = worldCamera != null ? worldCamera : safeGetDefaultCamera();
    if (cam == null) {
      worldX = screenX;
      worldY = screenY;
      return;
    }
    tmpUnproject.set(screenX, screenY);
    cam.getViewport().unproject(tmpUnproject);
    worldX = tmpUnproject.x;
    worldY = tmpUnproject.y;
  }

  @Nullable
  private static FlixelCamera safeGetDefaultCamera() {
    try {
      return Flixel.getCamera();
    } catch (Exception e) {
      return null;
    }
  }

  /** Sets the camera for world coordinates; {@code null} uses {@link Flixel#getCamera()}. */
  public void setWorldCamera(@Nullable FlixelCamera worldCamera) {
    this.worldCamera = worldCamera;
  }

  @Nullable
  public FlixelCamera getWorldCamera() {
    return worldCamera;
  }

  /**
   * Call at end of frame after game logic (with
   * {@link me.stringdotjar.flixelgdx.input.keyboard.FlixelKeyInputManager#endFrame()}). Resets
   * {@link #getScrollDeltaX()} and {@link #getScrollDeltaY()} to zero for the next frame.
   */
  public void endFrame() {
    for (int i = 0; i <= MAX_BUTTON; i++) {
      prevPressed[i] = Gdx.input.isButtonPressed(i);
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

  public float getWorldX(@NotNull FlixelCamera cam) {
    tmpUnproject.set(screenX, screenY);
    cam.getViewport().unproject(tmpUnproject);
    return tmpUnproject.x;
  }

  public float getWorldY(@NotNull FlixelCamera cam) {
    tmpUnproject.set(screenX, screenY);
    cam.getViewport().unproject(tmpUnproject);
    return tmpUnproject.y;
  }

  /**
   * Sum of horizontal scroll amounts received this frame via {@link InputProcessor#scrolled(float, float)}
   * {@code amountX} (not cleared until {@link #endFrame()}). Use for sideways scroll; for typical wheel
   * up/down use {@link #getScrollDeltaY()}.
   */
  public float getScrollDeltaX() {
    return scrollDeltaX;
  }

  /**
   * Sum of vertical scroll amounts received this frame via {@link InputProcessor#scrolled(float, float)}
   * {@code amountY} (not cleared until {@link #endFrame()}). Sign and magnitude are device-dependent; see
   * class Javadoc.
   */
  public float getScrollDeltaY() {
    return scrollDeltaY;
  }

  /**
   * Returns whether the given mouse button is held down. Returns {@code false} while the active
   * debug overlay reports that another UI layer (typically the imgui debugger) is capturing
   * mouse input, so clicking inside an imgui window cannot leak into game logic.
   *
   * <p>Use {@link #rawPressed(int)} when you specifically need the raw, unsuppressed state.
   * The debug overlay's own sprite picker / camera tools use the raw variants so they can opt
   * in to "ignore the suppression" when needed.
   *
   * @param button libGDX button index (e.g. {@code 0} for left mouse button).
   * @return {@code true} if the button is pressed and input is enabled and not suppressed by UI.
   */
  public boolean pressed(int button) {
    if (isCapturedByDebugUI()) {
      return false;
    }
    return rawPressed(button);
  }

  /**
   * Same as {@link #pressed(int)} but bypasses the "captured by debug UI" check.
   *
   * @param button libGDX button index.
   * @return {@code true} if the button is pressed and input is enabled, regardless of UI capture.
   */
  public boolean rawPressed(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && Gdx.input.isButtonPressed(button);
  }

  /**
   * Returns whether the given mouse button was just pressed this frame. Returns {@code false}
   * while the debug UI reports that another UI layer is capturing mouse input.
   *
   * @param button libGDX button index.
   * @return {@code true} if the button was just pressed and input is enabled and not suppressed.
   */
  public boolean justPressed(int button) {
    if (isCapturedByDebugUI()) {
      return false;
    }
    return rawJustPressed(button);
  }

  /**
   * Same as {@link #justPressed(int)} but bypasses the "captured by debug UI" check.
   *
   * @param button libGDX button index.
   * @return {@code true} if the button was just pressed and input is enabled, regardless of UI capture.
   */
  public boolean rawJustPressed(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && justPressed[button];
  }

  /**
   * Returns whether the given mouse button was just released this frame. Returns {@code false}
   * while the debug UI reports that another UI layer is capturing mouse input.
   *
   * @param button libGDX button index.
   * @return {@code true} if the button was just released and input is enabled and not suppressed.
   */
  public boolean justReleased(int button) {
    if (isCapturedByDebugUI()) {
      return false;
    }
    return rawJustReleased(button);
  }

  /**
   * Same as {@link #justReleased(int)} but bypasses the "captured by debug UI" check.
   *
   * @param button libGDX button index.
   * @return {@code true} if the button was just released and input is enabled, regardless of UI capture.
   */
  public boolean rawJustReleased(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && justReleased[button];
  }

  /**
   * Returns {@code true} when the active {@link FlixelDebugOverlay} reports that another UI
   * layer is consuming mouse input. Used by {@link #pressed(int)}, {@link #justPressed(int)},
   * and {@link #justReleased(int)} to suppress game-level input while the cursor is over a
   * debug UI panel.
   */
  private static boolean isCapturedByDebugUI() {
    FlixelDebugOverlay overlay = Flixel.getDebugOverlay();
    return overlay != null && overlay.isMouseCapturedByUI();
  }

  public boolean overlap(@NotNull FlixelObject obj) {
    float x = getWorldX();
    float y = getWorldY();
    return x >= obj.getX()
      && x <= obj.getX() + obj.getWidth()
      && y >= obj.getY()
      && y <= obj.getY() + obj.getHeight();
  }
}
