/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.logging;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base for custom entries that are displayed in the debug overlay console.
 *
 * <p>Extend this class and implement {@link #getConsoleLines()} to supply one or more lines
 * of text that will be rendered in the debug console alongside regular log output. Register
 * instances via {@link FlixelLogger#addConsoleEntry(FlixelDebugConsoleEntry)}.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class EnemyCountEntry extends FlixelDebugConsoleEntry {
 *   private final EnemyManager manager;
 *
 *   public EnemyCountEntry(EnemyManager manager) {
 *     super("Enemy Count");
 *     this.manager = manager;
 *   }
 *
 *   @Override
 *   public List<String> getConsoleLines() {
 *     return List.of("Enemies alive: " + manager.getAliveCount());
 *   }
 * }
 * }</pre>
 */
public abstract class FlixelDebugConsoleEntry {

  private final String name;

  /**
   * @param name A short display name for this entry (shown as a header in the console).
   */
  protected FlixelDebugConsoleEntry(String name) {
    this.name = name;
  }

  /** Returns the display name of this console entry. */
  public String getName() {
    return name;
  }

  /**
   * Called each frame (or each stats-refresh interval) by the debug overlay to retrieve the
   * lines of text this entry wants to display. Return an empty list to hide the entry
   * temporarily.
   *
   * @return An unmodifiable list of display lines, never {@code null}.
   */
  public abstract List<String> getConsoleLines();

  /**
   * Convenience method for entries that only need a single line.
   *
   * @return A single-element list, or empty if the text is {@code null}.
   */
  protected final List<String> singleLine(String text) {
    return text != null ? List.of(text) : Collections.emptyList();
  }
}
