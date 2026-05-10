/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3.input;

import java.util.Locale;
import java.util.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import me.stringdotjar.flixelgdx.input.mouse.FlixelMouseIconManager;
import me.stringdotjar.flixelgdx.input.mouse.FlixelNativeMouseCursor;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

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

  @Override
  public void setNativeCursor(@NotNull FlixelNativeMouseCursor cursor) {
    Objects.requireNonNull(cursor, "cursor");
    if (!(Gdx.graphics instanceof Lwjgl3Graphics graphics)) {
      return;
    }
    graphics.getWindow().postRunnable(() ->
      setSystemCursorSwallowCursorUnavailable(mapToBestSystemCursor(cursor)));
  }

  @Override
  public void clearNativeCursor() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics graphics)) {
      return;
    }
    graphics.getWindow().postRunnable(() ->
      setSystemCursorSwallowCursorUnavailable(Cursor.SystemCursor.Arrow));
  }

  @Override
  public boolean supportsNativeCursor() {
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

  private static Cursor.SystemCursor mapToBestSystemCursor(FlixelNativeMouseCursor cursor) {
    if (cursor == FlixelNativeMouseCursor.WAIT
      || cursor == FlixelNativeMouseCursor.GRAB
      || cursor == FlixelNativeMouseCursor.GRABBING) {
      return Cursor.SystemCursor.Arrow;
    }
    if (likelyLinuxOs()) {
      if (cursor == FlixelNativeMouseCursor.NORTH_WEST_SOUTH_EAST_RESIZE
        || cursor == FlixelNativeMouseCursor.NORTH_EAST_SOUTH_WEST_RESIZE
        || cursor == FlixelNativeMouseCursor.NOT_ALLOWED) {
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
