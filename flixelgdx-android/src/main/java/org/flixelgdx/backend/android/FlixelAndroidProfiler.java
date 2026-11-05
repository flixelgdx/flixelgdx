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
package org.flixelgdx.backend.android;

import org.flixelgdx.backend.FlixelProfiler;
import org.jetbrains.annotations.NotNull;

import android.os.Trace;

/**
 * Android {@link FlixelProfiler} that writes sections into the system trace.
 *
 * <p>Sections show up as labeled bars in Perfetto and in the Android Studio profiler's system trace,
 * next to the framework's own frame phases. Android limits a section name to 127 characters, so pass
 * compile-time constants. Calls allocate nothing.
 */
public final class FlixelAndroidProfiler implements FlixelProfiler {

  @Override
  public void begin(@NotNull String name) {
    Trace.beginSection(name);
  }

  @Override
  public void end() {
    Trace.endSection();
  }
}
