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
package org.flixelgdx.backend;

/**
 * Platform-specific haptic (vibration) feedback for mobile devices.
 *
 * <p>Access the active implementation via {@link org.flixelgdx.Flixel#haptics Flixel.haptics}.
 * On platforms without a vibration motor (desktop, web), the default no-op implementation is used
 * and all calls are safely ignored. Check {@link #isSupported()} first if your game logic depends
 * on knowing whether feedback will actually fire.
 *
 * <p>Launchers on supported platforms (for example, Android) install a real implementation before
 * {@link org.flixelgdx.Flixel#initialize(org.flixelgdx.FlixelGame) Flixel.initialize(...)} runs.
 * You should not need to call {@link org.flixelgdx.Flixel#setHaptics(FlixelHaptics) Flixel.setHaptics(...)}
 * from game code unless you are providing a custom backend.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Simple one-shot pulse on a coin pickup.
 * Flixel.haptics.vibrate(40);
 *
 * // SOS pattern: three short, three long, three short pulses with no repeat.
 * Flixel.haptics.vibrate(new long[] { 0, 100, 100, 100, 100, 100, 300, 300, 300, 300, 300, 300, 100, 100, 100 }, -1);
 *
 * // Stop an ongoing vibration (e.g. the player lets go of a rumble trigger).
 * Flixel.haptics.cancel();
 * }</pre>
 *
 * @see org.flixelgdx.Flixel#haptics
 */
public interface FlixelHaptics {

  /**
   * Vibrates the device for the given duration.
   *
   * @param ms Duration in milliseconds; values less than or equal to zero are ignored.
   */
  void vibrate(int ms);

  /**
   * Vibrates the device following a timed on/off pattern.
   *
   * <p>Each element in {@code pattern} is a duration in milliseconds. The first element is an
   * initial off (silent) delay, the second is the first on period, the third is the next off
   * period, and so on, alternating off and on.
   *
   * @param pattern Alternating off/on durations in milliseconds, starting with the off delay.
   *     Must not be {@code null} or empty.
   * @param repeat Index into {@code pattern} to restart from once the end is reached, or
   *     {@code -1} to play the pattern once and stop.
   */
  void vibrate(long[] pattern, int repeat);

  /**
   * Cancels any active vibration started by {@link #vibrate(int)} or {@link #vibrate(long[], int)}.
   *
   * <p>Safe to call even when no vibration is active.
   */
  void cancel();

  /**
   * Returns whether haptic feedback is available on this device and platform.
   *
   * <p>On desktop and web builds this always returns {@code false}. On Android it reflects
   * whether the device actually has a vibrator.
   *
   * @return {@code true} if calls to {@link #vibrate(int)} and {@link #vibrate(long[], int)}
   *     will produce real feedback.
   */
  boolean isSupported();
}
