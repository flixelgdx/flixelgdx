/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.jvm.audio;

import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.effect.MADelayNode;
import games.rednblack.miniaudio.effect.MAReverbNode;
import games.rednblack.miniaudio.filter.MALowPassFilter;
import me.stringdotjar.flixelgdx.audio.FlixelSoundBackend;

/**
 * JVM implementation of {@link FlixelSoundBackend.Factory} backed by the
 * MiniAudio native library.
 *
 * <p>This factory owns a single {@link MiniAudio} engine instance that is
 * created in the constructor and disposed when {@link #disposeEngine()} is
 * called. All sounds and groups are created through the engine.
 */
public class FlixelMiniAudioSoundHandler implements FlixelSoundBackend.Factory {

  private final MiniAudio engine;

  /** Creates the handler and initialises the MiniAudio engine. */
  public FlixelMiniAudioSoundHandler() {
    engine = new MiniAudio();
  }

  /** Returns the underlying MiniAudio engine for advanced or asset-loader use. */
  public MiniAudio getEngine() {
    return engine;
  }

  @Override
  public FlixelSoundBackend createSound(String path, short flags, Object group, boolean external) {
    MAGroup maGroup = (group instanceof MAGroup g) ? g : null;
    MASound ma = engine.createSound(path, flags, maGroup, external);
    return new FlixelMiniAudioSound(ma);
  }

  @Override
  public Object createGroup() {
    return engine.createGroup();
  }

  @Override
  public void disposeGroup(Object group) {
    if (group instanceof MAGroup g) {
      g.dispose();
    }
  }

  @Override
  public void groupPause(Object group) {
    if (group instanceof MAGroup g) {
      g.pause();
    }
  }

  @Override
  public void groupPlay(Object group) {
    if (group instanceof MAGroup g) {
      g.play();
    }
  }

  @Override
  public void setMasterVolume(float volume) {
    engine.setMasterVolume(volume);
  }

  @Override
  public void disposeEngine() {
    engine.dispose();
  }

  @Override
  public void attachToEngineOutput(FlixelSoundBackend sound, int outputBusIndex) {
    if (sound instanceof FlixelMiniAudioSound mas) {
      engine.attachToEngineOutput(mas.getMASound(), outputBusIndex);
    }
  }

  @Override
  public FlixelSoundBackend.EffectNode createReverbNode(float wet) {
    MAReverbNode rev = new MAReverbNode(engine);
    float w = Math.max(0f, Math.min(1f, wet));
    rev.setWet(w);
    rev.setDry(1f - w);
    return new MiniAudioEffectNode(rev);
  }

  @Override
  public FlixelSoundBackend.EffectNode createDelayNode(float delaySeconds, float decay) {
    MADelayNode node = new MADelayNode(engine, delaySeconds, decay);
    return new MiniAudioEffectNode(node);
  }

  @Override
  public FlixelSoundBackend.EffectNode createLowPassFilter(double cutoffHz, int order) {
    MALowPassFilter lp = new MALowPassFilter(engine, cutoffHz, order);
    return new MiniAudioEffectNode(lp);
  }

  /**
   * Wraps a MiniAudio {@link MANode} as a {@link FlixelSoundBackend.EffectNode}.
   */
  private static final class MiniAudioEffectNode implements FlixelSoundBackend.EffectNode {

    private final MANode node;

    MiniAudioEffectNode(MANode node) {
      this.node = node;
    }

    @Override
    public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
      MANode upstreamNode;
      if (upstream instanceof FlixelMiniAudioSound mas) {
        upstreamNode = mas.getMASound();
      } else {
        return;
      }
      node.attachToThisNode(upstreamNode, bus);
    }

    @Override
    public void detach(int bus) {
      node.detach(bus);
    }

    @Override
    public void dispose() {
      node.dispose();
    }
  }
}
