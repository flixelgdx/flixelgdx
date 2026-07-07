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
package org.flixelgdx.backend.android.video;

import com.badlogic.gdx.Gdx;

import org.flixelgdx.video.FlixelUnavailableVideo;
import org.flixelgdx.video.FlixelVideoBackend;
import org.flixelgdx.video.FlixelVideos;

/**
 * Android video backend factory powered by the platform {@link android.media.MediaPlayer
 * MediaPlayer}.
 *
 * <p>Install it once in your Android launcher, before the game starts:
 *
 * <pre>{@code
 * public class MyAndroidLauncher extends AndroidApplication {
 *   protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     FlixelAndroidVideoHandler.install();
 *     FlixelAndroidLauncher.launch(new MyGame(), this);
 *   }
 * }
 * }</pre>
 *
 * <p>When a video cannot be opened, the game keeps running: the video degrades to a
 * backend that is never ready (see {@link FlixelUnavailableVideo}) and the reason is
 * logged, so a bad file or codec never crashes the game.
 */
public final class FlixelAndroidVideoHandler implements FlixelVideoBackend.Factory {

  /**
   * Registers this handler as the video backend factory for {@link FlixelVideos}.
   * Safe to call multiple times.
   */
  public static void install() {
    FlixelVideos.setBackendFactory(new FlixelAndroidVideoHandler());
  }

  @Override
  public FlixelVideoBackend createVideo(String path, boolean external) {
    try {
      return new FlixelAndroidVideo(path, external);
    } catch (RuntimeException error) {
      Gdx.app.error("FlixelVideo", "Video is unavailable: " + error.getMessage());
      return FlixelUnavailableVideo.INSTANCE;
    }
  }
}
