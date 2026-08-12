/**
 * Structured logging with stack traces, sinks, and per-level filtering for FlixelGDX.
 *
 * <p>Game code typically uses the static shortcuts in {@link org.flixelgdx.Flixel Flixel}, although you
 * may want to access {@link org.flixelgdx.Flixel#log Flixel.log} directly for other purposes.
 *
 * <p>Example:
 * <pre>{@code
 * Flixel.info("Player respawned at " + x + ", " + y);
 * Flixel.warn("Asset not found: " + path);
 * Flixel.error("Save failed", exception);
 * Flixel.debug("Frame time: " + elapsed + " s");
 * }</pre>
 *
 * <h2>Log levels and modes</h2>
 * <p>{@link org.flixelgdx.logging.FlixelLogLevel FlixelLogLevel} defines the severity of an
 * individual message. {@link org.flixelgdx.logging.FlixelLogMode FlixelLogMode} controls how
 * the log is displayed. {@link org.flixelgdx.logging.FlixelLogMode#SIMPLE FlixelLogMode.SIMPLE} is
 * the default, which just outputs the package, file and line the log came from. If you want a more
 * professional feel for your game, use {@link org.flixelgdx.logging.FlixelLogMode#DETAILED FlixelLogMode.DETAILED}
 * instead.
 *
 * <h2>Stack traces</h2>
 * <p>Error-level messages include a call stack when available. The
 * {@link org.flixelgdx.logging.FlixelStackTraceProvider FlixelStackTraceProvider} abstraction
 * lets the backend supply platform-appropriate stack frames. Platform launchers install their
 * provider into {@link org.flixelgdx.Flixel#stackTraceProvider Flixel.stackTraceProvider}
 * before the game starts.
 *
 * <h2>File logging</h2>
 * <p>A {@link org.flixelgdx.logging.FlixelLogFileHandler FlixelLogFileHandler} can be attached
 * to write log output to disk. The desktop backend installs one automatically; the path is
 * determined by the platform's writable directory.
 *
 * @see org.flixelgdx.logging.FlixelLogger
 * @see org.flixelgdx.logging.FlixelLogLevel
 * @see org.flixelgdx.logging.FlixelLogMode
 */
package org.flixelgdx.logging;
