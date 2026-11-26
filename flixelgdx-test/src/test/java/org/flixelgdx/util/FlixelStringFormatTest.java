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
package org.flixelgdx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlixelStringFormatTest {

  @Test
  void replacesSinglePlaceholder() {
    FlixelString fs = new FlixelString();
    assertEquals("Score: 42", fs.setFormat("Score: {0}", "42").toString());
  }

  @Test
  void placeholdersCanRepeatAndReorder() {
    FlixelString fs = new FlixelString();
    fs.setFormat("{1} beat {0}, {1} wins", "Bob", "Ann");
    assertEquals("Ann beat Bob, Ann wins", fs.toString());
  }

  @Test
  void threeArgumentOverload() {
    FlixelString fs = new FlixelString();
    assertEquals("a-b-c", fs.setFormat("{0}-{1}-{2}", "a", "b", "c").toString());
  }

  @Test
  void varargsHandlesManyArguments() {
    FlixelString fs = new FlixelString();
    fs.setFormat("{0}{1}{2}{3}{4}{10}", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
    assertEquals("abcdek", fs.toString());
  }

  @Test
  void doubledBracesAreEscapes() {
    FlixelString fs = new FlixelString();
    assertEquals("{0} = x}", fs.setFormat("{{0}} = {0}}}", "x").toString());
  }

  @Test
  void unknownPlaceholdersAreKept() {
    FlixelString fs = new FlixelString();
    assertEquals("{1} {name} { {} x", fs.setFormat("{1} {name} { {} {0}", "x").toString());
  }

  @Test
  void unclosedPlaceholderIsKept() {
    FlixelString fs = new FlixelString();
    assertEquals("Hi {0", fs.setFormat("Hi {0", "x").toString());
  }

  @Test
  void nullArgumentWritesNull() {
    FlixelString fs = new FlixelString();
    assertEquals("v=null", fs.setFormat("v={0}", (CharSequence) null).toString());
  }

  @Test
  void acceptsFlixelStringArguments() {
    FlixelString number = new FlixelString().set(7);
    FlixelString fs = new FlixelString();
    assertEquals("Lives: 7", fs.setFormat("Lives: {0}", number).toString());
  }

  @Test
  void concatFormatAppends() {
    FlixelString fs = new FlixelString("HP ");
    fs.concatFormat("{0}/{1}", "3", "5");
    assertEquals("HP 3/5", fs.toString());
  }

  @Test
  void setFormatClearsFirst() {
    FlixelString fs = new FlixelString("old text");
    assertEquals("new", fs.setFormat("{0}", "new").toString());
  }

  @Test
  void formattingIntoItselfThrows() {
    FlixelString fs = new FlixelString("{0}");
    assertThrows(IllegalArgumentException.class, () -> fs.setFormat(fs, "x"));
    assertThrows(IllegalArgumentException.class, () -> fs.concatFormat("{0}", fs));
  }
}
