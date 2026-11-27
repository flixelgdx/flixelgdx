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

import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.junit.jupiter.api.Test;

import io.github.stringdotjar.jwebgpu.GpuBackendType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlixelGpuApiTest {

  @Test
  void everyForceableApiMapsToItsNativeBackend() {
    assertEquals(GpuBackendType.VULKAN, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.VULKAN));
    assertEquals(GpuBackendType.METAL, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.METAL));
    assertEquals(GpuBackendType.D3D11, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.DIRECT3D11));
    assertEquals(GpuBackendType.D3D12, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.DIRECT3D12));
    assertEquals(GpuBackendType.OPENGL, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.OPENGL));
    assertEquals(GpuBackendType.OPENGLES, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.OPENGL_ES));
    assertEquals(GpuBackendType.NULL, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.NOOP));
  }

  @Test
  void nullAndWebGpuMeanTheAutomaticChoice() {
    assertEquals(GpuBackendType.UNDEFINED, FlixelGpuGraphics.backendTypeOf(null));
    assertEquals(GpuBackendType.UNDEFINED, FlixelGpuGraphics.backendTypeOf(FlixelGraphicsApi.WEBGPU));
  }

  @Test
  void aCustomApiFallsBackToTheAutomaticChoice() {
    FlixelGraphicsApi custom = FlixelGraphicsApi.of("MyRenderer");
    assertEquals(GpuBackendType.UNDEFINED, FlixelGpuGraphics.backendTypeOf(custom));

    FlixelGpuGraphics graphics = new FlixelGpuGraphics();
    graphics.setApi(custom);
    assertSame(custom, graphics.getApi());
    assertEquals(GpuBackendType.UNDEFINED, graphics.getForcedBackendType());
  }

  @Test
  void getApiReportsTheRequestedApiBeforeTheGraphicsContextExists() {
    FlixelGpuGraphics graphics = new FlixelGpuGraphics();
    assertSame(FlixelGraphicsApi.WEBGPU, graphics.getApi());
    assertEquals(GpuBackendType.UNDEFINED, graphics.getForcedBackendType());

    graphics.setApi(FlixelGraphicsApi.VULKAN);
    assertSame(FlixelGraphicsApi.VULKAN, graphics.getApi());
    assertEquals(GpuBackendType.VULKAN, graphics.getForcedBackendType());

    graphics.setApi(FlixelGraphicsApi.WEBGPU);
    assertSame(FlixelGraphicsApi.WEBGPU, graphics.getApi());
    assertEquals(GpuBackendType.UNDEFINED, graphics.getForcedBackendType());
  }

  @Test
  void settingANullApiThrows() {
    FlixelGpuGraphics graphics = new FlixelGpuGraphics();
    assertThrows(NullPointerException.class, () -> graphics.setApi(null));
    assertSame(FlixelGraphicsApi.WEBGPU, graphics.getApi());
  }
}
