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
package org.flixelgdx.graphics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelRenderStatsTest {

  @Test
  void countersKeepLastFrameWhileTheNextOneRecords() {
    FlixelRenderStats stats = new FlixelRenderStats();
    stats.beginFrame();
    stats.addPass();
    stats.addDraw(2, 4);
    stats.addDraw(6, 12);
    stats.addSubmit();
    stats.endFrame();

    // The game and the debugger read the stats between beginFrame() and endFrame().
    stats.beginFrame();
    stats.addDraw(2, 4);
    assertEquals(2, stats.draws, "draws");
    assertEquals(8, stats.triangles, "triangles");
    assertEquals(16, stats.vertices, "vertices");
    assertEquals(1, stats.passes, "passes");
    assertEquals(1, stats.submits, "submits");

    stats.endFrame();
    assertEquals(1, stats.draws, "draws after the second frame");
    assertEquals(2, stats.triangles, "triangles after the second frame");
    assertEquals(0, stats.passes, "passes after the second frame");
  }

  @Test
  void skippedFrameKeepsPreviousValues() {
    FlixelRenderStats stats = new FlixelRenderStats();
    stats.beginFrame();
    stats.addDraw(2, 4);
    stats.endFrame();

    stats.beginFrame();
    assertEquals(1, stats.draws, "a frame without endFrame() does not clear the published values");
  }
}
