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
import org.flixelgdx.audio.FlixelSoundBuffer;
import org.flixelgdx.audio.FlixelSoundFactory;
import org.flixelgdx.audio.FlixelSoundGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.AudioNode;
import org.teavm.jso.webaudio.GainNode;

/**
 * The web audio backend, built on the browser's Web Audio API.
 *
 * <p>This is the web counterpart of the desktop miniaudio factory. It owns a single
 * {@code AudioContext} (the browser's mixing graph) and a master {@code GainNode} that every sound
 * routes through, so {@link #setMasterVolume(float)} scales the whole game at once. Each
 * {@link FlixelSound} it creates decodes its bytes through the context and plays through its own
 * chain of gain and pan nodes.
 *
 * <p>Browsers start an {@code AudioContext} in a suspended state and only allow it to produce sound
 * after the user has interacted with the page. To handle that transparently, the factory installs
 * one-time listeners that resume the context on the first click, key press, or touch, so games do
 * not have to think about the autoplay policy.
 */
public class FlixelWebAudioFactory implements FlixelSoundFactory {

  @NotNull
  private final AudioContext context;

  @NotNull
  private final GainNode masterGain;

  private FlixelWebAudioFactory(@NotNull AudioContext context, @NotNull GainNode masterGain) {
    this.context = context;
    this.masterGain = masterGain;
  }

  /**
   * Builds a factory with a fresh audio context and master gain wired to the speakers.
   *
   * @return A ready-to-use web audio factory.
   */
  @NotNull
  public static FlixelWebAudioFactory create() {
    AudioContext context = createContext();
    GainNode masterGain = context.createGain();
    connect(masterGain, context.getDestination());
    installResumeOnGesture(context);
    return new FlixelWebAudioFactory(context, masterGain);
  }

  @Override
  @NotNull
  public FlixelSound createSound(@NotNull FlixelSoundBuffer buffer, @Nullable FlixelSoundGroup group) {
    FlixelWebAudioGroup webGroup = group instanceof FlixelWebAudioGroup g ? g : null;
    return new FlixelWebAudioSound(context, masterGain, buffer, webGroup);
  }

  @Override
  @NotNull
  public FlixelSoundGroup createGroup() {
    return new FlixelWebAudioGroup();
  }

  @Override
  public void setMasterVolume(float volume) {
    masterGain.getGain().setValue(volume);
  }

  @Override
  public void destroyEngine() {
    context.close();
  }

  @JSBody(params = { "source", "target" }, script = "source.connect(target);")
  static native void connect(AudioNode source, AudioNode target);

  @JSBody(script = "return new (window.AudioContext || window.webkitAudioContext)();")
  private static native AudioContext createContext();

  @JSBody(params = "context", script = """
      var resume = function() {
        if (context.state === 'suspended') { context.resume(); }
        window.removeEventListener('pointerdown', resume);
        window.removeEventListener('keydown', resume);
        window.removeEventListener('touchstart', resume);
      };
      window.addEventListener('pointerdown', resume);
      window.addEventListener('keydown', resume);
      window.addEventListener('touchstart', resume);
      """)
  private static native void installResumeOnGesture(AudioContext context);
}
