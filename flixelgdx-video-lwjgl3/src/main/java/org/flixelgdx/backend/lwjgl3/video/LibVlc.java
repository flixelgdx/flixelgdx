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

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Minimal libvlc 3.x binding used by the desktop video backend.
 *
 * <p>The methods are registered against the discovered libvlc shared library through
 * JNA's direct mapping, which binds each declaration below to the exported C symbol of
 * the same name via JNI. Only the functions the video pipeline needs are mapped; this
 * is intentionally not a general-purpose libvlc wrapper.
 *
 * <p>All callback interfaces are invoked from libvlc's own decoder threads, never from
 * the render thread. Implementations must therefore stay allocation-free and hand data
 * over through fields that the render thread polls (see {@code FlixelVlcVideo}).
 */
final class LibVlc {

  /** libvlc_state_t: media player is actively playing. */
  static final int STATE_PLAYING = 3;

  /** libvlc_state_t: media player is paused. */
  static final int STATE_PAUSED = 4;

  /** libvlc_state_t: media player reached the end of the media. */
  static final int STATE_ENDED = 6;

  /** libvlc_state_t: media player hit an unrecoverable error. */
  static final int STATE_ERROR = 7;

  /** libvlc_event_e: playback reached the end of the stream. */
  static final int EVENT_END_REACHED = 265;

  /** libvlc_event_e: playback stopped due to an error. */
  static final int EVENT_ENCOUNTERED_ERROR = 266;

  private static boolean registered;

  private LibVlc() {}

  /**
   * Binds the native methods of this class to the given libvlc library. Safe to call
   * more than once; only the first call registers.
   *
   * @param library The loaded libvlc native library.
   */
  static synchronized void register(NativeLibrary library) {
    if (registered) {
      return;
    }
    Native.register(LibVlc.class, library);
    registered = true;
  }

  static native Pointer libvlc_new(int argc, Pointer argv);

  static native void libvlc_release(Pointer instance);

  static native String libvlc_get_version();

  static native Pointer libvlc_media_new_path(Pointer instance, String path);

  static native void libvlc_media_release(Pointer media);

  static native Pointer libvlc_media_player_new_from_media(Pointer media);

  static native void libvlc_media_player_release(Pointer mediaPlayer);

  static native int libvlc_media_player_play(Pointer mediaPlayer);

  static native void libvlc_media_player_set_pause(Pointer mediaPlayer, int doPause);

  static native void libvlc_media_player_stop(Pointer mediaPlayer);

  static native int libvlc_media_player_get_state(Pointer mediaPlayer);

  static native long libvlc_media_player_get_time(Pointer mediaPlayer);

  static native void libvlc_media_player_set_time(Pointer mediaPlayer, long timeMs);

  static native long libvlc_media_player_get_length(Pointer mediaPlayer);

  static native float libvlc_media_player_get_rate(Pointer mediaPlayer);

  static native int libvlc_media_player_set_rate(Pointer mediaPlayer, float rate);

  static native int libvlc_audio_get_volume(Pointer mediaPlayer);

  static native int libvlc_audio_set_volume(Pointer mediaPlayer, int volume);

  static native int libvlc_video_get_size(Pointer mediaPlayer, int num, IntByReference width,
      IntByReference height);

  static native void libvlc_video_set_callbacks(Pointer mediaPlayer, LockCallback lock,
      UnlockCallback unlock, DisplayCallback display, Pointer opaque);

  static native void libvlc_video_set_format_callbacks(Pointer mediaPlayer, FormatCallback setup,
      CleanupCallback cleanup);

  static native Pointer libvlc_media_player_event_manager(Pointer mediaPlayer);

  static native int libvlc_event_attach(Pointer eventManager, int eventType, EventCallback callback,
      Pointer userData);

  static native void libvlc_event_detach(Pointer eventManager, int eventType, EventCallback callback,
      Pointer userData);

  /**
   * libvlc_video_lock_cb: libvlc asks where to decode the next frame into.
   *
   * <p>The implementation writes the address of a pre-allocated pixel buffer into
   * {@code planes[0]} and returns an opaque picture handle (unused here, so {@code null}).
   */
  interface LockCallback extends Callback {
    Pointer invoke(Pointer opaque, Pointer planes);
  }

  /** libvlc_video_unlock_cb: libvlc finished writing the frame started in lock. */
  interface UnlockCallback extends Callback {
    void invoke(Pointer opaque, Pointer picture, Pointer planes);
  }

  /** libvlc_video_display_cb: the frame written in lock/unlock is ready to show. */
  interface DisplayCallback extends Callback {
    void invoke(Pointer opaque, Pointer picture);
  }

  /**
   * libvlc_video_format_cb: negotiate the in-memory pixel format.
   *
   * <p>The implementation reads the source dimensions, writes the desired chroma fourcc
   * into {@code chroma}, may shrink {@code width}/{@code height} (decode quality), and
   * fills {@code pitches}/{@code lines} for plane 0. Returns the number of pixel
   * buffers to allocate, or {@code 0} on error.
   */
  interface FormatCallback extends Callback {
    int invoke(PointerByReference opaque, Pointer chroma, IntByReference width,
        IntByReference height, Pointer pitches, Pointer lines);
  }

  /** libvlc_video_cleanup_cb: the format negotiated in setup is being torn down. */
  interface CleanupCallback extends Callback {
    void invoke(Pointer opaque);
  }

  /**
   * libvlc_callback_t: generic event notification.
   *
   * <p>The event struct starts with the {@code int} event type at offset 0, which is all
   * this backend reads. Never call back into libvlc from inside this callback; libvlc
   * documents that re-entering the player from its event thread can deadlock.
   */
  interface EventCallback extends Callback {
    void invoke(Pointer event, Pointer userData);
  }
}
