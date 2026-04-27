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
 * {@code console} with {@code %c} / {@code %s} styling so log levels and locations read clearly
 * in devtools (ANSI escapes from {@code System.out} are not used on web).
 */
public final class FlixelTeaVMLogConsole {

  private FlixelTeaVMLogConsole() {}

  /**
   * Forwards one structured log to JavaScript. Matches {@code FlixelLogConsoleSink#emit} for use
   * with {@code Flixel.setLogConsoleSink(FlixelTeaVMLogConsole::emit)}.
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
          + "var locA = lv === 0 ? 'color:#5aa0e8;font-weight:600' : lv === 1 ? 'color:#c9a020;font-weight:600' : 'color:#e85555;font-weight:600';"
          + "var msgA = lv === 0 ? 'color:#9ec8f5' : lv === 1 ? 'color:#e8c860' : 'color:#ff9a9a';"
          + "var tA = 'color:#7a7a7a;';"
          + "if (!detailed) {"
          + "  console.log('%c%s%c%s', locA, simpleLocation, msgA, message);"
          + "  return;"
          + "}"
          + "var lvStr = lv === 0 ? 'INFO' : lv === 1 ? 'WARN' : 'ERROR';"
          + "var meta = timestamp + ' [' + lvStr + '] [' + tag + '] [' + detailedFile + '] [' + methodLabel + ']\n';"
          + "console.log('%c%s%c%s', tA, meta, msgA, message);"
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
