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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.github.xpenatan.gdx.teavm.backends.web.WebGL20;
import com.github.xpenatan.gdx.teavm.backends.web.gl.WebGLRenderingContextExt;

import org.flixelgdx.video.FlixelVideo;
import org.flixelgdx.video.FlixelVideoQuality;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLVideoElement;

/**
 * Web video backend built on a hidden HTML video element.
 *
 * <p>The video element is used strictly as a decoding source and is never attached to
 * the DOM, so it cannot float above or below the game canvas; every frame is pulled
 * into a WebGL texture and drawn by the regular batch, which keeps state draw order
 * intact (a sprite added after the video renders above it).
 *
 * <p>At {@link FlixelVideoQuality#FULL} quality each new frame goes straight through
 * {@code texImage2D(..., video)}, which browsers implement as a GPU-to-GPU transfer
 * whenever the decoder runs on the GPU. Lower presets route through an offscreen
 * canvas ({@code drawImage} downscale, then {@code texImage2D(..., canvas)}).
 *
 * <p>Autoplay policies may block {@link #playMedia()} with sound before the first user
 * gesture; in that case playback resumes automatically on the next pointer or key
 * event (the rejection handler in {@link #jsPlay} registers one-shot listeners).
 */
public final class FlixelTeaVMVideo extends FlixelVideo {

  /** The hidden video element doing the decoding. */
  private final JSObject element;

  /** Offscreen canvas used only for the downscaled quality presets. */
  @Nullable
  private JSObject scaleCanvas;

  @Nullable
  private Texture texture;

  @NotNull
  private FlixelVideoQuality mediaQuality = FlixelVideoQuality.FULL;

  /** Texture dimensions currently allocated on the GPU. */
  private int textureWidth;
  private int textureHeight;

  private float volume = 1f;
  private float rate = 1f;

  /** Seek requested before the element had metadata; applied once seekable. -1 = none. */
  private float pendingSeekMs = -1f;

  /** currentTime (in seconds) of the last uploaded frame, to skip duplicate uploads. */
  private double lastUploadedTime = -1.0;

  private boolean looping;
  private boolean ready;
  private boolean disposed;

  /**
   * Creates a video element for the given URL path.
   *
   * @param url The video URL, typically an internal asset path relative to the page.
   */
  public FlixelTeaVMVideo(@NotNull String url) {
    super();
    element = jsCreateVideo(url);
  }

  @Override
  protected void playMedia() {
    if (disposed) {
      return;
    }
    jsPlay(element);
  }

  @Override
  protected void pauseMedia() {
    if (disposed) {
      return;
    }
    jsPause(element);
  }

  @Override
  protected void resumeMedia() {
    playMedia();
  }

  @Override
  protected void stopMedia() {
    if (disposed) {
      return;
    }
    jsPause(element);
    jsSetTime(element, 0.0);
    pendingSeekMs = -1f;
  }

  @Override
  protected boolean isMediaPlaying() {
    return !disposed && jsIsPlaying(element);
  }

  @Override
  protected boolean isMediaEnded() {
    return !disposed && jsIsEnded(element);
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
    return (float) (jsGetTime(element) * 1000.0);
  }

  @Override
  protected void setMediaTime(float timeMs) {
    if (disposed) {
      return;
    }
    float target = Math.max(0f, timeMs);
    if (jsGetReadyState(element) >= 1) {
      // HAVE_METADATA or better: the element accepts seeks immediately.
      jsSetTime(element, target / 1000.0);
      pendingSeekMs = -1f;
    } else {
      pendingSeekMs = target;
    }
  }

  @Override
  protected float getMediaLength() {
    if (disposed) {
      return 0f;
    }
    double duration = jsGetDuration(element);
    if (Double.isNaN(duration) || Double.isInfinite(duration)) {
      return 0f;
    }
    return (float) (duration * 1000.0);
  }

  @Override
  protected float getMediaRate() {
    return rate;
  }

  @Override
  protected void setMediaRate(float rate) {
    if (disposed || rate <= 0f) {
      return;
    }
    this.rate = rate;
    jsSetRate(element, rate);
  }

  @Override
  protected boolean isMediaLooped() {
    return looping;
  }

  @Override
  protected void setMediaLooped(boolean looped) {
    this.looping = looped;
    if (!disposed) {
      jsSetLoop(element, looped);
    }
  }

  @Override
  protected float getMediaVolume() {
    return volume;
  }

  @Override
  protected void setMediaVolume(float volume) {
    this.volume = Math.max(0f, Math.min(1f, volume));
    if (!disposed) {
      jsSetVolume(element, this.volume);
    }
  }

  @Override
  protected void applyMediaQuality(@NotNull FlixelVideoQuality quality) {
    if (this.mediaQuality == quality) {
      return;
    }
    this.mediaQuality = quality;
    // Force the texture to be recreated at the new decode size on the next frame.
    lastUploadedTime = -1.0;
    if (texture != null) {
      texture.dispose();
      texture = null;
      ready = false;
    }
  }

  @Override
  protected int getMediaVideoWidth() {
    if (disposed) {
      return 0;
    }
    return scaledDimension(jsGetVideoWidth(element));
  }

  @Override
  protected int getMediaVideoHeight() {
    if (disposed) {
      return 0;
    }
    return scaledDimension(jsGetVideoHeight(element));
  }

  @Override
  protected void updateMedia(float elapsed) {
    if (disposed) {
      return;
    }

    if (pendingSeekMs >= 0f && jsGetReadyState(element) >= 1) {
      jsSetTime(element, pendingSeekMs / 1000.0);
      pendingSeekMs = -1f;
    }

    // Re-assert volume every frame; some browsers reset v.volume on autoplay
    // initialization or when the audio context unlocks after a user gesture.
    jsSetVolume(element, volume);

    // HAVE_CURRENT_DATA (2) means a decoded frame is available for the current time.
    if (jsGetReadyState(element) < 2) {
      return;
    }
    int width = getMediaVideoWidth();
    int height = getMediaVideoHeight();
    if (width <= 0 || height <= 0) {
      return;
    }

    double time = jsGetTime(element);
    boolean firstFrame = texture == null || width != textureWidth || height != textureHeight;
    if (!firstFrame && time == lastUploadedTime) {
      return;
    }

    if (firstFrame) {
      recreateTexture(width, height);
    }
    uploadFrame(width, height);
    lastUploadedTime = time;
    ready = true;
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
    jsDispose(element);
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    scaleCanvas = null;
    ready = false;
  }

  /** Allocates (or reallocates) the GPU texture at the current decode size. */
  private void recreateTexture(int width, int height) {
    if (texture != null) {
      texture.dispose();
    }
    texture = new Texture(new WebVideoTextureData(width, height));
    // NPOT video dimensions in WebGL 1 require clamping and no mipmaps.
    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    textureWidth = width;
    textureHeight = height;
  }

  /** Pulls the current video frame into the bound GL texture. */
  private void uploadFrame(int width, int height) {
    Texture target = texture;
    if (target == null) {
      return;
    }
    Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, target.getTextureObjectHandle());
    if (mediaQuality == FlixelVideoQuality.FULL) {
      HTMLVideoElement video = element.cast();
      context().texImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, GL20.GL_RGBA,
          GL20.GL_UNSIGNED_BYTE, video);
    } else {
      if (scaleCanvas == null) {
        scaleCanvas = jsCreateCanvas();
      }
      jsDrawScaled(scaleCanvas, element, width, height);
      jsTexImage2DCanvas(context(), scaleCanvas);
    }
  }

  private int scaledDimension(int sourceSize) {
    if (sourceSize <= 0) {
      return 0;
    }
    if (mediaQuality == FlixelVideoQuality.FULL) {
      return sourceSize;
    }
    return Math.max(2, Math.round(sourceSize * mediaQuality.getScale()));
  }

  @NotNull
  private static WebGLRenderingContextExt context() {
    if (Gdx.gl20 instanceof WebGL20 webGl) {
      return webGl.gl;
    }
    throw new IllegalStateException(
        "FlixelGDX video requires the standard WebGL20 backend (got "
            + (Gdx.gl20 == null ? "no GL20" : Gdx.gl20.getClass().getName()) + ").");
  }

  @JSBody(params = { "url" }, script = "var v = document.createElement('video');"
      + "v.src = url;"
      + "v.crossOrigin = 'anonymous';"
      + "v.preload = 'auto';"
      + "v.playsInline = true;"
      + "v.flixelVolume = 1;"
      + "v.flixelLoop = false;"
      + "v.addEventListener('loadedmetadata', function() {"
      + "  v.volume = v.flixelVolume;"
      + "  v.loop = v.flixelLoop;"
      + "});"
      + "v.load();"
      + "return v;")
  private static native JSObject jsCreateVideo(String url);

  /**
   * Starts playback. If the browser's autoplay policy rejects the call (no user
   * gesture yet), one-shot listeners retry on the next pointer or key event.
   */
  @JSBody(params = { "v" }, script = "v.volume = v.flixelVolume;"
      + "var p = v.play();"
      + "if (p && p.catch) {"
      + "  p.catch(function() {"
      + "    if (v.flixelResumeArmed) return;"
      + "    v.flixelResumeArmed = true;"
      + "    var resume = function() {"
      + "      v.flixelResumeArmed = false;"
      + "      document.removeEventListener('pointerdown', resume);"
      + "      document.removeEventListener('keydown', resume);"
      + "      v.volume = v.flixelVolume;"
      + "      v.play();"
      + "    };"
      + "    document.addEventListener('pointerdown', resume);"
      + "    document.addEventListener('keydown', resume);"
      + "  });"
      + "}")
  private static native void jsPlay(JSObject v);

  @JSBody(params = { "v" }, script = "v.pause();")
  private static native void jsPause(JSObject v);

  @JSBody(params = { "v" }, script = "return !v.paused && !v.ended;")
  private static native boolean jsIsPlaying(JSObject v);

  @JSBody(params = { "v" }, script = "return v.ended;")
  private static native boolean jsIsEnded(JSObject v);

  @JSBody(params = { "v" }, script = "return v.currentTime;")
  private static native double jsGetTime(JSObject v);

  @JSBody(params = { "v", "seconds" }, script = "v.currentTime = seconds;")
  private static native void jsSetTime(JSObject v, double seconds);

  @JSBody(params = { "v" }, script = "return v.duration;")
  private static native double jsGetDuration(JSObject v);

  @JSBody(params = { "v", "rate" }, script = "v.playbackRate = rate;")
  private static native void jsSetRate(JSObject v, float rate);

  @JSBody(params = { "v", "loop" }, script = "v.flixelLoop = loop; v.loop = loop;")
  private static native void jsSetLoop(JSObject v, boolean loop);

  @JSBody(params = { "v", "volume" }, script = "v.flixelVolume = volume; v.volume = volume;")
  private static native void jsSetVolume(JSObject v, float volume);

  @JSBody(params = { "v" }, script = "return v.readyState;")
  private static native int jsGetReadyState(JSObject v);

  @JSBody(params = { "v" }, script = "return v.videoWidth;")
  private static native int jsGetVideoWidth(JSObject v);

  @JSBody(params = { "v" }, script = "return v.videoHeight;")
  private static native int jsGetVideoHeight(JSObject v);

  @JSBody(params = { "v" }, script = "v.pause();"
      + "v.removeAttribute('src');"
      + "v.load();")
  private static native void jsDispose(JSObject v);

  @JSBody(script = "return document.createElement('canvas');")
  private static native JSObject jsCreateCanvas();

  @JSBody(params = { "canvas", "v", "w", "h" }, script = ""
      + "if (canvas.width !== w) canvas.width = w;"
      + "if (canvas.height !== h) canvas.height = h;"
      + "canvas.getContext('2d').drawImage(v, 0, 0, w, h);")
  private static native void jsDrawScaled(JSObject canvas, JSObject v, int w, int h);

  /** texImage2D from a canvas source: 3553 = TEXTURE_2D, 6408 = RGBA, 5121 = UNSIGNED_BYTE. */
  @JSBody(params = { "ctx", "canvas" }, script = "ctx.texImage2D(3553, 0, 6408, 6408, 5121, canvas);")
  private static native void jsTexImage2DCanvas(JSObject ctx, JSObject canvas);

  /**
   * Custom texture data that allocates GPU storage for the video frames.
   *
   * <p>The actual pixels are re-specified every frame by {@code texImage2D} with a
   * video or canvas source, so this only performs the initial allocation that gives
   * the libGDX {@link Texture} wrapper its correct dimensions for UV math.
   */
  private record WebVideoTextureData(int width, int height) implements TextureData {

    @Override
    public TextureDataType getType() {
      return TextureDataType.Custom;
    }

    @Override
    public boolean isPrepared() {
      return true;
    }

    @Override
    public void prepare() {}

    @Override
    public Pixmap consumePixmap() {
      throw new UnsupportedOperationException("This TextureData implementation is custom.");
    }

    @Override
    public boolean disposePixmap() {
      return false;
    }

    @Override
    public void consumeCustomData(int target) {
      Gdx.gl.glTexImage2D(target, 0, GL20.GL_RGBA, width, height, 0,
          GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, null);
    }

    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public int getHeight() {
      return height;
    }

    @Override
    public Pixmap.Format getFormat() {
      return Pixmap.Format.RGBA8888;
    }

    @Override
    public boolean useMipMaps() {
      return false;
    }

    @Override
    public boolean isManaged() {
      return false;
    }
  }
}
