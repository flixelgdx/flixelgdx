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
package me.stringdotjar.flixelgdx.input.action;

import org.jetbrains.annotations.NotNull;

/**
 * Built-in {@link FlixelSteamActionReader} instances. Use {@link #EMPTY} on {@link FlixelActionSet#steamReader} when Steam
 * is not linked so field reads stay null-safe without custom classes.
 */
public final class FlixelSteamActionReaders {

  /** Reader that always reports inactive digital input and zero analog vectors. */
  @NotNull
  public static final FlixelSteamActionReader EMPTY = new FlixelSteamActionReader() {
    @Override
    public boolean getDigital(@NotNull String actionName) {
      return false;
    }

    @Override
    public float getAnalogX(@NotNull String actionName) {
      return 0f;
    }

    @Override
    public float getAnalogY(@NotNull String actionName) {
      return 0f;
    }
  };

  private FlixelSteamActionReaders() {}
}
