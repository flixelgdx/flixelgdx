/**
 * Desktop (LWJGL3) backend for the FlixelGDX video extension.
 *
 * <p>Frames are decoded by libvlc directly into memory through its video callbacks and
 * streamed into a reusable texture with double-buffered pixel buffer objects, so libvlc
 * never owns a window and videos layer like ordinary state members. Install with
 * {@link org.flixelgdx.backend.lwjgl3.video.FlixelVlcVideoHandler#install()} in your desktop
 * launcher.
 */
package org.flixelgdx.backend.lwjgl3.video;
