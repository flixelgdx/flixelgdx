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
 * A web audio group: a set of sounds that can be paused, resumed, stopped, and volume-controlled
 * together.
 *
 * <p>Web Audio has no native concept of a group, so this class manages membership itself. Each
 * sound created for this group registers on construction and deregisters on disposal. Group-wide
 * volume is applied through a shared {@code GainNode} that every member routes through before
 * reaching the master output, so one {@link #setVolume(float)} call scales all members at once.
 * Pause and stop walk the member list and suspend or reset each one individually, because Web Audio
 * has no group-level transport control.
 */
public class FlixelWebAudioGroup implements FlixelSoundGroup {

  private final FlixelArray<FlixelWebAudioSound> sounds = new FlixelArray<>();

  private final GainNode gainNode;

  /**
   * Creates a group and connects its volume node to the master output.
   *
   * @param context The shared audio context.
   * @param master The master gain node all groups route into.
   */
  FlixelWebAudioGroup(AudioContext context, GainNode master) {
    this.gainNode = context.createGain();
    FlixelWebAudioFactory.connect(gainNode, master);
  }

  /** Returns the group's gain node, used by member sounds for their audio routing. */
  GainNode getGainNode() {
    return gainNode;
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
    return (float) gainNode.getGain().getValue();
  }

  @Override
  public void setVolume(float volume) {
    gainNode.getGain().setValue(volume);
  }

  @Override
  public void add(@NotNull FlixelSound sound) {
    if (sound instanceof FlixelWebAudioSound s) {
      register(s);
    }
  }

  @Override
  public void remove(@NotNull FlixelSound sound) {
    if (sound instanceof FlixelWebAudioSound s) {
      unregister(s);
    }
  }

  @Override
  public void destroy() {
    sounds.clear();
  }

  /**
   * Adds a sound to this group. Called by the sound during construction.
   *
   * @param sound The sound to track.
   */
  void register(FlixelWebAudioSound sound) {
    if (!sounds.contains(sound, true)) {
      sounds.add(sound);
    }
  }

  /**
   * Removes a sound from this group. Called when a sound is disposed.
   *
   * @param sound The sound to stop tracking.
   */
  void unregister(FlixelWebAudioSound sound) {
    sounds.removeValue(sound, true);
  }
}
