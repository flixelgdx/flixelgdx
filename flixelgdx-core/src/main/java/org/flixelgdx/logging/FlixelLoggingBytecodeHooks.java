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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationLogger;

import org.flixelgdx.Flixel;

/**
 * Static entry points used only by the {@code flixelgdx-logging-plugin} bytecode weaver. At compile time,
 * {@code INVOKESTATIC Flixel.info(...)} (and similar) and {@code INVOKEINTERFACE Application.log(...)} calls in game
 * modules are rewritten to call these methods with embedded source file and line metadata so TeaVM and other runtimes
 * that lack JVM-style stack walking still print accurate locations.
 *
 * <p>The {@code bcGdx*} methods guard at runtime: if the active {@link ApplicationLogger} is a {@link FlixelLogger},
 * the call is routed through the appropriate {@code *WithSite} overload; otherwise the original {@link Application}
 * method is called unchanged so non-FlixelGDX loggers are never affected.
 *
 * <p>Do not call these methods from handwritten game code; keep using {@link Flixel#info(Object)} and related APIs.
 */
public final class FlixelLoggingBytecodeHooks {

  private FlixelLoggingBytecodeHooks() {}

  public static void bcInfo0(Object message, String sourceFile, int line, String declaringClass,
      String declaringMethod) {
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

  public static void bcWarn0(Object message, String sourceFile, int line, String declaringClass,
      String declaringMethod) {
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

  public static void bcErr0(String message, String sourceFile, int line, String declaringClass,
      String declaringMethod) {
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

  public static void bcGdxLog0(
      Application app,
      String tag,
      String message,
      String sourceFile,
      int line,
      String declaringClass,
      String declaringMethod) {
    ApplicationLogger logger = app.getApplicationLogger();
    if (logger instanceof FlixelLogger fl) {
      fl.infoWithSite(tag, message, sourceFile, line, declaringClass, declaringMethod);
    } else {
      app.log(tag, message);
    }
  }

  public static void bcGdxLog1(
      Application app,
      String tag,
      String message,
      Throwable exception,
      String sourceFile,
      int line,
      String declaringClass,
      String declaringMethod) {
    ApplicationLogger logger = app.getApplicationLogger();
    if (logger instanceof FlixelLogger fl) {
      fl.errorWithSite(tag, message, exception, sourceFile, line, declaringClass, declaringMethod);
    } else {
      app.log(tag, message, exception);
    }
  }

  public static void bcGdxDebug0(
      Application app,
      String tag,
      String message,
      String sourceFile,
      int line,
      String declaringClass,
      String declaringMethod) {
    ApplicationLogger logger = app.getApplicationLogger();
    if (logger instanceof FlixelLogger fl) {
      fl.debugWithSite(tag, message, sourceFile, line, declaringClass, declaringMethod);
    } else {
      app.debug(tag, message);
    }
  }

  public static void bcGdxDebug1(
      Application app,
      String tag,
      String message,
      Throwable exception,
      String sourceFile,
      int line,
      String declaringClass,
      String declaringMethod) {
    ApplicationLogger logger = app.getApplicationLogger();
    if (logger instanceof FlixelLogger fl) {
      fl.errorWithSite(tag, message, exception, sourceFile, line, declaringClass, declaringMethod);
    } else {
      app.debug(tag, message, exception);
    }
  }

  public static void bcGdxErr0(
      Application app,
      String tag,
      String message,
      String sourceFile,
      int line,
      String declaringClass,
      String declaringMethod) {
    ApplicationLogger logger = app.getApplicationLogger();
    if (logger instanceof FlixelLogger fl) {
      fl.errorWithSite(tag, message, sourceFile, line, declaringClass, declaringMethod);
    } else {
      app.error(tag, message);
    }
  }

  public static void bcGdxErr1(
      Application app,
      String tag,
      String message,
      Throwable exception,
      String sourceFile,
      int line,
      String declaringClass,
      String declaringMethod) {
    ApplicationLogger logger = app.getApplicationLogger();
    if (logger instanceof FlixelLogger fl) {
      fl.errorWithSite(tag, message, exception, sourceFile, line, declaringClass, declaringMethod);
    } else {
      app.error(tag, message, exception);
    }
  }
}
