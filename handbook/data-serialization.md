# JSON and XML: Load, Parse, and Handle Data Formats

Games are full of data that is not code: level layouts, enemy stats, dialog, item tables, settings, sprite atlases.
Keeping that data in files means designers can change it without recompiling. This guide covers FlixelGDX's own JSON
and XML parsers: reading JSON into a tree, writing JSON, generating JSON code for your own classes with
`@JsonSerializable`, reading XML, and the patterns and pitfalls of game data.

## Table of contents

1. [Why the framework has its own parsers](#why-the-framework-has-its-own-parsers)
2. [JSON in thirty seconds](#json-in-thirty-seconds)
3. [Reading JSON](#reading-json)
4. [Navigating a JSON tree](#navigating-a-json-tree)
5. [Arrays and iteration](#arrays-and-iteration)
6. [Relaxed JSON](#relaxed-json)
7. [Handling bad data](#handling-bad-data)
8. [Writing JSON](#writing-json)
9. [Automatic mapping with @JsonSerializable](#automatic-mapping-with-jsonserializable)
10. [Setting up the annotation processor](#setting-up-the-annotation-processor)
11. [Rules and gotchas for serializable classes](#rules-and-gotchas-for-serializable-classes)
12. [Reading XML](#reading-xml)
13. [Choosing between JSON and XML](#choosing-between-json-and-xml)
14. [Recipes](#recipes)
15. [Performance](#performance)
16. [Check your understanding](#check-your-understanding)

## Why the framework has its own parsers

Most Java JSON libraries use **reflection**: at runtime, they look inside your classes to find fields by name.
Reflection is slow, it creates garbage, and it breaks on platforms that compile your game ahead of time, such as the
web (TeaVM) and native images, unless you write configuration files listing every class. FlixelGDX's parsers use no
reflection at all, so the same code runs on desktop, Android, and the web with no configuration.

| Class                  | Role                                                                      |
|------------------------|---------------------------------------------------------------------------|
| `FlixelJson`           | Parses JSON text into a tree of `FlixelJsonValue` nodes.                  |
| `FlixelJsonValue`      | One node: an object, array, string, number, boolean, or null.             |
| `FlixelJsonWriter`     | Builds JSON text piece by piece.                                          |
| `@JsonSerializable`    | Generates `toJson` and `fromJson` code for your own classes at compile time. |
| `FlixelXml`            | Parses XML text into a tree of `FlixelXmlElement` nodes.                  |
| `FlixelXmlElement`     | One XML element: its name, attributes, text, and children.                |

## JSON in thirty seconds

JSON is a text format made of objects (`{ }`, named fields), arrays (`[ ]`, ordered lists), strings, numbers,
booleans, and `null`:

```json
{
  "title": "Forest",
  "gravity": 900,
  "spawn": { "x": 40, "y": 300 },
  "enemies": [
    { "type": "slime", "x": 200, "y": 310, "health": 3 },
    { "type": "bat", "x": 420, "y": 120, "health": 1 }
  ],
  "night": false
}
```

Think of a JSON object as a set of labeled drawers: open the drawer labeled `"spawn"` and you find another set of
drawers labeled `"x"` and `"y"`.

## Reading JSON

Read the file's text, then parse it:

```java
String text = Flixel.files.internal("data/forest.json").readString();
FlixelJsonValue root = FlixelJson.parse(text);

String title = root.getString("title", "Untitled"); // "Untitled" if the field is missing.
int gravity = root.getInt("gravity", 600);
boolean night = root.getBool("night", false);
float speed = root.getFloat("speed", 200f);
```

Every typed getter takes a **default**, returned when the field is missing. That makes loading forgiving: an older
data file without a new field still loads, and the game uses a sensible value.

## Navigating a JSON tree

`get(name)` steps into a field and `get(index)` into an array element. Both return `null` when there is nothing there,
so check before using the result:

```java
FlixelJsonValue spawn = root.get("spawn");
if (spawn != null) {
  player.setPosition(spawn.getFloat("x", 0f), spawn.getFloat("y", 0f));
}
```

Useful members of `FlixelJsonValue`:

| Member                                   | Meaning                                                        |
|------------------------------------------|----------------------------------------------------------------|
| `getKind()`                              | `OBJECT`, `ARRAY`, `STRING`, `NUMBER`, `BOOL`, or `NULL`.      |
| `isObject()`, `isArray()`, `isNull()`    | Quick kind checks.                                             |
| `has(name)`                              | Whether an object has a field.                                 |
| `getSize()`                              | Number of fields or elements.                                  |
| `getName()`                              | This node's field name inside its parent object.               |
| `asString()`, `asInt()`, `asFloat()`, `asDouble()`, `asBool()` | This node's own value.                 |
| `getString(name, def)`, `getInt(...)`, `getFloat(...)`, `getBool(...)` | A child's value, with a default. |

## Arrays and iteration

A `FlixelJsonValue` is iterable, so you can loop over an array's elements (or an object's fields) directly:

```java
FlixelJsonValue enemies = root.get("enemies");
if (enemies != null && enemies.isArray()) {
  for (FlixelJsonValue e : enemies) {
    spawnEnemy(e.getString("type", "slime"), e.getFloat("x", 0f), e.getFloat("y", 0f), e.getInt("health", 1));
  }
}
```

Or with an index:

```java
for (int i = 0; i < enemies.getSize(); i++) {
  FlixelJsonValue e = enemies.get(i);
  // ...
}
```

Looping over an object visits its fields, and `getName()` tells you each field's name. That is handy for "dictionary"
style data:

```json
{ "sword": 10, "axe": 14, "bow": 7 }
```

```java
for (FlixelJsonValue weapon : root.get("damage")) {
  damageTable.put(weapon.getName(), weapon.asInt());
}
```

## Relaxed JSON

Hand-edited files and tool exports often break strict JSON rules. The parser accepts two common relaxations:

- **Trailing commas:** `[1, 2, 3,]` and `{ "a": 1, }`.
- **Single-quoted strings:** `{ 'name': 'Slime' }`.

## Handling bad data

`FlixelJson.parse(...)` throws an `IllegalArgumentException` with a description when the text is not valid JSON. For
files players or modders can edit, catch it and fall back gracefully:

```java
FlixelJsonValue root;
try {
  root = FlixelJson.parse(text);
} catch (IllegalArgumentException e) {
  Flixel.error("Level file is broken: {}", e.getMessage());
  root = FlixelJson.parse("{}"); // An empty object: every getter returns its default.
}
```

## Writing JSON

`FlixelJsonWriter` builds JSON text one piece at a time. Commas are added for you:

```java
String json = new FlixelJsonWriter()
    .beginObject()
    .name("score").value(1200)
    .name("name").value("Ada")
    .name("items").beginArray()
        .value("sword")
        .value("shield")
    .endArray()
    .endObject()
    .toString();
// {"score":1200,"name":"Ada","items":["sword","shield"]}
```

Call `setIndent(2)` before writing for pretty, human-readable output. `raw(...)` inserts text that is already JSON,
such as the output of a generated serializer. A writer is a one-shot tool for saves, exports, and network messages, not
something to use every frame: build one, read `toString()`, and discard it.

## Automatic mapping with @JsonSerializable

Reading fields one by one is fine for a few values, but tedious for a class with twenty fields. Annotate the class
with `@JsonSerializable`, and an annotation processor writes the reading and writing code for you **at compile time**:

```java
import org.flixelgdx.json.JsonSerializable;

@JsonSerializable
public class EnemyData {
  public String type;
  public float x;
  public float y;
  public int health;
  public boolean boss;
}
```

The processor generates a companion class named `EnemyDataJsonSerializer` in the same package, with two static
methods:

```java
// Object to JSON text.
String text = EnemyDataJsonSerializer.toJson(enemy);

// JSON to a new object.
EnemyData loaded = EnemyDataJsonSerializer.fromJson(FlixelJson.parse(text));
```

Think of it like a tailor who measures you once and writes down a pattern, instead of measuring you every time you
need a shirt. The "measuring" (looking at your class's fields) happens once, when your game is compiled, so at runtime
there is nothing left to figure out.

Serializable classes can contain other serializable classes, enums, and arrays:

```java
@JsonSerializable
public class LevelData {
  public String name;
  public Difficulty difficulty;   // An enum, stored by its name ("HARD").
  public EnemyData[] enemies;     // Another @JsonSerializable type, in an array.
  public int[] tiles;
}
```

## Setting up the annotation processor

The annotation lives in `flixelgdx-core`, but the code generator is a separate artifact so it never ships with your
game. Add it as an annotation processor in the module that holds your annotated classes (usually `core`):

```groovy
// core/build.gradle
dependencies {
  api "org.flixelgdx:flixelgdx-core:$flixelVersion"
  annotationProcessor "org.flixelgdx:flixelgdx-json-processor:$flixelVersion"
}
```

Then build once. The generated `...JsonSerializer` classes appear under `build/generated/sources/annotationProcessor`
and your IDE picks them up. If your IDE marks the serializer as missing, run a Gradle build first, and make sure
annotation processing is enabled in the IDE's settings.

## Rules and gotchas for serializable classes

- **A no-argument constructor** must exist and be accessible.
- **Fields must not be `private`.** The generated class lives in the same package and reads fields directly (that is
  how it avoids reflection). Public or package-private fields both work.
- **`static`, `transient`, and `final` fields are skipped.** Use `transient` for fields you never want saved, such as a
  cached texture.
- **Supported field types:** the primitives, `String`, enums, other `@JsonSerializable` classes, and arrays of those
  (including multidimensional arrays). `FlixelArray`, `FlixelMap`, and other collections are not supported; use arrays.
- **Missing fields become zero, `false`, or `null`.** When a field is missing from the JSON, `fromJson` sets it to the
  type's empty value, *not* to the initial value in your class. If you need a default, apply it after loading:
  `if (data.speed == 0f) data.speed = 200f;`
- **Enums are stored by name.** Renaming an enum constant breaks old files that used the old name.
- **`long` values pass through a `double`**, so numbers above about 9 quadrillion lose precision.
- **Multidimensional arrays are "jagged."** `int[][]` is an array of rows, and each row can have its own length. If
  every row must have the same width (a tilemap), prefer a flat `int[] tiles` plus `width` and `height` fields, and
  check `tiles.length == width * height` right after loading.

## Reading XML

XML is the other common data format, used by Sparrow atlases, Tiled maps, and many older tools:

```xml
<TextureAtlas imagePath="hero.png">
  <SubTexture name="Hero Idle0000" x="0" y="0" width="64" height="64"/>
  <SubTexture name="Hero Idle0001" x="64" y="0" width="64" height="64"/>
</TextureAtlas>
```

```java
FlixelXmlElement root = FlixelXml.parse(Flixel.files.internal("images/hero.xml").readString());
String image = root.getAttribute("imagePath", "");

for (int i = 0; i < root.getChildCount(); i++) {
  FlixelXmlElement sub = root.getChild(i);
  if ("SubTexture".equals(sub.getName())) {
    String name = sub.getAttribute("name", "");
    int x = sub.getIntAttribute("x", 0);
    int y = sub.getIntAttribute("y", 0);
    // ...
  }
}
```

| Member                                      | Meaning                                                     |
|---------------------------------------------|-------------------------------------------------------------|
| `getName()`                                 | The tag name, such as `"SubTexture"`.                       |
| `getText()`                                 | The text between the opening and closing tags.              |
| `getChildCount()`, `getChild(i)`            | Child elements by index.                                    |
| `getChildrenByName(name)`                   | Every child with a tag name, in a new list.                 |
| `getAttribute(key, default)`                | A string attribute, with a default.                         |
| `getIntAttribute(...)`, `getFloatAttribute(...)`, `getBooleanAttribute(...)` | Typed attributes, with defaults. |
| `hasAttribute(key)`                         | Whether an attribute exists.                                |

The parser is deliberately small. It skips XML declarations, comments, `DOCTYPE`s, and `CDATA` wrappers (keeping the
text inside), which covers what tool exports contain. It does not validate documents against a schema and does not
understand namespaces, which game data does not need. `getChildrenByName(...)` creates a new list each call, so use the
index loop when you visit children often.

## Choosing between JSON and XML

| Use JSON when...                                         | Use XML when...                                     |
|----------------------------------------------------------|-----------------------------------------------------|
| You design the format yourself.                          | A tool already exports XML (Sparrow, Tiled `.tmx`). |
| You want automatic mapping with `@JsonSerializable`.     | The data is a document with nested text.            |
| The data is mostly numbers, lists, and settings.         |                                                     |

For your own formats, JSON is the better default.

## Recipes

**A data-driven enemy table.** Keep stats in a file so they can be tuned without recompiling:

```json
{ "enemies": [ { "type": "slime", "health": 3, "speed": 40 }, { "type": "bat", "health": 1, "speed": 120 } ] }
```

```java
private final FlixelMap<String, EnemyData> enemyTypes = new FlixelMap<>();

private void loadEnemyTypes() {
  FlixelJsonValue root = FlixelJson.parse(Flixel.files.internal("data/enemies.json").readString());
  for (FlixelJsonValue e : root.get("enemies")) {
    EnemyData data = EnemyDataJsonSerializer.fromJson(e);
    enemyTypes.put(data.type, data);
  }
}
```

**Dialog lines.**

```json
{ "intro": [ "Welcome, traveler.", "The road ahead is dangerous.", "Take this sword." ] }
```

```java
FlixelJsonValue lines = FlixelJson.parse(dialogText).get("intro");
String first = lines.get(0).asString();
```

**Saving a whole object.** Combine `@JsonSerializable` with [`Flixel.save`](saving.md): store `toJson(...)` text under
one key, and parse it back with `fromJson(...)`.

**Hot-reloading data during development.** On desktop, add a debugger console command that re-reads your data files,
so you can tweak numbers while the game runs:

```java
Flixel.debug.console.register("reloadEnemies", args -> loadEnemyTypes());
```

## Performance

Parsing builds a tree of objects, so it creates garbage, and that is fine as long as it happens at load time: in
`create()`, on a loading screen, or when a level starts. Never parse inside `update(...)`. Parse once, copy what you
need into your own objects or fields, and let the tree go.

## Check your understanding

1. A designer deletes the `"speed"` field from an enemy's JSON. What does `getFloat("speed", 200f)` return?
2. Your `@JsonSerializable` class has `private int health;`. Why does the build complain, and how do you fix it?
3. Your class has `public float speed = 200f;`, and the JSON has no `speed` field. What is `speed` after `fromJson`?

(Answers: `200f`, the default; the generated code reads fields directly and cannot see private ones, so make it public
or package-private; `0f`, because missing fields become the type's empty value, not the field's initial value.)

**Next:** [Collections and Pooling](collections-and-pooling.md)
