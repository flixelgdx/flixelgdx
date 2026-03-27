/**
 * The core package of the FlixelGDX framework.
 *
 * <p>This package contains the primary entry points and global services for the framework.
 * Most games interact with FlixelGDX through {@link me.stringdotjar.flixelgdx.Flixel} and a
 * subclass of {@link me.stringdotjar.flixelgdx.FlixelGame}.
 *
 * <p>Start here:
 * <ul>
 *   <li>{@link me.stringdotjar.flixelgdx.Flixel} - Global manager: initialization, state switching,
 *       signals, and access to core managers (input, audio, assets).</li>
 *   <li>{@link me.stringdotjar.flixelgdx.FlixelGame} - libGDX application listener that drives the
 *       main update and draw loop.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.FlixelState} - Screen like container for your game logic.</li>
 * </ul>
 *
 * <p>Assets are centralized under {@link me.stringdotjar.flixelgdx.Flixel#assets}. Prefer that API
 * and the typed handle helpers in {@link me.stringdotjar.flixelgdx.asset} instead of using libGDX
 * {@code AssetManager} directly, unless you need low level features.
 *
 * @see me.stringdotjar.flixelgdx.Flixel
 * @see me.stringdotjar.flixelgdx.FlixelGame
 * @see me.stringdotjar.flixelgdx.FlixelState
 */
package me.stringdotjar.flixelgdx;
