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
package org.flixelgdx.signal;

import org.flixelgdx.GdxHeadlessExtension;
import org.flixelgdx.util.signal.FlixelSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelSignalTest {

  @Test
  void dispatchRunsCallbacksInOrder() {
    List<Integer> order = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.add(data -> order.add(1));
    signal.add(data -> order.add(2));
    signal.dispatch();
    assertEquals(List.of(1, 2), order);
  }

  @Test
  void addOnceRunsOnceThenRemoved() {
    List<Integer> runs = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.addOnce(data -> runs.add(1));
    signal.dispatch();
    signal.dispatch();
    assertEquals(1, runs.size());
  }

  @Test
  void removePreventsCallback() {
    List<Integer> runs = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    FlixelSignal.SignalHandler<Void> h = data -> runs.add(1);
    signal.add(h);
    signal.remove(h);
    signal.dispatch();
    assertTrue(runs.isEmpty());
  }

  @Test
  void clearRemovesAllCallbacks() {
    List<Integer> runs = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.add(data -> runs.add(1));
    signal.addOnce(data -> runs.add(2));
    signal.clear();
    signal.dispatch();
    assertTrue(runs.isEmpty());
  }

  @Test
  void dispatchPassesDataToHandlers() {
    List<String> received = new ArrayList<>();
    FlixelSignal<String> signal = new FlixelSignal<>();
    signal.add(received::add);
    signal.dispatch("hello");
    assertEquals(List.of("hello"), received);
  }

  @Test
  void permanentHandlersFireBeforeOnceHandlers() {
    List<Integer> order = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.addOnce(data -> order.add(2));
    signal.add(data -> order.add(1));
    signal.dispatch();
    assertEquals(List.of(1, 2), order);
  }

  @Test
  void addNullDoesNotThrowAndDoesNotAddCallback() {
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.add(null);
    signal.dispatch();
  }

  @Test
  void addOnceNullDoesNotThrowAndDoesNotAddCallback() {
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.addOnce(null);
    signal.dispatch();
  }

  @Test
  void removeFromOnceListWorks() {
    List<Integer> runs = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    FlixelSignal.SignalHandler<Void> h = data -> runs.add(1);
    signal.addOnce(h);
    signal.remove(h);
    signal.dispatch();
    assertTrue(runs.isEmpty());
  }

  @Test
  void multipleDispatchesWithPermanentCallbacks() {
    List<Integer> runs = new ArrayList<>();
    FlixelSignal<Void> signal = new FlixelSignal<>();
    signal.add(data -> runs.add(1));
    signal.dispatch();
    signal.dispatch();
    signal.dispatch();
    assertEquals(3, runs.size());
  }
}
