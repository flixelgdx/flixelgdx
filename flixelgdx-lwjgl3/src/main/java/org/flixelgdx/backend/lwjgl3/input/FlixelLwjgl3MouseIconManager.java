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
package org.flixelgdx.backend.lwjgl3.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Cursor;

import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.input.mouse.FlixelMouseIconManager;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.Locale;
import java.util.Objects;

/**
 * LWJGL3 mapping for {@link FlixelMouseIconManager} using libGDX system cursors.
 *
 * <p>GLFW has no universal standard cursor for busy, grab, or dragging hands, and libGDX does not expose a wait shape. Those
 * presets resolve to {@link Cursor.SystemCursor#Arrow} here without printing noise.
 *
 * <p>Several Linux desktops (often under X11) ship cursor themes without diagonal resize or not-allowed glyphs, so GLFW can
 * only report {@link GLFW#GLFW_CURSOR_UNAVAILABLE}. Those presets downgrade to Arrow on Linux while
 * the console stays filtered. Prefer an HTML/CSS based Flixel backend when you rely on richer browser-native cursor presets.
 */
public final class FlixelLwjgl3MouseIconManager implements FlixelMouseIconManager {

  private FlixelMouseCursor current = FlixelMouseCursor.ARROW;

  @Override
  public void setCursor(@NotNull FlixelMouseCursor cursor) {
    Objects.requireNonNull(cursor, "cursor");
    current = cursor;
    if (!(Gdx.graphics instanceof Lwjgl3Graphics graphics)) {
      return;
    }
    graphics.getWindow().postRunnable(() -> setSystemCursorSwallowCursorUnavailable(mapToBestSystemCursor(cursor)));
  }

  @Override
  public void resetCursor() {
    current = FlixelMouseCursor.ARROW;
    if (!(Gdx.graphics instanceof Lwjgl3Graphics graphics)) {
      return;
    }
    graphics.getWindow().postRunnable(() -> setSystemCursorSwallowCursorUnavailable(Cursor.SystemCursor.Arrow));
  }

  @Override
  @NotNull
  public FlixelMouseCursor getCursor() {
    return current;
  }

  @Override
  public boolean supportsCursors() {
    return Gdx.graphics instanceof Lwjgl3Graphics;
  }

  /**
   * Temporarily installs a GLFW error callback that skips {@link GLFW#GLFW_CURSOR_UNAVAILABLE} chatter while forwarding every
   * other code to whichever callback GLFW had installed immediately before {@code glfwSetErrorCallback(...)} swapped in this filter.
   *
   * @param systemCursor Cursor preset to publish through libGDX.
   */
  private static void setSystemCursorSwallowCursorUnavailable(Cursor.SystemCursor systemCursor) {
    GLFWErrorCallback[] delegate = new GLFWErrorCallback[1];
    GLFWErrorCallback filter = GLFWErrorCallback.create((error, description) -> {
      if (error == GLFW.GLFW_CURSOR_UNAVAILABLE) {
        return;
      }
      GLFWErrorCallback chained = delegate[0];
      if (chained != null) {
        chained.invoke(error, description);
      }
    });
    delegate[0] = GLFW.glfwSetErrorCallback(filter);
    try {
      Gdx.graphics.setSystemCursor(systemCursor);
    } finally {
      GLFW.glfwSetErrorCallback(delegate[0]);
    }
  }

  private static boolean likelyLinuxOs() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("linux");
  }

  private static Cursor.SystemCursor mapToBestSystemCursor(FlixelMouseCursor cursor) {
    if (cursor == FlixelMouseCursor.WAIT
        || cursor == FlixelMouseCursor.GRAB
        || cursor == FlixelMouseCursor.GRABBING) {
      return Cursor.SystemCursor.Arrow;
    }
    if (likelyLinuxOs()) {
      if (cursor == FlixelMouseCursor.NORTH_WEST_SOUTH_EAST_RESIZE
          || cursor == FlixelMouseCursor.NORTH_EAST_SOUTH_WEST_RESIZE
          || cursor == FlixelMouseCursor.NOT_ALLOWED) {
        return Cursor.SystemCursor.Arrow;
      }
    }
    return switch (cursor) {
      case ARROW, WAIT, GRAB, GRABBING -> Cursor.SystemCursor.Arrow;
      case IBEAM -> Cursor.SystemCursor.Ibeam;
      case CROSSHAIR -> Cursor.SystemCursor.Crosshair;
      case HAND -> Cursor.SystemCursor.Hand;
      case HORIZONTAL_RESIZE -> Cursor.SystemCursor.HorizontalResize;
      case VERTICAL_RESIZE -> Cursor.SystemCursor.VerticalResize;
      case NORTH_WEST_SOUTH_EAST_RESIZE -> Cursor.SystemCursor.NWSEResize;
      case NORTH_EAST_SOUTH_WEST_RESIZE -> Cursor.SystemCursor.NESWResize;
      case ALL_RESIZE -> Cursor.SystemCursor.AllResize;
      case NOT_ALLOWED -> Cursor.SystemCursor.NotAllowed;
      case NONE -> Cursor.SystemCursor.None;
    };
  }
}
