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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A small WebSocket server for tests. It accepts one client at a time, does the handshake, and records what the
 * client sends. Its send methods write server frames (never masked).
 */
final class TestWebSocketServer implements AutoCloseable {

  final BlockingQueue<byte[]> binary = new LinkedBlockingQueue<>();
  final BlockingQueue<byte[]> pongs = new LinkedBlockingQueue<>();
  final BlockingQueue<Integer> closeCodes = new LinkedBlockingQueue<>();

  volatile boolean badAccept;
  volatile boolean stallReads;
  volatile int connections;
  volatile int unmaskedFrames;

  private final ServerSocket server;
  private volatile Socket client;
  private volatile OutputStream out;

  TestWebSocketServer(int port) throws IOException {
    server = new ServerSocket();
    server.setReuseAddress(true);
    server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
    Thread accept = new Thread(this::acceptLoop, "test-ws-accept");
    accept.setDaemon(true);
    accept.start();
  }

  int port() {
    return server.getLocalPort();
  }

  private void acceptLoop() {
    while (!server.isClosed()) {
      try {
        Socket socket = server.accept();
        socket.setTcpNoDelay(true);
        client = socket;
        Thread t = new Thread(() -> serve(socket), "test-ws-client");
        t.setDaemon(true);
        t.start();
      } catch (IOException e) {
        return;
      }
    }
  }

  private void serve(Socket socket) {
    try {
      InputStream in = new BufferedInputStream(socket.getInputStream());
      OutputStream output = socket.getOutputStream();
      ByteArrayOutputStream head = new ByteArrayOutputStream();
      int last4 = 0;
      while (true) {
        int b = in.read();
        if (b < 0) {
          return;
        }
        head.write(b);
        last4 = (last4 << 8) | b;
        if (last4 == 0x0D0A0D0A) {
          break;
        }
      }
      String key = null;
      for (String line : head.toString(StandardCharsets.US_ASCII).split("\r\n")) {
        if (line.toLowerCase().startsWith("sec-websocket-key:")) {
          key = line.substring(line.indexOf(':') + 1).trim();
        }
      }
      String accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
          .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));
      if (badAccept) {
        accept = "AAAAAAAAAAAAAAAAAAAAAAAAAAA=";
      }
      output.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
          + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
      output.flush();
      out = output;
      connections++;
      readFrames(socket, in, output);
    } catch (Exception e) {
      // The client went away; the test notices through its own checks.
    } finally {
      try {
        socket.close();
      } catch (IOException ignored) {
        // Nothing to do.
      }
    }
  }

  private void readFrames(Socket socket, InputStream in, OutputStream output) throws Exception {
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    while (true) {
      while (stallReads) {
        Thread.sleep(10);
      }
      int b0 = in.read();
      if (b0 < 0) {
        return;
      }
      int b1 = in.read();
      boolean fin = (b0 & 0x80) != 0;
      int opcode = b0 & 0x0F;
      boolean masked = (b1 & 0x80) != 0;
      long length = b1 & 0x7F;
      if (length == 126) {
        length = (in.read() << 8) | in.read();
      } else if (length == 127) {
        length = 0;
        for (int i = 0; i < 8; i++) {
          length = (length << 8) | in.read();
        }
      }
      byte[] mask = new byte[4];
      if (masked) {
        readFully(in, mask);
      } else {
        unmaskedFrames++;
      }
      byte[] payload = new byte[(int) length];
      readFully(in, payload);
      for (int i = 0; i < payload.length; i++) {
        payload[i] ^= mask[i & 3];
      }
      if (opcode == 0x2 || opcode == 0x0) {
        message.write(payload);
        if (fin) {
          binary.add(message.toByteArray());
          message.reset();
        }
      } else if (opcode == 0xA) {
        pongs.add(payload);
      } else if (opcode == 0x8) {
        int code = payload.length >= 2 ? ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF) : 1005;
        closeCodes.add(code);
        synchronized (this) {
          output.write(new byte[] { (byte) 0x88, 2, (byte) (code >> 8), (byte) code });
          output.flush();
        }
        return;
      }
    }
  }

  private static void readFully(InputStream in, byte[] into) throws IOException {
    int done = 0;
    while (done < into.length) {
      int n = in.read(into, done, into.length - done);
      if (n < 0) {
        throw new EOFException();
      }
      done += n;
    }
  }

  void sendFrame(int firstByte, byte[] payload) throws IOException {
    ByteArrayOutputStream frame = new ByteArrayOutputStream();
    frame.write(firstByte);
    if (payload.length < 126) {
      frame.write(payload.length);
    } else if (payload.length <= 0xFFFF) {
      frame.write(126);
      frame.write(payload.length >> 8);
      frame.write(payload.length);
    } else {
      frame.write(127);
      for (int shift = 56; shift >= 0; shift -= 8) {
        frame.write(shift >= 32 ? 0 : payload.length >> shift);
      }
    }
    frame.write(payload);
    synchronized (this) {
      out.write(frame.toByteArray());
      out.flush();
    }
  }

  void sendBinary(byte[] payload) throws IOException {
    sendFrame(0x82, payload);
  }

  void sendText(String text) throws IOException {
    sendFrame(0x81, text.getBytes(StandardCharsets.UTF_8));
  }

  void sendPing(byte[] payload) throws IOException {
    sendFrame(0x89, payload);
  }

  void sendClose(int code) throws IOException {
    sendFrame(0x88, new byte[] { (byte) (code >> 8), (byte) code });
  }

  /** Writes only the header of a binary frame that claims the given length. */
  void sendHugeHeader(long length) throws IOException {
    ByteArrayOutputStream frame = new ByteArrayOutputStream();
    frame.write(0x82);
    frame.write(127);
    for (int shift = 56; shift >= 0; shift -= 8) {
      frame.write((int) (length >> shift));
    }
    synchronized (this) {
      out.write(frame.toByteArray());
      out.flush();
    }
  }

  byte[] awaitBinary(long millis) throws InterruptedException {
    return binary.poll(millis, TimeUnit.MILLISECONDS);
  }

  /** Drops the current client without a close frame. */
  void dropClient() throws IOException {
    Socket socket = client;
    if (socket != null) {
      socket.close();
    }
  }

  @Override
  public void close() throws IOException {
    server.close();
    dropClient();
  }
}
