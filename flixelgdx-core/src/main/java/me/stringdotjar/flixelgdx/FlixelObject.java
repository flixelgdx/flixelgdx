/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import me.stringdotjar.flixelgdx.box2d.FlixelBox2DObject;
import me.stringdotjar.flixelgdx.debug.FlixelDebugDrawable;
import me.stringdotjar.flixelgdx.util.FlixelConstants;

/**
 * The base class for all visual/spatial objects in Flixel. Extends {@link FlixelBasic} with
 * position ({@link #x}, {@link #y}), dimensions ({@link #width}, {@link #height}),
 * rotation ({@link #angle}), and a full kinematic physics model (velocity, acceleration,
 * drag, collision flags) modeled after
 * <a href="https://api.haxeflixel.com/flixel/FlxObject.html">HaxeFlixel's FlxObject</a>.
 *
 * <p>Most games interact with this through {@link FlixelSprite}, which adds graphical
 * capabilities on top of this spatial foundation.
 *
 * <h3>Collision</h3>
 * Use {@link me.stringdotjar.flixelgdx.Flixel#overlap Flixel.overlap()} and
 * {@link me.stringdotjar.flixelgdx.Flixel#collide Flixel.collide()} for overlap/separation
 * checks. The static {@link #separate(FlixelObject, FlixelObject)} method resolves overlaps
 * by adjusting positions and velocities.
 *
 * <h3>Box2D</h3>
 * To use Box2D physics instead of the built-in kinematic model, implement
 * {@link FlixelBox2DObject} on your subclass. See that interface's documentation for details.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html">FlxObject (HaxeFlixel)</a>
 * @see FlixelBox2DObject
 */
public class FlixelObject extends FlixelBasic implements FlixelDebugDrawable {

  /** X position of the upper left corner of this object in world space. */
  private float x = 0f;

  /** Y position of the upper left corner of this object in world space. */
  private float y = 0f;

  /** The width of this object's hitbox. */
  private float width = 0f;

  /** The height of this object's hitbox. */
  private float height = 0f;

  /** X position at the start of the current frame, before motion. */
  protected float lastX = 0f;

  /** Y position at the start of the current frame, before motion. */
  protected float lastY = 0f;

  /** The angle (in degrees) of this object. Does not affect collision. */
  private float angle = 0f;

  /** Horizontal velocity in pixels per second. */
  protected float velocityX = 0f;

  /** Vertical velocity in pixels per second. */
  protected float velocityY = 0f;

  /** Horizontal acceleration in pixels per second squared. */
  protected float accelerationX = 0f;

  /** Vertical acceleration in pixels per second squared. */
  protected float accelerationY = 0f;

  /** Deceleration applied when {@link #accelerationX} is zero. Only applied when greater than {@code 0}. */
  protected float dragX = 0f;

  /** Deceleration applied when {@link #accelerationY} is zero. Only applied when greater than {@code 0}. */
  protected float dragY = 0f;

  /** Maximum absolute horizontal velocity. */
  protected float maxVelocityX = 10000f;

  /** Maximum absolute vertical velocity. */
  protected float maxVelocityY = 10000f;

  /** Rotational speed in degrees per second. */
  protected float angularVelocity = 0f;

  /** Rotational acceleration in degrees per second squared. */
  protected float angularAcceleration = 0f;

  /** Rotational drag in degrees per second squared. */
  protected float angularDrag = 0f;

  /** Maximum angular velocity in degrees per second. */
  protected float maxAngularVelocity = 10000f;

  /** When {@code true}, {@link #updateMotion(float)} runs each frame. */
  protected boolean moves = true;

  /**
   * Bit field of direction flags indicating which sides allow collision.
   * Use {@link FlixelConstants.Physics#ANY} (default) for full collision,
   * {@link FlixelConstants.Physics#NONE} for no collision, or combine
   * individual flags.
   *
   * @see FlixelConstants.Physics
   */
  protected int allowCollisions = FlixelConstants.Physics.ANY;

  /**
   * Bit field indicating which surfaces this object is currently touching.
   * Set by {@link #separate} and reset at the start of each {@link #update}.
   */
  protected int touching = FlixelConstants.Physics.NONE;

  /**
   * Copy of {@link #touching} from the previous frame, useful for detecting
   * the moment an object lands ({@link #justTouched}).
   */
  protected int wasTouching = FlixelConstants.Physics.NONE;

  /**
   * When {@code true}, this object will not be moved by collision resolution.
   * Other objects will still be pushed away from it.
   */
  protected boolean immovable = false;

  /**
   * Bounciness factor used during collision resolution. {@code 0} means no
   * bounce (default), {@code 1} means fully elastic.
   */
  protected float elasticity = 0f;

  /**
   * Virtual mass used during elastic collision resolution. Default is
   * {@code 1}. Only matters when both colliding objects have
   * {@code elasticity > 0}.
   */
  protected float mass = 1f;

  /**
   * Color when {@code allowCollisions == ANY} and {@code !immovable} (solid).
   * Default: red.
   *
   * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#debugBoundingBoxColorSolid">FlxObject.debugBoundingBoxColorSolid</a>
   */
  protected final float[] debugColorSolid = { 1f, 0.2f, 0.2f, 0.6f };

  /**
   * Color when {@code immovable} is {@code true} or {@code allowCollisions}
   * is partial. Default: green.
   *
   * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#debugBoundingBoxColorPartial">FlxObject.debugBoundingBoxColorPartial</a>
   */
  protected final float[] debugColorImmovable = { 0.2f, 0.9f, 0.2f, 0.6f };

  /**
   * Color when {@code allowCollisions == NONE}. Default: blue.
   *
   * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#debugBoundingBoxColorNotSolid">FlxObject.debugBoundingBoxColorNotSolid</a>
   */
  protected final float[] debugColorNoCollision = { 0.2f, 0.4f, 1f, 0.6f };

  /**
   * If non-null, forces the debug bounding box to this color regardless
   * of collision state.
   */
  protected float[] debugColorOverride = null;

  public FlixelObject() {
    this(0f, 0f, 0f, 0f);
  }

  public FlixelObject(float x, float y) {
    this(x, y, 0f, 0f);
  }

  public FlixelObject(float x, float y, float width, float height) {
    super();
    this.x = x;
    this.y = y;
    this.lastX = x;
    this.lastY = y;
    this.width = width;
    this.height = height;
  }

  /**
   * Updates this object's collision bookkeeping and, when {@link #moves} is
   * {@code true}, calls {@link #updateMotion(float)}.
   *
   * @param elapsed Seconds since the last frame.
   */
  @Override
  public void update(float elapsed) {
    lastX = x;
    lastY = y;

    if (moves) {
      updateMotion(elapsed);
    }

    wasTouching = touching;
    touching = FlixelConstants.Physics.NONE;
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

  public float getAngle() {
    return angle;
  }

  public void setAngle(float angle) {
    this.angle = angle;
  }

  public float getLastX() {
    return lastX;
  }

  public float getLastY() {
    return lastY;
  }

  /**
   * Helper function to set the coordinates of this object.
   *
   * @param x The new x position.
   * @param y The new y position.
   */
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Shortcut for setting both {@link #width} and {@link #height}.
   *
   * @param width The new width.
   * @param height The new height.
   */
  public void setSize(float width, float height) {
    this.width = width;
    this.height = height;
  }

  /** Adds {@code dx} to the current X position. */
  public void changeX(float dx) {
    setX(x + dx);
  }

  /** Adds {@code dy} to the current Y position. */
  public void changeY(float dy) {
    setY(y + dy);
  }

  /** Adds {@code dr} degrees to the current rotation angle. */
  public void changeRotation(float dr) {
    setAngle(angle + dr);
  }

  /** Returns the center X coordinate of this object. */
  public float getMidpointX() {
    return x + width / 2f;
  }

  /** Returns the center Y coordinate of this object. */
  public float getMidpointY() {
    return y + height / 2f;
  }

  public float getVelocityX() {
    return velocityX;
  }

  public void setVelocityX(float velocityX) {
    this.velocityX = velocityX;
  }

  public float getVelocityY() {
    return velocityY;
  }

  public void setVelocityY(float velocityY) {
    this.velocityY = velocityY;
  }

  public void setVelocity(float vx, float vy) {
    this.velocityX = vx;
    this.velocityY = vy;
  }

  public float getAccelerationX() {
    return accelerationX;
  }

  public void setAccelerationX(float ax) {
    this.accelerationX = ax;
  }

  public float getAccelerationY() {
    return accelerationY;
  }

  public void setAccelerationY(float ay) {
    this.accelerationY = ay;
  }

  public void setAcceleration(float ax, float ay) {
    this.accelerationX = ax;
    this.accelerationY = ay;
  }

  public float getDragX() {
    return dragX;
  }

  public void setDragX(float dx) {
    this.dragX = dx;
  }

  public float getDragY() {
    return dragY;
  }

  public void setDragY(float dy) {
    this.dragY = dy;
  }

  public void setDrag(float dx, float dy) {
    this.dragX = dx;
    this.dragY = dy;
  }

  public float getMaxVelocityX() {
    return maxVelocityX;
  }

  public void setMaxVelocityX(float mvx) {
    this.maxVelocityX = mvx;
  }

  public float getMaxVelocityY() {
    return maxVelocityY;
  }

  public void setMaxVelocityY(float mvy) {
    this.maxVelocityY = mvy;
  }

  public void setMaxVelocity(float mvx, float mvy) {
    this.maxVelocityX = mvx;
    this.maxVelocityY = mvy;
  }

  public float getAngularVelocity() {
    return angularVelocity;
  }

  public void setAngularVelocity(float av) {
    this.angularVelocity = av;
  }

  public float getAngularAcceleration() {
    return angularAcceleration;
  }

  public void setAngularAcceleration(float aa) {
    this.angularAcceleration = aa;
  }

  public float getAngularDrag() {
    return angularDrag;
  }

  public void setAngularDrag(float ad) {
    this.angularDrag = ad;
  }

  public float getMaxAngularVelocity() {
    return maxAngularVelocity;
  }

  public void setMaxAngularVelocity(float mav) {
    this.maxAngularVelocity = mav;
  }

  public boolean getMoves() {
    return moves;
  }

  public void setMoves(boolean moves) {
    this.moves = moves;
  }

  public int getAllowCollisions() {
    return allowCollisions;
  }

  public void setAllowCollisions(int flags) {
    this.allowCollisions = flags;
  }

  /**
   * Convenience accessor, returns {@code true} when {@code allowCollisions} is not {@link FlixelConstants.Physics#NONE}.
   */
  public boolean isSolid() {
    return allowCollisions != FlixelConstants.Physics.NONE;
  }

  /**
   * Convenience setter: sets {@link #allowCollisions} to {@code ANY} when
   * {@code solid} is true, or {@code NONE} when false.
   */
  public void setSolid(boolean solid) {
    allowCollisions = solid ? FlixelConstants.Physics.ANY : FlixelConstants.Physics.NONE;
  }

  public int getTouching() {
    return touching;
  }

  public int getWasTouching() {
    return wasTouching;
  }

  public boolean isImmovable() {
    return immovable;
  }

  public void setImmovable(boolean immovable) {
    this.immovable = immovable;
  }

  public float getElasticity() {
    return elasticity;
  }

  public void setElasticity(float elasticity) {
    this.elasticity = elasticity;
  }

  public float getMass() {
    return mass;
  }

  public void setMass(float mass) {
    this.mass = mass;
  }

  /**
   * Returns {@code true} if this object is currently touching the given
   * surface(s). Flags are set by {@link Flixel#collide} and reset each frame.
   *
   * @param direction One or more direction flags ORed together.
   */
  public boolean isTouching(int direction) {
    return (touching & direction) != 0;
  }

  /**
   * Returns {@code true} if this object <em>just</em> started touching the
   * given surface(s) this frame (was not touching last frame).
   *
   * @param direction One or more direction flags ORed together.
   */
  public boolean justTouched(int direction) {
    return (touching & direction) != 0 && (wasTouching & direction) == 0;
  }

  /**
   * Resets this object to a new position, reviving it and clearing physics state.
   *
   * @param x New X position.
   * @param y New Y position.
   */
  public void reset(float x, float y) {
    revive();
    setPosition(x, y);
    lastX = x;
    lastY = y;
    velocityX = velocityY = 0f;
    touching = wasTouching = FlixelConstants.Physics.NONE;
  }

  /**
   * Checks if this object's AABB overlaps another {@link FlixelObject}.
   *
   * @param other The other object.
   * @return {@code true} if the two AABBs overlap.
   */
  public boolean overlaps(FlixelObject other) {
    if (other == null || !other.exists || !this.exists) {
      return false;
    }
    return x < other.x + other.width
      && x + width > other.x
      && y < other.y + other.height
      && y + height > other.y;
  }

  /**
   * Checks if a point in world space overlaps this object's AABB.
   *
   * @param px X coordinate.
   * @param py Y coordinate.
   * @return {@code true} if the point is inside.
   */
  public boolean overlapsPoint(float px, float py) {
    return px >= x && px <= x + width && py >= y && py <= y + height;
  }

  /**
   * Returns {@code true} if the object is within the world bounds
   * configured on the global manager class.
   */
  public boolean inWorldBounds() {
    float[] wb = Flixel.getWorldBounds();
    return x + width > wb[0]
      && x < wb[0] + wb[2]
      && y + height > wb[1]
      && y < wb[1] + wb[3];
  }

  /**
   * Internal function for updating the position and speed of this object
   * using a velocity-verlet style integration matching
   * <a href="https://api.haxeflixel.com/flixel/FlxObject.html">FlxObject</a>.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  public void updateMotion(float elapsed) {
    // Angular motion.
    float velocityDelta = 0.5f * (computeVelocity(angularVelocity, angularAcceleration, angularDrag, maxAngularVelocity, elapsed) - angularVelocity);
    angularVelocity += velocityDelta;
    angle += angularVelocity * elapsed;
    angularVelocity += velocityDelta;

    // Horizontal motion.
    velocityDelta = 0.5f * (computeVelocity(velocityX, accelerationX, dragX, maxVelocityX, elapsed) - velocityX);
    velocityX += velocityDelta;
    float delta = velocityX * elapsed;
    velocityX += velocityDelta;
    x += delta;

    // Vertical motion.
    velocityDelta = 0.5f * (computeVelocity(velocityY, accelerationY, dragY, maxVelocityY, elapsed) - velocityY);
    velocityY += velocityDelta;
    delta = velocityY * elapsed;
    velocityY += velocityDelta;
    y += delta;
  }

  /**
   * Internal function for computing velocity after acceleration and drag.
   *
   * @param velocity Current velocity.
   * @param acceleration Acceleration to apply.
   * @param drag Drag to apply when acceleration is zero.
   * @param max Maximum allowed velocity.
   * @param elapsed Seconds elapsed since the last frame.
   * @return Updated velocity.
   */
  public float computeVelocity(float velocity, float acceleration, float drag, float max, float elapsed) {
    if (acceleration != 0) {
      velocity += acceleration * elapsed;
    } else if (drag != 0) {
      float dragDelta = drag * elapsed;
      if (velocity - dragDelta > 0) {
        velocity -= dragDelta;
      } else if (velocity + dragDelta < 0) {
        velocity += dragDelta;
      } else {
        velocity = 0;
      }
    }

    if ((velocity != 0) && (max != 10000f)) {
      if (velocity > max) {
        velocity = max;
      } else if (velocity < -max) {
        velocity = -max;
      }
    }

    return velocity;
  }

  /**
   * Separates two overlapping objects by adjusting positions and velocities.
   * Calls {@link #separateX} then {@link #separateY}.
   *
   * @param object1 First object.
   * @param object2 Second object.
   * @return {@code true} if the objects were overlapping and were separated.
   * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#separate">FlxObject.separate</a>
   */
  public static boolean separate(FlixelObject object1, FlixelObject object2) {
    boolean separatedX = separateX(object1, object2);
    boolean separatedY = separateY(object1, object2);
    return separatedX || separatedY;
  }

  /**
   * Separates two overlapping objects on the X axis.
   *
   * @return {@code true} if the objects overlapped and were separated on X.
   */
  public static boolean separateX(FlixelObject object1, FlixelObject object2) {
    boolean immovable1 = object1.immovable;
    boolean immovable2 = object2.immovable;
    if (immovable1 && immovable2) {
      return false;
    }

    float overlap = computeOverlapX(object1, object2, true);
    if (overlap == 0) {
      return false;
    }

    float v1 = object1.velocityX;
    float v2 = object2.velocityX;

    if (!immovable1 && !immovable2) {
      overlap *= 0.5f;
      object1.x -= overlap;
      object2.x += overlap;

      float avgElasticity = (object1.elasticity + object2.elasticity) * 0.5f;
      if (avgElasticity > 0) {
        float m1 = object1.mass;
        float m2 = object2.mass;
        float totalMass = m1 + m2;
        object1.velocityX = ((m1 - m2) * v1 + 2 * m2 * v2) / totalMass * avgElasticity;
        object2.velocityX = ((m2 - m1) * v2 + 2 * m1 * v1) / totalMass * avgElasticity;
      } else {
        float avg = (v1 + v2) * 0.5f;
        object1.velocityX = avg;
        object2.velocityX = avg;
      }
    } else if (immovable1) {
      object2.x += overlap;
      object2.velocityX = v1 - v2 * object2.elasticity;
    } else {
      object1.x -= overlap;
      object1.velocityX = v2 - v1 * object1.elasticity;
    }

    return true;
  }

  /**
   * Separates two overlapping objects on the Y axis.
   *
   * @return {@code true} if the objects overlapped and were separated on Y.
   */
  public static boolean separateY(FlixelObject object1, FlixelObject object2) {
    boolean immovable1 = object1.immovable;
    boolean immovable2 = object2.immovable;
    if (immovable1 && immovable2) {
      return false;
    }

    float overlap = computeOverlapY(object1, object2, true);
    if (overlap == 0) {
      return false;
    }

    float v1 = object1.velocityY;
    float v2 = object2.velocityY;

    if (!immovable1 && !immovable2) {
      overlap *= 0.5f;
      object1.y -= overlap;
      object2.y += overlap;

      float avgElasticity = (object1.elasticity + object2.elasticity) * 0.5f;
      if (avgElasticity > 0) {
        float m1 = object1.mass;
        float m2 = object2.mass;
        float totalMass = m1 + m2;
        object1.velocityY = ((m1 - m2) * v1 + 2 * m2 * v2) / totalMass * avgElasticity;
        object2.velocityY = ((m2 - m1) * v2 + 2 * m1 * v1) / totalMass * avgElasticity;
      } else {
        float avg = (v1 + v2) * 0.5f;
        object1.velocityY = avg;
        object2.velocityY = avg;
      }
    } else if (immovable1) {
      object2.y += overlap;
      object2.velocityY = v1 - v2 * object2.elasticity;
    } else {
      object1.y -= overlap;
      object1.velocityY = v2 - v1 * object1.elasticity;
    }

    return true;
  }

  /**
   * Computes the overlap between two objects on the X axis and updates their
   * {@link #touching} flags. Returns {@code 0} if they do not overlap or if their
   * {@code allowCollisions} flags disallow collision on the overlapping sides.
   *
   * @param object1 First object.
   * @param object2 Second object.
   * @param checkMaxOverlap Whether to reject overlaps greater than combined movement + {@code SEPARATE_BIAS}.
   * @return Signed overlap in pixels (positive means object1's right side penetrates object2's left side).
   * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#computeOverlapX">FlxObject.computeOverlapX</a>
   */
  public static float computeOverlapX(FlixelObject object1, FlixelObject object2, boolean checkMaxOverlap) {
    float obj1delta = object1.x - object1.lastX;
    float obj2delta = object2.x - object2.lastX;

    if (obj1delta == obj2delta) {
      return 0;
    }

    float obj1deltaAbs = Math.abs(obj1delta);
    float obj2deltaAbs = Math.abs(obj2delta);

    // Swept AABBs
    float rect1x = object1.x - (obj1delta > 0 ? obj1delta : 0);
    float rect1w = object1.width + obj1deltaAbs;
    float rect1y = object1.lastY;
    float rect1h = object1.height;

    float rect2x = object2.x - (obj2delta > 0 ? obj2delta : 0);
    float rect2w = object2.width + obj2deltaAbs;
    float rect2y = object2.lastY;
    float rect2h = object2.height;

    if (rect1x + rect1w <= rect2x || rect1x >= rect2x + rect2w
      || rect1y + rect1h <= rect2y || rect1y >= rect2y + rect2h) {
      return 0;
    }

    float maxOverlap = checkMaxOverlap ? (obj1deltaAbs + obj2deltaAbs + FlixelConstants.Physics.SEPARATE_BIAS) : 0;
    float overlap;

    if (obj1delta > obj2delta) {
      overlap = object1.x + object1.width - object2.x;
      if (checkMaxOverlap && overlap > maxOverlap) return 0;
      if ((object1.allowCollisions & FlixelConstants.Physics.RIGHT) == 0
        || (object2.allowCollisions & FlixelConstants.Physics.LEFT) == 0) return 0;
      object1.touching |= FlixelConstants.Physics.RIGHT;
      object2.touching |= FlixelConstants.Physics.LEFT;
    } else {
      overlap = object1.x - object2.width - object2.x;
      if (checkMaxOverlap && -overlap > maxOverlap) return 0;
      if ((object1.allowCollisions & FlixelConstants.Physics.LEFT) == 0
        || (object2.allowCollisions & FlixelConstants.Physics.RIGHT) == 0) return 0;
      object1.touching |= FlixelConstants.Physics.LEFT;
      object2.touching |= FlixelConstants.Physics.RIGHT;
    }

    return overlap;
  }

  /**
   * Computes the overlap between two objects on the Y axis and updates their
   * {@link #touching} flags. Same semantics as {@link #computeOverlapX}.
   *
   * @param object1 First object.
   * @param object2 Second object.
   * @param checkMaxOverlap Whether to reject overlaps greater than combined movement + {@code SEPARATE_BIAS}.
   * @return Signed overlap in pixels (positive means object1's bottom side penetrates object2's top side).
   * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html#computeOverlapY">FlxObject.computeOverlapY</a>
   */
  public static float computeOverlapY(FlixelObject object1, FlixelObject object2, boolean checkMaxOverlap) {
    float obj1delta = object1.y - object1.lastY;
    float obj2delta = object2.y - object2.lastY;

    if (obj1delta == obj2delta) {
      return 0;
    }

    float obj1deltaAbs = Math.abs(obj1delta);
    float obj2deltaAbs = Math.abs(obj2delta);

    float rect1x = object1.lastX;
    float rect1w = object1.width;
    float rect1y = object1.y - (obj1delta > 0 ? obj1delta : 0);
    float rect1h = object1.height + obj1deltaAbs;

    float rect2x = object2.lastX;
    float rect2w = object2.width;
    float rect2y = object2.y - (obj2delta > 0 ? obj2delta : 0);
    float rect2h = object2.height + obj2deltaAbs;

    if (rect1x + rect1w <= rect2x || rect1x >= rect2x + rect2w
      || rect1y + rect1h <= rect2y || rect1y >= rect2y + rect2h) {
      return 0;
    }

    float maxOverlap = checkMaxOverlap ? (obj1deltaAbs + obj2deltaAbs + FlixelConstants.Physics.SEPARATE_BIAS) : 0;
    float overlap;

    // libGDX Y-up: positive delta = moving up -> object1's top hits object2's bottom.
    if (obj1delta > obj2delta) {
      overlap = object1.y + object1.height - object2.y;
      if (checkMaxOverlap && overlap > maxOverlap) return 0;
      if ((object1.allowCollisions & FlixelConstants.Physics.UP) == 0
        || (object2.allowCollisions & FlixelConstants.Physics.DOWN) == 0) return 0;
      object1.touching |= FlixelConstants.Physics.UP;
      object2.touching |= FlixelConstants.Physics.DOWN;
    } else {
      overlap = object1.y - object2.height - object2.y;
      if (checkMaxOverlap && -overlap > maxOverlap) return 0;
      if ((object1.allowCollisions & FlixelConstants.Physics.DOWN) == 0
        || (object2.allowCollisions & FlixelConstants.Physics.UP) == 0) return 0;
      object1.touching |= FlixelConstants.Physics.DOWN;
      object2.touching |= FlixelConstants.Physics.UP;
    }

    return overlap;
  }

  /**
   * Checks if two objects overlap and updates their {@link #touching} flags
   * without separating them.
   *
   * @param object1 First object.
   * @param object2 Second object.
   *
   * @return {@code true} if the two objects are touching.
   */
  public static boolean updateTouchingFlags(FlixelObject object1, FlixelObject object2) {
    boolean x = computeOverlapX(object1, object2, false) != 0;
    boolean y = computeOverlapY(object1, object2, false) != 0;
    return x || y;
  }

  @Override
  public float getDebugX() {
    return x;
  }

  @Override
  public float getDebugY() {
    return y;
  }

  @Override
  public float getDebugWidth() {
    return width;
  }

  @Override
  public float getDebugHeight() {
    return height;
  }

  /**
   * Returns the appropriate debug color based on collision state:
   * <ul>
   *   <li>Override color if set</li>
   *   <li>Blue when {@code allowCollisions == NONE}</li>
   *   <li>Green when {@code immovable} or partial collision</li>
   *   <li>Red (normal/solid) otherwise</li>
   * </ul>
   */
  @Override
  public float[] getDebugBoundingBoxColor() {
    if (debugColorOverride != null) return debugColorOverride;
    if (allowCollisions == FlixelConstants.Physics.NONE) return debugColorNoCollision;
    if (immovable || allowCollisions != FlixelConstants.Physics.ANY) return debugColorImmovable;
    return debugColorSolid;
  }

  /**
   * Forces a specific debug bounding box color, overriding the automatic
   * selection. Pass {@code null} to re-enable automatic color selection.
   *
   * @param rgba 4-element float array {@code [r, g, b, a]}, or {@code null}.
   */
  public void setDebugBoundingBoxColor(float[] rgba) {
    this.debugColorOverride = rgba;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(ID=" + ID
      + ", x=" + x + ", y=" + y
      + ", w=" + width + ", h=" + height + ")";
  }
}
