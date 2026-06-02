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
package org.flixelgdx.backend.teavm.audio;

import org.flixelgdx.audio.FlixelSoundBackend;

/**
 * TeaVM/web implementation of {@link FlixelSoundBackend.Factory} that falls
 * back to libGDX {@code Gdx.audio} for sound playback.
 *
 * <p>Groups and effect nodes are no-ops because Web Audio does not expose the
 * same graph-based API as MiniAudio.
 */
public class FlixelDefaultSoundHandler implements FlixelSoundBackend.Factory {

  private float masterVolume = 1f;

  @Override
  public FlixelSoundBackend createSound(String path, short flags, Object group, boolean external) {
    return new FlixelGdxSound(path, external);
  }

  @Override
  public Object createGroup() {
    return new Object();
  }

  @Override
  public void disposeGroup(Object group) {
    // No-op on web.
  }

  @Override
  public void groupPause(Object group) {
    // No-op on web.
  }

  @Override
  public void groupPlay(Object group) {
    // No-op on web.
  }

  @Override
  public void setMasterVolume(float volume) {
    masterVolume = Math.max(0f, Math.min(1f, volume));
  }

  /**
   * Returns the tracked master volume.
   *
   * @return Master volume in [0, 1].
   */
  public float getMasterVolume() {
    return masterVolume;
  }

  @Override
  public void disposeEngine() {
    // No native engine to dispose on web.
  }

  @Override
  public void attachToEngineOutput(FlixelSoundBackend sound, int outputBusIndex) {
    // No-op on web.
  }

  @Override
  public FlixelSoundBackend.EffectNode createReverbNode(float wet) {
    return NoOpEffectNode.INSTANCE;
  }

  @Override
  public FlixelSoundBackend.EffectNode createDelayNode(float delaySeconds, float decay) {
    return NoOpEffectNode.INSTANCE;
  }

  @Override
  public FlixelSoundBackend.EffectNode createLowPassFilter(double cutoffHz, int order) {
    return NoOpEffectNode.INSTANCE;
  }

  /** Singleton no-op effect node for platforms that do not support audio graphs. */
  private static final class NoOpEffectNode implements FlixelSoundBackend.EffectNode {

    static final NoOpEffectNode INSTANCE = new NoOpEffectNode();

    @Override
    public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
      // No-op.
    }

    @Override
    public void detach(int bus) {
      // No-op.
    }

    @Override
    public void dispose() {
      // No-op.
    }
  }
}
