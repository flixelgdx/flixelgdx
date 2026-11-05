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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single entry point for everything related to the FlixelGDX debugger.
 *
 * <p>An instance of this class is automatically created when
 * {@link Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner)} runs and is
 * exposed as the static field {@link Flixel#debug}, mirroring how {@link Flixel#sound},
 * {@link Flixel#assets}, and friends work.
 *
 * <p>The debugger is made of <strong>panels</strong>. A panel samples one kind of data from the game
 * (performance numbers, log lines, watched values, and so on) and knows how to encode it for the
 * remote debugger app. Think of the manager as the front desk of a hospital: it keeps the list of
 * departments (the panels), makes sure each one gets a turn every frame, and lets staff look one up by
 * name. The built-in panels are ready-made fields:
 *
 * <ul>
 *   <li>{@link #stats}: frame rate, frame time, draw calls, memory, and the camera.</li>
 *   <li>{@link #logs}: the most recent log lines.</li>
 *   <li>{@link #watches}: live values that you register from game code.</li>
 *   <li>{@link #trackers}: named groups of values.</li>
 *   <li>{@link #controls}: pause, step, hitboxes, and debugger settings.</li>
 *   <li>{@link #console}: a command line with commands that you can register.</li>
 *   <li>{@link #inspector}: select an object and see or edit its properties.</li>
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.watches.addFloat("Player X", player::getX);
 * Flixel.debug.console.register("god", "god [on|off]", "Makes the player invincible.", args -> {
 *   player.setInvincible(args.getBoolean(0, true));
 * });
 * Flixel.debug.controls.setHitboxes(true);
 *
 * // Show your own class in the inspector.
 * Flixel.debug.inspector.register(Player.class, "Player", b -> b
 *     .intProp("Health", Player::getHealth, Player::setHealth));
 *
 * // Add a panel with rows and buttons.
 * Flixel.debug.register(new FlixelDebugCustomPanel("ai", "Enemy AI", p -> p
 *     .intProp("Active enemies", () -> enemies.countLiving())
 *     .action("Kill all", enemies::killAll)));
 *
 * // Add a fully custom panel and find it again later.
 * Flixel.debug.register(new ScorePanel());
 * ScorePanel score = Flixel.debug.get("custom.score", ScorePanel.class);
 * }</pre>
 *
 * <p>Panels are only updated while the game runs in debug mode, so they cost nothing in release builds.
 *
 * <h2>Thread safety</h2>
 *
 * <p>All public methods are intended to be called from the game's main thread. If you must run one from
 * another thread, you can use {@link FlixelGraphicsManager#queueMainThread Flixel.graphics.queueMainThread(...)}
 * and run the debug method there instead.
 */
public class FlixelDebugManager {

  /**
   * Extra {@link FlixelBatch} instances registered by game code via {@link #trackBatch(FlixelBatch)}.
   * The framework's own batch is counted separately by the stats panel, so only user-supplied
   * batches live here.
   */
  private final FlixelArray<FlixelBatch> trackedBatches = new FlixelArray<>(false, 4);

  /** Every registered panel, in registration order. */
  private final FlixelArray<FlixelDebugPanel> panels = new FlixelArray<>();

  /** The performance panel: frame rate, frame time, draw calls, memory, and the camera. */
  public final FlixelDebugStatsPanel stats;

  /** The log panel: the most recent log lines. */
  public final FlixelDebugLogPanel logs;

  /**
   * The watch panel: live values shown in the debugger.
   * <pre>{@code
   * Flixel.debug.watches.addInt("Score", () -> score);
   * }</pre>
   */
  public final FlixelDebugWatchPanel watches;

  /** The tracker panel: named groups of values. */
  public final FlixelDebugTrackerPanel trackers;

  /**
   * The control panel: pause, step, hitboxes, and debugger settings.
   * <pre>{@code
   * Flixel.debug.controls.setPaused(true);
   * }</pre>
   */
  public final FlixelDebugControlsPanel controls;

  /**
   * The console panel. Register custom commands and run command lines through it:
   * <pre>{@code
   * Flixel.debug.console.register("god", args -> player.setInvincible(true));
   * Flixel.debug.console.execute("god");
   * }</pre>
   */
  public final FlixelDebugConsolePanel console;

  /**
   * The inspector panel: select an object and see or edit its properties.
   * <pre>{@code
   * Flixel.debug.inspector.select(player);
   * Flixel.debug.inspector.register(Player.class, "Player", b -> b.intProp("Health", Player::getHealth));
   * }</pre>
   */
  public final FlixelDebugInspectorPanel inspector;

  /**
   * The registry that gives objects the numeric ids the debugger app refers to them by. The inspector and the
   * scene share it.
   */
  public final FlixelDebugObjectIds ids;

  /** Goes up whenever a panel is registered or unregistered. */
  private long panelRevision;

  /** Creates the debug manager with all of its built-in panels registered. */
  public FlixelDebugManager() {
    controls = new FlixelDebugControlsPanel();
    stats = new FlixelDebugStatsPanel(controls, this);
    logs = new FlixelDebugLogPanel();
    watches = new FlixelDebugWatchPanel();
    trackers = new FlixelDebugTrackerPanel();
    console = new FlixelDebugConsolePanel(this);
    ids = new FlixelDebugObjectIds();
    inspector = new FlixelDebugInspectorPanel(ids);
    FlixelDebugDefaultSections.register(inspector);
    register(stats);
    register(logs);
    register(watches);
    register(trackers);
    register(controls);
    register(console);
    register(inspector);
  }

  /**
   * Registers a panel. The panel is updated every frame from now on and listed to the debugger app.
   *
   * @param panel The panel to add.
   * @param <T> The panel type, so the call can be chained: {@code ScorePanel p = Flixel.debug.register(new ScorePanel())}.
   * @return The same panel.
   * @throws IllegalArgumentException If a panel with the same id is already registered.
   */
  public <T extends FlixelDebugPanel> T register(@NotNull T panel) {
    if (indexOf(panel.getId()) >= 0) {
      throw new IllegalArgumentException("A debug panel with the id '" + panel.getId() + "' is already registered.");
    }
    panels.add(panel);
    panelRevision++;
    return panel;
  }

  /**
   * Removes a panel.
   *
   * @param id The id of the panel to remove.
   * @return The removed panel, or {@code null} when no panel has that id.
   */
  @Nullable
  public FlixelDebugPanel unregister(@NotNull String id) {
    int index = indexOf(id);
    if (index < 0) {
      return null;
    }
    FlixelDebugPanel removed = panels.removeIndex(index);
    panelRevision++;
    return removed;
  }

  /**
   * Finds a panel by id.
   *
   * @param id The id to look for.
   * @return The panel, or {@code null} when no panel has that id.
   */
  @Nullable
  public FlixelDebugPanel get(@NotNull String id) {
    int index = indexOf(id);
    return index >= 0 ? panels.get(index) : null;
  }

  /**
   * Finds a panel by id and checks its type.
   *
   * @param id The id to look for.
   * @param type The class the panel must be an instance of.
   * @param <T> The panel type.
   * @return The panel, or {@code null} when no panel has that id or it is not a {@code type}.
   */
  @Nullable
  public <T extends FlixelDebugPanel> T get(@NotNull String id, @NotNull Class<T> type) {
    FlixelDebugPanel panel = get(id);
    return panel != null && type.isInstance(panel) ? type.cast(panel) : null;
  }

  /**
   * Starts the debugger for a running game: the log panel begins to collect messages.
   *
   * <p>{@link FlixelGame} calls this when it starts in debug mode.
   */
  public void start() {
    logs.attach();
  }

  /**
   * Updates every enabled panel once.
   *
   * <p>{@link FlixelGame} calls this every frame while the game runs in debug mode, even while the game is
   * paused. It does not allocate.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  public void update(float elapsed) {
    for (int i = 0; i < panels.getSize(); i++) {
      FlixelDebugPanel panel = panels.get(i);
      if (panel.isEnabled()) {
        panel.update(elapsed);
      }
    }
  }

  /**
   * Shuts the debugger down with the game: the log panel stops collecting messages and every panel is
   * reset.
   *
   * <p>{@link FlixelGame} calls this when it is destroyed.
   */
  public void dispose() {
    logs.detach();
    for (int i = 0; i < panels.getSize(); i++) {
      panels.get(i).reset();
    }
  }

  /**
   * Registers a {@link FlixelBatch} whose {@link FlixelBatch#getRenderCalls()} will be included in
   * the debugger's total render-call count shown in the stats panel. The framework's own batch is
   * already counted automatically; call this for any additional batches your game creates.
   *
   * <p>Registering a batch that is already tracked is a no-op. The batch must remain valid (not
   * disposed) for as long as it is registered. Call {@link #untrackBatch(FlixelBatch)} before
   * disposing a tracked batch.
   *
   * <p>Example:
   * <pre>{@code
   * FlixelBatch uiBatch = new FlixelSpriteBatch();
   * Flixel.debug.trackBatch(uiBatch);
   * }</pre>
   *
   * @param batch The batch to track. Must not be {@code null}.
   */
  public void trackBatch(@NotNull FlixelBatch batch) {
    if (batch == null || trackedBatches.contains(batch, true)) {
      return;
    }
    trackedBatches.add(batch);
  }

  /**
   * Removes a batch that was previously registered via {@link #trackBatch(FlixelBatch)}.
   * Passing a batch that was never registered is a no-op.
   *
   * @param batch The batch to stop tracking.
   */
  public void untrackBatch(@NotNull FlixelBatch batch) {
    if (batch != null) {
      trackedBatches.removeValue(batch, true);
    }
  }

  private int indexOf(String id) {
    for (int i = 0, n = panels.getSize(); i < n; i++) {
      if (panels.get(i).getId().equals(id)) {
        return i;
      }
    }
    return -1;
  }

  public FlixelArray<FlixelBatch> getTrackedBatches() {
    return trackedBatches;
  }

  /**
   * Returns how many panels are registered. Together with {@link #getPanel(int)} this walks the panels in
   * registration order without creating an iterator.
   *
   * @return The panel count.
   */
  public int getPanelCount() {
    return panels.getSize();
  }

  /**
   * Returns the panel at the given position in registration order.
   *
   * @param index A position from 0 to {@link #getPanelCount()} minus 1.
   * @return The panel.
   */
  @NotNull
  public FlixelDebugPanel getPanel(int index) {
    return panels.get(index);
  }

  /**
   * Returns a number that goes up whenever a panel is registered or unregistered.
   *
   * <p>The network agent compares it with the value it saw last and re-sends the panel list when it
   * differs.
   *
   * @return The panel list revision.
   */
  public long getPanelRevision() {
    return panelRevision;
  }
}
