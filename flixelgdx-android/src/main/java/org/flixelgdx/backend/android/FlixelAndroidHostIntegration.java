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

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelMonitor;
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * Android host integration that reports {@link FlixelPlatform#ANDROID}.
 */
public class FlixelAndroidHostIntegration implements FlixelHostIntegration {

  private final FlixelLogger LOG = Flixel.log.tagged("Host");
  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();
  private final FlixelArray<FlixelMonitor> monitors = new FlixelArray<>(FlixelMonitor[]::new);
  private final Activity activity;

  /**
   * Creates a new host integration for Android.
   *
   * @param activity The Android {@link Activity} used for some specific features,
   *     such as opening URLs through an {@link Intent}.
   */
  public FlixelAndroidHostIntegration(Activity activity) {
    this.activity = activity;
  }

  @Override
  @NotNull
  public FlixelPlatform getPlatform() {
    return FlixelPlatform.ANDROID;
  }

  @Override
  @NotNull
  public FlixelSignal<String> onTextPasted() {
    return onTextPasted;
  }

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    // Not implemented in this release.
  }

  @Override
  public void openUrl(@NotNull String url) {
    try {
      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (Exception e) {
      LOG.error("Failed to open url.", e);
    }
  }

  @Override
  @NotNull
  public FlixelList<FlixelMonitor> getMonitors() {
    return monitors;
  }
}
