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
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.remote.FlixelDebugEndpoint;
import org.jetbrains.annotations.Nullable;

/**
 * Wires the remote debugger into a JVM game: it installs the socket transport and the address of the debugger app.
 *
 * <p>The desktop and Android launchers call {@link #install(String)} in debug mode, before the game starts. A game
 * never needs to call it. The address text comes from the launcher's own setting (see {@link FlixelDebugEndpoint}
 * for the accepted format): {@code -Dflixel.debugHost} on desktop, the {@code flixel.debugHost} intent extra on
 * Android. Without one, the game connects to {@code 127.0.0.1:6510}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelJvmDebugger.install(System.getProperty(FlixelJvmDebugger.HOST_KEY));
 * }</pre>
 */
public final class FlixelJvmDebugger {

  /** The name of the setting that holds the {@code host[:port]} text on every platform. */
  public static final String HOST_KEY = "flixel.debugHost";

  private FlixelJvmDebugger() {}

  /**
   * Installs a {@link FlixelSocketWebSocketTransport} and the address of the debugger app into
   * {@link FlixelDebugManager}.
   *
   * @param hostSpec The {@code host[:port]} text, or {@code null} for the default address. Text that cannot be read
   *   logs a warning and falls back to the default address.
   */
  public static void install(@Nullable String hostSpec) {
    FlixelDebugEndpoint endpoint = FlixelDebugEndpoint.tryParse(hostSpec);
    if (endpoint == null) {
      if (hostSpec != null && !hostSpec.isBlank()) {
        Flixel.warn("Could not read {} '{}'. Use host or host:port. Falling back to {}.", HOST_KEY, hostSpec,
            FlixelDebugEndpoint.DEFAULT);
      }
      endpoint = FlixelDebugEndpoint.DEFAULT;
    }
    FlixelDebugManager.defaultTransport = new FlixelSocketWebSocketTransport();
    FlixelDebugManager.defaultEndpoint = endpoint;
  }
}
