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
package org.flixelgdx.logging;

import org.jetbrains.annotations.NotNull;

/**
 * Finds out which line of game code made a log call.
 *
 * <p>Each platform finds the call site in its own way: the desktop backend walks the stack, the web
 * backend reads a marker that the compiler inserted, and so on. The logger only asks for the answer
 * when a sink actually reads {@link FlixelLogEntry#site()}, so a resolver costs nothing for sinks that
 * never look at it.
 *
 * <p>Install one with {@link FlixelLogger#setSiteResolver(FlixelLogSiteResolver)}.
 */
@FunctionalInterface
public interface FlixelLogSiteResolver {

  /**
   * Fills {@code out} with the location of the code that logged the current message.
   *
   * <p>This is called on the thread that logged the message, while the logger's lock is held. Write
   * the answer into {@code out} in place (see {@link FlixelLogSite#set(String, int, String, String)})
   * instead of creating new objects.
   *
   * @param out The holder to fill in.
   * @return {@code true} if the site was found and {@code out} was filled, {@code false} if it is unknown.
   */
  boolean resolve(@NotNull FlixelLogSite out);
}
