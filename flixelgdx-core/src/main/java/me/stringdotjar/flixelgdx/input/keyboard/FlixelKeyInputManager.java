package me.stringdotjar.flixelgdx.input.keyboard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
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

  /** Input processor that tracks key state. */
  private final InputProcessor inputProcessor = new InputProcessor() {
    @Override
    public boolean keyDown(int keycode) {
      currentPressedKeys.add(keycode);
      return false;
    }

    @Override
    public boolean keyUp(int keycode) {
      currentPressedKeys.remove(keycode);
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
   */
  public void update() {
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
   * Returns the first key code that is currently pressed, or {@link FlixelKey#NONE} if none.
   *
   * @return First pressed key code, or {@link FlixelKey#NONE} if none.
   */
  public int firstPressed() {
    if (!enabled || currentPressedKeys.size == 0) {
      return FlixelKey.NONE;
    }
    return currentPressedKeys.first();
  }

  /**
   * Returns the first key code that was just pressed this frame, or -1 if none.
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
  }
}
