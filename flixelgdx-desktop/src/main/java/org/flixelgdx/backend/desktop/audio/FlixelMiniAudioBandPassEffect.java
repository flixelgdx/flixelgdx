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

import org.flixelgdx.audio.FlixelBandPassEffect;

/**
 * Miniaudio-backed {@link FlixelBandPassEffect}.
 *
 * <p>Delegates cutoff changes to the native bridge, which calls {@code ma_bpf_node_reinit}
 * to apply them without audible glitches.
 *
 * <p>Parameter ID 0 is the cutoff frequency in hertz. The Q (quality) factor is stored
 * Java-side for API symmetry; miniaudio's band-pass filter controls bandwidth through the
 * filter order rather than Q, so {@link #setQ(double)} does not affect the native node.
 */
class FlixelMiniAudioBandPassEffect extends FlixelMiniAudioEffect implements FlixelBandPassEffect {

  private double cutoff;
  private double q;

  FlixelMiniAudioBandPassEffect(long nodeHandle, float[] params) {
    super(nodeHandle);
    this.cutoff = params.length > 0 ? params[0] : 0.0;
    this.q = params.length > 1 ? params[1] : 1.0;
  }

  @Override
  public double getCutoff() {
    return cutoff;
  }

  @Override
  public double getQ() {
    return q;
  }

  @Override
  public void setCutoff(double hz) {
    cutoff = hz;
    setParam(0, (float) hz);
  }

  @Override
  public void setQ(double q) {
    this.q = q;
  }
}
