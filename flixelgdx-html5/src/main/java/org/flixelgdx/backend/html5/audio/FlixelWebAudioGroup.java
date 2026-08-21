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

import org.flixelgdx.audio.FlixelSoundGroup;
import org.flixelgdx.collections.FlixelArray;

/**
 * A web audio group: a set of sounds that pause and resume together.
 *
 * <p>Web Audio has no native concept of a group, so this class keeps the membership itself. Each
 * sound created for this group registers on construction; {@link #pause()} and {@link #resume()}
 * then walk the members and suspend or wake each one. That is how a game can, for example, silence
 * every gameplay sound at once when a menu opens while leaving the menu music untouched.
 */
public class FlixelWebAudioGroup implements FlixelSoundGroup {

  private final FlixelArray<FlixelWebAudioSound> sounds = new FlixelArray<>();

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
