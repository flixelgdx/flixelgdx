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
package org.flixelgdx.backend.html5;

import org.flixelgdx.backend.gpu.FlixelGpuGraphics;
import org.flixelgdx.graphics.wgsl.FlixelWgsl;
import org.jetbrains.annotations.NotNull;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

/**
 * Tells the TeaVM compiler which classpath files the web build must carry inside the program.
 *
 * <p>A browser has no class path to read from, so TeaVM only keeps a classpath file when something names it at
 * build time. The shared renderer reads two WGSL files that way, through {@code Class.getResourceAsStream(...)}: the
 * preamble that goes in front of every shader and the built-in sprite shader. Listing them here makes TeaVM embed
 * both into the translated program, on the JavaScript and the WebAssembly target alike, so
 * {@link FlixelWgsl#readResource(String)} finds them at run time exactly as it does on the desktop. The text is
 * never copied into Java source, which keeps the two {@code .wgsl} files the single source of truth.
 *
 * <p>A game's own shaders are not listed here. The HTML5 Gradle plugin publishes them next to the other assets and
 * the renderer reads them from there.
 *
 * <p>TeaVM finds this class by itself through {@code META-INF/services/org.teavm.classlib.ResourceSupplier} inside
 * the {@code flixelgdx-html5} jar, so having the dependency is enough. It only exists inside the TeaVM compiler: the
 * game never references it, so it is not part of the translated program.
 */
public final class FlixelHtml5ResourceSupplier implements ResourceSupplier {

  @Override
  public String[] supplyResources(@NotNull ResourceSupplierContext context) {
    return new String[] { FlixelWgsl.PREAMBLE_PATH, FlixelGpuGraphics.SPRITE_SHADER };
  }
}
