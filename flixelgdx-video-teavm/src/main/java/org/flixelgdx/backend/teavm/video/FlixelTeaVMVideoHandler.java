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
package org.flixelgdx.backend.teavm.video;

import org.flixelgdx.video.FlixelVideo;
import org.flixelgdx.video.FlixelVideoFactory;
import org.flixelgdx.video.FlixelVideos;

/**
 * Web video backend factory powered by the browser's own decoder.
 *
 * <p>Install it once in your web launcher, before the game starts:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *   FlixelTeaVMVideoHandler.install();
 *   FlixelTeaVMLauncher.launch(new MyGame());
 * }
 * }</pre>
 */
public final class FlixelTeaVMVideoHandler implements FlixelVideoFactory {

  /**
   * Registers this handler as the video backend factory for {@link FlixelVideos}.
   * Safe to call multiple times.
   */
  public static void install() {
    FlixelVideos.setBackendFactory(new FlixelTeaVMVideoHandler());
  }

  @Override
  public FlixelVideo createVideo(String path, boolean external) {
    return new FlixelTeaVMVideo(resolveUrl(path, external));
  }

  /**
   * Maps an internal asset path to the URL it is served from.
   *
   * <p>The web packaging (flixelgdx-teavm-plugin) copies game assets into an
   * {@code assets/} directory next to {@code index.html}, so a relative internal path
   * like {@code "videos/intro.mp4"} is reachable at {@code "assets/videos/intro.mp4"}.
   * Absolute URLs and external paths pass through untouched.
   */
  private static String resolveUrl(String path, boolean external) {
    if (external || path.contains("://") || path.startsWith("/")) {
      return path;
    }
    return "assets/" + path;
  }
}
