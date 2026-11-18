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
package org.flixelgdx.backend.html5.input;

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.html5.file.FlixelHtml5Files;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.input.mouse.FlixelMouseCursor;
import org.flixelgdx.input.mouse.FlixelMouseIconManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.dom.html.HTMLCanvasElement;

/**
 * Web cursor manager that styles the game canvas with the CSS {@code cursor} property.
 *
 * <p>A browser does not let a page swap the operating system cursor directly. Instead, every element
 * on the page can say which cursor should show while the pointer is over it, much like a shop sign
 * that tells visitors how to behave only while they are inside that shop. This manager sets that
 * sign on the game canvas, so the cursor changes while the pointer is over the game and goes back
 * to normal as soon as it leaves.
 *
 * <p>Every {@link FlixelMouseCursor} preset maps to a standard CSS cursor keyword, including
 * {@link FlixelMouseCursor#GRAB} and {@link FlixelMouseCursor#GRABBING}, which have no desktop
 * equivalent. {@link FlixelMouseCursor#ALL_RESIZE} uses the {@code move} keyword.
 *
 * <p>Custom cursor images are supported for graphics loaded from a bundled asset path. The browser
 * downloads the image itself from the published {@code assets/} folder, so graphics created at
 * runtime (such as those made with {@code makeGraphic(...)}) cannot be used as cursors and are
 * ignored with a warning. Most browsers also refuse cursor images larger than 128 by 128 pixels,
 * and 32 by 32 pixels is the safest size across browsers.
 *
 * <p>Cursor changes made before the canvas exists are remembered and applied once the runner binds
 * the canvas.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.mouse.icons.setCursor(FlixelMouseCursor.HAND);
 * Flixel.mouse.icons.setCustomCursor(cursorGraphic, 0, 0);
 * Flixel.mouse.icons.resetCursor();
 * }</pre>
 */
public class FlixelHtml5MouseIconManager implements FlixelMouseIconManager {

  /** The CSS cursor keyword for each {@link FlixelMouseCursor} ordinal. */
  private static final String[] CSS_CURSORS = {
      "default",     // ARROW
      "text",        // IBEAM
      "wait",        // WAIT
      "crosshair",   // CROSSHAIR
      "pointer",     // HAND
      "grab",        // GRAB
      "grabbing",    // GRABBING
      "ew-resize",   // HORIZONTAL_RESIZE
      "ns-resize",   // VERTICAL_RESIZE
      "nwse-resize", // NORTH_WEST_SOUTH_EAST_RESIZE
      "nesw-resize", // NORTH_EAST_SOUTH_WEST_RESIZE
      "move",        // ALL_RESIZE
      "not-allowed", // NOT_ALLOWED
      "none",        // NONE
  };

  private HTMLCanvasElement canvas;
  private FlixelMouseCursor current = FlixelMouseCursor.ARROW;
  private String style = CSS_CURSORS[FlixelMouseCursor.ARROW.ordinal()];

  /**
   * Binds this manager to the canvas the game renders into and applies the current cursor to it.
   *
   * <p>Called once by the runner during startup, after the canvas has been found or created.
   *
   * @param canvas The game canvas.
   */
  public void bind(@NotNull HTMLCanvasElement canvas) {
    this.canvas = canvas;
    apply();
  }

  @Override
  public void setCursor(@NotNull FlixelMouseCursor cursor) {
    current = cursor;
    style = CSS_CURSORS[cursor.ordinal()];
    apply();
  }

  @Override
  public void resetCursor() {
    setCursor(FlixelMouseCursor.ARROW);
  }

  /**
   * Replaces the cursor over the canvas with a bundled image asset.
   *
   * <p>The browser loads the image from the published {@code assets/} folder using the graphic's
   * original path. If the image cannot be loaded or is too large, the browser falls back to the
   * default arrow. Graphics that own a runtime-created texture have no asset path, so they are
   * ignored with a warning.
   *
   * @param image The cursor image; must not be {@code null}.
   * @param hotspotX The click point's X offset from the image's left edge, in pixels.
   * @param hotspotY The click point's Y offset from the image's top edge, in pixels.
   */
  @Override
  public void setCustomCursor(@NotNull FlixelGraphic image, int hotspotX, int hotspotY) {
    if (image.isOwned()) {
      Flixel.warn("Cannot use runtime-created graphic '{}' as a web cursor. Use a bundled image asset instead.",
          image.getPath());
      return;
    }
    String path = image.getPath();
    // Asset paths may start with a slash, which would make the URL point at the site root instead of the assets folder.
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    // A CSS cursor needs a fallback keyword after the image, used while the image is loading or if it fails.
    style = "url(\"" + FlixelHtml5Files.ASSET_ROOT + escape(path) + "\") "
        + Math.max(0, hotspotX) + " " + Math.max(0, hotspotY) + ", auto";
    apply();
  }

  @Override
  @NotNull
  public FlixelMouseCursor getCursor() {
    return current;
  }

  @Override
  public boolean supportsCursors() {
    return true;
  }

  @Override
  public boolean supportsCustomCursors() {
    return true;
  }

  /**
   * Writes the current cursor style onto the canvas, if one is bound yet.
   */
  private void apply() {
    if (canvas != null) {
      canvas.getStyle().setProperty("cursor", style);
    }
  }

  /**
   * Escapes the characters that would end a quoted CSS {@code url("...")} string early.
   *
   * @param path The asset path to escape.
   * @return The path, safe to place inside double quotes in CSS.
   */
  @NotNull
  private static String escape(@NotNull String path) {
    if (path.indexOf('"') < 0 && path.indexOf('\\') < 0) {
      return path;
    }
    return path.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  @Nullable
  public HTMLCanvasElement getCanvas() {
    return canvas;
  }
}
