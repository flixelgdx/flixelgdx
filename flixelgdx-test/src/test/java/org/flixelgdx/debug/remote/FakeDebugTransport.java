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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** An in-memory {@link FlixelDebugTransport} that tests drive by hand. */
final class FakeDebugTransport implements FlixelDebugTransport {

  final List<byte[]> sent = new ArrayList<>();
  final ArrayDeque<byte[]> inbound = new ArrayDeque<>();

  String host;
  int port;
  int sentCount;
  boolean opened;
  boolean closed;
  boolean connected;
  boolean event;
  boolean accept = true;
  boolean keepSent = true;

  /** Pretends the app connected. */
  void connect() {
    connected = true;
    event = true;
  }

  /** Pretends the connection dropped. */
  void disconnect() {
    connected = false;
    inbound.clear();
  }

  /** Queues a message from the app. */
  void receive(byte[] message) {
    inbound.add(message);
  }

  /** Returns the sent messages and forgets them. */
  List<byte[]> drain() {
    List<byte[]> copy = new ArrayList<>(sent);
    sent.clear();
    return copy;
  }

  @Override
  public void open(@NotNull String host, int port) {
    this.host = host;
    this.port = port;
    opened = true;
  }

  @Override
  public boolean isConnected() {
    return connected && !closed;
  }

  @Override
  public boolean send(@NotNull ByteBuffer message) {
    if (!accept || !isConnected()) {
      return false;
    }
    sentCount++;
    if (keepSent) {
      byte[] copy = new byte[message.remaining()];
      message.get(copy, 0, copy.length);
      message.position(message.position() - copy.length);
      sent.add(copy);
    }
    return true;
  }

  @Override
  public int poll(@NotNull ByteBuffer into) {
    into.clear();
    byte[] next = inbound.poll();
    if (next == null) {
      return 0;
    }
    if (next.length > into.capacity()) {
      return -next.length;
    }
    into.put(next);
    into.flip();
    return next.length;
  }

  @Override
  public boolean consumeConnectedEvent() {
    boolean was = event;
    event = false;
    return was;
  }

  @Override
  public void close() {
    closed = true;
  }
}
