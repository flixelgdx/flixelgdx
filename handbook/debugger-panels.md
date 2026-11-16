# Custom Debugger Panels: The Widget Toolkit

Every panel in the debugger app, including the built-in ones, is built by your game out of a small set of widgets. The
app only draws them and reports what the person clicked. This guide shows how to build your own panels, lay them out,
fill tables, trees, graphs, and property grids, handle events, and replace or remove the built-in panels.

## Table of contents

1. [The puppet show](#the-puppet-show)
2. [Your first panel](#your-first-panel)
3. [Registering, docking, and ids](#registering-docking-and-ids)
4. [The widgets](#the-widgets)
5. [Keeping widgets up to date](#keeping-widgets-up-to-date)
6. [Tables](#tables)
7. [Trees](#trees)
8. [Graphs](#graphs)
9. [Property grids](#property-grids)
10. [Events: what the person did](#events-what-the-person-did)
11. [What the app does on its own](#what-the-app-does-on-its-own)
12. [Replacing and removing built-in panels](#replacing-and-removing-built-in-panels)
13. [Why panels are cheap](#why-panels-are-cheap)
14. [Rules for panels](#rules-for-panels)
15. [Check your understanding](#check-your-understanding)

## The puppet show

Most debuggers ship their screens inside the debugger program, so a game that wants a new screen has to change the
debugger itself. FlixelGDX turns this around. Think of a puppet show: the game holds the strings. It builds the
puppets (widgets), arranges them on the stage (the panel), and moves them when something changes. The app is the stage
and the curtain: the audience sees the puppets move, and when someone pokes a puppet, the stage tells the puppeteer,
who decides what happens next.

Because the app never decides anything by itself, a panel can do anything your game can do, and you can redesign any
panel without a new version of the app.

## Your first panel

Create widgets once, in the constructor, and keep references to the ones you change later. `FlixelDebugUi` has short
factory methods so a panel reads like an outline:

```java
import static org.flixelgdx.debug.ui.FlixelDebugUi.*;

public class EnemyPanel extends FlixelDebugPanel {

  private final FlixelDebugProgress wave = new FlixelDebugProgress();
  private final EnemyManager enemies;

  public EnemyPanel(EnemyManager enemies) {
    super("game.enemies", "Enemies"); // A stable id, and the title on the tab.
    this.enemies = enemies;
    setDock(FlixelDebugProtocol.DOCK_RIGHT);
    setContent(column(
        heading("Enemies"),
        label(out -> out.concat(enemies.getAliveCount()).concat(" alive")),
        checkbox("Frozen", enemies::isFrozen, enemies::setFrozen),
        slider(0.0, 2.0, () -> enemies.speedScale, v -> enemies.speedScale = (float) v),
        wave,
        row(
            button("Spawn wave", enemies::spawnWave),
            button("Kill all", enemies::killAll)
                .setConfirm("Kill every enemy?")
                .setStyle(FlixelDebugProtocol.TONE_ERROR))));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed); // Refreshes the label, checkbox, and slider above.
    wave.setValue(enemies.getWaveProgress()); // 0 to 1.
  }
}

// In your state's create():
Flixel.debug.panels.register(new EnemyPanel(enemyManager));
```

## Registering, docking, and ids

```java
Flixel.debug.panels.register(panel);   // Add it. The app shows a new tab.
Flixel.debug.panels.unregister("game.enemies");
Flixel.debug.panels.show("game.enemies"); // Ask the app to bring it to the front.
Flixel.debug.panels.toast(FlixelDebugProtocol.TONE_SUCCESS, "Wave cleared!"); // A short message in the app.
```

- **Ids must be stable and unique.** The app remembers where the person put each panel by its id, so keep an id the
  same between versions of your game. Start your ids with something like `"game."` so they never clash with the
  built-in ones.
- **Dock** is where the panel first appears: `DOCK_LEFT`, `DOCK_RIGHT`, `DOCK_BOTTOM`, or `DOCK_CENTER`. After that,
  the person can move it anywhere.
- A panel with an id that is only useful in one state should be unregistered in that state's `destroy()`.

A one-off panel does not even need a subclass, since the constructor can take the content directly:

```java
Flixel.debug.panels.register(new FlixelDebugPanel("game.ai", "Enemy AI", grid));
```

## The widgets

**Containers** arrange other widgets:

| Widget                 | Factory                      | Arranges children...                                     |
|------------------------|------------------------------|----------------------------------------------------------|
| `FlixelDebugStack`     | `column(...)`, `row(...)`    | In a column or a row. Children with a grow share leftover space. |
| `FlixelDebugWrap`      | `wrap(...)`                  | In a row that wraps onto new lines, like words.          |
| `FlixelDebugGrid`      |                              | In cells, filled one row after another.                  |
| `FlixelDebugSplit`     |                              | In two areas with a draggable handle.                    |
| `FlixelDebugTabs`      |                              | On pages behind tabs (`addPage(title, widget)`).         |
| `FlixelDebugScroll`    | `scroll(child)`              | In an area that scrolls.                                 |
| `FlixelDebugExpander`  | `expander(header, child)`    | Under a header that folds open and closed.               |

**Controls** show a value or let the person change one:

| Widget                    | Factory                                   | Use                                    |
|---------------------------|-------------------------------------------|----------------------------------------|
| `FlixelDebugLabel`        | `label(text)`, `label(appender)`, `heading(...)`, `hint(...)` | Text, live text, titles, notes. |
| `FlixelDebugSeparator`    | `separator()`                             | A dividing line.                       |
| `FlixelDebugImage`        |                                           | A picture, such as a texture thumbnail. |
| `FlixelDebugProgress`     |                                           | A progress bar from 0 to 1.            |
| `FlixelDebugButton`       | `button(text, onClick)`                   | An action. `setConfirm(...)` asks first. |
| `FlixelDebugCheckbox`     | `checkbox(text, getter, onChange)`        | A true/false setting.                  |
| `FlixelDebugToggle`       | `toggle(text, getter, onChange)`          | A switch-style true/false setting.     |
| `FlixelDebugDropdown`     |                                           | A choice from a list.                  |
| `FlixelDebugTextBox`      | `textBox(placeholder)`                    | Typed text, with history and completion. |
| `FlixelDebugNumber`       | `number(getter, onChange)`                | A number field.                        |
| `FlixelDebugSlider`       | `slider(min, max, getter, onChange)`      | A number in a range.                   |
| `FlixelDebugColorPicker`  |                                           | A color.                               |

**Data widgets** show many values at once: `FlixelDebugTable`, `FlixelDebugTree`, `FlixelDebugProperties` (a property
grid), `FlixelDebugGraph`, and `FlixelDebugCanvas` (a 2D view of boxes that can be clicked, dragged, panned, and zoomed,
which is what the viewport panel uses).

Every widget shares a few properties: `setName(...)` (how tools and AI agents find it), `setVisible(...)`,
`setEnabled(...)`, `setTooltip(...)`, `setWidth(...)`, `setHeight(...)`, `setGrow(...)`, and `setStyle(...)` (a tone
such as `TONE_ACCENT`, `TONE_WARNING`, or `TONE_ERROR`).

## Keeping widgets up to date

There are two ways to keep a widget's value current:

1. **Give it a getter.** Widgets built with a getter (a checkbox with a `BooleanSupplier`, a label with an appender, a
   slider with a `DoubleSupplier`) refresh themselves every frame while the panel is open.
2. **Set it in `update(...)`.** For everything else, set the new value in your panel's `update(...)`.

`update(...)` only runs while the panel is open in the app. Setters do nothing when the new value equals the old one,
so setting the same value every frame sends nothing.

Live text uses an **appender** instead of a getter that returns a `String`, so no new string is created each frame:

```java
label(out -> out.concat("FPS: ").concat(Flixel.debug.stats.getFps()))
```

## Tables

Tables are rebuilt from scratch whenever you like, usually every frame in `update(...)`. Between `beginRows()` and
`endRows()`, add one row per item, giving each row a stable id (not its position) so selection survives sorting:

```java
private final FlixelDebugTable table = new FlixelDebugTable()
    .column("Name")
    .column("HP", 60f, FlixelDebugProtocol.COLUMN_SORTABLE | FlixelDebugProtocol.COLUMN_RIGHT)
    .onSelect(id -> Flixel.debug.inspector.select(findEnemy(id)))
    .setMenu("Kill", "Heal")
    .setFilter(FlixelDebugUi.textBox("Filter by name"))
    .setGrow(1f);

@Override
public void update(float elapsed) {
  super.update(elapsed);
  table.beginRows();
  Enemy[] items = enemies.getItems();
  for (int i = 0, n = enemies.getSize(); i < n; i++) {
    Enemy e = items[i];
    int style = e.getHealth() < 2 ? FlixelDebugProtocol.TONE_WARNING : 0;
    table.row(e.id, style).cell(e.getName()).cell(e.getHealth());
  }
  table.endRows(); // Sent only when the rows really changed.
}
```

Column flags: `COLUMN_SORTABLE` (click the header to sort, done by the app), `COLUMN_RIGHT` (right-aligned, good for
numbers), and `COLUMN_MONO` (monospaced). `setFollow(true)` keeps the newest row in view, like a log.

## Trees

Trees work like tables, with each node naming its parent:

```java
tree.beginNodes();
tree.node(1, 0, "World");          // id 1, no parent (0).
tree.node(2, 1, "Enemies");        // id 2, child of 1.
tree.node(3, 2, "Slime #3");
tree.endNodes();
```

## Graphs

A graph plots numbers over time. Add every series first, then push one sample per series each time you sample:

```java
private final FlixelDebugGraph graph = new FlixelDebugGraph("AI cost")
    .series("Pathfinding", 0, "ms")    // 0 = let the app pick a color, or 0xAARRGGBB.
    .series("Decisions", 0, "ms")
    .setLimit(300);

// In update(...):
graph.push(pathMs, decideMs);
```

The game only sends new samples; the app keeps the older ones it already has, up to the limit. If a graph must keep
collecting while its panel is closed (so it has history when opened), call `setUpdatedWhileClosed(true)` on the panel.

## Property grids

A property grid shows named values in foldable sections, with editors for values that can change. Describe each
section once with getters (and setters for editable rows):

```java
FlixelDebugProperties grid = new FlixelDebugProperties()
    .section("Enemy AI", s -> s
        .intProp("Active enemies", () -> enemyManager.getAliveCount())
        .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)
        .boolProp("Paused", () -> ai.paused, v -> ai.paused = v))
    .section("World", s -> s
        .doubleProp("Time", () -> world.time));

Flixel.debug.panels.register(new FlixelDebugPanel("game.ai", "Enemy AI", grid));
```

The grid reads every getter each frame while the panel is open and sends only values that changed. It uses the same
one-method-per-type builder as the inspector (see [Extending the Debugger](debugger-extending.md#your-classes-in-the-inspector)).

## Events: what the person did

When the person uses a widget, the app sends an event and the widget calls the listener you gave it, on the game's main
thread, during the frame: a click, a change, a submit (Enter in a text box), a select, an activate (double click), a
menu pick, an edit (of a property row), or a pick, drag, pan, or zoom (on a canvas).

```java
FlixelDebugTextBox search = FlixelDebugUi.textBox("Enemy name")
    .onSubmit(text -> highlightEnemy(text.toString())); // Enter was pressed. The text buffer is reused, so copy it.

FlixelDebugDropdown difficulty = new FlixelDebugDropdown()
    .setItems("Easy", "Normal", "Hard")
    .onChange(index -> game.setDifficulty(index));

table.onActivate(id -> Flixel.debug.inspector.select(findEnemy(id)));  // Double click on a row.
table.onMenu((id, item) -> handleMenu(id, item));                        // A pick from the row's right-click menu.
```

| Widget                         | Listeners                                   |
|--------------------------------|---------------------------------------------|
| Button                         | `onClick`                                   |
| Checkbox, toggle, expander     | `onChange(boolean)`                         |
| Number, slider, split          | `onChange(double)`                          |
| Dropdown, tabs, color picker   | `onChange(int)`                             |
| Text box                       | `onChange(text)`, `onSubmit(text)`          |
| Table, tree                    | `onSelect(id)`, `onActivate(id)`, `onMenu(...)` |
| Property grid                  | `onEdit(...)`                               |

Events for a widget that is disabled or no longer in the panel are ignored. A listener that throws is logged once and
does not crash the game.

## What the app does on its own

Some behaviors only change what is shown, and a round trip to the game for each would feel slow. The game declares
them on the widget, and the app carries them out locally:

- Sorting a table by a sortable column, and hiding rows or nodes that do not match a filter box.
- Tab completion from a list of completions, and Up and Down history in a text box.
- Scrolling a graph (the app keeps old samples).
- Moving a box on a canvas while the person drags it. The game's next update puts it where the game decided.
- Asking "are you sure?" before a button with `setConfirm(...)`.

## Replacing and removing built-in panels

The built-in panels are ordinary panels in the same list as yours, with stable ids in `FlixelDebugDefaultPanels`:
`STATS`, `PERFORMANCE`, `LOGS`, `SCENE`, `VIEWPORT`, `INSPECTOR`, `TEXTURES`, `SAVE`, `WATCHES`, `CONTROLS`,
`TRACKERS`, and `CONSOLE`.

**Remove** one you do not need. Its feed keeps running, so tools can still read that data:

```java
Flixel.debug.panels.unregister(FlixelDebugDefaultPanels.TRACKERS);
```

**Replace** one with your own design by building a panel with the same id. The app shows yours in the same place:

```java
public class TinyStatsPanel extends FlixelDebugPanel {

  public TinyStatsPanel() {
    super(FlixelDebugDefaultPanels.STATS, "Stats");
    setContent(FlixelDebugUi.column(
        FlixelDebugUi.label(out -> out.concat("FPS: ").concat(Flixel.debug.stats.getFps())),
        FlixelDebugUi.label(out -> out.concat("Heap: ").concat(Flixel.debug.stats.getHeapMb()).concat(" MB"))));
  }
}

Flixel.debug.panels.replace(new TinyStatsPanel());
```

Read data from the same feed the built-in panel used (here `Flixel.debug.stats`), so panels and tools keep agreeing.

**Start over** with `Flixel.debug.panels.clear()`, register only your own panels, and bring back any built-in ones
you still want. `addTo(...)` skips ids that are already taken, so your replacements stay in place:

```java
Flixel.debug.panels.clear();
Flixel.debug.panels.register(new MySavePanel());       // Uses the "save" id.
FlixelDebugDefaultPanels.addTo(Flixel.debug);          // Every built-in panel except "save".
```

**Extend** a built-in panel: their classes are public and not final, so you can subclass one, override `update(...)`
or its hooks, and register the subclass with `replace(...)`. Their source code is also the best collection of toolkit
examples.

## Why panels are cheap

Panels are cheap enough to refresh every frame, for three reasons:

- **Only open panels update.** A closed tab costs nothing (unless it asked to keep updating).
- **Only changes travel.** A setter with an unchanged value does nothing. A changed property is sent alone, in a small
  patch, at most a few dozen times a second. The whole widget tree is sent only when the panel opens or widgets are
  added or removed.
- **Rebuilt data is compared byte by byte.** Tables, trees, grids, and canvases encode new rows into a reused buffer
  and compare them with what the app already has, so nothing is sent when nothing changed.

## Rules for panels

- **Main thread only.** Build, change, and register panels and widgets from your game code.
- **One parent per widget.** A widget belongs to at most one panel and one container at a time.
- **No allocation in getters and appenders.** They run every frame while the panel is open.
- **Fixed widget types.** The app has to know how to draw each widget, so the set of widget types is fixed by the
  protocol. You can still subclass a widget to add behavior; see the `org.flixelgdx.debug.ui` package documentation.

## Check your understanding

1. Your panel's `update(...)` sets a label to the same text every frame. Does that flood the network?
2. You redesigned the save panel. What id do you give it, and which method do you call?
3. Why should table rows use a stable id (like an object's id) instead of the row index?

(Answers: no, a setter with an unchanged value sends nothing; `FlixelDebugDefaultPanels.SAVE`, with
`Flixel.debug.panels.replace(...)`; so the selection and sorting stay attached to the same item when rows move.)

**Next:** [Optimization](optimization.md)
