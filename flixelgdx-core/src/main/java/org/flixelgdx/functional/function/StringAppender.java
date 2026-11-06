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
package org.flixelgdx.functional.function;

import org.flixelgdx.util.FlixelString;

/**
 * Represents a function that writes some text about an object into a reusable buffer, so reading a text value
 * never has to create a new {@link String}.
 *
 * <p>Think of it as filling in a form that someone else already printed. The caller hands over the same blank
 * form every time ({@code out}), and the function only writes into it. The buffer is cleared before each call.
 *
 * <pre>{@code
 * StringAppender<FlixelText> shownText = (text, out) -> out.concat(text.getTextBuffer());
 * }</pre>
 *
 * @param <T> The type of the input object.
 */
@FunctionalInterface
public interface StringAppender<T> {

  /**
   * Appends the text for the given object to the buffer.
   *
   * @param value The input object.
   * @param out The empty buffer to append to. Do not keep a reference to it.
   */
  void appendTo(T value, FlixelString out);
}
