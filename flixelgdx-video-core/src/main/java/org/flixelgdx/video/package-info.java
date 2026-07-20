/**
 * Platform-neutral video playback API for FlixelGDX.
 *
 * <p>{@link org.flixelgdx.video.FlixelVideo} is the base class games interact with, and
 * {@link org.flixelgdx.video.FlixelVideos} creates instances backed by the platform
 * backend registered once per launcher. Platform backends extend
 * {@link org.flixelgdx.video.FlixelVideo}; the desktop, web, and Android implementations
 * ship in the sibling {@code flixelgdx-video-lwjgl3}, {@code flixelgdx-video-teavm},
 * and {@code flixelgdx-video-android} modules.
 */
package org.flixelgdx.video;
