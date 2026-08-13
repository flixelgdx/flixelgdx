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
package org.flixelgdx.backend.desktop.graphics;

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The bgfx sprite batch: accumulates textured quads into a CPU buffer and submits them to bgfx as
 * transient geometry, grouping by texture, blend mode, and shader.
 *
 * <p>Each vertex is position (2 floats), texture coordinate (2 floats), and packed ABGR color
 * (4 bytes), matching the layout the graphics manager registers. A flush allocates transient
 * vertex and index buffers, uploads the accumulated quads, binds the current texture and program,
 * and submits one draw to the active bgfx view.
 *
 * <p>A quad's four corners are transformed on the CPU (translate/scale/rotate/flip), so the vertex
 * shader only applies the view-projection matrix. This keeps the shader trivial and identical
 * across every sprite.
 */
public class FlixelBgfxBatch implements FlixelBatch {

  /** Maximum quads buffered before an automatic flush. */
  private static final int MAX_QUADS = 8192;
  private static final int FLOATS_PER_VERTEX = 5; // x, y, u, v, packed-color-as-float
  private static final int VERTICES_PER_QUAD = 4;
  private static final int INDICES_PER_QUAD = 6;
  private static final int VERTEX_STRIDE_BYTES = 20; // 2*4 + 2*4 + 4

  @NotNull
  private final FlixelBgfxGraphics graphics;

  @NotNull
  private final float[] vertices = new float[MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];

  @NotNull
  private final FlixelColor color = new FlixelColor();

  @NotNull
  private final FlixelMatrix projection = new FlixelMatrix();

  @NotNull
  private final FlixelMatrix transform = new FlixelMatrix();

  @Nullable
  private FlixelBgfxTexture currentTexture;

  @Nullable
  private FlixelShader shader;

  @NotNull
  private FlixelBlendMode blendMode = FlixelBlendMode.NORMAL;

  private int quadCount;
  private int renderCalls;
  private int totalRenderCalls;

  private boolean drawing;
  // True on non-OpenGL backends (Vulkan, Metal, D3D) where the color attribute uses BGRA memory order.
  private boolean bgra;

  FlixelBgfxBatch(@NotNull FlixelBgfxGraphics graphics) {
    this.graphics = graphics;
  }

  /** Switches between RGBA (OpenGL) and BGRA (Vulkan, Metal, D3D) vertex color packing. */
  void setBgra(boolean bgra) {
    this.bgra = bgra;
  }

  @Override
  public void begin() {
    drawing = true;
    renderCalls = 0;
    quadCount = 0;
    currentTexture = null;
  }

  @Override
  public void end() {
    flush();
    drawing = false;
  }

  @Override
  public void flush() {
    if (quadCount == 0) {
      return;
    }
    graphics.submitQuads(vertices, quadCount, currentTexture, blendMode, shader, projection, transform);
    renderCalls++;
    totalRenderCalls++;
    quadCount = 0;
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height) {
    draw(texture, x, y, width, height, 0f, 0f, 1f, 1f);
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
      float u, float v, float u2, float v2) {
    switchTexture(texture);
    pushQuad(x, y, x + width, y + height, u, v, u2, v2);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {
    switchTexture(frame.getTexture());
    pushQuad(x, y, x + width, y + height, frame.getU(), frame.getV(), frame.getU2(), frame.getV2());
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation,
      boolean flipX, boolean flipY) {
    switchTexture(frame.getTexture());
    float u = flipX ? frame.getU2() : frame.getU();
    float u2 = flipX ? frame.getU() : frame.getU2();
    float v = flipY ? frame.getV2() : frame.getV();
    float v2 = flipY ? frame.getV() : frame.getV2();

    // Corners relative to the origin, then scaled and rotated around it.
    float cos = (float) Math.cos(Math.toRadians(rotation));
    float sin = (float) Math.sin(Math.toRadians(rotation));
    float px = x + originX;
    float py = y + originY;
    // Local corner offsets from the origin (bottom-left based, y-up).
    float lx0 = -originX * scaleX;
    float ly0 = -originY * scaleY;
    float lx1 = (width - originX) * scaleX;
    float ly1 = (height - originY) * scaleY;

    float x0 = px + lx0 * cos - ly0 * sin;
    float y0 = py + lx0 * sin + ly0 * cos;
    float x1 = px + lx1 * cos - ly0 * sin;
    float y1 = py + lx1 * sin + ly0 * cos;
    float x2 = px + lx1 * cos - ly1 * sin;
    float y2 = py + lx1 * sin + ly1 * cos;
    float x3 = px + lx0 * cos - ly1 * sin;
    float y3 = py + lx0 * sin + ly1 * cos;

    pushQuadCorners(x0, y0, x1, y1, x2, y2, x3, y3, u, v, u2, v2);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine t) {
    switchTexture(frame.getTexture());
    // Transform the four unit-quad corners (0,0)-(width,height) through the affine.
    float x0 = t.m02;
    float y0 = t.m12;
    float x1 = t.m00 * width + t.m02;
    float y1 = t.m10 * width + t.m12;
    float x2 = t.m00 * width + t.m01 * height + t.m02;
    float y2 = t.m10 * width + t.m11 * height + t.m12;
    float x3 = t.m01 * height + t.m02;
    float y3 = t.m11 * height + t.m12;
    pushQuadCorners(x0, y0, x1, y1, x2, y2, x3, y3, frame.getU(), frame.getV(), frame.getU2(), frame.getV2());
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float @NotNull [] vertices, int offset, int count) {
    int floatsPerQuad = VERTICES_PER_QUAD * FLOATS_PER_VERTEX;
    int end = offset + count;
    // The caller's layout already matches this batch's internal one, so whole quads copy directly.
    for (int i = offset; i + floatsPerQuad <= end; i += floatsPerQuad) {
      switchTexture(texture);
      int base = quadCount * floatsPerQuad;
      System.arraycopy(vertices, i, this.vertices, base, floatsPerQuad);
      quadCount++;
    }
  }

  private void switchTexture(@NotNull FlixelTexture texture) {
    FlixelBgfxTexture bgfxTexture = (texture instanceof FlixelBgfxTexture t) ? t : null;
    if (bgfxTexture != currentTexture) {
      flush();
      currentTexture = bgfxTexture;
    }
    if (quadCount >= MAX_QUADS) {
      flush();
    }
  }

  private void pushQuad(float x0, float y0, float x1, float y1, float u, float v, float u2, float v2) {
    pushQuadCorners(x0, y0, x1, y0, x1, y1, x0, y1, u, v, u2, v2);
  }

  private void pushQuadCorners(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
      float u, float v, float u2, float v2) {
    float packed = Float.intBitsToFloat(packAbgr());
    int base = quadCount * VERTICES_PER_QUAD * FLOATS_PER_VERTEX;
    // Bottom-left, bottom-right, top-right, top-left. UVs: v at bottom, v2 at top.
    set(base, x0, y0, u, v2, packed);
    set(base + FLOATS_PER_VERTEX, x1, y1, u2, v2, packed);
    set(base + 2 * FLOATS_PER_VERTEX, x2, y2, u2, v, packed);
    set(base + 3 * FLOATS_PER_VERTEX, x3, y3, u, v, packed);
    quadCount++;
  }

  private void set(int i, float x, float y, float u, float v, float packed) {
    vertices[i] = x;
    vertices[i + 1] = y;
    vertices[i + 2] = u;
    vertices[i + 3] = v;
    vertices[i + 4] = packed;
  }

  /**
   * Packs the current tint into a 32-bit integer whose bytes match the memory layout the active
   * backend expects for the vertex color attribute.
   *
   * <p>On OpenGL the layout is RGBA in memory, expressed as the ABGR integer
   * {@code (a<<24)|(b<<16)|(g<<8)|r}. On Vulkan, Metal, and Direct3D, bgfx maps the color
   * attribute to BGRA memory order, so the bytes must be reversed to BGRA, expressed as the ARGB
   * integer {@code (a<<24)|(r<<16)|(g<<8)|b}.
   */
  private int packAbgr() {
    int r = clamp255(color.r);
    int g = clamp255(color.g);
    int b = clamp255(color.b);
    int a = clamp255(color.a);
    if (bgra) {
      return (a << 24) | (r << 16) | (g << 8) | b;
    }
    return (a << 24) | (b << 16) | (g << 8) | r;
  }

  private static int clamp255(float v) {
    int i = (int) (v * 255f + 0.5f);
    return i < 0 ? 0 : Math.min(i, 255);
  }

  @Override
  public int getRenderCalls() {
    return renderCalls;
  }

  @Override
  public int getTotalRenderCalls() {
    return totalRenderCalls;
  }

  @NotNull
  @Override
  public FlixelColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NotNull FlixelColor color) {
    this.color.setColor(color);
  }

  @Override
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
  }

  @NotNull
  @Override
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  @Override
  public void setBlendMode(@Nullable FlixelBlendMode mode) {
    FlixelBlendMode next = mode != null ? mode : FlixelBlendMode.NORMAL;
    if (next != blendMode) {
      flush();
      blendMode = next;
    }
  }

  @Nullable
  @Override
  public FlixelShader getShader() {
    return shader;
  }

  @Override
  public void setShader(@Nullable FlixelShader shader) {
    if (shader != this.shader) {
      flush();
      this.shader = shader;
    }
  }

  @NotNull
  @Override
  public FlixelMatrix getProjection() {
    return projection;
  }

  @Override
  public void setProjection(@NotNull FlixelMatrix projection) {
    if (drawing) {
      flush();
    }
    this.projection.set(projection);
  }

  @NotNull
  @Override
  public FlixelMatrix getTransform() {
    return transform;
  }

  @Override
  public void setTransform(@NotNull FlixelMatrix transform) {
    flush();
    this.transform.set(transform);
  }

  @Override
  public void destroy() {
    quadCount = 0;
  }

  /** The number of floats each quad occupies, exposed so the manager can size transient buffers. */
  static int floatsPerQuad() {
    return VERTICES_PER_QUAD * FLOATS_PER_VERTEX;
  }

  static int verticesPerQuad() {
    return VERTICES_PER_QUAD;
  }

  /** The maximum quads a single flush can hold, used to size the shared static index buffer. */
  static int maxQuads() {
    return MAX_QUADS;
  }

  static int indicesPerQuad() {
    return INDICES_PER_QUAD;
  }

  static int vertexStrideBytes() {
    return VERTEX_STRIDE_BYTES;
  }
}
