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
package org.flixelgdx.gradle.logging;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Registers a finalize step after each compile task that rewrites {@code FlixelLogger}
 * invocations to {@code *WithSite} overloads so stack traces carry accurate file/line info.
 *
 * <p>Both {@link AbstractCompile} tasks (Java) and Kotlin compile tasks are covered. Kotlin
 * compile tasks stopped extending {@link AbstractCompile} in KGP 2.x, so they are detected
 * by walking the class hierarchy and matched by name to avoid a compile-time KGP dependency.
 */
public class FlixelLoggingPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    FlixelLoggingExtension ext = project.getExtensions().create("flixelLogging", FlixelLoggingExtension.class);

    project.afterEvaluate(pr -> {
      if (!ext.getEnabled().get()) {
        return;
      }

      pr.getTasks().withType(AbstractCompile.class).configureEach(compile -> compile.doLast(task -> {
        Path root = compile.getDestinationDirectory().get().getAsFile().toPath();
        try {
          FlixelTransformLoggingTask.weaveDirectory(root, ext.getVerbose().get(), task.getLogger());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }));

      // KGP 2.x KotlinCompile doesn't extend AbstractCompile, so we hook it separately.
      // Skip tasks already covered by the AbstractCompile hook above to avoid double-weaving.
      pr.getTasks().configureEach(task -> {
        if (task instanceof AbstractCompile || !isKotlinCompileTask(task.getClass())) {
          return;
        }
        task.doLast(t -> {
          try {
            DirectoryProperty destDir = (DirectoryProperty) task.getClass()
                .getMethod("getDestinationDirectory")
                .invoke(task);
            Path root = destDir.get().getAsFile().toPath();
            FlixelTransformLoggingTask.weaveDirectory(root, ext.getVerbose().get(), t.getLogger());
          } catch (ReflectiveOperationException e) {
            task.getLogger().warn("Flixel logging: could not get destination directory for '{}'", task.getName(), e);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      });
    });
  }

  private static boolean isKotlinCompileTask(Class<?> cls) {
    while (cls != null && !Object.class.equals(cls)) {
      String name = cls.getName();
      if (name.equals("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")
          || name.equals("org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon")) {
        return true;
      }
      cls = cls.getSuperclass();
    }
    return false;
  }
}
