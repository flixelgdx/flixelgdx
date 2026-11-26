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
package org.flixelgdx.xml;

import org.flixelgdx.collections.FlixelArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelXmlTest {

  private static final String ATLAS = """
      <?xml version="1.0" encoding="utf-8"?>
      <!-- Created with a texture packer -->
      <!DOCTYPE TextureAtlas>
      <TextureAtlas imagePath="player.png">
        <SubTexture name="idle0000" x="0" y="0" width="32" height="48" frameX="-2" rotated="true"/>
        <SubTexture name='run0000' x = "32" y="0" width="32" height="48" />
        <Note>Tom &amp; Jerry &lt;3</Note>
        <![CDATA[ ignored ]]>
      </TextureAtlas>
      """;

  @Test
  void parsesElementsAttributesAndChildren() {
    FlixelXmlElement root = FlixelXml.parse(ATLAS);
    assertEquals("TextureAtlas", root.getName());
    assertEquals("player.png", root.getAttribute("imagePath", null));
    assertEquals(3, root.getChildCount());

    FlixelArray<FlixelXmlElement> frames = root.getChildrenByName("SubTexture");
    assertEquals(2, frames.getSize());
    FlixelXmlElement idle = frames.get(0);
    assertEquals("idle0000", idle.getAttribute("name", null));
    assertEquals(48, idle.getIntAttribute("height", 0));
    assertEquals(-2f, idle.getFloatAttribute("frameX", 0f));
    assertTrue(idle.getBooleanAttribute("rotated", false));
    assertNull(idle.getText());

    FlixelXmlElement run = frames.get(1);
    assertEquals("run0000", run.getAttribute("name", null), "single quotes work too");
    assertEquals(32, run.getIntAttribute("x", 0), "spaces around = are allowed");
  }

  @Test
  void textContentIsTrimmedAndUnescaped() {
    FlixelXmlElement note = FlixelXml.parse(ATLAS).getChild(2);
    assertEquals("Note", note.getName());
    assertEquals("Tom & Jerry <3", note.getText());
    FlixelXmlElement q = FlixelXml.parse("<q a=\"&quot;hi&quot; &apos;there&apos; &gt;\">  plain  </q>");
    assertEquals("\"hi\" 'there' >", q.getAttribute("a", null));
    assertEquals("plain", q.getText());
  }

  @Test
  void missingOrBadAttributesFallBackToDefaults() {
    FlixelXmlElement e = FlixelXml.parse("<e n=\"abc\" f=\"x1\" b=\"YES\" ok=\" TRUE \"/>");
    assertEquals(7, e.getIntAttribute("n", 7));
    assertEquals(7, e.getIntAttribute("missing", 7));
    assertEquals(1.5f, e.getFloatAttribute("f", 1.5f));
    assertEquals(1.5f, e.getFloatAttribute("missing", 1.5f));
    assertFalse(e.getBooleanAttribute("b", true));
    assertTrue(e.getBooleanAttribute("ok", false));
    assertTrue(e.getBooleanAttribute("missing", true));
    assertEquals("d", e.getAttribute("missing", "d"));
    assertTrue(e.hasAttribute("n"));
    assertFalse(e.hasAttribute("missing"));
    assertEquals(0, e.getChildrenByName("x").getSize());
  }

  @Test
  void malformedAttributesAreSkipped() {
    FlixelXmlElement e = FlixelXml.parse("<e flag noquote=bare good=\"1\"></e>");
    assertFalse(e.hasAttribute("flag"));
    assertEquals(1, e.getIntAttribute("good", 0));
  }

  @Test
  void nestedElementsAndUnclosedDocuments() {
    FlixelXmlElement root = FlixelXml.parse("<a><b><c/></b><b/>tail</a>");
    assertEquals(2, root.getChildCount());
    assertEquals(1, root.getChild(0).getChildCount());
    assertEquals("tail", root.getText());

    FlixelXmlElement open = FlixelXml.parse("<a><b>");
    assertEquals("a", open.getName());
    FlixelXmlElement cut = FlixelXml.parse("<a x=\"1");
    assertEquals("a", cut.getName());
    assertThrows(IllegalArgumentException.class, () -> FlixelXml.parse("<?xml version=\"1.0\"<a/>"),
        "an unclosed declaration swallows the rest of the document");
  }

  @Test
  void documentsWithoutARootAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlixelXml.parse(""));
    assertThrows(IllegalArgumentException.class, () -> FlixelXml.parse("just text"));
    assertThrows(IllegalArgumentException.class, () -> FlixelXml.parse("<!-- only a comment -->"));
    assertThrows(IllegalArgumentException.class, () -> FlixelXml.parse("</closing>"));
    assertThrows(IllegalArgumentException.class, () -> FlixelXml.parse("<!-- never closed"));
  }
}
