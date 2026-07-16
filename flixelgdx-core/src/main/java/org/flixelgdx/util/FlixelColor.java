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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable color wrapper that owns a single {@link Color} instance for stable tinting
 * and tween endpoints without per-frame allocations.
 *
 * <p>Shared presets such as {@link #WHITE} and {@link #RED} point at the same instances as libGDX
 * {@link Color} namesakes. Mutating a preset affects every place that uses that reference. For a
 * private copy, use {@code new FlixelColor(FlixelColor.RED)} or {@link #setColor(FlixelColor)} on your
 * own instance.
 *
 * <p>Use {@link #getGdxColor()} when you need component access, lerping, or batch tinting; use
 * {@link #getColor()} when you need a compact RGBA8888 value.
 */
public class FlixelColor {

  public static final FlixelColor WHITE = new FlixelColor(Color.WHITE);
  public static final FlixelColor BLACK = new FlixelColor(Color.BLACK);
  public static final FlixelColor RED = new FlixelColor(Color.RED);
  public static final FlixelColor GREEN = new FlixelColor(Color.GREEN);
  public static final FlixelColor BLUE = new FlixelColor(Color.BLUE);
  public static final FlixelColor YELLOW = new FlixelColor(Color.YELLOW);
  public static final FlixelColor CYAN = new FlixelColor(Color.CYAN);
  public static final FlixelColor MAGENTA = new FlixelColor(Color.MAGENTA);
  public static final FlixelColor GRAY = new FlixelColor(Color.GRAY);
  public static final FlixelColor CLEAR = new FlixelColor(Color.CLEAR);
  public static final FlixelColor ORANGE = new FlixelColor(Color.ORANGE);
  public static final FlixelColor PINK = new FlixelColor(Color.PINK);
  public static final FlixelColor PURPLE = new FlixelColor(Color.PURPLE);
  public static final FlixelColor BROWN = new FlixelColor(Color.BROWN);
  public static final FlixelColor OLIVE = new FlixelColor(Color.OLIVE);
  public static final FlixelColor MAROON = new FlixelColor(Color.MAROON);
  public static final FlixelColor NAVY = new FlixelColor(Color.NAVY);
  public static final FlixelColor TEAL = new FlixelColor(Color.TEAL);

  @NotNull
  private final Color color;
  @Nullable
  private float[] hsv;

  /**
   * Creates a new color with the default white color.
   */
  public FlixelColor() {
    color = new Color(Color.WHITE);
  }

  /**
   * Creates a new color with the given RGBA values. Values must be in the range 0-255 (with alpha
   * being {@code [0, 1]}).
   *
   * @param r The red channel.
   * @param g The green channel.
   * @param b The blue channel.
   * @param a The alpha channel (ranged from {@code [0, 1]}).
   */
  public FlixelColor(int r, int g, int b, float a) {
    float nr = clamp(r);
    float ng = clamp(g);
    float nb = clamp(b);
    float na = MathUtils.clamp(a, 0, 1);
    color = new Color(nr, ng, nb, na);
  }

  /**
   * Creates a new color with the given RGBA values. Values must be in the range 0-1.
   *
   * @param r The red channel.
   * @param g The green channel.
   * @param b The blue channel.
   * @param a The alpha channel.
   */
  public FlixelColor(float r, float g, float b, float a) {
    color = new Color(r, g, b, a);
  }

  /**
   * Creates a new color from the given packed RGBA8888 value.
   *
   * @param rgba8888 The packed RGBA8888 value.
   */
  public FlixelColor(int rgba8888) {
    color = new Color(rgba8888);
  }

  /**
   * Creates a new color from the given {@link Color} value.
   *
   * @param source The {@link Color} value to copy.
   */
  public FlixelColor(@NotNull Color source) {
    color = new Color(source);
  }

  /**
   * Creates a new color from the given {@code FlixelColor} value.
   *
   * @param source The {@code FlixelColor} value to copy.
   */
  public FlixelColor(@NotNull FlixelColor source) {
    color = new Color(source.color);
    hsv = source.hsv;
  }

  /**
   * @return Packed RGBA8888, same as libGDX {@link Color#rgba8888(Color)} on the backing color.
   */
  public int getColor() {
    return Color.rgba8888(color);
  }

  /**
   * Copies RGBA from {@code other} into this wrapper.
   *
   * @param other The libGDX color to copy. Must not be {@code null}.
   */
  public void setColor(@NotNull Color other) {
    color.set(other);
  }

  /**
   * Copies RGBA from {@code other} into this wrapper.
   *
   * @param other The Flixel color to copy. Must not be {@code null}.
   */
  public void setColor(@NotNull FlixelColor other) {
    color.set(other.color);
  }

  /**
   * Sets this color from a hex string, same format accepted by libGDX {@link Color#valueOf(String)}
   * (for example {@code "ff0000"} or {@code "ff0000ff"}).
   *
   * @param hexFormat The hex string to parse. Must not be {@code null}.
   */
  public void setColor(@NotNull String hexFormat) {
    color.set(Color.valueOf(hexFormat));
  }

  /**
   * @return The hue component of this color, in degrees {@code [0, 360)}.
   */
  public float getHue() {
    return hsv()[0];
  }

  /**
   * @return The saturation component of this color, in the range {@code [0, 1]}.
   */
  public float getSaturation() {
    return hsv()[1];
  }

  /**
   * @return The value (brightness) component of this color, in the range {@code [0, 1]}.
   */
  public float getValue() {
    return hsv()[2];
  }

  /**
   * Sets the hue component of this color, leaving saturation, value, and alpha unchanged.
   *
   * @param hue The new hue, in degrees {@code [0, 360)}.
   */
  public void setHue(float hue) {
    float[] v = hsv();
    color.fromHsv(hue, v[1], v[2]);
  }

  /**
   * Sets the saturation component of this color, leaving hue, value, and alpha unchanged.
   *
   * @param saturation The new saturation, in the range {@code [0, 1]}.
   */
  public void setSaturation(float saturation) {
    float[] v = hsv();
    color.fromHsv(v[0], saturation, v[2]);
  }

  /**
   * Sets the value (brightness) component of this color, leaving hue, saturation, and alpha unchanged.
   *
   * @param value The new value, in the range {@code [0, 1]}.
   */
  public void setValue(float value) {
    float[] v = hsv();
    color.fromHsv(v[0], v[1], value);
  }

  /**
   * Returns the red channel of this color in the range of {@code 0-255}.
   *
   * @return The red channel.
   */
  public int getRed() {
    return (int) (color.r * 255);
  }

  /**
   * Returns the green channel of this color in the range of {@code 0-255}.
   *
   * @return The green channel.
   */
  public int getGreen() {
    return (int) (color.g * 255);
  }

  /**
   * Returns the blue channel of this color in the range of {@code 0-255}.
   *
   * @return The blue channel.
   */
  public int getBlue() {
    return (int) (color.b * 255);
  }

  /**
   * Returns the alpha channel of this color in the range of {@code 0-1}.
   *
   * @return The alpha value.
   */
  public float getAlpha() {
    return color.a;
  }

  /**
   * Sets the red channel for this color.
   *
   * @param r The new value for this color's red channel. Should be between {@code 0-255}.
   */
  public void setRed(int r) {
    color.r = clamp(r);
  }

  /**
   * Sets the green channel for this color.
   *
   * @param g The new value for this color's green channel. Should be between {@code 0-255}.
   */
  public void setGreen(int g) {
    color.g = clamp(g);
  }

  /**
   * Sets the blue channel of this color.
   *
   * @param b The new value for this color's blue channel. Should be between {@code 0-255}.
   */
  public void setBlue(int b) {
    color.b = clamp(b);
  }

  /**
   * Sets the alpha channel of this color.
   *
   * @param a The new value for this color's alpha channel. Should be between {@code 0-1}.
   */
  public void setAlpha(float a) {
    color.a = MathUtils.clamp(a, 0, 1);
  }

  /**
   * Sets this color with the given RGBA values. Values must be in the range 0-255 (with alpha
   * being {@code [0, 1]}).
   *
   * @param r The red channel.
   * @param g The green channel.
   * @param b The blue channel.
   * @param a The alpha channel (ranged from {@code [0, 1]}).
   */
  public void set(int r, int g, int b, float a) {
    color.set(clamp(r), clamp(g), clamp(b), a);
  }

  /**
   * Sets this color with the given RGBA values. Values must be in the range 0-1.
   *
   * @param r The red channel.
   * @param g The green channel.
   * @param b The blue channel.
   * @param a The alpha channel.
   */
  public void set(float r, float g, float b, float a) {
    color.set(
      MathUtils.clamp(r, 0, 1),
      MathUtils.clamp(g, 0, 1),
      MathUtils.clamp(b, 0, 1),
      MathUtils.clamp(a, 0, 1)
    );
  }

  /**
   * @return The backing libGDX color (mutable).
   */
  @NotNull
  public Color getGdxColor() {
    return color;
  }

  /**
   * Lazily allocates the backing HSV scratch array, then refreshes it from the current color.
   *
   * @return The reused HSV scratch array, refreshed to match {@link #color}.
   */
  @NotNull
  private float[] hsv() {
    if (hsv == null) {
      hsv = new float[3];
    }
    return color.toHsv(hsv);
  }

  /**
   * Clamps the provided integer from range {@code 0-255} to a valid float between {@code 0-1}.
   *
   * <p>This is primarily used for converting integer-based RGB values into a valid number
   * for libGDX {@link Color} to consume.
   *
   * @param value The value to clamp.
   * @return The newly clamped value.
   */
  private float clamp(int value) {
    return MathUtils.clamp(value, 0, 255) / 255f;
  }
}
