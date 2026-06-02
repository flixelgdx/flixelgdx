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
package org.flixelgdx.input.mouse;

import com.badlogic.gdx.Input;
import org.flixelgdx.GdxHeadlessExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelMouseManagerTest {

  @Test
  void scrollAccumulatesUntilEndFrame() {
    FlixelMouseManager m = new FlixelMouseManager();
    m.getInputProcessor().scrolled(0f, -2f);
    assertEquals(-2f, m.getScrollDeltaY(), 1e-6f);
    m.endFrame();
    assertEquals(0f, m.getScrollDeltaY(), 1e-6f);
  }

  @Test
  void buttonIndicesAreBounded() {
    FlixelMouseManager m = new FlixelMouseManager();
    m.update();
    assertFalse(m.justPressed(-1));
    assertFalse(m.justPressed(Input.Buttons.FORWARD + 5));
  }
}
