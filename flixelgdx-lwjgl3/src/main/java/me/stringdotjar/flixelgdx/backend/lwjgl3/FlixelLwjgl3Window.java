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
 *
 * <p>Window position updates use a single logical {@code (x, y)} pair and at most one {@code postRunnable} per frame batch so
 * separate {@link #setX(int)} and {@link #setY(int)} calls (for example from independent tween goals) still produce one consistent
 * {@code glfwSetWindowPos} snapshot without stale axis readbacks.
 */
public final class FlixelLwjgl3Window implements FlixelWindow {

  @Nullable
  private static volatile FlixelLwjgl3ChainingWindowListener closeHook;

  private int logicalX, logicalY;

  /** {@code false} until the first mutation seeds logical coordinates from the live window position. */
  private boolean logicalPositionInitialized;

  /**
   * When {@code true}, a runnable is already queued to apply {@link #logicalX} and {@link #logicalY}.
   */
  private boolean positionFlushPosted;

  static void configureCloseHandlingHook(@Nullable FlixelLwjgl3ChainingWindowListener hook) {
    closeHook = hook;
  }

  /**
   * Copies the OS window position into the logical pair whenever this is not the middle of a pending flush burst, so manual
   * drags stay visible before the next driven move.
   */
  private void prepareLogicalForMutation(Lwjgl3Graphics graphics) {
    if (!logicalPositionInitialized || !positionFlushPosted) {
      logicalX = graphics.getWindow().getPositionX();
      logicalY = graphics.getWindow().getPositionY();
      logicalPositionInitialized = true;
    }
  }

  /**
   * Queues exactly one GLFW position apply for this frame burst. Repeated calls mutate {@link #logicalX} /
   * {@link #logicalY} only until the runnable runs.
   *
   * @param graphics Backing LWJGL3 graphics handle.
   */
  private void scheduleMergedPositionFlush(Lwjgl3Graphics graphics) {
    if (positionFlushPosted) {
      return;
    }
    positionFlushPosted = true;
    graphics.getWindow().postRunnable(() -> {
      positionFlushPosted = false;
      if (!(Gdx.graphics instanceof Lwjgl3Graphics latest)) {
        return;
      }
      latest.getWindow().setPosition(logicalX, logicalY);
    });
  }

  /**
   * @param graphics Backend handle.
   * @return Preferred X for {@link #getX()} while a flush is still pending ({@linkplain #logicalX logical coordinates} stay aligned
   * with pending writes).
   */
  private int readPositionX(Lwjgl3Graphics graphics) {
    if (logicalPositionInitialized && positionFlushPosted) {
      return logicalX;
    }
    return graphics.getWindow().getPositionX();
  }

  /**
   * @param graphics Backend handle.
   * @return Preferred Y for {@link #getY()} while a flush is still pending.
   */
  private int readPositionY(Lwjgl3Graphics graphics) {
    if (logicalPositionInitialized && positionFlushPosted) {
      return logicalY;
    }
    return graphics.getWindow().getPositionY();
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
    return readPositionX(g);
  }

  @Override
  public int getY() {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return 0;
    }
    return readPositionY(g);
  }

  @Override
  public void setX(int x) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    prepareLogicalForMutation(g);
    logicalX = x;
    scheduleMergedPositionFlush(g);
  }

  @Override
  public void setY(int y) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    prepareLogicalForMutation(g);
    logicalY = y;
    scheduleMergedPositionFlush(g);
  }

  @Override
  public void setPosition(int x, int y) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    prepareLogicalForMutation(g);
    logicalX = x;
    logicalY = y;
    scheduleMergedPositionFlush(g);
  }

  @Override
  public void changeX(int deltaX) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    prepareLogicalForMutation(g);
    logicalX += deltaX;
    scheduleMergedPositionFlush(g);
  }

  @Override
  public void changeY(int deltaY) {
    if (!(Gdx.graphics instanceof Lwjgl3Graphics g)) {
      return;
    }
    prepareLogicalForMutation(g);
    logicalY += deltaY;
    scheduleMergedPositionFlush(g);
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
