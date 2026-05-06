/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.logging;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Registers a finalize step after each {@link JavaCompile} task that rewrites
 * {@link me.stringdotjar.flixelgdx.logging.FlixelLogger} invocations to {@code *WithSite} overloads.
 */
public class FlixelLoggingPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    FlixelLoggingExtension ext = project.getExtensions().create("flixelLogging", FlixelLoggingExtension.class);
    project.getPlugins().withType(JavaPlugin.class, p -> wire(project, ext));
  }

  private static void wire(Project project, FlixelLoggingExtension ext) {
    project.afterEvaluate(pr -> {
      if (!ext.getEnabled().get()) {
        return;
      }
      pr.getTasks().withType(JavaCompile.class).configureEach(compile ->
        compile.doLast(task -> {
          Path root = compile.getDestinationDirectory().get().getAsFile().toPath();
          try {
            FlixelTransformLoggingTask.weaveDirectory(root, ext.getVerbose().get(), task.getLogger());
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }));
    });
  }
}
