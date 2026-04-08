/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm.audio;

import me.stringdotjar.flixelgdx.audio.FlixelSoundBackend;

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
