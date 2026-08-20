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
package org.flixelgdx.backend.html5.graphics;

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLUniformLocation;

/**
 * A WebGL2 sprite batch: the web backend's implementation of {@link FlixelBatch}.
 *
 * <p>It works the same way every 2D batcher does. Each {@code draw} call appends one textured quad
 * (four vertices, eight floats each: position, texture coordinates, and an RGBA tint) into a CPU
 * array. Nothing touches the GPU until a {@link #flush()}, which happens automatically when the
 * bound texture, blend mode, or matrices change, when the buffer fills, or at {@link #end()}.
 * Grouping many quads into one upload and one draw call is what keeps 2D rendering fast, so drawing
 * many sprites from a single atlas costs only one submission.
 *
 * <p>Custom shaders are not yet wired on the web backend, so {@link #setShader(FlixelShader)} is
 * recorded but the built-in sprite shader is always used. The additive, multiply, and screen blend
 * modes map to WebGL blend functions; the modes that need a separate blend equation fall back to
 * normal alpha blending.
 */
public class FlixelWebGlBatch implements FlixelBatch {

  private static final int MAX_QUADS = 2000;
  private static final int FLOATS_PER_VERTEX = 8;
  private static final int FLOATS_PER_QUAD = FLOATS_PER_VERTEX * 4;

  @NotNull
  private final WebGLRenderingContext gl;

  @NotNull
  private final WebGLProgram program;

  @NotNull
  private final WebGLBuffer vertexBuffer;

  @NotNull
  private final WebGLBuffer indexBuffer;

  @Nullable
  private final WebGLUniformLocation projTransLocation;

  @Nullable
  private final WebGLUniformLocation textureLocation;

  private final int positionAttrib;
  private final int texCoordAttrib;
  private final int colorAttrib;

  private final float[] vertices = new float[MAX_QUADS * FLOATS_PER_QUAD];
  private final float[] combined = new float[16];

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
  private FlixelWebGlTexture currentTexture;

  private int quadCount;
  private int renderCalls;
  private int totalRenderCalls;

  private boolean drawing;

  /**
   * Builds the batch and its GPU resources.
   *
   * @param gl The WebGL rendering context.
   */
  public FlixelWebGlBatch(@NotNull WebGLRenderingContext gl) {
    this.gl = gl;
    this.program = buildProgram(gl);
    this.positionAttrib = gl.getAttribLocation(program, "a_position");
    this.texCoordAttrib = gl.getAttribLocation(program, "a_texCoord");
    this.colorAttrib = gl.getAttribLocation(program, "a_color");
    this.projTransLocation = gl.getUniformLocation(program, "u_projTrans");
    this.textureLocation = gl.getUniformLocation(program, "u_texture");

    this.vertexBuffer = gl.createBuffer();
    this.indexBuffer = gl.createBuffer();
    uploadIndices();
  }

  @Override
  public void begin() {
    drawing = true;
    renderCalls = 0;
    quadCount = 0;
    gl.useProgram(program);
    if (textureLocation != null) {
      gl.uniform1i(textureLocation, 0);
    }
  }

  @Override
  public void end() {
    flush();
    drawing = false;
    currentTexture = null;
  }

  @Override
  public void flush() {
    if (quadCount == 0 || currentTexture == null) {
      return;
    }

    multiply(combined, projection.val, transform.val);
    applyBlendMode();

    gl.useProgram(program);
    if (projTransLocation != null) {
      gl.uniformMatrix4fv(projTransLocation, false, combined);
    }

    gl.activeTexture(WebGLRenderingContext.TEXTURE0);
    gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, currentTexture.getGlTexture());

    gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);
    gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, Float32Array.fromJavaArray(vertices),
        WebGLRenderingContext.DYNAMIC_DRAW);

    int stride = FLOATS_PER_VERTEX * 4;
    enable(positionAttrib, 2, stride, 0);
    enable(texCoordAttrib, 2, stride, 2 * 4);
    enable(colorAttrib, 4, stride, 4 * 4);

    gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);
    gl.drawElements(WebGLRenderingContext.TRIANGLES, quadCount * 6, WebGLRenderingContext.UNSIGNED_SHORT, 0);

    quadCount = 0;
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
    switchTexture(texture);
    appendQuad(x, y + height, x + width, y + height, x + width, y, x, y, u, v2, u2, v,
        color.r, color.g, color.b, color.a);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {
    switchTexture(frame.getTexture());
    appendQuad(x, y + height, x + width, y + height, x + width, y, x, y,
        frame.getU(), frame.getV2(), frame.getU2(), frame.getV(),
        color.r, color.g, color.b, color.a);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
      float width, float height, float scaleX, float scaleY, float rotation,
      boolean flipX, boolean flipY) {
    switchTexture(frame.getTexture());

    float worldOriginX = x + originX;
    float worldOriginY = y + originY;
    float localX = -originX;
    float localY = -originY;
    float localX2 = localX + width;
    float localY2 = localY + height;

    localX *= scaleX;
    localY *= scaleY;
    localX2 *= scaleX;
    localY2 *= scaleY;

    float cos = 1f;
    float sin = 0f;
    if (rotation != 0f) {
      float radians = (float) Math.toRadians(rotation);
      cos = (float) Math.cos(radians);
      sin = (float) Math.sin(radians);
    }

    float x1 = worldOriginX + cos * localX - sin * localY;
    float y1 = worldOriginY + sin * localX + cos * localY;
    float x2 = worldOriginX + cos * localX - sin * localY2;
    float y2 = worldOriginY + sin * localX + cos * localY2;
    float x3 = worldOriginX + cos * localX2 - sin * localY2;
    float y3 = worldOriginY + sin * localX2 + cos * localY2;
    float x4 = worldOriginX + cos * localX2 - sin * localY;
    float y4 = worldOriginY + sin * localX2 + cos * localY;

    float u = flipX ? frame.getU2() : frame.getU();
    float u2 = flipX ? frame.getU() : frame.getU2();
    float v = flipY ? frame.getV2() : frame.getV();
    float v2 = flipY ? frame.getV() : frame.getV2();

    // Vertices wound bottom-left, bottom-right, top-right, top-left.
    appendQuad(x2, y2, x3, y3, x4, y4, x1, y1, u, v2, u2, v,
        color.r, color.g, color.b, color.a);
  }

  @Override
  public void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine transform) {
    switchTexture(frame.getTexture());

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
        color.r, color.g, color.b, color.a);
  }

  @Override
  public void draw(@NotNull FlixelTexture texture, float @NotNull [] verts, int offset, int count) {
    switchTexture(texture);
    int quads = count / 20;
    for (int q = 0; q < quads; q++) {
      int base = offset + q * 20;
      if (quadCount >= MAX_QUADS) {
        flush();
      }
      int out = quadCount * FLOATS_PER_QUAD;
      for (int corner = 0; corner < 4; corner++) {
        int in = base + corner * 5;
        float packed = verts[in + 4];
        int bits = Float.floatToRawIntBits(packed);
        float r = (bits & 0xFF) / 255f * color.r;
        float g = ((bits >>> 8) & 0xFF) / 255f * color.g;
        float b = ((bits >>> 16) & 0xFF) / 255f * color.b;
        float a = ((bits >>> 24) & 0xFF) / 255f * color.a;
        out = writeVertex(out, verts[in], verts[in + 1], verts[in + 2], verts[in + 3], r, g, b, a);
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
    if (shader != this.shader) {
      flush();
      this.shader = shader;
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
    gl.deleteBuffer(vertexBuffer);
    gl.deleteBuffer(indexBuffer);
    gl.deleteProgram(program);
  }

  /**
   * Switches the bound texture, flushing first when the texture actually changes so quads never mix
   * two textures in one draw call.
   *
   * @param texture The texture the next quad samples from.
   */
  private void switchTexture(FlixelTexture texture) {
    FlixelWebGlTexture webGl = (FlixelWebGlTexture) texture;
    if (currentTexture == null || currentTexture.getHandle() != webGl.getHandle() || quadCount >= MAX_QUADS) {
      flush();
      currentTexture = webGl;
    }
  }

  /**
   * Appends one quad's four vertices in the winding the batch expects.
   *
   * @param x1 Bottom-left x.
   * @param y1 Bottom-left y.
   * @param x2 Bottom-right x.
   * @param y2 Bottom-right y.
   * @param x3 Top-right x.
   * @param y3 Top-right y.
   * @param x4 Top-left x.
   * @param y4 Top-left y.
   * @param u Left texture coordinate.
   * @param v Bottom texture coordinate.
   * @param u2 Right texture coordinate.
   * @param v2 Top texture coordinate.
   * @param r Red tint in {@code [0, 1]}.
   * @param g Green tint in {@code [0, 1]}.
   * @param b Blue tint in {@code [0, 1]}.
   * @param a Alpha tint in {@code [0, 1]}.
   */
  private void appendQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
      float u, float v, float u2, float v2, float r, float g, float b, float a) {
    if (quadCount >= MAX_QUADS) {
      flush();
    }
    int out = quadCount * FLOATS_PER_QUAD;
    out = writeVertex(out, x1, y1, u, v, r, g, b, a);
    out = writeVertex(out, x2, y2, u2, v, r, g, b, a);
    out = writeVertex(out, x3, y3, u2, v2, r, g, b, a);
    writeVertex(out, x4, y4, u, v2, r, g, b, a);
    quadCount++;
  }

  /**
   * Writes one vertex into the CPU vertex array.
   *
   * @param out The float index to write at.
   * @param x Vertex x.
   * @param y Vertex y.
   * @param u Texture u.
   * @param v Texture v.
   * @param r Red tint.
   * @param g Green tint.
   * @param b Blue tint.
   * @param a Alpha tint.
   * @return The float index just past the written vertex.
   */
  private int writeVertex(int out, float x, float y, float u, float v, float r, float g, float b, float a) {
    vertices[out] = x;
    vertices[out + 1] = y;
    vertices[out + 2] = u;
    vertices[out + 3] = v;
    vertices[out + 4] = r;
    vertices[out + 5] = g;
    vertices[out + 6] = b;
    vertices[out + 7] = a;
    return out + FLOATS_PER_VERTEX;
  }

  /**
   * Points a vertex attribute at its slice of the interleaved vertex buffer and enables it.
   *
   * @param attrib The attribute location, or negative when the shader dropped it.
   * @param size The number of floats in the attribute.
   * @param stride The byte stride between vertices.
   * @param offset The byte offset of the attribute within a vertex.
   */
  private void enable(int attrib, int size, int stride, int offset) {
    if (attrib < 0) {
      return;
    }
    gl.enableVertexAttribArray(attrib);
    gl.vertexAttribPointer(attrib, size, WebGLRenderingContext.FLOAT, false, stride, offset);
  }

  /** Selects the WebGL blend function for the current blend mode. */
  private void applyBlendMode() {
    gl.enable(WebGLRenderingContext.BLEND);
    switch (blendMode) {
      case ADD -> gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE);
      case MULTIPLY -> gl.blendFunc(WebGLRenderingContext.DST_COLOR, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
      case SCREEN -> gl.blendFunc(WebGLRenderingContext.ONE, WebGLRenderingContext.ONE_MINUS_SRC_COLOR);
      default -> gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
    }
  }

  /** Uploads the static quad index pattern once, since it never changes. */
  private void uploadIndices() {
    short[] indices = new short[MAX_QUADS * 6];
    short vertex = 0;
    for (int i = 0; i < indices.length; i += 6) {
      indices[i] = vertex;
      indices[i + 1] = (short) (vertex + 1);
      indices[i + 2] = (short) (vertex + 2);
      indices[i + 3] = (short) (vertex + 2);
      indices[i + 4] = (short) (vertex + 3);
      indices[i + 5] = vertex;
      vertex += 4;
    }
    gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indexBuffer);
    gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, Int16Array.fromJavaArray(indices),
        WebGLRenderingContext.STATIC_DRAW);
  }

  /**
   * Multiplies two column-major 4x4 matrices into a destination array without allocating.
   *
   * @param out The destination for the product; sixteen floats.
   * @param a The left matrix.
   * @param b The right matrix.
   */
  private static void multiply(float[] out, float[] a, float[] b) {
    for (int col = 0; col < 4; col++) {
      for (int row = 0; row < 4; row++) {
        out[col * 4 + row] = a[row] * b[col * 4]
            + a[4 + row] * b[col * 4 + 1]
            + a[8 + row] * b[col * 4 + 2]
            + a[12 + row] * b[col * 4 + 3];
      }
    }
  }

  /**
   * Compiles and links the built-in sprite shader program.
   *
   * @param gl The rendering context.
   * @return The linked program.
   */
  private static WebGLProgram buildProgram(WebGLRenderingContext gl) {
    WebGLShader vertex = compile(gl, WebGLRenderingContext.VERTEX_SHADER, VERTEX_SOURCE);
    WebGLShader fragment = compile(gl, WebGLRenderingContext.FRAGMENT_SHADER, FRAGMENT_SOURCE);
    WebGLProgram program = gl.createProgram();
    gl.attachShader(program, vertex);
    gl.attachShader(program, fragment);
    gl.linkProgram(program);
    return program;
  }

  /**
   * Compiles a single shader stage.
   *
   * @param gl The rendering context.
   * @param type The shader stage constant.
   * @param source The GLSL source.
   * @return The compiled shader.
   */
  private static WebGLShader compile(WebGLRenderingContext gl, int type, String source) {
    WebGLShader shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    return shader;
  }

  private static final String VERTEX_SOURCE =
      "#version 300 es\n"
      + "in vec2 a_position;\n"
      + "in vec2 a_texCoord;\n"
      + "in vec4 a_color;\n"
      + "uniform mat4 u_projTrans;\n"
      + "out vec2 v_texCoord;\n"
      + "out vec4 v_color;\n"
      + "void main() {\n"
      + "  v_texCoord = a_texCoord;\n"
      + "  v_color = a_color;\n"
      + "  gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);\n"
      + "}\n";

  private static final String FRAGMENT_SOURCE =
      "#version 300 es\n"
      + "precision mediump float;\n"
      + "in vec2 v_texCoord;\n"
      + "in vec4 v_color;\n"
      + "uniform sampler2D u_texture;\n"
      + "out vec4 fragColor;\n"
      + "void main() {\n"
      + "  fragColor = v_color * texture(u_texture, v_texCoord);\n"
      + "}\n";
}
