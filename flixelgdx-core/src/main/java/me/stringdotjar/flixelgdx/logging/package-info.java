/**
 * Logging utilities for FlixelGDX.
 *
 * <p>This package contains the default logger implementation, log modes, and stack trace provider
 * abstraction used to attach useful context to log messages.
 *
 * <p>Platform launchers typically configure the stack trace provider during startup using
 * {@link me.stringdotjar.flixelgdx.Flixel#setStackTraceProvider(me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider)}
 * before calling {@link me.stringdotjar.flixelgdx.Flixel#initialize(me.stringdotjar.flixelgdx.FlixelGame)}.
 *
 * <p>The default logger is {@link me.stringdotjar.flixelgdx.logging.FlixelLogger}, although it can be replaced with a custom logger
 * through the {@link me.stringdotjar.flixelgdx.Flixel#setLogger(me.stringdotjar.flixelgdx.logging.FlixelLogger)} method.
 */
package me.stringdotjar.flixelgdx.logging;
