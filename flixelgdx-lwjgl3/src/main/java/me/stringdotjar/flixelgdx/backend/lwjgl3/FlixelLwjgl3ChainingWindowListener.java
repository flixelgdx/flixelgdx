/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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
