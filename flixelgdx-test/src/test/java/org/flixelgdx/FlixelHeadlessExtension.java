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
package org.flixelgdx;

import org.flixelgdx.backend.jvm.file.FlixelJvmFiles;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Installs the minimal framework state a unit test needs, without any GPU or windowing backend.
 *
 * <p>The framework's systems are normally brought up by a platform launcher. Tests do not have one,
 * so this extension fills the one gap that pure logic tests still hit: file access. It installs the
 * real java.io {@link FlixelJvmFiles} backend on {@code Flixel.files} so anything that reads or
 * writes files (for example {@code FlixelSave}) works against a real temp directory instead of the
 * silent no-op default. Everything else the framework exposes already defaults to a safe no-op, so
 * no rendering, audio, or input backend is required.
 */
public final class FlixelHeadlessExtension implements BeforeAllCallback {

  @Override
  public synchronized void beforeAll(ExtensionContext context) {
    if (!(Flixel.files instanceof FlixelJvmFiles)) {
      Flixel.files = new FlixelJvmFiles();
    }
  }
}
