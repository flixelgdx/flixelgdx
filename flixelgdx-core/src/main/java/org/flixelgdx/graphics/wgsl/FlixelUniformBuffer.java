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
package org.flixelgdx.graphics.wgsl;

import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

/**
 * A CPU-side copy of a shader's uniform struct, written to by name and uploaded to the GPU when it changes.
 *
 * <p>The bytes follow the {@link FlixelWgslLayout} of the shader's WGSL uniform struct exactly, so the renderer can
 * copy {@link #getData()} straight into a GPU buffer. Setting a value is a hash lookup plus a few byte writes and
 * never allocates. Every setter marks the buffer dirty only when a value actually changes, so a renderer that
 * checks {@link #isDirty()} uploads once per change instead of once per draw.
 *
 * <p>Setting a name that the shader does not declare does nothing, which lets one piece of game code drive several
 * shaders that each use a different subset of uniforms. Use {@link #has(String)} to check.
 *
 * <pre>{@code
 * FlixelUniformBuffer uniforms = new FlixelUniformBuffer(FlixelWgslLayout.parse(source));
 * uniforms.setFloat("amount", 0.5f);
 * uniforms.setColor("tint", FlixelColor.RED);
 * }</pre>
 */
public final class FlixelUniformBuffer {

  private final FlixelWgslLayout layout;
  private final byte[] data;

  private boolean dirty = true;

  /**
   * Creates a zero-filled buffer for the given layout.
   *
   * @param layout The layout of the shader's uniform struct.
   */
  public FlixelUniformBuffer(@NotNull FlixelWgslLayout layout) {
    this.layout = layout;
    this.data = new byte[layout.getSize()];
  }

  /**
   * Returns whether the shader declares a uniform with this name.
   *
   * @param name The member name.
   * @return {@code true} when {@link #setFloat(String, float)} and friends would write something.
   */
  public boolean has(@NotNull String name) {
    return layout.find(name) >= 0;
  }

  /**
   * Sets a scalar uniform.
   *
   * <p>An {@code f32} member stores the value as-is. An {@code i32} or {@code u32} member stores the value
   * truncated to an integer. For a vector member, the value goes into the first component.
   *
   * @param name The member name.
   * @param value The value to store.
   */
  public void setFloat(@NotNull String name, float value) {
    int f = layout.find(name);
    if (f >= 0) {
      writeScalar(f, layout.getOffset(f), value);
    }
  }

  /**
   * Sets an integer uniform.
   *
   * <p>An {@code f32} member stores the value converted to a float.
   *
   * @param name The member name.
   * @param value The value to store.
   */
  public void setInt(@NotNull String name, int value) {
    int f = layout.find(name);
    if (f < 0) {
      return;
    }
    int at = layout.getOffset(f);
    if (layout.getScalar(f) == FlixelWgslLayout.F32) {
      putInt(at, Float.floatToRawIntBits((float) value));
    } else {
      putInt(at, value);
    }
  }

  /**
   * Sets the first two components of a vector uniform from a vector.
   *
   * @param name The member name.
   * @param value The vector whose {@code x} and {@code y} are stored.
   */
  public void setVector(@NotNull String name, @NotNull FlixelVector value) {
    set(name, value.x, value.y);
  }

  /**
   * Sets a {@code vec3} or {@code vec4} uniform from a color: red, green, blue, and alpha when there is room.
   *
   * @param name The member name.
   * @param value The color to store.
   */
  public void setColor(@NotNull String name, @NotNull FlixelColor value) {
    set(name, value.r, value.g, value.b, value.a);
  }

  /**
   * Sets a matrix uniform from a 4x4 matrix.
   *
   * <p>A {@code mat4x4} member receives the whole matrix. A smaller matrix receives the matching upper-left part.
   *
   * @param name The member name.
   * @param value The matrix to store.
   */
  public void setMatrix(@NotNull String name, @NotNull FlixelMatrix value) {
    int f = layout.find(name);
    if (f < 0) {
      return;
    }
    int cols = layout.getColumns(f);
    int rows = layout.getRows(f);
    int base = layout.getOffset(f);
    int stride = layout.getColumnStride(f);
    if (stride == 0) {
      return;
    }
    float[] m = value.val;
    for (int c = 0; c < cols; c++) {
      for (int r = 0; r < rows; r++) {
        putFloat(base + c * stride + r * 4, m[c * 4 + r]);
      }
    }
  }

  /**
   * Sets the first two components of a vector uniform.
   *
   * @param name The member name.
   * @param x The first component.
   * @param y The second component.
   */
  public void set(@NotNull String name, float x, float y) {
    int f = layout.find(name);
    if (f >= 0) {
      writeComponents(f, x, y, 0f, 0f, 2);
    }
  }

  /**
   * Sets the first three components of a vector uniform.
   *
   * @param name The member name.
   * @param x The first component.
   * @param y The second component.
   * @param z The third component.
   */
  public void set(@NotNull String name, float x, float y, float z) {
    int f = layout.find(name);
    if (f >= 0) {
      writeComponents(f, x, y, z, 0f, 3);
    }
  }

  /**
   * Sets all four components of a vector uniform. A narrower vector keeps only its first components.
   *
   * @param name The member name.
   * @param x The first component.
   * @param y The second component.
   * @param z The third component.
   * @param w The fourth component.
   */
  public void set(@NotNull String name, float x, float y, float z, float w) {
    int f = layout.find(name);
    if (f >= 0) {
      writeComponents(f, x, y, z, w, 4);
    }
  }

  /** Marks the buffer as uploaded. Renderers call this right after copying {@link #getData()} to the GPU. */
  public void clearDirty() {
    dirty = false;
  }

  /**
   * Returns whether any value changed since the last {@link #clearDirty()}. A new buffer starts dirty so its
   * zeros are uploaded once.
   *
   * @return {@code true} when the bytes need to be uploaded again.
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Returns the bytes in the exact layout of the WGSL struct. The array is the live backing store, so do not
   * modify it directly.
   *
   * @return The uniform bytes.
   */
  public byte @NotNull [] getData() {
    return data;
  }

  /**
   * Returns the size of the struct in bytes.
   *
   * @return The byte count.
   */
  public int getSize() {
    return data.length;
  }

  /**
   * Returns the layout this buffer follows.
   *
   * @return The parsed layout.
   */
  @NotNull
  public FlixelWgslLayout getLayout() {
    return layout;
  }

  private void writeComponents(int field, float x, float y, float z, float w, int provided) {
    int rows = layout.getRows(field);
    int at = layout.getOffset(field);
    int scalar = layout.getScalar(field);
    int n = Math.min(rows, provided);
    for (int i = 0; i < n; i++) {
      float v = i == 0 ? x : i == 1 ? y : i == 2 ? z : w;
      if (scalar == FlixelWgslLayout.F32) {
        putFloat(at + i * 4, v);
      } else {
        putInt(at + i * 4, (int) v);
      }
    }
  }

  private void writeScalar(int field, int at, float value) {
    if (layout.getScalar(field) == FlixelWgslLayout.F32) {
      putFloat(at, value);
    } else {
      putInt(at, (int) value);
    }
  }

  private void putFloat(int at, float value) {
    putInt(at, Float.floatToRawIntBits(value));
  }

  private void putInt(int at, int bits) {
    if (at < 0 || at + 4 > data.length) {
      return;
    }
    byte b0 = (byte) bits;
    byte b1 = (byte) (bits >> 8);
    byte b2 = (byte) (bits >> 16);
    byte b3 = (byte) (bits >> 24);
    if (data[at] != b0 || data[at + 1] != b1 || data[at + 2] != b2 || data[at + 3] != b3) {
      data[at] = b0;
      data[at + 1] = b1;
      data[at + 2] = b2;
      data[at + 3] = b3;
      dirty = true;
    }
  }
}
