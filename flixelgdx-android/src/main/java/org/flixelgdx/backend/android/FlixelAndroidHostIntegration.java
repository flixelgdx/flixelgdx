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

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelMonitor;
import org.flixelgdx.backend.FlixelNoopMonitor;
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Android host integration that reports {@link FlixelPlatform#Android}.
 *
 * <p>Notifications and clipboard are not wired in this initial implementation; only the platform
 * identity and no-op signal are provided. Full clipboard and notification support can be layered
 * on later without changing the launcher.
 */
public class FlixelAndroidHostIntegration implements FlixelHostIntegration {

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();
  private final FlixelArray<FlixelMonitor> monitors = new FlixelArray<>(FlixelMonitor[]::new);

  @Override
  @NotNull
  public FlixelPlatform getPlatform() {
    return FlixelPlatform.Android;
  }

  @Override
  public boolean supportsNotifications() {
    return false;
  }

  @Override
  public boolean supportsClipboard() {
    return false;
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
  @NotNull
  public FlixelMonitor getPrimaryMonitor() {
    return FlixelNoopMonitor.INSTANCE;
  }

  @Override
  @NotNull
  public FlixelList<FlixelMonitor> getMonitors() {
    return monitors;
  }
}
