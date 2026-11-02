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

import org.jetbrains.annotations.NotNull;

/**
 * The byte layout of a shader's uniform struct: where each named member lives and what shape it has.
 *
 * <p>A shader keeps its settings in one struct on the GPU. Before Java can set those settings by name, something
 * has to work out the byte offset of every member, following the shader language's alignment rules. The renderer
 * does that once, when the shader is created, and hands the result over as a layout. Think of it as a floor plan:
 * the layout says which room each name belongs to, and {@link FlixelUniformBuffer} drops values into those rooms.
 *
 * <p>Members are addressed by an index from {@link #find(String)}. Names are flattened, so a member of a nested
 * struct is {@code outer.inner} and an array element is {@code name[2]}.
 *
 * <pre>{@code
 * int f = layout.find("tint");
 * if (f >= 0) {
 *   int offset = layout.getOffset(f); // Where the tint starts in the buffer.
 *   int width = layout.getRows(f);    // 4 for a vec4.
 * }
 * }</pre>
 */
public interface FlixelUniformLayout {

  /** The 32-bit float scalar kind. */
  int F32 = 0;

  /** The 32-bit signed integer scalar kind. */
  int I32 = 1;

  /** The 32-bit unsigned integer scalar kind. */
  int U32 = 2;

  /**
   * Returns the index of a uniform by name.
   *
   * @param name The member name, such as {@code tint}, {@code light.color} or {@code weights[2]}.
   * @return The member index, or {@code -1} when the shader has no such uniform.
   */
  int find(@NotNull String name);

  /**
   * Returns the number of settable members, counting every array element separately.
   *
   * @return The member count.
   */
  int getFieldCount();

  /**
   * Returns the name of a member.
   *
   * @param field The member index.
   * @return The flattened name.
   */
  @NotNull
  String getName(int field);

  /**
   * Returns the byte offset of a member in the uniform buffer.
   *
   * @param field The member index.
   * @return The offset in bytes.
   */
  int getOffset(int field);

  /**
   * Returns the scalar kind of a member: {@link #F32}, {@link #I32} or {@link #U32}.
   *
   * @param field The member index.
   * @return The scalar kind.
   */
  int getScalar(int field);

  /**
   * Returns the number of columns: {@code 1} for scalars and vectors, {@code 2} to {@code 4} for matrices.
   *
   * @param field The member index.
   * @return The column count.
   */
  int getColumns(int field);

  /**
   * Returns the number of rows, which is the vector width: {@code 1} for scalars, {@code 2} to {@code 4} for
   * vectors, and the matrix height for matrices.
   *
   * @param field The member index.
   * @return The row count.
   */
  int getRows(int field);

  /**
   * Returns the distance in bytes between two columns of a matrix member.
   *
   * @param field The member index.
   * @return The column stride, or {@code 0} for scalars and vectors.
   */
  int getColumnStride(int field);

  /**
   * Returns the size of the whole uniform buffer in bytes, rounded up to the struct's alignment.
   *
   * @return The byte size, or {@code 0} when there are no uniforms.
   */
  int getSize();

  /**
   * Returns whether the shader has no uniforms at all.
   *
   * @return {@code true} when {@link #getSize()} is zero.
   */
  default boolean isEmpty() {
    return getSize() == 0;
  }
}
