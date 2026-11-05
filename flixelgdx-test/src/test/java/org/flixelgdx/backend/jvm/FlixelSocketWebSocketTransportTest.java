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
package org.flixelgdx.backend.jvm;

import org.flixelgdx.Flixel;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelSocketWebSocketTransportTest {

  private static final long WAIT = 5000L;

  private FlixelLogger originalLog;
  private TestWebSocketServer server;
  private FlixelSocketWebSocketTransport transport;

  @BeforeEach
  void setUp() throws IOException {
    originalLog = Flixel.log;
    FlixelDefaultLogger quiet = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    quiet.setConsoleSink(null);
    Flixel.log = quiet;
    server = new TestWebSocketServer(0);
    transport = new FlixelSocketWebSocketTransport();
    transport.setReconnectMillis(100);
  }

  @AfterEach
  void tearDown() throws IOException {
    transport.close();
    server.close();
    Flixel.log = originalLog;
  }

  private static boolean await(BooleanSupplier condition) throws InterruptedException {
    long end = System.currentTimeMillis() + WAIT;
    while (System.currentTimeMillis() < end) {
      if (condition.getAsBoolean()) {
        return true;
      }
      Thread.sleep(5);
    }
    return condition.getAsBoolean();
  }

  private void connect() throws InterruptedException {
    transport.open("127.0.0.1", server.port());
    assertTrue(await(transport::isConnected), "the transport connects");
  }

  private byte[] receive() throws InterruptedException {
    ByteBuffer buffer = ByteBuffer.allocate(1 << 20);
    long end = System.currentTimeMillis() + WAIT;
    while (System.currentTimeMillis() < end) {
      int length = transport.poll(buffer);
      if (length > 0) {
        byte[] out = new byte[length];
        buffer.get(out);
        return out;
      }
      Thread.sleep(5);
    }
    return null;
  }

  private static byte[] pattern(int length) {
    byte[] data = new byte[length];
    for (int i = 0; i < length; i++) {
      data[i] = (byte) (i * 31 + 7);
    }
    return data;
  }

  @Test
  void connectsAndReportsTheConnectionEventOnce() throws Exception {
    assertFalse(transport.isConnected());
    connect();
    assertTrue(transport.consumeConnectedEvent());
    assertFalse(transport.consumeConnectedEvent());
    assertEquals(1, server.connections);
    assertEquals(0, server.unmaskedFrames);
  }

  @Test
  void aWrongAcceptKeyIsRejectedAndRetried() throws Exception {
    server.badAccept = true;
    transport.open("127.0.0.1", server.port());
    Thread.sleep(500);
    assertFalse(transport.isConnected());
    assertFalse(transport.consumeConnectedEvent(), "the handshake never completed");

    server.badAccept = false;
    assertTrue(await(transport::isConnected), "it connects once the server is correct");
  }

  @Test
  void binaryMessagesTravelBothWays() throws Exception {
    connect();
    for (int size : new int[] { 1, 125, 126, 300, 65535, 65536, 400_000 }) {
      byte[] up = pattern(size);
      assertTrue(transport.send(ByteBuffer.wrap(up)));
      assertArrayEquals(up, server.awaitBinary(WAIT), "client to server, " + size + " bytes");

      byte[] down = pattern(size + 3);
      server.sendBinary(down);
      assertArrayEquals(down, receive(), "server to client, " + (size + 3) + " bytes");
    }
    assertEquals(0, server.unmaskedFrames, "a client must mask every frame");
  }

  @Test
  void sendCopiesTheBytesAndLeavesTheBufferAlone() throws Exception {
    connect();
    ByteBuffer message = ByteBuffer.wrap(new byte[] { 9, 8, 7, 6 });
    message.position(1);
    assertTrue(transport.send(message));
    assertEquals(1, message.position());
    assertArrayEquals(new byte[] { 8, 7, 6 }, server.awaitBinary(WAIT));
  }

  @Test
  void messagesKeepTheirOrder() throws Exception {
    connect();
    for (int i = 0; i < 40; i++) {
      assertTrue(transport.send(ByteBuffer.wrap(new byte[] { (byte) i })));
    }
    for (int i = 0; i < 40; i++) {
      assertArrayEquals(new byte[] { (byte) i }, server.awaitBinary(WAIT));
    }
    for (int i = 0; i < 40; i++) {
      server.sendBinary(new byte[] { (byte) i, 1 });
    }
    for (int i = 0; i < 40; i++) {
      assertArrayEquals(new byte[] { (byte) i, 1 }, receive());
    }
  }

  @Test
  void fragmentedMessagesAreJoinedEvenWithAPingInTheMiddle() throws Exception {
    connect();
    byte[] whole = pattern(1000);
    byte[] a = Arrays.copyOfRange(whole, 0, 100);
    byte[] b = Arrays.copyOfRange(whole, 100, 700);
    byte[] c = Arrays.copyOfRange(whole, 700, 1000);
    server.sendFrame(0x02, a);
    server.sendPing(new byte[] { 1, 2, 3 });
    server.sendFrame(0x00, b);
    server.sendFrame(0x80, c);

    assertArrayEquals(whole, receive());
    assertArrayEquals(new byte[] { 1, 2, 3 }, server.pongs.poll(WAIT, TimeUnit.MILLISECONDS));
  }

  @Test
  void textFramesAreIgnored() throws Exception {
    connect();
    server.sendText("hello");
    server.sendFrame(0x01, new byte[] { 'a' });
    server.sendFrame(0x80, new byte[] { 'b' });
    server.sendBinary(new byte[] { 42 });

    assertArrayEquals(new byte[] { 42 }, receive());
    ByteBuffer buffer = ByteBuffer.allocate(16);
    assertEquals(0, transport.poll(buffer));
  }

  @Test
  void aPingGetsAPongWithTheSamePayload() throws Exception {
    connect();
    server.sendPing(new byte[0]);
    server.sendPing(pattern(125));

    assertArrayEquals(new byte[0], server.pongs.poll(WAIT, TimeUnit.MILLISECONDS));
    assertArrayEquals(pattern(125), server.pongs.poll(WAIT, TimeUnit.MILLISECONDS));
  }

  @Test
  void aServerCloseIsAnsweredAndTheTransportReconnects() throws Exception {
    connect();
    assertTrue(transport.consumeConnectedEvent());
    server.sendClose(1001);

    assertEquals(1001, server.closeCodes.poll(WAIT, TimeUnit.MILLISECONDS));
    assertTrue(await(() -> server.connections >= 2), "it dials again");
    assertTrue(await(transport::isConnected));
    assertTrue(await(transport::consumeConnectedEvent), "the new connection is announced");
  }

  @Test
  void aDroppedConnectionIsNoticedAndRestored() throws Exception {
    connect();
    server.dropClient();
    assertTrue(await(() -> server.connections >= 2));
    assertTrue(await(transport::isConnected));
  }

  @Test
  void theTransportReconnectsAfterTheServerRestarts() throws Exception {
    connect();
    int port = server.port();
    server.close();
    assertTrue(await(() -> !transport.isConnected()), "the loss is noticed");
    assertFalse(transport.send(ByteBuffer.wrap(new byte[1])), "nothing can be sent while disconnected");

    Thread.sleep(300);
    server = new TestWebSocketServer(port);
    assertTrue(await(transport::isConnected), "connected to the restarted server");
    assertTrue(transport.consumeConnectedEvent());
    assertTrue(transport.send(ByteBuffer.wrap(new byte[] { 5 })));
    assertArrayEquals(new byte[] { 5 }, server.awaitBinary(WAIT));
  }

  @Test
  void closeSendsACloseFrameAndStopsReconnecting() throws Exception {
    connect();
    transport.close();

    assertEquals(1000, server.closeCodes.poll(WAIT, TimeUnit.MILLISECONDS));
    assertFalse(transport.isConnected());
    int connections = server.connections;
    Thread.sleep(400);
    assertEquals(connections, server.connections, "no reconnect after close()");
    transport.close();
  }

  @Test
  void aMessageOverTheLimitClosesTheConnectionWith1009() throws Exception {
    connect();
    server.sendHugeHeader(FlixelSocketWebSocketTransport.MAX_MESSAGE_BYTES + 1L);

    assertEquals(1009, server.closeCodes.poll(WAIT, TimeUnit.MILLISECONDS));
    assertTrue(await(() -> server.connections >= 2), "it reconnects afterward");
  }

  @Test
  void aMessageThatDoesNotFitTheCallersBufferIsDiscardedWithItsNegativeLength() throws Exception {
    connect();
    server.sendBinary(pattern(5000));
    server.sendBinary(new byte[] { 1, 2 });
    ByteBuffer small = ByteBuffer.allocate(100);
    long end = System.currentTimeMillis() + WAIT;
    int length = 0;
    while (length == 0 && System.currentTimeMillis() < end) {
      length = transport.poll(small);
      Thread.sleep(5);
    }
    assertEquals(-5000, length);
    assertEquals(1, transport.getDroppedIn());
    assertEquals(2, transport.poll(small));
    assertEquals(2, small.limit());
    assertEquals(0, small.position());
  }

  @Test
  void aFullOutgoingQueueMakesSendReturnFalse() throws Exception {
    connect();
    server.stallReads = true;
    ByteBuffer big = ByteBuffer.wrap(new byte[256 * 1024]);
    boolean refused = false;
    for (int i = 0; i < 4000 && !refused; i++) {
      refused = !transport.send(big);
    }
    assertTrue(refused, "send reports a full queue instead of blocking");
    assertTrue(transport.getDroppedOut() >= 1);
    server.stallReads = false;
  }

  @Test
  void openingTwiceOrAfterCloseDoesNothing() throws Exception {
    connect();
    transport.open("127.0.0.1", 1);
    Thread.sleep(200);
    assertTrue(transport.isConnected());
    transport.close();
    transport.open("127.0.0.1", server.port());
    Thread.sleep(300);
    assertFalse(transport.isConnected());
    assertNotNull(transport);
    assertNull(server.binary.poll());
  }
}
