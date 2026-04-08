/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.text.FlixelText;
import me.stringdotjar.flixelgdx.util.FlixelColor;
import me.stringdotjar.flixelgdx.util.FlixelSpriteUtil;
import me.stringdotjar.flixelgdx.util.FlixelString;
import me.stringdotjar.flixelgdx.util.FlixelStringUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;

/**
 * A UI bar for progress, health, stamina, experience, cooldowns, loading, or any value mapped to a
 * numeric range. It extends {@link FlixelSprite} so you can add instances to a
 * {@link me.stringdotjar.flixelgdx.group.FlixelSpriteGroup}, use sprite transforms (position, scale,
 * rotation, tint, alpha) with the rest of your HUD, and rely on the same camera and lifecycle rules
 * as other sprites.
 *
 * <p><b>Rendering</b>: The bar draws custom geometry with a shared white-pixel texture and optional
 * {@link TextureRegion} backgrounds and fills. It does not use {@link FlixelSprite#loadGraphic}; those
 * entry points are blocked so a bar never accidentally shows a loaded texture on top of the bar UI.
 *
 * <p><b>Value and range</b>: You set a logical range with {@link #setRange(float, float)} and either
 * {@link #setValue(float)} or {@link #setTrack(FloatSupplier)} so the fill updates each frame. Use
 * {@link #setMaxSupplier(FloatSupplier)} when the maximum changes at runtime (for example leveling
 * systems) without calling {@code setRange} manually.
 *
 * <p><b>Fill direction</b>: {@link BarFillDirection} controls whether the fill grows left-to-right,
 * right-to-left, top-to-bottom, or bottom-to-top.
 *
 * <p><b>Smoothing</b>: {@link #setLerp(float)} applies frame-rate independent smoothing to the displayed
 * value so the bar can lag slightly behind the target, similar to camera follow smoothing in
 * {@link me.stringdotjar.flixelgdx.FlixelCamera}.
 *
 * <p><b>Appearance</b>: Solid colors, custom empty and filled regions, optional two-color gradients,
 * optional border, and threshold-based fill colors with optional color smoothing when the fill percent
 * drops. Optional overlay text uses {@link #setText(java.util.function.Consumer)} with a
 * {@link FlixelString} scratch buffer (see that method for why).
 *
 * <p><b>Screen space</b>: With {@link #setScreenSpace(boolean)} {@code true}, the bar is offset by the
 * current draw camera scroll so it stays fixed on screen while the world moves.
 */
public class FlixelBar extends FlixelSprite {

  private static final Comparator<ThresholdStop> THRESHOLD_BY_PERCENT =
    (a, b) -> Float.compare(a.percent, b.percent);

  public static final float DEFAULT_MIN = 0f;
  public static final float DEFAULT_MAX = 100f;

  private float min = DEFAULT_MIN;
  private float max = DEFAULT_MAX;

  private float value = DEFAULT_MIN;
  private float displayedValue = DEFAULT_MIN;

  @Nullable
  private FloatSupplier valueSupplier;

  @Nullable
  private FloatSupplier maxSupplier;

  private BarFillDirection fillDirection = BarFillDirection.LEFT_TO_RIGHT;

  private boolean screenSpace = false;

  // Value smoothing: 1 = snap, lower = smoother.
  private float lerp = 1f;
  private float lastElapsed = 1f / 60f;
  private float cachedTargetFramerate = 60f;

  // Empty/fill rendering configuration.
  private final Color emptyColor = new Color(0f, 0f, 0f, 0.5f);
  private final Color filledColor = new Color(0f, 1f, 0f, 1f);

  @Nullable
  private TextureRegion emptyRegion;
  @Nullable
  private TextureRegion filledRegion;

  // Optional gradient (drawn instead of filledColor/filledRegion when enabled).
  @Nullable
  private Color gradientStart;
  @Nullable
  private Color gradientEnd;
  @Nullable
  private Texture gradientTexture;
  @Nullable
  private TextureRegion gradientRegion;
  private int gradientTexW = 0;
  private int gradientTexH = 0;
  private float lastGradientBasisW = -1f;
  private float lastGradientBasisH = -1f;

  // Border.
  @Nullable
  private Color borderColor;
  private float borderThickness = 0f;

  // Threshold coloring: libGDX Array for indexed hot-path iteration (no per-frame Iterator).
  private boolean thresholdEnabled = false;
  private final Array<ThresholdStop> thresholdStops = new Array<>(true, 8);
  private float thresholdColorLerp = 1f;
  private boolean thresholdSmoothOnDecreaseOnly = true;
  private float lastPercentForThreshold = 1f;
  private final Color thresholdCurrentColor = new Color(Color.WHITE);
  private final Color thresholdDesiredColor = new Color(Color.WHITE);
  private final Color thresholdScratch = new Color(Color.WHITE);

  @Nullable
  private Consumer<FlixelString> textFormatter;
  private final FlixelString overlayTextScratch = new FlixelString(48);
  /**
   * Snapshot of the last label passed to {@link FlixelText#setText}; never the same instance as
   * {@link #overlayTextScratch}.
   */
  private final FlixelString overlayTextLast = new FlixelString(48);

  /**
   * The text object used to display the overlay text.
   */
  public FlixelText text;
  private float textOffsetX = 0f;
  private float textOffsetY = 0f;

  // Per-instance 1x1 texture for rectangle drawing.
  @Nullable
  private Texture whitePixel;

  /**
   * Creates a bar at the given world position with the given hitbox size. No texture is loaded on the
   * sprite; all visuals come from the bar configuration API.
   *
   * @param x Left edge in world space (or screen-anchored space if {@link #setScreenSpace(boolean)} is used).
   * @param y Top edge (Flixel convention: Y down).
   * @param width Bar width in pixels; used as the drawable width and for gradient resolution hints.
   * @param height Bar height in pixels.
   */
  public FlixelBar(float x, float y, float width, float height) {
    super();
    setPosition(x, y);
    updateHitbox(width, height);
    setRange(DEFAULT_MIN, DEFAULT_MAX);
    setValue(DEFAULT_MIN);
    ensureWhitePixel();
  }

  /**
   * @throws UnsupportedOperationException always.
   */
  @Override
  public final FlixelSprite loadGraphic(Texture texture, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException(
      "FlixelBar does not use loadGraphic; use setEmptyColor, setFilledColor, setEmptyGraphic, setFilledGraphic, or setGradient.");
  }

  /**
   * @throws UnsupportedOperationException always.
   */
  @Override
  public final FlixelSprite makeGraphic(int width, int height, Color color) {
    throw new UnsupportedOperationException(
      "FlixelBar does not use makeGraphic; use setEmptyColor, setFilledColor, setEmptyGraphic, setFilledGraphic, or setGradient.");
  }

  /**
   * When {@code true}, each draw adds the current {@link Flixel#getDrawCamera()} scroll to the bar
   * position so the bar stays fixed on the monitor while the camera moves. When {@code false}, the bar
   * uses normal sprite coordinates (moves with the world).
   *
   * @param screenSpace {@code true} to pin to the viewport in screen space; {@code false} for world space.
   * @return {@code this} for chaining.
   */
  public FlixelBar setScreenSpace(boolean screenSpace) {
    this.screenSpace = screenSpace;
    return this;
  }

  /**
   * Sets which edge of the bar is the fill origin and which axis the fill grows along. Changing this
   * may rebuild the internal gradient texture if a gradient is enabled.
   *
   * @param direction One of {@link BarFillDirection}; must not be {@code null}.
   * @return {@code this} for chaining.
   */
  public FlixelBar setFillDirection(@NotNull BarFillDirection direction) {
    this.fillDirection = Objects.requireNonNull(direction);
    rebuildGradientIfNeeded();
    return this;
  }

  @NotNull
  public BarFillDirection getFillDirection() {
    return fillDirection;
  }

  /**
   * Sets the inclusive logical range {@code [min, max]} used to map {@link #getValue()} to fill percent.
   * If {@code max} is less than {@code min}, the two are swapped. Current and displayed values are
   * clamped into the new range.
   *
   * @param min Lower bound of the value range (for example {@code 0} for health).
   * @param max Upper bound (for example max HP). Must define a positive span after ordering for a non-zero fill.
   * @return {@code this} for chaining.
   */
  public FlixelBar setRange(float min, float max) {
    if (max < min) {
      float tmp = min;
      min = max;
      max = tmp;
    }
    this.min = min;
    this.max = max;
    value = clampToRange(value);
    displayedValue = clampToRange(displayedValue);
    return this;
  }

  public float getMin() {
    return min;
  }

  public float getMax() {
    return max;
  }

  /**
   * Supplies a new maximum each frame (for example current max HP from a stats object). When non-null,
   * {@link #update(float)} calls {@link #setRange(float, float)} with the current minimum and the
   * supplied max so the bar stays consistent when max changes without manual range updates.
   *
   * @param maxSupplier {@code null} to use only {@link #setRange(float, float)}; otherwise polled each update.
   * @return {@code this} for chaining.
   */
  public FlixelBar setMaxSupplier(@Nullable FloatSupplier maxSupplier) {
    this.maxSupplier = maxSupplier;
    return this;
  }

  @Nullable
  public FloatSupplier getMaxSupplier() {
    return maxSupplier;
  }

  /**
   * Sets the target value when not using {@link #setTrack(FloatSupplier)}. Ignored while a track
   * supplier is set. If {@link #getLerp()} is {@code 1}, the displayed value snaps immediately;
   * otherwise the displayed value catches up in {@link #update(float)}.
   *
   * @param value Logical value clamped to the current {@link #setRange(float, float)}.
   * @return {@code this} for chaining.
   */
  public FlixelBar setValue(float value) {
    this.value = clampToRange(value);
    if (lerp >= 1f) {
      this.displayedValue = this.value;
    }
    return this;
  }

  public float getValue() {
    return value;
  }

  public float getDisplayedValue() {
    return displayedValue;
  }

  /**
   * When non-null, {@link #update(float)} sets the target value from {@link FloatSupplier#getAsFloat()}
   * each frame (for example {@code player::getHealth}). When {@code null}, {@link #setValue(float)} drives
   * the bar. Primitive supplier avoids boxing.
   *
   * @param supplier {@code null} for manual values; otherwise the polled value source.
   * @return {@code this} for chaining.
   */
  public FlixelBar setTrack(@Nullable FloatSupplier supplier) {
    this.valueSupplier = supplier;
    return this;
  }

  @Nullable
  public FloatSupplier getTrack() {
    return valueSupplier;
  }

  /**
   * Smoothing factor for the <em>value</em> animation: {@code 1} means the displayed fill matches the
   * target immediately; values between {@code 0} and {@code 1} apply exponential smoothing scaled by
   * elapsed time and target framerate (same idea as camera follow lerp in {@link FlixelCamera}).
   * This is separate from {@link #setThresholdSmoothing(float, boolean)} which only affects threshold colors.
   *
   * @param lerp Smoothing amount in {@code [0, 1]}; clamped if out of range.
   * @return {@code this} for chaining.
   */
  public FlixelBar setLerp(float lerp) {
    this.lerp = MathUtils.clamp(lerp, 0f, 1f);
    if (this.lerp >= 1f) {
      this.displayedValue = this.value;
    }
    return this;
  }

  public float getLerp() {
    return lerp;
  }

  /**
   * Sets the tint for the empty (background) strip when no {@link #setEmptyGraphic(TextureRegion)} is set.
   * Clears any empty graphic region so the bar uses solid color for the background again.
   *
   * @param c libGDX color; not null.
   * @return {@code this} for chaining.
   */
  public FlixelBar setEmptyColor(@NotNull Color c) {
    emptyColor.set(Objects.requireNonNull(c));
    emptyRegion = null;
    return this;
  }

  /**
   * Same as {@link #setEmptyColor(Color)} using a {@link FlixelColor} object.
   *
   * @param c Engine color wrapper; not null.
   * @return {@code this} for chaining.
   */
  public FlixelBar setEmptyColor(@NotNull FlixelColor c) {
    return setEmptyColor(Objects.requireNonNull(c).getGdxColor());
  }

  /**
   * Sets the tint for the filled portion when no {@link #setFilledGraphic(TextureRegion)} or gradient is used,
   * unless threshold coloring overrides the fill color. Clears any filled graphic region.
   *
   * @param c LibGDX color; not null.
   * @return {@code this} for chaining.
   */
  public FlixelBar setFilledColor(@NotNull Color c) {
    filledColor.set(Objects.requireNonNull(c));
    filledRegion = null;
    return this;
  }

  /**
   * Same as {@link #setFilledColor(Color)} using {@link FlixelColor}.
   *
   * @param c Engine color wrapper; not null.
   * @return {@code this} for chaining.
   */
  public FlixelBar setFilledColor(@NotNull FlixelColor c) {
    return setFilledColor(Objects.requireNonNull(c).getGdxColor());
  }

  /**
   * Uses a texture region for the full empty background stretched to the bar size. Set {@code null} to fall
   * back to {@link #setEmptyColor(Color)}.
   *
   * @param region Empty-bar art, or {@code null} for solid {@link #setEmptyColor(Color)}.
   * @return {@code this} for chaining.
   */
  public FlixelBar setEmptyGraphic(@Nullable TextureRegion region) {
    this.emptyRegion = region;
    return this;
  }

  /**
   * Uses a texture region for the fill; the bar crops UVs so only a fraction matching the current percent is shown.
   * Set {@code null} to use {@link #setFilledColor(Color)} or {@link #setGradient(Color, Color)}.
   *
   * @param region Fill art, or {@code null} for color or gradient fill.
   * @return {@code this} for chaining.
   */
  public FlixelBar setFilledGraphic(@Nullable TextureRegion region) {
    this.filledRegion = region;
    return this;
  }

  /**
   * Enables a two-color linear gradient for the filled portion along the fill axis. Either argument {@code null}
   * disables the gradient and restores solid or textured fill. Rebuilds an internal gradient texture when size
   * or {@link #setFillDirection(BarFillDirection)} changes.
   *
   * <p>Use this if you need something like a health bar, where at the start of the bar the color is green, and
   * at the end of the bar the color is red.
   *
   * @param start Color at the start of the gradient axis (left or bottom of the fill direction).
   * @param end Color at the end of the gradient axis.
   * @return {@code this} for chaining.
   */
  public FlixelBar setGradient(@Nullable Color start, @Nullable Color end) {
    this.gradientStart = start != null ? new Color(start) : null;
    this.gradientEnd = end != null ? new Color(end) : null;
    rebuildGradientIfNeeded();
    return this;
  }

  /**
   * Same as {@link #setGradient(Color, Color)} using {@link FlixelColor}.
   *
   * @param start Start color, or {@code null} to help disable the gradient.
   * @param end End color, or {@code null} to help disable the gradient.
   * @return {@code this} for chaining.
   */
  public FlixelBar setGradient(@Nullable FlixelColor start, @Nullable FlixelColor end) {
    Color s = start != null ? start.getGdxColor() : null;
    Color e = end != null ? end.getGdxColor() : null;
    return setGradient(s, e);
  }

  /**
   * Draws a simple axis-aligned frame by tinting four rectangles. Pass {@code null} color or non-positive
   * thickness to draw no border (or use {@link #clearBorder()}).
   *
   * @param color Border tint; {@code null} clears the border.
   * @param thickness Width of each border strip in pixels; values below zero are clamped to zero.
   * @return {@code this} for chaining.
   */
  public FlixelBar setBorder(@Nullable Color color, float thickness) {
    this.borderColor = color != null ? new Color(color) : null;
    this.borderThickness = Math.max(0f, thickness);
    return this;
  }

  /**
   * Removes the border drawn by {@link #setBorder(Color, float)}.
   *
   * @return {@code this} for chaining.
   */
  public FlixelBar clearBorder() {
    this.borderColor = null;
    this.borderThickness = 0f;
    return this;
  }

  /**
   * Convenience for two-stop threshold coloring: from {@code lowColor} at {@code lowPercent} up to
   * {@code fullColor} at 100% fill. Replaces any previous threshold stops from
   * {@link #setThresholdStops(Collection)}.
   *
   * @param fullColor Color used when fill percent is at or above the top stop (full bar).
   * @param lowColor Color blended in below {@code lowPercent}.
   * @param lowPercent Fill fraction in {@code [0,1]} where the low color applies; clamped if out of range.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdColors(@NotNull Color fullColor, @NotNull Color lowColor, float lowPercent) {
    Objects.requireNonNull(fullColor);
    Objects.requireNonNull(lowColor);
    lowPercent = MathUtils.clamp(lowPercent, 0f, 1f);

    thresholdStops.clear();
    thresholdStops.add(new ThresholdStop(lowPercent, lowColor));
    thresholdStops.add(new ThresholdStop(1f, fullColor));
    thresholdStops.sort(THRESHOLD_BY_PERCENT);
    thresholdEnabled = true;

    thresholdCurrentColor.set(fullColor);
    thresholdDesiredColor.set(fullColor);
    lastPercentForThreshold = 1f;
    return this;
  }

  /**
   * Replaces threshold stops from a {@link Collection}. Values are copied into an internal
   * {@link Array} and sorted by percent. Null entries are skipped.
   *
   * <p>For {@link List} implementations that also implement {@link RandomAccess}, copying uses index
   * loops and avoids iterator allocation on this (typically rare) call. For a libGDX {@link Array}, use
   * {@link #setThresholdStops(Array)}.
   *
   * @param stops Non-null collection; may be empty to clear thresholds.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdStops(@NotNull Collection<? extends ThresholdStop> stops) {
    Objects.requireNonNull(stops);
    thresholdStops.clear();
    copyThresholdStopsFromCollection(stops);
    sortThresholdStopsAndUpdateEnabled();
    return this;
  }

  /**
   * Same as {@link #setThresholdStops(Collection)} but reads stops from a libGDX {@link Array} by index
   * (no iterator on the source).
   *
   * @param stops Non-null libGDX array; null entries are skipped.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdStops(@NotNull Array<ThresholdStop> stops) {
    Objects.requireNonNull(stops);
    thresholdStops.clear();
    for (int i = 0, n = stops.size; i < n; i++) {
      ThresholdStop s = stops.get(i);
      if (s != null) {
        thresholdStops.add(s);
      }
    }
    sortThresholdStopsAndUpdateEnabled();
    return this;
  }

  /**
   * Disables threshold-based fill coloring so the bar uses {@link #setFilledColor(Color)} or gradient only.
   *
   * @return {@code this} for chaining.
   */
  public FlixelBar clearThresholds() {
    thresholdStops.clear();
    thresholdEnabled = false;
    return this;
  }

  /**
   * Same as {@link #setThresholdSmoothing(float, boolean)} with decrease-only smoothing enabled.
   *
   * @param lerp Smoothing factor in {@code [0,1]} for threshold color transitions.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdSmoothing(float lerp) {
    return setThresholdSmoothing(lerp, true);
  }

  /**
   * When {@code lerp} is below {@code 1}, the displayed threshold color eases toward the target color each
   * frame. If {@code onDecreaseOnly} is {@code true}, smoothing applies when fill percent drops (typical for
   * damage feedback); if {@code false}, color also smooths when the percent increases.
   *
   * @param lerp Smoothing amount in {@code [0,1]}; clamped if out of range.
   * @param onDecreaseOnly {@code true} to smooth mainly on falling health; {@code false} to smooth on any change.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdSmoothing(float lerp, boolean onDecreaseOnly) {
    this.thresholdColorLerp = MathUtils.clamp(lerp, 0f, 1f);
    this.thresholdSmoothOnDecreaseOnly = onDecreaseOnly;
    return this;
  }

  /**
   * Sets an optional label rendered on top of the bar. Each {@link #update(float)}, the given callback
   * receives a cleared {@link FlixelString}; use {@link FlixelString#concat} and {@link FlixelString#set}
   * overloads to build the visible text without allocating a
   * {@link String} each frame. Text is centered on the bar by default; use {@link #setTextOffset(float, float)}
   * to nudge it. Pass {@code null} to remove overlay text.
   *
   * <p><b>Why a {@code Consumer<FlixelString>} and not {@code Supplier<String>}?</b> A supplier that returns
   * a new {@link String} every frame (common with {@link String#format} or {@code +}) allocates on the heap
   * every frame per bar, which adds up quickly on typical JVMs and on TeaVM. This API reuses one
   * {@link FlixelString} per bar, compares the new characters to the last label without building a
   * {@link String} when nothing changed, and only then calls {@link FlixelText#setText(CharSequence)}.
   *
   * @param formatter {@code null} for no overlay; otherwise invoked each update with the shared scratch buffer.
   * @return {@code this} for chaining.
   */
  public FlixelBar setText(@Nullable Consumer<FlixelString> formatter) {
    if (this.textFormatter != formatter) {
      overlayTextLast.clear();
    }
    this.textFormatter = formatter;
    if (formatter == null) {
      text = null;
      overlayTextLast.clear();
      return this;
    }
    if (text == null) {
      text = new FlixelText(0f, 0f, 0f, "", 8);
      text.cameras = cameras;
    }
    return this;
  }

  /**
   * Pixel offset added to the centered text position after {@link #setText(java.util.function.Consumer)}.
   *
   * @param dx Horizontal offset in pixels (positive moves right).
   * @param dy Vertical offset in pixels (positive moves down in Flixel coordinates).
   * @return {@code this} for chaining.
   */
  public FlixelBar setTextOffset(float dx, float dy) {
    this.textOffsetX = dx;
    this.textOffsetY = dy;
    return this;
  }

  /**
   * Updates sprite animation state, then applies max supplier, value tracking, value smoothing, and refreshes
   * overlay text from the {@link #setText(java.util.function.Consumer)} formatter when set.
   *
   * @param elapsed Seconds since last frame; passed to {@link FlixelSprite#update(float)} and smoothing.
   */
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    lastElapsed = elapsed;
    cachedTargetFramerate = resolveTargetFramerate();

    if (maxSupplier != null) {
      float newMax = maxSupplier.getAsFloat();
      if (Float.isFinite(newMax) && !MathUtils.isEqual(max, newMax)) {
        setRange(min, newMax);
      }
    }

    float target = valueSupplier != null ? valueSupplier.getAsFloat() : value;
    if (!Float.isFinite(target)) {
      target = min;
    }
    value = clampToRange(target);

    if (lerp >= 1f) {
      displayedValue = value;
    } else {
      float lerpFactor = resolveFrameRateIndependentLerp(lerp, elapsed);
      displayedValue = MathUtils.lerp(displayedValue, value, lerpFactor);
    }

    if (text != null && textFormatter != null) {
      overlayTextScratch.clear();
      textFormatter.accept(overlayTextScratch);
      if (!FlixelStringUtil.contentEquals(overlayTextScratch, overlayTextLast)) {
        text.setText(overlayTextScratch);
        overlayTextLast.set(overlayTextScratch);
      }
    }
  }

  @Override
  public void draw(Batch batch) {
    if (!isOnDrawCamera()) {
      return;
    }
    ensureWhitePixel();

    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.getCamera();
    float px = getX();
    float py = getY();
    if (screenSpace) {
      if (cam != null) {
        px += cam.scroll.x * getScrollX();
        py += cam.scroll.y * getScrollY();
      }
    } else if (cam != null) {
      px -= cam.scroll.x * getScrollX();
      py -= cam.scroll.y * getScrollY();
    }

    float w = getWidth();
    float h = getHeight();
    float percent = resolvePercent(displayedValue);

    // Background (empty).
    drawFullEmpty(batch, px, py, w, h);

    // Foreground (filled portion).
    drawFilled(batch, px, py, w, h, percent);

    // Border.
    if (borderColor != null && borderThickness > 0f) {
      drawBorder(batch, px, py, w, h, borderColor, borderThickness);
    }

    // Text overlay.
    if (text != null) {
      float oldX = text.getX();
      float oldY = text.getY();
      float tcx = getX() + w / 2f + textOffsetX;
      float tcy = getY() + h / 2f + textOffsetY;

      // Logical center so FlixelText applies the same scrollFactor as other sprites (matches bar quads above).
      text.setPosition(tcx - text.getWidth() / 2f, tcy - text.getHeight() / 2f);
      text.cameras = cameras;
      text.draw(batch);
      text.setPosition(oldX, oldY);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    if (gradientTexture != null) {
      gradientTexture.dispose();
      gradientTexture = null;
      gradientRegion = null;
    }
    whitePixel = null;
    text = null;
    textFormatter = null;
  }

  private void ensureWhitePixel() {
    if (whitePixel != null) {
      return;
    }
    whitePixel = FlixelSpriteUtil.obtainWhitePixelTexture(Flixel.ensureAssets());
  }

  private void drawFullEmpty(Batch batch, float x, float y, float w, float h) {
    if (emptyRegion != null) {
      batch.setColor(Color.WHITE);
      batch.draw(emptyRegion, x, y, w, h);
      batch.setColor(Color.WHITE);
      return;
    }
    batch.setColor(emptyColor);
    batch.draw(Objects.requireNonNull(whitePixel), x, y, w, h);
    batch.setColor(Color.WHITE);
  }

  private void drawFilled(Batch batch, float x, float y, float w, float h, float percent) {
    if (percent <= 0f) {
      return;
    }
    percent = MathUtils.clamp(percent, 0f, 1f);

    if (gradientStart != null && gradientEnd != null) {
      rebuildGradientIfNeeded();
    }

    TextureRegion regionToDraw = resolveFilledRegionForCurrentSettings();
    if (regionToDraw == null) {
      batch.setColor(resolveFilledColorForCurrentSettings(percent));
      drawFilledRect(batch, x, y, w, h, percent);
      batch.setColor(Color.WHITE);
      return;
    }

    batch.setColor(resolveFilledColorForCurrentSettings(percent));
    drawFilledRegion(batch, regionToDraw, x, y, w, h, percent);
    batch.setColor(Color.WHITE);
  }

  @Nullable
  private TextureRegion resolveFilledRegionForCurrentSettings() {
    if (gradientRegion != null) {
      return gradientRegion;
    }
    return filledRegion;
  }

  private Color resolveFilledColorForCurrentSettings(float percent) {
    if (gradientRegion != null) {
      return Color.WHITE;
    }
    if (!thresholdEnabled || thresholdStops.size == 0) {
      return filledColor;
    }

    // Compute desired threshold color.
    thresholdDesiredColor.set(sampleThresholdColorIntoScratch(percent));

    boolean shouldSmooth = thresholdColorLerp < 1f;
    if (thresholdSmoothOnDecreaseOnly) {
      shouldSmooth = shouldSmooth && percent < lastPercentForThreshold;
    }

    if (!shouldSmooth) {
      thresholdCurrentColor.set(thresholdDesiredColor);
    } else {
      float lf = resolveFrameRateIndependentLerp(thresholdColorLerp, lastElapsed);
      thresholdCurrentColor.lerp(thresholdDesiredColor, lf);
    }

    lastPercentForThreshold = percent;
    return thresholdCurrentColor;
  }

  private Color sampleThresholdColorIntoScratch(float percent) {
    percent = MathUtils.clamp(percent, 0f, 1f);

    ThresholdStop prev = null;
    for (int i = 0, n = thresholdStops.size; i < n; i++) {
      ThresholdStop stop = thresholdStops.get(i);
      if (stop.percent >= percent) {
        if (prev == null) {
          thresholdScratch.set(stop.color);
          return thresholdScratch;
        }
        float t = (percent - prev.percent) / Math.max(0.00001f, (stop.percent - prev.percent));
        thresholdScratch.set(prev.color).lerp(stop.color, MathUtils.clamp(t, 0f, 1f));
        return thresholdScratch;
      }
      prev = stop;
    }
    thresholdScratch.set(thresholdStops.get(thresholdStops.size - 1).color);
    return thresholdScratch;
  }

  private void sortThresholdStopsAndUpdateEnabled() {
    thresholdStops.sort(THRESHOLD_BY_PERCENT);
    thresholdEnabled = thresholdStops.size > 0;
  }

  private void copyThresholdStopsFromCollection(Collection<? extends ThresholdStop> stops) {
    if (stops instanceof List<?> list && stops instanceof RandomAccess) {
      int n = list.size();
      for (int i = 0; i < n; i++) {
        Object o = list.get(i);
        if (o instanceof ThresholdStop s) {
          thresholdStops.add(s);
        }
      }
      return;
    }
    for (ThresholdStop s : stops) {
      if (s != null) {
        thresholdStops.add(s);
      }
    }
  }

  private void drawFilledRect(Batch batch, float x, float y, float w, float h, float percent) {
    float fx = x;
    float fy = y;
    float fw = w;
    float fh = h;

    switch (fillDirection) {
      case LEFT_TO_RIGHT -> fw = w * percent;
      case RIGHT_TO_LEFT -> {
        fw = w * percent;
        fx = x + (w - fw);
      }
      case TOP_TO_BOTTOM -> {
        fh = h * percent;
        fy = y + (h - fh);
      }
      case BOTTOM_TO_TOP -> fh = h * percent;
    }

    batch.draw(Objects.requireNonNull(whitePixel), fx, fy, fw, fh);
  }

  private void drawFilledRegion(Batch batch, TextureRegion region, float x, float y, float w, float h, float percent) {
    float fx = x;
    float fy = y;
    float fw = w;
    float fh = h;

    float u = region.getU();
    float v = region.getV();
    float u2 = region.getU2();
    float v2 = region.getV2();

    switch (fillDirection) {
      case LEFT_TO_RIGHT -> {
        fw = w * percent;
        float du = (u2 - u) * percent;
        u2 = u + du;
      }
      case RIGHT_TO_LEFT -> {
        fw = w * percent;
        fx = x + (w - fw);
        float du = (u2 - u) * percent;
        u = u2 - du;
      }
      case TOP_TO_BOTTOM -> {
        fh = h * percent;
        fy = y + (h - fh);
        float dv = (v2 - v) * percent;
        v = v2 - dv;
      }
      case BOTTOM_TO_TOP -> {
        fh = h * percent;
        float dv = (v2 - v) * percent;
        v2 = v + dv;
      }
    }

    Texture tex = region.getTexture();
    batch.draw(tex, fx, fy, fw, fh, u, v, u2, v2);
  }

  private void drawBorder(Batch batch, float x, float y, float w, float h, Color c, float t) {
    t = Math.max(0f, t);
    if (t <= 0f) return;
    batch.setColor(c);
    Texture px = Objects.requireNonNull(whitePixel);
    // Top.
    batch.draw(px, x, y + h - t, w, t);
    // Bottom.
    batch.draw(px, x, y, w, t);
    // Left.
    batch.draw(px, x, y, t, h);
    // Right.
    batch.draw(px, x + w - t, y, t, h);
    batch.setColor(Color.WHITE);
  }

  private float clampToRange(float v) {
    if (!Float.isFinite(v)) {
      return min;
    }
    return MathUtils.clamp(v, min, max);
  }

  private float resolvePercent(float v) {
    float denom = (max - min);
    if (denom <= 0f) {
      return 0f;
    }
    return MathUtils.clamp((v - min) / denom, 0f, 1f);
  }

  private void rebuildGradientIfNeeded() {
    if (gradientStart == null || gradientEnd == null) {
      if (gradientTexture != null) {
        gradientTexture.dispose();
      }
      gradientTexture = null;
      gradientRegion = null;
      gradientTexW = 0;
      gradientTexH = 0;
      lastGradientBasisW = -1f;
      lastGradientBasisH = -1f;
      return;
    }

    // Build a small texture and stretch it. Avoid huge pixmaps for large UI.
    int desiredW = 1;
    int desiredH = 1;

    boolean horizontal = (fillDirection == BarFillDirection.LEFT_TO_RIGHT || fillDirection == BarFillDirection.RIGHT_TO_LEFT);
    if (horizontal) {
      desiredW = Math.max(2, Math.min(256, Math.round(getWidth())));
    } else {
      desiredH = Math.max(2, Math.min(256, Math.round(getHeight())));
    }

    if (MathUtils.isEqual(lastGradientBasisW, getWidth()) && MathUtils.isEqual(lastGradientBasisH, getHeight())
      && gradientTexture != null && desiredW == gradientTexW && desiredH == gradientTexH) {
      return;
    }

    if (gradientTexture != null && desiredW == gradientTexW && desiredH == gradientTexH) {
      return;
    }

    if (gradientTexture != null) {
      gradientTexture.dispose();
    }

    gradientTexture = FlixelSpriteUtil.createLinearGradientTexture(
      desiredW,
      desiredH,
      gradientStart,
      gradientEnd,
      horizontal
    );
    gradientRegion = new TextureRegion(gradientTexture);
    gradientTexW = desiredW;
    gradientTexH = desiredH;
    lastGradientBasisW = getWidth();
    lastGradientBasisH = getHeight();
  }

  private float resolveFrameRateIndependentLerp(float lerp, float elapsed) {
    // Copied conceptually from FlixelCamera.updateFollow. Converts lerp into a per-frame factor.
    elapsed = Math.max(0f, elapsed);
    return 1f - (float) Math.pow(1f - lerp, elapsed * cachedTargetFramerate);
  }

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
   * One entry in a piecewise-linear threshold color ramp. At fill percent {@link #percent} the bar uses
   * {@link #color}, interpolating between stops for values in between.
   *
   * @param percent Fill fraction in {@code [0,1]} where this stop applies.
   * @param color Color at this stop.
   */
  public record ThresholdStop(float percent, Color color) {

    /**
     * @param percent Fill fraction; clamped to {@code [0,1]}.
     * @param color Stop color; copied internally.
     */
    public ThresholdStop(float percent, @NotNull Color color) {
      this.percent = MathUtils.clamp(percent, 0f, 1f);
      this.color = new Color(Objects.requireNonNull(color));
    }
  }

  /**
   * Fill direction for {@link FlixelBar}.
   */
  public enum BarFillDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP
  }
}
