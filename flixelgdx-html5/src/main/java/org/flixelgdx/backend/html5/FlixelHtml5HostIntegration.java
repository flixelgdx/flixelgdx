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
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

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
 */
public class FlixelHtml5HostIntegration implements FlixelHostIntegration {

  @NotNull
  private final FlixelSignal<String> textPasted = new FlixelSignal<>();

  @NotNull
  private final FlixelArray<FlixelMonitor> monitors = new FlixelArray<>();

  @Override
  public void requestNotificationPermission() {
    requestNotificationPermissionJs();
  }

  @Override
  public void requestAttention() {
    flashTitle();
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
  @NotNull
  public FlixelPlatform getPlatform() {
    return FlixelPlatform.Web;
  }

  @JSBody(script = "if (typeof Notification !== 'undefined') { Notification.requestPermission(); }")
  private static native void requestNotificationPermissionJs();

  @JSBody(script = "return typeof Notification !== 'undefined' && Notification.permission === 'granted';")
  private static native boolean notificationsGranted();

  @JSBody(params = { "title", "body" }, script = "new Notification(title, { body: body });")
  private static native void showNotification(String title, String body);

  @JSBody(script = "return !!(navigator.clipboard);")
  private static native boolean clipboardSupported();

  @JSBody(params = "text", script = "if (navigator.clipboard) { navigator.clipboard.writeText(text); }")
  private static native void writeClipboard(String text);

  @JSBody(params = "callback",
      script = "if (navigator.clipboard && navigator.clipboard.readText) {"
          + "  navigator.clipboard.readText().then(function(t) { callback.accept(t); }).catch(function() {});"
          + "}")
  private static native void readClipboard(TextCallback callback);

  @JSBody(script = "return !!(navigator.wakeLock);")
  private static native boolean wakeLockSupported();

  @JSBody(script = "if (navigator.wakeLock) {"
      + "  navigator.wakeLock.request('screen').then(function(s) { window.__flixelWakeLock = s; }).catch(function() {});"
      + "}")
  private static native void acquireWakeLock();

  @JSBody(
      script = "if (window.__flixelWakeLock) { window.__flixelWakeLock.release(); window.__flixelWakeLock = null; }")
  private static native void releaseWakeLock();

  @JSBody(params = "message",
      script = "window.__flixelExitMessage = message;"
          + "if (!window.__flixelBeforeUnload) {"
          + "  window.__flixelBeforeUnload = function(e) {"
          + "    if (window.__flixelExitMessage) { e.preventDefault(); e.returnValue = window.__flixelExitMessage; }"
          + "  };"
          + "  window.addEventListener('beforeunload', window.__flixelBeforeUnload);"
          + "}")
  private static native void setBeforeUnload(String message);

  @JSBody(script = "var original = document.title; var count = 0;"
      + "var id = setInterval(function() {"
      + "  document.title = (count % 2 === 0) ? '(!) ' + original : original; count++;"
      + "  if (count > 6 || document.hasFocus()) { clearInterval(id); document.title = original; }"
      + "}, 500);")
  private static native void flashTitle();

  /** Receives a resolved clipboard string from the browser's asynchronous read. */
  @JSFunctor
  private interface TextCallback extends JSObject {
    void accept(String text);
  }
}
