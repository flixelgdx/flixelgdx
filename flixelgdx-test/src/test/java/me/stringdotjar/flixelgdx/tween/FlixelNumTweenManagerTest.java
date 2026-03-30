/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween;

import me.stringdotjar.flixelgdx.GdxHeadlessExtension;
import me.stringdotjar.flixelgdx.tween.builders.FlixelNumTweenBuilder;
import me.stringdotjar.flixelgdx.tween.type.FlixelNumTween;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelNumTweenManagerTest {

  @Test
  void duplicateRegistrationThrows() {
    FlixelTweenManager manager = new FlixelTweenManager();
    manager.registerTweenType(FlixelNumTween.class, FlixelNumTweenBuilder.class, () -> new FlixelNumTween(0, 0, null, null));
    assertThrows(IllegalArgumentException.class, () ->
      manager.registerTweenType(FlixelNumTween.class, FlixelNumTweenBuilder.class, () -> new FlixelNumTween(0, 0, null, null)));
  }

  @Test
  void numTweenReachesEndValueLinear() {
    FlixelTweenManager manager = new FlixelTweenManager();
    manager.registerTweenType(FlixelNumTween.class, FlixelNumTweenBuilder.class, () -> new FlixelNumTween(0, 0, null, null));

    AtomicReference<Float> last = new AtomicReference<>(Float.NaN);
    new FlixelNumTweenBuilder()
      .setManager(manager)
      .from(0f)
      .to(10f)
      .setCallback(last::set)
      .setDuration(1f)
      .start();

    manager.update(0.5f);
    assertEquals(5f, last.get(), 1e-4f);

    manager.update(0.5f);
    assertTrue(last.get() >= 10f - 1e-3f);
  }
}
