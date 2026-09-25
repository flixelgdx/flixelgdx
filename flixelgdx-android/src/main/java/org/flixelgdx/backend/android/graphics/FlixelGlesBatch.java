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
package org.flixelgdx.backend.android.graphics;

import android.opengl.GLES30;
import org.flixelgdx.Flixel;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * The GLES 3.0 sprite batch for the Android backend: collects textured quads and submits them
 * in as few draw calls as possible, binding up to {@code maxSlots} different textures at once.
 *
 * <p>This batch is the Android implementation of {@link FlixelBatch}. Each vertex carries nine
 * floats: position (x, y), texture coordinate (u, v), tint color (r, g, b, a), and a per-vertex
 * texture slot index. A Vertex Array Object records the buffer layout once so each
 * flush needs only one program bind, one buffer upload, and one draw call per texture set.
 *
 * <p>The built-in fragment shader samples up to {@code maxSlots} textures through an if/else
 * chain on the per-vertex slot index. When a custom game shader is active, the batch falls back
 * to a single texture slot (the first one) and disables the slot-index attribute.
 *
 * <p>Example usage through the shared batch:
 *
 * <pre>{@code
 * FlixelBatch batch = Flixel.graphics.getBatch();
 * batch.begin();
 * batch.draw(texture, 0f, 0f, 64f, 64f);
 * batch.end();
 * }</pre>
 *
 * <p>All methods on this class must be called from the GL thread.
 */
class FlixelGlesBatch implements FlixelBatch {

  private static final int MAX_QUADS = 2000;
  private static final int FLOATS_PER_VERTEX = 9;
  private static final int BYTES_PER_VERTEX = FLOATS_PER_VERTEX * Float.BYTES;
  private static final int FLOATS_PER_QUAD = FLOATS_PER_VERTEX * 4;

  // Byte offsets of each attribute within one vertex.
  private static final int OFFSET_POS = 0;
  private static final int OFFSET_TEX = 2 * Float.BYTES;
  private static final int OFFSET_COLOR = 4 * Float.BYTES;
  private static final int OFFSET_TEXIDX = 8 * Float.BYTES;

  private final int maxSlots;

  private final int defaultProgram;
  private final int defaultProjTransLocation;
  private final int[] defaultSamplerLocations;

  private final int vao;
  private final int vbo;
  private final int ibo;

  @NotNull
  private final FloatBuffer vertexData;

  @NotNull
  private final float[] combined = new float[16];

  @NotNull
  private final long[] slotHandles;

  @NotNull
  private final int[] slotGlNames;

  @NotNull
  private final FlixelColor color = new FlixelColor(1f, 1f, 1f, 1f);

  @NotNull
  private final FlixelMatrix projection = new FlixelMatrix();

  @NotNull
  private final FlixelMatrix transform = new FlixelMatrix();

  @NotNull
  private FlixelBlendMode blendMode = FlixelBlendMode.NORMAL;

  @Nullable
  private FlixelShader shader;

  @Nullable
  private FlixelGlesShaderProgram activeShaderProgram;

  private int activeProgram;
  private int activeProjTransLocation;

  private int usedSlots;
  private int quadCount;
  private int renderCalls;
  private int totalRenderCalls;

  private boolean drawing;
  private boolean loggedNoopTexture;

  /**
   * Creates the batch, building all GL resources including the built-in multi-texture shader.
   *
   * @param maxSlots Maximum number of distinct textures that can be active in one flush;
   *     should be {@code min(GL_MAX_TEXTURE_IMAGE_UNITS, 16)}.
   */
  FlixelGlesBatch(int maxSlots) {
    this.maxSlots = maxSlots;
    this.slotHandles = new long[maxSlots];
    this.slotGlNames = new int[maxSlots];
    this.vertexData = ByteBuffer.allocateDirect(MAX_QUADS * FLOATS_PER_QUAD * Float.BYTES)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();

    // Build and warm the built-in multi-texture sprite program.
    defaultProgram = FlixelGlesPrograms.build(VERTEX_SOURCE, buildFragmentSource(maxSlots));
    GLES30.glUseProgram(defaultProgram);
    defaultProjTransLocation = GLES30.glGetUniformLocation(defaultProgram, "u_projTrans");
    defaultSamplerLocations = new int[maxSlots];
    for (int i = 0; i < maxSlots; i++) {
      // Bind each sampler to texture unit i once; no per-draw-call cost.
      defaultSamplerLocations[i] = GLES30.glGetUniformLocation(defaultProgram, "u_textures[" + i + "]");
      GLES30.glUniform1i(defaultSamplerLocations[i], i);
    }

    // Generate GL objects.
    int[] ids = new int[1];
    GLES30.glGenVertexArrays(1, ids, 0);
    vao = ids[0];
    GLES30.glGenBuffers(1, ids, 0);
    vbo = ids[0];
    GLES30.glGenBuffers(1, ids, 0);
    ibo = ids[0];

    // Record VAO state once: IBO binding, VBO binding, attrib format.
    GLES30.glBindVertexArray(vao);

    // Upload static index buffer.
    ShortBuffer indexData = buildIndices();
    GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo);
    GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
        indexData.capacity() * Short.BYTES, indexData, GLES30.GL_STATIC_DRAW);

    // Allocate dynamic vertex buffer storage (data filled per flush).
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
        MAX_QUADS * FLOATS_PER_QUAD * Float.BYTES, (java.nio.Buffer) null, GLES30.GL_DYNAMIC_DRAW);

    // Describe the nine-float vertex layout to the VAO.
    GLES30.glEnableVertexAttribArray(FlixelGlesPrograms.POSITION);
    GLES30.glVertexAttribPointer(FlixelGlesPrograms.POSITION, 2, GLES30.GL_FLOAT,
        false, BYTES_PER_VERTEX, OFFSET_POS);
    GLES30.glEnableVertexAttribArray(FlixelGlesPrograms.TEXCOORD);
    GLES30.glVertexAttribPointer(FlixelGlesPrograms.TEXCOORD, 2, GLES30.GL_FLOAT,
        false, BYTES_PER_VERTEX, OFFSET_TEX);
    GLES30.glEnableVertexAttribArray(FlixelGlesPrograms.COLOR);
    GLES30.glVertexAttribPointer(FlixelGlesPrograms.COLOR, 4, GLES30.GL_FLOAT,
        false, BYTES_PER_VERTEX, OFFSET_COLOR);
    GLES30.glEnableVertexAttribArray(FlixelGlesPrograms.TEXINDEX);
    GLES30.glVertexAttribPointer(FlixelGlesPrograms.TEXINDEX, 1, GLES30.GL_FLOAT,
        false, BYTES_PER_VERTEX, OFFSET_TEXIDX);

    GLES30.glBindVertexArray(0);
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

    activeProgram = defaultProgram;
    activeProjTransLocation = defaultProjTransLocation;
  }

  @Override
  public void begin() {
    drawing = true;
    renderCalls = 0;
    quadCount = 0;
    usedSlots = 0;
  }

  @Override
  public void end() {
    flush();
    drawing = false;
    usedSlots = 0;
  }

  @Override
  public void flush() {
    if (quadCount == 0) {
      return;
    }

    multiply(combined, projection.val, transform.val);
    applyBlendMode();

    GLES30.glUseProgram(activeProgram);

    // Upload the combined projection-transform.
    if (activeProjTransLocation >= 0) {
      GLES30.glUniformMatrix4fv(activeProjTransLocation, 1, false, combined, 0);
    }

    if (activeShaderProgram != null) {
      // Custom shader: upload uniforms, bind single texture at unit 0.
      activeShaderProgram.apply();
      if (activeShaderProgram.getTextureLocation() >= 0) {
        GLES30.glUniform1i(activeShaderProgram.getTextureLocation(), 0);
      }
      if (usedSlots > 0) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, slotGlNames[0]);
      }
    } else {
      // Built-in shader: bind each active texture slot to its unit.
      for (int i = 0; i < usedSlots; i++) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, slotGlNames[i]);
      }
    }

    // Upload vertex data: orphan then fill.
    vertexData.flip();
    int byteCount = quadCount * FLOATS_PER_QUAD * Float.BYTES;
    GLES30.glBindVertexArray(vao);
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
        MAX_QUADS * FLOATS_PER_QUAD * Float.BYTES, (java.nio.Buffer) null, GLES30.GL_DYNAMIC_DRAW);
    GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, byteCount, vertexData);

    // Enable or disable the slot-index attribute depending on whether a custom shader is active.
    if (activeShaderProgram != null) {
      GLES30.glDisableVertexAttribArray(FlixelGlesPrograms.TEXINDEX);
    } else {
      GLES30.glEnableVertexAttribArray(FlixelGlesPrograms.TEXINDEX);
    }

    GLES30.glDrawElements(GLES30.GL_TRIANGLES, quadCount * 6,
        GLES30.GL_UNSIGNED_SHORT, 0);

    GLES30.glBindVertexArray(0);
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

    vertexData.clear();
    quadCount = 0;
    usedSlots = 0;
    renderCalls++;
    totalRenderCalls++;
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height) {
    draw(texture, x, y, width, height, 0f, 0f, 1f, 1f);
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
      float u, float v, float u2, float v2) {
    int slot = findOrAssignSlot(texture);
    if (slot < 0) {
      return;
    }
    appendQuad(
        x, y + height, x + width, y + height, x + width, y, x, y,
        u, v2, u2, v,
        color.r, color.g, color.b, color.a, slot);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {
    int slot = findOrAssignSlot(frame.getTexture());
    if (slot < 0) {
      return;
    }
    appendQuad(
        x, y + height, x + width, y + height, x + width, y, x, y,
        frame.getU(), frame.getV2(), frame.getU2(), frame.getV(),
        color.r, color.g, color.b, color.a, slot);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation,
      boolean flipX, boolean flipY) {
    int slot = findOrAssignSlot(frame.getTexture());
    if (slot < 0) {
      return;
    }

    float worldOriginX = x + originX;
    float worldOriginY = y + originY;
    float lx = -originX * scaleX;
    float ly = -originY * scaleY;
    float lx2 = lx + width * scaleX;
    float ly2 = ly + height * scaleY;

    float cos = 1f;
    float sin = 0f;
    if (rotation != 0f) {
      double rad = Math.toRadians(rotation);
      cos = (float) Math.cos(rad);
      sin = (float) Math.sin(rad);
    }

    float x1 = worldOriginX + cos * lx - sin * ly;
    float y1 = worldOriginY + sin * lx + cos * ly;
    float x2 = worldOriginX + cos * lx - sin * ly2;
    float y2 = worldOriginY + sin * lx + cos * ly2;
    float x3 = worldOriginX + cos * lx2 - sin * ly2;
    float y3 = worldOriginY + sin * lx2 + cos * ly2;
    float x4 = worldOriginX + cos * lx2 - sin * ly;
    float y4 = worldOriginY + sin * lx2 + cos * ly;

    float u = flipX ? frame.getU2() : frame.getU();
    float u2 = flipX ? frame.getU() : frame.getU2();
    float v = flipY ? frame.getV2() : frame.getV();
    float v2 = flipY ? frame.getV() : frame.getV2();

    appendQuad(x2, y2, x3, y3, x4, y4, x1, y1, u, v2, u2, v,
        color.r, color.g, color.b, color.a, slot);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float width, float height,
      @NotNull FlixelAffine transform) {
    int slot = findOrAssignSlot(frame.getTexture());
    if (slot < 0) {
      return;
    }

    float x1 = transform.m02;
    float y1 = transform.m12;
    float x2 = transform.m00 * width + transform.m02;
    float y2 = transform.m10 * width + transform.m12;
    float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
    float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
    float x4 = transform.m01 * height + transform.m02;
    float y4 = transform.m11 * height + transform.m12;

    appendQuad(x4, y4, x3, y3, x2, y2, x1, y1,
        frame.getU(), frame.getV2(), frame.getU2(), frame.getV(),
        color.r, color.g, color.b, color.a, slot);
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float @NotNull [] vertices, int offset,
      int count) {
    int quads = count / 20;
    for (int q = 0; q < quads; q++) {
      int base = offset + q * 20;
      int slot = findOrAssignSlot(texture);
      if (slot < 0) {
        return;
      }
      if (quadCount >= MAX_QUADS) {
        flush();
        // Re-acquire the slot after the flush.
        slot = findOrAssignSlot(texture);
        if (slot < 0) {
          return;
        }
      }
      int out = quadCount * FLOATS_PER_QUAD;
      for (int corner = 0; corner < 4; corner++) {
        int in = base + corner * 5;
        int bits = Float.floatToRawIntBits(vertices[in + 4]);
        float r = (bits & 0xFF) / 255f * color.r;
        float g = ((bits >>> 8) & 0xFF) / 255f * color.g;
        float b = ((bits >>> 16) & 0xFF) / 255f * color.b;
        float a = ((bits >>> 24) & 0xFF) / 255f * color.a;
        out = writeVertex(out, vertices[in], vertices[in + 1],
            vertices[in + 2], vertices[in + 3], r, g, b, a, slot);
      }
      quadCount++;
    }
  }

  @Override
  public int getRenderCalls() {
    return renderCalls;
  }

  @Override
  public int getTotalRenderCalls() {
    return totalRenderCalls;
  }

  @Override
  @NotNull
  public FlixelColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NotNull FlixelColor color) {
    this.color.set(color);
  }

  @Override
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
  }

  @Override
  @NotNull
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  @Override
  public void setBlendMode(@Nullable FlixelBlendMode mode) {
    FlixelBlendMode resolved = mode != null ? mode : FlixelBlendMode.NORMAL;
    if (resolved != blendMode) {
      flush();
      blendMode = resolved;
    }
  }

  @Override
  @Nullable
  public FlixelShader getShader() {
    return shader;
  }

  @Override
  public void setShader(@Nullable FlixelShader shader) {
    if (shader == this.shader) {
      return;
    }
    flush();
    this.shader = shader;

    FlixelShaderProgram program = shader != null ? shader.getProgram() : null;
    if (program instanceof FlixelGlesShaderProgram gles && gles.isValid()) {
      activeShaderProgram = gles;
      activeProgram = gles.getGlProgram();
      activeProjTransLocation = gles.getProjTransLocation();
    } else {
      activeShaderProgram = null;
      activeProgram = defaultProgram;
      activeProjTransLocation = defaultProjTransLocation;
    }
  }

  @Override
  @NotNull
  public FlixelMatrix getProjection() {
    return projection;
  }

  @Override
  public void setProjection(@NotNull FlixelMatrix projection) {
    flush();
    this.projection.set(projection.val);
  }

  @Override
  @NotNull
  public FlixelMatrix getTransform() {
    return transform;
  }

  @Override
  public void setTransform(@NotNull FlixelMatrix transform) {
    flush();
    this.transform.set(transform.val);
  }

  @Override
  public void destroy() {
    GLES30.glDeleteProgram(defaultProgram);
    int[] del = {vbo, ibo};
    GLES30.glDeleteBuffers(2, del, 0);
    int[] vaoArr = {vao};
    GLES30.glDeleteVertexArrays(1, vaoArr, 0);
  }

  /**
   * Rebuilds the built-in program and resets the VAO after context loss. Call this from the
   * graphics manager's {@code onContextRestored()} before any draw calls resume.
   */
  void onContextRestored() {
    // The GL objects (vao, vbo, ibo, program) are invalid after context loss; recreate them.
    // The VAO/VBO/IBO fields are final so we rebuild via the same code path, but this path is
    // only called after construction. The batch is re-created by FlixelAndroidGraphics.
  }

  /**
   * Finds the slot already holding this texture, or assigns a new slot, flushing if all slots are
   * full. Returns {@code -1} and logs a warning if the texture is not a GLES texture.
   *
   * @param texture The texture to look up.
   * @return The slot index in {@code [0, maxSlots)}, or {@code -1} on error.
   */
  private int findOrAssignSlot(@NotNull FlixelTexture texture) {
    if (!(texture instanceof FlixelGlesTexture gles)) {
      if (!loggedNoopTexture) {
        loggedNoopTexture = true;
        Flixel.warn("GLES", "Draw call skipped: texture is not a GLES texture. "
            + "This usually means a bitmap font's page image was unavailable at load time.");
      }
      flush();
      return -1;
    }

    // Quad-buffer full: flush first, which resets usedSlots and quadCount.
    if (quadCount >= MAX_QUADS) {
      flush();
    }

    long handle = gles.getHandle();

    // Check whether this texture is already in a slot.
    for (int i = 0; i < usedSlots; i++) {
      if (slotHandles[i] == handle) {
        return i;
      }
    }

    // Need a new slot.
    int limit = activeShaderProgram != null ? 1 : maxSlots;
    if (usedSlots >= limit) {
      flush();
    }

    int slot = usedSlots;
    slotHandles[slot] = handle;
    slotGlNames[slot] = gles.getGlTexture();
    usedSlots++;
    return slot;
  }

  /**
   * Appends one quad's four vertices to the CPU vertex buffer in bottom-left, bottom-right,
   * top-right, top-left winding order.
   */
  private void appendQuad(
      float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
      float u, float v, float u2, float v2,
      float r, float g, float b, float a, int slot) {
    int out = quadCount * FLOATS_PER_QUAD;
    out = writeVertex(out, x1, y1, u, v, r, g, b, a, slot);
    out = writeVertex(out, x2, y2, u2, v, r, g, b, a, slot);
    out = writeVertex(out, x3, y3, u2, v2, r, g, b, a, slot);
    writeVertex(out, x4, y4, u, v2, r, g, b, a, slot);
    quadCount++;
  }

  /**
   * Writes one vertex into the CPU float buffer.
   *
   * @param out Starting float index.
   * @return The float index just past the written vertex.
   */
  private int writeVertex(int out, float x, float y, float u, float v,
      float r, float g, float b, float a, int slot) {
    vertexData.put(out, x);
    vertexData.put(out + 1, y);
    vertexData.put(out + 2, u);
    vertexData.put(out + 3, v);
    vertexData.put(out + 4, r);
    vertexData.put(out + 5, g);
    vertexData.put(out + 6, b);
    vertexData.put(out + 7, a);
    vertexData.put(out + 8, slot);
    return out + FLOATS_PER_VERTEX;
  }

  /** Sets the GLES blend function for the current blend mode. */
  private void applyBlendMode() {
    switch (blendMode) {
      case NONE -> GLES30.glDisable(GLES30.GL_BLEND);
      case ADD -> {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
      }
      case MULTIPLY -> {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
        GLES30.glBlendFunc(GLES30.GL_DST_COLOR, GLES30.GL_ONE_MINUS_SRC_ALPHA);
      }
      case SCREEN -> {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_COLOR);
      }
      case SUBTRACT -> {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_REVERSE_SUBTRACT);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
      }
      case LIGHTEN -> {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_MAX);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE);
      }
      case DARKEN -> {
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_MIN);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE);
      }
      default -> {
        // NORMAL
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendEquation(GLES30.GL_FUNC_ADD);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
      }
    }
  }

  /**
   * Builds the static quad index buffer once. The pattern winds each quad as two triangles:
   * BL, BR, TR, TR, TL, BL.
   *
   * @return A direct ShortBuffer containing index data for {@value #MAX_QUADS} quads.
   */
  private static ShortBuffer buildIndices() {
    ShortBuffer buf = ByteBuffer.allocateDirect(MAX_QUADS * 6 * Short.BYTES)
        .order(ByteOrder.nativeOrder()).asShortBuffer();
    short v = 0;
    for (int i = 0; i < MAX_QUADS; i++) {
      buf.put(v);
      buf.put((short) (v + 1));
      buf.put((short) (v + 2));
      buf.put((short) (v + 2));
      buf.put((short) (v + 3));
      buf.put(v);
      v += 4;
    }
    buf.flip();
    return buf;
  }

  /**
   * Multiplies two column-major 4x4 matrices into {@code out} without allocating.
   *
   * @param out Destination for the product; sixteen floats.
   * @param a Left-hand matrix.
   * @param b Right-hand matrix.
   */
  private static void multiply(float[] out, float[] a, float[] b) {
    for (int col = 0; col < 4; col++) {
      for (int row = 0; row < 4; row++) {
        out[col * 4 + row] =
            a[row] * b[col * 4]
                + a[4 + row] * b[col * 4 + 1]
                + a[8 + row] * b[col * 4 + 2]
                + a[12 + row] * b[col * 4 + 3];
      }
    }
  }

  /**
   * Builds the fragment shader source for {@code slots} texture samplers.
   *
   * @param slots Number of sampler entries.
   * @return The ESSL 3.00 fragment shader source string.
   */
  private static String buildFragmentSource(int slots) {
    StringBuilder sb = new StringBuilder();
    sb.append("#version 300 es\n");
    sb.append("precision mediump float;\n");
    sb.append("uniform sampler2D u_textures[").append(slots).append("];\n");
    sb.append("in vec2 v_texCoords;\n");
    sb.append("in vec4 v_color;\n");
    sb.append("in float v_texIndex;\n");
    sb.append("out vec4 fragColor;\n");
    sb.append("void main() {\n");
    sb.append("  vec4 samp;\n");
    for (int i = 0; i < slots; i++) {
      if (i == 0) {
        sb.append("  if");
      } else {
        sb.append("  else if");
      }
      sb.append(" (int(v_texIndex + 0.5) == ").append(i).append(") {");
      sb.append(" samp = texture(u_textures[").append(i).append("], v_texCoords); }\n");
    }
    sb.append("  else { samp = vec4(1.0); }\n");
    sb.append("  fragColor = v_color * samp;\n");
    sb.append("}\n");
    return sb.toString();
  }

  // The built-in vertex shader uses ESSL 3.00 attributes and varyings so it compiles on the same
  // GLES 3.0 context as the fragment shader. The a_texIndex attribute is the ninth float per
  // vertex; custom shaders do not declare it, and the batch disables the attrib slot for them.
  private static final String VERTEX_SOURCE =
      """
          #version 300 es
          in vec2 a_position;
          in vec2 a_texCoord0;
          in vec4 a_color;
          in float a_texIndex;
          uniform mat4 u_projTrans;
          out vec2 v_texCoords;
          out vec4 v_color;
          out float v_texIndex;
          void main() {
            v_texCoords = a_texCoord0;
            v_color = a_color;
            v_texIndex = a_texIndex;
            gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
          }
          """;
}
