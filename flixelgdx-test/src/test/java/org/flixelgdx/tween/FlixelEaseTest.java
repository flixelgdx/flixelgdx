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

import org.flixelgdx.tween.ease.FlixelEase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelEaseTest {

  private static final float TIGHT = 1e-5f;
  private static final float APPROX = 2e-3f;

  @ParameterizedTest(name = "{0}: f(0)=0 and f(1)=1")
  @MethodSource("exactEndpointCases")
  void endpointsAreZeroAndOne(String name, float atZero, float atOne) {
    assertEquals(0f, atZero, TIGHT, name + "(0)");
    assertEquals(1f, atOne, TIGHT, name + "(1)");
  }

  static Stream<Arguments> exactEndpointCases() {
    return Stream.of(
        Arguments.of("linear", FlixelEase.linear(0f), FlixelEase.linear(1f)),
        Arguments.of("quadIn", FlixelEase.quadIn(0f), FlixelEase.quadIn(1f)),
        Arguments.of("quadOut", FlixelEase.quadOut(0f), FlixelEase.quadOut(1f)),
        Arguments.of("quadInOut", FlixelEase.quadInOut(0f), FlixelEase.quadInOut(1f)),
        Arguments.of("cubeIn", FlixelEase.cubeIn(0f), FlixelEase.cubeIn(1f)),
        Arguments.of("cubeOut", FlixelEase.cubeOut(0f), FlixelEase.cubeOut(1f)),
        Arguments.of("cubeInOut", FlixelEase.cubeInOut(0f), FlixelEase.cubeInOut(1f)),
        Arguments.of("quartIn", FlixelEase.quartIn(0f), FlixelEase.quartIn(1f)),
        Arguments.of("quartOut", FlixelEase.quartOut(0f), FlixelEase.quartOut(1f)),
        Arguments.of("quartInOut", FlixelEase.quartInOut(0f), FlixelEase.quartInOut(1f)),
        Arguments.of("quintIn", FlixelEase.quintIn(0f), FlixelEase.quintIn(1f)),
        Arguments.of("quintOut", FlixelEase.quintOut(0f), FlixelEase.quintOut(1f)),
        Arguments.of("quintInOut", FlixelEase.quintInOut(0f), FlixelEase.quintInOut(1f)),
        Arguments.of("smoothStepIn", FlixelEase.smoothStepIn(0f), FlixelEase.smoothStepIn(1f)),
        Arguments.of("smoothStepOut", FlixelEase.smoothStepOut(0f), FlixelEase.smoothStepOut(1f)),
        Arguments.of("smoothStepInOut", FlixelEase.smoothStepInOut(0f), FlixelEase.smoothStepInOut(1f)),
        Arguments.of("smootherStepIn", FlixelEase.smootherStepIn(0f), FlixelEase.smootherStepIn(1f)),
        Arguments.of("smootherStepOut", FlixelEase.smootherStepOut(0f), FlixelEase.smootherStepOut(1f)),
        Arguments.of("smootherStepInOut", FlixelEase.smootherStepInOut(0f), FlixelEase.smootherStepInOut(1f)),
        Arguments.of("sineIn", FlixelEase.sineIn(0f), FlixelEase.sineIn(1f)),
        Arguments.of("sineOut", FlixelEase.sineOut(0f), FlixelEase.sineOut(1f)),
        Arguments.of("sineInOut", FlixelEase.sineInOut(0f), FlixelEase.sineInOut(1f)),
        Arguments.of("bounceIn", FlixelEase.bounceIn(0f), FlixelEase.bounceIn(1f)),
        Arguments.of("bounceOut", FlixelEase.bounceOut(0f), FlixelEase.bounceOut(1f)),
        Arguments.of("bounceInOut", FlixelEase.bounceInOut(0f), FlixelEase.bounceInOut(1f)),
        Arguments.of("circIn", FlixelEase.circIn(0f), FlixelEase.circIn(1f)),
        Arguments.of("circOut", FlixelEase.circOut(0f), FlixelEase.circOut(1f)),
        Arguments.of("circInOut", FlixelEase.circInOut(0f), FlixelEase.circInOut(1f)),
        Arguments.of("backIn", FlixelEase.backIn(0f), FlixelEase.backIn(1f)),
        Arguments.of("backOut", FlixelEase.backOut(0f), FlixelEase.backOut(1f)),
        Arguments.of("backInOut", FlixelEase.backInOut(0f), FlixelEase.backInOut(1f)));
  }

  @Test
  void expoEndpointsApproximate() {
    // Math.pow(2, 10*(0-1)) is ~0.001, not exactly 0, so a loose tolerance is required.
    assertEquals(0f, FlixelEase.expoIn(0f), APPROX);
    assertEquals(1f, FlixelEase.expoIn(1f), TIGHT);
    assertEquals(0f, FlixelEase.expoOut(0f), TIGHT);
    assertEquals(1f, FlixelEase.expoOut(1f), APPROX);
    assertEquals(0f, FlixelEase.expoInOut(0f), APPROX);
    assertEquals(1f, FlixelEase.expoInOut(1f), APPROX);
  }

  @Test
  void elasticEndpointsApproximate() {
    // Elastic functions use sin/pow combinations that do not reach exactly 0 or 1 at both edges.
    assertEquals(0f, FlixelEase.elasticIn(0f), APPROX);
    assertEquals(1f, FlixelEase.elasticIn(1f), TIGHT);
    assertEquals(0f, FlixelEase.elasticOut(0f), TIGHT);
    assertEquals(1f, FlixelEase.elasticOut(1f), APPROX);
    assertEquals(0f, FlixelEase.elasticInOut(0f), APPROX);
    assertEquals(1f, FlixelEase.elasticInOut(1f), APPROX);
  }

  @ParameterizedTest(name = "{0}(0.5) = 0.5")
  @MethodSource("inOutMidpointCases")
  void inOutPassesThroughHalf(String name, float midValue) {
    assertEquals(0.5f, midValue, TIGHT, name + "(0.5)");
  }

  static Stream<Arguments> inOutMidpointCases() {
    return Stream.of(
        Arguments.of("quadInOut", FlixelEase.quadInOut(0.5f)),
        Arguments.of("cubeInOut", FlixelEase.cubeInOut(0.5f)),
        Arguments.of("quartInOut", FlixelEase.quartInOut(0.5f)),
        Arguments.of("quintInOut", FlixelEase.quintInOut(0.5f)),
        Arguments.of("smoothStepInOut", FlixelEase.smoothStepInOut(0.5f)),
        Arguments.of("smootherStepInOut", FlixelEase.smootherStepInOut(0.5f)),
        Arguments.of("sineInOut", FlixelEase.sineInOut(0.5f)),
        Arguments.of("bounceInOut", FlixelEase.bounceInOut(0.5f)),
        Arguments.of("circInOut", FlixelEase.circInOut(0.5f)));
  }

  @ParameterizedTest(name = "{0}: value increases from t=0.25 to t=0.75")
  @MethodSource("monotonicityCases")
  void functionIncreasesFromQuarterToThreeQuarters(String name, float at25, float at75) {
    assertTrue(at25 < at75, name + " should produce a larger value at t=0.75 than t=0.25");
  }

  static Stream<Arguments> monotonicityCases() {
    // Elastic is excluded, as it oscillates and does not satisfy this property in all sub-ranges.
    return Stream.of(
        Arguments.of("linear", FlixelEase.linear(0.25f), FlixelEase.linear(0.75f)),
        Arguments.of("quadIn", FlixelEase.quadIn(0.25f), FlixelEase.quadIn(0.75f)),
        Arguments.of("quadOut", FlixelEase.quadOut(0.25f), FlixelEase.quadOut(0.75f)),
        Arguments.of("quadInOut", FlixelEase.quadInOut(0.25f), FlixelEase.quadInOut(0.75f)),
        Arguments.of("cubeIn", FlixelEase.cubeIn(0.25f), FlixelEase.cubeIn(0.75f)),
        Arguments.of("cubeOut", FlixelEase.cubeOut(0.25f), FlixelEase.cubeOut(0.75f)),
        Arguments.of("cubeInOut", FlixelEase.cubeInOut(0.25f), FlixelEase.cubeInOut(0.75f)),
        Arguments.of("quartIn", FlixelEase.quartIn(0.25f), FlixelEase.quartIn(0.75f)),
        Arguments.of("quartOut", FlixelEase.quartOut(0.25f), FlixelEase.quartOut(0.75f)),
        Arguments.of("quartInOut", FlixelEase.quartInOut(0.25f), FlixelEase.quartInOut(0.75f)),
        Arguments.of("quintIn", FlixelEase.quintIn(0.25f), FlixelEase.quintIn(0.75f)),
        Arguments.of("quintOut", FlixelEase.quintOut(0.25f), FlixelEase.quintOut(0.75f)),
        Arguments.of("quintInOut", FlixelEase.quintInOut(0.25f), FlixelEase.quintInOut(0.75f)),
        Arguments.of("smoothStepIn", FlixelEase.smoothStepIn(0.25f), FlixelEase.smoothStepIn(0.75f)),
        Arguments.of("smoothStepOut", FlixelEase.smoothStepOut(0.25f), FlixelEase.smoothStepOut(0.75f)),
        Arguments.of("smoothStepInOut", FlixelEase.smoothStepInOut(0.25f), FlixelEase.smoothStepInOut(0.75f)),
        Arguments.of("smootherStepIn", FlixelEase.smootherStepIn(0.25f), FlixelEase.smootherStepIn(0.75f)),
        Arguments.of("smootherStepOut", FlixelEase.smootherStepOut(0.25f), FlixelEase.smootherStepOut(0.75f)),
        Arguments.of("smootherStepInOut", FlixelEase.smootherStepInOut(0.25f), FlixelEase.smootherStepInOut(0.75f)),
        Arguments.of("sineIn", FlixelEase.sineIn(0.25f), FlixelEase.sineIn(0.75f)),
        Arguments.of("sineOut", FlixelEase.sineOut(0.25f), FlixelEase.sineOut(0.75f)),
        Arguments.of("sineInOut", FlixelEase.sineInOut(0.25f), FlixelEase.sineInOut(0.75f)),
        Arguments.of("bounceIn", FlixelEase.bounceIn(0.25f), FlixelEase.bounceIn(0.75f)),
        Arguments.of("bounceOut", FlixelEase.bounceOut(0.25f), FlixelEase.bounceOut(0.75f)),
        Arguments.of("bounceInOut", FlixelEase.bounceInOut(0.25f), FlixelEase.bounceInOut(0.75f)),
        Arguments.of("circIn", FlixelEase.circIn(0.25f), FlixelEase.circIn(0.75f)),
        Arguments.of("circOut", FlixelEase.circOut(0.25f), FlixelEase.circOut(0.75f)),
        Arguments.of("circInOut", FlixelEase.circInOut(0.25f), FlixelEase.circInOut(0.75f)),
        Arguments.of("expoIn", FlixelEase.expoIn(0.25f), FlixelEase.expoIn(0.75f)),
        Arguments.of("expoOut", FlixelEase.expoOut(0.25f), FlixelEase.expoOut(0.75f)),
        Arguments.of("expoInOut", FlixelEase.expoInOut(0.25f), FlixelEase.expoInOut(0.75f)),
        Arguments.of("backIn", FlixelEase.backIn(0.25f), FlixelEase.backIn(0.75f)),
        Arguments.of("backOut", FlixelEase.backOut(0.25f), FlixelEase.backOut(0.75f)),
        Arguments.of("backInOut", FlixelEase.backInOut(0.25f), FlixelEase.backInOut(0.75f)));
  }

  @Test
  void inFunctionsAreSlowerThanLinearAtMidpoint() {
    assertTrue(FlixelEase.quadIn(0.5f) < 0.5f, "quadIn");
    assertTrue(FlixelEase.cubeIn(0.5f) < 0.5f, "cubeIn");
    assertTrue(FlixelEase.quartIn(0.5f) < 0.5f, "quartIn");
    assertTrue(FlixelEase.quintIn(0.5f) < 0.5f, "quintIn");
    assertTrue(FlixelEase.sineIn(0.5f) < 0.5f, "sineIn");
    assertTrue(FlixelEase.expoIn(0.5f) < 0.5f, "expoIn");
    assertTrue(FlixelEase.circIn(0.5f) < 0.5f, "circIn");
  }

  @Test
  void outFunctionsAreFasterThanLinearAtMidpoint() {
    assertTrue(FlixelEase.quadOut(0.5f) > 0.5f, "quadOut");
    assertTrue(FlixelEase.cubeOut(0.5f) > 0.5f, "cubeOut");
    assertTrue(FlixelEase.quartOut(0.5f) > 0.5f, "quartOut");
    assertTrue(FlixelEase.quintOut(0.5f) > 0.5f, "quintOut");
    assertTrue(FlixelEase.sineOut(0.5f) > 0.5f, "sineOut");
    assertTrue(FlixelEase.expoOut(0.5f) > 0.5f, "expoOut");
    assertTrue(FlixelEase.circOut(0.5f) > 0.5f, "circOut");
  }
}
