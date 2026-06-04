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

import org.flixelgdx.FlixelSprite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the registration and opt-in semantics of the per-animation offset API. Playback that
 * actually applies an offset needs real frames (a GPU texture), so these tests focus on the
 * texture-free bookkeeping: registration, removal, and the rule that the feature never touches a
 * sprite's offset until it is opted into.
 */
class FlixelAnimationOffsetTest {

  @Test
  void offsetsRegisterAndUnregister() {
    FlixelAnimationController anim = new FlixelSprite().ensureAnimation();

    assertFalse(anim.hasOffset("idle"), "No offsets should be registered up front.");

    anim.addOffset("idle", 4f, -8f);
    anim.addOffset("singLEFT", 12f, 0f);
    assertTrue(anim.hasOffset("idle"));
    assertTrue(anim.hasOffset("singLEFT"));

    anim.removeOffset("idle");
    assertFalse(anim.hasOffset("idle"));
    assertTrue(anim.hasOffset("singLEFT"), "Removing one offset must not drop the others.");

    anim.clearOffsets();
    assertFalse(anim.hasOffset("singLEFT"));
  }

  @Test
  void clearAlsoDropsOffsets() {
    FlixelAnimationController anim = new FlixelSprite().ensureAnimation();
    anim.addOffset("idle", 1f, 2f);

    anim.clear();

    assertFalse(anim.hasOffset("idle"), "clear() should reset offsets along with clips.");
  }

  @Test
  void registeringOffsetForAnInactiveClipDoesNotMoveTheSprite() {
    // Until a clip actually plays, registering its offset must leave the sprite's current offset
    // alone. This is what keeps the opt-in feature from disturbing sprites that never use it.
    FlixelSprite sprite = new FlixelSprite();
    sprite.setOffset(3f, 5f);
    FlixelAnimationController anim = sprite.ensureAnimation();

    anim.addOffset("idle", 100f, 100f);

    assertEquals(3f, sprite.getOffsetX(), 0f);
    assertEquals(5f, sprite.getOffsetY(), 0f);
  }
}
