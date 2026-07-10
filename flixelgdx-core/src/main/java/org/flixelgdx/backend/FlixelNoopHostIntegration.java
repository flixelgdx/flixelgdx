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
package org.flixelgdx.backend;

import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Default {@link FlixelHostIntegration} used on platforms without host shell integration.
 *
 * <p>All operations are no-ops. Capability checks return {@code false}. Signals are live instances
 * that never dispatch on their own, but callers may still add handlers to them safely.
 */
public enum FlixelNoopHostIntegration implements FlixelHostIntegration {

  /** Shared no-op instance. */
  INSTANCE;

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();
  private final FlixelSignal<FlixelGraphic> onImagePasted = new FlixelSignal<>();

  @Override
  public void requestNotificationPermission() {}

  @Override
  public void requestAttention() {}

  @Override
  public void keepScreenAwake(boolean awake) {}

  @Override
  public void setExitConfirmation(@Nullable String message) {}

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message");
  }

  @Override
  public void copyToClipboard(@NotNull String text) {
    Objects.requireNonNull(text, "text");
  }

  @Override
  public void copyImageToClipboard(@NotNull FlixelGraphic graphic) {
    Objects.requireNonNull(graphic, "graphic");
  }

  @Override
  public void pasteFromClipboard() {}

  @Override
  public void pasteImageFromClipboard() {}

  @Override
  public boolean supportsDesktopNotification() {
    return false;
  }

  @Override
  public boolean supportsWakeLock() {
    return false;
  }

  @Override
  public boolean supportsClipboard() {
    return false;
  }

  @Override
  public boolean supportsImageClipboard() {
    return false;
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
}
