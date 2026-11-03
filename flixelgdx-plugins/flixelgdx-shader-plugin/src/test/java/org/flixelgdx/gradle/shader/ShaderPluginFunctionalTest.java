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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs real Gradle builds with the plugin applied, to check the whole path from a {@code .wgsl} file to the
 * resources folder.
 *
 * <p>The builds validate with Dawn, using the jwebgpu jars from Maven Central at the version the plugin bundles. On a
 * machine jwebgpu has no Dawn library for, the tests are skipped instead of failed.
 */
class ShaderPluginFunctionalTest {

  private static final String TINT = """
      struct Params {
        tint: vec4f,
        amount: f32,
      }
      @group(1) @binding(0) var<uniform> params: Params;

      @fragment
      fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
        let c = textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
        return vec4f(mix(c.rgb, params.tint.rgb, params.amount), c.a);
      }
      """;

  @TempDir
  Path dir;

  @BeforeAll
  static void checkHost() {
    // The validator needs a Dawn library for this machine; jwebgpu ships one for each supported desktop target.
    Assumptions.assumeTrue(isSupportedHost(), "jwebgpu has no Dawn library for this machine");
  }

  private static boolean isSupportedHost() {
    try {
      HostTarget.of(System.getProperty("os.name"), System.getProperty("os.arch"));
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }

  @Test
  void validShaderPassesAndLandsInResources() throws IOException {
    write("src/main/shaders/tint.wgsl", TINT);
    write("src/main/shaders/fx/plain.wgsl", """
        @fragment
        fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
          return textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
        }
        """);
    setUp();

    BuildResult result = runner("processResources").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":processShaders").getOutcome());
    Path out = dir.resolve("build/generated/flixelgdx/shaders/shaders");
    assertEquals(TINT, Files.readString(out.resolve("tint.wgsl")));
    assertTrue(Files.isRegularFile(out.resolve("fx/plain.wgsl")));
    assertTrue(Files.isRegularFile(dir.resolve("build/resources/main/shaders/tint.wgsl")));
    assertTrue(Files.isRegularFile(dir.resolve("build/resources/main/shaders/fx/plain.wgsl")));
  }

  @Test
  void syntaxErrorFailsWithTheLineOfTheUsersFile() throws IOException {
    write("src/main/shaders/broken.wgsl", """
        // A comment first, so the error is not on line 1.
        @fragment
        fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
          let x = 1.0
          return vec4f(x);
        }
        """);
    setUp();

    BuildResult result = runner("processShaders").buildAndFail();

    String output = result.getOutput();
    Path file = dir.resolve("src/main/shaders/broken.wgsl").toRealPath();
    // The missing semicolon is reported where the statement ends (line 4) or where the next token starts (line 5).
    assertTrue(output.contains(file + ":4:") || output.contains(file + ":5:"), output);
    assertTrue(output.contains("error:"), output);
    assertFalse(Files.exists(dir.resolve("build/generated/flixelgdx/shaders/shaders/broken.wgsl")), output);
  }

  @Test
  void wrongBindingFailsTheTrialPipeline() throws IOException {
    write("src/main/shaders/badbinding.wgsl", """
        @group(1) @binding(0) var extra: texture_2d<f32>;

        @fragment
        fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
          return textureSample(extra, flixelSampler, in.uv);
        }
        """);
    setUp();

    BuildResult result = runner("processShaders").buildAndFail();

    assertTrue(result.getOutput().contains("badbinding.wgsl"), result.getOutput());
    assertTrue(result.getOutput().contains("Found 1 shader error"), result.getOutput());
    assertTrue(result.getOutput().contains("doesn't match"), result.getOutput());
  }

  @Test
  void missingFragmentEntryFails() throws IOException {
    write("src/main/shaders/nofs.wgsl", """
        @fragment
        fn not_the_entry(in: FlixelVertexOutput) -> @location(0) vec4f {
          return in.color;
        }
        """);
    setUp();

    BuildResult result = runner("processShaders").buildAndFail();

    assertTrue(result.getOutput().contains("nofs.wgsl"), result.getOutput());
    assertTrue(result.getOutput().contains("fs_main"), result.getOutput());
  }

  @Test
  void customVertexEntryIsUsed() throws IOException {
    write("src/main/shaders/wobble.wgsl", """
        @vertex
        fn vs_main(in: FlixelVertexInput) -> FlixelVertexOutput {
          var out: FlixelVertexOutput;
          out.position = flixel.proj * vec4f(in.position + vec2f(1.0, 0.0), 0.0, 1.0);
          out.uv = in.uv;
          out.color = in.color;
          return out;
        }

        @fragment
        fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
          return in.color;
        }
        """);
    setUp();

    BuildResult result = runner("processShaders").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":processShaders").getOutcome());
  }

  @Test
  void secondRunIsUpToDateAndAChangeRunsAgain() throws IOException {
    write("src/main/shaders/tint.wgsl", TINT);
    setUp();

    assertEquals(TaskOutcome.SUCCESS, runner("processShaders").build().task(":processShaders").getOutcome());
    assertEquals(TaskOutcome.UP_TO_DATE, runner("processShaders").build().task(":processShaders").getOutcome());

    write("src/main/shaders/tint.wgsl", TINT.replace("amount", "strength"));
    assertEquals(TaskOutcome.SUCCESS, runner("processShaders").build().task(":processShaders").getOutcome());

    Files.delete(dir.resolve("src/main/shaders/tint.wgsl"));
    write("src/main/shaders/other.wgsl", TINT);
    assertEquals(TaskOutcome.SUCCESS, runner("processShaders").build().task(":processShaders").getOutcome());
    assertFalse(Files.exists(dir.resolve("build/generated/flixelgdx/shaders/shaders/tint.wgsl")));
    assertTrue(Files.exists(dir.resolve("build/generated/flixelgdx/shaders/shaders/other.wgsl")));
  }

  @Test
  void noShadersSkipsTheTask() throws IOException {
    setUp();

    BuildResult result = runner("processShaders").build();

    assertEquals(TaskOutcome.NO_SOURCE, result.task(":processShaders").getOutcome());
  }

  @Test
  void validationCanBeSwitchedOff() throws IOException {
    write("src/main/shaders/broken.wgsl", "this is not wgsl");
    setUp("shaders { validate = false }");

    BuildResult result = runner("processShaders").build();

    assertEquals(TaskOutcome.SUCCESS, result.task(":processShaders").getOutcome());
    assertTrue(Files.isRegularFile(dir.resolve("build/generated/flixelgdx/shaders/shaders/broken.wgsl")));
  }

  @Test
  void sourceDirectoryCanBeChanged() throws IOException {
    write("assets/fx/tint.wgsl", TINT);
    setUp("shaders { sourceDir = layout.projectDirectory.dir('assets/fx') }");

    runner("processShaders").build();

    assertTrue(Files.isRegularFile(dir.resolve("build/generated/flixelgdx/shaders/shaders/tint.wgsl")));
  }

  private void setUp() throws IOException {
    setUp("");
  }

  private void setUp(String extra) throws IOException {
    write("settings.gradle", "rootProject.name = 'game'\n");
    write("build.gradle", """
        plugins {
          id 'java'
          id 'org.flixelgdx.shaders'
        }
        repositories { mavenCentral() }
        """ + extra + "\n");
    StringBuilder props = new StringBuilder("org.gradle.java.installations.auto-download=false\n");
    String jdks = System.getProperty("flixel.test.jdkPaths");
    if (jdks != null) {
      props.append("org.gradle.java.installations.paths=").append(jdks.replace("\\", "/")).append('\n');
    }
    write("gradle.properties", props.toString());
  }

  private GradleRunner runner(String... tasks) {
    GradleRunner runner = GradleRunner.create()
        .withProjectDir(dir.toFile())
        .withPluginClasspath()
        .withArguments(tasks);
    String home = System.getProperty("flixel.test.gradleUserHome");
    if (home != null) {
      runner.withTestKitDir(Path.of(home).toFile());
    }
    return runner;
  }

  private void write(String relative, String text) throws IOException {
    Path file = dir.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, text, StandardCharsets.UTF_8);
  }
}
