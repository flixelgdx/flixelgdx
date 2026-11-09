/**
 * The toolkit for debugger panels: the widgets a game builds and the remote debugger app draws.
 *
 * <p>Most debuggers ship their screens inside the debugger program. That means a game that wants a new screen, or a
 * different layout for an old one, has to change the debugger itself. FlixelGDX turns this around: the debugger app
 * has no built-in knowledge of any panel at all. It only knows how to draw a small set of widgets (buttons, tables,
 * graphs, and so on) and how to report what the person did with them. The game builds every panel, the built-in ones
 * included, out of those widgets, and decides what every click means. So the game is the single source of truth,
 * and any panel can be redesigned, replaced, or removed from game code alone.
 *
 * <p>Think of a puppet show. The game holds the strings: it builds the puppets (widgets), arranges them on the stage
 * (the panel), and moves them whenever something changes. The app is the stage and the curtain: the audience sees the
 * puppets move, and when someone pokes a puppet, the stage tells the puppeteer, who decides what happens next. The
 * stage never decides anything by itself, which is why a panel can do anything the game can do.
 *
 * <h2>Where to start</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugPanel FlixelDebugPanel}: one tab of the app. Extend it, give it a
 *       root widget, and refresh the widgets in {@code update(...)}.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugPanels FlixelDebugPanels}: the list of every panel the app shows,
 *       reached as {@link org.flixelgdx.debug.FlixelDebugManager#panels Flixel.debug.panels}. Register, replace, and
 *       remove panels here.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugUi FlixelDebugUi}: short factory methods such as
 *       {@code column(...)}, {@code button(...)}, and {@code checkbox(...)}, so a panel reads like an outline.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugWidget FlixelDebugWidget}: the base class of every widget, with the
 *       properties they all share (visibility, enabled, tooltip, size, grow, and style).</li>
 * </ul>
 *
 * <h2>Building a custom panel</h2>
 *
 * <p>Create the widgets once, in the constructor, and keep references to the ones you want to change later. Widgets
 * built with a getter (a checkbox with a {@code BooleanSupplier}, a label with an appender) refresh themselves every
 * frame. For anything else, set the new values in {@code update(...)}:
 *
 * <pre>{@code
 * import static org.flixelgdx.debug.ui.FlixelDebugUi.*;
 *
 * public class EnemyPanel extends FlixelDebugPanel {
 *
 *   private final FlixelDebugProgress wave = new FlixelDebugProgress();
 *   private final EnemyGroup enemies;
 *
 *   public EnemyPanel(EnemyGroup enemies) {
 *     super("game.enemies", "Enemies"); // A stable id, and the title on the tab.
 *     this.enemies = enemies;
 *     setDock(FlixelDebugProtocol.DOCK_RIGHT);
 *     setContent(column(
 *         heading("Enemies"),
 *         label(out -> out.concat(enemies.countLiving()).concat(" alive")),
 *         checkbox("Frozen", enemies::isFrozen, enemies::setFrozen),
 *         slider(0.0, 2.0, () -> enemies.speedScale, v -> enemies.speedScale = (float) v),
 *         wave,
 *         row(
 *             button("Spawn wave", enemies::spawnWave),
 *             button("Kill all", enemies::killAll)
 *                 .setConfirm("Kill every enemy?")
 *                 .setStyle(FlixelDebugProtocol.TONE_ERROR))));
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed); // Refreshes the label, checkbox, and slider above.
 *     wave.setValue(enemies.getWaveProgress());
 *   }
 * }
 *
 * // In your state's create():
 * Flixel.debug.panels.register(new EnemyPanel(enemies));
 * }</pre>
 *
 * <p>Pick ids that will not clash with the built-in ones, for example by starting them with {@code "game."}. The app
 * remembers where the person put each panel by its id, so keep an id the same between versions of your game.
 *
 * <h2>Replacing a built-in panel</h2>
 *
 * <p>The built-in panels are registered in the same list as yours and are not special in any way. To redesign one,
 * build your own panel with the built-in id and call
 * {@link org.flixelgdx.debug.ui.FlixelDebugPanels#replace(org.flixelgdx.debug.ui.FlixelDebugPanel) replace(...)}.
 * The app keeps the panel where the person put it and simply shows yours. Read the data from the same feed the
 * built-in panel used, so tools and panels keep agreeing:
 *
 * <pre>{@code
 * public class TinyStatsPanel extends FlixelDebugPanel {
 *
 *   public TinyStatsPanel() {
 *     super(FlixelDebugDefaultPanels.STATS, "Stats");
 *     setContent(FlixelDebugUi.column(
 *         FlixelDebugUi.label(out -> out.concat("FPS: ").concat(Flixel.debug.stats.getFps())),
 *         FlixelDebugUi.label(out -> out.concat("Heap: ").concat(Flixel.debug.stats.getHeapMb()).concat(" MB"))));
 *   }
 * }
 *
 * Flixel.debug.panels.replace(new TinyStatsPanel()); // Swaps the built-in stats panel.
 * Flixel.debug.panels.unregister("trackers");       // Removes a built-in panel you do not need.
 * }</pre>
 *
 * <p>{@link org.flixelgdx.debug.ui.FlixelDebugPanels#clear() clear()} removes every panel, which is useful when you
 * want to start from scratch. The registry can also ask the app to
 * {@linkplain org.flixelgdx.debug.ui.FlixelDebugPanels#show(String) bring a panel to the front} or
 * {@linkplain org.flixelgdx.debug.ui.FlixelDebugPanels#toast(int, String) show a short message}.
 *
 * <h2>The widgets</h2>
 *
 * <p>Containers arrange other widgets:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugStack FlixelDebugStack}: a column or a row. Children with a
 *       {@linkplain org.flixelgdx.debug.ui.FlixelDebugWidget#setGrow(float) grow} share the leftover space.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugWrap FlixelDebugWrap}: a row that wraps onto new lines, like
 *       words in a paragraph.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugGrid FlixelDebugGrid}: a table-like layout that fills its cells one row
 *       after another.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugSplit FlixelDebugSplit}: two areas with a handle between them.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugTabs FlixelDebugTabs}: pages behind tabs.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugScroll FlixelDebugScroll}: an area that scrolls.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugExpander FlixelDebugExpander}: a header that folds its content
 *       open and closed.</li>
 * </ul>
 *
 * <p>Controls show a value or let the person change one:
 *
 * <ul>
 *   <li>Text and decoration: {@link org.flixelgdx.debug.ui.FlixelDebugLabel FlixelDebugLabel},
 *       {@link org.flixelgdx.debug.ui.FlixelDebugSeparator FlixelDebugSeparator},
 *       {@link org.flixelgdx.debug.ui.FlixelDebugImage FlixelDebugImage}, and
 *       {@link org.flixelgdx.debug.ui.FlixelDebugProgress FlixelDebugProgress}.</li>
 *   <li>Buttons and switches: {@link org.flixelgdx.debug.ui.FlixelDebugButton FlixelDebugButton},
 *       {@link org.flixelgdx.debug.ui.FlixelDebugCheckbox FlixelDebugCheckbox}, and
 *       {@link org.flixelgdx.debug.ui.FlixelDebugToggle FlixelDebugToggle}.</li>
 *   <li>Choices and input: {@link org.flixelgdx.debug.ui.FlixelDebugDropdown FlixelDebugDropdown},
 *       {@link org.flixelgdx.debug.ui.FlixelDebugTextBox FlixelDebugTextBox},
 *       {@link org.flixelgdx.debug.ui.FlixelDebugNumber FlixelDebugNumber},
 *       {@link org.flixelgdx.debug.ui.FlixelDebugSlider FlixelDebugSlider}, and
 *       {@link org.flixelgdx.debug.ui.FlixelDebugColorPicker FlixelDebugColorPicker}.</li>
 * </ul>
 *
 * <p>Data widgets show a lot of values at once:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugTable FlixelDebugTable}: rows of cells under column headers.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugTree FlixelDebugTree}: nodes nested under other nodes.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugProperties FlixelDebugProperties}: a property grid of named,
 *       editable values in foldable sections.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugGraph FlixelDebugGraph}: lines of numbers over time.</li>
 *   <li>{@link org.flixelgdx.debug.ui.FlixelDebugCanvas FlixelDebugCanvas}: a 2D view of boxes that can be
 *       clicked, dragged, panned, and zoomed.</li>
 * </ul>
 *
 * <h2>Events: what the person did</h2>
 *
 * <p>When the person uses a widget, the app sends an event back to the game: a click, a change, a submit (Enter in a
 * text box), a select, an activate (a double click), a menu pick, an edit (of a property row), or a pick, drag, pan,
 * or zoom (on a canvas). The widget turns each event into a call to the listener you gave it, on the game's main
 * thread, during the frame:
 *
 * <pre>{@code
 * FlixelDebugTable textures = new FlixelDebugTable()
 *     .column("Name")
 *     .column("Size", 90f, FlixelDebugProtocol.COLUMN_SORTABLE | FlixelDebugProtocol.COLUMN_RIGHT)
 *     .onSelect(id -> showDetails(id))     // The id you gave the row, not its position.
 *     .setMenu("Reload", "Unload")
 *     .setFilter(FlixelDebugUi.textBox("Filter by name"))
 *     .setGrow(1f);
 * }</pre>
 *
 * <p>Events for a widget that is disabled or no longer part of the panel are ignored. A listener that throws is logged
 * once and does not crash the game. Interfaces such as
 * {@link org.flixelgdx.debug.ui.FlixelDebugMenuListener FlixelDebugMenuListener} and
 * {@link org.flixelgdx.debug.ui.FlixelDebugEditListener FlixelDebugEditListener} carry the events that have more than
 * one argument.
 *
 * <h2>What the app does on its own</h2>
 *
 * <p>Every decision runs in the game, but some behaviors only change what is shown, and a round trip to the game for
 * each of them would feel slow. The game declares those behaviors on the widget, and the app carries them out
 * locally:
 *
 * <ul>
 *   <li>Sorting a table by a {@linkplain org.flixelgdx.debug.remote.FlixelDebugProtocol#COLUMN_SORTABLE sortable}
 *       column, and hiding rows or nodes that do not match a
 *       {@linkplain org.flixelgdx.debug.ui.FlixelDebugTable#setFilter(org.flixelgdx.debug.ui.FlixelDebugTextBox) filter box}.
 *       </li>
 *   <li>Tab completion from a list of
 *       {@linkplain org.flixelgdx.debug.ui.FlixelDebugTextBox#beginCompletions() completions}, and bringing back
 *       earlier lines of a text box with Up and Down
 *       ({@linkplain org.flixelgdx.debug.ui.FlixelDebugTextBox#setHistory(int) history}).</li>
 *   <li>Scrolling a graph. The game only sends the new samples, and the app keeps the older ones it already has,
 *       up to the graph's {@linkplain org.flixelgdx.debug.ui.FlixelDebugGraph#setLimit(int) limit}.</li>
 *   <li>Moving a box on a canvas while the person drags it, so the drag feels instant. The game's next update puts
 *       the box where the game decided.</li>
 * </ul>
 *
 * <h2>Performance: patches and byte comparison</h2>
 *
 * <p>Panels are cheap enough to refresh every frame, for three reasons:
 *
 * <ul>
 *   <li><b>Only open panels update.</b> {@code update(...)} runs only while the app shows the panel, so a closed tab
 *       costs nothing. A panel that must keep collecting while closed, such as one with graphs, turns on
 *       {@link org.flixelgdx.debug.ui.FlixelDebugPanel#setUpdatedWhileClosed(boolean) setUpdatedWhileClosed(true)}.
 *       </li>
 *   <li><b>Only changes travel.</b> A setter does nothing when the new value equals the old one. When it differs,
 *       only that one property is sent, in a small patch, at most a few dozen times a second. The whole widget tree
 *       is sent only when the panel opens or widgets are added or removed.</li>
 *   <li><b>Rebuilt data is compared byte by byte.</b> Tables, trees, property grids, and canvases can be rebuilt from
 *       scratch every frame. They encode the new rows into a reused buffer and compare the bytes with what the app
 *       already has, so nothing is sent when nothing changed. See
 *       {@link org.flixelgdx.debug.ui.FlixelDebugListBuffer FlixelDebugListBuffer}.</li>
 * </ul>
 *
 * <p>Setters and row writers do not allocate once their buffers have grown to fit, so calling them from
 * {@code update(...)} is fine. The getters and appenders you hand to widgets run every frame while the panel is open,
 * so they must not allocate either.
 *
 * <h2>Writing your own widget type</h2>
 *
 * <p>The protocol has a fixed set of widget types, because the app has to know how to draw each one. You can still
 * subclass a widget to add behavior: store a value, call {@code markDirty(...)} from your setter, and read events in
 * {@code handleEvent(...)} with {@link org.flixelgdx.debug.ui.FlixelDebugEventArgs FlixelDebugEventArgs}. A widget
 * that reads its own value from game code implements {@link org.flixelgdx.debug.ui.FlixelDebugBound FlixelDebugBound}.
 * {@link org.flixelgdx.debug.ui.FlixelDebugContainer FlixelDebugContainer},
 * {@link org.flixelgdx.debug.ui.FlixelDebugBooleanWidget FlixelDebugBooleanWidget}, and
 * {@link org.flixelgdx.debug.ui.FlixelDebugNumberWidget FlixelDebugNumberWidget} are the shared bases of the built-in
 * widgets.
 *
 * <h2>Threading</h2>
 *
 * <p>Build, change, and register panels and widgets on the game's main thread only. A widget belongs to at most one
 * panel and one parent at a time.
 *
 * @see org.flixelgdx.debug.ui.FlixelDebugPanel
 * @see org.flixelgdx.debug.ui.FlixelDebugPanels
 * @see org.flixelgdx.debug.ui.FlixelDebugUi
 * @see org.flixelgdx.debug.panels.FlixelDebugDefaultPanels
 */
package org.flixelgdx.debug.ui;
