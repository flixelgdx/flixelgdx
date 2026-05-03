package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import me.stringdotjar.flixelgdx.FlixelDestroyable;

/**
 * Powerful and logical class for abstracting away input to be more generalized, regardless of input type.
 *
 * <p>While directly accessing the input system is good for simplicity and prototyping, it can very quickly become a
 * nightmare to maintain and expand upon. By using an action set, you can easily add new input types and actions without
 * having to change your game code.
 *
 * <p>It's as simple as creating a new action, adding it to the action set, and then registering it as an {@link InputProcessor}.
 * Not only does this make your code cleaner and more maintainable, but it also makes it incredibly easier for the
 * player, too.
 *
 * <p>Let's say for example, you are making a game, and you want to make a specific bind changeable by the player. If
 * you tried to directly access the input system, you would have to write a ton of unnecessary boilerplate code just to
 * make a simple bind changeable, which can lead to bugs and become very messy to maintain; however, if you used an
 * action set, all you need to do is simply change a key from an action in an action set, and that's it.
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
 * }</pre>
 *
 * Then, you register the new action so it can actually be used:
 *
 * <pre>{@code
 * // Add the new action set to the processor system on the libGDX layer.
 * // It's recommended to check if the current processor is a multiplexer (usually it is)
 * // because you still might want to keep the other processors you or the framework set.
 * // FlixelGDX and libGDX usually add their own processors, so it's a good practice to not
 * // remove or override them!
 * InputProcessor current = Gdx.input.getInputProcessor();
 * if (current instanceof InputMultiplexer m) {
 *   m.addProcessor(new PlayerControls());
 * }
 * }</pre>
 */
public class FlixelActionSet implements InputProcessor, FlixelDestroyable {

  protected Array<FlixelAction> members;

  public FlixelActionSet() {
    members = new Array<>(FlixelAction[]::new);
  }

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
