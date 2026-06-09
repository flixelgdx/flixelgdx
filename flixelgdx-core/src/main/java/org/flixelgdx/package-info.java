/**
 * The core package of the FlixelGDX framework.
 *
 * <p>This package contains the primary entry points and global services for the framework.
 * Most games interact with FlixelGDX through {@link org.flixelgdx.Flixel} and a
 * subclass of {@link org.flixelgdx.FlixelGame}.
 *
 * <p>Start here:
 * <ul>
 *   <li>{@link org.flixelgdx.Flixel} - Global manager: initialization, state switching,
 *       signals, and access to core managers (input, audio, assets).</li>
 *   <li>{@link org.flixelgdx.FlixelGame} - libGDX application listener that drives the
 *       main update and draw loop.</li>
 *   <li>{@link org.flixelgdx.FlixelState} - Screen like container for your game logic.</li>
 * </ul>
 *
 * <p>Assets are centralized under {@link org.flixelgdx.Flixel#assets}. Prefer that API
 * and the typed handle helpers in {@link org.flixelgdx.asset} instead of using libGDX
 * {@code AssetManager} directly, unless you need low level features.
 *
 * @see org.flixelgdx.Flixel
 * @see org.flixelgdx.FlixelGame
 * @see org.flixelgdx.FlixelState
 */
package org.flixelgdx;
