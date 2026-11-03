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
package org.flixelgdx.backend.gpu;

/**
 * Packs the three things a render pipeline depends on into one {@code long} so it can key a hash map without
 * allocating.
 *
 * <p>A pipeline is the GPU's compiled "recipe" for one kind of draw, and building one is expensive. The renderer
 * therefore builds each distinct recipe once and looks it up by this key: the blend mode, the format of the
 * texture being drawn into, and the shader.
 *
 * <p>Layout, from the lowest bit: 4 bits of blend id, 8 bits of target format id, and 20 bits of shader id.
 */
public final class FlixelPipelineKey {

  /** The most blend ids that fit in a key. */
  public static final int MAX_BLEND = 1 << 4;

  /** The most format ids that fit in a key. */
  public static final int MAX_FORMAT = 1 << 8;

  /** The most shader ids that fit in a key. */
  public static final int MAX_SHADER = 1 << 20;

  private FlixelPipelineKey() {}

  /**
   * Builds a key.
   *
   * @param blend The blend id, from {@link FlixelGpuBlend#id}.
   * @param format The target texture format id.
   * @param shader The shader id.
   * @return The packed key.
   * @throws IllegalArgumentException if a part does not fit in its bits.
   */
  public static long of(int blend, int format, int shader) {
    if (blend < 0 || blend >= MAX_BLEND || format < 0 || format >= MAX_FORMAT || shader < 0 || shader >= MAX_SHADER) {
      throw new IllegalArgumentException("Pipeline key part out of range: " + blend + ", " + format + ", " + shader);
    }
    return blend | ((long) format << 4) | ((long) shader << 12);
  }

  /**
   * Extracts the blend id from a key.
   *
   * @param key The packed key.
   * @return The blend id.
   */
  public static int blendOf(long key) {
    return (int) (key & 0xF);
  }

  /**
   * Extracts the target format id from a key.
   *
   * @param key The packed key.
   * @return The format id.
   */
  public static int formatOf(long key) {
    return (int) ((key >> 4) & 0xFF);
  }

  /**
   * Extracts the shader id from a key.
   *
   * @param key The packed key.
   * @return The shader id.
   */
  public static int shaderOf(long key) {
    return (int) ((key >> 12) & 0xFFFFF);
  }
}
