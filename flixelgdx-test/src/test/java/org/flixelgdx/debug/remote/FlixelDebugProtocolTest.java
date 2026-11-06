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

import org.flixelgdx.debug.remote.DebugProtocolFixtures.Op;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the debug protocol codec against the committed fixture files.
 *
 * <p>Set the environment variable {@code FLIXEL_REGEN_FIXTURES=1} to rewrite the {@code .bin} files from the
 * descriptions in {@link DebugProtocolFixtures}; otherwise the committed files are the reference.
 */
class FlixelDebugProtocolTest {

  static Stream<String> fixtureNames() {
    return DebugProtocolFixtures.ALL.keySet().stream();
  }

  static byte[] encode(Op[] ops) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(8);
    // Value-only fixtures carry no message type, so they start a message with a throwaway type byte.
    w.beginMessage(0);
    ByteBuffer buf;
    for (Op op : ops) {
      switch (op.type()) {
        case 'b' -> w.writeU8((Integer) op.value());
        case 's' -> w.writeU16((Integer) op.value());
        case 'i' -> w.writeI32((Integer) op.value());
        case 'l' -> w.writeI64((Long) op.value());
        case 'f' -> w.writeF32((Float) op.value());
        case 'd' -> w.writeF64((Double) op.value());
        case '?' -> w.writeBool((Boolean) op.value());
        case 'S' -> w.writeString((String) op.value());
        case 'B' -> w.writeBytes((byte[]) op.value());
        case '[' -> w.beginArray();
        case ']' -> w.endArray((Integer) op.value());
        case 'E' -> w.writeErrorValue((String) op.value());
        case 'V' -> {
          Object v = op.value();
          if (v instanceof Boolean x) {
            w.writeValue(x.booleanValue());
          } else if (v instanceof Integer x) {
            w.writeValue(x.intValue());
          } else if (v instanceof Long x) {
            w.writeValue(x.longValue());
          } else if (v instanceof Float x) {
            w.writeValue(x.floatValue());
          } else if (v instanceof Double x) {
            w.writeValue(x.doubleValue());
          } else {
            w.writeValue((String) v);
          }
        }
        default -> throw new IllegalStateException();
      }
    }
    buf = w.finish();
    // Drop the throwaway leading byte.
    byte[] out = new byte[buf.limit() - 1];
    buf.position(1);
    buf.get(out);
    return out;
  }

  static void decodeAndCompare(Op[] ops, byte[] bytes) {
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(ByteBuffer.wrap(bytes));
    FlixelString text = new FlixelString();
    int[] counts = new int[FlixelDebugMessageWriter.MAX_DEPTH];
    int depth = 0;
    for (Op op : ops) {
      switch (op.type()) {
        case 'b' -> assertEquals(op.value(), r.readU8());
        case 's' -> assertEquals(op.value(), r.readU16());
        case 'i' -> assertEquals(op.value(), r.readI32());
        case 'l' -> assertEquals(op.value(), r.readI64());
        case 'f' -> assertEquals(op.value(), r.readF32());
        case 'd' -> assertEquals(op.value(), r.readF64());
        case '?' -> assertEquals(op.value(), r.readBool());
        case 'S' -> assertEquals(op.value(), r.readString(text).toString());
        case 'B' -> {
          byte[] expected = (byte[]) op.value();
          int len = r.readBytesLength();
          byte[] got = new byte[len];
          r.readBytes(got, 0, len);
          assertArrayEquals(expected, got);
        }
        case '[' -> counts[depth++] = r.readCount();
        case ']' -> assertEquals(op.value(), counts[--depth]);
        case 'E' -> {
          assertEquals(FlixelDebugProtocol.VALUE_ERROR, r.readU8());
          assertEquals(op.value(), r.readString(text).toString());
        }
        case 'V' -> {
          Object v = op.value();
          int kind = r.readU8();
          if (v instanceof Boolean) {
            assertEquals(FlixelDebugProtocol.VALUE_BOOL, kind);
            assertEquals(v, r.readBool());
          } else if (v instanceof Integer) {
            assertEquals(FlixelDebugProtocol.VALUE_INT, kind);
            assertEquals(v, r.readI32());
          } else if (v instanceof Long) {
            assertEquals(FlixelDebugProtocol.VALUE_LONG, kind);
            assertEquals(v, r.readI64());
          } else if (v instanceof Float) {
            assertEquals(FlixelDebugProtocol.VALUE_FLOAT, kind);
            assertEquals(v, r.readF32());
          } else if (v instanceof Double) {
            assertEquals(FlixelDebugProtocol.VALUE_DOUBLE, kind);
            assertEquals(v, r.readF64());
          } else {
            assertEquals(FlixelDebugProtocol.VALUE_STRING, kind);
            assertEquals(v, r.readString(text).toString());
          }
        }
        default -> throw new IllegalStateException();
      }
    }
    assertEquals(0, r.getRemaining(), "all bytes consumed");
  }

  static String describe(Op[] ops) {
    StringBuilder sb = new StringBuilder();
    for (Op op : ops) {
      if (sb.length() > 0 && op.type() != ']') {
        sb.append(' ');
      }
      switch (op.type()) {
        case 'b' -> sb.append("u8:").append(op.value());
        case 's' -> sb.append("u16:").append(op.value());
        case 'i' -> sb.append("i32:").append(op.value());
        case 'l' -> sb.append("i64:").append(op.value());
        case 'f' -> sb.append("f32:").append(op.value());
        case 'd' -> sb.append("f64:").append(op.value());
        case '?' -> sb.append("bool:").append(op.value());
        case 'S' -> sb.append("string:").append(escape((String) op.value()));
        case 'B' -> sb.append("bytes:").append(Arrays.toString((byte[]) op.value()));
        case '[' -> sb.append("array[");
        case ']' -> sb.append("](count ").append(op.value()).append(')');
        case 'E' -> sb.append("value:ERROR:").append(escape((String) op.value()));
        default -> {
          Object v = op.value();
          String kind = v instanceof Boolean ? "BOOL" : v instanceof Integer ? "INT" : v instanceof Long ? "LONG"
              : v instanceof Float ? "FLOAT" : v instanceof Double ? "DOUBLE" : "STRING";
          sb.append("value:").append(kind).append(':').append(v instanceof String x ? escape(x) : v);
        }
      }
    }
    return sb.toString();
  }

  static String escape(String text) {
    StringBuilder sb = new StringBuilder("\"");
    text.codePoints().forEach(cp -> {
      if (cp == '\n') {
        sb.append("\\n");
      } else if (cp == '\t') {
        sb.append("\\t");
      } else if (cp == '"') {
        sb.append("\\\"");
      } else if (cp >= 0x20 && cp < 0x7F) {
        sb.append((char) cp);
      } else {
        sb.append("\\u{").append(Integer.toHexString(cp).toUpperCase()).append('}');
      }
    });
    return sb.append('"').toString();
  }

  static String readme() {
    StringBuilder sb = new StringBuilder();
    sb.append("# Remote debug protocol fixtures\n\n");
    sb.append("Binary test vectors for `DEBUG_PROTOCOL.md`. Every file is one complete protocol message (or, for\n");
    sb.append(
        "`value_*.bin`, one bare value). An implementation should decode each file and assert the values below,\n");
    sb.append("and encode the same values and compare the bytes.\n\n");
    sb.append("This file is generated by `FlixelDebugProtocolTest` (run the tests with `FLIXEL_REGEN_FIXTURES=1`).\n");
    sb.append("Do not edit it by hand. Notation: `type:value` fields appear in wire order; `array[ ... ](count N)`\n");
    sb.append("is a `u16` count followed by the elements; `value:KIND:x` is a property value (kind byte then data);\n");
    sb.append(
        "`\\u{XXXX}` is a Unicode code point written as UTF-8. Panel indices used: stats 0, logs 1, watches 2,\n");
    sb.append("trackers 3, textures 4, controls 5, console 6, scene 7, inspector 8, save 9, custom 10,\n");
    sb.append("viewport 11.\n\n");
    for (Map.Entry<String, Op[]> e : DebugProtocolFixtures.ALL.entrySet()) {
      sb.append("## ").append(e.getKey()).append(".bin\n\n`").append(describe(e.getValue())).append("`\n\n");
    }
    return sb.toString();
  }

  static byte[] committed(String name) throws IOException {
    try (InputStream in =
        FlixelDebugProtocolTest.class.getResourceAsStream(DebugProtocolFixtures.RESOURCE_DIR + name + ".bin")) {
      assertTrue(in != null, "missing fixture " + name + ".bin (run with FLIXEL_REGEN_FIXTURES=1)");
      return in.readAllBytes();
    }
  }

  @ParameterizedTest
  @MethodSource("fixtureNames")
  void fixtureMatchesCommittedBytesAndDecodes(String name) throws IOException {
    Op[] ops = DebugProtocolFixtures.ALL.get(name);
    byte[] encoded = encode(ops);
    if (System.getenv("FLIXEL_REGEN_FIXTURES") != null) {
      Path dir = Path.of(DebugProtocolFixtures.SOURCE_DIR);
      Files.createDirectories(dir);
      Files.write(dir.resolve(name + ".bin"), encoded);
    }
    byte[] expected = committed(name);
    assertArrayEquals(expected, encoded, name + " encodes to the committed bytes");
    decodeAndCompare(ops, expected);
  }

  @Test
  void fixtureReadmeIsUpToDate() throws IOException {
    Path file = Path.of(DebugProtocolFixtures.SOURCE_DIR + "README.md");
    if (System.getenv("FLIXEL_REGEN_FIXTURES") != null) {
      Files.writeString(file, readme());
    }
    assertEquals(readme(), Files.readString(file));
  }

  @Test
  void everyFixtureFileIsCovered() throws IOException {
    Path dir = Path.of(DebugProtocolFixtures.SOURCE_DIR);
    try (Stream<Path> files = Files.list(dir)) {
      long bins = files.filter(p -> p.toString().endsWith(".bin")).count();
      assertEquals(DebugProtocolFixtures.ALL.size(), bins);
    }
  }

  @Test
  void writerGrowsByDoublingAndKeepsData() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(16);
    w.beginMessage(FlixelDebugProtocol.MSG_PING);
    for (int i = 0; i < 100; i++) {
      w.writeI64(i);
    }
    assertEquals(801, w.getSize());
    assertEquals(1024, w.getCapacity());
    ByteBuffer buf = w.finish();
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(buf);
    assertEquals(FlixelDebugProtocol.MSG_PING, r.readU8());
    for (int i = 0; i < 100; i++) {
      assertEquals(i, r.readI64());
    }
  }

  @Test
  void writerReusesItsBufferBetweenMessages() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    ByteBuffer first = w.beginMessage(1).writeI32(5).finish();
    ByteBuffer second = w.beginMessage(2).finish();
    assertSame(first, second);
    assertEquals(1, second.limit());
  }

  @Test
  void arrayBackpatchSurvivesGrowth() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(16);
    w.beginMessage(1).beginArray();
    for (int i = 0; i < 50; i++) {
      w.writeI32(i);
    }
    w.endArray(50);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    assertEquals(50, r.readCount());
  }

  @Test
  void unfinishedArrayAndBadCountsAreRejected() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    w.beginMessage(1).beginArray();
    assertThrows(FlixelDebugProtocolException.class, w::finish);
    w.beginMessage(1);
    assertThrows(FlixelDebugProtocolException.class, () -> w.endArray(0));
    w.beginMessage(1).beginArray();
    assertThrows(FlixelDebugProtocolException.class, () -> w.endArray(65536));
  }

  @Test
  void stringTruncatesOnACharacterBoundary() {
    // Three bytes per character: 21845 characters fill 65535 bytes exactly, so one more must be cut.
    String three = DebugProtocolFixtures.CJK.repeat(21846);
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.beginMessage(1).writeString(three).finish());
    r.readU8();
    FlixelString out = r.readString(new FlixelString());
    assertEquals(21845, out.length());
    assertEquals(DebugProtocolFixtures.CJK.repeat(21845), out.toString());

    // Four bytes per character: 16383 characters is 65532 bytes, and the next one would need 65536.
    String four = DebugProtocolFixtures.EMOJI.repeat(16384);
    r = new FlixelDebugMessageReader(w.beginMessage(1).writeString(four).finish());
    r.readU8();
    out = r.readString(new FlixelString());
    assertEquals(16383 * 2, out.length());

    // A pair must never be split: ASCII filler then a pair that straddles the limit.
    String straddle = "a".repeat(65534) + DebugProtocolFixtures.EMOJI;
    r = new FlixelDebugMessageReader(w.beginMessage(1).writeString(straddle).finish());
    r.readU8();
    assertEquals(65534, r.readString(new FlixelString()).length());
  }

  @Test
  void unpairedSurrogateBecomesQuestionMark() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.beginMessage(1).writeString("a\uD83Db").finish());
    r.readU8();
    assertEquals("a?b", r.readString(new FlixelString()).toString());
  }

  @Test
  void nullStringIsEmpty() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.beginMessage(1).writeString(null).finish());
    r.readU8();
    assertEquals(0, r.readString(new FlixelString()).length());
  }

  @Test
  void stringMatchesJavaUtf8Encoding() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    ByteBuffer buf = w.beginMessage(1).writeString(DebugProtocolFixtures.UNICODE).finish();
    byte[] expected = DebugProtocolFixtures.UNICODE.getBytes(StandardCharsets.UTF_8);
    byte[] got = new byte[buf.limit() - 3];
    buf.position(3);
    buf.get(got);
    assertArrayEquals(expected, got);
  }

  @Test
  void readerToleratesTrailingBytes() {
    byte[] bytes = encode(DebugProtocolFixtures.ALL.get("msg_hello_ack"));
    byte[] padded = Arrays.copyOf(bytes, bytes.length + 5);
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(ByteBuffer.wrap(padded));
    assertEquals(FlixelDebugProtocol.MSG_HELLO_ACK, r.readU8());
    assertEquals(1, r.readU16());
    assertEquals(0, r.readU16());
    assertEquals(5, r.getRemaining());
  }

  @Test
  void readerRejectsTruncatedInput() throws IOException {
    for (String name : DebugProtocolFixtures.ALL.keySet()) {
      Op[] ops = DebugProtocolFixtures.ALL.get(name);
      byte[] full = committed(name);
      byte[] cut = Arrays.copyOf(full, full.length - 1);
      assertThrows(FlixelDebugProtocolException.class, () -> decodeAndCompare(ops, cut), name);
    }
  }

  @Test
  void readerRejectsStringLongerThanMessage() {
    byte[] bytes = { (byte) 0xFF, 0x00, 'a' };
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(ByteBuffer.wrap(bytes));
    assertThrows(FlixelDebugProtocolException.class, () -> r.readString(new FlixelString()));
  }

  @Test
  void readerRejectsNegativeByteBlock() {
    byte[] bytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(ByteBuffer.wrap(bytes));
    assertThrows(FlixelDebugProtocolException.class, r::readBytesLength);
  }

  @Test
  void malformedUtf8BecomesReplacementCharacter() {
    byte[] bytes = { 2, 0, (byte) 0xC3, 'a' };
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(ByteBuffer.wrap(bytes));
    assertEquals(Character.toString(0xFFFD), r.readString(new FlixelString()).toString());
  }

  @Test
  void skipValueWalksEveryKind() {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter();
    w.beginMessage(1).writeValue(true).writeValue(1).writeValue(2L).writeValue(3f).writeValue(4.0)
        .writeValue("five").writeErrorValue("six");
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(w.finish());
    r.readU8();
    FlixelString scratch = new FlixelString();
    for (int i = 0; i < 7; i++) {
      r.skipValue(scratch);
    }
    assertEquals(0, r.getRemaining());
  }
}
