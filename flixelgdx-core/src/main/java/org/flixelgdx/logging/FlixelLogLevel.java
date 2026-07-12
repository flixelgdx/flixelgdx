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

/**
 * An enum that defines the log levels for FlixelGDX's logging system. This is used to determine the
 * severity of a log message and how it should be displayed in the console.
 */
public enum FlixelLogLevel {

  /**
   * Simple white/gray text and simple informational log level that is used for general information about the game.
   */
  INFO,

  /**
   * Highlighted yellow in the console and, although not critical, indicates that something may be
   * wrong and should be looked into.
   */
  WARN,

  /**
   * Highlighted red in the console and indicates an error. Shows something is wrong and
   * should be looked into immediately.
   */
  ERROR,

  /**
   * Highlighted blue in the console and used for verbose, low-priority messages that help during active
   * development. Matches libGDX's {@code Application.debug(...)} severity level.
   */
  DEBUG
}
