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
import org.flixelgdx.testing.RecordingBatch;
import org.flixelgdx.testing.RecordingGraphics;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelRenderManagerTest {

  private RecordingGraphics graphics;
  private FlixelRenderManager render;
  private StringBuilder log;
  private FlixelManagersTest.Recorder recorder;

  @BeforeEach
  void setUp() {
    graphics = new RecordingGraphics();
    Flixel.graphics = graphics;
    render = new FlixelRenderManager();
    log = new StringBuilder();
    Flixel.cameras.clear();
    Flixel.cameraManager = new FlixelCameraManager();
    recorder = new FlixelManagersTest.Recorder("m", 0, log);
    Flixel.managers.add(recorder);
  }

  @AfterEach
  void tearDown() {
    Flixel.managers.remove(recorder);
    Flixel.cameras.clear();
  }

  @Test
  void theRendererDrawsFirstAndKeepsItsName() {
    assertEquals(FlixelManagers.Manager.ORDER_RENDER, render.getOrder());
    assertEquals("flixel.render", render.getName());
    assertTrue(FlixelManagers.Manager.ORDER_RENDER < FlixelManagers.Manager.ORDER_CAMERAS);
  }

  @Test
  void aFrameWithoutCamerasStillClearsAndStretchesTheScene() {
    render.draw(new RecordingBatch());
    assertEquals("clear beginScene endScene ", graphics.log.toString());
  }

  @Test
  void theGlobalShadersWrapEveryCameraPass() {
    graphics.globalShaders = true;
    render.draw(new RecordingBatch());
    assertEquals("clear beginScene beginShaders endShaders endScene ", graphics.log.toString());
  }

  @Test
  void eachCameraPassDrawsTheStateThenTheManagers() {
    FlixelCamera camera = new FlixelCamera(100, 100);
    Flixel.cameras.add(camera);
    Flixel.game = new FlixelGame("Render", DrawingState::new) {
    };
    DrawingState state = new DrawingState(log);
    Flixel.state = state;
    RecordingBatch batch = new RecordingBatch();
    render.draw(batch);
    assertEquals("state.draw m.drawCamera ", log.toString());
    assertSame(camera, recorder.lastCamera);
    assertEquals(1, batch.begins);
    assertEquals(1, batch.ends);
    assertNull(Flixel.getDrawCamera());
  }

  @Test
  void theOverlayPassRunsWithTheOverlayCamera() {
    Flixel.config = new FlixelConfig.Builder("Render").size(100, 100).build();
    Flixel.cameraManager.create();
    Flixel.cameras.clear();
    render.draw(new RecordingBatch());
    assertEquals("m.drawOverlay ", log.toString());
  }

  @Test
  void theWindowIsClearedToTheBackgroundColor() {
    render.bgColor.set(FlixelColor.BLUE);
    assertSame(render.bgColor, render.getBgColor());
    render.draw(new RecordingBatch());
    assertTrue(graphics.log.toString().startsWith("clear "));
  }

  @Test
  void losingFocusLowersTheFrameRateOnlyWithAutoPause() {
    Flixel.autoPause = false;
    render.onFocusLost();
    assertEquals(60, graphics.targetFps);
    assertFalse(render.isThrottled());

    Flixel.autoPause = true;
    render.onFocusLost();
    assertEquals(FlixelRenderManager.BACKGROUND_FPS, graphics.targetFps);
    assertTrue(render.isThrottled());
    render.onFocusLost();
    render.onFocusGained();
    assertEquals(60, graphics.targetFps, "the original frame rate comes back, not the lowered one");
    assertFalse(render.isThrottled());
  }

  @Test
  void regainingFocusWithoutThrottlingLeavesTheFrameRateAlone() {
    graphics.targetFps = 144;
    render.onFocusGained();
    assertEquals(144, graphics.targetFps);
  }

  @Test
  void resizingKeepsTheGlobalShadersSized() {
    render.resize(10, 10);
    assertEquals("resizeShaders ", graphics.log.toString());
  }

  @Test
  void theFrameEndsOpaqueWhenTheFramebufferIsTransparentButTheWindowIsNot() {
    Flixel.config = new FlixelConfig.Builder("Render").build();
    render.endFrame();
    assertEquals("", graphics.log.toString());
    Flixel.config = new FlixelConfig.Builder("Render").transparentFramebuffer(true).build();
    render.endFrame();
    assertEquals("opaque ", graphics.log.toString());
  }

  @Test
  void destroyReleasesTheGlobalShaders() {
    Flixel.autoPause = true;
    render.onFocusLost();
    render.destroy();
    assertEquals("disposeShaders ", graphics.log.toString());
    assertFalse(render.isThrottled());
    assertNull(render.getBgPixel());
  }

  private static final class DrawingState extends FlixelState {

    private final StringBuilder log;

    DrawingState() {
      this(new StringBuilder());
    }

    DrawingState(StringBuilder log) {
      this.log = log;
    }

    @Override
    public void draw(FlixelBatch batch) {
      log.append("state.draw ");
    }
  }
}
