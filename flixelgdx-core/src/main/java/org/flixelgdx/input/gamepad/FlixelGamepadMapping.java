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
package org.flixelgdx.input.gamepad;

import org.flixelgdx.collections.FlixelMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Translates logical gamepad inputs to the native button and axis indices a specific device reports.
 *
 * <p>Every physical controller numbers its buttons and sticks differently. This mapping is how the
 * framework turns a layout-agnostic idea like {@link FlixelGamepadButton#A} into the exact index
 * that a particular controller uses for it. Game code never consults this directly; it works
 * through {@link FlixelGamepadInputManager}, which resolves logical inputs through the mapping
 * stored on each connected {@link FlixelGamepad}.
 *
 * <p>A backend populates a mapping by calling {@link #register(FlixelGamepadButton, int)} and
 * {@link #registerAxis(FlixelGamepadAxis, int)} for each input the hardware actually exposes.
 * Any button or axis not registered returns {@link #UNDEFINED} from its getter, signalling the
 * manager that the input is unavailable on this device.
 *
 * <p>The L2/R2 trigger duality is handled naturally: register L2 as a button index when the
 * hardware reports it digitally, or register {@link FlixelGamepadAxis#L2} as an axis index when
 * the hardware reports it as an analog value. The manager checks both and synthesizes button state
 * from the axis when only the axis is present.
 *
 * <p>Example - building a mapping for an Xbox-style controller:
 *
 * <pre>{@code
 * FlixelGamepadMapping mapping = new FlixelGamepadMapping();
 * mapping.register(FlixelGamepadButton.A,     0);
 * mapping.register(FlixelGamepadButton.B,     1);
 * mapping.register(FlixelGamepadButton.X,     2);
 * mapping.register(FlixelGamepadButton.Y,     3);
 * mapping.register(FlixelGamepadButton.L1,    4);
 * mapping.register(FlixelGamepadButton.R1,    5);
 * mapping.register(FlixelGamepadButton.BACK,  6);
 * mapping.register(FlixelGamepadButton.START, 7);
 * mapping.registerAxis(FlixelGamepadAxis.LEFT_X,  0);
 * mapping.registerAxis(FlixelGamepadAxis.LEFT_Y,  1);
 * mapping.registerAxis(FlixelGamepadAxis.RIGHT_X, 2);
 * mapping.registerAxis(FlixelGamepadAxis.RIGHT_Y, 3);
 * mapping.registerAxis(FlixelGamepadAxis.L2,      4);
 * mapping.registerAxis(FlixelGamepadAxis.R2,      5);
 * }</pre>
 *
 * @see FlixelGamepadButton
 * @see FlixelGamepadAxis
 * @see FlixelGamepadMappingResolver
 */
public class FlixelGamepadMapping {

  /** Sentinel returned when a button or axis is not registered in this mapping. */
  public static final int UNDEFINED = -1;

  private final FlixelMap<FlixelGamepadButton, Integer> buttons = new FlixelMap<>();
  private final FlixelMap<FlixelGamepadAxis, Integer> axes = new FlixelMap<>();
  private final FlixelMap<Integer, FlixelGamepadButton> buttonsByIndex = new FlixelMap<>();

  /** Creates an empty mapping; populate with {@link #register} and {@link #registerAxis}. */
  public FlixelGamepadMapping() {}

  /**
   * Registers the native index for a logical button.
   *
   * <p>Calling this a second time for the same button overwrites the previous entry. Passing a
   * native index of {@link #UNDEFINED} is equivalent to removing the button from the mapping.
   *
   * @param button The logical button; must not be {@code null}.
   * @param nativeIndex The hardware button index this controller reports for that button.
   */
  public void register(@NotNull FlixelGamepadButton button, int nativeIndex) {
    Integer previous = buttons.get(button);
    if (previous != null && buttonsByIndex.get(previous) == button) {
      // Drop the stale reverse entry so it does not outlive its logical button.
      buttonsByIndex.remove(previous);
    }
    if (nativeIndex == UNDEFINED) {
      buttons.remove(button);
    } else {
      buttons.put(button, nativeIndex);
      buttonsByIndex.put(nativeIndex, button);
    }
  }

  /**
   * Registers the native index for a logical axis.
   *
   * <p>Calling this a second time for the same axis overwrites the previous entry. Passing a
   * native index of {@link #UNDEFINED} is equivalent to removing the axis from the mapping.
   *
   * @param axis The logical axis; must not be {@code null}.
   * @param nativeIndex The hardware axis index this controller reports for that axis.
   */
  public void registerAxis(@NotNull FlixelGamepadAxis axis, int nativeIndex) {
    if (nativeIndex == UNDEFINED) {
      axes.remove(axis);
    } else {
      axes.put(axis, nativeIndex);
    }
  }

  /**
   * Returns the native button index for the given logical button, or {@link #UNDEFINED} if the
   * button is not registered in this mapping.
   *
   * @param button The logical button to look up; must not be {@code null}.
   * @return The native index, or {@link #UNDEFINED} when unavailable.
   */
  public int getButtonIndex(@NotNull FlixelGamepadButton button) {
    Integer index = buttons.get(button);
    return index != null ? index : UNDEFINED;
  }

  /**
   * Returns the logical button registered at the given native index, or {@code null} when no button
   * maps to it.
   *
   * <p>This is the reverse of {@link #register(FlixelGamepadButton, int)}, letting the manager turn a
   * hardware button index back into the logical {@link FlixelGamepadButton} it stands for without
   * scanning every registration. When two buttons share a native index (unusual), the most recently
   * registered one wins.
   *
   * @param nativeIndex A native hardware button index.
   * @return The logical button at that index, or {@code null} when unmapped.
   */
  @Nullable
  public FlixelGamepadButton getButtonForIndex(int nativeIndex) {
    return buttonsByIndex.get(nativeIndex);
  }

  /**
   * Returns the native axis index for the given logical axis, or {@link #UNDEFINED} if the axis
   * is not registered in this mapping.
   *
   * @param axis The logical axis to look up; must not be {@code null}.
   * @return The native index, or {@link #UNDEFINED} when unavailable.
   */
  public int getAxisIndex(@NotNull FlixelGamepadAxis axis) {
    Integer index = axes.get(axis);
    return index != null ? index : UNDEFINED;
  }

  /** Removes all button and axis registrations from this mapping. */
  public void clear() {
    buttons.clear();
    axes.clear();
    buttonsByIndex.clear();
  }
}
