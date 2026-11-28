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

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelOverlayManager;
import org.flixelgdx.testing.RecordingBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelOverlayManagerTest {

  private FlixelOverlayManager overlay;
  private CountingBasic member;

  @BeforeEach
  void setUp() {
    overlay = new FlixelOverlayManager();
    member = new CountingBasic();
  }

  @Test
  void membersCanBeAddedBeforeTheGameStarts() {
    overlay.add(member);
    assertEquals(1, overlay.getGroup().getMembers().getSize());
    overlay.remove(member);
    assertEquals(0, overlay.getGroup().getMembers().getSize());
  }

  @Test
  void theOverlayStaysIdleWhileItIsOff() {
    overlay.add(member);
    overlay.postUpdate(0.016f);
    overlay.drawOverlay(new RecordingBatch());
    assertFalse(overlay.isEnabled());
    assertEquals(0, member.updates);
    assertEquals(0, member.draws);
  }

  @Test
  void theOverlayUpdatesAndDrawsWhileItIsOn() {
    overlay.add(member);
    overlay.setEnabled(true);
    overlay.postUpdate(0.016f);
    overlay.drawOverlay(new RecordingBatch());
    assertEquals(1, member.updates);
    assertEquals(1, member.draws);
  }

  @Test
  void toggleFlipsTheOverlay() {
    assertTrue(overlay.toggle());
    assertFalse(overlay.toggle());
  }

  @Test
  void destroyEmptiesTheOverlayAndTurnsItOff() {
    overlay.add(member);
    overlay.setEnabled(true);
    overlay.destroy();
    assertFalse(overlay.isEnabled());
    assertEquals(0, overlay.getGroup().getMembers().getSize());
    assertFalse(member.isExists());
  }

  @Test
  void theOverlayUpdatesAfterTheCameras() {
    assertEquals(FlixelManagers.Manager.ORDER_OVERLAY, overlay.getOrder());
    assertEquals("flixel.overlay", overlay.getName());
    assertTrue(FlixelManagers.Manager.ORDER_CAMERAS < FlixelManagers.Manager.ORDER_OVERLAY);
  }

  private static final class CountingBasic extends FlixelBasic {

    int updates;
    int draws;

    @Override
    public void update(float elapsed) {
      updates++;
    }

    @Override
    public void draw(FlixelBatch batch) {
      draws++;
    }
  }
}
