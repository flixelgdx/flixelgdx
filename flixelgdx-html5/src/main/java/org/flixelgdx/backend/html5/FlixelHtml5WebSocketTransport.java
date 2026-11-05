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
package org.flixelgdx.backend.html5;

import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.remote.FlixelDebugTransport;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;

import java.nio.ByteBuffer;

/**
 * The connection between a web game in debug mode and the remote debugger app, carried by the browser's own
 * {@code WebSocket}.
 *
 * <p>Think of the browser as a mail room that does everything by itself: the transport hands it an outgoing
 * message with {@link #send(ByteBuffer)} and, once per frame, collects the mail that arrived since the last visit
 * with {@link #poll(ByteBuffer)}. The mail room dials the app, redials every
 * {@link FlixelDebugProtocol#RECONNECT_MILLIS} milliseconds while the app is away, and drops messages when it is
 * overloaded.
 *
 * <h2>How it talks to the browser</h2>
 *
 * <p>Everything crosses the Java and browser boundary through small {@code @JSBody} functions with only numbers,
 * strings, and typed arrays, the same way the rest of this backend does. No Java callback is ever handed to the
 * browser, which keeps the transport free of anything specific to TeaVM's JavaScript or WebAssembly target: the
 * browser's event handlers are plain script that stores each incoming message in a queue on the page, and the game
 * reads that queue during its frame. Because the browser runs the game and the event handlers on one thread, the
 * queue needs no locks.
 *
 * <ul>
 *   <li>The socket uses {@code binaryType = "arraybuffer"}; text messages are ignored.</li>
 *   <li>The incoming queue holds at most {@value #INBOUND_CAPACITY} messages, and newer ones are dropped and
 *       counted while it is full.</li>
 *   <li>{@link #send(ByteBuffer)} refuses a message while the browser still holds more than
 *       {@value #OUTBOUND_LIMIT} bytes that it has not sent yet.</li>
 *   <li>The page keeps one connection, so only one instance of this transport is used per page.</li>
 * </ul>
 *
 * <p>A page served over {@code https} may only open an unencrypted {@code ws://} connection to {@code localhost}, so
 * debug a game that is hosted on a remote {@code https} address with the debugger app running on the same computer as
 * the browser.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugManager.defaultTransport = new FlixelHtml5WebSocketTransport();
 * FlixelDebugManager.defaultEndpoint = FlixelDebugEndpoint.parse("127.0.0.1:6510");
 * }</pre>
 *
 * @see FlixelDebugTransport
 */
public class FlixelHtml5WebSocketTransport implements FlixelDebugTransport {

  /** How many received messages wait for the game before new ones are dropped. */
  public static final int INBOUND_CAPACITY = 64;

  /** The most unsent bytes the browser may hold before {@link #send(ByteBuffer)} refuses a message. */
  public static final int OUTBOUND_LIMIT = 4 * 1024 * 1024;

  private boolean opened;

  @Override
  public void open(@NotNull String host, int port) {
    if (opened) {
      return;
    }
    opened = true;
    String authority = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
    start("ws://" + authority + ":" + port + "/", FlixelDebugProtocol.RECONNECT_MILLIS, INBOUND_CAPACITY);
  }

  @Override
  public boolean isConnected() {
    return opened && isUp();
  }

  @Override
  public boolean send(@NotNull ByteBuffer message) {
    if (!isConnected() || buffered() > OUTBOUND_LIMIT) {
      return false;
    }
    sendBytes(Int8Array.fromJavaBuffer(message));
    return true;
  }

  @Override
  public int poll(@NotNull ByteBuffer into) {
    into.clear();
    if (!opened) {
      return 0;
    }
    int length = inboxLength();
    if (length <= 0) {
      return 0;
    }
    if (length > into.capacity()) {
      inboxPop();
      return -length;
    }
    Int8Array head = inboxHead();
    for (int i = 0; i < length; i++) {
      into.put(head.get(i));
    }
    inboxPop();
    into.flip();
    return length;
  }

  @Override
  public boolean consumeConnectedEvent() {
    return opened && takeEvent();
  }

  @Override
  public void close() {
    if (opened) {
      opened = false;
      stop();
    }
  }

  /**
   * Returns how many received messages were dropped because the incoming queue was full.
   *
   * @return The count since the transport was opened.
   */
  public int getDroppedIn() {
    return opened ? dropped() : 0;
  }

  @JSBody(params = { "url", "delay", "capacity" }, script = """
      if (window.__flixelDebugWs) { return; }
      var s = window.__flixelDebugWs = {
        ws: null, up: false, event: false, closed: false, timer: 0, dropped: 0, inbox: []
      };
      function schedule() {
        if (!s.closed) { s.timer = setTimeout(connect, delay); }
      }
      function connect() {
        s.timer = 0;
        if (s.closed) { return; }
        var ws;
        try { ws = new WebSocket(url); } catch (e) { schedule(); return; }
        ws.binaryType = 'arraybuffer';
        s.ws = ws;
        function lost() {
          if (s.ws !== ws) { return; }
          s.ws = null; s.up = false; s.inbox.length = 0;
          schedule();
        }
        ws.onopen = function() {
          if (s.ws === ws) { s.up = true; s.event = true; s.inbox.length = 0; }
        };
        ws.onmessage = function(ev) {
          if (s.ws !== ws || typeof ev.data === 'string') { return; }
          if (s.inbox.length >= capacity) { s.dropped++; return; }
          s.inbox.push(new Int8Array(ev.data));
        };
        ws.onclose = lost;
        ws.onerror = lost;
      }
      connect();
      """)
  private static native void start(String url, int delay, int capacity);

  @JSBody(script = """
      var s = window.__flixelDebugWs;
      if (!s) { return; }
      s.closed = true;
      if (s.timer) { clearTimeout(s.timer); s.timer = 0; }
      var ws = s.ws;
      s.ws = null; s.up = false; s.inbox.length = 0;
      if (ws) { try { ws.close(1000); } catch (e) {} }
      window.__flixelDebugWs = null;
      """)
  private static native void stop();

  @JSBody(script = "var s = window.__flixelDebugWs; return !!(s && s.up);")
  private static native boolean isUp();

  @JSBody(script = """
      var s = window.__flixelDebugWs;
      if (s && s.event) { s.event = false; return true; }
      return false;
      """)
  private static native boolean takeEvent();

  @JSBody(script = "var s = window.__flixelDebugWs; return (s && s.ws) ? s.ws.bufferedAmount : 0;")
  private static native double buffered();

  @JSBody(params = "data", script = "var s = window.__flixelDebugWs; if (s && s.ws && s.up) { s.ws.send(data); }")
  private static native void sendBytes(Int8Array data);

  @JSBody(script = "var s = window.__flixelDebugWs; return (s && s.inbox.length) ? s.inbox[0].length : 0;")
  private static native int inboxLength();

  @JSBody(script = "return window.__flixelDebugWs.inbox[0];")
  private static native Int8Array inboxHead();

  @JSBody(script = "var s = window.__flixelDebugWs; if (s && s.inbox.length) { s.inbox.shift(); }")
  private static native void inboxPop();

  @JSBody(script = "var s = window.__flixelDebugWs; return s ? s.dropped : 0;")
  private static native int dropped();
}
