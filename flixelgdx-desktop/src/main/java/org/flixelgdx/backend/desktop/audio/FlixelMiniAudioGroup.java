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
package org.flixelgdx.backend.desktop.audio;

import org.flixelgdx.audio.FlixelSound;
import org.flixelgdx.audio.FlixelSoundGroup;
import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link FlixelSoundGroup} backed by one miniaudio sound group.
 *
 * <p>Groups let the framework pause, resume, stop, and volume-control whole categories of audio
 * (such as sound effects or music) at once. Member sounds register themselves on construction and
 * deregister on disposal, so the group always reflects the live set.
 */
public class FlixelMiniAudioGroup implements FlixelSoundGroup {

  /** Native miniaudio group handle, or {@code 0} once destroyed. */
  private long handle;

  private final FlixelArray<FlixelSound> sounds = new FlixelArray<>();

  /**
   * Wraps a native group handle.
   *
   * @param handle The native handle from {@link FlixelMiniAudio#groupInit}.
   */
  FlixelMiniAudioGroup(long handle) {
    this.handle = handle;
  }

  /** Returns the native group handle, used when creating sounds in this group. */
  long getHandle() {
    return handle;
  }

  @Override
  public void pause() {
    if (handle != 0L) {
      FlixelMiniAudio.groupStop(handle);
    }
  }

  @Override
  public void resume() {
    if (handle != 0L) {
      FlixelMiniAudio.groupStart(handle);
    }
  }

  @Override
  public void stop() {
    if (handle != 0L) {
      FlixelMiniAudio.groupStop(handle);
    }
    for (int i = 0; i < sounds.getSize(); i++) {
      sounds.get(i).setTime(0f);
    }
  }

  @Override
  public float getVolume() {
    return handle != 0L ? FlixelMiniAudio.groupGetVolume(handle) : 1f;
  }

  @Override
  public void setVolume(float volume) {
    if (handle != 0L) {
      FlixelMiniAudio.groupSetVolume(handle, volume);
    }
  }

  @Override
  public void add(@NotNull FlixelSound sound) {
    if (!sounds.contains(sound, true)) {
      sounds.add(sound);
    }
  }

  @Override
  public void remove(@NotNull FlixelSound sound) {
    sounds.removeValue(sound, true);
  }

  @Override
  public void destroy() {
    if (handle != 0L) {
      FlixelMiniAudio.groupUninit(handle);
      handle = 0L;
    }
    sounds.clear();
  }
}
