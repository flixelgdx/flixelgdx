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

/**
 * A bucket of sounds that pause, resume, and are categorized together.
 *
 * <p>{@link FlixelSoundManager} keeps two groups: one for sound effects and one for music, so
 * losing window focus can pause everything at once and each category can be controlled
 * independently. Create additional groups through {@link FlixelSoundFactory#createGroup()} and
 * pass them when creating or playing sounds.
 *
 * <p>Implementations are provided by the audio backend (miniaudio on native platforms, the Web
 * Audio API on web); game code only ever holds this interface.
 */
public interface FlixelSoundGroup extends FlixelDestroyable {

  /** Pauses every sound currently playing in this group. */
  void pause();

  /** Resumes every sound in this group that was playing when the group was paused. */
  void resume();
}
