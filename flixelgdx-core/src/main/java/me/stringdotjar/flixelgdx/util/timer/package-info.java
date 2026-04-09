/**
 * Frame-based timers package for FlixelGDX.
 *
 * <p>Note that this package does not use libGDX {@code Timer} and does not use background threads. All global timer
 * objects are updated in the main game loop.
 *
 * <p><b>Usage</b>
 * <ul>
 *   <li>Schedule work: {@link me.stringdotjar.flixelgdx.util.timer.FlixelTimer#getGlobalManager()}{@code .start(seconds, callback, loops)}
 *     or static helpers {@link me.stringdotjar.flixelgdx.util.timer.FlixelTimer#wait(float, me.stringdotjar.flixelgdx.util.timer.FlixelTimerListener)}
 *     and {@link me.stringdotjar.flixelgdx.util.timer.FlixelTimer#loop(float, me.stringdotjar.flixelgdx.util.timer.FlixelTimerListener, int)}.</li>
 *   <li>Scaling: {@link me.stringdotjar.flixelgdx.FlixelGame} passes {@code elapsed} times {@link me.stringdotjar.flixelgdx.Flixel#getTimeScale()} into
 *     {@link me.stringdotjar.flixelgdx.util.timer.FlixelTimerManager#update(float)}.</li>
 *   <li>Pooled instances: do not store {@link me.stringdotjar.flixelgdx.util.timer.FlixelTimer} references across {@link me.stringdotjar.flixelgdx.util.timer.FlixelTimer#cancel()} or completion;
 *     the manager returns them to an internal {@link com.badlogic.gdx.utils.Pool}.</li>
 * </ul>
 *
 * @see me.stringdotjar.flixelgdx.util.timer.FlixelTimer
 * @see me.stringdotjar.flixelgdx.util.timer.FlixelTimerManager
 */
package me.stringdotjar.flixelgdx.util.timer;
