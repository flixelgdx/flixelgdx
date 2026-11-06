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

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugMessageQueueTest {

  @Test
  void messagesComeOutInTheOrderTheyWentIn() {
    FlixelDebugMessageQueue queue = new FlixelDebugMessageQueue(3, 4);
    assertTrue(queue.isEmpty());
    assertEquals(-1, queue.peekLength());
    assertTrue(queue.offer(new byte[] { 1, 2, 3 }, 0, 3));
    assertTrue(queue.offer(ByteBuffer.wrap(new byte[] { 9, 8 })));
    assertEquals(2, queue.getSize());
    assertEquals(3, queue.peekLength());

    byte[] out = new byte[8];
    assertEquals(3, queue.poll(out, 1));
    assertArrayEquals(new byte[] { 0, 1, 2, 3, 0 }, Arrays.copyOf(out, 5));
    ByteBuffer buffer = ByteBuffer.allocate(8);
    assertEquals(2, queue.poll(buffer));
    assertEquals(2, buffer.position());
    assertEquals(-1, queue.poll(out, 0));
  }

  @Test
  void aFullQueueRefusesMessages() {
    FlixelDebugMessageQueue queue = new FlixelDebugMessageQueue(2, 4);
    assertTrue(queue.offer(new byte[1], 0, 1));
    assertTrue(queue.offer(new byte[1], 0, 1));
    assertFalse(queue.offer(new byte[1], 0, 1));
    assertFalse(queue.offer(ByteBuffer.allocate(1)));
    queue.discard();
    assertTrue(queue.offer(new byte[1], 0, 1));
    assertEquals(2, queue.getCapacity());
    queue.clear();
    assertTrue(queue.isEmpty());
  }

  @Test
  void slotsGrowForBiggerMessagesAndWrapAround() {
    FlixelDebugMessageQueue queue = new FlixelDebugMessageQueue(2, 2);
    byte[] big = new byte[100];
    for (int i = 0; i < big.length; i++) {
      big[i] = (byte) i;
    }
    for (int round = 0; round < 5; round++) {
      assertTrue(queue.offer(big, 0, big.length));
      byte[] out = new byte[100];
      assertEquals(100, queue.poll(out, 0));
      assertArrayEquals(big, out);
    }
  }

  @Test
  void offeringABufferLeavesItsPositionAlone() {
    FlixelDebugMessageQueue queue = new FlixelDebugMessageQueue(1, 2);
    ByteBuffer direct = ByteBuffer.allocateDirect(4);
    direct.put(new byte[] { 5, 6, 7, 8 });
    direct.flip();
    direct.get();
    assertTrue(queue.offer(direct));
    assertEquals(1, direct.position());
    byte[] out = new byte[3];
    assertEquals(3, queue.poll(out, 0));
    assertArrayEquals(new byte[] { 6, 7, 8 }, out);
  }
}
