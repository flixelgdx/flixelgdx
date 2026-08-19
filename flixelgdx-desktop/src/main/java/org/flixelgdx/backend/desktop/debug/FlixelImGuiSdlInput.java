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
package org.flixelgdx.backend.desktop.debug;

import org.flixelgdx.input.FlixelKeyboardListener;
import org.flixelgdx.input.FlixelMouseListener;
import org.flixelgdx.input.keyboard.FlixelKey;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiKey;

/**
 * Feeds SDL3 input into Dear ImGui: the platform backend for the desktop debug overlay.
 *
 * <p>Dear ImGui does not read the keyboard or mouse itself; a platform backend has to push events
 * into its {@link ImGuiIO} each frame. imgui-java ships a GLFW backend, but this project runs on
 * SDL3, so this class bridges the two. Rather than parse SDL events a second time, it registers as a
 * {@link FlixelKeyboardListener} and {@link FlixelMouseListener} on the desktop input device and
 * receives the same translated events the game's input layer does. Printable characters arrive
 * through {@link #keyTyped(char)} (driven by SDL text-input events), which is what makes the debug
 * command line typeable.
 *
 * <p>All forwarding is gated on {@link #setActive(boolean)}. The overlay marks the backend active
 * only while the Dear ImGui context exists and the overlay is visible, so events are never pushed
 * into a destroyed context, and keys typed while the overlay is hidden do not queue up and flush into
 * a text field the next time it opens.
 */
public final class FlixelImGuiSdlInput implements FlixelKeyboardListener, FlixelMouseListener {

  private boolean active;
  private boolean ctrlLeft;
  private boolean ctrlRight;
  private boolean shiftLeft;
  private boolean shiftRight;
  private boolean altLeft;
  private boolean altRight;
  private boolean superDown;

  /**
   * Enables or disables event forwarding. When disabled, any queued Dear ImGui input is cleared so a
   * later frame does not replay stale keys or mouse state.
   *
   * @param active {@code true} while the Dear ImGui context exists and the overlay is visible.
   */
  public void setActive(boolean active) {
    if (this.active == active) {
      return;
    }
    this.active = active;
    if (!active) {
      clearInput();
    }
  }

  /**
   * Updates Dear ImGui's per-frame display size and delta time. Call once each frame, before
   * {@code ImGui.newFrame()}.
   *
   * @param width The current back buffer width in pixels.
   * @param height The current back buffer height in pixels.
   * @param deltaSeconds The real elapsed time since the previous frame, in seconds.
   */
  public void newFrame(int width, int height, float deltaSeconds) {
    ImGuiIO io = ImGui.getIO();
    io.setDisplaySize(width, height);
    io.setDisplayFramebufferScale(1f, 1f);
    io.setDeltaTime(deltaSeconds > 0f ? deltaSeconds : 1f / 60f);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (active) {
      int imKey = FlixelImGuiKeyMap.toImGuiKey(keycode);
      if (imKey != ImGuiKey.None) {
        ImGui.getIO().addKeyEvent(imKey, true);
      }
      if (updateModifierState(keycode, true)) {
        syncModifiers();
      }
    }
    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (active) {
      int imKey = FlixelImGuiKeyMap.toImGuiKey(keycode);
      if (imKey != ImGuiKey.None) {
        ImGui.getIO().addKeyEvent(imKey, false);
      }
      if (updateModifierState(keycode, false)) {
        syncModifiers();
      }
    }
    return false;
  }

  @Override
  public boolean keyTyped(char character) {
    // SDL text-input events only deliver actual text, never control keys, so every character here is
    // safe to hand straight to the focused Dear ImGui widget.
    if (active && character != 0) {
      ImGui.getIO().addInputCharacter(character);
    }
    return false;
  }

  @Override
  public boolean mouseDown(int button, int x, int y) {
    if (active) {
      ImGuiIO io = ImGui.getIO();
      io.addMousePosEvent(x, y);
      if (button >= 0 && button <= 4) {
        io.addMouseButtonEvent(button, true);
      }
    }
    return false;
  }

  @Override
  public boolean mouseUp(int button, int x, int y) {
    if (active) {
      ImGuiIO io = ImGui.getIO();
      io.addMousePosEvent(x, y);
      if (button >= 0 && button <= 4) {
        io.addMouseButtonEvent(button, false);
      }
    }
    return false;
  }

  @Override
  public boolean mouseMoved(int x, int y) {
    if (active) {
      ImGui.getIO().addMousePosEvent(x, y);
    }
    return false;
  }

  @Override
  public boolean mouseDragged(int x, int y) {
    if (active) {
      ImGui.getIO().addMousePosEvent(x, y);
    }
    return false;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    // SDL and Dear ImGui agree on wheel signs (positive y scrolls up, positive x scrolls right), so
    // the amounts pass through unchanged.
    if (active) {
      ImGui.getIO().addMouseWheelEvent(amountX, amountY);
    }
    return false;
  }

  /**
   * Clears Dear ImGui's queued keyboard and mouse input, resetting the tracked modifier state.
   *
   * <p>Only ever runs on an active-to-inactive transition, which the overlay always performs while
   * the Dear ImGui context is still alive (it deactivates before destroying the context), so the IO
   * calls here are safe.
   */
  private void clearInput() {
    ctrlLeft = false;
    ctrlRight = false;
    shiftLeft = false;
    shiftRight = false;
    altLeft = false;
    altRight = false;
    superDown = false;
    ImGuiIO io = ImGui.getIO();
    io.clearInputKeys();
    io.clearInputMouse();
    io.clearEventsQueue();
  }

  /**
   * Records a modifier key's pressed state.
   *
   * @param flixelKey The key that changed.
   * @param down Whether it was pressed ({@code true}) or released ({@code false}).
   * @return {@code true} if the key was a modifier, so the aggregate modifier flags should be resent.
   */
  private boolean updateModifierState(int flixelKey, boolean down) {
    if (flixelKey == FlixelKey.CONTROL_LEFT) {
      ctrlLeft = down;
    } else if (flixelKey == FlixelKey.CONTROL_RIGHT) {
      ctrlRight = down;
    } else if (flixelKey == FlixelKey.SHIFT_LEFT) {
      shiftLeft = down;
    } else if (flixelKey == FlixelKey.SHIFT_RIGHT) {
      shiftRight = down;
    } else if (flixelKey == FlixelKey.ALT_LEFT) {
      altLeft = down;
    } else if (flixelKey == FlixelKey.ALT_RIGHT) {
      altRight = down;
    } else if (flixelKey == FlixelKey.SYM) {
      superDown = down;
    } else {
      return false;
    }
    return true;
  }

  /** Resends the four aggregate modifier flags (Ctrl, Shift, Alt, Super) to Dear ImGui. */
  private void syncModifiers() {
    ImGuiIO io = ImGui.getIO();
    io.addKeyEvent(ImGuiKey.ModCtrl, ctrlLeft || ctrlRight);
    io.addKeyEvent(ImGuiKey.ModShift, shiftLeft || shiftRight);
    io.addKeyEvent(ImGuiKey.ModAlt, altLeft || altRight);
    io.addKeyEvent(ImGuiKey.ModSuper, superDown);
  }
}
