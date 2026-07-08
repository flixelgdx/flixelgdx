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
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * GLFW window listener that drives Flixel window lifecycle hooks and optionally forwards
 * events to a user-supplied {@link Lwjgl3WindowListener}.
 *
 * <p>This listener handles three responsibilities in one place:
 * <ul>
 *   <li>Routing GLFW focus and minimize events to {@link FlixelGame#onFocusLost()},
 *       {@link FlixelGame#onFocusGained()}, and {@link FlixelGame#onMinimized()}.</li>
 *   <li>Vetoing GLFW close requests when {@link #setAbsorbCloseRequests(boolean)} is
 *       {@code true}.</li>
 *   <li>Forwarding all events to the optional user listener passed at construction.</li>
 * </ul>
 */
public class FlixelLwjgl3WindowListener implements Lwjgl3WindowListener {

  private static final Array<Runnable> focusLostHooks = new Array<>(4);
  private static final Array<Runnable> focusGainedHooks = new Array<>(4);

  @Nullable
  private final Lwjgl3WindowListener next;

  private volatile boolean absorbCloseRequests;

  public FlixelLwjgl3WindowListener(@Nullable Lwjgl3WindowListener next) {
    this.next = next;
  }

  /**
   * Registers a pair of callbacks that fire on every focus-gained and focus-lost event,
   * after the built-in Flixel lifecycle hooks run.
   *
   * <p>Intended for backend modules (such as the LWJGL3/VLC video module) that need reliable
   * focus notifications without relying on {@link Flixel.Signals}, which
   * can be cleared by developer code.
   *
   * @param onFocusGained Called when the window regains focus.
   * @param onFocusLost Called when the window loses focus.
   */
  public static void addFocusHooks(@NotNull Runnable onFocusGained, @NotNull Runnable onFocusLost) {
    focusGainedHooks.add(onFocusGained);
    focusLostHooks.add(onFocusLost);
  }

  void setAbsorbCloseRequests(boolean absorb) {
    this.absorbCloseRequests = absorb;
  }

  boolean isAbsorbCloseRequests() {
    return absorbCloseRequests;
  }

  @Override
  public void created(Lwjgl3Window window) {
    if (next != null) {
      next.created(window);
    }
  }

  @Override
  public void iconified(boolean isIconified) {
    if (isIconified) {
      FlixelGame game = Flixel.getGame();
      if (game != null) {
        game.onMinimized();
      }
    }
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
    FlixelGame game = Flixel.game;
    if (game != null) {
      game.onFocusLost();
    }
    for (int i = 0, n = focusLostHooks.size; i < n; i++) {
      focusLostHooks.get(i).run();
    }
    if (next != null) {
      next.focusLost();
    }
  }

  @Override
  public void focusGained() {
    FlixelGame game = Flixel.game;
    if (game != null) {
      game.onFocusGained();
    }
    for (int i = 0, n = focusGainedHooks.size; i < n; i++) {
      focusGainedHooks.get(i).run();
    }
    if (next != null) {
      next.focusGained();
    }
  }

  @Override
  public boolean closeRequested() {
    if (absorbCloseRequests) {
      return false;
    }
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
