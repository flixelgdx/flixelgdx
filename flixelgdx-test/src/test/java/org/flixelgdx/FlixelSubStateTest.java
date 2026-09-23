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
package org.flixelgdx;

import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link FlixelSubState}'s own background overlay color, making sure it never reaches
 * into {@link Flixel#cameras} the way {@link FlixelState#setBgColor(FlixelColor)} does.
 *
 * <p>No {@link FlixelGame} is running in this test, so the inherited
 * {@link FlixelState#setBgColor(FlixelColor)} would already be a no-op here (it bails out when
 * {@link Flixel#game} is {@code null}). To still prove a substate never touches a camera even
 * when a game IS running, this populates {@link Flixel#cameras} directly and asserts its
 * contents stay untouched by every substate operation exercised below.
 */
class FlixelSubStateTest {

  private FlixelCamera camera;

  @BeforeEach
  void setUp() {
    camera = new FlixelCamera(200, 150);
    camera.bgColor.set(FlixelColor.BLUE);
    Flixel.cameras.add(camera);
  }

  @AfterEach
  void tearDown() {
    Flixel.cameras.removeValue(camera, true);
  }

  @Test
  void constructorNeverTouchesCameraBackground() {
    new TestSubState(new FlixelColor(FlixelColor.RED));
    assertEquals(FlixelColor.BLUE.getRgba8888(), camera.bgColor.getRgba8888(),
        "opening a substate must not recolor any camera");
  }

  @Test
  void setBgColorOnlyChangesItsOwnOverlay() {
    TestSubState sub = new TestSubState(FlixelColor.CLEAR);
    sub.setBgColor(new FlixelColor(FlixelColor.RED));
    assertEquals(FlixelColor.RED.getRgba8888(), sub.getBgColor().getRgba8888());
    assertEquals(FlixelColor.BLUE.getRgba8888(), camera.bgColor.getRgba8888(),
        "FlixelSubState.setBgColor must not touch any camera");
  }

  @Test
  void getBgColorReturnsItsOwnColorInsteadOfACamerasColor() {
    TestSubState sub = new TestSubState(new FlixelColor(FlixelColor.RED));
    assertEquals(FlixelColor.RED.getRgba8888(), sub.getBgColor().getRgba8888());
  }

  @Test
  void constructorCopiesTheGivenColorInsteadOfStoringItsReference() {
    FlixelColor source = new FlixelColor(FlixelColor.RED);
    TestSubState sub = new TestSubState(source);
    source.set(FlixelColor.GREEN);
    assertEquals(FlixelColor.RED.getRgba8888(), sub.getBgColor().getRgba8888(),
        "the substate must keep its own copy of the color, not the caller's instance");
  }

  private static final class TestSubState extends FlixelSubState {

    TestSubState(FlixelColor bgColor) {
      super(bgColor);
    }
  }
}
