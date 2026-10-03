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
import org.flixelgdx.backend.android.file.FlixelAndroidUriFile;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFilePickListener;
import org.flixelgdx.file.FlixelFilePicker;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

/**
 * Android host integration that reports {@link FlixelPlatform#ANDROID}.
 *
 * <p>The file picker uses the Storage Access Framework ({@code ACTION_OPEN_DOCUMENT}) through an
 * invisible helper activity, {@link FlixelAndroidPickerActivity}, that the library manifest
 * registers automatically. Extensions are converted to MIME types with {@link MimeTypeMap}; if any
 * extension has no known MIME type, the dialog shows every file and the result is filtered by name
 * afterward. Chosen files are {@link FlixelAndroidUriFile} instances that read through the
 * {@link ContentResolver}. Only one picker can be open at a time; a second request while one is open
 * receives an empty array.
 */
public class FlixelAndroidHostIntegration implements FlixelHostIntegration {

  private final FlixelLogger log = Flixel.log.tagged("Host");
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
  public void pickFile(@NotNull FlixelFilePickListener listener, @NotNull String... extensions) {
    showPicker(listener, extensions, false);
  }

  @Override
  public void pickFiles(@NotNull FlixelFilePickListener listener, @NotNull String... extensions) {
    showPicker(listener, extensions, true);
  }

  @Override
  public boolean supportsFilePicker() {
    return true;
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
      log.error("Failed to open url.", e);
    }
  }

  @Override
  @NotNull
  public FlixelList<FlixelMonitor> getMonitors() {
    return monitors;
  }

  private void showPicker(FlixelFilePickListener listener, String[] extensions, boolean many) {
    if (FlixelAndroidPickerActivity.isPending()) {
      FlixelFilePicker.deliver(listener, FlixelFilePicker.NO_FILES);
      return;
    }
    final String[] exts = FlixelFilePicker.normalize(extensions);
    String[] types = new String[exts.length];
    boolean allKnown = true;
    MimeTypeMap map = MimeTypeMap.getSingleton();
    for (int i = 0; i < exts.length; i++) {
      types[i] = map.getMimeTypeFromExtension(exts[i]);
      if (types[i] == null) {
        allKnown = false;
        break;
      }
    }
    final boolean filterByName = exts.length > 0 && !allKnown;
    FlixelAndroidPickerActivity.setPending(uris -> {
      ContentResolver resolver = activity.getContentResolver();
      FlixelFile[] files = new FlixelFile[uris.length];
      int count = 0;
      for (int i = 0; i < uris.length; i++) {
        String name = null;
        long size = -1L;
        try (Cursor cursor = resolver.query(uris[i], null, null, null, null)) {
          if (cursor != null && cursor.moveToFirst()) {
            int nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (nameCol >= 0 && !cursor.isNull(nameCol)) {
              name = cursor.getString(nameCol);
            }
            if (sizeCol >= 0 && !cursor.isNull(sizeCol)) {
              size = cursor.getLong(sizeCol);
            }
          }
        } catch (RuntimeException e) {
          log.warn("Could not read the picked file's details.", e);
        }
        if (name == null) {
          String last = uris[i].getLastPathSegment();
          name = last != null ? last : "file";
        }
        if (filterByName && !FlixelFilePicker.matches(name, exts)) {
          continue;
        }
        files[count++] = new FlixelAndroidUriFile(resolver, uris[i], name, size);
      }
      if (count < files.length) {
        FlixelFile[] trimmed = new FlixelFile[count];
        System.arraycopy(files, 0, trimmed, 0, count);
        files = trimmed;
      }
      FlixelFilePicker.deliver(listener, files);
    });
    try {
      Intent intent = new Intent(activity, FlixelAndroidPickerActivity.class);
      if (!filterByName) {
        intent.putExtra(FlixelAndroidPickerActivity.EXTRA_TYPES, types);
      }
      intent.putExtra(FlixelAndroidPickerActivity.EXTRA_MANY, many);
      activity.startActivity(intent);
    } catch (RuntimeException e) {
      log.error("Failed to open the file picker.", e);
      FlixelAndroidPickerActivity.setPending(null);
      FlixelFilePicker.deliver(listener, FlixelFilePicker.NO_FILES);
    }
  }
}
