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

  public boolean pressed(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && Gdx.input.isButtonPressed(button);
  }

  public boolean justPressed(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && justPressed[button];
  }

  public boolean justReleased(int button) {
    return enabled && button >= 0 && button <= MAX_BUTTON && justReleased[button];
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
