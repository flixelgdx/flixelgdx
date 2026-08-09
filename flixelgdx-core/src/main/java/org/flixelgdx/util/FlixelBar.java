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
package org.flixelgdx.util;


import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.functional.supplier.FloatSupplier;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.text.FlixelText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * A UI bar for progress, health, stamina, experience, cooldowns, loading, or any value mapped to a
 * numeric range. It extends {@link FlixelSprite} so you can add instances to a
 * {@link org.flixelgdx.group.FlixelSpriteGroup FlixelSpriteGroup}, use sprite transforms (position, scale,
 * rotation, tint, alpha) with the rest of your HUD, and rely on the same camera and lifecycle rules
 * as other sprites.
 *
 * <p><b>Rendering</b>: The bar draws custom geometry with a shared white-pixel texture and optional
 * {@link FlixelFrame} backgrounds and fills. It does not use {@link FlixelSprite#loadGraphic}; those
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
 * {@link org.flixelgdx.FlixelCamera FlixelCamera}.
 *
 * <p><b>Appearance</b>: Solid colors, custom empty and filled regions, optional two-color gradients,
 * optional border, and threshold-based fill colors with optional color smoothing when the fill percent
 * drops. Optional overlay text is set with {@link #setText(CharSequence)}; pass a reused
 * {@link FlixelString} for allocation-free live labels (see that method for why).
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

  // Value smoothing: 1 = snap, lower = smoother.
  private float lerp = 1f;
  private float lastElapsed = 1f / 60f;

  // Empty/fill rendering configuration.
  private final FlixelColor emptyColor = new FlixelColor(0f, 0f, 0f, 0.5f);
  private final FlixelColor filledColor = new FlixelColor(0f, 1f, 0f, 1f);

  @Nullable
  private FlixelFrame emptyRegion;
  @Nullable
  private FlixelFrame filledRegion;

  // Optional gradient (drawn instead of filledColor/filledRegion when enabled).
  @Nullable
  private FlixelColor gradientStart;
  @Nullable
  private FlixelColor gradientEnd;
  @Nullable
  private FlixelTexture gradientTexture;
  @Nullable
  private FlixelFrame gradientRegion;
  private int gradientTexW = 0;
  private int gradientTexH = 0;
  private float lastGradientBasisW = -1f;
  private float lastGradientBasisH = -1f;

  // Border.
  @Nullable
  private FlixelColor borderColor;
  private float borderThickness = 0f;

  private final FlixelArray<ThresholdStop> thresholdStops = new FlixelArray<>(true, 8);
  private float thresholdColorLerp = 1f;
  private float lastPercentForThreshold = 1f;
  private final FlixelColor thresholdCurrentColor = new FlixelColor(FlixelColor.WHITE);
  private final FlixelColor thresholdDesiredColor = new FlixelColor(FlixelColor.WHITE);
  private final FlixelColor thresholdScratch = new FlixelColor(FlixelColor.WHITE);

  /** Current overlay label source, or {@code null} for none. May be a live {@link FlixelString} you mutate. */
  @Nullable
  private CharSequence overlayText;

  /**
   * Snapshot of the last label pushed to {@link FlixelText#setText}; used to skip redundant updates when the
   * overlay text has not changed. Never the same instance as {@link #overlayText}.
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
  private FlixelFrame whitePixel;

  private boolean screenSpace = false;
  private boolean thresholdEnabled = false;
  private boolean thresholdSmoothOnDecreaseOnly = true;

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

  @Override
  public final FlixelSprite loadGraphic(FlixelTexture texture, int frameWidth, int frameHeight) {
    throw new UnsupportedOperationException(
        "FlixelBar does not use loadGraphic; use setEmptyColor, setFilledColor, setEmptyGraphic, setFilledGraphic, or setGradient.");
  }

  @Override
  public final FlixelSprite makeGraphic(int width, int height, @NotNull FlixelColor color) {
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
    this.lerp = FlixelMath.clamp(lerp, 0f, 1f);
    if (this.lerp >= 1f) {
      this.displayedValue = this.value;
    }
    return this;
  }

  public float getLerp() {
    return lerp;
  }

  /**
   * Sets the tint for the empty (background) strip when no {@link #setEmptyGraphic(FlixelFrame)} is set.
   * Clears any empty graphic region so the bar uses solid color for the background again.
   *
   * @param c The color; not null.
   * @return {@code this} for chaining.
   */
  public FlixelBar setEmptyColor(@NotNull FlixelColor c) {
    emptyColor.set(Objects.requireNonNull(c));
    emptyRegion = null;
    return this;
  }

  /**
   * Sets the tint for the filled portion when no {@link #setFilledGraphic(FlixelFrame)} or gradient is used,
   * unless threshold coloring overrides the fill color. Clears any filled graphic region.
   *
   * @param c The color; not null.
   * @return {@code this} for chaining.
   */
  public FlixelBar setFilledColor(@NotNull FlixelColor c) {
    filledColor.set(Objects.requireNonNull(c));
    filledRegion = null;
    return this;
  }

  /**
   * Uses a texture region for the full empty background stretched to the bar size. Set {@code null} to fall
   * back to {@link #setEmptyColor(FlixelColor)}.
   *
   * @param region Empty-bar art, or {@code null} for solid {@link #setEmptyColor(FlixelColor)}.
   * @return {@code this} for chaining.
   */
  public FlixelBar setEmptyGraphic(@Nullable FlixelFrame region) {
    this.emptyRegion = region;
    return this;
  }

  /**
   * Uses a texture region for the fill; the bar crops UVs so only a fraction matching the current percent is shown.
   * Set {@code null} to use {@link #setFilledColor(FlixelColor)} or {@link #setGradient(FlixelColor, FlixelColor)}.
   *
   * @param region Fill art, or {@code null} for color or gradient fill.
   * @return {@code this} for chaining.
   */
  public FlixelBar setFilledGraphic(@Nullable FlixelFrame region) {
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
   * @param start FlixelColor at the start of the gradient axis (left or bottom of the fill direction).
   * @param end FlixelColor at the end of the gradient axis.
   * @return {@code this} for chaining.
   */
  public FlixelBar setGradient(@Nullable FlixelColor start, @Nullable FlixelColor end) {
    this.gradientStart = start != null ? new FlixelColor(start) : null;
    this.gradientEnd = end != null ? new FlixelColor(end) : null;
    rebuildGradientIfNeeded();
    return this;
  }

  /**
   * Draws a simple axis-aligned frame by tinting four rectangles. Pass {@code null} color or non-positive
   * thickness to draw no border (or use {@link #clearBorder()}).
   *
   * @param color Border tint; {@code null} clears the border.
   * @param thickness Width of each border strip in pixels; values below zero are clamped to zero.
   * @return {@code this} for chaining.
   */
  public FlixelBar setBorder(@Nullable FlixelColor color, float thickness) {
    this.borderColor = color != null ? new FlixelColor(color) : null;
    this.borderThickness = Math.max(0f, thickness);
    return this;
  }

  /**
   * Removes the border drawn by {@link #setBorder(FlixelColor, float)}.
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
   * @param fullColor FlixelColor used when fill percent is at or above the top stop (full bar).
   * @param lowColor FlixelColor blended in below {@code lowPercent}.
   * @param lowPercent Fill fraction in {@code [0,1]} where the low color applies; clamped if out of range.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdColors(@NotNull FlixelColor fullColor, @NotNull FlixelColor lowColor, float lowPercent) {
    Objects.requireNonNull(fullColor);
    Objects.requireNonNull(lowColor);
    lowPercent = FlixelMath.clamp(lowPercent, 0f, 1f);

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
   * {@link FlixelArray} and sorted by percent. Null entries are skipped.
   *
   * <p>For {@link List} implementations that also implement {@link RandomAccess}, copying uses index
   * loops and avoids iterator allocation on this (typically rare) call. For a libGDX {@link FlixelArray}, use
   * {@link #setThresholdStops(FlixelArray)}.
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
   * Same as {@link #setThresholdStops(Collection)} but reads stops from a libGDX {@link FlixelArray} by index
   * (no iterator on the source).
   *
   * @param stops Non-null libGDX array; null entries are skipped.
   * @return {@code this} for chaining.
   */
  public FlixelBar setThresholdStops(@NotNull FlixelArray<ThresholdStop> stops) {
    Objects.requireNonNull(stops);
    thresholdStops.clear();
    for (int i = 0, n = stops.getSize(); i < n; i++) {
      ThresholdStop s = stops.get(i);
      if (s != null) {
        thresholdStops.add(s);
      }
    }
    sortThresholdStopsAndUpdateEnabled();
    return this;
  }

  /**
   * Disables threshold-based fill coloring so the bar uses {@link #setFilledColor(FlixelColor)} or gradient only.
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
    this.thresholdColorLerp = FlixelMath.clamp(lerp, 0f, 1f);
    this.thresholdSmoothOnDecreaseOnly = onDecreaseOnly;
    return this;
  }

  /**
   * Sets an optional label rendered on top of the bar. Text is centered on the bar by default; use
   * {@link #setTextOffset(float, float)} to nudge it. Pass {@code null} to remove overlay text.
   *
   * <p>The bar keeps the {@link CharSequence} you pass and re-reads it each {@link #update(float)}, so for
   * <b>live</b> text (a score, a percentage) hand it a {@link FlixelString} that you mutate in place: the bar
   * picks up the new characters without you allocating a fresh {@link String} every frame, compares them to the
   * last label, and only calls {@link FlixelText#setText(CharSequence)} when they actually change. For
   * <b>static</b> text, a plain {@link String} works just as well.
   *
   * @param label {@code null} for no overlay; otherwise the label source (often a reused {@link FlixelString}).
   * @return {@code this} for chaining.
   */
  public FlixelBar setText(@Nullable CharSequence label) {
    this.overlayText = label;
    overlayTextLast.clear();
    if (label == null) {
      text = null;
      return this;
    }
    if (text == null) {
      text = new FlixelText(0f, 0f, 0f, "", 8);
      text.cameras = cameras;
    }
    return this;
  }

  /**
   * Pixel offset added to the centered text position after {@link #setText(CharSequence)}.
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
   * overlay text from the {@link #setText(CharSequence)} source when set.
   *
   * @param elapsed Seconds since last frame; passed to {@link FlixelSprite#update(float)} and smoothing.
   */
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    lastElapsed = elapsed;

    if (maxSupplier != null) {
      float newMax = maxSupplier.getAsFloat();
      if (Float.isFinite(newMax) && !FlixelMath.isEqual(max, newMax)) {
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
      displayedValue = FlixelMath.lerp(displayedValue, value, lerpFactor);
    }

    if (text != null && overlayText != null) {
      if (!FlixelStringUtil.contentEquals(overlayText, overlayTextLast)) {
        text.setText(overlayText);
        overlayTextLast.set(overlayText);
      }
    }
  }

  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!isOnDrawCamera()) {
      return;
    }
    ensureWhitePixel();

    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
    float px = getX();
    float py = getY();
    if (screenSpace) {
      if (cam != null) {
        px += cam.scrollX * getScrollX();
        py += cam.scrollY * getScrollY();
      }
    } else if (cam != null) {
      px = cam.worldToViewX(px, getScrollX());
      py = cam.worldToViewY(py, getScrollY());
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
      gradientTexture.destroy();
      gradientTexture = null;
      gradientRegion = null;
    }
    whitePixel = null;
    text = null;
    overlayText = null;
  }

  private void ensureWhitePixel() {
    if (whitePixel != null) {
      return;
    }
    whitePixel = FlixelSpriteUtil.obtainWhitePixel(Flixel.assets);
  }

  private void drawFullEmpty(FlixelBatch batch, float x, float y, float w, float h) {
    if (emptyRegion != null) {
      batch.setColor(FlixelColor.WHITE);
      batch.draw(emptyRegion, x, y, w, h);
      batch.setColor(FlixelColor.WHITE);
      return;
    }
    batch.setColor(emptyColor);
    batch.draw(Objects.requireNonNull(whitePixel), x, y, w, h);
    batch.setColor(FlixelColor.WHITE);
  }

  private void drawFilled(FlixelBatch batch, float x, float y, float w, float h, float percent) {
    if (percent <= 0f) {
      return;
    }
    percent = FlixelMath.clamp(percent, 0f, 1f);

    if (gradientStart != null && gradientEnd != null) {
      rebuildGradientIfNeeded();
    }

    FlixelFrame regionToDraw = resolveFilledRegionForCurrentSettings();
    if (regionToDraw == null) {
      batch.setColor(resolveFilledColorForCurrentSettings(percent));
      drawFilledRect(batch, x, y, w, h, percent);
      batch.setColor(FlixelColor.WHITE);
      return;
    }

    batch.setColor(resolveFilledColorForCurrentSettings(percent));
    drawFilledRegion(batch, regionToDraw, x, y, w, h, percent);
    batch.setColor(FlixelColor.WHITE);
  }

  @Nullable
  private FlixelFrame resolveFilledRegionForCurrentSettings() {
    if (gradientRegion != null) {
      return gradientRegion;
    }
    return filledRegion;
  }

  private FlixelColor resolveFilledColorForCurrentSettings(float percent) {
    if (gradientRegion != null) {
      return FlixelColor.WHITE;
    }
    if (!thresholdEnabled || thresholdStops.getSize() == 0) {
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

  private FlixelColor sampleThresholdColorIntoScratch(float percent) {
    percent = FlixelMath.clamp(percent, 0f, 1f);

    ThresholdStop prev = null;
    for (int i = 0, n = thresholdStops.getSize(); i < n; i++) {
      ThresholdStop stop = thresholdStops.get(i);
      if (stop.percent >= percent) {
        if (prev == null) {
          thresholdScratch.set(stop.color);
          return thresholdScratch;
        }
        float t = (percent - prev.percent) / Math.max(0.00001f, (stop.percent - prev.percent));
        thresholdScratch.set(prev.color).lerp(stop.color, FlixelMath.clamp(t, 0f, 1f));
        return thresholdScratch;
      }
      prev = stop;
    }
    thresholdScratch.set(thresholdStops.get(thresholdStops.getSize() - 1).color);
    return thresholdScratch;
  }

  private void sortThresholdStopsAndUpdateEnabled() {
    thresholdStops.sort(THRESHOLD_BY_PERCENT);
    thresholdEnabled = thresholdStops.getSize() > 0;
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

  private void drawFilledRect(FlixelBatch batch, float x, float y, float w, float h, float percent) {
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

  private void drawFilledRegion(FlixelBatch batch, FlixelFrame region, float x, float y, float w, float h, float percent) {
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

    FlixelTexture tex = region.getTexture();
    batch.draw(tex, fx, fy, fw, fh, u, v, u2, v2);
  }

  private void drawBorder(FlixelBatch batch, float x, float y, float w, float h, FlixelColor c, float t) {
    t = Math.max(0f, t);
    if (t <= 0f)
      return;
    batch.setColor(c);
    FlixelFrame px = Objects.requireNonNull(whitePixel);
    // Top.
    batch.draw(px, x, y + h - t, w, t);
    // Bottom.
    batch.draw(px, x, y, w, t);
    // Left.
    batch.draw(px, x, y, t, h);
    // Right.
    batch.draw(px, x + w - t, y, t, h);
    batch.setColor(FlixelColor.WHITE);
  }

  private float clampToRange(float v) {
    if (!Float.isFinite(v)) {
      return min;
    }
    return FlixelMath.clamp(v, min, max);
  }

  private float resolvePercent(float v) {
    float denom = (max - min);
    if (denom <= 0f) {
      return 0f;
    }
    return FlixelMath.clamp((v - min) / denom, 0f, 1f);
  }

  private void rebuildGradientIfNeeded() {
    if (gradientStart == null || gradientEnd == null) {
      if (gradientTexture != null) {
        gradientTexture.destroy();
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

    boolean horizontal =
        (fillDirection == BarFillDirection.LEFT_TO_RIGHT || fillDirection == BarFillDirection.RIGHT_TO_LEFT);
    if (horizontal) {
      desiredW = Math.max(2, Math.min(256, Math.round(getWidth())));
    } else {
      desiredH = Math.max(2, Math.min(256, Math.round(getHeight())));
    }

    if (FlixelMath.isEqual(lastGradientBasisW, getWidth())
        && FlixelMath.isEqual(lastGradientBasisH, getHeight())
        && gradientTexture != null && desiredW == gradientTexW && desiredH == gradientTexH) {
      return;
    }

    if (gradientTexture != null && desiredW == gradientTexW && desiredH == gradientTexH) {
      return;
    }

    if (gradientTexture != null) {
      gradientTexture.destroy();
    }

    gradientTexture = FlixelSpriteUtil.createLinearGradientTexture(
        desiredW,
        desiredH,
        gradientStart,
        gradientEnd,
        horizontal);
    gradientRegion = new FlixelFrame(gradientTexture);
    gradientTexW = desiredW;
    gradientTexH = desiredH;
    lastGradientBasisW = getWidth();
    lastGradientBasisH = getHeight();
  }

  private float resolveFrameRateIndependentLerp(float lerp, float elapsed) {
    // Copied conceptually from FlixelCamera.updateFollow. Converts lerp into a per-frame factor.
    elapsed = Math.max(0f, elapsed);
    return 1f - (float) Math.pow(1f - lerp, elapsed * 60f);
  }

  /**
   * One entry in a piecewise-linear threshold color ramp. At fill percent {@link #percent} the bar uses
   * {@link #color}, interpolating between stops for values in between.
   *
   * @param percent Fill fraction in {@code [0,1]} where this stop applies.
   * @param color FlixelColor at this stop.
   */
  public record ThresholdStop(float percent, FlixelColor color) {

    /**
     * @param percent Fill fraction; clamped to {@code [0,1]}.
     * @param color Stop color; copied internally.
     */
    public ThresholdStop(float percent, @NotNull FlixelColor color) {
      this.percent = FlixelMath.clamp(percent, 0f, 1f);
      this.color = new FlixelColor(Objects.requireNonNull(color));
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
