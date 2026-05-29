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
