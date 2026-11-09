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
package org.flixelgdx.debug.ui;

import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A 2D view of boxes that the person can click, drag, pan, and zoom, such as a live map of what a game camera shows.
 *
 * <p>The game describes a view of a certain size ({@linkplain #setView(float, float) in view units}, for example the
 * visible area of a camera) and the boxes inside it. The app scales the view to fit the widget, draws the boxes, and
 * turns gestures into events in view units, so the game never has to know how big the window is. Think of it as a
 * glass table with paper cutouts on it: the game places the cutouts, and the person can tap one, slide one, or move
 * the whole table.
 *
 * <pre>{@code
 * canvas.setView(camera.width, camera.height)
 *     .setInteract(FlixelDebugProtocol.INTERACT_PICK | FlixelDebugProtocol.INTERACT_DRAG)
 *     .setListener(new FlixelDebugCanvas.Listener() {
 *       public void onPick(float x, float y) { select(x, y); }
 *       public void onDrag(long id, float dx, float dy) { move(id, dx, dy); }
 *     });
 *
 * canvas.beginShapes();
 * canvas.shape(player.id, player.x, player.y, 16, 16, FlixelDebugProtocol.TONE_ACCENT, "Player");
 * canvas.endShapes();
 * }</pre>
 *
 * <p>While the person drags a box, the app moves it on its own so the drag feels instant, and the game's next
 * shapes put it where the game decided. A picture can be drawn under the boxes with
 * {@link #setBackground(byte[], int, int)}.
 */
public class FlixelDebugCanvas extends FlixelDebugWidget<FlixelDebugCanvas> {

  private final FlixelDebugListBuffer shapes = new FlixelDebugListBuffer();
  private FlixelDebugMessageWriter builder;
  private byte[] background = new byte[0];
  private Listener listener;
  private float viewWidth = 1f;
  private float viewHeight = 1f;
  private int backgroundLength;
  private int interact;
  private int shapeCount;

  private boolean smooth;
  private boolean building;

  /** Creates an empty canvas with a view of 1 by 1. */
  public FlixelDebugCanvas() {
    super(FlixelDebugProtocol.WIDGET_CANVAS);
  }

  /** Starts replacing the shapes. Add them with {@link #shape}, then call {@link #endShapes()}. */
  public void beginShapes() {
    builder = shapes.begin();
    shapeCount = 0;
    building = true;
  }

  /**
   * Adds a box, drawn on top of the boxes added before it.
   *
   * @param id The box's id, which {@code DRAG} events report.
   * @param x The left edge, in view units.
   * @param y The top edge, in view units.
   * @param width The width, in view units.
   * @param height The height, in view units.
   * @param style A tone combined with {@link FlixelDebugProtocol#SHAPE_SELECTED},
   *   {@link FlixelDebugProtocol#SHAPE_DASHED}, {@link FlixelDebugProtocol#SHAPE_DIM}, or
   *   {@link FlixelDebugProtocol#SHAPE_FILLED}.
   * @param label The text the app shows when the pointer rests on the box; it is copied.
   * @throws IllegalStateException When {@link #beginShapes()} was not called first.
   */
  public void shape(long id, float x, float y, float width, float height, int style, @Nullable CharSequence label) {
    if (!building) {
      throw new IllegalStateException("Call beginShapes() before shape(...).");
    }
    builder.beginList().writeValue(id).writeValue(x).writeValue(y).writeValue(width).writeValue(height)
        .writeValue(style).writeValue(label).endList(7);
    shapeCount++;
  }

  /**
   * Finishes replacing the shapes. They are only sent when they changed.
   *
   * @return {@code true} when the shapes changed.
   */
  public boolean endShapes() {
    building = false;
    if (shapes.end(shapeCount)) {
      markDirty(FlixelDebugProtocol.PROP_SHAPES);
      return true;
    }
    return false;
  }

  /**
   * Draws a picture under the boxes, stretched over the whole view. The bytes are copied.
   *
   * @param png The array that holds a PNG file.
   * @param offset The position of the first byte.
   * @param count How many bytes the PNG has.
   * @return This canvas, for chaining.
   */
  @NotNull
  public FlixelDebugCanvas setBackground(byte @NotNull [] png, int offset, int count) {
    if (background.length < count) {
      background = new byte[count];
    }
    System.arraycopy(png, offset, background, 0, count);
    backgroundLength = count;
    markDirty(FlixelDebugProtocol.PROP_IMAGE);
    return this;
  }

  /**
   * Removes the picture under the boxes.
   *
   * @return This canvas, for chaining.
   */
  @NotNull
  public FlixelDebugCanvas clearBackground() {
    if (backgroundLength != 0) {
      backgroundLength = 0;
      markDirty(FlixelDebugProtocol.PROP_IMAGE);
    }
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_VIEW) | bit(FlixelDebugProtocol.PROP_SHAPES);
    if (interact != 0) {
      mask |= bit(FlixelDebugProtocol.PROP_INTERACT);
    }
    if (backgroundLength > 0) {
      mask |= bit(FlixelDebugProtocol.PROP_IMAGE);
    }
    if (smooth) {
      mask |= bit(FlixelDebugProtocol.PROP_SMOOTH);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_VIEW -> writer.beginList().writeValue(viewWidth).writeValue(viewHeight).endList(2);
      case FlixelDebugProtocol.PROP_SHAPES -> shapes.write(writer);
      case FlixelDebugProtocol.PROP_INTERACT -> writer.writeValue(interact);
      case FlixelDebugProtocol.PROP_IMAGE -> {
        if (backgroundLength > 0) {
          writer.writeBytesValue(background, 0, backgroundLength);
        } else {
          writer.writeNullValue();
        }
      }
      case FlixelDebugProtocol.PROP_SMOOTH -> writer.writeValue(smooth);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (listener == null) {
      return;
    }
    switch (event) {
      case FlixelDebugProtocol.EVENT_PICK -> {
        float x = args.nextFloat(Float.NaN);
        float y = args.nextFloat(Float.NaN);
        if (Float.isFinite(x) && Float.isFinite(y)) {
          listener.onPick(x, y);
        }
      }
      case FlixelDebugProtocol.EVENT_DRAG -> {
        long id = args.nextLong(-1L);
        float dx = args.nextFloat(Float.NaN);
        float dy = args.nextFloat(Float.NaN);
        if (Float.isFinite(dx) && Float.isFinite(dy)) {
          listener.onDrag(id, dx, dy);
        }
      }
      case FlixelDebugProtocol.EVENT_PAN -> {
        float dx = args.nextFloat(Float.NaN);
        float dy = args.nextFloat(Float.NaN);
        if (Float.isFinite(dx) && Float.isFinite(dy)) {
          listener.onPan(dx, dy);
        }
      }
      case FlixelDebugProtocol.EVENT_ZOOM -> {
        float factor = args.nextFloat(Float.NaN);
        float x = args.nextFloat(Float.NaN);
        float y = args.nextFloat(Float.NaN);
        if (Float.isFinite(factor) && factor > 0f && Float.isFinite(x) && Float.isFinite(y)) {
          listener.onZoom(factor, x, y);
        }
      }
      default -> {
      }
    }
  }

  /**
   * Returns how many boxes the canvas holds.
   *
   * @return The count.
   */
  public int getShapeCount() {
    return shapes.getCount();
  }

  public float getViewWidth() {
    return viewWidth;
  }

  public float getViewHeight() {
    return viewHeight;
  }

  /**
   * Sets the size of the view in view units. The app keeps this aspect ratio when it scales the view.
   *
   * @param width The width, above 0.
   * @param height The height, above 0.
   * @return This canvas, for chaining.
   */
  @NotNull
  public FlixelDebugCanvas setView(float width, float height) {
    float w = width > 0f ? width : 1f;
    float h = height > 0f ? height : 1f;
    if (viewWidth != w || viewHeight != h) {
      viewWidth = w;
      viewHeight = h;
      markDirty(FlixelDebugProtocol.PROP_VIEW);
    }
    return this;
  }

  public int getInteract() {
    return interact;
  }

  /**
   * Sets which gestures the person may use.
   *
   * @param interact {@link FlixelDebugProtocol#INTERACT_PICK}, {@link FlixelDebugProtocol#INTERACT_DRAG},
   *   {@link FlixelDebugProtocol#INTERACT_PAN}, and {@link FlixelDebugProtocol#INTERACT_ZOOM}, combined with
   *   {@code |}.
   * @return This canvas, for chaining.
   */
  @NotNull
  public FlixelDebugCanvas setInteract(int interact) {
    if (this.interact != interact) {
      this.interact = interact;
      markDirty(FlixelDebugProtocol.PROP_INTERACT);
    }
    return this;
  }

  public boolean isSmooth() {
    return smooth;
  }

  /**
   * Sets whether the background picture is smoothed when it is scaled.
   *
   * @param smooth {@code true} to smooth it; {@code false} keeps pixel art sharp.
   * @return This canvas, for chaining.
   */
  @NotNull
  public FlixelDebugCanvas setSmooth(boolean smooth) {
    if (this.smooth != smooth) {
      this.smooth = smooth;
      markDirty(FlixelDebugProtocol.PROP_SMOOTH);
    }
    return this;
  }

  @Nullable
  public Listener getListener() {
    return listener;
  }

  /**
   * Sets what runs when the person picks, drags, pans, or zooms.
   *
   * @param listener The listener, or {@code null} for nothing.
   * @return This canvas, for chaining.
   */
  @NotNull
  public FlixelDebugCanvas setListener(@Nullable Listener listener) {
    this.listener = listener;
    return this;
  }

  /**
   * Receives the gestures of a canvas, in view units. Override only the ones you allowed with
   * {@link #setInteract(int)}.
   */
  public interface Listener {

    /**
     * The person clicked a point.
     *
     * @param x The point's x, in view units.
     * @param y The point's y, in view units.
     */
    default void onPick(float x, float y) {}

    /**
     * The person dragged a box.
     *
     * @param id The box's id.
     * @param dx How far the pointer moved since the last drag event, in view units.
     * @param dy How far the pointer moved since the last drag event, in view units.
     */
    default void onDrag(long id, float dx, float dy) {}

    /**
     * The person dragged the view with the right or middle button.
     *
     * @param dx How far the pointer moved since the last pan event, in view units.
     * @param dy How far the pointer moved since the last pan event, in view units.
     */
    default void onPan(float dx, float dy) {}

    /**
     * The person turned the mouse wheel.
     *
     * @param factor The zoom factor, above 1 to zoom in.
     * @param x The x of the point under the pointer, in view units.
     * @param y The y of the point under the pointer, in view units.
     */
    default void onZoom(float factor, float x, float y) {}
  }
}
