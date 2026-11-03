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
import org.flixelgdx.backend.gpu.wgsl.FlixelWgsl;
import org.flixelgdx.backend.gpu.wgsl.FlixelWgslLayout;
import org.flixelgdx.collections.FlixelLongMap;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.stringdotjar.jwebgpu.GpuAddressMode;
import io.github.stringdotjar.jwebgpu.GpuBindGroup;
import io.github.stringdotjar.jwebgpu.GpuBindGroupDescriptor;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayout;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayoutDescriptor;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayoutEntry;
import io.github.stringdotjar.jwebgpu.GpuBlendState;
import io.github.stringdotjar.jwebgpu.GpuBuffer;
import io.github.stringdotjar.jwebgpu.GpuBufferBindingType;
import io.github.stringdotjar.jwebgpu.GpuCallbackMode;
import io.github.stringdotjar.jwebgpu.GpuColorWriteMask;
import io.github.stringdotjar.jwebgpu.GpuCullMode;
import io.github.stringdotjar.jwebgpu.GpuDevice;
import io.github.stringdotjar.jwebgpu.GpuErrorFilter;
import io.github.stringdotjar.jwebgpu.GpuErrorType;
import io.github.stringdotjar.jwebgpu.GpuFilterMode;
import io.github.stringdotjar.jwebgpu.GpuFragmentState;
import io.github.stringdotjar.jwebgpu.GpuFrontFace;
import io.github.stringdotjar.jwebgpu.GpuMipmapFilterMode;
import io.github.stringdotjar.jwebgpu.GpuPipelineLayout;
import io.github.stringdotjar.jwebgpu.GpuPipelineLayoutDescriptor;
import io.github.stringdotjar.jwebgpu.GpuPopErrorScopeStatus;
import io.github.stringdotjar.jwebgpu.GpuPrimitiveTopology;
import io.github.stringdotjar.jwebgpu.GpuRenderPipeline;
import io.github.stringdotjar.jwebgpu.GpuRenderPipelineDescriptor;
import io.github.stringdotjar.jwebgpu.GpuSampler;
import io.github.stringdotjar.jwebgpu.GpuSamplerBindingType;
import io.github.stringdotjar.jwebgpu.GpuSamplerDescriptor;
import io.github.stringdotjar.jwebgpu.GpuShaderModule;
import io.github.stringdotjar.jwebgpu.GpuShaderModuleDescriptor;
import io.github.stringdotjar.jwebgpu.GpuShaderSourceWGSL;
import io.github.stringdotjar.jwebgpu.GpuShaderStage;
import io.github.stringdotjar.jwebgpu.GpuTextureFormat;
import io.github.stringdotjar.jwebgpu.GpuTextureSampleType;
import io.github.stringdotjar.jwebgpu.GpuTextureView;
import io.github.stringdotjar.jwebgpu.GpuTextureViewDimension;
import io.github.stringdotjar.jwebgpu.GpuVertexAttribute;
import io.github.stringdotjar.jwebgpu.GpuVertexBufferLayout;
import io.github.stringdotjar.jwebgpu.GpuVertexFormat;
import io.github.stringdotjar.jwebgpu.GpuVertexStepMode;

/**
 * Builds and caches everything a draw needs from the GPU's point of view: bind group layouts, samplers, shader
 * programs and render pipelines.
 *
 * <p>A render pipeline is the GPU's compiled recipe for one kind of draw, and it is expensive to build. This class
 * keeps each recipe in a map keyed by {@link FlixelPipelineKey} (blend mode, target format, shader), so the
 * renderer builds each recipe once and every later draw is a hash lookup. All descriptors are created once and
 * refilled, so a cache hit allocates nothing.
 *
 * <p>The vertex layout is fixed for every pipeline: a 20 byte vertex made of a {@code vec2f} position, a
 * {@code vec2f} texture coordinate and a {@code unorm8x4} color, which matches the {@code FlixelVertexInput}
 * struct of the shader preamble.
 */
public final class FlixelGpuPipelines {

  private static final FlixelLogger LOG = Flixel.log.tagged("Graphics");

  private final FlixelLongMap<GpuRenderPipeline> cache = new FlixelLongMap<>();
  private final GpuDevice device;

  private final GpuBindGroupLayoutDescriptor layoutDescriptor = new GpuBindGroupLayoutDescriptor();
  private final GpuPipelineLayoutDescriptor pipelineLayoutDescriptor = new GpuPipelineLayoutDescriptor();
  private final GpuBindGroupDescriptor groupDescriptor = new GpuBindGroupDescriptor();
  private final GpuSamplerDescriptor samplerDescriptor = new GpuSamplerDescriptor();
  private final GpuShaderModuleDescriptor moduleDescriptor = new GpuShaderModuleDescriptor();
  private final GpuShaderSourceWGSL wgsl = new GpuShaderSourceWGSL();
  private final GpuRenderPipelineDescriptor pipelineDescriptor = new GpuRenderPipelineDescriptor();
  private final GpuFragmentState fragment = new GpuFragmentState(1, 1);
  private final GpuBlendState blendState = new GpuBlendState();
  private final GpuBindGroupLayout globalsLayout;
  private final GpuPipelineLayout globalsPipelineLayout;
  private final GpuSampler nearestSampler;
  private final GpuSampler linearSampler;

  private int nextShaderId = 1;

  /**
   * Creates the shared layouts and samplers.
   *
   * @param device The device to build everything on.
   */
  public FlixelGpuPipelines(@NotNull GpuDevice device) {
    this.device = device;

    GpuVertexBufferLayout vertexLayout = new GpuVertexBufferLayout(3);
    GpuVertexAttribute position = vertexLayout.attributes[0];
    position.format = GpuVertexFormat.FLOAT32X2;
    position.offset = 0L;
    position.shaderLocation = 0;
    GpuVertexAttribute uv = vertexLayout.attributes[1];
    uv.format = GpuVertexFormat.FLOAT32X2;
    uv.offset = 8L;
    uv.shaderLocation = 1;
    GpuVertexAttribute color = vertexLayout.attributes[2];
    color.format = GpuVertexFormat.UNORM8X4;
    color.offset = 16L;
    color.shaderLocation = 2;
    vertexLayout.attributeCount = 3;
    vertexLayout.arrayStride = FlixelGpuBatch.vertexStrideBytes();
    vertexLayout.stepMode = GpuVertexStepMode.VERTEX;

    pipelineDescriptor.vertex.bufferCount = 1;
    pipelineDescriptor.vertex.buffers[0] = vertexLayout;
    pipelineDescriptor.primitive.topology = GpuPrimitiveTopology.TRIANGLE_LIST;
    pipelineDescriptor.primitive.frontFace = GpuFrontFace.CCW;
    pipelineDescriptor.primitive.cullMode = GpuCullMode.NONE;
    fragment.targetCount = 1;
    fragment.constants[0].key = FlixelWgsl.DARKEN_CONSTANT;
    fragment.targets[0].writeMask = GpuColorWriteMask.ALL;
    pipelineDescriptor.fragment = fragment;

    layoutDescriptor.label = "flixel globals";
    layoutDescriptor.entryCount = 3;
    GpuBindGroupLayoutEntry[] e = layoutDescriptor.entries;
    e[0].reset();
    e[0].binding = 0;
    e[0].visibility = GpuShaderStage.VERTEX | GpuShaderStage.FRAGMENT;
    e[0].buffer.type = GpuBufferBindingType.UNIFORM;
    e[0].buffer.hasDynamicOffset = true;
    e[0].buffer.minBindingSize = FlixelWgsl.GLOBALS_SIZE;
    e[1].reset();
    e[1].binding = 1;
    e[1].visibility = GpuShaderStage.FRAGMENT;
    e[1].texture.sampleType = GpuTextureSampleType.FLOAT;
    e[1].texture.viewDimension = GpuTextureViewDimension.D2;
    e[2].reset();
    e[2].binding = 2;
    e[2].visibility = GpuShaderStage.FRAGMENT;
    e[2].sampler.type = GpuSamplerBindingType.FILTERING;
    globalsLayout = device.createBindGroupLayout(layoutDescriptor);

    pipelineLayoutDescriptor.label = "flixel sprite layout";
    pipelineLayoutDescriptor.bindGroupLayoutCount = 1;
    pipelineLayoutDescriptor.bindGroupLayouts[0] = globalsLayout;
    globalsPipelineLayout = device.createPipelineLayout(pipelineLayoutDescriptor);

    nearestSampler = createSampler(GpuFilterMode.NEAREST, "flixel nearest");
    linearSampler = createSampler(GpuFilterMode.LINEAR, "flixel linear");
  }

  private GpuSampler createSampler(GpuFilterMode filter, String label) {
    samplerDescriptor.reset();
    samplerDescriptor.label = label;
    samplerDescriptor.addressModeU = GpuAddressMode.CLAMP_TO_EDGE;
    samplerDescriptor.addressModeV = GpuAddressMode.CLAMP_TO_EDGE;
    samplerDescriptor.addressModeW = GpuAddressMode.CLAMP_TO_EDGE;
    samplerDescriptor.magFilter = filter;
    samplerDescriptor.minFilter = filter;
    samplerDescriptor.mipmapFilter = GpuMipmapFilterMode.NEAREST;
    return device.createSampler(samplerDescriptor);
  }

  /**
   * Creates the bind group for bind group index 0 of one texture: the dynamic uniform slot, the texture and the
   * sampler.
   *
   * @param uniforms The uniform ring buffer the globals are written into.
   * @param view The texture view.
   * @param smooth Whether to sample with linear filtering instead of nearest-neighbor.
   * @return The bind group, to be reused for every draw of that texture.
   */
  @NotNull
  public GpuBindGroup createTextureGroup(@NotNull GpuBuffer uniforms, @NotNull GpuTextureView view, boolean smooth) {
    groupDescriptor.reset();
    groupDescriptor.label = "flixel texture";
    groupDescriptor.layout = globalsLayout;
    groupDescriptor.entryCount = 3;
    groupDescriptor.entries[0].reset();
    groupDescriptor.entries[0].binding = 0;
    groupDescriptor.entries[0].buffer = uniforms;
    groupDescriptor.entries[0].offset = 0L;
    groupDescriptor.entries[0].size = FlixelWgsl.GLOBALS_SIZE;
    groupDescriptor.entries[1].reset();
    groupDescriptor.entries[1].binding = 1;
    groupDescriptor.entries[1].textureView = view;
    groupDescriptor.entries[2].reset();
    groupDescriptor.entries[2].binding = 2;
    groupDescriptor.entries[2].sampler = smooth ? linearSampler : nearestSampler;
    return device.createBindGroup(groupDescriptor);
  }

  /**
   * Compiles a shader file into a program.
   *
   * <p>The preamble is put in front of the source, the uniform struct is laid out, and the shader module and
   * layouts are built. The result is checked once with a trial pipeline inside a validation error scope; if the
   * GPU rejects the shader the error is logged and the program is marked invalid.
   *
   * @param source The text of the {@code .wgsl} shader file.
   * @param label A name for logs and GPU debug tools.
   * @param uniforms The uniform ring buffer that bind group 1 is created over.
   * @param pump Something that lets pending GPU callbacks run, or {@code null} when the platform delivers them
   *     on its own.
   * @param graphics The renderer that owns the program.
   * @return The program, which reports {@link FlixelGpuShaderProgram#isValid()} as {@code false} when it could
   *     not be built.
   */
  @Nullable
  public FlixelGpuShaderProgram compile(@NotNull String source, @NotNull String label, @NotNull GpuBuffer uniforms,
      @Nullable Runnable pump, @NotNull FlixelGpuGraphics graphics) {
    FlixelWgslLayout layout;
    String fullSource;
    try {
      layout = FlixelWgslLayout.parse(source);
      fullSource = FlixelWgsl.assemble(source);
    } catch (RuntimeException e) {
      LOG.error("Shader '{}' could not be read: {}", label, e.getMessage());
      return null;
    }
    int id = nextShaderId++;
    if (id >= FlixelPipelineKey.MAX_SHADER) {
      LOG.error("Too many shaders were created; '{}' is ignored.", label);
      return null;
    }
    String vertexEntry = FlixelWgsl.vertexEntryOf(source);
    String fragmentEntry = FlixelWgsl.fragmentEntryOf(source);
    if (!fragmentEntry.equals(FlixelWgsl.PREMULTIPLIED_FRAGMENT_ENTRY)) {
      LOG.warn("Shader '{}' does not have a plain '@fragment fn {}(...) -> @location(0) vec4f', so its output is"
          + " blended as straight color and some blend modes may show the color of transparent pixels.", label,
          FlixelWgsl.FRAGMENT_ENTRY);
    }
    device.pushErrorScope(GpuErrorFilter.VALIDATION);
    GpuShaderModule module = createModule(fullSource, label);
    FlixelGpuShaderProgram program = new FlixelGpuShaderProgram(graphics, id, module, vertexEntry, fragmentEntry,
        layout);
    GpuBindGroupLayout groupLayout = null;
    GpuBindGroup group = null;
    GpuPipelineLayout pipelineLayout = globalsPipelineLayout;
    if (!layout.isEmpty()) {
      layoutDescriptor.label = label + " uniforms";
      layoutDescriptor.entryCount = 1;
      layoutDescriptor.entries[0].reset();
      layoutDescriptor.entries[0].binding = FlixelWgsl.SHADER_BINDING;
      layoutDescriptor.entries[0].visibility = GpuShaderStage.VERTEX | GpuShaderStage.FRAGMENT;
      layoutDescriptor.entries[0].buffer.type = GpuBufferBindingType.UNIFORM;
      layoutDescriptor.entries[0].buffer.hasDynamicOffset = true;
      layoutDescriptor.entries[0].buffer.minBindingSize = layout.getSize();
      groupLayout = device.createBindGroupLayout(layoutDescriptor);
      pipelineLayoutDescriptor.label = label + " layout";
      pipelineLayoutDescriptor.bindGroupLayoutCount = 2;
      pipelineLayoutDescriptor.bindGroupLayouts[0] = globalsLayout;
      pipelineLayoutDescriptor.bindGroupLayouts[1] = groupLayout;
      pipelineLayout = device.createPipelineLayout(pipelineLayoutDescriptor);
      groupDescriptor.reset();
      groupDescriptor.label = label + " uniforms";
      groupDescriptor.layout = groupLayout;
      groupDescriptor.entryCount = 1;
      groupDescriptor.entries[0].reset();
      groupDescriptor.entries[0].binding = FlixelWgsl.SHADER_BINDING;
      groupDescriptor.entries[0].buffer = uniforms;
      groupDescriptor.entries[0].offset = 0L;
      groupDescriptor.entries[0].size = layout.getSize();
      group = device.createBindGroup(groupDescriptor);
    } else {
      globalsPipelineLayout.addRef();
    }
    program.attach(pipelineLayout, groupLayout, group);
    // Build the most common pipeline now: it forces the GPU to validate the shader, and it is needed anyway.
    GpuRenderPipeline trial = get(FlixelGpuBlend.NORMAL, GpuTextureFormat.RGBA8_UNORM, program);
    boolean[] finished = new boolean[1];
    device.popErrorScope(GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, type, message) -> {
      if (status == GpuPopErrorScopeStatus.SUCCESS && type != null && type != GpuErrorType.NO_ERROR) {
        LOG.error("Shader '{}' was rejected by the GPU: {}", label, message);
        program.invalidate();
      }
      finished[0] = true;
    });
    if (pump != null) {
      long deadline = System.nanoTime() + 2_000_000_000L;
      while (!finished[0] && System.nanoTime() < deadline) {
        pump.run();
      }
    }
    if (trial == null) {
      program.invalidate();
    }
    return program;
  }

  private GpuShaderModule createModule(String fullSource, String label) {
    wgsl.code = fullSource;
    moduleDescriptor.reset();
    moduleDescriptor.label = label;
    moduleDescriptor.nextInChain = wgsl;
    return device.createShaderModule(moduleDescriptor);
  }

  /**
   * Compiles the built-in sprite shader, which has id 0 and uses the shared bind group 0 layout only.
   *
   * @param source The text of the sprite shader file.
   * @param graphics The renderer that owns the program.
   * @return The sprite program.
   */
  @NotNull
  public FlixelGpuShaderProgram createSpriteProgram(@NotNull String source, @NotNull FlixelGpuGraphics graphics) {
    GpuShaderModule module = createModule(FlixelWgsl.assemble(source), "flixel sprite");
    FlixelGpuShaderProgram program = new FlixelGpuShaderProgram(graphics, 0, module, FlixelWgsl.DEFAULT_VERTEX_ENTRY,
        FlixelWgsl.fragmentEntryOf(source), FlixelWgslLayout.EMPTY);
    globalsPipelineLayout.addRef();
    program.attach(globalsPipelineLayout, null, null);
    return program;
  }

  /**
   * Returns the pipeline for a draw, building it on the first request.
   *
   * @param blend A blend id from {@link FlixelGpuBlend}.
   * @param format The format of the texture being drawn into.
   * @param program The shader program.
   * @return The pipeline, or {@code null} when the program has been destroyed.
   */
  @Nullable
  public GpuRenderPipeline get(int blend, @NotNull GpuTextureFormat format, @NotNull FlixelGpuShaderProgram program) {
    long key = FlixelPipelineKey.of(blend, format.ordinal(), program.getId());
    GpuRenderPipeline found = cache.get(key);
    if (found != null) {
      return found;
    }
    GpuShaderModule module = program.getModule();
    GpuPipelineLayout layout = program.getPipelineLayout();
    if (module == null || layout == null) {
      return null;
    }
    pipelineDescriptor.label = "flixel pipeline";
    pipelineDescriptor.layout = layout;
    pipelineDescriptor.vertex.module = module;
    pipelineDescriptor.vertex.entryPoint = program.getVertexEntry();
    fragment.module = module;
    fragment.entryPoint = program.getFragmentEntry();
    boolean premultiplied = program.isPremultiplied();
    // The darken switch only exists in the framework's premultiplying entry point.
    fragment.constantCount = premultiplied ? 1 : 0;
    fragment.constants[0].value = blend == FlixelGpuBlend.DARKEN ? 1.0 : 0.0;
    fragment.targets[0].format = format;
    fragment.targets[0].blend = FlixelGpuBlend.fill(blend, premultiplied, blendState) ? blendState : null;
    GpuRenderPipeline pipeline = device.createRenderPipeline(pipelineDescriptor);
    cache.put(key, pipeline);
    return pipeline;
  }

  /**
   * Releases every pipeline built for a shader, so a destroyed shader leaves nothing behind.
   *
   * @param shaderId The id of the shader.
   */
  public void forget(int shaderId) {
    for (int blend = 0; blend < FlixelPipelineKey.MAX_BLEND; blend++) {
      for (int format = 0; format < FlixelPipelineKey.MAX_FORMAT; format++) {
        GpuRenderPipeline removed = cache.remove(FlixelPipelineKey.of(blend, format, shaderId));
        if (removed != null) {
          removed.release();
        }
      }
    }
  }

  /** Releases everything this class created. */
  public void release() {
    for (GpuRenderPipeline pipeline : cache.values()) {
      pipeline.release();
    }
    cache.clear();
    nearestSampler.release();
    linearSampler.release();
    globalsPipelineLayout.release();
    globalsLayout.release();
  }

  /**
   * Returns the number of pipelines built so far.
   *
   * @return The cache size.
   */
  public int getPipelineCount() {
    return cache.getSize();
  }
}
