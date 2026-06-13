/**
 * Higher-level UI widgets and bars that build on libGDX batch rendering and FlixelGDX cameras.
 *
 * <p>These types are optional. Many games only use {@link org.flixelgdx.FlixelSprite FlixelSprite} and
 * {@link org.flixelgdx.FlixelState FlixelState} for HUDs. Use this package when you need ready-made meters, bars, or
 * layout helpers that integrate with {@link org.flixelgdx.Flixel#cameras Flixel.cameras} and the main {@link org.flixelgdx.FlixelGame FlixelGame} loop.
 *
 * <p>Prefer updating UI elements from {@link org.flixelgdx.FlixelState#update(float) FlixelState.update(float)} so input and draw order stay deterministic.
 */
package org.flixelgdx.ui;
