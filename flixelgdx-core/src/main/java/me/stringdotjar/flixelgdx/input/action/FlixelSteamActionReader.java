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
 * Optional bridge for Steam Input (or similar): digital and analog values keyed by the same logical names as
 * {@link FlixelAction#getName()}. Assign to {@link FlixelActionSet#steamReader}; {@link FlixelActionDigital} and
 * {@link FlixelActionAnalog} query it every {@link FlixelActionSet#update(float)} alongside physical bindings.
 *
 * <p>Implementations usually live outside {@code flixelgdx-core} (steamworks4j, native JNI). Keep methods allocation-free
 * and fast; they run for every registered action set each frame.
 *
 * <p>Manifest action names in {@code steam_input_manifest.vdf} (see resource under this package) should match
 * {@link FlixelAction#getName()} so Steam overlay and your UI use one vocabulary.
 */
public interface FlixelSteamActionReader {

  /**
   * Whether the named digital Steam action is active this frame.
   *
   * @param actionName Logical action name (matches VDF and {@link FlixelAction#getName()}).
   * @return {@code true} when active; default implementations may return {@code false}.
   */
  boolean getDigital(@NotNull String actionName);

  /**
   * Analog X for the named vector action, typically in {@code -1..1}.
   *
   * @param actionName Logical action name.
   * @return X component.
   */
  float getAnalogX(@NotNull String actionName);

  /**
   * Analog Y for the named vector action, typically in {@code -1..1}.
   *
   * @param actionName Logical action name.
   * @return Y component.
   */
  float getAnalogY(@NotNull String actionName);
}
