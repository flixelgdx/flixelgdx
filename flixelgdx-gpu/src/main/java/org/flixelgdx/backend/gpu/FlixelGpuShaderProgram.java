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

import org.flixelgdx.backend.gpu.wgsl.FlixelWgsl;
import org.flixelgdx.backend.gpu.wgsl.FlixelWgslLayout;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelUniformBuffer;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.stringdotjar.jwebgpu.GpuBindGroup;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayout;
import io.github.stringdotjar.jwebgpu.GpuPipelineLayout;
import io.github.stringdotjar.jwebgpu.GpuShaderModule;

/**
 * A compiled WGSL shader: the GPU shader module, its pipeline layout, and the CPU copy of its uniform struct.
 *
 * <p>The uniform struct (bind group 1) is parsed once when the shader is created; {@code setUniform(...)} then
 * writes values by name into the CPU copy. The renderer uploads that copy into the uniform ring only when it
 * changed, and binds it with a dynamic offset. Everything here is created once at load time, never per frame.
 */
public final class FlixelGpuShaderProgram implements FlixelShaderProgram {

  private long uploadEpoch = -1L;

  private final int id;
  private final int epoch;
  private final String vertexEntry;
  private final String fragmentEntry;
  private final FlixelUniformBuffer uniforms;
  private final FlixelGpuGraphics graphics;
  private GpuShaderModule module;
  private GpuPipelineLayout pipelineLayout;
  private GpuBindGroupLayout groupLayout;
  private GpuBindGroup bindGroup;

  private int uploadOffset;

  private boolean valid = true;

  FlixelGpuShaderProgram(@NotNull FlixelGpuGraphics graphics, int id, @NotNull GpuShaderModule module,
      @NotNull String vertexEntry, @NotNull String fragmentEntry, @NotNull FlixelWgslLayout layout) {
    this.graphics = graphics;
    this.epoch = graphics.getDeviceEpoch();
    this.id = id;
    this.module = module;
    this.vertexEntry = vertexEntry;
    this.fragmentEntry = fragmentEntry;
    this.uniforms = layout.isEmpty() ? null : new FlixelUniformBuffer(layout);
  }

  /**
   * Hands over the GPU objects built for this program.
   *
   * @param pipelineLayout The layout shared by every pipeline of this shader.
   * @param groupLayout The layout of bind group 1, or {@code null} when the shader has no uniforms.
   * @param bindGroup The bind group over the uniform ring, or {@code null} when the shader has no uniforms.
   */
  void attach(@NotNull GpuPipelineLayout pipelineLayout, @Nullable GpuBindGroupLayout groupLayout,
      @Nullable GpuBindGroup bindGroup) {
    this.pipelineLayout = pipelineLayout;
    this.groupLayout = groupLayout;
    this.bindGroup = bindGroup;
  }

  /**
   * Marks the program unusable, for example after the GPU reported a compile error.
   */
  void invalidate() {
    valid = false;
  }

  /**
   * Returns the offset of this shader's current uniforms inside the uniform ring, uploading them first when they
   * changed or when the ring was rewound since the last upload.
   *
   * @param upload The renderer's uploader.
   * @return The dynamic offset to bind, or {@code -1} when the ring is full.
   */
  int uniformOffset(@NotNull FlixelGpuGraphics upload) {
    FlixelUniformBuffer u = uniforms;
    if (u == null) {
      return 0;
    }
    long epoch = upload.getRingEpoch();
    if (!u.isDirty() && epoch == uploadEpoch) {
      return uploadOffset;
    }
    int at = upload.uploadUniforms(u.getData(), u.getSize());
    if (at >= 0) {
      u.clearDirty();
      uploadEpoch = upload.getRingEpoch();
      uploadOffset = at;
    }
    return at;
  }

  @Override
  public boolean isValid() {
    return valid && module != null && epoch == graphics.getDeviceEpoch();
  }

  @Override
  public void setUniform(@NotNull String name, float value) {
    if (uniforms != null) {
      uniforms.setFloat(name, value);
    }
  }

  @Override
  public void setUniform(@NotNull String name, int value) {
    if (uniforms != null) {
      uniforms.setInt(name, value);
    }
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelVector value) {
    if (uniforms != null) {
      uniforms.setVector(name, value);
    }
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelColor value) {
    if (uniforms != null) {
      uniforms.setColor(name, value);
    }
  }

  @Override
  public void setUniform(@NotNull String name, @NotNull FlixelMatrix value) {
    if (uniforms != null) {
      uniforms.setMatrix(name, value);
    }
  }

  @Override
  public void setUniform(@NotNull String name, float x, float y) {
    if (uniforms != null) {
      uniforms.set(name, x, y);
    }
  }

  @Override
  public void setUniform(@NotNull String name, float x, float y, float z) {
    if (uniforms != null) {
      uniforms.set(name, x, y, z);
    }
  }

  @Override
  public void setUniform(@NotNull String name, float x, float y, float z, float w) {
    if (uniforms != null) {
      uniforms.set(name, x, y, z, w);
    }
  }

  @Override
  public void destroy() {
    if (module == null) {
      return;
    }
    graphics.forgetProgram(this);
    if (bindGroup != null) {
      bindGroup.release();
      bindGroup = null;
    }
    if (groupLayout != null) {
      groupLayout.release();
      groupLayout = null;
    }
    if (pipelineLayout != null) {
      pipelineLayout.release();
      pipelineLayout = null;
    }
    module.release();
    module = null;
    valid = false;
  }

  /**
   * Returns the id used in pipeline cache keys.
   *
   * @return The shader id.
   */
  public int getId() {
    return id;
  }

  /**
   * Returns the name of the vertex entry point to use.
   *
   * @return {@code vs_main} or the framework default.
   */
  @NotNull
  public String getVertexEntry() {
    return vertexEntry;
  }

  /**
   * Returns the name of the fragment entry point to use.
   *
   * @return The premultiplying entry point the framework adds, or {@code fs_main} for a shader it could not wrap.
   */
  @NotNull
  public String getFragmentEntry() {
    return fragmentEntry;
  }

  /**
   * Returns whether the fragment output is premultiplied, which decides the blend factors.
   *
   * @return {@code true} when the framework's premultiplying entry point is used.
   */
  public boolean isPremultiplied() {
    return fragmentEntry.equals(FlixelWgsl.PREMULTIPLIED_FRAGMENT_ENTRY);
  }

  /**
   * Returns the shader module.
   *
   * @return The module, or {@code null} after {@link #destroy()}.
   */
  @Nullable
  public GpuShaderModule getModule() {
    return module;
  }

  /**
   * Returns the pipeline layout.
   *
   * @return The layout, or {@code null} before it is attached or after {@link #destroy()}.
   */
  @Nullable
  public GpuPipelineLayout getPipelineLayout() {
    return pipelineLayout;
  }

  /**
   * Returns bind group 1, the shader's own uniforms.
   *
   * @return The bind group, or {@code null} when the shader has no uniforms.
   */
  @Nullable
  public GpuBindGroup getBindGroup() {
    return bindGroup;
  }

  /**
   * Returns the CPU copy of the uniform struct.
   *
   * @return The uniform buffer, or {@code null} when the shader has no uniforms.
   */
  @Nullable
  public FlixelUniformBuffer getUniforms() {
    return uniforms;
  }
}
