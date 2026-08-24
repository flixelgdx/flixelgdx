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
package org.flixelgdx.backend;

import org.flixelgdx.logging.FlixelNoopStackTraceProvider;
import org.flixelgdx.logging.FlixelStackTraceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link FlixelRuntimeDevice} used before a backend is installed and on platforms that do
 * not report memory or classpath layout (such as web targets).
 *
 * <p>Memory readings are {@code 0}, and layout classification is
 * {@link FlixelRunEnvironment#UNKNOWN}. As a best effort it still resolves the code-source path and a
 * default logs directory next to it, so early startup logging has somewhere to go even without a
 * platform device.
 */
public enum FlixelNoopRuntimeDevice implements FlixelRuntimeDevice {

  /** Shared no-op instance. */
  INSTANCE;

  private FlixelRuntimeMode runtimeMode = FlixelRuntimeMode.RELEASE;
  private FlixelStackTraceProvider stackTraceProvider = FlixelNoopStackTraceProvider.INSTANCE;

  @Override
  @NotNull
  public FlixelRuntimeMode getMode() {
    return runtimeMode;
  }

  @Override
  public void setMode(@NotNull FlixelRuntimeMode mode) {
    if (mode != null) {
      this.runtimeMode = mode;
    }
  }

  @Override
  @NotNull
  public FlixelStackTraceProvider getStackTraceProvider() {
    return stackTraceProvider;
  }

  @Override
  public void setStackTraceProvider(@NotNull FlixelStackTraceProvider provider) {
    if (provider != null) {
      this.stackTraceProvider = provider;
    }
  }

  @Override
  @Nullable
  public String getWorkingDirectory() {
    return defaultCodeSourcePath();
  }

  @Override
  @Nullable
  public String getDefaultLogsFolderPath() {
    String path = defaultCodeSourcePath();
    if (path == null) {
      path = "";
    }
    path = path.replaceAll("/$", "");
    String cwd = System.getProperty("user.dir", "");
    String base = (cwd.isEmpty() ? path : cwd).replaceAll("/$", "");
    if (base.endsWith("/assets")) {
      base = base.substring(0, base.length() - "/assets".length());
    }
    return base + "/logs";
  }

  @Nullable
  private static String defaultCodeSourcePath() {
    try {
      return FlixelNoopRuntimeDevice.class
          .getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .toURI()
          .getPath();
    } catch (Exception e) {
      return null;
    }
  }
}
