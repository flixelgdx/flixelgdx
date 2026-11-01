/**
 * The shared WebGPU renderer.
 *
 * <p>This package implements the framework's graphics abstractions once, on top of {@code jwebgpu-api}, so the
 * same code draws on every platform. It contains no desktop, Android or web types: a platform installs a jwebgpu
 * backend, creates a surface for its window, runs a {@link org.flixelgdx.backend.gpu.FlixelGpuBootstrap}, and hands
 * the device to {@link org.flixelgdx.backend.gpu.FlixelGpuGraphics#initialize}.
 *
 * <p>The shader contract (the preamble, bind group 0 and the shader's own uniform struct at group 1) is described
 * in {@link org.flixelgdx.graphics.wgsl.FlixelWgsl}.
 */
package org.flixelgdx.backend.gpu;
