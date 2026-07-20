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
package org.flixelgdx.backend.lwjgl3.video.graal;

import com.sun.jna.Callback;
import com.sun.jna.CallbackReference;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * GraalVM native image Feature that registers the JNA entry points the video
 * extension's libvlc bindings need at run time.
 *
 * <p>Activated automatically when the {@code flixelgdx-video-lwjgl3} JAR is on the
 * native-image classpath, via
 * {@code META-INF/native-image/org.flixelgdx.video/native-image.properties}. Game
 * projects do not need to configure anything; JNA itself additionally ships its own
 * reachability metadata on Maven Central, which the GraalVM Gradle plugin picks up.
 *
 * <p>JNA's dispatcher calls back into the JVM through JNI when libvlc invokes the
 * video callbacks, so every callback interface (and JNA's core plumbing) must remain
 * visible to JNI in the closed world of a native image.
 */
public class FlixelVideoGraalFeature implements Feature {

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    try {
      registerJnaCore();
      registerVideoCallbacks(access);
    } catch (NoSuchMethodException | NoSuchFieldException e) {
      // A missing entry means a JNA version change renamed internals. The build does
      // not fail here, but the resulting binary may crash when a callback first fires.
      System.err.println("[FlixelGDX] Warning: video native image JNI registration failed: " + e);
      System.err.println("[FlixelGDX] Re-run the tracing agent and update FlixelVideoGraalFeature.");
    }
  }

  /**
   * Registers the JNA dispatch types our bindings touch from native code. JNA ships
   * its own reachability metadata for the rest of its internals; this list only covers
   * the types the libvlc stubs and callbacks exchange directly.
   */
  private void registerJnaCore() throws NoSuchMethodException, NoSuchFieldException {
    RuntimeJNIAccess.register(Native.class);
    RuntimeJNIAccess.register(Callback.class);
    RuntimeJNIAccess.register(CallbackReference.class);
    RuntimeJNIAccess.register(Pointer.class);
    RuntimeJNIAccess.register(Pointer.class.getDeclaredConstructor(long.class));
    RuntimeJNIAccess.register(Pointer.class.getDeclaredField("peer"));
    RuntimeJNIAccess.register(IntByReference.class);
    RuntimeJNIAccess.register(PointerByReference.class);
    RuntimeReflection.register(Structure.class);
  }

  /**
   * Registers the libvlc callback interfaces so JNA can build their JNI trampolines.
   * The callbacks are looked up reflectively by JNA, hence both reflection and JNI
   * registration of each interface method.
   */
  private void registerVideoCallbacks(BeforeAnalysisAccess access) {
    String[] callbackTypes = {
        "org.flixelgdx.backend.lwjgl3.video.LibVlc$LockCallback",
        "org.flixelgdx.backend.lwjgl3.video.LibVlc$UnlockCallback",
        "org.flixelgdx.backend.lwjgl3.video.LibVlc$DisplayCallback",
        "org.flixelgdx.backend.lwjgl3.video.LibVlc$FormatCallback",
        "org.flixelgdx.backend.lwjgl3.video.LibVlc$CleanupCallback",
        "org.flixelgdx.backend.lwjgl3.video.LibVlc$EventCallback"
    };
    for (String typeName : callbackTypes) {
      Class<?> type = access.findClassByName(typeName);
      if (type == null) {
        continue;
      }
      RuntimeReflection.register(type);
      RuntimeJNIAccess.register(type);
      for (var method : type.getDeclaredMethods()) {
        RuntimeReflection.register(method);
        RuntimeJNIAccess.register(method);
      }
    }
  }
}
