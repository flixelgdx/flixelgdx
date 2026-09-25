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
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

/**
 * An off-screen surface backed by an OpenGL ES 3.0 framebuffer with a color texture attachment.
 *
 * <p>Between {@link #begin()} and {@link #end()}, all drawing goes into this framebuffer instead
 * of the screen. The graphics manager keeps a stack of active targets so they nest correctly
 * (a camera's target inside the whole-scene target), restoring the previous surface on
 * {@link #end()}.
 *
 * <p>Like all OpenGL framebuffers, the color texture is stored bottom-up, so
 * {@link #isFlipped()} returns {@code true} and composite passes must flip the vertical
 * texture coordinate when drawing this target back to the screen.
 */
class FlixelGlesRenderTarget implements FlixelRenderTarget {

  private final int width;
  private final int height;

  private final int framebuffer;
  private final FlixelGlesTexture texture;
  private final FlixelAndroidGraphics graphics;

  /**
   * Creates the framebuffer and its color texture attachment, then restores whatever surface
   * was active before creation.
   *
   * @param graphics The graphics manager that owns the render-target stack.
   * @param width Target width in pixels.
   * @param height Target height in pixels.
   * @param smooth {@code true} for linear filtering on the color texture.
   */
  FlixelGlesRenderTarget(@NotNull FlixelAndroidGraphics graphics, int width, int height,
      boolean smooth) {
    this.graphics = graphics;
    this.width = width;
    this.height = height;
    this.texture = new FlixelGlesTexture(width, height, smooth);

    int[] ids = new int[1];
    GLES30.glGenFramebuffers(1, ids, 0);
    framebuffer = ids[0];

    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer);
    GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
        GLES30.GL_TEXTURE_2D, texture.getGlTexture(), 0);

    // Restore whatever surface was active before this target was created, so construction
    // never steals drawing away from an existing render pass.
    graphics.restoreActiveFramebuffer();
  }

  @Override
  public void begin() {
    graphics.pushRenderTarget(this);
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

  @Override
  @NotNull
  public FlixelTexture getTexture() {
    return texture;
  }

  @Override
  public boolean isFlipped() {
    return true;
  }

  @Override
  public void destroy() {
    int[] ids = {framebuffer};
    GLES30.glDeleteFramebuffers(1, ids, 0);
    texture.destroy();
  }

  /**
   * Returns the GL framebuffer name so the graphics manager can bind this target.
   *
   * @return The GL framebuffer object name.
   */
  int getFramebuffer() {
    return framebuffer;
  }
}
