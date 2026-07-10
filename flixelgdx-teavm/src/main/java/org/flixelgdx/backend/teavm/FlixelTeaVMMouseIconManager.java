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
package org.flixelgdx.backend.teavm;

import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.input.mouse.FlixelMouseIconManager;
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
  public void setCursor(@NotNull FlixelMouseCursor cursor) {
    setCanvasCursorCss(canvasElementId, cssFor(cursor));
  }

  @Override
  public void resetCursor() {
    setCanvasCursorCss(canvasElementId, "default");
  }

  @Override
  public boolean supportsCursors() {
    return !canvasElementId.isEmpty();
  }

  private static String cssFor(FlixelMouseCursor cursor) {
    return switch (cursor) {
      case ARROW -> "default";
      case IBEAM -> "text";
      case WAIT -> "wait";
      case CROSSHAIR -> "crosshair";
      case HAND -> "pointer";
      case GRAB -> "grab";
      case GRABBING -> "grabbing";
      case HORIZONTAL_RESIZE -> "ew-resize";
      case VERTICAL_RESIZE -> "ns-resize";
      case NORTH_WEST_SOUTH_EAST_RESIZE -> "nwse-resize";
      case NORTH_EAST_SOUTH_WEST_RESIZE -> "nesw-resize";
      case ALL_RESIZE -> "move";
      case NOT_ALLOWED -> "not-allowed";
      case NONE -> "none";
    };
  }

  @JSBody(params = { "canvasId", "css" }, script = "var e=document.getElementById(canvasId);\n"
      + "if (e !== null) {\n"
      + "  e.style.cursor = css;\n"
      + "}\n")
  private static native void setCanvasCursorCss(String canvasId, String css);
}
