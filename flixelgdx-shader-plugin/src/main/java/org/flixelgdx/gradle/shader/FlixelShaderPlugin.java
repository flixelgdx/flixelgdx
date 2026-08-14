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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;

/**
 * Gradle plugin that cross-compiles a game's GLSL shaders into every FlixelGDX backend variant at
 * build time.
 *
 * <p>A developer writes one plain GLSL fragment shader (and, rarely, a vertex shader). The plugin
 * drives bgfx's {@code shaderc} to produce the OpenGL, Vulkan, Metal, and Direct3D bytecode the
 * runtime loads, so no game code ever has to be written twice or compiled by hand. The compiled
 * variants are added to the module's resources automatically, so they ship inside the game.
 *
 * <h2>Usage</h2>
 *
 * <p>Apply the plugin and declare shaders in the game module's {@code build.gradle}:
 *
 * <pre>{@code
 * plugins {
 *   id 'java'
 *   id 'org.flixelgdx.shaders' version '0.1.0-beta'
 * }
 *
 * flixelShaders {
 *   shader('crt') {
 *     fragment = 'crt.frag.glsl'   // under src/main/shaders by default
 *   }
 * }
 * }</pre>
 *
 * <p>Load the result at runtime by the same name:
 *
 * <pre>{@code
 * FlixelShader crt = FlixelShader.load("crt");
 * Flixel.cameras.first().setShader(crt);
 * }</pre>
 *
 * @see FlixelShaderExtension
 * @see FlixelCompileShadersTask
 */
public class FlixelShaderPlugin implements Plugin<Project> {

  private static final String TASK_GROUP = "flixelgdx";
  private static final String TASK_NAME = "compileFlixelShaders";

  @Override
  public void apply(Project project) {
    FlixelShaderExtension ext =
        project.getExtensions().create(FlixelShaderExtension.NAME, FlixelShaderExtension.class);
    ext.getSourceDir()
        .convention(project.getLayout().getProjectDirectory().dir(FlixelShaderExtension.DEFAULT_SOURCE_DIR));

    TaskProvider<FlixelCompileShadersTask> compile =
        project.getTasks().register(TASK_NAME, FlixelCompileShadersTask.class, task -> {
          task.setGroup(TASK_GROUP);
          task.setDescription("Cross-compiles the game's GLSL shaders into every FlixelGDX backend variant.");
          task.getSourceDir().convention(ext.getSourceDir());
          task.getShadercPath().convention(ext.getShadercPath());
          task.getGeneratedResourcesDir()
              .convention(project.getLayout().getBuildDirectory().dir("generated/flixelShaders/resources"));
          task.getWorkDir().convention(project.getLayout().getBuildDirectory().dir("tmp/flixelShaders"));
        });

    // Populate the task's inputs from the DSL once the build script has been evaluated, since the
    // shader container is filled during evaluation.
    project.afterEvaluate(p -> compile.configure(task -> {
      File sourceDir = ext.getSourceDir().get().getAsFile();
      ext.getShaders().forEach(spec -> {
        if (!spec.getFragment().isPresent()) {
          throw new IllegalStateException(
              "FlixelGDX: shader '" + spec.getName() + "' is missing a required 'fragment' source.");
        }
        String fragment = spec.getFragment().get();
        task.getFragmentSources().put(spec.getName(), fragment);
        task.getSourceFiles().from(resolve(sourceDir, fragment));
        if (spec.getVertex().isPresent()) {
          String vertex = spec.getVertex().get();
          task.getVertexSources().put(spec.getName(), vertex);
          task.getSourceFiles().from(resolve(sourceDir, vertex));
        }
      });
    }));

    // Bundle the compiled variants into the module's resources so they ship inside the game.
    project.getPlugins().withId("java", plugin -> {
      Provider<File> generated =
          compile.flatMap(FlixelCompileShadersTask::getGeneratedResourcesDir).map(d -> d.getAsFile());
      SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
      sourceSets.getByName("main").getResources().srcDir(generated);
      project.getTasks().withType(ProcessResources.class).configureEach(task -> task.dependsOn(compile));
    });
  }

  private static File resolve(File sourceDir, String path) {
    File file = new File(path);
    return file.isAbsolute() ? file : new File(sourceDir, path);
  }
}
