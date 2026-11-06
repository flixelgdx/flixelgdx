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
import org.jetbrains.annotations.Nullable;

/**
 * The address of the debugger app: a host and a port.
 *
 * <p>Think of it as the street address printed on an envelope. The default is the same computer the game runs
 * on ({@code 127.0.0.1}) and the protocol's well-known port ({@link FlixelDebugProtocol#DEFAULT_PORT}). A
 * launcher can point the game somewhere else with a short {@code host[:port]} text, which each platform
 * reads from its own place:
 *
 * <ul>
 *   <li>Desktop: the {@code -Dflixel.debugHost=...} system property.</li>
 *   <li>Android: the {@code flixel.debugHost} string extra of the launch intent.</li>
 *   <li>Web: the {@code flixel.debugHost} query parameter of the page URL.</li>
 * </ul>
 *
 * <h2>Accepted text</h2>
 *
 * <ul>
 *   <li>{@code host}: the host with the default port, for example {@code 192.168.1.20}.</li>
 *   <li>{@code host:port}: both, for example {@code my-pc.local:7000}.</li>
 *   <li>{@code :port}: the default host with another port.</li>
 *   <li>{@code [::1]} and {@code [::1]:7000}: an IPv6 address needs square brackets when it has a port. An IPv6
 *       address with several colons and no brackets is read as a host with the default port.</li>
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugEndpoint endpoint = FlixelDebugEndpoint.parse(System.getProperty("flixel.debugHost"));
 * FlixelDebugManager.defaultEndpoint = endpoint;
 * }</pre>
 */
public final class FlixelDebugEndpoint {

  /** The host the game uses when nothing else is given. */
  public static final String DEFAULT_HOST = "127.0.0.1";

  /** The address used when nothing else is given: {@code 127.0.0.1} and {@link FlixelDebugProtocol#DEFAULT_PORT}. */
  public static final FlixelDebugEndpoint DEFAULT = new FlixelDebugEndpoint(DEFAULT_HOST,
      FlixelDebugProtocol.DEFAULT_PORT);

  private final int port;
  private final String host;

  /**
   * Creates an endpoint.
   *
   * @param host The host name or address.
   * @param port The port, from 1 to 65535.
   */
  public FlixelDebugEndpoint(@NotNull String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Reads a {@code host[:port]} text, falling back to {@link #DEFAULT} when it is missing or malformed.
   *
   * @param spec The text, or {@code null}.
   * @return The endpoint, never {@code null}.
   */
  @NotNull
  public static FlixelDebugEndpoint parse(@Nullable String spec) {
    FlixelDebugEndpoint endpoint = tryParse(spec);
    return endpoint != null ? endpoint : DEFAULT;
  }

  /**
   * Reads a {@code host[:port]} text.
   *
   * @param spec The text, or {@code null}.
   * @return The endpoint, or {@code null} when the text is missing, blank, or malformed (an empty host with a
   *   bad port, a port that is not a number from 1 to 65535, or an unclosed bracket).
   */
  @Nullable
  public static FlixelDebugEndpoint tryParse(@Nullable String spec) {
    if (spec == null) {
      return null;
    }
    String text = spec.trim();
    if (text.isEmpty()) {
      return null;
    }
    String host;
    String portText = null;
    if (text.charAt(0) == '[') {
      int close = text.indexOf(']');
      if (close < 2) {
        return null;
      }
      host = text.substring(1, close);
      if (close + 1 < text.length()) {
        if (text.charAt(close + 1) != ':') {
          return null;
        }
        portText = text.substring(close + 2);
      }
    } else {
      int first = text.indexOf(':');
      if (first < 0 || first != text.lastIndexOf(':')) {
        // No colon, or several colons without brackets (a bare IPv6 address): the whole text is the host.
        host = text;
      } else {
        host = text.substring(0, first);
        portText = text.substring(first + 1);
      }
    }
    int port = FlixelDebugProtocol.DEFAULT_PORT;
    if (portText != null) {
      port = parsePort(portText);
      if (port < 0) {
        return null;
      }
    }
    if (host.isEmpty()) {
      host = DEFAULT_HOST;
    }
    for (int i = 0; i < host.length(); i++) {
      if (Character.isWhitespace(host.charAt(i))) {
        return null;
      }
    }
    return new FlixelDebugEndpoint(host, port);
  }

  private static int parsePort(String text) {
    int length = text.length();
    if (length == 0 || length > 5) {
      return -1;
    }
    int port = 0;
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      if (c < '0' || c > '9') {
        return -1;
      }
      port = port * 10 + (c - '0');
    }
    return port >= 1 && port <= 65535 ? port : -1;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof FlixelDebugEndpoint e && e.port == port && e.host.equals(host);
  }

  @Override
  public int hashCode() {
    return host.hashCode() * 31 + port;
  }

  @Override
  public String toString() {
    return host.indexOf(':') >= 0 ? "[" + host + "]:" + port : host + ":" + port;
  }

  /**
   * Returns the host name or address, without brackets.
   *
   * @return The host.
   */
  @NotNull
  public String getHost() {
    return host;
  }

  /**
   * Returns the port.
   *
   * @return The port, from 1 to 65535.
   */
  public int getPort() {
    return port;
  }
}
