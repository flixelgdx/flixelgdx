/**
 * The FlixelGDX debugger: look inside a running game, change it while it runs, and let tools drive it.
 *
 * <p>Bugs in games are often about things you cannot see: an enemy that is one pixel inside a wall, a timer that
 * never resets, a save value that was written with the wrong key. Printing log lines helps, but you have to guess
 * what to print before you run the game. The debugger turns that around. It shows the whole game (its scene, its
 * objects, its numbers, its textures, its save data) while it runs, lets you pause, step, and change things, and
 * lets programs such as an AI agent do the same.
 *
 * <p>Think of a race car and its pit wall. The car (your game) is out on the track and carries sensors everywhere.
 * A radio link (the agent) sends their readings to the pit wall (the debugger app, a separate desktop program),
 * where the crew watches screens they designed themselves and can radio instructions back. The car never needs a
 * screen of its own, and the pit wall never needs to know how the car is built. It only shows what the car sends.
 *
 * <h2>Where to start</h2>
 *
 * <p>Everything is reached through {@link org.flixelgdx.Flixel#debug Flixel.debug}, the
 * {@link org.flixelgdx.debug.FlixelDebugManager FlixelDebugManager} that the framework creates for you when the game
 * starts. You never construct it yourself. The most common first steps are:
 *
 * <ul>
 *   <li>Show a value you care about with
 *       {@link org.flixelgdx.debug.FlixelDebugManager#watches Flixel.debug.watches}.</li>
 *   <li>Add a cheat or test command with {@link org.flixelgdx.debug.FlixelDebugManager#console Flixel.debug.console}.
 *       </li>
 *   <li>Show your own classes in the inspector with
 *       {@link org.flixelgdx.debug.FlixelDebugManager#inspector Flixel.debug.inspector}.</li>
 *   <li>Add a whole panel of your own to the app with
 *       {@link org.flixelgdx.debug.FlixelDebugManager#panels Flixel.debug.panels}.</li>
 * </ul>
 *
 * <pre>{@code
 * // In your FlixelState.create():
 * Flixel.debug.watches.addFloat("Player X", player::getX);
 * Flixel.debug.watches.addInt("Score", () -> score);
 *
 * Flixel.debug.console.register("god", "god [on|off]", "Makes the player invincible.", args -> {
 *   player.invincible = args.getBoolean(0, !player.invincible);
 *   Flixel.info("God mode: " + player.invincible);
 * });
 *
 * Flixel.debug.inspector.register(Player.class, "Player", b -> b
 *     .intProp("Health", Player::getHealth, Player::setHealth)
 *     .boolProp("On ground", Player::isOnGround));
 * }</pre>
 *
 * <p>Calls like these are safe to leave in your code. The debugger only updates while the game runs in debug mode,
 * so in a release build they register a few small objects and then cost nothing.
 *
 * <h2>How the debug packages fit together</h2>
 *
 * <p>The debugger is split into a few packages, each with one job:
 *
 * <ul>
 *   <li>{@code org.flixelgdx.debug} (this package): the manager, plus the shared helpers every other part uses,
 *       such as object ids, scene paths, and picking.</li>
 *   <li>{@code org.flixelgdx.debug.feeds}: the <em>feeds</em>. Each feed watches one kind of game data (stats, logs,
 *       the scene, the save, and so on) and holds the logic that changes it. Feeds also send their data in a fixed
 *       format to programs, such as the app's tools for AI agents. See
 *       {@link org.flixelgdx.debug.feeds.FlixelDebugFeed FlixelDebugFeed}.</li>
 *   <li>{@code org.flixelgdx.debug.ui}: the toolkit for <em>panels</em>, the tabs a person sees in the app. A panel
 *       is a tree of widgets (buttons, tables, graphs, and so on) that the game builds and the app only draws. See
 *       {@link org.flixelgdx.debug.ui.FlixelDebugPanel FlixelDebugPanel}.</li>
 *   <li>{@code org.flixelgdx.debug.panels}: the panels FlixelGDX ships with, built with that same toolkit. See
 *       {@link org.flixelgdx.debug.panels.FlixelDebugDefaultPanels FlixelDebugDefaultPanels}.</li>
 *   <li>{@code org.flixelgdx.debug.inspector}: the building blocks for rows of values, used by the inspector and by
 *       property grids. See {@link org.flixelgdx.debug.inspector.FlixelSectionBuilder FlixelSectionBuilder}.</li>
 *   <li>{@code org.flixelgdx.debug.remote}: the wire protocol and the agent that carries messages between the game
 *       and the app. See {@link org.flixelgdx.debug.remote.FlixelDebugAgent FlixelDebugAgent}.</li>
 * </ul>
 *
 * <p>The most important idea is the split between panels and feeds. Panels are for people: the game decides what
 * every button and row does, and you can redesign, replace, or remove any panel, the built-in ones included. Feeds
 * are for programs: their format is fixed and written down in {@code DEBUG_PROTOCOL.md} at the repository root, so a
 * tool that reads the scene keeps working no matter how you redesign the scene panel. Both sides share the same
 * feed objects, so a panel button and a tool request run exactly the same game code.
 *
 * <h2>Connecting to the debugger app</h2>
 *
 * <p>When the game starts in debug mode, the manager starts a
 * {@link org.flixelgdx.debug.remote.FlixelDebugAgent FlixelDebugAgent}, which connects to the app over the
 * {@link org.flixelgdx.debug.remote.FlixelDebugTransport FlixelDebugTransport} that the platform launcher installed
 * in {@link org.flixelgdx.debug.FlixelDebugManager#defaultTransport FlixelDebugManager.defaultTransport}. You do not
 * have to set anything up. The game looks for the app on the same computer by default, and keeps trying every few
 * seconds, so you can start the app before or after the game.
 *
 * <p>To reach the app on another machine, give the game a {@code host[:port]} address through the launcher's
 * {@code flixel.debugHost} setting. See {@link org.flixelgdx.debug.remote.FlixelDebugEndpoint FlixelDebugEndpoint}
 * for where each platform reads it from. A phone connected over USB can reach the app on your computer through
 * {@code adb reverse tcp:6510 tcp:6510} without any address at all.
 *
 * <h2>Pausing, stepping, and hitboxes</h2>
 *
 * <p>The controls feed ({@link org.flixelgdx.debug.feeds.FlixelDebugControls FlixelDebugControls}) works even when
 * no app is connected. These keys are on by default and can be changed:
 *
 * <ul>
 *   <li>F3 pauses and resumes the game.</li>
 *   <li>F2 shows and hides the hitbox of every object.</li>
 *   <li>F4 turns the free camera on and off. Fly it with W, A, S, D, the arrow keys, the mouse wheel, and the right
 *       mouse button, while the game does not react to those inputs.</li>
 *   <li>Alt plus a left click selects the object under the mouse in the inspector, and outlines it in yellow.</li>
 * </ul>
 *
 * <pre>{@code
 * Flixel.debug.controls.setPaused(true);
 * Flixel.debug.controls.stepFrame();       // Run exactly one frame, then stay paused.
 * Flixel.debug.controls.setHitboxes(true);
 * Flixel.debug.controls.goTo(1200f, 300f); // Fly the free camera to a spot in the world.
 * }</pre>
 *
 * <p>Any class can draw its own hitbox by implementing
 * {@link org.flixelgdx.debug.FlixelDebugDrawable FlixelDebugDrawable}. Every
 * {@link org.flixelgdx.FlixelObject FlixelObject} already does.
 *
 * <h2>Finding objects: ids, paths, and picking</h2>
 *
 * <p>The app is a separate program, so it cannot hold a reference to a Java object. The helpers in this package give
 * every object a name the app can use:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.FlixelDebugObjectIds FlixelDebugObjectIds} hands out small numbers, like coat
 *       check tickets. The same object always gets the same number, and a number is never reused. Game code reaches
 *       the shared registry as {@link org.flixelgdx.debug.FlixelDebugManager#ids Flixel.debug.ids}.</li>
 *   <li>{@link org.flixelgdx.debug.FlixelDebugPaths FlixelDebugPaths} turns an object into a readable address such
 *       as {@code state.3.0} (the first member of the fourth member of the current state) and back again. The
 *       console's {@code select <path>} command uses it.</li>
 *   <li>{@link org.flixelgdx.debug.FlixelDebugPicker FlixelDebugPicker} finds the object drawn at a point on the
 *       screen, topmost first, the way a player would point at it.</li>
 * </ul>
 *
 * <p>When a tool (such as an AI agent) selects, moves, edits, or clicks an object through a feed, the object is
 * marked with {@link org.flixelgdx.debug.FlixelDebugObjectIds#markAgentTouched(Object) markAgentTouched(...)}. The
 * built-in panels show those objects with an "agent" badge, so you can always see what a tool did to your game.
 *
 * <h2>Objects the debugger cannot walk on its own</h2>
 *
 * <p>The scene tree, the viewport map, and the picker know how to walk groups and how to measure objects with a
 * hitbox. Anything else, such as a user interface widget with its own children, implements
 * {@link org.flixelgdx.debug.FlixelDebugNode FlixelDebugNode} to list its children and report the box it covers on a
 * camera. It then appears in the scene tree, gets a box on the viewport map, and can be picked and clicked like any
 * other object. {@link org.flixelgdx.debug.FlixelDebugNodes FlixelDebugNodes} is the helper that reads children from
 * either kind.
 *
 * <h2>Grouped values and textures</h2>
 *
 * <p>For a tidy, always current table of related values (an inventory, an AI state machine, spawn weights), extend
 * {@link org.flixelgdx.debug.FlixelDebugTrackerEntry FlixelDebugTrackerEntry} and add it with
 * {@code Flixel.debug.trackers.add(...)}. For pictures of textures,
 * {@link org.flixelgdx.debug.FlixelDebugThumbnails FlixelDebugThumbnails} reads pixels back from the graphics card
 * and turns them into small PNG files on request, when the graphics backend supports it.
 *
 * <h2>Threading and performance</h2>
 *
 * <ul>
 *   <li>Call every debugger method from the game's main thread. From another thread, queue the call with
 *       {@link org.flixelgdx.graphics.FlixelGraphicsManager#queueMainThread Flixel.graphics.queueMainThread(...)}.
 *       </li>
 *   <li>The debugger runs every frame in debug mode, so its frame work never allocates. Getters you give it
 *       (watches, inspector rows, labels) are read every frame or every few frames, so keep them cheap and
 *       allocation free too.</li>
 *   <li>Panels only update while they are open in the app, and feeds only send data while the app subscribes to
 *       them, so a debugger nobody is looking at costs very little.</li>
 *   <li>A getter, command, or panel that throws is logged once and does not crash the game.</li>
 * </ul>
 *
 * @see org.flixelgdx.debug.FlixelDebugManager
 * @see org.flixelgdx.debug.feeds.FlixelDebugFeed
 * @see org.flixelgdx.debug.ui.FlixelDebugPanel
 */
package org.flixelgdx.debug;
