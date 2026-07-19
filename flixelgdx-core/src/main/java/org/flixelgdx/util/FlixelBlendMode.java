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
package org.flixelgdx.util;

import org.flixelgdx.FlixelSprite;

/**
 * Enum for different {@link FlixelSprite} blend modes.
 *
 * <p>Using any blend mode other than {@link #NORMAL} will send an extra flush to the GPU, so it's a
 * good idea to be mindful of what sprites to apply blending to.
 *
 * <p>Layering also matters. Most blend types are usually affected when something is above it, so keep
 * that in mind when using this.
 *
 * @author nebulastellanova
 */
public enum FlixelBlendMode {

  /** Normal drawing, no special blending. The default for every sprite. */
  NORMAL,

  /**
   * Adds this sprite's color to whatever is already drawn underneath, brightening it.
   * Good for glows, fire, and light effects.
   */
  ADD,

  /** Multiplies this sprite's color with what's underneath, darkening it. Good for shadows and tinting. */
  MULTIPLY,

  /**
   * Lightens what's underneath based on this sprite's color, the opposite of {@link #MULTIPLY}. Good for lighting
   * without fully blowing out to white like {@link #ADD} can. */
  SCREEN,

  /** Subtracts this sprite's color from whatever is underneath, darkening it. Good for smoke or ink effects. */
  SUBTRACT,

  /**
   * Keeps whichever is brighter, this sprite's color or what's underneath, pixel by pixel. Good for particles
   * and highlights that shouldn't over-brighten on overlap.
   */
  LIGHTEN,

  /**
   * Keeps whichever is darker, this sprite's color or what's underneath, pixel by pixel. The opposite of LIGHTEN.
   * Good for shading and vignette-style overlays.
   */
  DARKEN;

  public static final String BLEND_VERTEX_SHADER = """
      attribute vec4 a_position;
      attribute vec4 a_color;
      attribute vec2 a_texCoord0;
      uniform mat4 u_projTrans;
      varying vec4 v_color;
      varying vec2 v_texCoords;
      void main() {
          v_color = a_color;
          v_color.a = v_color.a * (255.0/254.0);
          v_texCoords = a_texCoord0;
          gl_Position = u_projTrans * a_position;
      }""";

  public static final String PREMULTIPLIED_FRAGMENT_SHADER = """
      #ifdef GL_ES
      precision mediump float;
      #endif
      varying vec4 v_color;
      varying vec2 v_texCoords;
      uniform sampler2D u_texture;
      void main() {
          vec4 c = v_color * texture2D(u_texture, v_texCoords);
          gl_FragColor = vec4(c.rgb * c.a, c.a);
      }""";

  public static final String WHITE_MIX_FRAGMENT_SHADER = """
      #ifdef GL_ES
      precision mediump float;
      #endif
      varying vec4 v_color;
      varying vec2 v_texCoords;
      uniform sampler2D u_texture;
      void main() {
          vec4 c = v_color * texture2D(u_texture, v_texCoords);
          vec3 result = mix(vec3(1.0), c.rgb, c.a);
          gl_FragColor = vec4(result, c.a);
      }""";
}
