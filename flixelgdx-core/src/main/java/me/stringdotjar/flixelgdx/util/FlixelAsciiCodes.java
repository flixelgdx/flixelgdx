/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

/**
 * ANSI escape sequences for console text styling used by {@link me.stringdotjar.flixelgdx.logging.FlixelLogger},
 * although you may find this class useful for other purposes.
 */
public final class FlixelAsciiCodes {

  public static final String RESET = "\u001B[0m";
  public static final String BOLD = "\033[0;1m";
  public static final String ITALIC = "\u001B[3m";
  public static final String UNDERLINE = "\u001B[4m";
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";

  private FlixelAsciiCodes() {}
}
