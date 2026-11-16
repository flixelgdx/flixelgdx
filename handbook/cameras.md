# Cameras: Following, Effects, HUDs, and Split Screens

A camera decides which part of your game world is visible and where on the window it appears. This guide covers
scrolling and following, keeping the camera inside the level, zoom, screen effects (shake, flash, fade), parallax,
HUD cameras, split screens, how the picture is scaled to fit any window, and converting between world and screen
positions.

## Table of contents

1. [What a camera is](#what-a-camera-is)
2. [The default camera](#the-default-camera)
3. [Scrolling by hand](#scrolling-by-hand)
4. [Following a target](#following-a-target)
5. [Keeping the camera inside the level](#keeping-the-camera-inside-the-level)
6. [Zoom](#zoom)
7. [Screen effects: shake, flash, and fade](#screen-effects-shake-flash-and-fade)
8. [Parallax with scroll factors](#parallax-with-scroll-factors)
9. [HUD cameras](#hud-cameras)
10. [Split screen and picture-in-picture](#split-screen-and-picture-in-picture)
11. [Fitting any window: viewports and scaling](#fitting-any-window-viewports-and-scaling)
12. [Converting between world and screen positions](#converting-between-world-and-screen-positions)
13. [Camera tint, rotation, and shaders](#camera-tint-rotation-and-shaders)
14. [Cameras and state switches](#cameras-and-state-switches)
15. [Check your understanding](#check-your-understanding)

## What a camera is

Moving a camera does not move the objects in the world; it moves *the view*, the same way a film camera pans across a
set while the actors stay where they are. Your level can be ten screens wide, and the camera shows one screen's worth
of it at a time.

Every frame, each camera in `Flixel.cameras` draws the current state from its own point of view, in list order. So two
cameras can show the same world from two places (split screen), or one camera can show the world while another shows
only the HUD.

## The default camera

The game starts with one camera that covers the whole window at your design resolution. Get it with:

```java
FlixelCamera camera = Flixel.cameras.first();
```

Its background color (`camera.bgColor`) is what shows where nothing is drawn. The state's `setBgColor(...)` sets it for
you in most games.

## Scrolling by hand

A camera's scroll is the world position of its top-left corner:

```java
camera.scrollX = 400f; // The left edge of the view is at world x = 400.
camera.scrollY = 0f;
camera.focusOn(1200f, 300f); // Center the view on a world point.
```

## Following a target

Most games let the camera follow the player. Pick a style suited to your genre:

```java
camera.follow(player, FlixelCamera.FollowStyle.PLATFORMER);
```

| Style              | Behavior                                                                                         |
|--------------------|--------------------------------------------------------------------------------------------------|
| `LOCKON`           | Keeps the target exactly centered every frame. The default.                                      |
| `PLATFORMER`       | A wide, short dead zone near the bottom: the camera ignores small hops and follows big moves.    |
| `TOPDOWN`          | A centered dead zone for free movement in every direction.                                       |
| `TOPDOWN_TIGHT`    | Like `TOPDOWN`, with a smaller dead zone.                                                        |
| `SCREEN_BY_SCREEN` | Jumps one full screen at a time when the target leaves the view, like classic adventure games.   |
| `NO_DEAD_ZONE`     | Does not track the target; the camera only moves when you scroll it.                             |

A **dead zone** is a box in the middle of the screen where the target can move without the camera following. Think of
a dog on a loose leash: the owner only moves when the dog pulls the leash tight. It makes the camera feel calm
instead of jittery. For a custom box, set `camera.deadzone` to a `FlixelRect` in screen pixels.

Fine-tune following with these fields:

```java
camera.follow(player, FlixelCamera.FollowStyle.LOCKON, 0.1f); // A lerp of 0.1: smooth, slightly lagging.
camera.followLerp = 0.1f;     // 1 snaps to the target each frame; smaller values glide behind it.
camera.followLeadX = 40f;     // Look ahead: show more of what is in front of the player.
camera.targetOffsetY = -30f;  // Aim a little above the target.
camera.snapToTarget();        // Jump straight to the target, for example right after a level loads.
```

The target can be anything with a position (any `FlixelPositional`), including emitters and invisible objects. A
trick for cutscenes is to follow an invisible object and tween it between points of interest.

## Keeping the camera inside the level

Without limits, the camera happily shows the empty space beyond the edges of your level. Scroll bounds stop it:

```java
// minX, maxX, minY, maxY in world pixels.
camera.setScrollBounds(0f, levelWidth, 0f, levelHeight);

// Or as a rectangle: x, y, width, height.
camera.setScrollBoundsRect(0f, 0f, levelWidth, levelHeight);
```

## Zoom

```java
camera.setZoom(2f);       // Twice as close.
camera.changeZoom(0.1f);  // A little closer.
float zoom = camera.getZoom();
```

Zoom is applied around the camera's center. Tween it for a dramatic punch-in:

```java
FlixelTween.tween(camera, new FlixelTweenSettings()
    .addGoal(camera::getZoom, 1.5f, camera::setZoom)
    .setDuration(0.4f)
    .setEase(FlixelEase::quadOut));
```

## Screen effects: shake, flash, and fade

Cameras have three built-in effects, each with an optional callback when it finishes:

```java
camera.shake(0.01f, 0.3f);                   // Intensity (a fraction of the screen), duration in seconds.
camera.flash(FlixelColor.WHITE, 0.2f);       // A quick white flash, like a camera bulb.
camera.fade(FlixelColor.BLACK, 1f);          // Fade TO black over one second.
camera.fade(FlixelColor.BLACK, 1f, true);    // Fade FROM black (fade in).
```

Intensity for shake is a fraction of the screen size, so small numbers go a long way: `0.005f` is a gentle rumble,
`0.03f` is an earthquake. The longest forms take a callback, a `force` flag that restarts an effect already running,
and (for shake) which axes to shake:

```java
camera.shake(0.02f, 0.5f, () -> Flixel.info("Done shaking"), true, FlixelAxes.X); // Only shake sideways.
camera.flash(FlixelColor.RED, 0.3f, null, true);
```

A fade with a callback is the classic way to move between states without a hard cut:

```java
camera.fade(FlixelColor.BLACK, 0.5f, false, () -> Flixel.switchState(NextState::new), false);
```

Stop effects early with `stopShake()`, `stopFlash()`, `stopFade()`, or `stopFX()` for all of them. Check whether one
is running with `isShakeActive()`, `isFlashActive()`, and `isFadeActive()`.

## Parallax with scroll factors

Every object has a **scroll factor** that controls how much it moves when the camera scrolls:

| Scroll factor | Effect                                                         |
|---------------|----------------------------------------------------------------|
| `1`           | Normal. The object is part of the world.                       |
| `0.5`         | Moves at half speed. Looks far away, like distant mountains.   |
| `0`           | Never moves. Pinned to the screen.                             |
| `1.5`         | Moves faster than the world. Looks close to the camera, like foreground leaves. |

Layering backgrounds with different scroll factors creates **parallax**, the depth effect you see from a train window
where nearby fences rush by while distant hills crawl:

```java
FlixelSprite sky = new FlixelSprite(0f, 0f, Flixel.files.internal("images/sky.png"));
sky.setScrollFactor(0f, 0f);       // Never moves.
FlixelSprite hills = new FlixelSprite(0f, 120f, Flixel.files.internal("images/hills.png"));
hills.setScrollFactor(0.3f, 0.3f); // Far away.
FlixelSprite trees = new FlixelSprite(0f, 200f, Flixel.files.internal("images/trees.png"));
trees.setScrollFactor(0.6f, 0.6f); // Closer.
add(sky);
add(hills);
add(trees);
```

## HUD cameras

A scroll factor of `0` pins one object to the screen, which is fine for a quick label. But if the world camera zooms or
shakes, the label zooms and shakes too. A real HUD uses its own camera that never scrolls, never zooms, and is drawn
on top of the world:

```java
FlixelCamera hud = new FlixelCamera(Flixel.getDesignWidth(), Flixel.getDesignHeight());
hud.bgColor.a = 0f;            // Transparent, so the world shows through.
hud.useBgAlphaBlending = true; // Needed for the transparent background to render properly.
hud.defaultDrawTarget = false; // World objects stay off this camera.
Flixel.cameras.add(hud);       // Added last, so it draws on top.

scoreText.cameras = new FlixelCamera[] {hud};
healthBar.cameras = new FlixelCamera[] {hud};
```

How this works: an object with no `cameras` list of its own draws on every camera whose `defaultDrawTarget` is `true`.
Turning that off for the HUD camera keeps the world off it, and HUD objects opt in by listing it explicitly. Think of
the HUD camera as a projector that only shows what you point at it directly.

Remember to tell the mouse and touch managers which camera to use when a click should land on HUD buttons:
`Flixel.mouse.getWorldX(hud)` or `Flixel.mouse.overlap(...)` on a button whose position is in HUD space.

## Split screen and picture-in-picture

Each camera can be given its own rectangle of the window. For two-player split screen, add a second camera and give
each one half of the window:

```java
FlixelCamera left = Flixel.cameras.first();
left.setRegionMode(FlixelCamera.RegionMode.NORMALIZED_RECT);
left.setScreenRegionNormalized(0f, 0f, 0.5f, 1f);  // Left half.
left.follow(player1, FlixelCamera.FollowStyle.TOPDOWN);

FlixelCamera right = new FlixelCamera(Flixel.getDesignWidth() / 2, Flixel.getDesignHeight());
right.setRegionMode(FlixelCamera.RegionMode.NORMALIZED_RECT);
right.setScreenRegionNormalized(0.5f, 0f, 0.5f, 1f); // Right half.
right.follow(player2, FlixelCamera.FollowStyle.TOPDOWN);
Flixel.cameras.add(right);
```

Normalized regions use fractions of the window (0 to 1) with a top-left origin, so they adapt to any window size. Pixel
regions are also available with `setScreenRegion(x, y, width, height)` and the region modes `PIXEL_TOP_LEFT`,
`PIXEL_BOTTOM_LEFT`, and `PIXEL_CENTERED`. A minimap is the same idea: a small, zoomed-out camera in a corner. Use
`clearScreenRegion()` to go back to the full window.

## Fitting any window: viewports and scaling

Players run your game in windows of every shape. Each camera owns a `FlixelViewport` that maps your design resolution
onto the real window with a scaling policy:

| Policy    | What happens when the window shape does not match your design size                        |
|-----------|--------------------------------------------------------------------------------------------|
| `FIT`     | Scales uniformly and adds black bars (letterboxing). Nothing is cut off or stretched. The default. |
| `EXTEND`  | Scales uniformly, then shows *more world* to fill the window. No bars, but players with wider screens see more. |
| `STRETCH` | Stretches to fill the window. Distorts the picture.                                        |

To change the policy for every camera the framework creates, replace the factory early (for example in your first
state, or a boot callback):

```java
FlixelCamera.viewportFactory = (w, h) -> new FlixelViewport(w, h, FlixelViewport.Scaling.EXTEND);
```

With `EXTEND`, use `Flixel.getVisibleWidth()` and `getVisibleHeight()` to place things against the real edges of the
screen. The separate topic of render resolution (drawing at a low resolution and scaling up, for crisp pixel art) is
covered in [Graphics and Shaders](graphics-and-shaders.md).

## Converting between world and screen positions

The mouse and touch managers convert for you (`Flixel.mouse.getWorldX()`), but sometimes you need to do it yourself,
for example to place a HUD arrow pointing at an off-screen enemy:

```java
FlixelVector point = FlixelVector.get(enemy.getMidpointX(), enemy.getMidpointY());
camera.project(point);   // World to screen, in place.
// ... use point.x and point.y ...
point.put();             // Return the vector to its pool.
```

`unproject(...)` goes the other way, from screen to world. Other handy checks:

```java
boolean visible = camera.containsPoint(enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());
float left = camera.getViewLeft();   // World X of the view's left edge, after zoom.
float right = camera.getViewRight();
```

## Camera tint, rotation, and shaders

```java
camera.color.set(FlixelColor.RED); // Tint everything this camera shows (via a copy, never the preset).
camera.alpha = 0.8f;
camera.angle = 5f;                 // A tilted view, in degrees.
camera.setShader(crtShader);       // Post-process this camera's picture.
camera.pixelPerfectRender = true;  // Snap drawing to whole pixels, for very crisp pixel art.
```

A camera shader renders the camera's picture to an off-screen image first, then draws it through the shader. Use it
for effects on one camera only (an underwater wobble on the world but not the HUD). For an effect on the entire screen,
use a global shader instead. See [Graphics and Shaders](graphics-and-shaders.md).

## Cameras and state switches

**Every state switch resets the camera list** to a single fresh default camera. That is deliberate: one level's
zoom, follow target, and HUD camera should never leak into the next screen. The flip side is that every state must
set up its own cameras in `create()`, and that you should not keep a `FlixelCamera` from one state in a field that
outlives it.

## Check your understanding

1. Your score text shakes when the camera shakes. What is the cleanest fix?
2. On ultra-wide monitors, players complain about black bars. Which scaling policy removes them, and what changes for
   your layout?
3. Why does the camera you configured in `MenuState.create()` lose its zoom when `PlayState` starts?

(Answers: draw it on a HUD camera with `defaultDrawTarget = false`; `EXTEND`, and you should place edge-hugging UI with
`Flixel.getVisibleWidth()`; cameras are reset on every state switch.)

**Next:** [Text, HUDs, and Bars](text-and-ui.md)
