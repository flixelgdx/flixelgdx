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
package org.flixelgdx;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.SnapshotArray;

import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.group.FlixelBasicGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core building block for every FlixelGDX game.
 *
 * <p>A state is a collection of {@link IFlixelBasic} objects that can be used for any
 * important part of your game. This can be a level, a menu, or anything else.
 *
 * <p>Members are not pooled by the engine: {@link #remove} only unlinks objects. Prefer {@link FlixelBasic#kill()} /
 * {@link FlixelBasic#revive()} or {@link FlixelBasicGroup#recycle()} for reuse. {@link #createMemberForRecycle()} supplies
 * new {@link FlixelSprite} instances when {@link FlixelBasicGroup#recycle()} has no dead member to revive. Override it if
 * your state recycles another {@link IFlixelBasic} implementation.
 *
 * <p>A state can open a {@link FlixelSubState} on top of itself.
 * By default, when a substate is active the parent state will continue to be drawn
 * ({@link #persistentDraw} = {@code true}) but will stop updating
 * ({@link #persistentUpdate} = {@code false}).
 *
 * @see IFlixelBasic
 * @see FlixelBasic
 * @see FlixelBasicGroup
 */
public abstract class FlixelState extends FlixelBasicGroup<IFlixelBasic> implements Screen {

  /** The currently active substate opened on top of {@code this} state. */
  private FlixelSubState subState;

  /** Should {@code this} state update its logic even when a substate is currently opened? */
  public boolean persistentUpdate = false;

  /** Should {@code this} state draw its members even when a substate is currently opened? */
  public boolean persistentDraw = true;

  /**
   * If substates get destroyed when they are closed. Setting this to {@code false} might
   * reduce state creation time, at the cost of greater memory usage.
   */
  public boolean destroySubStates = true;

  /** Creates a new state with no limit on member count. */
  protected FlixelState() {
    super(IFlixelBasic[]::new);
  }

  /**
   * Creates a new state with a maximum member count ({@code 0} means unlimited).
   *
   * @param maxSize Maximum members ({@code 0} = unlimited).
   */
  protected FlixelState(int maxSize) {
    super(IFlixelBasic[]::new, maxSize);
  }

  @Override
  protected IFlixelBasic createMemberForRecycle() {
    return new FlixelSprite();
  }

  @Override
  public IFlixelBasic recycle() {
    IFlixelBasic member = super.recycle();
    if (member instanceof FlixelSprite sprite) {
      sprite.setAntialiasing(Flixel.isAntialiasing());
    }
    return member;
  }

  @Override
  public void show() {
    create();
  }

  @Override
  public void render(float delta) {
    update(delta);
  }

  /**
   * Called when the state is first created. This is where you want to assign your
   * sprites and set up everything your state uses.
   *
   * <p>Make sure to override this, NOT the constructor!
   */
  public void create() {}

  /**
   * Updates the logic of {@code this} state.
   *
   * @param elapsed The amount of time that's occurred since the last frame.
   */
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
  }

  /**
   * Draws {@code this} state's members onto the screen.
   *
   * @param batch The batch that's used to draw {@code this} state's members.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    super.draw(batch);
  }

  /**
   * Opens a {@link FlixelSubState} on top of {@code this} state. If there is already
   * an active substate, it will be closed first.
   *
   * @param toOpen The substate to open.
   */
  public void openSubState(FlixelSubState toOpen) {
    if (toOpen == null) {
      return;
    }
    if (subState == toOpen) {
      return;
    }
    if (subState != null) {
      closeSubState();
    }

    subState = toOpen;
    toOpen.parentState = this;
    toOpen.create();
    toOpen.syncBackgroundToCameras();

    if (toOpen.openCallback != null) {
      toOpen.openCallback.run();
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
   * Called from {@link org.flixelgdx.Flixel#switchState(FlixelState) Flixel.switchState(FlixelState)} before
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

  /**
   * Do not override this method. Override {@link #onFocusLost()} instead.
   */
  @Override
  public final void pause() {
    onFocusLost();
  }

  /**
   * Do not override this method. Override {@link #onFocusGained()} instead.
   */
  @Override
  public final void resume() {
    onFocusGained();
  }

  /**
   * Called when the game window loses focus or the application goes to the background.
   *
   * <p>Override this to react to focus loss in your state. The default implementation
   * walks down to the active {@link FlixelSubState} so it is also notified.
   *
   * @see FlixelGame#onFocusLost()
   */
  public void onFocusLost() {
    FlixelSubState sub = getSubState();
    if (sub != null) {
      sub.onFocusLost();
    }
  }

  /**
   * Called when the game window regains focus or the application returns to the foreground.
   *
   * <p>Override this to react to focus gain in your state. The default implementation
   * walks down to the active {@link FlixelSubState} so it is also notified.
   *
   * @see FlixelGame#onFocusGained()
   */
  public void onFocusGained() {
    FlixelSubState sub = getSubState();
    if (sub != null) {
      sub.onFocusGained();
    }
  }

  /**
   * Called when the desktop window is minimized.
   *
   * <p>This is a desktop-only event; it is never called on mobile or web platforms.
   * The default implementation walks down to the active {@link FlixelSubState} so it is
   * also notified.
   *
   * @see FlixelGame#onMinimized()
   */
  public void onMinimized() {
    FlixelSubState sub = getSubState();
    if (sub != null) {
      sub.onMinimized();
    }
  }

  @Override
  public void hide() {}

  @Override
  public void destroy() {
    hide();
    if (subState != null) {
      closeSubState();
    }
    super.destroy();
  }

  /**
   * Adds a new member to {@code this} state.
   *
   * @param basic The new member to add to the state.
   */
  @Override
  public void add(@NotNull IFlixelBasic basic) {
    super.add(basic);
    if (basic instanceof FlixelAntialiasable b && Flixel.applyAntialiasingOnStateAdd) {
      b.setAntialiasing(Flixel.isAntialiasing());
    }
  }

  @Nullable
  public FlixelSubState getSubState() {
    return subState;
  }

  /**
   * Reads the first camera's {@link FlixelCamera#bgColor}.
   *
   * @return The background color of the first camera.
   */
  public Color getBgColor() {
    if (Flixel.game == null) {
      return Color.BLACK;
    }
    return Flixel.cameras.first().bgColor;
  }

  /**
   * Assigns every listed camera's {@link FlixelCamera#bgColor}.
   *
   * @param value The background color to set.
   */
  public void setBgColor(@Nullable Color value) {
    if (value == null) {
      return;
    }
    if (Flixel.game == null) {
      return;
    }
    for (FlixelCamera cam : Flixel.game.getCameras()) {
      cam.bgColor.set(value);
    }
  }

  @Override
  public String toString() {
    SnapshotArray<?> m = getMembers();
    return "FlixelState(members=" + (m != null ? m.size : 0) + ", subState=" + subState + ")";
  }
}
