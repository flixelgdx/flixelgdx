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
package org.flixelgdx.graphics;

import org.flixelgdx.Flixel;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelRect;
import org.flixelgdx.math.FlixelVector;
import org.jetbrains.annotations.NotNull;

/**
 * Maps a rectangular world view onto a rectangle of the screen: the combination of an
 * orthographic 2D camera and a scaling policy.
 *
 * <p>Every {@link org.flixelgdx.FlixelCamera FlixelCamera} owns one of these. The
 * {@link Scaling} policy decides what happens when the window's shape does not match the
 * game's design resolution: {@link Scaling#FIT} letterboxes, {@link Scaling#EXTEND} grows the
 * visible world to fill the screen, and {@link Scaling#STRETCH} distorts. The viewport also
 * holds the camera's center position, zoom, and rotation, and builds the combined
 * projection matrix batches draw with.
 *
 * <p>Screen coordinates follow the renderer's convention: the viewport rectangle is measured
 * in physical pixels from the window's bottom-left corner.
 */
public class FlixelViewport {

  @NotNull
  private final FlixelMatrix combined = new FlixelMatrix();

  private final float designWidth;
  private final float designHeight;

  private float worldWidth;
  private float worldHeight;
  private float cameraX;
  private float cameraY;

  /** Visible-world multiplier: {@code 1} shows exactly the world size, {@code 0.5} zooms in 2x. */
  private float viewScale = 1f;

  private float rotation;

  private int screenX;
  private int screenY;
  private int screenWidth;
  private int screenHeight;

  @NotNull
  private Scaling scaling;

  private boolean matrixDirty = true;

  /**
   * Creates a letterboxing ({@link Scaling#FIT}) viewport.
   *
   * @param worldWidth The design world width in game pixels.
   * @param worldHeight The design world height in game pixels.
   */
  public FlixelViewport(float worldWidth, float worldHeight) {
    this(worldWidth, worldHeight, Scaling.FIT);
  }

  /**
   * Creates a viewport with an explicit scaling policy.
   *
   * @param worldWidth The design world width in game pixels.
   * @param worldHeight The design world height in game pixels.
   * @param scaling How the world maps onto mismatched window shapes.
   */
  public FlixelViewport(float worldWidth, float worldHeight, @NotNull Scaling scaling) {
    this.designWidth = Math.max(1f, worldWidth);
    this.designHeight = Math.max(1f, worldHeight);
    this.worldWidth = this.designWidth;
    this.worldHeight = this.designHeight;
    this.scaling = scaling;
    this.cameraX = this.designWidth / 2f;
    this.cameraY = this.designHeight / 2f;
  }

  /**
   * Recomputes the viewport for a new screen size.
   *
   * @param newScreenWidth The available screen width in pixels.
   * @param newScreenHeight The available screen height in pixels.
   * @param centerCamera When {@code true}, the camera recenters on the world's middle.
   */
  public void update(int newScreenWidth, int newScreenHeight, boolean centerCamera) {
    int sw = Math.max(1, newScreenWidth);
    int sh = Math.max(1, newScreenHeight);
    final float scale = Math.min(sw / designWidth, sh / designHeight);
    switch (scaling) {
      case FIT -> {
        int vw = Math.max(1, Math.round(designWidth * scale));
        int vh = Math.max(1, Math.round(designHeight * scale));
        screenX = (sw - vw) / 2;
        screenY = (sh - vh) / 2;
        screenWidth = vw;
        screenHeight = vh;
        worldWidth = designWidth;
        worldHeight = designHeight;
      }
      case EXTEND -> {
        screenX = 0;
        screenY = 0;
        screenWidth = sw;
        screenHeight = sh;
        worldWidth = sw / scale;
        worldHeight = sh / scale;
      }
      case STRETCH -> {
        screenX = 0;
        screenY = 0;
        screenWidth = sw;
        screenHeight = sh;
        worldWidth = designWidth;
        worldHeight = designHeight;
      }
    }
    if (centerCamera) {
      cameraX = worldWidth / 2f;
      cameraY = worldHeight / 2f;
    }
    matrixDirty = true;
  }

  /**
   * Overrides the screen rectangle directly, used for split-screen style sub-viewports.
   *
   * @param x Left edge in pixels from the window's left.
   * @param y Bottom edge in pixels from the window's bottom.
   * @param width Viewport width in pixels.
   * @param height Viewport height in pixels.
   */
  public void setScreenBounds(int x, int y, int width, int height) {
    screenX = x;
    screenY = y;
    screenWidth = Math.max(1, width);
    screenHeight = Math.max(1, height);
    matrixDirty = true;
  }

  /** Activates this viewport's screen rectangle on the graphics backend. */
  public void apply() {
    Flixel.graphics.setViewport(screenX, screenY, screenWidth, screenHeight);
  }

  /**
   * Converts window coordinates (pixels, y measured downward from the window's top) into world
   * coordinates, writing the result in place.
   *
   * @param screenCoords In: window coordinates; out: world coordinates.
   * @return The same vector, for chaining.
   */
  @NotNull
  public FlixelVector unproject(@NotNull FlixelVector screenCoords) {
    float windowHeight = Math.max(1, Flixel.graphics.getBackBufferHeight());
    float yFromBottom = windowHeight - screenCoords.y;
    float relX = (screenCoords.x - screenX) / screenWidth;
    float relY = (yFromBottom - screenY) / screenHeight;
    float visibleW = worldWidth * viewScale;
    float visibleH = worldHeight * viewScale;
    screenCoords.x = cameraX + (relX - 0.5f) * visibleW;
    screenCoords.y = cameraY + (relY - 0.5f) * visibleH;
    return screenCoords;
  }

  /**
   * Projects an axis-aligned rectangle from world coordinates into framebuffer pixels (measured
   * from the window's bottom-left corner), the form the graphics scissor takes.
   *
   * <p>Used for sprite clip rectangles. Because it derives the mapping from the same visible-world
   * rectangle the projection matrix uses, the scissor lines up exactly with what the batch draws.
   * Rotation is ignored, which is fine for the axis-aligned clip rectangles game code sets.
   *
   * @param worldX Left edge in world coordinates.
   * @param worldY Bottom edge in world coordinates.
   * @param worldW Width in world units.
   * @param worldH Height in world units.
   * @param out Reused output rectangle: {@code (x, y, width, height)} in framebuffer pixels.
   * @return The same {@code out} rectangle, for chaining.
   */
  @NotNull
  public FlixelRect projectToScissor(float worldX, float worldY, float worldW, float worldH, @NotNull FlixelRect out) {
    float visibleW = worldWidth * viewScale;
    float visibleH = worldHeight * viewScale;
    float left = cameraX - visibleW / 2f;
    float bottom = cameraY - visibleH / 2f;
    float sx = screenX + ((worldX - left) / visibleW) * screenWidth;
    float sy = screenY + ((worldY - bottom) / visibleH) * screenHeight;
    float sw = (worldW / visibleW) * screenWidth;
    float sh = (worldH / visibleH) * screenHeight;
    return out.set(sx, sy, sw, sh);
  }

  /**
   * Returns the combined view-projection matrix, rebuilding it when camera state changed.
   *
   * @return The matrix mapping world coordinates to normalized device coordinates.
   */
  @NotNull
  public FlixelMatrix getCombined() {
    if (matrixDirty) {
      matrixDirty = false;
      float visibleW = worldWidth * viewScale;
      float visibleH = worldHeight * viewScale;
      combined.setToOrtho2D(cameraX - visibleW / 2f, cameraY - visibleH / 2f, visibleW, visibleH,
          Flixel.graphics.isDepthZeroToOne());
      if (rotation != 0f) {
        combined.translate(cameraX, cameraY, 0f);
        combined.rotateZ(-rotation);
        combined.translate(-cameraX, -cameraY, 0f);
      }
    }
    return combined;
  }

  /**
   * Places the camera center.
   *
   * @param x World x of the view center.
   * @param y World y of the view center.
   */
  public void setCameraPosition(float x, float y) {
    if (cameraX != x || cameraY != y) {
      cameraX = x;
      cameraY = y;
      matrixDirty = true;
    }
  }

  /**
   * Sets the visible-world multiplier. {@code 1} shows exactly the world size; smaller values
   * zoom in (cameras pass {@code 1 / zoom}).
   *
   * @param viewScale The visible-world multiplier; clamped above {@code 0}.
   */
  public void setViewScale(float viewScale) {
    float clamped = Math.max(0.0001f, viewScale);
    if (this.viewScale != clamped) {
      this.viewScale = clamped;
      matrixDirty = true;
    }
  }

  /**
   * Rotates the view around the camera center.
   *
   * @param degrees Rotation in degrees.
   */
  public void setRotation(float degrees) {
    if (rotation != degrees) {
      rotation = degrees;
      matrixDirty = true;
    }
  }

  public float getCameraX() {
    return cameraX;
  }

  public float getCameraY() {
    return cameraY;
  }

  public float getViewScale() {
    return viewScale;
  }

  public float getWorldWidth() {
    return worldWidth;
  }

  public float getWorldHeight() {
    return worldHeight;
  }

  public int getScreenX() {
    return screenX;
  }

  public int getScreenY() {
    return screenY;
  }

  public int getScreenWidth() {
    return screenWidth;
  }

  public int getScreenHeight() {
    return screenHeight;
  }

  @NotNull
  public Scaling getScaling() {
    return scaling;
  }

  /**
   * Switches the scaling policy. Takes effect on the next {@link #update(int, int, boolean)}.
   *
   * @param scaling The new policy.
   */
  public void setScaling(@NotNull Scaling scaling) {
    this.scaling = scaling;
  }

  /** How a viewport maps its world onto a window whose shape does not match. */
  public enum Scaling {
    /** Scale the world uniformly and letterbox the leftover space. The classic default. */
    FIT,

    /**
     * Scale uniformly and grow the visible world to fill the screen (no bars, more world shown).
     *
     * <p>This policy works as described when the game renders directly to the window (render
     * resolution disabled via {@link org.flixelgdx.FlixelGame.Config.Builder#disableRenderResolution()}).
     * When a fixed render resolution is active (the default), the scene surface is a fixed size and
     * cannot extend, so this policy behaves the same as {@link #FIT} within that surface.
     */
    EXTEND,

    /**
     * Stretch each axis independently to fill the screen, distorting the art.
     *
     * <p>Like {@link #EXTEND}, this policy only fills the full window when the game renders
     * directly to the window (render resolution disabled). With a fixed render resolution active,
     * the distortion is applied to the fixed surface, which is then upscaled by the compositor.
     */
    STRETCH
  }
}
