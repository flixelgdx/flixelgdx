/**
 * Shared JVM backend utilities for FlixelGDX, consumed by all JVM-based platform backends.
 *
 * <p>This module factors out the parts of the backend that every JVM platform (such as desktop and
 * Android) shares so they are not duplicated. It is not used on non-JVM targets such as HTML5.
 * Game code should very rarely import from this module directly; it only uses the abstractions in
 * {@code flixelgdx-core}.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code asset} - JVM asset manager ({@code FlixelJvmAssetManager}) backed by an
 *       {@link java.util.concurrent.ExecutorService ExecutorService} for async loading.</li>
 *   <li>{@code file} - JVM file system seam ({@code FlixelJvmFile}, {@code FlixelJvmFiles})
 *       that resolves paths against both the classpath and the OS filesystem and is installed as
 *       {@link org.flixelgdx.Flixel#files Flixel.files} on every JVM platform.</li>
 *   <li>{@code logging} - JVM logging helpers: {@code FlixelJvmStackTraceProvider} uses
 *       {@link java.lang.StackWalker StackWalker} to find the real log call site, and
 *       {@code FlixelJvmLogFileHandler} writes log output to a file in the platform's writable
 *       directory.</li>
 *   <li>{@code runtime} - {@code FlixelJvmRuntimeDevice} implements
 *       {@link org.flixelgdx.backend.FlixelRuntimeDevice FlixelRuntimeDevice} with JVM-specific
 *       heap sampling, classpath and JAR detection, and IDE detection.</li>
 * </ul>
 */
package org.flixelgdx.backend.jvm;
