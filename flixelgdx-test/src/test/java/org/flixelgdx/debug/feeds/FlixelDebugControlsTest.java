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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.feeds.FlixelDebugControls;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugControlsTest {

  private FlixelLogger originalLog;
  private FlixelDebugControls controls;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    controls = new FlixelDebugManager().controls;
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
  }

  @Test
  void defaultStateEncodesPausedHitboxesFreeCamRateLogLevelAndKeybinds() {
    FlixelDebugMessageReader r = DebugTestSupport.payload(controls);
    assertFalse(r.readBool(), "paused");
    assertFalse(r.readBool(), "hitboxes");
    assertFalse(r.readBool(), "freeCam");
    assertEquals(20, r.readU8(), "rate");
    assertEquals(FlixelDebugProtocol.LEVEL_DEFAULT, r.readU8(), "logLevel");
    assertEquals(8, r.readCount());
    assertEquals("F3", DebugTestSupport.str(r));
    assertEquals("Pause or resume the game", DebugTestSupport.str(r));
    assertEquals("F2", DebugTestSupport.str(r));
    assertEquals("Show or hide hitboxes", DebugTestSupport.str(r));
    assertEquals("F4", DebugTestSupport.str(r));
    assertEquals("Turn the free camera on or off", DebugTestSupport.str(r));
    assertEquals("Alt + Click", DebugTestSupport.str(r));
    assertEquals("Select the object under the mouse", DebugTestSupport.str(r));
    assertEquals("W A S D / Arrows", DebugTestSupport.str(r));
    assertEquals("Move the free camera", DebugTestSupport.str(r));
    assertEquals("Wheel / Q / E", DebugTestSupport.str(r));
    assertEquals("Zoom the free camera", DebugTestSupport.str(r));
    assertEquals("Shift", DebugTestSupport.str(r));
    assertEquals("Move the free camera 4 times faster", DebugTestSupport.str(r));
    assertEquals("Right Drag", DebugTestSupport.str(r));
    assertEquals("Pan the free camera", DebugTestSupport.str(r));
    assertEquals(0, r.getRemaining());
  }

  @Test
  void everyActionChangesTheMatchingState() {
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_HITBOXES, w -> w.writeBool(true));
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_FREECAM, w -> w.writeBool(true));
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_RATE, w -> w.writeU8(8));
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_LOG_LEVEL,
        w -> w.writeU8(FlixelDebugProtocol.LEVEL_ERROR));
    assertTrue(controls.isHitboxes());
    assertTrue(controls.isFreeCam());
    assertEquals(8, controls.getRate());
    assertEquals(FlixelLogLevel.ERROR, Flixel.log.getLevel());

    FlixelDebugMessageReader r = DebugTestSupport.payload(controls);
    assertFalse(r.readBool());
    assertTrue(r.readBool());
    assertTrue(r.readBool());
    assertEquals(8, r.readU8());
    assertEquals(FlixelDebugProtocol.LEVEL_ERROR, r.readU8());

    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_HITBOXES, w -> w.writeBool(false));
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_LOG_LEVEL,
        w -> w.writeU8(FlixelDebugProtocol.LEVEL_DEFAULT));
    assertFalse(controls.isHitboxes());
    assertEquals(FlixelLogLevel.DEBUG, Flixel.log.getLevel(), "the logger's own level is restored");
  }

  @Test
  void theRateIsClampedToOneThroughThirty() {
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_RATE, w -> w.writeU8(200));
    assertEquals(30, controls.getRate());
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_RATE, w -> w.writeU8(0));
    assertEquals(1, controls.getRate());
  }

  @Test
  void pauseAndStepDoNothingWithoutAGame() {
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_SET_PAUSED, w -> w.writeBool(true));
    DebugTestSupport.send(controls, FlixelDebugProtocol.CONTROLS_STEP, w -> {
    });
    assertFalse(controls.isPaused());
    assertFalse(controls.stepFrame());
  }

  @Test
  void unknownActionCodesAreIgnored() {
    DebugTestSupport.send(controls, 99, w -> w.writeBool(true));
    assertFalse(controls.isHitboxes());
  }

  @Test
  void hotkeysAreConfigurableAndShownAsKeybindRows() {
    assertEquals(FlixelKey.F3, controls.getPauseKey());
    assertEquals(FlixelKey.F2, controls.getHitboxesKey());
    assertEquals(FlixelKey.F4, controls.getFreeCamKey());
    controls.setPauseKey(FlixelKey.P);
    controls.setHitboxesKey(FlixelKey.NONE);
    controls.setFreeCamKey(FlixelKey.NONE);

    FlixelDebugMessageReader r = DebugTestSupport.payload(controls);
    r.readBool();
    r.readBool();
    r.readBool();
    r.readU8();
    r.readU8();
    assertEquals(6, r.readCount());
    assertEquals("P", DebugTestSupport.str(r));
  }

  @Test
  void updateNoticesStateChangesAndBumpsTheRevision() {
    controls.update(0.016f);
    long steady = controls.getRevision();
    controls.update(0.016f);
    assertEquals(steady, controls.getRevision());

    controls.setHitboxes(true);
    controls.update(0.016f);
    assertTrue(controls.getRevision() > steady);
  }

  @Test
  void resetTurnsEverythingBackOff() {
    controls.setHitboxes(true);
    controls.setFreeCam(true);
    controls.setRate(5);
    controls.setLogLevel(FlixelDebugProtocol.LEVEL_WARN);
    controls.reset();
    assertFalse(controls.isHitboxes());
    assertFalse(controls.isFreeCam());
    assertEquals(FlixelDebugControls.DEFAULT_RATE, controls.getRate());
    assertEquals(FlixelDebugProtocol.LEVEL_DEFAULT, controls.getLogLevel());
    assertEquals(FlixelLogLevel.DEBUG, Flixel.log.getLevel());
  }
}
