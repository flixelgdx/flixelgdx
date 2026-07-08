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
package org.flixelgdx.backend.lwjgl3.window;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;

import org.jetbrains.annotations.Nullable;

/**
 * Forwards window lifecycle events to an optional user {@link Lwjgl3WindowListener}.
 */
public class FlixelLwjgl3NotifyWindowListener implements Lwjgl3WindowListener {

  @Nullable
  private final Lwjgl3WindowListener next;

  public FlixelLwjgl3NotifyWindowListener(@Nullable Lwjgl3WindowListener next) {
    this.next = next;
  }

  @Override
  public void created(Lwjgl3Window window) {
    if (next != null) {
      next.created(window);
    }
  }

  @Override
  public void iconified(boolean isIconified) {
    if (next != null) {
      next.iconified(isIconified);
    }
  }

  @Override
  public void maximized(boolean isMaximized) {
    if (next != null) {
      next.maximized(isMaximized);
    }
  }

  @Override
  public void focusLost() {
    if (next != null) {
      next.focusLost();
    }
  }

  @Override
  public void focusGained() {
    if (next != null) {
      next.focusGained();
    }
  }

  @Override
  public boolean closeRequested() {
    return next == null || next.closeRequested();
  }

  @Override
  public void filesDropped(String[] files) {
    if (next != null) {
      next.filesDropped(files);
    }
  }

  @Override
  public void refreshRequested() {
    if (next != null) {
      next.refreshRequested();
    }
  }
}
