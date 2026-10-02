/**
 * Logging pieces for the desktop backend, built on the sink and resolver seams of
 * {@link org.flixelgdx.logging.FlixelLogger FlixelLogger}.
 *
 * <ul>
 *   <li>{@link org.flixelgdx.backend.desktop.logging.FlixelJvmLogSiteResolver FlixelJvmLogSiteResolver}
 *       finds the file, line, and method of a log call by walking the JVM stack.</li>
 *   <li>{@link org.flixelgdx.backend.desktop.logging.FlixelAnsiConsoleSink FlixelAnsiConsoleSink}
 *       prints colored output to the terminal. Colors turn off when the {@code NO_COLOR} environment
 *       variable is set or the {@code flixel.log.color} system property is {@code false}.</li>
 *   <li>{@link org.flixelgdx.backend.desktop.logging.FlixelJvmLogFileSink FlixelJvmLogFileSink}
 *       writes {@code flixel-*.log} files on a background thread without creating text objects for
 *       every line.</li>
 * </ul>
 *
 * <p>The desktop launcher installs all three before the game starts.
 */
package org.flixelgdx.backend.desktop.logging;
