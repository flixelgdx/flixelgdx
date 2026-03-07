package me.stringdotjar.flixelgdx.signal;

import com.badlogic.gdx.utils.SnapshotArray;

/**
 * Utility class for creating objects that can execute multiple callbacks when it is dispatched (or triggered).
 */
public class FlixelSignal<T> {

  private final SnapshotArray<SignalHandler<T>> callbacks;
  private final SnapshotArray<SignalHandler<T>> tempCallbacks;

  public FlixelSignal() {
    callbacks = new SnapshotArray<>();
    tempCallbacks = new SnapshotArray<>();
  }

  /**
   * Adds a new callback to {@code this} signal to be executed upon {@code dispatch()} being
   * called.
   *
   * @param callback The new callback to add to the signal, with the type intended to be a Java {@code record}.
   */
  public void add(SignalHandler<T> callback) {
    if (callback != null) {
      callbacks.add(callback);
    }
  }

  /**
   * Adds a temporary callback that only gets ran <i>once</i>. When {@code dispatch()} is
   * executed, the temporary callback is removed.
   *
   * @param callback The new temporary callback to add, with the type intended to be a Java {@code record}.
   */
  public void addOnce(SignalHandler<T> callback) {
    if (callback != null) {
      tempCallbacks.add(callback);
    }
  }

  /**
   * Removes a specific callback from {@code this} signal.
   *
   * @param callback The callback to remove.
   */
  public void remove(SignalHandler<T> callback) {
    callbacks.removeValue(callback, true);
    tempCallbacks.removeValue(callback, true);
  }

  /** Removes all callbacks from {@code this} signal. */
  public void clear() {
    callbacks.clear();
    tempCallbacks.clear();
  }

  /** Triggers {@code this} signal and executes all callbacks. */
  public void dispatch() {
    dispatch(null);
  }

  /**
   * Triggers {@code this} signal and executes all callbacks.
   *
   * @param data The parameters that {@code this} signal takes.
   */
  @SuppressWarnings("unchecked")
  public void dispatch(T data) {
    Object[] items = callbacks.begin();
    for (int i = 0, n = callbacks.size; i < n; i++) {
      SignalHandler<T> callback = (SignalHandler<T>) items[i];
      if (callback != null) {
        callback.execute(data);
      }
    }
    callbacks.end();

    if (tempCallbacks.size > 0) {
      Object[] tempItems = tempCallbacks.begin();
      for (int i = 0, n = tempCallbacks.size; i < n; i++) {
        SignalHandler<T> callback = (SignalHandler<T>) tempItems[i];
        if (callback != null) {
          callback.execute(data);
        }
      }
      tempCallbacks.end();
      tempCallbacks.clear();
    }
  }

  public interface SignalHandler<T> {
    void execute(T data);
  }
}
