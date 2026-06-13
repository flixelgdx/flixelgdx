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
 * Flixel-style kill and revive.
 *
 * <p>A killed object should not run normal updates or draws, but can be
 * revived later without reallocating. See {@link org.flixelgdx.FlixelBasic#kill() FlixelBasic.kill()} and
 * {@link org.flixelgdx.FlixelBasic#revive() FlixelBasic.revive()}.
 */
public interface FlixelKillable {

  /**
   * @return {@code true} when this instance is killed (disabled) in the Flixel sense.
   */
  boolean isKilled();

  /**
   * @param killed {@code true} to {@link #kill()}, {@code false} to {@link #revive()}.
   */
  void setKilled(boolean killed);

  void toggleKilled();

  void kill();

  void revive();
}
