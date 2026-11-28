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
package org.flixelgdx;

import org.flixelgdx.backend.FlixelBootManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.functional.FlixelCreatable;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.signal.FlixelSignalData.StateSwitchSignalData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The global registry for any component that needs to be created, updated, drawn, and destroyed
 * in the global lifecycle of the game loop.
 *
 * <p>Think of it like a subway train and its engine: it always goes to the same locations and
 * drives the power of the train itself, but people can board it and tag along with it to go
 * through the same paths to get to their desired location.
 *
 * <p>This class and its interface {@link Manager} work just like that. {@link FlixelManagers}
 * hooks into every step of the global lifecycle (creation, every part of the update and draw
 * passes, resizing, focus changes, pausing, state switches, and destruction), and any class that
 * implements {@link Manager} can tag right along with it, without subclassing {@link FlixelGame}
 * or weird hacks. Every hook has an empty default, so a manager only overrides the ones it needs.
 *
 * <p>Managers run in a fixed order. Each one has an {@linkplain Manager#getOrder() order}, and
 * every hook visits the managers from the lowest order to the highest. Managers with the same
 * order run in the order they were added. {@link #destroy()} is the one exception: it runs from
 * the highest order to the lowest, so a manager is always destroyed before the managers it was
 * created after.
 *
 * <p>If you're planning to make a library or extension (or want to make something that hooks
 * into the lifecycle of the engine), this is the place to do it. It's advised that you configure
 * and register your managers at boot time before the game starts; a {@link FlixelBootManager boot callback}
 * is a good place to do so. A manager added while the game is already running is created right
 * away, and a manager removed while the game is running is destroyed right away.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyManager implements FlixelManagers.Manager {
 *
 *   @Override
 *   public void create() {
 *     // Configure values here.
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     // Update logic here. This runs before the state updates.
 *   }
 *
 *   @Override
 *   public void destroy() {
 *     // Cleanup logic here.
 *   }
 * }
 *
 * // Then during the boot sequence, add your manager. It will now be driven by the same exact
 * // lifecycle the framework and its engine use.
 * Flixel.boot.afterStart(() -> Flixel.managers.add(new MyManager()));
 * }</pre>
 *
 * <p>The framework calls the hook methods of this class (such as {@link #update(float, boolean)} or
 * {@link #drawCamera(FlixelCamera, FlixelBatch)}) at the matching point of every frame. Game code
 * normally never calls them itself.
 */
public class FlixelManagers implements FlixelCreatable, FlixelUpdatable, FlixelDestroyable {

  private static final int CREATE = 0;
  private static final int PRE_UPDATE = 1;
  private static final int UPDATE = 2;
  private static final int POST_UPDATE = 3;
  private static final int PRE_DRAW = 4;
  private static final int DRAW_CAMERA = 5;
  private static final int DRAW_OVERLAY = 6;
  private static final int POST_DRAW = 7;
  private static final int END_FRAME = 8;
  private static final int RESIZE = 9;
  private static final int FOCUS_LOST = 10;
  private static final int FOCUS_GAINED = 11;
  private static final int MINIMIZED = 12;
  private static final int SUSPENDED = 13;
  private static final int PAUSED = 14;
  private static final int PRE_STATE_SWITCH = 15;
  private static final int POST_STATE_SWITCH = 16;

  /** Every registered slot, sorted by order. Removed slots stay here until the next flush. */
  private final FlixelArray<Slot> slots = new FlixelArray<>(32);

  /** Slots added while a hook was running, inserted into {@link #slots} once it finishes. */
  private final FlixelArray<Slot> pending = new FlixelArray<>(4);

  /** How many hooks are running right now. Hooks can nest, for example a state switch inside an update. */
  private int depth;

  /** Whether {@link #create()} ran and {@link #destroy()} has not run since. */
  private boolean created;

  /** Whether a slot was removed while a hook was running and still has to be dropped from {@link #slots}. */
  private boolean dirty;

  /**
   * Adds a manager, keeping the list sorted by {@link Manager#getOrder()}.
   *
   * <p>The order is read once, right here, so changing what {@code getOrder()} returns later has
   * no effect. Adding a manager that is already registered does nothing. If the game is already
   * running, the manager is created right away.
   *
   * @param manager The manager to add.
   */
  public void add(@NotNull Manager manager) {
    Objects.requireNonNull(manager, "The manager cannot be null.");
    if (contains(manager)) {
      return;
    }
    register(new Slot(manager, null, manager.getOrder()));
  }

  /**
   * Adds a slot whose manager is looked up from {@code source} every time a hook runs.
   *
   * <p>Use this when the manager lives in a field that can be replaced, such as {@link Flixel#sound}.
   * The slot always reaches whatever object the field holds at that moment, so swapping the field
   * never leaves a stale manager behind. When {@code source} returns {@code null}, the slot is
   * skipped. Adding the same source twice does nothing.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.managers.add(() -> MyLibrary.instance, FlixelManagers.Manager.ORDER_DEFAULT);
   * }</pre>
   *
   * @param source Supplies the manager for this slot. It may return {@code null}.
   * @param order Where the slot runs, as described in {@link Manager#getOrder()}.
   */
  public void add(@NotNull Supplier<? extends Manager> source, int order) {
    Objects.requireNonNull(source, "The manager source cannot be null.");
    if (contains(source)) {
      return;
    }
    register(new Slot(null, source, order));
  }

  /**
   * Removes a manager added with {@link #add(Manager)}. If the game is running, the manager is
   * destroyed right away. Removing a manager that is not registered does nothing.
   *
   * @param manager The manager to remove.
   */
  public void remove(@NotNull Manager manager) {
    Objects.requireNonNull(manager, "The manager cannot be null.");
    unregister(find(manager, null));
  }

  /**
   * Removes a slot added with {@link #add(Supplier, int)}. If the game is running, the manager the
   * slot currently supplies is destroyed right away. Removing a source that is not registered does
   * nothing.
   *
   * @param source The source to remove.
   */
  public void remove(@NotNull Supplier<? extends Manager> source) {
    Objects.requireNonNull(source, "The manager source cannot be null.");
    unregister(find(null, source));
  }

  /**
   * Returns whether the given manager was added with {@link #add(Manager)} and not removed since.
   *
   * @param manager The manager to look for.
   * @return {@code true} if the manager is registered.
   */
  public boolean contains(@Nullable Manager manager) {
    return manager != null && find(manager, null) != null;
  }

  /**
   * Returns whether the given source was added with {@link #add(Supplier, int)} and not removed since.
   *
   * @param source The source to look for.
   * @return {@code true} if the source is registered.
   */
  public boolean contains(@Nullable Supplier<? extends Manager> source) {
    return source != null && find(null, source) != null;
  }

  /**
   * Creates every manager, from the lowest order to the highest.
   *
   * <p>{@link FlixelGame#create()} calls this. Calling it again before {@link #destroy()} does nothing.
   */
  @Override
  public void create() {
    if (created) {
      return;
    }
    created = true;
    dispatch(CREATE, 0f, false, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#preUpdate(float)} on every manager, at the very start of the update pass.
   *
   * @param elapsed The scaled seconds since the last frame.
   * @param paused Whether the game is paused. Managers whose {@link Manager#isPausable()} returns
   *     {@code true} are skipped while it is.
   */
  public void preUpdate(float elapsed, boolean paused) {
    dispatch(PRE_UPDATE, elapsed, paused, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#update(float)} on every manager, ignoring whether the game is paused.
   *
   * @param elapsed The scaled seconds since the last frame.
   */
  @Override
  public void update(float elapsed) {
    update(elapsed, false);
  }

  /**
   * Runs {@link Manager#update(float)} on every manager, right before the state updates.
   *
   * @param elapsed The scaled seconds since the last frame.
   * @param paused Whether the game is paused. Managers whose {@link Manager#isPausable()} returns
   *     {@code true} are skipped while it is.
   */
  public void update(float elapsed, boolean paused) {
    dispatch(UPDATE, elapsed, paused, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#postUpdate(float)} on every manager, after the state and cameras updated.
   *
   * @param elapsed The scaled seconds since the last frame.
   * @param paused Whether the game is paused. Managers whose {@link Manager#isPausable()} returns
   *     {@code true} are skipped while it is.
   */
  public void postUpdate(float elapsed, boolean paused) {
    dispatch(POST_UPDATE, elapsed, paused, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#preDraw(FlixelBatch)} on every manager, before anything is drawn this frame.
   *
   * @param batch The framework batch. It has not been begun.
   */
  public void preDraw(@NotNull FlixelBatch batch) {
    dispatch(PRE_DRAW, 0f, false, batch, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#drawCamera(FlixelCamera, FlixelBatch)} on every manager, inside one camera's pass.
   *
   * @param camera The camera being drawn.
   * @param batch The framework batch, already begun with the camera's projection.
   */
  public void drawCamera(@NotNull FlixelCamera camera, @NotNull FlixelBatch batch) {
    dispatch(DRAW_CAMERA, 0f, false, batch, camera, 0, 0, null);
  }

  /**
   * Runs {@link Manager#drawOverlay(FlixelBatch)} on every manager, on top of every camera.
   *
   * @param batch The framework batch, already begun with the overlay camera's projection.
   */
  public void drawOverlay(@NotNull FlixelBatch batch) {
    dispatch(DRAW_OVERLAY, 0f, false, batch, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#postDraw(FlixelBatch)} on every manager, after the frame was drawn.
   *
   * @param batch The framework batch. It has not been begun.
   */
  public void postDraw(@NotNull FlixelBatch batch) {
    dispatch(POST_DRAW, 0f, false, batch, null, 0, 0, null);
  }

  /** Runs {@link Manager#endFrame()} on every manager, after the frame was updated and drawn. */
  public void endFrame() {
    dispatch(END_FRAME, 0f, false, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#resize(int, int)} on every manager.
   *
   * @param width The new surface width in pixels.
   * @param height The new surface height in pixels.
   */
  public void resize(int width, int height) {
    dispatch(RESIZE, 0f, false, null, null, width, height, null);
  }

  /** Runs {@link Manager#onFocusLost()} on every manager. */
  public void onFocusLost() {
    dispatch(FOCUS_LOST, 0f, false, null, null, 0, 0, null);
  }

  /** Runs {@link Manager#onFocusGained()} on every manager. */
  public void onFocusGained() {
    dispatch(FOCUS_GAINED, 0f, false, null, null, 0, 0, null);
  }

  /** Runs {@link Manager#onMinimized()} on every manager. */
  public void onMinimized() {
    dispatch(MINIMIZED, 0f, false, null, null, 0, 0, null);
  }

  /** Runs {@link Manager#onSuspended()} on every manager. */
  public void onSuspended() {
    dispatch(SUSPENDED, 0f, false, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#onPaused(boolean)} on every manager.
   *
   * @param paused {@code true} when the game was just paused, {@code false} when it was resumed.
   */
  public void onPaused(boolean paused) {
    dispatch(PAUSED, 0f, paused, null, null, 0, 0, null);
  }

  /**
   * Runs {@link Manager#preStateSwitch(StateSwitchSignalData)} on every manager.
   *
   * @param data Holds the state that is about to be destroyed.
   */
  public void preStateSwitch(@NotNull StateSwitchSignalData data) {
    dispatch(PRE_STATE_SWITCH, 0f, false, null, null, 0, 0, data);
  }

  /**
   * Runs {@link Manager#postStateSwitch(StateSwitchSignalData)} on every manager.
   *
   * @param data Holds the state that was just created.
   */
  public void postStateSwitch(@NotNull StateSwitchSignalData data) {
    dispatch(POST_STATE_SWITCH, 0f, false, null, null, 0, 0, data);
  }

  /**
   * Destroys every manager, from the highest order to the lowest.
   *
   * <p>{@link FlixelGame#destroy()} calls this. The managers stay registered, so the next
   * {@link #create()} creates them again. Calling this when the managers were not created does
   * nothing.
   */
  @Override
  public void destroy() {
    if (!created) {
      return;
    }
    created = false;
    depth++;
    try {
      for (int i = slots.getSize() - 1; i >= 0; i--) {
        Slot slot = slots.get(i);
        Manager manager = slot.resolve();
        if (!slot.removed && manager != null) {
          manager.destroy();
        }
      }
    } finally {
      finishDispatch();
    }
  }

  private void register(Slot slot) {
    if (depth > 0) {
      pending.add(slot);
    } else {
      insert(slot);
    }
    if (created) {
      Manager manager = slot.resolve();
      if (manager != null) {
        manager.create();
      }
    }
  }

  private void unregister(@Nullable Slot slot) {
    if (slot == null) {
      return;
    }
    slot.removed = true;
    if (created) {
      Manager manager = slot.resolve();
      if (manager != null) {
        manager.destroy();
      }
    }
    if (depth > 0) {
      dirty = true;
    } else {
      compact();
    }
  }

  private void insert(Slot slot) {
    int index = slots.getSize();
    for (int i = 0, n = slots.getSize(); i < n; i++) {
      if (slots.get(i).order > slot.order) {
        index = i;
        break;
      }
    }
    slots.insert(index, slot);
  }

  private void compact() {
    for (int i = slots.getSize() - 1; i >= 0; i--) {
      if (slots.get(i).removed) {
        slots.removeIndex(i);
      }
    }
    dirty = false;
  }

  @Nullable
  private Slot find(@Nullable Manager manager, @Nullable Supplier<? extends Manager> source) {
    Slot slot = find(slots, manager, source);
    return slot != null ? slot : find(pending, manager, source);
  }

  @Nullable
  private static Slot find(FlixelArray<Slot> list, @Nullable Manager manager,
      @Nullable Supplier<? extends Manager> source) {
    for (int i = 0, n = list.getSize(); i < n; i++) {
      Slot slot = list.get(i);
      if (slot.removed) {
        continue;
      }
      if ((manager != null && slot.manager == manager) || (source != null && slot.source == source)) {
        return slot;
      }
    }
    return null;
  }

  private void dispatch(int hook, float elapsed, boolean flag, @Nullable FlixelBatch batch,
      @Nullable FlixelCamera camera, int width, int height, @Nullable StateSwitchSignalData data) {
    boolean updatePass = hook == PRE_UPDATE || hook == UPDATE || hook == POST_UPDATE;
    depth++;
    try {
      // The size is read every time so a slot inserted by a nested flush is still reached safely.
      for (int i = 0; i < slots.getSize(); i++) {
        Slot slot = slots.get(i);
        if (slot.removed) {
          continue;
        }
        Manager manager = slot.resolve();
        if (manager == null || (updatePass && flag && manager.isPausable())) {
          continue;
        }
        switch (hook) {
          case CREATE -> manager.create();
          case PRE_UPDATE -> manager.preUpdate(elapsed);
          case UPDATE -> manager.update(elapsed);
          case POST_UPDATE -> manager.postUpdate(elapsed);
          case PRE_DRAW -> manager.preDraw(batch);
          case DRAW_CAMERA -> manager.drawCamera(camera, batch);
          case DRAW_OVERLAY -> manager.drawOverlay(batch);
          case POST_DRAW -> manager.postDraw(batch);
          case END_FRAME -> manager.endFrame();
          case RESIZE -> manager.resize(width, height);
          case FOCUS_LOST -> manager.onFocusLost();
          case FOCUS_GAINED -> manager.onFocusGained();
          case MINIMIZED -> manager.onMinimized();
          case SUSPENDED -> manager.onSuspended();
          case PAUSED -> manager.onPaused(flag);
          case PRE_STATE_SWITCH -> manager.preStateSwitch(data);
          case POST_STATE_SWITCH -> manager.postStateSwitch(data);
          default -> throw new IllegalStateException("Unknown manager hook: " + hook);
        }
      }
    } finally {
      finishDispatch();
    }
  }

  private void finishDispatch() {
    depth--;
    if (depth > 0) {
      return;
    }
    if (dirty) {
      compact();
    }
    for (int i = 0, n = pending.getSize(); i < n; i++) {
      Slot slot = pending.get(i);
      if (!slot.removed) {
        insert(slot);
      }
    }
    pending.clear();
  }

  /**
   * Returns the number of registered managers and slots.
   *
   * @return How many managers are registered.
   */
  public int getSize() {
    int size = 0;
    for (int i = 0, n = slots.getSize(); i < n; i++) {
      if (!slots.get(i).removed) {
        size++;
      }
    }
    for (int i = 0, n = pending.getSize(); i < n; i++) {
      if (!pending.get(i).removed) {
        size++;
      }
    }
    return size;
  }

  /**
   * Returns whether {@link #create()} ran and {@link #destroy()} has not run since.
   *
   * @return {@code true} while the managers are created.
   */
  public boolean isCreated() {
    return created;
  }

  /** One entry in the list: either a fixed manager or a source that supplies one. */
  private static final class Slot {

    @Nullable
    final Manager manager;

    @Nullable
    final Supplier<? extends Manager> source;

    final int order;

    boolean removed;

    Slot(@Nullable Manager manager, @Nullable Supplier<? extends Manager> source, int order) {
      this.manager = manager;
      this.source = source;
      this.order = order;
    }

    @Nullable
    Manager resolve() {
      return source != null ? source.get() : manager;
    }
  }

  /**
   * An object that follows the game's global lifecycle, from creation to destruction.
   *
   * <p>Every hook has an empty default, so a manager only overrides the hooks it needs. Register
   * a manager with {@link FlixelManagers#add(Manager)}, usually through {@link Flixel#managers}.
   *
   * <p>One frame calls the hooks in this order:
   * <ol>
   *   <li>{@link #preUpdate(float)}, at the start of the frame.</li>
   *   <li>{@link #update(float)}, right before the state updates.</li>
   *   <li>The state, the cameras, and the global overlay update.</li>
   *   <li>{@link #postUpdate(float)}, after everything above.</li>
   *   <li>{@link #preDraw(FlixelBatch)}, before anything is drawn.</li>
   *   <li>{@link #drawCamera(FlixelCamera, FlixelBatch)}, once for each camera, after the state drew
   *       on it.</li>
   *   <li>{@link #drawOverlay(FlixelBatch)}, on top of every camera, the global shaders, and the
   *       global overlay.</li>
   *   <li>{@link #postDraw(FlixelBatch)}, after the frame is drawn.</li>
   *   <li>{@link #endFrame()}, after the frame is updated and drawn.</li>
   * </ol>
   *
   * <p>The other hooks run when their event happens: {@link #create()}, {@link #resize(int, int)},
   * the focus hooks, {@link #onSuspended()}, {@link #onPaused(boolean)}, the state switch hooks, and
   * {@link #destroy()}.
   *
   * <p>While the game is paused (by the debugger, or by {@link Flixel#autoPause} when the window
   * loses focus), the three update hooks are skipped for every manager whose {@link #isPausable()}
   * returns {@code true}. The draw hooks and the event hooks always run.
   */
  public interface Manager extends FlixelCreatable, FlixelUpdatable, FlixelDestroyable {

    /** The order a manager gets unless it overrides {@link #getOrder()}. */
    int ORDER_DEFAULT = 0;

    /**
     * Returns where this manager runs compared to the others. Lower orders run first, and managers
     * with the same order run in the order they were added.
     *
     * <p>{@link FlixelManagers#add(Manager)} reads this once, when the manager is added.
     *
     * @return The order of this manager. The default is {@link #ORDER_DEFAULT}.
     */
    default int getOrder() {
      return ORDER_DEFAULT;
    }

    /**
     * Returns whether the update hooks of this manager stop while the game is paused.
     *
     * <p>Return {@code false} for work that must keep going while the game is paused, such as
     * reading input or talking to the debugger.
     *
     * @return {@code true} (the default) if the update hooks are skipped while the game is paused.
     */
    default boolean isPausable() {
      return true;
    }

    /** Called once when the game starts, or right away when added to a running game. */
    @Override
    default void create() {}

    /**
     * Called at the very start of the update pass, before any {@link #update(float)}.
     *
     * @param elapsed The scaled seconds since the last frame.
     */
    default void preUpdate(float elapsed) {}

    /**
     * Called every frame right before the state updates.
     *
     * @param elapsed The scaled seconds since the last frame.
     */
    @Override
    default void update(float elapsed) {}

    /**
     * Called every frame after the state, the cameras, and the global overlay updated.
     *
     * @param elapsed The scaled seconds since the last frame.
     */
    default void postUpdate(float elapsed) {}

    /**
     * Called every frame before anything is drawn, so a manager can prepare its own render targets.
     *
     * @param batch The framework batch. It has not been begun.
     */
    default void preDraw(@NotNull FlixelBatch batch) {}

    /**
     * Called once for every camera, after the state drew on it and before the camera's effects
     * (such as a flash or a fade) are drawn on top.
     *
     * <p>The batch is already begun with the camera's projection, so drawing at world coordinates
     * lands in the right place. Do not end the batch. {@link Flixel#getDrawCamera()} returns
     * {@code camera} during this call.
     *
     * @param camera The camera being drawn.
     * @param batch The framework batch, already begun.
     */
    default void drawCamera(@NotNull FlixelCamera camera, @NotNull FlixelBatch batch) {}

    /**
     * Called every frame on top of every camera, the global shaders, and the global overlay.
     *
     * <p>The batch is already begun with the overlay camera's projection, whose scroll is always
     * zero, so drawing at {@code (x, y)} lands at those design-resolution coordinates. Do not end
     * the batch.
     *
     * @param batch The framework batch, already begun.
     */
    default void drawOverlay(@NotNull FlixelBatch batch) {}

    /**
     * Called every frame after the whole frame is drawn and stretched to the window.
     *
     * @param batch The framework batch. It has not been begun.
     */
    default void postDraw(@NotNull FlixelBatch batch) {}

    /**
     * Called every frame after the frame is updated and drawn. Input managers use it to finalize
     * this frame's state, which keeps checks such as {@code justPressed()} valid for the whole frame.
     */
    default void endFrame() {}

    /**
     * Called when the game's drawable surface changes size.
     *
     * @param width The new surface width in pixels.
     * @param height The new surface height in pixels.
     */
    default void resize(int width, int height) {}

    /** Called when the window loses focus, or the application goes to the background. */
    default void onFocusLost() {}

    /** Called when the window regains focus, or the application returns to the foreground. */
    default void onFocusGained() {}

    /** Called when the desktop window is minimized. Never called on mobile or web. */
    default void onMinimized() {}

    /** Called when the platform may stop the game soon without a clean shutdown, so data should be saved now. */
    default void onSuspended() {}

    /**
     * Called when the game is paused or resumed through {@link FlixelGame#setGamePaused(boolean)}.
     *
     * @param paused {@code true} when the game was just paused, {@code false} when it was resumed.
     */
    default void onPaused(boolean paused) {}

    /**
     * Called when a state switch begins, before the current state is destroyed.
     *
     * @param data Holds the state that is about to be destroyed, which is {@code null} for the
     *     first state.
     */
    default void preStateSwitch(@NotNull StateSwitchSignalData data) {}

    /**
     * Called when a state switch ends, after the new state was created.
     *
     * @param data Holds the state that was just created.
     */
    default void postStateSwitch(@NotNull StateSwitchSignalData data) {}

    /** Called once when the game closes, or right away when removed from a running game. */
    @Override
    default void destroy() {}
  }
}
