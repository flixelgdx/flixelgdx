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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import org.flixelgdx.functional.FlixelColorable;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.functional.FlixelShaderable;
import org.flixelgdx.util.FlixelAxes;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A powerful camera class that controls world-to-screen projection, parallax scrolling, zoom,
 * and screen effects such as flash, fade, and shake.
 *
 * <p>In a full FlixelGDX game, cameras are usually managed by {@link FlixelGame}.
 * You can also use {@code FlixelCamera} in a plain libGDX {@code ApplicationListener}. When
 * {@link Flixel#game} is {@code null}, window size and follow frame rate fall back to
 * {@link Gdx#graphics} (call {@link #update(int, int, boolean)} from {@code resize} as usual).
 *
 * <p>Every camera wraps a libGDX {@link Camera} and {@link Viewport} internally. By default, an
 * {@link OrthographicCamera} and {@link FitViewport} are used. The viewport type is controlled by
 * the static {@link #viewportFactory}; platform launchers override it to supply a different
 * viewport (for example, the Android launcher installs an
 * {@link com.badlogic.gdx.utils.viewport.ExtendViewport} so the game fills the screen without
 * letterboxing). Custom types can also be provided directly via the constructor overloads.
 *
 * <p>{@link FitViewport} scales the game world to the window, so the world-to-screen factor is often
 * not a whole number when the window is larger than the camera's internal size (e.g. fullscreen).
 * libGDX {@link BitmapFont} defaults to integer-snapped glyph quads, which looks blocky under
 * that scaling. Set {@link BitmapFont#setUseIntegerPositions(boolean)} to {@code false} on fonts
 * you draw through this pipeline (FlixelGDX does this for {@link org.flixelgdx.text.FlixelText FlixelText}
 * and registry fonts automatically).
 */
public class FlixelCamera extends FlixelBasic implements FlixelColorable, FlixelShaderable {

  /**
   * Any {@code FlixelCamera} with a zoom of {@code <= 0} (the default constructor value) will
   * receive this zoom level instead.
   */
  public static float defaultZoom = 1.0f;

  /**
   * Factory used to create the default {@link Viewport} whenever a new {@link FlixelCamera} is
   * constructed without an explicit viewport.
   *
   * <p>The built-in default creates a {@link FitViewport}, which letterboxes the game world to
   * fit the window. Platform launchers replace this before any camera is built - for example,
   * the Android launcher assigns an {@link com.badlogic.gdx.utils.viewport.ExtendViewport} so
   * the game fills the device screen without black bars.
   *
   * <p>To use a custom viewport for every camera created after the assignment:
   *
   * <pre>{@code
   * FlixelCamera.viewportFactory = (w, h, cam) -> new ScreenViewport(cam);
   * }</pre>
   *
   * <p>Passing a custom {@link Viewport} to the constructor always takes priority over this field.
   */
  public static ViewportFactory viewportFactory = (w, h, cam) -> new FitViewport(w, h, cam);

  /** Zoom captured at construction time; useful for resetting to the original scale. */
  public final float initialZoom;

  /** The alpha value of this camera's display, from {@code 0.0} (invisible) to {@code 1.0} (fully opaque). */
  public float alpha = 1.0f;

  /** The rotation angle of the camera display in degrees. */
  public float angle = 0f;

  /**
   * Horizontal look-ahead offset applied to the follow target's world position.
   * Positive values shift the visible region ahead of the target along the X axis.
   */
  public float followLeadX;

  /**
   * Vertical look-ahead offset applied to the follow target's world position.
   * Positive values shift the visible region ahead of the target along the Y axis.
   */
  public float followLeadY;

  /**
   * The ratio of the distance to the follow target the camera moves per 1/60 sec.
   * {@code 1.0} snaps to the target each frame. {@code 0.0} stops all movement. Lower values
   * produce smoother, lagging motion.
   */
  public float followLerp = 1.0f;

  /**
   * Upper bound of the camera's scroll on the X axis. {@code Float.NaN} means unbounded.
   *
   * @see #setScrollBounds(float, float, float, float)
   */
  public float maxScrollX = Float.NaN;

  /**
   * Upper bound of the camera's scroll on the Y axis. {@code Float.NaN} means unbounded.
   *
   * @see #setScrollBounds(float, float, float, float)
   */
  public float maxScrollY = Float.NaN;

  /**
   * Lower bound of the camera's scroll on the X axis. {@code Float.NaN} means unbounded.
   *
   * @see #setScrollBounds(float, float, float, float)
   */
  public float minScrollX = Float.NaN;

  /**
   * Lower bound of the camera's scroll on the Y axis. {@code Float.NaN} means unbounded.
   *
   * @see #setScrollBounds(float, float, float, float)
   */
  public float minScrollY = Float.NaN;

  /**
   * The camera's X scroll position in world coordinates. This is the left edge of the visible
   * world region. Use {@link #focusOn(float, float)} to center on a specific world point.
   */
  public float scrollX;

  /**
   * The camera's Y scroll position in world coordinates. This is the top edge of the visible
   * world region. Use {@link #focusOn(float, float)} to center on a specific world point.
   */
  public float scrollY;

  /**
   * Horizontal offset applied to the follow target's world position before follow math runs.
   * Use this to track a point offset from the target's center (e.g. track ahead of a character).
   */
  public float targetOffsetX;

  /**
   * Vertical offset applied to the follow target's world position before follow math runs.
   * Use this to track a point offset from the target's center.
   */
  public float targetOffsetY;

  /**
   * The X position of this camera's display in native screen pixels.
   *
   * <p>Note that {@link #getZoom()} does NOT affect this value.
   */
  public float x;

  /**
   * The Y position of this camera's display in native screen pixels.
   *
   * <p>Note that{@link #getZoom()} does NOT affect this value.
   */
  public float y;

  /** How wide the camera display is, in game pixels. */
  public int width;

  /** How tall the camera display is, in game pixels. */
  public int height;

  /** The natural background color of the camera. Defaults to black. */
  public Color bgColor = new Color(Color.BLACK);

  /** The color tint of the camera display. */
  public Color color = new Color(Color.WHITE);

  /**
   * The dead zone rectangle, measured from the camera's bottom-left corner in game pixels.
   *
   * <p>The camera always keeps the follow target inside this zone, unless bumping against scroll
   * bounds. For rapid prototyping, use the preset styles via
   * {@link #follow(FlixelPositional, FollowStyle, float)}.
   */
  public Rectangle deadzone;

  /** The current follow style used when a {@link #target} is set. */
  public FollowStyle style = FollowStyle.LOCKON;

  /**
   * The {@link FlixelPositional} this camera follows.
   *
   * <p>You can set this value via {@link #follow(FlixelPositional)}.
   */
  public FlixelPositional target;

  private float fadeAlpha = 0f;
  private float fadeDuration = 1f;
  private float fadeElapsed = 0f;
  private float flashAlpha = 0f;
  private float flashDuration = 1f;
  private float flashElapsed = 0f;
  private float normalizedRegionHeight = 1f;
  private float normalizedRegionWidth = 1f;
  private float normalizedRegionX = 0f;
  private float normalizedRegionY = 0f;
  private float regionX = 0f;
  private float regionY = 0f;
  private float shakeElapsed = 0f;
  private float shakeIntensity = 0.05f;
  private float shakeDuration = 0.5f;
  private float shakeOffsetX = 0f;
  private float shakeOffsetY = 0f;

  /**
   * Internal zoom storage.
   *
   * <p>Use {@link #getZoom()} / {@link #setZoom(float)} to access.
   * A value of {@code 1} = 1:1, {@code 2} = 2x magnification (world appears larger).
   */
  private float zoom;

  private int regionHeight = 0;
  private int regionWidth = 0;

  private final Camera camera;
  private final Color fadeColor = new Color(Color.BLACK);
  private final Color flashColor = new Color(Color.WHITE);
  private final Rectangle tmpRect = new Rectangle();
  private final Viewport viewport;
  private Runnable fadeOnComplete;
  private Runnable flashOnComplete;
  private Runnable shakeOnComplete;
  private FlixelAxes shakeAxes = FlixelAxes.XY;
  private FlixelShader shader;
  private FrameBuffer fbo;
  private TextureRegion fboRegion;

  private RegionMode regionMode = RegionMode.PIXEL_TOP_LEFT;

  /**
   * Whether the libGDX viewport should re-center the camera when the game window is resized.
   *
   * <p>Split-screen setups often want this enabled (default) to match existing behavior.
   * Disable it to preserve the scroll position through resizes.
   */
  public boolean centerCameraOnResize = true;

  /** Whether positions of rendered objects are rounded to whole pixels. */
  public boolean pixelPerfectRender = false;

  /**
   * If {@code true}, screen shake offsets are rounded to whole pixels.
   * Falls back to {@link #pixelPerfectRender} when {@code false}.
   */
  public boolean pixelPerfectShake = false;

  /** Whether to use alpha blending for the camera's background fill. */
  public boolean useBgAlphaBlending = false;

  /**
   * When {@code true}, {@link #update(int, int, boolean)} fits this camera into the screen
   * rectangle {@code (x, y, width, height)} instead of the full window. When {@code false},
   * placement is inferred when {@link Flixel#game} has multiple cameras
   * (horizontal/vertical strips, picture-in-picture, etc.).
   */
  public boolean useSubScreenViewport = false;

  private boolean fadeActive = false;
  private boolean fadeIn = false;
  private boolean flashActive = false;

  /**
   * Whether pixel-based region placement should use the explicit region fields instead of
   * {@link #x}/{@link #y}/{@link #width}/{@link #height}.
   */
  private boolean hasCustomPixelRegion = false;

  private boolean shakeActive = false;

  /**
   * Creates a camera sized to the current window using the default
   * {@link OrthographicCamera} and {@link #viewportFactory}.
   */
  public FlixelCamera() {
    this(0f, 0f, resolveWindowWidth(), resolveWindowHeight(), 0f);
  }

  /**
   * Creates a camera with the given dimensions using the default camera and viewport types.
   *
   * @param width The width of the camera display in game pixels.
   * @param height The height of the camera display in game pixels.
   */
  public FlixelCamera(int width, int height) {
    this(0f, 0f, width, height, 0f);
  }

  /**
   * Creates a camera with a custom libGDX {@link Camera}, wrapped in the default viewport from {@link #viewportFactory}.
   *
   * @param width The width of the camera display in game pixels.
   * @param height The height of the camera display in game pixels.
   * @param camera A custom libGDX Camera (e.g. {@link com.badlogic.gdx.graphics.PerspectiveCamera}).
   */
  public FlixelCamera(int width, int height, Camera camera) {
    this(0f, 0f, width, height, 0f, camera, null);
  }

  /**
   * Creates a camera with a custom libGDX {@link Viewport}. The camera is extracted from the viewport.
   *
   * @param width The width of the camera display in game pixels.
   * @param height The height of the camera display in game pixels.
   * @param viewport A custom libGDX Viewport (e.g. {@link com.badlogic.gdx.utils.viewport.ScreenViewport}).
   */
  public FlixelCamera(int width, int height, Viewport viewport) {
    this(0f, 0f, width, height, 0f, null, viewport);
  }

  /**
   * Creates a camera at the given display position, size, and zoom level using default types.
   *
   * @param x X location of the camera's display in native screen pixels.
   * @param y Y location of the camera's display in native screen pixels.
   * @param width The width of the camera display in game pixels. {@code 0} = window width.
   * @param height The height of the camera display in game pixels. {@code 0} = window height.
   * @param zoom The initial zoom level. {@code 0} = {@link #defaultZoom}.
   */
  public FlixelCamera(float x, float y, int width, int height, float zoom) {
    this(x, y, width, height, zoom, null, null);
  }

  /**
   * Full constructor allowing fully custom libGDX {@link Camera} and {@link Viewport} types.
   *
   * <p>If {@code viewport} is provided, its camera is used (the {@code camera} parameter is ignored).
   * If only {@code camera} is provided, it is wrapped in a default viewport.
   * If neither is provided, an {@link OrthographicCamera} and a default viewport are created.
   *
   * <p>When {@code viewport} is {@code null}, the viewport is created by {@link #viewportFactory}.
   * Platform launchers set this factory before any camera is built, so the right viewport type
   * is used automatically (for example, {@link com.badlogic.gdx.utils.viewport.FitViewport} on
   * desktop and {@link com.badlogic.gdx.utils.viewport.ExtendViewport} on Android).
   *
   * @param x X location of the camera's display in native screen pixels.
   * @param y Y location of the camera's display in native screen pixels.
   * @param width The width of the camera display in game pixels. {@code 0} = window width.
   * @param height The height of the camera display in game pixels. {@code 0} = window height.
   * @param zoom The initial zoom level. {@code 0} = {@link #defaultZoom}. {@code 2} = 2x magnification.
   * @param camera Custom libGDX Camera, or {@code null} for a default {@link OrthographicCamera}.
   * @param viewport Custom libGDX Viewport, or {@code null} to use {@link #viewportFactory}.
   */
  public FlixelCamera(float x, float y, int width, int height, float zoom, Camera camera, Viewport viewport) {
    super();
    this.x = x;
    this.y = y;
    this.width = (width <= 0) ? resolveWindowWidth() : width;
    this.height = (height <= 0) ? resolveWindowHeight() : height;

    if (viewport != null) {
      this.viewport = viewport;
      this.camera = viewport.getCamera();
    } else if (camera != null) {
      this.camera = camera;
      this.viewport = viewportFactory.create(this.width, this.height, this.camera);
    } else {
      this.camera = new OrthographicCamera(this.width, this.height);
      this.viewport = viewportFactory.create(this.width, this.height, this.camera);
    }

    this.zoom = (zoom == 0f) ? defaultZoom : zoom;
    this.initialZoom = this.zoom;
    applyZoom();

    update(resolveActualScreenWidth(), resolveActualScreenHeight(), centerCameraOnResize);
  }

  /**
   * Applies the viewport's OpenGL scissor rectangle.
   *
   * <p>Call before rendering through {@code this} camera.</p>
   */
  public void apply() {
    viewport.apply();
  }

  /**
   * Updates the viewport in response to a window resize event.
   *
   * @param screenWidth The new screen width in pixels.
   * @param screenHeight The new screen height in pixels.
   * @param centerCamera Whether to re-center the camera in the viewport.
   */
  public void update(int screenWidth, int screenHeight, boolean centerCamera) {
    if (shouldUseSubScreenViewport(screenWidth, screenHeight)) {
      updateSubScreenViewport(screenWidth, screenHeight, centerCamera);
    } else {
      viewport.update(screenWidth, screenHeight, centerCamera);
    }
    // Viewport.apply(centerCamera) resets the libGDX camera to the world center when centerCamera is true.
    // Flixel owns the visible region via scrollX/scrollY, so re-apply the transform to stay correct.
    applyLibCameraTransform();
  }

  /**
   * Updates the camera scroll, follow logic, and active effects. Called once per frame.
   *
   * @param elapsed Seconds that have elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    if (!active || !exists) {
      return;
    }

    if (shader != null) {
      shader.update(elapsed);
    }

    updateFollow(elapsed);
    updateScroll();
    updateFlash(elapsed);
    updateFade(elapsed);
    updateShake(elapsed);
    applyLibCameraTransform();
  }

  /**
   * Pushes {@link #scrollX}/{@link #scrollY}, zoom, angle, and shake offsets into the underlying libGDX {@link Camera}.
   *
   * <p>Call this after mutating scroll outside {@link #update(float)} (e.g., during a debug pause pan) and before
   * {@link Viewport#unproject(Vector2)} or any rendering.
   * Safe to call every frame; {@link #update(float)} ends with this automatically.
   *
   * <p>Drawables use view (batch) coordinates from {@link #worldToViewX(float, float)} and
   * {@link #worldToViewY(float, float)} (see {@link FlixelSprite#draw}).
   * The orthographic camera looks at the center of that space ({@code viewW/2, viewH/2}).
   */
  public void applyLibCameraTransform() {
    // TODO: Find a way to avoid explicit casting.
    if (camera instanceof OrthographicCamera ortho) {
      ortho.up.set(0, 1, 0);
      ortho.direction.set(0, 0, -1);
      if (angle != 0f) {
        ortho.rotate(angle);
      }
    }

    float camX = getViewWidth() / 2f + shakeOffsetX;
    float camY = getViewHeight() / 2f + shakeOffsetY;
    camera.position.set(camX, camY, 0);
    camera.update();
  }

  /**
   * Tells this camera to follow the given object using {@link FollowStyle#LOCKON} and a lerp of
   * {@code 1.0f} (instant snap).
   *
   * @param target The object to follow. Pass {@code null} to stop following.
   */
  public void follow(FlixelPositional target) {
    follow(target, FollowStyle.LOCKON, 1.0f);
  }

  /**
   * Tells this camera to follow the given object with the specified style and a lerp of {@code 1.0f}.
   *
   * @param target The object to follow. Pass {@code null} to stop following.
   * @param style One of the preset {@link FollowStyle} dead zone presets.
   */
  public void follow(FlixelPositional target, FollowStyle style) {
    follow(target, style, 1.0f);
  }

  /**
   * Tells this camera to follow the given object.
   *
   * @param target The object to follow. Pass {@code null} to stop following.
   * @param style One of the preset {@link FollowStyle} dead zone presets.
   * @param lerp How much lag the camera has. {@code 1.0f} = snap, lower = smoother.
   */
  public void follow(FlixelPositional target, FollowStyle style, float lerp) {
    this.target = target;
    this.style = style != null ? style : FollowStyle.LOCKON;
    this.followLerp = lerp;
    updateDeadzoneForStyle();
  }

  /**
   * Instantly moves the camera so the given world point is centered.
   *
   * @param worldX World-space X to center on.
   * @param worldY World-space Y to center on.
   */
  public void focusOn(float worldX, float worldY) {
    // Scroll = point - full camera buffer/2, not inner view/2, so worldToView*
    // centers stay correct when zoom != 1.
    scrollX = worldX - width * 0.5f;
    scrollY = worldY - height * 0.5f;
  }

  /**
   * Snaps the camera to the current {@link #target} position with no easing, then clamps scroll to
   * bounds. Useful after teleporting the target.
   */
  public void snapToTarget() {
    if (target == null) {
      return;
    }
    float fsx = followScrollFactorX(target);
    float fsy = followScrollFactorY(target);
    float tx = target.getX() + target.getWidth() / 2f + targetOffsetX + followLeadX;
    float ty = target.getY() + target.getHeight() / 2f + targetOffsetY + followLeadY;
    scrollX = scrollXForFollowCenter(tx, fsx);
    scrollY = scrollYForFollowCenter(ty, fsy);
    updateScroll();
  }

  /**
   * Specifies the scroll bounds for each axis. Pass {@code Float.NaN} for any side to leave it
   * unbounded.
   *
   * @param minX Lower X bound, or {@code Float.NaN}.
   * @param maxX Upper X bound, or {@code Float.NaN}.
   * @param minY Lower Y bound, or {@code Float.NaN}.
   * @param maxY Upper Y bound, or {@code Float.NaN}.
   */
  public void setScrollBounds(float minX, float maxX, float minY, float maxY) {
    this.minScrollX = minX;
    this.maxScrollX = maxX;
    this.minScrollY = minY;
    this.maxScrollY = maxY;
  }

  /**
   * Specifies scroll bounds as a bounding rectangle (typically the level size).
   *
   * @param x Smallest X value (usually {@code 0}).
   * @param y Smallest Y value (usually {@code 0}).
   * @param w Largest X extent (usually level width).
   * @param h Largest Y extent (usually level height).
   */
  public void setScrollBoundsRect(float x, float y, float w, float h) {
    setScrollBoundsRect(x, y, w, h, false);
  }

  /**
   * Specifies scroll bounds as a bounding rectangle.
   *
   * @param x Smallest X value (usually {@code 0}).
   * @param y Smallest Y value (usually {@code 0}).
   * @param w Largest X extent (usually level width).
   * @param h Largest Y extent (usually level height).
   * @param updateWorld Reserved for future use (quad-tree bounds).
   */
  public void setScrollBoundsRect(float x, float y, float w, float h, boolean updateWorld) {
    minScrollX = x;
    maxScrollX = x + w;
    minScrollY = y;
    maxScrollY = y + h;
  }

  /**
   * Clamps {@link #scrollX} and {@link #scrollY} to the configured scroll bounds.
   * Called automatically each frame by {@link #update(float)}.
   */
  public void updateScroll() {
    float vw = getViewWidth();
    float vh = getViewHeight();
    if (!Float.isNaN(minScrollX) && scrollX < minScrollX) {
      scrollX = minScrollX;
    }
    if (!Float.isNaN(maxScrollX) && scrollX + vw > maxScrollX) {
      scrollX = maxScrollX - vw;
    }
    if (!Float.isNaN(minScrollY) && scrollY < minScrollY) {
      scrollY = minScrollY;
    }
    if (!Float.isNaN(maxScrollY) && scrollY + vh > maxScrollY) {
      scrollY = maxScrollY - vh;
    }
  }

  /** Flashes white for 1 second. */
  public void flash() {
    flash(Color.WHITE, 1f, null, false);
  }

  /**
   * Flashes the given color for 1 second.
   *
   * @param color The color to flash.
   */
  public void flash(@NotNull Color color) {
    flash(color, 1f, null, false);
  }

  /**
   * Flashes the given color for the specified duration.
   *
   * @param color The color to flash.
   * @param duration How long the flash takes to fade out, in seconds.
   */
  public void flash(@NotNull Color color, float duration) {
    flash(color, duration, null, false);
  }

  /**
   * Fills the screen with the given color and gradually fades it back to normal.
   *
   * @param color The color to flash.
   * @param duration How long the flash takes to fade out, in seconds.
   * @param onComplete Callback invoked when the flash finishes, or {@code null}.
   * @param force If {@code true}, resets any currently running flash.
   */
  public void flash(Color color, float duration, Runnable onComplete, boolean force) {
    if (flashActive && !force) {
      return;
    }
    flashActive = true;
    flashColor.set(color);
    flashDuration = Math.max(duration, 0.001f);
    flashElapsed = 0f;
    flashAlpha = 1f;
    flashOnComplete = onComplete;
  }

  /**
   * Flashes the given color for 1 second.
   *
   * @param color The color to flash.
   */
  public void flash(@NotNull FlixelColor color) {
    flash(color.getGdxColor(), 1f, null, false);
  }

  /**
   * Flashes the given color for the specified duration.
   *
   * @param color The color to flash.
   * @param duration How long the flash takes to fade out, in seconds.
   */
  public void flash(@NotNull FlixelColor color, float duration) {
    flash(color.getGdxColor(), duration, null, false);
  }

  /**
   * Fills the screen with the given color and gradually fades it back to normal.
   *
   * @param color The color to flash.
   * @param duration How long the flash takes to fade out, in seconds.
   * @param onComplete Callback invoked when the flash finishes, or {@code null}.
   * @param force If {@code true}, resets any currently running flash.
   */
  public void flash(@NotNull FlixelColor color, float duration, Runnable onComplete, boolean force) {
    flash(color.getGdxColor(), duration, onComplete, force);
  }

  /** Fades to black over 1 second. */
  public void fade() {
    fade(Color.BLACK, 1f, false, null, false);
  }

  /**
   * Fades to the given color over 1 second.
   *
   * @param color The color to fade to.
   */
  public void fade(@NotNull Color color) {
    fade(color, 1f, false, null, false);
  }

  /**
   * Fades to the given color over the specified duration.
   *
   * @param color The color to fade to.
   * @param duration How long the fade takes, in seconds.
   */
  public void fade(@NotNull Color color, float duration) {
    fade(color, duration, false, null, false);
  }

  /**
   * Fades to or from the given color.
   *
   * @param color The color to fade to or from.
   * @param duration How long the fade takes, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false} = fade TO the color.
   */
  public void fade(@NotNull Color color, float duration, boolean fadeIn) {
    fade(color, duration, fadeIn, null, false);
  }

  /**
   * Gradually fills the screen with or clears it of the given color.
   *
   * @param color The color to fade to or from.
   * @param duration How long the fade takes, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false} = fade TO the color.
   * @param onComplete Callback invoked when the fade finishes, or {@code null}.
   * @param force If {@code true}, resets any currently running fade.
   */
  public void fade(Color color, float duration, boolean fadeIn, Runnable onComplete, boolean force) {
    if (fadeActive && !force) {
      return;
    }
    fadeActive = true;
    fadeColor.set(color);
    fadeDuration = Math.max(duration, 0.001f);
    fadeElapsed = 0f;
    this.fadeIn = fadeIn;
    fadeAlpha = fadeIn ? 1f : 0f;
    fadeOnComplete = onComplete;
  }

  /**
   * Fades to the given color over 1 second.
   *
   * @param color The color to fade to.
   */
  public void fade(@NotNull FlixelColor color) {
    fade(color.getGdxColor(), 1f, false, null, false);
  }

  /**
   * Fades to the given color over the specified duration.
   *
   * @param color The color to fade to.
   * @param duration How long the fade takes, in seconds.
   */
  public void fade(@NotNull FlixelColor color, float duration) {
    fade(color.getGdxColor(), duration, false, null, false);
  }

  /**
   * Fades to or from the given color.
   *
   * @param color The color to fade to or from.
   * @param duration How long the fade takes, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false} = fade TO the color.
   */
  public void fade(@NotNull FlixelColor color, float duration, boolean fadeIn) {
    fade(color.getGdxColor(), duration, fadeIn, null, false);
  }

  /**
   * Gradually fills the screen with or clears it of the given color.
   *
   * @param color The color to fade to or from.
   * @param duration How long the fade takes, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false} = fade TO the color.
   * @param onComplete Callback invoked when the fade finishes, or {@code null}.
   * @param force If {@code true}, resets any currently running fade.
   */
  public void fade(@NotNull FlixelColor color, float duration, boolean fadeIn, Runnable onComplete, boolean force) {
    fade(color.getGdxColor(), duration, fadeIn, onComplete, force);
  }

  /** Shakes with default intensity ({@code 0.05}) for {@code 0.5} seconds on both axes. */
  public void shake() {
    shake(0.05f, 0.5f, null, true, FlixelAxes.XY);
  }

  /**
   * Shakes with the given intensity for {@code 0.5} seconds on both axes.
   *
   * @param intensity The intensity of the shake. Typically a very small number like {@code 0.05f}.
   */
  public void shake(float intensity) {
    shake(intensity, 0.5f, null, true, FlixelAxes.XY);
  }

  /**
   * Shakes with the given intensity and duration on both axes.
   *
   * @param intensity The intensity of the shake. Typically a very small number like {@code 0.05f}.
   * @param duration How long the shake lasts, in seconds.
   */
  public void shake(float intensity, float duration) {
    shake(intensity, duration, null, true, FlixelAxes.XY);
  }

  /**
   * Applies a screen-shake effect.
   *
   * @param intensity Fraction of the camera size used as the max shake offset distance.
   *   Typically a very small number like {@code 0.05f} or {@code 0.01f}.
   * @param duration How long the shake lasts, in seconds.
   * @param onComplete Callback invoked when the shake finishes, or {@code null}.
   * @param force If {@code true}, resets any currently running shake (default behavior unlike flash/fade).
   * @param axes Which axes to shake on.
   */
  public void shake(float intensity, float duration, Runnable onComplete, boolean force, FlixelAxes axes) {
    if (shakeActive && !force) {
      return;
    }
    shakeActive = true;
    shakeIntensity = intensity;
    shakeDuration = Math.max(duration, 0.001f);
    shakeElapsed = 0f;
    shakeAxes = (axes != null) ? axes : FlixelAxes.XY;
    shakeOnComplete = onComplete;
    shakeOffsetX = 0f;
    shakeOffsetY = 0f;
  }

  /** Stops all screen effects (flash, fade, and shake) on this camera. */
  public void stopFX() {
    stopFlash();
    stopFade();
    stopShake();
  }

  /** Stops the flash effect on this camera. */
  public void stopFlash() {
    flashActive = false;
    flashAlpha = 0f;
  }

  /** Stops the fade effect on this camera. */
  public void stopFade() {
    fadeActive = false;
    fadeAlpha = 0f;
  }

  /** Stops the shake effect on this camera. */
  public void stopShake() {
    shakeActive = false;
    shakeOffsetX = 0f;
    shakeOffsetY = 0f;
  }

  /**
   * Fills the camera display with the specified color using the given batch and a 1x1 white
   * {@link Texture}.
   *
   * @param fillColor The color to fill with (alpha channel is respected).
   * @param blendAlpha Whether to blend the alpha or overwrite previous contents.
   * @param fxAlpha Additional alpha multiplier, from {@code 0.0} to {@code 1.0}.
   * @param batch An active {@link Batch} (must be between {@code begin()} and {@code end()}).
   * @param whitePixel A 1x1 white {@link Texture} used for color drawing.
   */
  public void fill(Color fillColor, boolean blendAlpha, float fxAlpha, Batch batch, Texture whitePixel) {
    float r = fillColor.r;
    float g = fillColor.g;
    float b = fillColor.b;
    float a = fillColor.a * fxAlpha;
    // Use the viewport's actual world dimensions rather than the design dimensions so that
    // viewports like ExtendViewport which extend the visible area beyond the design size
    // are fully covered. camera.position is in view space (getVisibleWidth/2 + shake offset),
    // so the fill rect is derived from it to stay correctly anchored.
    float fw = viewport.getWorldWidth() / zoom;
    float fh = viewport.getWorldHeight() / zoom;
    float fx = camera.position.x - fw / 2f;
    float fy = camera.position.y - fh / 2f;
    if (blendAlpha) {
      if (a <= 0f) {
        return;
      }
      batch.setColor(r, g, b, a);
      batch.draw(whitePixel, fx, fy, fw, fh);
    } else {
      boolean wasBlending = batch.isBlendingEnabled();
      batch.disableBlending();
      batch.setColor(r, g, b, a);
      batch.draw(whitePixel, fx, fy, fw, fh);
      if (wasBlending) {
        batch.enableBlending();
      }
    }
    batch.setColor(Color.WHITE);
  }

  /**
   * Draws active screen effects (flash and fade overlays) using the given batch. Call this after
   * drawing all game objects but before {@code batch.end()}.
   *
   * @param batch An active {@link Batch} (must be between begin/end).
   * @param whitePixel A 1x1 white {@link Texture} used for color drawing.
   */
  public void drawFX(Batch batch, Texture whitePixel) {
    if ((!flashActive || flashAlpha <= 0f) && !fadeActive && fadeAlpha <= 0f) {
      return;
    }
    float fw = viewport.getWorldWidth() / zoom;
    float fh = viewport.getWorldHeight() / zoom;
    float fx = camera.position.x - fw / 2f;
    float fy = camera.position.y - fh / 2f;
    if (flashActive && flashAlpha > 0f) {
      batch.setColor(flashColor.r, flashColor.g, flashColor.b, flashAlpha * alpha);
      batch.draw(whitePixel, fx, fy, fw, fh);
    }
    if (fadeActive || fadeAlpha > 0f) {
      batch.setColor(fadeColor.r, fadeColor.g, fadeColor.b, fadeAlpha * alpha);
      batch.draw(whitePixel, fx, fy, fw, fh);
    }
    batch.setColor(Color.WHITE);
  }

  /**
   * Checks whether this camera's display area contains the given point in screen coordinates.
   *
   * @param px Screen-space X to test.
   * @param py Screen-space Y to test.
   * @return {@code true} if the point is inside the camera display.
   */
  public boolean containsPoint(float px, float py) {
    return containsPoint(px, py, 0, 0);
  }

  /**
   * Checks whether this camera's display area overlaps a rectangle at the given screen position.
   *
   * @param px Bottom-left X of the rectangle in screen coordinates.
   * @param py Bottom-left Y of the rectangle in screen coordinates.
   * @param w Width of the rectangle.
   * @param h Height of the rectangle.
   * @return {@code true} if any part of the rectangle overlaps the camera display.
   */
  public boolean containsPoint(float px, float py, float w, float h) {
    return px + w > x
        && px < x + this.width
        && py + h > y
        && py < y + this.height;
  }

  /**
   * Checks whether this camera's display area overlaps the given rectangle in screen coordinates.
   *
   * @param rect The rectangle to test.
   * @return {@code true} if the rectangle overlaps the camera display.
   */
  public boolean containsRect(Rectangle rect) {
    return containsPoint(rect.x, rect.y, rect.width, rect.height);
  }

  /**
   * Converts a world X coordinate into this camera's view (batch) X coordinate. Parallax uses the
   * object's scroll factor on {@link #scrollX} only; zoom is handled by {@link #getViewMarginX()}.
   *
   * @param worldX World-space X.
   * @param scrollFactor Parallax factor ({@code 1} = moves fully with the camera).
   * @return View-space X (the same space used by {@link FlixelSprite#draw} before libGDX projection).
   */
  public float worldToViewX(float worldX, float scrollFactor) {
    return worldX - scrollX * scrollFactor - getViewMarginX();
  }

  /**
   * Converts a world Y coordinate into this camera's view (batch) Y coordinate.
   *
   * @param worldY World-space Y.
   * @param scrollFactor Parallax factor ({@code 1} = moves fully with the camera).
   * @return View-space Y.
   * @see #worldToViewX(float, float)
   */
  public float worldToViewY(float worldY, float scrollFactor) {
    return worldY - scrollY * scrollFactor - getViewMarginY();
  }

  /**
   * Sets the zoom level. {@code 1} = 1:1, {@code 2} = 2x magnification (world appears larger).
   * Cameras always zoom toward their center. {@link #getViewWidth()} and margins update with zoom;
   * {@link #worldToViewX(float, float)} and {@link #worldToViewY(float, float)} keep parallax
   * and foreground objects aligned. Scroll is not auto-adjusted here (same idea as HaxeFlixel's
   * {@code set_zoom}).
   *
   * @param zoom The new zoom level. Non-positive values fall back to {@link #defaultZoom}.
   */
  public void setZoom(float zoom) {
    this.zoom = (zoom <= 0f) ? defaultZoom : zoom;
    applyZoom();
    if (target != null && style != null && style != FollowStyle.NO_DEAD_ZONE) {
      updateDeadzoneForStyle();
    }
  }

  /**
   * Changes the zoom level by the given delta.
   *
   * @param amount The amount to add to the current zoom level.
   */
  public void changeZoom(float amount) {
    setZoom(getZoom() + amount);
  }

  /**
   * Restores scroll and zoom together without re-running follow or deadzone setup. Used when
   * leaving debug pause so inspect-tool mutations can be reverted exactly as they were before.
   *
   * @param scrollX World scroll X to restore.
   * @param scrollY World scroll Y to restore.
   * @param zoomLevel Zoom to restore. Non-positive values fall back to {@link #defaultZoom}.
   */
  public void restoreScrollAndZoom(float scrollX, float scrollY, float zoomLevel) {
    if (zoomLevel <= 0f) {
      zoomLevel = defaultZoom;
    }
    this.zoom = zoomLevel;
    this.scrollX = scrollX;
    this.scrollY = scrollY;
    applyZoom();
    if (target != null && style != null && style != FollowStyle.NO_DEAD_ZONE) {
      updateDeadzoneForStyle();
    }
  }

  /**
   * Sets the zoom-based scale of this camera. Because cameras use a single uniform zoom value,
   * this assigns zoom to the average of {@code scaleX} and {@code scaleY}.
   *
   * @param scaleX The desired horizontal scale.
   * @param scaleY The desired vertical scale.
   */
  public void setScale(float scaleX, float scaleY) {
    setZoom((scaleX + scaleY) / 2f);
  }

  /**
   * Copies the bounds, follow target, deadzone info, and scroll from another camera.
   *
   * @param other The camera to copy from.
   * @return This camera for chaining.
   */
  public FlixelCamera copyFrom(FlixelCamera other) {
    x = other.x;
    y = other.y;
    width = other.width;
    height = other.height;
    useSubScreenViewport = other.useSubScreenViewport;
    centerCameraOnResize = other.centerCameraOnResize;
    regionMode = other.regionMode;
    hasCustomPixelRegion = other.hasCustomPixelRegion;
    regionX = other.regionX;
    regionY = other.regionY;
    regionWidth = other.regionWidth;
    regionHeight = other.regionHeight;
    normalizedRegionX = other.normalizedRegionX;
    normalizedRegionY = other.normalizedRegionY;
    normalizedRegionWidth = other.normalizedRegionWidth;
    normalizedRegionHeight = other.normalizedRegionHeight;
    scrollX = other.scrollX;
    scrollY = other.scrollY;

    target = other.target;
    targetOffsetX = other.targetOffsetX;
    targetOffsetY = other.targetOffsetY;
    followLeadX = other.followLeadX;
    followLeadY = other.followLeadY;
    followLerp = other.followLerp;
    style = other.style;
    deadzone = (other.deadzone != null) ? new Rectangle(other.deadzone) : null;

    minScrollX = other.minScrollX;
    maxScrollX = other.maxScrollX;
    minScrollY = other.minScrollY;
    maxScrollY = other.maxScrollY;

    setZoom(other.zoom);
    return this;
  }

  /**
   * Called by the game's front-end on window resize. Repositions and resizes the internal viewport.
   */
  public void onResize() {
    update(resolveActualScreenWidth(), resolveActualScreenHeight(), centerCameraOnResize);
  }

  /**
   * Cleans up this camera's state, stopping all effects and clearing the follow target.
   */
  @Override
  public void destroy() {
    super.destroy();
    stopFX();
    disposeFbo();
    target = null;
    deadzone = null;
    shader = null;
    flashOnComplete = null;
    fadeOnComplete = null;
    shakeOnComplete = null;
  }

  private void initFbo() {
    disposeFbo();
    fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
    fboRegion = new TextureRegion(fbo.getColorBufferTexture());
    fboRegion.flip(false, true);
  }

  private void disposeFbo() {
    if (fbo != null) {
      fbo.dispose();
      fbo = null;
      fboRegion = null;
    }
  }

  private void updateFlash(float elapsed) {
    if (!flashActive) {
      return;
    }
    flashElapsed += elapsed;
    flashAlpha = 1f - (flashElapsed / flashDuration);
    if (flashAlpha <= 0f) {
      flashAlpha = 0f;
      flashActive = false;
      if (flashOnComplete != null) {
        flashOnComplete.run();
      }
    }
  }

  private void updateFade(float elapsed) {
    if (!fadeActive) {
      return;
    }
    fadeElapsed += elapsed;
    float progress = fadeElapsed / fadeDuration;
    fadeAlpha = fadeIn ? (1f - progress) : progress;
    if (progress >= 1f) {
      fadeAlpha = fadeIn ? 0f : 1f;
      fadeActive = false;
      if (fadeOnComplete != null) {
        fadeOnComplete.run();
      }
    }
  }

  private void updateShake(float elapsed) {
    if (!shakeActive) {
      return;
    }
    shakeElapsed += elapsed;
    if (shakeElapsed >= shakeDuration) {
      shakeActive = false;
      shakeOffsetX = 0f;
      shakeOffsetY = 0f;
      if (shakeOnComplete != null) {
        shakeOnComplete.run();
      }
      return;
    }

    float sx = (shakeAxes == FlixelAxes.Y) ? 0 : (MathUtils.random(-1f, 1f) * shakeIntensity * width);
    float sy = (shakeAxes == FlixelAxes.X) ? 0 : (MathUtils.random(-1f, 1f) * shakeIntensity * height);

    if (pixelPerfectShake || pixelPerfectRender) {
      sx = Math.round(sx);
      sy = Math.round(sy);
    }

    shakeOffsetX = sx;
    shakeOffsetY = sy;
  }

  private void applyZoom() {
    if (camera instanceof OrthographicCamera ortho) {
      ortho.zoom = 1f / zoom;
    }
  }

  private float followScrollFactorX(FlixelPositional t) {
    float s = t.getScrollX();
    return (s > 0f && !Float.isNaN(s)) ? s : 1f;
  }

  private float followScrollFactorY(FlixelPositional t) {
    float s = t.getScrollY();
    return (s > 0f && !Float.isNaN(s)) ? s : 1f;
  }

  private float scrollXForFollowCenter(float tx, float sx) {
    return (tx - getViewMarginX() - getViewWidth() * 0.5f) / sx;
  }

  private float scrollYForFollowCenter(float ty, float sy) {
    return (ty - getViewMarginY() - getViewHeight() * 0.5f) / sy;
  }

  private float scrollXForFollowEdge(float numerator, float sx) {
    return (numerator - getViewMarginX()) / sx;
  }

  private float scrollYForFollowEdge(float numerator, float sy) {
    return (numerator - getViewMarginY()) / sy;
  }

  /**
   * Runs camera follow math using the same view-space contract as {@link #worldToViewX(float, float)}
   * and {@link FlixelSprite#draw}.
   *
   * @param elapsed Seconds that have elapsed since the last frame.
   */
  private void updateFollow(float elapsed) {
    if (target == null) {
      return;
    }

    float fsx = followScrollFactorX(target);
    float fsy = followScrollFactorY(target);

    float tx = target.getX() + target.getWidth() / 2f + targetOffsetX + followLeadX;
    float ty = target.getY() + target.getHeight() / 2f + targetOffsetY + followLeadY;

    float desiredX = scrollXForFollowCenter(tx, fsx);
    float desiredY = scrollYForFollowCenter(ty, fsy);

    if (followLerp >= 1.0f) {
      scrollX = desiredX;
      scrollY = desiredY;
      return;
    }

    if (deadzone != null) {
      float dzLeft = scrollX + deadzone.x;
      float dzRight = dzLeft + deadzone.width;
      float dzTop = scrollY + deadzone.y;
      float dzBottom = dzTop + deadzone.height;

      if (tx < dzLeft) {
        desiredX = scrollXForFollowEdge(tx - deadzone.x, fsx);
      } else if (tx > dzRight) {
        desiredX = scrollXForFollowEdge(tx - deadzone.x - deadzone.width, fsx);
      } else {
        desiredX = scrollX;
      }

      if (ty < dzTop) {
        desiredY = scrollYForFollowEdge(ty - deadzone.y, fsy);
      } else if (ty > dzBottom) {
        desiredY = scrollYForFollowEdge(ty - deadzone.y - deadzone.height, fsy);
      } else {
        desiredY = scrollY;
      }
    }

    float lerpFactor = 1f - (float) Math.pow(1f - followLerp, elapsed * 60f);
    scrollX = MathUtils.lerp(scrollX, desiredX, lerpFactor);
    scrollY = MathUtils.lerp(scrollY, desiredY, lerpFactor);
  }

  private void updateDeadzoneForStyle() {
    if (style == null) {
      return;
    }

    float vw = getViewWidth();
    float vh = getViewHeight();
    float w, h;
    switch (style) {
      case LOCKON -> {
        w = 1;
        h = 1;
      }
      case PLATFORMER -> {
        w = vw / 8f;
        h = vh / 3f;
      }
      case TOPDOWN -> {
        w = vw / 3f;
        h = vh / 3f;
      }
      case TOPDOWN_TIGHT -> {
        w = vw / 8f;
        h = vh / 8f;
      }
      case SCREEN_BY_SCREEN -> {
        w = vw;
        h = vh;
      }
      default -> {
        deadzone = null;
        return;
      }
    }
    deadzone = new Rectangle((vw - w) / 2f, (vh - h) / 2f, w, h);
  }

  private static int resolveWindowWidth() {
    if (Flixel.game != null) {
      return Flixel.getWidth();
    }
    if (Gdx.graphics != null) {
      return Math.max(1, Gdx.graphics.getWidth());
    }
    return 1;
  }

  /**
   * Window height for defaults: {@link Flixel#getHeight()} when a game exists, otherwise
   * {@link com.badlogic.gdx.Graphics#getHeight()}.
   */
  private static int resolveWindowHeight() {
    if (Flixel.game != null) {
      return Flixel.getHeight();
    }
    if (Gdx.graphics != null) {
      return Math.max(1, Gdx.graphics.getHeight());
    }
    return 1;
  }

  /**
   * Returns the actual screen width in pixels from {@link Graphics}, falling back to
   * {@link #resolveWindowWidth()} when no GL context exists (e.g. unit tests).
   *
   * <p>This differs from {@link #resolveWindowWidth()}, which returns the game's design width.
   * Use this method wherever a viewport needs the real screen dimensions, not the design resolution.
   */
  private static int resolveActualScreenWidth() {
    if (Gdx.graphics != null) {
      return Math.max(1, Gdx.graphics.getWidth());
    }
    return resolveWindowWidth();
  }

  /**
   * Returns the actual screen height in pixels from {@link Graphics}, falling back to
   * {@link #resolveWindowHeight()} when no GL context exists (e.g. unit tests).
   *
   * @see #resolveActualScreenWidth()
   */
  private static int resolveActualScreenHeight() {
    if (Gdx.graphics != null) {
      return Math.max(1, Gdx.graphics.getHeight());
    }
    return resolveWindowHeight();
  }

  /**
   * Fits the world into {@link #width}x{@link #height} pixels, then places that rectangle at
   * {@link #x},{@link #y} (top-left origin, Y down, converted to libGDX bottom-left for
   * {@code glViewport}).
   */
  private void updateSubScreenViewport(int screenWidth, int screenHeight, boolean centerCamera) {
    resolveScreenRegionTopLeft(screenWidth, screenHeight, tmpRect);
    int rx = Math.round(tmpRect.x);
    int ryTop = Math.round(tmpRect.y);
    int rw = Math.max(1, Math.round(tmpRect.width));
    int rh = Math.max(1, Math.round(tmpRect.height));
    int regionBottomY = screenHeight - ryTop - rh;

    viewport.update(rw, rh, centerCamera);
    int fittedX = viewport.getScreenX();
    int fittedY = viewport.getScreenY();
    int fittedW = viewport.getScreenWidth();
    int fittedH = viewport.getScreenHeight();

    viewport.setScreenBounds(
        rx + fittedX,
        regionBottomY + fittedY,
        fittedW,
        fittedH);
    viewport.apply(centerCamera);
  }

  private boolean shouldUseSubScreenViewport(int screenWidth, int screenHeight) {
    if (useSubScreenViewport || hasCustomPixelRegion || regionMode != RegionMode.PIXEL_TOP_LEFT) {
      return true;
    }
    FlixelGame game = Flixel.game;
    if (game == null || game.getCameras() == null || game.getCameras().size <= 1) {
      return false;
    }
    boolean coversFullWindow = x <= 0f && y <= 0f && width >= screenWidth && height >= screenHeight;
    if (coversFullWindow) {
      return false;
    }
    boolean horizontalStrip = width < screenWidth && Math.abs(height - screenHeight) <= 1;
    boolean verticalStrip = height < screenHeight && Math.abs(width - screenWidth) <= 1;
    boolean positioned = x != 0f || y != 0f;
    return horizontalStrip || verticalStrip || positioned;
  }

  /**
   * Resolves the desired screen region to top-left pixel coordinates.
   *
   * <p>This method is the single source of truth for how {@link RegionMode} interprets region
   * coordinates. The returned rectangle uses top-left screen origin semantics (Y down). Callers
   * should convert to libGDX bottom-left coordinates right before
   * {@link Viewport#setScreenBounds(int, int, int, int)}.
   */
  private void resolveScreenRegionTopLeft(int screenWidth, int screenHeight, Rectangle out) {
    int resolvedRegionWidth = hasCustomPixelRegion
        ? regionWidth
        : ((width > 0) ? width : screenWidth);
    int resolvedRegionHeight = hasCustomPixelRegion
        ? regionHeight
        : ((height > 0) ? height : screenHeight);
    resolvedRegionWidth = Math.max(1, resolvedRegionWidth);
    resolvedRegionHeight = Math.max(1, resolvedRegionHeight);
    float px = hasCustomPixelRegion ? regionX : x;
    float py = hasCustomPixelRegion ? regionY : y;

    float topLeftX;
    float topLeftY;

    switch (regionMode) {
      case PIXEL_BOTTOM_LEFT -> {
        topLeftX = px;
        topLeftY = screenHeight - py - resolvedRegionHeight;
      }
      case PIXEL_CENTERED -> {
        topLeftX = px - (resolvedRegionWidth / 2f);
        topLeftY = py - (resolvedRegionHeight / 2f);
      }
      case NORMALIZED_RECT -> {
        float nx = MathUtils.clamp(normalizedRegionX, 0f, 1f);
        float ny = MathUtils.clamp(normalizedRegionY, 0f, 1f);
        float nw = MathUtils.clamp(normalizedRegionWidth, 0f, 1f);
        float nh = MathUtils.clamp(normalizedRegionHeight, 0f, 1f);
        float resolvedW = Math.max(1f, nw * screenWidth);
        float resolvedH = Math.max(1f, nh * screenHeight);
        topLeftX = nx * screenWidth;
        topLeftY = ny * screenHeight;
        out.set(topLeftX, topLeftY, resolvedW, resolvedH);
        return;
      }
      default -> {
        topLeftX = px;
        topLeftY = py;
      }
    }

    out.set(topLeftX, topLeftY, resolvedRegionWidth, resolvedRegionHeight);
  }

  /**
   * Assigns a post-processing shader to this camera. When set, the camera renders its scene into
   * an internal {@link FrameBuffer} each frame and then composites the result to screen using
   * the given shader.
   *
   * <p>The shader is NOT owned by the camera. Call {@link FlixelShader#destroy()} yourself
   * when you are done with it. To remove the shader and return to direct rendering, pass
   * {@code null}.
   *
   * <p>Changing the shader recreates the internal framebuffer. Passing the same shader object
   * that is already set is safe but still recreates the framebuffer, so avoid calling this
   * every frame.
   *
   * @param shader The shader to apply as a camera post-processing effect, or {@code null} to
   *   disable post-processing.
   */
  public void setShader(FlixelShader shader) {
    this.shader = shader;
    if (shader != null) {
      initFbo();
    } else {
      disposeFbo();
    }
  }

  /**
   * Returns the shader currently assigned to this camera, or {@code null} if none is set.
   *
   * @return The active {@link FlixelShader}, or {@code null}.
   */
  @Nullable
  public FlixelShader getShader() {
    return shader;
  }

  /**
   * Returns the internal {@link FrameBuffer} used for post-processing, or {@code null} if no
   * shader has been assigned via {@link #setShader(FlixelShader)}.
   *
   * @return The camera's framebuffer, or {@code null}.
   */
  public FrameBuffer getFbo() {
    return fbo;
  }

  /**
   * Returns a Y-flipped {@link TextureRegion} wrapping the internal framebuffer's color texture.
   * Used by {@link FlixelGame} to draw the captured scene through the camera's shader.
   * Returns {@code null} if no shader has been assigned.
   *
   * @return The framebuffer region, or {@code null}.
   */
  public TextureRegion getFboRegion() {
    return fboRegion;
  }

  /** Returns the underlying libGDX {@link Camera} used for projection. */
  public Camera getCamera() {
    return camera;
  }

  /** Returns the underlying libGDX {@link Viewport} used for screen scaling. */
  public Viewport getViewport() {
    return viewport;
  }

  /**
   * Returns the world width as reported by the viewport. May differ from {@link #width} after
   * zoom or when a {@link FitViewport} adds letterbox bars.
   */
  public float getWorldWidth() {
    return viewport.getWorldWidth();
  }

  /**
   * Returns the world height as reported by the viewport. May differ from {@link #height} after
   * zoom or when a {@link FitViewport} adds letterbox bars.
   */
  public float getWorldHeight() {
    return viewport.getWorldHeight();
  }

  /**
   * Returns the visible width of the camera's view in world units, accounting for the current
   * zoom level ({@code width / zoom}).
   */
  public float getViewWidth() {
    return width / zoom;
  }

  /**
   * Returns the visible height of the camera's view in world units, accounting for the current
   * zoom level ({@code height / zoom}).
   */
  public float getViewHeight() {
    return height / zoom;
  }

  /**
   * Returns the left edge of the visible world region in world coordinates.
   * Equivalent to {@link #scrollX} plus the zoom margin.
   */
  public float getViewX() {
    return scrollX + getViewMarginX();
  }

  /**
   * Returns the top edge of the visible world region in world coordinates.
   * Equivalent to {@link #scrollY} plus the zoom margin.
   */
  public float getViewY() {
    return scrollY + getViewMarginY();
  }

  /** Returns the left world coordinate of the visible region. Alias for {@link #getViewX()}. */
  public float getViewLeft() {
    return getViewX();
  }

  /** Returns the top world coordinate of the visible region. Alias for {@link #getViewY()}. */
  public float getViewTop() {
    return getViewY();
  }

  /** Returns the right world coordinate of the visible region. */
  public float getViewRight() {
    return getViewX() + getViewWidth();
  }

  /** Returns the bottom world coordinate of the visible region. */
  public float getViewBottom() {
    return getViewY() + getViewHeight();
  }

  /**
   * Returns {@code true} if an axis-aligned rectangle in view space overlaps this camera's visible
   * region.
   *
   * <p>The coordinates must already be in view space - that is, converted by
   * {@link #worldToViewX(float, float)} and {@link #worldToViewY(float, float)} before being
   * passed here. The check is a simple AABB overlap against {@code [0, viewWidth] x [0, viewHeight]}
   * and does not account for sprite rotation, making it a conservative test that errs on the side
   * of drawing.
   *
   * @param viewX Left edge of the rectangle in view space.
   * @param viewY Bottom edge of the rectangle in view space.
   * @param width Width of the rectangle.
   * @param height Height of the rectangle.
   * @return {@code true} if the rectangle is at least partially visible.
   */
  public boolean isInView(float viewX, float viewY, float width, float height) {
    return viewX + width > 0f && viewX < getViewWidth()
        && viewY + height > 0f && viewY < getViewHeight();
  }

  /**
   * Returns the horizontal margin between the camera buffer edge and the visible view area,
   * in world units. This margin grows as zoom increases above {@code 1}.
   */
  public float getViewMarginX() {
    return (width - getViewWidth()) / 2f;
  }

  /**
   * Returns the vertical margin between the camera buffer edge and the visible view area,
   * in world units.
   */
  public float getViewMarginY() {
    return (height - getViewHeight()) / 2f;
  }

  /** Returns the left view margin in world units. Alias for {@link #getViewMarginX()}. */
  public float getViewMarginLeft() {
    return getViewMarginX();
  }

  /** Returns the right view margin in world units. Alias for {@link #getViewMarginX()}. */
  public float getViewMarginRight() {
    return getViewMarginX();
  }

  /** Returns the top view margin in world units. Alias for {@link #getViewMarginY()}. */
  public float getViewMarginTop() {
    return getViewMarginY();
  }

  /** Returns the bottom view margin in world units. Alias for {@link #getViewMarginY()}. */
  public float getViewMarginBottom() {
    return getViewMarginY();
  }

  /**
   * Returns a rectangle representing the view area within the camera buffer, using view-space
   * coordinates. The returned {@link Rectangle} is an internal temporary instance shared by this
   * camera - copy the result if you need to hold onto it past the current frame.
   */
  public Rectangle getViewMarginRect() {
    return tmpRect.set(getViewMarginLeft(), getViewMarginTop(), getViewWidth(), getViewHeight());
  }

  /** Returns the current zoom level. {@code 1} = 1:1, {@code 2} = 2x magnification. */
  public float getZoom() {
    return zoom;
  }

  /** Returns the horizontal scale, which is equal to the zoom level. */
  public float getScaleX() {
    return zoom;
  }

  /** Returns the vertical scale, which is equal to the zoom level. */
  public float getScaleY() {
    return zoom;
  }

  /** Returns the total horizontal scale. Equivalent to {@link #getScaleX()}. */
  public float getTotalScaleX() {
    return getScaleX();
  }

  /** Returns the total vertical scale. Equivalent to {@link #getScaleY()}. */
  public float getTotalScaleY() {
    return getScaleY();
  }

  /**
   * Sets the screen-space display position of this camera.
   *
   * @param x X position in native screen pixels.
   * @param y Y position in native screen pixels.
   */
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Sets both {@link #width} and {@link #height} of the camera display.
   *
   * @param width The new width in game pixels.
   * @param height The new height in game pixels.
   */
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    if (shader != null) {
      initFbo();
    }
  }

  /**
   * Sets the screen-region mode for this camera. This changes how {@link #x}/{@link #y} and
   * region sizes are interpreted when placing the viewport rectangle on screen. It does not affect
   * world-space object movement, physics, or camera follow math.
   *
   * @param regionMode The new region mode. Must not be {@code null}.
   */
  public void setRegionMode(@NotNull RegionMode regionMode) {
    if (regionMode == null) {
      return;
    }
    this.regionMode = regionMode;
  }

  /** Returns the current {@link RegionMode} used for screen-space viewport placement. */
  public RegionMode getRegionMode() {
    return regionMode;
  }

  /**
   * Sets the camera's screen rectangle in pixels. Interpretation depends on the current
   * {@link #getRegionMode()}: top-left anchored, bottom-left anchored, or center anchored.
   *
   * @param x Region X coordinate in pixels.
   * @param y Region Y coordinate in pixels.
   * @param width Region width in pixels.
   * @param height Region height in pixels.
   */
  public void setScreenRegion(float x, float y, int width, int height) {
    this.regionX = x;
    this.regionY = y;
    this.regionWidth = width;
    this.regionHeight = height;
    this.hasCustomPixelRegion = true;
  }

  /**
   * Sets the camera's screen rectangle in normalized coordinates ({@code 0..1}), relative to the
   * window size. Only used when {@link #getRegionMode()} is {@link RegionMode#NORMALIZED_RECT}.
   * Normalized coordinates use a top-left origin (Y increases downward) for consistency with
   * HaxeFlixel-style layout.
   *
   * @param x Normalized X position ({@code 0..1}).
   * @param y Normalized Y position ({@code 0..1}).
   * @param width Normalized width ({@code 0..1}).
   * @param height Normalized height ({@code 0..1}).
   */
  public void setScreenRegionNormalized(float x, float y, float width, float height) {
    normalizedRegionX = x;
    normalizedRegionY = y;
    normalizedRegionWidth = width;
    normalizedRegionHeight = height;
  }

  /**
   * Clears the custom pixel region set by {@link #setScreenRegion(float, float, int, int)}.
   * After clearing, pixel-based modes fall back to the legacy fields
   * ({@link #x}, {@link #y}, {@link #width}, {@link #height}).
   */
  public void clearScreenRegion() {
    hasCustomPixelRegion = false;
  }

  /** Returns {@code true} if a flash effect is currently active on this camera. */
  public boolean isFlashActive() {
    return flashActive;
  }

  /** Returns {@code true} if a flash effect is currently active on this camera. */
  public boolean getFlashActive() {
    return flashActive;
  }

  /** Returns {@code true} if a fade effect is currently active on this camera. */
  public boolean isFadeActive() {
    return fadeActive;
  }

  /** Returns {@code true} if a fade effect is currently active on this camera. */
  public boolean getFadeActive() {
    return fadeActive;
  }

  /** Returns {@code true} if a shake effect is currently active on this camera. */
  public boolean isShakeActive() {
    return shakeActive;
  }

  /** Returns {@code true} if a shake effect is currently active on this camera. */
  public boolean getShakeActive() {
    return shakeActive;
  }

  /** Returns the current flash overlay color. */
  public Color getFlashColor() {
    return flashColor;
  }

  /** Returns the current flash overlay alpha, from {@code 0.0} to {@code 1.0}. */
  public float getFlashAlpha() {
    return flashAlpha;
  }

  /** Returns the current fade overlay color. */
  public Color getFadeColor() {
    return fadeColor;
  }

  /** Returns the current fade overlay alpha, from {@code 0.0} to {@code 1.0}. */
  public float getFadeAlpha() {
    return fadeAlpha;
  }

  /** {@inheritDoc} */
  @Override
  public int getColor() {
    return Color.rgba8888(color);
  }

  /** {@inheritDoc} */
  @Override
  @NotNull
  public Color getGdxColor() {
    return color;
  }

  /** {@inheritDoc} */
  @Override
  public void setColor(@NotNull Color tint) {
    color.set(tint);
  }

  /** {@inheritDoc} */
  @Override
  public void setColor(@NotNull FlixelColor tint) {
    color.set(tint.getGdxColor());
  }

  /**
   * Sets the background color of this camera.
   *
   * @param tint The background color to set. Must not be {@code null}.
   */
  public void setBgColor(@NotNull Color tint) {
    bgColor.set(tint);
  }

  /**
   * Sets the background color of this camera.
   *
   * @param tint The background color to set. Must not be {@code null}.
   */
  public void setBgColor(@NotNull FlixelColor tint) {
    bgColor.set(tint.getGdxColor());
  }

  /**
   * Factory that creates a libGDX {@link Viewport} for a new {@link FlixelCamera}.
   *
   * <p>Assign a custom implementation to {@link #viewportFactory} to control the default viewport
   * type for all subsequently created cameras. Platform launchers use this to install the
   * appropriate viewport without touching game code. For one-off cameras, pass a {@link Viewport}
   * directly to the constructor instead.
   */
  @FunctionalInterface
  public interface ViewportFactory {

    /**
     * Creates a new {@link Viewport} for a camera with the given design dimensions.
     *
     * @param width The camera's design width in game pixels.
     * @param height The camera's design height in game pixels.
     * @param camera The libGDX camera this viewport will wrap.
     * @return A new Viewport instance. Must not be {@code null}.
     */
    Viewport create(int width, int height, Camera camera);
  }

  /**
   * Determines how a {@link FlixelCamera} follows a {@link FlixelPositional}.
   */
  public enum FollowStyle {

    /**
     * Camera follows the target and keeps it centered on the screen with no dead zone.
     * The camera snaps to the target's position each frame.
     */
    LOCKON,

    /**
     * A horizontally-biased dead zone placed near the bottom of the camera. Useful for
     * platformers to show more of what is ahead and to prevent the camera from moving up and
     * down too frequently.
     */
    PLATFORMER,

    /**
     * The dead zone is centered, allowing free camera movement in all directions. Commonly
     * used in top-down games.
     */
    TOPDOWN,

    /**
     * Like {@link #TOPDOWN} but with a tighter (smaller) dead zone, so the camera follows
     * the target more closely.
     */
    TOPDOWN_TIGHT,

    /**
     * The camera moves in whole-screen increments, jumping once the target leaves the current
     * screen. Good for classic puzzle or arcade games with discrete screen segments.
     */
    SCREEN_BY_SCREEN,

    /**
     * No dead zone. The camera only moves when explicitly scrolled; it does not track the
     * target automatically.
     */
    NO_DEAD_ZONE
  }

  /**
   * Defines how a {@link FlixelCamera}'s screen region coordinates are interpreted when placing
   * its viewport on the window.
   *
   * <p>These modes affect only screen-space camera placement and clipping. They do not change
   * world coordinates, object movement, physics directions, or camera follow logic.
   */
  public enum RegionMode {

    /**
     * Pixel coordinates with a top-left origin (X right, Y down) for the camera region.
     *
     * <p>Before calling libGDX {@link Viewport#setScreenBounds(int, int, int, int)}, FlixelGDX
     * converts this top-left region to libGDX's bottom-left screen bounds. This is also the
     * default region mode.
     *
     * <p>Recommended for users familiar with HaxeFlixel-style screen layout semantics.
     */
    PIXEL_TOP_LEFT,

    /**
     * Pixel coordinates with a bottom-left origin (X right, Y up) for the camera region.
     *
     * <p>This is already in libGDX/OpenGL viewport terms, so conversion to
     * {@link Viewport#setScreenBounds(int, int, int, int)} is direct.
     *
     * <p>Recommended for users who prefer native libGDX viewport coordinate conventions.
     */
    PIXEL_BOTTOM_LEFT,

    /**
     * Pixel coordinates where {@code x/y} represent the region center.
     *
     * <p>FlixelGDX first resolves a top-left rectangle from the center anchor, then converts to
     * libGDX bottom-left screen bounds for {@link Viewport#setScreenBounds(int, int, int, int)}.
     *
     * <p>Recommended for stable split-screen or picture-in-picture placement across
     * resize and maximize events.
     */
    PIXEL_CENTERED,

    /**
     * Normalized region values ({@code 0..1}) relative to current window size, using a top-left
     * origin.
     *
     * <p>The normalized rectangle is converted to pixel top-left coordinates, then converted
     * again to libGDX bottom-left bounds for
     * {@link Viewport#setScreenBounds(int, int, int, int)}.
     *
     * <p>Recommended for resolution-independent camera layouts that scale with window size.
     */
    NORMALIZED_RECT
  }
}
