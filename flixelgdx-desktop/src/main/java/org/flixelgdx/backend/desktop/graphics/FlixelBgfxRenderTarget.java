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

import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.bgfx.BGFX;

/**
 * A bgfx off-screen surface, backed by a framebuffer with one RGBA color attachment.
 *
 * <p>bgfx renders through numbered views rather than an immediate-mode bind, so redirection is
 * done by asking the graphics manager for a fresh view bound to this framebuffer and telling the
 * batch to submit there. {@link #begin()} pushes that view; {@link #end()} pops back to the
 * previous one, which is what lets a per-camera target render inside a whole-scene target.
 */
public class FlixelBgfxRenderTarget implements FlixelRenderTarget {

  @NotNull
  private final FlixelBgfxGraphics graphics;

  @NotNull
  private final FlixelBgfxTexture texture;

  private final int width;
  private final int height;

  private short frameBuffer;

  FlixelBgfxRenderTarget(@NotNull FlixelBgfxGraphics graphics, int width, int height) {
    this.graphics = graphics;
    this.width = width;
    this.height = height;
    this.frameBuffer = BGFX.bgfx_create_frame_buffer(width, height, BGFX.BGFX_TEXTURE_FORMAT_RGBA8,
        BGFX.BGFX_TEXTURE_NONE);
    short colorTexture = BGFX.bgfx_get_texture(frameBuffer, 0);
    this.texture = new FlixelBgfxTexture(colorTexture, width, height);
  }

  @Override
  public void begin() {
    graphics.pushRenderTarget(frameBuffer, width, height);
  }

  @Override
  public void end() {
    graphics.popRenderTarget();
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @NotNull
  @Override
  public FlixelTexture getTexture() {
    return texture;
  }

  /** The bgfx framebuffer handle, so the graphics manager can bind extra views to this surface. */
  short getFrameBuffer() {
    return frameBuffer;
  }

  @Override
  public boolean isFlipped() {
    // bgfx render-target textures are stored top-down when the renderer's clip space is top-left
    // (D3D, Metal, Vulkan) and bottom-up on OpenGL. The framework's composite passes flip based on
    // this flag, so report the OpenGL case.
    return BGFX.bgfx_get_renderer_type() == BGFX.BGFX_RENDERER_TYPE_OPENGL;
  }

  @Override
  public void destroy() {
    if (frameBuffer != -1) {
      BGFX.bgfx_destroy_frame_buffer(frameBuffer);
      frameBuffer = -1;
    }
  }
}
