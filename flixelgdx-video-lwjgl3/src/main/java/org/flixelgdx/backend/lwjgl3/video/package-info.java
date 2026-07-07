/**
 * Desktop (LWJGL3) backend for the FlixelGDX video extension.
 *
 * <p>Frames are decoded by JavaCPP-bundled FFmpeg in a background thread, converted
 * to RGBA, and streamed into a reusable texture through double-buffered pixel buffer
 * objects so the GPU DMA and the CPU copy never stall each other. Audio is decoded
 * to stereo 16-bit PCM and streamed to an OpenAL source. No system VLC or any other
 * external library is required. Install with
 * {@link org.flixelgdx.backend.lwjgl3.video.FlixelFfmpegVideoHandler#install()} in
 * your desktop launcher.
 */
package org.flixelgdx.backend.lwjgl3.video;
