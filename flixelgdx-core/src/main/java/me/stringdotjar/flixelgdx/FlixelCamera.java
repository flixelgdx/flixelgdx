/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import org.jetbrains.annotations.NotNull;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import me.stringdotjar.flixelgdx.util.FlixelAxes;

/**
 * A powerful camera class that allows you to control the camera's position, zoom, and more.
 *
 * <p>
 * In a full FlixelGDX game, cameras are usually managed by {@link me.stringdotjar.flixelgdx.FlixelGame}.
 * You can also use {@code FlixelCamera} in a plain libGDX {@code ApplicationListener}. When
 * {@link me.stringdotjar.flixelgdx.Flixel#getGame()} is {@code null}, window size and follow framerate fall back to
 * {@link Gdx#graphics} (call {@link #update(int, int, boolean)} from {@code resize} as usual).
 *
 * <p>
 * Every camera wraps a libGDX {@link Camera} and {@link Viewport} internally. By default, an
 * {@link OrthographicCamera} and {@link FitViewport} are used, but custom types can be provided
 * via the constructor overloads.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxCamera.html">FlxCamera (HaxeFlixel)</a>
 */
public class FlixelCamera extends FlixelBasic {

  /**
   * Any {@code FlixelCamera} with a zoom of anything {@code <= 0} (the default constructor value)
   * will receive this zoom level instead.
   */
  public static float defaultZoom = 1.0f;

  /** The underlying libGDX {@link Camera} used for projection. */
  private Camera camera;

  /** The underlying libGDX {@link Viewport} used for screen scaling. */
  private Viewport viewport;

  /** The alpha value of this camera display (0.0 to 1.0). */
  public float alpha = 1.0f;

  /** The angle of the camera display in degrees. */
  public float angle = 0f;

  /** The natural background color of the camera. Defaults to black. */
  public Color bgColor = new Color(Color.BLACK);

  /** The color tint of the camera display. */
  public Color color = new Color(Color.WHITE);

  /** Whether positions of rendered objects are rounded to whole pixels. */
  public boolean pixelPerfectRender = false;

  /**
   * If {@code true}, screen shake offsets will be rounded to whole pixels.
   * If {@code null}, {@link #pixelPerfectRender} is used instead.
   */
  public boolean pixelPerfectShake = false;

  /** Whether to use alpha blending for the camera's background fill. */
  public boolean useBgAlphaBlending = false;

  /**
   * Whether the libGDX viewport should (re-)center the camera when the game window is resized.
   *
   * <p>Split-screen setups often want this to stay enabled (default), matching existing
   * behavior. Disable it if you want strict scroll preservation through resizes.
   */
  public boolean centerCameraOnResize = true;

  /**
   * The X position of this camera's display in native screen pixels.
   * {@link #zoom} does NOT affect this value.
   */
  public float x;

  /**
   * The Y position of this camera's display in native screen pixels.
   * {@link #zoom} does NOT affect this value.
   */
  public float y;

  /** How wide the camera display is, in game pixels. */
  public int width;

  /** How tall the camera display is, in game pixels. */
  public int height;

  /**
   * The basic parallax scrolling values, essentially the camera's top-left corner position in world coordinates.
   * Use {@link #focusOn(Vector2)} to look at a specific world point.
   */
  public final Vector2 scroll = new Vector2();

  /** Lower bound of the camera's scroll on the X axis. {@code null} = unbounded. */
  public Float minScrollX;

  /** Upper bound of the camera's scroll on the X axis. {@code null} = unbounded. */
  public Float maxScrollX;

  /** Lower bound of the camera's scroll on the Y axis. {@code null} = unbounded. */
  public Float minScrollY;

  /** Upper bound of the camera's scroll on the Y axis. {@code null} = unbounded. */
  public Float maxScrollY;

  /**
   * The dead zone rectangle, measured from the camera's upper left corner in game pixels.
   * The camera will always keep the focus object inside this zone unless bumping against
   * scroll bounds. For rapid prototyping, use the preset styles with
   * {@link #follow(FlixelObject, FollowStyle, float)}.
   */
  public Rectangle deadzone;

  /** Used to force the camera to look ahead of the target. */
  public final Vector2 followLead = new Vector2();

  /**
   * The ratio of the distance to the follow target the camera moves per 1/60 sec.
   * {@code 1.0} = snap to target. {@code 0.0} = don't move. Lower values produce
   * smoother motion.
   */
  public float followLerp = 1.0f;

  /** The current follow style. */
  public FollowStyle style = FollowStyle.LOCKON;

  /** The {@link FlixelObject} the camera follows. Set via {@link #follow}. */
  public FlixelObject target;

  /** Offset applied to the follow target's position. */
  public final Vector2 targetOffset = new Vector2();

  /** Camera's initial zoom value, captured at construction time. */
  public final float initialZoom;

  /**
   * When {@code true}, {@link #update(int, int, boolean)} fits this camera into the screen rectangle
   * {@code (x, y, width, height)} (see {@link #x}) instead of the full window. When {@code false}, placement
   * is inferred when {@link Flixel#getGame()} has multiple cameras (horizontal/vertical strips, PiP, etc.).
   */
  public boolean useSubScreenViewport = false;

  /**
   * Controls how this camera interprets its screen region coordinates.
   * This only affects where the camera is placed on the window, not world/object coordinate behavior.
   */
  private RegionMode regionMode = RegionMode.PIXEL_TOP_LEFT;

  /**
   * Whether pixel-based region placement should use explicit region fields instead of x/y/width/height.
   */
  private boolean hasCustomPixelRegion = false;

  /** Explicit pixel region X for screen placement. */
  private float regionX = 0f;

  /** Explicit pixel region Y for screen placement. */
  private float regionY = 0f;

  /** Explicit pixel region width for screen placement. */
  private int regionWidth = 0;

  /** Explicit pixel region height for screen placement. */
  private int regionHeight = 0;

  /** Normalized screen-region X position used by {@link RegionMode#NORMALIZED_RECT} (0..1). */
  private float normalizedRegionX = 0f;

  /** Normalized screen-region Y position used by {@link RegionMode#NORMALIZED_RECT} (0..1). */
  private float normalizedRegionY = 0f;

  /** Normalized screen-region width used by {@link RegionMode#NORMALIZED_RECT} (0..1). */
  private float normalizedRegionWidth = 1f;

  /** Normalized screen-region height used by {@link RegionMode#NORMALIZED_RECT} (0..1). */
  private float normalizedRegionHeight = 1f;

  /**
   * Internal zoom storage. Use {@link #getZoom()} / {@link #setZoom(float)} to access.
   * A value of {@code 1} = 1:1. {@code 2} = 2x zoom in (world appears larger).
   */
  private float zoom;

  private boolean flashActive = false;
  private final Color flashColor = new Color(Color.WHITE);
  private float flashDuration = 1f;
  private float flashElapsed = 0f;
  private float flashAlpha = 0f;
  private Runnable flashOnComplete;

  private boolean fadeActive = false;
  private final Color fadeColor = new Color(Color.BLACK);
  private float fadeDuration = 1f;
  private float fadeElapsed = 0f;
  private float fadeAlpha = 0f;
  private boolean fadeIn = false;
  private Runnable fadeOnComplete;

  private boolean shakeActive = false;
  private float shakeIntensity = 0.05f;
  private float shakeDuration = 0.5f;
  private float shakeElapsed = 0f;
  private FlixelAxes shakeAxes = FlixelAxes.XY;
  private Runnable shakeOnComplete;
  private float shakeOffsetX = 0f;
  private float shakeOffsetY = 0f;

  /** Reusable vector for internal calculations that would otherwise allocate. */
  private final Vector2 tmpVec = new Vector2();

  /** Reusable rectangle for internal calculations that would otherwise allocate. */
  private final Rectangle tmpRect = new Rectangle();

  /**
   * Creates a camera sized to the current window using the default
   * {@link OrthographicCamera} and {@link FitViewport}.
   */
  public FlixelCamera() {
    this(0f, 0f, resolveWindowWidth(), resolveWindowHeight(), 0f);
  }

  /**
   * Creates a camera with the given dimensions using the default camera and
   * viewport types.
   *
   * @param width The width of the camera display in game pixels.
   * @param height The height of the camera display in game pixels.
   */
  public FlixelCamera(int width, int height) {
    this(0f, 0f, width, height, 0f);
  }

  /**
   * Creates a camera with a custom libGDX {@link Camera}, wrapped in a default
   * {@link FitViewport}.
   *
   * @param width The width of the camera display in game pixels.
   * @param height The height of the camera display in game pixels.
   * @param camera A custom libGDX Camera (e.g. {@link com.badlogic.gdx.graphics.PerspectiveCamera}).
   */
  public FlixelCamera(int width, int height, Camera camera) {
    this(0f, 0f, width, height, 0f, camera, null);
  }

  /**
   * Creates a camera with a custom libGDX {@link Viewport}. The camera is
   * extracted from the viewport.
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
   * @param width The width of the camera display in game pixels. 0 = window width.
   * @param height The height of the camera display in game pixels. 0 = window height.
   * @param zoom The initial zoom level. 0 = {@link #defaultZoom}.
   */
  public FlixelCamera(float x, float y, int width, int height, float zoom) {
    this(x, y, width, height, zoom, null, null);
  }

  /**
   * Full constructor allowing fully custom libGDX {@link Camera} and {@link Viewport} types.
   *
   * <p>
   * If {@code viewport} is provided, its camera is used (the {@code camera} parameter is ignored).
   * If only {@code camera} is provided, it is wrapped in a new {@link FitViewport}.
   * If neither is provided, an {@link OrthographicCamera} and {@link FitViewport}
   * are created.
   *
   * @param x X location of the camera's display in native screen pixels.
   * @param y Y location of the camera's display in native screen pixels.
   * @param width The width of the camera display in game pixels. 0 = window width.
   * @param height The height of the camera display in game pixels. 0 = window height.
   * @param zoom The initial zoom level. 0 = {@link #defaultZoom}. 2 = 2x magnification.
   * @param camera Custom libGDX Camera, or {@code null} for a default {@link OrthographicCamera}.
   * @param viewport Custom libGDX Viewport, or {@code null} for a default {@link FitViewport}.
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
      this.viewport = new FitViewport(this.width, this.height, this.camera);
    } else {
      this.camera = new OrthographicCamera(this.width, this.height);
      this.viewport = new FitViewport(this.width, this.height, this.camera);
    }

    this.zoom = (zoom == 0f) ? defaultZoom : zoom;
    this.initialZoom = this.zoom;
    applyZoom();

    update(resolveWindowWidth(), resolveWindowHeight(), centerCameraOnResize);
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
   * Applies the viewport's OpenGL viewport rectangle. Call before rendering
   * through {@code this} camera.
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
  }

  public float getWorldWidth() {
    return viewport.getWorldWidth();
  }

  public float getWorldHeight() {
    return viewport.getWorldHeight();
  }

  /**
   * Updates the camera scroll, follow logic, and active effects.
   * Called once per frame from the game loop.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    if (!active || !exists) {
      return;
    }

    updateFollow(elapsed);
    updateScroll();
    updateFlash(elapsed);
    updateFade(elapsed);
    updateShake(elapsed);

    if (camera instanceof OrthographicCamera ortho) {
      ortho.up.set(0, 1, 0);
      ortho.direction.set(0, 0, -1);
      if (angle != 0f) {
        ortho.rotate(angle);
      }
    }

    float camX = scroll.x + getViewWidth() / 2f + shakeOffsetX;
    float camY = scroll.y + getViewHeight() / 2f + shakeOffsetY;
    camera.position.set(camX, camY, 0);
    camera.update();
  }

  /**
   * Tells this camera to follow the given sprite using {@link FollowStyle#LOCKON} and lerp of {@code 1.0f}.
   *
   * @param target The sprite to follow. Pass {@code null} to stop following.
   */
  public void follow(FlixelObject target) {
    follow(target, FollowStyle.LOCKON, 1.0f);
  }

  /**
   * Tells this camera to follow the given sprite with the specified style and a lerp of {@code 1.0f}.
   *
   * @param target The sprite to follow. Pass {@code null} to stop following.
   * @param style One of the preset {@link FollowStyle} dead zone presets.
   */
  public void follow(FlixelObject target, FollowStyle style) {
    follow(target, style, 1.0f);
  }

  /**
   * Tells {@code this} camera to follow the given sprite.
   *
   * @param target The sprite to follow. Pass {@code null} to stop following.
   * @param style One of the preset {@link FollowStyle} dead zone presets.
   * @param lerp How much lag the camera should have. {@code 1.0f} = snap, lower = smoother.
   */
  public void follow(FlixelObject target, FollowStyle style, float lerp) {
    this.target = target;
    this.style = style;
    this.followLerp = lerp;
    updateDeadzoneForStyle();
  }

  /**
   * Instantly moves the camera so the given world point is centered.
   *
   * @param point The world-space point to focus on.
   */
  public void focusOn(Vector2 point) {
    scroll.set(
      point.x - getViewWidth() / 2f,
      point.y - getViewHeight() / 2f
    );
  }

  /**
   * Snaps the camera to the current {@link #target} position with no easing, then
   * clamps scroll to bounds. Useful after teleporting the target.
   */
  public void snapToTarget() {
    if (target == null) {
      return;
    }
    float tx = target.getX() + target.getWidth() / 2f + targetOffset.x + followLead.x;
    float ty = target.getY() + target.getHeight() / 2f + targetOffset.y + followLead.y;
    focusOn(tmpVec.set(tx, ty));
    updateScroll();
  }

  private void updateFollow(float elapsed) {
    if (target == null) {
      return;
    }

    float tx = target.getX() + target.getWidth() / 2f + targetOffset.x + followLead.x;
    float ty = target.getY() + target.getHeight() / 2f + targetOffset.y + followLead.y;

    float desiredX = tx - getViewWidth() / 2f;
    float desiredY = ty - getViewHeight() / 2f;

    if (followLerp >= 1.0f) {
      scroll.set(desiredX, desiredY);
      return;
    }

    if (deadzone != null) {
      float dzLeft = scroll.x + deadzone.x;
      float dzRight = dzLeft + deadzone.width;
      float dzTop = scroll.y + deadzone.y;
      float dzBottom = dzTop + deadzone.height;

      if (tx < dzLeft) {
        desiredX = tx - deadzone.x;
      } else if (tx > dzRight) {
        desiredX = tx - deadzone.x - deadzone.width;
      } else {
        desiredX = scroll.x;
      }

      if (ty < dzTop) {
        desiredY = ty - deadzone.y;
      } else if (ty > dzBottom) {
        desiredY = ty - deadzone.y - deadzone.height;
      } else {
        desiredY = scroll.y;
      }
    }

    float lerpFactor = 1f - (float) Math.pow(1f - followLerp, elapsed * resolveTargetFramerate());
    scroll.x = MathUtils.lerp(scroll.x, desiredX, lerpFactor);
    scroll.y = MathUtils.lerp(scroll.y, desiredY, lerpFactor);
  }

  private void updateDeadzoneForStyle() {
    if (style == null || style == FollowStyle.NO_DEAD_ZONE) {
      deadzone = null;
      return;
    }

    float w, h;
    switch (style) {
      case LOCKON -> {
        w = 1;
        h = 1;
      }
      case PLATFORMER -> {
        w = width / 8f;
        h = height / 3f;
      }
      case TOPDOWN -> {
        w = width / 3f;
        h = height / 3f;
      }
      case TOPDOWN_TIGHT -> {
        w = width / 8f;
        h = height / 8f;
      }
      case SCREEN_BY_SCREEN -> {
        w = width;
        h = height;
      }
      default -> {
        deadzone = null;
        return;
      }
    }
    deadzone = new Rectangle((width - w) / 2f, (height - h) / 2f, w, h);
  }

  /**
   * Specifies the bounds of where the camera scroll is allowed. Pass {@code null}
   * for any side to leave it unbounded.
   *
   * @param minX Lower X bound (or {@code null}).
   * @param maxX Upper X bound (or {@code null}).
   * @param minY Lower Y bound (or {@code null}).
   * @param maxY Upper Y bound (or {@code null}).
   */
  public void setScrollBounds(Float minX, Float maxX, Float minY, Float maxY) {
    this.minScrollX = minX;
    this.maxScrollX = maxX;
    this.minScrollY = minY;
    this.maxScrollY = maxY;
  }

  /**
   * Specifies scroll bounds as a bounding rectangle (typically the level size).
   *
   * @param x Smallest X value (usually 0).
   * @param y Smallest Y value (usually 0).
   * @param w Largest X extent (usually level width).
   * @param h Largest Y extent (usually level height).
   */
  public void setScrollBoundsRect(float x, float y, float w, float h) {
    setScrollBoundsRect(x, y, w, h, false);
  }

  /**
   * Specifies scroll bounds as a bounding rectangle.
   *
   * @param x Smallest X value (usually 0).
   * @param y Smallest Y value (usually 0).
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
   * Clamps the current {@link #scroll} to the configured scroll bounds.
   * Called automatically each frame by {@link #update(float)}.
   */
  public void updateScroll() {
    float vw = getViewWidth();
    float vh = getViewHeight();
    if (minScrollX != null && scroll.x < minScrollX) {
      scroll.x = minScrollX;
    }
    if (maxScrollX != null && scroll.x + vw > maxScrollX) {
      scroll.x = maxScrollX - vw;
    }
    if (minScrollY != null && scroll.y < minScrollY) {
      scroll.y = minScrollY;
    }
    if (maxScrollY != null && scroll.y + vh > maxScrollY) {
      scroll.y = maxScrollY - vh;
    }
  }

  /**
   * Clamps the given scroll position to {@code this} camera's min/max bounds, modifying it in-place.
   *
   * @param scrollPos The scroll position to restrict.
   * @return The same {@link Vector2} passed in, clamped within bounds.
   */
  public Vector2 bindScrollPos(Vector2 scrollPos) {
    float vw = getViewWidth();
    float vh = getViewHeight();
    if (minScrollX != null) {
      scrollPos.x = Math.max(scrollPos.x, minScrollX);
    }
    if (maxScrollX != null) {
      scrollPos.x = Math.min(scrollPos.x, maxScrollX - vw);
    }
    if (minScrollY != null) {
      scrollPos.y = Math.max(scrollPos.y, minScrollY);
    }
    if (maxScrollY != null) {
      scrollPos.y = Math.min(scrollPos.y, maxScrollY - vh);
    }
    return scrollPos;
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
   * @param duration How long the flash takes to fade, in seconds.
   */
  public void flash(@NotNull Color color, float duration) {
    flash(color, duration, null, false);
  }

  /**
   * The screen is filled with this color and gradually returns to normal.
   *
   * @param color The color to flash.
   * @param duration How long the flash takes to fade, in seconds.
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
   * @param duration How long the fade takes to fade, in seconds.
   */
  public void fade(@NotNull Color color, float duration) {
    fade(color, duration, false, null, false);
  }

  /**
   * Fades to/from the given color.
   *
   * @param color The color to fade to/from.
   * @param duration How long the fade takes to fade, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false} = fade TO the color.
   */
  public void fade(@NotNull Color color, float duration, boolean fadeIn) {
    fade(color, duration, fadeIn, null, false);
  }

  /**
   * The screen is gradually filled with (or cleared of) this color.
   *
   * @param color The color to fade to/from.
   * @param duration How long the fade takes, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false} = fade TO the color.
   * @param onComplete Callback invoked when the fade finishes, or {@code null}.
   * @param force If {@code true}, resets any currently-running fade.
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

  /** Shakes with default intensity (0.05) for 0.5 seconds on both axes. */
  public void shake() {
    shake(0.05f, 0.5f, null, true, FlixelAxes.XY);
  }

  /**
   * Shakes with the given intensity for 0.5 seconds on both axes.
   *
   * @param intensity The intensity of the shake. This is typically VERY small numbers like {@code 0.05f} or {@code 0.01f}.
   */
  public void shake(float intensity) {
    shake(intensity, 0.5f, null, true, FlixelAxes.XY);
  }

  /**
   * Shakes with the given intensity and duration on both axes.
   *
   * @param intensity The intensity of the shake. This is typically VERY small numbers like {@code 0.05f} or {@code 0.01f}.
   * @param duration How long the shake lasts, in seconds.
   */
  public void shake(float intensity, float duration) {
    shake(intensity, duration, null, true, FlixelAxes.XY);
  }

  /**
   * A simple screen-shake effect.
   *
   * @param intensity Fraction of camera size representing the max shake distance.
   * This is typically VERY small numbers like {@code 0.05f} or {@code 0.01f}.
   * @param duration How long the shake lasts, in seconds.
   * @param onComplete Callback invoked when the shake finishes, or {@code null}.
   * @param force If {@code true}, resets any currently-running shake (default unlike flash/fade).
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

    boolean pp = pixelPerfectShake || pixelPerfectRender;
    if (pp) {
      sx = Math.round(sx);
      sy = Math.round(sy);
    }

    shakeOffsetX = sx;
    shakeOffsetY = sy;
  }

  /** Stops all screen effects (flash, fade, shake) on this camera. */
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
   * Fills the camera display with the specified color using the given batch and a 1x1 white {@link Texture}.
   *
   * @param fillColor The color to fill with (an alpha channel is respected).
   * @param blendAlpha Whether to blend the alpha or overwrite previous contents.
   * @param fxAlpha Additional alpha multiplier (0.0 to 1.0).
   * @param batch An active {@link Batch} to draw with (must be between {@code begin()} and {@code end()}).
   * @param whitePixel A 1x1 white {@link Texture} used for color drawing.
   */
  public void fill(Color fillColor, boolean blendAlpha, float fxAlpha, Batch batch, Texture whitePixel) {
    float r = fillColor.r;
    float g = fillColor.g;
    float b = fillColor.b;
    float a = fillColor.a * fxAlpha;
    if (blendAlpha) {
      if (a <= 0f) {
        return;
      }
      batch.setColor(r, g, b, a);
      batch.draw(whitePixel, scroll.x, scroll.y, getViewWidth(), getViewHeight());
    } else {
      boolean wasBlending = batch.isBlendingEnabled();
      batch.disableBlending();
      batch.setColor(r, g, b, a);
      batch.draw(whitePixel, scroll.x, scroll.y, getViewWidth(), getViewHeight());
      if (wasBlending) {
        batch.enableBlending();
      }
    }
    batch.setColor(Color.WHITE);
  }

  /**
   * Draws active screen effects (flash and fade overlays) using the given batch.
   * Call this after drawing all game objects but before {@code batch.end()}.
   *
   * @param batch An active {@link Batch} (must be between begin/end).
   * @param whitePixel A 1x1 white {@link Texture} used for color drawing.
   */
  public void drawFX(Batch batch, Texture whitePixel) {
    if (flashActive && flashAlpha > 0f) {
      batch.setColor(flashColor.r, flashColor.g, flashColor.b, flashAlpha * alpha);
      batch.draw(whitePixel, scroll.x, scroll.y, getViewWidth(), getViewHeight());
    }
    if (fadeActive || fadeAlpha > 0f) {
      batch.setColor(fadeColor.r, fadeColor.g, fadeColor.b, fadeAlpha * alpha);
      batch.draw(whitePixel, scroll.x, scroll.y, getViewWidth(), getViewHeight());
    }
    batch.setColor(Color.WHITE);
  }

  /**
   * Checks whether this camera's display area contains the given point (screen coordinates).
   *
   * @param point The point to test.
   * @return {@code true} if the point is inside the camera display.
   */
  public boolean containsPoint(Vector2 point) {
    return containsPoint(point, 0, 0);
  }

  /**
   * Checks whether this camera's display area overlaps a rectangle at the given point.
   *
   * @param point Top-left corner of the rectangle in screen coordinates.
   * @param width Width of the rectangle.
   * @param height Height of the rectangle.
   * @return {@code true} if any part of the rectangle overlaps the camera
   * display.
   */
  public boolean containsPoint(Vector2 point, float width, float height) {
    return point.x + width > x
      && point.x < x + this.width
      && point.y + height > y
      && point.y < y + this.height;
  }

  /**
   * Checks whether this camera's display area overlaps the given rectangle
   * (screen coordinates).
   *
   * @param rect The rectangle to test.
   * @return {@code true} if the rectangle overlaps the camera display.
   */
  public boolean containsRect(Rectangle rect) {
    return containsPoint(tmpVec.set(rect.x, rect.y), rect.width, rect.height);
  }

  public float getViewWidth() {
    return width / zoom;
  }

  public float getViewHeight() {
    return height / zoom;
  }

  public float getViewX() {
    return scroll.x + getViewMarginX();
  }

  public float getViewY() {
    return scroll.y + getViewMarginY();
  }

  public float getViewLeft() {
    return getViewX();
  }

  public float getViewTop() {
    return getViewY();
  }

  public float getViewRight() {
    return getViewX() + getViewWidth();
  }

  public float getViewBottom() {
    return getViewY() + getViewHeight();
  }

  public float getViewMarginX() {
    return (width - getViewWidth()) / 2f;
  }

  public float getViewMarginY() {
    return (height - getViewHeight()) / 2f;
  }

  public float getViewMarginLeft() {
    return getViewMarginX();
  }

  public float getViewMarginRight() {
    return getViewMarginX();
  }

  public float getViewMarginTop() {
    return getViewMarginY();
  }

  public float getViewMarginBottom() {
    return getViewMarginY();
  }

  public Rectangle getViewMarginRect() {
    return tmpRect.set(getViewMarginLeft(), getViewMarginTop(), getViewWidth(), getViewHeight());
  }

  public float getZoom() {
    return zoom;
  }

  /**
   * Sets the zoom level. {@code 1} = 1:1, {@code 2} = 2x magnification (world appears larger).
   * Cameras always zoom toward their center.
   *
   * @param zoom The new zoom level.
   */
  public void setZoom(float zoom) {
    float oldZoom = this.zoom;
    this.zoom = zoom;
    // Keep the center of the view fixed in world space so zoom happens from center, not from the left edge.
    float centerX = scroll.x + width / (2f * oldZoom);
    float centerY = scroll.y + height / (2f * oldZoom);
    scroll.x = centerX - width / (2f * this.zoom);
    scroll.y = centerY - height / (2f * this.zoom);
    applyZoom();
  }

  /**
   * Changes the zoom level by the given amount.
   *
   * @param zoom The amount to change the zoom level by.
   */
  public void changeZoom(float zoom) {
    setZoom(getZoom() + zoom);
  }

  private void applyZoom() {
    if (camera instanceof OrthographicCamera ortho) {
      ortho.zoom = 1f / zoom;
    }
  }

  public float getScaleX() {
    return zoom;
  }

  public float getScaleY() {
    return zoom;
  }

  public float getTotalScaleX() {
    return getScaleX();
  }

  public float getTotalScaleY() {
    return getScaleY();
  }

  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Sets the screen-region mode for this camera.
   *
   * <p>
   * This changes how {@link #x}/{@link #y} and region sizes are interpreted when placing the viewport rectangle
   * on screen. It does not change world-space object movement, physics, or camera follow math.
   *
   * @param regionMode The new region mode. {@code null} is ignored.
   */
  public void setRegionMode(@NotNull RegionMode regionMode) {
    if (regionMode == null) {
      return;
    }
    this.regionMode = regionMode;
  }

  /**
   * Returns the current screen-region mode.
   */
  public RegionMode getRegionMode() {
    return regionMode;
  }

  /**
   * Sets the camera's screen rectangle in pixels.
   *
   * <p>
   * Interpretation depends on {@link #regionMode}:
   * top-left anchored, bottom-left anchored, or center anchored.
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
   * Sets the camera's screen rectangle in normalized coordinates (0..1), relative to window size.
   *
   * <p>
   * This is used only when {@link #regionMode} is {@link RegionMode#NORMALIZED_RECT}.
   * Normalized coordinates use a top-left origin (Y increases downward) for consistency with HaxeFlixel-style layout.
   *
   * @param x Normalized X position (0..1).
   * @param y Normalized Y position (0..1).
   * @param width Normalized width (0..1).
   * @param height Normalized height (0..1).
   */
  public void setScreenRegionNormalized(float x, float y, float width, float height) {
    normalizedRegionX = x;
    normalizedRegionY = y;
    normalizedRegionWidth = width;
    normalizedRegionHeight = height;
  }

  /**
   * Clears the custom pixel region set by {@link #setScreenRegion(float, float, int, int)}.
   *
   * <p>
   * After clearing, pixel-based modes fall back to legacy fields ({@link #x}, {@link #y}, {@link #width}, {@link #height}).
   */
  public void clearScreenRegion() {
    hasCustomPixelRegion = false;
  }

  /**
   * Sets the zoom-based scale of this camera. Because cameras use a single zoom
   * value, this sets zoom to the average of {@code scaleX} and {@code scaleY}.
   *
   * @param scaleX The desired horizontal scale.
   * @param scaleY The desired vertical scale.
   */
  public void setScale(float scaleX, float scaleY) {
    setZoom((scaleX + scaleY) / 2f);
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
    scroll.set(other.scroll);

    target = other.target;
    targetOffset.set(other.targetOffset);
    followLead.set(other.followLead);
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
   * Called by the game's front-end on window resize. Triggers repositioning of internal display objects.
   */
  public void onResize() {
    // Use the same logic as update(...) so x/y/width/height-based split regions remain stable.
    update(resolveWindowWidth(), resolveWindowHeight(), centerCameraOnResize);
  }

  /**
   * Cleans up this camera's state, stopping all effects and clearing the follow target.
   */
  @Override
  public void destroy() {
    stopFX();
    target = null;
    deadzone = null;
    flashOnComplete = null;
    fadeOnComplete = null;
    shakeOnComplete = null;
    super.destroy();
  }

  public boolean isFlashActive() {
    return flashActive;
  }

  public boolean isFadeActive() {
    return fadeActive;
  }

  public boolean isShakeActive() {
    return shakeActive;
  }

  public Color getFlashColor() {
    return flashColor;
  }

  public float getFlashAlpha() {
    return flashAlpha;
  }

  public Color getFadeColor() {
    return fadeColor;
  }

  public float getFadeAlpha() {
    return fadeAlpha;
  }

  /**
   * Window width for defaults: {@link Flixel#getViewWidth()} when {@link Flixel#getGame()} exists, else
   * {@link com.badlogic.gdx.Graphics#getWidth()}.
   */
  private static int resolveWindowWidth() {
    if (Flixel.getGame() != null) {
      return Flixel.getViewWidth();
    }
    if (Gdx.graphics != null) {
      return Math.max(1, Gdx.graphics.getWidth());
    }
    return 1;
  }

  /**
   * Window height for defaults: {@link Flixel#getViewHeight()} when {@link Flixel#getGame()} exists, else
   * {@link com.badlogic.gdx.Graphics#getHeight()}.
   */
  private static int resolveWindowHeight() {
    if (Flixel.getGame() != null) {
      return Flixel.getViewHeight();
    }
    if (Gdx.graphics != null) {
      return Math.max(1, Gdx.graphics.getHeight());
    }
    return 1;
  }

  /** Target updates per second for follow lerp; matches {@link FlixelGame#getFramerate()} when in FlixelGDX. */
  private static float resolveTargetFramerate() {
    FlixelGame game = Flixel.getGame();
    if (game != null) {
      return game.getFramerate();
    }
    if (Gdx.graphics != null) {
      int hz = Gdx.graphics.getDisplayMode().refreshRate;
      if (hz > 0) {
        return hz;
      }
    }
    return 60f;
  }

  /**
   * Fits the world into {@link #width}x{@link #height} pixels, then places that rectangle at {@link #x},{@link #y}
   * (top-left origin, Y down, converted to libGDX bottom-left for {@code glViewport}).
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
    FlixelGame game = Flixel.getGame();
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
   * <p>
   * This method is the single source of truth for how {@link #regionMode} interprets region coordinates.
   * The returned rectangle uses top-left screen origin semantics (Y down). Callers should convert to libGDX
   * bottom-left coordinates right before {@link Viewport#setScreenBounds(int, int, int, int)}.
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
      case PIXEL_TOP_LEFT -> {
        topLeftX = px;
        topLeftY = py;
      }
      default -> {
        topLeftX = px;
        topLeftY = py;
      }
    }

    out.set(topLeftX, topLeftY, resolvedRegionWidth, resolvedRegionHeight);
  }

  /**
   * Determines how a {@link FlixelCamera} follows a {@link FlixelObject}.
   */
  public enum FollowStyle {

    /**
     * Camera follows the target and keeps it centered on the screen (with no dead zone).
     * The camera snaps to the target's position each frame.
     */
    LOCKON,

    /**
     * A horizontally-biased dead zone placed near the bottom of the camera.
     * Useful for platformers to show more of what is ahead and to prevent
     * the camera from moving up and down too frequently.
     */
    PLATFORMER,

    /**
     * The dead zone is centered, allowing free camera movement in all directions,
     * commonly used in top-down games.
     */
    TOPDOWN,

    /**
     * Like TOPDOWN but with a tighter (smaller) dead zone, so the camera
     * follows the target more closely.
     */
    TOPDOWN_TIGHT,

    /**
     * The camera moves in whole-screen increments, or "pages", jumping
     * once the target leaves the current screen. Good for classic puzzle or
     * arcade games with discrete screen segments.
     */
    SCREEN_BY_SCREEN,

    /**
     * No dead zone, the camera only follows the target when explicitly moved;
     * the camera does not track the target automatically.
     */
    NO_DEAD_ZONE
  }

  /**
   * Defines how a {@link FlixelCamera}'s screen region coordinates are interpreted when placing its viewport on the window.
   *
   * <p>
   * These modes affect only screen-space camera placement/clipping. They do not change world coordinates,
   * object movement, physics directions, or camera follow logic.
   */
  public enum RegionMode {

    /**
     * Pixel coordinates with a top-left origin (X right, Y down) for the camera region.
     *
     * <p>
     * Before calling libGDX {@link Viewport#setScreenBounds(int, int, int, int)}, FlixelGDX converts this top-left
     * region to libGDX's bottom-left screen bounds. This is also the default region mode.
     *
     * <p>
     * Recommended for users familiar with HaxeFlixel-style screen layout semantics.
     */
    PIXEL_TOP_LEFT,

    /**
     * Pixel coordinates with a bottom-left origin (X right, Y up) for the camera region.
     *
     * <p>
     * This is already in libGDX/OpenGL viewport terms, so conversion to {@link Viewport#setScreenBounds(int, int, int, int)}
     * is direct.
     *
     * <p>
     * Recommended for users who prefer native libGDX viewport coordinate conventions.
     */
    PIXEL_BOTTOM_LEFT,

    /**
     * Pixel coordinates where {@code x/y} represent the region center.
     *
     * <p>
     * FlixelGDX first resolves a top-left rectangle from the center anchor, then converts to libGDX bottom-left
     * screen bounds for {@link Viewport#setScreenBounds(int, int, int, int)}.
     *
     * <p>
     * Recommended for users who want stable split-screen or picture-in-picture placement across resize/maximize events.
     */
    PIXEL_CENTERED,

    /**
     * Normalized region values (0..1) relative to current window size, using top-left origin semantics.
     *
     * <p>
     * The normalized rectangle is converted to pixel top-left coordinates, then converted again to libGDX bottom-left
     * bounds for {@link Viewport#setScreenBounds(int, int, int, int)}.
     *
     * <p>
     * Recommended for users who want resolution-independent camera layouts that scale with window size.
     */
    NORMALIZED_RECT
  }
}
