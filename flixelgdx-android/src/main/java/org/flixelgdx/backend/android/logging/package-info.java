/**
 * Android logging pieces: a call site resolver, a Logcat sink, and a log file sink.
 *
 * <p>This package is an implementation detail of the Android backend and is not part of the
 * public framework API. {@link org.flixelgdx.backend.android.FlixelAndroidLauncher} installs
 * {@link org.flixelgdx.backend.android.logging.FlixelAndroidLogSiteResolver},
 * {@link org.flixelgdx.backend.android.logging.FlixelLogcatSink}, and
 * {@link org.flixelgdx.backend.android.logging.FlixelAndroidLogFileSink} on
 * {@link org.flixelgdx.Flixel#log Flixel.log} before the game starts.
 */
package org.flixelgdx.backend.android.logging;
