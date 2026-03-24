package me.stringdotjar.flixelgdx.box2d;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.util.FlixelConstants;

/**
 * Opt-in interface for integrating libGDX Box2D physics with any {@link FlixelObject}
 * subclass. Implementors get a managed {@link Body} whose position and angle are
 * automatically synced back to the object's {@code x}, {@code y}, and {@code angle}
 * each frame (via {@link #syncFromBody()}).
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * public class Crate extends FlixelSprite implements FlixelBox2DObject {
 *   private Body body;
 *
 *   public Body getBody() { return body; }
 *   public void setBody(Body body) { this.body = body; }
 *
 *   public void create() {
 *     loadGraphic(...);
 *     createRectangularBody(Flixel.getWorld());
 *   }
 * }
 * }</pre>
 *
 * <h3>Pixel-to-Meter Scale</h3>
 * All default helpers use {@link FlixelConstants.Physics#PIXELS_PER_METER}
 * (100 px = 1 m). Override individual methods if your game uses a different scale.
 *
 * <h3>Motion</h3>
 * When a Box2D body is present, the implementor should <em>not</em> rely on
 * {@link FlixelObject#updateMotion(float)} for movement; instead the
 * {@link World} step drives the body and {@link #syncFromBody()} copies the result
 * back to the object. If you still set {@code moves = true} alongside a body, the
 * custom integration will run <em>after</em> the body sync and may produce
 * unexpected results.
 *
 * @see <a href="https://libgdx.com/wiki/extensions/physics/box2d">libGDX Box2D</a>
 */
public interface FlixelBox2DObject {

  /**
   * Returns the Box2D body attached to this object, or {@code null} if no body
   * has been created yet.
   */
  Body getBody();

  /**
   * Stores a reference to the given Box2D body. Called by the default
   * {@code createRectangularBody} helpers; implementors must provide the
   * backing field.
   */
  void setBody(Body body);

  /**
   * Creates a dynamic rectangular body from the object's current
   * {@code x}, {@code y}, {@code width}, and {@code height}.
   *
   * @param world The Box2D world to create the body in.
   * @return The created body, or {@code null} if {@code this} is not a {@link FlixelObject}.
   */
  default Body createRectangularBody(World world) {
    BodyDef def = new BodyDef();
    def.type = BodyDef.BodyType.DynamicBody;
    return createRectangularBody(world, def);
  }

  /**
   * Creates a rectangular body with a custom {@link BodyDef} from the object's
   * current dimensions. A default box {@link PolygonShape} and
   * {@link FixtureDef} (density = 1, friction = 0.3, restitution = 0) are applied. Override
   * {@link #createRectangularBody(World, BodyDef, FixtureDef)} for full control.
   *
   * @param world The Box2D world.
   * @param bodyDef The body definition to use.
   * @return The created body, or {@code null} if {@code this} is not a {@link FlixelObject}.
   */
  default Body createRectangularBody(World world, BodyDef bodyDef) {
    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.density = 1f;
    fixtureDef.friction = 0.3f;
    fixtureDef.restitution = 0f;
    return createRectangularBody(world, bodyDef, fixtureDef);
  }

  /**
   * Creates a rectangular body with full control over body and fixture
   * definitions. The body is positioned at the center of the object's
   * bounding box (converted to meters) and a box shape is created from the
   * object's width and height.
   *
   * @param world The Box2D world.
   * @param bodyDef The body definition.
   * @param fixtureDef The fixture definition. The shape will be set automatically.
   * @return The created body, or {@code null} if {@code this} is not a {@link FlixelObject}.
   */
  default Body createRectangularBody(World world, BodyDef bodyDef, FixtureDef fixtureDef) {
    if (!(this instanceof FlixelObject obj)) {
      return null;
    }

    float ppm = FlixelConstants.Physics.PIXELS_PER_METER;
    float halfW = (obj.getWidth() / ppm) / 2f;
    float halfH = (obj.getHeight() / ppm) / 2f;
    float centerX = (obj.getX() + obj.getWidth() / 2f) / ppm;
    float centerY = (obj.getY() + obj.getHeight() / 2f) / ppm;

    bodyDef.position.set(centerX, centerY);

    Body body = world.createBody(bodyDef);

    PolygonShape shape = new PolygonShape();
    shape.setAsBox(halfW, halfH);
    fixtureDef.shape = shape;
    body.createFixture(fixtureDef);
    shape.dispose();

    setBody(body);
    return body;
  }

  /**
   * Copies the Box2D body's position and angle back to the {@link FlixelObject}'s
   * {@code x}, {@code y}, and {@code angle}. Called automatically by the engine
   * after each world step for every active {@code FlixelBox2DObject}.
   *
   * <p>Box2D positions represent the body <em>center</em>; this method converts
   * to the top-left convention used by {@link FlixelObject}.
   */
  default void syncFromBody() {
    if (!(this instanceof FlixelObject obj)) {
      return;
    }

    Body body = getBody();
    if (body == null) {
      return;
    }

    float ppm = FlixelConstants.Physics.PIXELS_PER_METER;
    Vector2 pos = body.getPosition();
    obj.setX(pos.x * ppm - obj.getWidth() / 2f);
    obj.setY(pos.y * ppm - obj.getHeight() / 2f);
    obj.setAngle((float) Math.toDegrees(body.getAngle()));
  }

  /**
   * Copies the {@link FlixelObject}'s current {@code x}, {@code y}, and
   * {@code angle} to the Box2D body. Useful after teleporting an object.
   */
  default void syncToBody() {
    if (!(this instanceof FlixelObject obj)) {
      return;
    }

    Body body = getBody();
    if (body == null) {
      return;
    }

    float ppm = FlixelConstants.Physics.PIXELS_PER_METER;
    float centerX = (obj.getX() + obj.getWidth() / 2f) / ppm;
    float centerY = (obj.getY() + obj.getHeight() / 2f) / ppm;
    body.setTransform(centerX, centerY, (float) Math.toRadians(obj.getAngle()));
  }

  /**
   * Applies the object's current {@code velocityX} / {@code velocityY} to the
   * body as a linear velocity (converted from px/s to m/s).
   */
  default void applyVelocityToBody() {
    if (!(this instanceof FlixelObject obj)) {
      return;
    }

    Body body = getBody();
    if (body == null) {
      return;
    }

    float ppm = FlixelConstants.Physics.PIXELS_PER_METER;
    body.setLinearVelocity(obj.getVelocityX() / ppm, obj.getVelocityY() / ppm);
  }

  /**
   * Applies an impulse at the body's center of mass.
   *
   * @param impulseX Horizontal impulse in Newton-seconds.
   * @param impulseY Vertical impulse in Newton-seconds.
   */
  default void applyLinearImpulse(float impulseX, float impulseY) {
    Body body = getBody();
    if (body == null) {
      return;
    }
    body.applyLinearImpulse(impulseX, impulseY, body.getWorldCenter().x, body.getWorldCenter().y, true);
  }

  /**
   * Applies a force at the body's center of mass.
   *
   * @param forceX Horizontal force in Newtons.
   * @param forceY Vertical force in Newtons.
   */
  default void applyForce(float forceX, float forceY) {
    Body body = getBody();
    if (body == null) {
      return;
    }
    body.applyForceToCenter(forceX, forceY, true);
  }

  /**
   * Destroys the body in the given world and sets the internal reference to
   * {@code null}. Safe to call when the body is already {@code null}.
   *
   * @param world The Box2D world the body belongs to.
   */
  default void destroyBody(World world) {
    Body body = getBody();
    if (body == null) {
      return;
    }
    world.destroyBody(body);
    setBody(null);
  }
}
