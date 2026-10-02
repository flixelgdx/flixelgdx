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
package org.flixelgdx.backend.html5.logging;

import org.jetbrains.annotations.NotNull;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * A TeaVM compiler plugin that records the source location of every log call on the web.
 *
 * <p>On desktop and Android the logger finds out who called it by walking the stack. A browser build
 * has no stack to walk, so the location has to be written down at compile time instead. This plugin
 * hooks into TeaVM while it translates the game, and a {@link FlixelLogSiteTransformer} inserts a
 * call to {@code FlixelLogSiteMarker.mark(file, line, class, method)} just before every log call.
 * Think of it as a clerk who sticks a note with the file, line, class, and method onto each letter
 * before it goes into the mailbox. The logger later reads that note through
 * {@code FlixelLogEntry.site()}.
 *
 * <p>Game developers do not need to do anything. TeaVM finds this plugin by itself, because it is
 * listed in {@code META-INF/services/org.teavm.vm.spi.TeaVMPlugin} inside the {@code flixelgdx-html5}
 * jar, which is the same way TeaVM discovers its own JavaScript interop plugin. Having the
 * {@code flixelgdx-html5} dependency is enough.
 *
 * <p>This class and the transformer only exist inside the TeaVM compiler. The game never references
 * them, so TeaVM never compiles them into the web bundle, and {@code teavm-core} is a compile-only
 * dependency of {@code flixelgdx-html5}.
 *
 * @see FlixelLogSiteTransformer
 */
public final class FlixelLogSiteTeaVMPlugin implements TeaVMPlugin {

  @Override
  public void install(@NotNull TeaVMHost host) {
    host.add(new FlixelLogSiteTransformer());
  }
}
