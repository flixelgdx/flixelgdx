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
 * <p>Any preset except {@link FlixelMouseCursor#NONE} can also be given a custom image. The browser
 * downloads the image itself from the published {@code assets/} folder, so only graphics loaded
 * from a bundled asset path can be used. Graphics created at runtime (such as those made with
 * {@code makeGraphic(...)}) are ignored with a warning. Most browsers also refuse cursor images
 * larger than 128 by 128 pixels, and 32 by 32 pixels is the safest size across browsers. If an
 * image fails to load, the browser shows its default arrow.
 *
 * <p>Each preset's CSS value is built once when its image changes, so {@link #setCursor(FlixelMouseCursor)}
 * is cheap enough to call every frame (for example, when syncing the cursor from a UI pointer). The
 * canvas style is only written when the value actually changes. Cursor changes made before the
 * canvas exists are remembered and applied once the runner binds the canvas.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.mouse.icons.setCustomCursor(FlixelMouseCursor.ARROW, pointerGraphic);
 * Flixel.mouse.icons.setCursor(FlixelMouseCursor.HAND);
 * Flixel.mouse.icons.resetCursor(); // Shows pointerGraphic again.
 * }</pre>
 */
public class FlixelHtml5MouseIconManager implements FlixelMouseIconManager {

  /** The CSS cursor keyword for each {@link FlixelMouseCursor} ordinal. */
  private static final String[] KEYWORDS = {
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

  /** The CSS value shown for each {@link FlixelMouseCursor} ordinal, including any custom image. */
  private final String[] styles = KEYWORDS.clone();

  private HTMLCanvasElement canvas;
  private FlixelMouseCursor current = FlixelMouseCursor.ARROW;

  /** The CSS value last written to the canvas, or {@code null} when nothing has been written yet. */
  private String applied;

  /**
   * Binds this manager to the canvas the game renders into and applies the current cursor to it.
   *
   * <p>Called once by the runner during startup, after the canvas has been found or created.
   *
   * @param canvas The game canvas.
   */
  public void bind(@NotNull HTMLCanvasElement canvas) {
    this.canvas = canvas;
    applied = null;
    apply();
  }

  @Override
  public void setCursor(@NotNull FlixelMouseCursor cursor) {
    current = cursor;
    apply();
  }

  @Override
  public void resetCursor() {
    setCursor(FlixelMouseCursor.ARROW);
  }

  /**
   * Gives a cursor preset a custom image loaded from a bundled asset.
   *
   * <p>The browser loads the image from the published {@code assets/} folder using the graphic's
   * original path. Graphics that own a runtime-created texture have no asset path, so they are
   * ignored with a warning, as is {@link FlixelMouseCursor#NONE}.
   *
   * @param cursor The preset to give an image; {@link FlixelMouseCursor#NONE} is ignored.
   * @param image The cursor image, or {@code null} to go back to the browser cursor for this preset.
   * @param hotspotX The click point's X offset from the image's left edge, in pixels.
   * @param hotspotY The click point's Y offset from the image's top edge, in pixels.
   */
  @Override
  public void setCustomCursor(@NotNull FlixelMouseCursor cursor, @Nullable FlixelGraphic image, int hotspotX,
      int hotspotY) {
    if (cursor == FlixelMouseCursor.NONE) {
      Flixel.warn("FlixelMouseCursor.NONE hides the cursor, so it cannot have a custom image.");
      return;
    }
    int ordinal = cursor.ordinal();
    if (image == null) {
      styles[ordinal] = KEYWORDS[ordinal];
      apply();
      return;
    }
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
    styles[ordinal] = "url(\"" + FlixelHtml5Files.ASSET_ROOT + escape(path) + "\") "
        + Math.max(0, hotspotX) + " " + Math.max(0, hotspotY) + ", " + KEYWORDS[ordinal];
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
   * Writes the active cursor's CSS value onto the canvas, if one is bound and the value changed.
   */
  private void apply() {
    String style = styles[current.ordinal()];
    // Identity comparison is enough, since each style string is only replaced when its image changes.
    if (canvas == null || style == applied) {
      return;
    }
    canvas.getStyle().setProperty("cursor", style);
    applied = style;
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
