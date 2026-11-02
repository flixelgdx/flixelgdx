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

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gradle plugin that checks a game's WGSL shaders with Dawn at build time and bundles them as resources.
 *
 * <p>FlixelGDX shaders are written in WGSL only, and every platform compiles the same text at run time. The
 * plugin's job is to move the mistakes from run time to build time: it compiles each {@code .wgsl} file with the
 * exact compiler the game uses (Dawn), fails the build with {@code file:line:column} messages when something is
 * wrong, and copies the files to {@code build/generated/flixelgdx/shaders/shaders/<name>.wgsl}, a folder that is
 * added to the {@code main} resources. {@code FlixelShader.load(name)} then finds every shader on every platform.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * plugins {
 *   id 'java'
 *   id 'org.flixelgdx.shaders' version '0.1.0-beta'
 * }
 * }</pre>
 *
 * <p>Put shaders in {@code src/main/shaders} and load them by their path without the extension:
 *
 * <pre>{@code
 * FlixelShader wave = FlixelShader.load("wave");
 * }</pre>
 *
 * <h2>How validation runs</h2>
 *
 * <p>The check runs in a forked Java 25 process (Gradle's worker API with process isolation), because it calls
 * Dawn through Java's foreign function API. The jwebgpu libraries it needs live in a dedicated configuration of
 * this plugin, so they never reach the game's own class path.
 *
 * @see ShaderExtension
 * @see ProcessShadersTask
 */
public class ShaderPlugin implements Plugin<Project> {

  /** The name of the task that validates and copies the shaders. */
  public static final String TASK_NAME = "processShaders";

  /** The name of the configuration that holds the jwebgpu libraries of the validator. */
  public static final String CONFIGURATION_NAME = "shaderValidator";

  private static final String JWEBGPU_GROUP = "io.github.stringdotjar";

  @Override
  public void apply(Project project) {
    ShaderExtension ext = project.getExtensions().create(ShaderExtension.NAME, ShaderExtension.class);
    ext.getSourceDir().convention(project.getLayout().getProjectDirectory().dir(ShaderExtension.DEFAULT_SOURCE_DIR));
    ext.getValidate().convention(true);
    ext.getJwebgpuVersion().convention(bundledJwebgpuVersion());

    Configuration validator = project.getConfigurations().create(CONFIGURATION_NAME, config -> {
      config.setCanBeConsumed(false);
      config.setCanBeResolved(true);
      config.setDescription("The jwebgpu libraries that the shader validator runs with. Not part of the game.");
      config.defaultDependencies(deps -> {
        String version = ext.getJwebgpuVersion().getOrNull();
        String suffix = version == null || version.isEmpty() ? "" : ":" + version;
        deps.add(project.getDependencies().create(JWEBGPU_GROUP + ":jwebgpu-api" + suffix));
        deps.add(project.getDependencies().create(JWEBGPU_GROUP + ":jwebgpu-ffm" + suffix));
        deps.add(project.getDependencies()
            .create(JWEBGPU_GROUP + ":jwebgpu-natives-" + HostTarget.current() + suffix));
      });
    });

    JavaToolchainService toolchains = project.getExtensions().getByType(JavaToolchainService.class);
    Provider<JavaLauncher> launcher =
        toolchains.launcherFor(spec -> spec.getLanguageVersion().set(JavaLanguageVersion.of(25)));

    TaskProvider<ProcessShadersTask> process =
        project.getTasks().register(TASK_NAME, ProcessShadersTask.class, task -> {
          task.setGroup("shaders");
          task.setDescription("Validates the game's WGSL shaders with Dawn and copies them into the resources.");
          task.getSourceDir().convention(ext.getSourceDir());
          task.getSources()
              .from(ext.getSourceDir().map(dir -> project.fileTree(dir, tree -> tree.include("**/*.wgsl"))));
          task.getValidate().convention(ext.getValidate());
          task.getValidatorClasspath().from(
              project.provider(() -> ext.getValidate().get() ? validator : project.files()));
          task.getLauncher().convention(launcher);
          task.getOutputDir().convention(project.getLayout().getBuildDirectory().dir(ProcessShadersTask.OUTPUT_DIR));
        });

    // Bundle the shaders into the module's resources so they ship inside the game on every platform. The output
    // directory is registered as a provider of the task, so building the resources runs the task first.
    project.getPlugins().withId("java", plugin -> {
      SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
      Provider<Directory> out = process.flatMap(ProcessShadersTask::getOutputDir);
      sourceSets.getByName("main").getResources().srcDir(out);
    });
  }

  /**
   * Reads the jwebgpu version the framework was built with, which the plugin's build bundles as a resource.
   *
   * @return The version, for example {@code 0.1.0}.
   * @throws GradleException if the resource is missing, which means the plugin jar is broken.
   */
  private static String bundledJwebgpuVersion() {
    try (InputStream in = ShaderPlugin.class.getResourceAsStream("/org/flixelgdx/gradle/shader/jwebgpu.properties")) {
      if (in == null) {
        throw new GradleException("The FlixelGDX shader plugin is missing jwebgpu.properties.");
      }
      Properties props = new Properties();
      props.load(in);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new GradleException("Could not read the jwebgpu version bundled with the shader plugin.", e);
    }
  }
}
