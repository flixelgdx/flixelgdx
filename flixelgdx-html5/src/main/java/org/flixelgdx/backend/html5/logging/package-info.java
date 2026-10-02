/**
 * Logging pieces for the HTML5 backend, built on the sink and resolver seams of
 * {@link org.flixelgdx.logging.FlixelLogger FlixelLogger}.
 *
 * <ul>
 *   <li>{@link org.flixelgdx.backend.html5.logging.FlixelHtml5ConsoleSink FlixelHtml5ConsoleSink}
 *       prints each message to the browser's developer tools console, using the console method that
 *       matches the log level.</li>
 *   <li>{@link org.flixelgdx.backend.html5.logging.FlixelLogSiteTeaVMPlugin FlixelLogSiteTeaVMPlugin}
 *       and {@link org.flixelgdx.backend.html5.logging.FlixelLogSiteTransformer FlixelLogSiteTransformer}
 *       are TeaVM compiler plugin classes. At build time they record the file, line, class, and
 *       method of every log call, which
 *       {@link org.flixelgdx.logging.FlixelLogSiteMarker FlixelLogSiteMarker} hands back to the logger
 *       at run time, because a browser has no stack to walk. TeaVM finds the plugin automatically, so
 *       games do not need to configure anything.</li>
 * </ul>
 *
 * <p>The HTML5 launcher installs the console sink and the marker resolver before the game starts.
 * There is no file sink on the web.
 */
package org.flixelgdx.backend.html5.logging;
