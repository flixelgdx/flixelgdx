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
import com.badlogic.gdx.utils.Array;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.lwjgl3.window.FlixelLwjgl3WindowListener;
import org.flixelgdx.video.FlixelUnavailableVideo;
import org.flixelgdx.video.FlixelVideo;
import org.flixelgdx.video.FlixelVideoFactory;
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
 * {@link FlixelVideo} is created, at which point {@link FlixelVlcDiscovery} locates
 * libvlc (bundled natives, a game-shipped {@code vlc/} folder, or a system installation)
 * and a single shared libvlc instance is created for the whole game.
 *
 * <p>When no working VLC can be found at all, videos are still created; they just
 * stay in a never-ready state (see {@link FlixelUnavailableVideo}) and the
 * reason is logged, so a missing decoder degrades the game instead of crashing it.
 */
public final class FlixelVlcVideoHandler implements FlixelVideoFactory {

  private static final Array<FlixelVlcVideo> activeVideos = new Array<>(8);

  private static Pointer instance;

  /** Set after discovery fails once, so every later video degrades without re-probing. */
  private static boolean unavailable;

  /** Guards focus hook registration so repeated {@link #install()} calls do not stack duplicates. */
  private static boolean installed;

  /**
   * Registers this handler as the video backend factory for {@link FlixelVideos} and wires
   * up desktop focus hooks for automatic video pause/resume. Safe to call multiple times.
   */
  public static void install() {
    FlixelVideos.setBackendFactory(new FlixelVlcVideoHandler());
    if (!installed) {
      installed = true;
      FlixelLwjgl3WindowListener.addFocusHooks(
          FlixelVlcVideoHandler::onFocusGained,
          FlixelVlcVideoHandler::onFocusLost);
    }
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

  @Override
  public FlixelVideo createVideo(String path, boolean external) {
    // A broken or missing VLC installation must not crash the game: the video
    // degrades to a backend that is never ready, and the reason is logged loudly so
    // the problem is diagnosable.
    if (unavailable) {
      return new FlixelUnavailableVideo();
    }
    try {
      ensureInstance();
    } catch (IllegalStateException | LinkageError error) {
      unavailable = true;
      Gdx.app.error("FlixelVideo", "Video playback is disabled for this session: " + error.getMessage());
      return new FlixelUnavailableVideo();
    }
    try {
      return new FlixelVlcVideo(instance, path);
    } catch (IllegalStateException error) {
      Gdx.app.error("FlixelVideo", error.getMessage());
      return new FlixelUnavailableVideo();
    }
  }

  static void track(FlixelVlcVideo video) {
    activeVideos.add(video);
  }

  static void untrack(FlixelVlcVideo video) {
    activeVideos.removeValue(video, true);
  }

  private static synchronized void ensureInstance() {
    if (instance != null) {
      return;
    }
    LibVlc.register(FlixelVlcDiscovery.load());
    // HVideo output is negotiated per player through the vmem callbacks, so libvlc never opens a window.
    String[] args = { "--intf=dummy", "--quiet", "--no-xlib" };
    Pointer created = LibVlc.libvlc_new(args.length, new StringArray(args));
    if (created == null) {
      throw new IllegalStateException(
          "libvlc_new failed. The located VLC installation may be incomplete (missing plugins).");
    }
    instance = created;
    // A one-time confirmation of which libvlc actually satisfied the game, so a
    // "plugins cannot be found" report can be traced to the exact install that loaded.
    Gdx.app.log("FlixelVideo", "libvlc " + LibVlc.libvlc_get_version()
        + " initialized from " + FlixelVlcDiscovery.getLoadedFrom() + ".");
  }

  private static void onFocusLost() {
    FlixelGame game = Flixel.game;
    if (game == null || !game.autoPause) {
      return;
    }
    for (int i = 0, n = activeVideos.size; i < n; i++) {
      activeVideos.get(i).autoPause();
    }
  }

  private static void onFocusGained() {
    for (int i = 0, n = activeVideos.size; i < n; i++) {
      activeVideos.get(i).autoResume();
    }
  }
}
