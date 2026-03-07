package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.graphics.g2d.Batch;

import me.stringdotjar.flixelgdx.display.FlixelCamera;

/**
 * This is the most generic Flixel object. Both {@link FlixelObject} and {@link FlixelCamera}
 * extend this class. It has no size, position, or graphical data, only lifecycle flags and
 * a unique ID.
 *
 * @see <a href="https://api.haxeflixel.com/flixel/FlxBasic.html">FlxBasic (HaxeFlixel)</a>
 */
public class FlixelBasic {

  private static int idEnumerator = 0;

  /** A unique ID starting from 0 and increasing by 1 for each subsequent {@code FlixelBasic} created. */
  public final int ID;

  /** Controls whether {@link #update(float)} is automatically called. */
  public boolean active = true;

  /**
   * Whether this object is alive. {@link #kill()} and {@link #revive()} both flip this
   * switch (along with {@link #exists}).
   */
  public boolean alive = true;

  /** Controls whether {@link #update(float)} and {@link #draw(Batch)} are automatically called. */
  public boolean exists = true;

  /** Controls whether {@link #draw(Batch)} is automatically called. */
  public boolean visible = true;

  public FlixelBasic() {
    this.ID = idEnumerator++;
  }

  /**
   * Updates the logic of {@code this} FlixelBasic.
   *
   * <p>Override this function to update your object's position and appearance.
   * This is where most game rules and behavioral code will go.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  public void update(float elapsed) {}

  /**
   * Override this function to control how the object is drawn. Doing so is rarely necessary
   * but can be very useful.
   *
   * @param batch The batch used for rendering.
   */
  public void draw(Batch batch) {}

  /**
   * Cleans up this object so it can be garbage-collected. A destroyed {@code FlixelBasic}
   * should not be used anymore. Use {@link #kill()} if you only want to disable it
   * temporarily and {@link #revive()} it later.
   */
  public void destroy() {
    exists = false;
    active = false;
  }

  /**
   * Flags this object as nonexistent and dead. Default behavior sets both {@link #alive}
   * and {@link #exists} to {@code false}. Use {@link #revive()} to bring it back.
   */
  public void kill() {
    alive = false;
    exists = false;
  }

  /**
   * Brings this object back to life by setting {@link #alive} and {@link #exists} to
   * {@code true}.
   */
  public void revive() {
    alive = true;
    exists = true;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(ID=" + ID + ")";
  }
}
