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
package org.flixelgdx.debug;

import org.flixelgdx.debug.panels.FlixelDebugPanel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.util.FlixelString;

import java.util.function.Consumer;

/** Helpers that turn a panel's {@code write} output or a hand-built action back into readers. */
final class DebugTestSupport {

  private static final FlixelString TEXT = new FlixelString();

  private DebugTestSupport() {}

  /** Encodes the panel inside a PANEL_DATA message and returns a reader positioned after the panel index. */
  static FlixelDebugMessageReader payload(FlixelDebugPanel panel) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_DATA);
    w.writeU16(7);
    panel.write(w);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    r.readU16();
    return r;
  }

  /** Builds a PANEL_ACTION message and returns a reader positioned after the action code. */
  static FlixelDebugMessageReader action(int action, Consumer<FlixelDebugMessageWriter> fields) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_ACTION);
    w.writeU16(7);
    w.writeU8(action);
    fields.accept(w);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    r.readU16();
    r.readU8();
    return r;
  }

  /** Sends an action to the panel the way the network agent would. */
  static void send(FlixelDebugPanel panel, int action, Consumer<FlixelDebugMessageWriter> fields) {
    panel.handleAction(action, action(action, fields));
  }

  /** Reads a string field. */
  static String str(FlixelDebugMessageReader r) {
    return r.readString(TEXT).toString();
  }
}
