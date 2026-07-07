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
   *
   * <p>JavaCPP's {@code Pointer} class enforces a physical-memory guard that defaults
   * to four times the JVM heap ({@code -Xmx}). FFmpeg's native codec libraries can
   * exceed that limit on their own before any video is even opened, causing a spurious
   * {@link OutOfMemoryError}. This method raises the cap to the machine's total physical
   * RAM so the guard only fires on genuine runaway growth, not on a normal codec load.
   */
  public static void install() {
    // JavaCPP's Pointer reads this property in its static initializer to cap
    // physical-memory usage. The default is 4x the JVM heap (-Xmx), so with a
    // small heap FFmpeg's codec libraries trigger the guard just by loading. Set
    // it to Long.MAX_VALUE to remove the cap; the system itself limits how much
    // RAM the process can actually consume, so the guard adds nothing here.
    System.setProperty("org.bytedeco.javacpp.maxPhysicalBytes",
        Long.toString(Long.MAX_VALUE));
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
