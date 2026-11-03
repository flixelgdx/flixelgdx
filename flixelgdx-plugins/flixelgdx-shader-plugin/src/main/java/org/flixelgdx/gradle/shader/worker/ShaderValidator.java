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
package org.flixelgdx.gradle.shader.worker;

import io.github.stringdotjar.jwebgpu.GpuAdapter;
import io.github.stringdotjar.jwebgpu.GpuBackendType;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayout;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayoutDescriptor;
import io.github.stringdotjar.jwebgpu.GpuBindGroupLayoutEntry;
import io.github.stringdotjar.jwebgpu.GpuBufferBindingType;
import io.github.stringdotjar.jwebgpu.GpuCallbackMode;
import io.github.stringdotjar.jwebgpu.GpuColorWriteMask;
import io.github.stringdotjar.jwebgpu.GpuCompilationMessage;
import io.github.stringdotjar.jwebgpu.GpuCompilationMessageType;
import io.github.stringdotjar.jwebgpu.GpuCullMode;
import io.github.stringdotjar.jwebgpu.GpuDevice;
import io.github.stringdotjar.jwebgpu.GpuDeviceDescriptor;
import io.github.stringdotjar.jwebgpu.GpuErrorFilter;
import io.github.stringdotjar.jwebgpu.GpuErrorType;
import io.github.stringdotjar.jwebgpu.GpuFragmentState;
import io.github.stringdotjar.jwebgpu.GpuFrontFace;
import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.GpuPipelineLayout;
import io.github.stringdotjar.jwebgpu.GpuPipelineLayoutDescriptor;
import io.github.stringdotjar.jwebgpu.GpuPopErrorScopeStatus;
import io.github.stringdotjar.jwebgpu.GpuPrimitiveTopology;
import io.github.stringdotjar.jwebgpu.GpuRenderPipeline;
import io.github.stringdotjar.jwebgpu.GpuRenderPipelineDescriptor;
import io.github.stringdotjar.jwebgpu.GpuRequestAdapterOptions;
import io.github.stringdotjar.jwebgpu.GpuSamplerBindingType;
import io.github.stringdotjar.jwebgpu.GpuShaderModule;
import io.github.stringdotjar.jwebgpu.GpuShaderModuleDescriptor;
import io.github.stringdotjar.jwebgpu.GpuShaderSourceWGSL;
import io.github.stringdotjar.jwebgpu.GpuShaderStage;
import io.github.stringdotjar.jwebgpu.GpuTextureFormat;
import io.github.stringdotjar.jwebgpu.GpuTextureSampleType;
import io.github.stringdotjar.jwebgpu.GpuTextureViewDimension;
import io.github.stringdotjar.jwebgpu.GpuVertexAttribute;
import io.github.stringdotjar.jwebgpu.GpuVertexBufferLayout;
import io.github.stringdotjar.jwebgpu.GpuVertexFormat;
import io.github.stringdotjar.jwebgpu.GpuVertexStepMode;
import io.github.stringdotjar.jwebgpu.JWebGpu;
import io.github.stringdotjar.jwebgpu.ffm.JWebGpuFfm;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Compiles FlixelGDX shaders with Dawn, the same compiler the game uses at run time, and reports what it finds.
 *
 * <p>One validator brings up one Dawn device on the <em>Null</em> backend, a fake GPU that compiles and validates
 * shaders but never draws, so no graphics card or display is needed (it works on CI machines). That device is
 * reused for every shader of a run.
 *
 * <p>Each shader goes through the same two checks the renderer performs when it creates a shader:
 *
 * <ol>
 *   <li>The preamble and the file are joined exactly like the framework joins them, compiled as one module inside a
 *       validation error scope, and the compiler's messages are read. Every message is mapped back to the user's
 *       file by subtracting the preamble's lines.</li>
 *   <li>A trial render pipeline is built with the framework's explicit bind group layouts (group 0: the globals
 *       uniform, the texture and the sampler; group 1: the shader's own uniform buffer), the framework's vertex
 *       layout and the entry points {@code fs_main} and {@code vs_main} (or the preamble's {@code flixel_vs}). This
 *       catches what compiling alone cannot: a missing entry point, or a binding that does not match the layout.</li>
 * </ol>
 *
 * <p>The layouts are built from the same numbers the renderer uses (read from the contract), and the vertex layout
 * follows the {@code FlixelVertexInput} struct of the preamble: a {@code vec2f} position, a {@code vec2f} texture
 * coordinate and a {@code unorm8x4} color, packed into 20 bytes.
 */
final class ShaderValidator implements AutoCloseable {

  private static final long TIMEOUT_NANOS = 20_000_000_000L;
  private static final int VERTEX_STRIDE = 20;

  private final Contract contract;
  private final GpuInstance instance;
  private final GpuAdapter adapter;
  private final GpuDevice device;
  private final GpuBindGroupLayout[] groupLayouts;
  private final GpuPipelineLayout pipelineLayout;

  private final GpuShaderSourceWGSL wgsl = new GpuShaderSourceWGSL();
  private final GpuShaderModuleDescriptor moduleDescriptor = new GpuShaderModuleDescriptor();
  private final GpuRenderPipelineDescriptor pipelineDescriptor = new GpuRenderPipelineDescriptor();
  private final GpuFragmentState fragment = new GpuFragmentState(0, 1);
  private final GpuVertexBufferLayout vertexLayout = new GpuVertexBufferLayout(3);

  /**
   * Brings up the Dawn device and the shared layouts.
   *
   * @param contract The shader contract to validate against.
   * @throws IllegalStateException if Dawn cannot be loaded or the device cannot be created.
   */
  ShaderValidator(Contract contract) {
    this.contract = contract;
    try {
      JWebGpuFfm.install();
    } catch (IllegalStateException | UnsatisfiedLinkError e) {
      throw new IllegalStateException("Dawn could not be loaded: " + e.getMessage(), e);
    }
    instance = JWebGpu.createInstance();
    adapter = requestAdapter();
    device = requestDevice();
    groupLayouts = createGroupLayouts();
    pipelineLayout = createPipelineLayout();
    fillPipelineDescriptor();
  }

  /**
   * Checks one shader and appends what it finds.
   *
   * @param path The path to show in messages (the user's file).
   * @param source The text of the user's file.
   * @param out The list that receives the findings.
   */
  void validate(String path, String source, List<Diagnostic> out) {
    boolean ok = compile(path, source, out);
    if (!ok) {
      return;
    }
    // A fresh module is built for the pipeline check; the first one only existed to read its messages.
    device.pushErrorScope(GpuErrorFilter.VALIDATION);
    GpuShaderModule module = createModule(contract.assemble(source), path);
    String vertexEntry = SourceText.definesFunction(source, contract.vertexEntry)
        ? contract.vertexEntry : contract.defaultVertexEntry;
    pipelineDescriptor.label = path;
    pipelineDescriptor.vertex.module = module;
    pipelineDescriptor.vertex.entryPoint = vertexEntry;
    fragment.module = module;
    GpuRenderPipeline pipeline = device.createRenderPipeline(pipelineDescriptor);
    String problem = popScope();
    if (problem != null) {
      out.add(new Diagnostic(true, SourceText.format(path, 0, 0, "error", clean(problem), source)));
    }
    pipeline.release();
    module.release();
  }

  @Override
  public void close() {
    pipelineLayout.release();
    for (GpuBindGroupLayout layout : groupLayouts) {
      layout.release();
    }
    device.release();
    adapter.release();
    instance.release();
  }

  private boolean compile(String path, String source, List<Diagnostic> out) {
    device.pushErrorScope(GpuErrorFilter.VALIDATION);
    GpuShaderModule module = createModule(contract.assemble(source), path);
    boolean[] done = new boolean[1];
    int before = out.size();
    module.getCompilationInfo(GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, info) -> {
      for (int i = 0; i < info.messageCount; i++) {
        report(path, source, info.messages[i], out);
      }
      done[0] = true;
    });
    await(() -> done[0]);
    String problem = popScope();
    module.release();

    boolean failed = false;
    for (int i = before; i < out.size(); i++) {
      failed |= out.get(i).isError();
    }
    if (!failed && problem != null) {
      // The compiler had no message to point at, but the device still rejected the module.
      out.add(new Diagnostic(true, SourceText.format(path, 0, 0, "error", clean(problem), source)));
      failed = true;
    }
    return !failed;
  }

  private void report(String path, String source, GpuCompilationMessage message, List<Diagnostic> out) {
    GpuCompilationMessageType type = message.type;
    boolean error = type == GpuCompilationMessageType.ERROR || type == null;
    String severity = error ? "error" : type == GpuCompilationMessageType.WARNING ? "warning" : "note";
    String text = message.message == null ? "unknown problem" : message.message;
    int assembledLine = (int) message.lineNum;
    int line = assembledLine - contract.preambleLines;
    if (assembledLine <= 0) {
      out.add(new Diagnostic(error, SourceText.format(path, 0, 0, severity, text, source)));
    } else if (line < 1) {
      // The message points into the framework's preamble, usually a note attached to a clash with its names.
      out.add(new Diagnostic(error, path + ": " + severity + ": " + text + " (in the framework preamble, line "
          + assembledLine + ":" + message.linePos + ")"));
    } else {
      int column = SourceText.charColumn(SourceText.lineOf(source, line), message.linePos);
      out.add(new Diagnostic(error, SourceText.format(path, line, column, severity, text, source)));
    }
  }

  private GpuShaderModule createModule(String full, String label) {
    wgsl.code = full;
    moduleDescriptor.reset();
    moduleDescriptor.label = label;
    moduleDescriptor.nextInChain = wgsl;
    return device.createShaderModule(moduleDescriptor);
  }

  private String popScope() {
    String[] problem = new String[1];
    boolean[] done = new boolean[1];
    device.popErrorScope(GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, type, message) -> {
      if (status == GpuPopErrorScopeStatus.SUCCESS && type != null && type != GpuErrorType.NO_ERROR) {
        problem[0] = message == null ? type.toString() : message;
      }
      done[0] = true;
    });
    await(() -> done[0]);
    return problem[0];
  }

  private static String clean(String message) {
    StringBuilder out = new StringBuilder();
    for (String line : message.strip().split("\\R")) {
      // Dawn appends a call trace ("- While validating ...") that only talks about the validator's own objects.
      if (line.strip().startsWith("- While")) {
        continue;
      }
      if (out.length() > 0) {
        out.append('\n');
      }
      out.append(line.replace("\"\"", "\"").replaceAll("\\s*\\[ShaderModule \"[^\"]*\"\\]", ""));
    }
    return out.toString();
  }

  private GpuAdapter requestAdapter() {
    GpuRequestAdapterOptions options = new GpuRequestAdapterOptions();
    options.backendType = GpuBackendType.NULL;
    GpuAdapter[] result = new GpuAdapter[1];
    String[] why = new String[1];
    boolean[] done = new boolean[1];
    instance.requestAdapter(options, GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, found, message) -> {
      result[0] = found;
      why[0] = status + " " + message;
      done[0] = true;
    });
    await(() -> done[0]);
    if (result[0] == null) {
      throw new IllegalStateException("Dawn gave no adapter on its Null backend (" + why[0] + ").");
    }
    return result[0];
  }

  private GpuDevice requestDevice() {
    GpuDeviceDescriptor descriptor = new GpuDeviceDescriptor();
    descriptor.label = "flixel shader validator";
    descriptor.deviceLostMode = GpuCallbackMode.ALLOW_SPONTANEOUS;
    descriptor.deviceLostCallback = (lost, reason, message) -> {
    };
    // Validation errors are caught by the error scopes around each check; anything that escapes is not ours to show.
    descriptor.uncapturedErrorCallback = (source, type, message) -> {
    };
    GpuDevice[] result = new GpuDevice[1];
    String[] why = new String[1];
    boolean[] done = new boolean[1];
    adapter.requestDevice(descriptor, GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, found, message) -> {
      result[0] = found;
      why[0] = status + " " + message;
      done[0] = true;
    });
    await(() -> done[0]);
    if (result[0] == null) {
      throw new IllegalStateException("Dawn gave no device on its Null backend (" + why[0] + ").");
    }
    return result[0];
  }

  private GpuBindGroupLayout[] createGroupLayouts() {
    GpuBindGroupLayout[] layouts = new GpuBindGroupLayout[Math.max(contract.frameworkGroup, contract.shaderGroup) + 1];
    GpuBindGroupLayoutDescriptor descriptor = new GpuBindGroupLayoutDescriptor();

    descriptor.label = "flixel globals";
    descriptor.entryCount = 3;
    GpuBindGroupLayoutEntry[] e = descriptor.entries;
    e[0].reset();
    e[0].binding = 0;
    e[0].visibility = GpuShaderStage.VERTEX | GpuShaderStage.FRAGMENT;
    e[0].buffer.type = GpuBufferBindingType.UNIFORM;
    e[0].buffer.hasDynamicOffset = true;
    e[0].buffer.minBindingSize = contract.globalsSize;
    e[1].reset();
    e[1].binding = 1;
    e[1].visibility = GpuShaderStage.FRAGMENT;
    e[1].texture.sampleType = GpuTextureSampleType.FLOAT;
    e[1].texture.viewDimension = GpuTextureViewDimension.D2;
    e[2].reset();
    e[2].binding = 2;
    e[2].visibility = GpuShaderStage.FRAGMENT;
    e[2].sampler.type = GpuSamplerBindingType.FILTERING;
    layouts[contract.frameworkGroup] = device.createBindGroupLayout(descriptor);

    // The renderer sizes this entry from the shader's own struct. The validator leaves the minimum open, so the
    // layout accepts any size and the shader's own struct decides.
    descriptor.reset();
    descriptor.label = "flixel shader uniforms";
    descriptor.entryCount = 1;
    descriptor.entries[0].reset();
    descriptor.entries[0].binding = contract.shaderBinding;
    descriptor.entries[0].visibility = GpuShaderStage.VERTEX | GpuShaderStage.FRAGMENT;
    descriptor.entries[0].buffer.type = GpuBufferBindingType.UNIFORM;
    descriptor.entries[0].buffer.hasDynamicOffset = true;
    layouts[contract.shaderGroup] = device.createBindGroupLayout(descriptor);
    return layouts;
  }

  private GpuPipelineLayout createPipelineLayout() {
    GpuPipelineLayoutDescriptor descriptor = new GpuPipelineLayoutDescriptor();
    descriptor.label = "flixel shader layout";
    descriptor.bindGroupLayoutCount = groupLayouts.length;
    for (int i = 0; i < groupLayouts.length; i++) {
      descriptor.bindGroupLayouts[i] = groupLayouts[i];
    }
    return device.createPipelineLayout(descriptor);
  }

  private void fillPipelineDescriptor() {
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
    vertexLayout.arrayStride = VERTEX_STRIDE;
    vertexLayout.stepMode = GpuVertexStepMode.VERTEX;

    pipelineDescriptor.layout = pipelineLayout;
    pipelineDescriptor.vertex.bufferCount = 1;
    pipelineDescriptor.vertex.buffers[0] = vertexLayout;
    pipelineDescriptor.primitive.topology = GpuPrimitiveTopology.TRIANGLE_LIST;
    pipelineDescriptor.primitive.frontFace = GpuFrontFace.CCW;
    pipelineDescriptor.primitive.cullMode = GpuCullMode.NONE;
    fragment.entryPoint = contract.fragmentEntry;
    fragment.targetCount = 1;
    fragment.targets[0].format = GpuTextureFormat.RGBA8_UNORM;
    fragment.targets[0].writeMask = GpuColorWriteMask.ALL;
    pipelineDescriptor.fragment = fragment;
  }

  private void await(BooleanSupplier done) {
    long deadline = System.nanoTime() + TIMEOUT_NANOS;
    while (!done.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new IllegalStateException("Timed out waiting for Dawn.");
      }
      instance.processEvents();
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for Dawn.", e);
      }
    }
  }
}
