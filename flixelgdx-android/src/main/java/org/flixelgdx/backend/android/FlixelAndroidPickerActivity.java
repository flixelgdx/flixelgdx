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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

/**
 * A tiny invisible activity that runs the system document picker for the framework.
 *
 * <p>Android only returns picker results to an activity. Rather than forcing every game to forward
 * result callbacks, the framework launches this transparent helper, which opens the Storage Access
 * Framework picker with the modern Activity Result API, hands the result to the pending
 * {@link Listener}, and closes itself. It is declared in this library's manifest, so games do not
 * register it by hand. Game code never starts it directly; use
 * {@link org.flixelgdx.backend.FlixelHostIntegration#pickFile} instead.
 */
public class FlixelAndroidPickerActivity extends ComponentActivity {

  /** Intent extra holding the MIME types to accept as a {@code String[]}. */
  static final String EXTRA_TYPES = "org.flixelgdx.PICK_TYPES";

  /** Intent extra holding whether several files may be chosen. */
  static final String EXTRA_MANY = "org.flixelgdx.PICK_MANY";

  private static final String[] ANY_TYPE = {"*/*"};

  @Nullable
  private static Listener pending;

  /**
   * Sets who is told about the next picker result.
   *
   * @param listener The listener, or {@code null} to clear it.
   */
  static void setPending(@Nullable Listener listener) {
    pending = listener;
  }

  /**
   * Returns {@code true} if a picker is currently waiting for a result.
   *
   * @return Whether a pick is in flight.
   */
  static boolean isPending() {
    return pending != null;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent request = getIntent();
    boolean many = request.getBooleanExtra(EXTRA_MANY, false);
    String[] types = request.getStringArrayExtra(EXTRA_TYPES);
    if (types == null || types.length == 0) {
      types = ANY_TYPE;
    }

    ActivityResultLauncher<String[]> single = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), uri -> finishWith(uri != null ? new Uri[] {uri} : null));
    ActivityResultLauncher<String[]> multiple = registerForActivityResult(
        new ActivityResultContracts.OpenMultipleDocuments(), this::finishWithList);

    if (savedInstanceState != null) {
      return;
    }
    if (pending == null) {
      finish();
      return;
    }
    try {
      if (many) {
        multiple.launch(types);
      } else {
        single.launch(types);
      }
    } catch (RuntimeException e) {
      finishWith(null);
    }
  }

  private void finishWithList(@Nullable List<Uri> list) {
    if (list == null || list.isEmpty()) {
      finishWith(null);
      return;
    }
    Uri[] uris = new Uri[list.size()];
    for (int i = 0; i < uris.length; i++) {
      uris[i] = list.get(i);
    }
    finishWith(uris);
  }

  private void finishWith(@Nullable Uri[] uris) {
    if (uris != null) {
      for (int i = 0; i < uris.length; i++) {
        try {
          getContentResolver().takePersistableUriPermission(uris[i], Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (RuntimeException ignored) {
          // Not every provider offers persistable grants; the temporary grant is enough to read now.
        }
      }
    }
    Listener listener = pending;
    pending = null;
    finish();
    if (listener != null) {
      listener.onResult(uris != null ? uris : new Uri[0]);
    }
  }

  /** Receives the URIs the user chose. */
  interface Listener {

    /**
     * Called once with the chosen URIs.
     *
     * @param uris The chosen URIs; empty on cancel.
     */
    void onResult(@NotNull Uri @NotNull [] uris);
  }
}
