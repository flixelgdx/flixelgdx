/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

/**
 * Optional sink for a single structured log line to the host console. Used on platforms where
 * {@code System.out} is not appropriate or where ANSI colors from the default path do not render
 * (for example, browser devtools with styled {@code console.log}).
 *
 * <p>Register with {@link me.stringdotjar.flixelgdx.Flixel#setLogConsoleSink(FlixelLogConsoleSink)} before
 * {@link me.stringdotjar.flixelgdx.Flixel#initialize} from the platform launcher. When set, the logger calls
 * this instead of writing ANSI text to standard output; file logging and in-game log listeners are unchanged.
 */
@FunctionalInterface
public interface FlixelLogConsoleSink {

  /**
   * Emits one log line. Arguments mirror the data used to build the normal console and file lines, without
   * ANSI codes.
   *
   * @param level The log level.
   * @param tag The tag, never {@code null} but may be empty.
   * @param message The message body.
   * @param simpleLocation In simple log mode, the location prefix such as {@code mypkg/MyClass.java:42:}; when
   *   the call site is unknown, a placeholder (for example, {@code unknown:0:}).
   * @param detailedFile Short file/line ID used in detailed mode, such as {@code MyClass.java:42}.
   * @param methodLabel Method name with parentheses, for example, {@code update()}.
   * @param timestamp The formatted timestamp used in detailed mode.
   * @param detailed {@code true} if {@link FlixelLogMode#DETAILED} formatting applies; otherwise simple mode.
   */
  void emit(
      FlixelLogLevel level,
      String tag,
      String message,
      String simpleLocation,
      String detailedFile,
      String methodLabel,
      String timestamp,
      boolean detailed);
}
