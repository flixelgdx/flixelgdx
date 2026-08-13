/**
 * Frame-based timers for scheduling callbacks after a delay or on a repeating interval.
 *
 * <p>Timers in FlixelGDX are not backed by platform clocks or background threads. They are
 * updated once per game frame from the main loop, which means they respect
 * {@link org.flixelgdx.Flixel#timeScale Flixel.timeScale} automatically. Slowing time to half
 * speed with {@code timeScale = 0.5f} also makes all active timers count down at half speed,
 * with no extra code required.
 *
 * <p>The two main entry points are the static helpers
 * {@link org.flixelgdx.util.timer.FlixelTimer#wait(float, org.flixelgdx.util.timer.FlixelTimerListener) FlixelTimer.wait(...)}
 * and {@link org.flixelgdx.util.timer.FlixelTimer#loop(float, org.flixelgdx.util.timer.FlixelTimerListener, int) FlixelTimer.loop(...)},
 * both of which create a pooled timer registered with the global manager.
 *
 * <h2>One-shot delay</h2>
 * <p>Use {@link org.flixelgdx.util.timer.FlixelTimer#wait(float, org.flixelgdx.util.timer.FlixelTimerListener) FlixelTimer.wait(...)}
 * when you need to run some code once after a delay, such as spawning an explosion effect
 * two seconds after an impact:
 *
 * <pre>{@code
 * FlixelTimer.wait(2f, timer -> spawnExplosion(x, y));
 * }</pre>
 *
 * <h2>Repeating timer</h2>
 * <p>Use {@link org.flixelgdx.util.timer.FlixelTimer#loop(float, org.flixelgdx.util.timer.FlixelTimerListener, int) FlixelTimer.loop(...)}
 * when you need to run code on a fixed interval. Pass {@code 0} as the loop count for an infinite
 * loop, or a positive number to run a fixed number of times:
 *
 * <pre>{@code
 * // Fire every 1.5 seconds, 5 times total.
 * FlixelTimer.loop(1.5f, timer -> spawnEnemy(), 5);
 *
 * // Fire every 3 seconds, forever (until canceled).
 * FlixelTimer.loop(3f, timer -> dropPowerup(), 0);
 * }</pre>
 *
 * <h2>Canceling a timer</h2>
 * <p>Both helpers return the {@link org.flixelgdx.util.timer.FlixelTimer FlixelTimer} instance. Hold
 * onto it if you need to cancel the timer before it finishes:
 *
 * <pre>{@code
 * FlixelTimer spawnTimer = FlixelTimer.loop(1f, timer -> spawnEnemy(), 0);
 *
 * // Later, when the wave ends:
 * spawnTimer.cancel();
 * }</pre>
 *
 * <p><b>Do not store a {@code FlixelTimer} reference past a call to
 * {@link org.flixelgdx.util.timer.FlixelTimer#cancel() cancel()} or past the timer's natural
 * completion.</b> The manager returns the instance to an internal
 * {@link org.flixelgdx.collections.FlixelPool FlixelPool} at that point, and it may be reused
 * for a completely different timer.
 *
 * <h2>The global manager and scoped managers</h2>
 * <p>All static helpers use a single shared
 * {@link org.flixelgdx.util.timer.FlixelTimerManager FlixelTimerManager} held inside
 * {@link org.flixelgdx.util.timer.FlixelTimer FlixelTimer}. The game loop drives it automatically
 * via {@link org.flixelgdx.FlixelGame FlixelGame}, so you never need to call {@code update} on it
 * yourself when using the static entry points.
 *
 * <p>For more control (for example limiting a set of timers to a single game state so they
 * all stop automatically when the state is destroyed) create a dedicated
 * {@link org.flixelgdx.util.timer.FlixelTimerManager FlixelTimerManager} and add it to your
 * {@link org.flixelgdx.FlixelState FlixelState} like any other member:
 *
 * <pre>{@code
 * // In your FlixelState subclass:
 * private final FlixelTimerManager timers = new FlixelTimerManager();
 *
 * @Override
 * public void create() {
 *   // Adding it to a state will automatically call update and destroy for you.
 *   add(timers);
 * }
 * }</pre>
 *
 * <h2>Zero-duration timers</h2>
 * <p>Passing {@code 0} as the duration is valid. A zero-duration timer fires its callback on the
 * very next frame update rather than immediately. This is useful when you need a one-frame deferral
 * to let the current update cycle finish before running some code.
 *
 * <h2>Pooling and memory</h2>
 * <p>Timer instances are managed by a {@link org.flixelgdx.collections.FlixelPool FlixelPool}
 * inside each {@link org.flixelgdx.util.timer.FlixelTimerManager FlixelTimerManager}. Starting a
 * timer obtains an instance from the pool; completing or canceling a timer returns it. This means
 * scheduling timers frequently (for example once per enemy spawn) does not generate garbage after
 * the pool is warmed up.
 *
 * @see org.flixelgdx.util.timer.FlixelTimer
 * @see org.flixelgdx.util.timer.FlixelTimerManager
 * @see org.flixelgdx.util.timer.FlixelTimerListener
 */
package org.flixelgdx.util.timer;
