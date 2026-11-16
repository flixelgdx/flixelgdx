# Groups and Recycling: Managing Many Objects at Once

A group is a container of objects that is itself an object. Groups let you treat many things as one (collide the
player against "all walls," kill "all enemies," draw "all bullets" on one layer), and they power **recycling**, the
habit that keeps games with lots of short-lived objects smooth. This guide covers every group type, iteration,
sorting, recycling, and the difference between removing, killing, and destroying.

## Table of contents

1. [Why groups?](#why-groups)
2. [The five group types](#the-five-group-types)
3. [FlixelBasicGroup: the everyday choice](#flixelbasicgroup-the-everyday-choice)
4. [Looping over members without garbage](#looping-over-members-without-garbage)
5. [Recycling: the toy box pattern](#recycling-the-toy-box-pattern)
6. [Size limits](#size-limits)
7. [Remove, detach, kill, and destroy](#remove-detach-kill-and-destroy)
8. [FlixelSpriteGroup: many sprites, one visual unit](#flixelspritegroup-many-sprites-one-visual-unit)
9. [Sorting and draw order](#sorting-and-draw-order)
10. [FlixelGroup: a plain organized list](#flixelgroup-a-plain-organized-list)
11. [Groups and collisions](#groups-and-collisions)
12. [Nesting groups](#nesting-groups)
13. [Common mistakes](#common-mistakes)
14. [Check your understanding](#check-your-understanding)

## Why groups?

Think of a shopping basket. It holds items together, so you can carry, count, or empty all of them at once without
juggling each one. A group does the same for game objects:

- **One call for many objects.** `Flixel.collide(player, walls)` checks the player against every wall in the group.
- **Layers.** Everything in a group is drawn together, so a "background" group and a "foreground" group keep their
  order no matter when members are added.
- **Cleanup.** Destroying a group destroys every member.
- **Recycling.** A group can hand you a "dead" member to reuse instead of creating a new one.

A `FlixelState` is itself a group, which is why `add(...)` works in it.

## The five group types

| Type                     | What it is                                                                       | Use it when                                         |
|--------------------------|----------------------------------------------------------------------------------|-----------------------------------------------------|
| `FlixelGroupable`        | The base interface: add, remove, clear, and iteration.                           | You write your own container type.                  |
| `FlixelGroup`            | A plain list backed by a `FlixelArray`. Works with any type. You drive it.       | You only need an organized list.                    |
| `FlixelBasicGroupable`   | `FlixelGroupable` for game objects, plus dead-member lookups.                    | You write your own game-object container.           |
| `FlixelBasicGroup`       | A `FlixelBasic` that updates and draws every member, and destroys them with it.  | **The everyday choice** for enemies, bullets, coins. |
| `FlixelSpriteGroup`      | A `FlixelSprite` whose position, alpha, color, scale, and angle apply to every child. | Something built from several pieces that moves as one. |

## FlixelBasicGroup: the everyday choice

`FlixelBasicGroup` is abstract, so you create one with an anonymous subclass. The empty braces `{}` after the
constructor call are what make that subclass; leaving them out is a compile error. The constructor takes an array
factory such as `Enemy[]::new`, which lets the group store members in a properly typed array without reflection.

```java
private FlixelBasicGroup<Enemy> enemies;

@Override
public void create() {
  super.create();
  enemies = new FlixelBasicGroup<>(Enemy[]::new) {};
  add(enemies); // The group is updated and drawn because the state updates and draws it.

  enemies.add(new Enemy(200f, 100f));
  enemies.add(new Enemy(300f, 100f));
}
```

Every frame, the group updates each member that exists and is active, and draws each member that exists and is
visible.

## Looping over members without garbage

There are two good ways to visit every member.

**`forEachMember(...)`** is the shortest. Store the lambda in a field when you call it every frame, so a new lambda
object is not created each time:

```java
private final Consumer<Enemy> offScreenCheck = enemy -> {
  if (enemy.getX() > Flixel.getDesignWidth()) {
    enemy.kill();
  }
};

// In update(...):
enemies.forEachMember(offScreenCheck);
```

There is also `forEachMemberType(Type.class, callback)`, which only visits members of one class.

**An indexed loop** is the fastest and needs no lambda at all:

```java
FlixelArray<Enemy> list = enemies.getMembers();
if (list != null) {
  Enemy[] items = list.getItems();
  for (int i = 0, n = list.getSize(); i < n; i++) {
    Enemy enemy = items[i];
    if (enemy != null && enemy.exists) {
      enemy.think(elapsed);
    }
  }
}
```

`getMembers()` can return `null` before the first member is added, which is why the example checks it. Avoid
for-each loops (`for (Enemy e : list)`) in code that runs every frame. `FlixelArray` reuses its iterator, so it does
not create garbage, but nested for-each loops over the same array are not safe.

## Recycling: the toy box pattern

Creating a brand new object every time the player fires a bullet is slow, and it fills memory with garbage that the
computer must clean up later, which causes small stutters. Instead, keep a group of bullets and **recycle** them:

1. When a bullet is done (it leaves the screen or hits something), `kill()` it. It stays in the group, switched off.
2. When you need a bullet, call `recycle()`. The group revives the first dead member it finds.
3. Only when no dead member exists does the group create a new one, by calling `createMemberForRecycle()`.

Think of a box of rubber ducks at a bath. You do not buy a new duck every time you want one in the water; you grab one
that is sitting on the side, and only buy a new one when every duck is already floating.

```java
public class Bullet extends FlixelSprite {

  public Bullet() {
    makeGraphic(4, 4, FlixelColor.YELLOW);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (getY() < -getHeight()) {
      kill(); // Off the top of the screen: back in the toy box.
    }
  }
}
```

```java
// In PlayState:
private FlixelBasicGroup<Bullet> bullets;

@Override
public void create() {
  super.create();
  // Up to 32 bullets. The group calls createMemberForRecycle() when it needs a new one.
  bullets = new FlixelBasicGroup<>(Bullet[]::new, 32) {
    @Override
    protected Bullet createMemberForRecycle() {
      return new Bullet();
    }
  };
  add(bullets);
}

private void shoot() {
  Bullet bullet = bullets.recycle(); // A revived old bullet, a new one, or null when all 32 are in use.
  if (bullet != null) {
    bullet.reset(player.getMidpointX(), player.getY()); // Revive and move in one call.
    bullet.setVelocity(0f, -500f);
  }
}
```

The most important rule of recycling: **`recycle()` hands back an old object, so reset everything that might still
hold an old value** (position, velocity, health, animation, tint) right after you get it. `reset(x, y)` revives the
object, moves it, and clears its velocity in one call. For your own fields, override `revive()` or `reset(...)` in the
subclass and reset them there.

A state can recycle too: override `createMemberForRecycle()` in your state and call `recycle()` on it. Particles have
their own, even faster recycling built in; see [Particles](particles.md). For objects that are not game objects
(path nodes, events, messages), use a `FlixelPool`; see [Collections and Pooling](collections-and-pooling.md).

## Size limits

Pass a maximum size to the constructor to cap a group:

```java
new FlixelBasicGroup<>(Bullet[]::new, 32) { /* ... */ };
```

With a cap, `recycle()` returns `null` once every slot holds a living member, and `add(...)` quietly ignores members
beyond the cap. A cap is a good safety net: it turns a bug that would spawn infinite objects into a harmless "no more
bullets right now." Change it later with `setMaxSize(...)`; `0` means no limit.

## Remove, detach, kill, and destroy

These four operations are not interchangeable:

| Operation                        | What happens to the member                           | Can it come back?            |
|----------------------------------|------------------------------------------------------|------------------------------|
| `group.remove(member)`           | Unlinked from the group, otherwise untouched.        | Yes, add it somewhere else.  |
| `group.detach(member)`           | Same as remove: unlinked, untouched.                 | Yes.                         |
| `member.kill()`                  | Stays in the group, switched off.                    | Yes, `recycle()` revives it. |
| `group.removeMember(member, true)` | Unlinked, then destroyed.                          | No.                          |
| `group.destroy()`                | The group and every member are destroyed.            | No.                          |

Prefer `kill()` and `recycle()` for objects that come and go frequently. Use `remove(...)` when moving an object from
one group to another, such as an enemy that becomes a "corpse" in a background layer.

## FlixelSpriteGroup: many sprites, one visual unit

A `FlixelSpriteGroup` is a sprite made of sprites. Moving, tinting, fading, or scaling the group changes every child,
so the group behaves as one thing. It suits characters with separate parts (a body and a hat), ships with turrets, and
UI panels built from pieces.

```java
FlixelSpriteGroup ship = new FlixelSpriteGroup();
ship.setPosition(100f, 200f);

FlixelSprite body = new FlixelSprite(0f, 0f, Flixel.files.internal("images/ship.png"));
FlixelSprite cannon = new FlixelSprite(12f, -6f, Flixel.files.internal("images/cannon.png"));
ship.add(body);
ship.add(cannon);
add(ship);

ship.setAlpha(0.5f);            // Fade the whole ship.
ship.setPosition(150f, 250f);   // Body and cannon both follow.
```

Because it is a sprite, you can tween it, put it in other groups, and nest sprite groups inside each other.

How rotation applies to the children is chosen with `setRotationMode(...)`:

| Mode         | What `setAngle(...)` does                                                                 |
|--------------|-------------------------------------------------------------------------------------------|
| `INDIVIDUAL` | The default. Each child rotates in place around its own origin. Nothing moves.            |
| `WHEEL`      | Children are spaced evenly in a circle around the group's center, at the rotation radius. Great for orbiting shields and bullet rings. |
| `ORBIT`      | The whole group turns like one rigid object around its origin, children included.          |

```java
FlixelSpriteGroup shield = new FlixelSpriteGroup(8, 40f, 0f); // Up to 8 orbs, radius 40, starting angle 0.
shield.setRotationMode(FlixelSpriteGroup.RotationMode.WHEEL);
// In update(...): shield.setAngle(shield.getAngle() + 90f * elapsed);
```

`FlixelSpriteGroup` also has list helpers: `get(index)`, `getLength()`, `countMembers()`, `contains(...)`,
`indexOf(...)`, `getRandom()`, `getFirstMatching(...)`, `insert(...)`, `replace(...)`, `swapMembers(...)`,
`getBounds(...)`, and `getMidpoint(...)`.

## Sorting and draw order

Members are drawn in list order, so the order of a group is its draw order. `FlixelSpriteGroup` can reorder:

```java
ship.bringToFront(cannon);
ship.sendToBack(body);
```

Top-down games often sort characters by their Y position each frame, so a character lower on the screen draws in
front. Keep the comparator in a constant so sorting does not create a new object every frame:

```java
private static final Comparator<FlixelSprite> BY_Y =
    (a, b) -> Float.compare(a.getY() + a.getHeight(), b.getY() + b.getHeight());

// In update(...):
characters.sort(BY_Y);
```

## FlixelGroup: a plain organized list

`FlixelGroup` is a simple list for any type, game object or not. It does not update or draw anything; you decide what
to do with its members. It is useful for things like "every checkpoint in the level" or "every enemy spawner."

```java
FlixelGroup<Checkpoint> checkpoints = new FlixelGroup<>(Checkpoint[]::new);
checkpoints.add(new Checkpoint(100f, 300f));
checkpoints.forEachMember(cp -> cp.highlight());
```

## Groups and collisions

Either side of `Flixel.collide(...)` and `Flixel.overlap(...)` can be a group, so one line checks many objects:

```java
Flixel.collide(player, walls);             // The player against every wall.
Flixel.overlap(bullets, enemies, onHit, null); // Every bullet against every enemy.
```

Dead members (killed ones) are skipped. See [Physics and Collisions](physics-and-collisions.md).

## Nesting groups

Groups can contain other groups. A common layout for a level is one group per layer:

```java
add(background);  // Back.
add(enemies);
add(player);
add(bullets);
add(hud);         // Front.
```

Each layer keeps its place in the draw order even when members are added to it later, which a single flat list
cannot do.

## Common mistakes

- **Forgetting the braces.** `new FlixelBasicGroup<>(Enemy[]::new)` does not compile; it needs `{}`.
- **Forgetting to add the group.** A group that is never added to a state is never updated or drawn.
- **Not resetting recycled objects.** A recycled bullet keeps its old velocity and tint until you change them.
- **Returning `null` from `createMemberForRecycle()`.** The default does that, so without an override `recycle()` can
  only revive existing dead members.
- **Creating lambdas every frame.** Store per-frame callbacks and comparators in fields.

## Check your understanding

1. Why is `kill()` better than `destroy()` for a coin the player picked up, if more coins will appear later?
2. A recycled enemy appears already flashing red from its previous life. What did you forget?
3. You want a ring of orbs that spins around the player. Which group type and rotation mode?

(Answers: the coin can be recycled instead of creating a new one, which avoids garbage; resetting its tint (and other
state) after `recycle()`; a `FlixelSpriteGroup` with `RotationMode.WHEEL`.)

**Next:** [Physics and Collisions](physics-and-collisions.md)
