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

import android.app.Activity;
import android.app.AlertDialog;
import org.flixelgdx.backend.FlixelAlerter;
import org.jetbrains.annotations.NotNull;

/**
 * Android {@link FlixelAlerter} backed by an {@link AlertDialog} on the UI thread.
 *
 * <p>All calls are dispatched to the UI thread via {@link Activity#runOnUiThread} so the dialog
 * can be shown safely from the GL thread.
 */
public class FlixelAndroidAlerter implements FlixelAlerter {

  @NotNull
  private final Activity activity;

  /**
   * Creates an alerter tied to the given activity.
   *
   * @param activity The activity that hosts the game.
   */
  public FlixelAndroidAlerter(@NotNull Activity activity) {
    this.activity = activity;
  }

  @Override
  public void info(String title, String message) {
    show("Info", title, message);
  }

  @Override
  public void warn(String title, String message) {
    show("Warning", title, message);
  }

  @Override
  public void error(String title, String message) {
    show("Error", title, message);
  }

  private void show(String fallbackTitle, String title, String message) {
    String resolvedTitle = title != null && !title.isEmpty() ? title : fallbackTitle;
    String resolvedMessage = message != null ? message : "";
    activity.runOnUiThread(() -> {
      try {
        new AlertDialog.Builder(activity)
            .setTitle(resolvedTitle)
            .setMessage(resolvedMessage)
            .setPositiveButton("OK", null)
            .show();
      } catch (Throwable ignored) {
        // Never let a failed dialog crash the game; the message is already logged.
      }
    });
  }
}
