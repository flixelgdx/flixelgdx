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

import org.gradle.api.artifacts.transform.CacheableTransform;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Gradle artifact transform that runs the FlixelGDX bytecode weaver over a single JAR from
 * {@code runtimeClasspath}. Registered by {@link FlixelLoggingPlugin} when
 * {@link FlixelLoggingExtension#getWeaveDependencies()} is {@code true}.
 *
 * <p>Gradle caches the output keyed on the input JAR content, so the transform only re-runs
 * when a dependency actually changes. Non-class entries (resources, manifests) are copied verbatim.
 * If the weaver fails on an individual class the original bytes are kept, so the output JAR is
 * always a valid copy of the input.
 */
@CacheableTransform
public abstract class FlixelJarWeaverTransform implements TransformAction<TransformParameters.None> {

  @InputArtifact
  @PathSensitive(PathSensitivity.NAME_ONLY)
  public abstract Provider<FileSystemLocation> getInputArtifact();

  @Override
  public void transform(TransformOutputs outputs) {
    File input = getInputArtifact().get().getAsFile();
    File output = outputs.file(input.getName());
    try {
      FlixelTransformLoggingTask.weaveJar(input.toPath(), output.toPath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
