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
 * A {@link FlixelDebugTransport} that connects to nothing.
 *
 * <p>It is the default, so a game that never installs a transport (a release build, a headless test, a platform
 * without network support) simply has no remote debugger and pays nothing for it.
 */
public final class FlixelNoopDebugTransport implements FlixelDebugTransport {

  /** The one shared instance. */
  public static final FlixelNoopDebugTransport INSTANCE = new FlixelNoopDebugTransport();

  private FlixelNoopDebugTransport() {}

  @Override
  public void open(@NotNull String host, int port) {}

  @Override
  public boolean isConnected() {
    return false;
  }

  @Override
  public boolean send(@NotNull ByteBuffer message) {
    return false;
  }

  @Override
  public int poll(@NotNull ByteBuffer into) {
    return 0;
  }

  @Override
  public boolean consumeConnectedEvent() {
    return false;
  }

  @Override
  public void close() {}
}
