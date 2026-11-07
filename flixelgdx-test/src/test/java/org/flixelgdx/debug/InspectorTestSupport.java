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
import org.flixelgdx.debug.remote.FlixelDebugProtocol;

import java.util.ArrayList;
import java.util.List;

/** Decodes inspector and custom panel payloads into plain objects so tests can assert on them. */
final class InspectorTestSupport {

  private InspectorTestSupport() {}

  /** One decoded property row. */
  record Row(String name, int kind, String value, boolean editable) {
  }

  /** One decoded section. */
  record Section(String typeName, String displayName, List<Row> rows) {
  }

  /** One decoded inspector STATE payload. */
  record State(int subtype, int selectedId, String path, List<Section> sections, boolean hasTexture) {

    List<String> typeNames() {
      List<String> names = new ArrayList<>();
      for (Section s : sections) {
        names.add(s.typeName());
      }
      return names;
    }

    Row row(int section, int index) {
      return sections.get(section).rows().get(index);
    }
  }

  /** One decoded custom panel payload. */
  record Custom(List<Row> rows, List<String> buttons, List<Integer> buttonIndices) {
  }

  static State state(FlixelDebugPanel panel) {
    FlixelDebugMessageReader r = DebugTestSupport.payload(panel);
    int subtype = r.readU8();
    int id = r.readI32();
    String path = DebugTestSupport.str(r);
    int count = r.readCount();
    List<Section> sections = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String type = DebugTestSupport.str(r);
      String display = DebugTestSupport.str(r);
      sections.add(new Section(type, display, rows(r)));
    }
    boolean hasTexture = r.readBool();
    if (r.getRemaining() != 0) {
      throw new AssertionError("Trailing bytes: " + r.getRemaining());
    }
    return new State(subtype, id, path, sections, hasTexture);
  }

  static Custom custom(FlixelDebugPanel panel) {
    FlixelDebugMessageReader r = DebugTestSupport.payload(panel);
    List<Row> rows = rows(r);
    int count = r.readCount();
    List<String> buttons = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      indices.add(r.readU16());
      buttons.add(DebugTestSupport.str(r));
    }
    if (r.getRemaining() != 0) {
      throw new AssertionError("Trailing bytes: " + r.getRemaining());
    }
    return new Custom(rows, buttons, indices);
  }

  private static List<Row> rows(FlixelDebugMessageReader r) {
    int count = r.readCount();
    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String name = DebugTestSupport.str(r);
      int kind = r.readU8();
      String value = switch (kind) {
        case FlixelDebugProtocol.VALUE_BOOL -> String.valueOf(r.readBool());
        case FlixelDebugProtocol.VALUE_INT -> String.valueOf(r.readI32());
        case FlixelDebugProtocol.VALUE_LONG -> String.valueOf(r.readI64());
        case FlixelDebugProtocol.VALUE_FLOAT -> String.valueOf(r.readF32());
        case FlixelDebugProtocol.VALUE_DOUBLE -> String.valueOf(r.readF64());
        default -> DebugTestSupport.str(r);
      };
      rows.add(new Row(name, kind, value, r.readBool()));
    }
    return rows;
  }
}
