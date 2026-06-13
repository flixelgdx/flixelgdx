/**
 * Logging utilities for FlixelGDX.
 *
 * <p>This package contains the default logger implementation, log modes, and stack trace provider
 * abstraction used to attach useful context to log messages.
 *
 * <p>Platform launchers typically configure the stack trace provider during startup using
 * {@link org.flixelgdx.Flixel#setStackTraceProvider(org.flixelgdx.logging.FlixelStackTraceProvider) Flixel.setStackTraceProvider(FlixelStackTraceProvider)}
 * before calling {@link org.flixelgdx.Flixel#initialize(org.flixelgdx.FlixelGame) Flixel.initialize(FlixelGame)}.
 *
 * <p>The default logger is {@link org.flixelgdx.logging.FlixelLogger FlixelLogger}, which is exposed as the
 * public {@code Flixel.log} field and can be replaced by assigning a new instance to that field.
 */
package org.flixelgdx.logging;
