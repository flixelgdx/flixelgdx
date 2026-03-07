package me.stringdotjar.flixelgdx.backend.android.alert;

import android.app.Activity;
import android.app.AlertDialog;
import me.stringdotjar.flixelgdx.backend.FlixelAlerter;

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
