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
package org.flixelgdx.backend.common.audio;

import org.flixelgdx.audio.FlixelSoundBackend;

import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.effect.MADelayNode;
import games.rednblack.miniaudio.effect.MAReverbNode;
import games.rednblack.miniaudio.filter.MALowPassFilter;

/**
 * JVM-family implementation of {@link FlixelSoundBackend.Factory} backed by the MiniAudio native
 * library. Shared by desktop (LWJGL3), Android, and iOS launchers that load native MiniAudio.
 *
 * <p>This factory owns a single {@link MiniAudio} engine instance that is created in the
 * constructor and disposed when {@link #disposeEngine()} is called. All sounds and groups are
 * created through the engine.
 */
public class FlixelMiniAudioSoundHandler implements FlixelSoundBackend.Factory {

  private final MiniAudio engine;

  /** Creates the handler and initializes the MiniAudio engine. */
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
  private record MiniAudioEffectNode(MANode node) implements FlixelSoundBackend.EffectNode {

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
