/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm.logging;

import me.stringdotjar.flixelgdx.logging.FlixelLogLevel;

import org.teavm.jso.JSBody;

/**
 * Routes {@link me.stringdotjar.flixelgdx.logging.FlixelLogConsoleSink} output to the browser
 * {@code console} with {@code %c} styling so log levels and locations read clearly in devtools
 * (ANSI escapes from {@code System.out} are not used on web).
 *
 * <p>The native script also picks the right {@code console} method per level (so the browser's
 * own filter pills for warnings and errors work as expected): {@code console.log} for
 * {@link FlixelLogLevel#INFO INFO}, {@code console.warn} for {@link FlixelLogLevel#WARN WARN},
 * and {@code console.error} for {@link FlixelLogLevel#ERROR ERROR}.
 */
public final class FlixelTeaVMLogConsole {

  private FlixelTeaVMLogConsole() {}

  /**
   * Forwards one structured log to JavaScript. Matches {@code FlixelLogConsoleSink#emit} for use
   * with {@code Flixel.setLogConsoleSink(FlixelTeaVMLogConsole::emit)}.
   *
   * @param level The log level to color and route on.
   * @param tag The associated tag, may be empty.
   * @param message The body text of the log line.
   * @param simpleLocation The short {@code package/File.java:line:} prefix for simple mode.
   * @param detailedFile The detailed {@code File.java:line} string used in detailed mode.
   * @param methodLabel The method label such as {@code update()}.
   * @param timestamp The pre-formatted timestamp string.
   * @param detailed {@code true} to render in detailed mode (timestamp + tags + body).
   */
  public static void emit(
      FlixelLogLevel level,
      String tag,
      String message,
      String simpleLocation,
      String detailedFile,
      String methodLabel,
      String timestamp,
      boolean detailed) {
    int o = (level == FlixelLogLevel.INFO) ? 0 : (level == FlixelLogLevel.WARN) ? 1 : 2;
    emit0(
        o,
        nullToE(tag),
        nullToE(message),
        nullToE(simpleLocation),
        nullToE(detailedFile),
        nullToE(methodLabel),
        nullToE(timestamp),
        detailed);
  }

  private static String nullToE(String s) {
    return s != null ? s : "";
  }

  // The script is intentionally one long concatenation of string literals (TeaVM @JSBody requires
  // the script value to be a compile-time constant).
  //
  // Two important details:
  //   1. The literal newline between the meta line and the body in detailed mode MUST be the JS
  //      escape sequence '\\n'. Java compiles '\n' into a real 0x0A character at compile time,
  //      which would land inside a single-quoted JavaScript string literal, crash the parser, and
  //      silently take out every subsequent log call.
  //   2. User-supplied strings (tag/message/etc.) are passed through positional %s substitutions
  //      instead of being concatenated into the format string, so a stray '%' in a log message
  //      does not eat the next argument or trigger console formatter quirks.
  @JSBody(
      params = {
        "l",
        "tag",
        "message",
        "simpleLocation",
        "detailedFile",
        "methodLabel",
        "timestamp",
        "detailed"
      },
      script = ""
          + "var lv = l | 0;"
          + "var sink = lv === 1 ? console.warn : (lv === 2 ? console.error : console.log);"
          + "var locStyle = lv === 0 ? 'color:#5aa0e8;font-weight:600'"
          + "  : lv === 1 ? 'color:#c9a020;font-weight:600'"
          + "  : 'color:#e85555;font-weight:600';"
          + "var msgStyle = lv === 0 ? 'color:#9ec8f5'"
          + "  : lv === 1 ? 'color:#e8c860'"
          + "  : 'color:#ff9a9a';"
          + "var metaStyle = 'color:#7a7a7a';"
          + "if (!detailed) {"
          + "  sink('%c%s %c%s', locStyle, simpleLocation, msgStyle, message);"
          + "  return;"
          + "}"
          + "var lvStr = lv === 0 ? 'INFO' : lv === 1 ? 'WARN' : 'ERROR';"
          + "var meta = timestamp + ' [' + lvStr + '] [' + tag + '] [' + detailedFile + '] [' + methodLabel + ']';"
          + "sink('%c%s\\n%c%s', metaStyle, meta, msgStyle, message);"
  )
  private static native void emit0(
      int l,
      String tag,
      String message,
      String simpleLocation,
      String detailedFile,
      String methodLabel,
      String timestamp,
      boolean detailed);
}
