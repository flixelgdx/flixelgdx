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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Cross-compiles every configured shader into all backend variants and writes them into a
 * generated resources directory the game module bundles.
 *
 * <p>Each shader goes through three steps, like a document that is proofread once and then
 * translated for each audience:
 * <ol>
 *   <li>{@link Glslang} compiles the developer's GLSL (wrapped by {@link ShaderSources}) into SPIR-V.
 *   Any mistake in the shader fails the build here, with the developer's own file name and line
 *   number.</li>
 *   <li>{@link SpirvCross} translates that SPIR-V into ESSL 3.00 for the web and Android backends,
 *   written to {@code shaders/<name>/essl/vs.glsl} and {@code fs.glsl}.</li>
 *   <li>bgfx's {@code shaderc} compiles the bgfx {@code .sc} sources once per stage per
 *   {@link ShaderTarget} for the desktop backend, written to {@code shaders/<name>/<variant>/vs.bin}
 *   and {@code fs.bin}.</li>
 * </ol>
 *
 * <p>The Direct3D variant needs Microsoft's FXC compiler (native on Windows, or via the
 * {@code d3d4linux} Wine shim elsewhere); when it is unavailable that one variant is skipped with a
 * warning, exactly as the framework's own shader build does, while every other variant still
 * compiles.
 */
public abstract class CompileShadersTask extends DefaultTask {

  /** The directory relative shader source paths are resolved against. */
  @Internal
  public abstract DirectoryProperty getSourceDir();

  /** An explicit {@code shaderc} path, or unset to use the bundled or {@code PATH} compiler. */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.NONE)
  public abstract RegularFileProperty getShadercPath();

  /** An explicit {@code glslang} path, or unset to use the bundled or {@code PATH} compiler. */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.NONE)
  public abstract RegularFileProperty getGlslangPath();

  /** An explicit {@code spirv-cross} path, or unset to use the bundled or {@code PATH} translator. */
  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.NONE)
  public abstract RegularFileProperty getSpirvCrossPath();

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

  /** A scratch directory for the assembled sources, the SPIR-V modules, and the extracted tools. */
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
      getLogger().info("[FlixelGDX] No shaders declared in the shaders block; nothing to compile.");
      return;
    }

    Map<String, String> vertices = getVertexSources().get();
    File sourceDir = getSourceDir().get().getAsFile();
    File outputRoot = getGeneratedResourcesDir().get().getAsFile();
    File workDir = getWorkDir().get().getAsFile();
    File shadersOut = new File(outputRoot, "shaders");
    deleteRecursively(shadersOut);
    writeNativeImageResourceConfig(outputRoot);

    Glslang glslang = Glslang.prepare(workDir, optionalFile(getGlslangPath()));
    SpirvCross spirvCross = SpirvCross.prepare(workDir, optionalFile(getSpirvCrossPath()));
    Shaderc shaderc = Shaderc.prepare(workDir, optionalFile(getShadercPath()));

    File varyingFile = new File(workDir, "varying.def.sc");
    Files.writeString(varyingFile.toPath(), ShaderSources.varyingDef(), StandardCharsets.UTF_8);

    for (Map.Entry<String, String> entry : fragments.entrySet()) {
      String name = entry.getKey();
      String fragmentPath = entry.getValue();
      String fragmentGlsl = Files.readString(resolve(sourceDir, fragmentPath).toPath(), StandardCharsets.UTF_8);
      String vertexPath = vertices.get(name);
      String vertexGlsl = vertexPath == null
          ? null
          : Files.readString(resolve(sourceDir, vertexPath).toPath(), StandardCharsets.UTF_8);

      File shaderWork = new File(workDir, name);
      Files.createDirectories(shaderWork.toPath());

      // Compile to SPIR-V first. This is where a broken shader fails, with a readable error.
      File vsGlsl = new File(shaderWork, "vs.vert");
      File fsGlsl = new File(shaderWork, "fs.frag");
      String vertexName = vertexPath == null ? "flixel-default.vert.glsl" : vertexPath;
      Files.writeString(vsGlsl.toPath(), ShaderSources.glslVertex(vertexGlsl, vertexName), StandardCharsets.UTF_8);
      Files.writeString(fsGlsl.toPath(), ShaderSources.glslFragment(fragmentGlsl, fragmentPath),
          StandardCharsets.UTF_8);
      File vsSpv = new File(shaderWork, "vs.spv");
      File fsSpv = new File(shaderWork, "fs.spv");
      compileSpirv(glslang, vsGlsl, "vert", vsSpv, "vertex", name);
      compileSpirv(glslang, fsGlsl, "frag", fsSpv, "fragment", name);

      // The web and Android backends compile ESSL at runtime rather than loading bgfx bytecode.
      File esslDir = new File(shadersOut, name + "/essl");
      translateEssl(spirvCross, vsSpv, new File(esslDir, "vs.glsl"), "vertex", name);
      translateEssl(spirvCross, fsSpv, new File(esslDir, "fs.glsl"), "fragment", name);

      // bgfx compiles from the developer's own source; see ShaderSources for why.
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

  /**
   * Writes a GraalVM {@code resource-config.json} into the generated resources directory so that
   * all compiled shader binaries are included in a native-image build automatically.
   *
   * <p>The single pattern {@code shaders/.*\\.bin} covers every variant the task emits
   * ({@code shaders/<name>/<variant>/vs.bin} and {@code fs.bin}), so this file does not need to
   * be regenerated per shader - one write per task execution is enough.
   *
   * @param outputRoot The generated resources directory the shader binaries are written into.
   * @throws IOException When the config file cannot be written.
   */
  private static void writeNativeImageResourceConfig(File outputRoot) throws IOException {
    File configDir = new File(outputRoot, "META-INF/native-image/org.flixelgdx/shaders");
    Files.createDirectories(configDir.toPath());
    String config = """
        {
          "resources": {
            "includes": [
              {"pattern": "shaders/.*\\\\.bin"}
            ]
          }
        }
        """;
    Files.writeString(new File(configDir, "resource-config.json").toPath(), config, StandardCharsets.UTF_8);
  }

  /**
   * Compiles one stage to SPIR-V, failing the build with glslang's diagnostics when the shader is
   * invalid.
   */
  private void compileSpirv(Glslang glslang, File source, String stage, File out, String type, String name)
      throws IOException {
    ToolResult result = glslang.compile(source, stage, out);
    String diagnostics = diagnostics(result.log());
    if (!result.success()) {
      throw new GradleException("[FlixelGDX] The " + type + " stage of shader '" + name
          + "' does not compile:\n" + (diagnostics.isEmpty() ? result.log() : diagnostics));
    }
    if (!diagnostics.isEmpty()) {
      getLogger().warn("[FlixelGDX] Warnings in the {} stage of shader '{}':\n{}", type, name, diagnostics);
    }
  }

  /**
   * Translates one SPIR-V stage into ESSL. A failure here is a tooling problem rather than a mistake
   * in the shader, since glslang has already accepted it.
   */
  private static void translateEssl(SpirvCross spirvCross, File spirv, File out, String type, String name)
      throws IOException {
    ToolResult result = spirvCross.toEssl(spirv, out);
    if (!result.success()) {
      throw new GradleException("[FlixelGDX] spirv-cross could not translate the " + type + " stage of shader '"
          + name + "' to ESSL:\n" + result.log());
    }
  }

  /**
   * Keeps only the {@code ERROR:} and {@code WARNING:} lines of a glslang log, dropping the echoed
   * file path and the trailing summary lines.
   */
  private static String diagnostics(String log) {
    StringBuilder out = new StringBuilder();
    for (String line : log.split("\\R")) {
      if (line.startsWith("ERROR:") || line.startsWith("WARNING:")) {
        out.append(out.length() == 0 ? "" : "\n").append(line);
      }
    }
    return out.toString();
  }

  private void compileStage(Shaderc shaderc, File sc, File varying, File out, String type,
      ShaderTarget target, String name) throws IOException {
    ToolResult result = shaderc.compile(sc, varying, out, type, target);
    if (result.success()) {
      return;
    }
    String message = "[FlixelGDX] Failed to compile the " + type + " stage of shader '" + name
        + "' for the " + target.dir() + " backend:\n" + result.log();
    // When FXC ran and reported an error (D3DCompile failed, or an error code like X3014), the
    // shader itself is at fault, so it fails the build like any other variant would.
    boolean compilerRan = result.log().contains("D3DCompile failed")
        || result.log().matches("(?s).*error X\\d+.*");
    if (target.hostLimited() && !compilerRan) {
      getLogger().warn("{}\n[FlixelGDX] Skipping the {} variant on this host, since Microsoft's FXC "
          + "compiler could not run. The bundled d3d4linux shim needs Wine installed on Linux and "
          + "macOS; install Wine, or produce this variant on a Windows CI runner, so the shader works "
          + "with the Direct3D renderer. Every other variant was still compiled.",
          message, target.dir());
      return;
    }
    throw new GradleException(message);
  }

  @Nullable
  private static File optionalFile(RegularFileProperty property) {
    return property.isPresent() ? property.getAsFile().get() : null;
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
