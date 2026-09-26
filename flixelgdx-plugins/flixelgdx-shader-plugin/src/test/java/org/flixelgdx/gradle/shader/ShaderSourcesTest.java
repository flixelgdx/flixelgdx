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
 * <p>These tests cover the ESSL vertex wrapping logic: pass-through when no custom body is given,
 * alias injection when a custom body is present, and compatibility of the documented default body
 * with the ESSL preamble.
 */
class ShaderSourcesTest {

  @Test
  void esslVertexPassThroughWhenNull() {
    // No custom body: must emit the fixed pass-through, not the alias preamble.
    String result = ShaderSources.esslVertex(null);
    assertTrue(result.contains("attribute vec2 a_position;"));
    assertTrue(result.contains("attribute vec2 a_texCoord0;"));
    assertTrue(result.contains("attribute vec4 a_color;"));
    assertTrue(result.contains("uniform mat4 u_projTrans;"));
    assertTrue(result.contains("varying vec2 v_texCoords;"));
    assertTrue(result.contains("varying vec4 v_color;"));
    assertTrue(result.contains("u_projTrans * vec4(a_position, 0.0, 1.0)"));
    // Pass-through must not contain the alias defines; they belong only in the custom-body path.
    assertFalse(result.contains("#define a_texcoord0"));
    assertFalse(result.contains("#define mul(a, b)"));
  }

  @Test
  void esslVertexPassThroughWhenBlank() {
    // Blank string must behave identically to null.
    String resultNull = ShaderSources.esslVertex(null);
    String resultBlank = ShaderSources.esslVertex("   ");
    assertTrue(resultBlank.contains("u_projTrans * vec4(a_position, 0.0, 1.0)"));
    assertFalse(resultBlank.contains("#define a_texcoord0"));
    // Content equality ignoring leading/trailing whitespace differences.
    assertTrue(resultBlank.strip().equals(resultNull.strip()));
  }

  @Test
  void esslVertexCustomBodyIncludesAliases() {
    // A custom body must be wrapped with declarations and all four bgfx-to-ESSL aliases.
    String body = "void main() {\n"
        + "  gl_Position = mul(u_modelViewProj, vec4(a_position.xy, 0.0, 1.0));\n"
        + "  v_texCoords = a_texcoord0;\n"
        + "  v_color = a_color0;\n"
        + "}";
    String result = ShaderSources.esslVertex(body);
    // Declarations must be present.
    assertTrue(result.contains("attribute vec2 a_position;"));
    assertTrue(result.contains("attribute vec2 a_texCoord0;"));
    assertTrue(result.contains("attribute vec4 a_color;"));
    assertTrue(result.contains("uniform mat4 u_projTrans;"));
    assertTrue(result.contains("varying vec2 v_texCoords;"));
    assertTrue(result.contains("varying vec4 v_color;"));
    // All four aliases must be present.
    assertTrue(result.contains("#define a_texcoord0 a_texCoord0"));
    assertTrue(result.contains("#define a_color0 a_color"));
    assertTrue(result.contains("#define u_modelViewProj u_projTrans"));
    assertTrue(result.contains("#define mul(a, b) ((a) * (b))"));
    // The body itself must be preserved.
    assertTrue(result.contains(body.strip()));
  }

  @Test
  void esslVertexDefaultBodyCoveredByAliases() {
    // The documented DEFAULT_VERTEX body uses all four bgfx names; wrapping it must inject aliases
    // for each one, so the body would compile on ESSL without modification.
    String result = ShaderSources.esslVertex(ShaderSources.DEFAULT_VERTEX);
    // All bgfx names used by DEFAULT_VERTEX must be aliased.
    assertTrue(result.contains("#define u_modelViewProj u_projTrans"),
        "u_modelViewProj alias missing from ESSL vertex wrapper");
    assertTrue(result.contains("#define a_texcoord0 a_texCoord0"),
        "a_texcoord0 alias missing from ESSL vertex wrapper");
    assertTrue(result.contains("#define a_color0 a_color"),
        "a_color0 alias missing from ESSL vertex wrapper");
    assertTrue(result.contains("#define mul(a, b) ((a) * (b))"),
        "mul(a, b) alias missing from ESSL vertex wrapper");
    // The DEFAULT_VERTEX body lines must appear in the output.
    assertTrue(result.contains("gl_Position = mul(u_modelViewProj, vec4(a_position.xy, 0.0, 1.0))"),
        "DEFAULT_VERTEX body line missing from wrapped ESSL vertex");
    assertTrue(result.contains("v_texCoords = a_texcoord0"),
        "DEFAULT_VERTEX body line missing from wrapped ESSL vertex");
    assertTrue(result.contains("v_color = a_color0"),
        "DEFAULT_VERTEX body line missing from wrapped ESSL vertex");
  }

  @Test
  void esslFragmentContainsPreambleAndBody() {
    // Fragment wrapper must inject the ESSL preamble and preserve the developer body.
    String body = "void main() {\n  gl_FragColor = v_color * flixel_texture(v_texCoords);\n}";
    String result = ShaderSources.esslFragment(body);
    assertTrue(result.contains("uniform sampler2D u_texture;"));
    assertTrue(result.contains("varying vec2 v_texCoords;"));
    assertTrue(result.contains("varying vec4 v_color;"));
    assertTrue(result.contains("vec4 flixel_texture(vec2 uv)"));
    assertTrue(result.contains(body.strip()));
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
  void bgfxStagesPromoteUniformsButEsslKeepsThem() {
    String body = "uniform float u_time;\nvoid main() {\n  gl_FragColor = vec4(u_time);\n}";
    assertTrue(ShaderSources.fragment(body).contains("#define u_time u_time.x"));
    assertTrue(ShaderSources.vertex(body).contains("#define u_time u_time.x"));
    assertTrue(ShaderSources.esslFragment(body).contains("uniform float u_time;"));
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
