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
package org.flixelgdx.backend.desktop.debug;

import org.flixelgdx.backend.desktop.graphics.FlixelBgfxGraphics;
import org.flixelgdx.math.FlixelMatrix;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXTransientIndexBuffer;
import org.lwjgl.bgfx.BGFXTransientVertexBuffer;
import org.lwjgl.bgfx.BGFXVertexLayout;

import java.nio.ByteBuffer;

import imgui.ImDrawData;
import imgui.ImVec4;

/**
 * Renders Dear ImGui draw data through bgfx.
 *
 * <p>Dear ImGui is renderer-agnostic: each frame it produces an {@link ImDrawData} object, which is
 * just a list of vertex buffers, index buffers, and draw commands (a clip rectangle, a texture, and
 * a slice of indices). A renderer backend walks that list and submits it to a graphics API. imgui-java
 * ships backends for raw OpenGL and Vulkan, but this project renders everything through bgfx, so this
 * class is the bespoke bgfx backend.
 *
 * <p>The ImGui vertex format is identical to the framework's sprite vertex format: two floats of
 * position, two floats of texture coordinate, then four bytes of packed color. The sprite fragment
 * shader already outputs {@code texture * vertexColor}, which is exactly what ImGui needs, so this
 * renderer reuses {@link FlixelBgfxGraphics#getSpriteProgram() the sprite program} instead of
 * compiling a dedicated ImGui shader for every backend.
 *
 * <p>Everything is uploaded through bgfx transient buffers (the same short-lived, per-frame geometry
 * path the sprite batch uses), so no GPU buffers are retained between frames. The only persistent GPU
 * resource is the font atlas texture, created once in {@link #init(ByteBuffer, int, int)} and
 * destroyed in {@link #dispose()}.
 */
public final class FlixelImGuiBgfxRenderer {

  /**
   * Byte offset of the packed color inside each vertex (after the two position and two UV floats).
   * This is fixed by the ImGui vertex format regardless of the total vertex stride.
   */
  private static final int COLOR_OFFSET = 16;

  /** Point-sampled, clamped filtering so both the bitmap font and inspected pixel-art stay crisp. */
  private static final int SAMPLER_FLAGS =
      BGFX.BGFX_SAMPLER_POINT | BGFX.BGFX_SAMPLER_U_CLAMP | BGFX.BGFX_SAMPLER_V_CLAMP;

  /**
   * Standard straight-alpha "over" blend, matching {@link org.flixelgdx.util.FlixelBlendMode#NORMAL}.
   * Color blends with source alpha; the destination alpha accumulates so the overlay composites
   * correctly onto an alpha-capable back buffer.
   */
  private static final long DRAW_STATE = BGFX.BGFX_STATE_WRITE_RGB | BGFX.BGFX_STATE_WRITE_A
      | BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_SRC_ALPHA, BGFX.BGFX_STATE_BLEND_INV_SRC_ALPHA,
          BGFX.BGFX_STATE_BLEND_ONE, BGFX.BGFX_STATE_BLEND_INV_SRC_ALPHA);

  @NotNull
  private final FlixelBgfxGraphics graphics;

  @NotNull
  private final BGFXVertexLayout layout = BGFXVertexLayout.create();

  @NotNull
  private final BGFXTransientVertexBuffer tvb = BGFXTransientVertexBuffer.create();

  @NotNull
  private final BGFXTransientIndexBuffer tib = BGFXTransientIndexBuffer.create();

  /** Reused ortho matrix rebuilt each frame to map ImGui's pixel coordinates onto the back buffer. */
  @NotNull
  private final FlixelMatrix ortho = new FlixelMatrix();

  /** Reused clip-rectangle holder filled per draw command (min x, min y, max x, max y). */
  @NotNull
  private final ImVec4 clip = new ImVec4();

  /** The font atlas texture handle, or {@code -1} until {@link #init(ByteBuffer, int, int)} uploads it. */
  private short fontTexture = -1;

  private boolean initialized;

  /**
   * Creates a renderer bound to the desktop bgfx graphics manager.
   *
   * @param graphics The bgfx graphics manager whose sprite program, texture uniform, and view
   *     allocation the renderer reuses.
   */
  public FlixelImGuiBgfxRenderer(@NotNull FlixelBgfxGraphics graphics) {
    this.graphics = graphics;
  }

  /**
   * Builds the ImGui vertex layout and uploads the font atlas texture.
   *
   * <p>Call once, after the Dear ImGui context exists and its fonts have been built, but before the
   * first {@link #render(ImDrawData)}.
   *
   * @param fontPixels The RGBA8 font atlas pixels from {@code io.getFonts().getTexDataAsRGBA32(...)}.
   * @param fontWidth The atlas width in pixels.
   * @param fontHeight The atlas height in pixels.
   * @return The bgfx texture handle assigned to the atlas, so the caller can register it as the
   *     Dear ImGui font texture id, or {@code -1} if the texture could not be created.
   */
  public long init(@NotNull ByteBuffer fontPixels, int fontWidth, int fontHeight) {
    int renderer = BGFX.bgfx_get_renderer_type();
    BGFX.bgfx_vertex_layout_begin(layout, renderer);
    BGFX.bgfx_vertex_layout_add(layout, BGFX.BGFX_ATTRIB_POSITION, 2, BGFX.BGFX_ATTRIB_TYPE_FLOAT, false, false);
    BGFX.bgfx_vertex_layout_add(layout, BGFX.BGFX_ATTRIB_TEXCOORD0, 2, BGFX.BGFX_ATTRIB_TYPE_FLOAT, false, false);
    BGFX.bgfx_vertex_layout_add(layout, BGFX.BGFX_ATTRIB_COLOR0, 4, BGFX.BGFX_ATTRIB_TYPE_UINT8, true, false);
    BGFX.bgfx_vertex_layout_end(layout);

    // The atlas is white with per-texel coverage in the alpha channel, so the red/blue swizzle that
    // some backends need for arbitrary textures does not matter here; upload the pixels as-is.
    fontTexture = BGFX.bgfx_create_texture_2d(fontWidth, fontHeight, false, 1,
        BGFX.BGFX_TEXTURE_FORMAT_RGBA8, BGFX.BGFX_TEXTURE_NONE, BGFX.bgfx_copy(fontPixels), 0L);
    initialized = fontTexture != -1;
    return fontTexture;
  }

  /**
   * Submits one frame of Dear ImGui draw data to bgfx.
   *
   * <p>Reuses the sprite program and reserves a fresh top-most view (see
   * {@link FlixelBgfxGraphics#beginOverlayView()}) so the overlay draws over the finished game frame.
   * Each ImGui command list is uploaded into a transient vertex and index buffer, then every command
   * within it becomes one bgfx draw with its own scissor rectangle and bound texture.
   *
   * @param drawData The draw data returned by {@code ImGui.getDrawData()} after {@code ImGui.render()}.
   */
  public void render(@NotNull ImDrawData drawData) {
    short program = graphics.getSpriteProgram();
    short textureUniform = graphics.getTextureUniform();
    if (!initialized || program == -1 || textureUniform == -1) {
      return;
    }
    int cmdListCount = drawData.getCmdListsCount();
    if (cmdListCount <= 0) {
      return;
    }

    float displayWidth = drawData.getDisplaySizeX();
    float displayHeight = drawData.getDisplaySizeY();
    if (displayWidth <= 0f || displayHeight <= 0f) {
      return;
    }

    // ImGui's vertex and index sizes are decided by the native build (16- or 32-bit indices, and the
    // vertex stride), so read them at runtime rather than assuming, exactly as imgui-java's own
    // renderers do. Getting either wrong scrambles the geometry into stray triangles.
    int vertexStride = ImDrawData.sizeOfImDrawVert();
    int indexStride = ImDrawData.sizeOfImDrawIdx();
    boolean index32 = indexStride >= 4;

    int view = graphics.beginOverlayView();
    // ImGui works in top-left, y-down pixel space, so a y-down ortho over the display maps its
    // vertices straight to the back buffer. Match the backend's depth range so the geometry is not
    // clipped on the [0, 1] backends (Vulkan, Metal, Direct3D).
    ortho.setToOrtho2DYDown(0, 0, displayWidth, displayHeight, graphics.isDepthZeroToOne());
    BGFX.bgfx_set_view_transform(view, null, ortho.val);

    boolean bgra = graphics.isDepthZeroToOne();
    int fbWidth = Math.round(displayWidth);
    int fbHeight = Math.round(displayHeight);

    for (int n = 0; n < cmdListCount; n++) {
      // Read the element counts from the draw list rather than a buffer's byte size. imgui-java hands
      // back the same reused ByteBuffer for a command list's vertex and index data (see below), so the
      // vertex buffer's remaining() cannot be trusted once the index buffer has been fetched.
      int vertexCount = drawData.getCmdListVtxBufferSize(n);
      int indexCount = drawData.getCmdListIdxBufferSize(n);
      if (vertexCount == 0 || indexCount == 0) {
        continue;
      }
      // bgfx clamps over-allocation and warns; skipping a list that does not fit avoids a partially
      // uploaded frame if the overlay ever produces more geometry than the transient pool holds.
      if (BGFX.bgfx_get_avail_transient_vertex_buffer(vertexCount, layout) < vertexCount
          || BGFX.bgfx_get_avail_transient_index_buffer(indexCount, index32) < indexCount) {
        continue;
      }

      // imgui-java returns a single shared ByteBuffer from both getCmdListVtxBufferData and
      // getCmdListIdxBufferData, re-pointing it at whichever was requested last. The vertices must
      // therefore be fetched and fully copied into the transient buffer BEFORE the index buffer is
      // fetched; otherwise the vertex upload reads the index data instead, producing degenerate,
      // invisible geometry.
      BGFX.bgfx_alloc_transient_vertex_buffer(tvb, vertexCount, layout);
      copyVertices(drawData.getCmdListVtxBufferData(n), tvb.data(), vertexCount, vertexStride, bgra);

      BGFX.bgfx_alloc_transient_index_buffer(tib, indexCount, index32);
      ByteBuffer indexData = drawData.getCmdListIdxBufferData(n);
      ByteBuffer tibData = tib.data();
      int origIdxLim = indexData.limit();
      indexData.limit(indexData.position() + indexCount * indexStride);
      tibData.put(indexData);
      indexData.limit(origIdxLim);

      int cmdCount = drawData.getCmdListCmdBufferSize(n);
      for (int cmd = 0; cmd < cmdCount; cmd++) {
        int elemCount = drawData.getCmdListCmdBufferElemCount(n, cmd);
        if (elemCount == 0) {
          continue;
        }
        int idxOffset = drawData.getCmdListCmdBufferIdxOffset(n, cmd);
        drawData.getCmdListCmdBufferClipRect(clip, n, cmd);

        int clipX = Math.max(0, (int) clip.x);
        int clipY = Math.max(0, (int) clip.y);
        int clipMaxX = Math.min(fbWidth, (int) clip.z);
        int clipMaxY = Math.min(fbHeight, (int) clip.w);
        if (clipMaxX <= clipX || clipMaxY <= clipY) {
          continue;
        }

        // Bind the whole command list's vertices (start vertex 0) and let the index buffer address
        // them, matching bgfx's own Dear ImGui integration. Dear ImGui keeps each command list under
        // the 16-bit index limit (the RendererHasVtxOffset backend flag is left off), so no per-command
        // base vertex is needed, and this avoids relying on bgfx's transient base-vertex path.
        short texture = (short) drawData.getCmdListCmdBufferTextureId(n, cmd);
        BGFX.bgfx_set_scissor(clipX, clipY, clipMaxX - clipX, clipMaxY - clipY);
        BGFX.bgfx_set_transient_vertex_buffer(0, tvb, 0, vertexCount);
        BGFX.bgfx_set_transient_index_buffer(tib, idxOffset, elemCount);
        BGFX.bgfx_set_texture(0, textureUniform, texture, SAMPLER_FLAGS);
        BGFX.bgfx_set_state(DRAW_STATE, 0);
        BGFX.bgfx_submit(view, program, 0, BGFX.BGFX_DISCARD_ALL);
      }
    }
  }

  /**
   * Copies one command list's vertices into the transient buffer, swapping each vertex color's red
   * and blue bytes when the active backend stores colors in BGRA order.
   *
   * <p>ImGui always packs vertex color as RGBA bytes ({@code IM_COL32}). On OpenGL that matches the
   * color attribute's memory order, so the whole block copies verbatim. On Vulkan, Metal, and
   * Direct3D the sprite layout uses BGRA order (the same convention the sprite batch follows), so the
   * red and blue bytes are swapped in place after the bulk copy.
   *
   * <p>The bulk copy uses {@link ByteBuffer#put(ByteBuffer)} rather than a raw address-based copy
   * because the TVB's backing memory is not reliably accessible through its native address on all
   * Vulkan/Linux driver and JVM combinations. The JDK's direct-buffer path (the same one the sprite
   * batch uses for its TVB uploads) is the only path that is guaranteed to reach the GPU-visible
   * allocation.
   *
   * @param src The ImGui vertex bytes for one command list.
   * @param dst The transient vertex buffer memory to fill.
   * @param vertexCount The number of vertices to copy.
   * @param vertexStride The byte size of one ImGui vertex.
   * @param bgra Whether the active backend expects BGRA color byte order.
   */
  private static void copyVertices(@NotNull ByteBuffer src, @NotNull ByteBuffer dst, int vertexCount,
      int vertexStride, boolean bgra) {
    int origLim = src.limit();
    src.limit(src.position() + vertexCount * vertexStride);
    dst.put(src);
    src.limit(origLim);
    if (!bgra) {
      return;
    }
    for (int i = 0; i < vertexCount; i++) {
      int base = i * vertexStride + COLOR_OFFSET;
      byte r = dst.get(base);
      dst.put(base, dst.get(base + 2));
      dst.put(base + 2, r);
    }
  }

  /** Destroys the font atlas texture. Safe to call more than once. */
  public void dispose() {
    if (fontTexture != -1) {
      BGFX.bgfx_destroy_texture(fontTexture);
      fontTexture = -1;
    }
    initialized = false;
  }
}
