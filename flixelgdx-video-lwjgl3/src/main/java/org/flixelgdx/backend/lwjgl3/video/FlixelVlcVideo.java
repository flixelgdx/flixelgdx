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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import org.flixelgdx.video.FlixelVideo;
import org.flixelgdx.video.FlixelVideoQuality;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;

import java.nio.ByteBuffer;

/**
 * Desktop video backend that decodes through libvlc's in-memory video callbacks.
 *
 * <p>libvlc never owns a window here. The format callback negotiates an RGBA buffer
 * (optionally downscaled for {@link FlixelVideoQuality}), the lock callback hands
 * libvlc one of two pre-allocated native pixel buffers to decode into, and the display
 * callback flips a dirty flag. Audio decoding and output stay entirely inside libvlc.
 *
 * <p>On the render thread, {@link #updateMedia(float)} streams the latest completed
 * frame into a reusable {@link Texture} through two ping-ponged pixel buffer objects:
 * each frame the pixels are written into one PBO (orphaned first so the driver never
 * blocks on the previous transfer) while the texture upload reads from it via DMA, and
 * the next frame uses the other PBO. All buffers, textures, and PBOs are allocated once
 * per format and reused; the per-frame path allocates nothing.
 *
 * <p>Threading contract: every public method must be called on the render thread. The
 * inner callbacks run on libvlc decoder threads and only touch the shared frame buffers
 * under {@code bufferLock} plus a handful of volatile flags.
 */
final class FlixelVlcVideo extends FlixelVideo {

  /** Protects the frame buffer swap between the libvlc thread and the render thread. */
  private final Object bufferLock = new Object();

  /** Native pixel buffers libvlc decodes into; index flipped on every displayed frame. */
  private final Memory[] frameBuffers = new Memory[2];

  /** Direct views over {@link #frameBuffers}, reused for the PBO copies. */
  private final ByteBuffer[] frameViews = new ByteBuffer[2];

  /** Ping-ponged pixel buffer object handles for asynchronous texture uploads. */
  private final int[] pbos = new int[2];

  /** Reused out-parameters for libvlc_video_get_size (avoids per-frame allocation). */
  private final IntByReference sizeWidthRef = new IntByReference();
  private final IntByReference sizeHeightRef = new IntByReference();

  private Pointer mediaPlayer;
  private Pointer eventManager;

  @Nullable
  private Texture texture;

  @NotNull
  private volatile FlixelVideoQuality mediaQuality = FlixelVideoQuality.FULL;

  // Strong references keep the JNA callback trampolines alive while libvlc holds them.
  private final LibVlc.LockCallback lockCallback;
  private final LibVlc.UnlockCallback unlockCallback;
  private final LibVlc.DisplayCallback displayCallback;
  private final LibVlc.FormatCallback formatCallback;
  private final LibVlc.CleanupCallback cleanupCallback;
  private final LibVlc.EventCallback eventCallback;

  /** Decoded frame width in pixels, written by the format callback. */
  private volatile int frameWidth;

  /** Decoded frame height in pixels, written by the format callback. */
  private volatile int frameHeight;

  /** Buffer dimensions libvlc originally proposed, before quality scaling. */
  private volatile int setupSourceWidth;
  private volatile int setupSourceHeight;

  /**
   * Codec buffers carry alignment padding rows below the visible picture (a 1080p
   * H.264 stream decodes into a 1088 or 1090 row buffer). These are the dimensions of
   * the real picture inside the texture, derived from libvlc_video_get_size(...).
   * Render thread only.
   */
  private int visibleWidth;
  private int visibleHeight;

  /** Frame dimensions the visible size was computed for, to detect format changes. */
  private int visibleBasisWidth;
  private int visibleBasisHeight;

  /** Which frame buffer libvlc writes into next. Guarded by {@link #bufferLock}. */
  private int writeIndex;

  /** Which frame buffer holds the latest displayed frame. Guarded by {@link #bufferLock}. */
  private int readyIndex = -1;

  /** Which PBO receives the next upload. Render thread only. */
  private int pboIndex;

  /** Size in bytes of the current frame buffers. */
  private int frameBytes;

  /** Width/height the GPU objects were created for. Render thread only. */
  private int textureWidth;
  private int textureHeight;

  private float desiredVolume = 1f;
  private float desiredRate = 1f;

  /** Seek queued until the player actually reaches a seekable state. -1 = none. */
  private float pendingSeekMs = -1f;

  /** Set by the display callback when a new frame is ready for upload. */
  private volatile boolean frameDirty;

  /** Set by the end-reached event; consumed on the render thread. */
  private volatile boolean endReached;

  /** Set by the error event. */
  private volatile boolean playbackError;

  /** Sticky end state reported by {@link #isMediaEnded()} for non-looping playback. */
  private boolean ended;

  private boolean looping;

  /** The playback rate is re-pushed once per (re)start when the player is live. */
  private boolean settingsApplied;

  private boolean ready;

  private boolean disposed;

  /**
   * Creates a media player for the given file and wires all libvlc callbacks.
   *
   * @param instance The shared libvlc instance.
   * @param path Absolute path of the video file to open.
   * @throws IllegalStateException If libvlc cannot open the media.
   */
  FlixelVlcVideo(@NotNull Pointer instance, @NotNull String path) {
    super();
    Pointer media = LibVlc.libvlc_media_new_path(instance, path);
    if (media == null) {
      throw new IllegalStateException("libvlc could not open media: " + path);
    }
    mediaPlayer = LibVlc.libvlc_media_player_new_from_media(media);
    LibVlc.libvlc_media_release(media);
    if (mediaPlayer == null) {
      throw new IllegalStateException("libvlc could not create a media player for: " + path);
    }

    lockCallback = this::onLock;
    unlockCallback = (opaque, picture, planes) -> {
    };
    displayCallback = this::onDisplay;
    formatCallback = this::onFormat;
    cleanupCallback = opaque -> {
    };
    eventCallback = this::onEvent;

    LibVlc.libvlc_video_set_format_callbacks(mediaPlayer, formatCallback, cleanupCallback);
    LibVlc.libvlc_video_set_callbacks(mediaPlayer, lockCallback, unlockCallback, displayCallback, null);

    eventManager = LibVlc.libvlc_media_player_event_manager(mediaPlayer);
    LibVlc.libvlc_event_attach(eventManager, LibVlc.EVENT_END_REACHED, eventCallback, null);
    LibVlc.libvlc_event_attach(eventManager, LibVlc.EVENT_ENCOUNTERED_ERROR, eventCallback, null);
    FlixelVlcVideoHandler.track(this);
  }

  @Override
  protected void playMedia() {
    if (disposed) {
      return;
    }
    if (LibVlc.libvlc_media_player_get_state(mediaPlayer) == LibVlc.STATE_ENDED) {
      // libvlc 3 refuses to replay from the Ended state until the player is stopped.
      LibVlc.libvlc_media_player_stop(mediaPlayer);
    }
    ended = false;
    endReached = false;
    settingsApplied = false;
    LibVlc.libvlc_media_player_play(mediaPlayer);
  }

  @Override
  protected void pauseMedia() {
    if (disposed) {
      return;
    }
    LibVlc.libvlc_media_player_set_pause(mediaPlayer, 1);
  }

  @Override
  protected void resumeMedia() {
    if (disposed) {
      return;
    }
    LibVlc.libvlc_media_player_set_pause(mediaPlayer, 0);
  }

  @Override
  protected void stopMedia() {
    if (disposed) {
      return;
    }
    ended = false;
    endReached = false;
    pendingSeekMs = -1f;
    LibVlc.libvlc_media_player_stop(mediaPlayer);
  }

  @Override
  protected boolean isMediaPlaying() {
    return !disposed && LibVlc.libvlc_media_player_get_state(mediaPlayer) == LibVlc.STATE_PLAYING;
  }

  @Override
  protected boolean isMediaEnded() {
    if (disposed) {
      return false;
    }
    return ended || LibVlc.libvlc_media_player_get_state(mediaPlayer) == LibVlc.STATE_ENDED;
  }

  @Override
  protected boolean isMediaReady() {
    return ready;
  }

  @Override
  protected float getMediaTime() {
    if (disposed) {
      return 0f;
    }
    if (pendingSeekMs >= 0f) {
      return pendingSeekMs;
    }
    long time = LibVlc.libvlc_media_player_get_time(mediaPlayer);
    return time < 0 ? 0f : time;
  }

  @Override
  protected void setMediaTime(float timeMs) {
    if (disposed) {
      return;
    }
    float target = Math.max(0f, timeMs);
    int state = LibVlc.libvlc_media_player_get_state(mediaPlayer);
    if (state == LibVlc.STATE_PLAYING || state == LibVlc.STATE_PAUSED) {
      LibVlc.libvlc_media_player_set_time(mediaPlayer, (long) target);
      pendingSeekMs = -1f;
    } else {
      // The player is still opening (or stopped); apply once it is actually running.
      pendingSeekMs = target;
    }
    ended = false;
    endReached = false;
  }

  @Override
  protected float getMediaLength() {
    if (disposed) {
      return 0f;
    }
    long length = LibVlc.libvlc_media_player_get_length(mediaPlayer);
    return length < 0 ? 0f : length;
  }

  @Override
  protected float getMediaRate() {
    return desiredRate;
  }

  @Override
  protected void setMediaRate(float rate) {
    if (disposed || rate <= 0f) {
      return;
    }
    desiredRate = rate;
    LibVlc.libvlc_media_player_set_rate(mediaPlayer, rate);
  }

  @Override
  protected boolean isMediaLooped() {
    return looping;
  }

  @Override
  protected void setMediaLooped(boolean looped) {
    this.looping = looped;
  }

  @Override
  protected float getMediaVolume() {
    return desiredVolume;
  }

  @Override
  protected void setMediaVolume(float volume) {
    desiredVolume = Math.max(0f, Math.min(1f, volume));
    if (!disposed) {
      LibVlc.libvlc_audio_set_volume(mediaPlayer, (int) (desiredVolume * 100f));
    }
  }

  @Override
  protected void applyMediaQuality(@NotNull FlixelVideoQuality quality) {
    if (disposed || this.mediaQuality == quality) {
      return;
    }
    this.mediaQuality = quality;
    int state = LibVlc.libvlc_media_player_get_state(mediaPlayer);
    if (state == LibVlc.STATE_PLAYING || state == LibVlc.STATE_PAUSED) {
      // The vmem format is negotiated at playback start, so rebuild the pipeline in
      // place: remember where we were, restart, and seek back.
      float resumeAt = getMediaTime();
      boolean wasPaused = state == LibVlc.STATE_PAUSED;
      LibVlc.libvlc_media_player_stop(mediaPlayer);
      playMedia();
      pendingSeekMs = resumeAt;
      if (wasPaused) {
        // Let the pipeline restart and produce the frame, then re-pause on the next pump.
        LibVlc.libvlc_media_player_set_pause(mediaPlayer, 1);
      }
    }
  }

  @Override
  protected int getMediaVideoWidth() {
    return visibleWidth > 0 ? visibleWidth : frameWidth;
  }

  @Override
  protected int getMediaVideoHeight() {
    return visibleHeight > 0 ? visibleHeight : frameHeight;
  }

  @Override
  protected void updateMedia(float elapsed) {
    if (disposed) {
      return;
    }

    if (playbackError) {
      playbackError = false;
      Gdx.app.error("FlixelVideo", "libvlc reported a playback error; the video was stopped.");
    }

    if (endReached) {
      endReached = false;
      if (looping) {
        // libvlc 3 parks the player in the Ended state; restart from the render thread
        // (never from the event thread, which libvlc forbids re-entering).
        LibVlc.libvlc_media_player_stop(mediaPlayer);
        playMedia();
      } else {
        ended = true;
      }
    }

    int state = LibVlc.libvlc_media_player_get_state(mediaPlayer);
    if (state == LibVlc.STATE_PLAYING) {
      if (pendingSeekMs >= 0f) {
        LibVlc.libvlc_media_player_set_time(mediaPlayer, (long) pendingSeekMs);
        pendingSeekMs = -1f;
      }
      // Each player re-asserts its own volume every frame while it is live. Guarding
      // this with libvlc_audio_get_volume(...) is not enough: that call returns the
      // value libvlc last stored, not the real output gain, so when an audio server
      // restores a different stream's volume onto this one (PulseAudio restores the
      // application's most recent stream) the guard sees no change and never corrects
      // it, and two videos end up sharing a volume. Pushing the value unconditionally
      // is a cheap idempotent native call that keeps every player pinned to its own
      // volume, which is what stops simultaneous videos from mixing their levels up.
      LibVlc.libvlc_audio_set_volume(mediaPlayer, (int) (desiredVolume * 100f));
      if (!settingsApplied) {
        settingsApplied = true;
        if (desiredRate != 1f) {
          LibVlc.libvlc_media_player_set_rate(mediaPlayer, desiredRate);
        }
      }
    }

    if (frameDirty) {
      uploadLatestFrame();
    }
  }

  @Override
  @Nullable
  protected Texture getMediaTexture() {
    return ready ? texture : null;
  }

  @Override
  protected void disposeMedia() {
    if (disposed) {
      return;
    }
    disposed = true;
    autoPaused = false;
    FlixelVlcVideoHandler.untrack(this);
    LibVlc.libvlc_event_detach(eventManager, LibVlc.EVENT_END_REACHED, eventCallback, null);
    LibVlc.libvlc_event_detach(eventManager, LibVlc.EVENT_ENCOUNTERED_ERROR, eventCallback, null);
    LibVlc.libvlc_media_player_stop(mediaPlayer);
    LibVlc.libvlc_media_player_release(mediaPlayer);
    mediaPlayer = null;
    eventManager = null;
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    if (pbos[0] != 0) {
      GL15C.glDeleteBuffers(pbos);
      pbos[0] = 0;
      pbos[1] = 0;
    }
    synchronized (bufferLock) {
      frameBuffers[0] = null;
      frameBuffers[1] = null;
      frameViews[0] = null;
      frameViews[1] = null;
      readyIndex = -1;
    }
    ready = false;
  }

  void autoPause() {
    if (isMediaPlaying()) {
      pauseMedia();
      autoPaused = true;
    }
  }

  void autoResume() {
    if (autoPaused) {
      autoPaused = false;
      resumeMedia();
    }
  }

  /**
   * Streams the most recent completed frame into the texture through the PBO pair.
   *
   * <p>The write PBO is orphaned before the copy so the driver can hand back fresh
   * storage instead of stalling on an in-flight transfer, and the texture upload reads
   * from the PBO (a GPU-side DMA), not from client memory. Alternating PBOs each frame
   * keeps the copy of frame N independent from the upload of frame N-1.
   */
  private void uploadLatestFrame() {
    // The whole upload runs under bufferLock so the dimensions, byte count, and frame
    // contents stay consistent even if libvlc renegotiates the format mid-play. The
    // lock is only held for the PBO copy (about a millisecond); if libvlc wants to
    // swap buffers meanwhile it briefly waits, which beats showing a torn frame.
    synchronized (bufferLock) {
      int width = frameWidth;
      int height = frameHeight;
      if (width <= 0 || height <= 0 || readyIndex < 0) {
        return;
      }
      ensureGpuObjects(width, height);
      Texture target = texture;
      if (target == null) {
        return;
      }

      int pbo = pbos[pboIndex];
      pboIndex ^= 1;

      GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, pbo);
      GL15C.glBufferData(GL21C.GL_PIXEL_UNPACK_BUFFER, frameBytes, GL15C.GL_STREAM_DRAW);
      ByteBuffer frame = frameViews[readyIndex];
      frame.clear();
      GL15C.glBufferSubData(GL21C.GL_PIXEL_UNPACK_BUFFER, 0, frame);
      frameDirty = false;

      target.bind();
      GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, width, height,
          GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, 0L);
      GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0);
      ready = true;
    }
    updateVisibleSize();
  }

  /**
   * Derives the visible picture size inside the (padding-aligned) frame buffer.
   *
   * <p>libvlc reports the true display resolution through libvlc_video_get_size(...);
   * scaling it by the ratio between our decode buffer and the source buffer maps it
   * into texture pixels, so draw code can crop away the codec padding rows.
   */
  private void updateVisibleSize() {
    int width = frameWidth;
    int height = frameHeight;
    if (width <= 0 || height <= 0) {
      return;
    }
    if (visibleWidth > 0 && width == visibleBasisWidth && height == visibleBasisHeight) {
      return;
    }
    if (LibVlc.libvlc_video_get_size(mediaPlayer, 0, sizeWidthRef, sizeHeightRef) != 0) {
      return;
    }
    int displayWidth = sizeWidthRef.getValue();
    int displayHeight = sizeHeightRef.getValue();
    int sourceWidth = setupSourceWidth;
    int sourceHeight = setupSourceHeight;
    if (displayWidth <= 0 || displayHeight <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
      return;
    }
    visibleWidth = Math.min(width, Math.round(width * (displayWidth / (float) sourceWidth)));
    visibleHeight = Math.min(height, Math.round(height * (displayHeight / (float) sourceHeight)));
    visibleBasisWidth = width;
    visibleBasisHeight = height;
  }

  /** (Re)creates the texture and PBO pair when the decoded frame size changes. */
  private void ensureGpuObjects(int width, int height) {
    if (texture != null && width == textureWidth && height == textureHeight) {
      return;
    }
    if (texture != null) {
      texture.dispose();
    }
    texture = new Texture(new GLOnlyTextureData(width, height, 0,
        GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE));
    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    textureWidth = width;
    textureHeight = height;
    if (pbos[0] == 0) {
      GL15C.glGenBuffers(pbos);
    }
    pboIndex = 0;
    ready = false;
  }

  /** libvlc format negotiation; runs on a libvlc thread. */
  private int onFormat(PointerByReference opaque, Pointer chroma, IntByReference width,
      IntByReference height, Pointer pitches, Pointer lines) {
    int sourceWidth = width.getValue();
    int sourceHeight = height.getValue();
    setupSourceWidth = sourceWidth;
    setupSourceHeight = sourceHeight;
    float scale = mediaQuality.getScale();
    // Even dimensions keep chroma subsampled sources (which is nearly all of them) happy.
    int decodeWidth = Math.max(2, ((int) (sourceWidth * scale)) & ~1);
    int decodeHeight = Math.max(2, ((int) (sourceHeight * scale)) & ~1);

    chroma.setByte(0, (byte) 'R');
    chroma.setByte(1, (byte) 'G');
    chroma.setByte(2, (byte) 'B');
    chroma.setByte(3, (byte) 'A');
    width.setValue(decodeWidth);
    height.setValue(decodeHeight);
    pitches.setInt(0, decodeWidth * 4);
    lines.setInt(0, decodeHeight);

    int bytes = decodeWidth * 4 * decodeHeight;
    synchronized (bufferLock) {
      if (frameBuffers[0] == null || frameBytes != bytes) {
        frameBuffers[0] = new Memory(bytes);
        frameBuffers[1] = new Memory(bytes);
        frameViews[0] = frameBuffers[0].getByteBuffer(0, bytes);
        frameViews[1] = frameBuffers[1].getByteBuffer(0, bytes);
        frameBytes = bytes;
        writeIndex = 0;
        readyIndex = -1;
      }
      frameWidth = decodeWidth;
      frameHeight = decodeHeight;
    }
    return 1;
  }

  /** libvlc asks for the buffer to decode the next frame into; runs on a libvlc thread. */
  private Pointer onLock(Pointer opaque, Pointer planes) {
    synchronized (bufferLock) {
      Memory buffer = frameBuffers[writeIndex];
      planes.setPointer(0, buffer);
    }
    return null;
  }

  /** A decoded frame is ready to be shown; runs on a libvlc thread. */
  private void onDisplay(Pointer opaque, Pointer picture) {
    synchronized (bufferLock) {
      readyIndex = writeIndex;
      writeIndex ^= 1;
      frameDirty = true;
    }
  }

  /** Player events; runs on the libvlc event thread, so it only flips flags. */
  private void onEvent(Pointer event, Pointer userData) {
    int type = event.getInt(0);
    if (type == LibVlc.EVENT_END_REACHED) {
      endReached = true;
    } else if (type == LibVlc.EVENT_ENCOUNTERED_ERROR) {
      playbackError = true;
      endReached = true;
    }
  }
}
