# Text, HUDs, and Bars

This guide covers drawing text with `FlixelText`, choosing and registering fonts, formatting and borders, wrapping
and measuring text, updating text every frame without making garbage, and building HUD pieces such as health bars,
loading bars, and buttons.

## Table of contents

1. [Text is a sprite](#text-is-a-sprite)
2. [Creating text](#creating-text)
3. [Formatting: size, color, alignment, and style](#formatting-size-color-alignment-and-style)
4. [Shadows and outlines](#shadows-and-outlines)
5. [Fixed-width fields and word wrap](#fixed-width-fields-and-word-wrap)
6. [Fonts](#fonts)
7. [Text that changes every frame](#text-that-changes-every-frame)
8. [Measuring text](#measuring-text)
9. [Bars: health, stamina, and loading](#bars-health-stamina-and-loading)
10. [Building a HUD](#building-a-hud)
11. [A simple clickable button](#a-simple-clickable-button)
12. [Common mistakes](#common-mistakes)
13. [Check your understanding](#check-your-understanding)

## Text is a sprite

`FlixelText` extends `FlixelSprite`, so text is placed in states and groups like any other sprite, and you can move,
tint, fade, scale, rotate, and tween it. A title that slides in and fades up is just a sprite tween. The only things a
text object cannot do are load an image or play an animation, since its picture is the text itself.

## Creating text

The full constructor takes a position, a field width, the text, and a size in pixels:

```java
// x, y, field width (0 = grow to fit the text), text, size.
FlixelText title = new FlixelText(0f, 40f, 0f, "My Awesome Game", 32);
title.screenCenter(FlixelAxes.X);
add(title);
```

Shorter constructors leave out the trailing arguments: `new FlixelText(x, y)`, `new FlixelText(x, y, width)`, and
`new FlixelText(x, y, width, text)` (size 8). A built-in font is always available, so text works with no setup.

## Formatting: size, color, alignment, and style

```java
label.setTextSize(24);
label.setColor(FlixelColor.YELLOW);
label.setAlignment(FlixelText.Alignment.CENTER); // LEFT, CENTER, or RIGHT inside the field width.
label.setBold(true);
label.setItalic(true);
label.setLetterSpacing(2f);                      // Extra pixels between letters.

// Several at once:
label.setFormat(24, FlixelColor.YELLOW);                 // Size and color, default font.
label.setFormat("ui", 24, FlixelColor.YELLOW);           // A registered font, size, and color.
```

Alignment only matters when the text has a field width wider than the text (see below).

## Shadows and outlines

Text on a busy background is hard to read. A border fixes that:

```java
// Black drop shadow, 2 pixels offset, quality 1.
label.setBorderStyle(FlixelText.BorderStyle.SHADOW, FlixelColor.BLACK, 2f, 1f);

// Black outline, 1 pixel thick.
label.setBorderStyle(FlixelText.BorderStyle.OUTLINE, FlixelColor.BLACK, 1f, 1f);

// Shorter forms use a size and quality of 1.
label.setBorderStyle(FlixelText.BorderStyle.OUTLINE, FlixelColor.BLACK);
label.setBorderStyle(FlixelText.BorderStyle.NONE);
```

An outline draws the text several times, so prefer a shadow for large amounts of text.

## Fixed-width fields and word wrap

A field width of `0` makes a single-line label that grows to fit its text. A positive field width makes a column of
that width, and long text wraps onto new lines, like a paragraph in a word processor:

```java
FlixelText dialog = new FlixelText(20f, 280f, 600f, "Welcome, traveler! The road ahead is long...", 16);
dialog.setWordWrap(true);
dialog.setAlignment(FlixelText.Alignment.LEFT);
add(dialog);
```

Change the box later with `setFieldWidth(...)` and `setFieldHeight(...)`, and turn automatic sizing on or off with
`setAutoSize(...)`.

## Fonts

There are three ways to choose a font.

**1. Register a scalable font once, then use it everywhere by name.** This is the recommended approach. TrueType
(`.ttf`) and OpenType (`.otf`) files are turned into sharp glyph images at whatever pixel size you ask for, and each
size is cached, so asking again is instant.

```java
// Once, early (for example in your first state's create()):
FlixelFontRegistry.register("ui", Flixel.files.internal("fonts/Nunito.ttf"));
FlixelFontRegistry.register("title", Flixel.files.internal("fonts/Bangers.ttf"));
FlixelFontRegistry.setDefault("ui"); // Text without a font now uses "ui".

// Anywhere:
FlixelText heading = new FlixelText(0f, 20f, 0f, "Level Complete!", 32);
heading.setFont("title");
```

**2. Point a single text object at a file**, without registering it: `label.setFont(Flixel.files.internal("fonts/Mono.ttf"))`.

**3. Use a bitmap font.** Pixel-art games often ship a hand-drawn font in the AngelCode `.fnt` format plus a page image.
Build a `FlixelFont` from it with `FlixelFont.fromFnt(...)` and pass it to `setFont(FlixelFont)`.

If a font cannot be found or the platform cannot rasterize it, the built-in font is used and a warning is logged, so
text never disappears. Font smoothing follows the text's antialiasing setting: turn antialiasing off for crisp pixel
fonts.

## Text that changes every frame

Building a new `String` like `"Score: " + score` every frame creates garbage, and garbage causes stutter. For a score,
timer, or FPS counter, keep one reusable `FlixelString` and write numbers into it:

```java
private final FlixelString scoreBuffer = new FlixelString();
private FlixelText scoreText;

@Override
public void create() {
  super.create();
  scoreText = new FlixelText(8f, 8f, 0f, "", 16);
  add(scoreText);
}

@Override
public void update(float elapsed) {
  super.update(elapsed);
  scoreBuffer.set("Score: ").concat(score);
  scoreText.setText(scoreBuffer); // No new String is created.
}
```

`FlixelString` is a reusable text buffer. `set(...)` replaces its contents, and `concat(...)` appends text, numbers,
booleans, or characters without creating objects. For decimals, use `concatFloatRounded(value, decimals)`:

```java
timerBuffer.set("Time: ").concatFloatRounded(timeLeft, 1).concat("s");
```

`setText(...)` also checks whether the text actually changed, and skips all the layout work when it did not, so
calling it every frame with the same value is cheap. Avoid `getText()` in per-frame code, because it creates a new
`String`; read `getTextBuffer()` instead. See [Optimization](optimization.md) for more garbage-free habits.

## Measuring text

Layout methods tell you exactly where the text landed, which helps with centering, typing effects, and click-to-select:

```java
float w = label.getTextWidth();      // Width of the text itself.
float h = label.getTextHeight();
int lines = label.getLineCount();
float lineHeight = label.getLineHeight();
float charX = label.getCharX(5);     // X of the sixth character.
int index = label.getIndexAt(mx, my); // Which character is at a point.
```

A classic **typewriter effect** reveals text one character at a time. Keep the full text in one buffer and copy a
growing slice into another:

```java
private final FlixelString shown = new FlixelString();
private final String full = "It was a dark and stormy night...";
private float typed;

// In update(...):
typed = Math.min(full.length(), typed + 30f * elapsed); // 30 characters per second.
shown.set(full).setLength((int) typed);
dialog.setText(shown);
```

## Bars: health, stamina, and loading

`FlixelBar` draws a value as a filled bar. It is a sprite, so it can be positioned, tinted, and grouped like the rest of
your HUD.

```java
FlixelBar health = new FlixelBar(10f, 10f, 120f, 12f)  // x, y, width, height.
    .setRange(0f, 100f)
    .setTrack(player::getHealth)                       // Read the value every frame.
    .setLerp(0.15f)                                    // Glide toward the new value instead of snapping.
    .setEmptyColor(FlixelColor.BLACK)
    .setFilledColor(FlixelColor.GREEN)
    .setBorder(FlixelColor.WHITE, 1f)
    .setThresholdColors(FlixelColor.GREEN, FlixelColor.RED, 0.25f); // Turn red below 25%.
add(health);
```

| Method                                        | Purpose                                                           |
|-----------------------------------------------|-------------------------------------------------------------------|
| `setRange(min, max)` / `setMaxSupplier(...)`  | The value range, fixed or read every frame (for leveling up).     |
| `setValue(v)` or `setTrack(supplier)`         | Set the value yourself, or have the bar read it every frame.      |
| `setFillDirection(...)`                       | `LEFT_TO_RIGHT`, `RIGHT_TO_LEFT`, `TOP_TO_BOTTOM`, or `BOTTOM_TO_TOP`. |
| `setLerp(amount)`                             | Smoothing for the displayed value (`1` snaps instantly).          |
| `setGradient(start, end)`                     | A two-color fill.                                                 |
| `setEmptyGraphic(...)` / `setFilledGraphic(...)` | Use image frames instead of colors.                            |
| `setThresholdColors(...)` / `setThresholdStops(...)` | Change the fill color at certain percentages.              |
| `setText(...)` / `setTextOffset(...)`         | A label on the bar. Pass a reused `FlixelString` for live numbers. |
| `setScreenSpace(true)`                        | Stay fixed on screen while the camera moves.                      |

A loading bar for a preloading screen:

```java
FlixelBar loading = new FlixelBar(170f, 300f, 300f, 10f).setRange(0f, 1f);
loading.setTrack(Flixel.assets::getProgress);
```

## Building a HUD

A HUD is usually a handful of text objects and bars that must not scroll or zoom with the world. The cleanest setup is
a dedicated HUD camera (see [Cameras](cameras.md)):

```java
@Override
public void create() {
  super.create();
  // ... world objects ...

  FlixelCamera hudCam = new FlixelCamera(Flixel.getDesignWidth(), Flixel.getDesignHeight());
  hudCam.bgColor.a = 0f;
  hudCam.useBgAlphaBlending = true;
  hudCam.defaultDrawTarget = false;
  Flixel.cameras.add(hudCam);

  FlixelCamera[] hudOnly = {hudCam};
  scoreText = new FlixelText(8f, 8f, 0f, "", 16);
  scoreText.cameras = hudOnly;
  healthBar = new FlixelBar(8f, 30f, 100f, 8f).setRange(0f, 100f).setTrack(player::getHealth);
  healthBar.cameras = hudOnly;
  add(scoreText);
  add(healthBar);
}
```

Add HUD objects last so they also draw last. For a label that must stay on screen across every state, such as an FPS
counter, use the game's global overlay (see [The Game Loop](game-loop.md)).

## A simple clickable button

FlixelGDX's core does not include a widget library, but a button is a few lines with what you already know: a sprite or
text, a mouse overlap check, and `justPressed`.

```java
public class TextButton extends FlixelText {

  private final Runnable onClick;

  public TextButton(float x, float y, String label, Runnable onClick) {
    super(x, y, 0f, label, 20);
    this.onClick = onClick;
    setBorderStyle(BorderStyle.OUTLINE, FlixelColor.BLACK);
  }

  @Override
  public void draw(FlixelBatch batch) {
    boolean hover = Flixel.mouse.overlap(this);
    setColor(hover ? FlixelColor.YELLOW : FlixelColor.WHITE);
    if (hover && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
      onClick.run();
    }
    super.draw(batch);
  }
}

// In a menu state:
add(new TextButton(40f, 100f, "Play", () -> Flixel.switchState(PlayState::new)));
```

`FlixelText.update(...)` is final, which is why this example checks the mouse in `draw(...)`; in a sprite-based button,
put the check in `update(...)`. For touch screens, check `Flixel.touches.justTouchedWorld(...)` the same way.

## Common mistakes

- **`"Score: " + score` every frame.** Use a `FlixelString`.
- **Calling `getText()` every frame.** It returns a new `String`. Use `getTextBuffer()`.
- **Registering fonts every frame or every state.** Register once; the registry caches baked sizes.
- **Blurry pixel fonts.** Turn antialiasing off on the text (`setAntialiasing(false)`).
- **The HUD shakes with the screen.** Put it on its own camera.

## Check your understanding

1. Why is `scoreText.setText("Score: " + score)` in `update(...)` a problem, and what replaces it?
2. A dialog box should wrap at 300 pixels. What do you pass as the field width, and what else do you turn on?
3. Your health bar snaps instantly when the player is hit. How do you make it drain smoothly?

(Answers: it creates a new string every frame; a reused `FlixelString` passed to `setText(...)`; `300f`, plus
`setWordWrap(true)`; `setLerp(...)` with a value below 1.)

**Next:** [Audio](audio.md)
