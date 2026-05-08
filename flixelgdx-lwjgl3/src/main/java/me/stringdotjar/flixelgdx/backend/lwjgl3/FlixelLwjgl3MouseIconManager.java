/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import java.util.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import me.stringdotjar.flixelgdx.input.mouse.FlixelMouseIconManager;
import me.stringdotjar.flixelgdx.input.mouse.FlixelNativeMouseCursor;

import org.jetbrains.annotations.NotNull;

/**
 * LWJGL3 mapping for {@link FlixelMouseIconManager} via {@link com.badlogic.gdx.Graphics#setSystemCursor(Cursor.SystemCursor)}.
 */
public final class FlixelLwjgl3MouseIconManager implements FlixelMouseIconManager {

  @Override
  public void setNativeCursor(@NotNull FlixelNativeMouseCursor cursor) {
    Cursor.SystemCursor mapped = map(Objects.requireNonNull(cursor, "cursor"));
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    g.getWindow().postRunnable(() -> Gdx.graphics.setSystemCursor(mapped));
  }

  @Override
  public void clearNativeCursor() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    g.getWindow().postRunnable(() -> Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow));
  }

  @Override
  public boolean supportsNativeCursor() {
    return Gdx.graphics instanceof Lwjgl3Graphics;
  }

  private static Cursor.SystemCursor map(FlixelNativeMouseCursor cursor) {
    return switch (cursor) {
      case ARROW, WAIT -> Cursor.SystemCursor.Arrow;
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
