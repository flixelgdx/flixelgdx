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
package org.flixelgdx.backend.android.alert;

import android.app.Activity;
import android.app.AlertDialog;
import org.flixelgdx.backend.alert.FlixelAlerter;

public class FlixelAndroidAlerter implements FlixelAlerter {

  private final Activity activity;

  public FlixelAndroidAlerter(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void showInfoAlert(String title, String message) {
    showAlert(title, message, android.R.drawable.ic_dialog_info);
  }

  @Override
  public void showWarningAlert(String title, String message) {
    showAlert(title, message, android.R.drawable.ic_dialog_alert);
  }

  @Override
  public void showErrorAlert(String title, String message) {
    showAlert(title, message, android.R.drawable.stat_notify_error);
  }

  private void showAlert(final String title, final String message, final int iconResId) {
    activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
      .setTitle(title)
      .setMessage(message)
      .setIcon(iconResId)
      .setPositiveButton("OK", null)
      .setCancelable(true)
      .show());
  }
}
