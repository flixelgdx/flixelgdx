# Animation: Spritesheets, Atlases, Rigs, and State Machines

This guide shows how to bring sprites to life: cutting a spritesheet into frames, loading Sparrow atlases, playing
and controlling clips, lining up clips with offsets, reacting to frame events, playing Adobe Animate rigs, and
organizing complex animation rules with a state machine.

## Table of contents

1. [How sprite animation works](#how-sprite-animation-works)
2. [Turning animation on](#turning-animation-on)
3. [Grid spritesheets](#grid-spritesheets)
4. [Sparrow atlases](#sparrow-atlases)
5. [Playing, pausing, and reversing](#playing-pausing-and-reversing)
6. [Lining clips up with offsets](#lining-clips-up-with-offsets)
7. [Reacting to frames and endings](#reacting-to-frames-and-endings)
8. [Adobe Animate rigs](#adobe-animate-rigs)
9. [Animation state machines](#animation-state-machines)
10. [Performance notes](#performance-notes)
11. [Common mistakes](#common-mistakes)
12. [Check your understanding](#check-your-understanding)

## How sprite animation works

Sprite animation is a flipbook. You draw a character in several poses, put the poses on one image, and show them one
after another quickly enough that the eye sees motion. Each pose is a **frame**, and a named sequence of frames (such
as "walk" or "jump") is a **clip** (also called an animation).

Every sprite has an animation controller, `sprite.animation`, that keeps a list of clips, knows which one is playing,
and swaps the sprite's frame at the right moments during `update(...)`.

## Turning animation on

The controller is created on demand, so sprites that never animate pay nothing for it. Call `ensureAnimation()` once
before using `sprite.animation`; it creates the controller if needed and returns it:

```java
FlixelAnimationController anim = player.ensureAnimation();
// From now on, player.animation is the same object.
```

## Grid spritesheets

If your frames are laid out on a grid of equally sized cells, load the sheet with the frame size, then list the frame
numbers for each clip. Numbers count from the top-left cell, left to right, then top to bottom:

```
+----+----+----+----+
| 0  | 1  | 2  | 3  |
+----+----+----+----+
| 4  | 5  | 6  | 7  |
+----+----+----+----+
```

```java
player.loadGraphic(Flixel.files.internal("images/hero.png"), 32, 32); // Each cell is 32 by 32.
player.ensureAnimation();
player.animation.add("idle", new int[] {0, 1}, 4);         // Name, frames, frames per second.
player.animation.add("walk", new int[] {2, 3, 4, 5}, 10);  // Clips loop by default.
player.animation.play("idle", true);                       // true = loop.
```

Frames can repeat and appear in any order, so `new int[] {0, 1, 2, 1}` makes a back-and-forth breathing clip from
three drawings.

## Sparrow atlases

Grids waste space when frames have different sizes. A **Sparrow atlas** packs frames tightly into one PNG and
describes each frame's rectangle in an XML file with the same name. Tools such as Adobe Animate and TexturePacker can
export it. Pass the shared path *without* an extension, and both files are found:

```java
// Loads images/hero.png and images/hero.xml.
FlixelSprite hero = new FlixelSprite();
hero.ensureAnimation().addSparrowFrames(Flixel.files.internal("images/hero"));
```

Then build clips from **frame name prefixes**. Frames in a Sparrow atlas are usually named like `Hero Idle0000`,
`Hero Idle0001`, and so on, so the prefix `"Hero Idle"` collects every idle frame in order:

```java
hero.animation.addByPrefix("idle", "Hero Idle", 24, true);   // Name, prefix, frames per second, loop.
hero.animation.addByPrefix("jump", "Hero Jump", 24, false);
hero.animation.play("idle");
```

If the PNG and XML do not share a name, pass both files: `addSparrowFrames(pngFile, xmlFile)`.

To pick specific frames by the **number in their name** instead of taking the whole prefix, use `addByIndices`. The
number after the prefix is matched by value, so index `3` finds `Hero Idle0003`. Frames can be picked in any order and
repeated, which is handy for splitting one long sequence into two clips:

```java
// Name, prefix, frame numbers, postfix (often ""), frames per second, loop.
hero.animation.addByIndices("danceLeft", "Hero Dance", new int[] {0, 1, 2, 3, 4, 5}, "", 24, false);
hero.animation.addByIndices("danceRight", "Hero Dance", new int[] {6, 7, 8, 9, 10, 11}, "", 24, false);
```

Use the postfix when your exporter adds text after the number, such as `"Hero Dance0003.png"` with a postfix of
`".png"`. Numbers with no matching frame are skipped and logged as a warning.

## Playing, pausing, and reversing

```java
anim.play("walk");                 // Play with the clip's own loop setting.
anim.play("walk", true);           // Play and loop.
anim.play("attack", false, true);  // Restart from the first frame even if it is already playing.

anim.pause();
anim.resume();
anim.reverse();                    // Play backward from the current frame.

String current = anim.getCurrentAnim();
boolean done = anim.isFinished();  // True when a non-looping clip has reached its end.
int frame = anim.getCurrentKeyframeIndex();
```

Calling `play(...)` with the clip that is already playing does nothing, so it is safe to call every frame:

```java
anim.play(Math.abs(player.getVelocityX()) > 1f ? "walk" : "idle");
```

## Lining clips up with offsets

Different clips are often drawn at slightly different places in their frames. A sword swing might extend to the left,
so the character's body seems to jump when the clip changes. An **offset** shifts the picture for one clip so the
body stays put:

```java
anim.addOffset("idle", 0f, 0f);
anim.addOffset("attack", 12f, 6f); // Positive values move the picture left and up.
anim.play("attack");               // The offset is applied automatically when the clip plays.
```

`removeOffset(...)`, `clearOffsets()`, `hasOffset(...)`, and `centerOffsets()` manage them. Offsets only move the
picture, never the hitbox (see [Basics, Objects, and Sprites](sprites-and-objects.md)).

## Reacting to frames and endings

The controller has two signals:

```java
// Every time the displayed frame changes. Great for footstep sounds.
anim.onFrameChanged.add(data -> {
  if ("walk".equals(data.getAnimationName()) && data.getFrameIndex() == 2) {
    Flixel.sound.play(Flixel.files.internal("sounds/step.ogg"));
  }
});

// When a non-looping clip finishes.
anim.onAnimationFinished.add(data -> {
  if ("attack".equals(data.getAnimationName())) {
    anim.play("idle");
  }
});
```

Add these listeners once, in `create()`, not every frame. The data object is reused, so read from it inside the
callback and do not keep it.

## Adobe Animate rigs

Adobe Animate can export a character as a texture atlas rig: a spritemap (`spritemap1.png` and `spritemap1.json`)
plus a timeline (`Animation.json`) that says how pieces of the image move. Rigs are smaller than baking every frame
into a sheet, and they keep the smooth motion of the original animation. FlixelGDX plays them with
`FlixelAnimateSprite`, including exports from the Better Texture Atlas extension:

```java
FlixelAnimateSprite boss = new FlixelAnimateSprite();
boss.addSpritemapAndAnimation(Flixel.files.internal("images/boss")); // A folder holding the three files.
boss.setScale(0.65f);
boss.setAntialiasing(true);
boss.updateHitbox();
boss.screenCenter();
boss.animation.play("Idle");
add(boss);
```

The folder form expects the default file names (`spritemap1` and `Animation`); change them with the static fields
`FlixelAnimateSprite.defaultSpritemapFileName` and `defaultAnimationFileName`, or pass the three files explicitly.
Calling `addSpritemapAndAnimation(...)` again merges another export into the same rig, so a character can be split
across several exports. A rig sprite still has a normal animation controller, so you can mix in Sparrow frames:

```java
boss.animation.addSparrowFrames(Flixel.files.internal("images/boss-extras"));
```

Everything else (position, scale, rotation, tint, flip, hitbox, collisions) works like any other sprite.

## Animation state machines

As a character grows, animation rules multiply. "Attack plays once and then goes back to idle." "You cannot start
running in the middle of an attack." "When the hurt clip starts, flash red." Scattering `play(...)` calls through
gameplay code to enforce these rules gets messy quickly.

A state machine collects the rules in one place. Think of a board game: each square is a state, and arrows say which
squares you may move to next. The game code only says "I want to be in `attack` now," and the machine decides whether
that move is legal, plays the right clip, and runs any hooks.

```java
FlixelAnimationStateMachine fsm = new FlixelAnimationStateMachine(player);

fsm.addState("idle", "idle").allowTo("run", "attack", "hurt");
fsm.addState("run", "run").allowTo("idle", "attack", "hurt");
fsm.addState("attack", "attack")
    .autoAdvanceTo("idle")             // Go back to idle when the clip finishes.
    .onEnter(() -> sword.swing());
fsm.addState("hurt", "hurt")
    .autoAdvanceTo("idle")
    .delay(0.2f)                       // Hold the last frame for 0.2 seconds before advancing.
    .onEnter(() -> player.setColor(FlixelColor.RED))
    .onExit(() -> player.setColor(FlixelColor.WHITE));

fsm.setState("idle");
player.animation.setStateMachine(fsm); // The controller now updates the machine every frame.
```

Then gameplay code only expresses intent:

```java
if (controls.attack.justPressed()) {
  fsm.setState("attack");
} else if (Math.abs(player.getVelocityX()) > 1f) {
  fsm.setState("run");
} else {
  fsm.setState("idle");
}
```

While the attack plays, the requests for "run" and "idle" are rejected because `attack` does not allow them, so the
swing finishes. `setState(...)` returns `false` when a move is rejected. `setState(name, true)` forces a re-entry,
which replays the clip and runs `onEnter` again (handy for attacking again mid-swing).

State options at a glance:

| Method                     | Effect                                                                  |
|----------------------------|-------------------------------------------------------------------------|
| `allowTo(names...)`        | The only states this one may move to. With no `allowTo`, any move is allowed. |
| `loop(boolean)`            | Whether the clip loops while in this state.                             |
| `autoAdvanceTo(name)`      | Move to another state when the clip finishes (implies no looping).      |
| `delay(seconds)`           | Hold the final frame before auto-advancing.                             |
| `onEnter(action)`          | Run code when the state is entered.                                     |
| `onExit(action)`           | Run code when the state is left.                                        |

## Performance notes

- Do not call `ensureAnimation()` on sprites that never animate. A controller holds several collections, and creating
  one for every tile in a large level adds up.
- Sprites that share the same atlas image share one texture, so a hundred animated enemies still draw in very few
  batches.
- Register clips once (in `create()` or the sprite's constructor), never in `update(...)`.

## Common mistakes

- **`animation` is `null`.** Call `ensureAnimation()` first.
- **The clip does not restart.** `play(...)` on the clip already playing does nothing. Pass `forceRestart = true`.
- **Sparrow frames not found.** The prefix must match the start of the frame names in the XML exactly, including
  capitalization and spaces.
- **The character jumps when the clip changes.** Add per-clip offsets.

## Check your understanding

1. Your idle clip is frames 0 and 1 at 4 frames per second. What do you pass to `add(...)`?
2. You call `anim.play("walk")` every frame. Does the walk clip keep restarting?
3. A one-shot "attack" must always finish before the character can run. What tool makes that rule easy?

(Answers: `add("idle", new int[] {0, 1}, 4)`; no, playing the current clip again does nothing; a
`FlixelAnimationStateMachine` where `attack` has no `allowTo("run")` and auto-advances to `idle`.)

**Next:** [Cameras](cameras.md)
