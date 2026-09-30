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
 * Tests the pure string transforms in {@link ShaderSources}.
 *
 * <p>These tests cover the GLSL 4.50 wrapping that feeds glslang (default vertex body, fixed
 * attribute slots, aliases, and the named {@code #line} directive) and the bgfx {@code .sc}
 * rewrites.
 */
class ShaderSourcesTest {

  @Test
  void glslVertexUsesDefaultBodyWhenNullOrBlank() {
    String fromNull = ShaderSources.glslVertex(null, "default.vert.glsl");
    String fromBlank = ShaderSources.glslVertex("   ", "default.vert.glsl");
    assertEquals(fromNull, fromBlank);
    assertTrue(fromNull.contains("gl_Position = mul(u_modelViewProj, vec4(a_position.xy, 0.0, 1.0))"));
  }

  @Test
  void glslVertexDeclaresLayoutAndAliases() {
    String result = ShaderSources.glslVertex("void main() {}", "wave.vert.glsl");
    assertTrue(result.startsWith("#version 450\n"));
    // Attribute slots must match the fixed slots the GL backends bind.
    assertTrue(result.contains("layout(location = 0) in vec2 a_position;"));
    assertTrue(result.contains("layout(location = 1) in vec2 a_texCoord0;"));
    assertTrue(result.contains("layout(location = 2) in vec4 a_color;"));
    assertTrue(result.contains("uniform mat4 u_projTrans;"));
    assertTrue(result.contains("#define a_texcoord0 a_texCoord0"));
    assertTrue(result.contains("#define a_color0 a_color"));
    assertTrue(result.contains("#define u_modelViewProj u_projTrans"));
    assertTrue(result.contains("#define mul(a, b) ((a) * (b))"));
  }

  @Test
  void glslFragmentPutsBodyAfterNamedLineDirective() {
    String body = "void main() {\n  gl_FragColor = v_color * flixel_texture(v_texCoords);\n}";
    String result = ShaderSources.glslFragment(body, "effects\\crt.frag.glsl");
    assertTrue(result.contains("#define gl_FragColor flixel_FragColor"));
    assertTrue(result.contains("#define texture2D texture"));
    assertTrue(result.contains("vec4 flixel_texture(vec2 uv)"));
    // Windows separators are normalized so the directive stays a valid string.
    assertTrue(result.endsWith("#line 1 \"effects/crt.frag.glsl\"\n" + body + "\n"));
  }

  @Test
  void promoteUniformsRewritesScalarsAndVectorsToVec4() {
    String src = "uniform float u_time;\n"
        + "uniform highp vec2 u_resolution;\n"
        + "uniform vec3 u_tint;\n"
        + "uniform int u_mode;\n"
        + "uniform vec4 u_color;\n"
        + "uniform mat4 u_matrix;\n";
    String result = ShaderSources.promoteUniforms(src);
    assertEquals("uniform vec4 u_time;\n#define u_time u_time.x\n"
        + "uniform vec4 u_resolution;\n#define u_resolution u_resolution.xy\n"
        + "uniform vec4 u_tint;\n#define u_tint u_tint.xyz\n"
        + "uniform vec4 u_mode;\n#define u_mode int(u_mode.x)\n"
        + "uniform vec4 u_color;\n"
        + "uniform mat4 u_matrix;\n", result);
  }

  @Test
  void bgfxStagesPromoteUniformsButGlslKeepsThem() {
    String body = "uniform float u_time;\nvoid main() {\n  gl_FragColor = vec4(u_time);\n}";
    assertTrue(ShaderSources.fragment(body).contains("#define u_time u_time.x"));
    assertTrue(ShaderSources.vertex(body).contains("#define u_time u_time.x"));
    assertTrue(ShaderSources.glslFragment(body, "a.frag.glsl").contains("uniform float u_time;"));
  }

  @Test
  void bgfxStagesRenameHlslOnlyKeywords() {
    String body = "void main() {\n  float line = 1.0;\n  gl_FragColor = vec4(line);\n}";
    assertTrue(ShaderSources.fragment(body).contains("#define line flx_line"));
    assertFalse(ShaderSources.fragment(body).contains("#define point"));
    // Only whole words count: "outline" must not trigger the rename.
    assertEquals("", ShaderSources.renameHlslKeywords("float outline = 1.0;"));
  }

  @Test
  void expandVectorConstructorsRewritesSingleArgumentCalls() {
    assertEquals("flx_vec3(gray)", ShaderSources.expandVectorConstructors("vec3(gray)"));
    assertEquals("vec2(0.0, 0.0)", ShaderSources.expandVectorConstructors("vec2(0.0)"));
    assertEquals("vec4(flx_vec3(a), 1.0)", ShaderSources.expandVectorConstructors("vec4(vec3(a), 1.0)"));
    assertEquals("vec2(max(a, b), 1.0)", ShaderSources.expandVectorConstructors("vec2(max(a, b), 1.0)"));
    assertEquals("const vec3 K = vec3(A, A, A);",
        ShaderSources.expandVectorConstructors("const vec3 K = vec3(A);"));
    // Names that merely end in "vec3" are not constructors.
    assertEquals("myvec3(x)", ShaderSources.expandVectorConstructors("myvec3(x)"));
  }

  @Test
  void bgfxStagesAddVectorHelpersOnlyWhenUsed() {
    String body = "void main() {\n  gl_FragColor = vec4(vec3(v_color.r), 1.0);\n}";
    String result = ShaderSources.fragment(body);
    assertTrue(result.contains("vec3 flx_vec3(float x)"));
    assertFalse(result.contains("flx_vec2(float x)"));
  }
}
