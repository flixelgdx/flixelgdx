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
package org.flixelgdx.debug;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Abstract base for custom entries shown in the debug overlay's <b>Tracker</b> panel.
 *
 * <p>Each entry contributes a named, collapsible group of {@code label -> value} pairs (rendered like the
 * Watch panel, but grouped under your entry's name). It is the right tool for a tidy, always-current dump
 * of related values, such as an inventory, an AI state machine, or a table of spawn weights, that would be
 * hard to read as a flood of {@code Flixel.info} log lines.
 *
 * <p>To keep the overlay allocation-free, hold a single reusable {@link ObjectMap} as a field and return it
 * from {@link #getTrackedValues()}, only updating its values in place each frame. Keep the same set of keys
 * between frames so the panel's row order stays stable. Register instances via
 * {@link FlixelDebugManager#addTrackerEntry(FlixelDebugTrackerEntry)} (for example
 * {@code Flixel.debug.addTrackerEntry(...)}).
 *
 * <p>Example usage:
 * <pre>{@code
 * public class EnemyTrackerEntry extends FlixelDebugTrackerEntry {
 *
 *   private final EnemyManager manager;
 *   // Reuse one map; we only update its values each frame, never reallocate it.
 *   private final ObjectMap<String, String> values = new ObjectMap<>();
 *
 *   public EnemyTrackerEntry(EnemyManager manager) {
 *     super("Enemies");
 *     this.manager = manager;
 *   }
 *
 *   @Override
 *   public ObjectMap<String, String> getTrackedValues() {
 *     values.put("Alive", String.valueOf(manager.getAliveCount()));
 *     values.put("Pooled", String.valueOf(manager.getPooledCount()));
 *     return values;
 *   }
 * }
 *
 * // Register it once, for example in your state's create():
 * Flixel.debug.addTrackerEntry(new EnemyTrackerEntry(enemyManager));
 * }</pre>
 */
public abstract class FlixelDebugTrackerEntry {

  private final String name;

  /**
   * @param name A short display name for this entry (shown as the collapsible header in the Tracker panel).
   */
  protected FlixelDebugTrackerEntry(String name) {
    this.name = name;
  }

  /** Returns the display name of this tracker entry. */
  public String getName() {
    return name;
  }

  /**
   * Called by the debug overlay (on its refresh interval) to retrieve the {@code label -> value} pairs this
   * entry wants to display. Return an empty map to hide the entry temporarily.
   *
   * <p>Prefer returning a single reused {@link ObjectMap} field whose values you update in place, rather than
   * allocating a new map each call. This keeps the debugger free of per-frame garbage.
   *
   * @return The label-to-value map to display, never {@code null}.
   */
  public abstract ObjectMap<String, String> getTrackedValues();
}
