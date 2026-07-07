package org.flixelgdx.video;

/**
 * Platform-specific factory for creating video backends.
 *
 * <p>One instance is registered through
 * {@link FlixelVideos#setBackendFactory(FlixelVideoFactory)} by the
 * platform launcher (for example {@code FlixelFfmpegVideoHandler.install()} on
 * desktop) before any {@link FlixelVideo} is created.
 */
public interface FlixelVideoFactory {

  /**
   * Creates a new video backend for the given path.
   *
   * @param path Resolved path to the video file. For internal assets this is an
   * absolute filesystem path on JVM platforms and a URL path on the web.
   * @param external {@code true} if the path points outside the game's assets.
   * @return A new backend instance.
   */
  FlixelVideo createVideo(String path, boolean external);
}
