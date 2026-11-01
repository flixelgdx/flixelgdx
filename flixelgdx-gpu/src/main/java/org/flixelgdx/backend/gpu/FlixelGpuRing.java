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

import org.jetbrains.annotations.NotNull;

import io.github.stringdotjar.jwebgpu.GpuBuffer;
import io.github.stringdotjar.jwebgpu.GpuBufferDescriptor;
import io.github.stringdotjar.jwebgpu.GpuBufferUsage;
import io.github.stringdotjar.jwebgpu.GpuDevice;
import io.github.stringdotjar.jwebgpu.GpuQueue;

import java.nio.ByteBuffer;

/**
 * One big GPU buffer that a frame fills from front to back, plus the bookkeeping that tells where the next free
 * byte is.
 *
 * <p>Every write goes through {@code queue.writeBuffer(...)}, which copies straight from Java arrays with no
 * allocation. Each write returns the byte offset it landed at, and the draw that follows binds that offset. The
 * ring is rewound once per frame (or earlier if it runs full; see {@link FlixelGpuGraphics}).
 *
 * <p>Because WebGPU orders queue writes with queue submits, the write for frame N + 1 cannot overtake the
 * commands of frame N, so one buffer is enough; there is no need to keep a buffer per frame in flight.
 */
public final class FlixelGpuRing {

  private final FlixelRingAllocator allocator;
  private final GpuBuffer buffer;

  /**
   * Creates the ring buffer on the device.
   *
   * @param device The device that owns the buffer.
   * @param usage A combination of {@code GpuBufferUsage} flags. {@code COPY_DST} is added automatically.
   * @param capacity The size of the buffer in bytes.
   * @param label A debug name for the buffer.
   */
  public FlixelGpuRing(@NotNull GpuDevice device, long usage, long capacity, @NotNull String label) {
    GpuBufferDescriptor d = new GpuBufferDescriptor();
    d.label = label;
    d.usage = usage | GpuBufferUsage.COPY_DST;
    d.size = capacity;
    GpuBuffer created = device.createBuffer(d);
    if (created == null) {
      throw new IllegalStateException("The GPU could not allocate the " + label + " (" + capacity + " bytes).");
    }
    this.buffer = created;
    this.allocator = new FlixelRingAllocator(capacity);
  }

  /**
   * Copies floats into the ring.
   *
   * @param queue The queue to write through.
   * @param data The source floats.
   * @param floatCount How many floats to copy from the start of {@code data}.
   * @param alignment The required alignment of the destination offset in bytes.
   * @return The byte offset the data was written to, or {@code -1} when the ring is full.
   */
  public long write(@NotNull GpuQueue queue, float @NotNull [] data, int floatCount, int alignment) {
    long at = allocator.allocate((long) floatCount * 4L, alignment);
    if (at >= 0L) {
      queue.writeBuffer(buffer, at, data, 0, floatCount);
    }
    return at;
  }

  /**
   * Copies bytes into the ring. The count must be a multiple of four, as WebGPU requires.
   *
   * @param queue The queue to write through.
   * @param data The source bytes.
   * @param byteCount How many bytes to copy from the start of {@code data}.
   * @param alignment The required alignment of the destination offset in bytes.
   * @return The byte offset the data was written to, or {@code -1} when the ring is full.
   */
  public long write(@NotNull GpuQueue queue, byte @NotNull [] data, int byteCount, int alignment) {
    long at = allocator.allocate(byteCount, alignment);
    if (at >= 0L) {
      queue.writeBuffer(buffer, at, data, 0, byteCount);
    }
    return at;
  }

  /**
   * Copies the remaining bytes of a buffer into the ring. The remaining count must be a multiple of four.
   *
   * @param queue The queue to write through.
   * @param data The source bytes, from its position to its limit.
   * @param alignment The required alignment of the destination offset in bytes.
   * @return The byte offset the data was written to, or {@code -1} when the ring is full.
   */
  public long write(@NotNull GpuQueue queue, @NotNull ByteBuffer data, int alignment) {
    long at = allocator.allocate(data.remaining(), alignment);
    if (at >= 0L) {
      queue.writeBuffer(buffer, at, data);
    }
    return at;
  }

  /** Rewinds the ring so the next frame writes from the start. */
  public void reset() {
    allocator.reset();
  }

  /** Releases the GPU buffer. */
  public void release() {
    buffer.release();
  }

  /**
   * Returns the GPU buffer, for binding.
   *
   * @return The buffer.
   */
  @NotNull
  public GpuBuffer getBuffer() {
    return buffer;
  }

  /**
   * Returns the allocator that tracks usage.
   *
   * @return The allocator.
   */
  @NotNull
  public FlixelRingAllocator getAllocator() {
    return allocator;
  }
}
