# Movement, Physics, and Collisions

Every `FlixelObject` (and so every sprite) has a small, built-in physics system: velocity, acceleration, drag, and
speed limits, plus collision checks that can detect touching and push objects apart. This guide explains how motion
is calculated, how to build platformer and top-down movement, and how to use `overlap` and `collide` correctly.

## Table of contents

1. [Describe motion, do not do it by hand](#describe-motion-do-not-do-it-by-hand)
2. [Velocity, acceleration, drag, and max velocity](#velocity-acceleration-drag-and-max-velocity)
3. [Rotation physics](#rotation-physics)
4. [Recipe: a platformer character](#recipe-a-platformer-character)
5. [Recipe: top-down movement](#recipe-top-down-movement)
6. [Overlap versus collide](#overlap-versus-collide)
7. [Collision callbacks](#collision-callbacks)
8. [Filtering pairs with a process callback](#filtering-pairs-with-a-process-callback)
9. [Collision settings: immovable, one-way, bounce, and mass](#collision-settings-immovable-one-way-bounce-and-mass)
10. [Which sides are touching?](#which-sides-are-touching)
11. [Simple shape checks](#simple-shape-checks)
12. [Performance and limits](#performance-and-limits)
13. [Common mistakes](#common-mistakes)
14. [Check your understanding](#check-your-understanding)

## Describe motion, do not do it by hand

You can move objects yourself every frame with `changeX(speed * elapsed)`, and for simple cases that is fine. But
most game motion is easier to *describe* than to *do*. Instead of "add 3 pixels this frame, then 3.1 next frame because
it is speeding up," you say "this ball has a speed of 200 pixels per second and gravity pulls it down at 900," and the
framework does the math every frame.

Think of the difference between pushing a toy car with your finger the whole way and giving it one shove. With
velocity, you give the shove and the car keeps rolling on its own.

## Velocity, acceleration, drag, and max velocity

Each axis (X and Y) has four motion values:

| Property       | Meaning                                                                  | Setter                              |
|----------------|--------------------------------------------------------------------------|-------------------------------------|
| Velocity       | Speed in pixels per second. The object moves by itself.                  | `setVelocity(x, y)`, `setVelocityX(...)` |
| Acceleration   | Change in velocity per second. Gravity is a constant downward acceleration. | `setAcceleration(x, y)`, `setAccelerationY(...)` |
| Drag           | Slows the object down, like friction, but **only while acceleration on that axis is zero**. | `setDrag(x, y)` |
| Max velocity   | A speed limit in each direction.                                         | `setMaxVelocity(x, y)`              |

Every frame, during `super.update(elapsed)`, the object:

1. Adds acceleration to velocity (or applies drag if there is no acceleration).
2. Clamps velocity to the max.
3. Moves by velocity times `elapsed`.

The math uses the average velocity across the frame, which keeps motion accurate even when the frame rate changes.

```java
ball.setVelocity(150f, -300f);   // Thrown up and to the right.
ball.setAcceleration(0f, 900f);  // Gravity (positive Y is down).
ball.setMaxVelocity(400f, 600f); // Never faster than this.
```

Turn built-in motion off for one object with `setMoves(false)`. Its position then only changes when you change it.
This is useful for objects that a tween or a path moves.

## Rotation physics

The same model exists for rotation:

```java
asteroid.setAngularVelocity(90f);       // Spin 90 degrees per second.
asteroid.setAngularAcceleration(0f);
asteroid.setAngularDrag(30f);           // Slowly stop spinning.
asteroid.setMaxAngularVelocity(360f);
```

## Recipe: a platformer character

A platformer character uses all four motion values at once:

```java
// In create():
player.setAcceleration(0f, 900f);   // Gravity pulls down.
player.setMaxVelocity(200f, 600f);  // Horizontal and falling speed limits.
player.setDrag(800f, 0f);           // Stop quickly when no key is held.

// In update(...), BEFORE super.update(elapsed):
player.setAccelerationX(0f);        // No key held means drag takes over.
if (Flixel.keys.pressed(FlixelKey.LEFT)) {
  player.setAccelerationX(-1000f);
  player.setFacing(FlixelDirectionFlags.LEFT);
} else if (Flixel.keys.pressed(FlixelKey.RIGHT)) {
  player.setAccelerationX(1000f);
  player.setFacing(FlixelDirectionFlags.RIGHT);
}
if (Flixel.keys.justPressed(FlixelKey.SPACE) && player.isTouching(FlixelDirectionFlags.FLOOR)) {
  player.setVelocityY(-350f);       // Jump: a quick upward push.
}

super.update(elapsed);              // Move everything.

Flixel.collide(player, level);      // AFTER moving: push the player out of walls and floors.
```

Why accelerate instead of setting velocity directly? Acceleration plus a max velocity gives the character a tiny
wind-up when starting and, with drag, a short slide when stopping. It feels much less robotic than instant speed.

**Variable jump height** is a common touch: if the player lets go of jump early, cut the upward speed.

```java
if (Flixel.keys.justReleased(FlixelKey.SPACE) && player.getVelocityY() < 0f) {
  player.setVelocityY(player.getVelocityY() * 0.5f);
}
```

## Recipe: top-down movement

A top-down character has no gravity, and usually moves at a steady speed in eight directions. Read a direction,
normalize it so diagonals are not faster, and set velocity:

```java
float dx = 0f;
float dy = 0f;
if (Flixel.keys.anyPressed(FlixelKey.A, FlixelKey.LEFT)) {
  dx -= 1f;
}
if (Flixel.keys.anyPressed(FlixelKey.D, FlixelKey.RIGHT)) {
  dx += 1f;
}
if (Flixel.keys.anyPressed(FlixelKey.W, FlixelKey.UP)) {
  dy -= 1f;
}
if (Flixel.keys.anyPressed(FlixelKey.S, FlixelKey.DOWN)) {
  dy += 1f;
}
if (dx != 0f && dy != 0f) {
  dx *= 0.7071f; // 1 divided by the square root of 2, so diagonals are the same speed.
  dy *= 0.7071f;
}
player.setVelocity(dx * 150f, dy * 150f);
```

[Input Actions](input-actions.md) can do the normalizing for you, and handle gamepad sticks at the same time.

## Overlap versus collide

There are two kinds of collision checks, and choosing the right one matters:

| Method                    | What it does                              | Use it for                                     |
|---------------------------|-------------------------------------------|------------------------------------------------|
| `Flixel.overlap(a, b)`    | Only **asks** whether things touch. Nothing moves. | Pickups, damage zones, triggers, "is the mouse over this?" |
| `Flixel.collide(a, b)`    | Touches **and pushes apart**.             | Solid things: walls, floors, crates, platforms. |

Either argument can be a single object or a whole group (or a state), so one call can check the player against every
wall. Passing `null` means "the whole current state." Both return `true` if anything touched.

```java
Flixel.collide(player, walls);          // Walls and floors push the player out.

if (Flixel.overlap(player, spikes)) {   // Just a yes-or-no question.
  player.hurt(1);
}
```

Call them in `update(...)` *after* `super.update(elapsed)`, so you check where things ended up this frame.

## Collision callbacks

Both methods accept a callback that runs once for every touching pair. The two arguments arrive as `FlixelObject`,
in the same order as the groups you passed, so cast them to your own types:

```java
// Store the callback in a field so a new lambda is not created every frame.
private final BiConsumer<FlixelObject, FlixelObject> onCoin = (p, c) -> {
  c.kill();
  score++;
  Flixel.sound.play(Flixel.files.internal("sounds/coin.ogg"));
};

// In update(...):
Flixel.overlap(player, coins, onCoin, null);
```

```java
private final BiConsumer<FlixelObject, FlixelObject> onBulletHitsEnemy = (b, e) -> {
  b.kill();
  ((Enemy) e).hurt(1);
};

// In update(...):
Flixel.overlap(bullets, enemies, onBulletHitsEnemy, null);
```

`collide(a, b, callback)` works the same way, and the callback runs after the pair has been pushed apart.

## Filtering pairs with a process callback

`overlap(...)` takes a second, optional callback that decides whether a touching pair *counts*. Return `false` to
ignore that pair; the notify callback then does not run for it. This is how you make an invincible player ignore
enemies for a moment after being hit:

```java
private final BiPredicate<FlixelObject, FlixelObject> canBeHurt = (p, e) -> !player.isInvincible();
private final BiConsumer<FlixelObject, FlixelObject> onEnemyTouch = (p, e) -> player.hurt(1);

// In update(...):
Flixel.overlap(player, enemies, onEnemyTouch, canBeHurt);
```

(`collide` uses this slot itself, for the code that pushes objects apart, which is why only `overlap` exposes it.)

## Collision settings: immovable, one-way, bounce, and mass

| Setting                              | Effect                                                                      |
|--------------------------------------|-----------------------------------------------------------------------------|
| `setImmovable(true)`                 | The object never gets pushed. Floors, walls, and moving platforms should be immovable. |
| `setAllowCollisions(flags)`          | Only collide on certain sides. `FlixelDirectionFlags.UP` on a platform means only its top is solid, so you can jump up through it. |
| `setSolid(false)`                    | Shorthand for "collide on no sides." The object passes through everything. |
| `setElasticity(0.8f)`                | Bounciness when pushed apart, from `0` (no bounce) to `1` (perfect bounce). |
| `setMass(2f)`                        | Heavier objects push lighter ones more when neither is immovable.          |

A one-way platform you can jump up through but stand on:

```java
FlixelSprite platform = new FlixelSprite(100f, 250f).makeGraphic(80, 8, FlixelColor.GRAY);
platform.setImmovable(true);
platform.setAllowCollisions(FlixelDirectionFlags.UP); // Only the top side is solid.
level.add(platform);
```

A bouncing ball:

```java
ball.setAcceleration(0f, 600f);
ball.setElasticity(0.7f); // Loses 30% of its speed on every bounce.
```

## Which sides are touching?

After `Flixel.collide(...)`, every object remembers which of its sides touched something this frame. Ask with
`isTouching(...)` and `justTouched(...)`, using the constants in `FlixelDirectionFlags`:

| Constant  | Meaning                                       |
|-----------|-----------------------------------------------|
| `LEFT`, `RIGHT`, `UP`, `DOWN` | One side.                     |
| `FLOOR`   | Same as `DOWN`: standing on something.        |
| `CEILING` | Same as `UP`: bumping into something above.   |
| `WALL`    | `LEFT` or `RIGHT`.                            |
| `ANY`     | Any side.                                     |
| `NONE`    | No side.                                      |

```java
boolean onGround = player.isTouching(FlixelDirectionFlags.FLOOR);

if (player.justTouched(FlixelDirectionFlags.FLOOR)) {
  Flixel.sound.play(Flixel.files.internal("sounds/land.ogg")); // Only on the first frame of landing.
}

if (player.isTouching(FlixelDirectionFlags.WALL) && !onGround) {
  // Wall slide!
}
```

`justTouched(...)` is true only on the frame contact *starts*, which makes it perfect for landing sounds and dust
puffs. The flags are flags, so you can combine them: `FlixelDirectionFlags.LEFT | FlixelDirectionFlags.UP`.

## Simple shape checks

Sometimes you only need to compare two specific objects, or an object and a point, without a callback:

```java
if (player.overlaps(door)) {
  openDoor();
}
if (button.overlapsPoint(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY())) {
  button.setColor(FlixelColor.YELLOW);
}
if (!bullet.inWorldBounds()) { // Outside Flixel.setWorldBounds(...).
  bullet.kill();
}
```

For the mouse there is also `Flixel.mouse.overlap(object)`, covered in [Input](input.md).

## Performance and limits

- **Checks are pair by pair.** Checking a group of 100 bullets against a group of 100 enemies tests 10,000 pairs. That
  is fine for most games, but checking every object against every other object in a huge level is not. Keep groups
  focused (only the things that can actually hit each other) and kill objects that are off screen.
- **Hitboxes are rectangles that do not rotate.** Rotating a sprite rotates its picture, not its hitbox. For a rotated
  shape, use a smaller hitbox that covers the important part.
- **Very fast objects can pass through thin walls.** If a bullet moves 50 pixels in one frame and the wall is 8 pixels
  thick, the bullet can be on one side in one frame and the other side the next ("tunneling"). Make walls thicker,
  lower the maximum speed, or check with `overlapsPoint(...)` along the path for very fast projectiles.
- **Collision callbacks run in the middle of the check.** Killing an object inside a callback is safe; it is skipped
  from then on.

## Common mistakes

- **Checking collisions before moving.** Call `collide` after `super.update(elapsed)`, or the result is one frame old.
- **Forgetting `setImmovable(true)` on the floor.** Without it, the player pushes the floor down as much as the floor
  pushes the player up.
- **Using `collide` for pickups.** Coins should not push the player around. Use `overlap`.
- **Writing the callback inline in `update(...)`.** A capturing lambda can create a new object every frame. Store it in
  a field.
- **Expecting drag to slow an accelerating object.** Drag only applies while that axis has no acceleration.

## Check your understanding

1. You set `setDrag(500f, 0f)` but the player keeps sliding while holding the right arrow. Why is that correct?
2. How do you make a platform that the player can jump up through but stand on?
3. Your coin callback is `(p, c) -> c.kill()`. Why should it be a field instead of written inline?

(Answers: drag only applies when acceleration on that axis is zero, and holding the key sets acceleration; make it
immovable and set `setAllowCollisions(FlixelDirectionFlags.UP)`; an inline lambda in a per-frame method can be a new
object every frame, which makes garbage.)

**Next:** [Input](input.md)
