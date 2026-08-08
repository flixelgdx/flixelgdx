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
package org.flixelgdx.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelInputMultiplexerTest {

  /** Records how many events it saw and can be told to consume them. */
  private static final class Recorder implements FlixelInputProcessor {

    int keyDownCount;

    private final boolean consume;

    Recorder(boolean consume) {
      this.consume = consume;
    }

    @Override
    public boolean keyDown(int keycode) {
      keyDownCount++;
      return consume;
    }
  }

  @Test
  void dispatchesInAddOrderUntilConsumed() {
    Recorder first = new Recorder(true);
    Recorder second = new Recorder(false);
    FlixelInputMultiplexer mux = new FlixelInputMultiplexer();
    mux.addProcessor(first);
    mux.addProcessor(second);

    assertTrue(mux.keyDown(42));
    assertEquals(1, first.keyDownCount);
    assertEquals(0, second.keyDownCount, "A consumed event must not reach later processors.");
  }

  @Test
  void unconsumedEventReachesEveryProcessor() {
    Recorder first = new Recorder(false);
    Recorder second = new Recorder(false);
    FlixelInputMultiplexer mux = new FlixelInputMultiplexer();
    mux.addProcessor(first);
    mux.addProcessor(second);

    assertFalse(mux.keyDown(42));
    assertEquals(1, first.keyDownCount);
    assertEquals(1, second.keyDownCount);
  }

  @Test
  void insertingAtFrontGivesPriority() {
    Recorder existing = new Recorder(false);
    Recorder inserted = new Recorder(true);
    FlixelInputMultiplexer mux = new FlixelInputMultiplexer();
    mux.addProcessor(existing);
    mux.addProcessor(0, inserted);

    assertTrue(mux.keyDown(42));
    assertEquals(1, inserted.keyDownCount);
    assertEquals(0, existing.keyDownCount);
  }

  @Test
  void removedProcessorStopsReceivingEvents() {
    Recorder recorder = new Recorder(false);
    FlixelInputMultiplexer mux = new FlixelInputMultiplexer();
    mux.addProcessor(recorder);

    assertTrue(mux.removeProcessor(recorder));
    assertEquals(0, mux.getSize());
    assertFalse(mux.keyDown(42));
    assertEquals(0, recorder.keyDownCount);
  }

  @Test
  void emptyMultiplexerReportsUnhandled() {
    FlixelInputMultiplexer mux = new FlixelInputMultiplexer();
    assertFalse(mux.keyDown(42));
    assertFalse(mux.touchDown(0, 0, 0, 0));
    assertFalse(mux.scrolled(0f, 0f));
  }
}
