/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3.graal;

import games.rednblack.miniaudio.MiniAudio;

import imgui.ImFontAtlas;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.assertion.ImAssertCallback;
import imgui.binding.ImGuiStruct;
import imgui.callback.ImGuiInputTextCallback;
import imgui.callback.ImListClipperCallback;
import imgui.callback.ImPlatformFuncViewport;
import imgui.callback.ImPlatformFuncViewportFloat;
import imgui.callback.ImPlatformFuncViewportImVec2;
import imgui.callback.ImPlatformFuncViewportString;
import imgui.callback.ImPlatformFuncViewportSuppBoolean;
import imgui.callback.ImPlatformFuncViewportSuppFloat;
import imgui.callback.ImPlatformFuncViewportSuppImVec2;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.internal.ImRect;
import imgui.type.ImString;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess;
import org.lwjgl.system.CallbackI;

/**
 * GraalVM native image Feature that registers all JNI entries for FlixelGDX's
 * built-in native libraries: imgui-java and gdx-miniaudio.
 *
 * <p>Activated automatically when the framework JAR is on the native-image classpath,
 * via {@code META-INF/native-image/me.stringdotjar.flixelgdx/native-image.properties}.
 * Game projects do not need to configure anything.
 *
 * <p>If you add a third-party library that also uses JNI or reflection, run the
 * tracing agent to capture configuration for those additional registrations. See
 * your project README for the {@code generateNativeConfig} task instructions.
 *
 * <p><strong>Versioning note:</strong> This class registers against imgui-java 1.90.x
 * and gdx-miniaudio 0.7. If the framework upgrades either dependency, the registration
 * lists here must be re-validated with the tracing agent.
 */
public class FlixelGraalFeature implements Feature {

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    try {
      registerImGui();
      registerMiniAudio();
    } catch (NoSuchFieldException | NoSuchMethodException e) {
      // A missing entry means a library version change introduced or removed a field/method.
      // The build will not fail here, but the resulting binary may crash at the call site.
      System.err.println("[FlixelGDX] Warning: native image JNI registration failed: " + e);
      System.err.println("[FlixelGDX] Re-run the tracing agent and update FlixelGraalFeature.");
    }
  }

  /**
   * Registers imgui-java 1.90.x JNI fields and callbacks.
   *
   * <p>{@code nInitJni()} caches every field and method ID listed here at startup.
   * GraalVM returns null for anything unregistered, causing a NullPointerException
   * inside {@code GetFieldID} that surfaces as {@code ExceptionInInitializerError}.
   */
  private void registerImGui() throws NoSuchFieldException, NoSuchMethodException {
    // Base native pointer inherited by every imgui wrapper object.
    RuntimeJNIAccess.register(ImGuiStruct.class.getDeclaredField("ptr"));

    RuntimeJNIAccess.register(ImVec2.class.getDeclaredField("x"));
    RuntimeJNIAccess.register(ImVec2.class.getDeclaredField("y"));

    RuntimeJNIAccess.register(ImVec4.class.getDeclaredField("x"));
    RuntimeJNIAccess.register(ImVec4.class.getDeclaredField("y"));
    RuntimeJNIAccess.register(ImVec4.class.getDeclaredField("z"));
    RuntimeJNIAccess.register(ImVec4.class.getDeclaredField("w"));

    RuntimeJNIAccess.register(ImRect.class.getDeclaredField("min"));
    RuntimeJNIAccess.register(ImRect.class.getDeclaredField("max"));

    RuntimeJNIAccess.register(ImFontAtlas.class.getDeclaredMethod("createAlpha8Pixels", int.class));
    RuntimeJNIAccess.register(ImFontAtlas.class.getDeclaredMethod("createRgba32Pixels", int.class));

    RuntimeJNIAccess.register(ImString.class.getDeclaredMethod("resizeInternal", int.class));

    Class<?> inputData = ImString.InputData.class;
    RuntimeJNIAccess.register(inputData.getDeclaredField("isDirty"));
    RuntimeJNIAccess.register(inputData.getDeclaredField("isResized"));
    RuntimeJNIAccess.register(inputData.getDeclaredField("size"));

    RuntimeJNIAccess.register(Boolean.class.getDeclaredMethod("getBoolean", String.class));
    RuntimeJNIAccess.register(CallbackI.class.getDeclaredMethod("callback", long.class, long.class));

    // Runtime callbacks invoked by the imgui native layer.
    RuntimeJNIAccess.register(ImAssertCallback.class.getDeclaredMethod("imAssert", String.class, int.class, String.class));
    RuntimeJNIAccess.register(ImGuiInputTextCallback.class.getDeclaredMethod("accept", long.class));
    RuntimeJNIAccess.register(ImListClipperCallback.class.getDeclaredMethod("accept", int.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewport.class.getDeclaredMethod("accept", ImGuiViewport.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewportFloat.class.getDeclaredMethod("accept", ImGuiViewport.class, float.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewportImVec2.class.getDeclaredMethod("accept", ImGuiViewport.class, ImVec2.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewportString.class.getDeclaredMethod("accept", ImGuiViewport.class, String.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewportSuppBoolean.class.getDeclaredMethod("get", ImGuiViewport.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewportSuppFloat.class.getDeclaredMethod("get", ImGuiViewport.class));
    RuntimeJNIAccess.register(ImPlatformFuncViewportSuppImVec2.class.getDeclaredMethod("get", ImGuiViewport.class, ImVec2.class));
    RuntimeJNIAccess.register(ImStrConsumer.class.getDeclaredMethod("accept", String.class));
    RuntimeJNIAccess.register(ImStrSupplier.class.getDeclaredMethod("get"));
  }

  /**
   * Registers gdx-miniaudio JNI callbacks.
   *
   * <p>gdx-miniaudio ships no GraalVM metadata; the native audio engine calls
   * {@code GetMethodID} for these three methods at startup.
   */
  private void registerMiniAudio() throws NoSuchMethodException {
    RuntimeJNIAccess.register(MiniAudio.class.getDeclaredMethod("on_native_notification", int.class));
    RuntimeJNIAccess.register(MiniAudio.class.getDeclaredMethod("on_native_sound_end", long.class));
    RuntimeJNIAccess.register(MiniAudio.class.getDeclaredMethod("on_native_log", int.class, String.class));
  }
}
