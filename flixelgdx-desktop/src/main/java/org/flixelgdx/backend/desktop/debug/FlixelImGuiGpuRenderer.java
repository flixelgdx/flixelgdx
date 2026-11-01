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

import org.flixelgdx.backend.gpu.FlixelGpuGraphics;
import org.flixelgdx.backend.gpu.FlixelGpuTexture;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.math.FlixelMatrix;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import imgui.ImDrawData;
import imgui.ImVec4;

/**
 * Draws Dear ImGui's draw lists with the shared WebGPU renderer.
 *
 * <p>ImGui hands over, for each window, a block of vertices, a block of indices and a list of commands (a
 * texture, a clip rectangle and a range of indices). This class copies the vertices and indices into the
 * renderer's per-frame ring buffers and turns each command into one indexed draw with its own scissor rectangle.
 * ImGui's vertex is exactly the sprite vertex (position, texture coordinate and an RGBA byte color), so the
 * sprite pipeline draws it as is.
 */
public final class FlixelImGuiGpuRenderer {

  private final FlixelGpuGraphics graphics;
  private final FlixelMatrix ortho = new FlixelMatrix();
  private final ImVec4 clip = new ImVec4();

  private FlixelGpuTexture fontTexture;
  private ByteBuffer scratch = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder());

  private boolean initialized;

  /**
   * Creates a renderer bound to the shared WebGPU graphics manager.
   *
   * @param graphics The graphics manager whose sprite pipeline and ring buffers the renderer reuses.
   */
  public FlixelImGuiGpuRenderer(@NotNull FlixelGpuGraphics graphics) {
    this.graphics = graphics;
  }

  /**
   * Uploads the font atlas texture.
   *
   * <p>Call once, after the Dear ImGui context exists and its fonts have been built, but before the first
   * {@link #render(ImDrawData)}.
   *
   * @param fontPixels The RGBA8 font atlas pixels from {@code io.getFonts().getTexDataAsRGBA32(...)}.
   * @param fontWidth The atlas width in pixels.
   * @param fontHeight The atlas height in pixels.
   * @return The texture id to register as the Dear ImGui font texture, or {@code -1} if the texture could not
   *     be created.
   */
  public long init(@NotNull ByteBuffer fontPixels, int fontWidth, int fontHeight) {
    FlixelImage atlas = new FlixelImage(fontWidth, fontHeight, fontPixels);
    fontTexture = (FlixelGpuTexture) graphics.createTexture(atlas);
    // Point sampling keeps the bitmap font and inspected pixel art crisp.
    fontTexture.setSmooth(false);
    initialized = true;
    return fontTexture.getHandle();
  }

  /**
   * Draws one frame of Dear ImGui draw data into the back buffer.
   *
   * @param drawData The draw data returned by {@code ImGui.getDrawData()} after {@code ImGui.render()}.
   */
  public void render(@NotNull ImDrawData drawData) {
    if (!initialized) {
      return;
    }
    int cmdListCount = drawData.getCmdListsCount();
    float displayWidth = drawData.getDisplaySizeX();
    float displayHeight = drawData.getDisplaySizeY();
    if (cmdListCount <= 0 || displayWidth <= 0f || displayHeight <= 0f || !graphics.beginOverlay()) {
      return;
    }
    // ImGui works in top-left, y-down pixel space, so a y-down ortho over the display maps its vertices
    // straight to the back buffer.
    ortho.setToOrtho2DYDown(0, 0, displayWidth, displayHeight, true);
    int fbWidth = Math.round(displayWidth);
    int fbHeight = Math.round(displayHeight);
    // The sizes are decided by the native build (16 or 32 bit indices, the vertex stride), so read them at
    // run time instead of assuming.
    int vertexStride = ImDrawData.sizeOfImDrawVert();
    int indexStride = ImDrawData.sizeOfImDrawIdx();
    boolean index32 = indexStride >= 4;

    for (int n = 0; n < cmdListCount; n++) {
      int vertexCount = drawData.getCmdListVtxBufferSize(n);
      int indexCount = drawData.getCmdListIdxBufferSize(n);
      if (vertexCount == 0 || indexCount == 0) {
        continue;
      }
      long vertexBytes = (long) vertexCount * vertexStride;
      long indexBytes = (long) indexCount * indexStride;
      long vertexOffset = -1L;
      long indexOffset = -1L;
      for (int attempt = 0; attempt < 2 && (vertexOffset < 0L || indexOffset < 0L); attempt++) {
        if (attempt == 1) {
          // A ring ran full: submit what is recorded so far, rewind, and upload this list again.
          graphics.restartCommands();
        }
        // imgui-java returns one shared ByteBuffer for both getCmdListVtxBufferData and
        // getCmdListIdxBufferData, re-pointing it at whichever was requested last. The vertices must be
        // copied into the ring BEFORE the indices are fetched.
        ByteBuffer vertices = drawData.getCmdListVtxBufferData(n);
        int vertexLimit = vertices.limit();
        vertices.limit(vertices.position() + (int) vertexBytes);
        vertexOffset = graphics.writeOverlayVertices(vertices);
        vertices.limit(vertexLimit);
        ByteBuffer indices = drawData.getCmdListIdxBufferData(n);
        int indexLimit = indices.limit();
        indexOffset = graphics.writeOverlayIndices(paddedIndices(indices, (int) indexBytes));
        indices.limit(indexLimit);
      }
      if (vertexOffset < 0L || indexOffset < 0L) {
        continue;
      }
      long paddedIndexBytes = (indexBytes + 3L) & ~3L;
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
        FlixelGpuTexture texture = graphics.getTexture(drawData.getCmdListCmdBufferTextureId(n, cmd));
        if (texture == null) {
          continue;
        }
        graphics.drawOverlay(texture, vertexOffset, vertexBytes, indexOffset, paddedIndexBytes, index32, idxOffset,
            elemCount, ortho, clipX, clipY, clipMaxX - clipX, clipMaxY - clipY);
      }
    }
  }

  /**
   * Returns the index bytes of one draw list sized to a multiple of four, which is what a GPU buffer write
   * requires. A 16-bit index list with an odd length gets two bytes of zero padding.
   *
   * <p>The limit of {@code indices} is changed; the caller restores it.
   */
  private ByteBuffer paddedIndices(ByteBuffer indices, int byteCount) {
    int padded = (byteCount + 3) & ~3;
    if (padded == byteCount) {
      indices.limit(indices.position() + byteCount);
      return indices;
    }
    if (scratch.capacity() < padded) {
      scratch = ByteBuffer.allocateDirect(padded * 2).order(ByteOrder.nativeOrder());
    }
    scratch.clear();
    indices.limit(indices.position() + byteCount);
    scratch.put(indices);
    while (scratch.position() < padded) {
      scratch.put((byte) 0);
    }
    scratch.flip();
    return scratch;
  }

  /** Destroys the font atlas texture. Safe to call more than once. */
  public void dispose() {
    if (fontTexture != null) {
      fontTexture.destroy();
      fontTexture = null;
    }
    initialized = false;
  }
}
