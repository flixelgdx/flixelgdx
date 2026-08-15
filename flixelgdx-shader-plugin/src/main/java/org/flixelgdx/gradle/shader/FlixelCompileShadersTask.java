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

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Cross-compiles every configured shader into all backend variants and writes them into a
 * generated resources directory the game module bundles.
 *
 * <p>For each shader the task assembles the bgfx {@code .sc} sources from the developer's plain
 * GLSL (see {@link ShaderSources}), then runs {@code shaderc} once per stage per
 * {@link ShaderTarget}. The compiled bytecode lands at
 * {@code shaders/<name>/<variant>/vs.bin} and {@code fs.bin}, matching the layout the runtime
 * reads. The Direct3D variant needs Microsoft's FXC compiler (native on Windows, or via the
 * {@code d3d4linux} Wine shim elsewhere); when it is unavailable that one variant is skipped with a
 * warning, exactly as the framework's own shader build does, while every other variant still
 * compiles.
 */
public abstract class FlixelCompileShadersTask extends DefaultTask {

  /** The directory relative shader source paths are resolved against. */
  @Internal
  public abstract DirectoryProperty getSourceDir();

  /** An explicit {@code shaderc} path, or unset to use the bundled or {@code PATH} compiler. */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.NONE)
  public abstract RegularFileProperty getShadercPath();

  /** Maps each shader name to its fragment source path (relative to the source directory). */
  @Input
  public abstract MapProperty<String, String> getFragmentSources();

  /** Maps each shader name to its optional vertex source path (relative to the source directory). */
  @Input
  public abstract MapProperty<String, String> getVertexSources();

  /** Every resolved source file, tracked so edits re-run the compile. */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getSourceFiles();

  /** The generated resources directory the compiled variants are written into. */
  @OutputDirectory
  public abstract DirectoryProperty getGeneratedResourcesDir();

  /** A scratch directory for the assembled {@code .sc} sources and the extracted compiler. */
  @Internal
  public abstract DirectoryProperty getWorkDir();

  /**
   * Assembles and compiles every configured shader.
   *
   * @throws IOException When a source cannot be read or an output cannot be written.
   */
  @TaskAction
  public void compile() throws IOException {
    Map<String, String> fragments = getFragmentSources().get();
    if (fragments.isEmpty()) {
      getLogger().info("[FlixelGDX] No shaders declared in the flixelShaders block; nothing to compile.");
      return;
    }

    Map<String, String> vertices = getVertexSources().get();
    File sourceDir = getSourceDir().get().getAsFile();
    File outputRoot = getGeneratedResourcesDir().get().getAsFile();
    File workDir = getWorkDir().get().getAsFile();
    File shadersOut = new File(outputRoot, "shaders");
    deleteRecursively(shadersOut);

    File override = getShadercPath().isPresent() ? getShadercPath().getAsFile().get() : null;
    Shaderc shaderc = Shaderc.prepare(workDir, override);

    File varyingFile = new File(workDir, "varying.def.sc");
    Files.writeString(varyingFile.toPath(), ShaderSources.varyingDef(), StandardCharsets.UTF_8);

    for (Map.Entry<String, String> entry : fragments.entrySet()) {
      String name = entry.getKey();
      String fragmentGlsl = Files.readString(resolve(sourceDir, entry.getValue()).toPath(), StandardCharsets.UTF_8);
      String vertexPath = vertices.get(name);
      String vertexGlsl = vertexPath == null
          ? null
          : Files.readString(resolve(sourceDir, vertexPath).toPath(), StandardCharsets.UTF_8);

      File shaderWork = new File(workDir, name);
      Files.createDirectories(shaderWork.toPath());
      File vsSc = new File(shaderWork, "vs.sc");
      File fsSc = new File(shaderWork, "fs.sc");
      Files.writeString(vsSc.toPath(), ShaderSources.vertex(vertexGlsl), StandardCharsets.UTF_8);
      Files.writeString(fsSc.toPath(), ShaderSources.fragment(fragmentGlsl), StandardCharsets.UTF_8);

      for (ShaderTarget target : ShaderTarget.values()) {
        File variantDir = new File(shadersOut, name + "/" + target.dir());
        compileStage(shaderc, vsSc, varyingFile, new File(variantDir, "vs.bin"), "vertex", target, name);
        compileStage(shaderc, fsSc, varyingFile, new File(variantDir, "fs.bin"), "fragment", target, name);
      }
      getLogger().lifecycle("[FlixelGDX] Compiled shader '{}'.", name);
    }
  }

  private void compileStage(Shaderc shaderc, File sc, File varying, File out, String type,
      ShaderTarget target, String name) throws IOException {
    Shaderc.Result result = shaderc.compile(sc, varying, out, type, target);
    if (result.success()) {
      return;
    }
    String message = "[FlixelGDX] Failed to compile the " + type + " stage of shader '" + name
        + "' for the " + target.dir() + " backend:\n" + result.log();
    if (target.hostLimited()) {
      getLogger().warn("{}\n[FlixelGDX] Skipping the {} variant on this host, since Microsoft's FXC "
          + "compiler could not run. The bundled d3d4linux shim needs Wine installed on Linux and "
          + "macOS; install Wine, or produce this variant on a Windows CI runner, so the shader works "
          + "with the Direct3D renderer. Every other variant was still compiled.",
          message, target.dir());
      return;
    }
    throw new GradleException(message);
  }

  private static File resolve(File sourceDir, String path) {
    File file = new File(path);
    return file.isAbsolute() ? file : new File(sourceDir, path);
  }

  private static void deleteRecursively(File file) throws IOException {
    if (!file.exists()) {
      return;
    }
    try (var stream = Files.walk(file.toPath())) {
      stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new RuntimeException("Could not clean stale shader output: " + p, e);
        }
      });
    }
  }
}
