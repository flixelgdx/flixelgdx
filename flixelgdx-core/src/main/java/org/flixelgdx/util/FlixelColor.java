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

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Mutable RGBA color with float components, used for tinting, backgrounds, and tween endpoints
 * without per-frame allocations.
 *
 * <p>Each component lives in {@code [0, 1]}. Read a component with its getter, such as
 * {@link #getAlpha()}, and change it with its setter, such as {@link #setAlpha(float)}. Red,
 * green, and blue also have setters that take a whole number in {@code [0, 255]}, such as
 * {@link #setRed(int)}. Every setter clamps its value into range, so a color can never hold a
 * component that would wrap around or overflow when it is packed for the GPU. The setters return
 * nothing so that Kotlin can treat each component as a property ({@code color.alpha = 0.5f}).
 *
 * <p>Think of the four components like the volume knobs on a mixing desk: each knob turns from
 * {@code 0} (off) to {@code 1} (full), and the knob simply stops at either end instead of spinning
 * past it.
 *
 * <pre>{@code
 * FlixelColor tint = new FlixelColor(FlixelColor.RED);
 * tint.setAlpha(0.5f);         // Half transparent red.
 * tint.setGreen(2f);           // Clamped to 1, so the color becomes half transparent yellow.
 * tint.setBlue(128);           // Whole numbers work too: 128 out of 255 is about 0.5.
 * float red = tint.getRed();   // 1
 * }</pre>
 *
 * <p>Shared presets such as {@link #WHITE} and {@link #RED} are single shared instances. Mutating
 * a preset affects every place that uses that reference. For a private copy, use
 * {@code new FlixelColor(FlixelColor.RED)} or {@link #set(FlixelColor)} on your own instance.
 *
 * <p>Use {@link #getRgba8888()} when you need a compact packed RGBA8888 value.
 */
public class FlixelColor {

  public static final FlixelColor WHITE = new FlixelColor(1f, 1f, 1f, 1f);
  public static final FlixelColor BLACK = new FlixelColor(0f, 0f, 0f, 1f);
  public static final FlixelColor RED = new FlixelColor(1f, 0f, 0f, 1f);
  public static final FlixelColor GREEN = new FlixelColor(0f, 1f, 0f, 1f);
  public static final FlixelColor BLUE = new FlixelColor(0f, 0f, 1f, 1f);
  public static final FlixelColor YELLOW = new FlixelColor(1f, 1f, 0f, 1f);
  public static final FlixelColor CYAN = new FlixelColor(0f, 1f, 1f, 1f);
  public static final FlixelColor MAGENTA = new FlixelColor(1f, 0f, 1f, 1f);
  public static final FlixelColor GRAY = new FlixelColor(0.5f, 0.5f, 0.5f, 1f);
  public static final FlixelColor CLEAR = new FlixelColor(0f, 0f, 0f, 0f);
  public static final FlixelColor ORANGE = new FlixelColor(1f, 0.65f, 0f, 1f);
  public static final FlixelColor PINK = new FlixelColor(1f, 0.41f, 0.71f, 1f);
  public static final FlixelColor PURPLE = new FlixelColor(0.63f, 0.13f, 0.94f, 1f);
  public static final FlixelColor BROWN = new FlixelColor(0.545f, 0.271f, 0.075f, 1f);
  public static final FlixelColor OLIVE = new FlixelColor(0.5f, 0.5f, 0f, 1f);
  public static final FlixelColor MAROON = new FlixelColor(0.69f, 0.19f, 0.38f, 1f);
  public static final FlixelColor NAVY = new FlixelColor(0f, 0f, 0.5f, 1f);
  public static final FlixelColor TEAL = new FlixelColor(0f, 0.5f, 0.5f, 1f);

  private float r;
  private float g;
  private float b;
  private float a;

  private float @Nullable [] hsv;

  /**
   * Creates a new color with the default white color.
   */
  public FlixelColor() {
    this(1f, 1f, 1f, 1f);
  }

  /**
   * Creates a new color from the given packed RGBA8888 value.
   *
   * @param rgba8888 The packed RGBA8888 value.
   */
  public FlixelColor(int rgba8888) {
    setRgba8888(rgba8888);
  }

  /**
   * Creates a new color from the given {@code FlixelColor} value.
   *
   * @param source The {@code FlixelColor} value to copy.
   */
  public FlixelColor(@NotNull FlixelColor source) {
    this(source.r, source.g, source.b, source.a);
    if (source.hsv != null) {
      hsv = Arrays.copyOf(source.hsv, source.hsv.length);
    }
  }

  /**
   * Creates a new color with the given RGBA values. Values must be in the range 0-255 (with alpha
   * being {@code [0, 1]}).
   *
   * @param r The red component.
   * @param g The green component.
   * @param b The blue component.
   * @param a The alpha component (ranged from {@code [0, 1]}).
   */
  public FlixelColor(int r, int g, int b, float a) {
    this.r = FlixelMath.clamp(r, 0, 255) / 255f;
    this.g = FlixelMath.clamp(g, 0, 255) / 255f;
    this.b = FlixelMath.clamp(b, 0, 255) / 255f;
    this.a = FlixelMath.clamp(a, 0f, 1f);
  }

  /**
   * Creates a new color with the given RGBA values in the range {@code [0, 1]}.
   *
   * <p>Values outside that range are clamped.
   *
   * @param r The red component.
   * @param g The green component.
   * @param b The blue component.
   * @param a The alpha component.
   */
  public FlixelColor(float r, float g, float b, float a) {
    set(r, g, b, a);
  }

  /**
   * Sets this color from a hex string such as {@code "#FF00FF"}, {@code "FF00FF"}, or an
   * eight-digit form with alpha such as {@code "#FF00FF80"}.
   *
   * @param hexFormat The hex string to parse. Must not be {@code null}.
   * @return This color, for chaining.
   * @throws NumberFormatException If the string is not valid hexadecimal.
   */
  public FlixelColor set(@NotNull String hexFormat) {
    String hex = hexFormat.startsWith("#") ? hexFormat.substring(1) : hexFormat;
    long value = Long.parseLong(hex, 16);
    if (hex.length() <= 6) {
      setRgba8888((int) ((value << 8) | 0xFF));
    } else {
      setRgba8888((int) value);
    }
    return this;
  }

  /**
   * Copies RGBA from {@code other} into this color.
   *
   * @param other The color to copy.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelColor set(@NotNull FlixelColor other) {
    r = other.r;
    g = other.g;
    b = other.b;
    a = other.a;
    return this;
  }

  /**
   * Sets all four components at once with {@code [0, 1]} ranges.
   *
   * <p>Values outside that range are clamped.
   *
   * @param r The red component in {@code [0, 1]}.
   * @param g The green component in {@code [0, 1]}.
   * @param b The blue component in {@code [0, 1]}.
   * @param a The alpha component in {@code [0, 1]}.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelColor set(float r, float g, float b, float a) {
    this.r = clamp(r);
    this.g = clamp(g);
    this.b = clamp(b);
    this.a = clamp(a);
    return this;
  }

  /**
   * Sets this color from a packed RGBA8888 value.
   *
   * @param rgba8888 The packed value, red in the highest byte.
   */
  public void setRgba8888(int rgba8888) {
    r = ((rgba8888 >>> 24) & 0xFF) / 255f;
    g = ((rgba8888 >>> 16) & 0xFF) / 255f;
    b = ((rgba8888 >>> 8) & 0xFF) / 255f;
    a = (rgba8888 & 0xFF) / 255f;
  }

  /**
   * Linearly interpolates this color toward {@code target}, writing the result in place.
   *
   * <p>A factor outside {@code [0, 1]} (such as from an elastic ease) can overshoot, so the
   * result is clamped into {@code [0, 1]}.
   *
   * @param target The color to move toward.
   * @param t Interpolation factor in {@code [0, 1]}; {@code 0} keeps this color, {@code 1} becomes the target.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelColor lerp(@NotNull FlixelColor target, float t) {
    return set(r + (target.r - r) * t, g + (target.g - g) * t, b + (target.b - b) * t, a + (target.a - a) * t);
  }

  /**
   * Returns the packed RGBA8888 value with red in the highest byte.
   *
   * @return The packed RGBA8888 integer representing this color.
   */
  public int getRgba8888() {
    return ((int) (r * 255f) << 24) | ((int) (g * 255f) << 16) | ((int) (b * 255f) << 8) | (int) (a * 255f);
  }

  /**
   * Packs this color into a single float for a vertex color attribute.
   *
   * <p>The bits are laid out as ABGR8888 (alpha in the highest byte), the order the sprite batch's
   * per-vertex color expects. This is meant for building custom vertex arrays passed to
   * {@link FlixelBatch#draw(FlixelTexture, float[], int, int)};
   * for normal drawing, tint through the batch or sprite instead.
   *
   * @return This color packed as ABGR8888, reinterpreted as a float.
   */
  public float toFloatBits() {
    return toFloatBits(r, g, b, a);
  }

  /**
   * Packs loose color components into a single float for a vertex color attribute.
   *
   * <p>This is the allocation-free twin of {@link #toFloatBits()} for code that keeps its color as
   * plain floats instead of a {@link FlixelColor} instance, such as particles. Components outside
   * {@code [0, 1]} are clamped.
   *
   * @param r The red component, in {@code [0, 1]}.
   * @param g The green component, in {@code [0, 1]}.
   * @param b The blue component, in {@code [0, 1]}.
   * @param a The alpha component, in {@code [0, 1]}.
   * @return The color packed as ABGR8888, reinterpreted as a float.
   */
  public static float toFloatBits(float r, float g, float b, float a) {
    int r8 = clampByte(r);
    int g8 = clampByte(g);
    int b8 = clampByte(b);
    int a8 = clampByte(a);
    return Float.intBitsToFloat((a8 << 24) | (b8 << 16) | (g8 << 8) | r8);
  }

  private static float clamp(float value) {
    return FlixelMath.clamp(value, 0f, 1f);
  }

  private static int clampByte(float value) {
    int scaled = (int) (value * 255f + 0.5f);
    if (scaled < 0) {
      return 0;
    }
    return Math.min(scaled, 255);
  }

  /**
   * Returns the hue of this color in degrees.
   *
   * @return Hue in {@code [0, 360)}.
   */
  public float getHue() {
    updateHsv();
    return hsv[0];
  }

  /**
   * Returns the saturation of this color.
   *
   * @return Saturation in {@code [0, 1]}.
   */
  public float getSaturation() {
    updateHsv();
    return hsv[1];
  }

  /**
   * Returns the value (brightness) of this color.
   *
   * @return Value in {@code [0, 1]}.
   */
  public float getValue() {
    updateHsv();
    return hsv[2];
  }

  /**
   * Sets the hue of this color, keeping saturation and value.
   *
   * @param hue Hue in degrees; wrapped into {@code [0, 360)}.
   */
  public void setHue(float hue) {
    updateHsv();
    hsv[0] = ((hue % 360f) + 360f) % 360f;
    applyHsv();
  }

  /**
   * Sets the saturation of this color, keeping hue and value.
   *
   * @param saturation Saturation in {@code [0, 1]}.
   */
  public void setSaturation(float saturation) {
    updateHsv();
    hsv[1] = FlixelMath.clamp(saturation, 0f, 1f);
    applyHsv();
  }

  /**
   * Sets the value (brightness) of this color, keeping hue and saturation.
   *
   * @param value Value in {@code [0, 1]}.
   */
  public void setValue(float value) {
    updateHsv();
    hsv[2] = FlixelMath.clamp(value, 0f, 1f);
    applyHsv();
  }

  /** Recomputes the cached HSV triple from the current RGB components. */
  private void updateHsv() {
    if (hsv == null) {
      hsv = new float[3];
    }
    float max = Math.max(r, Math.max(g, b));
    float min = Math.min(r, Math.min(g, b));
    float range = max - min;
    float hue;
    if (range == 0f) {
      hue = 0f;
    } else if (max == r) {
      hue = (60f * (g - b) / range + 360f) % 360f;
    } else if (max == g) {
      hue = 60f * (b - r) / range + 120f;
    } else {
      hue = 60f * (r - g) / range + 240f;
    }
    hsv[0] = hue;
    hsv[1] = max == 0f ? 0f : range / max;
    hsv[2] = max;
  }

  /** Writes the cached HSV triple back into the RGB components. */
  private void applyHsv() {
    float h = hsv[0];
    float s = hsv[1];
    float v = hsv[2];
    float c = v * s;
    float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
    float m = v - c;
    float tr;
    float tg;
    float tb;
    if (h < 60f) {
      tr = c;
      tg = x;
      tb = 0f;
    } else if (h < 120f) {
      tr = x;
      tg = c;
      tb = 0f;
    } else if (h < 180f) {
      tr = 0f;
      tg = c;
      tb = x;
    } else if (h < 240f) {
      tr = 0f;
      tg = x;
      tb = c;
    } else if (h < 300f) {
      tr = x;
      tg = 0f;
      tb = c;
    } else {
      tr = c;
      tg = 0f;
      tb = x;
    }
    r = tr + m;
    g = tg + m;
    b = tb + m;
  }

  public float getRed() {
    return r;
  }

  /**
   * Sets the red component from a whole number in {@code [0, 255]}.
   *
   * @param r The red component in {@code [0, 255]}; values outside that range are clamped.
   */
  public void setRed(int r) {
    this.r = FlixelMath.clamp(r, 0, 255) / 255f;
  }

  /**
   * Sets the red component.
   *
   * @param r The red component in {@code [0, 1]}; values outside that range are clamped.
   */
  public void setRed(float r) {
    this.r = clamp(r);
  }

  public float getGreen() {
    return g;
  }

  /**
   * Sets the green component from a whole number in {@code [0, 255]}.
   *
   * @param g The green component in {@code [0, 255]}; values outside that range are clamped.
   */
  public void setGreen(int g) {
    this.g = FlixelMath.clamp(g, 0, 255) / 255f;
  }

  /**
   * Sets the green component.
   *
   * @param g The green component in {@code [0, 1]}; values outside that range are clamped.
   */
  public void setGreen(float g) {
    this.g = clamp(g);
  }

  public float getBlue() {
    return b;
  }

  /**
   * Sets the blue component from a whole number in {@code [0, 255]}.
   *
   * @param b The blue component in {@code [0, 255]}; values outside that range are clamped.
   */
  public void setBlue(int b) {
    this.b = FlixelMath.clamp(b, 0, 255) / 255f;
  }

  /**
   * Sets the blue component.
   *
   * @param b The blue component in {@code [0, 1]}; values outside that range are clamped.
   */
  public void setBlue(float b) {
    this.b = clamp(b);
  }

  /**
   * Returns the alpha component, where {@code 0} is fully transparent and {@code 1} is fully opaque.
   *
   * @return The alpha component in {@code [0, 1]}.
   */
  public float getAlpha() {
    return a;
  }

  /**
   * Sets the alpha component, where {@code 0} is fully transparent and {@code 1} is fully opaque.
   *
   * @param a The alpha component in {@code [0, 1]}; values outside that range are clamped.
   */
  public void setAlpha(float a) {
    this.a = clamp(a);
  }
}
