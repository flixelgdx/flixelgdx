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
package org.flixelgdx.input.touch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.input.FlixelInputProcessorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Multitouch input manager backed by libGDX's {@link InputProcessor} callbacks.
 *
 * <p>Access via {@link org.flixelgdx.Flixel#touches Flixel.touches} after the framework is
 * initialized. The manager tracks up to {@link #getMaxPointers()} simultaneous fingers in the
 * {@link #list} array. Each slot is a reused {@link FlixelTouch} instance; slot {@code 0} always
 * corresponds to pointer index {@code 0} (the first finger), slot {@code 1} to index {@code 1},
 * and so on.
 *
 * <pre>{@code
 * // Check the first finger each frame.
 * if (Flixel.touches.list[0].isJustPressed()) {
 *   spawnEffect(Flixel.touches.list[0].worldX, Flixel.touches.list[0].worldY);
 * }
 *
 * // React to any active touch.
 * if (Flixel.touches.anyTouched()) {
 *   player.move(Flixel.touches.list[0].worldX, Flixel.touches.list[0].worldY);
 * }
 *
 * // Detect a tap on a sprite using world coordinates.
 * if (Flixel.touches.justTouchedWorld(
 *         mySprite.getX(), mySprite.getY(),
 *         mySprite.getWidth(), mySprite.getHeight())) {
 *   onButtonTapped();
 * }
 *
 * // Track whether a finger is currently held inside a screen region.
 * if (Flixel.touches.touchingScreen(0, 0, halfWidth, screenHeight)) {
 *   moveLeft();
 * }
 * }</pre>
 *
 * <h2>Coordinate systems</h2>
 *
 * <p>Each {@link FlixelTouch} carries both screen and world coordinates. Screen coordinates use
 * libGDX's top-left origin (Y increases downward). World coordinates are unprojected via the
 * manager's active camera and use the standard bottom-left origin (Y increases upward), matching
 * the rest of the scene. Set a custom camera with {@link #setWorldCamera(FlixelCamera)};
 * otherwise the first camera in {@link Flixel#cameras} is used.
 *
 * <h2>Frame contract</h2>
 *
 * <p>The {@link InputProcessor} is the authoritative source for {@code justPressed},
 * {@code justReleased}, and {@code justCancelled} flags; they are set inside the callbacks and
 * cleared by {@link #endFrame()}. {@link #update()} refreshes screen and world coordinates each
 * frame for all active pointers.
 *
 * <h2>Max pointer count</h2>
 *
 * <p>The default limit is {@code 10}, which covers the maximum simultaneous touches supported by
 * virtually all Android hardware. Call {@link #setMaxPointers(int)} to raise or lower the limit;
 * existing live touch state is preserved for pointers that fall within the new size.
 */
public class FlixelTouchManager implements FlixelInputProcessorManager {

  /** Default maximum number of simultaneous touch pointers tracked. */
  public static final int DEFAULT_MAX_POINTERS = 10;

  /**
   * Per-pointer state array. Read {@link FlixelTouch} instances directly for zero-overhead access.
   * Do not reassign this field from outside the manager; use {@link #setMaxPointers(int)} instead.
   *
   * <p>Every slot is always a non-null {@link FlixelTouch}. Check {@link FlixelTouch#pressed()}
   * to determine whether a slot represents an active finger.
   */
  public FlixelTouch[] list;

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
      if (pointer >= list.length) {
        return false;
      }
      FlixelTouch t = list[pointer];
      t.screenX = screenX;
      t.screenY = screenY;
      t.pressed = true;
      t.justPressed = true;
      return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
      if (pointer >= list.length) {
        return false;
      }
      FlixelTouch t = list[pointer];
      t.screenX = screenX;
      t.screenY = screenY;
      t.pressed = false;
      t.justReleased = true;
      t.dragging = false;
      return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
      if (pointer >= list.length) {
        return false;
      }
      FlixelTouch t = list[pointer];
      t.pressed = false;
      t.justCancelled = true;
      t.dragging = false;
      return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
      if (pointer >= list.length) {
        return false;
      }
      FlixelTouch t = list[pointer];
      t.screenX = screenX;
      t.screenY = screenY;
      t.dragging = true;
      return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
      return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
      return false;
    }
  };

  private int maxPointers;

  /**
   * When {@code false}, all queries return inactive state.
   *
   * <p>Note that this is typically {@code false} by default, although mobile backends will
   * automatically enable the touch manager when a game is compiled for mobile platforms.
   */
  public boolean enabled = false;

  public FlixelTouchManager() {
    this(DEFAULT_MAX_POINTERS);
  }

  public FlixelTouchManager(int maxPointers) {
    this.maxPointers = maxPointers;
    list = new FlixelTouch[maxPointers];
    for (int i = 0; i < maxPointers; i++) {
      FlixelTouch t = new FlixelTouch();
      t.pointer = i;
      list[i] = t;
    }
  }

  /**
   * Resizes the pointer limit and reallocates {@link #list}. Existing {@link FlixelTouch} objects
   * are preserved for indices that fall within the new size; new slots beyond the old size receive
   * fresh instances. Reducing the limit discards state for pointers above the new ceiling.
   *
   * @param n New maximum pointer count (must be positive).
   */
  public void setMaxPointers(int n) {
    if (n <= 0) {
      throw new IllegalArgumentException("Max pointers must be greater than zero.");
    }
    if (n == maxPointers) {
      return;
    }
    FlixelTouch[] newList = new FlixelTouch[n];
    int keep = Math.min(maxPointers, n);
    System.arraycopy(list, 0, newList, 0, keep);
    for (int i = keep; i < n; i++) {
      FlixelTouch t = new FlixelTouch();
      t.pointer = i;
      newList[i] = t;
    }
    list = newList;
    maxPointers = n;
  }

  /** Returns the current maximum number of tracked pointers. */
  public int getMaxPointers() {
    return maxPointers;
  }

  /**
   * Returns the stable {@link InputProcessor} that must be registered on the
   * {@link com.badlogic.gdx.InputMultiplexer InputMultiplexer} for the lifetime of the game
   * session. Registered automatically by {@code FlixelGame.create()}.
   */
  @Override
  @NotNull
  public InputProcessor getInputProcessor() {
    return inputProcessor;
  }

  /**
   * Refreshes screen and world coordinates for all active pointers. Called once per frame by
   * {@code FlixelGame.update()} before game logic runs.
   */
  @Override
  public void update() {
    if (!enabled) {
      return;
    }
    FlixelCamera cam = worldCamera != null ? worldCamera : safeGetDefaultCamera();
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      if (!t.pressed) {
        continue;
      }
      t.screenX = Gdx.input.getX(p);
      t.screenY = Gdx.input.getY(p);
      if (cam != null) {
        tmpUnproject.set(t.screenX, t.screenY);
        cam.getViewport().unproject(tmpUnproject);
        t.worldX = tmpUnproject.x + cam.getViewX();
        t.worldY = tmpUnproject.y + cam.getViewY();
      } else {
        t.worldX = t.screenX;
        t.worldY = t.screenY;
      }
    }
  }

  /**
   * Clears per-frame edge flags ({@link FlixelTouch#justPressed()},
   * {@link FlixelTouch#justReleased()}, {@link FlixelTouch#justCancelled()}) for all pointers.
   * Called once per frame by {@code FlixelGame.render()} after game logic and drawing finish.
   */
  @Override
  public void endFrame() {
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      t.justPressed = false;
      t.justReleased = false;
      t.justCancelled = false;
    }
  }

  /** Resets all pointer state to inactive without changing the pointer limit or camera. */
  @Override
  public void reset() {
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      t.pressed = false;
      t.justPressed = false;
      t.justReleased = false;
      t.dragging = false;
      t.justCancelled = false;
      t.screenX = 0;
      t.screenY = 0;
      t.worldX = 0f;
      t.worldY = 0f;
    }
  }

  /**
   * Sets the camera used to unproject screen coordinates to world coordinates in {@link #update()}.
   * Pass {@code null} to fall back to the first camera in {@link Flixel#cameras}.
   *
   * @param camera Camera to use for unprojection, or {@code null} for the default.
   */
  public void setWorldCamera(@Nullable FlixelCamera camera) {
    this.worldCamera = camera;
  }

  /** Returns the camera used for world coordinate unprojection, or {@code null} if using default. */
  @Nullable
  public FlixelCamera getWorldCamera() {
    return worldCamera;
  }

  /**
   * Returns {@code true} while the given pointer index is currently in contact with the screen.
   *
   * @param pointer Zero-based pointer index.
   * @return {@code true} if the pointer is pressed and input is enabled.
   */
  public boolean pressed(int pointer) {
    return enabled && pointer >= 0 && pointer < maxPointers && list[pointer].pressed;
  }

  /**
   * Returns {@code true} on the single frame the given pointer first touched the screen.
   *
   * @param pointer Zero-based pointer index.
   * @return {@code true} if the pointer was just pressed and input is enabled.
   */
  public boolean justPressed(int pointer) {
    return enabled && pointer >= 0 && pointer < maxPointers && list[pointer].justPressed;
  }

  /**
   * Returns {@code true} on the single frame the given pointer lifted off the screen.
   *
   * @param pointer Zero-based pointer index.
   * @return {@code true} if the pointer was just released and input is enabled.
   */
  public boolean justReleased(int pointer) {
    return enabled && pointer >= 0 && pointer < maxPointers && list[pointer].justReleased;
  }

  /**
   * Returns {@code true} if any tracked pointer is currently in contact with the screen.
   *
   * @return {@code true} if at least one pointer is pressed and input is enabled.
   */
  public boolean anyTouched() {
    if (!enabled) {
      return false;
    }
    for (int p = 0; p < maxPointers; p++) {
      if (list[p].pressed) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the number of pointers currently in contact with the screen.
   *
   * @return Active pointer count, or {@code 0} when input is disabled.
   */
  public int count() {
    if (!enabled) {
      return 0;
    }
    int c = 0;
    for (int p = 0; p < maxPointers; p++) {
      if (list[p].pressed) {
        c++;
      }
    }
    return c;
  }

  /**
   * Returns {@code true} while any active pointer is inside the given world-space rectangle.
   *
   * <p>Coordinates use the bottom-left origin (Y increases upward), matching the game world.
   * Use this to track a finger held inside an area. For detecting the moment a finger lands,
   * use {@link #justTouchedWorld(float, float, float, float)} instead.
   *
   * <pre>{@code
   * if (Flixel.touches.touchingWorld(button.getX(), button.getY(),
   *         button.getWidth(), button.getHeight())) {
   *   button.setHighlighted(true);
   * }
   * }</pre>
   *
   * @param x Left edge of the rectangle in world units.
   * @param y Bottom edge of the rectangle in world units.
   * @param width Width of the rectangle in world units.
   * @param height Height of the rectangle in world units.
   * @return {@code true} if at least one pressed pointer is inside the rectangle.
   */
  public boolean touchingWorld(float x, float y, float width, float height) {
    if (!enabled) {
      return false;
    }
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      if (t.pressed
          && t.worldX >= x && t.worldX <= x + width
          && t.worldY >= y && t.worldY <= y + height) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} while any active pointer is inside the given screen-space rectangle.
   *
   * <p>Coordinates use the top-left origin (Y increases downward), matching libGDX screen space.
   * Use this to track a finger held inside an area. For detecting the moment a finger lands,
   * use {@link #justTouchedScreen(float, float, float, float)} instead.
   *
   * <pre>{@code
   * float half = Gdx.graphics.getWidth() / 2f;
   * if (Flixel.touches.touchingScreen(half, 0, half, Gdx.graphics.getHeight())) {
   *   moveRight();
   * }
   * }</pre>
   *
   * @param x Left edge of the rectangle in screen pixels.
   * @param y Top edge of the rectangle in screen pixels.
   * @param width Width of the rectangle in screen pixels.
   * @param height Height of the rectangle in screen pixels.
   * @return {@code true} if at least one pressed pointer is inside the rectangle.
   */
  public boolean touchingScreen(float x, float y, float width, float height) {
    if (!enabled) {
      return false;
    }
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      if (t.pressed
          && t.screenX >= x && t.screenX <= x + width
          && t.screenY >= y && t.screenY <= y + height) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} on the single frame any pointer first touches the given world-space rectangle.
   *
   * <p>Coordinates use the bottom-left origin (Y increases upward), matching the game world.
   * Clears to {@code false} after the frame ends, matching the timing of
   * {@link FlixelTouch#justPressed()}.
   *
   * <pre>{@code
   * if (Flixel.touches.justTouchedWorld(button.getX(), button.getY(),
   *         button.getWidth(), button.getHeight())) {
   *   onButtonTapped();
   * }
   * }</pre>
   *
   * @param x Left edge of the rectangle in world units.
   * @param y Bottom edge of the rectangle in world units.
   * @param width Width of the rectangle in world units.
   * @param height Height of the rectangle in world units.
   * @return {@code true} if any pointer was just pressed inside the rectangle this frame.
   */
  public boolean justTouchedWorld(float x, float y, float width, float height) {
    if (!enabled) {
      return false;
    }
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      if (t.justPressed
          && t.worldX >= x && t.worldX <= x + width
          && t.worldY >= y && t.worldY <= y + height) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} on the single frame any pointer first touches the given screen-space rectangle.
   *
   * <p>Coordinates use the top-left origin (Y increases downward), matching libGDX screen space.
   * Clears to {@code false} after the frame ends, matching the timing of
   * {@link FlixelTouch#justPressed()}.
   *
   * <pre>{@code
   * if (Flixel.touches.justTouchedScreen(0, 0, Gdx.graphics.getWidth() / 2f,
   *         Gdx.graphics.getHeight())) {
   *   onLeftSideTapped();
   * }
   * }</pre>
   *
   * @param x Left edge of the rectangle in screen pixels.
   * @param y Top edge of the rectangle in screen pixels.
   * @param width Width of the rectangle in screen pixels.
   * @param height Height of the rectangle in screen pixels.
   * @return {@code true} if any pointer was just pressed inside the rectangle this frame.
   */
  public boolean justTouchedScreen(float x, float y, float width, float height) {
    if (!enabled) {
      return false;
    }
    for (int p = 0; p < maxPointers; p++) {
      FlixelTouch t = list[p];
      if (t.justPressed
          && t.screenX >= x && t.screenX <= x + width
          && t.screenY >= y && t.screenY <= y + height) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static FlixelCamera safeGetDefaultCamera() {
    try {
      return Flixel.cameras.first();
    } catch (Exception e) {
      return null;
    }
  }
}
