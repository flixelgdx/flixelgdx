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
package org.flixelgdx.gradle.shader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link ShaderSources} wraps developer GLSL in the bgfx {@code .sc} contract the
 * compiler expects.
 */
class ShaderSourcesTest {

  @Test
  void fragmentInjectsInputAndSamplerAndAliases() {
    String user = "void main() { gl_FragColor = flixel_texture(v_texCoords) * v_color; }";
    String out = ShaderSources.fragment(user);

    assertTrue(out.contains("$input v_texcoord0, v_color0"), "fragment must declare its varyings");
    assertTrue(out.contains("#include <bgfx_shader.sh>"), "fragment must include the bgfx preamble");
    assertTrue(out.contains("SAMPLER2D(s_texture, 0)"), "sampler must be bound at the stage the batch uses");
    assertTrue(out.contains("#define u_texture s_texture"), "friendly sampler alias must be present");
    assertTrue(out.contains("#define flixel_texture(_uv) texture2D(s_texture, _uv)"), "texture helper alias missing");
    assertTrue(out.contains(user), "the developer's body must be preserved verbatim");
  }

  @Test
  void vertexUsesDefaultWhenSourceIsMissing() {
    String out = ShaderSources.vertex(null);

    assertTrue(out.contains("$input a_position, a_texcoord0, a_color0"), "vertex must declare attributes");
    assertTrue(out.contains("$output v_texcoord0, v_color0"), "vertex must declare outputs");
    assertTrue(out.contains("u_modelViewProj"), "default vertex must apply the view-projection matrix");
    assertEquals(ShaderSources.vertex(""), out, "blank source must behave like a missing one");
  }

  @Test
  void vertexUsesCustomSourceWhenProvided() {
    String custom = "void main() { gl_Position = mul(u_modelViewProj, vec4(a_position.xy, 0.5, 1.0));"
        + " v_texCoords = a_texcoord0; v_color = a_color0; }";
    String out = ShaderSources.vertex(custom);

    assertTrue(out.contains(custom), "custom vertex body must be used");
    assertFalse(out.contains("vec4(a_position.xy, 0.0, 1.0)"), "default body must not leak in");
  }

  @Test
  void varyingDefDeclaresTheSharedLayout() {
    String out = ShaderSources.varyingDef();

    assertTrue(out.contains("a_position  : POSITION"), "position attribute must be declared");
    assertTrue(out.contains("v_color0    : COLOR0"), "color varying must be declared");
  }
}
