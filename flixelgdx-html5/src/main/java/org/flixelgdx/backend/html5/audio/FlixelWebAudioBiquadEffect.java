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

import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webaudio.BiquadFilterNode;

/**
 * Shared base for the Web Audio biquad filter effects (low-pass, high-pass, band-pass).
 *
 * <p>All three types use a single {@code BiquadFilterNode} differing only in their type string and
 * which parameters they expose. This base handles the cutoff frequency that all three share and
 * routes {@code setParam(0, hz)} to the filter's frequency {@code AudioParam}.
 */
abstract class FlixelWebAudioBiquadEffect extends FlixelWebAudioEffect {

  private double cutoff;

  FlixelWebAudioBiquadEffect(@NotNull BiquadFilterNode filter, @NotNull String type, double cutoffHz) {
    super(filter);
    this.cutoff = cutoffHz;
    filter.setType(type);
    filter.getFrequency().setValue((float) cutoffHz);
  }

  @NotNull
  final BiquadFilterNode filter() {
    return (BiquadFilterNode) audioNode();
  }

  final double cutoff() {
    return cutoff;
  }

  final void applyCutoff(double hz) {
    cutoff = hz;
    filter().getFrequency().setValue((float) hz);
  }

  @Override
  public void setParam(int paramId, float value) {
    if (paramId == 0) {
      applyCutoff(value);
    }
  }

  @Override
  public float getParam(int paramId) {
    return paramId == 0 ? (float) cutoff : 0f;
  }
}
