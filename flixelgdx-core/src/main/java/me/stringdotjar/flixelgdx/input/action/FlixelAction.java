package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.utils.IntArray;
import org.jetbrains.annotations.Nullable;

/**
 * Generic base class for new action types to be made.
 */
public abstract class FlixelAction {

  /** The ID used to identify {@code this} action. */
  private String name;

  /** The numerical bindings that trigger {@code this} action. */
  private IntArray binds;

  /** Code that gets executed when {@code this} action is triggered. */
  @Nullable
  public Runnable callback;

  /** Whether {@code this} action should receive updates in the {@link FlixelActionSet} it's in. */
  public boolean active = true;

  /**
   * Adds a new key code that can trigger {@code this} action.
   *
   * @param key The key to add.
   */
  public void addKey(int key) {
    binds.add(key);
  }

  /**
   * Adds a new gamepad button code that can trigger {@code this} action.
   *
   * @param button The gamepad button to add.
   */
  public void addGamepad(int button) {
    binds.add(button);
  }
}
