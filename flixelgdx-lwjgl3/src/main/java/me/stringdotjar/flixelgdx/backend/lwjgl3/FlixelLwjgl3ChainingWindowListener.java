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
package me.stringdotjar.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;

import org.jetbrains.annotations.NotNull;

/**
 * Outermost GLFW listener that can veto close requests when absorption is enabled.
 */
public final class FlixelLwjgl3ChainingWindowListener implements Lwjgl3WindowListener {

  @NotNull
  private final Lwjgl3WindowListener delegate;

  private volatile boolean absorbCloseRequests;

  public FlixelLwjgl3ChainingWindowListener(@NotNull Lwjgl3WindowListener delegate) {
    this.delegate = delegate;
  }

  void setAbsorbCloseRequests(boolean absorbCloseRequests) {
    this.absorbCloseRequests = absorbCloseRequests;
  }

  boolean isAbsorbCloseRequests() {
    return absorbCloseRequests;
  }

  @NotNull
  public Lwjgl3WindowListener getDelegate() {
    return delegate;
  }

  @Override
  public void created(Lwjgl3Window window) {
    delegate.created(window);
  }

  @Override
  public void iconified(boolean isIconified) {
    delegate.iconified(isIconified);
  }

  @Override
  public void maximized(boolean isMaximized) {
    delegate.maximized(isMaximized);
  }

  @Override
  public void focusLost() {
    delegate.focusLost();
  }

  @Override
  public void focusGained() {
    delegate.focusGained();
  }

  @Override
  public boolean closeRequested() {
    if (absorbCloseRequests) {
      return false;
    }
    return delegate.closeRequested();
  }

  @Override
  public void filesDropped(String[] files) {
    delegate.filesDropped(files);
  }

  @Override
  public void refreshRequested() {
    delegate.refreshRequested();
  }
}
