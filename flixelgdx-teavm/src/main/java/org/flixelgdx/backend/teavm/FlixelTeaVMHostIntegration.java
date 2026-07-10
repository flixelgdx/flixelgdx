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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int8Array;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Web (TeaVM) {@link FlixelHostIntegration}: Browser Notification API for toasts, tab-title
 * flashing for attention, Screen Wake Lock API, {@code beforeunload} exit guard, and the
 * Clipboard API for both text and image access.
 */
final class FlixelTeaVMHostIntegration implements FlixelHostIntegration {

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();
  private final FlixelSignal<FlixelGraphic> onImagePasted = new FlixelSignal<>();

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
  public void copyImageToClipboard(@NotNull FlixelGraphic graphic) {
    Objects.requireNonNull(graphic, "graphic");
    if (!supportsImageClipboard()) {
      return;
    }
    Texture texture = graphic.getTexture();
    int w = texture.getWidth();
    int h = texture.getHeight();
    Pixmap pixmap = readTexturePixels(texture, w, h);
    try {
      ByteBuffer buf = pixmap.getPixels();
      buf.rewind();
      byte[] bytes = new byte[w * h * 4];
      buf.get(bytes);
      // glReadPixels returns rows bottom-to-top; jsCopyImageRgba flips them for canvas.
      jsCopyImageRgba(Int8Array.fromJavaArray(bytes), w, h);
    } finally {
      pixmap.dispose();
    }
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
  public void pasteImageFromClipboard() {
    if (!supportsImageClipboard()) {
      return;
    }
    jsReadImage((data, width, height) -> Gdx.app.postRunnable(() -> {
      byte[] bytes = data.copyToJavaArray();
      Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
      ByteBuffer buf = pixmap.getPixels();
      buf.rewind();
      buf.put(bytes);
      buf.flip();
      Texture texture = new Texture(pixmap);
      pixmap.dispose();
      FlixelAssetManager assets = Flixel.ensureAssets();
      FlixelGraphic graphic = new FlixelGraphic(assets, assets.allocateSyntheticKey(), texture);
      assets.register(graphic);
      onImagePasted.dispatch(graphic);
    }));
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
  public boolean supportsImageClipboard() {
    return jsSupportsImageClipboard();
  }

  @Override
  @NotNull
  public FlixelSignal<String> onTextPasted() {
    return onTextPasted;
  }

  @Override
  @NotNull
  public FlixelSignal<FlixelGraphic> onImagePasted() {
    return onImagePasted;
  }

  private static Pixmap readTexturePixels(Texture texture, int w, int h) {
    FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
    SpriteBatch batch = new SpriteBatch();
    Matrix4 proj = new Matrix4().setToOrtho2D(0, 0, w, h);
    batch.setProjectionMatrix(proj);
    fbo.begin();
    try {
      batch.begin();
      batch.draw(texture, 0, 0, w, h);
      batch.end();
      return Pixmap.createFromFrameBuffer(0, 0, w, h);
    } finally {
      fbo.end();
      fbo.dispose();
      batch.dispose();
    }
  }

  @JSBody(script = "if (typeof Notification !== 'undefined' && Notification.permission !== 'granted') {"
      + "Notification.requestPermission();}")
  private static native void jsRequestNotificationPermission();

  @JSBody(script = "var orig = document.title;"
      + "if (!document.hidden) return;"
      + "var on = false;"
      + "var id = setInterval(function(){"
      + "  on = !on;"
      + "  document.title = on ? '* ' + orig : orig;"
      + "}, 800);"
      + "function stop(){"
      + "  if (!document.hidden){"
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

  @JSBody(params = { "arr", "width", "height" },
      script = "if (!navigator.clipboard || !navigator.clipboard.write || typeof ClipboardItem === 'undefined') return;"
          + "var src = new Uint8ClampedArray(arr.buffer);"
          + "var flipped = new Uint8ClampedArray(src.length);"
          + "var row = width * 4;"
          + "for (var y = 0; y < height; y++){"
          + "  var s = (height - 1 - y) * row, d = y * row;"
          + "  flipped.set(src.subarray(s, s + row), d);"
          + "}"
          + "var imgData = new ImageData(flipped, width, height);"
          + "var c = document.createElement('canvas');"
          + "c.width = width; c.height = height;"
          + "c.getContext('2d').putImageData(imgData, 0, 0);"
          + "c.toBlob(function(blob){"
          + "  navigator.clipboard.write([new ClipboardItem({'image/png': blob})]).catch(function(){});"
          + "}, 'image/png');")
  private static native void jsCopyImageRgba(Int8Array arr, int width, int height);

  @JSBody(params = "callback",
      script = "if (!navigator.clipboard || !navigator.clipboard.readText) return;"
          + "navigator.clipboard.readText().then(function(t){ callback(t); }).catch(function(){});")
  private static native void jsReadText(StringPasteCallback callback);

  @JSBody(params = "callback",
      script = "if (!navigator.clipboard || !navigator.clipboard.read || typeof ClipboardItem === 'undefined') return;"
          + "navigator.clipboard.read().then(function(items){"
          + "  for (var i = 0; i < items.length; i++){"
          + "    if (items[i].types.indexOf('image/png') !== -1){"
          + "      items[i].getType('image/png').then(function(blob){"
          + "        var url = URL.createObjectURL(blob);"
          + "        var img = new Image();"
          + "        img.onload = function(){"
          + "          var c = document.createElement('canvas');"
          + "          c.width = img.width; c.height = img.height;"
          + "          var ctx = c.getContext('2d');"
          + "          ctx.drawImage(img, 0, 0);"
          + "          var d = ctx.getImageData(0, 0, img.width, img.height).data;"
          + "          URL.revokeObjectURL(url);"
          + "          callback(new Int8Array(d.buffer), img.width, img.height);"
          + "        };"
          + "        img.onerror = function(){ URL.revokeObjectURL(url); };"
          + "        img.src = url;"
          + "      });"
          + "      break;"
          + "    }"
          + "  }"
          + "}).catch(function(){});")
  private static native void jsReadImage(ImagePasteCallback callback);

  @JSBody(script = "return typeof Notification !== 'undefined' && Notification.permission === 'granted';")
  private static native boolean jsNotificationGranted();

  @JSBody(script = "return 'wakeLock' in navigator;")
  private static native boolean jsSupportsWakeLock();

  @JSBody(script = "return !!(navigator.clipboard && navigator.clipboard.readText);")
  private static native boolean jsSupportsClipboard();

  @JSBody(
      script = "return !!(navigator.clipboard && navigator.clipboard.read && typeof ClipboardItem !== 'undefined');")
  private static native boolean jsSupportsImageClipboard();

  @JSFunctor
  interface StringPasteCallback extends JSObject {
    void call(String text);
  }

  @JSFunctor
  interface ImagePasteCallback extends JSObject {
    void call(Int8Array data, int width, int height);
  }
}
