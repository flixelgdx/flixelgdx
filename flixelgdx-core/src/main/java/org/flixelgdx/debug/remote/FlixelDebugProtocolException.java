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

/**
 * Thrown when a remote debug message cannot be read or written.
 *
 * <p>The reader throws it when a message is shorter than its own fields claim, so a corrupted or
 * truncated message from the debugger app never crashes the game: the agent catches this exception,
 * drops the message, and carries on. The writer throws it when a value cannot be represented in the
 * wire format, such as an array with more than 65535 elements.
 */
public class FlixelDebugProtocolException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with the given description.
   *
   * @param message What went wrong.
   */
  public FlixelDebugProtocolException(String message) {
    super(message);
  }
}
