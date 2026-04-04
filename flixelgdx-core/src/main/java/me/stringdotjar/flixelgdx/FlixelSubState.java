/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.SnapshotArray;

/**
 * A {@code FlixelSubState} can be opened inside a {@link FlixelState}. By default, it
 * stops the parent state from updating, making it convenient for pause screens or menus.
 *
 * <p>The parent state's {@link FlixelState#persistentUpdate} and
 * {@link FlixelState#persistentDraw} flags control whether it continues to update and
 * draw while this substate is active.
 *
 * <p>Substates can be nested: a substate can open another substate on top of itself.
 */
public abstract class FlixelSubState extends FlixelState {

  /** Called when this substate is opened or resumed. */
  public Runnable openCallback;

  /** Called when this substate is closed. */
  public Runnable closeCallback;

  /** The parent state that opened this substate. Set internally by {@link FlixelState#openSubState}. */
  FlixelState parentState;

  /** Preserved so {@link #syncBackgroundToCameras()} can run after the game exists (constructor may run earlier). */
  private final Color subStateBackground;

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
    subStateBackground = bgColor != null ? new Color(bgColor) : new Color(Color.CLEAR);
    setBgColor(subStateBackground);
  }

  /** Re-applies this substate's background to all cameras (needed if the constructor ran before {@link me.stringdotjar.flixelgdx.FlixelGame#create}). */
  protected void syncBackgroundToCameras() {
    setBgColor(subStateBackground);
  }

  /** Closes this substate by telling the parent state to remove it. */
  public void close() {
    if (parentState != null) {
      parentState.closeSubState();
    }
  }

  @Override
  public String toString() {
    SnapshotArray<?> m = getMembers();
    return "FlixelSubState(members=" + (m != null ? m.size : 0) + ")";
  }
}
