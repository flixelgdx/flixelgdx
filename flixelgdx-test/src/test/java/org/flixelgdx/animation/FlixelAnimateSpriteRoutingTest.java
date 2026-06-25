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
package org.flixelgdx.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure decision that routes a clip to the rig draw path versus the standard Sparrow /
 * atlas frame path on a {@link FlixelAnimateSprite}. The full render path needs a GPU texture, so
 * this focuses on the texture-free rule: the rig is used only when one is installed and it actually
 * holds the clip being played.
 */
class FlixelAnimateSpriteRoutingTest {

  @Test
  void rigPathRequiresBothARigAndAMatchingClip() {
    assertTrue(FlixelAnimateSprite.useRigClip(true, true), "Rig installed and owns the clip.");
    assertFalse(
        FlixelAnimateSprite.useRigClip(true, false),
        "A merged Sparrow clip is not in the rig, so it must use the frame path.");
    assertFalse(
        FlixelAnimateSprite.useRigClip(false, false), "No rig means the standard frame path.");
    assertFalse(
        FlixelAnimateSprite.useRigClip(false, true),
        "Without a rig there is nothing to draw the clip from, regardless of the clip flag.");
  }
}
