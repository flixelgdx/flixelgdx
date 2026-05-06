/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight wrapper around the positional arguments passed to a custom debug console
 * command. Provides typed accessors with default values so command handlers can stay free
 * of boilerplate parsing logic.
 *
 * <p>Each command line is split on whitespace; the first token is the command name and the
 * remaining tokens are wrapped in this object. Tokens are kept as raw {@link String} values
 * and parsed lazily when a typed getter is called.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * Flixel.debug.registerCommand("setScale", args -> {
 *   String key = args.getString(0, "");
 *   float scale = args.getFloat(1, 1f);
 *   // ... do something with key and scale ...
 * });
 * }</pre>
 *
 * <p>Out-of-range indices return the default value (or an empty {@link String} for
 * {@link #getString(int)}). Bad numeric strings also fall back to the default. This keeps
 * commands resilient to typos in the console without throwing.
 */
public final class FlixelDebugCommandArgs {

  private static final String[] EMPTY = new String[0];

  private final String[] args;

  /**
   * @param args The positional argument tokens (must not be {@code null}; pass {@code new String[0]} for none).
   */
  public FlixelDebugCommandArgs(@NotNull String[] args) {
    this.args = args != null ? args : EMPTY;
  }

  /** Returns the number of positional arguments. */
  public int size() {
    return args.length;
  }

  /** Returns {@code true} if there are no positional arguments. */
  public boolean isEmpty() {
    return args.length == 0;
  }

  /**
   * Returns the raw token at {@code index}, or an empty {@link String} if the index is out of range.
   *
   * @param index The zero-based argument index.
   * @return The raw token, or {@code ""} if the index is invalid.
   */
  @NotNull
  public String getString(int index) {
    return getString(index, "");
  }

  /**
   * Returns the raw token at {@code index}, or {@code defaultValue} if the index is out of range.
   *
   * @param index The zero-based argument index.
   * @param defaultValue Value to return when the index is invalid.
   * @return The raw token, or {@code defaultValue} if the index is invalid.
   */
  @Nullable
  public String getString(int index, @Nullable String defaultValue) {
    if (index < 0 || index >= args.length) {
      return defaultValue;
    }
    return args[index];
  }

  /**
   * Parses the token at {@code index} as an integer, returning {@code defaultValue} on a missing
   * argument or a parse failure.
   *
   * @param index The zero-based argument index.
   * @param defaultValue The value to return on miss or parse error.
   * @return The parsed integer, or {@code defaultValue}.
   */
  public int getInt(int index, int defaultValue) {
    String s = getString(index, null);
    if (s == null || s.isEmpty()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parses the token at {@code index} as a long, returning {@code defaultValue} on a missing
   * argument or a parse failure.
   *
   * @param index The zero-based argument index.
   * @param defaultValue The value to return on miss or parse error.
   * @return The parsed long, or {@code defaultValue}.
   */
  public long getLong(int index, long defaultValue) {
    String s = getString(index, null);
    if (s == null || s.isEmpty()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parses the token at {@code index} as a float, returning {@code defaultValue} on a missing
   * argument or a parse failure.
   *
   * @param index The zero-based argument index.
   * @param defaultValue The value to return on miss or parse error.
   * @return The parsed float, or {@code defaultValue}.
   */
  public float getFloat(int index, float defaultValue) {
    String s = getString(index, null);
    if (s == null || s.isEmpty()) {
      return defaultValue;
    }
    try {
      return Float.parseFloat(s);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parses the token at {@code index} as a double, returning {@code defaultValue} on a missing
   * argument or a parse failure.
   *
   * @param index The zero-based argument index.
   * @param defaultValue The value to return on miss or parse error.
   * @return The parsed double, or {@code defaultValue}.
   */
  public double getDouble(int index, double defaultValue) {
    String s = getString(index, null);
    if (s == null || s.isEmpty()) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parses the token at {@code index} as a boolean. Accepts {@code "true"}, {@code "false"},
   * {@code "1"}, {@code "0"}, {@code "yes"}, {@code "no"}, {@code "on"}, and {@code "off"}
   * (case-insensitive). Returns {@code defaultValue} on a missing argument or unrecognized token.
   *
   * @param index The zero-based argument index.
   * @param defaultValue The value to return on miss or parse failure.
   * @return The parsed boolean, or {@code defaultValue}.
   */
  public boolean getBoolean(int index, boolean defaultValue) {
    String s = getString(index, null);
    if (s == null || s.isEmpty()) {
      return defaultValue;
    }
    String lower = s.toLowerCase();
    return switch (lower) {
      case "true", "1", "yes", "on" -> true;
      case "false", "0", "no", "off" -> false;
      default -> defaultValue;
    };
  }

  /**
   * Returns the underlying token array. The returned array is shared with this instance and
   * must not be modified. Useful for handlers that want to forward the raw tokens elsewhere.
   *
   * @return The shared backing array of tokens.
   */
  @NotNull
  public String[] getRawArgs() {
    return args;
  }
}
