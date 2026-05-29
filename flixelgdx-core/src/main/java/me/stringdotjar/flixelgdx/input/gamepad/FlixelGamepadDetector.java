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
package me.stringdotjar.flixelgdx.input.gamepad;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a controller name string (for example from {@link com.badlogic.gdx.controllers.Controller#getName()})
 * to a {@link FlixelGamepadModel}. Intended for connect-time use only; do not call from per-frame
 * hot paths.
 *
 * <p>Matching uses ordered substring checks on a once-normalized lowercase name. Add new literal
 * fragments when you encounter unknown devices in the wild, keeping more specific fragments before
 * broader ones.
 */
public final class FlixelGamepadDetector {

  private FlixelGamepadDetector() {}

  /**
   * Detects the best {@link FlixelGamepadModel} for the given controller name.
   *
   * @param controllerName Raw name from the backend, or {@code null}.
   * @return Never {@code null}; {@link FlixelGamepadModel#UNKNOWN} when no rule matches.
   */
  @NotNull
  public static FlixelGamepadModel detect(@Nullable String controllerName) {
    if (controllerName == null) {
      return FlixelGamepadModel.UNKNOWN;
    }
    String trimmed = controllerName.trim();
    if (trimmed.isEmpty()) {
      return FlixelGamepadModel.UNKNOWN;
    }
    String n = trimmed.toLowerCase(Locale.ROOT);

    if (n.contains("dualsense")) {
      return FlixelGamepadModel.PS5;
    }
    if (n.contains("dualshock") || n.contains("ds4")) {
      return FlixelGamepadModel.PS4;
    }
    if (n.contains("sony")) {
      return FlixelGamepadModel.PS4;
    }
    if (n.contains("xbox") && n.contains("360")) {
      return FlixelGamepadModel.XBOX_360;
    }
    if (n.contains("xbox") || n.contains("xinput")) {
      return FlixelGamepadModel.XBOX_ONE;
    }
    if (n.contains("wireless controller") && !n.contains("nintendo") && !n.contains("xbox")) {
      return FlixelGamepadModel.PS4;
    }
    if (n.contains("rvl") || n.contains("wii")) {
      return FlixelGamepadModel.WII;
    }
    if (n.contains("nintendo") || n.contains("switch") || n.contains("pro controller")) {
      return FlixelGamepadModel.SWITCH_PRO;
    }
    if (n.contains("ouya")) {
      return FlixelGamepadModel.OUYA;
    }
    if (n.contains("playstation")) {
      return FlixelGamepadModel.PS4;
    }
    return FlixelGamepadModel.UNKNOWN;
  }
}
