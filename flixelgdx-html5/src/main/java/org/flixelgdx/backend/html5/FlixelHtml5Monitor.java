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

import org.flixelgdx.backend.FlixelMonitor;
import org.jetbrains.annotations.NotNull;

/**
 * The HTML5 representation of a computer monitor, using provided information from the native
 * Window Management API.
 *
 * <p>Do not create objects of this class directly. It's advised you handle monitors through the
 * {@link FlixelHtml5HostIntegration} class instead, as monitor information is much more accurate
 * through that API.
 *
 * <p>Note that due to an API limitation and security restrictions, there's no way to obtain the
 * refresh rate of a monitor. Because of this, {@link #getRefreshRate()} returns {@code 0.0f}.
 */
class FlixelHtml5Monitor implements FlixelMonitor {

  @NotNull
  final String name;

  final int virtualX;
  final int virtualY;
  final int width;
  final int height;
  final boolean isPrimary;

  FlixelHtml5Monitor(@NotNull String name, int virtualX, int virtualY, int width, int height, boolean isPrimary) {
    this.name = name;
    this.virtualX = virtualX;
    this.virtualY = virtualY;
    this.width = width;
    this.height = height;
    this.isPrimary = isPrimary;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public int getVirtualX() {
    return virtualX;
  }

  @Override
  public int getVirtualY() {
    return virtualY;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public float getRefreshRate() {
    return 0.0f;
  }

  @Override
  public boolean isPrimary() {
    return isPrimary;
  }
}
