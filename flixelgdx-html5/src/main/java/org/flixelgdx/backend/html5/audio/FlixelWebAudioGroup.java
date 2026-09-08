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
package org.flixelgdx.backend.html5.audio;

import org.flixelgdx.audio.FlixelSound;
import org.flixelgdx.audio.FlixelSoundGroup;
import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;

/**
 * A web audio group: a set of sounds that share volume, pause, resume, and stop controls.
 *
 * <p>Web Audio has no native group concept, so this class owns a {@code GainNode} that every
 * member sound connects its output through. The gain node connects directly to the master gain,
 * so setting group volume ({@link #setVolume(float)}) scales all member sounds at once without
 * touching their individual volumes.
 *
 * <p>Pause and resume walk the Java-side membership list and call per-sound suspend/wake hooks.
 * The group is constructed by {@link FlixelWebAudioFactory} with the shared context and master
 * gain; it is not designed to be created directly.
 */
public class FlixelWebAudioGroup implements FlixelSoundGroup {

  @NotNull
  private final GainNode groupGain;

  private final FlixelArray<FlixelWebAudioSound> sounds = new FlixelArray<>();

  private float volume = 1f;

  /**
   * Creates a group routed through a fresh gain node into the master gain.
   *
   * @param context The shared audio context.
   * @param masterGain The master gain node this group feeds into.
   */
  FlixelWebAudioGroup(@NotNull AudioContext context, @NotNull GainNode masterGain) {
    this.groupGain = context.createGain();
    FlixelWebAudioFactory.connect(groupGain, masterGain);
  }

  /** Returns the group gain node so sounds created for this group can connect through it. */
  @NotNull
  GainNode getGroupGain() {
    return groupGain;
  }

  @Override
  public void pause() {
    for (int i = 0; i < sounds.getSize(); i++) {
      sounds.get(i).suspendForGroup();
    }
  }

  @Override
  public void resume() {
    for (int i = 0; i < sounds.getSize(); i++) {
      sounds.get(i).resumeForGroup();
    }
  }

  @Override
  public void stop() {
    for (int i = 0; i < sounds.getSize(); i++) {
      sounds.get(i).stop();
    }
  }

  @Override
  public float getVolume() {
    return volume;
  }

  @Override
  public void setVolume(float volume) {
    this.volume = volume;
    groupGain.getGain().setValue(volume);
  }

  @Override
  public void add(@NotNull FlixelSound sound) {
    if (sound instanceof FlixelWebAudioSound webSound && !sounds.contains(webSound, true)) {
      sounds.add(webSound);
    }
  }

  @Override
  public void remove(@NotNull FlixelSound sound) {
    if (sound instanceof FlixelWebAudioSound webSound) {
      sounds.removeValue(webSound, true);
    }
  }

  @Override
  public void destroy() {
    sounds.clear();
  }
}
