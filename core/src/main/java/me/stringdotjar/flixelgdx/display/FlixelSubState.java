package me.stringdotjar.flixelgdx.display;

import com.badlogic.gdx.graphics.Color;

/**
 * A {@code FlixelSubState} can be opened inside a {@link FlixelState}. By default, it
 * stops the parent state from updating, making it convenient for pause screens or menus.
 *
 * <p>The parent state's {@link FlixelState#persistentUpdate} and
 * {@link FlixelState#persistentDraw} flags control whether it continues to update and
 * draw while this substate is active.
 *
 * <p>Substates can be nested: a substate can open another substate on top of itself.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxSubState.html">FlxSubState (HaxeFlixel)</a>
 */
public abstract class FlixelSubState extends FlixelState {

  /** Called when this substate is opened or resumed. */
  public Runnable openCallback;

  /** Called when this substate is closed. */
  public Runnable closeCallback;

  /** The parent state that opened this substate. Set internally by {@link FlixelState#openSubState}. */
  FlixelState parentState;

  /**
   * Creates a new substate with a clear background.
   */
  public FlixelSubState() {
    this(Color.CLEAR);
  }

  /**
   * Creates a new substate with the given background color.
   *
   * @param bgColor The background color for this substate.
   */
  public FlixelSubState(Color bgColor) {
    super();
    this.bgColor = bgColor;
  }

  /** Closes this substate by telling the parent state to remove it. */
  public void close() {
    if (parentState != null) {
      parentState.closeSubState();
    }
  }
}
