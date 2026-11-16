# Extending the Debugger: Watches, Commands, Inspectors, and Trackers

The built-in debugger panels show what the framework knows about. This guide shows how to teach the debugger about
*your* game: live values with watches, cheat and test commands with the console, your own classes in the inspector,
grouped values with trackers, extra save files, custom hitboxes and scene children, and using the debugger's features
from your own code.

To build entire panels of your own, see [Custom Debugger Panels](debugger-panels.md). For the basics of the debugger
app, see [Debugging](debugging.md).

## Table of contents

1. [Where to register things](#where-to-register-things)
2. [Watches: live values](#watches-live-values)
3. [Console commands](#console-commands)
4. [Your classes in the inspector](#your-classes-in-the-inspector)
5. [Trackers: tidy tables of related values](#trackers-tidy-tables-of-related-values)
6. [Showing extra save files](#showing-extra-save-files)
7. [Custom hitboxes](#custom-hitboxes)
8. [Objects with their own children](#objects-with-their-own-children)
9. [Using the debugger from code](#using-the-debugger-from-code)
10. [Feeds: the debugger for programs](#feeds-the-debugger-for-programs)
11. [Rules for everything you register](#rules-for-everything-you-register)
12. [Check your understanding](#check-your-understanding)

## Where to register things

Everything lives on `Flixel.debug`, which the framework creates when the game starts. Register things once, usually in
a state's `create()`. Things that belong to one state (a watch on that level's player) should be removed in the
state's `destroy()`; things that are useful in every state can be registered once at startup, for example from a
boot callback (see [The Boot Sequence](boot-sequence.md)).

All of these calls are safe in release builds: they create a few small objects once, and nothing runs per frame.

## Watches: live values

A watch is a name and a small function that reads a value. The watches panel shows every watch, refreshed about ten
times a second:

```java
Flixel.debug.watches.addFloat("Player X", player::getX);
Flixel.debug.watches.addFloat("Velocity Y", player::getVelocityY);
Flixel.debug.watches.addInt("Enemies alive", () -> enemyManager.getAliveCount());
Flixel.debug.watches.addBoolean("On ground", () -> player.isTouching(FlixelDirectionFlags.FLOOR));
Flixel.debug.watches.add("AI state", () -> boss.getState()); // Any object, shown as text.

Flixel.debug.watches.addMouse();   // Built-in: the mouse position.
Flixel.debug.watches.addCamera();  // Built-in: the main camera's scroll and zoom.

Flixel.debug.watches.remove("Player X");
```

Use the typed methods (`addInt`, `addFloat`, `addBoolean`, `addLong`, `addDouble`, `addByte`, `addShort`, `addChar`)
for numbers and flags. They avoid boxing, and they tell the app the exact type. `add(...)` is for objects, which are
shown with `toString()`, so it can create garbage; keep it for values that are cheap to describe.

## Console commands

A console command is a name and a handler. You type it in the app's console, an AI agent sends it, or your own code
runs it. Commands are perfect for cheats and test shortcuts: jumping to a level, giving items, spawning enemies.

```java
Flixel.debug.console.register("god", "god [on|off]", "Makes the player invincible.", args -> {
  player.invincible = args.getBoolean(0, !player.invincible);
  Flixel.info("God mode: {}", player.invincible);
});

Flixel.debug.console.register("level", "level <number>", "Jumps to a level.", args -> {
  int n = args.getInt(0, 1);
  Flixel.switchState(() -> new PlayState(n));
});

Flixel.debug.console.register("heal", args -> player.heal()); // Short form: name and handler only.
```

The two strings in the long form are the **usage** (shown in `help` and as a hint) and the **description**. Arguments
are read with typed getters that take a fallback for a missing or malformed argument, counted from 0 after the
command's name:

| Getter                                   | Reads                    |
|------------------------------------------|--------------------------|
| `getString(i)`, `getString(i, fallback)` | Text.                    |
| `getInt(i, fallback)`, `getLong(...)`    | Whole numbers.           |
| `getFloat(i, fallback)`, `getDouble(...)` | Decimal numbers.        |
| `getBoolean(i, fallback)`                | `true`/`false`, `on`/`off`, and similar. |
| `getArgumentCount()`, `isEmpty()`        | How many arguments there are. |

Double quotes keep spaces in one argument: `spawn "big slime" 3`.

Run a command line from code (for example, from a hotkey) with `execute(...)`:

```java
Flixel.debug.console.execute("level 3");
```

`unregister(name)` removes a command and `has(name)` checks for one.

## Your classes in the inspector

When an object is selected, the inspector shows one section per class in its family tree, most specific first. A
`Player` that extends `FlixelSprite` shows a "Player" section, then the sections the framework provides for sprites,
objects, and basics. You only describe what your own class adds:

```java
Flixel.debug.inspector.register(Player.class, "Player", b -> b
    .intProp("Health", Player::getHealth, Player::setHealth) // With a setter, the row is editable.
    .floatProp("Speed", Player::getSpeed)                    // Without one, it is read-only.
    .boolProp("Invincible", p -> p.invincible, (p, v) -> p.invincible = v)
    .stringProp("Name", (p, out) -> out.concat(p.getName()))  // Text is appended into a reused buffer.
    .enumProp("State", Player::getState)
    .intProp("Id", p -> p.id).once());                       // Read once per selection.
```

There is one method per value type: `boolProp`, `intProp`, `longProp`, `floatProp`, `doubleProp`, `stringProp`, and
`enumProp`. Separate names (instead of one overloaded `prop`) mean a lambda always means exactly one type, and numbers
are read as primitives without boxing.

A section can also show an object *related* to the selection, such as the player's weapon. Call `find(...)` before
adding rows; when it returns `null`, the section is hidden for that selection:

```java
Flixel.debug.inspector.register(Player.class, "Weapon", b -> b
    .find(Player::getWeapon)
    .intProp("Ammo", Weapon::getAmmo, Weapon::setAmmo)
    .floatProp("Cooldown", Weapon::getCooldown));
```

**Editing values.** When you change an editable row in the app, the new value is sent back and converted to the row's
type, then your setter runs on the game's main thread. A value of the wrong kind is ignored with a warning. Text
setters receive a reused buffer, so call `toString()` on it if you need to keep the text.

## Trackers: tidy tables of related values

A tracker is a named group of values, shown as one section in the trackers panel. It suits whole systems: an
inventory, an AI state machine, spawn weights. Extend `FlixelDebugTrackerEntry` and fill a reused map:

```java
public class EnemyTracker extends FlixelDebugTrackerEntry {

  private final EnemyManager manager;
  private final FlixelMap<String, String> values = new FlixelMap<>(); // Reused, never recreated.

  public EnemyTracker(EnemyManager manager) {
    super("Enemies");
    this.manager = manager;
  }

  @Override
  public FlixelMap<String, String> getTrackedValues() {
    values.put("Alive", String.valueOf(manager.getAliveCount()));
    values.put("Pooled", String.valueOf(manager.getPooledCount()));
    values.put("Next wave", String.valueOf(manager.getSecondsToNextWave()));
    return values;
  }
}

Flixel.debug.trackers.add(new EnemyTracker(enemyManager));
```

Trackers are read about ten times a second, only in debug mode. Remove one with `Flixel.debug.trackers.remove(...)`.

## Showing extra save files

The save panel shows `Flixel.save` by default. If your game keeps other `FlixelSave` objects (a separate settings save,
for example), add them so the panel can switch between them:

```java
Flixel.debug.save.add(settingsSave);
```

## Custom hitboxes

Hitboxes drawn with F2 come from the `FlixelDebugDrawable` interface, which every `FlixelObject` already implements.
Any other class can implement it too, to show its own box: return its position, size, and an RGBA color array.

```java
public class TriggerZone implements FlixelDebugDrawable {

  private static final float[] COLOR = {1f, 0.8f, 0f, 0.6f}; // Yellow, created once.
  private float x;
  private float y;
  private float width;
  private float height;

  @Override public float getDebugX() { return x; }
  @Override public float getDebugY() { return y; }
  @Override public float getDebugWidth() { return width; }
  @Override public float getDebugHeight() { return height; }
  @Override public float[] getDebugBoundingBoxColor() { return COLOR; }
}
```

A `FlixelObject` can also change the color of its own box with `setDebugBoundingBoxColor(rgba)`, which is a handy way
to highlight, say, enemies that are currently aggressive.

## Objects with their own children

The scene tree, the viewport map, and the picker know how to walk groups. If you write a class that holds its own
children without being a group (a UI widget with sub-widgets, a tilemap made of chunks), implement `FlixelDebugNode`.
List the children, and report the box the object covers on a camera:

```java
public class Window implements FlixelDebugNode {

  private final FlixelArray<Widget> widgets = new FlixelArray<>(Widget[]::new);

  @Override
  public int getDebugChildCount() {
    return widgets.getSize();
  }

  @Override
  public Object getDebugChild(int index) {
    return widgets.get(index);
  }

  @Override
  public boolean getDebugBounds(FlixelCamera cam, float[] out) {
    out[0] = cam.worldToViewX(x, 0f); // Left, in the camera's view space.
    out[1] = cam.worldToViewY(y, 0f); // Top.
    out[2] = width;
    out[3] = height;
    return true;
  }
}
```

The object then appears in the scene tree, gets a box on the viewport map, and can be picked and clicked like any
other object.

## Using the debugger from code

Everything the debugger does is also available to your own code, through the same objects the panels use:

```java
Flixel.debug.inspector.select(player);          // Select an object, as a click in the app would.
Flixel.debug.save.set("options.musicVolume", "0.5"); // Edit a save value by its dotted path.
Flixel.debug.save.saveNow();                    // Write the save.
Flixel.debug.viewport.pan(0f, -100f);           // Fly the camera up a little.
Flixel.debug.input.key(FlixelKey.SPACE, true);  // Press a key, as if a player did.
Flixel.debug.input.key(FlixelKey.SPACE, false); // And release it.
Flixel.debug.capture.request(1, 640);           // Ask for a picture of the next frame, at most 640 pixels wide.
```

Injected input is queued and applied one change per frame, so a tap takes two frames. This makes the debugger a useful
base for automated playtests.

## Feeds: the debugger for programs

A debugger needs two things that sound alike but are not: a way for **people** to look at the game (panels, which can
be redesigned freely) and a way for **programs** to look at the game (data in a format that never changes under them).
Feeds are the part for programs.

Think of a newsroom. Each feed is a reporter assigned to one beat: the scene, the logs, the save. When a subscriber (a
tool in the app, or an AI agent) wants the story, the reporter files it in a fixed house style. The panels a person
sees are the front page: an editor can lay it out any way they like, but the facts come from the same reporters.

Each field of `Flixel.debug` you used above is a feed: `stats`, `logs`, `watches`, `trackers`, `textures`, `controls`,
`console`, `scene`, `inspector`, `save`, `viewport`, `capture`, and `input`. The built-in panels hold no game logic of
their own; a "Save now" button calls `Flixel.debug.save.saveNow()`, exactly what an AI agent's request or your own
code calls. So a person, a tool, and your code always agree about the state of the game.

Feeds only send data while something subscribes to them, but they keep sampling either way, because panels read from
them. To stop one entirely, call `setEnabled(false)` on it. The exact data format of every feed is documented in
`DEBUG_PROTOCOL.md` at the repository root.

## Rules for everything you register

- **Main thread only.** Register and call debugger methods from your game code, not from other threads.
- **Keep getters cheap and allocation-free.** Watch getters, inspector rows, and labels are read many times a second
  in debug mode. Return fields or simple calculations; avoid building strings (use `stringProp` with the appender).
- **Errors are contained.** A getter, command, or panel that throws is logged once and shows its message in place of
  the value. It never crashes the game.
- **Clean up per-state registrations** in the state's `destroy()`, so a watch does not keep a destroyed player alive.

## Check your understanding

1. Why does the inspector builder have `intProp` and `floatProp` instead of one `prop(...)` method?
2. You want a command `give sword 3`. Which getters read `sword` and `3`?
3. Your custom UI window's buttons do not show up in the scene tree. What should the window implement?

(Answers: so each lambda has exactly one meaning, and numbers are read without boxing; `args.getString(0, ...)` and
`args.getInt(1, 1)`; `FlixelDebugNode`, listing the buttons as children.)

**Next:** [Custom Debugger Panels](debugger-panels.md)
