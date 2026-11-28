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
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.FlixelManagers;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelSpriteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The manager that draws every frame: each camera with the state on it, the global shaders, and the
 * overlay, then stretches the result onto the window.
 *
 * <p>Think of it as the projectionist in a cinema. The state is the film, the cameras are the
 * projectors, and this manager runs them in order every frame: it clears the screen to
 * {@link #bgColor}, runs one pass per camera in {@link Flixel#cameras} (where the state draws
 * itself), sends the combined picture through any global shaders, draws the overlay on top, and
 * finally stretches the whole scene to fit the window.
 *
 * <p>It is registered in {@link Flixel#managers} at {@link FlixelManagers.Manager#ORDER_RENDER} and
 * reached through {@link Flixel#render}. Other managers join its passes through
 * {@link FlixelManagers.Manager#drawCamera(FlixelCamera, FlixelBatch)} and
 * {@link FlixelManagers.Manager#drawOverlay(FlixelBatch)}.
 *
 * <p>It also owns the render settings: it applies the render scale and resolution from
 * {@link Flixel#config} when the game starts, keeps the global shaders sized to the window, and lowers
 * the frame rate while the window is out of focus and {@link Flixel#autoPause} is on.
 *
 * <p>Example:
 * <pre>{@code
 * Flixel.render.bgColor.set(FlixelColor.BLUE); // The color behind every camera.
 * }</pre>
 */
public class FlixelRenderManager implements FlixelManagers.Manager {

  /** The frame rate the render loop is capped to while the window is out of focus and {@link Flixel#autoPause} is on. */
  public static final int BACKGROUND_FPS = 10;

  /** The color the whole window is cleared to before any camera draws. */
  @NotNull
  public final FlixelColor bgColor = new FlixelColor(FlixelColor.BLACK);

  /** The projection reused for each camera's shader composite pass. */
  private final FlixelMatrix fboOrtho = new FlixelMatrix();

  /** The shared 1x1 white pixel used to fill camera backgrounds and effects. */
  @Nullable
  private FlixelFrame bgPixel;

  /** The camera size {@link #fboOrtho} was built for. {@code -1} means it has not been built yet. */
  private int fboOrthoW = -1;

  private int fboOrthoH = -1;

  /** The render calls of the last finished frame, summed over every camera pass. */
  private int frameRenderCalls;

  /** The frame rate cap saved when the window lost focus, put back when it returns. */
  private int savedTargetFps;

  /** Whether the frame rate is lowered because the window is out of focus. */
  private boolean throttled;

  /**
   * Applies the render scale and resolution from {@link Flixel#config}, now that the graphics backend
   * is running, and loads the shared white pixel.
   */
  @Override
  public void create() {
    // The resolution is on by default at the design size, which is also the starting window size.
    Flixel.graphics.setRenderSmooth(Flixel.config.isRenderSmooth());
    Flixel.graphics.setRenderScale(Flixel.config.getRenderScale());
    if (Flixel.config.isRenderResolutionEnabled()) {
      Flixel.graphics.setRenderResolution(Flixel.config.getRenderWidth(), Flixel.config.getRenderHeight(),
          Flixel.config.isRenderSmooth());
    } else {
      Flixel.graphics.clearRenderResolution();
    }
    bgPixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);
  }

  /**
   * Draws the whole frame: every camera with the state on it, the global shaders, and the overlay.
   *
   * @param batch The framework batch.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    Flixel.graphics.clear(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
    int renderCallsBefore = batch.getTotalRenderCalls();
    FlixelGame game = Flixel.game;
    boolean paused = game != null && game.isGamePaused();

    boolean useGlobalShaders = Flixel.graphics.hasGlobalShaders();
    // The two nest: the scaled scene surface is the outer target, and the global shader capture runs
    // inside it at the same size. The chain's final pass then lands on the scene surface, which
    // endScene() stretches onto the window afterwards. The backend decides whether a scene surface
    // is needed at all, and both calls do nothing when the scene draws straight to the window.
    Flixel.graphics.beginScene();
    if (useGlobalShaders) {
      Flixel.graphics.beginGlobalShaderCapture();
    }

    for (int i = 0, n = Flixel.cameras.getSize(); i < n; i++) {
      drawCamera(Flixel.cameras.get(i), game, paused, batch);
    }

    if (useGlobalShaders) {
      Flixel.graphics.endGlobalShaderCapture();
      Flixel.graphics.applyGlobalShaderChain(batch);
    }

    FlixelCamera.Manager cameraManager = Flixel.cameraManager;
    FlixelCamera overlayCamera = cameraManager != null ? cameraManager.getOverlayCamera() : null;
    if (overlayCamera != null) {
      drawOverlay(overlayCamera, paused, batch);
    }

    // Stretch the scene surface (cameras plus overlay) to fill the window in one pass.
    Flixel.graphics.endScene();
    frameRenderCalls = batch.getTotalRenderCalls() - renderCallsBefore;
  }

  /**
   * Makes the window opaque again at the end of the frame when the framebuffer has an alpha channel
   * but window transparency is off, after every manager finished drawing.
   */
  @Override
  public void endFrame() {
    FlixelConfig config = Flixel.config;
    if (config != null && config.isTransparentFramebuffer() && !Flixel.window.isTransparencyActive()) {
      Flixel.graphics.forceOpaqueAlpha();
    }
  }

  /**
   * Keeps the global shaders sized to the new surface.
   *
   * @param width The new surface width in pixels.
   * @param height The new surface height in pixels.
   */
  @Override
  public void resize(int width, int height) {
    Flixel.graphics.resizeGlobalShaders();
  }

  /** Lowers the frame rate to {@link #BACKGROUND_FPS} while the window is out of focus and {@link Flixel#autoPause} is on. */
  @Override
  public void onFocusLost() {
    if (Flixel.autoPause && !throttled) {
      savedTargetFps = Flixel.graphics.getTargetFps();
      Flixel.graphics.setTargetFps(BACKGROUND_FPS);
      throttled = true;
    }
  }

  /** Puts the frame rate back when the window regains focus. */
  @Override
  public void onFocusGained() {
    if (throttled) {
      Flixel.graphics.setTargetFps(savedTargetFps);
      throttled = false;
    }
  }

  /** Releases the global shaders and window transparency when the game closes. */
  @Override
  public void destroy() {
    Flixel.setDrawCamera(null);
    Flixel.graphics.disposeGlobalShaders();
    Flixel.window.resetTransparency();
    fboOrthoW = -1;
    fboOrthoH = -1;
    // The white pixel is a shared asset owned by the asset manager, so it is not destroyed here.
    bgPixel = null;
    throttled = false;
  }

  private void drawCamera(FlixelCamera camera, @Nullable FlixelGame game, boolean paused, FlixelBatch batch) {
    Flixel.graphics.beginCameraPass();
    Flixel.setDrawCamera(camera);
    try {
      if (paused) {
        camera.applyCameraTransform();
      }
      camera.applyViewport();

      FlixelShader cameraShader = camera.getShader();
      if (cameraShader != null) {
        camera.getFbo().begin();
        Flixel.graphics.clear(0f, 0f, 0f, 0f);
      }

      batch.setProjection(camera.getCombinedMatrix());
      batch.begin();
      camera.fill(camera.bgColor, camera.useBgAlphaBlending, 1f, batch, bgPixel);
      if (game != null) {
        game.drawState(batch);
      }
      Flixel.managers.drawCamera(camera, batch);
      camera.drawFX(batch, bgPixel);
      batch.end();

      // Per-sprite shader draws restore themselves, but reset here as a backstop so the next camera
      // pass always starts with the default batch shader.
      batch.setShader(null);

      if (cameraShader != null) {
        compositeCamera(camera, cameraShader, batch);
      }
    } finally {
      Flixel.setDrawCamera(null);
    }
  }

  private void compositeCamera(FlixelCamera camera, FlixelShader cameraShader, FlixelBatch batch) {
    // Ending the camera's render target returns drawing to the global target (when active) or the
    // screen, since render targets nest.
    camera.getFbo().end();
    camera.applyViewport();
    if (camera.width != fboOrthoW || camera.height != fboOrthoH) {
      fboOrthoW = camera.width;
      fboOrthoH = camera.height;
      // Y-down composite ortho so the blit matches the batch's Y-down vertex layout and the camera's
      // render target draws upright. Match the active backend's depth range too; the [-1, 1] default
      // depth clips the composite quad to black on Vulkan, Metal, and Direct3D.
      fboOrtho.setToOrtho2DYDown(0, 0, fboOrthoW, fboOrthoH, Flixel.graphics.isDepthZeroToOne());
    }
    batch.setProjection(fboOrtho);
    batch.setShader(cameraShader);
    batch.begin();
    cameraShader.applyUniforms();
    FlixelRenderTarget fbo = camera.getFbo();
    if (fbo.isFlipped()) {
      batch.draw(fbo.getTexture(), 0, 0, camera.width, camera.height, 0f, 1f, 1f, 0f);
    } else {
      batch.draw(fbo.getTexture(), 0, 0, camera.width, camera.height);
    }
    batch.end();
    batch.setShader(null);
  }

  private void drawOverlay(FlixelCamera overlayCamera, boolean paused, FlixelBatch batch) {
    Flixel.graphics.beginCameraPass();
    Flixel.setDrawCamera(overlayCamera);
    try {
      if (paused) {
        overlayCamera.applyCameraTransform();
      }
      overlayCamera.applyViewport();
      batch.setProjection(overlayCamera.getCombinedMatrix());
      batch.begin();
      Flixel.managers.drawOverlay(batch);
      batch.end();
      batch.setShader(null);
    } finally {
      Flixel.setDrawCamera(null);
    }
  }

  @Override
  public int getOrder() {
    return ORDER_RENDER;
  }

  @NotNull
  @Override
  public String getName() {
    return "flixel.render";
  }

  @NotNull
  public FlixelColor getBgColor() {
    return bgColor;
  }

  /**
   * Returns the shared 1x1 white pixel used to fill camera backgrounds and effects.
   *
   * @return The white pixel, or {@code null} before the game was created.
   */
  @Nullable
  public FlixelFrame getBgPixel() {
    return bgPixel;
  }

  /**
   * Returns how many render calls the framework batch issued in the last finished frame, summed
   * over every camera pass. Counting the difference of {@link FlixelBatch#getTotalRenderCalls()}
   * means several begin and end cycles in one frame never erase the earlier cameras' counts.
   *
   * @return The render calls of the last finished frame.
   */
  public int getFrameRenderCalls() {
    return frameRenderCalls;
  }

  /**
   * Returns whether the frame rate is currently lowered because the window is out of focus.
   *
   * @return {@code true} while the frame rate is lowered.
   */
  public boolean isThrottled() {
    return throttled;
  }
}
