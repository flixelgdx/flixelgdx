/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm;

import me.stringdotjar.flixelgdx.input.mouse.FlixelMouseIconManager;
import me.stringdotjar.flixelgdx.input.mouse.FlixelNativeMouseCursor;

import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;

/**
 * Sets the HTML5 canvas CSS {@code cursor} for TeaVM builds when a {@code canvas} element ID is known.
 */
public final class FlixelTeaVMMouseIconManager implements FlixelMouseIconManager {

  private final @NotNull String canvasElementId;

  public FlixelTeaVMMouseIconManager(@NotNull String canvasElementId) {
    this.canvasElementId = canvasElementId;
  }

  @Override
  public void setNativeCursor(@NotNull FlixelNativeMouseCursor cursor) {
    setCanvasCursorCss(canvasElementId, cssFor(cursor));
  }

  @Override
  public void clearNativeCursor() {
    setCanvasCursorCss(canvasElementId, "default");
  }

  @Override
  public boolean supportsNativeCursor() {
    return !canvasElementId.isEmpty();
  }

  private static String cssFor(FlixelNativeMouseCursor cursor) {
    return switch (cursor) {
      case ARROW -> "default";
      case IBEAM -> "text";
      case WAIT -> "wait";
      case CROSSHAIR -> "crosshair";
      case HAND -> "pointer";
      case HORIZONTAL_RESIZE -> "ew-resize";
      case VERTICAL_RESIZE -> "ns-resize";
      case NORTH_WEST_SOUTH_EAST_RESIZE -> "nwse-resize";
      case NORTH_EAST_SOUTH_WEST_RESIZE -> "nesw-resize";
      case ALL_RESIZE -> "move";
      case NOT_ALLOWED -> "not-allowed";
      case NONE -> "none";
    };
  }

  @JSBody(params = {"canvasId", "css"}, script = "var e=document.getElementById(canvasId);\n"
      + "if (e !== null) {\n"
      + "  e.style.cursor = css;\n"
      + "}\n")
  private static native void setCanvasCursorCss(String canvasId, String css);
}
