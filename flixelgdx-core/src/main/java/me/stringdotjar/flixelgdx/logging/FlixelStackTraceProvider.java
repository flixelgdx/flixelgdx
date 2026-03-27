/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

/**
 * Interface for providing stack trace information in a platform-independent way.
 */
public interface FlixelStackTraceProvider {

  /**
   * Gets the stack frame of the caller of the logger.
   *
   * @return The stack frame of the caller, or {@code null} if it cannot be determined.
   */
  FlixelStackFrame getCaller();
}
