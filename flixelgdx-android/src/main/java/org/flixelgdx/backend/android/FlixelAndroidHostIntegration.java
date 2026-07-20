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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.WindowManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Android {@link FlixelHostIntegration}: system notifications via {@link NotificationManager},
 * screen wake lock via {@code FLAG_KEEP_SCREEN_ON}, back-button exit confirmation via an
 * {@link InputMultiplexer}, and text clipboard access via {@link ClipboardManager}.
 *
 * <p>Installed automatically by {@link FlixelAndroidLauncher}; game code should not need to
 * instantiate this directly.
 *
 * <h2>Notification permission</h2>
 *
 * <p>Android 13 (API 33) and above require the {@code POST_NOTIFICATIONS} runtime permission
 * before notifications are displayed. Call {@link #requestNotificationPermission()} early in your
 * game, ideally during a loading screen or in response to a user gesture, and confirm
 * {@link #supportsNotifications()} returns {@code true} before calling
 * {@link #sendNotification(String, String)}.
 *
 * <p>You must also declare the permission in your {@code AndroidManifest.xml}:
 *
 * <pre>{@code
 * <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
 * }</pre>
 *
 * <h2>Exit confirmation</h2>
 *
 * <p>When a message is set via {@link #setExitConfirmation(String)}, the hardware or gesture back
 * action is caught by an {@link InputMultiplexer} inserted in front of any existing
 * {@link InputProcessor}. Pressing back shows an {@link AlertDialog} with the message and an
 * "Exit" button that calls {@code Gdx.app.exit()}. Passing {@code null} removes the guard and
 * restores the previous processor.
 */
public class FlixelAndroidHostIntegration implements FlixelHostIntegration {

  private static final int NOTIFICATION_ID = 0;
  private static final String NOTIFICATION_CHANNEL_ID = "flixelgdx";
  private static final String NOTIFICATION_CHANNEL_NAME = "Game Notifications";

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();
  private final Activity activity;
  private final NotificationManager notificationManager;
  private final ClipboardManager clipboardManager;
  private final InputAdapter backKeyHandler;

  @Nullable
  private String exitConfirmationMessage;
  @Nullable
  private InputProcessor previousProcessor;

  FlixelAndroidHostIntegration(Activity activity) {
    this.activity = activity;
    this.notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
    this.clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
    this.backKeyHandler = new InputAdapter() {
      @Override
      public boolean keyDown(int keycode) {
        if (keycode == Keys.BACK && exitConfirmationMessage != null) {
          showExitDialog(exitConfirmationMessage);
          return true;
        }
        return false;
      }
    };
    if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.createNotificationChannel(new NotificationChannel(
          NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
    }
  }

  @Override
  public void requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) {
      activity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 0);
    }
  }

  @Override
  public void requestAttention() {}

  @Override
  public void keepScreenAwake(boolean awake) {
    activity.runOnUiThread(() -> {
      if (awake) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      } else {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }
    });
  }

  @Override
  public void setExitConfirmation(@Nullable String message) {
    boolean wasActive = exitConfirmationMessage != null;
    exitConfirmationMessage = message;
    if (message != null && !wasActive) {
      Gdx.input.setCatchKey(Keys.BACK, true);
      InputProcessor current = Gdx.input.getInputProcessor();
      previousProcessor = current;
      InputMultiplexer multiplexer = new InputMultiplexer();
      multiplexer.addProcessor(backKeyHandler);
      if (current != null) {
        multiplexer.addProcessor(current);
      }
      Gdx.input.setInputProcessor(multiplexer);
    } else if (message == null && wasActive) {
      Gdx.input.setCatchKey(Keys.BACK, false);
      Gdx.input.setInputProcessor(previousProcessor);
      previousProcessor = null;
    }
  }

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message");
    if (!supportsNotifications()) {
      return;
    }
    notificationManager.notify(NOTIFICATION_ID, buildNotification(title != null ? title : "", message));
  }

  @Override
  public void copyToClipboard(@NotNull String text) {
    Objects.requireNonNull(text, "text");
    if (!supportsClipboard()) {
      return;
    }
    clipboardManager.setPrimaryClip(ClipData.newPlainText("", text));
  }

  @Override
  public void pasteFromClipboard() {
    if (!supportsClipboard()) {
      return;
    }
    ClipData clip = clipboardManager.getPrimaryClip();
    if (clip == null || clip.getItemCount() == 0) {
      return;
    }
    CharSequence text = clip.getItemAt(0).coerceToText(activity);
    if (text != null && text.length() > 0) {
      onTextPasted.dispatch(text.toString());
    }
  }

  @Override
  public boolean supportsNotifications() {
    return notificationManager != null && notificationManager.areNotificationsEnabled();
  }

  @Override
  public boolean supportsWakeLock() {
    return true;
  }

  @Override
  public boolean supportsClipboard() {
    return clipboardManager != null;
  }

  @Override
  @NotNull
  public FlixelSignal<String> onTextPasted() {
    return onTextPasted;
  }

  @SuppressWarnings("deprecation")
  private Notification buildNotification(String title, String message) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new Notification.Builder(activity, NOTIFICATION_CHANNEL_ID)
          .setContentTitle(title)
          .setContentText(message)
          .setSmallIcon(android.R.drawable.ic_dialog_info)
          .setAutoCancel(true)
          .build();
    }
    return new Notification.Builder(activity)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setAutoCancel(true)
        .build();
  }

  private void showExitDialog(String message) {
    activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
        .setMessage(message)
        .setPositiveButton("Exit", (dialog, which) -> Gdx.app.exit())
        .setNegativeButton("Cancel", null)
        .setCancelable(false)
        .show());
  }
}
