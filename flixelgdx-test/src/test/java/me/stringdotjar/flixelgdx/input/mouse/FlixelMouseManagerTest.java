/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.mouse;

import com.badlogic.gdx.Input;
import me.stringdotjar.flixelgdx.GdxHeadlessExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelMouseManagerTest {

  @Test
  void scrollAccumulatesUntilEndFrame() {
    FlixelMouseManager m = new FlixelMouseManager();
    m.getInputProcessor().scrolled(0f, -2f);
    assertEquals(-2f, m.getScrollDeltaY(), 1e-6f);
    m.endFrame();
    assertEquals(0f, m.getScrollDeltaY(), 1e-6f);
  }

  @Test
  void buttonIndicesAreBounded() {
    FlixelMouseManager m = new FlixelMouseManager();
    m.update();
    assertFalse(m.justPressed(-1));
    assertFalse(m.justPressed(Input.Buttons.FORWARD + 5));
  }
}
