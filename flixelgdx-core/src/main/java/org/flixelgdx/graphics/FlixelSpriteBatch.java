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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.IntBuffer;

/**
 * A multi-texture sprite batch that reduces GPU draw calls by binding up to {@link #getMaxTextureSlots()}
 * distinct textures per flush instead of one.
 *
 * <p>The standard libGDX {@code SpriteBatch} flushes its vertex buffer every time the active
 * texture changes. {@code FlixelSpriteBatch} instead assigns each texture to an OpenGL texture
 * unit (slot 0 through N-1) and records that slot index as a per-vertex attribute. The generated
 * fragment shader reads from the correct sampler based on that index, so a single draw call can
 * render quads from up to N different textures at once.
 *
 * <p>A flush is only triggered when:
 * <ul>
 *   <li>All texture slots are occupied and a new, unseen texture is encountered.</li>
 *   <li>The internal vertex buffer is full.</li>
 *   <li>{@link #end()} is called.</li>
 *   <li>Blend state or matrices are changed mid-frame.</li>
 * </ul>
 *
 * <p>The slot count is determined at construction from {@code GL_MAX_TEXTURE_IMAGE_UNITS},
 * capped at 16 so the fragment shader's if-else chain stays reasonable on all drivers.
 *
 * <p>Usage example:
 * <pre>{@code
 * FlixelBatch batch = new FlixelSpriteBatch();
 * batch.setProjectionMatrix(camera.combined);
 * batch.begin();
 * batch.draw(texture, x, y, width, height);
 * batch.end();
 * }</pre>
 *
 * <p><b>Custom shaders:</b> a shader passed to {@link #setShader(ShaderProgram)} must declare the
 * same vertex attributes as the built-in shader ({@code a_position}, {@code a_color},
 * {@code a_texCoord0}, {@code a_texIndex}) and the same sampler uniforms
 * ({@code u_texture0} through {@code u_textureN-1}) to work correctly with multi-texture draws.
 */
public class FlixelSpriteBatch implements FlixelBatch {

  private static final int FLOATS_PER_VERTEX = 6;
  private static final int VERTICES_PER_QUAD = 4;
  private static final int INDICES_PER_QUAD = 6;
  private static final int DEFAULT_MAX_QUADS = 1000;
  private static final int MAX_SLOTS_DESKTOP = 16;

  private final int maxTextureSlots;
  private int renderCalls;
  private int totalRenderCalls;
  private int vertexIdx;
  private int usedSlots;
  private int blendSrcFunc = GL20.GL_SRC_ALPHA;
  private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
  private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
  private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

  private float colorPacked;

  private final Mesh mesh;
  private final float[] vertices;
  private final Texture[] textures;
  private final ShaderProgram builtinShader;
  private ShaderProgram customShader;
  private final Matrix4 projectionMatrix = new Matrix4();
  private final Matrix4 transformMatrix = new Matrix4();
  private final Matrix4 combinedMatrix = new Matrix4();
  private final Color color = new Color(Color.WHITE);

  private boolean drawing;
  private boolean blendingEnabled = true;

  /** Creates a batch with {@value #DEFAULT_MAX_QUADS} max quads and auto-detected texture slot count. */
  public FlixelSpriteBatch() {
    this(DEFAULT_MAX_QUADS);
  }

  /**
   * Creates a batch with the given sprite capacity.
   *
   * @param maxQuads Maximum quads (sprites) that can be batched before an automatic flush.
   */
  public FlixelSpriteBatch(int maxQuads) {
    colorPacked = color.toFloatBits();

    IntBuffer glBuf = BufferUtils.newIntBuffer(1);
    Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, glBuf);
    maxTextureSlots = Math.max(1, Math.min(glBuf.get(0), MAX_SLOTS_DESKTOP));
    textures = new Texture[maxTextureSlots];

    vertices = new float[maxQuads * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];

    short[] indices = new short[maxQuads * INDICES_PER_QUAD];
    for (int i = 0, j = 0, v = 0; i < maxQuads; i++, j += 6, v += 4) {
      indices[j] = (short) v;
      indices[j + 1] = (short) (v + 1);
      indices[j + 2] = (short) (v + 2);
      indices[j + 3] = (short) (v + 2);
      indices[j + 4] = (short) (v + 3);
      indices[j + 5] = (short) v;
    }

    mesh = new Mesh(true, maxQuads * VERTICES_PER_QUAD, maxQuads * INDICES_PER_QUAD,
        new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
        new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
        new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
        new VertexAttribute(Usage.Generic, 1, "a_texIndex"));
    mesh.setIndices(indices);

    builtinShader = buildShader(maxTextureSlots);
    builtinShader.bind();
    for (int i = 0; i < maxTextureSlots; i++) {
      builtinShader.setUniformi("u_texture" + i, i);
    }
  }

  @Override
  public void begin() {
    if (drawing) {
      throw new IllegalStateException("end() must be called before begin()");
    }
    drawing = true;
    renderCalls = 0;
    ShaderProgram s = activeShader();
    s.bind();
    if (s == builtinShader) {
      for (int i = 0; i < maxTextureSlots; i++) {
        builtinShader.setUniformi("u_texture" + i, i);
      }
    }
    setupMatrices();
    if (blendingEnabled) {
      Gdx.gl.glEnable(GL20.GL_BLEND);
      Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
    }
  }

  @Override
  public void end() {
    if (!drawing) {
      throw new IllegalStateException("begin() must be called before end()");
    }
    flush();
    drawing = false;
    if (blendingEnabled) {
      Gdx.gl.glDisable(GL20.GL_BLEND);
    }
  }

  @Override
  public void flush() {
    if (vertexIdx == 0) {
      return;
    }
    renderCalls++;
    totalRenderCalls++;
    for (int i = 0; i < usedSlots; i++) {
      textures[i].bind(i);
    }
    int quadCount = vertexIdx / (FLOATS_PER_VERTEX * VERTICES_PER_QUAD);
    mesh.setVertices(vertices, 0, vertexIdx);
    mesh.render(activeShader(), GL20.GL_TRIANGLES, 0, quadCount * INDICES_PER_QUAD);
    vertexIdx = 0;
    usedSlots = 0;
  }

  @Override
  public void setColor(Color tint) {
    color.set(tint);
    colorPacked = color.toFloatBits();
  }

  @Override
  public void setColor(float r, float g, float b, float a) {
    color.set(r, g, b, a);
    colorPacked = color.toFloatBits();
  }

  @Override
  public void setPackedColor(float packed) {
    colorPacked = packed;
  }

  @Override
  public Color getColor() {
    return color;
  }

  @Override
  public float getPackedColor() {
    return colorPacked;
  }

  @Override
  public void draw(Texture texture, float x, float y, float originX, float originY, float w, float h,
      float scaleX, float scaleY, float rotation,
      int srcX, int srcY, int srcW, int srcH, boolean flipX, boolean flipY) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(texture);

    float invW = 1f / texture.getWidth();
    float invH = 1f / texture.getHeight();
    float u = srcX * invW;
    float u2 = (srcX + srcW) * invW;
    float v = (srcY + srcH) * invH;
    float v2 = srcY * invH;
    if (flipX) {
      float t = u;
      u = u2;
      u2 = t;
    }
    if (flipY) {
      float t = v;
      v = v2;
      v2 = t;
    }

    writeRotatedQuad(x, y, originX, originY, w, h, scaleX, scaleY, rotation, tidx, u, v, u2, v2);
  }

  @Override
  public void draw(Texture texture, float x, float y, float w, float h,
      int srcX, int srcY, int srcW, int srcH, boolean flipX, boolean flipY) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(texture);

    float invW = 1f / texture.getWidth();
    float invH = 1f / texture.getHeight();
    float u = srcX * invW;
    float u2 = (srcX + srcW) * invW;
    float v = (srcY + srcH) * invH;
    float v2 = srcY * invH;
    if (flipX) {
      float t = u;
      u = u2;
      u2 = t;
    }
    if (flipY) {
      float t = v;
      v = v2;
      v2 = t;
    }

    float x2 = x + w;
    float y2 = y + h;
    putVertex(x, y, u, v, tidx);
    putVertex(x, y2, u, v2, tidx);
    putVertex(x2, y2, u2, v2, tidx);
    putVertex(x2, y, u2, v, tidx);
  }

  @Override
  public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcW, int srcH) {
    draw(texture, x, y, srcW, srcH, srcX, srcY, srcW, srcH, false, false);
  }

  @Override
  public void draw(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(texture);
    float x2 = x + w;
    float y2 = y + h;
    putVertex(x, y, u, v, tidx);
    putVertex(x, y2, u, v2, tidx);
    putVertex(x2, y2, u2, v2, tidx);
    putVertex(x2, y, u2, v, tidx);
  }

  @Override
  public void draw(Texture texture, float x, float y, float w, float h) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(texture);
    float x2 = x + w;
    float y2 = y + h;
    putVertex(x, y, 0f, 1f, tidx);
    putVertex(x, y2, 0f, 0f, tidx);
    putVertex(x2, y2, 1f, 0f, tidx);
    putVertex(x2, y, 1f, 1f, tidx);
  }

  @Override
  public void draw(Texture texture, float x, float y) {
    draw(texture, x, y, texture.getWidth(), texture.getHeight());
  }

  @Override
  public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
    // spriteVertices uses SpriteBatch's 5-float-per-vertex format: x, y, color, u, v.
    // Expand each vertex to 6 floats by appending the texture slot index.
    int end = offset + count;
    int si = offset;
    while (si < end) {
      if (vertexIdx + FLOATS_PER_VERTEX * VERTICES_PER_QUAD > vertices.length) {
        flush();
      }
      float tidx = (float) getOrAssignSlot(texture);
      int remaining = (end - si) / 5;
      int capacity = (vertices.length - vertexIdx) / FLOATS_PER_VERTEX;
      int toProcess = Math.min(remaining, capacity);
      for (int i = 0; i < toProcess; i++, si += 5) {
        vertices[vertexIdx++] = spriteVertices[si];
        vertices[vertexIdx++] = spriteVertices[si + 1];
        vertices[vertexIdx++] = spriteVertices[si + 2];
        vertices[vertexIdx++] = spriteVertices[si + 3];
        vertices[vertexIdx++] = spriteVertices[si + 4];
        vertices[vertexIdx++] = tidx;
      }
    }
  }

  @Override
  public void draw(TextureRegion region, float x, float y) {
    draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
  }

  @Override
  public void draw(TextureRegion region, float x, float y, float w, float h) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(region.getTexture());

    float u = region.getU();
    float u2 = region.getU2();
    float v = region.getV2();
    float v2 = region.getV();

    float x2 = x + w;
    float y2 = y + h;
    putVertex(x, y, u, v, tidx);
    putVertex(x, y2, u, v2, tidx);
    putVertex(x2, y2, u2, v2, tidx);
    putVertex(x2, y, u2, v, tidx);
  }

  @Override
  public void draw(TextureRegion region, float x, float y, float originX, float originY, float w, float h,
      float scaleX, float scaleY, float rotation) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(region.getTexture());

    float u = region.getU();
    float u2 = region.getU2();
    float v = region.getV2();
    float v2 = region.getV();

    writeRotatedQuad(x, y, originX, originY, w, h, scaleX, scaleY, rotation, tidx, u, v, u2, v2);
  }

  @Override
  public void draw(TextureRegion region, float x, float y, float originX, float originY, float w, float h,
      float scaleX, float scaleY, float rotation, boolean clockwise) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(region.getTexture());

    // Clockwise atlas-packed sprite: UV corners are reassigned to undo the 90-degree CW rotation.
    float u, u2, v, v2;
    if (clockwise) {
      u = region.getU2();
      u2 = region.getU();
      v = region.getV2();
      v2 = region.getV();
    } else {
      u = region.getU();
      u2 = region.getU2();
      v = region.getV2();
      v2 = region.getV();
    }

    writeRotatedQuad(x, y, originX, originY, w, h, scaleX, scaleY, rotation, tidx, u, v, u2, v2);
  }

  @Override
  public void draw(TextureRegion region, float width, float height, Affine2 transform) {
    checkSpace();
    float tidx = (float) getOrAssignSlot(region.getTexture());

    float m00 = transform.m00, m01 = transform.m01, m02 = transform.m02;
    float m10 = transform.m10, m11 = transform.m11, m12 = transform.m12;

    float x1 = m02;
    float y1 = m12;
    float x2 = m01 * height + m02;
    float y2 = m11 * height + m12;
    float x3 = m00 * width + m01 * height + m02;
    float y3 = m10 * width + m11 * height + m12;
    float x4 = m00 * width + m02;
    float y4 = m10 * width + m12;

    float u = region.getU();
    float u2 = region.getU2();
    float v = region.getV2();
    float v2 = region.getV();

    putVertex(x1, y1, u, v, tidx);
    putVertex(x2, y2, u, v2, tidx);
    putVertex(x3, y3, u2, v2, tidx);
    putVertex(x4, y4, u2, v, tidx);
  }

  @Override
  public void disableBlending() {
    if (!blendingEnabled) {
      return;
    }
    flush();
    blendingEnabled = false;
    Gdx.gl.glDisable(GL20.GL_BLEND);
  }

  @Override
  public void enableBlending() {
    if (blendingEnabled) {
      return;
    }
    flush();
    blendingEnabled = true;
    Gdx.gl.glEnable(GL20.GL_BLEND);
    Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
  }

  @Override
  public void setBlendFunction(int src, int dst) {
    setBlendFunctionSeparate(src, dst, src, dst);
  }

  @Override
  public void setBlendFunctionSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
    if (blendSrcFunc == srcColor && blendDstFunc == dstColor
        && blendSrcFuncAlpha == srcAlpha && blendDstFuncAlpha == dstAlpha) {
      return;
    }
    flush();
    blendSrcFunc = srcColor;
    blendDstFunc = dstColor;
    blendSrcFuncAlpha = srcAlpha;
    blendDstFuncAlpha = dstAlpha;
    if (blendingEnabled) {
      Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
    }
  }

  @Override
  public int getBlendSrcFunc() {
    return blendSrcFunc;
  }

  @Override
  public int getBlendDstFunc() {
    return blendDstFunc;
  }

  @Override
  public int getBlendSrcFuncAlpha() {
    return blendSrcFuncAlpha;
  }

  @Override
  public int getBlendDstFuncAlpha() {
    return blendDstFuncAlpha;
  }

  @Override
  public Matrix4 getProjectionMatrix() {
    return projectionMatrix;
  }

  @Override
  public Matrix4 getTransformMatrix() {
    return transformMatrix;
  }

  @Override
  public void setProjectionMatrix(Matrix4 projection) {
    if (drawing) {
      flush();
    }
    projectionMatrix.set(projection);
    if (drawing) {
      setupMatrices();
    }
  }

  @Override
  public void setTransformMatrix(Matrix4 transform) {
    if (drawing) {
      flush();
    }
    transformMatrix.set(transform);
    if (drawing) {
      setupMatrices();
    }
  }

  @Override
  public void setShader(ShaderProgram shader) {
    if (drawing) {
      flush();
    }
    customShader = shader;
    if (drawing) {
      ShaderProgram s = activeShader();
      s.bind();
      setupMatrices();
    }
  }

  @Override
  public ShaderProgram getShader() {
    return customShader != null ? customShader : builtinShader;
  }

  @Override
  public boolean isBlendingEnabled() {
    return blendingEnabled;
  }

  @Override
  public boolean isDrawing() {
    return drawing;
  }

  @Override
  public void dispose() {
    mesh.dispose();
    builtinShader.dispose();
  }

  @Override
  public int getRenderCalls() {
    return renderCalls;
  }

  @Override
  public int getTotalRenderCalls() {
    return totalRenderCalls;
  }

  /** Returns the number of texture slots this batch can hold before it must flush. */
  public int getMaxTextureSlots() {
    return maxTextureSlots;
  }

  private ShaderProgram activeShader() {
    return customShader != null ? customShader : builtinShader;
  }

  private void setupMatrices() {
    combinedMatrix.set(projectionMatrix).mul(transformMatrix);
    activeShader().setUniformMatrix("u_projTrans", combinedMatrix);
  }

  private int getOrAssignSlot(Texture texture) {
    for (int i = 0; i < usedSlots; i++) {
      if (textures[i] == texture) {
        return i;
      }
    }
    if (usedSlots >= maxTextureSlots) {
      flush();
    }
    textures[usedSlots] = texture;
    return usedSlots++;
  }

  private void checkSpace() {
    if (vertexIdx + FLOATS_PER_VERTEX * VERTICES_PER_QUAD > vertices.length) {
      flush();
    }
  }

  private void putVertex(float x, float y, float u, float v, float texIdx) {
    vertices[vertexIdx++] = x;
    vertices[vertexIdx++] = y;
    vertices[vertexIdx++] = colorPacked;
    vertices[vertexIdx++] = u;
    vertices[vertexIdx++] = v;
    vertices[vertexIdx++] = texIdx;
  }

  private void writeRotatedQuad(float x, float y, float originX, float originY, float w, float h,
      float scaleX, float scaleY, float rotation, float tidx, float u, float v, float u2, float v2) {
    float worldOriginX = x + originX;
    float worldOriginY = y + originY;
    float fx = -originX;
    float fy = -originY;
    float fx2 = w - originX;
    float fy2 = h - originY;

    if (scaleX != 1f || scaleY != 1f) {
      fx *= scaleX;
      fy *= scaleY;
      fx2 *= scaleX;
      fy2 *= scaleY;
    }

    float x1, y1, x2, y2, x3, y3, x4, y4;
    if (rotation != 0f) {
      float cos = MathUtils.cosDeg(rotation);
      float sin = MathUtils.sinDeg(rotation);
      x1 = cos * fx - sin * fy;
      y1 = sin * fx + cos * fy;
      x2 = cos * fx - sin * fy2;
      y2 = sin * fx + cos * fy2;
      x3 = cos * fx2 - sin * fy2;
      y3 = sin * fx2 + cos * fy2;
      x4 = cos * fx2 - sin * fy;
      y4 = sin * fx2 + cos * fy;
    } else {
      x1 = fx;
      y1 = fy;
      x2 = fx;
      y2 = fy2;
      x3 = fx2;
      y3 = fy2;
      x4 = fx2;
      y4 = fy;
    }

    x1 += worldOriginX;
    y1 += worldOriginY;
    x2 += worldOriginX;
    y2 += worldOriginY;
    x3 += worldOriginX;
    y3 += worldOriginY;
    x4 += worldOriginX;
    y4 += worldOriginY;

    putVertex(x1, y1, u, v, tidx);
    putVertex(x2, y2, u, v2, tidx);
    putVertex(x3, y3, u2, v2, tidx);
    putVertex(x4, y4, u2, v, tidx);
  }

  private static ShaderProgram buildShader(int slots) {
    String vert = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
        + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
        + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
        + "attribute float a_texIndex;\n"
        + "uniform mat4 u_projTrans;\n"
        + "varying vec4 v_color;\n"
        + "varying vec2 v_texCoord;\n"
        + "varying float v_texIndex;\n"
        + "void main() {\n"
        + "    v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
        + "    v_color.a = v_color.a * (255.0 / 254.0);\n"
        + "    v_texCoord = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
        + "    v_texIndex = a_texIndex;\n"
        + "    gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
        + "}\n";

    StringBuilder frag = new StringBuilder(256 + slots * 80);
    frag.append("#ifdef GL_ES\nprecision mediump float;\n#endif\n");
    frag.append("varying vec4 v_color;\nvarying vec2 v_texCoord;\nvarying float v_texIndex;\n");
    for (int i = 0; i < slots; i++) {
      frag.append("uniform sampler2D u_texture").append(i).append(";\n");
    }
    frag.append("void main() {\n    vec4 tex;\n");
    for (int i = 0; i < slots; i++) {
      frag.append(i == 0 ? "    if" : "    else if");
      frag.append(" (v_texIndex < ")
          .append(i)
          .append(".5) tex = texture2D(u_texture")
          .append(i)
          .append(", v_texCoord);\n");
    }
    frag.append("    gl_FragColor = v_color * tex;\n}\n");

    ShaderProgram shader = new ShaderProgram(vert, frag.toString());
    if (!shader.isCompiled()) {
      throw new IllegalStateException("FlixelSpriteBatch shader compilation failed:\n" + shader.getLog());
    }
    return shader;
  }
}
