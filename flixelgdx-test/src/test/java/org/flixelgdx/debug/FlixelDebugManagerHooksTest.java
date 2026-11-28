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
import org.flixelgdx.FlixelManagers;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.testing.RecordingBatch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FlixelDebugManagerHooksTest {

  private FlixelDebugManager manager;
  private CountingPanel panel;

  @BeforeEach
  void setUp() {
    manager = new FlixelDebugManager();
    panel = manager.panels.register(new CountingPanel());
    panel.setOpen(true);
  }

  @Test
  void theDebuggerRunsRightAfterInputAndKeepsRunningWhilePaused() {
    assertEquals(FlixelManagers.Manager.ORDER_DEBUG, manager.getOrder());
    assertFalse(manager.isPausable());
  }

  @Test
  void postUpdateSamplesTheFrameInDebugMode() {
    Flixel.runtime = new ModeDevice(FlixelRuntimeMode.DEBUG);
    manager.postUpdate(0.016f);
    assertEquals(1, panel.updates);
  }

  @Test
  void theHooksDoNothingOutsideDebugMode() {
    Flixel.runtime = new ModeDevice(FlixelRuntimeMode.RELEASE);
    manager.create();
    manager.preUpdate(0.016f);
    manager.update(0.016f);
    manager.postUpdate(0.016f);
    manager.postDraw(new RecordingBatch());
    assertEquals(0, panel.updates);
  }

  @Test
  void theInputHooksRunInDebugMode() {
    Flixel.runtime = new ModeDevice(FlixelRuntimeMode.DEBUG);
    assertDoesNotThrow(() -> {
      manager.create();
      manager.preUpdate(0.016f);
      manager.update(0.016f);
      manager.postDraw(new RecordingBatch());
    });
  }

  @Test
  void destroyShutsTheDebuggerDown() {
    manager.destroy();
    assertFalse(panel.isOpen());
  }

  private static final class CountingPanel extends FlixelDebugPanel {

    int updates;

    CountingPanel() {
      super("counting", "Counting");
    }

    @Override
    public void update(float elapsed) {
      super.update(elapsed);
      updates++;
    }
  }

  private static final class ModeDevice implements FlixelRuntimeDevice {

    private final FlixelRuntimeMode mode;

    ModeDevice(FlixelRuntimeMode mode) {
      this.mode = mode;
    }

    @NotNull
    @Override
    public FlixelRuntimeMode getMode() {
      return mode;
    }

    @Override
    public void setMode(@NotNull FlixelRuntimeMode mode) {}
  }
}
