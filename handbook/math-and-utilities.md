# Math, Randomness, Colors, and Utilities

This guide covers the small helpers almost every game uses: `FlixelMath` for clamping, interpolation, and fast
trigonometry; `FlixelRandom` for dice rolls and loot; `FlixelVector` and `FlixelRect` for geometry; bounds and ranges
for controlled randomness; `FlixelColor` for colors; `FlixelString` for text that changes; and the small flag and
constant classes used throughout the framework.

## Table of contents

1. [FlixelMath: everyday math helpers](#flixelmath-everyday-math-helpers)
2. [Randomness with FlixelRandom](#randomness-with-flixelrandom)
3. [Seeds: making randomness repeatable](#seeds-making-randomness-repeatable)
4. [Vectors and rectangles](#vectors-and-rectangles)
5. [Borrowing temporary vectors and rectangles](#borrowing-temporary-vectors-and-rectangles)
6. [Bounds and ranges](#bounds-and-ranges)
7. [Matrices and affine transforms](#matrices-and-affine-transforms)
8. [Colors](#colors)
9. [FlixelString: text without garbage](#flixelstring-text-without-garbage)
10. [Flags and constants](#flags-and-constants)
11. [Other helpers](#other-helpers)
12. [Check your understanding](#check-your-understanding)

## FlixelMath: everyday math helpers

Every method in `FlixelMath` is static and allocation-free, so it is safe to call inside `update(...)`:

```java
speed = FlixelMath.clamp(speed, 0f, MAX_SPEED);                      // Keep a value in a range.
float mid = FlixelMath.lerp(startX, endX, 0.5f);                      // Halfway between two values.
float turned = FlixelMath.lerpAngle(fromDeg, toDeg, 0.1f);            // Turn the short way around a circle.
displayedHp = FlixelMath.approach(displayedHp, hp, 120f * elapsed);   // Move toward a target without overshooting.
float wrapped = FlixelMath.wrap(angle, 0f, 360f);                     // Wrap around a range.
float pct = FlixelMath.remap(value, 0f, 255f, 0f, 1f);                // Convert from one range to another.
float snapped = FlixelMath.snap(x, 16f);                              // Snap to a 16-pixel grid.
int sign = FlixelMath.signOf(velocity);                               // -1, 0, or 1.
boolean same = FlixelMath.isEqual(a, b);                              // Compare floats with a small tolerance.
float rounded = FlixelMath.round(3.14159f, 2);                        // 3.14
```

Fast trigonometry uses a lookup table: a tiny loss of precision for a big speed win, which suits games well.

```java
float dx = FlixelMath.cos(angleRadians) * speed;
float dy = FlixelMath.sin(angleRadians) * speed;
float dxDeg = FlixelMath.cosDeg(angleDegrees) * speed;  // Degree versions too.
float aim = FlixelMath.atan2(targetY - y, targetX - x); // The angle from one point to another, in radians.
```

**Why `approach` instead of `lerp` for smoothing?** `lerp(a, b, 0.1f)` every frame moves 10% of the remaining distance
per frame, so it depends on the frame rate and never quite arrives. `approach(a, b, speed * elapsed)` moves a fixed
amount per second and stops exactly at the target. A health bar that drains at 120 points per second is an
`approach`.

## Randomness with FlixelRandom

`Flixel.random` is a ready-made random generator:

```java
int roll = Flixel.random.nextInt(1, 6);           // 1 to 6, both included. A die.
int index = Flixel.random.nextInt(10);            // 0 to 9.
float spread = Flixel.random.nextFloat(-5f, 5f);  // -5 up to (not including) 5.
float t = Flixel.random.nextFloat();              // 0 up to 1.
boolean coin = Flixel.random.nextBool();          // 50/50.
boolean crit = Flixel.random.nextBool(0.15f);     // True 15% of the time.
int dir = Flixel.random.sign();                   // -1 or 1.

String name = Flixel.random.pick(names);          // A random element of an array.
Flixel.random.shuffle(deck);                      // Shuffle an array in place.
```

A simple weighted loot table:

```java
float r = Flixel.random.nextFloat();
String loot = r < 0.6f ? "coin" : r < 0.9f ? "potion" : "sword"; // 60%, 30%, 10%.
```

## Seeds: making randomness repeatable

A random generator is not truly random: it is a long, fixed sequence of numbers that *looks* random, and the **seed**
picks where in the sequence you start. The same seed always produces the same sequence. Think of a shuffled deck of
cards with a serial number: shuffle two decks with the same serial number and they come out in the same order.

That makes seeds very useful:

```java
FlixelRandom levelRandom = new FlixelRandom(12345L); // Same seed, same level, every time.

// A "seed of the day" so every player gets the same challenge:
FlixelRandom daily = new FlixelRandom(dayNumber);
```

Use your own `FlixelRandom` for procedural generation, so other random calls in the game (particles, sound pitch)
do not change the level. Every particle emitter has its own `random` for the same reason.

## Vectors and rectangles

`FlixelVector` is a 2D point or direction (`x` and `y`). `FlixelRect` is a rectangle (`x`, `y`, `width`, `height`).
Both are mutable, and most methods change the object and return it, so calls chain:

```java
FlixelVector dir = new FlixelVector(targetX - x, targetY - y);
dir.normalize().scale(200f);              // A direction 200 pixels long.
float dist = a.distanceTo(b);
float len = dir.length();
dir.add(5f, 0f).setY(0f);

FlixelRect zone = new FlixelRect(100f, 100f, 64f, 64f);
boolean inside = zone.contains(px, py);
boolean hit = zone.overlaps(otherRect);
float right = zone.getRight();
float bottom = zone.getBottom();
```

## Borrowing temporary vectors and rectangles

Creating a new `FlixelVector` every frame for a quick calculation makes garbage. Both types have built-in pools.
Borrow with `get(...)`, return with `put()`:

```java
FlixelVector tmp = FlixelVector.get(enemy.getMidpointX(), enemy.getMidpointY());
camera.project(tmp);
drawArrowAt(tmp.x, tmp.y);
tmp.put(); // Return it. Never use tmp after this.
```

For a value you only pass straight into a framework method that accepts it, borrow a **weak** one with
`FlixelVector.weak(...)` or `FlixelRect.weak(...)`. The method that receives it returns it to the pool for you. If you
need a value for longer than one calculation, keep your own instance in a field instead of borrowing.

## Bounds and ranges

Two small families describe values that vary. Bounds say "pick something between these." Ranges say "slide from this
to that over a lifetime."

| Type                         | Meaning                                                                        |
|-------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `FlixelBounds`               | A minimum and maximum to roll a number between.                                |
| `FlixelPointBounds`          | Minimum and maximum points to roll a point between.                            |
| `FlixelRange`                | A start and end value to blend between with `lerp(t)`.                         |
| `FlixelPointRange`           | The same, for points.                                                          |
| `FlixelRangeBounds`          | The recipe for a range: bounds for the start, bounds for the end, and an optional ease. |
| `FlixelPointRangeBounds`     | The same, for points.                                                          |
| `FlixelColorBounds`, `FlixelColorRange`, `FlixelColorRangeBounds` | The color versions, in the `util` package. |

Particle emitters are the main user (see [Particles](particles.md)), but they work anywhere you want controlled
randomness:

```java
FlixelBounds enemySpeed = new FlixelBounds(80f, 120f);
float speed = Flixel.random.nextFloat(enemySpeed.min, enemySpeed.max);

FlixelRange shrink = new FlixelRange(1f, 0f);
sprite.setScale(shrink.lerp(age / lifespan)); // From full size to nothing over its life.
```

## Matrices and affine transforms

`FlixelMatrix` (a 4 by 4 matrix) and `FlixelAffine` (a 2D transform: move, rotate, scale, skew) are what cameras and
the renderer use to place things on screen. Most games never touch them. You meet them when writing a custom renderer,
drawing directly with the batch, or passing a projection to a shader.

## Colors

`FlixelColor` is a mutable color with float components from 0 to 1, read with `getRed()`, `getGreen()`, `getBlue()`, and
`getAlpha()` and changed with the matching setters, which clamp into that range:

```java
FlixelColor sky = new FlixelColor(0.4f, 0.7f, 1f, 1f);     // Floats from 0 to 1.
FlixelColor grass = new FlixelColor(34, 139, 34, 1f);      // 0 to 255 for RGB, 0 to 1 for alpha.
FlixelColor fromHex = new FlixelColor().set("#FF8800");    // Hex, with an optional alpha: "#FF880080".
FlixelColor packed = new FlixelColor(0xFF8800FF);          // A packed RGBA8888 int.

sky.setAlpha(0.5f);                           // Setters clamp, so setAlpha(2f) stores 1.
sky.setRed(255);                              // Red, green, and blue also take 0 to 255.
sky.lerp(FlixelColor.ORANGE, 0.25f);          // Move a quarter of the way toward orange.
sky.setHue(200f);                             // Hue, saturation, and value are adjustable too.
sky.setSaturation(0.5f);
```

Named presets: `WHITE`, `BLACK`, `RED`, `GREEN`, `BLUE`, `YELLOW`, `CYAN`, `MAGENTA`, `GRAY`, `CLEAR`, `ORANGE`, `PINK`,
`PURPLE`, `BROWN`, `OLIVE`, `MAROON`, `NAVY`, and `TEAL`.

**Never change a preset.** `FlixelColor.RED` is a shared object; changing it changes red everywhere in your game.
Copy it first:

```java
FlixelColor myRed = new FlixelColor(FlixelColor.RED);
myRed.a = 0.5f;
```

Colors are mutable so you can reuse one object every frame instead of creating new ones. Keep a color in a field and
`set(...)` it, rather than writing `new FlixelColor(...)` in `update(...)`.

## FlixelString: text without garbage

`FlixelString` is a reusable text buffer for text that changes often. It is covered in depth in
[Text, HUDs, and Bars](text-and-ui.md):

```java
private final FlixelString buffer = new FlixelString();

// Every frame:
buffer.set("HP: ").concat(hp).concat("/").concat(maxHp);
hpText.setText(buffer);
```

## Flags and constants

| Class                    | What it holds                                                                 |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `FlixelDirectionFlags`   | `LEFT`, `RIGHT`, `UP`, `DOWN`, `FLOOR`, `CEILING`, `WALL`, `ANY`, `NONE`, for collisions and facing. Combine with `\|`. |
| `FlixelAxes`             | `X`, `Y`, and `XY`, for centering, shaking, and similar per-axis options.     |
| `FlixelAlign`            | Horizontal and vertical alignment constants for text and layout helpers.      |
| `FlixelBlendMode`        | `NORMAL`, `ADD`, `MULTIPLY`, `SCREEN`, `SUBTRACT`, `LIGHTEN`, `DARKEN`.       |
| `FlixelAsciiCodes`       | Named character codes, handy when handling typed text.                        |

## Other helpers

- `FlixelSpriteUtil`: keep sprites in bounds, screen wrapping, fills, brightness, and simple shape drawing. See
  [Basics, Objects, and Sprites](sprites-and-objects.md).
- `FlixelBar`: health and progress bars. See [Text, HUDs, and Bars](text-and-ui.md).
- `FlixelExceptionUtil.getFullExceptionMessage(throwable)`: an exception and its causes as one string, handy for
  showing errors in a crash screen.

## Check your understanding

1. You need a die roll from 1 to 6. Is `nextInt(1, 6)` correct, or `nextInt(1, 7)`?
2. You set `FlixelColor.WHITE.setAlpha(0.5f)` to fade one sprite, and suddenly lots of things are see-through. Why?
3. Two players should get the exact same randomly generated dungeon. What do they need to share?

(Answers: `nextInt(1, 6)`, since both ends are included; you changed the shared preset instead of a copy; the seed.)

**Next:** [Graphics and Shaders](graphics-and-shaders.md)
