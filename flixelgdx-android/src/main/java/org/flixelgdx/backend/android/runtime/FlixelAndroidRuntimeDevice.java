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
package org.flixelgdx.backend.android.runtime;

import android.content.Context;
import android.os.Debug;
import org.flixelgdx.backend.FlixelCrashHandler;
import org.flixelgdx.backend.FlixelRunEnvironment;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.logging.FlixelNoopStackTraceProvider;
import org.flixelgdx.logging.FlixelStackTraceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * Android implementation of {@link FlixelRuntimeDevice}: native heap via {@link Debug}, JVM heap
 * via {@link Runtime}, and log directory under the app's private files directory.
 */
public class FlixelAndroidRuntimeDevice implements FlixelRuntimeDevice {

  @NotNull
  private final File logsFolder;

  @NotNull
  private FlixelRuntimeMode mode = FlixelRuntimeMode.RELEASE;

  @NotNull
  private FlixelStackTraceProvider stackTraceProvider = FlixelNoopStackTraceProvider.INSTANCE;

  private boolean runtimeModeSet = false;

  /**
   * Creates a runtime device for the given Android context.
   *
   * @param context The application context used to resolve storage directories.
   */
  public FlixelAndroidRuntimeDevice(@NotNull Context context) {
    logsFolder = new File(context.getFilesDir(), "logs");
  }

  @Override
  public long getJavaHeap() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  @Override
  public long getNativeHeap() {
    return Debug.getNativeHeapAllocatedSize();
  }

  @Override
  public FlixelRunEnvironment getEnvironment() {
    return FlixelRunEnvironment.JAR;
  }

  @Override
  @Nullable
  public String getWorkingDirectory() {
    return null;
  }

  @Override
  @Nullable
  public String getDefaultLogsFolderPath() {
    return logsFolder.getAbsolutePath();
  }

  @Override
  @NotNull
  public FlixelRuntimeMode getMode() {
    return mode;
  }

  @Override
  public void setMode(@NotNull FlixelRuntimeMode mode) {
    Objects.requireNonNull(mode, "The provided runtime mode cannot be null.");
    if (!runtimeModeSet) {
      this.mode = mode;
      runtimeModeSet = true;
    } else {
      throw new RuntimeException("The runtime mode has already been set, it cannot be changed.");
    }
  }

  @Override
  @NotNull
  public FlixelStackTraceProvider getStackTraceProvider() {
    return stackTraceProvider;
  }

  @Override
  public void setStackTraceProvider(@NotNull FlixelStackTraceProvider provider) {
    this.stackTraceProvider = Objects.requireNonNull(provider, "provider cannot be null.");
  }

  @Override
  public void setCrashHandler(@NotNull FlixelCrashHandler handler) {
    Thread.setDefaultUncaughtExceptionHandler(handler::onCrash);
  }
}
