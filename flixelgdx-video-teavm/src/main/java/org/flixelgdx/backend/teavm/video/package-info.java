/**
 * Web (TeaVM) backend for the FlixelGDX video extension.
 *
 * <p>A hidden HTML video element decodes the stream, and each frame is pulled into a
 * WebGL texture with {@code texImage2D}, so videos draw through the normal batch and
 * layer with other state members. Install with
 * {@link org.flixelgdx.backend.teavm.video.FlixelTeaVMVideoHandler#install()} in your web
 * launcher.
 */
package org.flixelgdx.backend.teavm.video;
