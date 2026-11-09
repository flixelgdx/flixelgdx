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
package org.flixelgdx.debug.ui;

import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The arguments of one event that the debugger app sent for a widget, read one after another.
 *
 * <p>An event such as "the person changed this checkbox" carries a few values (here, the new {@code true} or
 * {@code false}). Think of the arguments as a short queue of envelopes: {@link #next()} opens the next envelope and
 * hands you what is inside, and the typed helpers such as {@link #nextBoolean(boolean)} also check that the envelope
 * holds the kind of thing you expect, giving you a fallback otherwise. Nothing is allocated; the same holder is
 * reused for every value.
 *
 * <p>Widgets read their arguments in {@link FlixelDebugWidget#handleEvent(int, FlixelDebugEventArgs)}. Game code
 * only meets this class when it writes its own widget type.
 */
public final class FlixelDebugEventArgs {

  private final FlixelDebugValue value = new FlixelDebugValue();
  private FlixelDebugMessageReader reader;
  private int remaining;

  /** Creates an empty argument list. */
  public FlixelDebugEventArgs() {}

  /**
   * Points this list at the arguments of a new event.
   *
   * @param reader The reader positioned at the first argument.
   * @param count How many arguments the event has.
   */
  public void reset(@NotNull FlixelDebugMessageReader reader, int count) {
    this.reader = reader;
    this.remaining = count;
  }

  /**
   * Returns whether another argument is waiting.
   *
   * @return {@code true} when {@link #next()} returns a value.
   */
  public boolean hasNext() {
    return remaining > 0;
  }

  /**
   * Reads the next argument.
   *
   * @return The reused value holder, or {@code null} when there are no more arguments. The holder is overwritten by
   *   the next call.
   */
  @Nullable
  public FlixelDebugValue next() {
    if (remaining <= 0 || reader == null) {
      return null;
    }
    remaining--;
    return value.read(reader);
  }

  /**
   * Reads the next argument as a boolean.
   *
   * @param fallback What to return when the argument is missing or is not a boolean.
   * @return The boolean, or the fallback.
   */
  public boolean nextBoolean(boolean fallback) {
    FlixelDebugValue v = next();
    return v != null && v.getKind() == FlixelDebugProtocol.VALUE_BOOL ? v.asBoolean() : fallback;
  }

  /**
   * Reads the next argument as an int. Any number is converted like a Java cast.
   *
   * @param fallback What to return when the argument is missing or is not a number.
   * @return The number, or the fallback.
   */
  public int nextInt(int fallback) {
    FlixelDebugValue v = next();
    return v != null && v.isNumber() ? v.asInt() : fallback;
  }

  /**
   * Reads the next argument as a long. Any number is converted like a Java cast.
   *
   * @param fallback What to return when the argument is missing, is {@code NULL}, or is not a number.
   * @return The number, or the fallback.
   */
  public long nextLong(long fallback) {
    FlixelDebugValue v = next();
    return v != null && v.isNumber() ? v.asLong() : fallback;
  }

  /**
   * Reads the next argument as a float. Any number is converted.
   *
   * @param fallback What to return when the argument is missing or is not a number.
   * @return The number, or the fallback.
   */
  public float nextFloat(float fallback) {
    FlixelDebugValue v = next();
    return v != null && v.isNumber() ? v.asFloat() : fallback;
  }

  /**
   * Reads the next argument as a double. Any number is converted.
   *
   * @param fallback What to return when the argument is missing or is not a number.
   * @return The number, or the fallback.
   */
  public double nextDouble(double fallback) {
    FlixelDebugValue v = next();
    return v != null && v.isNumber() ? v.asDouble() : fallback;
  }

  /**
   * Reads the next argument as text.
   *
   * @return The reused text buffer, or {@code null} when the argument is missing or is not a string. Call
   *   {@code toString()} to keep it.
   */
  @Nullable
  public CharSequence nextText() {
    FlixelDebugValue v = next();
    return v != null && v.getKind() == FlixelDebugProtocol.VALUE_STRING ? v.getText() : null;
  }

  /**
   * Returns how many arguments are still unread.
   *
   * @return The count.
   */
  public int getRemaining() {
    return remaining;
  }
}
