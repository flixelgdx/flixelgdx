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

/**
 * A bump allocator that hands out aligned byte ranges from a fixed-size ring and forgets them all at once.
 *
 * <p>The renderer writes per-frame data (vertices, uniforms) into one big GPU buffer. Because
 * {@code queue.writeBuffer(...)} takes effect before the commands submitted after it, every draw of a frame needs
 * its own slice, or a later write would overwrite an earlier draw's data. This class only does the arithmetic: it
 * tracks how many bytes are used and returns the byte offset for each request. It knows nothing about the GPU, so
 * it is cheap to unit test.
 *
 * <p>Think of a roll of paper on a table: each draw tears off the next strip, and at the start of the next frame the
 * roll is rewound. Asking for more strips than the roll has returns {@code -1}, and the caller decides what to do
 * (the renderer submits the frame so far and rewinds).
 */
public final class FlixelRingAllocator {

  private final long capacity;
  private long head;
  private long peak;

  /**
   * Creates an allocator for a buffer of the given size.
   *
   * @param capacity The buffer size in bytes.
   */
  public FlixelRingAllocator(long capacity) {
    this.capacity = capacity;
  }

  /**
   * Reserves a range.
   *
   * @param size The number of bytes needed.
   * @param alignment The required alignment of the returned offset in bytes. It must be a power of two.
   * @return The byte offset of the reserved range, or {@code -1} when the ring does not have enough room left.
   */
  public long allocate(long size, int alignment) {
    long start = (head + alignment - 1) & -(long) alignment;
    if (start + size > capacity) {
      return -1L;
    }
    head = start + size;
    if (head > peak) {
      peak = head;
    }
    return start;
  }

  /** Forgets every reservation so the whole ring can be used again. The peak usage is kept. */
  public void reset() {
    head = 0L;
  }

  /** Clears the recorded peak usage. */
  public void resetPeak() {
    peak = head;
  }

  /**
   * Returns the number of bytes reserved since the last {@link #reset()}, including alignment padding.
   *
   * @return The used byte count.
   */
  public long getUsed() {
    return head;
  }

  /**
   * Returns the largest value {@link #getUsed()} reached since the peak was last cleared.
   *
   * @return The peak byte count.
   */
  public long getPeak() {
    return peak;
  }

  /**
   * Returns the total size of the ring.
   *
   * @return The capacity in bytes.
   */
  public long getCapacity() {
    return capacity;
  }
}
