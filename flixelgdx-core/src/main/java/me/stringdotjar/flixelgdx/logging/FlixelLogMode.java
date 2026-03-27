/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

/**
 * An enum that defines the log modes for FlixelGDX's logging system. This is used to determine how the
 * log messages should be displayed in the console.
 */
public enum FlixelLogMode {

  /**
   * Provides a simple log output that only includes the location of the log call and the message.
   * Great for people who are familiar with Haxe and its {@code trace()} function!
   */
  SIMPLE,

  /**
   * Provides a more detailed log output that includes the time of the log call, class, line number, and
   * method of the call. Great for debugging purposes and people who like a more professional feel for their game!
   */
  DETAILED
}
