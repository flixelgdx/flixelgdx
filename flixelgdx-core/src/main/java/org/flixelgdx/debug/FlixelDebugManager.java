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
import org.flixelgdx.debug.feeds.FlixelDebugCapture;
import org.flixelgdx.debug.feeds.FlixelDebugConsole;
import org.flixelgdx.debug.feeds.FlixelDebugControls;
import org.flixelgdx.debug.feeds.FlixelDebugFeed;
import org.flixelgdx.debug.feeds.FlixelDebugInput;
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.debug.feeds.FlixelDebugLogs;
import org.flixelgdx.debug.feeds.FlixelDebugSave;
import org.flixelgdx.debug.feeds.FlixelDebugScene;
import org.flixelgdx.debug.feeds.FlixelDebugStats;
import org.flixelgdx.debug.feeds.FlixelDebugTextures;
import org.flixelgdx.debug.feeds.FlixelDebugTrackers;
import org.flixelgdx.debug.feeds.FlixelDebugViewport;
import org.flixelgdx.debug.feeds.FlixelDebugWatches;
import org.flixelgdx.debug.panels.FlixelDebugDefaultPanels;
import org.flixelgdx.debug.remote.FlixelDebugAgent;
import org.flixelgdx.debug.remote.FlixelDebugEndpoint;
import org.flixelgdx.debug.remote.FlixelDebugTransport;
import org.flixelgdx.debug.remote.FlixelNoopDebugTransport;
import org.flixelgdx.debug.ui.FlixelDebugPanels;
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
 * <p>The debugger is made of two layers. Think of a hospital: the <strong>feeds</strong> are the departments that do
 * the actual work (measuring, recording, changing things), and the <strong>panels</strong> are the screens in the
 * waiting room that show what the departments report and let visitors press buttons. The manager is the front desk
 * that owns both.
 *
 * <h2>Feeds</h2>
 *
 * <p>Each feed samples one kind of data from the game and holds the logic to change it. Game code talks to them
 * through these fields:
 *
 * <ul>
 *   <li>{@link #stats}: frame rate, frame time, draw calls, memory, and the camera.</li>
 *   <li>{@link #logs}: the most recent log lines.</li>
 *   <li>{@link #watches}: live values that you register from game code.</li>
 *   <li>{@link #trackers}: named groups of values.</li>
 *   <li>{@link #textures}: loaded textures, their memory use, and thumbnails.</li>
 *   <li>{@link #controls}: pause, step, hitboxes, the free camera, picking, and debugger settings.</li>
 *   <li>{@link #console}: a command line with commands that you can register.</li>
 *   <li>{@link #scene}: the tree of states, sub states, and groups.</li>
 *   <li>{@link #inspector}: select an object and see or edit its properties.</li>
 *   <li>{@link #save}: view and edit the game's save data.</li>
 *   <li>{@link #viewport}: a live map of what a camera shows, with remote picking, moving, and camera flying.</li>
 *   <li>{@link #capture}: a picture of the finished game frame on request.</li>
 *   <li>{@link #input}: keys, pointer, scroll, and text injected on behalf of the debugger app.</li>
 * </ul>
 *
 * <p>Feeds also send their data, in a fixed format, to programs that subscribe to it, such as the app's tools for
 * AI agents.
 *
 * <h2>Panels</h2>
 *
 * <p>{@link #panels} lists every tab the debugger app shows. Every panel is built by the game out of widgets, the
 * built-in ones included, so you can redesign, replace, or remove any of them, or add your own, and the app follows:
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
 * // Add a panel of your own.
 * Flixel.debug.panels.register(new FlixelDebugPanel("game.ai", "Enemy AI", FlixelDebugUi.column(
 *     new FlixelDebugProperties().section("AI", s -> s
 *         .intProp("Active enemies", enemies::countLiving)
 *         .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)),
 *     FlixelDebugUi.button("Kill all", enemies::killAll))));
 *
 * // Replace a built-in panel with your own design under the same id.
 * Flixel.debug.panels.replace(new MySavePanel());
 * }</pre>
 *
 * <p>Feeds and panels are only updated while the game runs in debug mode, so they cost nothing in release builds.
 *
 * <h2>The remote debugger app</h2>
 *
 * <p>In debug mode the manager also runs a {@link FlixelDebugAgent} that talks to the debugger app over the installed
 * {@link FlixelDebugTransport}. A platform launcher installs the transport and the address of the app in
 * {@link #defaultTransport} and {@link #defaultEndpoint} before the game starts. Without a transport the agent does
 * not run.
 *
 * <h2>Thread safety</h2>
 *
 * <p>All public methods are intended to be called from the game's main thread. If you must run one from
 * another thread, you can use {@link FlixelGraphicsManager#queueMainThread Flixel.graphics.queueMainThread(...)}
 * and run the debug method there instead.
 */
public class FlixelDebugManager {

  /**
   * The transport new managers use to reach the debugger app. A platform launcher sets it before
   * {@link Flixel#start(FlixelGame, org.flixelgdx.backend.FlixelGameRunner)} runs, the same way it installs
   * {@code FlixelSave.defaultHandler}. The default is {@link FlixelNoopDebugTransport#INSTANCE}, which means no
   * remote debugger.
   */
  public static FlixelDebugTransport defaultTransport = FlixelNoopDebugTransport.INSTANCE;

  /**
   * The address of the debugger app that new managers connect to. Launchers fill it from their
   * {@code flixel.debugHost} setting; see {@link FlixelDebugEndpoint}.
   */
  public static FlixelDebugEndpoint defaultEndpoint = FlixelDebugEndpoint.DEFAULT;

  /**
   * Extra {@link FlixelBatch} instances registered by game code via {@link #trackBatch(FlixelBatch)}.
   * The framework's own batch is counted separately by the stats feed, so only user-supplied
   * batches live here.
   */
  private final FlixelArray<FlixelBatch> trackedBatches = new FlixelArray<>(false, 4);

  /** Every feed, in the order of the feed list sent to the app. */
  private final FlixelArray<FlixelDebugFeed> feeds = new FlixelArray<>();

  /** The performance feed: frame rate, frame time, draw calls, memory, and the camera. */
  public final FlixelDebugStats stats;

  /** The log feed: the most recent log lines. */
  public final FlixelDebugLogs logs;

  /**
   * The watch feed: live values shown in the debugger.
   * <pre>{@code
   * Flixel.debug.watches.addInt("Score", () -> score);
   * }</pre>
   */
  public final FlixelDebugWatches watches;

  /**
   * The save feed: view and edit the values of {@link Flixel#save} (or of another listed save), then save or erase
   * them.
   * <pre>{@code
   * Flixel.debug.save.add(profile); // Lets the app switch to a second save.
   * Flixel.debug.save.set("player.coins", "999");
   * Flixel.debug.save.saveNow();
   * }</pre>
   */
  public final FlixelDebugSave save;

  /**
   * The viewport feed: a live map of every object a camera draws, so the debugger app can pick objects, drag
   * them, and fly the camera without the device's keyboard or mouse.
   * <pre>{@code
   * Flixel.debug.viewport.setCameraIndex(1);
   * Flixel.debug.viewport.pan(50f, 0f);
   * }</pre>
   */
  public final FlixelDebugViewport viewport;

  /**
   * The capture feed: a picture of the game's finished frame on request, for the debugger app.
   * <pre>{@code
   * Flixel.debug.capture.request(1, 640);
   * }</pre>
   */
  public final FlixelDebugCapture capture;

  /**
   * The input feed: presses keys, moves and clicks a pointer, scrolls, and types text in the game on behalf of the
   * debugger app, through the same input managers real input uses.
   * <pre>{@code
   * Flixel.debug.input.key(FlixelKey.SPACE, true);
   * Flixel.debug.input.click(Flixel.debug.ids.idOf(playButton), FlixelMouseButton.LEFT);
   * }</pre>
   */
  public final FlixelDebugInput input;

  /** The tracker feed: named groups of values. */
  public final FlixelDebugTrackers trackers;

  /**
   * The texture feed: every loaded texture with its size, GPU memory, users, and a thumbnail on request.
   * <pre>{@code
   * Flixel.debug.textures.refresh();
   * long bytes = Flixel.debug.textures.getTotalBytes();
   * }</pre>
   */
  public final FlixelDebugTextures textures;

  /**
   * The control feed: pause, step, hitboxes, the free camera, picking, and debugger settings.
   * <pre>{@code
   * Flixel.debug.controls.setPaused(true);
   * }</pre>
   */
  public final FlixelDebugControls controls;

  /**
   * The console feed. Register custom commands and run command lines through it:
   * <pre>{@code
   * Flixel.debug.console.register("god", args -> player.setInvincible(true));
   * Flixel.debug.console.execute("god");
   * }</pre>
   */
  public final FlixelDebugConsole console;

  /**
   * The scene feed: the tree of the current state, its open sub states, and their groups.
   * <pre>{@code
   * Flixel.debug.scene.refresh();
   * }</pre>
   */
  public final FlixelDebugScene scene;

  /**
   * The inspector feed: select an object and see or edit its properties.
   * <pre>{@code
   * Flixel.debug.inspector.select(player);
   * Flixel.debug.inspector.register(Player.class, "Player", b -> b.intProp("Health", Player::getHealth));
   * }</pre>
   */
  public final FlixelDebugInspector inspector;

  /**
   * The registry that gives objects the numeric ids the debugger app refers to them by. The inspector and the
   * scene share it.
   */
  public final FlixelDebugObjectIds ids;

  /**
   * Every panel the debugger app shows, the built-in ones included. Register, replace, or remove panels here.
   * <pre>{@code
   * Flixel.debug.panels.register(new EnemyPanel(enemies));
   * Flixel.debug.panels.unregister("trackers");
   * }</pre>
   */
  public final FlixelDebugPanels panels;

  private FlixelDebugTransport transport = defaultTransport;
  private FlixelDebugEndpoint endpoint = defaultEndpoint;
  private FlixelDebugAgent agent;
  private boolean started;

  /** Creates the debug manager with all of its feeds and built-in panels. */
  public FlixelDebugManager() {
    ids = new FlixelDebugObjectIds();
    inspector = new FlixelDebugInspector(ids);
    controls = new FlixelDebugControls(inspector);
    stats = new FlixelDebugStats(controls, this);
    logs = new FlixelDebugLogs();
    watches = new FlixelDebugWatches();
    trackers = new FlixelDebugTrackers();
    textures = new FlixelDebugTextures(ids);
    console = new FlixelDebugConsole(this);
    save = new FlixelDebugSave();
    scene = new FlixelDebugScene(ids, inspector);
    viewport = new FlixelDebugViewport(controls, inspector);
    capture = new FlixelDebugCapture();
    input = new FlixelDebugInput(viewport, ids);
    inspector.setFocusHandler(controls::goTo);
    FlixelDebugDefaultSections.register(inspector);
    feeds.add(stats);
    feeds.add(logs);
    feeds.add(watches);
    feeds.add(trackers);
    feeds.add(textures);
    feeds.add(controls);
    feeds.add(console);
    feeds.add(scene);
    feeds.add(inspector);
    feeds.add(save);
    feeds.add(viewport);
    feeds.add(capture);
    feeds.add(input);
    panels = new FlixelDebugPanels(this);
    FlixelDebugDefaultPanels.addTo(this);
  }

  /**
   * Starts the debugger for a running game: the log feed begins to collect messages.
   *
   * <p>{@link FlixelGame} calls this when it starts in debug mode. When a transport is installed, this also opens
   * the connection to the debugger app (in the background).
   */
  public void start() {
    logs.attach();
    started = true;
    startAgent();
  }

  /**
   * Applies the input that the debugger app queued (keys, pointer, scroll, and text) to the input managers.
   *
   * <p>{@link FlixelGame} calls this every frame while the game runs in debug mode, before the input devices are
   * read, so the game sees injected input like real input. It does not allocate. See {@link #input}.
   */
  public void applyInput() {
    input.applyQueued();
  }

  /**
   * Lets the debugger take the input it owns (the free camera and Alt click picking) before the game reads it.
   *
   * <p>{@link FlixelGame} calls this every frame while the game runs in debug mode, right after the input devices
   * are updated and before the game logic runs. It does not allocate.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  public void updateInput(float elapsed) {
    controls.updateInput(elapsed);
  }

  /**
   * Updates every enabled feed, then every open panel, then talks to the debugger app.
   *
   * <p>{@link FlixelGame} calls this every frame while the game runs in debug mode, even while the game is
   * paused. It does not allocate.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  public void update(float elapsed) {
    for (int i = 0, n = feeds.getSize(); i < n; i++) {
      FlixelDebugFeed feed = feeds.get(i);
      if (feed.isEnabled()) {
        feed.update(elapsed);
      }
    }
    panels.update(elapsed);
    if (agent != null) {
      agent.update(elapsed);
    }
  }

  /**
   * Shuts the debugger down with the game: the log feed stops collecting messages, every feed is reset, and every
   * panel is closed.
   *
   * <p>{@link FlixelGame} calls this when it is destroyed.
   */
  public void dispose() {
    started = false;
    stopAgent();
    logs.detach();
    for (int i = 0, n = feeds.getSize(); i < n; i++) {
      feeds.get(i).reset();
    }
    panels.closeAll();
  }

  /**
   * Registers a {@link FlixelBatch} whose {@link FlixelBatch#getRenderCalls()} will be included in
   * the debugger's total render-call count of the stats feed. The framework's own batch is
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

  /**
   * Replaces the transport that reaches the debugger app.
   *
   * <p>When the debugger is already running, the old transport is closed and the new one is opened at once.
   * Use {@link #defaultTransport} instead to install a transport before the game starts.
   *
   * @param transport The new transport; {@link FlixelNoopDebugTransport#INSTANCE} turns the remote debugger off.
   */
  public void setTransport(@NotNull FlixelDebugTransport transport) {
    stopAgent();
    this.transport = transport;
    if (started) {
      startAgent();
    }
  }

  /**
   * Replaces the address of the debugger app. A running agent keeps its current connection; the new address is
   * used the next time the transport is set or the debugger starts.
   *
   * @param endpoint The new address.
   */
  public void setEndpoint(@NotNull FlixelDebugEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  private void startAgent() {
    if (transport == FlixelNoopDebugTransport.INSTANCE || agent != null) {
      return;
    }
    agent = new FlixelDebugAgent(this, transport);
    agent.start(endpoint.getHost(), endpoint.getPort());
  }

  private void stopAgent() {
    if (agent != null) {
      agent.stop();
      agent = null;
    }
  }

  public FlixelArray<FlixelBatch> getTrackedBatches() {
    return trackedBatches;
  }

  /**
   * Returns the transport the agent uses.
   *
   * @return The transport, {@link FlixelNoopDebugTransport#INSTANCE} when there is none.
   */
  @NotNull
  public FlixelDebugTransport getTransport() {
    return transport;
  }

  /**
   * Returns the address of the debugger app.
   *
   * @return The address.
   */
  @NotNull
  public FlixelDebugEndpoint getEndpoint() {
    return endpoint;
  }

  /**
   * Returns the agent that talks to the debugger app.
   *
   * @return The agent, or {@code null} when the debugger has not started or no transport is installed.
   */
  @Nullable
  public FlixelDebugAgent getAgent() {
    return agent;
  }

  /**
   * Returns how many feeds exist. Together with {@link #getFeed(int)} this walks the feeds in order without creating
   * an iterator.
   *
   * @return The feed count.
   */
  public int getFeedCount() {
    return feeds.getSize();
  }

  /**
   * Returns the feed at the given position.
   *
   * @param index A position from 0 to {@link #getFeedCount()} minus 1.
   * @return The feed.
   */
  @NotNull
  public FlixelDebugFeed getFeed(int index) {
    return feeds.get(index);
  }

  /**
   * Finds a feed by id.
   *
   * @param id The id to look for, for example {@code "scene"}.
   * @return The feed, or {@code null} when no feed has that id.
   */
  @Nullable
  public FlixelDebugFeed getFeed(@NotNull String id) {
    for (int i = 0, n = feeds.getSize(); i < n; i++) {
      if (feeds.get(i).getId().equals(id)) {
        return feeds.get(i);
      }
    }
    return null;
  }
}
