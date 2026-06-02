/**
 * Logging utilities for FlixelGDX.
 *
 * <p>This package contains the default logger implementation, log modes, and stack trace provider
 * abstraction used to attach useful context to log messages.
 *
 * <p>Platform launchers typically configure the stack trace provider during startup using
 * {@link org.flixelgdx.Flixel#setStackTraceProvider(org.flixelgdx.logging.FlixelStackTraceProvider)}
 * before calling {@link org.flixelgdx.Flixel#initialize(org.flixelgdx.FlixelGame)}.
 *
 * <p>The default logger is {@link org.flixelgdx.logging.FlixelLogger}, although it can be replaced with a custom logger
 * through the {@link org.flixelgdx.Flixel#setLogger(org.flixelgdx.logging.FlixelLogger)} method.
 */
package org.flixelgdx.logging;
