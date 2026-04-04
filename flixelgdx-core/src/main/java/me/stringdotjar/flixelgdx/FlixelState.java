/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.group.FlixelBasicGroup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core building block for every FlixelGDX game.
 *
 * <p>A state is a collection of {@link FlixelBasic} objects that can be used for any
 * important part of your game. This can be a level, a menu, or anything else.
 *
 * <p>Members are not pooled by the engine: {@link #remove} only unlinks objects. Prefer {@link FlixelBasic#kill()} /
 * {@link FlixelBasic#revive()} or {@link FlixelBasicGroup#recycle()} for reuse. {@link #createMemberForRecycle()} supplies
 * new {@link FlixelSprite} instances when {@link FlixelBasicGroup#recycle()} has no dead member to revive. Override it if
 * your state recycles another {@link FlixelBasic} subtype.
 *
 * <p>A state can open a {@link FlixelSubState} on top of itself.
 * By default, when a substate is active the parent state will continue to be drawn
 * ({@link #persistentDraw} = {@code true}) but will stop updating
 * ({@link #persistentUpdate} = {@code false}).
 *
 * @see FlixelBasic
 * @see FlixelBasicGroup
 */
public abstract class FlixelState extends FlixelBasicGroup<FlixelBasic> implements Screen {

  /** Should {@code this} state update its logic even when a substate is currently opened? */
  public boolean persistentUpdate = false;

  /** Should {@code this} state draw its members even when a substate is currently opened? */
  public boolean persistentDraw = true;

  /**
   * If substates get destroyed when they are closed. Setting this to {@code false} might
   * reduce state creation time, at the cost of greater memory usage.
   */
  public boolean destroySubStates = true;

  /** The currently active substate opened on top of {@code this} state. */
  private FlixelSubState subState;

  /** Creates a new state with no limit on member count. */
  protected FlixelState() {
    super(FlixelBasic[]::new);
  }

  /**
   * Creates a new state with a maximum member count ({@code 0} means unlimited).
   *
   * @param maxSize Maximum members ({@code 0} = unlimited).
   */
  protected FlixelState(int maxSize) {
    super(FlixelBasic[]::new, maxSize);
  }

  @Override
  protected FlixelBasic createMemberForRecycle() {
    return new FlixelSprite();
  }

  @Override
  public FlixelBasic recycle() {
    FlixelBasic member = super.recycle();
    if (member instanceof FlixelSprite sprite) {
      sprite.setAntialiasing(Flixel.globalAntialiasing());
    }
    return member;
  }

  @Override
  public void show() {
    create();
  }

  @Override
  public void render(float delta) {}

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
   * @param elapsed The amount of time that's occurred since the last frame.
   */
  public void update(float elapsed) {
    super.update(elapsed);
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
   * @param toOpen The substate to open.
   */
  public void openSubState(FlixelSubState toOpen) {
    if (toOpen == null) {
      return;
    }
    if (this.subState == toOpen) {
      return;
    }
    if (this.subState != null) {
      closeSubState();
    }

    this.subState = toOpen;
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
   * Adds a new object to {@code this} state.
   *
   * @param basic The object to add to the state.
   */
  @Override
  public void add(@NotNull FlixelBasic basic) {
    super.add(basic);
    if (basic instanceof FlixelSprite sprite) {
      sprite.setAntialiasing(Flixel.globalAntialiasing());
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
    FlixelGame game = Flixel.getGame();
    if (game == null) {
      return Color.BLACK;
    }
    return game.getCamera().bgColor;
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
    FlixelGame game = Flixel.getGame();
    if (game == null) {
      return;
    }
    for (FlixelCamera cam : game.getCameras()) {
      cam.bgColor.set(value);
    }
  }

  @Override
  public String toString() {
    SnapshotArray<?> m = getMembers();
    return "FlixelState(members=" + (m != null ? m.size : 0) + ", subState=" + subState + ")";
  }
}
