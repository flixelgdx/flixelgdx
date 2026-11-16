# Basics, Objects, and Sprites: The Building Blocks

Nearly everything you put on screen comes from a small family of classes: `FlixelBasic`, `FlixelObject`, and
`FlixelSprite`. This guide explains what each layer adds, the four lifecycle flags every object has, the difference
between killing and destroying, and everything a sprite can do with its picture.

## Table of contents

1. [The family tree](#the-family-tree)
2. [FlixelBasic: the four lifecycle flags](#flixelbasic-the-four-lifecycle-flags)
3. [kill(), revive(), and destroy()](#kill-revive-and-destroy)
4. [FlixelObject: position, size, and angle](#flixelobject-position-size-and-angle)
5. [FlixelSprite: giving an object a picture](#flixelsprite-giving-an-object-a-picture)
6. [Positioning helpers](#positioning-helpers)
7. [Looks: scale, rotation, color, alpha, and flipping](#looks-scale-rotation-color-alpha-and-flipping)
8. [The hitbox and the picture are separate](#the-hitbox-and-the-picture-are-separate)
9. [Blend modes, antialiasing, and shaders](#blend-modes-antialiasing-and-shaders)
10. [Clipping part of a sprite](#clipping-part-of-a-sprite)
11. [Draw order](#draw-order)
12. [Making your own sprite classes](#making-your-own-sprite-classes)
13. [Choosing which cameras draw an object](#choosing-which-cameras-draw-an-object)
14. [Sprite helper functions](#sprite-helper-functions)
15. [How textures are shared](#how-textures-are-shared)
16. [Check your understanding](#check-your-understanding)

## The family tree

Each class adds one layer on top of the previous one, like Russian nesting dolls:

| Class          | Adds                                                                 | Use it for                                          |
|----------------|----------------------------------------------------------------------|-----------------------------------------------------|
| `FlixelBasic`  | Updating, drawing, and four on/off flags. No position.               | Invisible "manager" objects that tick every frame.  |
| `FlixelObject` | A position, a size (its hitbox), an angle, and built-in physics.     | Invisible trigger zones, walls, and spawn points.   |
| `FlixelSprite` | A picture: images, animations, scale, tint, transparency, flipping.  | Almost everything you can see. The class you will use most. |

Other visual classes build on top of `FlixelSprite`: `FlixelText` (text), `FlixelSpriteGroup` (several sprites
moved as one), `FlixelParticle` (particles), `FlixelBar` (health and progress bars), and `FlixelAnimateSprite`
(Adobe Animate rigs). Because they are all sprites, everything in this guide also works for them.

## FlixelBasic: the four lifecycle flags

Every `FlixelBasic` has four public flags that control what the framework does with it each frame:

| Flag      | When `false`...                                                               |
|-----------|-------------------------------------------------------------------------------|
| `active`  | `update(...)` is skipped. The object freezes in place but is still drawn.     |
| `visible` | `draw(...)` is skipped. The object is invisible but keeps updating.           |
| `exists`  | Neither `update(...)` nor `draw(...)` runs. It is switched off but kept in memory. |
| `alive`   | Nothing changes by itself. It is a gameplay flag meaning "this has not been defeated." |

They are plain fields, so you can set them directly, or use the matching setters (`setActive(...)`,
`setVisible(...)`, `setExists(...)`) and helpers such as `toggleVisible()`.

```java
enemy.active = false;   // Frozen, like a statue.
enemy.visible = false;  // Invisible, but still walking around.
enemy.exists = false;   // Gone from the game, but not freed.
```

Every `FlixelBasic` also has a unique, read-only `id` number, which the debugger uses to identify it.

## kill(), revive(), and destroy()

These three sound similar but mean very different things:

- `kill()` switches an object off (`alive = false`, `exists = false`) **without** freeing it. It is like putting a toy
  back in the toy box: it is out of the game, but you can take it out again later.
- `revive()` takes it back out of the toy box (`alive = true`, `exists = true`).
- `destroy()` throws the toy away for good and releases its memory and textures. Never use an object after
  destroying it.

The golden rule: **for things that come and go often (bullets, enemies, coins, particles), kill and revive instead of
creating and destroying.** Creating new objects while the game runs makes garbage, and garbage makes the game stutter.
[Groups and Recycling](groups-and-recycling.md) shows the recycling pattern that makes this easy.

You rarely call `destroy()` yourself. A state destroys all of its members when it is replaced, and a group destroys
its members when it is destroyed.

## FlixelObject: position, size, and angle

A `FlixelObject` is a rectangle in the world: a position, a width, and a height. That rectangle is its **hitbox**, the
shape used for collisions. It also has an angle and a full set of motion properties (velocity, acceleration, drag),
which are covered in [Physics and Collisions](physics-and-collisions.md).

```java
FlixelObject trigger = new FlixelObject(200f, 300f, 64f, 32f); // x, y, width, height.

trigger.getX();          // 200
trigger.getMidpointX();  // 232: the horizontal center.
trigger.setPosition(10f, 20f);
trigger.setSize(32f, 32f);
trigger.changeX(5f);     // Move 5 pixels right.
trigger.setAngle(45f);   // Degrees, clockwise.
```

An invisible `FlixelObject` is handy for areas the player should not see: a door trigger, the bottom of a pit, or a
spawn point.

## FlixelSprite: giving an object a picture

A sprite needs a graphic. There are three common ways to give it one:

```java
// From an image in the assets folder.
FlixelSprite player = new FlixelSprite(100f, 50f);
player.loadGraphic(Flixel.files.internal("images/player.png"));

// Or in one line, through the constructor.
FlixelSprite tree = new FlixelSprite(300f, 80f, Flixel.files.internal("images/tree.png"));

// A solid colored rectangle: great for prototypes, walls, and fade overlays.
FlixelSprite floor = new FlixelSprite(0f, 340f);
floor.makeGraphic(640, 20, FlixelColor.GRAY);
```

`loadGraphic(...)` resizes the sprite (and its hitbox) to match the image. To cut an image into equally sized frames
for animation, pass the frame size, and the hitbox matches one frame:

```java
player.loadGraphic(Flixel.files.internal("images/hero.png"), 32, 32); // 32 by 32 frames.
```

`loadGraphic(...)` and `makeGraphic(...)` return the sprite, so you can chain calls:

```java
add(new FlixelSprite().makeGraphic(8, 8, FlixelColor.YELLOW));
```

Animating those frames is covered in [Animation](animation.md).

## Positioning helpers

```java
player.setPosition(120f, 200f);
player.setX(10f);
player.changeX(5f);                 // Move 5 pixels to the right.
player.screenCenter();              // Center on the screen on both axes.
player.screenCenter(FlixelAxes.X);  // Center horizontally only.
float centerX = player.getMidpointX();
```

`screenCenter()` uses the design resolution, so it works the same no matter how big the real window is.

## Looks: scale, rotation, color, alpha, and flipping

```java
player.setScale(2f);                         // Twice as big, grown around the origin. The hitbox is unchanged.
player.updateHitbox();                       // Make the hitbox match the new size and line up with it.
player.setGraphicSize(64, 64);               // Stretch the picture to an exact pixel size.
player.setAngle(45f);                        // Rotate 45 degrees clockwise.
player.setOrigin(0f, 0f);                    // Rotate and scale around the top-left corner instead of the center.
player.setOriginCenter();                    // Back to the center.
player.setAlpha(0.5f);                       // Half transparent.
player.setColor(FlixelColor.RED);            // Tint red. White means "no tint."
player.setFlipX(true);                       // Mirror horizontally.
player.setFacing(FlixelDirectionFlags.LEFT); // Facing left draws the picture mirrored automatically.
```

A few details worth knowing:

- **Tint multiplies.** A white sprite tinted red becomes red, but a blue sprite tinted red becomes black. Tinting works
  best on light or grayscale art.
- **Facing is a convenience.** Set `setFacing(FlixelDirectionFlags.LEFT)` when the player walks left and
  `RIGHT` when they walk right, and the sprite flips itself. It saves you from tracking `flipX` by hand.
- **Never change the color presets.** `FlixelColor.RED` and friends are shared objects. To tweak one, copy it first:
  `new FlixelColor(FlixelColor.RED)`.

## The hitbox and the picture are separate

This is one of the most important ideas in the framework. The **hitbox** (`getX()`, `getY()`, `getWidth()`,
`getHeight()`) is what collisions use. The **picture** can be scaled, rotated, or shifted without moving the hitbox.

Think of a cartoon character wearing a big hat. You want the character to fit through doors even though the hat is
wider than the door. So you give the character a slimmer, invisible "body box" for bumping into things, and draw the
picture (hat and all) on top of it.

```java
player.loadGraphic(Flixel.files.internal("images/player.png")); // A 32 by 48 image.
player.setSize(20f, 40f);   // A slimmer hitbox.
player.setOffset(6f, 8f);   // Shift the picture so it lines up with the hitbox.
```

`setOffset(x, y)` moves the picture relative to the hitbox. After scaling, `updateHitbox()` recomputes the size and
offset so the hitbox matches the scaled picture again:

```java
player.setScale(2f);
player.updateHitbox(); // The hitbox doubles too.
```

Turn on hitboxes in debug mode (F2) to see the boxes while you tune them. See [Debugging](debugging.md).

## Blend modes, antialiasing, and shaders

```java
player.setBlendMode(FlixelBlendMode.ADD); // Glowing, additive blending. Good for fire, lasers, and magic.
player.setAntialiasing(false);            // Crisp pixels for pixel art.
player.setShader(myShader);               // A custom WGSL shader for this sprite.
```

The blend modes are `NORMAL`, `ADD`, `MULTIPLY`, `SCREEN`, `SUBTRACT`, `LIGHTEN`, and `DARKEN`. Antialiasing is off
by default, which suits pixel art; turn it on for smooth, high-resolution art, or globally with
`Flixel.setAntialiasing(true)`. Changing the blend mode or shader between sprites makes the GPU start a new batch, so
keep sprites with the same blend mode next to each other in draw order. Shaders are covered in
[Graphics and Shaders](graphics-and-shaders.md).

## Clipping part of a sprite

A clip rectangle shows only part of a sprite, like looking at it through a window. It is useful for reveal effects,
scrolling text boxes, and meters:

```java
// Only show the left half of the sprite.
sprite.setClipRect(0, 0, sprite.getWidth() * 0.5f, sprite.getHeight());

// Slide the window right by 10 pixels later.
sprite.changeClipRectX(10);

// Show the whole sprite again.
sprite.clearClipRect();
```

## Draw order

Objects are drawn in the order you `add()` them: the first added is at the back, the last added is in front. Add the
background first and the HUD last. To reorder later, groups have helpers such as `bringToFront(...)`,
`sendToBack(...)`, and `sort(...)` (see [Groups and Recycling](groups-and-recycling.md)). A common trick for
top-down games is putting characters in their own group and sorting it by Y each frame, so lower characters draw in
front of higher ones.

## Making your own sprite classes

For anything with behavior, make a subclass. Keep its logic inside, and call `super` in every override:

```java
import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;

public class Coin extends FlixelSprite {

  private float bobTime;

  public Coin(float x, float y) {
    super(x, y, Flixel.files.internal("images/coin.png"));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed); // Moves the sprite and advances its animation.
    bobTime += elapsed;
    setOffset(0f, (float) Math.sin(bobTime * 4f) * 2f); // Bob the picture, not the hitbox.
  }

  @Override
  public void revive() {
    super.revive();
    bobTime = 0f; // Reset anything that might hold an old value when reused.
  }
}
```

Keep in mind that `update(...)` runs every frame, so avoid creating objects in it. See
[Optimization](optimization.md).

## Choosing which cameras draw an object

Every `FlixelBasic` has a public `cameras` array. When it is `null` (the default), the object draws on every camera
whose `defaultDrawTarget` is `true`. Set it to draw only on specific cameras, which is how HUDs stay off the world
camera and vice versa:

```java
scoreText.cameras = new FlixelCamera[] {hudCamera};
```

`setScrollFactor(x, y)` is the other half of this story: it controls how much an object moves when a camera scrolls.
`1` is normal, `0.5` moves at half speed (a distant background for parallax), and `0` never moves (pinned to the
screen). See [Cameras](cameras.md).

## Sprite helper functions

`FlixelSpriteUtil` holds small helpers that work on any sprite:

```java
// Keep a sprite inside an area.
FlixelSpriteUtil.bound(player, 0f, levelWidth, 0f, levelHeight);

// Asteroids-style wrapping: leave one side, appear on the other.
FlixelSpriteUtil.screenWrap(ship, true, true, true, true);

// Fill a sprite's graphic with a solid color, or change its brightness.
FlixelSpriteUtil.fill(sprite, FlixelColor.BLACK);
FlixelSpriteUtil.setBrightness(sprite, 0.5f);
```

## How textures are shared

When ten sprites load `images/coin.png`, the image is loaded and uploaded to the GPU only once. All ten sprites share
one `FlixelGraphic`, which counts how many sprites use it. When the last user is destroyed, the texture is freed. You
get this for free with `loadGraphic(...)`; you only manage it yourself when you work with the asset manager directly
(see [Files and Assets](files-and-assets.md)).

## Check your understanding

1. You want an enemy to stop moving while a cutscene plays, but stay visible. Which flag do you change?
2. Your character's hair keeps bumping into ceilings. What do you change, and does the picture move?
3. A bullet left the screen. Should you call `kill()` or `destroy()`?

(Answers: `active = false`; shrink the hitbox with `setSize(...)` and line the picture up with `setOffset(...)`, and the
picture stays where it was drawn; `kill()`, so the bullet can be recycled.)

**Next:** [The Flixel Class](the-flixel-class.md)
