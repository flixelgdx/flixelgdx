/**
 * Logging helpers for the desktop backend.
 *
 * <p>{@link org.flixelgdx.backend.desktop.logging.FlixelJvmStackTraceProvider FlixelJvmStackTraceProvider}
 * uses {@link java.lang.StackWalker StackWalker} to find the real log call site.
 * {@link org.flixelgdx.backend.desktop.logging.FlixelJvmLogFileHandler FlixelJvmLogFileHandler}
 * writes log output to a file in the platform's writable directory.
 */
package org.flixelgdx.backend.desktop.logging;
