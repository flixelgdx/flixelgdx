/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;

import me.stringdotjar.flixelgdx.Flixel;

import org.jetbrains.annotations.Nullable;

/**
 * Runs Flixel window lifecycle hooks, then forwards to an optional user {@link Lwjgl3WindowListener}.
 */
final class FlixelLwjgl3NotifyWindowListener implements Lwjgl3WindowListener {

  @Nullable
  private final Lwjgl3WindowListener next;

  FlixelLwjgl3NotifyWindowListener(@Nullable Lwjgl3WindowListener next) {
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
    Flixel.getGame().onWindowMinimized(isIconified);
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
    if (!Flixel.getGame().isMinimized()) {
      Flixel.getGame().onWindowUnfocused();
      if (next != null) {
        next.focusLost();
      }
    }
  }

  @Override
  public void focusGained() {
    Flixel.getGame().onWindowFocused();
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
