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
package org.flixelgdx.input.keyboard;

import org.flixelgdx.Flixel;
import org.flixelgdx.input.FlixelBaseInputDevice;
import org.flixelgdx.input.FlixelKeyboardListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelKeyInputManagerTest {

  private static final int A = FlixelKey.A;
  private static final int B = FlixelKey.B;
  private static final int C = FlixelKey.C;
  private static final int D = FlixelKey.D;
  private static final int E = FlixelKey.E;
  private static final int F = FlixelKey.F;
  private static final int G = FlixelKey.G;

  private FlixelKeyInputManager keys;

  @BeforeEach
  void setUp() {
    keys = new FlixelKeyInputManager();
  }

  private void tap(int key) {
    keys.keyDown(key);
  }

  @Test
  void anyPressedChecksEveryArgument() {
    tap(G);
    assertFalse(keys.anyPressed(A));
    assertFalse(keys.anyPressed(A, B));
    assertFalse(keys.anyPressed(A, B, C));
    assertFalse(keys.anyPressed(A, B, C, D));
    assertFalse(keys.anyPressed(A, B, C, D, E));
    assertFalse(keys.anyPressed(A, B, C, D, E, F));
    assertFalse(keys.anyPressed(A, B, C, D, E, F, A));
    assertTrue(keys.anyPressed(G));
    assertTrue(keys.anyPressed(A, G));
    assertTrue(keys.anyPressed(A, B, G));
    assertTrue(keys.anyPressed(A, B, C, G));
    assertTrue(keys.anyPressed(A, B, C, D, G));
    assertTrue(keys.anyPressed(A, B, C, D, E, G));
    assertTrue(keys.anyPressed(A, B, C, D, E, F, G));
    assertFalse(keys.anyPressed((int[]) null));
  }

  @Test
  void anyJustPressedChecksEveryArgument() {
    tap(G);
    assertFalse(keys.anyJustPressed(A));
    assertFalse(keys.anyJustPressed(A, B));
    assertFalse(keys.anyJustPressed(A, B, C));
    assertFalse(keys.anyJustPressed(A, B, C, D));
    assertFalse(keys.anyJustPressed(A, B, C, D, E));
    assertFalse(keys.anyJustPressed(A, B, C, D, E, F));
    assertFalse(keys.anyJustPressed(A, B, C, D, E, F, A));
    assertTrue(keys.anyJustPressed(G));
    assertTrue(keys.anyJustPressed(A, G));
    assertTrue(keys.anyJustPressed(A, B, G));
    assertTrue(keys.anyJustPressed(A, B, C, G));
    assertTrue(keys.anyJustPressed(A, B, C, D, G));
    assertTrue(keys.anyJustPressed(A, B, C, D, E, G));
    assertTrue(keys.anyJustPressed(A, B, C, D, E, F, G));
    assertFalse(keys.anyJustPressed((int[]) null));
  }

  @Test
  void anyJustReleasedChecksEveryArgument() {
    tap(G);
    keys.endFrame();
    keys.keyUp(G);
    assertFalse(keys.anyJustReleased(A));
    assertFalse(keys.anyJustReleased(A, B));
    assertFalse(keys.anyJustReleased(A, B, C));
    assertFalse(keys.anyJustReleased(A, B, C, D));
    assertFalse(keys.anyJustReleased(A, B, C, D, E));
    assertFalse(keys.anyJustReleased(A, B, C, D, E, F));
    assertFalse(keys.anyJustReleased(A, B, C, D, E, F, A));
    assertTrue(keys.anyJustReleased(G));
    assertTrue(keys.anyJustReleased(A, G));
    assertTrue(keys.anyJustReleased(A, B, G));
    assertTrue(keys.anyJustReleased(A, B, C, G));
    assertTrue(keys.anyJustReleased(A, B, C, D, G));
    assertTrue(keys.anyJustReleased(A, B, C, D, E, G));
    assertTrue(keys.anyJustReleased(A, B, C, D, E, F, G));
    assertFalse(keys.anyJustReleased((int[]) null));
    assertTrue(keys.justReleased(FlixelKey.ANY));
    assertEquals(G, keys.firstJustReleased());
  }

  @Test
  void aDisabledManagerReportsNothing() {
    tap(A);
    keys.endFrame();
    keys.keyUp(A);
    tap(B);
    keys.enabled = false;
    assertFalse(keys.pressed(B));
    assertFalse(keys.justPressed(B));
    assertFalse(keys.justReleased(A));
    assertFalse(keys.rawPressed(B));
    assertFalse(keys.rawJustPressed(B));
    assertFalse(keys.rawJustReleased(A));
    assertFalse(keys.anyPressed(B));
    assertFalse(keys.anyPressed(A, B));
    assertFalse(keys.anyPressed(A, B, C, D, E, F, B));
    assertFalse(keys.anyJustPressed(B));
    assertFalse(keys.anyJustPressed(A, B, C, D, E, F, B));
    assertFalse(keys.anyJustReleased(A));
    assertFalse(keys.anyJustReleased(A, B, C, D, E, F, A));
    assertEquals(FlixelKey.NONE, keys.firstPressed());
    assertEquals(FlixelKey.NONE, keys.firstJustPressed());
    assertEquals(FlixelKey.NONE, keys.firstJustReleased());
  }

  @Test
  void invalidKeycodesAreIgnored() {
    keys.keyDown(-3);
    keys.keyUp(-3);
    assertFalse(keys.pressed(FlixelKey.MAX_KEYCODE + 5));
    assertFalse(keys.justPressed(FlixelKey.MAX_KEYCODE + 5));
    assertFalse(keys.justReleased(FlixelKey.MAX_KEYCODE + 5));
    assertFalse(keys.isValidKeycode(-1));
    keys.block(-1);
    assertFalse(keys.isBlocked(-1));
    keys.injectKey(-1, true);
    assertFalse(keys.isInjected(-1));
  }

  @Test
  void justPressedAnySeesANewKeyButNotAHeldOne() {
    tap(A);
    keys.endFrame();
    assertFalse(keys.justPressed(FlixelKey.ANY));
    tap(B);
    assertTrue(keys.justPressed(FlixelKey.ANY));
    assertEquals(B, keys.firstJustPressed());
    assertEquals(A, keys.firstPressed(), "the first key pressed comes first");
    keys.endFrame();
    assertFalse(keys.justReleased(FlixelKey.ANY));
    assertEquals(FlixelKey.NONE, keys.firstJustReleased());
    tap(A);
    assertEquals(2, countPressed());
  }

  private int countPressed() {
    int n = 0;
    for (int k : new int[] { A, B, C }) {
      if (keys.pressed(k)) {
        n++;
      }
    }
    return n;
  }

  @Test
  void injectedKeysPassThroughBlocksAndReachTheDevice() {
    Device device = new Device();
    Flixel.input = device;
    keys.block(A);
    keys.injectKey(A, true);
    assertTrue(keys.isInjected(A));
    assertTrue(keys.pressed(A), "an injected key is not blocked");
    assertTrue(keys.pressed(FlixelKey.ANY));
    assertEquals(1, device.downs);

    keys.injectTyped('x');
    assertEquals(1, device.typed);

    keys.releaseInjected();
    assertFalse(keys.isInjected(A));
    assertFalse(keys.rawPressed(A));
    assertEquals(1, device.ups);
  }

  @Test
  void injectingWithoutABaseDeviceStillUpdatesTheManager() {
    keys.injectKey(A, true);
    keys.injectTyped('x');
    assertTrue(keys.pressed(A));
    keys.injectKey(A, false);
    assertFalse(keys.pressed(A));
  }

  @Test
  void unblockAndResetClearState() {
    keys.block(A);
    keys.block(B);
    keys.unblock(A);
    assertFalse(keys.isBlocked(A));
    assertTrue(keys.isBlocked(B));
    keys.unblockAll();
    assertFalse(keys.isBlocked(B));

    tap(A);
    keys.endFrame();
    keys.injectKey(B, true);
    keys.reset();
    assertFalse(keys.pressed(FlixelKey.ANY));
    assertFalse(keys.justReleased(A));
    assertFalse(keys.isInjected(B));
    keys.update();
  }

  @Test
  void blockedKeysAreHiddenFromAnyReleases() {
    tap(A);
    keys.endFrame();
    keys.keyUp(A);
    keys.block(A);
    assertFalse(keys.justReleased(FlixelKey.ANY));
    assertTrue(keys.rawJustReleased(A));
  }

  private static final class Device extends FlixelBaseInputDevice {

    int downs;
    int ups;
    int typed;

    Device() {
      addKeyboardListener(new FlixelKeyboardListener() {
        @Override
        public void keyDown(int keycode) {
          downs++;
        }

        @Override
        public void keyUp(int keycode) {
          ups++;
        }

        @Override
        public void keyTyped(char character) {
          typed++;
        }
      });
    }
  }
}
