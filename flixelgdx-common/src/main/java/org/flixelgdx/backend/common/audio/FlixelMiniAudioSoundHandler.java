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

  @Override
  public FlixelSoundBackend createSound(String path, short flags, Object group, boolean external) {
    MAGroup maGroup = (group instanceof MAGroup g) ? g : null;
    MASound ma = engine.createSound(path, flags, maGroup, external);
    return new FlixelMiniAudioSound(ma, maGroup);
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
    if (!(sound instanceof FlixelMiniAudioSound mas)) {
      return;
    }
    MAGroup group = mas.getGroup();
    if (group != null) {
      // Reconnect the sound directly to its group, restoring the routing that existed
      // before any effect chain was added. Group-level pause then controls this sound again.
      group.attachToThisNode(mas.getMASound(), outputBusIndex);
    } else {
      engine.attachToEngineOutput(mas.getMASound(), outputBusIndex);
    }
  }

  @Override
  public void attachEffectToEngineOutput(FlixelSoundBackend.EffectNode node, int outputBusIndex) {
    MANode n = nodeOf(node);
    if (n != null) {
      engine.attachToEngineOutput(n, outputBusIndex);
    }
  }

  @Override
  public void attachEffectToEngineOutput(FlixelSoundBackend.EffectNode node, int outputBusIndex,
      FlixelSoundBackend sound) {
    MANode n = nodeOf(node);
    if (n == null) {
      return;
    }
    MAGroup group = (sound instanceof FlixelMiniAudioSound mas) ? mas.getGroup() : null;
    if (group != null) {
      // Route the tail of the effect chain through the group so that groupPause() and
      // groupPlay() still gate audio even while effects are active.
      group.attachToThisNode(n, outputBusIndex);
    } else {
      engine.attachToEngineOutput(n, outputBusIndex);
    }
  }

  @Override
  public FlixelSoundBackend.ReverbNode createReverbNode(float wet) {
    MAReverbNode rev = new MAReverbNode(engine);
    float w = Math.max(0f, Math.min(1f, wet));
    rev.setWet(w);
    rev.setDry(1f - w);
    return new MiniAudioReverbNode(rev, w, 1f - w);
  }

  @Override
  public FlixelSoundBackend.EchoNode createDelayNode(float delaySeconds, float decay) {
    return new MiniAudioEchoNode(new MADelayNode(engine, delaySeconds, decay));
  }

  @Override
  public FlixelSoundBackend.LowPassNode createLowPassFilter(double cutoffHz, int order) {
    return new MiniAudioLowPassNode(new MALowPassFilter(engine, cutoffHz, order), order, cutoffHz);
  }

  /** Returns the underlying MiniAudio engine for advanced or asset-loader use. */
  public MiniAudio getEngine() {
    return engine;
  }

  private static MANode nodeOf(FlixelSoundBackend.EffectNode n) {
    if (n instanceof MiniAudioReverbNode r)
      return r.node;
    if (n instanceof MiniAudioEchoNode e)
      return e.node;
    if (n instanceof MiniAudioLowPassNode lp)
      return lp.node;
    return null;
  }

  /** Reverb effect node backed by {@link MAReverbNode}. */
  private static final class MiniAudioReverbNode implements FlixelSoundBackend.ReverbNode {

    private final MAReverbNode node;
    private float wet;
    private float dry;
    private float roomSize = 0.7f;
    private float damping = 0.5f;
    private float width = 1.0f;
    private boolean frozen;

    MiniAudioReverbNode(MAReverbNode node, float wet, float dry) {
      this.node = node;
      this.wet = wet;
      this.dry = dry;
    }

    @Override
    public float getWet() {
      return wet;
    }

    @Override
    public float getDry() {
      return dry;
    }

    @Override
    public float getRoomSize() {
      return roomSize;
    }

    @Override
    public float getDamping() {
      return damping;
    }

    @Override
    public float getWidth() {
      return width;
    }

    @Override
    public boolean isFrozen() {
      return frozen;
    }

    @Override
    public void setWet(float wet) {
      this.wet = Math.max(0f, Math.min(1f, wet));
      node.setWet(this.wet);
    }

    @Override
    public void setDry(float dry) {
      this.dry = Math.max(0f, Math.min(1f, dry));
      node.setDry(this.dry);
    }

    @Override
    public void setRoomSize(float size) {
      roomSize = Math.max(0f, Math.min(1f, size));
      node.setRoomSize(roomSize);
    }

    @Override
    public void setDamping(float damping) {
      this.damping = Math.max(0f, Math.min(1f, damping));
      node.setDumping(this.damping);
    }

    @Override
    public void setWidth(float width) {
      this.width = Math.max(0f, Math.min(1f, width));
      node.setWidth(this.width);
    }

    @Override
    public void setFrozen(boolean frozen) {
      this.frozen = frozen;
      node.setMode(frozen ? 1f : 0f);
    }

    @Override
    public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
      if (upstream instanceof FlixelMiniAudioSound mas) {
        node.attachToThisNode(mas.getMASound(), bus);
      }
    }

    @Override
    public void attachToUpstreamNode(FlixelSoundBackend.EffectNode upstream, int bus) {
      MANode up = nodeOf(upstream);
      if (up != null) {
        node.attachToThisNode(up, bus);
      }
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

  /** Echo / delay effect node backed by {@link MADelayNode}. */
  private record MiniAudioEchoNode(MADelayNode node) implements FlixelSoundBackend.EchoNode {

    @Override
    public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
      if (upstream instanceof FlixelMiniAudioSound mas) {
        node.attachToThisNode(mas.getMASound(), bus);
      }
    }

    @Override
    public void attachToUpstreamNode(FlixelSoundBackend.EffectNode upstream, int bus) {
      MANode up = nodeOf(upstream);
      if (up != null) {
        node.attachToThisNode(up, bus);
      }
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

  /** Low-pass filter effect node backed by {@link MALowPassFilter}. */
  private static final class MiniAudioLowPassNode implements FlixelSoundBackend.LowPassNode {

    private double cutoffHz;
    private final MALowPassFilter node;
    private final int order;

    MiniAudioLowPassNode(MALowPassFilter node, int order, double cutoffHz) {
      this.node = node;
      this.order = order;
      this.cutoffHz = cutoffHz;
    }

    @Override
    public double getCutoff() {
      return cutoffHz;
    }

    @Override
    public void setCutoff(double hz) {
      cutoffHz = hz;
      node.reinit(hz);
    }

    @Override
    public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
      if (upstream instanceof FlixelMiniAudioSound mas) {
        node.attachToThisNode(mas.getMASound(), bus);
      }
    }

    @Override
    public void attachToUpstreamNode(FlixelSoundBackend.EffectNode upstream, int bus) {
      MANode up = nodeOf(upstream);
      if (up != null) {
        node.attachToThisNode(up, bus);
      }
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
