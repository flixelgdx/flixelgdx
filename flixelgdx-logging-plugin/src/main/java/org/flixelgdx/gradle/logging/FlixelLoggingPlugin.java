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
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Registers a finalize step after each {@link AbstractCompile} task (covers both
 * {@code compileJava} and {@code compileKotlin}) that rewrites {@code FlixelLogger}
 * invocations to {@code *WithSite} overloads so stack traces carry accurate file/line info.
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
    });
  }
}
