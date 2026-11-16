# Particles: Sparks, Smoke, Rain, and Explosions

Particle effects are many tiny, short-lived sprites launched together: sparks from a sword, smoke from a chimney,
rain, confetti, magic. This guide covers emitters, bursts and streams, describing particles with ranges, launch
direction, spawn shapes, graphics, custom particles, and how emitters stay fast.

## Table of contents

1. [The fountain analogy](#the-fountain-analogy)
2. [Your first emitter](#your-first-emitter)
3. [Bursts and streams](#bursts-and-streams)
4. [Describing particles with ranges](#describing-particles-with-ranges)
5. [Every emitter property](#every-emitter-property)
6. [Launch direction: circle and square modes](#launch-direction-circle-and-square-modes)
7. [Spawn shapes](#spawn-shapes)
8. [Moving, rotating, and following](#moving-rotating-and-following)
9. [Graphics and frames](#graphics-and-frames)
10. [Custom particles](#custom-particles)
11. [Recipes](#recipes)
12. [Memory and performance](#memory-and-performance)
13. [Reproducible effects](#reproducible-effects)
14. [Check your understanding](#check-your-understanding)

## The fountain analogy

Think of an emitter as a garden fountain. You decide where the fountain sits, how hard and in which directions it
sprays, and how long each drop lasts before it evaporates. The fountain launches every drop, moves it through the air,
and makes it disappear. You never track individual drops unless you want to.

| Class               | Role                                                                                     |
|---------------------|------------------------------------------------------------------------------------------|
| `FlixelEmitter<P>`  | The fountain. Owns a fixed number of particles, launches them, and updates and draws them. |
| `FlixelParticle`    | One drop. A regular `FlixelSprite` with an age and a lifespan.                           |

## Your first emitter

```java
private FlixelEmitter<FlixelParticle> sparks;

@Override
public void create() {
  super.create();
  sparks = new FlixelEmitter<>(64, FlixelParticle::new); // Room for 64 sparks at once.
  sparks.loadGraphic(Flixel.files.internal("images/spark.png"));
  sparks.setBlendMode(FlixelBlendMode.ADD);  // Overlapping sparks glow.
  sparks.launchAngle.set(-150f, -30f);       // Spray in an upward cone.
  sparks.speed.set(100f, 250f);
  sparks.acceleration.set(0f, 400f);         // Gravity pulls them back down.
  sparks.lifespan.set(0.3f, 0.6f);
  sparks.alpha.set(1f, 1f, 0f, 0f);          // Fade out over each spark's life.
  add(sparks);
}

private void onPlayerLanded(float footX, float footY) {
  sparks.setPosition(footX, footY);
  sparks.start(true, 0f, 20);                // Burst 20 sparks at once.
}
```

Angles are in degrees, measured clockwise from pointing right, since Y grows downward: `-90` points straight up, `0`
right, `90` straight down.

## Bursts and streams

`start(explode, frequency, quantity)` works in two ways:

- **Burst** (`explode = true`): launch a whole batch at once. Explosions, impacts, pickups.
- **Stream** (`explode = false`): launch one particle every `frequency` seconds. Smoke, fire, rain, trails.

`quantity` limits how many particles to launch; `0` means "keep going until `stop()`."

```java
explosion.start(true, 0f, 40);   // 40 at once.
smoke.start(false, 0.08f);       // One puff every 0.08 seconds, forever.
trail.start(false, 0.02f, 50);   // 50 particles, one every 0.02 seconds, then stop.

smoke.stop();                    // Stop launching. Living puffs finish their lives.
smoke.clear();                   // Remove every living puff right now.
boolean running = smoke.emitting;
```

For full control, `emitParticle()` launches one particle right now and returns it, so you can adjust it further.

## Describing particles with ranges

Every property of an emitter is a range, so each particle rolls its own values and no two look exactly alike. There
are two kinds:

- **Bounds** (`FlixelBounds`, such as `lifespan` and `launchAngle`) hold a minimum and a maximum. Each particle rolls one
  value between them and keeps it for its whole life.
- **Range bounds** (`FlixelRangeBounds`, such as `alpha`, `scale`, `color`, and `speed`) hold bounds for where a value
  *starts* and bounds for where it *ends*. Each particle rolls a start and an end, then slides between them as it ages.

```java
emitter.lifespan.set(1f, 2f);                           // Each lives 1 to 2 seconds.
emitter.alpha.set(1f, 1f, 0f, 0f);                      // Start fully visible, end invisible.
emitter.scale.set(0.5f, 0.5f, 1f, 1f);                  // Start between half and full size...
emitter.scale.set(0.5f, 0.5f, 1f, 1f, 2f, 2f, 3f, 3f);  // ...and grow to 2x to 3x.
emitter.color.set(FlixelColor.YELLOW, FlixelColor.YELLOW, FlixelColor.RED, FlixelColor.RED); // Yellow to red.
```

If you only set the **start** of a range bounds (the shorter `set(...)` forms), the end copies the start and each
particle keeps its rolled value for its whole life instead of sliding to a second value. Set `active = false` on a
range to leave that property alone entirely:

```java
emitter.alpha.active = false; // Do not touch opacity.
```

Every range bounds also has an `ease` that bends the slide into a curve, using any function from `FlixelEase`:

```java
emitter.alpha.set(1f, 1f, 0f, 0f);
emitter.alpha.ease = FlixelEase::quadIn; // Stay bright for a while, then fade quickly.
```

## Every emitter property

| Field              | Kind                    | Meaning                                                            |
|--------------------|-------------------------|--------------------------------------------------------------------|
| `lifespan`         | Bounds                  | Seconds each particle lives. Default 3.                            |
| `launchAngle`      | Bounds                  | Launch direction in degrees, in circle mode.                       |
| `speed`            | Range bounds            | Launch speed in pixels per second, in circle mode.                 |
| `velocity`         | Point range bounds      | X and Y launch velocity, in square mode.                           |
| `acceleration`     | Point bounds            | Constant acceleration, such as gravity.                            |
| `drag`             | Point bounds            | Slowing on each axis.                                              |
| `angle`            | Range bounds            | How each particle is rotated when drawn.                           |
| `angularVelocity`  | Range bounds            | Spin speed in degrees per second.                                  |
| `scale`            | Point range bounds      | Size over life.                                                    |
| `alpha`            | Range bounds            | Opacity over life.                                                 |
| `color`            | Color range bounds      | Tint over life.                                                    |
| `launchMode`       | `FlixelEmitterMode`     | `CIRCLE` or `SQUARE`.                                              |
| `shape`            | `FlixelEmitterShape`    | `RECTANGLE`, `CIRCLE`, or `RING` spawn area.                       |
| `ringThickness`    | float                   | Band width for `RING`.                                             |
| `frequency`        | float                   | Seconds between particles in a stream.                             |
| `recycleWhenFull`  | boolean                 | Reuse the oldest particle when all are alive (default `true`).     |
| `animateFrames`    | boolean                 | Step through every frame over each particle's life.                |
| `keepScaleRatio`   | boolean                 | Keep X and Y scale equal when rolling scale.                       |
| `ignoreAngularVelocity` | boolean            | Skip spin entirely.                                                |
| `random`           | `FlixelRandom`          | The emitter's own random generator.                                |

## Launch direction: circle and square modes

`FlixelEmitterMode` decides how particles pick their starting velocity:

- `CIRCLE` (the default): each particle picks a direction from `launchAngle` and a speed from `speed`. Anything that
  sprays outward: explosions, fountains, sparks.
- `SQUARE`: each particle picks its X and Y velocity separately from `velocity`. Drifting effects: snow, bubbles,
  falling leaves.

```java
// Snow: drift sideways a little while falling at different speeds.
snow.launchMode = FlixelEmitterMode.SQUARE;
snow.velocity.set(-15f, 30f, 15f, 60f); // X between -15 and 15, Y between 30 and 60.
```

## Spawn shapes

The emitter's position and size describe the area particles appear in; `shape` decides its outline. A size of zero
spawns everything from one point.

| Shape       | Particles appear...                                                  |
|-------------|----------------------------------------------------------------------|
| `RECTANGLE` | Anywhere inside the box. Stretch it across the top of the screen for rain. |
| `CIRCLE`    | Anywhere inside the circle that fits the box, spread evenly.         |
| `RING`      | In a band just inside the circle's edge, `ringThickness` wide.       |

```java
// A glowing halo around the player.
halo.setSize(64f, 64f);
halo.shape = FlixelEmitterShape.RING;
halo.ringThickness = 4f;
halo.focusOn(player); // Center the area on the player.
```

## Moving, rotating, and following

An emitter has a position, so anything that works with positions works with it: move it every frame to leave a trail,
tween it, or let a camera follow it. Its own angle rotates the direction particles launch in, so a tween can sweep a
spray back and forth like a lawn sprinkler:

```java
nozzle.launchAngle.set(-100f, -80f); // A narrow cone pointing up.
FlixelTween.angle(nozzle, -45f, 45f, new FlixelTweenSettings(FlixelTweenType.PINGPONG).setDuration(2f));
```

The emitter's angle is separate from the `angle` range, which rotates each particle's picture. Moving or rotating the
emitter only affects particles launched afterward; particles already in the air keep going.

## Graphics and frames

```java
sparks.loadGraphic(Flixel.files.internal("images/spark.png"));          // One image for every particle.
debris.loadGraphic(Flixel.files.internal("images/debris.png"), 8, 8);   // A sheet of 8 by 8 frames.
dust.makeGraphic(2, 2, FlixelColor.WHITE);                              // A plain square, great for testing.
```

With a frame sheet, each particle picks a random frame, an easy way to get variety from one image. Set `animateFrames`
to make each particle play through every frame over its life instead, like a tiny flipbook:

```java
smoke.loadGraphic(Flixel.files.internal("images/smoke.png"), 16, 16); // A 4-frame puff.
smoke.animateFrames = true;
```

If the emitter has no graphic, particles keep whatever graphic they load themselves, so a particle subclass can bring
its own art or animations. Blend mode, shader, and antialiasing are set once on the emitter
(`setBlendMode(...)`, `setShader(...)`, `setAntialiasing(...)`) and apply to all of its particles.

## Custom particles

A particle is a sprite, so custom behavior is just a subclass. Override `onEmit()` to set up each launch,
`update(...)` to add motion, and `onDeath()` to react when it ends. Pass the constructor (or a lambda that calls it) to
the emitter:

```java
public class Firework extends FlixelParticle {

  private final FlixelEmitter<FlixelParticle> sparks;

  public Firework(FlixelEmitter<FlixelParticle> sparks) {
    this.sparks = sparks;
  }

  @Override
  public void onDeath() {
    // Burst into sparks where this firework ended.
    sparks.setPosition(getMidpointX(), getMidpointY());
    sparks.start(true, 0f, 12);
  }
}

FlixelEmitter<Firework> fireworks = new FlixelEmitter<>(8, () -> new Firework(sparks));
```

A particle knows its `age`, its `lifespan`, and its `emitter`. If your subclass has its own fields, clear them in an
override of `reset(x, y)` and call `super.reset(x, y)`, so nothing leaks from one launch into the next.

## Recipes

**Rain across the whole screen:**

```java
FlixelEmitter<FlixelParticle> rain = new FlixelEmitter<>(0f, -10f, 200, FlixelParticle::new);
rain.setSize(Flixel.getDesignWidth(), 0f);  // A line along the top.
rain.makeGraphic(1, 6, FlixelColor.CYAN);
rain.launchMode = FlixelEmitterMode.SQUARE;
rain.velocity.set(-20f, 400f, -10f, 500f);
rain.lifespan.set(1f);
rain.alpha.set(0.6f);
rain.setScrollFactor(0f, 0f);               // Stay on screen while the camera moves.
rain.start(false, 0.01f);
add(rain);
```

**Dust puff when landing:**

```java
dust.makeGraphic(3, 3, FlixelColor.GRAY);
dust.launchAngle.set(-170f, -10f);
dust.speed.set(20f, 60f, 0f, 0f);       // Start moving, then slow to a stop.
dust.lifespan.set(0.3f, 0.5f);
dust.alpha.set(0.8f, 0.8f, 0f, 0f);

// When the player lands:
if (player.justTouched(FlixelDirectionFlags.FLOOR)) {
  dust.setPosition(player.getMidpointX(), player.getY() + player.getHeight());
  dust.start(true, 0f, 6);
}
```

## Memory and performance

An emitter creates every particle once, in its constructor, and reuses them forever. Launching a particle never
creates an object, and a dead particle is not thrown away; it waits to be launched again. Living particles are packed
at the front of one array, so updating and drawing them is a single tight loop.

So the capacity you pass to the constructor matters. Pick a number that covers the busiest moment of the effect
without going far over, since every slot is a full sprite in memory. When all slots are in use, the emitter recycles
its oldest particle (`recycleWhenFull = true`, the default); set it to `false` to skip new launches instead.

Particles that share the emitter's graphic draw from the same texture, so the GPU draws them all in one batch. Avoid
giving individual particles their own shaders or blend modes, since every change forces a new batch.

## Reproducible effects

Every roll an emitter makes comes from its own `random`. Seed it to make an effect play out exactly the same way every
time, which helps with replays, tests, and debugging:

```java
explosion.random.setSeed(42L);
explosion.start(true); // Identical every run.
```

## Check your understanding

1. You want each spark to fade out over its life. Which property, and which `set(...)` form?
2. A smoke stream should keep going until the fire is put out. What do you pass for `quantity`, and how do you stop it?
3. Your explosion sometimes looks "thin" when many happen at once. What is probably happening?

(Answers: `alpha`, with the four-value form such as `set(1f, 1f, 0f, 0f)`; `0`, then `stop()`; the emitter's capacity
is too small, so the oldest particles are being recycled early; raise the capacity.)

**Next:** [Files and Assets](files-and-assets.md)
