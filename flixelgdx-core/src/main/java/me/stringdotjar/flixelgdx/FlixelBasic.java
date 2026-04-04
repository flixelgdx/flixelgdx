/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

import org.jetbrains.annotations.Nullable;

/**
 * This is the most generic Flixel object. Both {@link FlixelObject} and {@link FlixelCamera}
 * extend this class. It has no size, position, or graphical data, only lifecycle flags and
 * a unique ID.
 *
 * <p>Prefer {@link #kill()} when an object should stop updating and drawing but might be {@link #revive()}d later
 * (bullets, particles, pooled gameplay objects). Call {@link #destroy()} when you are done with the instance for good:
 * it clears lifecycle state and, in subclasses such as {@link FlixelSprite}, releases graphics and
 * other resources. {@link #dispose()} and {@link #reset()} (for {@link com.badlogic.gdx.utils.Pool}) delegate to
 * {@link #destroy()}, which aligns with libGDX&rsquo;s {@link com.badlogic.gdx.utils.Disposable} expectations.
 *
 * <table border="1">
 *   <caption><strong>Lifecycle cheat sheet</strong></caption>
 *   <thead>
 *     <tr><th scope="col">Situation</th><th scope="col">Use</th><th scope="col">Avoid</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>Temporarily hide/disable; same object will spawn again</td>
 *       <td>{@link #kill()} then later {@link #revive()}</td>
 *       <td>{@link #destroy()} (drops resources you may still want)</td>
 *     </tr>
 *     <tr>
 *       <td>Reuse a &ldquo;dead&rdquo; slot in a {@link me.stringdotjar.flixelgdx.group.FlixelBasicGroup}</td>
 *       <td>{@link me.stringdotjar.flixelgdx.group.FlixelBasicGroup#recycle()} or {@link #revive()} after {@link #kill()}</td>
 *       <td>{@link #destroy()} unless you truly discard the instance</td>
 *     </tr>
 *     <tr>
 *       <td>Remove from group only; you still hold the reference</td>
 *       <td>{@link me.stringdotjar.flixelgdx.group.FlixelBasicGroup#remove} / {@link me.stringdotjar.flixelgdx.group.FlixelGroupable#detach}</td>
 *       <td>Assuming the group calls {@link #destroy()} for you (it does not)</td>
 *     </tr>
 *     <tr>
 *       <td>Object leaves the game for good (state end, level unload, texture swap)</td>
 *       <td>{@link #destroy()} (and remove from any group first if needed)</td>
 *       <td>{@link #kill()} alone (resources may leak until something calls {@link #destroy()})</td>
 *     </tr>
 *     <tr>
 *       <td>Container shut down ({@link me.stringdotjar.flixelgdx.group.FlixelBasicGroup#destroy()}, {@link FlixelState#destroy()})</td>
 *       <td>Let the group/state call {@link #destroy()} on each member</td>
 *       <td>Relying on {@link #kill()} for GPU/native cleanup</td>
 *     </tr>
 *     <tr>
 *       <td>Returning instance to a {@link com.badlogic.gdx.utils.Pool}</td>
 *       <td>{@code pool.free(object)} (invokes {@link #reset()} -> {@link #destroy()})</td>
 *       <td>Expecting {@link #kill()} to run pool reset logic</td>
 *     </tr>
 *   </tbody>
 * </table>
 */
public class FlixelBasic implements FlixelUpdatable, FlixelDrawable, FlixelDestroyable, Disposable, Pool.Poolable {

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

  /** Cameras this object may render on. {@code null} or an empty array means every camera. */
  @Nullable
  public FlixelCamera[] cameras;

  public FlixelBasic() {
    this.ID = idEnumerator++;
    this.cameras = null;
  }

  /**
   * Updates the logic of {@code this} FlixelBasic.
   *
   * <p>Override this function to update your object's position and appearance.
   * This is where most game rules and behavioral code will go.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {}

  /**
   * Override this function to control how the object is drawn. Doing so is rarely necessary
   * but can be very useful.
   *
   * @param batch The batch used for rendering.
   */
  @Override
  public void draw(Batch batch) {}

  /**
   * Whether this object should render in the current {@link FlixelGame} camera pass.
   */
  protected boolean isOnDrawCamera() {
    return Flixel.isOnDrawCamera(cameras);
  }

  /**
   * Cleans up this object so it can be garbage-collected. A destroyed {@code FlixelBasic}
   * should not be used anymore. Use {@link #kill()} if you only want to disable it
   * temporarily and {@link #revive()} it later.
   *
   * <p>Override this function to clean up any resources used by this object,
   * such as textures, fonts, sounds, etc.
   *
   * <p>This function is called automatically when {@link #dispose()} or
   * {@link #reset()} is executed, so you don't need to call it manually.
   *
   * @see #dispose()
   * @see #reset()
   */
  @Override
  public void destroy() {
    active = false;
    alive = true;
    exists = false;
    visible = true;
    cameras = null;
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

  /**
   * Automatically calls {@link #destroy()}. Marked as final to prevent subclasses from overriding it,
   * they should call {@link #destroy()} instead.
   */
  @Override
  public final void dispose() {
    destroy();
  }

  /**
   * Automatically calls {@link #destroy()}. Marked as final to prevent subclasses from overriding it,
   * they should call {@link #destroy()} instead.
   */
  @Override
  public final void reset() {
    destroy();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(ID=" + ID + ")";
  }
}
