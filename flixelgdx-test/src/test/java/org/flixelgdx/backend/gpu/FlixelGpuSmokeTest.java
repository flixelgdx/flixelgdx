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
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.stringdotjar.jwebgpu.GpuBackendType;
import io.github.stringdotjar.jwebgpu.GpuCallbackMode;
import io.github.stringdotjar.jwebgpu.GpuErrorFilter;
import io.github.stringdotjar.jwebgpu.GpuErrorType;
import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.JWebGpu;
import io.github.stringdotjar.jwebgpu.ffm.JWebGpuFfm;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real renderer against WebGPU's null backend, which accepts every command and executes none.
 *
 * <p>That is enough to prove that the code builds valid pipelines, bind groups and passes (the validation layer
 * still runs) without needing a GPU, a window, or a way to read pixels back. The tests skip themselves when the
 * native library cannot be loaded, for example on a platform without a natives module.
 */
class FlixelGpuSmokeTest {

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
  private static FlixelGpuGraphics graphics;
  private static FlixelGraphicsManager previousGraphics;

  @BeforeAll
  static void setUp() {
    try {
      JWebGpuFfm.install();
      instance = JWebGpu.createInstance();
    } catch (Throwable t) {
      assumeTrue(false, "WebGPU natives are not available: " + t);
    }
    FlixelGpuBootstrap boot = new FlixelGpuBootstrap(instance, null);
    boot.setBackendType(GpuBackendType.NULL);
    boot.start();
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (!boot.isDone() && System.nanoTime() < deadline) {
      instance.processEvents();
    }
    assumeTrue(boot.getDevice() != null, "No null-backend device: " + boot.getError());
    graphics = new FlixelGpuGraphics();
    graphics.setEventPump(instance::processEvents);
    graphics.initialize(boot.getAdapter(), boot.getDevice(), null, 320, 240, false, null);
    previousGraphics = Flixel.graphics;
    Flixel.graphics = graphics;
  }

  @AfterAll
  static void tearDown() {
    if (previousGraphics != null) {
      Flixel.graphics = previousGraphics;
    }
    if (graphics != null) {
      graphics.dispose();
    }
    if (instance != null) {
      instance.release();
    }
  }

  private static FlixelTexture solid(int size) {
    FlixelImage image = new FlixelImage(size, size);
    image.fill(0xFFFFFFFF);
    return graphics.createTexture(image);
  }

  /** Pops the validation error scope and returns the error it caught, or {@code null} for none. */
  private static String popValidationError() {
    String[] error = new String[1];
    boolean[] done = new boolean[1];
    graphics.getDevice().popErrorScope(GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, type, message) -> {
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

  private static FlixelMatrix ortho() {
    return new FlixelMatrix().setToOrtho2DYDown(0, 0, 320, 240, true);
  }

  @Test
  void aFrameWithEveryBlendModeAndATargetRendersWithoutErrors() {
    FlixelTexture a = solid(4);
    FlixelTexture b = solid(8);
    FlixelRenderTarget target = graphics.createRenderTarget(64, 64);
    graphics.beginFrame();
    assertTrue(graphics.isFrameActive());
    graphics.clear(0.1f, 0.2f, 0.3f, 1f);
    FlixelGpuBatch batch = (FlixelGpuBatch) graphics.getBatch();
    batch.setProjection(ortho());
    batch.begin();
    for (FlixelBlendMode mode : FlixelBlendMode.values()) {
      batch.setBlendMode(mode);
      batch.draw(a, 10, 10, 20, 20);
      batch.draw(b, 40, 10, 20, 20);
    }
    batch.end();
    target.begin();
    batch.setProjection(new FlixelMatrix().setToOrtho2DYDown(0, 0, 64, 64, true));
    batch.begin();
    batch.draw(a, 0, 0, 64, 64);
    batch.end();
    target.end();
    batch.setProjection(ortho());
    batch.begin();
    batch.draw(target.getTexture(), 100, 100, 64, 64);
    batch.end();
    graphics.endFrame();
    assertTrue(graphics.getStats().draws >= FlixelBlendMode.values().length * 2);
    assertEquals(0, graphics.getStats().submits - 1, "a small frame is submitted once");
    assertTrue(graphics.getStats().passes >= 3, "screen, target, then screen again");
    target.destroy();
    a.destroy();
    b.destroy();
  }

  @Test
  void aTextureReadbackCallsBackOnceWithPaddedRows() {
    FlixelTexture texture = solid(10);
    assertTrue(graphics.supportsReadPixels());
    int[] calls = new int[1];
    int[] shape = new int[3];
    boolean[] hadPixels = new boolean[1];
    graphics.getDevice().pushErrorScope(GpuErrorFilter.VALIDATION);
    graphics.readPixels(texture, 2, 1, 5, 4, (rgba, width, height, rowBytes) -> {
      calls[0]++;
      hadPixels[0] = rgba != null;
      shape[0] = width;
      shape[1] = height;
      shape[2] = rowBytes;
      if (rgba != null) {
        assertTrue(rgba.capacity() >= rowBytes * height);
        rgba.get((height - 1) * rowBytes + width * 4 - 1);
      }
    });
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (calls[0] == 0 && System.nanoTime() < deadline) {
      graphics.beginFrame();
      graphics.endFrame();
    }
    assertEquals(1, calls[0], "the callback runs exactly once");
    assertTrue(hadPixels[0], "the pixels were mapped");
    assertEquals(5, shape[0]);
    assertEquals(4, shape[1]);
    assertEquals(256, shape[2], "rows are padded to 256 bytes");
    assertNull(popValidationError());
    texture.destroy();
  }

  @Test
  void aSceneCaptureReadsTheFrameDrawnAfterTheRequestAndAnOlderRequestGetsNoPixels() {
    assertTrue(graphics.supportsCapture());
    int[] calls = new int[2];
    int[] shape = new int[3];
    boolean[] hadPixels = new boolean[2];
    graphics.getDevice().pushErrorScope(GpuErrorFilter.VALIDATION);
    graphics.captureScene((rgba, width, height, rowBytes) -> {
      calls[0]++;
      hadPixels[0] = rgba != null;
    });
    graphics.captureScene((rgba, width, height, rowBytes) -> {
      calls[1]++;
      hadPixels[1] = rgba != null;
      shape[0] = width;
      shape[1] = height;
      shape[2] = rowBytes;
    });
    assertEquals(1, calls[0], "asking again answers the older request with no pixels");
    assertFalse(hadPixels[0]);

    long deadline = System.nanoTime() + 5_000_000_000L;
    while (calls[1] == 0 && System.nanoTime() < deadline) {
      graphics.beginFrame();
      graphics.clear(0f, 0f, 0f, 1f);
      graphics.beginScene();
      graphics.endScene();
      graphics.endFrame();
    }
    assertEquals(1, calls[1], "the callback runs exactly once");
    assertTrue(hadPixels[1], "the scene was copied back");
    assertEquals(graphics.getBackBufferWidth(), shape[0]);
    assertEquals(graphics.getBackBufferHeight(), shape[1]);
    assertTrue(shape[2] >= shape[0] * 4);
    assertNull(popValidationError());
    graphics.beginFrame();
    graphics.beginScene();
    graphics.endFrame();
    assertEquals(1, calls[1], "a later frame does not capture again");
  }

  @Test
  void aReadbackOfADestroyedTextureReportsNoPixels() {
    FlixelTexture texture = solid(4);
    texture.destroy();
    boolean[] called = new boolean[1];
    graphics.readPixels(texture, 0, 0, 4, 4, (rgba, width, height, rowBytes) -> {
      called[0] = true;
      assertNull(rgba);
    });
    assertTrue(called[0], "a failure calls back right away");
  }

  @Test
  void aShaderWithUniformsCompilesAndDrawsAndABrokenOneIsRejected() {
    FlixelShaderProgram program = graphics.compileShaderSource(TINT);
    assertTrue(program.isValid());
    program.setUniform("amount", 0.5f);
    program.setUniform("tint", 1f, 0.5f, 0.25f, 1f);

    FlixelShaderProgram broken = graphics.compileShaderSource("@fragment fn fs_main() -> { this is not wgsl }");
    assertFalse(broken.isValid());

    FlixelTexture a = solid(4);
    graphics.beginFrame();
    FlixelGpuBatch batch = (FlixelGpuBatch) graphics.getBatch();
    FlixelShader shader = new FlixelShader(TINT);
    assertTrue(shader.isCompiled());
    shader.setUniform("amount", 1f);
    batch.setProjection(ortho());
    batch.setShader(shader);
    batch.begin();
    batch.draw(a, 0, 0, 10, 10);
    batch.end();
    batch.setShader(null);
    graphics.endFrame();
    assertNotNull(graphics.getStats());
    shader.destroy();
    program.destroy();
    a.destroy();
  }

  @Test
  void aSceneSurfaceCompositesAtTheRenderScale() {
    graphics.setRenderScale(0.5f);
    FlixelTexture a = solid(4);
    graphics.beginFrame();
    graphics.beginScene();
    assertEquals(160, graphics.getSceneWidth());
    assertEquals(120, graphics.getSceneHeight());
    FlixelGpuBatch batch = (FlixelGpuBatch) graphics.getBatch();
    batch.setProjection(new FlixelMatrix().setToOrtho2DYDown(0, 0, 160, 120, true));
    batch.begin();
    batch.draw(a, 0, 0, 10, 10);
    batch.end();
    graphics.endScene();
    graphics.endFrame();
    graphics.setRenderScale(1f);
    a.destroy();
  }

  @Test
  void fullRingsSubmitEarlyInsteadOfDroppingDraws() {
    FlixelTexture a = solid(2);
    FlixelTexture b = solid(3);
    graphics.beginFrame();
    FlixelGpuBatch batch = (FlixelGpuBatch) graphics.getBatch();
    batch.setProjection(ortho());
    batch.begin();
    // Alternating textures forces one flush (and one uniform slot) per quad: far more than the ring holds.
    int quads = 6000;
    for (int i = 0; i < quads; i++) {
      batch.draw(i % 2 == 0 ? a : b, i % 300, 5, 4, 4);
    }
    batch.end();
    graphics.endFrame();
    assertEquals(quads, graphics.getStats().draws, "no draw may be dropped");
    assertTrue(graphics.getStats().submits > 1, "the uniform ring must have been rewound at least once");
    a.destroy();
    b.destroy();
  }

  @Test
  void textureUpdatesAndSmoothingNeverThrow() {
    FlixelTexture t = solid(8);
    t.setSmooth(true);
    FlixelImage patch = new FlixelImage(2, 2);
    patch.fill(0xFF0000FF);
    t.update(1, 1, patch);
    graphics.beginFrame();
    FlixelGpuBatch batch = (FlixelGpuBatch) graphics.getBatch();
    batch.setProjection(ortho());
    batch.begin();
    batch.draw(t, 0, 0, 8, 8);
    t.setSmooth(false);
    batch.draw(t, 10, 0, 8, 8);
    batch.end();
    graphics.endFrame();
    assertTrue(graphics.getTexture(t.getHandle()) != null);
    t.destroy();
    assertEquals(null, graphics.getTexture(t.getHandle()));
  }

  @Test
  void compressedTexturesUploadAndDrawInEverySupportedFormat() {
    assertTrue(graphics.isBlockFormatSupported(FlixelGpuBlockFormat.RGBA8));
    int tested = 0;
    for (FlixelGpuBlockFormat format : FlixelGpuBlockFormat.values()) {
      if (!graphics.isBlockFormatSupported(format)) {
        continue;
      }
      // 50x37 is not a whole number of blocks, and 6 levels is more than a padded compressed chain can keep.
      ByteBuffer file = FlixelKtx2Files.raw(format, 50, 37, 6, 0);
      graphics.getDevice().pushErrorScope(GpuErrorFilter.VALIDATION);
      FlixelTexture texture = graphics.createCompressedTexture(file);
      assertNotNull(texture, format.name());
      texture.update(0, 0, new FlixelImage(2, 2));
      graphics.beginFrame();
      FlixelGpuBatch batch = (FlixelGpuBatch) graphics.getBatch();
      batch.setProjection(ortho());
      batch.begin();
      batch.draw(texture, 0, 0, 50, 37);
      texture.setSmooth(true);
      batch.draw(texture, 60, 0, 25, 18);
      batch.end();
      graphics.endFrame();
      assertNull(popValidationError(), format.name());

      FlixelGpuTexture gpu = (FlixelGpuTexture) texture;
      assertEquals(50, gpu.getWidth());
      assertEquals(37, gpu.getHeight());
      assertEquals(format.padded(50), gpu.getGpuWidth());
      assertEquals(format.padded(37), gpu.getGpuHeight());
      assertEquals(50f / format.padded(50), gpu.getUScale(), 1e-6f);
      assertEquals(format.isCompressed(), gpu.isCompressed());
      texture.destroy();
      tested++;
    }
    assertTrue(tested >= 1);
  }

  @Test
  void anUnusableCompressedTextureReturnsNull() {
    ByteBuffer basis = FlixelKtx2Files.fixture("etc1s_alpha.ktx2");
    assertNull(graphics.createCompressedTexture(basis), "there is no transcoder in this test");
    assertNull(graphics.createCompressedTexture(ByteBuffer.allocateDirect(16)));
  }
}
