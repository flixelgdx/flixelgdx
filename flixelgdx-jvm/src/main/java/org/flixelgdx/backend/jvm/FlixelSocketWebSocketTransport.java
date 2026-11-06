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
import org.flixelgdx.debug.remote.FlixelDebugMessageQueue;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.remote.FlixelDebugTransport;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;

/**
 * A minimal WebSocket client (RFC 6455) built on {@link Socket}, used by the desktop and Android backends to reach
 * the remote debugger app.
 *
 * <p>Think of it as a telephone line with a switchboard operator. The game thread only ever drops a message into
 * an outgoing tray or picks one from an incoming tray ({@link #send(ByteBuffer)}, {@link #poll(ByteBuffer)}). A
 * background operator does all the slow work: dialing (connecting and doing the HTTP upgrade handshake), reading
 * frames off the wire, writing frames onto it, answering pings, and dialing again when the line goes dead.
 *
 * <h2>What it supports</h2>
 *
 * <ul>
 *   <li>The client handshake with a random {@code Sec-WebSocket-Key}, checking {@code Sec-WebSocket-Accept}.</li>
 *   <li>Binary frames in both directions, with the masking that a client must apply. Text frames are ignored.</li>
 *   <li>Fragmented messages (continuation frames), ping (answered with pong), pong, and the close handshake in
 *       both directions.</li>
 *   <li>Messages up to {@value #MAX_MESSAGE_BYTES} bytes. A bigger incoming message closes the connection with code
 *       1009.</li>
 * </ul>
 *
 * <h2>Threads and memory</h2>
 *
 * <p>Two daemon threads serve a connection: one connects and reads, one writes. They exchange messages with the game
 * thread through two bounded {@link FlixelDebugMessageQueue}s of reusable {@code byte[]} slots, guarded by one lock.
 * Nothing is allocated per message once the slots have grown to the size of the biggest message. When the outgoing
 * queue is full, {@link #send(ByteBuffer)} returns {@code false}. When the incoming queue is full, new messages
 * are dropped and counted ({@link #getDroppedIn()}).
 *
 * <p>If the app is not there, the transport tries again every {@link FlixelDebugProtocol#RECONNECT_MILLIS}
 * milliseconds until {@link #close()}, and logs only the first failure of each period without a connection, at debug
 * level.
 *
 * <p>This class only uses Java APIs that Android has.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugManager.defaultTransport = new FlixelSocketWebSocketTransport();
 * FlixelDebugManager.defaultEndpoint = FlixelDebugEndpoint.parse(System.getProperty("flixel.debugHost"));
 * }</pre>
 *
 * @see FlixelDebugTransport
 */
public class FlixelSocketWebSocketTransport implements FlixelDebugTransport {

  /** The largest message in either direction, in bytes (16 MB). */
  public static final int MAX_MESSAGE_BYTES = 16 * 1024 * 1024;

  /** How many messages the outgoing queue holds before {@link #send(ByteBuffer)} starts refusing. */
  public static final int OUTBOUND_CAPACITY = 64;

  /** How many received messages wait for the game before new ones are dropped. */
  public static final int INBOUND_CAPACITY = 64;

  /** How long connecting to the app may take, in milliseconds. */
  public static final int CONNECT_TIMEOUT_MILLIS = 1500;

  /** How long the app may take to answer the handshake, in milliseconds. */
  public static final int HANDSHAKE_TIMEOUT_MILLIS = 3000;

  /** How long to wait for the app to answer our close frame, in milliseconds. */
  public static final int CLOSE_WAIT_MILLIS = 1000;

  private static final int OP_CONTINUATION = 0x0;
  private static final int OP_TEXT = 0x1;
  private static final int OP_BINARY = 0x2;
  private static final int OP_CLOSE = 0x8;
  private static final int OP_PING = 0x9;
  private static final int OP_PONG = 0xA;

  private static final int CLOSE_NORMAL = 1000;
  private static final int CLOSE_PROTOCOL = 1002;
  private static final int CLOSE_TOO_BIG = 1009;

  private static final int READ_TIMEOUT_MILLIS = 250;
  private static final int MAX_HANDSHAKE_BYTES = 16 * 1024;
  private static final int KIND_NONE = 0;
  private static final int KIND_BINARY = 1;
  private static final int KIND_TEXT = 2;
  private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private static final FlixelLogger LOG = Flixel.log.tagged("Debugger/Transport");

  private final Object lock = new Object();
  private final FlixelDebugMessageQueue outbound = new FlixelDebugMessageQueue(OUTBOUND_CAPACITY, 4096);
  private final FlixelDebugMessageQueue inbound = new FlixelDebugMessageQueue(INBOUND_CAPACITY, 1024);
  private final SecureRandom secure = new SecureRandom();

  private long droppedIn;
  private long droppedOut;

  private volatile Connection current;
  private Thread thread;
  private String host = "127.0.0.1";
  private int port = FlixelDebugProtocol.DEFAULT_PORT;
  private int reconnectMillis = FlixelDebugProtocol.RECONNECT_MILLIS;

  private volatile boolean connected;
  private boolean connectedEvent;
  private boolean closeRequested;

  @Override
  public void open(@NotNull String host, int port) {
    synchronized (lock) {
      if (thread != null || closeRequested) {
        return;
      }
      this.host = host;
      this.port = port;
      thread = new Thread(this::run, "flixel-debug-ws");
      thread.setDaemon(true);
      thread.start();
    }
  }

  @Override
  public boolean isConnected() {
    return connected;
  }

  @Override
  public boolean send(@NotNull ByteBuffer message) {
    synchronized (lock) {
      if (!connected) {
        return false;
      }
      if (message.remaining() > MAX_MESSAGE_BYTES || !outbound.offer(message)) {
        droppedOut++;
        return false;
      }
      lock.notifyAll();
      return true;
    }
  }

  @Override
  public int poll(@NotNull ByteBuffer into) {
    into.clear();
    int length;
    synchronized (lock) {
      length = inbound.peekLength();
      if (length < 0) {
        return 0;
      }
      if (length > into.capacity()) {
        inbound.discard();
        droppedIn++;
        return -length;
      }
      inbound.poll(into);
    }
    into.flip();
    return length;
  }

  @Override
  public boolean consumeConnectedEvent() {
    synchronized (lock) {
      boolean was = connectedEvent;
      connectedEvent = false;
      return was;
    }
  }

  @Override
  public void close() {
    Thread worker;
    Connection connection;
    synchronized (lock) {
      if (closeRequested) {
        return;
      }
      closeRequested = true;
      worker = thread;
      connection = current;
      lock.notifyAll();
    }
    if (worker == null) {
      return;
    }
    try {
      worker.join(CLOSE_WAIT_MILLIS + 250);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (worker.isAlive() && connection != null) {
      connection.kill();
    }
  }

  private void run() {
    boolean failureLogged = false;
    while (!isCloseRequested()) {
      Connection connection = null;
      try {
        connection = connect();
      } catch (IOException | RuntimeException e) {
        if (!failureLogged && !isCloseRequested()) {
          failureLogged = true;
          LOG.debug("Could not reach the remote debugger at {}:{} ({}). Trying again every {} ms.", host, port,
              e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(), reconnectMillis);
        }
      }
      if (connection != null) {
        failureLogged = false;
        try {
          current = connection;
          synchronized (lock) {
            outbound.clear();
            inbound.clear();
            connected = true;
            connectedEvent = true;
          }
          serve(connection);
        } catch (IOException | RuntimeException ignored) {
          // The line went dead. The game notices through isConnected() and the loop dials again.
        } finally {
          connection.kill();
          synchronized (lock) {
            connected = false;
            current = null;
            outbound.clear();
            inbound.clear();
            lock.notifyAll();
          }
        }
      }
      waitToReconnect();
    }
  }

  private void waitToReconnect() {
    long deadline = System.nanoTime() + reconnectMillis * 1_000_000L;
    synchronized (lock) {
      while (!closeRequested) {
        long left = (deadline - System.nanoTime()) / 1_000_000L;
        if (left <= 0L) {
          return;
        }
        try {
          lock.wait(left);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  private boolean isCloseRequested() {
    synchronized (lock) {
      return closeRequested;
    }
  }

  private Connection connect() throws IOException {
    Socket socket = new Socket();
    try {
      socket.setTcpNoDelay(true);
      socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
      socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
      Connection connection = new Connection(socket);
      handshake(connection);
      socket.setSoTimeout(READ_TIMEOUT_MILLIS);
      return connection;
    } catch (IOException | RuntimeException e) {
      try {
        socket.close();
      } catch (IOException ignored) {
        // Already failing; nothing more to do.
      }
      throw e;
    }
  }

  private void handshake(Connection connection) throws IOException {
    byte[] keyBytes = new byte[16];
    secure.nextBytes(keyBytes);
    String key = Base64.getEncoder().encodeToString(keyBytes);
    String authority = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
    String request = "GET / HTTP/1.1\r\n"
        + "Host: " + authority + ":" + port + "\r\n"
        + "Upgrade: websocket\r\n"
        + "Connection: Upgrade\r\n"
        + "Sec-WebSocket-Key: " + key + "\r\n"
        + "Sec-WebSocket-Version: 13\r\n\r\n";
    connection.out.write(request.getBytes(StandardCharsets.US_ASCII));
    connection.out.flush();

    StringBuilder head = new StringBuilder(256);
    while (!endsWithBlankLine(head)) {
      int b = connection.in.read();
      if (b < 0) {
        throw new EOFException("The server closed the connection during the handshake.");
      }
      head.append((char) b);
      if (head.length() > MAX_HANDSHAKE_BYTES) {
        throw new IOException("The handshake answer is too long.");
      }
    }
    String[] lines = head.toString().split("\r\n");
    String[] status = lines[0].split(" ");
    if (status.length < 2 || !status[0].startsWith("HTTP/1.") || !"101".equals(status[1])) {
      throw new IOException("The server did not switch protocols: " + lines[0]);
    }
    String accept = null;
    for (int i = 1; i < lines.length; i++) {
      int colon = lines[i].indexOf(':');
      if (colon > 0 && lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT).equals("sec-websocket-accept")) {
        accept = lines[i].substring(colon + 1).trim();
      }
    }
    if (accept == null || !accept.equals(acceptFor(key))) {
      throw new IOException("The server sent a wrong Sec-WebSocket-Accept.");
    }
  }

  private static boolean endsWithBlankLine(StringBuilder text) {
    int n = text.length();
    return n >= 4 && text.charAt(n - 4) == '\r' && text.charAt(n - 3) == '\n' && text.charAt(n - 2) == '\r'
        && text.charAt(n - 1) == '\n';
  }

  private static String acceptFor(String key) {
    try {
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      return Base64.getEncoder().encodeToString(sha1.digest((key + GUID).getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 is missing", e);
    }
  }

  private void serve(Connection connection) throws IOException {
    Thread writer = new Thread(() -> writeLoop(connection), "flixel-debug-ws-out");
    writer.setDaemon(true);
    writer.start();
    readLoop(connection);
  }

  private void writeLoop(Connection connection) {
    byte[] tx = new byte[4096];
    try {
      while (true) {
        int length;
        synchronized (lock) {
          while (!connection.dead && !closeRequested && outbound.isEmpty()) {
            lock.wait();
          }
          if (connection.dead) {
            return;
          }
          if (closeRequested) {
            break;
          }
          length = outbound.peekLength();
          if (tx.length < length) {
            tx = new byte[Math.max(length, tx.length * 2)];
          }
          outbound.poll(tx, 0);
        }
        writeFrame(connection, OP_BINARY, tx, 0, length);
      }
      sendClose(connection, CLOSE_NORMAL);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      connection.kill();
    } catch (IOException | RuntimeException e) {
      connection.kill();
    }
  }

  private void readLoop(Connection connection) throws IOException {
    byte[] head = new byte[8];
    byte[] control = new byte[125];
    byte[] rx = new byte[4096];
    int assembled = 0;
    int kind = KIND_NONE;
    while (!connection.dead) {
      readFully(connection, head, 0, 2);
      int first = head[0] & 0xFF;
      int second = head[1] & 0xFF;
      boolean fin = (first & 0x80) != 0;
      int opcode = first & 0x0F;
      boolean masked = (second & 0x80) != 0;
      long length = second & 0x7F;
      if ((first & 0x70) != 0) {
        fail(connection, CLOSE_PROTOCOL, "Reserved bits are set.");
      }
      if (length == 126) {
        readFully(connection, head, 0, 2);
        length = ((head[0] & 0xFF) << 8) | (head[1] & 0xFF);
      } else if (length == 127) {
        readFully(connection, head, 0, 8);
        length = 0;
        for (int i = 0; i < 8; i++) {
          length = (length << 8) | (head[i] & 0xFF);
        }
        if (length < 0) {
          fail(connection, CLOSE_TOO_BIG, "The frame is too big.");
        }
      }
      if (masked) {
        readFully(connection, head, 4, 4);
      }
      if (opcode >= OP_CLOSE) {
        if (!fin || length > 125) {
          fail(connection, CLOSE_PROTOCOL, "A control frame is fragmented or too long.");
        }
        int size = (int) length;
        readFully(connection, control, 0, size);
        if (masked) {
          mask(control, 0, size, head, 4);
        }
        if (opcode == OP_PING) {
          writeFrame(connection, OP_PONG, control, 0, size);
        } else if (opcode == OP_CLOSE) {
          int code = size >= 2 ? ((control[0] & 0xFF) << 8) | (control[1] & 0xFF) : CLOSE_NORMAL;
          sendClose(connection, code);
          return;
        } else if (opcode != OP_PONG) {
          fail(connection, CLOSE_PROTOCOL, "Unknown opcode.");
        }
        continue;
      }
      if (opcode == OP_BINARY || opcode == OP_TEXT) {
        if (kind != KIND_NONE) {
          fail(connection, CLOSE_PROTOCOL, "A new message started before the last one ended.");
        }
        kind = opcode == OP_BINARY ? KIND_BINARY : KIND_TEXT;
        assembled = 0;
      } else if (opcode == OP_CONTINUATION) {
        if (kind == KIND_NONE) {
          fail(connection, CLOSE_PROTOCOL, "A continuation frame has nothing to continue.");
        }
      } else {
        fail(connection, CLOSE_PROTOCOL, "Unknown opcode.");
      }
      // Text is read and thrown away, so it always reuses the start of the buffer.
      int base = kind == KIND_BINARY ? assembled : 0;
      if (length > MAX_MESSAGE_BYTES - base) {
        fail(connection, CLOSE_TOO_BIG, "The message is too big.");
      }
      int size = (int) length;
      if (rx.length < base + size) {
        byte[] bigger = new byte[Math.min(MAX_MESSAGE_BYTES, Math.max(base + size, rx.length * 2))];
        System.arraycopy(rx, 0, bigger, 0, base);
        rx = bigger;
      }
      readFully(connection, rx, base, size);
      if (masked) {
        mask(rx, base, size, head, 4);
      }
      if (kind == KIND_BINARY) {
        assembled += size;
      }
      if (fin) {
        if (kind == KIND_BINARY) {
          synchronized (lock) {
            if (!inbound.offer(rx, 0, assembled)) {
              droppedIn++;
            }
          }
        }
        kind = KIND_NONE;
        assembled = 0;
      }
    }
  }

  private void readFully(Connection connection, byte[] buffer, int offset, int length) throws IOException {
    int done = 0;
    while (done < length) {
      try {
        int n = connection.in.read(buffer, offset + done, length - done);
        if (n < 0) {
          throw new EOFException("The connection was closed.");
        }
        done += n;
      } catch (SocketTimeoutException e) {
        if (connection.dead || connection.closeWaitExpired()) {
          throw e;
        }
      }
    }
  }

  private void fail(Connection connection, int code, String reason) throws IOException {
    try {
      sendClose(connection, code);
    } catch (IOException ignored) {
      // The connection is going away anyway.
    }
    throw new IOException(reason);
  }

  private void sendClose(Connection connection, int code) throws IOException {
    synchronized (connection.writeLock) {
      if (connection.closeSentNanos != 0L) {
        return;
      }
      connection.closeSentNanos = System.nanoTime() | 1L;
    }
    byte[] payload = { (byte) (code >> 8), (byte) code };
    writeFrame(connection, OP_CLOSE, payload, 0, 2);
  }

  /**
   * Writes one frame with the mask a client must use. The payload array is masked in place, so it must belong to
   * the caller.
   */
  private void writeFrame(Connection connection, int opcode, byte[] data, int offset, int length) throws IOException {
    synchronized (connection.writeLock) {
      byte[] header = connection.header;
      int n = 0;
      header[n++] = (byte) (0x80 | opcode);
      if (length < 126) {
        header[n++] = (byte) (0x80 | length);
      } else if (length <= 0xFFFF) {
        header[n++] = (byte) (0x80 | 126);
        header[n++] = (byte) (length >> 8);
        header[n++] = (byte) length;
      } else {
        header[n++] = (byte) (0x80 | 127);
        for (int shift = 56; shift >= 0; shift -= 8) {
          header[n++] = (byte) (shift >= 32 ? 0 : (length >> shift));
        }
      }
      connection.random.nextBytes(connection.maskKey);
      System.arraycopy(connection.maskKey, 0, header, n, 4);
      n += 4;
      mask(data, offset, length, connection.maskKey, 0);
      connection.out.write(header, 0, n);
      connection.out.write(data, offset, length);
      connection.out.flush();
    }
  }

  private static void mask(byte[] data, int offset, int length, byte[] key, int keyOffset) {
    for (int i = 0; i < length; i++) {
      data[offset + i] ^= key[keyOffset + (i & 3)];
    }
  }

  /**
   * Returns the time to wait between connection attempts.
   *
   * @return The delay in milliseconds.
   */
  public int getReconnectMillis() {
    return reconnectMillis;
  }

  /**
   * Sets the time to wait between connection attempts. Call it before {@link #open(String, int)}; the default is
   * {@link FlixelDebugProtocol#RECONNECT_MILLIS}.
   *
   * @param reconnectMillis The delay in milliseconds.
   */
  public void setReconnectMillis(int reconnectMillis) {
    this.reconnectMillis = Math.max(1, reconnectMillis);
  }

  /**
   * Returns how many received messages were dropped because the game did not pick them up in time or they were
   * too big for its buffer.
   *
   * @return The count since this transport was created.
   */
  public long getDroppedIn() {
    synchronized (lock) {
      return droppedIn;
    }
  }

  /**
   * Returns how many messages {@link #send(ByteBuffer)} refused because the outgoing queue was full.
   *
   * @return The count since this transport was created.
   */
  public long getDroppedOut() {
    synchronized (lock) {
      return droppedOut;
    }
  }

  /** One live socket with its streams and per-connection state. */
  private static final class Connection {

    final Object writeLock = new Object();
    final byte[] header = new byte[14];
    final byte[] maskKey = new byte[4];
    final Random random = new Random();
    final Socket socket;
    final InputStream in;
    final OutputStream out;

    volatile boolean dead;
    long closeSentNanos;

    Connection(Socket socket) throws IOException {
      this.socket = socket;
      this.in = new BufferedInputStream(socket.getInputStream(), 8192);
      this.out = socket.getOutputStream();
    }

    boolean closeWaitExpired() {
      long sent = closeSentNanos;
      return sent != 0L && System.nanoTime() - sent > CLOSE_WAIT_MILLIS * 1_000_000L;
    }

    void kill() {
      dead = true;
      try {
        socket.close();
      } catch (IOException ignored) {
        // Closing a socket that is already broken.
      }
    }
  }
}
