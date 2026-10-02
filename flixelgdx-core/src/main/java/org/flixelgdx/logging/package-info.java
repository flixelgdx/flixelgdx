/**
 * Structured logging with tags, sinks, call sites, and per-level filtering for FlixelGDX.
 *
 * <p>Game code typically uses the static shortcuts in {@link org.flixelgdx.Flixel Flixel}, and keeps a
 * tagged logger in a static field for classes that log a lot. You can also use
 * {@link org.flixelgdx.Flixel#log Flixel.log} directly to configure logging.
 *
 * <p>Example:
 * <pre>{@code
 * private static final FlixelLogger LOG = Flixel.log.tagged("PlayState");
 *
 * Flixel.info("Player respawned.");
 * Flixel.warn("Asset not found: {}", path);
 * LOG.debug("Frame time: {} s", elapsed);
 * LOG.error("Save failed for slot {}", slot, exception);
 * }</pre>
 *
 * <h2>Messages</h2>
 * <p>Each {@code {}} in a message is replaced by the next argument. A trailing exception that no
 * {@code {}} used is attached to the log entry so its stack trace is printed. There are no tag
 * parameters: a logger carries its own tag, set with
 * {@link org.flixelgdx.logging.FlixelLogger#setTag(String) FlixelLogger.setTag(...)} or preset by
 * {@link org.flixelgdx.logging.FlixelLogger#tagged(String) FlixelLogger.tagged(...)}.
 *
 * <h2>Log levels and modes</h2>
 * <p>{@link org.flixelgdx.logging.FlixelLogLevel FlixelLogLevel} defines the severity of an
 * individual message, and the logger throws away messages below its minimum level before doing any
 * work for them. {@link org.flixelgdx.logging.FlixelLogMode FlixelLogMode} controls how the log is
 * displayed. {@link org.flixelgdx.logging.FlixelLogMode#SIMPLE FlixelLogMode.SIMPLE} is the default,
 * which just outputs the package, file, and line the log came from. If you want a more professional
 * feel for your game, use {@link org.flixelgdx.logging.FlixelLogMode#DETAILED FlixelLogMode.DETAILED}
 * instead.
 *
 * <h2>Sinks</h2>
 * <p>A logger formats each message once into a reused {@link org.flixelgdx.logging.FlixelLogEntry
 * FlixelLogEntry} and hands it to its {@link org.flixelgdx.logging.FlixelLogSink FlixelLogSink}s: one
 * console sink, an optional {@link org.flixelgdx.logging.FlixelLogFileSink FlixelLogFileSink}, and any
 * number of extra sinks added with
 * {@link org.flixelgdx.logging.FlixelLogger#addSink(org.flixelgdx.logging.FlixelLogSink)
 * FlixelLogger.addExtraSink(...)}. The core default console sink,
 * {@link org.flixelgdx.logging.FlixelPlainConsoleSink FlixelPlainConsoleSink}, prints plain text.
 * Platform launchers install sinks that suit their platform before the game starts.
 *
 * <h2>Call sites</h2>
 * <p>The file and line of a log call are found lazily, only when a sink asks for
 * {@link org.flixelgdx.logging.FlixelLogEntry#site() FlixelLogEntry.site()}. The platform supplies a
 * {@link org.flixelgdx.logging.FlixelLogSiteResolver FlixelLogSiteResolver} to do the lookup.
 * {@link org.flixelgdx.logging.FlixelLogSiteMarker FlixelLogSiteMarker} is the resolver for platforms
 * where a compiler plugin records the site just before each log call.
 *
 * <h2>File logging</h2>
 * <p>A {@link org.flixelgdx.logging.FlixelLogFileSink FlixelLogFileSink} writes log output to disk.
 * The desktop backend installs one; the path is determined by the platform's writable directory.
 *
 * @see org.flixelgdx.logging.FlixelLogger
 * @see org.flixelgdx.logging.FlixelDefaultLogger
 * @see org.flixelgdx.logging.FlixelLogLevel
 * @see org.flixelgdx.logging.FlixelLogMode
 */
package org.flixelgdx.logging;
