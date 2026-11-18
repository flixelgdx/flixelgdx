# Saving Progress: Persistent Game Data

`Flixel.save` stores small pieces of data (high scores, settings, unlocked levels) so they survive the game closing.
The same code works on desktop, Android, and the web. This guide covers the bind, read and write, flush routine; save
slots; nested data; defaults; checking whether a save loaded; saving safely on phones and browsers; storing your own
data classes; and replacing where saves are stored.

## Table of contents

1. [The notebook analogy](#the-notebook-analogy)
2. [Bind, read and write, flush](#bind-read-and-write-flush)
3. [What you can store](#what-you-can-store)
4. [Nested data and lists](#nested-data-and-lists)
5. [First launch and defaults](#first-launch-and-defaults)
6. [Checking whether a save loaded](#checking-whether-a-save-loaded)
7. [Save slots and multiple saves](#save-slots-and-multiple-saves)
8. [When to flush](#when-to-flush)
9. [Erasing a save](#erasing-a-save)
10. [Where saves are stored](#where-saves-are-stored)
11. [Storing your own classes](#storing-your-own-classes)
12. [Custom storage: cloud saves and tests](#custom-storage-cloud-saves-and-tests)
13. [Editing saves while debugging](#editing-saves-while-debugging)
14. [Things to keep in mind](#things-to-keep-in-mind)
15. [Check your understanding](#check-your-understanding)

## The notebook analogy

Saving works like a notebook:

1. **Bind:** open a named notebook. If it already has writing in it from last time, you can read it right away.
2. **Read and write:** look up and jot down values by name.
3. **Flush:** close the notebook so the ink dries. Nothing you wrote is permanent until you flush.

## Bind, read and write, flush

```java
// Once, early. For example in your first state's create().
Flixel.save.bind("MyGame", "slot1");

// Read, with a default for the very first run.
int best = Flixel.save.getInt("highScore", 0);
boolean musicOn = Flixel.save.getBool("musicOn", true);
float volume = Flixel.save.getFloat("volume", 0.8f);
String name = Flixel.save.getString("playerName", "Hero");

// Write, then flush to actually store it.
if (score > best) {
  Flixel.save.data.put("highScore", score);
  Flixel.save.flush();
}
```

`data` is a `FlixelMap<String, Object>` holding everything in the save. The typed getters (`getInt`, `getFloat`,
`getBool`, `getString`) read from it with a default, so a missing key never crashes.

Saving requires a company name in your `FlixelConfig` (`.company("My Studio")`), because it decides the folder on disk.
Without one, `bind(...)` logs an error and returns `false`.

## What you can store

| Type                                   | Notes                                               |
|----------------------------------------|-----------------------------------------------------|
| `String`                               |                                                     |
| Numbers (`int`, `float`, `double`, ...) | Stored as JSON numbers.                            |
| `Boolean`                              |                                                     |
| `FlixelMap<String, Object>`            | Nested groups of values.                            |
| `FlixelArray<Object>`                  | Lists.                                              |

Anything else is converted with `toString()` before writing, so it reads back as a `String`. Store structured data as
nested maps, or turn your own classes into JSON first (see below).

## Nested data and lists

Group related values in nested maps, the way folders group files:

```java
FlixelMap<String, Object> options = new FlixelMap<>();
options.put("musicVolume", 0.8f);
options.put("sfxVolume", 1f);
options.put("fullscreen", false);
Flixel.save.data.put("options", options);

FlixelArray<Object> unlocked = new FlixelArray<>();
unlocked.add("forest");
unlocked.add("caves");
Flixel.save.data.put("unlockedLevels", unlocked);

Flixel.save.flush();
```

Reading them back means a cast, since the map holds `Object` values:

```java
@SuppressWarnings("unchecked")
FlixelMap<String, Object> opts = (FlixelMap<String, Object>) Flixel.save.data.get("options");
if (opts != null) {
  Object music = opts.get("musicVolume");
  float musicVolume = music instanceof Number n ? n.floatValue() : 0.8f;
}
```

## First launch and defaults

The cleanest way to handle defaults is to merge a map of them into the save without overwriting anything the player
already has:

```java
FlixelMap<String, Object> defaults = new FlixelMap<>();
defaults.put("highScore", 0);
defaults.put("musicVolume", 1f);
defaults.put("difficulty", "normal");

// overwrite = false: only add keys the save does not have. flushAfter = true: store the result.
Flixel.save.mergeData(defaults, false, true);
```

This also handles updates gracefully: when a new version of your game adds a setting, old saves pick up its default
the next time they load.

## Checking whether a save loaded

After binding, `getStatus()` tells you what happened:

| Status                   | Meaning                                                          |
|--------------------------|------------------------------------------------------------------|
| `FlixelSaveStatus.EMPTY` | No save existed yet (a first launch), or it was empty.           |
| `FlixelSaveStatus.OK`    | Data loaded, or the last flush succeeded.                        |
| `FlixelSaveStatus.ERROR` | The save existed but could not be read, or the last flush failed. |

```java
Flixel.save.bind("MyGame", null);
if (Flixel.save.getStatus() == FlixelSaveStatus.EMPTY) {
  openSubState(new WelcomeSubState()); // First launch!
}
```

`flush()` also returns `true` or `false`, so you can warn the player if a save failed.

## Save slots and multiple saves

The second argument to `bind(...)` is a slot name. Each `(name, slot)` pair is a separate save file, so slots let one
game keep several independent saves:

```java
Flixel.save.bind("MyGame", "slot" + chosenSlot); // When the player picks a slot.
Flixel.save.bind("MyGame", null);                // A single save with no slot.
```

You can also keep several save objects at once. A common setup is one for settings (shared by every slot) and
`Flixel.save` for the current slot's progress:

```java
public static final FlixelSave settings = new FlixelSave();

// At startup:
settings.bind("MyGame", "settings");
Flixel.save.bind("MyGame", "slot1");
```

## When to flush

Flush as soon as something important changes: a level is completed, an item is unlocked, a setting is changed. Do not
wait until the game closes. Browsers and phones rarely shut a game down cleanly; a closed tab or an app swiped away is
simply stopped, so code that runs "on close" may never run there.

As a safety net, also flush when the platform warns that the game may be stopped. `gameSuspended` fires at the last
moment each platform reliably gives you:

```java
Flixel.Signals.gameSuspended.add(_ -> Flixel.save.flush());
```

Flushing writes the whole save, so do not flush every frame. Once per important event is right.

## Erasing a save

```java
Flixel.save.erase(); // Clears data and deletes the stored save. Still bound, so you can write again.
```

Put this behind an "Are you sure?" prompt.

## Where saves are stored

`FlixelSave` never touches storage itself. It turns `data` into JSON text and hands it to a `FlixelSaveHandler`, which
each platform installs:

| Platform | Location                                                       |
|----------|----------------------------------------------------------------|
| Windows  | `%APPDATA%\Company\Title\saves\`                               |
| macOS    | `~/Library/Application Support/Company/Title/saves/`           |
| Linux    | `$XDG_DATA_HOME/Company/Title/saves/`                          |
| Android  | The app's private storage, under `Company/Title/saves/`        |
| Web      | The browser's `localStorage`, under keys starting with `flixel:Company:Title:` |

`Company` and `Title` come from your `FlixelConfig`. Because saves are JSON text, you can open a desktop save in any
text editor while developing.

On the web, `localStorage` is per site and can be cleared by the player (or by private browsing). Keep that in mind
before promising permanent web saves.

## Storing your own classes

For structured data, define a class and let the framework generate its JSON code with `@JsonSerializable`, then store
the JSON text in the save:

```java
@JsonSerializable
public class RunState {
  public int level;
  public int coins;
  public String[] inventory;
}

// Save:
Flixel.save.data.put("run", RunStateJsonSerializer.toJson(run));
Flixel.save.flush();

// Load:
String json = Flixel.save.getString("run", null);
RunState run = json != null ? RunStateJsonSerializer.fromJson(FlixelJson.parse(json)) : new RunState();
```

### Working with the whole save as JSON

`toJson()` returns a copy of all of `data` as a `FlixelJsonValue` tree, and `fromJson(...)` replaces all of `data` with
the entries of a JSON object. Together they let a save plug straight into the rest of the JSON system:

```java
// Read the nested "options" map from earlier with typed getters instead of casts...
FlixelJsonValue options = Flixel.save.toJson().get("options");
float music = options != null ? options.getFloat("musicVolume", 0.8f) : 0.8f;

// ...or hand it straight to a generated serializer, if Options is a @JsonSerializable class.
Options opts = options != null ? OptionsJsonSerializer.fromJson(options) : new Options();

// Replace the whole save with a JSON object (for example an imported or downloaded save).
Flixel.save.fromJson(FlixelJson.parse(importedText));
Flixel.save.flush();
```

`toJson()` is a copy: editing the tree it returns does not change the save. `fromJson(...)` throws an
`IllegalArgumentException` if the value is not a JSON object, and like any other change, nothing is stored until you
`flush()`. Both build new objects, so use them when loading or exporting, not every frame.

See [JSON and XML](data-serialization.md) for how `@JsonSerializable` works and how to set it up.

## Custom storage: cloud saves and tests

To store one save somewhere else (a cloud service, a custom folder, or memory during tests), give that save object its
own handler. A handler has four methods: read, write, delete, and exists:

```java
public class MemorySaveHandler implements FlixelSaveHandler {

  private final FlixelMap<String, String> store = new FlixelMap<>();

  @Override
  public String read(String name) {
    return store.get(name);
  }

  @Override
  public boolean write(String name, String data) {
    store.put(name, data);
    return true;
  }

  @Override
  public boolean delete(String name) {
    return store.remove(name) != null;
  }
}

FlixelSave testSave = new FlixelSave(new MemorySaveHandler());
testSave.bind("MyGame", "test");
```

A save with its own handler skips the company-name check. To change the default handler for every new save object,
set `FlixelSave.defaultHandler` before the game starts (see [Extending the Framework](extending-the-framework.md)).

## Editing saves while debugging

In debug mode, the debugger app's **Save** panel shows every value by its dotted path (such as `options.musicVolume`),
highlights unsaved changes, and can set, remove, write, or erase values while the game runs. Your own code can do the
same through `Flixel.debug.save`. See [Debugging](debugging.md).

## Things to keep in mind

- **Main thread only.** `FlixelSave` is not thread-safe.
- **Small data only.** Scores, settings, unlocks, and similar compact data. Do not store large binary blobs.
- **Nothing is stored until you flush.** Forgetting to flush is the most common save bug.
- **Version your data.** Store a `"version"` number, so a future update can recognize and convert old saves.

## Check your understanding

1. Your high score resets every time the game starts, but there are no errors. What did you probably forget?
2. `bind(...)` returns `false` and logs an error on desktop. What is missing?
3. Why is "save when the player clicks Quit" not enough on Android and the web?

(Answers: `flush()` after writing; a company name in `FlixelConfig`; those platforms often stop the game without a
clean shutdown, so save when progress changes and on `gameSuspended`.)

**Next:** [JSON and XML](data-serialization.md)
