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

import org.flixelgdx.collections.FlixelArray;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelMainThreadQueueTest {

  @Test
  void tasksWaitUntilRunAndThenRunInOrder() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    FlixelArray<String> log = new FlixelArray<>();
    queue.add(() -> log.add("a"));
    queue.add(() -> log.add("b"));
    queue.add(() -> log.add("c"));
    assertEquals(0, log.getSize());
    assertEquals(3, queue.getSize());

    queue.run();

    assertEquals(3, log.getSize());
    assertEquals("a", log.get(0));
    assertEquals("b", log.get(1));
    assertEquals("c", log.get(2));
    assertEquals(0, queue.getSize());
  }

  @Test
  void eachTaskRunsOnlyOnce() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    int[] runs = new int[1];
    queue.add(() -> runs[0]++);
    queue.run();
    queue.run();
    assertEquals(1, runs[0]);
  }

  @Test
  void nullTasksAreIgnored() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    queue.add(null);
    assertEquals(0, queue.getSize());
    queue.run();
  }

  @Test
  void aTaskQueuedWhileRunningWaitsForTheNextRun() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    int[] inner = new int[1];
    queue.add(() -> queue.add(() -> inner[0]++));

    queue.run();
    assertEquals(0, inner[0]);
    assertEquals(1, queue.getSize());

    queue.run();
    assertEquals(1, inner[0]);
  }

  @Test
  void runningFromInsideATaskDoesNothing() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    int[] second = new int[1];
    queue.add(() -> {
      queue.add(() -> second[0]++);
      queue.run();
    });

    queue.run();
    assertEquals(0, second[0]);

    queue.run();
    assertEquals(1, second[0]);
  }

  @Test
  void aThrowingTaskIsPassedOnAndItsBatchIsDropped() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    IllegalStateException failure = new IllegalStateException("boom");
    int[] after = new int[1];
    queue.add(() -> {
      throw failure;
    });
    queue.add(() -> after[0]++);

    IllegalStateException thrown = assertThrows(IllegalStateException.class, queue::run);
    assertSame(failure, thrown);

    queue.run();
    assertEquals(0, after[0]);

    queue.add(() -> after[0]++);
    queue.run();
    assertEquals(1, after[0]);
  }

  @Test
  void clearDropsWaitingTasks() {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    int[] runs = new int[1];
    queue.add(() -> runs[0]++);
    queue.clear();
    assertEquals(0, queue.getSize());
    queue.run();
    assertEquals(0, runs[0]);
  }

  @Test
  void tasksAddedFromOtherThreadsRunOnTheThreadThatCallsRun() throws InterruptedException {
    FlixelMainThreadQueue queue = new FlixelMainThreadQueue();
    int threads = 4;
    int perThread = 250;
    CountDownLatch done = new CountDownLatch(threads);
    Thread main = Thread.currentThread();
    int[] runs = new int[1];
    boolean[] wrongThread = new boolean[1];
    for (int t = 0; t < threads; t++) {
      new Thread(() -> {
        for (int i = 0; i < perThread; i++) {
          queue.add(() -> {
            if (Thread.currentThread() != main) {
              wrongThread[0] = true;
            }
            runs[0]++;
          });
        }
        done.countDown();
      }).start();
    }
    while (done.getCount() > 0) {
      queue.run();
    }
    assertTrue(done.await(10, TimeUnit.SECONDS));
    queue.run();

    assertEquals(threads * perThread, runs[0]);
    assertFalse(wrongThread[0]);
  }
}
