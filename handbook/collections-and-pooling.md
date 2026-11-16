# Collections and Pooling: Garbage-Free Lists, Maps, and Pools

FlixelGDX ships its own lists, maps, sets, and object pools, and the framework itself uses nothing else. This guide
explains why the standard Java collections are a poor fit for games, how to use `FlixelArray`, `FlixelMap`, the
primitive collections, and `FlixelPool`, and the iteration rules that keep them fast and safe.

## Table of contents

1. [Why not ArrayList and HashMap?](#why-not-arraylist-and-hashmap)
2. [The collection family](#the-collection-family)
3. [FlixelArray: the everyday list](#flixelarray-the-everyday-list)
4. [Iterating a FlixelArray](#iterating-a-flixelarray)
5. [Changing a list while looping: snapshot mode](#changing-a-list-while-looping-snapshot-mode)
6. [Ordered and unordered lists](#ordered-and-unordered-lists)
7. [Primitive arrays: no boxing](#primitive-arrays-no-boxing)
8. [Maps and sets](#maps-and-sets)
9. [Iterating maps](#iterating-maps)
10. [Object pools](#object-pools)
11. [Pooled math types](#pooled-math-types)
12. [Pools versus recycling groups](#pools-versus-recycling-groups)
13. [Check your understanding](#check-your-understanding)

## Why not ArrayList and HashMap?

Java's standard collections are excellent for most programs, but games have an unusual problem: they run the same code
60 or more times per second, and every small object created along the way becomes garbage. When enough garbage piles
up, Java pauses to clean it, and the player sees a stutter.

The standard collections create garbage in places that are easy to miss:

- A for-each loop over an `ArrayList` creates a new iterator object every time.
- `HashMap<Integer, Enemy>` turns every `int` key into an `Integer` object (called **boxing**), and every entry is its
  own object.
- `ArrayList<Integer>` stores boxed numbers instead of plain `int`s.

The FlixelGDX collections are built on plain arrays, reuse their iterators, and have primitive versions that never box.
That is why the framework's rule is simple: **use the FlixelGDX collections in game code.**

## The collection family

| Type                              | Replaces                       | Notes                                          |
|-----------------------------------|--------------------------------|------------------------------------------------|
| `FlixelArray<T>`                  | `ArrayList<T>`                 | The everyday list.                             |
| `FlixelIntArray`, `FlixelFloatArray`, `FlixelLongArray`, `FlixelShortArray`, `FlixelByteArray`, `FlixelCharArray`, `FlixelBooleanArray` | `ArrayList<Integer>` and friends | Plain primitives, no boxing. |
| `FlixelMap<K, V>`                 | `HashMap<K, V>`                | Keys compared with `equals`.                   |
| `FlixelIdentityMap<K, V>`         | `IdentityHashMap<K, V>`        | Keys compared with `==`.                       |
| `FlixelIntMap<V>`, `FlixelLongMap<V>` | `HashMap<Integer, V>`      | Primitive keys.                                |
| `FlixelObjectIntMap<K>`, `FlixelObjectFloatMap<K>` | `HashMap<K, Integer>` | Primitive values. Great for counters. |
| `FlixelSet<T>`, `FlixelIntSet`, `FlixelLongSet` | `HashSet`        |                                                |
| `FlixelPool<T>`                   | (none)                         | Reuses objects instead of creating new ones.   |

## FlixelArray: the everyday list

Create it with an array constructor reference (`Enemy[]::new`). Java cannot create a generic array by itself, so this
tells the list how to make its typed storage:

```java
FlixelArray<Enemy> enemies = new FlixelArray<>(Enemy[]::new);

enemies.add(new Enemy());
Enemy first = enemies.get(0);
enemies.set(0, otherEnemy);
enemies.insert(1, bossEnemy);
enemies.removeIndex(0);
enemies.removeValue(bossEnemy, true); // true = compare with ==, false = compare with equals().
boolean has = enemies.contains(bossEnemy, true);
int count = enemies.getSize();
enemies.sort(BY_HEALTH);              // A Comparator stored in a constant.
Enemy random = enemies.getRandom(Flixel.random);
enemies.clear();
```

Always pass the array constructor if you will call `getItems()`. Without it, the list stores its items in an
`Object[]`, and assigning that to an `Enemy[]` fails at runtime.

## Iterating a FlixelArray

The fastest loop reads the backing array directly. Only the first `getSize()` entries are live; slots after that may
hold old values or `null`:

```java
Enemy[] items = enemies.getItems();
for (int i = 0, n = enemies.getSize(); i < n; i++) {
  items[i].think(elapsed);
}
```

For-each loops are also allowed and create no garbage, because a `FlixelArray` keeps **two** reused iterators. That
means you can nest at most two for-each loops over the *same* list at once. A third nested loop over the same list
would reuse an iterator that is still in use. Indexed loops have no such limit.

```java
for (Enemy e : enemies) {   // Fine.
  e.think(elapsed);
}
```

## Changing a list while looping: snapshot mode

Removing items from a list while you loop over it usually skips or repeats elements, because everything after the
removed item shifts down a slot. `begin()` and `end()` solve this: they give you a stable snapshot to loop over, and
any add or remove during the loop quietly happens on a fresh copy.

Think of taking a photo of a crowd and counting people in the photo while the real crowd keeps moving. Your count is
consistent, and the crowd still changes.

```java
Enemy[] snapshot = enemies.begin();
try {
  for (int i = 0, n = enemies.getSize(); i < n; i++) {
    Enemy enemy = snapshot[i];
    if (enemy.isDead()) {
      enemies.removeValue(enemy, true); // Safe during a snapshot loop.
    }
  }
} finally {
  enemies.end();
}
```

Groups and signals use this internally, which is why you can kill objects inside a collision callback or remove a
listener during a dispatch.

## Ordered and unordered lists

An **ordered** list (the default) keeps insertion order and shifts everything down when an item is removed. An
**unordered** list fills the hole with the last item instead, which makes removal instant. Use unordered lists for
"bags" where order does not matter, such as active bullets:

```java
FlixelArray<Bullet> active = new FlixelArray<>(Bullet[]::new, false, 64); // false = unordered, capacity 64.
```

Giving a starting capacity that fits your usual size also avoids the list having to grow (and copy itself) later.

## Primitive arrays: no boxing

```java
FlixelIntArray scores = new FlixelIntArray();
scores.add(1200);
scores.add(980);
int best = scores.get(0);

int[] raw = scores.getItems();
for (int i = 0, n = scores.getSize(); i < n; i++) {
  total += raw[i];
}
```

There is a primitive array for every primitive type. They have the same methods as `FlixelArray`, using plain values.

## Maps and sets

```java
FlixelMap<String, EnemyData> types = new FlixelMap<>();
types.put("slime", slimeData);
EnemyData data = types.get("slime");
EnemyData fallback = types.get("ghost", defaultData);
types.remove("slime");
boolean known = types.containsKey("bat");
```

Keys must not be `null`; values may be. Iteration order is not defined and can change as the map grows.

Primitive-key and primitive-value maps avoid boxing entirely:

```java
FlixelIntMap<Enemy> byId = new FlixelIntMap<>();
byId.put(enemy.id, enemy);

FlixelObjectIntMap<String> kills = new FlixelObjectIntMap<>();
kills.increment("slime", 1);           // Counters without boxing.
int slimes = kills.get("slime", 0);
```

Sets answer "is this in the group?":

```java
FlixelSet<String> unlocked = new FlixelSet<>();
unlocked.add("forest");
if (unlocked.contains("forest")) {
  // ...
}
```

## Iterating maps

`keys()`, `values()`, and `entries()` return iterators that are reused between loops, so ordinary iteration creates
no garbage. Two rules come with that:

- Do not run two loops of the same kind over one map at the same time (for example, a `keys()` loop inside another
  `keys()` loop of the same map).
- The `Entry` from `entries()` is reused for every step, so copy its `key` and `value` if you need to keep them.

```java
for (FlixelMap.Entry<String, EnemyData> e : types.entries()) {
  Flixel.info("{} has {} health", e.key, e.value.health);
}
```

## Object pools

A pool keeps objects you are done with and hands them out again, instead of creating new ones. Think of a library: you
borrow a book, return it, and the next reader borrows the same copy. No new books are printed.

```java
FlixelPool<PathNode> nodePool = new FlixelPool<>() {
  @Override
  protected PathNode newObject() {
    return new PathNode(); // Only called when the pool is empty.
  }
};

PathNode node = nodePool.obtain();  // Borrow (new only if needed).
node.set(x, y);
// ... use it ...
nodePool.free(node);                // Return it.
```

If the pooled class implements `FlixelPoolable`, its `reset()` method runs automatically on `free(...)`, so no stale
state leaks into the next borrower:

```java
public class PathNode implements FlixelPoolable {
  public float x;
  public float y;
  public PathNode parent;

  @Override
  public void reset() {
    x = 0f;
    y = 0f;
    parent = null;
  }
}
```

Pools can be limited: `new FlixelPool<>(initialCapacity, max)` keeps at most `max` free objects, and extra freed
objects are simply dropped. `getFree()` and `peak` help you see how big a pool really needs to be.

The golden rule of pools: **after you `free(...)` an object, never use it again.** Someone else may already have it.

## Pooled math types

`FlixelVector` and `FlixelRect` have built-in pools for temporary values:

```java
FlixelVector v = FlixelVector.get(10f, 20f); // Borrow.
// ... calculations ...
v.put();                                     // Return.

FlixelRect r = FlixelRect.get();
// ...
r.put();
```

There is also a "weak" variant (`FlixelVector.weak(...)`) for passing a temporary straight into a framework method
that accepts one; that method returns it to the pool when done, so you do not call `put()` yourself. See
[Math and Utilities](math-and-utilities.md).

## Pools versus recycling groups

Both reuse objects. Pick by what you are reusing:

| Use...                              | For...                                                              |
|-------------------------------------|---------------------------------------------------------------------|
| A `FlixelBasicGroup` with `recycle()` | Game objects that are updated and drawn: bullets, enemies, coins. See [Groups and Recycling](groups-and-recycling.md). |
| A `FlixelEmitter`                   | Particles. See [Particles](particles.md).                           |
| A `FlixelPool`                      | Plain data objects: path nodes, events, messages, temporary math.   |

## Check your understanding

1. Why does `HashMap<Integer, Enemy>` create garbage on every lookup, and what replaces it?
2. You remove dead enemies inside an indexed loop and some dead enemies survive. What is the fix?
3. Can you keep a `FlixelMap.Entry` from an `entries()` loop in a field?

(Answers: the `int` key is boxed into an `Integer` object; `FlixelIntMap<Enemy>`; loop over a `begin()` snapshot and
call `end()` afterward (or loop backward); no, the entry object is reused, so copy its key and value instead.)

**Next:** [Math and Utilities](math-and-utilities.md)
