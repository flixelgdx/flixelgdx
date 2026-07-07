/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.backend.lwjgl3.video;

import com.badlogic.gdx.Gdx;

import org.flixelgdx.video.FlixelBaseVideo;
import org.flixelgdx.video.FlixelUnavailableVideo;
import org.flixelgdx.video.FlixelVideoFactory;
import org.flixelgdx.video.FlixelVideos;

/**
 * Desktop video backend factory powered by JavaCPP-bundled FFmpeg.
 *
 * <p>Install it once in your desktop launcher, before the game starts:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *   FlixelFfmpegVideoHandler.install();
 *   FlixelLwjgl3Launcher.launch(new MyGame());
 * }
 * }</pre>
 *
 * <p>No system VLC or any other external video library is required. The FFmpeg
 * native libraries ship inside the framework's dependency JARs and are extracted
 * automatically by JavaCPP the first time a video is created.
 *
 * <p>When a video cannot be opened (bad path, unsupported codec, etc.) the game
 * keeps running: the video degrades to a backend that is never ready (see
 * {@link org.flixelgdx.video.FlixelUnavailableVideo FlixelUnavailableVideo}) and
 * the reason is logged, so a missing file never crashes the game.
 */
public final class FlixelFfmpegVideoHandler implements FlixelVideoFactory {

  /**
   * Registers this handler as the video backend factory for {@link FlixelVideos}.
   * Safe to call multiple times.
   */
  public static void install() {
    FlixelVideos.setBackendFactory(new FlixelFfmpegVideoHandler());
  }

  @Override
  public FlixelBaseVideo createVideo(String path, boolean external) {
    try {
      return new FlixelFfmpegVideo(path);
    } catch (RuntimeException error) {
      Gdx.app.error("FlixelVideo", "Video is unavailable: " + error.getMessage());
      return new FlixelUnavailableVideo();
    }
  }
}
