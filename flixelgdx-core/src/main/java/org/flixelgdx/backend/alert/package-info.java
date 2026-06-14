/**
 * Alert and notification backend abstraction.
 *
 * <p>This package defines the platform independent interface used by FlixelGDX to present simple
 * alert messages such as info, warnings, and errors. Each platform module provides an implementation
 * that integrates with that platform's UI or runtime capabilities.
 *
 * <p>Games configure the active implementation during startup using
 * {@link org.flixelgdx.Flixel#setAlerter(org.flixelgdx.backend.alert.FlixelAlerter) Flixel.setAlerter(FlixelAlerter)}
 * before calling {@link org.flixelgdx.Flixel#initialize(org.flixelgdx.FlixelGame) Flixel.initialize(FlixelGame)}.
 */
package org.flixelgdx.backend.alert;
