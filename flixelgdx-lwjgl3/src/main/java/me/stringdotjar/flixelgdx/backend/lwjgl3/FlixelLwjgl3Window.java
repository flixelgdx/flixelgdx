/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.math.MathUtils;

import me.stringdotjar.flixelgdx.backend.window.FlixelWindow;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * GLFW-backed {@link FlixelWindow} for the LWJGL3 backend.
 *
 * <p>Desktop see-through uses {@link me.stringdotjar.flixelgdx.FlixelGame#applyBackdropForDesktopTransparency(boolean)} from the
 * default {@link FlixelWindow#setDesktopTransparencyActive(boolean)} implementation. Do not call
 * {@code glfwSetWindowAttrib(GLFW_TRANSPARENT_FRAMEBUFFER, ...)}: GLFW reports {@code GLFW_INVALID_ENUM} and it is not a supported
 * dynamic attribute on common platforms.
 */
public final class FlixelLwjgl3Window implements FlixelWindow {

  @Nullable
  private static volatile FlixelLwjgl3ChainingWindowListener closeHook;

  static void configureCloseHandlingHook(@Nullable FlixelLwjgl3ChainingWindowListener hook) {
    closeHook = hook;
  }

  @Override
  public void setWindowOpacity(float opacity) {
    if (!supportsWindowOpacity() || !Float.isFinite(opacity)) {
      return;
    }
    float o = MathUtils.clamp(opacity, 0f, 1f);
    Lwjgl3Graphics g = (Lwjgl3Graphics) Gdx.graphics;
    g.getWindow().postRunnable(() -> GLFW.glfwSetWindowOpacity(g.getWindow().getWindowHandle(), o));
  }

  @Override
  public boolean supportsWindowOpacity() {
    return Gdx.graphics instanceof Lwjgl3Graphics;
  }

  @Override
  public void setWindowDecorated(boolean decorated) {
    if (!supportsSetWindowDecorated()) {
      return;
    }
    Lwjgl3Graphics g = (Lwjgl3Graphics) Gdx.graphics;
    g.getWindow().postRunnable(() -> g.setUndecorated(!decorated));
  }

  @Override
  public boolean supportsSetWindowDecorated() {
    return Gdx.graphics instanceof Lwjgl3Graphics;
  }

  @Override
  public int getX() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return 0;
    }
    return g.getWindow().getPositionX();
  }

  @Override
  public int getY() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return 0;
    }
    return g.getWindow().getPositionY();
  }

  @Override
  public void setPosition(int x, int y) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    g.getWindow().postRunnable(() -> g.getWindow().setPosition(x, y));
  }

  @Override
  public void changeX(int deltaX) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    g.getWindow().postRunnable(() -> {
      int nx = g.getWindow().getPositionX() + deltaX;
      int ny = g.getWindow().getPositionY();
      g.getWindow().setPosition(nx, ny);
    });
  }

  @Override
  public void changeY(int deltaY) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    g.getWindow().postRunnable(() -> {
      int nx = g.getWindow().getPositionX();
      int ny = g.getWindow().getPositionY() + deltaY;
      g.getWindow().setPosition(nx, ny);
    });
  }

  @Override
  public void bringWindowToForeground() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    g.getWindow().postRunnable(() -> g.getWindow().focusWindow());
  }

  @Override
  public boolean supportsBringWindowToForeground() {
    return Gdx.graphics instanceof Lwjgl3Graphics;
  }

  @Override
  public void setWindowFloating(boolean floating) {
    if (!supportsSetWindowFloating()) {
      return;
    }
    Lwjgl3Graphics g = (Lwjgl3Graphics) Gdx.graphics;
    long handle = g.getWindow().getWindowHandle();
    g.getWindow().postRunnable(() -> GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, floating ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE));
  }

  @Override
  public boolean supportsSetWindowFloating() {
    return Gdx.graphics instanceof Lwjgl3Graphics;
  }

  @Override
  public void setAbsorbCloseRequests(boolean absorb) {
    FlixelLwjgl3ChainingWindowListener hook = closeHook;
    if (hook != null) {
      hook.setAbsorbCloseRequests(absorb);
    }
  }

  @Override
  public boolean supportsAbsorbCloseRequests() {
    return closeHook != null;
  }
}
