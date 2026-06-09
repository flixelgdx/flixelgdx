/**
 * Frame-based timers package for FlixelGDX.
 *
 * <p>Note that this package does not use libGDX {@code Timer} and does not use background threads. All global timer
 * objects are updated in the main game loop.
 *
 * <p><b>Usage</b>
 * <ul>
 *   <li>Schedule work: {@link org.flixelgdx.util.timer.FlixelTimer#getGlobalManager()}{@code .start(seconds, callback, loops)}
 *     or static helpers {@link org.flixelgdx.util.timer.FlixelTimer#wait(float, org.flixelgdx.util.timer.FlixelTimerListener)}
 *     and {@link org.flixelgdx.util.timer.FlixelTimer#loop(float, org.flixelgdx.util.timer.FlixelTimerListener, int)}.</li>
 *   <li>Scaling: {@link org.flixelgdx.FlixelGame} passes {@code elapsed} times {@link org.flixelgdx.Flixel#timeScale()} into
 *     {@link org.flixelgdx.util.timer.FlixelTimerManager#update(float)}.</li>
 *   <li>Pooled instances: do not store {@link org.flixelgdx.util.timer.FlixelTimer} references across {@link org.flixelgdx.util.timer.FlixelTimer#cancel()} or completion;
 *     the manager returns them to an internal {@link com.badlogic.gdx.utils.Pool}.</li>
 * </ul>
 *
 * @see org.flixelgdx.util.timer.FlixelTimer
 * @see org.flixelgdx.util.timer.FlixelTimerManager
 */
package org.flixelgdx.util.timer;
