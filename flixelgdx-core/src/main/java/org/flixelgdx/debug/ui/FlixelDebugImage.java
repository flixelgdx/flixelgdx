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

/**
 * A picture, such as a texture preview or a screenshot, given as the bytes of a PNG file.
 *
 * <p>The app shows the picture at {@linkplain #setScale(float) a zoom} of your choice, or fits it into the widget
 * with a scale of {@code 0}. Pixel art stays sharp unless {@link #setSmooth(boolean) smoothing} is on.
 *
 * <p>A picture is large, so the image only sends it again when {@link #setPng(byte[], int, int)} is called with new
 * bytes; changing the scale is a tiny patch.
 *
 * <pre>{@code
 * FlixelDebugImage preview = new FlixelDebugImage().setScale(2f);
 * preview.setPng(pngBytes, 0, pngBytes.length);
 * }</pre>
 */
public class FlixelDebugImage extends FlixelDebugWidget<FlixelDebugImage> {

  private byte[] png = new byte[0];
  private float scale = 1f;
  private int length;

  private boolean smooth;

  /** Creates an image with no picture. */
  public FlixelDebugImage() {
    super(FlixelDebugProtocol.WIDGET_IMAGE);
  }

  /**
   * Shows a new picture. The bytes are copied, so the caller may reuse its array.
   *
   * @param data The array that holds the PNG file.
   * @param offset The position of the first byte.
   * @param count How many bytes the PNG has.
   * @return This image, for chaining.
   */
  @NotNull
  public FlixelDebugImage setPng(@NotNull byte[] data, int offset, int count) {
    if (png.length < count) {
      png = new byte[count];
    }
    System.arraycopy(data, offset, png, 0, count);
    length = count;
    markDirty(FlixelDebugProtocol.PROP_IMAGE);
    return this;
  }

  /**
   * Removes the picture.
   *
   * @return This image, for chaining.
   */
  @NotNull
  public FlixelDebugImage clearPng() {
    if (length != 0) {
      length = 0;
      markDirty(FlixelDebugProtocol.PROP_IMAGE);
    }
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (length > 0) {
      mask |= bit(FlixelDebugProtocol.PROP_IMAGE);
    }
    if (scale != 1f) {
      mask |= bit(FlixelDebugProtocol.PROP_SCALE);
    }
    if (smooth) {
      mask |= bit(FlixelDebugProtocol.PROP_SMOOTH);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_IMAGE -> {
        if (length > 0) {
          writer.writeBytesValue(png, 0, length);
        } else {
          writer.writeNullValue();
        }
      }
      case FlixelDebugProtocol.PROP_SCALE -> writer.writeValue(scale);
      case FlixelDebugProtocol.PROP_SMOOTH -> writer.writeValue(smooth);
      default -> super.writeProp(writer, key);
    }
  }

  /**
   * Returns whether the image shows a picture.
   *
   * @return {@code true} when a PNG is set.
   */
  public boolean hasPng() {
    return length > 0;
  }

  public float getScale() {
    return scale;
  }

  /**
   * Sets how big the picture is drawn.
   *
   * @param scale The zoom, for example {@code 2} for twice the size, or {@code 0} to fit the picture into the widget.
   * @return This image, for chaining.
   */
  @NotNull
  public FlixelDebugImage setScale(float scale) {
    if (this.scale != scale) {
      this.scale = scale;
      markDirty(FlixelDebugProtocol.PROP_SCALE);
    }
    return this;
  }

  public boolean isSmooth() {
    return smooth;
  }

  /**
   * Sets whether the picture is smoothed when it is scaled.
   *
   * @param smooth {@code true} to smooth it; {@code false} keeps pixel art sharp.
   * @return This image, for chaining.
   */
  @NotNull
  public FlixelDebugImage setSmooth(boolean smooth) {
    if (this.smooth != smooth) {
      this.smooth = smooth;
      markDirty(FlixelDebugProtocol.PROP_SMOOTH);
    }
    return this;
  }

  /**
   * Returns the size of the PNG in bytes.
   *
   * @return The size, or {@code 0} without a picture.
   */
  public int getPngLength() {
    return length;
  }
}
