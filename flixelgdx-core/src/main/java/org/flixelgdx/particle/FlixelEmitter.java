/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.particle;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelState;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.math.FlixelRandom;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Launches, updates, and draws a fixed number of lightweight particles for effects such as sparks,
 * smoke, fire, rain, and explosions.
 *
 * <p>Think of an emitter like a fountain. You decide where it sits, how hard and in which directions
 * it sprays, and how long each drop lasts before it disappears. The emitter takes care of launching
 * drops, moving them, and cleaning them up, and you never handle individual drops unless you want
 * to.
 *
 * <p>Every particle is created once, in the constructor, and reused forever after, so a running
 * emitter never allocates. Living particles are packed at the front of one array: launching takes
 * the next unused slot, and when a particle dies the last living particle is moved into its slot.
 * All particles share the emitter's texture and are drawn together in a single batch submission.
 *
 * <p>Each property is configured with a range object in the same style as HaxeFlixel's
 * {@code FlxEmitter}. A {@link FlixelBounds} picks one random value per particle (like
 * {@link #lifespan}), while a {@link FlixelRangeBounds} picks a start and an end value and slides
 * between them over the particle's life (like {@link #alpha} for fading out). Each range can also
 * follow an easing curve through its {@code ease} field.
 *
 * <p>An emitter is a {@link FlixelBasic}, so add it to a {@link FlixelState} or group and it updates
 * and draws automatically. Its position and size describe the spawn area: particles appear at a
 * random point inside the rectangle from ({@link #getX()}, {@link #getY()}) with the emitter's width
 * and height. Leave the size at zero to spawn every particle from a single point.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Landing sparks: a burst on demand.
 * FlixelEmitter<FlixelParticle> sparks = new FlixelEmitter<>(64, FlixelParticle::new);
 * sparks.loadGraphic(Flixel.files.internal("images/spark.png"));
 * sparks.setBlendMode(FlixelBlendMode.ADD);
 * sparks.launchAngle.set(-150f, -30f); // An upward cone.
 * sparks.speed.set(100f, 250f);
 * sparks.acceleration.set(0f, 400f);   // Gravity.
 * sparks.lifespan.set(0.3f, 0.6f);
 * sparks.alpha.set(1f, 1f, 0f, 0f);    // Fade out.
 * add(sparks);
 *
 * // Later, when the player lands:
 * sparks.setPosition(footX, footY);
 * sparks.start(true, 0f, 20);          // Explode 20 particles at once.
 *
 * // Campfire smoke: always running.
 * FlixelEmitter<FlixelParticle> smoke = new FlixelEmitter<>(320f, 400f, 128, FlixelParticle::new);
 * smoke.loadGraphic(Flixel.files.internal("images/smoke.png"));
 * smoke.setSize(24f, 4f);
 * smoke.launchMode = FlixelEmitterMode.SQUARE;
 * smoke.velocity.set(-10f, -60f, 10f, -30f);
 * smoke.scale.set(0.5f, 0.5f, 0.5f, 0.5f, 1.5f, 1.5f, 2f, 2f); // Grows as it rises.
 * smoke.scale.ease = FlixelEase::quadOut;
 * smoke.lifespan.set(1.5f, 2.5f);
 * smoke.start(false, 0.08f);           // One particle every 0.08 seconds, forever.
 * add(smoke);
 * }</pre>
 *
 * <p>Particles are purely visual and do not collide with anything. For gameplay objects that need
 * collision, use sprites in a recycled group instead.
 *
 * @param <P> The particle type this emitter launches. Use {@link FlixelParticle} unless you need
 *     custom per-particle behavior.
 */
public class FlixelEmitter<P extends FlixelParticle> extends FlixelBasic {

  private static final int FLOATS_PER_QUAD = 20;

  /**
   * The range of angles, in degrees, particles launch at in {@link FlixelEmitterMode#CIRCLE} mode.
   * {@code 0} points right and {@code 90} points down. Defaults to every direction.
   */
  public final FlixelBounds launchAngle = new FlixelBounds(-180f, 180f);

  /** How fast particles move in {@link FlixelEmitterMode#CIRCLE} mode, in pixels per second. */
  public final FlixelRangeBounds speed = new FlixelRangeBounds(0f, 100f);

  /** The X and Y velocity of particles in {@link FlixelEmitterMode#SQUARE} mode, in pixels per second. */
  public final FlixelPointRangeBounds velocity = new FlixelPointRangeBounds(-100f, -100f, 100f, 100f);

  /** How fast particles spin, in degrees per second. Ignored while {@link #ignoreAngularVelocity} is set. */
  public final FlixelRangeBounds angularVelocity = new FlixelRangeBounds(0f);

  /**
   * The angle particles start at, in degrees. While {@link #ignoreAngularVelocity} is set, particles
   * also turn from the start angle to the end angle over their life.
   */
  public final FlixelRangeBounds angle = new FlixelRangeBounds(0f);

  /** Constant acceleration applied to each particle, in pixels per second squared. Use Y for gravity. */
  public final FlixelPointBounds acceleration = new FlixelPointBounds(0f, 0f);

  /** Slowdown applied to each particle on any axis with no acceleration, in pixels per second squared. */
  public final FlixelPointBounds drag = new FlixelPointBounds(0f, 0f);

  /** How many seconds each particle lives. Zero or less means particles live until killed. */
  public final FlixelBounds lifespan = new FlixelBounds(3f);

  /** The draw scale of particles, where {@code 1} is the frame's natural size. */
  public final FlixelPointRangeBounds scale = new FlixelPointRangeBounds(1f, 1f);

  /** The opacity of particles, from {@code 0} (invisible) to {@code 1} (fully opaque). */
  public final FlixelRangeBounds alpha = new FlixelRangeBounds(1f);

  /** The tint of particles. Only red, green, and blue are used; see {@link #alpha} for opacity. */
  public final FlixelColorRangeBounds color = new FlixelColorRangeBounds(FlixelColor.WHITE);

  /**
   * The random number generator every roll uses. Call {@link FlixelRandom#setSeed(long)} on it to
   * make an effect play out identically every time, which helps with replays and tests.
   */
  public final FlixelRandom random = new FlixelRandom();

  /** How particles pick their starting velocity. Defaults to {@link FlixelEmitterMode#CIRCLE}. */
  @NotNull
  public FlixelEmitterMode launchMode = FlixelEmitterMode.CIRCLE;

  /**
   * Seconds between launches while emitting continuously. Zero or less launches one particle every
   * frame.
   */
  public float frequency = 0.1f;

  private final P[] particles;
  private final float[] vertices;

  @Nullable
  private FlixelGraphic graphic;

  @Nullable
  private FlixelFrame[] frames;

  @NotNull
  private FlixelBlendMode blendMode = FlixelBlendMode.NORMAL;

  private float x;
  private float y;
  private float width;
  private float height;
  private float scrollX = 1f;
  private float scrollY = 1f;
  private float timer;
  private int count;
  private int quantity;
  private int emitted;

  /** Whether the emitter is currently launching particles on its own. */
  public boolean emitting;

  /**
   * Whether particles turn from the start to the end of {@link #angle} over their life instead of
   * spinning with {@link #angularVelocity}.
   */
  public boolean ignoreAngularVelocity;

  /** Whether each particle's Y scale copies its X scale, so particles never stretch. */
  public boolean keepScaleRatio;

  /**
   * Whether particles step through every loaded frame over their life, like a short animation.
   * When {@code false}, each particle picks one random frame and keeps it.
   */
  public boolean animateFrames;

  /**
   * Whether launching a particle while every slot is in use recycles the oldest living particle.
   * When {@code false}, the launch is skipped instead.
   */
  public boolean recycleWhenFull = true;

  private boolean antialiasing;

  /**
   * Creates an emitter at the world origin that owns a fixed number of particles.
   *
   * @param capacity The most particles this emitter can have alive at once. Every one of them is
   *     created now, so pick a number that covers your busiest moment without going far over.
   * @param factory Creates one particle. Called {@code capacity} times, only in this constructor.
   *     Usually a constructor reference such as {@code FlixelParticle::new}.
   */
  public FlixelEmitter(int capacity, @NotNull Supplier<P> factory) {
    this(0f, 0f, capacity, factory);
  }

  /**
   * Creates an emitter at a position that owns a fixed number of particles.
   *
   * @param x The X position of the spawn area in world space.
   * @param y The Y position of the spawn area in world space.
   * @param capacity The most particles this emitter can have alive at once. Every one of them is
   *     created now, so pick a number that covers your busiest moment without going far over.
   * @param factory Creates one particle. Called {@code capacity} times, only in this constructor.
   *     Usually a constructor reference such as {@code FlixelParticle::new}.
   * @throws IllegalArgumentException If {@code capacity} is less than 1.
   */
  @SuppressWarnings("unchecked")
  public FlixelEmitter(float x, float y, int capacity, @NotNull Supplier<P> factory) {
    if (capacity < 1) {
      throw new IllegalArgumentException("Emitter capacity must be at least 1, got " + capacity + ".");
    }
    this.x = x;
    this.y = y;
    // Erasure makes P[] a FlixelParticle[] at runtime, so this cast is safe.
    this.particles = (P[]) new FlixelParticle[capacity];
    for (int i = 0; i < capacity; i++) {
      P p = factory.get();
      p.emitter = this;
      particles[i] = p;
    }
    this.vertices = new float[capacity * FLOATS_PER_QUAD];
  }

  /**
   * Starts exploding every particle at once.
   *
   * @return {@code this} emitter for chaining.
   * @see #start(boolean, float, int)
   */
  public FlixelEmitter<P> start() {
    return start(true, 0.1f, 0);
  }

  /**
   * Starts emitting, either all at once or continuously forever.
   *
   * @param explode {@code true} to launch every particle at once, {@code false} to launch one every
   *     {@link #frequency} seconds.
   * @return {@code this} emitter for chaining.
   * @see #start(boolean, float, int)
   */
  public FlixelEmitter<P> start(boolean explode) {
    return start(explode, 0.1f, 0);
  }

  /**
   * Starts emitting with a launch interval, either all at once or continuously forever.
   *
   * @param explode {@code true} to launch every particle at once, {@code false} to launch one every
   *     {@code frequency} seconds.
   * @param frequency Seconds between launches when not exploding.
   * @return {@code this} emitter for chaining.
   * @see #start(boolean, float, int)
   */
  public FlixelEmitter<P> start(boolean explode, float frequency) {
    return start(explode, frequency, 0);
  }

  /**
   * Starts emitting particles.
   *
   * <p>When {@code explode} is {@code true}, {@code quantity} particles launch immediately (every
   * slot when {@code quantity} is zero) and the emitter stops. Otherwise one particle launches every
   * {@code frequency} seconds until {@code quantity} have launched, or forever when {@code quantity}
   * is zero. Calling this again restarts the count.
   *
   * @param explode {@code true} to launch everything at once, {@code false} to launch over time.
   * @param frequency Seconds between launches when not exploding. Zero or less launches one per
   *     frame.
   * @param quantity How many particles to launch in total, or zero for no limit.
   * @return {@code this} emitter for chaining.
   */
  public FlixelEmitter<P> start(boolean explode, float frequency, int quantity) {
    exists = true;
    active = true;
    visible = true;
    this.frequency = frequency;
    this.quantity = quantity;
    emitted = 0;
    timer = 0f;
    if (explode) {
      emitting = false;
      int n = quantity > 0 ? quantity : particles.length;
      for (int i = 0; i < n; i++) {
        emitParticle();
      }
    } else {
      emitting = true;
    }
    return this;
  }

  /**
   * Stops launching new particles. Particles already alive keep going until they die.
   */
  public void stop() {
    emitting = false;
  }

  /**
   * Launches a single particle right now, using every range on this emitter.
   *
   * <p>If every slot is in use, the oldest particle is recycled when {@link #recycleWhenFull} is set;
   * otherwise nothing launches. A particle recycled this way is cut short rather than dying, so its
   * {@link FlixelParticle#onDeath()} does not run.
   *
   * @return The launched particle, or {@code null} if the emitter was full and recycling is off.
   */
  @Nullable
  public P emitParticle() {
    P p;
    if (count < particles.length) {
      p = particles[count++];
    } else if (recycleWhenFull) {
      p = particles[oldestIndex()];
    } else {
      return null;
    }

    p.reset();
    p.alive = true;
    p.lifespan = roll(lifespan.min, lifespan.max);
    p.x = x + roll(0f, width);
    p.y = y + roll(0f, height);
    boolean timed = p.lifespan > 0f;

    rollVelocity(p, timed);
    p.accelerationX = roll(acceleration.minX, acceleration.maxX);
    p.accelerationY = roll(acceleration.minY, acceleration.maxY);
    p.dragX = roll(drag.minX, drag.maxX);
    p.dragY = roll(drag.minY, drag.maxY);

    if (angle.active) {
      p.angleStart = roll(angle.start.min, angle.start.max);
      p.angleEnd = angle.changes() ? roll(angle.end.min, angle.end.max) : p.angleStart;
      p.angle = p.angleStart;
      p.angleRangeActive = ignoreAngularVelocity && timed && p.angleStart != p.angleEnd;
    }
    if (angularVelocity.active && !ignoreAngularVelocity) {
      p.angularVelocityStart = roll(angularVelocity.start.min, angularVelocity.start.max);
      p.angularVelocityEnd = angularVelocity.changes()
          ? roll(angularVelocity.end.min, angularVelocity.end.max)
          : p.angularVelocityStart;
      p.angularVelocity = p.angularVelocityStart;
      p.angularVelocityRangeActive = timed && p.angularVelocityStart != p.angularVelocityEnd;
    }

    if (scale.active) {
      p.scaleStartX = roll(scale.start.minX, scale.start.maxX);
      p.scaleStartY = keepScaleRatio ? p.scaleStartX : roll(scale.start.minY, scale.start.maxY);
      if (scale.changes()) {
        p.scaleEndX = roll(scale.end.minX, scale.end.maxX);
        p.scaleEndY = keepScaleRatio ? p.scaleEndX : roll(scale.end.minY, scale.end.maxY);
      } else {
        p.scaleEndX = p.scaleStartX;
        p.scaleEndY = p.scaleStartY;
      }
      p.scaleX = p.scaleStartX;
      p.scaleY = p.scaleStartY;
      p.scaleRangeActive = timed && (p.scaleStartX != p.scaleEndX || p.scaleStartY != p.scaleEndY);
    }

    if (alpha.active) {
      p.alphaStart = roll(alpha.start.min, alpha.start.max);
      p.alphaEnd = alpha.changes() ? roll(alpha.end.min, alpha.end.max) : p.alphaStart;
      p.alpha = p.alphaStart;
      p.alphaRangeActive = timed && p.alphaStart != p.alphaEnd;
    }

    if (color.active) {
      FlixelColor sMin = color.start.min;
      FlixelColor sMax = color.start.max;
      float t = random.nextFloat();
      p.redStart = FlixelMath.lerp(sMin.r, sMax.r, t);
      p.greenStart = FlixelMath.lerp(sMin.g, sMax.g, t);
      p.blueStart = FlixelMath.lerp(sMin.b, sMax.b, t);
      if (color.changes()) {
        FlixelColor eMin = color.end.min;
        FlixelColor eMax = color.end.max;
        t = random.nextFloat();
        p.redEnd = FlixelMath.lerp(eMin.r, eMax.r, t);
        p.greenEnd = FlixelMath.lerp(eMin.g, eMax.g, t);
        p.blueEnd = FlixelMath.lerp(eMin.b, eMax.b, t);
      } else {
        p.redEnd = p.redStart;
        p.greenEnd = p.greenStart;
        p.blueEnd = p.blueStart;
      }
      p.red = p.redStart;
      p.green = p.greenStart;
      p.blue = p.blueStart;
      p.colorRangeActive = timed
          && (p.redStart != p.redEnd || p.greenStart != p.greenEnd || p.blueStart != p.blueEnd);
    }

    int frameCount = getFrameCount();
    if (frameCount > 1 && !animateFrames) {
      p.frame = random.nextInt(frameCount);
    }

    p.onEmit();
    return p;
  }

  /**
   * Kills every living particle immediately, without running {@link FlixelParticle#onDeath()}.
   *
   * <p>This does not stop emission; call {@link #stop()} as well to clear the screen for good.
   */
  public void clear() {
    for (int i = 0; i < count; i++) {
      particles[i].alive = false;
    }
    count = 0;
  }

  /**
   * Moves the spawn area so it is centered on an object.
   *
   * @param object The object to center on.
   */
  public void focusOn(@NotNull FlixelObject object) {
    setPosition(object.getMidpointX() - width * 0.5f, object.getMidpointY() - height * 0.5f);
  }

  /**
   * Uses a whole image file as the single frame for every particle.
   *
   * @param file A handle to the image to load.
   * @return {@code this} emitter for chaining.
   */
  public FlixelEmitter<P> loadGraphic(@NotNull FlixelFile file) {
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(file.getPath()).retain().get();
    FlixelTexture t = g.getTexture();
    return loadGraphic(g, t.getWidth(), t.getHeight());
  }

  /**
   * Cuts an image file into a grid of equally sized frames for particles to use.
   *
   * <p>Each particle picks one random frame, or steps through all of them over its life when
   * {@link #animateFrames} is set.
   *
   * @param file A handle to the image to load.
   * @param frameWidth The width of each frame, in pixels.
   * @param frameHeight The height of each frame, in pixels.
   * @return {@code this} emitter for chaining.
   */
  public FlixelEmitter<P> loadGraphic(@NotNull FlixelFile file, int frameWidth, int frameHeight) {
    FlixelGraphic g = Flixel.assets.<FlixelGraphic>get(file.getPath()).retain().get();
    return loadGraphic(g, frameWidth, frameHeight);
  }

  /**
   * Cuts an already retained graphic into a grid of equally sized frames for particles to use.
   *
   * <p>The emitter takes over the caller's retain and releases it when the graphic is replaced or
   * the emitter is destroyed.
   *
   * @param g The graphic to use, already retained by the caller.
   * @param frameWidth The width of each frame, in pixels.
   * @param frameHeight The height of each frame, in pixels.
   * @return {@code this} emitter for chaining.
   */
  public FlixelEmitter<P> loadGraphic(@NotNull FlixelGraphic g, int frameWidth, int frameHeight) {
    if (graphic != null) {
      graphic.release();
    }
    graphic = g;
    FlixelTexture texture = g.getTexture();
    frames = splitFrames(texture, frameWidth, frameHeight);
    texture.setSmooth(antialiasing);
    return this;
  }

  /**
   * Gives every particle a solid-colored rectangle as its graphic, which is handy for quick
   * prototypes and simple pixel effects.
   *
   * <p>Tint particles with {@link #color}; a white rectangle is the most flexible choice.
   *
   * @param width The rectangle width, in pixels.
   * @param height The rectangle height, in pixels.
   * @param fill The rectangle color.
   * @return {@code this} emitter for chaining.
   */
  public FlixelEmitter<P> makeGraphic(int width, int height, @NotNull FlixelColor fill) {
    FlixelImage image = new FlixelImage(width, height);
    image.fill(fill);
    FlixelTexture texture = Flixel.graphics.createTexture(image);
    FlixelAssetManager assets = Flixel.assets;
    FlixelGraphic g = new FlixelGraphic(assets, assets.allocateSyntheticKey(), texture);
    assets.register(g);
    return loadGraphic(g.retain(), width, height);
  }

  /**
   * Ages, moves, and recycles every living particle, then launches new ones if emitting.
   *
   * <p>Living particles update first so a particle launched this frame is drawn exactly at its
   * spawn point before it starts moving.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    int i = 0;
    while (i < count) {
      P p = particles[i];
      if (p.alive) {
        p.update(elapsed);
      }
      if (p.alive) {
        i++;
        continue;
      }
      // Swap the last living particle into this slot. It has not updated yet this frame, so the
      // loop stays on this index to update it next. The swap happens before onDeath() so a hook
      // that launches new particles sees a consistent array.
      int last = --count;
      particles[i] = particles[last];
      particles[last] = p;
      p.onDeath();
    }

    if (!emitting) {
      return;
    }
    if (frequency <= 0f) {
      emitCounted();
      return;
    }
    timer += elapsed;
    while (emitting && timer >= frequency) {
      timer -= frequency;
      emitCounted();
    }
  }

  /**
   * Draws every living particle in one batch submission.
   *
   * <p>Particles outside the camera view are skipped. Nothing is drawn until a graphic is loaded
   * with {@link #loadGraphic(FlixelFile)} or {@link #makeGraphic(int, int, FlixelColor)}.
   *
   * @param batch The batch to draw into.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    FlixelFrame[] f = frames;
    if (!visible || count == 0 || f == null || !isOnDrawCamera()) {
      return;
    }
    FlixelCamera cam = Flixel.getDrawCamera() != null ? Flixel.getDrawCamera() : Flixel.cameras.first();
    // worldToView is a pure offset, so one conversion of the origin serves every particle.
    float viewOffX = cam.worldToViewX(0f, scrollX);
    float viewOffY = cam.worldToViewY(0f, scrollY);
    int frameCount = f.length;

    int n = 0;
    float[] v = vertices;
    for (int i = 0; i < count; i++) {
      P p = particles[i];
      if (!p.alive || p.alpha <= 0f) {
        continue;
      }
      FlixelFrame fr = f[Math.max(0, Math.min(p.frame, frameCount - 1))];
      float hw = fr.getRegionWidth() * 0.5f * p.scaleX;
      float hh = fr.getRegionHeight() * 0.5f * p.scaleY;
      float cx = p.x + viewOffX;
      float cy = p.y + viewOffY;

      // |hw| + |hh| covers the quad at any rotation, so one cheap check works for every angle.
      float r = Math.abs(hw) + Math.abs(hh);
      if (!cam.isInView(cx - r, cy - r, r * 2f, r * 2f)) {
        continue;
      }

      float cos = 1f;
      float sin = 0f;
      if (p.angle != 0f) {
        cos = FlixelMath.cosDeg(p.angle);
        sin = FlixelMath.sinDeg(p.angle);
      }
      float c = FlixelColor.toFloatBits(p.red, p.green, p.blue, p.alpha);
      float u = fr.getU();
      float u2 = fr.getU2();
      float tv = fr.getV();
      float tv2 = fr.getV2();

      // Corners wound bottom-left, bottom-right, top-right, top-left, where "bottom" is the larger
      // Y since the view's Y axis points down. Each corner is rotated around the particle center.
      n = putVertex(v, n, cx, cy, -hw, hh, cos, sin, u, tv2, c);
      n = putVertex(v, n, cx, cy, hw, hh, cos, sin, u2, tv2, c);
      n = putVertex(v, n, cx, cy, hw, -hh, cos, sin, u2, tv, c);
      n = putVertex(v, n, cx, cy, -hw, -hh, cos, sin, u, tv, c);
    }
    if (n == 0) {
      return;
    }

    // Non-NORMAL blend modes apply to everything the GPU draws until restored, so this emitter's
    // geometry gets its own flush, the same way FlixelSprite isolates its blend state.
    boolean blending = blendMode != FlixelBlendMode.NORMAL;
    if (blending) {
      batch.setBlendMode(blendMode);
    }
    batch.draw(f[0].getTexture(), v, 0, n);
    if (blending) {
      batch.flush();
      batch.setBlendMode(FlixelBlendMode.NORMAL);
    }
  }

  /**
   * Destroys this emitter, releasing its graphic and discarding every living particle.
   *
   * <p>The emitter cannot draw again until a new graphic is loaded.
   */
  @Override
  public void destroy() {
    super.destroy();
    clear();
    emitting = false;
    frames = null;
    if (graphic != null) {
      graphic.release();
      graphic = null;
    }
  }

  /** Launches one particle for continuous emission and stops once the requested quantity is reached. */
  private void emitCounted() {
    emitParticle();
    emitted++;
    if (quantity > 0 && emitted >= quantity) {
      emitting = false;
    }
  }

  /** Rolls a particle's starting (and, if it changes, ending) velocity for the current launch mode. */
  private void rollVelocity(P p, boolean timed) {
    if (launchMode == FlixelEmitterMode.CIRCLE) {
      if (!speed.active) {
        return;
      }
      float a = roll(launchAngle.min, launchAngle.max);
      float cos = FlixelMath.cosDeg(a);
      float sin = FlixelMath.sinDeg(a);
      float s0 = roll(speed.start.min, speed.start.max);
      float s1 = speed.changes() ? roll(speed.end.min, speed.end.max) : s0;
      p.velocityStartX = cos * s0;
      p.velocityStartY = sin * s0;
      p.velocityEndX = cos * s1;
      p.velocityEndY = sin * s1;
      p.velocityRangeActive = timed && s0 != s1;
    } else {
      if (!velocity.active) {
        return;
      }
      FlixelPointBounds s = velocity.start;
      p.velocityStartX = roll(s.minX, s.maxX);
      p.velocityStartY = roll(s.minY, s.maxY);
      if (velocity.changes()) {
        FlixelPointBounds e = velocity.end;
        p.velocityEndX = roll(e.minX, e.maxX);
        p.velocityEndY = roll(e.minY, e.maxY);
      } else {
        p.velocityEndX = p.velocityStartX;
        p.velocityEndY = p.velocityStartY;
      }
      p.velocityRangeActive = timed
          && (p.velocityStartX != p.velocityEndX || p.velocityStartY != p.velocityEndY);
    }
    p.velocityX = p.velocityStartX;
    p.velocityY = p.velocityStartY;
  }

  /** Returns the index of a particle killed but not yet recycled, or else the one with the greatest age. */
  private int oldestIndex() {
    int oldest = 0;
    float maxAge = -1f;
    for (int i = 0; i < count; i++) {
      if (!particles[i].alive) {
        return i;
      }
      float age = particles[i].age;
      if (age > maxAge) {
        maxAge = age;
        oldest = i;
      }
    }
    return oldest;
  }

  /** Picks a random value between two numbers, skipping the generator when they are equal. */
  private float roll(float min, float max) {
    return min == max ? min : random.nextFloat(min, max);
  }

  /** Writes one rotated corner into the vertex array and returns the next write index. */
  private static int putVertex(float[] v, int n, float cx, float cy, float lx, float ly,
      float cos, float sin, float u, float tv, float c) {
    v[n] = cx + cos * lx - sin * ly;
    v[n + 1] = cy + sin * lx + cos * ly;
    v[n + 2] = u;
    v[n + 3] = tv;
    v[n + 4] = c;
    return n + 5;
  }

  /** Cuts a texture into a grid of equally sized frames, row by row from the top-left. */
  private static FlixelFrame[] splitFrames(FlixelTexture texture, int frameWidth, int frameHeight) {
    int fw = Math.max(1, frameWidth);
    int fh = Math.max(1, frameHeight);
    int cols = Math.max(1, texture.getWidth() / fw);
    int rows = Math.max(1, texture.getHeight() / fh);
    FlixelFrame[] out = new FlixelFrame[rows * cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        out[i * cols + j] = new FlixelFrame(texture, j * fw, i * fh, fw, fh);
      }
    }
    return out;
  }

  /**
   * Returns the particle stored in a slot. Slots {@code 0} through {@link #getCount()} minus one
   * hold the living particles, in no particular order.
   *
   * @param index The slot to read.
   * @return The particle in that slot.
   * @throws ArrayIndexOutOfBoundsException If {@code index} is outside the capacity.
   */
  public P getParticle(int index) {
    return particles[index];
  }

  /**
   * Returns how many particles are alive right now.
   *
   * @return The number of living particles.
   */
  public int getCount() {
    return count;
  }

  public int getCapacity() {
    return particles.length;
  }

  /**
   * Returns how many frames the loaded graphic was cut into.
   *
   * @return The frame count, or zero if no graphic is loaded.
   */
  public int getFrameCount() {
    return frames != null ? frames.length : 0;
  }

  @Nullable
  public FlixelGraphic getGraphic() {
    return graphic;
  }

  public float getX() {
    return x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(float y) {
    this.y = y;
  }

  /**
   * Moves the top-left corner of the spawn area. Particles already alive stay where they are.
   *
   * @param x The new X position in world space.
   * @param y The new Y position in world space.
   */
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public float getWidth() {
    return width;
  }

  public void setWidth(float width) {
    this.width = width;
  }

  public float getHeight() {
    return height;
  }

  public void setHeight(float height) {
    this.height = height;
  }

  /**
   * Sets the size of the spawn area. Particles appear at a random point inside it.
   *
   * @param width The spawn area width, or zero for a vertical line or point.
   * @param height The spawn area height, or zero for a horizontal line or point.
   */
  public void setSize(float width, float height) {
    this.width = width;
    this.height = height;
  }

  public float getScrollX() {
    return scrollX;
  }

  public float getScrollY() {
    return scrollY;
  }

  /**
   * Sets how much particles move with the camera, for parallax.
   *
   * @param scrollX Horizontal factor, where {@code 1} moves fully with the camera and {@code 0} stays
   *     fixed on screen.
   * @param scrollY Vertical factor, with the same meaning as {@code scrollX}.
   */
  public void setScrollFactor(float scrollX, float scrollY) {
    this.scrollX = scrollX;
    this.scrollY = scrollY;
  }

  @NotNull
  public FlixelBlendMode getBlendMode() {
    return blendMode;
  }

  /**
   * Sets how particles mix with what is behind them. {@link FlixelBlendMode#ADD} makes overlapping
   * particles glow, which suits fire, sparks, and magic.
   *
   * @param blendMode The blend mode, or {@code null} for {@link FlixelBlendMode#NORMAL}.
   */
  public void setBlendMode(@Nullable FlixelBlendMode blendMode) {
    this.blendMode = blendMode != null ? blendMode : FlixelBlendMode.NORMAL;
  }

  public boolean isAntialiasing() {
    return antialiasing;
  }

  /**
   * Sets whether the particle texture is smoothed when scaled or rotated. Leave this off for crisp
   * pixel art.
   *
   * @param antialiasing {@code true} for smooth filtering, {@code false} for sharp pixels.
   */
  public void setAntialiasing(boolean antialiasing) {
    this.antialiasing = antialiasing;
    if (graphic != null && graphic.isLoaded()) {
      graphic.getTexture().setSmooth(antialiasing);
    }
  }
}
