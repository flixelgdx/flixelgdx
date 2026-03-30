/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.signal;

import me.stringdotjar.flixelgdx.GdxHeadlessExtension;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
}
