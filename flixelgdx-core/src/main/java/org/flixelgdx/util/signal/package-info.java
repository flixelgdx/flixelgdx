/**
 * Typed event emitters that decouple the source of an event from the code that reacts to it.
 *
 * <p>A signal works like a bulletin board. Other parts of your game register listeners (called
 * "subscribing"), and when something happens the signal is "dispatched", meaning every registered
 * listener is called automatically. No part of the code needs to know who is listening. The
 * player object does not need to reach into the UI, the audio system, or the achievement tracker;
 * it just dispatches a signal and the interested parties respond on their own.
 *
 * <p>The main class is {@link org.flixelgdx.util.signal.FlixelSignal FlixelSignal}, a generic
 * multicast listener list. The framework also defines a set of global signals on
 * {@link org.flixelgdx.Flixel.Signals Flixel.Signals} that fire for lifecycle events such as
 * state switches and per-frame updates.
 *
 * <h2>Subscribing to a signal</h2>
 * <p>Call {@link org.flixelgdx.util.signal.FlixelSignal#add(org.flixelgdx.util.signal.FlixelSignal.SignalHandler) FlixelSignal.add(...)}
 * to register a listener that fires every time the signal is dispatched. Because
 * {@link org.flixelgdx.util.signal.FlixelSignal.SignalHandler SignalHandler} is a functional
 * interface, you can use a lambda expression:
 *
 * <pre>{@code
 * FlixelSignal<Integer> onScoreChanged = new FlixelSignal<>();
 * onScoreChanged.add(score -> hud.setScore(score));
 * }</pre>
 *
 * <p>Use {@link org.flixelgdx.util.signal.FlixelSignal#addOnce(org.flixelgdx.util.signal.FlixelSignal.SignalHandler) FlixelSignal.addOnce(...)}
 * when you only need to react to the very next dispatch. The listener removes itself automatically
 * after firing once. This is ideal for one-time events like showing a tutorial prompt or playing
 * an intro cutscene exactly one time:
 *
 * <pre>{@code
 * onScoreChanged.addOnce(score -> showFirstPointBanner());
 * }</pre>
 *
 * <h2>Dispatching a signal</h2>
 * <p>Call {@link org.flixelgdx.util.signal.FlixelSignal#dispatch(Object) FlixelSignal.dispatch(...)}
 * to fire every registered listener with a value. Permanent listeners (added with {@code add}) fire
 * first in registration order, followed by one-time listeners (added with {@code addOnce}), which
 * are then removed:
 *
 * <pre>{@code
 * onScoreChanged.dispatch(100); // Both add and addOnce listeners fire.
 * onScoreChanged.dispatch(200); // Only the permanent add listener fires now.
 * }</pre>
 *
 * <p>If no data needs to accompany the event, use {@code Void} as the type parameter and call the
 * no-arg {@link org.flixelgdx.util.signal.FlixelSignal#dispatch() FlixelSignal.dispatch()} overload:
 *
 * <pre>{@code
 * FlixelSignal<Void> onPlayerDied = new FlixelSignal<>();
 * onPlayerDied.add(_ -> respawnPlayer());
 * onPlayerDied.dispatch(); // No data needed.
 * }</pre>
 *
 * <h2>Removing listeners</h2>
 * <p>To remove a specific listener, call
 * {@link org.flixelgdx.util.signal.FlixelSignal#remove(org.flixelgdx.util.signal.FlixelSignal.SignalHandler) FlixelSignal.remove(...)}
 * with the exact same reference that was originally registered. Lookup is by reference identity,
 * not by {@code equals}. To remove all listeners at once (for example when tearing down a game
 * state) call {@link org.flixelgdx.util.signal.FlixelSignal#clear() FlixelSignal.clear()}.
 *
 * <pre>{@code
 * FlixelSignal.SignalHandler<Integer> listener = score -> hud.setScore(score);
 * onScoreChanged.add(listener);
 * // Later, when done...
 * onScoreChanged.remove(listener);
 * }</pre>
 *
 * <h2>Safe dispatch during iteration</h2>
 * <p>Dispatch uses a snapshot-based iteration strategy backed by
 * {@link org.flixelgdx.collections.FlixelArray FlixelArray}'s {@code begin}/{@code end} mechanism.
 * This means listeners may safely add or remove other listeners during a dispatch call without
 * disrupting the current iteration. You do not need to defer modifications or guard against
 * {@code ConcurrentModificationException}.
 *
 * <h2>Global signals</h2>
 * <p>The framework exposes a set of pre-built signals on
 * {@link org.flixelgdx.Flixel.Signals Flixel.Signals} for common lifecycle events:
 *
 * <ul>
 *   <li>Pre- and post-update hooks (fired every game tick).</li>
 *   <li>State switch notifications (fired when the active {@link org.flixelgdx.FlixelState FlixelState} changes).</li>
 * </ul>
 *
 * <pre>{@code
 * // React every time the active state changes.
 * Flixel.Signals.onStateSwitch.add(data -> Flixel.info("Now in: " + data.state()));
 * }</pre>
 *
 * <h2>Signal data types and GC pressure</h2>
 * <p>The {@link org.flixelgdx.util.signal.FlixelSignalData FlixelSignalData} class groups the
 * carrier types used by the framework's own signals. The most important is
 * {@link org.flixelgdx.util.signal.FlixelSignalData.UpdateSignalData UpdateSignalData}, a mutable
 * object that is reused across every frame. Allocating a fresh object for every pre/post update
 * dispatch at 60 FPS would create noticeable GC pressure; the framework avoids this by mutating
 * and reusing the same instance instead.
 *
 * <p>This has one important consequence: <b>do not store a reference to an
 * {@code UpdateSignalData} past the callback return.</b> The values you read inside the callback
 * are valid only for the duration of that call.
 *
 * <pre>{@code
 * // Correct: read elapsed inside the callback.
 * Flixel.signals.onPreUpdate.add(data -> {
 *   float elapsed = data.elapsed(); // Read now, not later.
 *   mySystem.tick(elapsed);
 * });
 *
 * // Wrong: storing the reference for later reads will give stale or wrong values.
 * UpdateSignalData stored;
 * Flixel.signals.onPreUpdate.add(data -> stored = data); // Do not do this.
 * }</pre>
 *
 * @see org.flixelgdx.util.signal.FlixelSignal
 * @see org.flixelgdx.util.signal.FlixelSignalData
 * @see org.flixelgdx.Flixel.Signals
 */
package org.flixelgdx.util.signal;
