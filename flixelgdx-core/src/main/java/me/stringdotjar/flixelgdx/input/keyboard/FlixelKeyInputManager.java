/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.keyboard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;

/**
 * Keyboard input manager backed by {@link com.badlogic.gdx.Gdx#input}.
 *
 * <p>Access via {@code Flixel.keys} after the framework is initialized.
 *
 * <p>Tracks pressed keys via an internal libGDX {@link InputProcessor}. The processor is the
 * authoritative source of "is key X currently pressed", which keeps state correct across every
 * libGDX backend (LWJGL3, Android, iOS, TeaVM). The TeaVM/web backend in particular does not
 * always update {@link com.badlogic.gdx.Input#isKeyPressed(int) Gdx.input.isKeyPressed} for every
 * keycode, so the framework cannot rebuild its set from polling each frame; doing so would erase
 * any state the {@link InputProcessor} just wrote and break {@link #justPressed(int)} /
 * {@link #justReleased(int)} on web. Instead, {@link #update()} simply records this frame's
 * snapshot for "just" detection without ever touching {@link #currentPressedKeys}.
 *
 * <p>Make sure that the processor returned by {@link #getInputProcessor()} is added to the libGDX
 * input chain (this is wired automatically in {@code FlixelGame.create()} via an
 * {@link InputMultiplexer}). If you replace the input processor with your own, add this one first
 * so key state still updates.
 */
public class FlixelKeyInputManager {

  /** Whether keyboard input is currently enabled. When false, all key checks return false. */
  public boolean enabled = true;

  /** Keys currently pressed (updated only by {@code this} manager's {@link InputProcessor}). */
  private final IntSet currentPressedKeys = new IntSet();

  /** Keys that were pressed last frame, used to compute {@link #justPressed(int)} and {@link #justReleased(int)}. */
  private final IntSet previousPressedKeys = new IntSet();

  /** Order keys were pressed (chronological), so {@link #firstPressed()} returns the first key held. */
  private final IntArray pressedOrder = new IntArray();

  /** Input processor that tracks key state. */
  private final InputProcessor inputProcessor = new InputProcessor() {
    @Override
    public boolean keyDown(int keycode) {
      if (keycode < 0) {
        return false;
      }
      if (currentPressedKeys.add(keycode) && pressedOrder.indexOf(keycode) < 0) {
        pressedOrder.add(keycode);
      }
      return false;
    }

    @Override
    public boolean keyUp(int keycode) {
      if (keycode < 0) {
        return false;
      }
      currentPressedKeys.remove(keycode);
      pressedOrder.removeValue(keycode);
      return false;
    }

    @Override
    public boolean keyTyped(char character) { return false; }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }

    @Override
    public boolean scrolled(float amountX, float amountY) { return false; }
  };

  public FlixelKeyInputManager() {}

  /**
   * Returns the input processor that tracks key state. Add this first to an
   * {@link InputMultiplexer} (before other processors) so key state is correct.
   */
  public InputProcessor getInputProcessor() {
    return inputProcessor;
  }

  /**
   * Called once per frame from the game loop. Reserved for future polling-based fallbacks; today
   * the {@link InputProcessor} keeps {@link #currentPressedKeys} up to date in real time, so this
   * method intentionally does nothing.
   *
   * <p>Earlier versions rebuilt {@link #currentPressedKeys} from
   * {@link com.badlogic.gdx.Gdx#input Gdx.input.isKeyPressed} every frame, which clobbered any
   * state the {@link InputProcessor} had just written and silently broke {@link #justPressed(int)},
   * {@link #justReleased(int)}, and the "first" helpers on the TeaVM/web backend (where libGDX
   * does not expose every key through {@code isKeyPressed}). The processor is now the only writer.
   *
   * <p>Call {@link #endFrame()} at the end of the frame so "just pressed/released" detection works
   * next frame.
   */
  public void update() {
    // Intentionally empty: state is maintained by the InputProcessor.
  }

  /**
   * Captures current key state as "previous" for the next frame. Must be called once per frame
   * at the <i>end</i> of the update cycle (after all state updates) so that
   * {@link #firstJustPressed()} and {@link #firstJustReleased()} work correctly next frame.
   */
  public void endFrame() {
    previousPressedKeys.clear();
    previousPressedKeys.addAll(currentPressedKeys);
  }

  /**
   * Returns whether the given key is currently held down.
   *
   * @param key The key to check if it is pressed. (e.g. {@link FlixelKey#A}, {@link Input.Keys})
   * @return {@code true} if the key is pressed and input is enabled.
   */
  public boolean pressed(int key) {
    if (!enabled) {
      return false;
    }
    if (key == FlixelKey.ANY) {
      return currentPressedKeys.size > 0;
    }
    if (!isValidKeycode(key)) {
      return false;
    }
    return currentPressedKeys.contains(key) || Gdx.input.isKeyPressed(key);
  }

  /**
   * Returns whether the given key was just pressed this frame. Uses the same
   * {@code current} vs {@code previous} key sets as {@link #justReleased(int)} and
   * {@link #firstJustPressed()} so "just" transitions stay reliable on all backends
   * (for example, WebGL where {@code Gdx.input.isKeyJustPressed} is not dependable).
   *
   * @param key key code
   * @return {@code true} if the key was just pressed and input is enabled.
   */
  public boolean justPressed(int key) {
    if (!enabled) {
      return false;
    }
    if (key == FlixelKey.ANY) {
      for (IntSet.IntSetIterator it = currentPressedKeys.iterator(); it.hasNext; ) {
        int pressedKey = it.next();
        if (!previousPressedKeys.contains(pressedKey)) {
          return true;
        }
      }
      return false;
    }
    if (!isValidKeycode(key)) {
      return false;
    }
    return currentPressedKeys.contains(key) && !previousPressedKeys.contains(key);
  }

  /**
   * Returns whether the given key was just released this frame.
   *
   * @param key key code
   * @return {@code true} if the key was pressed last frame and is not pressed now, and input is enabled.
   */
  public boolean justReleased(int key) {
    if (!enabled) {
      return false;
    }
    if (key == FlixelKey.ANY) {
      for (IntSet.IntSetIterator it = previousPressedKeys.iterator(); it.hasNext; ) {
        int pressedKey = it.next();
        if (!currentPressedKeys.contains(pressedKey)) {
          return true;
        }
      }
      return false;
    }
    if (!isValidKeycode(key)) {
      return false;
    }
    return previousPressedKeys.contains(key) && !currentPressedKeys.contains(key);
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param k1 The first key code to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int k1) {
    return enabled && pressed(k1);
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int k1, int k2) {
    return enabled && (pressed(k1) || pressed(k2));
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int k1, int k2, int k3) {
    return enabled && (pressed(k1) || pressed(k2) || pressed(k3));
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int k1, int k2, int k3, int k4) {
    return enabled && (pressed(k1) || pressed(k2) || pressed(k3) || pressed(k4));
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @param k5 The fifth key code to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int k1, int k2, int k3, int k4, int k5) {
    return enabled && (pressed(k1) || pressed(k2) || pressed(k3) || pressed(k4) || pressed(k5));
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @param k5 The fifth key code to check.
   * @param k6 The sixth key code to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int k1, int k2, int k3, int k4, int k5, int k6) {
    return enabled && (pressed(k1) || pressed(k2) || pressed(k3) || pressed(k4) || pressed(k5) || pressed(k6));
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param keys The keys to check.
   * @return {@code true} if any key in the given list is pressed and input is enabled.
   */
  public boolean anyPressed(int... keys) {
    if (!enabled || keys == null) {
      return false;
    }
    for (int key : keys) {
      if (pressed(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param k1 The first key code to check.
   * @return true if any key in the array was just pressed and input is enabled
   */
  public boolean anyJustPressed(int k1) {
    return enabled && justPressed(k1);
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @return {@code true} if any key in the given list was just pressed and input is enabled.
   */
  public boolean anyJustPressed(int k1, int k2) {
    return enabled && (justPressed(k1) || justPressed(k2));
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @return {@code true} if any key in the given list was just pressed and input is enabled.
   */
  public boolean anyJustPressed(int k1, int k2, int k3) {
    return enabled && (justPressed(k1) || justPressed(k2) || justPressed(k3));
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @return {@code true} if any key in the given list was just pressed and input is enabled.
   */
  public boolean anyJustPressed(int k1, int k2, int k3, int k4) {
    return enabled
      && (justPressed(k1) || justPressed(k2) || justPressed(k3) || justPressed(k4));
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @param k5 The fifth key code to check.
   * @return {@code true} if any key in the given list was just pressed and input is enabled.
   */
  public boolean anyJustPressed(int k1, int k2, int k3, int k4, int k5) {
    return enabled
      && (justPressed(k1)
        || justPressed(k2)
        || justPressed(k3)
        || justPressed(k4)
        || justPressed(k5));
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @param k5 The fifth key code to check.
   * @param k6 The sixth key code to check.
   * @return {@code true} if any key in the given list was just pressed and input is enabled.
   */
  public boolean anyJustPressed(int k1, int k2, int k3, int k4, int k5, int k6) {
    return enabled
      && (justPressed(k1)
        || justPressed(k2)
        || justPressed(k3)
        || justPressed(k4)
        || justPressed(k5)
        || justPressed(k6));
  }

  /**
   * Returns whether at least one of the given keys was just pressed this frame.
   *
   * @param keys The keys to check.
   * @return {@code true} if any key in the given list was just pressed and input is enabled.
   */
  public boolean anyJustPressed(int... keys) {
    if (!enabled || keys == null) {
      return false;
    }
    for (int key : keys) {
      if (justPressed(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param k1 The first key code to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int k1) {
    return enabled && justReleased(k1);
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int k1, int k2) {
    return enabled && (justReleased(k1) || justReleased(k2));
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int k1, int k2, int k3) {
    return enabled && (justReleased(k1) || justReleased(k2) || justReleased(k3));
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int k1, int k2, int k3, int k4) {
    return enabled && (justReleased(k1) || justReleased(k2) || justReleased(k3) || justReleased(k4));
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @param k5 The fifth key code to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int k1, int k2, int k3, int k4, int k5) {
    return enabled && (justReleased(k1) || justReleased(k2) || justReleased(k3) || justReleased(k4) || justReleased(k5));
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param k1 The first key code to check.
   * @param k2 The second key code to check.
   * @param k3 The third key code to check.
   * @param k4 The fourth key code to check.
   * @param k5 The fifth key code to check.
   * @param k6 The sixth key code to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int k1, int k2, int k3, int k4, int k5, int k6) {
    return enabled
      && (justReleased(k1) || justReleased(k2) || justReleased(k3) || justReleased(k4) || justReleased(k5) || justReleased(k6));
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param keys The keys to check.
   * @return {@code true} if any key in the given list was just released and input is enabled.
   */
  public boolean anyJustReleased(int... keys) {
    if (!enabled || keys == null) {
      return false;
    }
    for (int key : keys) {
      if (justReleased(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the key code that was pressed first (chronologically) among those currently held,
   * or {@link FlixelKey#NONE} if none.
   *
   * @return First pressed key code, or {@link FlixelKey#NONE} if none.
   */
  public int firstPressed() {
    if (!enabled || pressedOrder.size == 0) {
      return FlixelKey.NONE;
    }
    return pressedOrder.first();
  }

  /**
   * Returns the first key code that was just pressed this frame, or {@link FlixelKey#NONE} if none.
   *
   * @return First just-pressed key code, or {@link FlixelKey#NONE} if none.
   */
  public int firstJustPressed() {
    if (!enabled) {
      return FlixelKey.NONE;
    }
    for (IntSet.IntSetIterator it = currentPressedKeys.iterator(); it.hasNext; ) {
      int key = it.next();
      if (!previousPressedKeys.contains(key)) {
        return key;
      }
    }
    return FlixelKey.NONE;
  }

  /**
   * Returns the first key code that was just released this frame, or {@link FlixelKey#NONE} if none.
   *
   * @return First just-released key code, or {@link FlixelKey#NONE} if none.
   */
  public int firstJustReleased() {
    if (!enabled) {
      return FlixelKey.NONE;
    }
    for (IntSet.IntSetIterator it = previousPressedKeys.iterator(); it.hasNext; ) {
      int key = it.next();
      if (!currentPressedKeys.contains(key)) {
        return key;
      }
    }
    return FlixelKey.NONE;
  }

  /**
   * Resets internal state (e.g. clears pressed key tracking).
   * Does not change {@link #enabled}.
   */
  public void reset() {
    currentPressedKeys.clear();
    previousPressedKeys.clear();
    pressedOrder.clear();
  }

  private static boolean isValidKeycode(int key) {
    return key >= 0 && key <= FlixelKey.MAX_KEYCODE;
  }
}
