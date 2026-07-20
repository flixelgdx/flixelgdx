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
package org.flixelgdx.backend.android.haptics;

import android.app.Activity;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.flixelgdx.backend.FlixelHaptics;

/**
 * Android implementation of {@link FlixelHaptics} backed by the system {@link Vibrator} service.
 *
 * <p>On Android 8.0 (API 26) and above, vibration is driven by {@link VibrationEffect} for
 * accurate amplitude control. On older API levels the deprecated {@code Vibrator.vibrate(...)}
 * overloads are used as a fallback so that the minimum SDK (API 24) is still supported.
 *
 * <p>Requires the {@code android.permission.VIBRATE} permission in the game's
 * {@code AndroidManifest.xml}. Without it, all calls are silently ignored by the OS.
 *
 * <p>Installed automatically by {@link org.flixelgdx.backend.android.FlixelAndroidLauncher
 * FlixelAndroidLauncher}; game code should not need to instantiate this directly.
 */
public class FlixelAndroidHaptics implements FlixelHaptics {

  private final Vibrator vibrator;

  public FlixelAndroidHaptics(Activity activity) {
    vibrator = activity.getSystemService(Vibrator.class);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void vibrate(int ms) {
    if (ms <= 0 || vibrator == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
      vibrator.vibrate(ms);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public void vibrate(long[] pattern, int repeat) {
    if (pattern == null || pattern.length == 0 || vibrator == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat));
    } else {
      vibrator.vibrate(pattern, repeat);
    }
  }

  @Override
  public void cancel() {
    if (vibrator != null) {
      vibrator.cancel();
    }
  }

  @Override
  public boolean isSupported() {
    return vibrator != null && vibrator.hasVibrator();
  }
}
