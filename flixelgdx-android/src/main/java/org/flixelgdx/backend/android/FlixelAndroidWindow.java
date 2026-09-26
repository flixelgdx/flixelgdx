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

import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import org.flixelgdx.backend.FlixelWindow;
import org.jetbrains.annotations.NotNull;

/**
 * Android {@link FlixelWindow} that manages immersive fullscreen and Activity lifecycle.
 *
 * <p>Immersive fullscreen is applied via {@link WindowInsetsController} on API 30 and above;
 * on older devices the legacy system UI flags are used. The window is always considered fullscreen
 * on Android because the activity occupies the full screen.
 *
 * <p>Window focus changes are driven by Android lifecycle events in the launcher; this class cannot
 * override {@code Activity.onWindowFocusChanged} from outside the activity. Focus callbacks are
 * instead delivered through the {@link android.app.Application.ActivityLifecycleCallbacks} hooks
 * the launcher registers.
 *
 * <p>Title, position, and size setters are no-ops on Android as the activity always occupies
 * the full display.
 */
public class FlixelAndroidWindow implements FlixelWindow {

  @NotNull
  private final Activity activity;

  private int backBufferWidth;
  private int backBufferHeight;

  /**
   * Creates a window tied to the given activity.
   *
   * @param activity The activity that hosts the game.
   */
  public FlixelAndroidWindow(@NotNull Activity activity) {
    this.activity = activity;
  }

  /**
   * Makes the game fill the whole screen once its view has been set as the activity's content.
   *
   * <p>This hides the activity's title bar, lets the game draw edge to edge (including the area
   * around a display cutout on API 28 and above), and hides the system bars. Android clears
   * immersive mode whenever the window loses focus (a dialog, the notification shade, switching
   * apps), so it is reapplied every time the window regains focus.
   *
   * @param contentView The game view the launcher passed to {@code Activity.setContentView}.
   */
  void install(@NotNull View contentView) {
    ActionBar actionBar = activity.getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    Window window = activity.getWindow();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      WindowManager.LayoutParams attrs = window.getAttributes();
      attrs.layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
      window.setAttributes(attrs);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.setDecorFitsSystemWindows(false);
    }
    contentView.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
      if (hasFocus) {
        applyImmersiveFullscreen();
      }
    });
    applyImmersiveFullscreen();
  }

  /**
   * Updates the cached back-buffer dimensions, called from the GL renderer when the surface changes.
   *
   * @param w Back-buffer width in pixels.
   * @param h Back-buffer height in pixels.
   */
  public void setBackBufferSize(int w, int h) {
    backBufferWidth = w;
    backBufferHeight = h;
  }

  @Override
  public String getTitle() {
    return (String) activity.getTitle();
  }

  @Override
  public void setTitle(String title) {
    activity.setTitle(title);
  }

  @Override
  public int getBackBufferWidth() {
    return backBufferWidth;
  }

  @Override
  public int getBackBufferHeight() {
    return backBufferHeight;
  }

  @Override
  public boolean isFullscreen() {
    return true;
  }

  @Override
  public boolean supportsFullscreen() {
    return true;
  }

  @Override
  public void close() {
    activity.runOnUiThread(activity::finish);
  }

  @Override
  public boolean isFocused() {
    return activity.hasWindowFocus();
  }

  /**
   * Applies immersive (hide-all-system-bars) fullscreen using the best available API for the
   * running Android version. On API 30 and above, {@link WindowInsetsController} is used; on
   * older versions the legacy system UI visibility flags are set, which is the only available
   * mechanism below API 30.
   */
  @SuppressWarnings("deprecation")
  private void applyImmersiveFullscreen() {
    activity.runOnUiThread(() -> {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          WindowInsetsController controller =
              activity.getWindow().getInsetsController();
          if (controller != null) {
            controller.hide(WindowInsets.Type.systemBars());
            controller.setSystemBarsBehavior(
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
          }
        } else {
          // API 24-29: legacy system UI flags.
          View decorView = activity.getWindow().getDecorView();
          int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
          decorView.setSystemUiVisibility(flags);
        }
      } catch (Throwable ignored) {
        // Immersive fullscreen is best-effort; keep running even if it fails.
      }
    });
  }
}
