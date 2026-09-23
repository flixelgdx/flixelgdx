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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * <p>Unlike {@link FlixelState#setBgColor(FlixelColor)}, a substate's background color never
 * touches any camera. Instead, it is drawn as a translucent overlay behind the substate's own
 * members, the same way a sheet of colored glass held up in front of a scene tints only what is
 * behind the glass without repainting the scene itself. This keeps a substate's own visuals from
 * bleeding into the parent state or any other camera (such as a HUD camera) once the substate is
 * closed. A pause screen that dims the game behind it might look like this:
 *
 * <pre>{@code
 * public class PauseSubState extends FlixelSubState {
 *   public PauseSubState() {
 *     super(new FlixelColor(0f, 0f, 0f, 0.5f)); // Half-transparent black overlay.
 *   }
 * }
 * }</pre>
 */
public abstract class FlixelSubState extends FlixelState {

  /** Called when this substate is opened or resumed. */
  public Runnable openCallback;

  /** Called when this substate is closed. */
  public Runnable closeCallback;

  /** The parent state that opened this substate. Set internally by {@link FlixelState#openSubState}. */
  FlixelState parentState;

  /** This substate's own overlay background color. Never {@code null}; see {@link #getBgColor()}. */
  private final FlixelColor bgColor;

  /** Shared white pixel used to draw {@link #bgColor}, cached lazily on first draw. */
  private FlixelFrame whitePixel;

  /**
   * Creates a new substate with a clear background.
   */
  public FlixelSubState() {
    this(FlixelColor.CLEAR);
  }

  /**
   * Creates a new substate with the given background color.
   *
   * @param bgColor The background overlay color for this substate.
   */
  public FlixelSubState(FlixelColor bgColor) {
    super();
    this.bgColor = bgColor != null ? new FlixelColor(bgColor) : new FlixelColor(FlixelColor.CLEAR);
  }

  /** Closes this substate by telling the parent state to remove it. */
  public void close() {
    if (parentState != null) {
      parentState.closeSubState();
    }
  }

  /**
   * Draws this substate's background overlay, then its members.
   *
   * <p>When {@link #getBgColor()} has a non-zero alpha, a rectangle covering the active camera's
   * visible view is filled with that color before {@code super.draw(batch)} draws the members, so
   * pause menus and similar substates can dim or tint the state beneath them without changing any
   * camera's own background color.
   *
   * @param batch The batch to draw into.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (bgColor.a > 0f) {
      FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
      if (whitePixel == null) {
        whitePixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);
      }
      cam.fill(bgColor, true, 1f, batch, whitePixel);
    }
    super.draw(batch);
  }

  /**
   * Returns this substate's own background overlay color.
   *
   * <p>Unlike {@link FlixelState#getBgColor()}, which reads a camera's clear color, this returns
   * the color this substate draws behind its members in {@link #draw(FlixelBatch)}.
   *
   * @return This substate's background overlay color; never {@code null}.
   */
  @Override
  @NotNull
  public FlixelColor getBgColor() {
    return bgColor;
  }

  /**
   * Sets this substate's own background overlay color.
   *
   * <p>Unlike {@link FlixelState#setBgColor(FlixelColor)}, which assigns every camera's clear
   * color, this only changes the translucent overlay this substate draws behind its members. No
   * camera is touched.
   *
   * @param value The overlay color to copy into this substate's background. {@code null} is ignored.
   */
  @Override
  public void setBgColor(@Nullable FlixelColor value) {
    if (value == null) {
      return;
    }
    bgColor.set(value);
  }

  @Override
  public String toString() {
    FlixelArray<?> m = getMembers();
    return "FlixelSubState(members=" + (m != null ? m.getSize() : 0) + ")";
  }
}
