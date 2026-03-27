/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

/**
 * Immutable snapshot of a single log message produced by {@link FlixelLogger}. Listeners
 * registered via {@link FlixelLogger#addLogListener} receive instances of this record so
 * they can display or store log data (e.g. the debug overlay console).
 */
public record FlixelLogEntry(FlixelLogLevel level, String tag, String message) {

  @Override
  public String toString() {
    return "[" + level + "] " + (tag.isEmpty() ? "" : "[" + tag + "] ") + message;
  }
}
