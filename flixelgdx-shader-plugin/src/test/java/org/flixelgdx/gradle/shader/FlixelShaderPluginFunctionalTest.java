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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Applies the plugin to a throwaway project and runs its task, proving the DSL, task wiring, and
 * resource output all work together.
 *
 * <p>The test only runs where a bundled {@code shaderc} exists for the host (currently Linux
 * x86_64); elsewhere it is skipped, since the actual compilation needs a real compiler.
 */
class FlixelShaderPluginFunctionalTest {

  @Test
  void compilesDeclaredShaderIntoResources(@TempDir File projectDir) throws Exception {
    Assumptions.assumeTrue(bundledBinaryAvailable(), "No bundled shaderc for this host; skipping.");

    write(new File(projectDir, "settings.gradle"), "rootProject.name = 'shader-smoke'\n");
    write(new File(projectDir, "build.gradle"), """
        plugins {
          id 'java'
          id 'org.flixelgdx.shaders'
        }

        flixelShaders {
          shader('tint') {
            fragment = 'tint.frag.glsl'
          }
        }
        """);
    write(new File(projectDir, "src/main/shaders/tint.frag.glsl"),
        "void main() { gl_FragColor = flixel_texture(v_texCoords) * v_color; }\n");

    BuildResult result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments("compileFlixelShaders", "--stacktrace")
        .build();

    assertTrue(result.task(":compileFlixelShaders").getOutcome() == TaskOutcome.SUCCESS,
        "compileFlixelShaders should succeed");

    File out = new File(projectDir, "build/generated/flixelShaders/resources/shaders/tint");
    for (String variant : new String[] { "glsl", "spirv", "metal" }) {
      assertTrue(new File(out, variant + "/vs.bin").isFile(), "missing " + variant + " vertex output");
      assertTrue(new File(out, variant + "/fs.bin").isFile(), "missing " + variant + " fragment output");
    }
  }

  private static void write(File file, String content) throws Exception {
    Files.createDirectories(file.getParentFile().toPath());
    Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
  }

  private static boolean bundledBinaryAvailable() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean linuxX64 = os.contains("linux") && (arch.contains("amd64") || arch.contains("x86_64"));
    return linuxX64
        && FlixelShaderPluginFunctionalTest.class.getResource(
            "/org/flixelgdx/gradle/shader/tools/linux-x86_64/shaderc") != null;
  }
}
