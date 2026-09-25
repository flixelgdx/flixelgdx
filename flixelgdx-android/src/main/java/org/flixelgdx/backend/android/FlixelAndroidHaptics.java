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
package org.flixelgdx.backend.android;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import org.flixelgdx.backend.FlixelHaptics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Android {@link FlixelHaptics} backed by the system {@link Vibrator} service.
 *
 * <p>On API 31 and above, {@link VibratorManager} is used to obtain the default vibrator. On
 * API 26-30, {@link VibrationEffect} is used with the deprecated {@link Vibrator} lookup. On
 * API 24-25, the legacy {@code vibrate(long)} call is used.
 *
 * <p>Requires the {@code android.permission.VIBRATE} permission in the manifest.
 */
public class FlixelAndroidHaptics implements FlixelHaptics {

  @Nullable
  private final Vibrator vibrator;

  /**
   * Creates a haptics provider for the given context.
   *
   * @param context The Android context used to look up the vibrator service.
   */
  @SuppressWarnings("deprecation")
  public FlixelAndroidHaptics(@NotNull Context context) {
    Vibrator v = null;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        VibratorManager manager =
            (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        if (manager != null) {
          v = manager.getDefaultVibrator();
        }
      } else {
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      }
    } catch (Throwable ignored) {
      // Leave vibrator null if the service is unavailable.
    }
    vibrator = v;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void vibrate(int ms) {
    if (ms <= 0 || vibrator == null) {
      return;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
      } else {
        vibrator.vibrate(ms);
      }
    } catch (Throwable ignored) {
      // Best-effort.
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void vibrate(long[] pattern, int repeat) {
    if (pattern == null || pattern.length == 0 || vibrator == null) {
      return;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat));
      } else {
        vibrator.vibrate(pattern, repeat);
      }
    } catch (Throwable ignored) {
      // Best-effort.
    }
  }

  @Override
  public void cancel() {
    if (vibrator == null) {
      return;
    }
    try {
      vibrator.cancel();
    } catch (Throwable ignored) {
      // Best-effort.
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isSupported() {
    if (vibrator == null) {
      return false;
    }
    try {
      return vibrator.hasVibrator();
    } catch (Throwable ignored) {
      return false;
    }
  }
}
