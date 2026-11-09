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
package org.flixelgdx.debug.panels;

import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.ui.FlixelDebugPanels;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the panels FlixelGDX ships with, and names their ids.
 *
 * <p>The built-in panels are ordinary {@linkplain org.flixelgdx.debug.ui.FlixelDebugPanel panels} with no special
 * powers: the debugger app has never heard of them. {@link FlixelDebugManager} calls {@link #addTo(FlixelDebugManager)}
 * once when it is created, which simply registers each of them in {@code Flixel.debug.panels}. After that a game can
 * treat them like its own panels: replace one with a redesign under the same id, remove the ones it does not need, or
 * {@linkplain FlixelDebugPanels#clear() clear} them all and start from scratch.
 *
 * <pre>{@code
 * // Swap the built-in save editor for your own one.
 * Flixel.debug.panels.replace(new MySavePanel()); // new FlixelDebugPanel(FlixelDebugDefaultPanels.SAVE, ...)
 *
 * // Bring back the built-in panels after clearing them.
 * Flixel.debug.panels.clear();
 * FlixelDebugDefaultPanels.addTo(Flixel.debug);
 * }</pre>
 *
 * <p>The order they are registered in is the order of the app's menu, and their dock positions recreate the classic
 * layout: stats, performance, and logs on the left, the scene, viewport, inspector, textures, and save editor in the
 * middle, watches, controls, and trackers on the right, and the console at the bottom.
 */
public final class FlixelDebugDefaultPanels {

  /** The id of the stats panel. */
  public static final String STATS = "stats";

  /** The id of the performance graphs panel. */
  public static final String PERFORMANCE = "performance";

  /** The id of the logs panel. */
  public static final String LOGS = "logs";

  /** The id of the scene tree panel. */
  public static final String SCENE = "scene";

  /** The id of the viewport map panel. */
  public static final String VIEWPORT = "viewport";

  /** The id of the inspector panel. */
  public static final String INSPECTOR = "inspector";

  /** The id of the textures panel. */
  public static final String TEXTURES = "textures";

  /** The id of the save editor panel. */
  public static final String SAVE = "save";

  /** The id of the watches panel. */
  public static final String WATCHES = "watches";

  /** The id of the controls panel. */
  public static final String CONTROLS = "controls";

  /** The id of the trackers panel. */
  public static final String TRACKERS = "trackers";

  /** The id of the console panel. */
  public static final String CONSOLE = "console";

  private FlixelDebugDefaultPanels() {}

  /**
   * Registers every built-in panel that is not registered yet in {@code manager.panels}. A panel whose id is already
   * taken (for example by a game's own design) is left alone.
   *
   * @param manager The debug manager to add the panels to.
   */
  public static void addTo(@NotNull FlixelDebugManager manager) {
    FlixelDebugPanels panels = manager.panels;
    if (panels.indexOf(STATS) < 0) {
      panels.register(new FlixelDebugStatsPanel(manager));
    }
    if (panels.indexOf(PERFORMANCE) < 0) {
      panels.register(new FlixelDebugPerformancePanel(manager));
    }
    if (panels.indexOf(LOGS) < 0) {
      panels.register(new FlixelDebugLogsPanel(manager));
    }
    if (panels.indexOf(SCENE) < 0) {
      panels.register(new FlixelDebugScenePanel(manager));
    }
    if (panels.indexOf(VIEWPORT) < 0) {
      panels.register(new FlixelDebugViewportPanel(manager));
    }
    if (panels.indexOf(INSPECTOR) < 0) {
      panels.register(new FlixelDebugInspectorPanel(manager));
    }
    if (panels.indexOf(TEXTURES) < 0) {
      panels.register(new FlixelDebugTexturesPanel(manager));
    }
    if (panels.indexOf(SAVE) < 0) {
      panels.register(new FlixelDebugSavePanel(manager));
    }
    if (panels.indexOf(WATCHES) < 0) {
      panels.register(new FlixelDebugWatchesPanel(manager));
    }
    if (panels.indexOf(CONTROLS) < 0) {
      panels.register(new FlixelDebugControlsPanel(manager));
    }
    if (panels.indexOf(TRACKERS) < 0) {
      panels.register(new FlixelDebugTrackersPanel(manager));
    }
    if (panels.indexOf(CONSOLE) < 0) {
      panels.register(new FlixelDebugConsolePanel(manager));
    }
  }
}
