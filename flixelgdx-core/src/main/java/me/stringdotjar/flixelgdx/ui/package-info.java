/**
 * Higher-level UI widgets and bars that build on libGDX batch rendering and FlixelGDX cameras.
 *
 * <p>These types are optional. Many games only use {@link me.stringdotjar.flixelgdx.FlixelSprite} and
 * {@link me.stringdotjar.flixelgdx.FlixelState} for HUDs. Use this package when you need ready-made meters, bars, or
 * layout helpers that integrate with {@link me.stringdotjar.flixelgdx.Flixel#getCamera()} and the main {@link me.stringdotjar.flixelgdx.FlixelGame} loop.
 *
 * <p>Prefer updating UI elements from {@link me.stringdotjar.flixelgdx.FlixelState#update(float)} so input and draw order stay deterministic.
 */
package me.stringdotjar.flixelgdx.ui;
