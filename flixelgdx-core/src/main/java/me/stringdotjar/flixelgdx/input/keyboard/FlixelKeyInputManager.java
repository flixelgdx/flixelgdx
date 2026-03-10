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
 * <p>Tracks pressed keys via an internal {@link InputProcessor}. If you use a custom
 * input processor, add the one from {@link #getInputProcessor()} first to a multiplexer
 * so key state stays correct.
 */
public class FlixelKeyInputManager {

  /** Whether keyboard input is currently enabled. When false, all key checks return false. */
  public boolean enabled = true;

  /** Keys currently pressed (updated by {@code this} manager's {@link InputProcessor}). */
  private final IntSet currentPressedKeys = new IntSet();

  /** Keys that were pressed last frame, used to compute {@link #justPressed(int)} and {@link #justReleased()}. */
  private final IntSet previousPressedKeys = new IntSet();

  /** Order keys were pressed (chronological), so {@link #firstPressed()} returns the first key held. */
  private final IntArray pressedOrder = new IntArray();

  /** Input processor that tracks key state. */
  private final InputProcessor inputProcessor = new InputProcessor() {
    @Override
    public boolean keyDown(int keycode) {
      currentPressedKeys.add(keycode);
      if (pressedOrder.indexOf(keycode) < 0) {
        pressedOrder.add(keycode);
      }
      return false;
    }

    @Override
    public boolean keyUp(int keycode) {
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
   * Updates internal key state. Must be called once per frame (e.g. from the game loop)
   * so that {@link #justReleased(int)} and {@link #firstJustPressed()} / {@link #firstJustReleased()} work correctly.
   *
   * <p>Syncs {@link #currentPressedKeys} from {@link com.badlogic.gdx.Gdx#input} so that
   * {@link #firstPressed()}, {@link #firstJustPressed()} and {@link #firstJustReleased()} work
   * even when the manager's {@link #getInputProcessor()} is not in the input chain. Call
   * {@link #endFrame()} at the end of the frame so "just pressed/released" detection works next frame.
   */
  public void update() {
    currentPressedKeys.clear();
    for (int i = 0; i <= FlixelKey.MAX_KEYCODE; i++) {
      if (Gdx.input.isKeyPressed(i)) {
        currentPressedKeys.add(i);
      }
    }
    // Keep pressedOrder in sync: remove released keys, add newly pressed (e.g. focus return) in keycode order.
    for (int i = pressedOrder.size - 1; i >= 0; i--) {
      if (!currentPressedKeys.contains(pressedOrder.get(i))) {
        pressedOrder.removeIndex(i);
      }
    }
    for (int i = 0; i <= FlixelKey.MAX_KEYCODE; i++) {
      if (currentPressedKeys.contains(i) && pressedOrder.indexOf(i) < 0) {
        pressedOrder.add(i);
      }
    }
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
    return enabled && (currentPressedKeys.contains(key) || Gdx.input.isKeyPressed(key));
  }

  /**
   * Returns whether the given key was just pressed this frame.
   *
   * @param key key code
   * @return {@code true} if the key was just pressed and input is enabled.
   */
  public boolean justPressed(int key) {
    return enabled && Gdx.input.isKeyJustPressed(key);
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
    return previousPressedKeys.contains(key) && !currentPressedKeys.contains(key);
  }

  /**
   * Returns whether at least one of the given keys is currently pressed.
   *
   * @param keys key codes to check
   * @return {@code true} if any key in the array is pressed and input is enabled.
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
   * @param keys key codes to check
   * @return true if any key in the array was just pressed and input is enabled
   */
  public boolean anyJustPressed(int... keys) {
    if (!enabled || keys == null) {
      return false;
    }
    for (int key : keys) {
      if (Gdx.input.isKeyJustPressed(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether at least one of the given keys was just released this frame.
   *
   * @param keys Keys to check if they were just released.
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
}
