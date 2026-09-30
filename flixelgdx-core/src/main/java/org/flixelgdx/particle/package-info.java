/**
 * Particle effects such as sparks, smoke, fire, rain, magic, and explosions.
 *
 * <p>Think of an emitter like a garden fountain. You decide where the fountain sits, how hard and
 * in which directions it sprays, and how long each drop of water lasts before it evaporates. The
 * fountain handles launching every drop, moving it through the air, and making it disappear. You
 * never have to track individual drops unless you want to.
 *
 * <p>The package has two main classes:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.particle.FlixelEmitter FlixelEmitter} - the fountain. It owns a fixed
 *       number of particles, launches them in bursts or over time, rolls a random look and motion
 *       for each one, and updates and draws them all.</li>
 *   <li>{@link org.flixelgdx.particle.FlixelParticle FlixelParticle} - one drop. It is a regular
 *       {@link org.flixelgdx.FlixelSprite FlixelSprite} with a lifespan, so everything a sprite can
 *       do, a particle can do too. Subclass it when you need custom behavior.</li>
 * </ul>
 *
 * <h2>Quick start</h2>
 * <p>Create an emitter, give it a graphic, describe how particles should behave, add it to your
 * state, and start it:
 *
 * <pre>{@code
 * // In your FlixelState subclass:
 * private FlixelEmitter<FlixelParticle> sparks;
 *
 * @Override
 * public void create() {
 *   sparks = new FlixelEmitter<>(64, FlixelParticle::new); // Room for 64 sparks at once.
 *   sparks.loadGraphic(Flixel.files.internal("images/spark.png"));
 *   sparks.setBlendMode(FlixelBlendMode.ADD);  // Overlapping sparks glow.
 *   sparks.launchAngle.set(-150f, -30f);       // Spray in an upward cone.
 *   sparks.speed.set(100f, 250f);
 *   sparks.acceleration.set(0f, 400f);         // Gravity pulls them back down.
 *   sparks.lifespan.set(0.3f, 0.6f);
 *   sparks.alpha.set(1f, 1f, 0f, 0f);          // Fade out over each spark's life.
 *   add(sparks);
 * }
 *
 * private void onPlayerLanded(float footX, float footY) {
 *   sparks.setPosition(footX, footY);
 *   sparks.start(true, 0f, 20);                // Burst 20 sparks at once.
 * }
 * }</pre>
 *
 * <h2>Bursts and continuous emission</h2>
 * <p>{@link org.flixelgdx.particle.FlixelEmitter#start(boolean, float, int) FlixelEmitter.start(...)}
 * works in two ways. With {@code explode} set to {@code true}, it launches a whole batch of
 * particles at once, which suits explosions, impacts, and pickups. With {@code explode} set to
 * {@code false}, it launches one particle every {@code frequency} seconds, which suits smoke,
 * fire, and rain. Pass a {@code quantity} to stop after that many particles, or zero to keep going
 * until you call {@link org.flixelgdx.particle.FlixelEmitter#stop() stop()}:
 *
 * <pre>{@code
 * smoke.start(false, 0.08f);     // One puff every 0.08 seconds, forever.
 * trail.start(false, 0.02f, 50); // 50 particles, one every 0.02 seconds, then stop.
 * smoke.stop();                  // Stop launching. Living puffs finish their lives.
 * smoke.clear();                 // Remove every living puff right now.
 * }</pre>
 *
 * <p>For full control, launch one particle yourself with
 * {@link org.flixelgdx.particle.FlixelEmitter#emitParticle() emitParticle()}, which returns the
 * particle so you can adjust it further.
 *
 * <h2>Describing particles with ranges</h2>
 * <p>Every property of an emitter is a range object in the same style as HaxeFlixel's
 * {@code FlxEmitter}, so each particle rolls its own values and no two look exactly alike. There
 * are two kinds:
 *
 * <ul>
 *   <li>Bounds, such as {@code lifespan} and {@code launchAngle}, hold a minimum and a maximum.
 *       Each particle rolls one value between them and keeps it. See
 *       {@link org.flixelgdx.math.FlixelBounds FlixelBounds}.</li>
 *   <li>Range bounds, such as {@code alpha}, {@code scale}, {@code color}, and {@code speed}, hold
 *       bounds for where a value starts and bounds for where it ends. Each particle rolls a start
 *       and an end, then slides between them as it ages. See
 *       {@link org.flixelgdx.math.FlixelRangeBounds FlixelRangeBounds}.</li>
 * </ul>
 *
 * <pre>{@code
 * emitter.scale.set(0.5f, 0.5f, 1f, 1f);                  // Start between half and full size...
 * emitter.scale.set(0.5f, 0.5f, 1f, 1f, 2f, 2f, 3f, 3f);  // ...and grow to 2x to 3x.
 * emitter.color.set(FlixelColor.YELLOW, FlixelColor.YELLOW, FlixelColor.RED, FlixelColor.RED);
 * emitter.alpha.active = false;                           // Leave opacity alone entirely.
 * }</pre>
 *
 * <p>If you only set the start of a range bounds (the two-value {@code set} overloads), the end
 * copies the start and each particle keeps its rolled value for its whole life instead of drifting
 * to a second random value.
 *
 * <p>Every range bounds also has an {@code ease} field that bends the slide into a curve, using any
 * function from {@link org.flixelgdx.tween.ease.FlixelEase FlixelEase}:
 *
 * <pre>{@code
 * emitter.alpha.set(1f, 1f, 0f, 0f);
 * emitter.alpha.ease = FlixelEase::quadIn; // Stay bright for a while, then fade quickly.
 * }</pre>
 *
 * <h2>Launch direction</h2>
 * <p>{@link org.flixelgdx.particle.FlixelEmitterMode FlixelEmitterMode} decides how particles pick
 * their starting velocity. In {@link org.flixelgdx.particle.FlixelEmitterMode#CIRCLE CIRCLE} mode
 * (the default), each particle picks a direction from {@code launchAngle} and a speed from
 * {@code speed}, which suits anything that sprays outward. In
 * {@link org.flixelgdx.particle.FlixelEmitterMode#SQUARE SQUARE} mode, each particle picks its X
 * and Y velocity separately from {@code velocity}, which suits drifting effects:
 *
 * <pre>{@code
 * // Snow: drift sideways a little while falling at different speeds.
 * snow.launchMode = FlixelEmitterMode.SQUARE;
 * snow.velocity.set(-15f, 30f, 15f, 60f);
 * }</pre>
 *
 * <h2>Spawn shapes</h2>
 * <p>The emitter's position and size describe the area particles appear in, and
 * {@link org.flixelgdx.particle.FlixelEmitterShape FlixelEmitterShape} decides its shape. A size of
 * zero spawns everything from a single point.
 *
 * <ul>
 *   <li>{@link org.flixelgdx.particle.FlixelEmitterShape#RECTANGLE RECTANGLE} - anywhere inside
 *       the box. Stretch it across the top of the screen for rain.</li>
 *   <li>{@link org.flixelgdx.particle.FlixelEmitterShape#CIRCLE CIRCLE} - anywhere inside the
 *       circle that fits the box, spread evenly over its area.</li>
 *   <li>{@link org.flixelgdx.particle.FlixelEmitterShape#RING RING} - in a band just inside the
 *       circle's edge, as wide as {@code ringThickness}.</li>
 * </ul>
 *
 * <pre>{@code
 * // A glowing halo around the player.
 * halo.setSize(64f, 64f);
 * halo.shape = FlixelEmitterShape.RING;
 * halo.ringThickness = 4f;
 * halo.focusOn(player); // Center the ring on the player.
 * }</pre>
 *
 * <h2>Graphics and frames</h2>
 * <p>{@link org.flixelgdx.particle.FlixelEmitter#loadGraphic(org.flixelgdx.file.FlixelFile, int, int) loadGraphic(...)}
 * cuts an image into a grid of frames that every particle shares. Each particle picks a random
 * frame, which is an easy way to get variety from one sprite sheet. Set
 * {@code animateFrames} to make each particle step through every frame over its life instead, like
 * a short flipbook. For quick tests,
 * {@link org.flixelgdx.particle.FlixelEmitter#makeGraphic(int, int, org.flixelgdx.util.FlixelColor) makeGraphic(...)}
 * gives every particle a plain rectangle.
 *
 * <pre>{@code
 * // A 4-frame puff that plays once over each particle's life.
 * smoke.loadGraphic(Flixel.files.internal("images/smoke.png"), 16, 16);
 * smoke.animateFrames = true;
 * }</pre>
 *
 * <p>If the emitter has no graphic, particles keep whatever graphic they load themselves, so a
 * particle subclass can bring its own art or animations.
 *
 * <h2>Custom particles</h2>
 * <p>Because a particle is a sprite, custom behavior is just a subclass. Override
 * {@link org.flixelgdx.particle.FlixelParticle#onEmit() onEmit()} to set up each launch,
 * {@link org.flixelgdx.particle.FlixelParticle#update(float) update(...)} to add motion, and
 * {@link org.flixelgdx.particle.FlixelParticle#onDeath() onDeath()} to react when it ends. Pass the
 * constructor, or a lambda that calls it, to the emitter:
 *
 * <pre>{@code
 * public class Firework extends FlixelParticle {
 *
 *   private final FlixelEmitter<FlixelParticle> sparks;
 *
 *   public Firework(FlixelEmitter<FlixelParticle> sparks) {
 *     this.sparks = sparks;
 *   }
 *
 *   @Override
 *   public void onDeath() {
 *     // Burst into sparks where this firework ended.
 *     sparks.setPosition(getMidpointX(), getMidpointY());
 *     sparks.start(true, 0f, 12);
 *   }
 * }
 *
 * FlixelEmitter<Firework> fireworks = new FlixelEmitter<>(8, () -> new Firework(sparks));
 * }</pre>
 *
 * <p>If your subclass has its own fields, clear them in an override of
 * {@link org.flixelgdx.particle.FlixelParticle#reset(float, float) reset(...)} and call
 * {@code super.reset(x, y)}, so nothing leaks from one launch into the next.
 *
 * <h2>Memory and performance</h2>
 * <p>An emitter creates every particle once, in its constructor, and reuses them forever. Launching
 * a particle never allocates, and a dead particle is not thrown away; it simply waits to be
 * launched again. Living particles are packed at the front of one array, so updating and drawing
 * them is a single tight loop.
 *
 * <p>This means the capacity you pass to the constructor matters. Pick a number that covers the
 * busiest moment of the effect without going far over, since every slot is a full sprite in
 * memory. When all slots are in use, the emitter recycles its oldest particle by default; set
 * {@code recycleWhenFull} to {@code false} to skip new launches instead.
 *
 * <p>Particles that share the emitter's graphic draw from the same texture, so the batch merges
 * them into a single GPU submission. The emitter also applies its blend mode once around all of
 * its particles, rather than once per particle. Avoid giving individual particles their own
 * shaders or blend modes, since each change forces the batch to flush.
 *
 * <h2>Reproducible effects</h2>
 * <p>Every roll an emitter makes comes from its own
 * {@link org.flixelgdx.math.FlixelRandom FlixelRandom}. Seed it to make an effect play out exactly
 * the same way every time, which helps with replays, tests, and debugging:
 *
 * <pre>{@code
 * explosion.random.setSeed(42L);
 * explosion.start(true); // Identical every run.
 * }</pre>
 *
 * @see org.flixelgdx.particle.FlixelEmitter
 * @see org.flixelgdx.particle.FlixelParticle
 * @see org.flixelgdx.math.FlixelRangeBounds
 */
package org.flixelgdx.particle;
