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
package org.flixelgdx.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the mapping from a graphics API to the shader variant directory the runtime loads,
 * which must stay in lock-step with the folder names the shader plugin emits.
 */
class FlixelShaderVariantTest {

  @Test
  void mapsEachApiToItsVariantFolder() {
    assertEquals("dx11", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.Direct3D11));
    assertEquals("dx11", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.Direct3D12));
    assertEquals("metal", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.Metal));
    assertEquals("spirv", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.Vulkan));
    assertEquals("glsl", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.OpenGL));
    assertEquals("glsl", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.OpenGLES));
  }

  @Test
  void unknownApiFallsBackToGlsl() {
    assertEquals("glsl", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.WebGPU));
    assertEquals("glsl", FlixelGraphicsManager.shaderVariantDir(FlixelGraphicsApi.Noop));
  }
}
