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
package org.flixelgdx.audio;

import org.flixelgdx.functional.FlixelDestroyable;
import org.jetbrains.annotations.NotNull;

/**
 * A named bucket of sounds that share volume, pause, resume, and stop controls.
 *
 * <p>{@link FlixelSoundManager} keeps two built-in groups - one for sound effects and one for
 * music - so losing window focus pauses everything at once and each category can be controlled
 * independently. Create additional groups through {@link FlixelSoundFactory#createGroup()} and
 * pass them when creating sounds.
 *
 * <p>Group volume is a multiplier applied on top of each individual sound's volume, so setting
 * a group to {@code 0.5f} halves the perceived loudness of every sound in the group without
 * changing the sounds' own volume values.
 *
 * <p>Implementations are provided by the audio backend (miniaudio on native platforms, the Web
 * Audio API on web); game code only ever holds this interface.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlixelSoundGroup sfx = Flixel.sound.getSfxGroup();
 * sfx.setVolume(0.4f);    // turn SFX down
 * sfx.pause();            // hush everything during a cutscene
 * sfx.resume();           // back to normal
 * sfx.stop();             // stop and reset all sounds in the group
 * }</pre>
 */
public interface FlixelSoundGroup extends FlixelDestroyable {

  /** Pauses every sound currently playing in this group. */
  void pause();

  /** Resumes every sound in this group that was playing when the group was paused. */
  void resume();

  /** Stops every sound in this group and resets their positions to 0. */
  void stop();

  /**
   * Returns the current group volume multiplier.
   *
   * @return Volume in [0, 1] (or above).
   */
  float getVolume();

  /**
   * Sets the group volume multiplier applied to all sounds in the group.
   *
   * <p>This does not affect individual sound volumes; it scales the whole group's output.
   *
   * @param volume Volume multiplier; 0 silences the group, 1 is the default.
   */
  void setVolume(float volume);

  /**
   * Registers a sound as a member of this group.
   *
   * <p>Called automatically when a sound is created with this group; do not call this manually.
   *
   * @param sound The sound joining this group.
   */
  void add(@NotNull FlixelSound sound);

  /**
   * Removes a sound from this group.
   *
   * <p>Called automatically when a sound is destroyed; do not call this manually.
   *
   * @param sound The sound leaving this group.
   */
  void remove(@NotNull FlixelSound sound);
}
