/**
 * The feeds of the debugger: typed streams of game data, plus the logic that reads and changes the game.
 *
 * <p>A debugger needs two things that sound alike but are not. It needs a way for <em>people</em> to look at the game
 * (buttons, tables, graphs), and it needs a way for <em>programs</em> to look at the game (an AI agent asking "what
 * is in the scene?" or "move object 42 to the left"). People like screens that change as the game grows. Programs
 * need a format that never changes under them. Feeds are the part for programs, and the logic underneath both.
 *
 * <p>Think of a newsroom. Each feed is a reporter assigned to one beat: the scene, the logs, the save file. Every
 * frame the reporter quietly gathers facts. When a subscriber (a tool in the debugger app) wants the story, the
 * reporter files it in the house style that never changes. The panels a person sees are like the front page of the
 * paper: an editor (the panel) can lay it out any way they like, but the facts all come from the same reporters, so
 * the front page and the wire service always agree.
 *
 * <h2>Where to start</h2>
 *
 * <p>Game code reaches every feed as a field of {@link org.flixelgdx.Flixel#debug Flixel.debug}:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugStats FlixelDebugStats}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#stats Flixel.debug.stats}): frame rate, frame time, draw
 *       calls, memory, and the camera, with a short history.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugLogs FlixelDebugLogs}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#logs Flixel.debug.logs}): the most recent log lines.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugWatches FlixelDebugWatches}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#watches Flixel.debug.watches}): live named values you
 *       register.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugTrackers FlixelDebugTrackers}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#trackers Flixel.debug.trackers}): named groups of values.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugTextures FlixelDebugTextures}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#textures Flixel.debug.textures}): every loaded texture, its
 *       memory, its users, and thumbnails.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugControls FlixelDebugControls}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#controls Flixel.debug.controls}): pause, step, hitboxes, the
 *       free camera, picking, and the debugger's own settings.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugConsole FlixelDebugConsole}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#console Flixel.debug.console}): a command line with commands
 *       you register.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugScene FlixelDebugScene}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#scene Flixel.debug.scene}): the tree of states, sub states,
 *       and groups.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugInspector FlixelDebugInspector}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#inspector Flixel.debug.inspector}): the selected object and
 *       its properties, grouped by class.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugSave FlixelDebugSave}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#save Flixel.debug.save}): every value of the save, with
 *       editing, writing, and erasing.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugViewport FlixelDebugViewport}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#viewport Flixel.debug.viewport}): a live map of what a camera
 *       draws, with remote picking, moving, panning, and zooming.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugCapture FlixelDebugCapture}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#capture Flixel.debug.capture}): a picture of the finished
 *       frame on request.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugInput FlixelDebugInput}
 *       ({@link org.flixelgdx.debug.FlixelDebugManager#input Flixel.debug.input}): keys, pointer, scroll, and text
 *       injected on behalf of the app.</li>
 * </ul>
 *
 * <h2>Watches and console commands</h2>
 *
 * <p>The two feeds you will use most are watches and the console. A watch is a name and a small function that reads
 * a value. Use the typed methods for numbers and flags, because they avoid boxing and tell the app the exact type:
 *
 * <pre>{@code
 * Flixel.debug.watches.addFloat("Player X", player::getX);
 * Flixel.debug.watches.addInt("Enemies", () -> enemies.countLiving());
 * Flixel.debug.watches.addBoolean("On ground", player::isOnGround);
 * Flixel.debug.watches.add("State", () -> machine.getName()); // Any object, shown as text.
 *
 * Flixel.debug.watches.remove("Player X");
 * }</pre>
 *
 * <p>A console command is a name and a handler. The person types the name and its arguments in the app, a tool sends
 * the same line, or your own code runs it. Read the arguments with the typed getters of
 * {@link org.flixelgdx.debug.FlixelDebugCommandArgs FlixelDebugCommandArgs}, which take a fallback for a missing or
 * malformed argument:
 *
 * <pre>{@code
 * Flixel.debug.console.register("spawn", "spawn <type> [count]", "Creates enemies at the cursor.", args -> {
 *   String type = args.getString(0, "grunt"); // 0 is the first argument after the name.
 *   int count = args.getInt(1, 1);
 *   spawner.spawn(type, count);
 *   Flixel.info("Spawned {} {}.", count, type);
 * });
 *
 * Flixel.debug.console.execute("spawn \"big grunt\" 3"); // Double quotes keep spaces in one argument.
 * }</pre>
 *
 * <p>Both of these show up in the built-in watches and console panels, and both are also readable by tools. You
 * register a value or a command once, and every way of using the debugger sees it.
 *
 * <h2>Why panels and tools both use feeds</h2>
 *
 * <p>The built-in panels hold no game logic of their own. When a person clicks "Save now" in the save panel, the
 * panel calls {@link org.flixelgdx.debug.feeds.FlixelDebugSave#saveNow() Flixel.debug.save.saveNow()}. When an AI
 * agent asks to write the save, the request arrives as a feed action and runs the very same method. That has three
 * benefits:
 *
 * <ul>
 *   <li>A person and a tool can never see different answers, because there is only one source.</li>
 *   <li>You can replace any panel with your own design without breaking a single tool, because tools never read
 *       panels.</li>
 *   <li>Your own code can do everything the debugger does, with the same methods:</li>
 * </ul>
 *
 * <pre>{@code
 * Flixel.debug.inspector.select(player);                // Select an object, as a click in the app would.
 * Flixel.debug.save.set("player.coins", "999");         // Edit a save value by its dotted path.
 * Flixel.debug.viewport.pan(0f, -100f);                 // Fly the camera up a little.
 * Flixel.debug.input.key(FlixelKey.SPACE, true);        // Press a key, as if a player did.
 * Flixel.debug.capture.request(1, 640);                 // Ask for a picture of the next frame.
 * }</pre>
 *
 * <p>Actions that arrive from a tool mark the objects they touch with
 * {@link org.flixelgdx.debug.FlixelDebugObjectIds#markAgentTouched(Object) markAgentTouched(...)}, and the built-in
 * scene and inspector panels show an "agent" badge on them, so a person can always see what a tool changed.
 *
 * <h2>How a feed works</h2>
 *
 * <p>Every feed implements {@link org.flixelgdx.debug.feeds.FlixelDebugFeed FlixelDebugFeed}, and most extend
 * {@link org.flixelgdx.debug.feeds.FlixelDebugFeedBase FlixelDebugFeedBase}, which keeps the id, kind, enabled flag,
 * and revision counter for them. A feed's life has four parts:
 *
 * <ol>
 *   <li><b>Sample</b>: {@link org.flixelgdx.debug.feeds.FlixelDebugFeed#update(float) update(...)} runs every frame
 *       in debug mode, even while the game is paused.</li>
 *   <li><b>Announce</b>: when the data changed, the feed raises its
 *       {@link org.flixelgdx.debug.feeds.FlixelDebugFeed#getRevision() revision}. The agent only sends a feed whose
 *       revision moved.</li>
 *   <li><b>Write</b>: {@link org.flixelgdx.debug.feeds.FlixelDebugFeed#write(org.flixelgdx.debug.remote.FlixelDebugMessageWriter) write(...)}
 *       encodes the payload in the format {@code DEBUG_PROTOCOL.md} gives its kind. One-off extras, such as a
 *       thumbnail or the result of a command, go through
 *       {@link org.flixelgdx.debug.feeds.FlixelDebugFeed#writePendingPayload(org.flixelgdx.debug.remote.FlixelDebugMessageWriter) writePendingPayload(...)}.
 *       </li>
 *   <li><b>Act</b>: {@link org.flixelgdx.debug.feeds.FlixelDebugFeed#handleAction(int, org.flixelgdx.debug.remote.FlixelDebugMessageReader) handleAction(...)}
 *       runs a request from a tool, such as selecting an object or setting a save value.</li>
 * </ol>
 *
 * <p>A feed only sends data while the app subscribes to it, but it keeps sampling either way, because the panels
 * read from it. Turn a feed off with
 * {@link org.flixelgdx.debug.feeds.FlixelDebugFeed#setEnabled(boolean) setEnabled(false)} when you want it to stop
 * sampling entirely. The panels that rely on it then stop changing too.
 *
 * <h2>Threading and performance</h2>
 *
 * <ul>
 *   <li>Every feed method runs on the game's main thread, and must be called from there.</li>
 *   <li>{@code update(...)} runs every frame and never allocates. The getters you register (watches, inspector rows)
 *       are read often, so keep them cheap and allocation free as well.</li>
 *   <li>Work that only happens when someone asks, such as a thumbnail or a frame capture, may allocate, because it
 *       never runs in the frame loop on its own.</li>
 *   <li>A getter or command that throws is reported in the app and the log instead of crashing the game.</li>
 *   <li>{@link org.flixelgdx.debug.feeds.FlixelDebugFeed#reset() reset()} clears recorded history, but keeps what
 *       your code registered, such as watches and commands.</li>
 * </ul>
 *
 * @see org.flixelgdx.debug.FlixelDebugManager
 * @see org.flixelgdx.debug.feeds.FlixelDebugFeed
 * @see org.flixelgdx.debug.ui.FlixelDebugPanel
 */
package org.flixelgdx.debug.feeds;
