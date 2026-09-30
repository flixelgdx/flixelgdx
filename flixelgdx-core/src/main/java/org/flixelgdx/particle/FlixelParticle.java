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
import org.flixelgdx.collections.FlixelPool;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.tween.ease.FlixelEaseFunction;
import org.jetbrains.annotations.Nullable;

/**
 * A single lightweight particle owned by a {@link FlixelEmitter}.
 *
 * <p>A particle is a plain bundle of numbers (position, velocity, scale, color, and age), not a
 * {@link FlixelSprite}. It has no graphic, hitbox, or animation of its own; the emitter draws every
 * particle it owns in one batch using its own texture. That keeps particles cheap enough to have
 * hundreds on screen at once.
 *
 * <p>Particles are never created while the game runs. The emitter builds all of them up front,
 * then hands out the same instances over and over: {@link #reset()} clears a particle, the emitter
 * rolls its new values, and {@link #onEmit()} runs. When a particle's {@link #age} reaches its
 * {@link #lifespan}, it dies, {@link #onDeath()} runs, and the emitter reuses it later. This is
 * different from a {@link FlixelPool}: the emitter keeps its particles in one packed array instead
 * of a free list, so it never has to allocate or discard anything.
 *
 * <p>Every field is public so {@link #update(float)} stays fast and subclasses can read and change
 * anything. Most games never touch particles directly; configure the emitter instead. To add custom
 * behavior, subclass this and pass the constructor to the emitter.
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
 *     super.update(elapsed); // Keep the standard movement, fading, and aging.
 *     wobble += elapsed * 4f;
 *     x += FlixelMath.sin(wobble) * 20f * elapsed; // Sway side to side while rising.
 *   }
 * }
 *
 * FlixelEmitter<Ember> embers = new FlixelEmitter<>(96, Ember::new);
 * }</pre>
 */
public class FlixelParticle {

  /**
   * The emitter that owns this particle. Set by the emitter when the particle is created, so it is
   * only {@code null} for a particle made by hand outside of an emitter.
   */
  @Nullable
  public FlixelEmitter<?> emitter;

  /** The X position of this particle's center in world space. */
  public float x;

  /** The Y position of this particle's center in world space. */
  public float y;

  /** Horizontal velocity in pixels per second. */
  public float velocityX;

  /** Vertical velocity in pixels per second. */
  public float velocityY;

  /** Horizontal acceleration in pixels per second squared. */
  public float accelerationX;

  /** Vertical acceleration in pixels per second squared. */
  public float accelerationY;

  /** Horizontal slowdown in pixels per second squared, applied only while {@link #accelerationX} is zero. */
  public float dragX;

  /** Vertical slowdown in pixels per second squared, applied only while {@link #accelerationY} is zero. */
  public float dragY;

  /** Rotation in degrees, clockwise on screen. */
  public float angle;

  /** Rotation speed in degrees per second. */
  public float angularVelocity;

  /** Horizontal draw scale, where {@code 1} is the frame's natural width. */
  public float scaleX = 1f;

  /** Vertical draw scale, where {@code 1} is the frame's natural height. */
  public float scaleY = 1f;

  /** Opacity, from {@code 0} (invisible) to {@code 1} (fully opaque). */
  public float alpha = 1f;

  /** The red tint channel, from {@code 0} to {@code 1}. */
  public float red = 1f;

  /** The green tint channel, from {@code 0} to {@code 1}. */
  public float green = 1f;

  /** The blue tint channel, from {@code 0} to {@code 1}. */
  public float blue = 1f;

  /** How many seconds this particle has been alive. */
  public float age;

  /**
   * How many seconds this particle lives before dying. Zero or less means it lives until killed,
   * and lifetime ranges (fading, shrinking, and so on) do not run.
   */
  public float lifespan;

  /** The velocity at the start of this particle's life, used while {@link #velocityRangeActive} is set. */
  public float velocityStartX;

  /** The velocity at the start of this particle's life, used while {@link #velocityRangeActive} is set. */
  public float velocityStartY;

  /** The velocity at the end of this particle's life, used while {@link #velocityRangeActive} is set. */
  public float velocityEndX;

  /** The velocity at the end of this particle's life, used while {@link #velocityRangeActive} is set. */
  public float velocityEndY;

  /** The rotation speed at the start of this particle's life. */
  public float angularVelocityStart;

  /** The rotation speed at the end of this particle's life. */
  public float angularVelocityEnd;

  /** The angle at the start of this particle's life, used while {@link #angleRangeActive} is set. */
  public float angleStart;

  /** The angle at the end of this particle's life, used while {@link #angleRangeActive} is set. */
  public float angleEnd;

  /** The horizontal scale at the start of this particle's life. */
  public float scaleStartX = 1f;

  /** The vertical scale at the start of this particle's life. */
  public float scaleStartY = 1f;

  /** The horizontal scale at the end of this particle's life. */
  public float scaleEndX = 1f;

  /** The vertical scale at the end of this particle's life. */
  public float scaleEndY = 1f;

  /** The opacity at the start of this particle's life. */
  public float alphaStart = 1f;

  /** The opacity at the end of this particle's life. */
  public float alphaEnd = 1f;

  /** The red channel at the start of this particle's life. */
  public float redStart = 1f;

  /** The green channel at the start of this particle's life. */
  public float greenStart = 1f;

  /** The blue channel at the start of this particle's life. */
  public float blueStart = 1f;

  /** The red channel at the end of this particle's life. */
  public float redEnd = 1f;

  /** The green channel at the end of this particle's life. */
  public float greenEnd = 1f;

  /** The blue channel at the end of this particle's life. */
  public float blueEnd = 1f;

  /** Which of the emitter's frames this particle draws, as an index into its frame list. */
  public int frame;

  /** Whether this particle is still alive. The emitter recycles it on its next update once this is {@code false}. */
  public boolean alive;

  /** Whether velocity slides from its start value to its end value instead of following acceleration. */
  public boolean velocityRangeActive;

  /** Whether rotation speed slides from its start value to its end value. */
  public boolean angularVelocityRangeActive;

  /** Whether the angle slides from its start value to its end value, ignoring rotation speed. */
  public boolean angleRangeActive;

  /** Whether scale slides from its start value to its end value. */
  public boolean scaleRangeActive;

  /** Whether opacity slides from its start value to its end value. */
  public boolean alphaRangeActive;

  /** Whether the tint slides from its start color to its end color. */
  public boolean colorRangeActive;

  /**
   * Advances this particle by one frame: ages it, slides its lifetime ranges, and moves it.
   *
   * <p>The emitter calls this for every living particle each frame. When the particle's age passes
   * its lifespan, it is killed here and nothing else runs. Override this to add custom motion, and
   * call {@code super.update(elapsed)} to keep the standard behavior.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  public void update(float elapsed) {
    age += elapsed;
    if (lifespan > 0f) {
      if (age >= lifespan) {
        kill();
        return;
      }
      updateRanges(age / lifespan);
    }

    if (!velocityRangeActive) {
      velocityX = computeVelocity(velocityX, accelerationX, dragX, elapsed);
      velocityY = computeVelocity(velocityY, accelerationY, dragY, elapsed);
    }
    x += velocityX * elapsed;
    y += velocityY * elapsed;

    if (!angleRangeActive) {
      angle += angularVelocity * elapsed;
    }
  }

  /**
   * Marks this particle as dead so its emitter recycles it on its next update.
   *
   * <p>Safe to call from anywhere, including from inside {@link #update(float)}.
   */
  public void kill() {
    alive = false;
  }

  /**
   * Called right after this particle is launched, once the emitter has rolled all of its values.
   *
   * <p>Override this to set up custom state for each launch. The default does nothing.
   */
  public void onEmit() {}

  /**
   * Called once when this particle dies, just before the emitter recycles it.
   *
   * <p>Override this to react to a particle ending, such as spawning a smaller burst where it
   * vanished. If you launch new particles from here, read this particle's fields first: the emitter
   * may reuse this very instance for the new launch. For the same reason, do not keep a reference
   * to this particle afterwards. The default does nothing.
   *
   * <p>This does not run when a full emitter recycles a particle early, or when
   * {@link FlixelEmitter#clear()} removes it.
   */
  public void onDeath() {}

  /**
   * Clears every field back to its default so no state carries over from a previous launch.
   *
   * <p>The emitter calls this before rolling new values. Subclasses with their own fields should
   * override this, clear those fields, and call {@code super.reset()}. The {@link #emitter}
   * reference is kept.
   */
  public void reset() {
    x = 0f;
    y = 0f;
    velocityX = 0f;
    velocityY = 0f;
    accelerationX = 0f;
    accelerationY = 0f;
    dragX = 0f;
    dragY = 0f;
    angle = 0f;
    angularVelocity = 0f;
    scaleX = 1f;
    scaleY = 1f;
    alpha = 1f;
    red = 1f;
    green = 1f;
    blue = 1f;
    age = 0f;
    lifespan = 0f;
    velocityStartX = 0f;
    velocityStartY = 0f;
    velocityEndX = 0f;
    velocityEndY = 0f;
    angularVelocityStart = 0f;
    angularVelocityEnd = 0f;
    angleStart = 0f;
    angleEnd = 0f;
    scaleStartX = 1f;
    scaleStartY = 1f;
    scaleEndX = 1f;
    scaleEndY = 1f;
    alphaStart = 1f;
    alphaEnd = 1f;
    redStart = 1f;
    greenStart = 1f;
    blueStart = 1f;
    redEnd = 1f;
    greenEnd = 1f;
    blueEnd = 1f;
    frame = 0;
    alive = false;
    velocityRangeActive = false;
    angularVelocityRangeActive = false;
    angleRangeActive = false;
    scaleRangeActive = false;
    alphaRangeActive = false;
    colorRangeActive = false;
  }

  /** Slides every active lifetime range to the given point in this particle's life. */
  private void updateRanges(float t) {
    FlixelEmitter<?> e = emitter;
    if (velocityRangeActive) {
      FlixelEaseFunction ease = null;
      if (e != null) {
        ease = e.launchMode == FlixelEmitterMode.CIRCLE ? e.speed.ease : e.velocity.ease;
      }
      float k = ease(ease, t);
      velocityX = FlixelMath.lerp(velocityStartX, velocityEndX, k);
      velocityY = FlixelMath.lerp(velocityStartY, velocityEndY, k);
    }
    if (angleRangeActive) {
      angle = FlixelMath.lerp(angleStart, angleEnd, ease(e != null ? e.angle.ease : null, t));
    } else if (angularVelocityRangeActive) {
      float k = ease(e != null ? e.angularVelocity.ease : null, t);
      angularVelocity = FlixelMath.lerp(angularVelocityStart, angularVelocityEnd, k);
    }
    if (scaleRangeActive) {
      float k = ease(e != null ? e.scale.ease : null, t);
      scaleX = FlixelMath.lerp(scaleStartX, scaleEndX, k);
      scaleY = FlixelMath.lerp(scaleStartY, scaleEndY, k);
    }
    if (alphaRangeActive) {
      alpha = FlixelMath.lerp(alphaStart, alphaEnd, ease(e != null ? e.alpha.ease : null, t));
    }
    if (colorRangeActive) {
      float k = ease(e != null ? e.color.ease : null, t);
      red = FlixelMath.lerp(redStart, redEnd, k);
      green = FlixelMath.lerp(greenStart, greenEnd, k);
      blue = FlixelMath.lerp(blueStart, blueEnd, k);
    }
    if (e != null && e.animateFrames) {
      int count = e.getFrameCount();
      if (count > 1) {
        frame = Math.min((int) (t * count), count - 1);
      }
    }
  }

  /** Runs an optional easing curve, falling back to a straight line when there is none. */
  private static float ease(@Nullable FlixelEaseFunction ease, float t) {
    return ease != null ? ease.compute(t) : t;
  }

  /** Applies acceleration, or drag toward zero when there is no acceleration, to one velocity axis. */
  private static float computeVelocity(float velocity, float acceleration, float drag, float elapsed) {
    if (acceleration != 0f) {
      return velocity + acceleration * elapsed;
    }
    if (drag != 0f) {
      float d = drag * elapsed;
      if (velocity - d > 0f) {
        return velocity - d;
      }
      if (velocity + d < 0f) {
        return velocity + d;
      }
      return 0f;
    }
    return velocity;
  }
}
