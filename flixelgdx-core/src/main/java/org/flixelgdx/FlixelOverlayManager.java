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
package org.flixelgdx;

import org.flixelgdx.functional.FlixelAntialiasable;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.group.FlixelBasicGroup;
import org.jetbrains.annotations.NotNull;

/**
 * The manager for the global overlay: a group drawn on top of every game camera that stays on
 * screen across state switches.
 *
 * <p>Think of it like a sticker on the TV screen instead of a prop on the stage: the state (the
 * stage) is swapped out on every switch, but the sticker stays where it is. That suits an FPS
 * counter, a "now playing" banner, or a watermark. The overlay draws with the overlay camera of
 * {@link Flixel#cameraManager}, whose scroll is always zero, so a member placed at {@code (x, y)}
 * always appears at those design-resolution coordinates. It draws after the global shaders, so
 * shaders never affect it.
 *
 * <p>It is registered in {@link Flixel#managers} at {@link FlixelManagers.Manager#ORDER_OVERLAY}
 * and reached through {@link Flixel#overlay}. The overlay is off until you turn it on, and while it
 * is off it is neither updated nor drawn.
 *
 * <p>Example:
 * <pre>{@code
 * FlixelText fps = new FlixelText(4f, 4f, 0f, "", 8);
 * Flixel.overlay.add(fps);       // Add to the overlay, not the state.
 * Flixel.overlay.setEnabled(true);
 * }</pre>
 */
public class FlixelOverlayManager implements FlixelManagers.Manager {

  /** The members of the overlay. Replaced with an empty group when the game closes. */
  @NotNull
  private FlixelBasicGroup<IFlixelBasic> group = newGroup();

  /** Whether the overlay is updated and drawn. */
  private boolean enabled;

  /**
   * Adds a member to the overlay, so it is updated and drawn on top of every game camera while the
   * overlay is on. This works at any time, even before the game starts or while the overlay is off.
   *
   * @param basic The object to add.
   */
  public void add(@NotNull IFlixelBasic basic) {
    group.add(basic);
    if (basic instanceof FlixelAntialiasable antialiasable && Flixel.applyAntialiasingOnStateAdd) {
      antialiasable.setAntialiasing(Flixel.isAntialiasing());
    }
  }

  /**
   * Removes a member from the overlay.
   *
   * @param basic The object to remove.
   */
  public void remove(@NotNull IFlixelBasic basic) {
    group.remove(basic);
  }

  /**
   * Turns the overlay on if it is off, and off if it is on.
   *
   * @return Whether the overlay is on after toggling.
   */
  public boolean toggle() {
    enabled = !enabled;
    return enabled;
  }

  /**
   * Updates the overlay's members after the cameras, while the overlay is on.
   *
   * @param elapsed The scaled seconds since the last frame.
   */
  @Override
  public void postUpdate(float elapsed) {
    if (enabled) {
      group.update(elapsed);
    }
  }

  /**
   * Draws the overlay's members on top of every game camera, while the overlay is on.
   *
   * @param batch The framework batch, already begun with the overlay camera.
   */
  @Override
  public void drawOverlay(@NotNull FlixelBatch batch) {
    if (enabled) {
      group.draw(batch);
    }
  }

  /** Destroys every member and turns the overlay off when the game closes. */
  @Override
  public void destroy() {
    group.destroy();
    group = newGroup();
    enabled = false;
  }

  @NotNull
  private static FlixelBasicGroup<IFlixelBasic> newGroup() {
    return new FlixelBasicGroup<>(IFlixelBasic[]::new) {
    };
  }

  @Override
  public int getOrder() {
    return ORDER_OVERLAY;
  }

  @NotNull
  @Override
  public String getName() {
    return "flixel.overlay";
  }

  /**
   * Returns the group that holds every member of the overlay.
   *
   * @return The overlay group.
   */
  @NotNull
  public FlixelBasicGroup<IFlixelBasic> getGroup() {
    return group;
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Turns the overlay on or off. While it is off, it is neither updated nor drawn, so it costs
   * nothing per frame.
   *
   * @param enabled Whether the overlay should be on.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
