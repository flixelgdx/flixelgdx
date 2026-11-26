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
package org.flixelgdx.backend;

import org.flixelgdx.Flixel;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFilePickerHelpers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelRuntimeDeviceMainThreadTest {

  @Test
  void theDefaultDeviceRunsTasksRightAway() {
    int[] runs = new int[1];
    FlixelNoopRuntimeDevice.INSTANCE.queueMainThread(() -> runs[0]++);
    assertEquals(1, runs[0]);
    FlixelNoopRuntimeDevice.INSTANCE.runMainThreadTasks();
    assertEquals(1, runs[0]);
  }

  @Test
  void theDefaultDeviceIgnoresNull() {
    FlixelNoopRuntimeDevice.INSTANCE.queueMainThread(null);
  }

  @Test
  void aQueueingDeviceRunsTasksOnlyWhenTheLoopAsks() {
    QueueingDevice device = new QueueingDevice();
    int[] runs = new int[1];
    device.queueMainThread(() -> runs[0]++);
    assertEquals(0, runs[0]);
    device.runMainThreadTasks();
    assertEquals(1, runs[0]);
  }

  @Test
  void filePickerResultsAreDeliveredThroughTheRuntime() {
    QueueingDevice device = new QueueingDevice();
    Flixel.runtime = device;
    FlixelFile[][] picked = new FlixelFile[1][];
    boolean[] saved = new boolean[1];

    FlixelFilePickerHelpers.deliver(files -> picked[0] = files, (FlixelFile[]) null);
    FlixelFilePickerHelpers.deliver(result -> saved[0] = result, true);
    assertNull(picked[0]);
    assertFalse(saved[0]);

    device.runMainThreadTasks();
    assertSame(FlixelFilePickerHelpers.NO_FILES, picked[0]);
    assertTrue(saved[0]);
  }

  private static final class QueueingDevice implements FlixelRuntimeDevice {

    private final FlixelMainThreadQueue queue = new FlixelMainThreadQueue();

    @Override
    public void queueMainThread(@NotNull Runnable action) {
      queue.add(action);
    }

    @Override
    public void runMainThreadTasks() {
      queue.run();
    }
  }
}
