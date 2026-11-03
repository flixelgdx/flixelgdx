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
package org.flixelgdx.gradle.shader.worker;

import org.gradle.workers.WorkAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The unit of work that runs in the forked validator process.
 *
 * <p>It brings up one Dawn device, checks every shader with it, and writes the findings to the report file. It
 * only throws when the validator itself cannot run (for example because the Dawn library is missing); mistakes in
 * a shader are findings, not exceptions.
 */
public abstract class ValidateAction implements WorkAction<ValidateParameters> {

  @Override
  public void execute() {
    ValidateParameters p = getParameters();
    Path root = p.getSourceDir().get().getAsFile().toPath();
    List<Diagnostic> findings = new ArrayList<>();
    try (ShaderValidator validator = new ShaderValidator(Contract.fromMap(p.getContract().get()))) {
      for (String name : p.getNames().get()) {
        Path file = root.resolve(name);
        String source = Files.readString(file, StandardCharsets.UTF_8);
        validator.validate(file.toAbsolutePath().toString(), source, findings);
      }
      DiagnosticReport.write(findings, p.getReport().get().getAsFile().toPath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
