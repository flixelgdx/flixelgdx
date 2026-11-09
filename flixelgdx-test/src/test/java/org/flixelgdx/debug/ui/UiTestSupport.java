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
package org.flixelgdx.debug.ui;

import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.util.FlixelString;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Decodes widget trees, patches, and values into plain Java objects so tests can read them easily. */
final class UiTestSupport {

  private UiTestSupport() {}

  /** One decoded widget. */
  record Node(int type, int id, String name, Map<Integer, Object> props, List<Node> children) {

    Node child(int index) {
      return children.get(index);
    }
  }

  /** One decoded patch operation. */
  record Op(int widget, int code, int key, Object value) {
  }

  /** An error value. */
  record Error(String message) {
  }

  static Object readValue(FlixelDebugMessageReader r) {
    int kind = r.readU8();
    return switch (kind) {
      case FlixelDebugProtocol.VALUE_BOOL -> r.readBool();
      case FlixelDebugProtocol.VALUE_INT -> r.readI32();
      case FlixelDebugProtocol.VALUE_LONG -> r.readI64();
      case FlixelDebugProtocol.VALUE_FLOAT -> r.readF32();
      case FlixelDebugProtocol.VALUE_DOUBLE -> r.readF64();
      case FlixelDebugProtocol.VALUE_STRING -> r.readString(new FlixelString()).toString();
      case FlixelDebugProtocol.VALUE_ERROR -> new Error(r.readString(new FlixelString()).toString());
      case FlixelDebugProtocol.VALUE_NULL -> null;
      case FlixelDebugProtocol.VALUE_BYTES -> {
        byte[] data = new byte[r.readBytesLength()];
        r.readBytes(data, 0, data.length);
        yield data;
      }
      case FlixelDebugProtocol.VALUE_LIST -> {
        int n = r.readCount();
        List<Object> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
          items.add(readValue(r));
        }
        yield items;
      }
      default -> throw new AssertionError("Unknown value kind " + kind);
    };
  }

  static Node readWidget(FlixelDebugMessageReader r) {
    int type = r.readU8();
    int id = r.readI32();
    String name = r.readString(new FlixelString()).toString();
    Map<Integer, Object> props = new LinkedHashMap<>();
    int count = r.readCount();
    int lastKey = 0;
    for (int i = 0; i < count; i++) {
      int key = r.readU8();
      if (key <= lastKey) {
        throw new AssertionError("Properties are not in key order");
      }
      lastKey = key;
      props.put(key, readValue(r));
    }
    List<Node> children = new ArrayList<>();
    int n = r.readCount();
    for (int i = 0; i < n; i++) {
      children.add(readWidget(r));
    }
    return new Node(type, id, name, props, children);
  }

  static Node tree(FlixelDebugPanel panel) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_TREE).writeU16(0);
    panel.writeTree(w);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    r.readU16();
    Node root = readWidget(r);
    if (r.getRemaining() != 0) {
      throw new AssertionError("Bytes left after the tree");
    }
    return root;
  }

  static List<Op> patch(FlixelDebugPanel panel) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_PATCH).writeU16(0);
    panel.writePatch(w);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    r.readU16();
    List<Op> ops = new ArrayList<>();
    int n = r.readCount();
    for (int i = 0; i < n; i++) {
      ops.add(new Op(r.readI32(), r.readU8(), r.readU8(), readValue(r)));
    }
    if (r.getRemaining() != 0) {
      throw new AssertionError("Bytes left after the patch");
    }
    return ops;
  }

  /** Sends an event as the app would. Arguments may be Boolean, Integer, Long, Float, Double, String, or null. */
  static void event(FlixelDebugPanel panel, FlixelDebugWidget<?> widget, int event, Object... args) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_EVENT).writeU16(0).writeI32(widget.getId()).writeU8(event);
    w.beginArray();
    for (Object arg : args) {
      if (arg == null) {
        w.writeNullValue();
      } else if (arg instanceof Boolean b) {
        w.writeValue(b.booleanValue());
      } else if (arg instanceof Integer i) {
        w.writeValue(i.intValue());
      } else if (arg instanceof Long l) {
        w.writeValue(l.longValue());
      } else if (arg instanceof Float f) {
        w.writeValue(f.floatValue());
      } else if (arg instanceof Double d) {
        w.writeValue(d.doubleValue());
      } else {
        w.writeValue((String) arg);
      }
    }
    w.endArray(args.length);
    ByteBuffer buffer = w.finish();
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(buffer);
    r.readU8();
    r.readU16();
    panel.handleEvent(r);
  }

  /** Opens a panel and consumes its first tree, so later changes show up as patches. */
  static FlixelDebugPanel opened(FlixelDebugPanel panel) {
    panel.setOpen(true);
    tree(panel);
    return panel;
  }
}
