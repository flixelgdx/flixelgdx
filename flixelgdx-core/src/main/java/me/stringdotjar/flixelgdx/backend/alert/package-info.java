/**
 * Alert and notification backend abstraction.
 *
 * <p>This package defines the platform independent interface used by FlixelGDX to present simple
 * alert messages such as info, warnings, and errors. Each platform module provides an implementation
 * that integrates with that platform's UI or runtime capabilities.
 *
 * <p>Games configure the active implementation during startup using
 * {@link me.stringdotjar.flixelgdx.Flixel#setAlerter(me.stringdotjar.flixelgdx.backend.alert.FlixelAlerter)}
 * before calling {@link me.stringdotjar.flixelgdx.Flixel#initialize(me.stringdotjar.flixelgdx.FlixelGame)}.
 */
package me.stringdotjar.flixelgdx.backend.alert;
