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
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;

import org.flixelgdx.video.FlixelUnavailableVideo;
import org.flixelgdx.video.FlixelVideoBackend;
import org.flixelgdx.video.FlixelVideos;
import org.jetbrains.annotations.NotNull;

/**
 * Desktop video backend factory powered by libvlc.
 *
 * <p>Install it once in your desktop launcher, before the game starts:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *   FlixelVlcVideoHandler.install();
 *   FlixelLwjgl3Launcher.launch(new MyGame());
 * }
 * }</pre>
 *
 * <p>Installation is cheap: no native library is touched until the first
 * {@link org.flixelgdx.video.FlixelVideo FlixelVideo} is created, at which point
 * {@link FlixelVlcDiscovery} locates libvlc (bundled natives, a game-shipped
 * {@code vlc/} folder, or a system installation) and a single shared libvlc instance
 * is created for the whole game.
 *
 * <p>When no working VLC can be found at all, videos are still created; they just
 * stay in a never-ready state (see
 * {@link org.flixelgdx.video.FlixelUnavailableVideo FlixelUnavailableVideo}) and the
 * reason is logged, so a missing decoder degrades the game instead of crashing it.
 */
public final class FlixelVlcVideoHandler implements FlixelVideoBackend.Factory {

  private static Pointer instance;

  /** Set after discovery fails once, so every later video degrades without re-probing. */
  private static boolean unavailable;

  /**
   * Registers this handler as the video backend factory for {@link FlixelVideos}.
   * Safe to call multiple times.
   */
  public static void install() {
    FlixelVideos.setBackendFactory(new FlixelVlcVideoHandler());
  }

  /**
   * Returns the libvlc runtime version string, loading libvlc if needed.
   *
   * <p>Useful for diagnostics screens; most games never need this.
   *
   * @return The libvlc version, e.g. {@code "3.0.23 Vetinari"}.
   */
  @NotNull
  public static String getLibVlcVersion() {
    ensureInstance();
    return LibVlc.libvlc_get_version();
  }

  private static synchronized void ensureInstance() {
    if (instance != null) {
      return;
    }
    LibVlc.register(FlixelVlcDiscovery.load());
    // Headless flags: no interface, no X11 requirement, no console spam. Video output
    // is negotiated per player through the vmem callbacks, so libvlc never opens a window.
    String[] args = { "--intf=dummy", "--quiet", "--no-xlib" };
    Pointer created = LibVlc.libvlc_new(args.length, new StringArray(args));
    if (created == null) {
      throw new IllegalStateException(
          "libvlc_new failed. The located VLC installation may be incomplete (missing plugins).");
    }
    instance = created;
  }

  @Override
  public FlixelVideoBackend createVideo(String path, boolean external) {
    // A broken or missing VLC installation must not crash the game: the video
    // degrades to a backend that is never ready, and the reason is logged loudly so
    // the problem is diagnosable.
    if (unavailable) {
      return FlixelUnavailableVideo.INSTANCE;
    }
    try {
      ensureInstance();
    } catch (IllegalStateException | LinkageError error) {
      unavailable = true;
      Gdx.app.error("FlixelVideo",
          "Video playback is disabled for this session: " + error.getMessage());
      return FlixelUnavailableVideo.INSTANCE;
    }
    try {
      return new FlixelVlcVideo(instance, path);
    } catch (IllegalStateException error) {
      Gdx.app.error("FlixelVideo", error.getMessage());
      return FlixelUnavailableVideo.INSTANCE;
    }
  }
}
