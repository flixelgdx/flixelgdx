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

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.flixelgdx.video.FlixelBaseVideo;
import org.flixelgdx.video.FlixelVideoQuality;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_flush_buffers;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avformat.AVSEEK_FLAG_BACKWARD;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_seek_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_default;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_get_buffer;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
import static org.bytedeco.ffmpeg.global.avutil.av_opt_set_int;
import static org.bytedeco.ffmpeg.global.avutil.av_opt_set_sample_fmt;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.bytedeco.ffmpeg.global.swresample.swr_alloc;
import static org.bytedeco.ffmpeg.global.swresample.swr_convert_frame;
import static org.bytedeco.ffmpeg.global.swresample.swr_free;
import static org.bytedeco.ffmpeg.global.swresample.swr_get_out_samples;
import static org.bytedeco.ffmpeg.global.swresample.swr_init;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR;
import static org.bytedeco.ffmpeg.global.swscale.sws_freeContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_getContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;

/**
 * Desktop video backend that decodes through JavaCPP-bundled FFmpeg.
 *
 * <p>All demuxing and decoding happens on a dedicated background thread. Decoded
 * video frames are converted to RGBA and placed into a small queue of pre-allocated
 * {@link BytePointer}s backed by JavaCPP's native allocator. The render thread borrows
 * a buffer from a pool, uploads it through a ping-ponged pair of pixel buffer objects
 * for asynchronous GPU DMA, then returns the buffer to the pool. Audio is decoded to
 * interleaved stereo 16-bit PCM and streamed to an OpenAL source via a small buffer
 * ring. Neither video frames nor audio chunks touch the Java heap or the JVM direct
 * buffer budget ({@code MaxDirectMemorySize}) at steady state.
 *
 * <p>The FFmpeg native libraries are bundled inside the framework's dependency JARs
 * and extracted automatically by JavaCPP on first use; no system VLC or other video
 * library is required.
 *
 * <p>Threading contract: all template methods ({@link #playMedia()},
 * {@link #updateMedia(float)}, {@link #disposeMedia()}, and the other
 * {@code protected} methods) must be called on the render thread. The decode thread
 * only touches the shared fields that are explicitly {@code volatile}, and the two
 * blocking queues which are thread-safe by construction.
 */
final class FlixelFfmpegVideo extends FlixelBaseVideo {

  /** Maximum decoded video frames held between the decode thread and render thread. */
  private static final int VIDEO_QUEUE_DEPTH = 4;

  /** Number of OpenAL buffers in the streaming ring. */
  private static final int AUDIO_BUFFER_COUNT = 8;

  /**
   * Maximum elapsed time (seconds) consumed per {@link #updateMedia(float)} call.
   * Caps how far the playback clock jumps after a long GC pause or OS preemption.
   */
  private static final float MAX_ELAPSED_S = 0.1f;

  /**
   * Rational used to convert FFmpeg timestamps to microseconds via
   * {@code av_rescale_q}. Equivalent to the C macro {@code AV_TIME_BASE_Q} inverted.
   */
  private static final AVRational US_RATIONAL = new AVRational().num(1).den(1_000_000);

  /** Current playback position in microseconds; render thread only. */
  private long playbackUs;

  /** Total stream duration in microseconds; written once by the decode thread. */
  private volatile long streamDurationUs;

  /**
   * Seek target in microseconds; -1 = no pending seek. Written by the render
   * thread, read and cleared by the decode thread.
   */
  private volatile long seekTargetUs = -1;

  private AVFormatContext formatCtx;
  private AVCodecContext videoCtx;
  private AVCodecContext audioCtx;
  private SwsContext swsCtx;
  private SwrContext swrCtx;

  private Thread decodeThread;

  /** Frame queue: decode thread produces, render thread consumes. */
  private final BlockingQueue<VideoFrame> frameQueue = new ArrayBlockingQueue<>(VIDEO_QUEUE_DEPTH);

  /**
   * Audio PCM queue: decode thread produces interleaved stereo 16-bit
   * JavaCPP-allocated {@link BytePointer}s, render thread streams them to OpenAL.
   */
  private final BlockingQueue<BytePointer> audioQueue = new ArrayBlockingQueue<>(64);

  /**
   * Pool of {@link BytePointer}s reused between frames. Memory is allocated through
   * JavaCPP's native allocator, which does not count against the JVM direct buffer
   * budget ({@code MaxDirectMemorySize}). The decode thread borrows one buffer, fills
   * it, and puts it in {@link #frameQueue}. The render thread returns each buffer
   * here after uploading it to the GPU. Pool size is {@code VIDEO_QUEUE_DEPTH + 1}:
   * the queue can hold {@code VIDEO_QUEUE_DEPTH} filled buffers while the decode
   * thread pre-fills one more.
   */
  private final BlockingQueue<BytePointer> freeFramePool = new ArrayBlockingQueue<>(VIDEO_QUEUE_DEPTH + 1);

  private Texture texture;

  /** Ping-ponged PBO handles for asynchronous texture upload. */
  private final int[] pbos = new int[2];

  @NotNull
  private volatile FlixelVideoQuality mediaQuality = FlixelVideoQuality.FULL;

  private int videoStreamIdx = -1;
  private int audioStreamIdx = -1;

  private int textureW;
  private int textureH;
  private int pboIndex;

  /** Decoded frame width at the current quality setting; written by decode thread. */
  private volatile int frameW;

  /** Decoded frame height at the current quality setting; written by decode thread. */
  private volatile int frameH;

  private int alSource;
  private int[] alBuffers;
  private int alFormat;
  private volatile int alSampleRate;

  private volatile float desiredVolume = 1f;
  private volatile float desiredRate = 1f;

  private volatile boolean playing;
  private volatile boolean paused;
  private volatile boolean looping;
  private volatile boolean ended;

  /** Set by the render thread after OpenAL is initialized; guards all AL calls. */
  private boolean audioInitialized;

  /**
   * Set by the decode thread once the first audio sample rate is known, signaling
   * the render thread to initialize OpenAL on its next {@link #updateMedia(float)}.
   */
  private volatile boolean audioMetaReady;

  private volatile boolean disposed;
  private boolean ready;

  FlixelFfmpegVideo(@NotNull String path) {
    super();
    av_log_set_level(AV_LOG_QUIET);
    openMedia(path);
    decodeThread = new Thread(this::runDecodeLoop, "FlixelVideo-decode");
    decodeThread.setDaemon(true);
    decodeThread.start();
  }

  @Override
  protected void playMedia() {
    if (disposed) {
      return;
    }
    ended = false;
    seekTargetUs = 0;
    playbackUs = 0;
    playing = true;
    paused = false;
    clearFrameQueue();
    audioQueue.clear();
  }

  @Override
  protected void pauseMedia() {
    paused = true;
    if (audioInitialized) {
      AL10.alSourcePause(alSource);
    }
  }

  @Override
  protected void resumeMedia() {
    paused = false;
    if (audioInitialized && playing) {
      AL10.alSourcePlay(alSource);
    }
  }

  @Override
  protected void stopMedia() {
    playing = false;
    paused = false;
    ended = false;
    seekTargetUs = 0;
    playbackUs = 0;
    clearFrameQueue();
    audioQueue.clear();
    if (audioInitialized) {
      AL10.alSourceStop(alSource);
    }
  }

  @Override
  protected boolean isMediaPlaying() {
    return playing && !paused && !disposed;
  }

  @Override
  protected boolean isMediaEnded() {
    return ended;
  }

  @Override
  protected boolean isMediaReady() {
    return ready;
  }

  @Override
  protected float getMediaTime() {
    return playbackUs / 1000f;
  }

  @Override
  protected void setMediaTime(float timeMs) {
    if (disposed) {
      return;
    }
    long targetUs = (long) (Math.max(0f, timeMs) * 1000L);
    playbackUs = targetUs;
    seekTargetUs = targetUs;
    clearFrameQueue();
    audioQueue.clear();
    ended = false;
  }

  @Override
  protected float getMediaLength() {
    return streamDurationUs / 1000f;
  }

  @Override
  protected float getMediaRate() {
    return desiredRate;
  }

  @Override
  protected void setMediaRate(float rate) {
    if (rate > 0f) {
      desiredRate = rate;
    }
  }

  @Override
  protected boolean isMediaLooped() {
    return looping;
  }

  @Override
  protected void setMediaLooped(boolean looped) {
    looping = looped;
  }

  @Override
  protected float getMediaVolume() {
    return desiredVolume;
  }

  @Override
  protected void setMediaVolume(float volume) {
    desiredVolume = Math.max(0f, Math.min(1f, volume));
    if (audioInitialized) {
      AL10.alSourcef(alSource, AL10.AL_GAIN, desiredVolume);
    }
  }

  @Override
  protected void applyMediaQuality(@NotNull FlixelVideoQuality quality) {
    mediaQuality = quality;
    // Decode thread rebuilds SwsContext on the next frame when it sees the new quality.
    clearFrameQueue();
  }

  @Override
  protected int getMediaVideoWidth() {
    return frameW;
  }

  @Override
  protected int getMediaVideoHeight() {
    return frameH;
  }

  @Override
  @Nullable
  protected Texture getMediaTexture() {
    return ready ? texture : null;
  }

  @Override
  protected void updateMedia(float elapsed) {
    if (disposed) {
      return;
    }

    if (playing && !paused) {
      float cappedElapsed = Math.min(elapsed, MAX_ELAPSED_S);
      playbackUs += (long) (cappedElapsed * desiredRate * 1_000_000L);
    }

    // Consume the latest video frame whose PTS has passed. Drop older frames
    // back into the pool so the decode thread can reuse their buffers.
    VideoFrame display = null;
    VideoFrame candidate;
    while ((candidate = frameQueue.peek()) != null && candidate.ptsUs() <= playbackUs) {
      if (display != null) {
        freeFramePool.offer(display.pixels());
      }
      display = frameQueue.poll();
    }
    if (display != null) {
      ensureGpuObjects(display.width(), display.height());
      uploadFrame(display);
    }

    // Initialize OpenAL once the decode thread signals that audio metadata is ready.
    if (!audioInitialized && audioMetaReady) {
      initOpenAl();
    }

    if (audioInitialized) {
      AL10.alSourcef(alSource, AL10.AL_GAIN, desiredVolume);
      pumpAudio();
    }
  }

  @Override
  protected void disposeMedia() {
    if (disposed) {
      return;
    }
    disposed = true;

    if (decodeThread != null) {
      decodeThread.interrupt();
      try {
        decodeThread.join(2000L);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      decodeThread = null;
    }

    closeFfmpeg();

    if (audioInitialized) {
      AL10.alSourceStop(alSource);
      AL10.alSourcei(alSource, AL10.AL_BUFFER, 0);
      AL10.alDeleteSources(alSource);
      AL10.alDeleteBuffers(alBuffers);
      audioInitialized = false;
    }

    if (texture != null) {
      texture.dispose();
      texture = null;
    }
    if (pbos[0] != 0) {
      GL15C.glDeleteBuffers(pbos);
      pbos[0] = 0;
      pbos[1] = 0;
    }

    clearFrameQueue();
    audioQueue.clear();
    freeFramePool.clear();
    ready = false;
  }

  private void openMedia(String path) {
    formatCtx = new AVFormatContext(null);
    if (avformat_open_input(formatCtx, path, null, null) < 0) {
      throw new IllegalStateException("Cannot open video file: " + path);
    }
    if (avformat_find_stream_info(formatCtx, (org.bytedeco.javacpp.PointerPointer<?>) null) < 0) {
      throw new IllegalStateException("Cannot read stream info for: " + path);
    }
    if (formatCtx.duration() != AV_NOPTS_VALUE) {
      streamDurationUs = formatCtx.duration();
    }

    for (int i = 0; i < formatCtx.nb_streams(); i++) {
      int type = formatCtx.streams(i).codecpar().codec_type();
      if (type == AVMEDIA_TYPE_VIDEO && videoStreamIdx < 0) {
        videoStreamIdx = i;
      } else if (type == AVMEDIA_TYPE_AUDIO && audioStreamIdx < 0) {
        audioStreamIdx = i;
      }
    }
    if (videoStreamIdx < 0) {
      throw new IllegalStateException("No video stream found in: " + path);
    }

    videoCtx = openDecoder(videoStreamIdx);
    if (audioStreamIdx >= 0) {
      try {
        audioCtx = openDecoder(audioStreamIdx);
      } catch (IllegalStateException e) {
        Gdx.app.log("FlixelVideo", "Audio unavailable: " + e.getMessage());
        audioStreamIdx = -1;
      }
    }
  }

  private AVCodecContext openDecoder(int streamIdx) {
    AVCodecParameters params = formatCtx.streams(streamIdx).codecpar();
    AVCodec codec = avcodec_find_decoder(params.codec_id());
    if (codec == null || codec.isNull()) {
      throw new IllegalStateException("No decoder for codec id " + params.codec_id());
    }
    AVCodecContext ctx = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(ctx, params);
    if (avcodec_open2(ctx, codec, (org.bytedeco.javacpp.PointerPointer<?>) null) < 0) {
      throw new IllegalStateException("Cannot open decoder for stream " + streamIdx);
    }
    return ctx;
  }

  private void closeFfmpeg() {
    if (swsCtx != null) {
      sws_freeContext(swsCtx);
      swsCtx = null;
    }
    if (swrCtx != null) {
      swr_free(swrCtx);
      swrCtx = null;
    }
    if (videoCtx != null) {
      avcodec_free_context(videoCtx);
      videoCtx = null;
    }
    if (audioCtx != null) {
      avcodec_free_context(audioCtx);
      audioCtx = null;
    }
    if (formatCtx != null) {
      avformat_close_input(formatCtx);
      formatCtx = null;
    }
  }

  private void runDecodeLoop() {
    AVPacket pkt = av_packet_alloc();
    AVFrame srcFrame = av_frame_alloc();
    AVFrame rgbaFrame = av_frame_alloc();
    AVFrame audioOutFrame = av_frame_alloc();

    FlixelVideoQuality decodeQuality = null;
    int decodeW = 0;
    int decodeH = 0;

    try {
      while (!disposed && !Thread.currentThread().isInterrupted()) {
        if (!playing || paused) {
          Thread.sleep(5L);
          continue;
        }

        // Handle a pending seek.
        long seek = seekTargetUs;
        if (seek >= 0) {
          seekTargetUs = -1;
          av_seek_frame(formatCtx, -1, seek, AVSEEK_FLAG_BACKWARD);
          avcodec_flush_buffers(videoCtx);
          if (audioCtx != null) {
            avcodec_flush_buffers(audioCtx);
          }
          clearFrameQueue();
          audioQueue.clear();
          continue;
        }

        int ret = av_read_frame(formatCtx, pkt);
        if (ret < 0) {
          av_packet_unref(pkt);
          if (looping) {
            av_seek_frame(formatCtx, -1, 0, AVSEEK_FLAG_BACKWARD);
            avcodec_flush_buffers(videoCtx);
            if (audioCtx != null) {
              avcodec_flush_buffers(audioCtx);
            }
            clearFrameQueue();
            audioQueue.clear();
          } else {
            ended = true;
            playing = false;
          }
          continue;
        }

        int idx = pkt.stream_index();
        if (idx == videoStreamIdx) {
          FlixelVideoQuality q = mediaQuality;
          int tw = scaledDimension(videoCtx.width(), q);
          int th = scaledDimension(videoCtx.height(), q);
          if (swsCtx == null || q != decodeQuality || tw != decodeW || th != decodeH) {
            if (swsCtx != null) {
              sws_freeContext(swsCtx);
            }
            swsCtx = sws_getContext(
                videoCtx.width(), videoCtx.height(), videoCtx.pix_fmt(),
                tw, th, AV_PIX_FMT_RGBA,
                SWS_BILINEAR, null, null, (double[]) null);
            av_frame_unref(rgbaFrame);
            rgbaFrame.format(AV_PIX_FMT_RGBA);
            rgbaFrame.width(tw);
            rgbaFrame.height(th);
            av_frame_get_buffer(rgbaFrame, 1);
            decodeQuality = q;
            decodeW = tw;
            decodeH = th;
            frameW = tw;
            frameH = th;

            // Repopulate the frame pool with correctly-sized JavaCPP-allocated
            // buffers. Any old buffers still in the pool are dropped here; JavaCPP
            // frees their native memory when they are GC'd. new BytePointer(n)
            // uses JavaCPP's own allocator and does not count against MaxDirectMemorySize.
            freeFramePool.clear();
            int frameBytes = tw * th * 4;
            for (int i = 0; i < VIDEO_QUEUE_DEPTH + 1; i++) {
              freeFramePool.offer(new BytePointer(frameBytes));
            }
          }

          avcodec_send_packet(videoCtx, pkt);
          while (avcodec_receive_frame(videoCtx, srcFrame) == 0) {
            if (disposed) {
              break;
            }
            sws_scale(swsCtx,
                srcFrame.data(), srcFrame.linesize(), 0, videoCtx.height(),
                rgbaFrame.data(), rgbaFrame.linesize());

            long pts = srcFrame.pts() != AV_NOPTS_VALUE
                ? srcFrame.pts() : srcFrame.best_effort_timestamp();
            long ptsUs = av_rescale_q(pts,
                formatCtx.streams(videoStreamIdx).time_base(), US_RATIONAL);

            // Borrow a JavaCPP-allocated buffer from the pool. The render thread
            // returns it after uploading, so the pool stays bounded and neither
            // heap nor MaxDirectMemorySize budget is touched per frame.
            BytePointer pixelBuf = freeFramePool.poll(200L, TimeUnit.MILLISECONDS);
            if (pixelBuf == null || disposed) {
              continue;
            }

            // Strip row padding: copy each row from native RGBA data into the
            // tight pool buffer via ByteBuffer views of both native regions.
            int stride = rgbaFrame.linesize(0);
            int rowBytes = tw * 4;
            BytePointer nativeData = rgbaFrame.data(0);
            nativeData.capacity((long) stride * th);
            ByteBuffer srcBuf = nativeData.asByteBuffer();
            ByteBuffer destBuf = pixelBuf.asByteBuffer();
            for (int row = 0; row < th; row++) {
              srcBuf.limit(row * stride + rowBytes).position(row * stride);
              destBuf.put(srcBuf);
            }

            frameQueue.put(new VideoFrame(pixelBuf, tw, th, ptsUs));
          }
        } else if (idx == audioStreamIdx && audioCtx != null) {
          decodeAudioPacket(pkt, srcFrame, audioOutFrame);
        }

        av_packet_unref(pkt);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      av_frame_unref(audioOutFrame);
      av_frame_free(audioOutFrame);
      av_frame_unref(rgbaFrame);
      av_frame_free(rgbaFrame);
      av_frame_free(srcFrame);
      av_packet_free(pkt);
    }
  }

  private void decodeAudioPacket(AVPacket pkt, AVFrame inFrame, AVFrame outFrame)
      throws InterruptedException {
    avcodec_send_packet(audioCtx, pkt);
    while (avcodec_receive_frame(audioCtx, inFrame) == 0) {
      if (disposed) {
        return;
      }

      if (swrCtx == null) {
        swrCtx = buildSwrContext();
        if (swrCtx == null) {
          audioStreamIdx = -1;
          return;
        }
        alSampleRate = audioCtx.sample_rate();
        alFormat = AL10.AL_FORMAT_STEREO16;
        audioMetaReady = true;
      }

      int outCount = (int) swr_get_out_samples(swrCtx, inFrame.nb_samples());
      if (outCount <= 0) {
        continue;
      }

      av_frame_unref(outFrame);
      outFrame.format(AV_SAMPLE_FMT_S16);
      outFrame.sample_rate(audioCtx.sample_rate());
      outFrame.nb_samples(outCount);
      av_channel_layout_default(outFrame.ch_layout(), 2);
      av_frame_get_buffer(outFrame, 0);

      if (swr_convert_frame(swrCtx, outFrame, inFrame) == 0 && outFrame.nb_samples() > 0) {
        int bytes = outFrame.nb_samples() * 4; // stereo * 2 bytes per sample
        BytePointer audioData = outFrame.data(0);
        audioData.capacity(bytes);
        BytePointer pcm = new BytePointer(bytes);
        pcm.asByteBuffer().put(audioData.asByteBuffer());
        audioQueue.put(pcm);
      }
    }
  }

  private SwrContext buildSwrContext() {
    SwrContext swr = swr_alloc();
    av_opt_set_int(swr, "in_channel_count", audioCtx.ch_layout().nb_channels(), 0);
    av_opt_set_int(swr, "in_sample_rate", audioCtx.sample_rate(), 0);
    av_opt_set_sample_fmt(swr, "in_sample_fmt", audioCtx.sample_fmt(), 0);
    av_opt_set_int(swr, "out_channel_count", 2, 0);
    av_opt_set_int(swr, "out_sample_rate", audioCtx.sample_rate(), 0);
    av_opt_set_sample_fmt(swr, "out_sample_fmt", AV_SAMPLE_FMT_S16, 0);
    if (swr_init(swr) < 0) {
      swr_free(swr);
      Gdx.app.log("FlixelVideo", "SwrContext init failed; audio will be silent.");
      return null;
    }
    return swr;
  }

  private void ensureGpuObjects(int w, int h) {
    if (texture != null && w == textureW && h == textureH) {
      return;
    }
    if (texture != null) {
      texture.dispose();
    }
    texture = new Texture(new GLOnlyTextureData(w, h, 0, GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE));
    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    textureW = w;
    textureH = h;
    if (pbos[0] == 0) {
      GL15C.glGenBuffers(pbos);
    }
    pboIndex = 0;
    ready = false;
  }

  private void uploadFrame(VideoFrame frame) {
    int w = frame.width();
    int h = frame.height();
    int bytes = w * h * 4;

    // Orphan the write PBO and copy the pixel data into it.
    int pbo = pbos[pboIndex];
    pboIndex ^= 1;
    GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, pbo);
    GL15C.glBufferData(GL21C.GL_PIXEL_UNPACK_BUFFER, bytes, GL15C.GL_STREAM_DRAW);
    BytePointer pixels = frame.pixels();
    GL15C.glBufferSubData(GL21C.GL_PIXEL_UNPACK_BUFFER, 0, pixels.asByteBuffer());

    // Upload from the PBO (GPU DMA, no CPU stall).
    texture.bind();
    GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, w, h,
        GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, 0L);

    GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0);

    // Return the buffer to the pool so the decode thread can reuse it.
    freeFramePool.offer(pixels);
    ready = true;
  }

  private void initOpenAl() {
    try {
      alSource = AL10.alGenSources();
      alBuffers = new int[AUDIO_BUFFER_COUNT];
      AL10.alGenBuffers(alBuffers);
      AL10.alSourcef(alSource, AL10.AL_GAIN, desiredVolume);
      AL10.alSourcef(alSource, AL10.AL_PITCH, 1f);

      for (int buf : alBuffers) {
        BytePointer pcm = audioQueue.poll();
        if (pcm == null) {
          break;
        }
        AL10.alBufferData(buf, alFormat, pcm.asByteBuffer(), alSampleRate);
        AL10.alSourceQueueBuffers(alSource, buf);
      }

      if (playing && !paused) {
        AL10.alSourcePlay(alSource);
      }
      audioInitialized = true;
    } catch (Exception e) {
      Gdx.app.log("FlixelVideo", "OpenAL init failed; audio will be silent: " + e.getMessage());
    }
  }

  private void pumpAudio() {
    int processed = AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
    while (processed-- > 0) {
      int buf = AL10.alSourceUnqueueBuffers(alSource);
      BytePointer pcm = audioQueue.poll();
      if (pcm != null) {
        AL10.alBufferData(buf, alFormat, pcm.asByteBuffer(), alSampleRate);
        AL10.alSourceQueueBuffers(alSource, buf);
      }
    }

    if (!paused && playing) {
      int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
      if (state != AL10.AL_PLAYING) {
        AL10.alSourcePlay(alSource);
      }
    }
  }

  /**
   * Drains the frame queue and returns each frame's buffer to the free pool.
   * Must be called instead of {@code frameQueue.clear()} to avoid leaking pool
   * buffers that would then be unreachable to the decode thread.
   */
  private void clearFrameQueue() {
    VideoFrame f;
    while ((f = frameQueue.poll()) != null) {
      freeFramePool.offer(f.pixels());
    }
  }

  private static int scaledDimension(int sourceSize, FlixelVideoQuality quality) {
    if (sourceSize <= 0) {
      return 0;
    }
    if (quality == FlixelVideoQuality.FULL) {
      return sourceSize;
    }
    return Math.max(2, Math.round(sourceSize * quality.getScale()) & ~1);
  }

  private record VideoFrame(BytePointer pixels, int width, int height, long ptsUs) {
  }
}
