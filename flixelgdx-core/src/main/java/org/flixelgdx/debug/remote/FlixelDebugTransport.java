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

import org.flixelgdx.debug.FlixelDebugManager;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * The pipe between the game and the debugger app: it carries whole binary messages in both directions.
 *
 * <p>Think of a mail slot in a door. The game drops a finished message into the outgoing slot and
 * picks up whatever the other side pushed through the incoming slot. Everything behind the door (opening a
 * socket, the WebSocket handshake, waiting for a slow network, trying again after a failure) is the
 * transport's business, so {@link FlixelDebugAgent} stays plain Java that runs on every platform, including the
 * web. Core never touches {@code java.net}: a platform module supplies the transport and installs it through
 * {@link FlixelDebugManager#defaultTransport}.
 *
 * <h2>Threading contract</h2>
 *
 * <p>Every method is called from the game thread, and only from there. An implementation that needs a
 * network thread starts and owns it itself and hands messages across with its own locks, so the game thread never
 * blocks on the network. Methods must return quickly and must not allocate once the transport is warmed up.
 *
 * <h2>Messages</h2>
 *
 * <p>One transport message is one protocol message, as described in {@code DEBUG_PROTOCOL.md}. The transport
 * does not look inside them.
 *
 * <p>Example of a transport that is driven by hand:
 *
 * <pre>{@code
 * transport.open("127.0.0.1", FlixelDebugProtocol.DEFAULT_PORT);
 * if (transport.consumeConnectedEvent()) {
 *   transport.send(helloMessage);
 * }
 * receiveBuffer.clear();
 * int length = transport.poll(receiveBuffer);
 * if (length > 0) {
 *   // receiveBuffer holds one message, ready to read.
 * }
 * }</pre>
 *
 * @see FlixelNoopDebugTransport
 * @see FlixelDebugAgent
 */
public interface FlixelDebugTransport {

  /**
   * Starts connecting to the debugger app and keeps trying until {@link #close()}.
   *
   * <p>This returns at once. Connecting happens in the background, and a lost connection is retried every
   * {@link FlixelDebugProtocol#RECONNECT_MILLIS} milliseconds. Calling it again while the transport is open
   * does nothing.
   *
   * @param host The host name or address of the debugger app.
   * @param port The port of the debugger app.
   */
  void open(@NotNull String host, int port);

  /**
   * Returns whether the transport is connected and ready to carry messages.
   *
   * @return {@code true} while connected.
   */
  boolean isConnected();

  /**
   * Queues one message for sending.
   *
   * <p>The bytes between the buffer's position and its limit are copied into a slot the transport owns and
   * reuses, so the caller may reuse the buffer right away. The buffer's position is left as it was. A message is
   * dropped instead of blocking when the outgoing queue is full, so a slow network never freezes the game. The
   * caller can then skip data that is not critical.
   *
   * @param message The message to send.
   * @return {@code true} when the message was queued, {@code false} when it was dropped because the queue is
   *   full or the transport is not connected.
   */
  boolean send(@NotNull ByteBuffer message);

  /**
   * Copies the next received message into the buffer.
   *
   * <p>The buffer is cleared first. After a successful call it is flipped, so it is ready to read: its position
   * is {@code 0} and its limit is the returned length. Call this in a loop until it returns {@code 0} to drain
   * the messages that arrived since the last frame.
   *
   * @param into A buffer with room for the largest message the caller accepts.
   * @return The length of the message in bytes, {@code 0} when no message is waiting, or the negative length
   *   ({@code -n}) of a message that is larger than the buffer's capacity. Such a message is discarded and the
   *   buffer is left empty.
   */
  int poll(@NotNull ByteBuffer into);

  /**
   * Returns whether a new connection was made since the last call, then forgets it.
   *
   * <p>A connection that dropped and came back between two calls still reports {@code true}, once, so the
   * caller always knows when to start the handshake over.
   *
   * @return {@code true} once after each new connection.
   */
  boolean consumeConnectedEvent();

  /**
   * Closes the connection and stops reconnecting. The transport cannot be reused afterward.
   */
  void close();
}
