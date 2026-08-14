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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the vendored {@code shaderc} end to end and checks that its output is the bgfx binary
 * container the runtime loads.
 *
 * <p>The test only runs where a bundled binary exists for the host (currently Linux x86_64). On
 * other hosts it is skipped rather than failing, since the Windows and macOS binaries are added
 * separately. Producing a real {@code .bin} and confirming its {@code VSH}/{@code FSH} magic proves
 * the whole authoring-to-bytecode path, not just the source assembly.
 */
class ShadercIntegrationTest {

  private static final String FRAGMENT =
      "void main() { gl_FragColor = flixel_texture(v_texCoords) * v_color; }";

  @Test
  void compilesToBgfxContainerForVulkanAndOpenGl(@TempDir File tmp) throws Exception {
    Assumptions.assumeTrue(bundledBinaryAvailable(), "No bundled shaderc for this host; skipping.");

    File work = new File(tmp, "work");
    Shaderc shaderc = Shaderc.prepare(work, null);

    File varying = new File(work, "varying.def.sc");
    Files.writeString(varying.toPath(), ShaderSources.varyingDef(), StandardCharsets.UTF_8);
    File vs = new File(work, "vs.sc");
    File fs = new File(work, "fs.sc");
    Files.writeString(vs.toPath(), ShaderSources.vertex(null), StandardCharsets.UTF_8);
    Files.writeString(fs.toPath(), ShaderSources.fragment(FRAGMENT), StandardCharsets.UTF_8);

    for (ShaderTarget target : new ShaderTarget[] { ShaderTarget.GLSL, ShaderTarget.SPIRV }) {
      File vsOut = new File(tmp, target.dir() + "/vs.bin");
      File fsOut = new File(tmp, target.dir() + "/fs.bin");
      Shaderc.Result vsr = shaderc.compile(vs, varying, vsOut, "vertex", target);
      Shaderc.Result fsr = shaderc.compile(fs, varying, fsOut, "fragment", target);

      assertTrue(vsr.success(), "vertex compile failed for " + target.dir() + ": " + vsr.log());
      assertTrue(fsr.success(), "fragment compile failed for " + target.dir() + ": " + fsr.log());
      assertMagic(vsOut, "VSH");
      assertMagic(fsOut, "FSH");
    }
  }

  private static void assertMagic(File bin, String magic) throws Exception {
    byte[] bytes = Files.readAllBytes(bin.toPath());
    assertTrue(bytes.length > 3, bin + " is empty");
    String actual = new String(bytes, 0, 3, StandardCharsets.US_ASCII);
    assertTrue(magic.equals(actual), bin + " should start with the bgfx magic " + magic + " but was " + actual);
  }

  private static boolean bundledBinaryAvailable() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean linuxX64 = os.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64"));
    return linuxX64
        && ShadercIntegrationTest.class.getResource(
            "/org/flixelgdx/gradle/shader/tools/linux-x86_64/shaderc") != null;
  }
}
