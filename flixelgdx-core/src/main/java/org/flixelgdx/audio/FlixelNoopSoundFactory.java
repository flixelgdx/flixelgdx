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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Silent {@link FlixelSoundFactory} used when no audio backend is installed (headless sessions,
 * unit tests, and pre-startup code paths).
 *
 * <p>Every sound it creates behaves like a zero-length clip that never plays, so gameplay code
 * that fires sound effects runs unchanged with no audio hardware present.
 */
public enum FlixelNoopSoundFactory implements FlixelSoundFactory {

  /** Shared silent instance. */
  INSTANCE;

  @NotNull
  @Override
  public FlixelSound createSound(@NotNull FlixelSoundBuffer buffer, @Nullable FlixelSoundGroup group) {
    NoopSound sound = new NoopSound();
    if (group != null) {
      sound.setGroup(group);
    }
    return sound;
  }

  @NotNull
  @Override
  public FlixelSoundGroup createGroup() {
    return new NoopGroup();
  }

  @Override
  public void setMasterVolume(float volume) {}

  @Override
  public void destroyEngine() {}

  /** A sound with no voice behind it; all state is remembered, nothing is heard. */
  private static final class NoopSound extends FlixelSound {

    private float volume = 1f;
    private float cursorMs;
    private float pitch = 1f;
    private float pan;

    private boolean looping;
    private boolean playing;

    @NotNull
    @Override
    public FlixelSound resume() {
      playing = true;
      return this;
    }

    @NotNull
    @Override
    public FlixelSound pause() {
      playing = false;
      return this;
    }

    @Override
    protected boolean isEnd() {
      return !playing;
    }

    @Override
    public float getVolume() {
      return volume;
    }

    @Override
    public FlixelSound setVolume(float volume) {
      this.volume = volume;
      return this;
    }

    @Override
    public float getPitch() {
      return pitch;
    }

    @Override
    public FlixelSound setPitch(float pitch) {
      this.pitch = pitch;
      return this;
    }

    @Override
    public float getPan() {
      return pan;
    }

    @Override
    public FlixelSound setPan(float pan) {
      this.pan = pan;
      return this;
    }

    @Override
    public float getTime() {
      return cursorMs;
    }

    @Override
    public FlixelSound setTime(float timeMs) {
      cursorMs = timeMs;
      return this;
    }

    @Override
    public float getLength() {
      return 0f;
    }

    @Override
    public boolean isLooped() {
      return looping;
    }

    @Override
    public FlixelSound setLooped(boolean looped) {
      this.looping = looped;
      return this;
    }

    @Override
    public boolean isPlaying() {
      return playing;
    }

    @Override
    protected void applyPosition(float x, float y, float z) {}

    @Override
    protected void disposeAudio() {}

    @Override
    protected void wireEffectNode(@NotNull FlixelSoundEffect node, @Nullable FlixelSoundEffect upstream) {}

    @Override
    protected void restoreDirectRouting() {}

    @NotNull
    @Override
    protected FlixelReverbEffect createReverbEffect(float wet) {
      return FlixelReverbEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelEchoEffect createEchoEffect(float delaySeconds, float decay) {
      return FlixelEchoEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelLowPassEffect createLowPassEffect(double cutoffHz, int order) {
      return FlixelLowPassEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelHighPassEffect createHighPassEffect(double cutoffHz, int order) {
      return FlixelHighPassEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelBandPassEffect createBandPassEffect(double cutoffHz, double q, int order) {
      return FlixelBandPassEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelSoundEffect createNode(int typeId, float[] params) {
      return FlixelSoundEffect.NOOP;
    }
  }

  /** A group that tracks nothing. */
  private static final class NoopGroup implements FlixelSoundGroup {

    private float volume = 1f;

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void stop() {}

    @Override
    public float getVolume() {
      return volume;
    }

    @Override
    public void setVolume(float volume) {
      this.volume = volume;
    }

    @Override
    public void add(@NotNull FlixelSound sound) {}

    @Override
    public void remove(@NotNull FlixelSound sound) {}

    @Override
    public void destroy() {}
  }
}
