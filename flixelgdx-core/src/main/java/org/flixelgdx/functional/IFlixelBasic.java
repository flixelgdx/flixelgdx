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

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

/**
 * Full {@link org.flixelgdx.FlixelBasic FlixelBasic}-style contract: per-frame update and draw hooks,
 * existence and active flags, visibility, kill and revive, teardown, and libGDX {@link Disposable} /
 * {@link Pool.Poolable} hooks. Extend {@link org.flixelgdx.FlixelBasic FlixelBasic} when you want the
 * default field-based implementation, or implement this interface on your own type when you need a
 * custom base class but still want to add instances to a {@link org.flixelgdx.FlixelState FlixelState}
 * or {@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup}.
 *
 * @see org.flixelgdx.FlixelBasic
 * @see org.flixelgdx.group.FlixelBasicGroup
 */
public interface IFlixelBasic extends
    FlixelUpdatable,
    FlixelDrawable,
    FlixelDestroyable,
    FlixelKillable,
    FlixelVisible,
    Disposable,
    Pool.Poolable {

  /**
   * When {@code false}, groups and states skip this instance for automatic {@link #update(float)}.
   *
   * @return The current {@code exists} flag.
   */
  boolean isExists();

  /**
   * @param exists The new {@code exists} flag.
   */
  void setExists(boolean exists);

  /**
   * When {@code false}, {@link #update(float)} is skipped even if {@link #isExists()} is {@code true}.
   *
   * @return The current {@code active} flag.
   */
  boolean isActive();

  /**
   * @param active The new {@code active} flag.
   */
  void setActive(boolean active);
}
