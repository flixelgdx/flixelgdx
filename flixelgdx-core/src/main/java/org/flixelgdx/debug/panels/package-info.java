/**
 * The panels FlixelGDX ships with: stats, performance, logs, scene, viewport, inspector, textures, save, watches,
 * controls, trackers, and console.
 *
 * <p>A debugger is only useful if it shows something the moment you open it. This package is that something: a
 * complete set of panels that covers the everyday questions (how fast is the game, what did it log, what is in the
 * scene, what does this object hold, what is in the save) without any setup. You get them for free as soon as the
 * game runs in debug mode.
 *
 * <p>The important part is what these panels are <em>not</em>. They are not built into the debugger app, and they
 * have no special powers. Each one is an ordinary {@link org.flixelgdx.debug.ui.FlixelDebugPanel FlixelDebugPanel},
 * built from the same widgets you can use, registered in the same list as your own panels. Think of them as the
 * furniture that comes with a rented apartment: it is there when you move in and it is good enough to live with, but
 * you are free to move it, swap a piece for your own, or carry it all out and start over.
 *
 * <h2>The built-in panels</h2>
 *
 * <p>Each panel has a stable id, kept as a constant in
 * {@link org.flixelgdx.debug.panels.FlixelDebugDefaultPanels FlixelDebugDefaultPanels}. The list below is in menu
 * order:
 *
 * <ul>
 *   <li>{@code "stats"} ({@link org.flixelgdx.debug.panels.FlixelDebugStatsPanel FlixelDebugStatsPanel}): the
 *       newest frame rate, memory, state, and camera numbers at a glance.</li>
 *   <li>{@code "performance"}
 *       ({@link org.flixelgdx.debug.panels.FlixelDebugPerformancePanel FlixelDebugPerformancePanel}): graphs of
 *       frame, render, memory, and network numbers over time. It keeps sampling while closed, so it has history
 *       when you open it.</li>
 *   <li>{@code "logs"} ({@link org.flixelgdx.debug.panels.FlixelDebugLogsPanel FlixelDebugLogsPanel}): the
 *       latest log lines with level filters, a text filter, and the full message and stack trace of the selected
 *       line.</li>
 *   <li>{@code "scene"} ({@link org.flixelgdx.debug.panels.FlixelDebugScenePanel FlixelDebugScenePanel}): the tree
 *       of the current state, its sub states, and their groups. Selecting a node selects its object.</li>
 *   <li>{@code "viewport"} ({@link org.flixelgdx.debug.panels.FlixelDebugViewportPanel FlixelDebugViewportPanel}):
 *       a live map of what a camera draws. Click to select, drag to move, right drag to pan, and scroll to zoom,
 *       even on a device without a mouse.</li>
 *   <li>{@code "inspector"}
 *       ({@link org.flixelgdx.debug.panels.FlixelDebugInspectorPanel FlixelDebugInspectorPanel}): the properties of
 *       the selected object, one section per class, with editors for values that can change.</li>
 *   <li>{@code "textures"} ({@link org.flixelgdx.debug.panels.FlixelDebugTexturesPanel FlixelDebugTexturesPanel}):
 *       every loaded texture with its size, memory, users, a duplicate mark, and a preview.</li>
 *   <li>{@code "save"} ({@link org.flixelgdx.debug.panels.FlixelDebugSavePanel FlixelDebugSavePanel}): a save
 *       editor that shows every value by its dotted path, highlights unsaved changes, and can set, remove, write, or
 *       erase values.</li>
 *   <li>{@code "watches"} ({@link org.flixelgdx.debug.panels.FlixelDebugWatchesPanel FlixelDebugWatchesPanel}):
 *       every value registered with {@code Flixel.debug.watches}.</li>
 *   <li>{@code "controls"} ({@link org.flixelgdx.debug.panels.FlixelDebugControlsPanel FlixelDebugControlsPanel}):
 *       pause, step, hitboxes, the free camera, the sample rate, the log level, and the debugger keys.</li>
 *   <li>{@code "trackers"} ({@link org.flixelgdx.debug.panels.FlixelDebugTrackersPanel FlixelDebugTrackersPanel}):
 *       one section per entry registered with {@code Flixel.debug.trackers}.</li>
 *   <li>{@code "console"} ({@link org.flixelgdx.debug.panels.FlixelDebugConsolePanel FlixelDebugConsolePanel}): a
 *       command line with Tab completion and history for the commands of {@code Flixel.debug.console}.</li>
 * </ul>
 *
 * <p>Their first positions recreate a familiar layout: stats, performance, and logs on the left; scene, viewport,
 * inspector, textures, and save in the middle; watches, controls, and trackers on the right; and the console at the
 * bottom. After the first run, the app remembers wherever the person moved them.
 *
 * <h2>How they work</h2>
 *
 * <p>None of these panels keeps game logic of its own. Each one reads from and writes to a feed (see
 * {@link org.flixelgdx.debug.feeds.FlixelDebugFeed FlixelDebugFeed}), the same objects game code reaches as
 * {@code Flixel.debug.save}, {@code Flixel.debug.inspector}, and so on. When the person presses "Save now" in the save
 * panel, the panel calls {@link org.flixelgdx.debug.feeds.FlixelDebugSave#saveNow() saveNow()} on the save feed,
 * exactly what a tool request or your own code would call. That is why a hotkey, a panel, a console command, and an
 * AI agent always agree about the state of the game.
 *
 * <p>When a tool such as an AI agent selects, moves, or edits an object, the scene panel shows an "agent" badge on
 * it and the inspector panel says the selection came from an agent, so a person can always follow along.
 *
 * <h2>Removing a panel</h2>
 *
 * <p>If your game has no use for a panel, remove it by id. The app closes its tab:
 *
 * <pre>{@code
 * Flixel.debug.panels.unregister(FlixelDebugDefaultPanels.TRACKERS);
 * Flixel.debug.panels.unregister(FlixelDebugDefaultPanels.TEXTURES);
 * }</pre>
 *
 * <p>Removing a panel does not turn off the feed behind it, so tools can still read that data.
 *
 * <h2>Replacing a panel</h2>
 *
 * <p>To redesign a panel, build your own {@link org.flixelgdx.debug.ui.FlixelDebugPanel FlixelDebugPanel} with the
 * built-in id and pass it to
 * {@link org.flixelgdx.debug.ui.FlixelDebugPanels#replace(org.flixelgdx.debug.ui.FlixelDebugPanel) replace(...)}. The
 * app shows yours in the same place. Here is a much smaller save panel that lists the values and writes them:
 *
 * <pre>{@code
 * public class SimpleSavePanel extends FlixelDebugPanel {
 *
 *   private final FlixelDebugTable rows = new FlixelDebugTable().column("Path").column("Value").setGrow(1f);
 *
 *   public SimpleSavePanel() {
 *     super(FlixelDebugDefaultPanels.SAVE, "Save");
 *     setContent(FlixelDebugUi.column(
 *         rows,
 *         FlixelDebugUi.button("Save now", () -> Flixel.debug.save.saveNow())));
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     FlixelDebugSave save = Flixel.debug.save;
 *     rows.beginRows();
 *     for (int i = 0, n = save.getRowCount(); i < n; i++) {
 *       int style = save.isRowUnsaved(i) ? FlixelDebugProtocol.TONE_WARNING : 0;
 *       rows.row(i, style).cell(save.getRowPath(i)).cell(save.getRowDisplay(i));
 *     }
 *     rows.endRows(); // Sent only when the rows really changed.
 *   }
 * }
 *
 * Flixel.debug.panels.replace(new SimpleSavePanel());
 * }</pre>
 *
 * <p>Rebuilding the rows every frame like this is fine: the table compares the new bytes with what the app already
 * has and stays quiet when nothing changed. The {@code update(...)} method only runs while the panel is open.
 *
 * <h2>Starting over</h2>
 *
 * <p>{@link org.flixelgdx.debug.ui.FlixelDebugPanels#clear() Flixel.debug.panels.clear()} removes every panel, built-in
 * ones included, so you can build a debugger that only shows your own panels.
 * {@link org.flixelgdx.debug.panels.FlixelDebugDefaultPanels#addTo(org.flixelgdx.debug.FlixelDebugManager) FlixelDebugDefaultPanels.addTo(...)}
 * brings the built-in ones back, and skips any id that is already taken, so your replacements stay in place:
 *
 * <pre>{@code
 * Flixel.debug.panels.clear();
 * Flixel.debug.panels.register(new SimpleSavePanel());
 * FlixelDebugDefaultPanels.addTo(Flixel.debug); // Adds every built-in panel except "save".
 * }</pre>
 *
 * <h2>Using a built-in panel as a starting point</h2>
 *
 * <p>The built-in panel classes are public and not final, so you can also extend one, override
 * {@code update(...)} or its hooks, and register your subclass with
 * {@link org.flixelgdx.debug.ui.FlixelDebugPanels#replace(org.flixelgdx.debug.ui.FlixelDebugPanel) replace(...)}.
 * Their source is also a good set of examples of the widget toolkit in
 * {@code org.flixelgdx.debug.ui}.
 *
 * <h2>Threading and performance</h2>
 *
 * <ul>
 *   <li>All panels run on the game's main thread. Register, replace, and remove them from there.</li>
 *   <li>A panel only updates while it is open in the app. Only the performance panel keeps sampling while closed.
 *       </li>
 *   <li>None of the built-in panels allocates in its {@code update(...)}, and several of them refresh less often
 *       than every frame (watches and trackers ten times a second, the scene when its shape changes).</li>
 * </ul>
 *
 * @see org.flixelgdx.debug.panels.FlixelDebugDefaultPanels
 * @see org.flixelgdx.debug.ui.FlixelDebugPanels
 * @see org.flixelgdx.debug.feeds.FlixelDebugFeed
 */
package org.flixelgdx.debug.panels;
