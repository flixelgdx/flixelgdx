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
package org.flixelgdx.backend.desktop.graal;

import org.flixelgdx.backend.desktop.audio.FlixelMiniAudio;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

/**
 * GraalVM {@link Feature} that prepares the desktop backend for native-image compilation.
 *
 * <p>GraalVM's closed-world assumption strips out classes, methods, and resources that are not
 * reachable through static analysis. This feature explicitly registers two things that the
 * static analyzer cannot see on its own:
 *
 * <ul>
 *   <li><b>JNI methods</b> - {@link FlixelMiniAudio} declares all its methods as {@code native},
 *       meaning the JVM resolves them through JNI at run time. Without explicit registration,
 *       GraalVM removes those method descriptors and the native library cannot bind to them.</li>
 *   <li><b>Bundled platform natives</b> - the miniaudio shared libraries
 *       ({@code libflixel_miniaudio.so}, {@code flixel_miniaudio.dll},
 *       {@code libflixel_miniaudio.dylib}) are embedded as classpath resources and extracted to a
 *       temp file at run time by {@code FlixelMiniAudio.ensureLoaded()}. GraalVM excludes
 *       resources unless they are explicitly declared; each platform variant is registered
 *       individually so the correct binary ships inside the native image.</li>
 * </ul>
 *
 * <p>LWJGL (SDL3, bgfx, stb, zstd) ships its own native-image support inside each module JAR, so
 * those libraries do not need to be handled here.
 *
 * <p>This feature is wired in automatically by the {@code META-INF/native-image} properties file
 * bundled with the desktop module JAR; game projects do not need to reference it directly.
 */
public class FlixelGraalFeature implements Feature {

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    // Register FlixelMiniAudio so GraalVM keeps every native method descriptor in the JNI table.
    // Without this, the miniaudio shared library cannot bind its C symbols to Java at run time.
    RuntimeJNIAccess.register(FlixelMiniAudio.class);
    RuntimeJNIAccess.register(FlixelMiniAudio.class.getDeclaredMethods());

    // Include the bundled platform natives so FlixelMiniAudio.ensureLoaded() can extract and
    // load them from a temp file at run time.
    Module module = FlixelGraalFeature.class.getModule();
    RuntimeResourceAccess.addResource(module, "org/flixelgdx/natives/libflixel_miniaudio.so");
    RuntimeResourceAccess.addResource(module, "org/flixelgdx/natives/flixel_miniaudio.dll");
    RuntimeResourceAccess.addResource(module, "org/flixelgdx/natives/libflixel_miniaudio.dylib");
  }
}
