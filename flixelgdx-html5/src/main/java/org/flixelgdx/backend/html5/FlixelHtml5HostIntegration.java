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
package org.flixelgdx.backend.html5;

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelMonitor;
import org.flixelgdx.backend.FlixelNoopMonitor;
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.backend.html5.file.FlixelHtml5MemoryFile;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFilePickListener;
import org.flixelgdx.file.FlixelFilePickerHelpers;
import org.flixelgdx.file.FlixelFileSaveListener;
import org.flixelgdx.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * Web host integration: the bridge between the game and the browser environment it runs in.
 *
 * <p>This maps the framework's OS-level helpers onto the browser APIs that provide the same thing:
 * the Notifications API for toasts, the Clipboard API for copy and paste, the Screen Wake Lock API
 * to keep the display awake, and {@code beforeunload} for an exit confirmation. Where the browser
 * has no equivalent (a real monitor list, for instance) the method returns an empty or default
 * value rather than pretending.
 *
 * <p>Several of these browser APIs answer asynchronously through promises, which cannot hand a
 * value straight back to synchronous Java. Those results are delivered through the framework's own
 * asynchronous channels instead: a clipboard read arrives on {@link #onTextPasted()} once the
 * promise resolves, exactly as the interface documents.
 *
 * <p>The file picker uses a hidden {@code <input type="file">} element. Browsers only open it from a
 * user gesture, so {@link #pickFile(FlixelFilePickListener, String...)} must be called from a click
 * or key handler. Chosen files are read fully into memory and wrapped as read-only
 * {@link FlixelHtml5MemoryFile} instances. The listener runs directly from the browser event, which
 * is already the game's only thread. Browsers that do not fire a cancel event never call the
 * listener when the user dismisses the dialog.
 *
 * <p>{@link #saveFile(byte[], String, FlixelFileSaveListener, String...)} uses the File System
 * Access API's save dialog where the browser has one. Elsewhere, or if that dialog cannot open, the
 * data is downloaded as a file instead, and the listener reports {@code true} once the download
 * starts, since the browser gives no further feedback.
 */
public class FlixelHtml5HostIntegration implements FlixelHostIntegration {

  @NotNull
  final FlixelArray<FlixelMonitor> monitors = new FlixelArray<>();

  @NotNull
  private final FlixelSignal<String> textPasted = new FlixelSignal<>();

  @Override
  public void requestNotificationPermission() {
    requestNotificationPermissionJs();
  }

  @Override
  public void requestAttention() {
    flashTitle();
  }

  @Override
  public void requestMonitorPermission() {
    // Since requestScreenDetails(...) registers a 'screenschange' callback on the
    // JavaScript side, we have a guard here to ensure the same callback doesn't get
    // added twice and cause issues.
    if (!FlixelHtml5MonitorHelper.isWindowManagementSupported()) {
      FlixelHtml5MonitorHelper.requestScreenDetails(this::updateMonitors);
    }
  }

  @Override
  public void keepScreenAwake(boolean awake) {
    if (awake) {
      acquireWakeLock();
    } else {
      releaseWakeLock();
    }
  }

  @Override
  public void setExitConfirmation(@Nullable String message) {
    setBeforeUnload(message);
  }

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    if (supportsNotifications()) {
      showNotification(title != null ? title : "", message);
    }
  }

  @Override
  public void copyToClipboard(@NotNull String text) {
    writeClipboard(text);
  }

  @Override
  public void pasteFromClipboard() {
    readClipboard(text -> {
      if (text != null) {
        textPasted.dispatch(text);
      }
    });
  }

  @Override
  public void pickFile(@NotNull FlixelFilePickListener listener, @NotNull String... extensions) {
    showFilePicker(listener, extensions, false);
  }

  @Override
  public void pickFiles(@NotNull FlixelFilePickListener listener, @NotNull String... extensions) {
    showFilePicker(listener, extensions, true);
  }

  @Override
  public void saveFile(
      byte @NotNull [] data,
      @NotNull String name,
      @NotNull FlixelFileSaveListener listener,
      @NotNull String... extensions) {
    String[] exts = FlixelFilePickerHelpers.normalize(extensions);
    saveBlob(Int8Array.copyFromJavaArray(data), FlixelFilePickerHelpers.withExtension(name, exts), String.join(",", exts),
        listener::onSave);
  }

  @Override
  public boolean supportsNotifications() {
    return notificationsGranted();
  }

  @Override
  public boolean supportsWakeLock() {
    return wakeLockSupported();
  }

  @Override
  public boolean supportsClipboard() {
    return clipboardSupported();
  }

  @Override
  public boolean supportsFilePicker() {
    return true;
  }

  @Override
  public boolean supportsMonitors() {
    return FlixelHtml5MonitorHelper.isWindowManagementSupported() && !monitors.isEmpty();
  }

  @Override
  @NotNull
  public FlixelSignal<String> onTextPasted() {
    return textPasted;
  }

  @Override
  @NotNull
  public FlixelList<FlixelMonitor> getMonitors() {
    return monitors;
  }

  @Override
  public @NotNull FlixelMonitor getPrimaryMonitor() {
    for (FlixelMonitor monitor : monitors) {
      if (monitor.isPrimary()) {
        return monitor;
      }
    }
    if (!monitors.isEmpty()) {
      return monitors.get(0);
    }
    return FlixelNoopMonitor.INSTANCE;
  }

  @Override
  public void openUrl(@NotNull String url) {
    jsOpenUrl(url);
  }

  @Override
  @NotNull
  public FlixelPlatform getPlatform() {
    return FlixelPlatform.HTML5;
  }

  /**
   * Updates the current {@link #monitors} array from the provided JavaScript array.
   *
   * <p>This is primarily meant to be used for {@link FlixelHtml5MonitorHelper#requestScreenDetails},
   * although you may find it useful for other purposes.
   *
   * @param screens The JavaScript array of the user's connected monitors. May be {@code null} if
   *     {@link FlixelHostIntegration#supportsMonitors()} returns {@code false}.
   */
  public void updateMonitors(@Nullable JSArray<FlixelHtml5MonitorHelper.JSScreen> screens) {
    monitors.clear();

    if (screens == null) {
      return;
    }

    for (int i = 0; i < screens.getLength(); i++) {
      FlixelHtml5MonitorHelper.JSScreen screen = screens.get(i);
      FlixelHtml5Monitor monitor = new FlixelHtml5Monitor(screen.getLabel(), screen.getLeft(), screen.getTop(),
          screen.getWidth(), screen.getHeight(), screen.isPrimary());
      monitors.add(monitor);
    }
  }

  private static void showFilePicker(FlixelFilePickListener listener, String[] extensions, boolean many) {
    String[] exts = FlixelFilePickerHelpers.normalize(extensions);
    StringBuilder accept = new StringBuilder();
    for (int i = 0; i < exts.length; i++) {
      if (i > 0) {
        accept.append(',');
      }
      accept.append('.').append(exts[i]);
    }
    openFileInput(accept.toString(), many, (names, data) -> {
      int count = names.getLength();
      FlixelFile[] files = new FlixelFile[count];
      for (int i = 0; i < count; i++) {
        files[i] = new FlixelHtml5MemoryFile(names.get(i).stringValue(), data.get(i).copyToJavaArray());
      }
      listener.onPick(files);
    });
  }

  @JSBody(params = { "accept", "multiple", "callback" }, script = """
      var input = document.createElement('input');
      input.type = 'file';
      input.accept = accept;
      input.multiple = multiple;
      input.style.display = 'none';
      document.body.appendChild(input);
      var done = false;
      function finish(names, data) {
        if (done) { return; }
        done = true;
        if (input.parentNode) { input.parentNode.removeChild(input); }
        callback(names, data);
      }
      input.addEventListener('cancel', function() { finish([], []); });
      input.addEventListener('change', function() {
        var list = input.files;
        if (!list || list.length === 0) { finish([], []); return; }
        Promise.all(Array.prototype.map.call(list, function(f) {
          return f.arrayBuffer()
              .then(function(b) { return { n: f.name, d: new Int8Array(b) }; })
              .catch(function() { return null; });
        })).then(function(results) {
          var names = [];
          var data = [];
          for (var i = 0; i < results.length; i++) {
            if (results[i]) { names.push(results[i].n); data.push(results[i].d); }
          }
          finish(names, data);
        }).catch(function() { finish([], []); });
      });
      input.click();
      """)
  private static native void openFileInput(String accept, boolean multiple, FilePickCallback callback);

  @JSBody(params = { "data", "name", "exts", "callback" }, script = """
      function download() {
        try {
          var url = URL.createObjectURL(new Blob([data]));
          var a = document.createElement('a');
          a.href = url;
          a.download = name;
          a.style.display = 'none';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          setTimeout(function() { URL.revokeObjectURL(url); }, 10000);
          callback(true);
        } catch (e) {
          callback(false);
        }
      }
      if (typeof window.showSaveFilePicker !== 'function') { download(); return; }
      var options = { suggestedName: name };
      if (exts) {
        options.types = [{
          description: 'Files',
          accept: { 'application/octet-stream': exts.split(',').map(function(e) { return '.' + e; }) }
        }];
      }
      var chosen = false;
      window.showSaveFilePicker(options)
          .then(function(handle) { chosen = true; return handle.createWritable(); })
          .then(function(writable) { return writable.write(data).then(function() { return writable.close(); }); })
          .then(function() { callback(true); }, function(err) {
            if (!chosen && !(err && err.name === 'AbortError')) { download(); } else { callback(false); }
          });
      """)
  private static native void saveBlob(Int8Array data, String name, String exts, SaveCallback callback);

  @JSBody(script = """
      if (typeof Notification !== 'undefined') {
        try {
          var p = Notification.requestPermission();
          if (p && typeof p.catch === 'function') { p.catch(function() {}); }
        } catch (e) {}
      }
      """)
  private static native void requestNotificationPermissionJs();

  @JSBody(script = "return typeof Notification !== 'undefined' && Notification.permission === 'granted';")
  private static native boolean notificationsGranted();

  @JSBody(params = { "title", "body" }, script = "new Notification(title, { body: body });")
  private static native void showNotification(String title, String body);

  @JSBody(script = "return !!(navigator.clipboard);")
  private static native boolean clipboardSupported();

  @JSBody(params = "text", script = """
      if (navigator.clipboard && navigator.clipboard.writeText) {
        try {
          var p = navigator.clipboard.writeText(text);
          if (p && typeof p.catch === 'function') { p.catch(function() {}); }
        } catch (e) {}
      }
      """)
  private static native void writeClipboard(String text);

  @JSBody(params = "callback", script = """
      if (navigator.clipboard && navigator.clipboard.readText) {
        navigator.clipboard.readText().then(function(t) { callback(t); }).catch(function() {});
      }
      """)
  private static native void readClipboard(TextCallback callback);

  @JSBody(script = "return !!(navigator.wakeLock);")
  private static native boolean wakeLockSupported();

  @JSBody(params = "url", script = "window.open(url, \"_blank\");")
  private static native void jsOpenUrl(String url);

  @JSBody(script = """
      if (navigator.wakeLock) {
        navigator.wakeLock.request('screen').then(function(s) { window.__flixelWakeLock = s; }).catch(function() {});
      }
      """)
  private static native void acquireWakeLock();

  @JSBody(script = """
      var lock = window.__flixelWakeLock;
      window.__flixelWakeLock = null;
      if (lock) {
        try {
          var p = lock.release();
          if (p && typeof p.catch === 'function') { p.catch(function() {}); }
        } catch (e) {}
      }
      """)
  private static native void releaseWakeLock();

  @JSBody(params = "message", script = """
      window.__flixelExitMessage = message;
      if (!window.__flixelBeforeUnload) {
        window.__flixelBeforeUnload = function(e) {
          if (window.__flixelExitMessage) { e.preventDefault(); e.returnValue = window.__flixelExitMessage; }
        };
        window.addEventListener('beforeunload', window.__flixelBeforeUnload);
      }
      """)
  private static native void setBeforeUnload(String message);

  @JSBody(script = """
      var original = document.title; var count = 0;
      var id = setInterval(function() {
        document.title = (count % 2 === 0) ? '(!) ' + original : original; count++;
        if (count > 6 || document.hasFocus()) { clearInterval(id); document.title = original; }
      }, 500);
      """)
  private static native void flashTitle();

  /** Receives a resolved clipboard string from the browser's asynchronous read. */
  @JSFunctor
  private interface TextCallback extends JSObject {
    void accept(String text);
  }

  /** Receives the names and raw bytes of the files the browser file input produced. */
  @JSFunctor
  private interface FilePickCallback extends JSObject {
    void accept(JSArray<JSString> names, JSArray<Int8Array> data);
  }

  /** Receives whether the browser saved or downloaded the file. */
  @JSFunctor
  private interface SaveCallback extends JSObject {
    void accept(boolean saved);
  }
}
