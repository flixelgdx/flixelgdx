/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

import me.stringdotjar.flixelgdx.Flixel;

/**
 * Static entry points used only by the {@code flixelgdx-logging-plugin} bytecode weaver. At compile time,
 * {@code INVOKESTATIC Flixel.info(...)} (and similar) in game modules are rewritten to call these methods with
 * embedded source file and line metadata so TeaVM and other runtimes that lack JVM-style stack walking still
 * print accurate locations.
 *
 * <p>Do not call these methods from handwritten game code; keep using {@link Flixel#info(Object)} and related APIs.
 */
public final class FlixelLoggingBytecodeHooks {

  private FlixelLoggingBytecodeHooks() {}

  public static void bcInfo0(Object message, String sourceFile, int line, String declaringClass, String declaringMethod) {
    Flixel.log.infoWithSite(message, sourceFile, line, declaringClass, declaringMethod);
  }

  public static void bcInfo1(
    String tag,
    Object message,
    String sourceFile,
    int line,
    String declaringClass,
    String declaringMethod) {
    Flixel.log.infoWithSite(tag, message, sourceFile, line, declaringClass, declaringMethod);
  }

  public static void bcWarn0(Object message, String sourceFile, int line, String declaringClass, String declaringMethod) {
    Flixel.log.warnWithSite(message, sourceFile, line, declaringClass, declaringMethod);
  }

  public static void bcWarn1(
    String tag,
    Object message,
    String sourceFile,
    int line,
    String declaringClass,
    String declaringMethod) {
    Flixel.log.warnWithSite(tag, message, sourceFile, line, declaringClass, declaringMethod);
  }

  public static void bcErr0(String message, String sourceFile, int line, String declaringClass, String declaringMethod) {
    Flixel.log.errorWithSite(message, sourceFile, line, declaringClass, declaringMethod);
  }

  public static void bcErr1(
    String tag,
    Object message,
    String sourceFile,
    int line,
    String declaringClass,
    String declaringMethod) {
    Flixel.log.errorWithSite(tag, message, sourceFile, line, declaringClass, declaringMethod);
  }

  public static void bcErr2(
    String tag,
    Object message,
    Throwable throwable,
    String sourceFile,
    int line,
    String declaringClass,
    String declaringMethod) {
    Flixel.log.errorWithSite(tag, message, throwable, sourceFile, line, declaringClass, declaringMethod);
  }
}
