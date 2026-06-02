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
package org.flixelgdx.tween;

import org.flixelgdx.GdxHeadlessExtension;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.flixelgdx.tween.type.FlixelNumTween;

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
    manager.registerTweenType(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null));
    assertThrows(IllegalArgumentException.class, () ->
      manager.registerTweenType(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null)));
  }

  @Test
  void numTweenReachesEndValueLinear() {
    FlixelTweenManager manager = new FlixelTweenManager();
    manager.registerTweenType(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null));

    AtomicReference<Float> last = new AtomicReference<>(Float.NaN);
    FlixelTweenSettings settings = new FlixelTweenSettings().setDuration(1f);
    FlixelNumTween tween = manager.obtainTween(FlixelNumTween.class, () -> new FlixelNumTween(0, 0, null, null));
    tween.setTweenSettings(settings);
    tween.setTarget(0f, 10f, last::set);
    manager.addTween(tween);

    manager.update(0.5f);
    assertEquals(5f, last.get(), 1e-4f);

    manager.update(0.5f);
    assertTrue(last.get() >= 10f - 1e-3f);
  }
}
