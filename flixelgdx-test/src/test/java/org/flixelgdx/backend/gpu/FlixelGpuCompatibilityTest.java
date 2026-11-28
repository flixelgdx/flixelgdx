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
package org.flixelgdx.backend.gpu;

import org.flixelgdx.Flixel;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.stringdotjar.jwebgpu.GpuAdapterInfo;
import io.github.stringdotjar.jwebgpu.GpuBackendType;
import io.github.stringdotjar.jwebgpu.GpuCallbackMode;
import io.github.stringdotjar.jwebgpu.GpuErrorFilter;
import io.github.stringdotjar.jwebgpu.GpuErrorType;
import io.github.stringdotjar.jwebgpu.GpuFeatureLevel;
import io.github.stringdotjar.jwebgpu.GpuFeatureName;
import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.GpuLimits;
import io.github.stringdotjar.jwebgpu.JWebGpu;
import io.github.stringdotjar.jwebgpu.ffm.JWebGpuFfm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks that the renderer stays within the rules of WebGPU compatibility mode, which is what OpenGL, OpenGL ES and
 * Direct3D 11 run in.
 *
 * <p>The device comes from the null backend, requested at the compatibility level with the core upgrade turned off,
 * so the same validation rules apply as on a real OpenGL machine, without needing one.
 */
class FlixelGpuCompatibilityTest {

  private static final String TINT = """
      struct Params {
        amount: f32,
        tint: vec4f,
      }
      @group(1) @binding(0) var<uniform> params: Params;

      @fragment
      fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
        let c = textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
        return vec4f(mix(c.rgb, params.tint.rgb, params.amount), c.a);
      }
      """;

  private static GpuInstance instance;
  private static FlixelGpuBootstrap boot;
  private static FlixelGpuGraphics graphics;

  @BeforeAll
  static void setUp() {
    try {
      JWebGpuFfm.install();
      instance = JWebGpu.createInstance();
    } catch (Throwable t) {
      assumeTrue(false, "WebGPU natives are not available: " + t);
    }
    boot = new FlixelGpuBootstrap(instance, null);
    boot.setBackendType(GpuBackendType.NULL);
    boot.setFeatureLevel(GpuFeatureLevel.COMPATIBILITY);
    boot.setCoreUpgrade(false);
    boot.start();
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (!boot.isDone() && System.nanoTime() < deadline) {
      instance.processEvents();
    }
    assumeTrue(boot.getDevice() != null, "No null-backend compatibility device: " + boot.getError());
    graphics = new FlixelGpuGraphics();
    graphics.setEventPump(instance::processEvents);
    graphics.initialize(boot.getAdapter(), boot.getDevice(), null, 320, 240, false, null);
    Flixel.graphics = graphics;
  }

  @AfterAll
  static void tearDown() {
    if (graphics != null) {
      graphics.dispose();
    }
    if (instance != null) {
      instance.release();
    }
  }

  private static FlixelTexture solid(int size) {
    return solid(graphics, size);
  }

  private static FlixelTexture solid(FlixelGpuGraphics on, int size) {
    FlixelImage image = new FlixelImage(size, size);
    image.fill(0xFFFFFFFF);
    return on.createTexture(image);
  }

  private static String popValidationError() {
    return popValidationError(graphics);
  }

  private static String popValidationError(FlixelGpuGraphics on) {
    String[] error = new String[1];
    boolean[] done = new boolean[1];
    on.getDevice().popErrorScope(GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, type, message) -> {
      if (type != null && type != GpuErrorType.NO_ERROR) {
        error[0] = type + ": " + message;
      }
      done[0] = true;
    });
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (!done[0] && System.nanoTime() < deadline) {
      instance.processEvents();
    }
    assertTrue(done[0], "the error scope never reported back");
    return error[0];
  }

  private static FlixelMatrix ortho(int width, int height) {
    return new FlixelMatrix().setToOrtho2DYDown(0, 0, width, height, true);
  }

  @Test
  void openGlOpenGlEsAndDirect3d11NeedTheCompatibilityLevel() {
    assertEquals(GpuFeatureLevel.COMPATIBILITY, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.OPENGL));
    assertEquals(GpuFeatureLevel.COMPATIBILITY, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.OPENGLES));
    assertEquals(GpuFeatureLevel.COMPATIBILITY, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.D3D11));
  }

  @Test
  void everyOtherBackendKeepsTheCoreLevel() {
    assertEquals(GpuFeatureLevel.CORE, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.UNDEFINED));
    assertEquals(GpuFeatureLevel.CORE, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.VULKAN));
    assertEquals(GpuFeatureLevel.CORE, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.METAL));
    assertEquals(GpuFeatureLevel.CORE, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.D3D12));
    assertEquals(GpuFeatureLevel.CORE, FlixelGpuBootstrap.featureLevelOf(GpuBackendType.NULL));
  }

  @Test
  void theDeviceStaysInCompatibilityModeWithTheAdapterLimits() {
    assertTrue(boot.isCompatibilityMode());
    assertFalse(graphics.getDevice().hasFeature(GpuFeatureName.CORE_FEATURES_AND_LIMITS));

    GpuLimits adapterLimits = new GpuLimits();
    GpuLimits deviceLimits = new GpuLimits();
    boot.getAdapter().getLimits(adapterLimits);
    graphics.getDevice().getLimits(deviceLimits);
    assertEquals(adapterLimits.maxTextureDimension2D, deviceLimits.maxTextureDimension2D);
    assertEquals(adapterLimits.maxUniformBufferBindingSize, deviceLimits.maxUniformBufferBindingSize);
  }

  @Test
  void aFrameWithEveryBlendModeATargetAndACustomShaderPassesValidation() {
    drawFrame(graphics);
  }

  /**
   * Forces a real OpenGL or OpenGL ES adapter the way a game does, and draws a frame on it.
   *
   * <p>Skipped when the machine has no such adapter or the natives were built without the OpenGL backends.
   */
  @ParameterizedTest
  @EnumSource(value = GpuBackendType.class, names = { "OPENGL", "OPENGLES" })
  void aForcedOpenGlApiGetsACompatibilityDeviceThatDraws(GpuBackendType backend) {
    FlixelGpuBootstrap glBoot = new FlixelGpuBootstrap(instance, null);
    glBoot.setBackendType(backend);
    glBoot.start();
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (!glBoot.isDone() && System.nanoTime() < deadline) {
      instance.processEvents();
    }
    assumeTrue(glBoot.getDevice() != null, "No " + backend + " adapter on this machine: " + glBoot.getError());

    GpuAdapterInfo info = new GpuAdapterInfo();
    glBoot.getAdapter().getInfo(info);
    assertEquals(backend, info.backendType);
    // OpenGL adapters can never do core, so the device always stays in compatibility mode.
    assertTrue(glBoot.isCompatibilityMode());

    FlixelGpuGraphics glGraphics = new FlixelGpuGraphics();
    glGraphics.setEventPump(instance::processEvents);
    glGraphics.initialize(glBoot.getAdapter(), glBoot.getDevice(), null, 320, 240, false, null);
    try {
      Flixel.graphics = glGraphics;
      drawFrame(glGraphics);
    } finally {
      Flixel.graphics = graphics;
      glGraphics.dispose();
    }
  }

  private static void drawFrame(FlixelGpuGraphics on) {
    FlixelTexture a = solid(on, 4);
    FlixelTexture b = solid(on, 8);
    FlixelRenderTarget target = on.createRenderTarget(64, 64);
    on.getDevice().pushErrorScope(GpuErrorFilter.VALIDATION);

    FlixelShaderProgram program = on.compileShaderSource(TINT);
    assertTrue(program.isValid());
    program.setUniform("amount", 0.5f);
    program.setUniform("tint", 1f, 0.5f, 0.25f, 1f);

    on.beginFrame();
    on.clear(0.1f, 0.2f, 0.3f, 1f);
    FlixelGpuBatch batch = (FlixelGpuBatch) on.getBatch();
    batch.setProjection(ortho(320, 240));
    batch.begin();
    for (FlixelBlendMode mode : FlixelBlendMode.values()) {
      batch.setBlendMode(mode);
      batch.draw(a, 10, 10, 20, 20);
      batch.draw(b, 40, 10, 20, 20);
    }
    batch.end();
    target.begin();
    batch.setProjection(ortho(64, 64));
    batch.begin();
    batch.draw(a, 0, 0, 64, 64);
    batch.end();
    target.end();
    batch.setProjection(ortho(320, 240));
    batch.begin();
    batch.draw(target.getTexture(), 100, 100, 64, 64);
    batch.end();
    on.endFrame();

    assertNull(popValidationError(on), "the renderer broke a compatibility mode rule");
    program.destroy();
    target.destroy();
    a.destroy();
    b.destroy();
  }
}
