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
  public abstract String[] getConsoleLines();

  /**
   * Convenience method for entries that only need a single line.
   *
   * @return A single-element list, or empty if the text is {@code null}.
   */
  protected final List<String> singleLine(String text) {
    return text != null ? List.of(text) : Collections.emptyList();
  }
}
