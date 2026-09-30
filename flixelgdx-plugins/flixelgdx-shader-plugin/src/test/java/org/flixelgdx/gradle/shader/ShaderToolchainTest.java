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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the bundled glslang and spirv-cross binaries end to end, the same way the compile task does.
 *
 * <p>The tests are skipped on a host the plugin bundles no binaries for.
 */
class ShaderToolchainTest {

  @TempDir
  static Path workDir;

  private static Glslang glslang;
  private static SpirvCross spirvCross;

  @BeforeAll
  static void prepareTools() {
    try {
      glslang = Glslang.prepare(workDir.toFile(), null);
      spirvCross = SpirvCross.prepare(workDir.toFile(), null);
    } catch (IOException e) {
      glslang = null;
      spirvCross = null;
    }
  }

  @Test
  void defaultVertexTranslatesToEsslWithRuntimeNames() throws IOException {
    String essl = toEssl("vert", ShaderSources.glslVertex(null, "default.vert.glsl"));
    assertTrue(essl.startsWith("#version 300 es"));
    assertTrue(essl.contains("layout(location = 0) in vec2 a_position;"));
    assertTrue(essl.contains("layout(location = 1) in vec2 a_texCoord0;"));
    assertTrue(essl.contains("layout(location = 2) in vec4 a_color;"));
    assertTrue(essl.contains("u_projTrans * vec4(a_position, 0.0, 1.0)"));
    assertTrue(essl.contains("out vec2 v_texCoords;"));
    assertTrue(essl.contains("out vec4 v_color;"));
    assertValidEssl(essl, "vert");
  }

  @Test
  void fragmentKeepsUniformNamesAndLegacySpellings() throws IOException {
    String body = """
        uniform float u_time, u_amount;
        void main() {
          vec4 base = texture2D(u_texture, v_texCoords);
          gl_FragColor = flixel_texture(v_texCoords) * v_color * u_amount + base * sin(u_time);
        }
        """;
    String essl = toEssl("frag", ShaderSources.glslFragment(body, "crt.frag.glsl"));
    assertTrue(essl.startsWith("#version 300 es"));
    // Uniforms stay separate and keep their names, so setUniform("u_time", ...) still finds them.
    assertTrue(essl.contains("uniform highp float u_time;"));
    assertTrue(essl.contains("uniform highp float u_amount;"));
    assertTrue(essl.contains("uniform highp sampler2D u_texture;"));
    assertTrue(essl.contains("in highp vec2 v_texCoords;"));
    assertValidEssl(essl, "frag");
  }

  @Test
  void brokenShaderReportsTheDevelopersFileAndLine() throws IOException {
    assumeTrue(glslang != null, "No bundled glslang for this host.");
    File source = workDir.resolve("broken.frag").toFile();
    String body = "void main() {\n  gl_FragColor = vec4(1.0)\n}";
    Files.writeString(source.toPath(), ShaderSources.glslFragment(body, "crt.frag.glsl"), StandardCharsets.UTF_8);
    ToolResult result = glslang.compile(source, "frag", workDir.resolve("broken.spv").toFile());
    assertFalse(result.success());
    assertTrue(result.log().contains("crt.frag.glsl:3"), result.log());
  }

  private static String toEssl(String stage, String glsl) throws IOException {
    assumeTrue(glslang != null && spirvCross != null, "No bundled shader tools for this host.");
    File source = workDir.resolve("in." + stage).toFile();
    File spirv = workDir.resolve(stage + ".spv").toFile();
    File essl = workDir.resolve(stage + ".essl").toFile();
    Files.writeString(source.toPath(), glsl, StandardCharsets.UTF_8);
    ToolResult compiled = glslang.compile(source, stage, spirv);
    assertTrue(compiled.success(), compiled.log());
    ToolResult translated = spirvCross.toEssl(spirv, essl);
    assertTrue(translated.success(), translated.log());
    return Files.readString(essl.toPath(), StandardCharsets.UTF_8);
  }

  /** Feeds generated ESSL back through glslang, which checks it against the ES 3.00 rules. */
  private static void assertValidEssl(String essl, String stage) throws IOException {
    File file = workDir.resolve("check." + stage).toFile();
    Files.writeString(file.toPath(), essl, StandardCharsets.UTF_8);
    File tool = workDir.resolve("bin").resolve(BundledTool.isWindows() ? "glslang.exe" : "glslang").toFile();
    Process process = new ProcessBuilder(List.of(tool.getAbsolutePath(), file.getAbsolutePath()))
        .redirectErrorStream(true).start();
    String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    try {
      assertTrue(process.waitFor() == 0, log);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
