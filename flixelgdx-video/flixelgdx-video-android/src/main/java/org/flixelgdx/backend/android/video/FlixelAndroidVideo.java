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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.opengl.GLES11Ext;
import android.view.Surface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import org.flixelgdx.video.FlixelVideoBackend;
import org.flixelgdx.video.FlixelVideoQuality;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Android {@link FlixelVideoBackend} that decodes through the platform
 * {@link MediaPlayer} into a {@link SurfaceTexture}.
 *
 * <p>The MediaPlayer renders each frame into a {@code GL_TEXTURE_EXTERNAL_OES} texture
 * fed by a SurfaceTexture, which keeps decoding (and its audio) entirely on the
 * platform side. libGDX cannot draw an external texture directly, so every frame is
 * blitted into a normal {@link FrameBuffer} texture by {@link FlixelOesBlitter}. The
 * framework then draws that ordinary texture through the regular batch, so a video
 * respects state draw order exactly like a sprite: added first means drawn under, added
 * last means drawn over.
 *
 * <p>Using MediaPlayer gives audio, per-video volume, looping, seeking, and playback
 * rate from the platform, so each video keeps its own audio level with no cross-talk.
 *
 * <p>Threading contract: {@link #update()} and {@link #dispose()} must run on the render
 * thread (they touch GL objects and the SurfaceTexture). MediaPlayer fires its prepared,
 * completion, error, and size callbacks on its own threads, so those only set volatile
 * flags that the render thread reads.
 */
final class FlixelAndroidVideo implements FlixelVideoBackend {

  /** Serializes MediaPlayer state changes against its callback threads and disposal. */
  private final Object playerLock = new Object();

  /** SurfaceTexture transform matrix for the current frame; render thread only. */
  private final float[] stMatrix = new float[16];

  private MediaPlayer player;
  private SurfaceTexture surfaceTexture;
  private Surface surface;

  @Nullable
  private FrameBuffer fbo;

  private FlixelOesBlitter blitter;

  /** Kept open for the media player's lifetime when playing a file-descriptor asset. */
  @Nullable
  private AssetFileDescriptor assetDescriptor;

  @NotNull
  private volatile FlixelVideoQuality quality = FlixelVideoQuality.FULL;

  private int oesTextureId;

  /** Source frame size reported by the decoder; written on a MediaPlayer callback thread. */
  private volatile int sourceWidth;
  private volatile int sourceHeight;

  /** Size the framebuffer texture was last built at (already quality-scaled). */
  private int fboWidth;
  private int fboHeight;

  private volatile float volume = 1f;
  private volatile float desiredRate = 1f;

  /** Seek requested before the player was prepared; applied once ready. -1 = none. */
  private float pendingSeekMs = -1f;

  private volatile boolean prepared;
  private volatile boolean pendingPlay;
  private volatile boolean frameAvailable;
  private volatile boolean ended;
  private volatile boolean errored;
  private volatile boolean looping;

  /** True once at least one frame has been blitted into the framebuffer. */
  private boolean ready;

  private boolean disposed;

  /**
   * Creates the decoder and wires it to a fresh OES texture.
   *
   * @param path Asset-relative path for internal assets, or an absolute path/URL for
   *     external sources.
   * @param external {@code true} when {@code path} points outside the game's assets.
   * @throws IllegalStateException If the media cannot be opened or the GL objects fail.
   */
  FlixelAndroidVideo(@NotNull String path, boolean external) {
    try {
      player = new MediaPlayer();
      setDataSource(player, path, external);

      oesTextureId = createOesTexture();
      surfaceTexture = new SurfaceTexture(oesTextureId);
      surfaceTexture.setOnFrameAvailableListener(texture -> frameAvailable = true);
      surface = new Surface(surfaceTexture);
      blitter = new FlixelOesBlitter();

      player.setSurface(surface);
      player.setOnPreparedListener(this::onPrepared);
      player.setOnVideoSizeChangedListener((mp, width, height) -> {
        sourceWidth = width;
        sourceHeight = height;
      });
      player.setOnCompletionListener(mp -> {
        if (!looping) {
          ended = true;
        }
      });
      player.setOnErrorListener((mp, what, extra) -> {
        errored = true;
        Gdx.app.error("FlixelVideo", "MediaPlayer error (what=" + what + ", extra=" + extra + ").");
        return true;
      });
      player.prepareAsync();
    } catch (Exception error) {
      dispose();
      throw new IllegalStateException("Could not open video: " + path, error);
    }
  }

  @Override
  public void play() {
    if (disposed || errored) {
      return;
    }
    ended = false;
    if (!prepared) {
      pendingPlay = true;
      return;
    }
    startPlayback();
  }

  @Override
  public void pause() {
    synchronized (playerLock) {
      if (prepared && !disposed && player != null && player.isPlaying()) {
        player.pause();
      }
    }
  }

  @Override
  public void resume() {
    if (disposed || errored) {
      return;
    }
    if (!prepared) {
      pendingPlay = true;
      return;
    }
    startPlayback();
  }

  @Override
  public void stop() {
    ended = false;
    pendingSeekMs = -1f;
    synchronized (playerLock) {
      if (prepared && !disposed && player != null) {
        player.pause();
        player.seekTo(0);
      }
    }
  }

  @Override
  public boolean isPlaying() {
    synchronized (playerLock) {
      return prepared && !disposed && player != null && player.isPlaying();
    }
  }

  @Override
  public boolean isEnd() {
    return ended;
  }

  @Override
  public boolean isReady() {
    return ready;
  }

  @Override
  public float getTime() {
    synchronized (playerLock) {
      if (!prepared || disposed || player == null) {
        return Math.max(pendingSeekMs, 0f);
      }
      return Math.max(0, player.getCurrentPosition());
    }
  }

  @Override
  public void setTime(float timeMs) {
    float target = Math.max(0f, timeMs);
    ended = false;
    synchronized (playerLock) {
      if (prepared && !disposed && player != null) {
        player.seekTo(Math.round(target));
        pendingSeekMs = -1f;
      } else {
        pendingSeekMs = target;
      }
    }
  }

  @Override
  public float getLength() {
    synchronized (playerLock) {
      if (!prepared || disposed || player == null) {
        return 0f;
      }
      int duration = player.getDuration();
      return duration < 0 ? 0f : duration;
    }
  }

  @Override
  public float getRate() {
    return desiredRate;
  }

  @Override
  public void setRate(float rate) {
    if (rate <= 0f) {
      return;
    }
    desiredRate = rate;
    synchronized (playerLock) {
      if (prepared && !disposed && player != null && player.isPlaying()) {
        applyRate();
      }
    }
  }

  @Override
  public boolean isLooping() {
    return looping;
  }

  @Override
  public void setLooping(boolean looping) {
    this.looping = looping;
    synchronized (playerLock) {
      if (prepared && !disposed && player != null) {
        player.setLooping(looping);
      }
    }
  }

  @Override
  public float getVolume() {
    return volume;
  }

  @Override
  public void setVolume(float volume) {
    this.volume = Math.max(0f, Math.min(1f, volume));
    synchronized (playerLock) {
      if (prepared && !disposed && player != null) {
        player.setVolume(this.volume, this.volume);
      }
    }
  }

  @Override
  public void setQuality(@NotNull FlixelVideoQuality quality) {
    // MediaPlayer always decodes at the source resolution, so the preset scales the
    // framebuffer the frame is copied into instead: a lower preset stores a smaller,
    // softer texture and uploads less. The change lands the next time a frame is blitted.
    this.quality = quality;
  }

  @Override
  public int getVideoWidth() {
    return scaledDimension(sourceWidth);
  }

  @Override
  public int getVideoHeight() {
    return scaledDimension(sourceHeight);
  }

  @Override
  public void update() {
    if (disposed || errored) {
      return;
    }

    if (pendingSeekMs >= 0f && prepared) {
      synchronized (playerLock) {
        if (!disposed && player != null) {
          player.seekTo(Math.round(pendingSeekMs));
        }
      }
      pendingSeekMs = -1f;
    }

    if (frameAvailable && surfaceTexture != null) {
      frameAvailable = false;
      surfaceTexture.updateTexImage();
      surfaceTexture.getTransformMatrix(stMatrix);
      int width = getVideoWidth();
      int height = getVideoHeight();
      if (width > 0 && height > 0) {
        ensureFbo(width, height);
        blitter.blit(oesTextureId, stMatrix, fbo);
        ready = true;
      }
    }
  }

  @Override
  @Nullable
  public Texture getTexture() {
    return ready && fbo != null ? fbo.getColorBufferTexture() : null;
  }

  @Override
  public void dispose() {
    synchronized (playerLock) {
      if (disposed) {
        return;
      }
      disposed = true;
      if (player != null) {
        try {
          player.setOnPreparedListener(null);
          player.setOnCompletionListener(null);
          player.setOnErrorListener(null);
          player.setOnVideoSizeChangedListener(null);
          player.reset();
          player.release();
        } catch (Exception ignored) {
          // A player that never finished preparing can throw here; nothing to recover.
        }
        player = null;
      }
    }
    if (surface != null) {
      surface.release();
      surface = null;
    }
    if (surfaceTexture != null) {
      surfaceTexture.setOnFrameAvailableListener(null);
      surfaceTexture.release();
      surfaceTexture = null;
    }
    if (oesTextureId != 0) {
      Gdx.gl.glDeleteTexture(oesTextureId);
      oesTextureId = 0;
    }
    if (fbo != null) {
      fbo.dispose();
      fbo = null;
    }
    if (blitter != null) {
      blitter.dispose();
      blitter = null;
    }
    if (assetDescriptor != null) {
      try {
        assetDescriptor.close();
      } catch (IOException ignored) {
        // Best effort; the process is releasing the descriptor regardless.
      }
      assetDescriptor = null;
    }
    ready = false;
  }

  /** Applies volume, looping, a queued seek, and a queued play once the player is ready. */
  private void onPrepared(MediaPlayer mp) {
    synchronized (playerLock) {
      if (disposed) {
        return;
      }
      prepared = true;
      try {
        mp.setVolume(volume, volume);
        mp.setLooping(looping);
      } catch (IllegalStateException ignored) {
        // The player was torn down between prepare and this callback.
      }
      if (pendingSeekMs >= 0f) {
        mp.seekTo(Math.round(pendingSeekMs));
        pendingSeekMs = -1f;
      }
      if (pendingPlay) {
        pendingPlay = false;
        startPlayback();
      }
    }
  }

  /** Starts (or restarts, if ended) playback and re-applies the playback rate. */
  private void startPlayback() {
    synchronized (playerLock) {
      if (disposed || player == null) {
        return;
      }
      try {
        if (ended) {
          player.seekTo(0);
          ended = false;
        }
        player.start();
        if (desiredRate != 1f) {
          applyRate();
        }
      } catch (IllegalStateException error) {
        Gdx.app.error("FlixelVideo", "MediaPlayer start failed: " + error.getMessage());
      }
    }
  }

  /** Pushes {@link #desiredRate} onto the player. Caller must hold {@link #playerLock}. */
  private void applyRate() {
    try {
      PlaybackParams params = new PlaybackParams();
      params.setSpeed(desiredRate);
      player.setPlaybackParams(params);
    } catch (Exception ignored) {
      // Some codecs reject a rate change; the video simply keeps its current speed.
    }
  }

  /** (Re)creates the framebuffer when the quality-scaled frame size changes. */
  private void ensureFbo(int width, int height) {
    if (fbo != null && width == fboWidth && height == fboHeight) {
      return;
    }
    if (fbo != null) {
      fbo.dispose();
    }
    fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
    Texture texture = fbo.getColorBufferTexture();
    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    fboWidth = width;
    fboHeight = height;
    ready = false;
  }

  private int scaledDimension(int sourceSize) {
    if (sourceSize <= 0) {
      return 0;
    }
    if (quality == FlixelVideoQuality.FULL) {
      return sourceSize;
    }
    // Keep the size even so the downscale stays friendly to chroma-subsampled sources.
    return Math.max(2, Math.round(sourceSize * quality.getScale()) & ~1);
  }

  /** Creates and configures an external OES texture for the SurfaceTexture to fill. */
  private static int createOesTexture() {
    int id = Gdx.gl.glGenTexture();
    Gdx.gl.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id);
    Gdx.gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
    Gdx.gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
    Gdx.gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
    Gdx.gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
    Gdx.gl.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    return id;
  }

  /**
   * Points the player at the requested source.
   *
   * <p>External paths are opened directly. Internal assets are opened through a file
   * descriptor when the APK stored them uncompressed; when that fails (the asset was
   * compressed by the packager) the asset is copied to the cache directory once and
   * played from there, which is the only reliable way to hand a compressed asset to
   * MediaPlayer.
   */
  private void setDataSource(MediaPlayer mp, String path, boolean external) throws IOException {
    if (external) {
      mp.setDataSource(path);
      return;
    }
    if (!(Gdx.app instanceof Context)) {
      throw new IOException("No Android context available to open asset: " + path);
    }
    Context context = (Context) Gdx.app;
    try {
      AssetFileDescriptor descriptor = context.getAssets().openFd(path);
      assetDescriptor = descriptor;
      mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
          descriptor.getLength());
    } catch (IOException compressed) {
      if (assetDescriptor != null) {
        assetDescriptor.close();
        assetDescriptor = null;
      }
      mp.setDataSource(copyAssetToCache(context, path).getAbsolutePath());
    }
  }

  /** Copies an internal asset to the cache directory so a compressed asset is playable. */
  private static File copyAssetToCache(Context context, String path) throws IOException {
    File output = new File(context.getCacheDir(), "flixel-video-" + path.replaceAll("[^a-zA-Z0-9.]", "_"));
    if (output.exists() && output.length() > 0) {
      return output;
    }
    try (InputStream in = Gdx.files.internal(path).read();
        OutputStream out = new FileOutputStream(output)) {
      byte[] buffer = new byte[1 << 16];
      int read;
      while ((read = in.read(buffer)) > 0) {
        out.write(buffer, 0, read);
      }
    }
    return output;
  }
}
