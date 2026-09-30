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

import org.flixelgdx.FlixelSprite;
import org.flixelgdx.math.FlixelPointRange;
import org.flixelgdx.math.FlixelRange;
import org.flixelgdx.tween.ease.FlixelEaseFunction;
import org.flixelgdx.util.FlixelColorRange;
import org.jetbrains.annotations.Nullable;

/**
 * A sprite with a limited lifetime, launched and recycled by a {@link FlixelEmitter}.
 *
 * <p>A particle is a regular {@link FlixelSprite}, so it moves, spins, scales, tints, animates,
 * and draws exactly like any other sprite. On top of that it adds a {@link #lifespan}, an
 * {@link #age}, and a set of ranges (such as {@link #alphaRange}) that it slides along as it ages.
 * When its age reaches its lifespan, it kills itself and its emitter recycles it for a later
 * launch.
 *
 * <p>Particles are never created while the game runs. The emitter builds all of them up front and
 * reuses them: {@link #reset(float, float)} clears a particle, the emitter rolls its new values,
 * and {@link #onEmit()} runs. Most games never touch particles directly; configure the emitter
 * instead. To add custom behavior, subclass this and pass the constructor to the emitter.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class Ember extends FlixelParticle {
 *
 *   private float wobble;
 *
 *   // Runs each time this particle is launched, after the emitter has set its values.
 *   @Override
 *   public void onEmit() {
 *     wobble = emitter.random.nextFloat(0f, FlixelMath.PI2);
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed); // Keep the standard aging, fading, and movement.
 *     wobble += elapsed * 4f;
 *     changeX(FlixelMath.sin(wobble) * 20f * elapsed); // Sway side to side while rising.
 *   }
 * }
 *
 * FlixelEmitter<Ember> embers = new FlixelEmitter<>(96, Ember::new);
 * }</pre>
 */
public class FlixelParticle extends FlixelSprite {

  /**
   * The emitter that owns this particle. Set by the emitter when the particle is created, so it is
   * only {@code null} for a particle made by hand outside of an emitter.
   */
  @Nullable
  public FlixelEmitter<?> emitter;

  /** The velocity this particle slides between over its life, when active. */
  public final FlixelPointRange velocityRange = new FlixelPointRange();

  /** The rotation speed this particle slides between over its life, when active. */
  public final FlixelRange angularVelocityRange = new FlixelRange();

  /** The angle this particle turns between over its life, when active. Rotation speed is ignored then. */
  public final FlixelRange angleRange = new FlixelRange();

  /** The scale this particle slides between over its life, when active. */
  public final FlixelPointRange scaleRange = new FlixelPointRange();

  /** The opacity this particle slides between over its life, when active. */
  public final FlixelRange alphaRange = new FlixelRange();

  /** The tint this particle blends between over its life, when active. Only red, green, and blue are used. */
  public final FlixelColorRange colorRange = new FlixelColorRange();

  /** How many seconds this particle has been alive. */
  public float age;

  /**
   * How many seconds this particle lives before dying. Zero or less means it lives until killed,
   * and its ranges never run.
   */
  public float lifespan;

  /** Creates a particle that starts dead, waiting for its emitter to launch it. */
  public FlixelParticle() {
    super();
    alive = false;
    exists = false;
  }

  /**
   * Ages this particle, slides its active ranges, then moves and animates it like any sprite.
   *
   * <p>When the particle's age passes its lifespan, it is killed here and does not move this
   * frame. Override this to add custom motion, and call {@code super.update(elapsed)} to keep the
   * standard behavior.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    age += elapsed;
    if (lifespan > 0f) {
      if (age >= lifespan) {
        kill();
        return;
      }
      updateRanges(age / lifespan);
    }
    super.update(elapsed);
  }

  /**
   * Revives this particle at a position and clears everything left over from its last launch.
   *
   * <p>The emitter calls this right before rolling new values, so the particle starts from plain
   * sprite defaults: no velocity, acceleration, drag, or rotation, normal scale, white, and fully
   * opaque, with every range inactive. Subclasses with their own fields should override this,
   * clear those fields, and call {@code super.reset(x, y)}.
   *
   * @param x The new X position of this particle's top-left corner.
   * @param y The new Y position of this particle's top-left corner.
   */
  @Override
  public void reset(float x, float y) {
    super.reset(x, y);
    setAcceleration(0f, 0f);
    setDrag(0f, 0f);
    setAngle(0f);
    setAngularVelocity(0f);
    setScale(1f);
    age = 0f;
    lifespan = 0f;
    visible = true;
    velocityRange.active = false;
    angularVelocityRange.active = false;
    angleRange.active = false;
    scaleRange.active = false;
    alphaRange.active = false;
    colorRange.active = false;
  }

  /**
   * Called right after this particle is launched, once the emitter has rolled all of its values.
   *
   * <p>Override this to set up custom state for each launch. The default does nothing.
   */
  public void onEmit() {}

  /**
   * Called once when this particle dies, just after its emitter has moved it out of the living
   * particles.
   *
   * <p>Override this to react to a particle ending, such as spawning a smaller burst where it
   * vanished. If you launch new particles from here, read this particle's values first: the
   * emitter may reuse this very instance for the new launch. For the same reason, do not keep a
   * reference to this particle afterwards. The default does nothing.
   *
   * <p>This does not run when a full emitter recycles a particle early, or when
   * {@link FlixelEmitter#clear()} removes it.
   */
  public void onDeath() {}

  /** Slides every active range to the given point in this particle's life. */
  private void updateRanges(float t) {
    FlixelEmitter<?> e = emitter;
    if (velocityRange.active) {
      FlixelEaseFunction ease = null;
      if (e != null) {
        ease = e.launchMode == FlixelEmitterMode.CIRCLE ? e.speed.ease : e.velocity.ease;
      }
      float k = ease(ease, t);
      velocityX = velocityRange.lerpX(k);
      velocityY = velocityRange.lerpY(k);
    }
    if (angleRange.active) {
      setAngle(angleRange.lerp(ease(e != null ? e.angle.ease : null, t)));
    } else if (angularVelocityRange.active) {
      angularVelocity = angularVelocityRange.lerp(ease(e != null ? e.angularVelocity.ease : null, t));
    }
    if (scaleRange.active) {
      float k = ease(e != null ? e.scale.ease : null, t);
      scaleX = scaleRange.lerpX(k);
      scaleY = scaleRange.lerpY(k);
    }
    if (colorRange.active) {
      // The color range carries no opacity of its own, so keep whatever alpha the particle has.
      float a = color.a;
      colorRange.lerp(ease(e != null ? e.color.ease : null, t), color);
      color.a = a;
    }
    if (alphaRange.active) {
      color.a = alphaRange.lerp(ease(e != null ? e.alpha.ease : null, t));
    }
    if (e != null && e.animateFrames) {
      int count = e.getFrameCount();
      if (count > 1) {
        setRegion(e.getFrame(Math.min((int) (t * count), count - 1)));
      }
    }
  }

  /** Runs an optional easing curve, falling back to a straight line when there is none. */
  private static float ease(@Nullable FlixelEaseFunction ease, float t) {
    return ease != null ? ease.compute(t) : t;
  }
}
