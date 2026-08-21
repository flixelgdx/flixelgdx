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

import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;

/**
 * An off-screen surface on the web, backed by a WebGL framebuffer with a color texture attachment.
 *
 * <p>This is what makes camera and global (scene-wide) shaders work on the web. The framework draws
 * a camera or the whole scene into one of these instead of the screen, then draws the target's
 * texture back through a shader. Between {@link #begin()} and {@link #end()} everything the batch
 * draws lands on this framebuffer; the graphics manager keeps a stack of active targets so they nest
 * correctly (a camera target inside the whole-scene target), restoring the previous surface on
 * {@link #end()}.
 *
 * <p>Like every OpenGL-family backend, a framebuffer's texture is stored bottom-up rather than
 * top-down, so {@link #isFlipped()} reports {@code true} and the framework's composite passes flip
 * the vertical texture coordinate when they draw it back.
 */
public class FlixelWebGlRenderTarget implements FlixelRenderTarget {

  @NotNull
  private final FlixelHtml5Graphics graphics;

  @NotNull
  private final WebGLRenderingContext gl;

  @NotNull
  private final FlixelWebGlTexture texture;

  private final int width;
  private final int height;

  private WebGLFramebuffer framebuffer;

  /**
   * Creates the framebuffer and its color texture, leaving the previously bound surface active.
   *
   * @param graphics The web graphics manager that owns the render-target stack.
   * @param gl The rendering context.
   * @param width Target width in pixels.
   * @param height Target height in pixels.
   */
  FlixelWebGlRenderTarget(@NotNull FlixelHtml5Graphics graphics, @NotNull WebGLRenderingContext gl,
      int width, int height) {
    this.graphics = graphics;
    this.gl = gl;
    this.width = width;
    this.height = height;
    this.texture = new FlixelWebGlTexture(gl, width, height, true);
    this.framebuffer = gl.createFramebuffer();

    gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
    gl.framebufferTexture2D(WebGLRenderingContext.FRAMEBUFFER, WebGLRenderingContext.COLOR_ATTACHMENT0,
        WebGLRenderingContext.TEXTURE_2D, texture.getGlTexture(), 0);
    // Binding the new framebuffer above left it active; hand control back to whatever surface was
    // current so merely creating a target never steals drawing away from it.
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
    if (framebuffer != null) {
      gl.deleteFramebuffer(framebuffer);
      framebuffer = null;
    }
    texture.destroy();
  }

  /**
   * Returns the WebGL framebuffer so the graphics manager can bind this target.
   *
   * @return The framebuffer object.
   */
  WebGLFramebuffer getFramebuffer() {
    return framebuffer;
  }
}
