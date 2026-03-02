package me.stringdotjar.flixelgdx.display;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.group.FlixelGroup;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for creating a better screen display with more functionality than the default {@link
 * com.badlogic.gdx.Screen} interface.
 *
 * <p>A {@code FlixelState} can open a {@link FlixelSubState} on top of itself.
 * By default, when a substate is active the parent state will continue to be drawn
 * ({@link #persistentDraw} = {@code true}) but will stop updating
 * ({@link #persistentUpdate} = {@code false}).
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxState.html">FlxState (HaxeFlixel)</a>
 */
public abstract class FlixelState extends FlixelGroup<FlixelBasic> implements Screen {

  /** Should {@code this} state update its logic even when a substate is currently opened? */
  public boolean persistentUpdate = false;

  /** Should {@code this} state draw its members even when a substate is currently opened? */
  public boolean persistentDraw = true;

  /**
   * If substates get destroyed when they are closed. Setting this to {@code false} might
   * reduce state creation time, at the cost of greater memory usage.
   */
  public boolean destroySubStates = true;

  /** The background color of {@code this} current state. */
  protected Color bgColor;

  /** The currently active substate opened on top of {@code this} state. */
  private FlixelSubState subState;

  public FlixelState() {
    super(0);
  }

  @Override
  public final void show() {}

  @Override
  public final void render(float delta) {}

  /**
   * Called when the state is first created. This is where you want to assign your
   * sprites and set up everything your state uses!
   *
   * <p>Make sure to override this, NOT the constructor!
   */
  public void create() {}

  /**
   * Updates the logic of {@code this} state.
   *
   * @param delta The amount of time that's occurred since the last frame.
   */
  public void update(float delta) {
    super.update(delta);
  }

  /**
   * Draws {@code this} state's members onto the screen.
   *
   * @param batch The batch that's used to draw {@code this} state's members.
   */
  public void draw(Batch batch) {
    super.draw(batch);
  }

  /**
   * Opens a {@link FlixelSubState} on top of {@code this} state. If there is already
   * an active substate, it will be closed first.
   *
   * @param subState The substate to open.
   */
  public void openSubState(FlixelSubState subState) {
    if (subState == null) {
      return;
    }
    if (this.subState == subState) {
      return;
    }
    if (this.subState != null) {
      closeSubState();
    }

    this.subState = subState;
    subState.parentState = this;
    subState.create();

    if (subState.openCallback != null) {
      subState.openCallback.run();
    }
  }

  /**
   * Closes the currently active substate, if one exists.
   */
  public void closeSubState() {
    if (subState == null) {
      return;
    }
    FlixelSubState closing = subState;
    subState = null;
    closing.parentState = null;

    if (closing.closeCallback != null) {
      closing.closeCallback.run();
    }
    if (destroySubStates) {
      closing.dispose();
    }
  }

  /**
   * Reloads the current substate's parent reference. Called internally after state
   * transitions to ensure the parent link is correct.
   */
  public void resetSubState() {
    if (subState != null) {
      subState.parentState = this;
    }
  }

  /**
   * Called from {@link me.stringdotjar.flixelgdx.Flixel#switchState(FlixelState)} before
   * the actual state switch happens. Override this to play an exit animation or transition,
   * then call {@code onOutroComplete} when finished.
   *
   * <p>The default implementation calls {@code onOutroComplete} immediately.
   *
   * @param onOutroComplete Callback to invoke when the outro is complete.
   */
  public void startOutro(Runnable onOutroComplete) {
    if (onOutroComplete != null) {
      onOutroComplete.run();
    }
  }

  @Override
  public void resize(int width, int height) {}

  @Override
  public void pause() {}

  @Override
  public void resume() {}

  @Override
  public final void hide() {}

  /**
   * Disposes {@code this} state, any active substate, and all members. Called automatically
   * when {@link me.stringdotjar.flixelgdx.Flixel#switchState(FlixelState)} is used, so that
   * sprites and other objects release their resources.
   */
  @Override
  public void dispose() {
    if (subState != null) {
      closeSubState();
    }
    if (members == null) {
      return;
    }
    Object[] items = members.begin();
    for (int i = 0, n = members.size; i < n; i++) {
      FlixelBasic obj = (FlixelBasic) items[i];
      if (obj != null) {
        obj.destroy();
      }
    }
    members.end();
    members.clear();
  }

  /**
   * Adds a new object to {@code this} state.
   *
   * @param basic The object to add to the state.
   */
  public void add(@NotNull FlixelBasic basic) {
    members.add(basic);

    if (basic instanceof FlixelSprite sprite) {
      sprite.setAntialiasing(Flixel.globalAntialiasing());
    }
  }

  /** Returns the currently active substate, or {@code null} if none is open. */
  public FlixelSubState getSubState() {
    return subState;
  }

  public Color getBgColor() {
    return (bgColor != null) ? bgColor : Color.BLACK;
  }
}
