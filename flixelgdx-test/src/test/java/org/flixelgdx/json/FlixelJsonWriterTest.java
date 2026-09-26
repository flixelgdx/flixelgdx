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
package org.flixelgdx.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link FlixelJsonWriter}, covering both compact and pretty-print output. */
class FlixelJsonWriterTest {

  @Test
  void compactObjectByDefault() {
    String json = new FlixelJsonWriter()
        .beginObject()
        .name("score").value(1200L)
        .name("name").value("Ada")
        .endObject()
        .toString();

    assertEquals("{\"score\":1200,\"name\":\"Ada\"}", json);
  }

  @Test
  void prettyPrintObjectWithTwoSpaceIndent() {
    String json = new FlixelJsonWriter()
        .setIndent(2)
        .beginObject()
        .name("score").value(1200L)
        .name("name").value("Ada")
        .endObject()
        .toString();

    String expected = "{\n  \"score\": 1200,\n  \"name\": \"Ada\"\n}";
    assertEquals(expected, json);
  }

  @Test
  void prettyPrintNestedObject() {
    String json = new FlixelJsonWriter()
        .setIndent(2)
        .beginObject()
        .name("player")
        .beginObject()
        .name("hp").value(100L)
        .endObject()
        .endObject()
        .toString();

    String expected = "{\n  \"player\": {\n    \"hp\": 100\n  }\n}";
    assertEquals(expected, json);
  }

  @Test
  void prettyPrintArrayElements() {
    String json = new FlixelJsonWriter()
        .setIndent(2)
        .beginObject()
        .name("scores")
        .beginArray()
        .value(10L)
        .value(20L)
        .value(30L)
        .endArray()
        .endObject()
        .toString();

    String expected = "{\n  \"scores\": [\n    10,\n    20,\n    30\n  ]\n}";
    assertEquals(expected, json);
  }

  @Test
  void compactOutputIsUnchangedWhenIndentIsZero() {
    String compact = new FlixelJsonWriter()
        .beginObject()
        .name("x").value(1L)
        .endObject()
        .toString();

    String explicit = new FlixelJsonWriter()
        .setIndent(0)
        .beginObject()
        .name("x").value(1L)
        .endObject()
        .toString();

    assertEquals(compact, explicit);
  }

  @Test
  void setIndentRejectsNegativeValues() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelJsonWriter().setIndent(-1));
  }

  @Test
  void prettyPrintOutputIsParseable() {
    String json = new FlixelJsonWriter()
        .setIndent(4)
        .beginObject()
        .name("alive").value(true)
        .name("count").value(7L)
        .endObject()
        .toString();

    FlixelJsonValue root = FlixelJson.parse(json);
    assertTrue(root.getBool("alive", false));
    assertEquals(7, root.getInt("count", 0));
  }

  @Test
  void isCompleteReflectsWriterProgress() {
    FlixelJsonWriter writer = new FlixelJsonWriter();
    assertFalse(writer.isComplete());

    writer.beginObject();
    assertFalse(writer.isComplete());

    writer.name("x").value(1L);
    assertFalse(writer.isComplete());

    writer.endObject();
    assertTrue(writer.isComplete());
  }

  @Test
  void topLevelScalarIsAllowedAndMarksTheDocumentComplete() {
    FlixelJsonWriter writer = new FlixelJsonWriter();
    writer.value(42L);

    assertEquals("42", writer.toString());
    assertTrue(writer.isComplete());
  }

  @Test
  void endObjectTwiceThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject().endObject();

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  @Test
  void endObjectWithNothingOpenThrows() {
    assertThrows(IllegalStateException.class, () -> new FlixelJsonWriter().endObject());
  }

  @Test
  void endObjectOnAnOpenArrayThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginArray();

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  @Test
  void endArrayOnAnOpenObjectThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject();

    assertThrows(IllegalStateException.class, writer::endArray);
  }

  @Test
  void endArrayWithNothingOpenThrows() {
    assertThrows(IllegalStateException.class, () -> new FlixelJsonWriter().endArray());
  }

  @Test
  void endObjectWhileNameIsDanglingThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject().name("x");

    assertThrows(IllegalStateException.class, writer::endObject);
  }

  @Test
  void nameAtTopLevelThrows() {
    assertThrows(IllegalStateException.class, () -> new FlixelJsonWriter().name("x"));
  }

  @Test
  void nameInsideAnArrayThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginArray();

    assertThrows(IllegalStateException.class, () -> writer.name("x"));
  }

  @Test
  void nameCalledTwiceInARowThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject().name("x");

    assertThrows(IllegalStateException.class, () -> writer.name("y"));
  }

  @Test
  void valueTwiceAfterOneNameThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject().name("x").value(1L);

    assertThrows(IllegalStateException.class, () -> writer.value(2L));
  }

  @Test
  void valueInsideAnObjectWithoutNameThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject();

    assertThrows(IllegalStateException.class, () -> writer.value(1L));
  }

  @Test
  void rawInsideAnObjectWithoutNameThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject();

    assertThrows(IllegalStateException.class, () -> writer.raw("1"));
  }

  @Test
  void beginObjectInsideAnObjectWithoutNameThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject();

    assertThrows(IllegalStateException.class, writer::beginObject);
  }

  @Test
  void beginArrayInsideAnObjectWithoutNameThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject();

    assertThrows(IllegalStateException.class, writer::beginArray);
  }

  @Test
  void secondTopLevelValueAfterDocumentCompleteThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject().endObject();

    assertThrows(IllegalStateException.class, writer::beginObject);
  }

  @Test
  void secondTopLevelScalarAfterDocumentCompleteThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().value(1L);

    assertThrows(IllegalStateException.class, () -> writer.value(2L));
  }

  @Test
  void setIndentAfterOutputHasBeenWrittenThrows() {
    FlixelJsonWriter writer = new FlixelJsonWriter().beginObject();

    assertThrows(IllegalStateException.class, () -> writer.setIndent(2));
  }

  @Test
  void valueRejectsNaN() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelJsonWriter().value(Double.NaN));
  }

  @Test
  void valueRejectsPositiveInfinity() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FlixelJsonWriter().value(Double.POSITIVE_INFINITY));
  }

  @Test
  void valueRejectsNegativeInfinity() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FlixelJsonWriter().value(Double.NEGATIVE_INFINITY));
  }

  @Test
  void arraysInsideObjectsStillFollowValidSequencing() {
    String json = new FlixelJsonWriter()
        .beginObject()
        .name("scores")
        .beginArray()
        .value(1L)
        .value(2L)
        .endArray()
        .endObject()
        .toString();

    assertEquals("{\"scores\":[1,2]}", json);
  }
}
