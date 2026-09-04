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
package org.flixelgdx.graphics;

import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.NotNull;

/**
 * Describes how the floats (and bytes) of one vertex are laid out in memory.
 *
 * <p>Every GPU needs to know what each vertex contains before it can draw: where the position is,
 * where the color is, where the texture coordinates are, and so on. Each backend expresses this in
 * its own way (bgfx has a {@code VertexLayout}, WebGPU bakes it into a render pipeline, WebGL uses
 * {@code vertexAttribPointer}), so this class is the one backend-neutral description they all read.
 *
 * <p>Build a layout once and reuse it for every {@link FlixelMesh} that shares the same vertex
 * shape. The sprite batch has its own fixed internal layout; you only need this for custom geometry.
 *
 * <p>Example (a 2D position, a packed color, and one texture coordinate pair):
 *
 * <pre>{@code
 * FlixelVertexLayout layout = FlixelVertexLayout.builder()
 *     .add(FlixelVertexLayout.Usage.POSITION, 2, FlixelVertexLayout.ComponentType.FLOAT, false)
 *     .add(FlixelVertexLayout.Usage.COLOR, 4, FlixelVertexLayout.ComponentType.UNSIGNED_BYTE, true)
 *     .add(FlixelVertexLayout.Usage.TEXCOORD0, 2, FlixelVertexLayout.ComponentType.FLOAT, false)
 *     .build();
 * }</pre>
 *
 * @see FlixelMesh
 */
public final class FlixelVertexLayout {

  private final FlixelArray<Element> elements;
  private final int stride;

  private FlixelVertexLayout(FlixelArray<Element> elements) {
    int computed = 0;
    Element[] items = elements.getItems();
    for (int i = 0; i < elements.getSize(); i++) {
      Element e = items[i];
      computed += e.components() * e.type().bytes();
    }
    this.elements = elements;
    this.stride = computed;
  }

  /**
   * Starts building a layout.
   *
   * @return A fresh builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the number of bytes one whole vertex occupies (the sum of every element's size).
   */
  public int getStride() {
    return stride;
  }

  /**
   * Returns the ordered elements that make up one vertex.
   *
   * <p>Treat the returned array as read-only; do not modify it. Iterate it with an indexed loop
   * over {@link FlixelArray#getItems()} to stay allocation-free.
   *
   * @return The layout's elements, in order; never {@code null}.
   */
  @NotNull
  public FlixelArray<Element> getElements() {
    return elements;
  }

  /**
   * Collects {@link Element}s in the order they appear in a vertex, then builds a {@link FlixelVertexLayout}.
   */
  public static final class Builder {

    private final FlixelArray<Element> elements = new FlixelArray<>(Element[]::new);

    private Builder() {}

    /**
     * Appends one attribute to the vertex.
     *
     * @param usage What this attribute means to the shader (position, color, and so on).
     * @param components How many values it has (for example, {@code 2} for a 2D position).
     * @param type The number type of each value.
     * @param normalized {@code true} to map integer values into the {@code [0, 1]} (or {@code [-1, 1]}) range
     *     in the shader; typically {@code true} for packed colors and {@code false} for positions.
     * @return This builder, for chaining.
     */
    public Builder add(@NotNull Usage usage, int components, @NotNull ComponentType type, boolean normalized) {
      elements.add(new Element(usage, components, type, normalized));
      return this;
    }

    /**
     * Returns a new immutable layout describing the added elements in order.
     */
    public FlixelVertexLayout build() {
      FlixelArray<Element> copy = new FlixelArray<>(Element[]::new, elements.getSize());
      copy.addAll(elements);
      return new FlixelVertexLayout(copy);
    }
  }

  /**
   * One attribute inside a vertex.
   *
   * @param usage What this attribute means to the shader.
   * @param components How many values the attribute holds.
   * @param type The number type of each value.
   * @param normalized Whether integer values are normalized into a floating-point range in the shader.
   */
  public record Element(@NotNull Usage usage, int components, @NotNull ComponentType type, boolean normalized) {
  }

  /**
   * The role an attribute plays, so backends can bind it to the matching shader input.
   */
  public enum Usage {

    /** Vertex position. */
    POSITION,

    /** Vertex color (often a single packed value). */
    COLOR,

    /** First texture coordinate set. */
    TEXCOORD0,

    /** Second texture coordinate set. */
    TEXCOORD1,

    /** Surface normal. */
    NORMAL,

    /** Any other custom per-vertex value (for example, the sprite batch's texture-slot index). */
    GENERIC
  }

  /**
   * The number format of a single component, with its size in bytes.
   */
  public enum ComponentType {

    /** 32-bit floating point. */
    FLOAT(4),

    /** 8-bit unsigned integer, commonly used four-at-a-time for a packed RGBA color. */
    UNSIGNED_BYTE(1),

    /** 16-bit signed integer. */
    SHORT(2),

    /** 16-bit unsigned integer. */
    UNSIGNED_SHORT(2);

    private final int bytes;

    ComponentType(int bytes) {
      this.bytes = bytes;
    }

    /**
     * Returns the size of one component of this type, in bytes.
     */
    public int bytes() {
      return bytes;
    }
  }
}
