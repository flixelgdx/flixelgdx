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
package org.flixelgdx.functional;

/**
 * Lifecycle flags controlling whether an object participates in the update-draw loop each frame.
 *
 * <p>The two flags have distinct effects:
 * <ul>
 *   <li>{@link #isExists()} - when {@code false}, groups and states skip this instance for both
 *       {@link FlixelUpdatable#update(float) update} and {@link FlixelDrawable#draw} entirely.</li>
 *   <li>{@link #isActive()} - when {@code false}, only {@link FlixelUpdatable#update(float) update}
 *       is skipped; drawing still proceeds as long as {@link #isExists()} is {@code true}.</li>
 * </ul>
 *
 * @see org.flixelgdx.FlixelBasic
 * @see IFlixelBasic
 */
public interface FlixelExistable {

  /**
   * When {@code false}, groups and states skip this instance for automatic
   * {@link FlixelUpdatable#update(float) update} and {@link FlixelDrawable#draw}.
   *
   * @return The current {@code exists} flag.
   */
  boolean isExists();

  /**
   * Sets the exists flag, controlling whether this object is updated and drawn.
   *
   * @param exists The new {@code exists} flag.
   */
  void setExists(boolean exists);

  /**
   * When {@code false}, {@link FlixelUpdatable#update(float) update} is skipped even if
   * {@link #isExists()} is {@code true}.
   *
   * @return The current {@code active} flag.
   */
  boolean isActive();

  /**
   * Sets the active flag, controlling whether this object's update runs.
   *
   * @param active The new {@code active} flag.
   */
  void setActive(boolean active);
}
