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
package org.flixelgdx.debug.remote;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * A bounded first-in first-out queue of messages that stores each one in a reusable {@code byte[]} slot.
 *
 * <p>Think of a coat check with a fixed number of hooks. Every message hangs on the next free hook, and the
 * hook keeps its size: it only grows when a bigger message arrives. Once the queue has seen the biggest message
 * it will ever carry, putting a message in and taking it out allocates nothing at all. When every hook is
 * taken, {@link #offer(byte[], int, int)} says no instead of growing, so the caller can drop data that is not
 * important.
 *
 * <p>Transports use it as the hand-off point between the game thread and a network thread (or a browser
 * callback). The queue itself does <strong>not</strong> lock anything. A transport with two threads must hold
 * one lock around every call, and keep slow work such as socket writes outside that lock.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugMessageQueue queue = new FlixelDebugMessageQueue(32, 1024);
 * synchronized (lock) {
 *   if (!queue.offer(message)) {
 *     dropped++;
 *   }
 * }
 * synchronized (lock) {
 *   int length = queue.peekLength();
 *   if (length >= 0) {
 *     queue.poll(scratch, 0);
 *   }
 * }
 * }</pre>
 */
public final class FlixelDebugMessageQueue {

  private final byte[][] slots;
  private final int[] lengths;
  private int head;
  private int size;

  /**
   * Creates a queue.
   *
   * @param capacity How many messages it holds at most.
   * @param initialSlotBytes The starting size of every slot. A slot grows when a bigger message needs it.
   */
  public FlixelDebugMessageQueue(int capacity, int initialSlotBytes) {
    slots = new byte[capacity][];
    lengths = new int[capacity];
    for (int i = 0; i < capacity; i++) {
      slots[i] = new byte[Math.max(1, initialSlotBytes)];
    }
  }

  /**
   * Adds a message, copying the bytes between the buffer's position and its limit. The buffer's position is left
   * as it was.
   *
   * @param message The message.
   * @return {@code true} when added, {@code false} when the queue is full.
   */
  public boolean offer(@NotNull ByteBuffer message) {
    if (size == slots.length) {
      return false;
    }
    int length = message.remaining();
    int slot = (head + size) % slots.length;
    byte[] bytes = fit(slot, length);
    if (message.hasArray()) {
      System.arraycopy(message.array(), message.arrayOffset() + message.position(), bytes, 0, length);
    } else {
      int position = message.position();
      message.get(bytes, 0, length);
      message.position(position);
    }
    lengths[slot] = length;
    size++;
    return true;
  }

  /**
   * Adds a message copied from an array.
   *
   * @param data The array that holds the message.
   * @param offset Where the message starts in the array.
   * @param length The length of the message.
   * @return {@code true} when added, {@code false} when the queue is full.
   */
  public boolean offer(byte @NotNull [] data, int offset, int length) {
    if (size == slots.length) {
      return false;
    }
    int slot = (head + size) % slots.length;
    System.arraycopy(data, offset, fit(slot, length), 0, length);
    lengths[slot] = length;
    size++;
    return true;
  }

  /**
   * Returns the length of the oldest message without removing it.
   *
   * @return The length in bytes, or {@code -1} when the queue is empty.
   */
  public int peekLength() {
    return size == 0 ? -1 : lengths[head];
  }

  /**
   * Removes the oldest message and copies it into an array.
   *
   * @param dst The array to fill. It must have room for {@link #peekLength()} bytes at {@code offset}.
   * @param offset Where to start writing in the array.
   * @return The length of the message, or {@code -1} when the queue is empty.
   */
  public int poll(byte @NotNull [] dst, int offset) {
    if (size == 0) {
      return -1;
    }
    int length = lengths[head];
    System.arraycopy(slots[head], 0, dst, offset, length);
    discard();
    return length;
  }

  /**
   * Removes the oldest message and copies it into a buffer at the buffer's position, advancing the position.
   *
   * @param dst The buffer to fill. It must have room for {@link #peekLength()} bytes.
   * @return The length of the message, or {@code -1} when the queue is empty.
   */
  public int poll(@NotNull ByteBuffer dst) {
    if (size == 0) {
      return -1;
    }
    int length = lengths[head];
    dst.put(slots[head], 0, length);
    discard();
    return length;
  }

  /** Removes the oldest message without copying it. Does nothing when the queue is empty. */
  public void discard() {
    if (size > 0) {
      head = (head + 1) % slots.length;
      size--;
    }
  }

  /** Removes every message. The slots keep their size. */
  public void clear() {
    head = 0;
    size = 0;
  }

  private byte[] fit(int slot, int length) {
    byte[] bytes = slots[slot];
    if (bytes.length < length) {
      bytes = new byte[Math.max(length, bytes.length * 2)];
      slots[slot] = bytes;
    }
    return bytes;
  }

  /**
   * Returns how many messages are waiting.
   *
   * @return The count.
   */
  public int getSize() {
    return size;
  }

  /**
   * Returns how many messages fit.
   *
   * @return The capacity.
   */
  public int getCapacity() {
    return slots.length;
  }

  /**
   * Returns whether no message is waiting.
   *
   * @return {@code true} when empty.
   */
  public boolean isEmpty() {
    return size == 0;
  }
}
