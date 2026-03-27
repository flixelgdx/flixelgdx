/**
 * Reflection abstraction for FlixelGDX.
 *
 * <p>This package provides a small reflection service that can be swapped based on platform
 * capabilities. Some platforms restrict reflection or require alternate implementations.
 *
 * <p>Games and launchers can set the active reflection handler using
 * {@link me.stringdotjar.flixelgdx.Flixel#setReflection(me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection)}
 * during startup.
 */
package me.stringdotjar.flixelgdx.backend.reflect;
