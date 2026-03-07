package me.stringdotjar.flixelgdx;

/**
 * The base class for all visual/spatial objects in Flixel. Extends {@link FlixelBasic} with
 * position ({@link #x}, {@link #y}), dimensions ({@link #width}, {@link #height}), and
 * rotation ({@link #angle}).
 *
 * <p>Most games interact with this through {@link me.stringdotjar.flixelgdx.FlixelSprite},
 * which adds graphical capabilities on top of this spatial foundation.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxObject.html">FlxObject (HaxeFlixel)</a>
 */
public class FlixelObject extends FlixelBasic {

  /** X position of the upper left corner of this object in world space. */
  private float x = 0f;

  /** Y position of the upper left corner of this object in world space. */
  private float y = 0f;

  /** The width of this object's hitbox. */
  private float width = 0f;

  /** The height of this object's hitbox. */
  private float height = 0f;

  /** The angle (in degrees) of this object. Used for visual rotation in sprites. */
  private float angle = 0f;

  /** The current velocity of this object in pixels-per-second. */
  protected float velocityX = 0f;

  /** The current velocity of this object in pixels-per-second. */
  protected float velocityY = 0f;

  /** The current acceleration of this object in pixels-per-second squared. */
  protected float accelerationX = 0f;

  /** The current acceleration of this object in pixels-per-second squared. */
  protected float accelerationY = 0f;

  /** The current drag of this object in pixels-per-second squared. */
  protected float dragX = 0f;

  /** The current drag of this object in pixels-per-second squared. */
  protected float dragY = 0f;

  /** The maximum velocity this object can reach in pixels-per-second. */
  protected float maxVelocityX = 10000f;

  /** The maximum velocity this object can reach in pixels-per-second. */
  protected float maxVelocityY = 10000f;

  /** The current angular velocity of this object in degrees-per-second. */
  protected float angularVelocity = 0f;

  /** The current angular acceleration of this object in degrees-per-second squared. */
  protected float angularAcceleration = 0f;

  /** The current angular drag of this object in degrees-per-second squared. */
  protected float angularDrag = 0f;

  /** The maximum angular velocity of this object can reach in degrees-per-second. */
  protected float maxAngularVelocity = 10000f;

  /** Whether this object is affected by its velocity and acceleration. */
  protected boolean moves = true;

  public FlixelObject() {
    super();
  }

  public FlixelObject(float x, float y) {
    super();
    this.x = x;
    this.y = y;
  }

  public FlixelObject(float x, float y, float width, float height) {
    super();
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
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

  /**
   * Internal function for updating the position and speed of this object.
   * Useful for advanced velocity-based movement.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  public void updateMotion(float elapsed) {
    float velocityDelta = 0.5f * (computeVelocity(angularVelocity, angularAcceleration, angularDrag, maxAngularVelocity, elapsed) - angularVelocity);
    angularVelocity += velocityDelta;
    angle += angularVelocity * elapsed;
    angularVelocity += velocityDelta;

    velocityDelta = 0.5f * (computeVelocity(velocityX, accelerationX, dragX, maxVelocityX, elapsed) - velocityX);
    velocityX += velocityDelta;
    float delta = velocityX * elapsed;
    velocityX += velocityDelta;
    x += delta;

    velocityDelta = 0.5f * (computeVelocity(velocityY, accelerationY, dragY, maxVelocityY, elapsed) - velocityY);
    velocityY += velocityDelta;
    delta = velocityY * elapsed;
    velocityY += velocityDelta;
    y += delta;
  }

  /**
   * Internal function for computing the velocity of an object.
   *
   * @param velocity Current velocity of the object.
   * @param acceleration Acceleration of the object.
   * @param drag Drag of the object.
   * @param max Max velocity of the object.
   * @param elapsed Seconds elapsed since the last frame.
   * @return The new velocity of the object.
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(ID=" + ID
      + ", x=" + x + ", y=" + y
      + ", w=" + width + ", h=" + height + ")";
  }
}
