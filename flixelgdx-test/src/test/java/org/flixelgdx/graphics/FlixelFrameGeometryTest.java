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

/**
 * Verifies the pure Sparrow-frame geometry that {@link org.flixelgdx.FlixelSprite#draw} relies on.
 *
 * <p>These tests use real {@code SubTexture} values taken from Friday Night Funkin' atlases
 * (Boyfriend, Pico, Darnell), the same assets that exposed the original rendering bug. Each row is
 * {@code {regionWidth, regionHeight, offsetX, offsetY}} where {@code offset = -frameX / -frameY}.
 * Because the helpers are pure integer math, no GPU texture or libGDX context is required.
 */
class FlixelFrameGeometryTest {

  @Test
  void verticalInsetStaysConstantSoFeetArePlanted() {
    // Darnell "Idle": the trimmed region changes height every frame, but the bottom inset must stay
    // identical or the character's feet would bob up and down as the clip loops.
    int darnellSourceHeight = 665;
    int[][] darnellIdle = {
        {374, 471, 44, 79},
        {391, 482, 47, 68},
        {388, 487, 47, 63},
    };
    assertConstantBottomInset(darnellSourceHeight, darnellIdle, 115);

    // Pico "Idle Dance": planted one pixel above the source box bottom.
    assertConstantBottomInset(475, new int[][] {
        {435, 461, 18, 13},
        {435, 461, 18, 13},
        {442, 468, 11, 6},
    }, 1);

    // Boyfriend "Note Left": planted flush with the source box bottom.
    assertConstantBottomInset(406, new int[][] {
        {383, 406, 0, 0},
        {383, 406, 0, 0},
        {374, 404, 11, 2},
    }, 0);
  }

  @Test
  void horizontalFlipMirrorsAroundSourceBox() {
    // Darnell idle frame 1: trimmed 374 wide inside a 643 wide source box, 44px gap on the left.
    int sourceWidth = 643;
    int regionWidth = 374;
    int offsetX = 44;
    assertEquals(44, FlixelFrame.regionInsetX(sourceWidth, regionWidth, offsetX, false),
        "Unflipped inset should equal the raw left offset.");
    // Flipping must swap the left and right gaps: 643 - 374 - 44 = 225.
    assertEquals(225, FlixelFrame.regionInsetX(sourceWidth, regionWidth, offsetX, true),
        "Flipped inset should mirror around the source box, not the trimmed region.");
  }

  @Test
  void verticalFlipSwapsTopAndBottomGaps() {
    int sourceHeight = 665;
    int regionHeight = 471;
    int offsetY = 79;
    assertEquals(115, FlixelFrame.regionInsetY(sourceHeight, regionHeight, offsetY, false));
    // A vertical flip leaves the (former top) offset as the new bottom inset.
    assertEquals(79, FlixelFrame.regionInsetY(sourceHeight, regionHeight, offsetY, true));
  }

  @Test
  void untrimmedFrameHasZeroInsetEvenWhenFlipped() {
    // A frame with no trim (source size == region size, no offset) sits flush in every orientation.
    int size = 200;
    assertEquals(0, FlixelFrame.regionInsetX(size, size, 0, false));
    assertEquals(0, FlixelFrame.regionInsetX(size, size, 0, true));
    assertEquals(0, FlixelFrame.regionInsetY(size, size, 0, false));
    assertEquals(0, FlixelFrame.regionInsetY(size, size, 0, true));
  }

  /**
   * Asserts that every frame in {@code frames} places its trimmed region the same distance above the
   * source box bottom (the planted-feet invariant), and that the distance is {@code expectedInset}.
   *
   * @param sourceHeight The shared untrimmed frame height for the animation.
   * @param frames Rows of {@code {regionWidth, regionHeight, offsetX, offsetY}}.
   * @param expectedInset The bottom inset all frames must share, in pixels.
   */
  private static void assertConstantBottomInset(int sourceHeight, int[][] frames, int expectedInset) {
    for (int[] frame : frames) {
      int regionHeight = frame[1];
      int offsetY = frame[3];
      assertEquals(expectedInset, FlixelFrame.regionInsetY(sourceHeight, regionHeight, offsetY, false),
          "Every frame in an animation must share the same bottom inset to keep feet planted.");
    }
  }
}
