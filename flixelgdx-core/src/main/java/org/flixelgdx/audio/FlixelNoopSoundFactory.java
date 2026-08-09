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
    return new NoopSound();
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
    private float cursorSeconds;

    private boolean looping;
    private boolean playing;

    @Override
    protected void backendPlay() {
      playing = true;
    }

    @Override
    protected void backendPause() {
      playing = false;
    }

    @Override
    protected void backendStop() {
      playing = false;
      cursorSeconds = 0f;
    }

    @Override
    protected boolean backendIsPlaying() {
      return playing;
    }

    @Override
    protected boolean backendIsEnd() {
      return !playing;
    }

    @Override
    protected float backendGetVolume() {
      return volume;
    }

    @Override
    protected void backendSetVolume(float volume) {
      this.volume = volume;
    }

    @Override
    protected void backendSetPitch(float pitch) {}

    @Override
    protected void backendSetPan(float pan) {}

    @Override
    protected float backendGetCursor() {
      return cursorSeconds;
    }

    @Override
    protected void backendSeek(float seconds) {
      cursorSeconds = seconds;
    }

    @Override
    protected float backendGetLength() {
      return 0f;
    }

    @Override
    protected boolean backendIsLooping() {
      return looping;
    }

    @Override
    protected void backendSetLooping(boolean looping) {
      this.looping = looping;
    }

    @Override
    protected void backendSetPosition(float x, float y, float z) {}

    @Override
    protected void backendDispose() {}

    @NotNull
    @Override
    protected FlixelReverbEffect backendCreateReverb(float wet) {
      return FlixelReverbEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelEchoEffect backendCreateEcho(float delaySeconds, float decay) {
      return FlixelEchoEffect.NOOP;
    }

    @NotNull
    @Override
    protected FlixelLowPassEffect backendCreateLowPass(double cutoffHz, int order) {
      return FlixelLowPassEffect.NOOP;
    }

    @Override
    protected void backendRouteTailToOutput(@NotNull FlixelSoundEffect tail) {}

    @Override
    protected void backendRestoreDirectRouting() {}
  }

  /** A group that tracks nothing. */
  private static final class NoopGroup implements FlixelSoundGroup {

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void destroy() {}
  }
}
