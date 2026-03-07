package me.stringdotjar.flixelgdx.display;

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
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.FlixelObject;

/**
 * The camera class is used to display the game's visuals.
 *
 * <p>
 * By default, one camera is created automatically, that is the same size as the
 * window. You can
 * add more cameras or even replace the main camera using utilities in
 * {@code FlixelGame}.
 *
 * <p>
 * Every camera wraps a libGDX {@link Camera} and {@link Viewport} internally.
 * By default, an
 * {@link OrthographicCamera} and {@link FitViewport} are used, but custom types can be provided
 * via the constructor overloads.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxCamera.html">FlxCamera
 * (HaxeFlixel)</a>
 */
public class FlixelCamera extends FlixelBasic {

  /**
   * Any {@code FlixelCamera} with a zoom of 0 (the default constructor value)
   * will receive this zoom level instead.
   */
  public static float defaultZoom = 1.0f;

  private Camera camera;
  private Viewport viewport;

  /** The alpha value of this camera display (0.0 to 1.0). */
  public float alpha = 1.0f;

  /** The angle of the camera display in degrees. */
  public float angle = 0f;

  /**
   * Whether the camera display is smooth and filtered, or chunky and pixelated.
   */
  public boolean antialiasing = false;

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
   * The basic parallax scrolling values, essentially the camera's top-left corner
   * position
   * in world coordinates. Use {@link #focusOn(Vector2)} to look at a specific
   * world point.
   */
  public final Vector2 scroll = new Vector2();

  /**
   * Lower bound of the camera's scroll on the X axis. {@code null} = unbounded.
   */
  public Float minScrollX;

  /**
   * Upper bound of the camera's scroll on the X axis. {@code null} = unbounded.
   */
  public Float maxScrollX;

  /**
   * Lower bound of the camera's scroll on the Y axis. {@code null} = unbounded.
   */
  public Float minScrollY;

  /**
   * Upper bound of the camera's scroll on the Y axis. {@code null} = unbounded.
   */
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
  private FlxAxes shakeAxes = FlxAxes.XY;
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
    this(0f, 0f, Flixel.getWindowWidth(), Flixel.getWindowHeight(), 0f);
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
   * @param camera A custom libGDX Camera (e.g.
   * {@link com.badlogic.gdx.graphics.PerspectiveCamera}).
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
   * @param viewport A custom libGDX Viewport (e.g.
   * {@link com.badlogic.gdx.utils.viewport.ScreenViewport}).
   */
  public FlixelCamera(int width, int height, Viewport viewport) {
    this(0f, 0f, width, height, 0f, null, viewport);
  }

  /**
   * Creates a camera at the given display position, size, and zoom level using
   * default types.
   *
   * @param x X location of the camera's display in native screen pixels.
   * @param y Y location of the camera's display in native screen pixels.
   * @param width The width of the camera display in game pixels. 0 = window
   * width.
   * @param height The height of the camera display in game pixels. 0 = window
   * height.
   * @param zoom The initial zoom level. 0 = {@link #defaultZoom}.
   */
  public FlixelCamera(float x, float y, int width, int height, float zoom) {
    this(x, y, width, height, zoom, null, null);
  }

  /**
   * Full constructor allowing fully custom libGDX {@link Camera} and
   * {@link Viewport} types.
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
    this.width = (width <= 0) ? Flixel.getWindowWidth() : width;
    this.height = (height <= 0) ? Flixel.getWindowHeight() : height;

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
   * through this camera.
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
    viewport.update(screenWidth, screenHeight, centerCamera);
  }

  /** Returns the world width of the underlying viewport. */
  public float getWorldWidth() {
    return viewport.getWorldWidth();
  }

  /** Returns the world height of the underlying viewport. */
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
   * Tells this camera to follow the given sprite using {@link FollowStyle#LOCKON}
   * and lerp of 1.
   *
   * @param target The sprite to follow. Pass {@code null} to stop following.
   */
  public void follow(FlixelObject target) {
    follow(target, FollowStyle.LOCKON, 1.0f);
  }

  /**
   * Tells this camera to follow the given sprite with the specified style and a
   * lerp of 1.
   *
   * @param target The sprite to follow. Pass {@code null} to stop following.
   * @param style One of the preset {@link FollowStyle} dead zone presets.
   */
  public void follow(FlixelObject target, FollowStyle style) {
    follow(target, style, 1.0f);
  }

  /**
   * Tells this camera to follow the given sprite.
   *
   * @param target The sprite to follow. Pass {@code null} to stop following.
   * @param style One of the preset {@link FollowStyle} dead zone presets.
   * @param lerp How much lag the camera should have. {@code 1.0} = snap, lower
   * = smoother.
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

    float lerpFactor = 1f - (float) Math.pow(1f - followLerp, elapsed * Flixel.getGame().getFramerate());
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
   * for any side
   * to leave it unbounded.
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
   * Clamps the given scroll position to the camera's min/max bounds, modifying it
   * in-place.
   *
   * @param scrollPos The scroll position to restrict.
   * @return The same vector, clamped within bounds.
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

  /** Flashes the given color for 1 second. */
  public void flash(Color color) {
    flash(color, 1f, null, false);
  }

  /** Flashes the given color for the specified duration. */
  public void flash(Color color, float duration) {
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

  /** Fades to the given color over 1 second. */
  public void fade(Color color) {
    fade(color, 1f, false, null, false);
  }

  /** Fades to the given color over the specified duration. */
  public void fade(Color color, float duration) {
    fade(color, duration, false, null, false);
  }

  /** Fades to/from the given color. */
  public void fade(Color color, float duration, boolean fadeIn) {
    fade(color, duration, fadeIn, null, false);
  }

  /**
   * The screen is gradually filled with (or cleared of) this color.
   *
   * @param color The color to fade to/from.
   * @param duration How long the fade takes, in seconds.
   * @param fadeIn {@code true} = fade FROM the color to clear. {@code false}
   * = fade TO the color.
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
    shake(0.05f, 0.5f, null, true, FlxAxes.XY);
  }

  /** Shakes with the given intensity for 0.5 seconds on both axes. */
  public void shake(float intensity) {
    shake(intensity, 0.5f, null, true, FlxAxes.XY);
  }

  /** Shakes with the given intensity and duration on both axes. */
  public void shake(float intensity, float duration) {
    shake(intensity, duration, null, true, FlxAxes.XY);
  }

  /**
   * A simple screen-shake effect.
   *
   * @param intensity Fraction of camera size representing the max shake
   * distance.
   * @param duration How long the shake lasts, in seconds.
   * @param onComplete Callback invoked when the shake finishes, or {@code null}.
   * @param force If {@code true}, resets any currently-running shake
   * (default unlike flash/fade).
   * @param axes Which axes to shake on.
   */
  public void shake(float intensity, float duration, Runnable onComplete, boolean force, FlxAxes axes) {
    if (shakeActive && !force) {
      return;
    }
    shakeActive = true;
    shakeIntensity = intensity;
    shakeDuration = Math.max(duration, 0.001f);
    shakeElapsed = 0f;
    shakeAxes = (axes != null) ? axes : FlxAxes.XY;
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

    float sx = (shakeAxes == FlxAxes.Y) ? 0 : (MathUtils.random(-1f, 1f) * shakeIntensity * width);
    float sy = (shakeAxes == FlxAxes.X) ? 0 : (MathUtils.random(-1f, 1f) * shakeIntensity * height);

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
   * Fills the camera display with the specified color using the given batch and a
   * 1x1 white texture.
   *
   * @param fillColor The color to fill with (an alpha channel is respected).
   * @param blendAlpha Whether to blend the alpha or overwrite previous contents.
   * @param fxAlpha Additional alpha multiplier (0.0 to 1.0).
   * @param batch An active {@link Batch} to draw with (must be between
   * begin/end).
   * @param whitePixel A 1x1 white {@link Texture} used for color drawing.
   */
  public void fill(Color fillColor, boolean blendAlpha, float fxAlpha, Batch batch, Texture whitePixel) {
    float a = blendAlpha ? fillColor.a * fxAlpha : fxAlpha;
    batch.setColor(fillColor.r, fillColor.g, fillColor.b, a);
    batch.draw(whitePixel, scroll.x, scroll.y, getViewWidth(), getViewHeight());
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
   * Checks whether this camera's display area contains the given point (screen
   * coordinates).
   *
   * @param point The point to test.
   * @return {@code true} if the point is inside the camera display.
   */
  public boolean containsPoint(Vector2 point) {
    return containsPoint(point, 0, 0);
  }

  /**
   * Checks whether this camera's display area overlaps a rectangle at the given
   * point.
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

  /** The width of the visible area in world-space, accounting for zoom. */
  public float getViewWidth() {
    return width / zoom;
  }

  /** The height of the visible area in world-space, accounting for zoom. */
  public float getViewHeight() {
    return height / zoom;
  }

  /** The left edge of the visible area in world-space. */
  public float getViewX() {
    return scroll.x + getViewMarginX();
  }

  /** The top edge of the visible area in world-space. */
  public float getViewY() {
    return scroll.y + getViewMarginY();
  }

  /** Alias for {@link #getViewX()}. */
  public float getViewLeft() {
    return getViewX();
  }

  /** Alias for {@link #getViewY()}. */
  public float getViewTop() {
    return getViewY();
  }

  /** The right edge of the visible area in world-space. */
  public float getViewRight() {
    return getViewX() + getViewWidth();
  }

  /** The bottom edge of the visible area in world-space. */
  public float getViewBottom() {
    return getViewY() + getViewHeight();
  }

  /** Margin cut off on each side horizontally by zoom, in world-space. */
  public float getViewMarginX() {
    return (width - getViewWidth()) / 2f;
  }

  /** Margin cut off on each side vertically by zoom, in world-space. */
  public float getViewMarginY() {
    return (height - getViewHeight()) / 2f;
  }

  /** Margin cut off on the left by zoom. */
  public float getViewMarginLeft() {
    return getViewMarginX();
  }

  /** Margin cut off on the right by zoom. */
  public float getViewMarginRight() {
    return getViewMarginX();
  }

  /** Margin cut off on the top by zoom. */
  public float getViewMarginTop() {
    return getViewMarginY();
  }

  /** Margin cut off on the bottom by zoom. */
  public float getViewMarginBottom() {
    return getViewMarginY();
  }

  /**
   * Returns a {@link Rectangle} describing the view margins (position = margin offsets, size = visible area).
   *
   * @return A new rectangle with the margin bounds.
   */
  public Rectangle getViewMarginRect() {
    return tmpRect.set(getViewMarginLeft(), getViewMarginTop(), getViewWidth(), getViewHeight());
  }

  /**
   * Returns the current zoom level. {@code 1} = 1:1, {@code 2} = 2x magnification.
   * Changing zoom affects all view properties ({@link #getViewWidth()}, etc.).
   */
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

  /** The horizontal scale factor derived from zoom. */
  public float getScaleX() {
    return zoom;
  }

  /** The vertical scale factor derived from zoom. */
  public float getScaleY() {
    return zoom;
  }

  /**
   * Product of the camera's {@link #getScaleX()} and the game's scale mode.
   * For a default setup this equals {@link #getScaleX()}.
   */
  public float getTotalScaleX() {
    return getScaleX();
  }

  /**
   * Product of the camera's {@link #getScaleY()} and the game's scale mode.
   * For a default setup this equals {@link #getScaleY()}.
   */
  public float getTotalScaleY() {
    return getScaleY();
  }

  /**
   * Sets the display position of this camera.
   *
   * @param x The new X display position.
   * @param y The new Y display position.
   */
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Sets the zoom-based scale of this camera. Because cameras use a single zoom
   * value, this
   * sets zoom to the average of {@code scaleX} and {@code scaleY}.
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
   * Copies the bounds, follow target, deadzone info, and scroll from another
   * camera.
   *
   * @param other The camera to copy from.
   * @return This camera for chaining.
   */
  public FlixelCamera copyFrom(FlixelCamera other) {
    x = other.x;
    y = other.y;
    width = other.width;
    height = other.height;
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
   * Called by the game front end on window resize. Triggers repositioning of
   * internal display
   * objects.
   */
  public void onResize() {
    viewport.update(
      Flixel.getWindowWidth(),
      Flixel.getWindowHeight(),
      true
    );
  }

  /**
   * Cleans up this camera's state, stopping all effects and clearing the follow
   * target.
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
   * Preset dead zone styles used with
   * {@link #follow(FlixelObject, FollowStyle, float)}.
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

  /** Axes on which an effect (e.g. shake) can operate. */
  public enum FlxAxes {
    X, Y, XY
  }
}
