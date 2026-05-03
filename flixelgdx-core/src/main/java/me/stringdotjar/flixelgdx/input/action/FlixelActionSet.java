package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.InputProcessor;
import me.stringdotjar.flixelgdx.FlixelDestroyable;

/**
 * Powerful and logical class for abstracting away input to be more generalized, regardless of input type.
 *
 * <p>While directly accessing the input system is good for simplicity and prototyping, it can very quickly become a
 * nightmare to maintain and expand upon. By using an action set, you can easily add new input types and actions without
 * having to change your game code.
 *
 * <p>It's as simple as creating a new action, adding it to the action set, and then checking if the action is triggered
 * in your game code. Not only does this make your code cleaner and more maintainable, but it also makes it incredibly
 * easier for the player, too.
 *
 * <p>Let's say for example, you are making a game, and you want to make a specific bind changeable by the player. If
 * you tried to directly access the input system, you would have to write a ton of unnecessary boilerplate code just to
 * make a simple bind changeable, which can lead to bugs and very messy to maintain; however, if you used an action set,
 * all you need to do is simply change a key from an action from an action set, and that's it.
 *
 * <p>It is recommended to use this system in the following way:
 *
 * <pre>{@code
 * // Define a new action set class. Use this for rebinding controls!
 * public class PlayerControls extends FlixelActionSet {
 *
 *   public FlixelActionDigital jump; // For simple boolean true/false checks.
 *   public FlixelActionAnalog move; // For more a float-ranged value of how hard the keys are pressed.
 *
 *   public PlayerControls() {
 *     jump = new FlixelActionDigital("jump");
 *     move = new FlixelActionAnalog("move");
 *   }
 * }
 * }
 */
public class FlixelActionSet implements InputProcessor, FlixelDestroyable {

  @Override
  public boolean keyDown(int keycode) {
    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    return false;
  }

  @Override
  public boolean keyTyped(char character) {
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  @Override
  public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    return false;
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    return false;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    return false;
  }

  @Override
  public void destroy() {

  }
}
