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

import org.flixelgdx.gradle.shader.worker.Contract;
import org.flixelgdx.gradle.shader.worker.Diagnostic;
import org.flixelgdx.gradle.shader.worker.DiagnosticReport;
import org.flixelgdx.gradle.shader.worker.ValidateAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutionException;
import org.gradle.workers.WorkerExecutor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

/**
 * Validates a game's {@code .wgsl} shaders with Dawn and copies them into a resource folder.
 *
 * <p>Think of it as a proofreader standing between the author and the printing press: every shader is read by the
 * same compiler the game will use, and only files that pass go on to be bundled.
 *
 * <p><strong>Output contract.</strong> A shader at {@code <sourceDir>/<name>.wgsl} is copied to
 * {@code <outputDir>/shaders/<name>.wgsl}, keeping subfolders. {@link #getOutputDir()} is the resource root
 * (default {@code build/generated/flixelgdx/shaders}), so it is added to the {@code main} resources and
 * {@code FlixelShader.load(name)} finds {@code shaders/<name>.wgsl}. Other plugins that need the shaders as files
 * (such as the HTML5 plugin, which serves them to the browser) should consume {@code outputDir} as a task output
 * instead of guessing a path.
 *
 * <p>The task is cacheable. It is up to date while the shaders, the settings and the plugin itself are unchanged,
 * and a failed validation never leaves stale shaders behind in the output folder.
 */
@CacheableTask
public abstract class ProcessShadersTask extends DefaultTask {

  /** The default output directory, relative to the build directory. */
  public static final String OUTPUT_DIR = "generated/flixelgdx/shaders";

  /** The folder inside the output directory that holds the shaders, as {@code FlixelShader.load(...)} expects. */
  public static final String SHADERS_FOLDER = "shaders";

  private static final String JAVA_ARGUMENT = "--enable-native-access=ALL-UNNAMED";

  /** Validates every shader, then copies them to the output directory. */
  @TaskAction
  public void process() {
    File sourceDir = getSourceDir().get().getAsFile();
    File outputDir = getOutputDir().get().getAsFile();

    // Remove the previous output first, so a failed run cannot leave an older, passing copy in the resources.
    getFileSystem().delete(spec -> spec.delete(outputDir));

    Path root = sourceDir.toPath();
    List<String> names = new ArrayList<>();
    for (File file : getSources().getFiles()) {
      names.add(root.relativize(file.toPath()).toString().replace(File.separatorChar, '/'));
    }
    Collections.sort(names);

    if (getValidate().get()) {
      validate(sourceDir, names);
    }

    getFileSystem().sync(spec -> {
      spec.from(sourceDir);
      spec.include("**/*.wgsl");
      spec.setIncludeEmptyDirs(false);
      spec.into(getOutputDir().dir(SHADERS_FOLDER));
    });
  }

  private void validate(File sourceDir, List<String> names) {
    File report = new File(getTemporaryDir(), "report.txt");
    getFileSystem().delete(spec -> spec.delete(report));

    JavaLauncher launcher = getLauncher().get();
    WorkQueue queue = getWorkerExecutor().processIsolation(spec -> {
      spec.getClasspath().from(pluginLocation());
      spec.getClasspath().from(getValidatorClasspath());
      spec.getForkOptions().setExecutable(launcher.getExecutablePath().getAsFile().getAbsolutePath());
      spec.getForkOptions().jvmArgs(JAVA_ARGUMENT);
    });
    queue.submit(ValidateAction.class, params -> {
      params.getSourceDir().set(sourceDir);
      params.getNames().set(names);
      params.getContract().set(Contract.load().toMap());
      params.getReport().set(report);
    });
    try {
      queue.await();
    } catch (WorkerExecutionException e) {
      throw new GradleException("The shader validator could not run: " + rootMessage(e), e);
    }

    List<Diagnostic> found;
    try {
      found = DiagnosticReport.read(report.toPath());
    } catch (IOException e) {
      throw new GradleException("The shader validator did not leave a report: " + e.getMessage(), e);
    }
    StringBuilder errors = new StringBuilder();
    int count = 0;
    for (Diagnostic d : found) {
      if (d.isError()) {
        count++;
        errors.append(System.lineSeparator()).append(d.getText());
      } else {
        getLogger().warn(d.getText());
      }
    }
    if (count > 0) {
      throw new GradleException("Found " + count + " shader error" + (count == 1 ? "" : "s") + ":" + errors);
    }
  }

  private static File pluginLocation() {
    try {
      return new File(ProcessShadersTask.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new GradleException("Could not locate the shader plugin.", e);
    }
  }

  private static String rootMessage(Throwable t) {
    Throwable cause = t;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    String message = cause.getMessage();
    return message == null ? cause.toString() : message;
  }

  /**
   * Returns the folder the shaders are read from.
   *
   * @return The source directory property.
   */
  @Internal
  public abstract DirectoryProperty getSourceDir();

  /**
   * Returns the {@code .wgsl} files to process.
   *
   * @return The shader files. The task is skipped when there are none.
   */
  @InputFiles
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getSources();

  /**
   * Returns whether the shaders are compiled with Dawn.
   *
   * @return The validation switch.
   */
  @Input
  public abstract Property<Boolean> getValidate();

  /**
   * Returns the jwebgpu libraries the validator process runs with.
   *
   * <p>Their contents decide the compiler version, so they take part in the up-to-date check.
   *
   * @return The class path, empty when validation is switched off.
   */
  @Classpath
  public abstract ConfigurableFileCollection getValidatorClasspath();

  /**
   * Returns the Java 25 launcher the validator process is started with.
   *
   * @return The launcher property.
   */
  @Nested
  public abstract Property<JavaLauncher> getLauncher();

  /**
   * Returns the resource root the shaders are written to.
   *
   * <p>The shaders land in its {@value #SHADERS_FOLDER} subfolder. Read this property to consume the result.
   *
   * @return The output directory property.
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDir();

  /**
   * Returns the worker executor used to start the validator process.
   *
   * @return The executor, injected by Gradle.
   */
  @Inject
  protected abstract WorkerExecutor getWorkerExecutor();

  /**
   * Returns the file system operations used to clean and fill the output directory.
   *
   * @return The operations, injected by Gradle.
   */
  @Inject
  protected abstract FileSystemOperations getFileSystem();
}
