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
package org.flixelgdx.util.signal;

import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.NotNull;

/**
 * A typed event emitter that notifies registered listeners when something happens in the game.
 *
 * <p>A signal works like a bulletin board: other parts of your game "subscribe" to the signal by
 * calling {@link #add(SignalHandler)} or {@link #addOnce(SignalHandler)}. When the event occurs,
 * you call {@link #dispatch(Object)} (or {@link #dispatch()} if no data needs to be passed), and
 * every registered listener is called automatically in the order they were added.
 *
 * <p>This decouples the source of an event from the code that reacts to it. A {@code FlixelSignal}
 * on your player object does not need to know which UI elements, audio systems, or achievement
 * trackers care about the player getting hit; they simply subscribe and react on their own.
 *
 * <h2>Basic usage</h2>
 *
 * <pre>{@code
 * // Create a signal that carries an Integer (the new score).
 * FlixelSignal<Integer> onScoreChanged = new FlixelSignal<>();
 *
 * // Subscribe a permanent listener.
 * onScoreChanged.add(score -> hud.setScore(score));
 *
 * // Subscribe a one-time listener (fires once, then auto-removes itself).
 * onScoreChanged.addOnce(score -> showFirstPointBanner());
 *
 * // Later, when the score changes, dispatch the signal.
 * onScoreChanged.dispatch(100);
 * // Both listeners above fire. The addOnce listener is now gone.
 * onScoreChanged.dispatch(200);
 * // Only the permanent listener fires.
 * }</pre>
 *
 * <p>If no data needs to be sent with the event, use {@code Void} as the type parameter and call
 * the no-arg {@link #dispatch()} overload:
 *
 * <pre>{@code
 * FlixelSignal<Void> onPlayerDied = new FlixelSignal<>();
 * onPlayerDied.add(ignored -> respawnPlayer());
 * onPlayerDied.dispatch();
 * }</pre>
 *
 * <p>Null callbacks passed to {@link #add(SignalHandler)} or {@link #addOnce(SignalHandler)} are
 * silently ignored so that call sites do not need a null check before subscribing.
 *
 * @param <T> the type of data carried by this signal when dispatched.
 */
public class FlixelSignal<T> {

  private final FlixelArray<SignalHandler<T>> callbacks;
  private final FlixelArray<SignalHandler<T>> tempCallbacks;

  /**
   * Constructs a new signal with no callbacks registered.
   */
  public FlixelSignal() {
    callbacks = new FlixelArray<>();
    tempCallbacks = new FlixelArray<>();
  }

  /**
   * Registers a permanent listener that is called every time this signal dispatches.
   *
   * <p>The listener stays registered until it is explicitly removed with {@link #remove(SignalHandler)}
   * or until {@link #clear()} is called. If you only want the listener to fire once, use
   * {@link #addOnce(SignalHandler)} instead. Passing {@code null} is a no-op.
   *
   * @param callback The listener to register; ignored if {@code null}.
   */
  public void add(@NotNull SignalHandler<T> callback) {
    if (callback != null) {
      callbacks.add(callback);
    }
  }

  /**
   * Registers a one-time listener that fires on the next dispatch and then removes itself.
   *
   * <p>Use this when you need to react to an event exactly once. For example, playing an
   * intro cutscene the first time the player enters a zone, or showing a tutorial prompt
   * the first time the player picks up an item. After the next {@link #dispatch(Object)} call,
   * the listener is gone automatically and will not fire again. Passing {@code null} is a no-op.
   *
   * @param callback The one-time listener to register; ignored if {@code null}.
   */
  public void addOnce(@NotNull SignalHandler<T> callback) {
    if (callback != null) {
      tempCallbacks.add(callback);
    }
  }

  /**
   * Removes a specific listener from this signal.
   *
   * <p>The listener is looked up by reference identity (not {@code equals}), so you must pass the
   * exact same object that was originally registered. The search covers both the permanent list
   * (registered via {@link #add(SignalHandler)}) and the one-time list (registered via
   * {@link #addOnce(SignalHandler)}). If the listener is not present in either list, this method
   * does nothing.
   *
   * @param callback The listener to remove.
   */
  public void remove(SignalHandler<T> callback) {
    callbacks.removeValue(callback, true);
    tempCallbacks.removeValue(callback, true);
  }

  /**
   * Removes all listeners from this signal, including both permanent and one-time listeners.
   *
   * <p>After this call, dispatching the signal has no effect until new listeners are registered.
   * This is useful when tearing down a game state or resetting a scene, where holding references
   * to stale listeners from a previous level could cause bugs or memory leaks.
   */
  public void clear() {
    callbacks.clear();
    tempCallbacks.clear();
  }

  /**
   * Dispatches this signal with no data, calling all registered listeners.
   *
   * <p>This is a convenience overload for signals that carry no meaningful payload. It is
   * equivalent to calling {@link #dispatch(Object) dispatch(null)}. Use the typed overload when
   * listeners need data about the event.
   */
  public void dispatch() {
    dispatch(null);
  }

  /**
   * Dispatches this signal, calling every registered listener with the provided data.
   *
   * <p>Permanent listeners (registered via {@link #add(SignalHandler)}) are called first, in
   * the order they were added. One-time listeners (registered via {@link #addOnce(SignalHandler)})
   * are called next, also in registration order, and are then all removed automatically.
   *
   * <p>Iteration is performed through {@link FlixelArray}'s stable snapshot mechanism
   * ({@code begin}/{@code end}), so listeners may safely add or remove other listeners during
   * dispatch without disturbing the current iteration.
   *
   * @param data The value passed to each listener; may be {@code null} if the signal was
   *     dispatched with no data via {@link #dispatch()}.
   */
  @SuppressWarnings("unchecked")
  public void dispatch(T data) {
    Object[] items = callbacks.begin();
    for (int i = 0, n = callbacks.getSize(); i < n; i++) {
      SignalHandler<T> callback = (SignalHandler<T>) items[i];
      if (callback != null) {
        callback.execute(data);
      }
    }
    callbacks.end();

    if (tempCallbacks.getSize() > 0) {
      Object[] tempItems = tempCallbacks.begin();
      for (int i = 0, n = tempCallbacks.getSize(); i < n; i++) {
        SignalHandler<T> callback = (SignalHandler<T>) tempItems[i];
        if (callback != null) {
          callback.execute(data);
        }
      }
      tempCallbacks.end();
      tempCallbacks.clear();
    }
  }

  /**
   * A listener that reacts to a {@link FlixelSignal} being dispatched.
   *
   * <p>Because this interface has a single abstract method, you can implement it with a lambda
   * expression instead of a named class:
   *
   * <pre>{@code
   * FlixelSignal<Integer> onHealthChanged = new FlixelSignal<>();
   * onHealthChanged.add(health -> healthBar.setValue(health));
   * }</pre>
   *
   * @param <T> The type of data received when the signal fires.
   */
  @FunctionalInterface
  public interface SignalHandler<T> {

    /**
     * Called when the owning {@link FlixelSignal} is dispatched.
     *
     * @param data The value the signal was dispatched with; may be {@code null} when the
     *     no-arg {@link FlixelSignal#dispatch()} overload was used.
     */
    void execute(T data);
  }
}
