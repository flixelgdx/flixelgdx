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
package org.flixelgdx.backend.android.video;

import android.opengl.GLES11Ext;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Copies a decoded video frame out of an external OES texture into an ordinary
 * framebuffer texture.
 *
 * <p>Android hands MediaPlayer frames to a {@link android.graphics.SurfaceTexture
 * SurfaceTexture}, whose backing texture is a {@code GL_TEXTURE_EXTERNAL_OES} target.
 * libGDX's batch cannot sample that target: its shader uses a normal {@code sampler2D}.
 * This blitter runs a tiny full-screen pass that samples the OES texture through a
 * {@code samplerExternalOES} and writes the result into a plain {@code GL_TEXTURE_2D}
 * framebuffer. The framework then draws that framebuffer texture like any other object,
 * so a video obeys state draw order exactly like a sprite.
 *
 * <p>The pass also applies the {@code SurfaceTexture} transform matrix, which crops the
 * decoder's padding and maps the picture into the framebuffer. The vertical flip below
 * lines the result up with the top-left texture orientation the framework draws with:
 * the bottom row of the framebuffer receives the top row of the video, so
 * {@code FlixelVideo} shows the frame upright without a per-platform flip.
 */
final class FlixelOesBlitter {

  private final ShaderProgram shader;
  private final Mesh mesh;

  /**
   * Compiles the OES blit shader and builds the full-screen quad.
   *
   * @throws IllegalStateException If the shader fails to compile.
   */
  FlixelOesBlitter() {
    shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    if (!shader.isCompiled()) {
      throw new IllegalStateException("FlixelGDX video OES blit shader failed: " + shader.getLog());
    }
    mesh = new Mesh(true, 4, 0,
        new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
        new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
    // Interleaved (x, y, u, v). The texture coordinates are flipped vertically relative to
    // a straight blit (bottom vertices carry v = 1) so the framebuffer's bottom row holds
    // the video's top row; see the class comment for why that produces an upright frame.
    mesh.setVertices(new float[] {
        -1f, -1f, 0f, 1f,
        1f, -1f, 1f, 1f,
        -1f, 1f, 0f, 0f,
        1f, 1f, 1f, 0f
    });
  }

  /**
   * Draws the current OES frame into the given framebuffer.
   *
   * <p>Must be called on the render thread with a current GL context.
   *
   * @param oesTextureId The external OES texture the SurfaceTexture updated.
   * @param stMatrix The 4x4 SurfaceTexture transform matrix for this frame.
   * @param target The framebuffer to render the frame into.
   */
  void blit(int oesTextureId, float[] stMatrix, FrameBuffer target) {
    target.begin();
    Gdx.gl.glDisable(GL20.GL_BLEND);
    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    Gdx.gl.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId);
    shader.bind();
    shader.setUniformi("u_texture", 0);
    shader.setUniformMatrix4fv("u_stMatrix", stMatrix, 0, 16);
    mesh.render(shader, GL20.GL_TRIANGLE_STRIP);
    Gdx.gl.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    target.end();
  }

  /** Releases the shader and quad. Must run on the render thread. */
  void dispose() {
    mesh.dispose();
    shader.dispose();
  }

  private static final String VERTEX_SHADER =
      "attribute vec2 a_position;\n"
          + "attribute vec2 a_texCoord0;\n"
          + "uniform mat4 u_stMatrix;\n"
          + "varying vec2 v_texCoord;\n"
          + "void main() {\n"
          + "  gl_Position = vec4(a_position, 0.0, 1.0);\n"
          + "  v_texCoord = (u_stMatrix * vec4(a_texCoord0, 0.0, 1.0)).xy;\n"
          + "}\n";

  // The extension directive must be the first line for the samplerExternalOES type to
  // exist, so this string is fed to the shader compiler exactly as written.
  private static final String FRAGMENT_SHADER =
      "#extension GL_OES_EGL_image_external : require\n"
          + "precision mediump float;\n"
          + "varying vec2 v_texCoord;\n"
          + "uniform samplerExternalOES u_texture;\n"
          + "void main() {\n"
          + "  gl_FragColor = texture2D(u_texture, v_texCoord);\n"
          + "}\n";
}
