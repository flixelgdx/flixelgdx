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
package org.flixelgdx.input.touch;

/**
 * State snapshot for a single touch pointer (one finger).
 *
 * <p>Instances are pre-allocated by {@link FlixelTouchManager} and reused across frames; do not
 * create them yourself. Access the array via {@link FlixelTouchManager#list}:
 *
 * <pre>{@code
 * FlixelTouch first = Flixel.touches.list[0];
 * if (first.isJustPressed()) {
 *   spawnEffect(first.worldX, first.worldY);
 * }
 * }</pre>
 *
 * <p>Screen coordinates use libGDX's top-left origin (Y increases downward). World coordinates are
 * unprojected via the touch manager's active camera and use the standard bottom-left origin
 * (Y increases upward), matching the rest of the scene.
 *
 * <p>State fields ({@link #screenX}, {@link #screenY}, {@link #worldX}, {@link #worldY},
 * {@link #pointer}) are public for zero-overhead reads. The boolean state is exposed through
 * getters to prevent accidental modification by game code; the manager is the only writer.
 *
 * @see FlixelTouchManager
 */
public final class FlixelTouch {

  /** World X coordinate of this pointer, unprojected from screen space via the active camera. */
  public float worldX;

  /** World Y coordinate of this pointer, unprojected from screen space via the active camera. */
  public float worldY;

  /** Screen X coordinate in pixels, top-left origin, matching libGDX conventions. */
  public int screenX;

  /** Screen Y coordinate in pixels, top-left origin, matching libGDX conventions. */
  public int screenY;

  /** Zero-based pointer index identifying this finger (0 = first touch, 1 = second, etc.). */
  public int pointer;

  boolean pressed;
  boolean justPressed;
  boolean justReleased;
  boolean dragging;
  boolean justCancelled;

  FlixelTouch() {}

  /** Returns {@code true} while this pointer is currently in contact with the screen. */
  public boolean isPressed() {
    return pressed;
  }

  /**
   * Returns {@code true} on the single frame this pointer first touched the screen. Clears to
   * {@code false} after {@link FlixelTouchManager#endFrame()} is called.
   */
  public boolean isJustPressed() {
    return justPressed;
  }

  /**
   * Returns {@code true} on the single frame this pointer lifted off the screen. Clears to
   * {@code false} after {@link FlixelTouchManager#endFrame()} is called.
   */
  public boolean isJustReleased() {
    return justReleased;
  }

  /**
   * Returns {@code true} once this pointer has moved after being pressed, and stays {@code true}
   * until the pointer is released. Use this to distinguish a tap from a drag gesture.
   */
  public boolean isDragging() {
    return dragging;
  }

  /**
   * Returns {@code true} on the single frame this pointer was cancelled by the system (for
   * example, an incoming phone call interrupting the touch session). Clears to {@code false} after
   * {@link FlixelTouchManager#endFrame()} is called.
   */
  public boolean isJustCancelled() {
    return justCancelled;
  }
}
