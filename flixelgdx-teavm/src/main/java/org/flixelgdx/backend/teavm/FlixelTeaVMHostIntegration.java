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
package org.flixelgdx.backend.teavm;

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.Objects;

/**
 * Web (TeaVM) {@link FlixelHostIntegration}: Browser Notification API for toasts, tab-title
 * flashing for attention, Screen Wake Lock API, {@code beforeunload} exit guard, and the
 * Clipboard API for text access.
 */
final class FlixelTeaVMHostIntegration implements FlixelHostIntegration {

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();

  @Override
  public void requestNotificationPermission() {
    jsRequestNotificationPermission();
  }

  @Override
  public void requestAttention() {
    jsRequestAttention();
  }

  @Override
  public void keepScreenAwake(boolean awake) {
    if (!supportsWakeLock()) {
      return;
    }
    if (awake) {
      jsAcquireWakeLock();
    } else {
      jsReleaseWakeLock();
    }
  }

  @Override
  public void setExitConfirmation(@Nullable String message) {
    if (message == null) {
      jsClearExitConfirmation();
    } else {
      jsSetExitConfirmation(message);
    }
  }

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message");
    if (!supportsDesktopNotification()) {
      return;
    }
    jsShowNotification(title == null ? "" : title, message);
  }

  @Override
  public void copyToClipboard(@NotNull String text) {
    Objects.requireNonNull(text, "text");
    if (!supportsClipboard()) {
      return;
    }
    jsCopyText(text);
  }

  @Override
  public void pasteFromClipboard() {
    if (!supportsClipboard()) {
      return;
    }
    jsReadText(text -> {
      if (text != null && !text.isEmpty()) {
        onTextPasted.dispatch(text);
      }
    });
  }

  @Override
  public boolean supportsDesktopNotification() {
    return jsNotificationGranted();
  }

  @Override
  public boolean supportsWakeLock() {
    return jsSupportsWakeLock();
  }

  @Override
  public boolean supportsClipboard() {
    return jsSupportsClipboard();
  }

  @Override
  @NotNull
  public FlixelSignal<String> onTextPasted() {
    return onTextPasted;
  }

  @JSBody(script = "if (typeof Notification !== 'undefined' && Notification.permission !== 'granted') {"
      + "Notification.requestPermission();}")
  private static native void jsRequestNotificationPermission();

  @JSBody(script = "var orig = document.title;"
      + "if (!document.hidden) return;"
      + "var on = false;"
      + "var id = setInterval(function() {"
      + "  on = !on;"
      + "  document.title = on ? '* ' + orig : orig;"
      + "}, 800);"
      + "function stop() {"
      + "  if (!document.hidden) {"
      + "    clearInterval(id);"
      + "    document.title = orig;"
      + "    document.removeEventListener('visibilitychange', stop);"
      + "  }"
      + "}"
      + "document.addEventListener('visibilitychange', stop);")
  private static native void jsRequestAttention();

  @JSBody(script = "if (!navigator.wakeLock) return;"
      + "navigator.wakeLock.request('screen').then(function(s){ window.__flxWakeLock = s; }).catch(function(){});")
  private static native void jsAcquireWakeLock();

  @JSBody(script = "if (window.__flxWakeLock){"
      + "window.__flxWakeLock.release();"
      + "window.__flxWakeLock = null;}")
  private static native void jsReleaseWakeLock();

  @JSBody(params = "msg", script = "window.onbeforeunload = function(e){"
      + "e.preventDefault();"
      + "e.returnValue = msg;"
      + "return msg;"
      + "};")
  private static native void jsSetExitConfirmation(String msg);

  @JSBody(script = "window.onbeforeunload = null;")
  private static native void jsClearExitConfirmation();

  @JSBody(params = { "title", "body" },
      script = "if (typeof Notification !== 'undefined' && Notification.permission === 'granted'){"
          + "new Notification(title, {body: body});}")
  private static native void jsShowNotification(String title, String body);

  @JSBody(params = "text",
      script = "if (navigator.clipboard && navigator.clipboard.writeText){"
          + "navigator.clipboard.writeText(text).catch(function(){});}")
  private static native void jsCopyText(String text);

  @JSBody(params = "callback",
      script = "if (!navigator.clipboard || !navigator.clipboard.readText) return;"
          + "navigator.clipboard.readText().then(function(t){ callback(t); }).catch(function(){});")
  private static native void jsReadText(StringPasteCallback callback);

  @JSBody(script = "return typeof Notification !== 'undefined' && Notification.permission === 'granted';")
  private static native boolean jsNotificationGranted();

  @JSBody(script = "return 'wakeLock' in navigator;")
  private static native boolean jsSupportsWakeLock();

  @JSBody(script = "return !!(navigator.clipboard && navigator.clipboard.readText);")
  private static native boolean jsSupportsClipboard();

  @JSFunctor
  interface StringPasteCallback extends JSObject {
    void call(String text);
  }
}
